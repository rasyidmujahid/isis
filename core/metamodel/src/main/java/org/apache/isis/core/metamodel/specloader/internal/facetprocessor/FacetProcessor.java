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


package org.apache.isis.core.metamodel.specloader.internal.facetprocessor;

import static org.apache.isis.core.commons.ensure.Ensure.ensureThatState;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.isis.core.commons.lang.ListUtils;
import org.apache.isis.core.metamodel.config.IsisConfiguration;
import org.apache.isis.core.metamodel.facets.FacetFactory;
import org.apache.isis.core.metamodel.facets.FacetHolder;
import org.apache.isis.core.metamodel.facets.MethodFilteringFacetFactory;
import org.apache.isis.core.metamodel.facets.MethodPrefixBasedFacetFactory;
import org.apache.isis.core.metamodel.facets.MethodRemover;
import org.apache.isis.core.metamodel.facets.MethodRemoverConstants;
import org.apache.isis.core.metamodel.facets.PropertyOrCollectionIdentifyingFacetFactory;
import org.apache.isis.core.metamodel.feature.FeatureType;
import org.apache.isis.core.metamodel.runtimecontext.RuntimeContext;
import org.apache.isis.core.metamodel.runtimecontext.RuntimeContextAware;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.specloader.collectiontyperegistry.CollectionTypeRegistry;
import org.apache.isis.core.metamodel.specloader.progmodelfacets.ProgrammingModelFacets;


public class FacetProcessor implements RuntimeContextAware {

    private final IsisConfiguration configuration;
    private final CollectionTypeRegistry collectionTypeRegistry;
    private final ProgrammingModelFacets programmingModelFacets;
    
    private final SpecificationLoader specificationLoader;
	private RuntimeContext runtimeContext;


    /**
     * Class<FacetFactory> => FacetFactory
     */
    private final Map<Class<? extends FacetFactory>, FacetFactory> factoryByFactoryType = new HashMap<Class<? extends FacetFactory>, FacetFactory>();

    /**
     * {@link FacetFactory Facet factories}, in order they were {@link #registerFactory(FacetFactory)
     * registered}.
     */
    private final List<FacetFactory> factories = new ArrayList<FacetFactory>();

    /**
     * All method prefixes to check in {@link #recognizes(Method)}.
     * 
     * <p>
     * Derived from factories that implement {@link MethodPrefixBasedFacetFactory}.
     * 
     * <p>
     * If <tt>null</tt>, indicates that the cache hasn't been built.
     */
    private List<String> cachedMethodPrefixes;

    /**
     * All registered {@link FacetFactory factories} that implement {@link MethodFilteringFacetFactory}.
     * 
     * <p>
     * Used within {@link #recognizes(Method)}.
     * 
     * <p>
     * If <tt>null</tt>, indicates that the cache hasn't been built.
     */
    private List<MethodFilteringFacetFactory> cachedMethodFilteringFactories;

    /**
     * All registered {@link FacetFactory factories} that implement
     * {@link PropertyOrCollectionIdentifyingFacetFactory}.
     * 
     * <p>
     * Used within {@link #recognizes(Method)}.
     * 
     * <p>
     * If <tt>null</tt>, indicates that the cache hasn't been built.
     */
    private List<PropertyOrCollectionIdentifyingFacetFactory> cachedPropertyOrCollectionIdentifyingFactories;

    /**
     * ObjectFeatureType => List<FacetFactory>
     * 
     * <p>
     * Lazily initialized, then cached. The lists remain in the same order that the factories were
     * {@link #registerFactory(FacetFactory) registered}.
     */
    private Map<FeatureType, List<FacetFactory>> factoryListByFeatureType = null;

    
    public FacetProcessor(
    		final IsisConfiguration configuration, 
    		final SpecificationLoader specificationLoader, 
    		final CollectionTypeRegistry collectionTypeRegistry, 
    		final ProgrammingModelFacets programmingModelFacets) {
        ensureThatState(configuration, is(notNullValue()));
        ensureThatState(collectionTypeRegistry, is(notNullValue()));
        ensureThatState(programmingModelFacets, is(notNullValue()));
        ensureThatState(specificationLoader, is(notNullValue()));
    	
    	this.configuration = configuration;
    	this.specificationLoader = specificationLoader;
    	this.programmingModelFacets = programmingModelFacets;
    	this.collectionTypeRegistry = collectionTypeRegistry;
    }
    
