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


package org.apache.isis.viewer.wicket.ui.pages;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.isis.core.metamodel.adapter.oid.stringable.OidStringifier;
import org.apache.isis.core.metamodel.services.ServicesInjector;
import org.apache.isis.core.metamodel.spec.SpecificationLoader;
import org.apache.isis.core.runtime.context.IsisContext;
import org.apache.isis.core.runtime.persistence.PersistenceSession;
import org.apache.isis.viewer.wicket.model.models.ApplicationActionsModel;
import org.apache.isis.viewer.wicket.ui.ComponentFactory;
import org.apache.isis.viewer.wicket.ui.ComponentType;
import org.apache.isis.viewer.wicket.ui.app.cssrenderer.ApplicationCssRenderer;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistry;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistryAccessor;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.model.IModel;

/**
 * Convenience adapter for {@link WebPage}s built up using {@link ComponentType}s.
 */
public abstract class PageAbstract extends WebPage {

	public static final String ID_MENU_LINK = "menuLink";
	
	private List<ComponentType> childComponentIds;
	private PageParameters pageParameters;

	public PageAbstract(PageParameters pageParameters, final ComponentType... childComponentIds) {
		addApplicationActionsComponent();
		this.childComponentIds = Collections.unmodifiableList(Arrays.asList(childComponentIds));
		this.pageParameters = pageParameters;
	}
	
	
	/**
	 * As provided in the {@link #PageAbstract(ComponentType) constructor}.
	 * 
	 * <p>
	 * This superclass doesn't do anything with this property directly, but requiring it to
	 * be provided enforces standardization of the implementation of the subclasses.
	 */
	public List<ComponentType> getChildModelTypes() {
		return childComponentIds;
	}
	

	@Override
	public PageParameters getPageParameters() {
		return pageParameters;
	}
	

	private void addApplicationActionsComponent() {
		ApplicationActionsModel model = new ApplicationActionsModel();
		addComponent(ComponentType.APPLICATION_ACTIONS, model);
	}

	/**
	 * For subclasses to call.
	 * 
	 * <p>
	 * Should be called in the subclass' constructor.
	 * 
	 * @param model - used to find the best matching {@link ComponentFactory} to render the model.
	 */
	protected void addChildComponents(IModel<?> model) {
		for (ComponentType componentType : getChildModelTypes()) {
			addComponent(componentType, model);
		}
	}

	private void addComponent(ComponentType componentType, IModel<?> model) {
		getComponentFactoryRegistry().addOrReplaceComponent(this, componentType, model);
	}

	
	@Override
	protected void onRender(MarkupStream markupStream) {
		super.onRender(markupStream);
	}
	
	/**
	 * Renders the application-supplied CSS, if any.
	 */
	@Override
	public void renderHead(HtmlHeaderContainer container) {
		super.renderHead(container);
		final ApplicationCssRenderer applicationCssRenderer = getApplicationCssRenderer();
        applicationCssRenderer.renderApplicationCss(container);
	}


	/////////////////////////////////////////////////////////////////////
	// Convenience
	/////////////////////////////////////////////////////////////////////

	protected ComponentFactoryRegistry getComponentFactoryRegistry() {
		final ComponentFactoryRegistryAccessor cfra = (ComponentFactoryRegistryAccessor) getApplication();
        return cfra.getComponentFactoryRegistry();
	}

    protected ApplicationCssRenderer getApplicationCssRenderer() {
        return (ApplicationCssRenderer) getApplication();
    }
    

	/////////////////////////////////////////////////////
	// System components
	/////////////////////////////////////////////////////

	protected ServicesInjector getServicesInjector() {
		return getPersistenceSession().getServicesInjector();
	}
	
	protected PersistenceSession getPersistenceSession() {
		return IsisContext.getPersistenceSession();
	}

	protected OidStringifier getOidStringifier() {
		return getPersistenceSession().getOidGenerator().getOidStringifier();
	}
	
	protected SpecificationLoader getSpecificationLoader() {
		return IsisContext.getSpecificationLoader();
	}
}
