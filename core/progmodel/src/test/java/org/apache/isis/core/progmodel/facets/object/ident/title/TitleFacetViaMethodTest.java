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


package org.apache.isis.core.progmodel.facets.object.ident.title;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.facets.FacetHolder;

@RunWith(JMock.class)
public class TitleFacetViaMethodTest  {

	private Mockery mockery = new JUnit4Mockery();
	
    private TitleFacetViaTitleMethod facet;
	private FacetHolder mockFacetHolder;

	private ObjectAdapter mockOwningAdapter;

	private DomainObjectWithProblemInItsTitleMethod pojo;

    public static class DomainObjectWithProblemInItsTitleMethod {
        public String title() {
            throw new NullPointerException();
        }
    }

    @Before
    public void setUp() throws Exception {

    	pojo = new DomainObjectWithProblemInItsTitleMethod();
    	mockFacetHolder = mockery.mock(FacetHolder.class);
    	mockOwningAdapter = mockery.mock(ObjectAdapter.class);
        Method iconNameMethod = DomainObjectWithProblemInItsTitleMethod.class.getMethod("title");
		facet = new TitleFacetViaTitleMethod(iconNameMethod, mockFacetHolder);
		
		mockery.checking(new Expectations(){{
			allowing(mockOwningAdapter).getObject();
			will(returnValue(pojo));
		}});
    }

    @After
    public void tearDown() throws Exception {
        facet = null;
    }

    @Test
    public void testTitleThrowsException() {
    	String title = facet.title(mockOwningAdapter);
    	assertThat(title, is(nullValue()));
    }

}

