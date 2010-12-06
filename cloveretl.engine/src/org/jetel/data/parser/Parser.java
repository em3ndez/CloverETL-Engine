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
package org.jetel.data.parser;

import java.io.IOException;

import org.jetel.data.DataRecord;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.IParserExceptionHandler;
import org.jetel.exception.JetelException;
import org.jetel.exception.PolicyType;

/**
 *  Interface to input data parsers
 *
 * @author     D.Pavlis
 * @since    March 27, 2002
 * @see        OtherClasses
 */
public interface Parser {

	/**
	 *  An operation that produces next record from Input data or null
	 *
	 * @return                  The Next value
	 * @exception  IOException  Description of Exception
	 * @since                   March 27, 2002
	 */
	public DataRecord getNext() throws JetelException;

	/**
	 * Skips specified number of records.
	 * @param nRec Number of records to be skipped. 
	 * @return Number of skipped records.
	 * @throws JetelException
	 */
	public int skip(int nRec) throws JetelException;

	// Operations
	/**
	 *  Initialization of data parser by given metadata.
	 *
	 * @param  _metadata  Description of Parameter
	 * @since             March 27, 2002
	 */
	public void init() throws ComponentNotReadyException;

    /**
     * Sets input data source. Some of parsers allow to call this method repeatedly.
     * 
     * @param inputDataSource
     * @throws IOException if releasing of previous source failed
     */
    public void setDataSource(Object inputDataSource) throws IOException, ComponentNotReadyException;

    /**
     * If releaseInputSource is false, the previous input data source is not released (input stream is not closed).
     * The input data source release is performing into the method 'setDataSource'. Default value is true.
     * 
     * @param releaseInputSource
     */
    public void setReleaseDataSource(boolean releaseInputSource);

	/**
     * @deprecated this method was substituted by another method {@link #free()}
     * 
	 *  Closing/deinitialization of parser
	 *
	 * @since    May 2, 2002
	 */
    @Deprecated
	public void close() throws IOException;


	/**
	 * @param record
	 * @return
	 */
	public DataRecord getNext(DataRecord record) throws JetelException;


	/**
	 * @param handler
	 */
	public void setExceptionHandler(IParserExceptionHandler handler);

    public IParserExceptionHandler getExceptionHandler();
    
    public PolicyType getPolicyType();

    /**
     * @deprecated this method was substituted by pair of another methods {@link #preExecute()}
     * and {@link #postExecute()}.
     * 
	 * Reset parser for next graph execution. 
     */
    @Deprecated
	public void reset() throws ComponentNotReadyException;
    
	/**
	 * Gets current position of source file.
	 * 
	 * @return position
	 */
	public Object getPosition();

	/**
	 * Sets position
	 * 
	 * @param position
	 */
	public void movePosition(Object position) throws IOException;
	
    /**
     * Life-cycle hook. Follows the semantics of Node.preExecute()
     * Called by the owner of the parser before each graph phase execution.
     *  
     * @throws ComponentNotReadyException Catch-all exception
     */
    public void preExecute() throws ComponentNotReadyException;
    
    /**
     * Life-cycle hook. Follows the semantics of Node.postExecute()
     * Called by the owner of the parser after each graph phase execution.
     * NOTE! postExecute should do exactly the same thing as deprecated method reset(). The only advantage
     * over reset() is that it matches with the name of Node's life-cycle method postExecute().
     *    
     * @throws ComponentNotReadyException Catch-all exception
     */
    public void postExecute() throws ComponentNotReadyException;

    /**
     * Life-cycle hook. Follows the semantics of Node.free()
     * Called by the owner of the parser to release resources when the parser is not needed anymore
     * NOTE! free() should do exactly the same thing as deprecated method close(). The only advantage
     * over close() is that it matches with the name of Node's life-cycle method free(). 
     *    
     * @throws ComponentNotReadyException Catch-all exception
     * @throws IOException
     */
    public void free() throws ComponentNotReadyException, IOException;

}
/*
 *  end class DataParser
 */

