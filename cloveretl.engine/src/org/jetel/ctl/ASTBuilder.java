package org.jetel.ctl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetel.ctl.ASTnode.CLVFAssignment;
import org.jetel.ctl.ASTnode.CLVFBlock;
import org.jetel.ctl.ASTnode.CLVFCaseStatement;
import org.jetel.ctl.ASTnode.CLVFFieldAccessExpression;
import org.jetel.ctl.ASTnode.CLVFForeachStatement;
import org.jetel.ctl.ASTnode.CLVFFunctionDeclaration;
import org.jetel.ctl.ASTnode.CLVFLiteral;
import org.jetel.ctl.ASTnode.CLVFLookupNode;
import org.jetel.ctl.ASTnode.CLVFParameters;
import org.jetel.ctl.ASTnode.CLVFSequenceNode;
import org.jetel.ctl.ASTnode.CLVFStart;
import org.jetel.ctl.ASTnode.CLVFStartExpression;
import org.jetel.ctl.ASTnode.CLVFSwitchStatement;
import org.jetel.ctl.ASTnode.CLVFType;
import org.jetel.ctl.ASTnode.CLVFUnaryExpression;
import org.jetel.ctl.ASTnode.CLVFVariableDeclaration;
import org.jetel.ctl.ASTnode.SimpleNode;
import org.jetel.ctl.data.TLType;
import org.jetel.ctl.data.TLTypePrimitive;
import org.jetel.ctl.data.UnknownTypeException;
import org.jetel.data.lookup.LookupTable;
import org.jetel.data.sequence.Sequence;
import org.jetel.exception.ComponentNotReadyException;
import org.jetel.exception.NotInitializedException;
import org.jetel.graph.TransformationGraph;
import org.jetel.metadata.DataFieldMetadata;
import org.jetel.metadata.DataRecordMetadata;

/**
 * Implementation of semantic checking for CTL compiler.
 * Resolves external references, functions and records.
 * Performs code structure validation, mostly for correct 
 * derivation of expression statements.
 * 
 * @author Michal Tomcanyi <michal.tomcanyi@javlin.cz>
 *
 */
public class ASTBuilder extends NavigatingVisitor {

	/** Metadata for component's input ports */
	private final DataRecordMetadata[] inputMetadata;
	/** Metadata for component's output ports */
	private final DataRecordMetadata[] outputMetadata;
	/** Name -> position mapping for input ports */
	private final TreeMap<String, Integer> inputRecordsMap;
	/** Name -> position mapping for output ports */
	private final TreeMap<String, Integer> outputRecordsMap;
	/** Name/ID -> metadata mapping for record-type variables */
	private final TreeMap<String, DataRecordMetadata> graphMetadata;
	/** Name/ID -> lookup mapping for lookup nodes */
	private final TreeMap<String, LookupTable> lookupMap;
	/** Name/ID -> lookup mapping for sequences */
	private final TreeMap<String, Sequence> sequenceMap;
	/** Current transformation graph */
	private final TransformationGraph graph;
	/** Function declarations */
	private final Map<String, List<CLVFFunctionDeclaration>> declaredFunctions;
	/** Problem collector */
	private ProblemReporter problemReporter;

