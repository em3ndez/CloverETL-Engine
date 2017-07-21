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
package org.jetel.ctl;

import org.jetel.ctl.ASTnode.*;


/**
 * Default implementation of visitor pattern for AST tree generated by
 * TransformLangParser. Automatically descends down the tree visiting every node
 * on the way.
 * 
 * @author Michal Tomcanyi <michal.tomcanyi@javlin.cz>
 *
 */
public class NavigatingVisitor implements TransformLangParserVisitor {

	protected Object visitNode(SimpleNode node, Object data) {
		if (node != null && node.jjtHasChildren()) {
			for (int i = 0; i < node.jjtGetNumChildren(); i++) {
				node.jjtGetChild(i).jjtAccept(this, data);
			}
		}

		return node;
	}

	@Override
	public Object visit(CLVFStart node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFStartExpression node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFImportSource node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFFunctionDeclaration node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFVariableDeclaration node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFAssignment node, Object data) {
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFOr node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFAnd node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFComparison node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFAddNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFSubNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFMulNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFDivNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFModNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFPostfixExpression node, Object data) {

		return visitNode(node, data);
	}

	public Object visit(CLVFUnaryExpression node, Object data) {

		return visitNode(node, data);
	}
	
	@Override
	public Object visit(CLVFUnaryStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFUnaryNonStatement node, Object data) {
		
		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFLiteral node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFListOfLiterals node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFBlock node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFIfStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFSwitchStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFCaseStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFWhileStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFForStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFForeachStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFDoStatement node, Object data) {

		return visitNode(node, data);
	}


	@Override
	public Object visit(CLVFBreakStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFContinueStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFReturnStatement node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFIsNullNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFNVLNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFNVL2Node node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFIIfNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFPrintStackNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFRaiseErrorNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFPrintErrNode node, Object data) {

		return visitNode(node, data);
	}

	public Object visit(CLVFEvalNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFPrintLogNode node, Object data) {

		return visitNode(node, data);
	}
	
	@Override
	public Object visit(CLVFSequenceNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFLookupNode node, Object data) {

		return visitNode(node, data);
	}

	@Override
	public Object visit(CLVFDictionaryNode node, Object data) {
		
		return visitNode(node, data);
	}
	
	@Override
	public Object visit(SimpleNode node, Object data) {
		throw new UnsupportedOperationException("Unreachable code");
		
	}
	
	@Override
	public Object visit(CLVFConditionalExpression node, Object data) {
		return visitNode(node,data);
	}
	
	@Override
	public Object visit(CLVFConditionalFailExpression node, Object data) {
		return visitNode(node, data);
	}


	@Override
	public Object visit(CLVFFieldAccessExpression node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFMemberAccessExpression node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFArrayAccessExpression node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFArguments node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFIdentifier node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFType node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFDateField node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFParameters node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFFunctionCall node, Object data) {
		return visitNode(node,data);
	}
	
	@Override
	public Object visit(CLVFLogLevel node, Object data) {
		return visitNode(node,data);
	}

	@Override
	public Object visit(CLVFInFunction node, Object data) {
		return visitNode(node,data);
	}
	
	/* ************************ Synthetic nodes ********************/
	@Override
	public Object visit(CastNode node, Object data) {
		return visitNode(node,data);
	}
	
	@Override
	public boolean inDebugMode() {
		return false;
	}
	
	@Override
	public void debug(SimpleNode node, Object data) {
	}
}
