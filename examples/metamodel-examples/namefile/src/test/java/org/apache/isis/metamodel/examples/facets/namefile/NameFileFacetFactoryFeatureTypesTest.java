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


package org.apache.isis.metamodel.examples.facets.namefile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.core.metamodel.feature.FeatureType;



public class NameFileFacetFactoryFeatureTypesTest {

    private NameFileFacetFactory facetFactory;

    @Before
    public void setUp() throws Exception {
        facetFactory = new NameFileFacetFactory();
    }

    @After
    public void tearDown() throws Exception {
        facetFactory = null;
    }

    @Test
    public void featureTypesLength() {
        List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertThat(featureTypes.size(), is(4));
    }

    @Test
    public void featureTypesContainsTypeRepresentingObject() {
        List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertThat(featureTypes, hasItem(FeatureType.OBJECT));
    }

    @Test
    public void featureTypesContainsTypeRepresentingProperty() {
        List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertThat(featureTypes, hasItem(FeatureType.PROPERTY));
    }

    @Test
    public void featureTypesContainsTypeRepresentingCollection() {
        List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertThat(featureTypes, hasItem(FeatureType.COLLECTION));
    }

    @Test
    public void featureTypesContainsTypeRepresentingAction() {
        List<FeatureType> featureTypes = facetFactory.getFeatureTypes();
        assertThat(featureTypes, hasItem(FeatureType.ACTION));
    }

}