	public ASTBuilder(TransformationGraph graph, DataRecordMetadata[] inputMetadata,
			DataRecordMetadata[] outputMetadata, Map<String, List<CLVFFunctionDeclaration>> declaredFunctions, ProblemReporter problemReporter) {
		super();
		this.graph = graph;
		this.inputMetadata = inputMetadata;
		this.outputMetadata = outputMetadata;
		this.inputRecordsMap = new TreeMap<String, Integer>();
		this.outputRecordsMap = new TreeMap<String, Integer>();
		this.graphMetadata = new TreeMap<String, DataRecordMetadata>();
		this.lookupMap = new TreeMap<String, LookupTable>();
		this.sequenceMap = new TreeMap<String, Sequence>();
		this.declaredFunctions = declaredFunctions;
		this.problemReporter = problemReporter;

		if (graph != null) {
			// populate name -> position mappings
			int i = 0;
			Integer prevPos = 0;
			// input metadata names can clash - warn user that we will not be able to resolve them correctly
			if (inputMetadata != null) {
				for (DataRecordMetadata m : inputMetadata) {
					if ((prevPos = inputRecordsMap.put(m.getName(), i++)) != null) {
						warn("Input record name '" + m.getName() + "' is ambiguous", "Use positional access or rename input metadata on port '" + prevPos + "' or '" + (i - 1) + "'");
					}
				}
			}
	
			// the same for output just different error message
			i = 0;
			if (outputMetadata != null) {
				for (DataRecordMetadata m : outputMetadata) {
					if (m != null) {
						if ((prevPos = outputRecordsMap.put(m.getName(), i++)) != null) {
							warn("Output record name '" + m.getName() + "' is ambiguous", "Use positional access or rename output metadata on port '" + prevPos + "' or '" + (i - 1) + "'");
						}
					}
				}
			}
	
			// all graph metadata for resolving record-type variable declarations
			Iterator<String> mi = graph.getDataRecordMetadata();
			while (mi.hasNext()) {
				String id = mi.next();
				DataRecordMetadata m = graph.getDataRecordMetadata(id);
				if (graphMetadata.put(m.getName(), m) != null) {
					warn("Metadata name '" + m.getName() + "' is ambiguous", "Rename the metadata to a unique name");
				}
			}
	
			// lookup tables
			Iterator<String> li = graph.getLookupTables();
			while (li.hasNext()) {
				LookupTable t = graph.getLookupTable(li.next());
				if (lookupMap.put(t.getName(), t) != null) {
					warn("Lookup table name '" + t.getName() + "' is ambiguous", "Rename the lookup table to a unique name");
				}
			}
	
			// sequences
			Iterator<String> si = graph.getSequences();
			while (si.hasNext()) {
				Sequence s = graph.getSequence(si.next());
				if (sequenceMap.put(s.getName(), s) != null) {
					warn("Sequence name '" + s.getName() + "' is ambiguous", "Rename the sequence to a unique name");
				}
			}

		}
	}

	/**
	 * AST builder entry method for complex transformations
	 * 
	 * @param tree
	 *            AST to resolve
	 */
	public void resolveAST(CLVFStart tree) {
		if (!checkGraph()) {
			return;
		}
		
		visit(tree, null);
		checkLocalFunctionsDuplicities();
	}
	
	/**
	 * AST builder entry method for simple expression (Filter)
	 * @param tree
	 */
	public void resolveAST(CLVFStartExpression tree) {
		if (!checkGraph()) {
			return;
		}
		visit(tree,null);
	}

	/**
	 * Field references are context-sensitive. Field reference "$out.someField" on LHS resolves to an OUTPUT field
	 * 'someField' in record name 'out'. The flag is sent in 'data' object.
	 */
	@Override
	public CLVFAssignment visit(CLVFAssignment node, Object data) {
		// if LHS, must send flag down the tree to inform FieldAccessExpression it is an output field
		node.jjtGetChild(0).jjtAccept(this, true);
		node.jjtGetChild(1).jjtAccept(this, false);
		return node;
	}
	
	@Override
	public CLVFBlock visit(CLVFBlock node, Object data) {
		super.visit(node,data);
		
		checkBlockParseTree(node);

		return node;
	}
	
	@Override
	public Object visit(CLVFStart node, Object data) {
		super.visit(node,data);
		
		checkBlockParseTree(node);

		return node;
	}
		
