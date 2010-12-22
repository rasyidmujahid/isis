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


package org.apache.isis.core.metamodel.spec;

import junit.framework.TestCase;


public class PersistableTest extends TestCase {

    private Persistability persistable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testToStringForProgramPersistable() {
        persistable = Persistability.PROGRAM_PERSISTABLE;
        assertEquals("Program Persistable", persistable.toString());
    }

    public void testToStringForTransient() {
        persistable = Persistability.TRANSIENT;
        assertEquals("Transient", persistable.toString());
    }

    public void testToStringForUserPersistable() {
        persistable = Persistability.USER_PERSISTABLE;
        assertEquals("User Persistable", persistable.toString());
    }

}

