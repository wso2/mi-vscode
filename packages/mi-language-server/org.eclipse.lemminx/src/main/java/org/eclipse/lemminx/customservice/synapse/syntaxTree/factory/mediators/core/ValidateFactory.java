/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     WSO2 LLC - support for WSO2 Micro Integrator Configuration
 */

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Feature;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.Validate;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateOnFail;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.validate.ValidateSchema;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.utils.SyntaxTreeUtils;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class ValidateFactory extends AbstractMediatorFactory {

    private static final String VALIDATE = "validate";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Validate validate = new Validate();
        validate.elementNode(element);
        populateAttributes(validate, element);
        List<DOMNode> children = element.getChildren();
        List<ValidateProperty> validateProperties = new ArrayList<>();
        List<ValidateSchema> validateSchemas = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        List<ValidateResource> validateResources = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String nodeName = node.getNodeName();
                if (nodeName.equalsIgnoreCase(Constant.PROPERTY)) {
                    ValidateProperty validateProperty = new ValidateProperty();
                    validateProperty.elementNode((DOMElement) node);
                    populatePropertyAttributes(validateProperty, (DOMElement) node);
                    validateProperties.add(validateProperty);
                } else if (nodeName.equalsIgnoreCase(Constant.SCHEMA)) {
                    ValidateSchema validateSchema = new ValidateSchema();
                    validateSchema.elementNode((DOMElement) node);
                    populateSchemaAttributes(validateSchema, (DOMElement) node);
                    validateSchemas.add(validateSchema);
                } else if (nodeName.equalsIgnoreCase(Constant.ON_FAIL)) {
                    ValidateOnFail validateOnFail = new ValidateOnFail();
                    validateOnFail.elementNode((DOMElement) node);
                    List<DOMNode> onFailChildren = node.getChildren();
                    if (onFailChildren != null && !onFailChildren.isEmpty()) {
                        List<Mediator> mediators = SyntaxTreeUtils.createMediators(onFailChildren);
                        validateOnFail.setMediatorList(mediators);
                    }
                    validate.setOnFail(validateOnFail);
                } else if (nodeName.equalsIgnoreCase(Constant.FEATURE)) {
                    Feature feature = new Feature();
                    feature.elementNode((DOMElement) node);
                    populateFeatureAttributes(feature, (DOMElement) node);
                    features.add(feature);
                } else if (nodeName.equalsIgnoreCase(Constant.RESOURCE)) {
                    ValidateResource validateResource = new ValidateResource();
                    validateResource.elementNode((DOMElement) node);
                    populateResourceAttributes(validateResource, (DOMElement) node);
                    validateResources.add(validateResource);
                } else {
                    //ignore
                }
            }
        }
        validate.setProperty(validateProperties.toArray(new ValidateProperty[validateProperties.size()]));
        validate.setSchema(validateSchemas.toArray(new ValidateSchema[validateSchemas.size()]));
        validate.setFeature(features.toArray(new Feature[features.size()]));
        validate.setResource(validateResources.toArray(new ValidateResource[validateResources.size()]));
        return validate;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Validate) node).setDescription(description);
        }
        String source = element.getAttribute(Constant.SOURCE);
        if (source != null) {
            ((Validate) node).setSource(source);
        }
        String cacheSchema = element.getAttribute(Constant.CACHE_SCHEMA);
        if (cacheSchema != null) {
            ((Validate) node).setCacheSchema(Boolean.parseBoolean(cacheSchema));
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Validate) node).setTraceFilter(traceFilter);
        }
    }

    private void populatePropertyAttributes(ValidateProperty validateProperty, DOMElement node) {

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            validateProperty.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null) {
            validateProperty.setValue(Boolean.parseBoolean(value));
        }
    }

    private void populateSchemaAttributes(ValidateSchema validateSchema, DOMElement node) {

        String key = node.getAttribute(Constant.KEY);
        if (key != null) {
            validateSchema.setKey(key);
        }
    }

    private void populateFeatureAttributes(Feature feature, DOMElement node) {

        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            feature.setName(name);
        }
        String value = node.getAttribute(Constant.VALUE);
        if (value != null) {
            feature.setValue(Boolean.parseBoolean(value));
        }
    }

    private void populateResourceAttributes(ValidateResource validateResource, DOMElement node) {

        String location = node.getAttribute(Constant.LOCATION);
        if (location != null) {
            validateResource.setLocation(location);
        }
        String key = node.getAttribute(Constant.KEY);
        if (key != null) {
            validateResource.setKey(key);
        }
    }

    @Override
    public String getTagName() {

        return VALIDATE;
    }
}