	/**
	 * Calculates positional references according to field names. 
	 * Validates that metadata and field references are valid
	 * in the current graph.
	 */
	@Override
	public CLVFFieldAccessExpression visit(CLVFFieldAccessExpression node, Object data) {
		Object id = node.getRecordId();
		// if the FieldAccessExpression appears somewhere except assignment 
		// the 'data' will be null so we treat it as a reference to the input field
		Boolean isLHS = data != null ? (Boolean)data : false;
		node.setMetadata(null);
		node.setOutput(isLHS);

		
		Integer recordPos = null;

		// resolve positional reference for record if necessary
		if (node.getRecordId() != null) {
			recordPos = node.getRecordId();
		} else {
			// calculate positional reference
			recordPos = isLHS ? getOutputPosition(node.getRecordName()) : getInputPosition(node.getRecordName());
			if (recordPos != null) {
				node.setRecordId(recordPos);
			} else {
				error(node, "Unable to resolve " + (isLHS ? "output" : "input") + " metadata '" + node.getRecordName() + "'");
				node.setType(TLType.ERROR);
				return node; // nothing else to do
			}

		}

		// check if we have metadata for this record
		DataRecordMetadata metadata = isLHS ? getOutputMetadata(recordPos) : getInputMetadata(recordPos);
		if (metadata != null) {
			node.setMetadata(metadata);
		} else {
			error(node, "No metadata found for " + (isLHS ? "output" : "input") + " port '" + id + "'", "Connect the port and assign metadata on the edge");
			node.setType(TLType.ERROR);
			return node;
		}

		if (node.isWildcard()) {
			// this is not a record reference - but access all record fields
			node.setType(TLType.forRecord(node.getMetadata(),false));
			return node; // this node access record -> we do not want resolve field access
		}

		// resolve and validate field identifier using record metadata
		Integer fieldId;
		if (node.getFieldId() != null) {
			fieldId = node.getFieldId();
			// fields are ordered from zero
			if (fieldId > metadata.getNumFields()-1) {
				error(node,"Field '" + fieldId + "' is out of range for record '" + metadata.getName() + "'");
				node.setType(TLType.ERROR);
				return node;
			}
		} else {
			fieldId = metadata.getFieldPosition(node.getFieldName());
			if (fieldId >= 0) {
				node.setFieldId(fieldId);
			} else {
				error(node, "Field '" + node.getFieldName() + "' does not exist in record '" + metadata.getName() + "'");
				node.setType(TLType.ERROR);
				return node;
			}
		}

		try {
			node.setType(TLTypePrimitive.fromCloverType(node.getMetadata().getField(fieldId)));
		} catch (UnknownTypeException e) {
			error(node, "Field type '" + e.getType() + "' does not match any CTL type");
			node.setType(TLType.ERROR);
			throw new IllegalArgumentException(e);
		}
		
		return node;
	}

	@Override
	public Object visit(CLVFForeachStatement node, Object data) {
		super.visit(node, data);

		CLVFVariableDeclaration loopVar = (CLVFVariableDeclaration)node.jjtGetChild(0);
		if (loopVar.jjtGetNumChildren() > 1) {
			error(loopVar,"Foreach loop variable must not have a initializer","Delete the initializer expression");
			node.setType(TLType.ERROR);
		}
		
		return node;
	}
	
	/**
	 * Populates function return type and formal parameters type.
	 * Sets node's type to {@link TLType#ERROR} in case of any issues.
	 */
	@Override
	public Object visit(CLVFFunctionDeclaration node, Object data) {
		// scan (return type), parameters and function body
		super.visit(node,data);
		
//		checkStatementOrBlock(node.jjtGetChild(2));
		
		CLVFType retType = (CLVFType)node.jjtGetChild(0);
		node.setType(retType.getType());
		
		CLVFParameters params = (CLVFParameters)node.jjtGetChild(1);
		TLType[] formalParm = new TLType[params.jjtGetNumChildren()];
		for (int i=0; i<params.jjtGetNumChildren(); i++) {
			CLVFVariableDeclaration p = (CLVFVariableDeclaration)params.jjtGetChild(i);
			if ((formalParm[i] = p.getType()) == TLType.ERROR) {
				// if function accepts some metadata-typed params, we can have error resolving them
				node.setType(TLType.ERROR);
			}
		}
		
		node.setFormalParameters(formalParm);
		
		
		return data;
	}
	
	/**
	 * Parses literal value to the corresponding object
	 * May result in additional errors in parsing.
	 * 
	 * @see #parseLiteral(CLVFLiteral)
	 */
	@Override
	public Object visit(CLVFLiteral node, Object data) {
		parseLiteral(node);
		return data;
	}

