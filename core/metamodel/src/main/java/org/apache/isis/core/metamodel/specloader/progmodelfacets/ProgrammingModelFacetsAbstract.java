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


package org.apache.isis.core.metamodel.specloader.progmodelfacets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.isis.core.commons.factory.InstanceFactory;
import org.apache.isis.core.metamodel.facets.FacetFactory;


public abstract class ProgrammingModelFacetsAbstract implements ProgrammingModelFacets {

    private final List<FacetFactory> facetFactories = new ArrayList<FacetFactory>();
    private final List<Class<? extends FacetFactory>> facetFactoryClasses = new ArrayList<Class<? extends FacetFactory>>();

    public final List<FacetFactory> getList() {
        return Collections.unmodifiableList(facetFactories);
    }

    public final void addFactory(final Class<? extends FacetFactory> factoryClass) {
        facetFactoryClasses.add(factoryClass);
    }

    public final void removeFactory(final Class<? extends FacetFactory> factoryClass) {
        facetFactoryClasses.remove(factoryClass);
    }


    public void init() {
    	for(Class<? extends FacetFactory> factoryClass: facetFactoryClasses) {
    		FacetFactory facetFactory = 
    			(FacetFactory) InstanceFactory.createInstance(factoryClass);
    		facetFactories.add(facetFactory);
    	}
    }

}
