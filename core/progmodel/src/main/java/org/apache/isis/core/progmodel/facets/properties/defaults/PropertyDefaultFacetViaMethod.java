/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.isis.core.progmodel.facets.properties.defaults;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.metamodel.adapter.AdapterInvokeUtils;
import org.apache.isis.core.metamodel.adapter.AdapterMap;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.ImperativeFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLookup;


public class PropertyDefaultFacetViaMethod extends PropertyDefaultFacetAbstract implements ImperativeFacet {

    private final Method method;
	private final SpecificationLookup specificationLookup;
	private final AdapterMap adapterMap;

    public PropertyDefaultFacetViaMethod(
    		final Method method, 
    		final FacetHolder holder, 
    		final SpecificationLookup specificationLookup, 
    		final AdapterMap adapterManager) {
        super(holder);
        this.method = method;
        this.specificationLookup = specificationLookup;
        this.adapterMap = adapterManager;
    }

    /**
     * Returns a singleton list of the {@link Method} provided in the constructor. 
     */
    @Override
    public List<Method> getMethods() {
    	return Collections.singletonList(method);
    }

	@Override
    public boolean impliesResolve() {
		return true;
	}

	@Override
    public boolean impliesObjectChanged() {
		return false;
	}

    @Override
    public ObjectAdapter getDefault(final ObjectAdapter owningAdapter) {
        final Object result = AdapterInvokeUtils.invoke(method, owningAdapter);
        return createAdapter(method.getReturnType(), result);
    }

    
    private ObjectAdapter createAdapter(final Class<?> type, final Object object) {
	    final ObjectSpecification specification = getSpecificationLookup().loadSpecification(type);
	    if (specification.isNotCollection()) {
	        return getAdapterMap().adapterFor(object);
	    } else {
	        throw new UnknownTypeException("not an object, is this a collection?");
	    }
	}


	@Override
    protected String toStringValues() {
        return "method=" + method;
    }


    // //////////////////////////////////////////////////////////////////
    // Dependencies (from constructor)
    // //////////////////////////////////////////////////////////////////


    private SpecificationLookup getSpecificationLookup() {
		return specificationLookup;
	}

    protected AdapterMap getAdapterMap() {
        return adapterMap;
    }
    

}

