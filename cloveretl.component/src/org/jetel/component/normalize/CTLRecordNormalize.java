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
package org.jetel.component.normalize;

import java.util.Properties;

import org.jetel.ctl.CTLAbstractTransform;
import org.jetel.ctl.CTLEntryPoint;
import org.jetel.data.DataRecord;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.TransformException;
import org.jetel.metadata.DataRecordMetadata;

/**
 * Base class of all Java transforms generated by CTL-to-Java compiler from CTL transforms in the Normalizer component.
 *
 * @author Michal Tomcanyi, Javlin a.s. &lt;michal.tomcanyi@javlin.cz&gt;
 * @author Martin Janik, Javlin a.s. &lt;martin.janik@javlin.eu&gt;
 *
 * @version 5th May 2010
 * @created 2nd April 2009
 *
 * @see RecordNormalize
 */
public abstract class CTLRecordNormalize extends CTLAbstractTransform implements RecordNormalize {

	public final boolean init(Properties parameters, DataRecordMetadata sourceMetadata, DataRecordMetadata targetMetadata)
			throws ComponentNotReadyException {
		// a single input/output data record is required
		this.inputRecords = new DataRecord[1];
		this.outputRecords = new DataRecord[1];

		globalScopeInit();

		return initDelegate();
	}

	/**
	 * Called by {@link #init(Properties, DataRecordMetadata, DataRecordMetadata)} to perform user-specific
	 * initialization defined in the CTL transform. The default implementation does nothing, may be overridden
	 * by the generated transform class.
	 *
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 *
	 * @throws ComponentNotReadyException if the initialization fails
	 */
	@CTLEntryPoint(name = "init", required = false)
	protected boolean initDelegate() throws ComponentNotReadyException {
		// does nothing and succeeds by default, may be overridden by generated transform classes
		return true;
	}

	public final int count(DataRecord source) throws TransformException {
		inputRecords[0] = source;

		try {
			return countDelegate();
		} catch (ComponentNotReadyException exception) {
			// the exception may be thrown by lookups, sequences, etc.
			throw new TransformException("Generated transform class threw an exception!", exception);
		}
	}

	/**
	 * Called by {@link #count(DataRecord)} to determine the number of output data records in a user-specific way
	 * defined in the CTL transform. Has to be overridden by the generated transform class.
	 *
	 * @throws ComponentNotReadyException if some internal initialization failed
	 * @throws TransformException if an error occurred
	 */
	@CTLEntryPoint(name = "count", required = true)
	protected abstract int countDelegate() throws ComponentNotReadyException, TransformException;

	public final int transform(DataRecord source, DataRecord target, int idx) throws TransformException {
		inputRecords[0] = source;
		outputRecords[0] = target;

		try {
			return transformDelegate(idx);
		} catch (ComponentNotReadyException exception) {
			// the exception may be thrown by lookups, sequences, etc.
			throw new TransformException("Generated transform class threw an exception!", exception);
		}
	}

	/**
	 * Called by {@link #transform(DataRecord, DataRecord, int)} to transform data records in a user-specific way
	 * defined in the CTL transform. Has to be overridden by the generated transform class.
	 *
	 * @throws ComponentNotReadyException if some internal initialization failed
	 * @throws TransformException if an error occurred
	 */
	@CTLEntryPoint(name = "transform", parameterNames = { "idx" }, required = true)
	protected abstract int transformDelegate(int idx) throws ComponentNotReadyException, TransformException;

	@CTLEntryPoint(name = "clean", required = false)
	public void clean() {
		// does nothing by default, may be overridden by generated transform classes
	}

	@CTLEntryPoint(name = "getMessage", required = false)
	public String getMessage() {
		// null by default, may be overridden by generated transform classes
		return null;
	}

	@CTLEntryPoint(name = "finished", required = false)
	public void finished() {
		// does nothing by default, may be overridden by generated transform classes
	}

	@CTLEntryPoint(name = "reset", required = false)
	public void reset() {
		// does nothing by default, may be overridden by generated transform classes
	}

}
