/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002-04  David Pavlis <david_pavlis@hotmail.com>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/
package org.jetel.database;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.metadata.DataFieldMetadata;
import org.jetel.metadata.DataRecordMetadata;

/**
 *  Various utilities for working with Databases
 *
 * @author      dpavlis
 * @since       September 25, 2002
 * @revision    $Revision: 1.10
 * @created     January 24, 2003
 */
public class SQLUtil {

	private final static String DEFAULT_DELIMITER = ";";
	private final static String END_RECORD_DELIMITER = "\n";

	static Log logger = LogFactory.getLog(SQLUtil.class);

	/**
	 *  Creates SQL insert statement based on metadata describing data flow and
	 *  supplied table name
	 *
	 * @param  metadata   Metadata describing data flow from which to feed database
	 * @param  tableName  Table name into which insert data
	 * @return            string containing SQL insert statement
	 * @since             October 2, 2002
	 */
	public static String assembleInsertSQLStatement(DataRecordMetadata metadata, String tableName) {
		StringBuffer strBuf = new StringBuffer();
		//StringBuffer strBuf2 = new StringBuffer();

		strBuf.append(" values(");
		for (int i = 0; i < metadata.getNumFields(); i++) {
			//strBuf2.append(metadata.getField(i).getName());
			strBuf.append("?");
			if (i < metadata.getNumFields() - 1) {
				strBuf.append(",");
				//strBuf2.append(",");
			}
		}
		//strBuf.insert(0, strBuf2.toString());
		//strBuf.insert(0, " (");
		strBuf.insert(0, tableName);
		strBuf.insert(0, "insert into ");
		strBuf.append(")");
		if (logger.isDebugEnabled()) {
			logger.debug(strBuf.toString());
		}
		return strBuf.toString();
	}


	/**
	 *  Description of the Method
	 *
	 * @param  tableName  Description of the Parameter
	 * @param  dbFields   Description of the Parameter
	 * @return            Description of the Return Value
	 */
	public static String assembleInsertSQLStatement(String tableName, String[] dbFields) {
		StringBuffer strBuf = new StringBuffer("insert into ");

		strBuf.append(tableName).append(" (");

		for (int i = 0; i < dbFields.length; i++) {
			strBuf.append(dbFields[i]);
			if (i < dbFields.length - 1) {
				strBuf.append(", ");
			}
		}
		strBuf.append(") values (");

		for (int i = 0; i < dbFields.length; i++) {
			strBuf.append("?");
			if (i < dbFields.length - 1) {
				strBuf.append(",");
			}
		}
		strBuf.append(")");
		return strBuf.toString();
	}


