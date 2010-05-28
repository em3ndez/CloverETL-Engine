/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.database.dbf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Properties;

import org.jetel.graph.runtime.EngineInitializer;
import org.jetel.metadata.DataFieldMetadata;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.metadata.DataRecordMetadataXMLReaderWriter;
import org.jetel.util.string.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * @author DPavlis
 * @since  29.6.2004
 *
 * Class which analyzes dBase/FoxBase/FoxPRO table/file and extracts metadata about defined fields.
 * Used by DBFDataReader to which it provides neccessary info for reading DBF records.
 */


@SuppressWarnings("EI")
public class DBFAnalyzer {
	
	private static final int DBF_HEADER_SIZE_BASIC=32;
	private static final int DBF_FIELD_DEF_SIZE=32;
	private static final int DBF_FIELD_DEF_SIZE_ENHANCE=48;
    private static final byte DBF_FIELD_HEADER_TERMINATOR=0x0D;
    private static final byte DBF_ENHANCE_RESERVED=4;
	private static final String HEADER_CHARACTER_ENCODING="ISO-8859-1";
	private final static String VERSION = "1.1";
	private final static String LAST_UPDATED = "2006/10/03";  
	
	private ByteBuffer buffer;
	private int dbfNumRows;
	private int dbfNumFields;
	private int dbfRecSize;
	private DBFFieldMetadata[] dbfFields;
	private byte dbfType;
	private int dbfDataOffset;
	private byte dbfCodePage;
	private Charset charset;
	private String dbfTableName;
    
	public DBFAnalyzer(){
		reset();
	}

	void analyze(String dbfFileName) throws IOException,DBFErrorException{
		FileChannel dbfFile=new FileInputStream(dbfFileName).getChannel();
		analyze(dbfFile,new File(dbfFileName).getName());
		dbfFile.close();
	}

	private void reset(){
		dbfNumRows = -1;
		dbfNumFields = -1;
		dbfRecSize = -1;
		dbfFields = new DBFFieldMetadata[0];
		dbfType = 0;
		dbfDataOffset = -1;
		dbfCodePage = 0;
		dbfTableName = null;
	}
	
