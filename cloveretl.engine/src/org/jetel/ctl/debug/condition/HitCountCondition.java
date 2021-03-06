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
package org.jetel.ctl.debug.condition;

import org.jetel.ctl.DebugTransformLangExecutor;
import org.jetel.ctl.TransformLangExecutorRuntimeException;
import org.jetel.ctl.ASTnode.SimpleNode;

/**
 * @author jan.michalica (info@cloveretl.com)
 *         (c) Javlin, a.s. (www.cloveretl.com)
 *
 * @created 4.7.2016
 */
public class HitCountCondition implements Condition {

	private final int maxHits;
	private int hitCount;

	public HitCountCondition(int hitCount) {
		super();
		this.maxHits = hitCount;
	}

	@Override
	public boolean isFulFilled() {
		if (hitCount >= maxHits) {
			hitCount = 0;
			return true;
		}
		return false;
	}

	@Override
	public void evaluate(DebugTransformLangExecutor executor, SimpleNode context) throws TransformLangExecutorRuntimeException {
		++hitCount;
	}
}
