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

package org.apache.isis.core.runtime.memento;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.ResolveState;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.encoding.DataInputStreamExtended;
import org.apache.isis.core.metamodel.encoding.DataOutputStreamExtended;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacetUtils;
import org.apache.isis.core.metamodel.facets.object.encodeable.EncodableFacet;
import org.apache.isis.core.metamodel.facets.propcoll.access.PropertyAccessorFacet;
import org.apache.isis.core.metamodel.facets.properties.modify.PropertySetterFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToManyAssociation;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.runtime.context.IsisContext;
import org.apache.isis.core.runtime.persistence.PersistenceSession;
import org.apache.isis.core.runtime.persistence.PersistenceSessionHydrator;
import org.apache.isis.core.runtime.persistence.PersistorUtil;

/**
 * Holds the state for the specified object in serializable form.
 * 
 * <p>
 * This object is {@link Serializable} and can be passed over the network easily. Also for a persistent objects only the
 * reference's {@link Oid}s are held, avoiding the need for serializing the whole object graph.
 */
public class Memento implements Serializable {

    private final static long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(Memento.class);

    private Data state;
    private final List<Oid> transientObjects = new ArrayList<Oid>();

    public Memento(final ObjectAdapter object) {
        state = object == null ? null : createData(object);
        if (LOG.isDebugEnabled()) {
            LOG.debug("created memento for " + this);
        }
    }

    private Data createData(final ObjectAdapter object) {
        if (object.getSpecification().isCollection()) {
            return createCollectionData(object);
        } else {
            return createObjectData(object);
        }
    }

    private Data createCollectionData(final ObjectAdapter object) {
        final CollectionFacet facet = CollectionFacetUtils.getCollectionFacetFromSpec(object);
        final Data[] collData = new Data[facet.size(object)];
        int i = 0;
        for (ObjectAdapter ref : facet.iterable(object)) {
            String resolveStateName = ref.getResolveState().name();
            String specName = ref.getSpecification().getFullName();
            Oid oid = ref.getOid();
            collData[i++] = new Data(oid, resolveStateName, specName);
        }
        String elementTypeSpecName = object.getSpecification().getFullName();
        return new CollectionData(object.getOid(), object.getResolveState(), elementTypeSpecName, collData);
    }

