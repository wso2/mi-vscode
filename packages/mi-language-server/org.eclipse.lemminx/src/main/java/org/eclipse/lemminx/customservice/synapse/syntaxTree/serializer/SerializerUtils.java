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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMComment;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.CommentMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.HashMap;
import javax.xml.stream.XMLStreamException;

public class SerializerUtils {

    /**
     * Serialize the expression to the OMElement
     *
     * @param expression    the expression to be serialized
     * @param element       the OMElement to which the serialization should be attached
     * @param attributeName the attribute name to which the expression should be attached
     * @param node          the STNode which contains the expression and namespaces
     */
    public static void serializeExpression(String expression, OMElement element, String attributeName, STNode node) {

        OMNamespace nullNS = element.getOMFactory()
                .createOMNamespace(Constant.EMPTY_STRING, "");

        if (expression != null) {
            element.addAttribute(attributeName, expression, nullNS);
            serializeNamespaces(node, element);
        } else {
            throw new InvalidConfigurationException("Expression is not set for the node: " + node);
        }
    }

    public static void serializeNamespaces(STNode node, OMElement element) {

        HashMap<String, String> namespaces = node.getNamespaces();
        if (namespaces != null) {
            for (String prefix : namespaces.keySet()) {
                if (prefix.contains(":")) {
                    String uri = namespaces.get(prefix);
                    prefix = prefix.split(":")[1];
                    element.declareNamespace(uri, prefix);
                }
            }
        }
    }

    public static OMElement stringToOM(String text) {

        OMElement omElement = null;
        try {
            omElement = AXIOMUtil.stringToOM(text);
        } catch (XMLStreamException e) {
            //
        }
        return omElement;
    }

    public static OMText stringToCDATA(String text) {

        String content = removeCDATAFromPayload(text);
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMText cdata = fac.createOMText(content, 12); // 12 is the type for CDATA
        return cdata;
    }

    private static String removeCDATAFromPayload(String inputPayload) {

        if (inputPayload.startsWith("<![CDATA[")) {
            inputPayload = inputPayload.substring(9);
            int i = inputPayload.lastIndexOf("]]>");
            if (i == -1)
                throw new IllegalStateException("argument starts with <![CDATA[ but cannot find pairing ]]>");
            inputPayload = inputPayload.substring(0, i);
        }

        return inputPayload;
    }

    public static void serializeParameters(Parameter[] parameter, OMElement parameterElt) {

        if (parameter != null) {
            for (Parameter param : parameter) {
                OMElement paramElt = serializeParameter(param);
                if (paramElt != null) {
                    parameterElt.addChild(paramElt);
                }
            }
        }
    }

    private static OMElement serializeParameter(Parameter param) {

        if (param != null) {
            OMElement paramElt = OMAbstractFactory.getOMFactory().createOMElement("parameter",
                    Constant.SYNAPSE_OMNAMESPACE);
            if (param.getName() != null) {
                paramElt.addAttribute("name", param.getName(), null);
            }
            if (param.getKey() != null) {
                paramElt.addAttribute("key", param.getKey(), null);
            }
            if (param.isLocked()) {
                paramElt.addAttribute("locked", String.valueOf(param.isLocked()), null);
            }
            if (param.getContent() != null) {
                paramElt.setText(param.getContent());
            }
            return paramElt;
        }
        return null;
    }

    public static void serializeMediatorProperties(OMElement mediatorElt, MediatorProperty[] properties) {

        if (properties != null) {
            for (MediatorProperty property : properties) {
                OMElement propertyElt = serializeMediatorProperty(property);
                mediatorElt.addChild(propertyElt);
            }
        }
    }

    private static OMElement serializeMediatorProperty(MediatorProperty property) {

        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement propertyElt = fac.createOMElement("property", Constant.SYNAPSE_OMNAMESPACE);
        if (property.getName() != null) {
            propertyElt.addAttribute("name", property.getName(), null);
        }
        if (property.getValue() != null) {
            propertyElt.addAttribute("value", property.getValue(), null);
        } else if (property.getExpression() != null) {
            serializeExpression(property.getExpression(), propertyElt, "expression", property);
        }
        return propertyElt;
    }
}