	/**
	 * Resolves of identifier to the corresponding graph lookup table
	 */
	@Override
	public CLVFLookupNode visit(CLVFLookupNode node, Object data) {
		super.visit(node, data);
		
		LookupTable table = resolveLookup(node.getLookupName());
		if (table == null) {
			error(node, "Unable to resolve lookup table '" + node.getLookupName() + "'");
			node.setType(TLType.ERROR);
			return node;
		} else {
			node.setLookupTable(table);
			// type will be set in composite reference node as it will build up the lookup node completely
		}
		
		// we need to call init() to get access to metadata, keys, etc. (e.g. for DBLookupTable)
		try {
			if (! node.getLookupTable().isInitialized()) {
				node.getLookupTable().init();
			}
		} catch (ComponentNotReadyException e) {
			// underlying lookup cannot initialize
			error(node,"Lookup table has configuration error: " + e.getMessage());
			node.setType(TLType.ERROR);
			return node;
		} 

		TLType getOrCountReturnType = null;
		DataRecordMetadata ret = null;
		switch (node.getOperation()) {
		// OP_COUNT and OP_GET use a common arguments validation and only differ in return type 
		case CLVFLookupNode.OP_COUNT:
				getOrCountReturnType = TLTypePrimitive.INTEGER;
				/* ------ no break here deliberately : key validation follows ----- */
		case CLVFLookupNode.OP_GET:
			try {
				DataRecordMetadata keyRecordMetadata = node.getLookupTable().getKeyMetadata();
				LinkedList<Integer> decimalInfo = new LinkedList<Integer>();
				if (keyRecordMetadata == null) {
					// fail safe step in case getKey() does not work properly -> exception is caught below
					throw new UnsupportedOperationException();
				}
				
				// extract lookup parameter types
				TLType[] formal = new TLType[keyRecordMetadata.getNumFields()];
				try {
					for (int i=0; i<keyRecordMetadata.getNumFields(); i++) {
						formal[i] = TLTypePrimitive.fromCloverType(keyRecordMetadata.getField(i));
						if (formal[i].isDecimal()) {
							final DataFieldMetadata f = keyRecordMetadata.getField(i);
							decimalInfo.add(f.getFieldProperties().getIntProperty(DataFieldMetadata.LENGTH_ATTR));
							decimalInfo.add(f.getFieldProperties().getIntProperty(DataFieldMetadata.SCALE_ATTR));
						}
					}
					node.setFormalParameters(formal);
					node.setDecimalPrecisions(decimalInfo);
				} catch (UnknownTypeException e) {
					error(node,"Lookup returned an unknown parameter type: '" + e.getType() + "'");
					node.setType(TLType.ERROR);
					return node;
				}
				
			} catch (UnsupportedOperationException e) {
				// can happen in case the JDBC driver does not provide info about SQL params
				warn(node,"Unable to validate lookup keys");
				
			} catch (NotInitializedException e) {
				// should never happen
				error(node,"Lookup not initialized");
				throw e;
			} catch (ComponentNotReadyException e) {
				// underlying lookup is misconfigured
				error(node,"Lookup table has configuration errors: " + e.getMessage());
				node.setType(TLType.ERROR);
				return node;
			}
			
			// if return type is already set to integer, we are validating a count() function - see case above
			// otherwise we are validating get() function and must compute return type from metadata
			if (getOrCountReturnType == null) {
				ret = node.getLookupTable().getMetadata();
				if (ret == null) {
					error(node,"Lookup table has no metadata specified");
					node.setType(TLType.ERROR);
					return node;
				}
				getOrCountReturnType = TLType.forRecord(ret);
			}
			node.setType(getOrCountReturnType);
			break;
			
		case CLVFLookupNode.OP_NEXT:
			// extract return type
			ret = node.getLookupTable().getMetadata();
			if (ret == null) {
				error(node,"Lookup table has no metadata specified");
				node.setType(TLType.ERROR);
				return node;
			}				
			node.setType(TLType.forRecord(ret));
			break;
			
		case CLVFLookupNode.OP_INIT:
			node.setType(TLType.VOID);
			break;
			
		case CLVFLookupNode.OP_FREE:
			node.setType(TLType.VOID);
			break;
		}

		return node;
		
	}

	
	/**
	 * Resolves of identifier to the corresponding graph sequence.
	 * Sequence return type is defined by user in syntax.
	 */
	@Override
	public CLVFSequenceNode visit(CLVFSequenceNode node, Object data) {
		Sequence seq = resolveSequence(node.getSequenceName());
		if (seq == null) {
			error(node, "Unable to resolve sequence '" + node.getSequenceName() + "'");
			node.setType(TLType.ERROR);
		} else {
			node.setSequence(seq);
		}

		return node;
	}
	
	/**
	 * Computes case statements indices
	 */
	public Object visit(CLVFSwitchStatement node, Object data) {
		super.visit(node,data);
		
		ArrayList<Integer> caseIndices = new ArrayList<Integer>();
		for (int i=0; i<node.jjtGetNumChildren(); i++) {
			SimpleNode child = (SimpleNode)node.jjtGetChild(i);
			if (child.getId() == TransformLangParserTreeConstants.JJTCASESTATEMENT) {
				if (((CLVFCaseStatement)child).isDefaultCase()) {
					node.setDefaultCaseIndex(i);
				} else {
					caseIndices.add(i);
				}
			}
		}
		
		node.setCaseIndices((Integer[]) caseIndices.toArray(new Integer[caseIndices.size()]));
		return node;
	}
	
	
	@Override
	public Object visit(CLVFType node, Object data) {
		node.setType(createType(node));
		
		return node;
	}
	