    private ObjectData createObjectData(final ObjectAdapter adapter) {
        final ObjectSpecification cls = adapter.getSpecification();
        final List<ObjectAssociation> fields = cls.getAssociations();
        final ObjectData data = new ObjectData(adapter.getOid(), adapter.getResolveState().name(), cls.getFullName());
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).isNotPersisted()) {
                if (fields.get(i).isOneToManyAssociation()) {
                    continue;
                }
                if (fields.get(i).containsFacet(PropertyAccessorFacet.class)
                    && !fields.get(i).containsFacet(PropertySetterFacet.class)) {
                    LOG.debug("ignoring not-settable field " + fields.get(i).getName());
                    continue;
                }
            }
            createFieldData(adapter, data, fields.get(i));
        }
        return data;
    }

    private void createFieldData(final ObjectAdapter object, final ObjectData data, final ObjectAssociation field) {
        Object fieldData;
        if (field.isOneToManyAssociation()) {
            final ObjectAdapter coll = field.get(object);
            fieldData = createCollectionData(coll);
        } else if (field.getSpecification().isEncodeable()) {
            final EncodableFacet facet = field.getSpecification().getFacet(EncodableFacet.class);
            final ObjectAdapter value = field.get(object);
            fieldData = facet.toEncodedString(value);
        } else if (field.isOneToOneAssociation()) {
            final ObjectAdapter ref = ((OneToOneAssociation) field).get(object);
            fieldData = createReferenceData(ref);
        } else {
            throw new UnknownTypeException(field);
        }
        data.addField(field.getId(), fieldData);
    }

    private Data createReferenceData(final ObjectAdapter ref) {
        if (ref == null) {
            return null;
        }

        Oid refOid = ref.getOid();
        if (refOid == null) {
            return createStandaloneData(ref);
        }

        if (refOid.isTransient() && !transientObjects.contains(refOid)) {
            transientObjects.add(refOid);
            return createObjectData(ref);
        }

        final String resolvedState = ref.getResolveState().name();
        final String specification = ref.getSpecification().getFullName();
        return new Data(refOid, resolvedState, specification);

    }

    private Data createStandaloneData(ObjectAdapter adapter) {
        return new StandaloneData(adapter);
    }

    protected Data getData() {
        return state;
    }

    public Oid getOid() {
        return state.getOid();
    }

    public ObjectAdapter recreateObject() {
        if (state == null) {
            return null;
        }
        final ObjectSpecification spec = getSpecificationLoader().loadSpecification(state.getClassName());
        ObjectAdapter object;
        ResolveState targetState;
        if (getOid().isTransient()) {
            object = getHydrator().recreateAdapter(getOid(), spec);
            targetState = ResolveState.SERIALIZING_TRANSIENT;
        } else {
            object = getHydrator().recreateAdapter(getOid(), spec);
            targetState = ResolveState.UPDATING;
        }
        if (object.getSpecification().isCollection()) {
            populateCollection(object, (CollectionData) state, targetState);
        } else {
            updateObject(object, state, targetState);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("recreated object " + object.getOid());
        }
        return object;
    }

    private void populateCollection(ObjectAdapter collection, CollectionData state, ResolveState targetState) {
        ObjectAdapter[] initData = new ObjectAdapter[state.elements.length];
        int i = 0;
        for (Data elementData : state.elements) {
            initData[i++] = recreateReference(elementData);
        }
        CollectionFacet facet = collection.getSpecification().getFacet(CollectionFacet.class);
        facet.init(collection, initData);
    }

    private ObjectAdapter recreateReference(final Data data) {
        final ObjectSpecification spec = getSpecificationLoader().loadSpecification(data.getClassName());

        if (data instanceof StandaloneData) {
            StandaloneData standaloneData = (StandaloneData) data;
            return standaloneData.getAdapter();
        }

        final Oid oid = data.getOid();
        if (oid == null) {
            return null;
        }

        ObjectAdapter ref;
        if (oid.isTransient()) {
            ref = getHydrator().recreateAdapter(oid, spec);
        } else {
            ref = getHydrator().recreateAdapter(oid, spec);
            ResolveState resolveState = ResolveState.GHOST;
            if (ref.getResolveState().isValidToChangeTo(resolveState)) {
                ref.changeState(resolveState);
            }
            // REVIEW is this needed, or is the object set up at this point
            if (data instanceof ObjectData) {
                updateObject(ref, data, resolveState);
            }
        }
        return ref;
    }

    /**
     * Updates the specified object (assuming it is the correct object for this memento) with the state held by this
     * memento.
     * 
     * @throws IllegalArgumentException
     *             if the memento was created from different logical object to the one specified (i.e. its oid differs).
     */
    public void updateObject(final ObjectAdapter object) {
        updateObject(object, state, ResolveState.RESOLVING);
    }

    private void updateObject(final ObjectAdapter object, final Data state, final ResolveState resolveState) {
        final Object oid = object.getOid();
        if (oid != null && !oid.equals(state.getOid())) {
            throw new IllegalArgumentException(
                "This memento can only be used to update the ObjectAdapter with the Oid " + state.getOid() + " but is "
                    + oid);

        } else {
            if (!(state instanceof ObjectData)) {
                throw new IsisException("Expected an ObjectData but got " + state.getClass());
            } else {
                updateObject(object, resolveState, state);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("object updated " + object.getOid());
            }
        }

    }

    private void updateObject(final ObjectAdapter object, final ResolveState resolveState, final Data state) {
        if (object.getResolveState().isValidToChangeTo(resolveState)) {
            PersistorUtil.start(object, resolveState);
            updateFields(object, state);
            PersistorUtil.end(object);
        } else if (object.getResolveState() == ResolveState.TRANSIENT && resolveState == ResolveState.TRANSIENT) {
            updateFields(object, state);
        } else {
            final ObjectData od = (ObjectData) state;
            if (od.containsField()) {
                throw new IsisException("Resolve state (for " + object
                    + ") inconsistent with fact that data exists for fields");
            }
        }
    }

    private void updateFields(final ObjectAdapter object, final Data state) {
        final ObjectData od = (ObjectData) state;
        final List<ObjectAssociation> fields = object.getSpecification().getAssociations();
        for (ObjectAssociation field : fields) {
            if (field.isNotPersisted()) {
                if (field.isOneToManyAssociation()) {
                    continue;
                }
                if (field.containsFacet(PropertyAccessorFacet.class) && !field.containsFacet(PropertySetterFacet.class)) {
                    LOG.debug("ignoring not-settable field " + field.getName());
                    continue;
                }
            }
            updateField(object, od, field);
        }
    }

    private void updateField(final ObjectAdapter object, final ObjectData od, final ObjectAssociation field) {
        final Object fieldData = od.getEntry(field.getId());

        if (field.isOneToManyAssociation()) {
            updateOneToManyAssociation(object, (OneToManyAssociation) field, (CollectionData) fieldData);

        } else if (field.getSpecification().containsFacet(EncodableFacet.class)) {
            final EncodableFacet facet = field.getSpecification().getFacet(EncodableFacet.class);
            final ObjectAdapter value = facet.fromEncodedString((String) fieldData);
            ((OneToOneAssociation) field).initAssociation(object, value);

        } else if (field.isOneToOneAssociation()) {
            updateOneToOneAssociation(object, (OneToOneAssociation) field, (Data) fieldData);
        }
    }

    private void updateOneToManyAssociation(final ObjectAdapter object, final OneToManyAssociation field,
        final CollectionData collectionData) {
        final ObjectAdapter collection = field.get(object);
        final CollectionFacet facet = CollectionFacetUtils.getCollectionFacetFromSpec(collection);
        final List<ObjectAdapter> original = Lists.newArrayList();
        for (ObjectAdapter adapter : facet.iterable(collection)) {
            original.add(adapter);
        }

        Data[] elements = collectionData.elements;
        for (Data data : elements) {
            final ObjectAdapter element = recreateReference(data);
            if (!facet.contains(collection, element)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  association " + field + " changed, added " + element.getOid());
                }
                field.addElement(object, element);
            } else {
                field.removeElement(object, element);
            }
        }

        for (ObjectAdapter element : original) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("  association " + field + " changed, removed " + element.getOid());
            }
            field.removeElement(object, element);
        }
    }

    private void updateOneToOneAssociation(final ObjectAdapter object, final OneToOneAssociation field,
        final Data fieldData) {
        if (fieldData == null) {
            field.initAssociation(object, null);
        } else {
            final ObjectAdapter ref = recreateReference(fieldData);
            if (field.get(object) != ref) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("  association " + field + " changed to " + ref.getOid());
                }
                field.initAssociation(object, ref);
            }
        }
    }

    public void encodedData(final DataOutputStreamExtended outputImpl) throws IOException {
        outputImpl.writeEncodable(state);
    }

    public void restore(final DataInputStreamExtended inputImpl) throws IOException {
        state = inputImpl.readEncodable(Data.class);
    }

    // ///////////////////////////////////////////////////////////////
    // toString, debug
    // ///////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "[" + (state == null ? null : state.getClassName() + "/" + state.getOid() + state) + "]";
    }

    public void debug(final DebugString debug) {
        if (state != null) {
            state.debug(debug);
        }
    }

    // ///////////////////////////////////////////////////////////////
    // Dependencies (from context)
    // ///////////////////////////////////////////////////////////////

    private static SpecificationLoader getSpecificationLoader() {
        return IsisContext.getSpecificationLoader();
    }

    private static PersistenceSession getPersistenceSession() {
        return IsisContext.getPersistenceSession();
    }

    private static PersistenceSessionHydrator getHydrator() {
        return getPersistenceSession();
    }

}
