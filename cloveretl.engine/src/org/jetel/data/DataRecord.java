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

// FILE: c:/projects/jetel/org/jetel/data/DataRecord.java

package org.jetel.data;
import java.io.*;
import java.nio.*;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.metadata.DataFieldMetadata;

/**
 *  A class that represents one data record
 *  Fields are not deleted & created all the time. Instead, they are created only once and
 *  updated when it is needed
 *  When we need to send record through the EDGE, we just serialize it (it isn't standard version of serializing)
 *
 * @author     D.Pavlis
 * @since    March 26, 2002
 * @see      org.jetel.metadata.DataRecordMetadata
 */
public class DataRecord implements Serializable {

	// Associations
	/**
	 * @since
	 */
	private transient DataRecordMetadata metadata;
	/**
	 * @since
	 */
	private DataField fields[];


	// Attributes
	/**
	 *  An attribute that represents ...
	 *
	 * @param  _metadata  Description of Parameter
	 * @since
	 */

	// Operations

	public DataRecord(DataRecordMetadata _metadata) {
		this.metadata = _metadata;
		fields = new DataField[metadata.getNumFields()];
	}


	/**
	 *  Sets the Metadata attribute of the DataRecord object
	 *
	 * @param  metadata  The new Metadata value
	 * @since            April 5, 2002
	 */
	public void setMetadata(DataRecordMetadata metadata) {
		this.metadata = metadata;
	}


	/**
	 *  An attribute that represents ...
	 *
	 *  An operation that does ...
	 *
	 * @return    The Metadata value
	 * @since
	 */
	public DataRecordMetadata getMetadata() {
		return metadata;
	}


	/**
	 *  An operation that does ...
	 *
	 * @return    The NumFields value
	 * @since
	 */
	public int getNumFields() {
		return metadata.getNumFields();
	}


	/**
	 *  An operation that does ...
	 *
	 * @param  _name  Description of Parameter
	 * @return        The Field value
	 * @since
	 */
	public DataField getField(String _name) {
		try{
			return fields[metadata.getFieldPosition(_name)];
		}catch (IndexOutOfBoundsException e) {
			return null;
		}
	}


	/**
	 *  An operation that does ...
	 *
	 * @param  _fieldNum  Description of Parameter
	 * @return            The Field value
	 * @since
	 */
	public DataField getField(int _fieldNum) {
		try {
			return fields[_fieldNum];
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @since    April 5, 2002
	 */
	public void init() {
		DataFieldMetadata fieldMetadata;
		// create appropriate data fields based on metadata supplied
		for (int i = 0; i < metadata.getNumFields(); i++) {
			fieldMetadata = metadata.getField(i);
			fields[i]=DataFieldFactory.createDataField(fieldMetadata.getType(),fieldMetadata);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  _fieldNum  Description of Parameter
	 * @since
	 */
	public void delField(int _fieldNum) {
		try {
			fields[_fieldNum] = null;
		}
		catch (IndexOutOfBoundsException e) {
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  buffer  Description of Parameter
	 * @since          April 23, 2002
	 */
	public void serialize(ByteBuffer buffer) {
		for (int i = 0; i < fields.length; i++) {
			fields[i].serialize(buffer);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  buffer  Description of Parameter
	 * @since          April 23, 2002
	 */
	public void deserialize(ByteBuffer buffer) {
		for (int i = 0; i < fields.length; i++) {
			fields[i].deserialize(buffer);
		}
	}


	/**
	 *  Description of the Method
	 *
	 * @param  obj  Description of Parameter
	 * @return      Description of the Returned Value
	 * @since       April 23, 2002
	 */
	public boolean equals(Object obj) {
		/* first test that both records have the same structure i.e. point to the same
		 metadata */
		if (metadata != ((DataRecord) obj).getMetadata()) {
			return false;
		}
		// check field by field that they are the same
		for (int i = 0; i < fields.length; i++) {
			if (!fields[i].equals(((DataRecord) obj).getField(i))) {
				return false;
			}
		}
		return true;
	}
}
/*
 *  end class DataRecord
 */

