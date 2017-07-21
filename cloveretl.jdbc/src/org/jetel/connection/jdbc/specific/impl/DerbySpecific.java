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
package org.jetel.connection.jdbc.specific.impl;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.jetel.metadata.DataFieldMetadata;

/**
 * Derby specific behaviour.
 * 
 * @author Pavel Simecek (pavel.simecek@javlin.eu)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 *
 * @created Apr 7, 2010
 */
public class DerbySpecific extends AbstractJdbcSpecific {

	private static final DerbySpecific INSTANCE = new DerbySpecific();
	
	protected DerbySpecific() {
		super(AutoGeneratedKeysType.SINGLE); //multi-row insert may cause errors, only single-row insert is supported by now
	}

	public static DerbySpecific getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns true, since Derby supports single-row auto-generated keys retrieval. 
	 */
	@Override
	public boolean supportsGetGeneratedKeys(DatabaseMetaData metadata) throws SQLException {
		return true;
	}
	
	@Override
	public String sqlType2str(int sqlType) {
		switch(sqlType) {
		case Types.BOOLEAN :
			return "SMALLINT";
		case Types.NUMERIC :
			return "DOUBLE";
		}
		return super.sqlType2str(sqlType);
	}
	@Override
	public String jetelType2sqlDDL(DataFieldMetadata field) {
		int sqlType = jetelType2sql(field);
		String ddlKeyword;
		switch(sqlType) {
		case Types.BINARY:
			ddlKeyword = "CHAR";
			break;
		case Types.VARBINARY:
			ddlKeyword = "VARCHAR";
			break;
		default: 
			return super.jetelType2sqlDDL(field);
		}
		return  ddlKeyword + "(" + (field.isFixed() ? String.valueOf(field.getSize()) : "80") + ")" + " FOR BIT DATA";
	}
	
	@Override
	public int jetelType2sql(DataFieldMetadata field) {
		switch (field.getType()) {
		case DataFieldMetadata.BOOLEAN_FIELD:
			return Types.SMALLINT;
        case DataFieldMetadata.NUMERIC_FIELD:
        	return Types.DOUBLE;
		default: 
        	return super.jetelType2sql(field);
		}
	}
	
	@Override
	public boolean isSchemaRequired() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.jetel.connection.jdbc.specific.impl.AbstractJdbcSpecific#getTables(java.sql.Connection, java.lang.String)
	 */
	@Override
	public ResultSet getTables(java.sql.Connection connection, String dbName) throws SQLException {
		return connection.getMetaData().getTables(null, dbName, "%", new String[] {"TABLE", "VIEW" });
	}
	


	
	
}
