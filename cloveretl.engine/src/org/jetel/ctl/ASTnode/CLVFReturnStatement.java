/* Generated By:JJTree: Do not edit this line. CLVFReturnStatement.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFReturnStatement extends SimpleNode {
	public CLVFReturnStatement(int id) {
		super(id);
	}

	public CLVFReturnStatement(ExpParser p, int id) {
		super(p, id);
	}

	public CLVFReturnStatement(CLVFReturnStatement returnStatement) {
		super(returnStatement);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	@Override
	public SimpleNode duplicate() {
		return new CLVFReturnStatement(this);
	}
}
