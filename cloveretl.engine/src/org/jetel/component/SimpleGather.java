/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2002  David Pavlis
*
*    This program is free software; you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation; either version 2 of the License, or
*    (at your option) any later version.
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.jetel.component;
import java.util.*;
import java.io.*;
import org.w3c.dom.NamedNodeMap;
import org.jetel.graph.*;
import org.jetel.data.DataRecord;
import org.jetel.exception.ComponentNotReadyException;

/**
 *  <h3>Simple Gather Component</h3>
 *
 * <!-- All records from all input ports are gathered and copied onto output port [0] -->
 * 
 * <table border="1">
 *  <th>Component:</th>
 * <tr><td><h4><i>Name:</i></h4></td>
 * <td>Simple Gather</td></tr>
 * <tr><td><h4><i>Category:</i></h4></td>
 * <td></td></tr>
 * <tr><td><h4><i>Description:</i></h4></td>
 * <td>All records from all input ports are gathered and copied onto output port [0].<br>
 *  It goes port by port (waiting/blocked) if there is currently no data on port.<br>
 *  Implements inverse RoundRobin.<br>
 * </td></tr>
 * <tr><td><h4><i>Inputs:</i></h4></td>
 * <td>At least one connected output port.</td></tr>
 * <tr><td><h4><i>Outputs:</i></h4></td>
 * <td>[0]- output records (gathered)</td></tr>
 * <tr><td><h4><i>Comment:</i></h4></td>
 * <td></td></tr>
 * </table>
 *  <br>  
 *  <table border="1">
 *  <th>XML attributes:</th>
 *  <tr><td><b>type</b></td><td>"SIMPLE_GATHER"</td></tr>
 *  <tr><td><b>id</b></td>
 *  <td>component identification</td>
 *  </tr>
 *  </table>  
 *
 * @author     dpavlis
 * @since    April 4, 2002
 */
public class SimpleGather extends Node {

	public static final String COMPONENT_TYPE="SIMPLE_GATHER";
	
	private static final int WRITE_TO_PORT=0;
	
	public SimpleGather(String id){
		super(id);
		
	}

	/**
	 *  Gets the Type attribute of the SimpleCopy object
	 *
	 * @return    The Type value
	 * @since     April 4, 2002
	 */
	public String getType() {
		return COMPONENT_TYPE;
	}


	/**
	 *  Main processing method for the SimpleCopy object
	 *
	 * @since    April 4, 2002
	 */
	public void run() {
		OutputPort outPort=getOutputPort(WRITE_TO_PORT);
		InputPort inPort;
		/* we need to keep track of all input ports - it they contain data or
		signalized that they are empty.*/
		int numActive,readFromPort;
		boolean[] isEOF=new boolean[getInPorts().size()];
		for(int i=0;i<isEOF.length;i++){
			isEOF[i]=false;
		}
		Collection inputPorts=getInPorts();
		Iterator iterator;
		numActive=inputPorts.size();	// counter of still active ports - those without EOF status
		// the metadata is taken from output port definition
		DataRecord record=new DataRecord(outPort.getMetadata());
		DataRecord inRecord;
		record.init();
			
		while(runIt && numActive>0){
			iterator=inputPorts.iterator();
			readFromPort=0;
			while(runIt && iterator.hasNext()){
				inPort=(InputPort)iterator.next();
				if (!isEOF[readFromPort]){
					try{
						inRecord=inPort.readRecord(record);
						if (inRecord!=null){
							outPort.writeRecord(inRecord);
						}else{
							isEOF[readFromPort]=true;
							numActive--;
						}
					}catch(IOException ex){
						resultMsg=ex.getMessage();
						resultCode=Node.RESULT_ERROR;
						closeAllOutputPorts();
						return;
					}catch(Exception ex){
						resultMsg=ex.getMessage();
						resultCode=Node.RESULT_FATAL_ERROR;
						return;
					}
				}
				readFromPort++;
			}
		}
		setEOF(WRITE_TO_PORT);
		if (runIt) resultMsg="OK"; else resultMsg="STOPPED";
		resultCode=Node.RESULT_OK;
	}	


	/**
	 *  Description of the Method
	 *
	 * @since    April 4, 2002
	 */
	public void init() throws ComponentNotReadyException {
		// test that we have at least one input port and one output
		if (inPorts.size()<1){
			throw new ComponentNotReadyException(getID()+" at least one input port has to be defined!");
		}else if (outPorts.size()<1){
			throw new ComponentNotReadyException(getID()+" at least one output port has to be defined!");
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @return    Description of the Returned Value
	 * @since     May 21, 2002
	 */
	public org.w3c.dom.Node toXML() {
		// TODO
		return null;
	}


	/**
	 *  Description of the Method
	 *
	 * @param  nodeXML  Description of Parameter
	 * @return          Description of the Returned Value
	 * @since           May 21, 2002
	 */
	public static Node fromXML(org.w3c.dom.Node nodeXML) {
		NamedNodeMap attribs=nodeXML.getAttributes();
		
		if (attribs!=null){
			String id=attribs.getNamedItem("id").getNodeValue();
			if (id!=null){
				return new SimpleGather(id);
			}
		}
		return null;
	}
}