	/**
	 * Analyzes if the unary expression is not just a negative literal.
	 * If yes, the negative literal is validated, parsed and the unary expression
	 * is replaced by the literal.
	 */
	@Override
	public Object visit(CLVFUnaryExpression node, Object data) {
		if (node.getOperator() == TransformLangParserConstants.MINUS &&
			((SimpleNode)node.jjtGetChild(0)).getId() == TransformLangParserTreeConstants.JJTLITERAL) {
			CLVFLiteral lit = (CLVFLiteral)node.jjtGetChild(0);
			int idx = 0;
			final SimpleNode parent = (SimpleNode)node.jjtGetParent();
			// store position of the unary expression in idx 
			for (idx = 0; idx < parent.jjtGetNumChildren(); idx++) {
				if (parent.jjtGetChild(idx) == node) {
					break;
				}
			}

			switch (lit.getTokenKind()) {
			case TransformLangParserConstants.FLOATING_POINT_LITERAL:
			case TransformLangParserConstants.LONG_LITERAL:
			case TransformLangParserConstants.INTEGER_LITERAL:
			case TransformLangParserConstants.DECIMAL_LITERAL:
				lit.setValue(lit.getTokenKind(), "-" + lit.getValueImage());
				break;
			default:
				error(node,"Operator '-' is not defined for this type of literal");
				return node;
			}

			
			
			// initialize literal value and fall back early on error
			if (!parseLiteral(lit)) {
				return node;
			} 			
			
			// literal was correctly initialized - replace the unary minus in AST
			lit.jjtSetParent(node.jjtGetParent());
			parent.jjtAddChild(lit, idx);
			
			return lit;
		}
		
		// not a minus literal, but other unary expression, validate children
		super.visit(node, data);
		return node;
	}
	
	/**
	 * Sets type of variable from type node
	 */
	@Override
	public CLVFVariableDeclaration visit(CLVFVariableDeclaration node, Object data) {
		super.visit(node, data);
		CLVFType typeNode = (CLVFType) node.jjtGetChild(0);
		// set the type of variable
		node.setType(typeNode.getType());
		return node;
	}

	
	
	// ----------------------------- Utility methods -----------------------------

	/**
	 * This method check that local declarations of functions do not contain any
	 * duplicate function declaration. Types of function parameters within declarations
	 * must have been already resolved (i.e. call this after AST pass has been completed)
	 * 
	 * Function declaration is duplicate iff it has the same:
	 * 	- return type
	 * 	- function name
	 *  - parameters
	 *  as some other locally declared function
	 */
	private void checkLocalFunctionsDuplicities() {
		for (String name : declaredFunctions.keySet()) {
			final List<CLVFFunctionDeclaration> functions = declaredFunctions.get(name);
			final int overloadCount = functions.size();
			if (overloadCount < 2) {
				// no duplicates possible
				continue;
			}
			
			for (int i=1; i<overloadCount; i++) {
				for (int j=i-i; j>=0; j--) {
					CLVFFunctionDeclaration valid = functions.get(j);
					CLVFFunctionDeclaration candidate = functions.get(i);
					
					/*
					 * This follows Java approach: overloading function must have different 
					 * parameters, difference only in return type is insufficient and is
					 * treated as a duplicate
					 */
					if (Arrays.equals(valid.getFormalParameters(), candidate.getFormalParameters())) {
						// the same name, return type and parameter types: duplicate
						error(valid,"Duplicate function '" + valid.toHeaderString() + "'");
						error(candidate,"Duplicate function '" + valid.toHeaderString() + "'");
						
					}
				}
			}
		}
	}
	

