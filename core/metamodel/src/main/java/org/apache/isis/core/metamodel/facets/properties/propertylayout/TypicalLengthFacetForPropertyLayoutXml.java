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

package org.apache.isis.core.metamodel.facets.properties.propertylayout;

import org.apache.isis.applib.layout.v1_0.PropertyLayout;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.objectvalue.typicallen.TypicalLengthFacet;
import org.apache.isis.core.metamodel.facets.objectvalue.typicallen.TypicalLengthFacetAbstract;

public class TypicalLengthFacetForPropertyLayoutXml extends TypicalLengthFacetAbstract {

    public static TypicalLengthFacet create(PropertyLayout propertyLayout, FacetHolder holder) {
        if(propertyLayout == null) {
            return null;
        }
        final Integer typicalLength = propertyLayout.getTypicalLength();
        return typicalLength != null && typicalLength != -1 ? new TypicalLengthFacetForPropertyLayoutXml(typicalLength, holder) : null;
    }

    private final int typicalLength;

    private TypicalLengthFacetForPropertyLayoutXml(int typicalLength, FacetHolder holder) {
        super(holder, Derivation.NOT_DERIVED);
        this.typicalLength = typicalLength;
    }

    @Override
    public int value() {
        return typicalLength;
    }

}
