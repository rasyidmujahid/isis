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


package org.apache.isis.core.runtime.persistence.query;

import java.io.IOException;

import org.apache.isis.core.metamodel.encoding.DataInputExtended;
import org.apache.isis.core.metamodel.encoding.DataInputStreamExtended;
import org.apache.isis.core.metamodel.encoding.DataOutputExtended;
import org.apache.isis.core.metamodel.encoding.Encodable;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.runtime.context.IsisContext;


public abstract class PersistenceQueryAbstract implements PersistenceQuery, Encodable {
	
    private final ObjectSpecification specification;

    public PersistenceQueryAbstract(
    		final ObjectSpecification specification) {
    	this.specification = specification;
    	initialized();
    }

    protected PersistenceQueryAbstract(DataInputExtended input) throws IOException {
    	String specName = input.readUTF();
        specification = getSpecificationLoader().loadSpecification(specName);
        initialized();
    }

    public void encode(DataOutputExtended output)
    		throws IOException {
    	output.writeUTF(specification.getFullName());
    }
    
    
    private void initialized() {
    	// nothing to do
    }
    
    /////////////////////////////////////////////////////////
    //
    /////////////////////////////////////////////////////////


    public ObjectSpecification getSpecification() {
        return specification;
    }

    
    /////////////////////////////////////////////////////////
    // equals, hashCode
    /////////////////////////////////////////////////////////

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PersistenceQueryAbstract other = (PersistenceQueryAbstract) obj;
        if (specification == null) {
            if (other.specification != null) {
                return false;
            }
        } else if (!specification.equals(other.specification)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + 1231;
        result = PRIME * result + ((specification == null) ? 0 : specification.hashCode());
        return result;
    }

    /////////////////////////////////////////////////////////
    // Dependencies (from context)
    /////////////////////////////////////////////////////////

	protected static SpecificationLoader getSpecificationLoader() {
		return IsisContext.getSpecificationLoader();
	}


}

