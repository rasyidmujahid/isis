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

package org.apache.isis.core.progmodel.facets.actions.choices;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.isis.core.commons.lang.ArrayUtils;
import org.apache.isis.core.metamodel.adapter.AdapterInvokeUtils;
import org.apache.isis.core.metamodel.adapter.AdapterMap;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.exceptions.ModelException;
import org.apache.isis.core.metamodel.facets.ChoicesUtils;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.ImperativeFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLookup;

public class ActionChoicesFacetViaMethod extends ActionChoicesFacetAbstract implements ImperativeFacet {

    private final Method method;
    private final Class<?> choicesType;
    private final SpecificationLookup specificationLookup;
    private final AdapterMap adapterMap;

    public ActionChoicesFacetViaMethod(final Method method, final Class<?> choicesType, final FacetHolder holder,
        final SpecificationLookup specificationLookup, final AdapterMap adapterManager) {
        super(holder);
        this.method = method;
        this.choicesType = choicesType;
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
    public Object[][] getChoices(final ObjectAdapter owningAdapter) {
        Object invoke = AdapterInvokeUtils.invoke(method, owningAdapter);
        if (!(invoke instanceof Object[])) {
            throw new ModelException(
                "Expected an array of collections (Object[]) containing choices for all parameters, but got " + invoke
                    + " instead. Perhaps the parameter number is missing!");
        }
        final Object[] options = (Object[]) invoke;
        final Object[][] results = new Object[options.length][];
        for (int i = 0; i < results.length; i++) {
            if (options[i] == null) {
                results[i] = null;
            } else if (options[i].getClass().isArray()) {
                results[i] = ArrayUtils.getObjectAsObjectArray(options[i]);
            } else {
                final ObjectSpecification specification = getSpecificationLookup().loadSpecification(choicesType);
                results[i] =
                    ChoicesUtils.getCollectionAsObjectArray(options[i], specification, getAdapterMap());
            }
        }
        return results;
    }

    @Override
    protected String toStringValues() {
        return "method=" + method + ",type=" + choicesType;
    }

    // ///////////////////////////////////////////////////////
    // Dependencies
    // ///////////////////////////////////////////////////////

    private SpecificationLookup getSpecificationLookup() {
        return specificationLookup;
    }

    private AdapterMap getAdapterMap() {
        return adapterMap;
    }

}