	/**
	 *  Converts SQL metadata into Clover's DataRecordMetadata
	 *
	 * @param  dbMetadata        SQL ResultSet metadata describing which columns are
	 *      returned by query
	 * @return                   DataRecordMetadata which correspond to the SQL
	 *      ResultSet
	 * @exception  SQLException  Description of the Exception
	 */
	public static DataRecordMetadata dbMetadata2jetel(ResultSetMetaData dbMetadata) throws SQLException {
		DataFieldMetadata fieldMetadata;
		DataRecordMetadata jetelMetadata = new DataRecordMetadata(dbMetadata.getTableName(1),
				DataRecordMetadata.DELIMITED_RECORD);

		for (int i = 1; i <= dbMetadata.getColumnCount(); i++) {
			try {
				fieldMetadata = new DataFieldMetadata(dbMetadata.getColumnName(i), DEFAULT_DELIMITER);

			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage() + " field name " + dbMetadata.getColumnName(i));
			}
			// set proper data type
			fieldMetadata.setType(SQLUtil.sqlType2jetel(dbMetadata.getColumnType(i)));

			if (i == dbMetadata.getColumnCount()) {
				fieldMetadata.setDelimiter(END_RECORD_DELIMITER);
			}

			if (dbMetadata.isNullable(i) == ResultSetMetaData.columnNullable) {
				fieldMetadata.setNullable(true);
			}
			/*
			 *  this is not safe - at least Oracle JDBC driver reports NUMBER to be currency
			 *  if (dbMetadata.isCurrency(i)) {
			 *  fieldMetadata.setFormatStr("�#.#");
			 *  }
			 */
			jetelMetadata.addField(fieldMetadata);
		}
		return jetelMetadata;
	}


	/**
	 *  For specified table returns names of individual fileds
	 *
	 * @param  conn       database connection
	 * @param  tableName  name of DB table
	 * @return            array of field names
	 */
	public static String[] getColumnNames(Connection conn, String tableName) {
		List tmp = new ArrayList();
		String[] out = null;
		try {
			ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, "%");

			while (rs.next()) {
				// FIELD NAME - 4 column in resultset
				// get DATA TYPE - 5 column in result set from Database metadata
				//out.add(rs.getString(4).toUpperCase(), new Integer(rs.getInt(5)));
				tmp.add(rs.getString(4));
			}
			out = new String[tmp.size()];
			tmp.toArray(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return out;
	}


	/**
	 *  Gets the FieldTypes of fields present in specified DB table
	 *
	 * @param  metadata          Description of Parameter
	 * @param  tableName         name of the table for which to get metadata (field names, types)
	 * @return                   list of  JDBC FieldTypes
	 * @exception  SQLException  Description of Exception
	 * @since                    October 4, 2002
	 * @see                      java.sql.DatabaseMetaData
	 */
	public static List getFieldTypes(DatabaseMetaData metadata, String tableName) throws SQLException {
		String[] tableSpec = new String[]{null, tableName.toUpperCase()};
		if (tableName.indexOf(".") != -1) {
			tableSpec = tableName.toUpperCase().split("\\.", 2);
		}
		ResultSet rs = metadata.getColumns(null, tableSpec[0], tableSpec[1], "%");//null as last parm
		List fieldTypes = new LinkedList();
		while (rs.next()) {
			// get DATA TYPE - fifth column in result set from Database metadata
			fieldTypes.add(new Integer(rs.getInt(5)));
		}
		if (fieldTypes.size() == 0) {
			//throw new RuntimeException("No metadata obtained for table: " + tableName);
			//Warn !
			logger.warn("No metadata obtained for table: \"" + tableName + "\", using workaround ...");
			// WE HAVE SOME PATCH, but ...
			ResultSetMetaData fieldsMetadata = getTableFieldsMetadata(metadata.getConnection(), tableName);
			for (int i = 0; i < fieldsMetadata.getColumnCount(); i++) {
				fieldTypes.add(new Integer(fieldsMetadata.getColumnType(i + 1)));
			}
		}

		return fieldTypes;
	}


	/**
	 *  Gets the FieldTypes of fields (enumerated in dbFields) present in specified DB table
	 *
	 * @param  metadata          Description of the Parameter
	 * @param  tableName         name of the table for which to get metadata (field names, types)
	 * @param  dbFields          array of field names
	 * @return                   list of  JDBC FieldTypes
	 * @exception  SQLException  Description of the Exception
	 */
	public static List getFieldTypes(DatabaseMetaData metadata, String tableName, String[] dbFields) throws SQLException {
		String[] tableSpec = new String[]{null, tableName.toUpperCase()};
		// if schema defined in table name, extract schema & table name into separate fields
		if (tableName.indexOf(".") != -1) {
			tableSpec = tableName.toUpperCase().split("\\.", 2);
		}
		ResultSet rs = metadata.getColumns(null, tableSpec[0], tableSpec[1], "%");//null as last parm
		Map dbFieldsMap = new HashMap();
		List fieldTypes = new LinkedList();
		Integer dataType;

		while (rs.next()) {
			// FIELD NAME - fourth columnt in resutl set
			// get DATA TYPE - fifth column in result set from Database metadata
			dbFieldsMap.put(rs.getString(4).toUpperCase(), new Integer(rs.getInt(5)));
		}
		if (dbFieldsMap.size() == 0) {
			//throw new RuntimeException("No metadata obtained for table: " + tableName);
			//Warn !
			logger.warn("No metadata obtained for table: \"" + tableName + "\", using workaround ...");
			// WE HAVE SOME PATCH, but ...
			ResultSetMetaData fieldsMetadata = getTableFieldsMetadata(metadata.getConnection(), tableName);
			for (int i = 0; i < fieldsMetadata.getColumnCount(); i++) {
				dbFieldsMap.put(fieldsMetadata.getColumnName(i + 1).toUpperCase(),
						new Integer(fieldsMetadata.getColumnType(i + 1)));
			}
		}
		for (int i = 0; i < dbFields.length; i++) {
			dataType = (Integer) dbFieldsMap.get(dbFields[i].toUpperCase());
			if (dataType == null) {
				throw new SQLException("Field \"" + dbFields[i] + "\" does not exists in table \"" + tableName + "\"");
			}
			fieldTypes.add(dataType);
		}
		return fieldTypes;
	}


	/**
	 *  Gets the fieldTypes attribute of the SQLUtil class
	 *
	 * @param  metadata          Description of the Parameter
	 * @return                   The fieldTypes value
	 * @exception  SQLException  Description of the Exception
	 */
	public static List getFieldTypes(ParameterMetaData metadata) throws SQLException {
		List fieldTypes = new LinkedList();
		for (int i = 1; i <= metadata.getParameterCount(); i++) {
			fieldTypes.add(new Integer(metadata.getParameterType(i)));
		}
		return fieldTypes;
	}


	/**
	 *  Gets the fieldTypes attribute of the SQLUtil class
	 *
	 * @param  metadata          Description of the Parameter
	 * @return                   The fieldTypes value
	 * @exception  SQLException  Description of the Exception
	 */
	public static List getFieldTypes(ResultSetMetaData metadata) throws SQLException {
		List fieldTypes = new LinkedList();
		for (int i = 1; i <= metadata.getColumnCount(); i++) {
			fieldTypes.add(new Integer(metadata.getColumnType(i)));
		}
		return fieldTypes;
	}


	/**
	 *  Gets the fieldTypes attribute of the SQLUtil class
	 *
	 * @param  metadata          Description of the Parameter
	 * @param  cloverFields      Description of the Parameter
	 * @return                   The fieldTypes value
	 * @exception  SQLException  Description of the Exception
	 */
	public static List getFieldTypes(DataRecordMetadata metadata, String[] cloverFields) throws SQLException {
		List fieldTypes = new LinkedList();
		DataFieldMetadata fieldMeta;
		for (int i = 0; i < cloverFields.length; i++) {
			if ((fieldMeta = metadata.getField(cloverFields[i])) != null) {
				fieldTypes.add(new Integer(jetelType2sql(fieldMeta.getType())));
			} else {
				throw new RuntimeException("Field name [" + cloverFields[i] + "] not found in " + metadata.getName());
			}
		}
		return fieldTypes;
	}


	/**
	 *  Gets the tableFieldsMetadata attribute of the SQLUtil class
	 *
	 * @param  con               Description of the Parameter
	 * @param  tableName         Description of the Parameter
	 * @return                   The tableFieldsMetadata value
	 * @exception  SQLException  Description of the Exception
	 */
	public static ResultSetMetaData getTableFieldsMetadata(Connection con, String tableName) throws SQLException {
		String queryStr = "select * from " + tableName + " where 1=0 ";

		ResultSet rs = con.createStatement().executeQuery(queryStr);
		return rs.getMetaData();
	}


	/**
	 *  Converts Jetel/Clover datatype into String
	 *
	 * @param  fieldType  Jetel datatype
	 * @return            Corresponding string name
	 */
	public static String jetelType2Str(char fieldType) {
		switch (fieldType) {
			case DataFieldMetadata.NUMERIC_FIELD:
				return "numeric";
			case DataFieldMetadata.INTEGER_FIELD:
				return "integer";
			case DataFieldMetadata.STRING_FIELD:
				return "string";
			case DataFieldMetadata.DATE_FIELD:
				return "date";
			default:
				throw new RuntimeException("Unsupported data type " + fieldType);
		}
	}


	/**
	 *  Converts Jetel data type into SQL data type
	 *
	 * @param  jetelType
	 * @return            corresponding Jetel data type
	 * @since             September 25, 2002
	 */
	public static int jetelType2sql(char jetelType) {
		switch (jetelType) {
			case DataFieldMetadata.INTEGER_FIELD:
				return Types.INTEGER;
			case DataFieldMetadata.NUMERIC_FIELD:
				return Types.NUMERIC;
			case DataFieldMetadata.STRING_FIELD:
				return Types.VARCHAR;
			case DataFieldMetadata.DATE_FIELD:
				return Types.DATE;
			default:
				return -1;
			// unknown or not possible to translate
		}
	}


	/**
	 *  Converts SQL data type into Jetel data type
	 *
	 * @param  sqlType  JDBC SQL data type
	 * @return          corresponding Jetel data type
	 * @since           September 25, 2002
	 */
	public static char sqlType2jetel(int sqlType) {
		switch (sqlType) {
			case Types.INTEGER:
			case Types.SMALLINT:
			case Types.TINYINT:
				return DataFieldMetadata.INTEGER_FIELD;
			case Types.BIGINT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT:
			case Types.NUMERIC:
			case Types.REAL:
				return DataFieldMetadata.NUMERIC_FIELD;
			//------------------
			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
			case Types.OTHER:
				return DataFieldMetadata.STRING_FIELD;
			//------------------
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				return DataFieldMetadata.DATE_FIELD;
			//-----------------
			// proximity assignment
			case Types.BOOLEAN:
			case Types.BIT:
				return DataFieldMetadata.STRING_FIELD;
			default:
				logger.warn("Unknown SQL type is: " + sqlType);
				return (char) -1;
			// unknown or not possible to translate
		}
	}

}