	public int analyze(ReadableByteChannel dbfFile,String dbfTableName)throws IOException,DBFErrorException{

		buffer=ByteBuffer.allocate(DBF_HEADER_SIZE_BASIC);
	    buffer.order(ByteOrder.LITTLE_ENDIAN);
	    int read = DBF_HEADER_SIZE_BASIC;
		int count=dbfFile.read(buffer);
		if (count!=32){
			throw new DBFErrorException("Problem reading DBF header - too short !");
		}
		this.dbfTableName=dbfTableName;
		
		buffer.flip();
		// read-in basic table definition
		buffer.position(0);
		dbfType=buffer.get();
       
		buffer.position(4);
		dbfNumRows=buffer.getInt();

		buffer.position(8);
		dbfDataOffset=buffer.getShort();
       
		buffer.position(29);
		dbfCodePage=buffer.get();
       
		buffer.position(10);
		dbfRecSize=buffer.getShort();

        int filedInfoLength=dbfDataOffset-DBF_HEADER_SIZE_BASIC;//dbfNumFields*DBF_FIELD_DEF_SIZE+1;
		if (filedInfoLength < 0){
			reset();
			throw new DBFErrorException("Invalid header!");
		}
		buffer=ByteBuffer.allocate(filedInfoLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        read += filedInfoLength;
        if (dbfFile.read(buffer)!=filedInfoLength){
            reset();
            throw new DBFErrorException("Problem reading DBF fields directory - too short !");
        }
        buffer.flip();

        // handle differently FoxPro and dBaseIII
		//dbfNumFields = (dbfType>0x03) ? (dbfDataOffset-296)/32 : (dbfDataOffset-33)/32;
        int subFieldEofMark = findSubRecordEofMark(buffer);
        if (subFieldEofMark == -1) {
            reset();
            throw new DBFErrorException("Problem reading DBF fields directory - wrong format !");
        }
        boolean shortFieldDesc = subFieldEofMark%32 == 0;
        
		if (shortFieldDesc) {
			dbfNumFields = subFieldEofMark / DBF_FIELD_DEF_SIZE;
	        buffer.position(0);
	        
	        // read-in definition of individual fields
	        int offset=0;
	        dbfFields=new DBFFieldMetadata[dbfNumFields];
	        charset=Charset.forName(HEADER_CHARACTER_ENCODING);
	        for(int i=0;i<dbfNumFields;i++){
	            dbfFields[i]=new DBFFieldMetadata();
	            buffer.limit(11+offset);
	            dbfFields[i].name=charset.decode(buffer).toString().trim();
	            buffer.limit(12+offset);
	            dbfFields[i].type=charset.decode(buffer).get();
	            buffer.limit(32+offset);
	            dbfFields[i].offset=buffer.getInt();
	            dbfFields[i].length=buffer.get();
	            dbfFields[i].decPlaces=buffer.get();
	            offset+=DBF_FIELD_DEF_SIZE;
	            buffer.position(offset);
	        }
		} else {
			if (subFieldEofMark <=DBF_FIELD_DEF_SIZE+DBF_ENHANCE_RESERVED) {
				buffer.position(DBF_FIELD_DEF_SIZE+DBF_ENHANCE_RESERVED);
				subFieldEofMark = findSubRecordEofMark(buffer);
				dbfNumFields = (subFieldEofMark) / DBF_FIELD_DEF_SIZE_ENHANCE;
			} else {
				dbfNumFields = (subFieldEofMark-DBF_FIELD_DEF_SIZE-DBF_ENHANCE_RESERVED) / DBF_FIELD_DEF_SIZE_ENHANCE;
			}
	        buffer.position(DBF_FIELD_DEF_SIZE+DBF_ENHANCE_RESERVED);
			
	        // read-in definition of individual fields
	        int offset=DBF_FIELD_DEF_SIZE+DBF_ENHANCE_RESERVED;
	        dbfFields=new DBFFieldMetadata[dbfNumFields];
	        charset=Charset.forName(HEADER_CHARACTER_ENCODING);
	        for(int i=0;i<dbfNumFields;i++){
	            dbfFields[i]=new DBFFieldMetadata();
	            buffer.limit(32+offset);
	        	buffer.position(offset);
	            dbfFields[i].name=charset.decode(buffer).toString().trim();
	            buffer.limit(33+offset);
	            dbfFields[i].type=charset.decode(buffer).get();
	            buffer.limit(34+offset);
	            dbfFields[i].length=buffer.get();
	            buffer.limit(35+offset);
	            dbfFields[i].decPlaces=buffer.get();
	            //dbfFields[i].offset=buffer.getInt();
	            offset+=DBF_FIELD_DEF_SIZE_ENHANCE;
	            buffer.position(offset);
	        }
		}
		
		// let's construct the valid character decoder based on info found in table header
		try{
			charset=Charset.forName(DBFTypes.dbfCodepage2Java(dbfCodePage));
		}catch (Exception ex){
			throw new DBFErrorException("Unsupported DBF codepage ID: "+dbfCodePage);
		}
		return read;
	}

	private int findSubRecordEofMark(ByteBuffer buffer) {
		int fEof;
		int fMax = buffer.limit();
		for (fEof = 0; fEof < fMax ;fEof+=8) {
			if (buffer.get(fEof) == DBF_FIELD_HEADER_TERMINATOR) return fEof;
		}
		return -1;
	}
	
	/**
	 * Main method which allows analyzing dBase file/table. It extracts Clover-style
	 * metadata from information specified in dBase table header.
	 * 
	 * @param args
	 */
	public static void main(String[] args){
	    int argsOffset=0;
        boolean verbose=false;
        
		if (args.length==0){
            System.out.println("***  CloverETL DBFAnalyzer (" + VERSION + ") created on "+LAST_UPDATED+" (c) 2002-06 D.Pavlis, released under GNU Lesser General Public license ***");
            System.out.println("Usage: DBFAnalyzer [-v(erbose)] <DBF filename> [<metadata output filename>]");
			System.exit(-1);
		}
       
		DBFAnalyzer dbf = new DBFAnalyzer();
		
        
        if (args[0].startsWith("-v")){
            argsOffset++;
            verbose=true;
        }
        
		try{
			dbf.analyze(args[0+argsOffset]);
		}catch(Exception ex){
            throw new RuntimeException(ex);
		}
        
        if(verbose){
        System.out.println("DBF type: "+ dbf.dbfType +" - "+dbf.getDBFTypeName(dbf.dbfType));
        System.out.println("Number of rows in table: "+dbf.dbfNumRows);
        System.out.println("Number of fields in table: "+dbf.dbfNumFields);
        System.out.println("Codepage: "+dbf.dbfCodePage+" - corresponds to: "+DBFTypes.dbfCodepage2Java(dbf.dbfCodePage));
        System.out.println("Record size (bytes): "+dbf.dbfRecSize);
        }
        
        EngineInitializer.initEngine((String)null, null, null);
        
		try{
			OutputStream outstream;
			if (args.length<2+argsOffset){
				outstream=System.out;
			}else{
				outstream=new BufferedOutputStream(new FileOutputStream(args[1+argsOffset]));
			}
			DataRecordMetadataXMLReaderWriter.write(dbf.getCloverMetadata(), outstream);
		}catch(IOException ex){
			System.err.println(ex.getMessage());
			System.exit(-1);
		}
		System.exit(0);
	}


	/**
	 * @return Returns the dbfFields array - contains description if individual
	 * fields in dBase table header. If analyzed file has invalid header empty array is returned. 
	 */
	public DBFFieldMetadata[] getFields() {
		return dbfFields;
	}
	/**
	 * @return Returns number of fields defined in table. If analyzed file has invalid header returns -1.
	 */
	public int getNumFields() {
		return dbfNumFields;
	}
	/**
	 * @return Returns number of rows present in dBase data file. If analyzed file has invalid header returns -1.
	 */
	public int getNumRows() {
		return dbfNumRows;
	}
	/**
	 * @return Returns the dbfType - can be used to distinguish which dBase
	 * version (dBase, FoxBase, FoxPRO) created the data file. If analyzed file has invalid header returns 0.
	 */
	public byte getDBFType() {
		return dbfType;
	}
	
	/**
	 * Method which returns name (String) of the dBase variant which
	 * created analyzed DBF file. For unknown types or if analyzed file has invalid header returns null.
	 * 
	 * @return
	 */
	public String getDBFTypeName(byte type){
	    for (int i=0;i<DBFTypes.KNOWN_TYPES.length;i++){
	        if (type==DBFTypes.KNOWN_TYPES[i]){
	            return DBFTypes.KNOWN_TYPES_NAMES[i];
	        }
	    }
	    return null;
	}
	
	/**
	 * @return Returns the dbfRecSize. If analyzed file has invalid header returns -1.
	 */
	public int getRecSize() {
		return dbfRecSize;
	}
	
	/**
	 * @param dBase field type
	 * @return CloverETL field type
	 */
	@SuppressWarnings("DB")
	public static char dbfFieldType2Clover(char type){
		switch(Character.toUpperCase(type)){
			case 'C': return DataFieldMetadata.STRING_FIELD;
			case 'N': return DataFieldMetadata.NUMERIC_FIELD;
			case 'D': return DataFieldMetadata.DATE_FIELD;
			case 'L': return DataFieldMetadata.BOOLEAN_FIELD;
			case 'M': return DataFieldMetadata.BYTE_FIELD;
			default: return DataFieldMetadata.STRING_FIELD;
            //throw new DBFErrorException("Unsupported DBF field type: \""+String.valueOf(type)+"\" hex: "+Integer.toHexString(type));
		}
	}
	
	public DataRecordMetadata getCloverMetadata(){
		DataRecordMetadata record=new DataRecordMetadata(StringUtils.normalizeName(dbfTableName),
							DataRecordMetadata.FIXEDLEN_RECORD);
	
		// set record properties - additional info for DBF-type of data 
		Properties recProp=new Properties();
		recProp.setProperty(DBFTypes.DATA_OFFSET_XML_ATTRIB_NAME,String.valueOf(dbfDataOffset));
		recProp.setProperty(DBFTypes.DATA_ENCODING_XML_ATTRIB_NAME,DBFTypes.dbfCodepage2Java(dbfCodePage));
		recProp.setProperty(DBFTypes.RECORD_SIZE_XML_ATTRIB_NAME,String.valueOf(dbfRecSize));
		record.setRecordProperties(recProp);
		
		// add "hidden" field indicatind deletion status
		record.addField(new DataFieldMetadata("_IS_DELETED_",DataFieldMetadata.STRING_FIELD,(short)1));
		for (int i=0;i<dbfNumFields;i++){
			// create field definition based on what we read from DBF file header
			DataFieldMetadata field=new DataFieldMetadata(dbfFields[i].name,
					dbfFieldType2Clover(dbfFields[i].type),
					dbfFields[i].length);

			// if DATE DBF type, then set format/date mask
			if (dbfFields[i].type=='D'){
				field.setFormatStr(DBFTypes.DATE_FORMAT_MASK);			
			}
			record.addField(field);
		}
		return record;
	}
	/**
	 * @return Returns the dbfCodePage. If analyzed file has invalid header returns 0.
	 */
	public byte getDBFCodePage() {
		return dbfCodePage;
	}
	/**
	 * @return Returns the dbfDataOffset. If analyzed file has invalid header returns -1.
	 */
	public int getDBFDataOffset() {
		return dbfDataOffset;
	}
}
