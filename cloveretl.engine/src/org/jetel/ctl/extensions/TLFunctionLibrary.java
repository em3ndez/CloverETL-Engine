/*
*    jETeL/Clover - Java based ETL application framework.
*    Copyright (C) 2005-07  Javlin Consulting <info@javlinconsulting.cz>
*    
*    This library is free software; you can redistribute it and/or
*    modify it under the terms of the GNU Lesser General Public
*    License as published by the Free Software Foundation; either
*    version 2.1 of the License, or (at your option) any later version.
*    
*    This library is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU    
*    Lesser General Public License for more details.
*    
*    You should have received a copy of the GNU Lesser General Public
*    License along with this library; if not, write to the Free Software
*    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
*/
package org.jetel.ctl.extensions;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetel.ctl.data.TLType;

/**
 * Recommended ascendant of all TL function libraries. This class is intended
 * to be subclassed by function libraries defined in external engine plugins.
 *  
 * @author Martin Zatopek (martin.zatopek@javlin.cz)
 * 		   Michal Tomcanyi (michal.tomcanyi@javlin.cz)
 *         (c) Javlin Consulting (www.javlinconsulting.cz)
 *
 * @created 30.5.2007
 */
public abstract class TLFunctionLibrary implements ITLFunctionLibrary {

	final Log logger = LogFactory.getLog(TLFunctionLibrary.class);

    protected Map<String, List<TLFunctionDescriptor>> library;

    public TLFunctionLibrary() {
        library = new HashMap<String, List<TLFunctionDescriptor>>();
    }

    
    public Map<String, List<TLFunctionDescriptor>> getAllFunctions() {
    	return Collections.unmodifiableMap(library);
    }
    
    public String getLibraryClassName() {
    	return getClass().getName();
    }
    
    /**
     * Allocates executable proxy object for given function existing within this library
     * @param functionName
     * @return
     * @throws IllegalArgumentException when function proxy cannot be created
     */
    public abstract TLFunctionPrototype getExecutable(String functionName) 
    throws IllegalArgumentException;
 
    private void registerFunction(TLFunctionDescriptor prototype) {
    	List<TLFunctionDescriptor> registration = library.get(prototype.getName());
    	if (registration == null) {
    		registration = new LinkedList<TLFunctionDescriptor>();
    		library.put(prototype.getName(),registration);
    	}
    	registration.add(prototype);
    }
    
    public void init() {
    	Class<? extends TLFunctionLibrary> clazz = getClass();
    	TLFunctionAnnotation a = null;
    	for (Method m : clazz.getMethods()) {
    		if ( (a = m.getAnnotation(TLFunctionAnnotation.class)) == null) {
    			continue;
    		}
    	
    		try {
	    		final String functionName = m.getName();
	    		
	    		// extract possible method type parameters for case like:
	    		// e.g. public static <E> List<E> clear(List<E> list)
	    		final List<Type> typeVariables = new LinkedList<Type>();
	    		for (Type t : m.getTypeParameters()) {
	    			typeVariables.add(t);
	    		}
	    		
	    		
	    		boolean isGenericMethod = typeVariables.size() > 0;
	    		
	    		/*
	    		 * Convert return type and formal parameters
	    		 */
	    		final Type javaRetType = m.getGenericReturnType();
	    		
	    		Type[] javaFormal = m.getGenericParameterTypes(); 
	    		Type[] toConvert = new Type[javaFormal.length+1];
	    		toConvert[0] = javaRetType;
	    		System.arraycopy(javaFormal, 0, toConvert, 1, javaFormal.length);
	    		
	    		
	    		TLType[] converted = new TLType[toConvert.length];
	    		for (int i = 0; i < toConvert.length; i++) {
	    			converted[i] = TLType.fromJavaType(toConvert[i]);
				}

	    		TLType returnType = converted[0];
	    		TLType[] formal = new TLType[javaFormal.length];
	    		System.arraycopy(converted, 1, formal, 0, formal.length);
	    		
	    		registerFunction(new TLFunctionDescriptor(this,functionName,a.value(),formal,returnType,isGenericMethod,m.isVarArgs()));
    		} catch (IllegalArgumentException e) {
    			logger.warn("Function '" + getClass().getName() + "." + m.getName() + "' ignored - " + e.getMessage());
    		}
    		
    	}
    }


	@SuppressWarnings("unchecked")
	public static <E> List<E> convertTo(List<Object> list) {
		final List<E> ret = new ArrayList<E>(list.size());
		for (Object o : list) {
			ret.add((E)o);
		}
		return ret;
	}
    
    
 }