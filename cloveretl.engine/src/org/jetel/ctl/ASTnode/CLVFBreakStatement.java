/* Generated By:JJTree: Do not edit this line. CLVFBreakStatement.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFBreakStatement extends SimpleNode {

	public CLVFBreakStatement(int id) {
		super(id);
	}

	public CLVFBreakStatement(ExpParser p, int id) {
		super(p, id);
	}

	public CLVFBreakStatement(CLVFBreakStatement node) {
		super(node);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFBreakStatement(this);
	}
}