    ////////////////////////////////////////////////////
    // init, shutdown (application scoped)
    ////////////////////////////////////////////////////
    
    public void init() {
        ensureThatState(runtimeContext, is(notNullValue()));
        programmingModelFacets.init();
        final List<FacetFactory> facetFactoryList = programmingModelFacets.getList();
        for (final FacetFactory facetFactory : facetFactoryList) {
            registerFactory(facetFactory);
        }
    }

	public void shutdown() {
	}

    public void registerFactory(final FacetFactory factory) {
        clearCaches();
        factoryByFactoryType.put(factory.getClass(), factory);
        factories.add(factory);
        
        injectDependenciesInto(factory);
    }

    /**
     * This is <tt>public</tt> so that can be used for <tt>@Facets</tt> processing 
     * (eg in <tt>JavaIntrospector</tt>).
     *
     * <p>
     * See NOF bug-517.
     */
	public void injectDependenciesInto(final FacetFactory factory) {
		getCollectionTypeRepository().injectInto(factory);
        getIsisConfiguration().injectInto(factory);
		
        // cascades all the subcomponents also
        getRuntimeContext().injectInto(factory); 
	}

	public FacetFactory getFactoryByFactoryType(final Class<? extends FacetFactory> factoryType) {
        return factoryByFactoryType.get(factoryType);
    }

    /**
     * Appends to the supplied {@link Set} all of the {@link Method}s that may represent a property or
     * collection.
     * 
     * <p>
     * Delegates to all known {@link PropertyOrCollectionIdentifyingFacetFactory}s.
     */
    public Set<Method> findPropertyOrCollectionCandidateAccessors(final List<Method> methods, final Set<Method> candidates) {
        cachePropertyOrCollectionIdentifyingFacetFactoriesIfRequired();
        for (Method method: methods) {
            if (method == null) {
                continue;
            }
            for (final PropertyOrCollectionIdentifyingFacetFactory facetFactory : cachedPropertyOrCollectionIdentifyingFactories) {
                if (facetFactory.isPropertyOrCollectionAccessorCandidate(method)) {
                    candidates.add(method);
                }
            }
        }
        return candidates;
    }

    /**
     * Use the provided {@link MethodRemover} to have all known
     * {@link PropertyOrCollectionIdentifyingFacetFactory}s to remove all property accessors, and append them
     * to the supplied methodList.
     * 
     * <p>
     * Intended to be called after {@link #findAndRemoveValuePropertyAccessors(MethodRemover, List)} once only
     * reference properties remain.
     * 
     * @see PropertyOrCollectionIdentifyingFacetFactory#findAndRemoveValuePropertyAccessors(MethodRemover,
     *      List)
     */
    public void findAndRemovePropertyAccessors(final MethodRemover methodRemover, final List<Method> methodListToAppendTo) {
        for (final PropertyOrCollectionIdentifyingFacetFactory facetFactory : cachedPropertyOrCollectionIdentifyingFactories) {
            facetFactory.findAndRemovePropertyAccessors(methodRemover, methodListToAppendTo);
        }
    }

    /**
     * Use the provided {@link MethodRemover} to have all known
     * {@link PropertyOrCollectionIdentifyingFacetFactory}s to remove all property accessors, and append them
     * to the supplied methodList.
     * 
     * @see PropertyOrCollectionIdentifyingFacetFactory#findAndRemoveCollectionAccessors(MethodRemover, List)
     */
    public void findAndRemoveCollectionAccessors(final MethodRemover methodRemover, final List<Method> methodListToAppendTo) {
        cachePropertyOrCollectionIdentifyingFacetFactoriesIfRequired();
        for (final PropertyOrCollectionIdentifyingFacetFactory facetFactory : cachedPropertyOrCollectionIdentifyingFactories) {
            facetFactory.findAndRemoveCollectionAccessors(methodRemover, methodListToAppendTo);
        }
    }