	/**
	 * Initializes literal by parsing its string representation into real type.
	 * 
	 * @return false (and reports error) when parsing did not succeed, true otherwise
	 */
	private boolean parseLiteral(CLVFLiteral lit) {
		String errorMessage = null;
		String hint = null;
		try {
			lit.computeValue();
		} catch (NumberFormatException e) {
			switch (lit.getTokenKind()) {
			case TransformLangParserConstants.FLOATING_POINT_LITERAL:
				errorMessage = "Literal '" + lit.getValueImage() + "' is out of range for type 'number'";
				hint = "Use 'D' distincter to treat literal as 'decimal' ";
				break;
			case TransformLangParserConstants.LONG_LITERAL:
				errorMessage = "Literal '" + lit.getValueImage() + "' is out of range for type 'long'";
				hint = "Use 'D' distincter to treat literal as 'decimal'";
				break;
			case TransformLangParserConstants.INTEGER_LITERAL:
				errorMessage = "Literal '" + lit.getValueImage() + "' is out of range for type 'int'";
				hint = "Use 'L' distincter to treat literal as 'long'";
				break;
			default:
				// should never happen
				errorMessage = "Unrecognized literal type '" + lit.getTokenKind() + "' with value '" + lit.getValueImage() + "'";
				hint = "Report as bug";
				break;
			}
		} catch (ParseException e) {
			switch (lit.getTokenKind()) {
			case TransformLangParserConstants.DATE_LITERAL:
				errorMessage = "Literal '" + lit.getValueImage() + "has invalid format for type 'date'";
				hint = "Date literal must match format pattern 'YYYY-MM-dd'";
				break;
			case TransformLangParserConstants.DATETIME_LITERAL:
				errorMessage = "Literal '" + lit.getValueImage() + "has invalid format for type 'date'";
				hint = "Date-time literal must match format pattern 'YYYY-MM-DD HH:MM:SS'";
				break;
			default:
				// should never happen
				errorMessage = "Unrecognized literal type '" + lit.getTokenKind() + "' with value '" + lit.getValueImage() + "'";
				hint = "Report as bug";
				break;
			}
		}
		
		// report error and fall back early
		if (errorMessage != null) {
			error(lit,errorMessage,hint);
			return false;
		}
		
		return true;
	}
	
	private boolean checkGraph() {
		if (this.graph == null) {
			problemReporter.error(1, 1, 1, 1, "Performing only syntactic validation because graph configuration is invalid.", 
					"Correct errors in graph configuration to enable code compilation");
			return false;
		}
		
		return true;
	}
	
	private Integer getInputPosition(String name) {
		return inputRecordsMap.get(name);
	}

	private Integer getOutputPosition(String name) {
		return outputRecordsMap.get(name);
	}

	private DataRecordMetadata getInputMetadata(int recordId) {
		return getMetadata(inputMetadata, recordId);
	}

	private DataRecordMetadata getOutputMetadata(int recordId) {
		return getMetadata(outputMetadata, recordId);
	}

	private DataRecordMetadata getMetadata(DataRecordMetadata[] metadata, int recordId) {
		// no metadata specified on component, or metadata not assigned on edge corresponding to recordId
		if (metadata == null || recordId >= metadata.length || metadata[recordId] == null) {
			return null;
		}

		return metadata[recordId];
	}

	private Sequence resolveSequence(String name) {
		return sequenceMap.get(name);
	}

	private LookupTable resolveLookup(String name) {
		return lookupMap.get(name);
	}

	private DataRecordMetadata resolveMetadata(String recordName) {
		return graphMetadata.get(recordName);
	}

	private Integer parseIntegerValue(String valueImage) throws NumberFormatException {
		if (valueImage.startsWith("0x")) {
			// hexadecimal literal -> skip 0x
			return Integer.parseInt(valueImage.substring(2), 16);
		} else if (valueImage.startsWith("0")) {
			// octal literal
			return Integer.parseInt(valueImage, 8);
		} else {
			// decimal literal
			return Integer.parseInt(valueImage);
		}
	}

