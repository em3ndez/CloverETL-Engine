/* Generated By:JJTree: Do not edit this line. CLVFNVLNode.java */

package org.jetel.ctl.ASTnode;

import org.jetel.ctl.ExpParser;
import org.jetel.ctl.TransformLangParserVisitor;

public class CLVFNVL2Node extends SimpleNode {
	public CLVFNVL2Node(int id) {
		super(id);
	}

	public CLVFNVL2Node(ExpParser p, int id) {
		super(p, id);
	}

	public CLVFNVL2Node(CLVFNVL2Node node) {
		super(node);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(TransformLangParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	
	@Override
	public SimpleNode duplicate() {
		return new CLVFNVL2Node(this);
	}
}