    /**
     * Whether this {@link Method method} is recognized by any of the {@link FacetFactory}s.
     * 
     * <p>
     * Typically this is when method has a specific prefix, such as <tt>validate</tt> or <tt>hide</tt>.
     * Specifically, it checks:
     * <ul>
     * <li>the method's prefix against the prefixes supplied by any {@link MethodPrefixBasedFacetFactory}</li>
     * <li>the method against any {@link MethodFilteringFacetFactory}</li>
     * </ul>
     * 
     * <p>
     * The design of {@link MethodPrefixBasedFacetFactory} (whereby this facet factory set does the work) is a
     * slight performance optimization for when there are multiple facet factories that search for the same
     * prefix.
     */
    public boolean recognizes(final Method method) {
        cacheMethodPrefixesIfRequired();
        final String methodName = method.getName();
        for (final String prefix : cachedMethodPrefixes) {
            if (methodName.startsWith(prefix)) {
                return true;
            }
        }

        cacheMethodFilteringFacetFactoriesIfRequired();
        for (final MethodFilteringFacetFactory factory : cachedMethodFilteringFactories) {
            if (factory.recognizes(method)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Attaches all facets applicable to the provided {@link FeatureType#OBJECT object}) to the
     * supplied {@link FacetHolder}.
     * 
     * <p>
     * Delegates to {@link FacetFactory#process(Class, FacetHolder)} for each appropriate factory.
     * 
     * @see FacetFactory#process(Class, MethodRemover, FacetHolder)
     * 
     * @param cls
     *            - class to process
     * @param facetHolder
     *            - holder to attach facets to.
     * 
     * @return <tt>true</tt> if any facets were added, <tt>false</tt> otherwise.
     */
    public boolean process(final Class<?> cls, final MethodRemover methodRemover, final FacetHolder facetHolder) {
        boolean facetsAdded = false;
        final List<FacetFactory> factoryList = getFactoryListByFeatureType(FeatureType.OBJECT);
        for (final FacetFactory facetFactory : factoryList) {
            facetsAdded = facetFactory.process(cls, removerElseNullRemover(methodRemover), facetHolder) | facetsAdded;
        }
        return facetsAdded;
    }

    /**
     * Attaches all facets applicable to the provided {@link FeatureType type of feature} to the
     * supplied {@link FacetHolder}.
     * 
     * <p>
     * Delegates to {@link FacetFactory#process(Method, FacetHolder)} for each appropriate factory.
     * 
     * @see FacetFactory#process(Method, FacetHolder)
     * 
     * @param cls
     *           - class in which introspect; allowing the helper methods to be found is subclasses of that which the method was originally found.
     * @param method
     *            - method to process
     * @param facetHolder
     *            - holder to attach facets to.
     * @param featureType
     *            - what type of feature the method represents (property, action, collection etc)
     * 
     * @return <tt>true</tt> if any facets were added, <tt>false</tt> otherwise.
     */
    public boolean process(
            final Class<?> cls,
            final Method method,
            final MethodRemover methodRemover,
            final FacetHolder facetHolder,
            final FeatureType featureType) {
        boolean facetsAdded = false;
        final List<FacetFactory> factoryList = getFactoryListByFeatureType(featureType);
        for (final FacetFactory facetFactory : factoryList) {
            facetsAdded = facetFactory.process(cls, method, removerElseNullRemover(methodRemover), facetHolder) | facetsAdded;
        }
        return facetsAdded;
    }

    /**
     * Attaches all facets applicable to the provided {@link FeatureType#ACTION_PARAMETER
     * parameter}), to the supplied {@link FacetHolder}.
     * 
     * <p>
     * Delegates to {@link FacetFactory#processParams(Method, int, FacetHolder)} for each appropriate factory.
     * 
     * @see FacetFactory#processParams(Method, int, FacetHolder)
     * 
     * @param method
     *            - action method to process
     * @param paramNum
     *            - 0-based
     * @param facetHolder
     *            - holder to attach facets to.
     * 
     * @return <tt>true</tt> if any facets were added, <tt>false</tt> otherwise.
     */
    public boolean processParams(final Method method, final int paramNum, final FacetHolder facetHolder) {
        boolean facetsAdded = false;
        final List<FacetFactory> factoryList = getFactoryListByFeatureType(FeatureType.ACTION_PARAMETER);
        for (final FacetFactory facetFactory : factoryList) {
            facetsAdded = facetFactory.processParams(method, paramNum, facetHolder) | facetsAdded;
        }
        return facetsAdded;
    }

    private List<FacetFactory> getFactoryListByFeatureType(final FeatureType featureType) {
        cacheByFeatureTypeIfRequired();
        return factoryListByFeatureType.get(featureType);
    }

    private void clearCaches() {
        factoryListByFeatureType = null;
        cachedMethodPrefixes = null;
        cachedMethodFilteringFactories = null;
        cachedPropertyOrCollectionIdentifyingFactories = null;
    }

    private synchronized void cacheByFeatureTypeIfRequired() {
        if (factoryListByFeatureType != null) {
            return;
        }
        factoryListByFeatureType = new HashMap<FeatureType, List<FacetFactory>>();
        for (final FacetFactory factory : factories) {
            final List<FeatureType> featureTypes = factory.getFeatureTypes();
            for (FeatureType featureType: featureTypes) {
                final List<FacetFactory> factoryList = getList(factoryListByFeatureType, featureType);
                factoryList.add(factory);
            }
        }
    }

    private synchronized void cacheMethodPrefixesIfRequired() {
        if (cachedMethodPrefixes != null) {
            return;
        }
        cachedMethodPrefixes = new ArrayList<String>();
        for (final FacetFactory facetFactory : factories) {
            if (facetFactory instanceof MethodPrefixBasedFacetFactory) {
                final MethodPrefixBasedFacetFactory methodPrefixBasedFacetFactory = (MethodPrefixBasedFacetFactory) facetFactory;
                ListUtils.merge(cachedMethodPrefixes, methodPrefixBasedFacetFactory.getPrefixes());
            }
        }
    }

    private synchronized void cacheMethodFilteringFacetFactoriesIfRequired() {
        if (cachedMethodFilteringFactories != null) {
            return;
        }
        cachedMethodFilteringFactories = new ArrayList<MethodFilteringFacetFactory>();
        for (final FacetFactory factory : factories) {
            if (factory instanceof MethodFilteringFacetFactory) {
                final MethodFilteringFacetFactory methodFilteringFacetFactory = (MethodFilteringFacetFactory) factory;
                cachedMethodFilteringFactories.add(methodFilteringFacetFactory);
            }
        }
    }

    private synchronized void cachePropertyOrCollectionIdentifyingFacetFactoriesIfRequired() {
        if (cachedPropertyOrCollectionIdentifyingFactories != null) {
            return;
        }
        cachedPropertyOrCollectionIdentifyingFactories = new ArrayList<PropertyOrCollectionIdentifyingFacetFactory>();
        final Iterator<FacetFactory> iter = factories.iterator();
        while (iter.hasNext()) {
            final FacetFactory factory = iter.next();
            if (factory instanceof PropertyOrCollectionIdentifyingFacetFactory) {
                final PropertyOrCollectionIdentifyingFacetFactory identifyingFacetFactory = (PropertyOrCollectionIdentifyingFacetFactory) factory;
                cachedPropertyOrCollectionIdentifyingFactories.add(identifyingFacetFactory);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(final Map map, final Object key) {
        List<T> list = (List<T>) map.get(key);
        if (list == null) {
            list = new ArrayList<T>();
            map.put(key, list);
        }
        return list;
    }

    private MethodRemover removerElseNullRemover(final MethodRemover methodRemover) {
        return methodRemover != null ? methodRemover : MethodRemoverConstants.NULL;
    }

    
    // ////////////////////////////////////////////////////////////////////
    // Dependencies (injected in constructor)
    // ////////////////////////////////////////////////////////////////////

    private IsisConfiguration getIsisConfiguration() {
        return configuration;
    }
    private SpecificationLoader getSpecificationLoader() {
        return specificationLoader;
    }
    private CollectionTypeRegistry getCollectionTypeRepository() {
        return collectionTypeRegistry;
    }


    // ////////////////////////////////////////////////////////////////////
    // Dependencies (injected via setter due to *Aware)
    // ////////////////////////////////////////////////////////////////////

    private RuntimeContext getRuntimeContext() {
		return runtimeContext;
	}
    /**
     * Injected so can propogate to any {@link #registerFactory(FacetFactory) registered} {@link FacetFactory}
     * s that are also {@link RuntimeContextAware}.
     */
	@Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
		this.runtimeContext = runtimeContext;
	}


}