	private TLType createType(CLVFType typeNode) {
		switch (typeNode.getKind()) {
		case TransformLangParserConstants.INT_VAR:
			return TLTypePrimitive.INTEGER;
		case TransformLangParserConstants.LONG_VAR:
			return TLTypePrimitive.LONG;
		case TransformLangParserConstants.DOUBLE_VAR:
			return TLTypePrimitive.DOUBLE;
		case TransformLangParserConstants.DECIMAL_VAR:
			return TLTypePrimitive.DECIMAL;
		case TransformLangParserConstants.STRING_VAR:
			return TLTypePrimitive.STRING;
		case TransformLangParserConstants.DATE_VAR:
			return TLTypePrimitive.DATETIME;
		case TransformLangParserConstants.BYTE_VAR:
			return TLTypePrimitive.BYTEARRAY;
		case TransformLangParserConstants.BOOLEAN_VAR:
			return TLTypePrimitive.BOOLEAN;
		case TransformLangParserConstants.IDENTIFIER:
			DataRecordMetadata meta = resolveMetadata(typeNode.getMetadataName());
			if (meta == null) {
				error(typeNode, "Unable to resolve metadata '" + typeNode.getMetadataName() + "'");
				return TLType.ERROR;
			}
			// variables of record type hold REFERENCE
			return TLType.forRecord(meta,true);
		case TransformLangParserConstants.MAP_VAR:
			return TLType.createMap((TLTypePrimitive)createType((CLVFType)typeNode.jjtGetChild(0)),createType((CLVFType)typeNode.jjtGetChild(1)));
		case TransformLangParserConstants.LIST_VAR:
			return TLType.createList(createType((CLVFType)typeNode.jjtGetChild(0)));
		case TransformLangParserConstants.VOID_VAR:
			return TLType.VOID;
		default:
			error(typeNode, "Unknown variable type: '" + typeNode.getKind() + "'");
			throw new IllegalArgumentException("Unknown variable type: '" + typeNode.getKind() + "'");
		}
	}
	
	/**
	 * Checks if block contains only legal statements (or statement expressions)
	 * 
	 * @param node	block node to check
	 */
	private final void checkBlockParseTree(SimpleNode node) {
		for (int i=0; i<node.jjtGetNumChildren(); i++) {
			final SimpleNode child = (SimpleNode)node.jjtGetChild(i);
			switch (child.getId()) {
			case TransformLangParserTreeConstants.JJTASSIGNMENT:
			case TransformLangParserTreeConstants.JJTBLOCK:
			case TransformLangParserTreeConstants.JJTBREAKSTATEMENT:
			case TransformLangParserTreeConstants.JJTCASESTATEMENT:
			case TransformLangParserTreeConstants.JJTCONTINUESTATEMENT:
			case TransformLangParserTreeConstants.JJTDOSTATEMENT:
			case TransformLangParserTreeConstants.JJTFOREACHSTATEMENT:
			case TransformLangParserTreeConstants.JJTFORSTATEMENT:
			case TransformLangParserTreeConstants.JJTFUNCTIONCALL:
			case TransformLangParserTreeConstants.JJTIFSTATEMENT:
			case TransformLangParserTreeConstants.JJTLOOKUPNODE:
			case TransformLangParserTreeConstants.JJTPOSTFIXEXPRESSION:
			case TransformLangParserTreeConstants.JJTPRINTERRNODE:
			case TransformLangParserTreeConstants.JJTPRINTLOGNODE:
			case TransformLangParserTreeConstants.JJTPRINTSTACKNODE:
			case TransformLangParserTreeConstants.JJTRAISEERRORNODE:
			case TransformLangParserTreeConstants.JJTRETURNSTATEMENT:
			case TransformLangParserTreeConstants.JJTSEQUENCENODE:
			case TransformLangParserTreeConstants.JJTSWITCHSTATEMENT:
			case TransformLangParserTreeConstants.JJTUNARYEXPRESSION:
			case TransformLangParserTreeConstants.JJTVARIABLEDECLARATION:
			case TransformLangParserTreeConstants.JJTWHILESTATEMENT:
				// all expression statements that can occur within block
				break;
			case TransformLangParserTreeConstants.JJTFUNCTIONDECLARATION:
			case TransformLangParserTreeConstants.JJTIMPORTSOURCE:
				// these two are only in for CLVFStart
				break;
			default:
				error(child,"Syntax error, statement expected");
			break;
			}
		}
	}
	
	

	// ----------------- Error Reporting --------------------------

	private void error(SimpleNode node, String error) {
		problemReporter.error(node.getBegin(), node.getEnd(),error, null);
	}

	private void error(SimpleNode node, String error, String hint) {
		problemReporter.error(node.getBegin(),node.getEnd(),error,hint);
	}

	private void warn(SimpleNode node, String warn) {
		problemReporter.warn(node.getBegin(),node.getEnd(), warn, null);
	}

	private void warn(SimpleNode node, String warn, String hint) {
		problemReporter.warn(node.getBegin(),node.getEnd(), warn, hint);
	}

	private void warn(String warn, String hint) {
		problemReporter.warn(1, 1, 1, 2, warn, hint);
	}

}