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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.other;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.AbstractMediatorFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.Attribute;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEvent;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributes;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributesArbitrary;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributesArbitraryAttribute;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributesCorrelation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributesMeta;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent.PublishEventAttributesPayload;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class PublishEventFactory extends AbstractMediatorFactory {

    private static final String PUBLISH_EVENT = "publishEvent";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        PublishEvent publishEvent = new PublishEvent();
        publishEvent.elementNode(element);
        populateAttributes(publishEvent, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.EVENT_SINK)) {
                    String eventSink = Utils.getInlineString(child.getFirstChild());
                    publishEvent.setEventSink(eventSink);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.STREAM_NAME)) {
                    String streamName = Utils.getInlineString(child.getFirstChild());
                    publishEvent.setStreamName(streamName);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.STREAM_VERSION)) {
                    String streamVersion = Utils.getInlineString(child.getFirstChild());
                    publishEvent.setStreamVersion(streamVersion);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ATTRIBUTES)) {
                    PublishEventAttributes attributes = createPublishEventAttributes(child);
                    publishEvent.setAttributes(attributes);
                }
            }
        }

        return publishEvent;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String async = element.getAttribute(Constant.ASYNC);
        if (async != null) {
            ((PublishEvent) node).setAsync(Boolean.parseBoolean(async));
        }
        String timeout = element.getAttribute(Constant.TIMEOUT);
        if (timeout != null) {
            ((PublishEvent) node).setTimeout(timeout);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((PublishEvent) node).setDescription(description);
        }
    }

    private PublishEventAttributes createPublishEventAttributes(DOMNode node) {

        PublishEventAttributes publishEventAttributes = new PublishEventAttributes();
        publishEventAttributes.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.META)) {
                    PublishEventAttributesMeta publishEventAttributesMeta = createPublishEventAttributesMeta(child);
                    publishEventAttributes.setMeta(publishEventAttributesMeta);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.CORRELATION)) {
                    PublishEventAttributesCorrelation publishEventAttributesCorrelation =
                            createPublishEventAttributesCorrelation(child);
                    publishEventAttributes.setCorrelation(publishEventAttributesCorrelation);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.PAYLOAD)) {
                    PublishEventAttributesPayload publishEventAttributesPayload =
                            createPublishEventAttributesPayload(child);
                    publishEventAttributes.setPayload(publishEventAttributesPayload);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.ARBITRARY)) {
                    PublishEventAttributesArbitrary publishEventAttributesArbitrary =
                            createPublishEventAttributesArbitrary(child);
                    publishEventAttributes.setArbitrary(publishEventAttributesArbitrary);
                }
            }
        }
        return publishEventAttributes;
    }

    private Attribute createAttribute(DOMNode childNode) {

        Attribute attribute = new Attribute();
        attribute.elementNode((DOMElement) childNode);
        String name = childNode.getAttribute(Constant.NAME);
        if (name != null) {
            attribute.setName(name);
        }
        String dataType = childNode.getAttribute(Constant.TYPE);
        if (dataType != null) {
            attribute.setDataType(dataType);
        }
        String _default = childNode.getAttribute(Constant.DEFAULT_VALUE);
        if (_default != null) {
            attribute.set_default(_default);
        }
        String value = childNode.getAttribute(Constant.VALUE);
        if (value != null) {
            attribute.setValue(value);
        }
        String expression = childNode.getAttribute(Constant.EXPRESSION);
        if (expression != null) {
            attribute.setExpression(expression);
        }
        return attribute;
    }

    private PublishEventAttributesMeta createPublishEventAttributesMeta(DOMNode child) {

        PublishEventAttributesMeta publishEventAttributesMeta = new PublishEventAttributesMeta();
        publishEventAttributesMeta.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Attribute> attributes = new ArrayList<>();
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.ATTRIBUTE)) {
                    Attribute attribute = createAttribute(childNode);
                    attributes.add(attribute);
                }
            }
            publishEventAttributesMeta.setAttributes(attributes.toArray(new Attribute[attributes.size()]));
        }
        return publishEventAttributesMeta;
    }

    private PublishEventAttributesCorrelation createPublishEventAttributesCorrelation(DOMNode child) {

        PublishEventAttributesCorrelation publishEventAttributesCorrelation = new PublishEventAttributesCorrelation();
        publishEventAttributesCorrelation.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Attribute> attributes = new ArrayList<>();
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.ATTRIBUTE)) {
                    Attribute attribute = createAttribute(childNode);
                    attributes.add(attribute);
                }
            }
            publishEventAttributesCorrelation.setAttributes(attributes.toArray(new Attribute[attributes.size()]));
        }
        return publishEventAttributesCorrelation;
    }

    private PublishEventAttributesPayload createPublishEventAttributesPayload(DOMNode child) {

        PublishEventAttributesPayload publishEventAttributesPayload = new PublishEventAttributesPayload();
        publishEventAttributesPayload.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<Attribute> attributes = new ArrayList<>();
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.ATTRIBUTE)) {
                    Attribute attribute = createAttribute(childNode);
                    attributes.add(attribute);
                }
            }
            publishEventAttributesPayload.setAttributes(attributes.toArray(new Attribute[attributes.size()]));
        }
        return publishEventAttributesPayload;
    }

    private PublishEventAttributesArbitrary createPublishEventAttributesArbitrary(DOMNode child) {

        PublishEventAttributesArbitrary publishEventAttributesArbitrary = new PublishEventAttributesArbitrary();
        publishEventAttributesArbitrary.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        if (children != null && !children.isEmpty()) {
            List<PublishEventAttributesArbitraryAttribute> attributes = new ArrayList<>();
            for (DOMNode childNode : children) {
                if (childNode.getNodeName().equalsIgnoreCase(Constant.ATTRIBUTE)) {
                    PublishEventAttributesArbitraryAttribute attribute = createArbitaryAttribute(childNode);
                    attributes.add(attribute);
                }
            }
            publishEventAttributesArbitrary.setAttributes(attributes.toArray(new PublishEventAttributesArbitraryAttribute[attributes.size()]));
        }
        return publishEventAttributesArbitrary;
    }

    private PublishEventAttributesArbitraryAttribute createArbitaryAttribute(DOMNode childNode) {

        PublishEventAttributesArbitraryAttribute attribute = new PublishEventAttributesArbitraryAttribute();
        attribute.elementNode((DOMElement) childNode);
        String name = childNode.getAttribute(Constant.NAME);
        if (name != null) {
            attribute.setName(name);
        }
        String dataType = childNode.getAttribute(Constant.DATA_TYPE);
        if (dataType != null) {
            attribute.setDataType(dataType);
        }
        String _default = childNode.getAttribute(Constant.DEFAULT);
        if (_default != null) {
            attribute.set_default(_default);
        }
        String value = childNode.getAttribute(Constant.VALUE);
        if (value != null) {
            attribute.setValue(value);
        }
        return attribute;
    }

    @Override
    public String getTagName() {

        return PUBLISH_EVENT;
    }
}
