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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.AnyTopLevelOptionalElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TBinding;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TBindingOperation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TBindingOperationFault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TBindingOperationMessage;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TDefinitions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TDocumentation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TFault;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TImport;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TMessage;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TOperation;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TParam;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TPart;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TPort;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TPortType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TTypes;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Wsdl11Factory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        TDefinitions definitions = new TDefinitions();
        definitions.elementNode(element);
        populateAttributes(definitions, element);
        List<DOMNode> children = element.getChildren();
        List<Object> any = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            AnyTopLevelOptionalElement anyTopLevelOptionalElement = new AnyTopLevelOptionalElement();
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.WSDL_IMPORT)) {
                    TImport tImport = createTImport(child);
                    anyTopLevelOptionalElement.set_import(Optional.ofNullable(tImport));
                } else if (name.equalsIgnoreCase(Constant.WSDL_TYPES)) {
                    TTypes tTypes = createTTypes(child);
                    anyTopLevelOptionalElement.setTypes(Optional.ofNullable(tTypes));
                } else if (name.equalsIgnoreCase(Constant.WSDL_MESSAGE)) {
                    TMessage tMessage = createTMessage(child);
                    anyTopLevelOptionalElement.setMessage(Optional.ofNullable(tMessage));
                } else if (name.equalsIgnoreCase(Constant.WSDL_PORT_TYPE)) {
                    TPortType tPortType = createTPortType(child);
                    anyTopLevelOptionalElement.setPortType(Optional.ofNullable(tPortType));
                } else if (name.equalsIgnoreCase(Constant.WSDL_BINDING)) {
                    TBinding tBinding = createTBinding(child);
                    anyTopLevelOptionalElement.setBinding(Optional.ofNullable(tBinding));
                } else if (name.equalsIgnoreCase(Constant.WSDL_SERVICE)) {
                    TService tService = createTService(child);
                    anyTopLevelOptionalElement.setService(Optional.ofNullable(tService));
                } else if (name.equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    definitions.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    any.add(xml);
                }
            }
            definitions.setAnyTopLevelOptionalElement(anyTopLevelOptionalElement);
            definitions.setAny(any.toArray());
        }
        return definitions;
    }

    private TImport createTImport(DOMNode node) {

        TImport tImport = new TImport();
        tImport.elementNode((DOMElement) node);
        String namespace = node.getAttribute(Constant.NAMESPACE);
        if (namespace != null) {
            tImport.setNamespace(namespace);
        }
        String location = node.getAttribute(Constant.LOCATION);
        if (location != null) {
            tImport.setLocation(location);
        }
        // TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tImport.setDocumentation(tDocumentation);
                }
            }
        }

        return tImport;
    }

    private TTypes createTTypes(DOMNode node) {

        TTypes tTypes = new TTypes();
        tTypes.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            tTypes.setAny(elements.toArray(new String[elements.size()]));
        }
        return tTypes;
    }

    private TMessage createTMessage(DOMNode node) {

        TMessage tMessage = new TMessage();
        tMessage.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tMessage.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> tparts = new ArrayList<>();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name1 = child.getNodeName();
                if (name1.equalsIgnoreCase(Constant.WSDL_PART)) {
                    TPart part = createTPart(child);
                    tparts.add(part);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tMessage.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tMessage.setPart(tparts.toArray(new TPart[tparts.size()]));
            tMessage.setAny(elements.toArray(new String[elements.size()]));
        }
        return tMessage;
    }

    private TPart createTPart(DOMNode node) {

        TPart tPart = new TPart();
        tPart.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tPart.setName(name);
        }
        String element = node.getAttribute(Constant.ELEMENT);
        if (element != null) {
            tPart.setElement(element);
        }
        String type = node.getAttribute(Constant.TYPE);
        if (type != null) {
            tPart.setType(type);
        }
        //TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tPart.setDocumentation(tDocumentation);
                }
            }
        }

        return tPart;
    }

    private TPortType createTPortType(DOMNode node) {

        TPortType tPortType = new TPortType();
        tPortType.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tPortType.setName(name);
        }
        //TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        List<TOperation> operations = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_OPERATION)) {
                    TOperation tBindingOperation = createTOperation(child);
                    operations.add(tBindingOperation);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tPortType.setDocumentation(tDocumentation);
                }
            }
            tPortType.setOperation(operations.toArray(new TOperation[operations.size()]));
        }
        return tPortType;
    }

    private TOperation createTOperation(DOMNode node) {

        TOperation tOperation = new TOperation();
        tOperation.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tOperation.setName(name);
        }
        String parameterOrder = node.getAttribute(Constant.PARAMETER_ORDER);
        if (parameterOrder != null) {
            tOperation.setParameterOrder(parameterOrder);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String nodeName = child.getNodeName();
                if (nodeName.equalsIgnoreCase(Constant.WSDL_INPUT)) {
                    TParam input = createTParam(child);
                    elements.add(input);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_OUTPUT)) {
                    TParam output = createTParam(child);
                    elements.add(output);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_FAULT)) {
                    TFault fault = createTFault(child);
                    elements.add(fault);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tOperation.setDocumentation(tDocumentation);
                }
            }
            tOperation.setRest(elements);
        }
        return tOperation;
    }

    private TParam createTParam(DOMNode node) {

        TParam tParam = new TParam();
        tParam.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tParam.setName(name);
        }
        String message = node.getAttribute(Constant.MESSAGE);
        if (message != null) {
            tParam.setMessage(message);
        }
        //TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tParam.setDocumentation(tDocumentation);
                }
            }
        }
        return tParam;
    }

    private TFault createTFault(DOMNode node) {

        TFault tFault = new TFault();
        tFault.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tFault.setName(name);
        }
        String message = node.getAttribute(Constant.MESSAGE);
        if (message != null) {
            tFault.setMessage(message);
        }
        //TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tFault.setDocumentation(tDocumentation);
                }
            }
        }
        return tFault;
    }

    private TBinding createTBinding(DOMNode node) {

        TBinding tBinding = new TBinding();
        tBinding.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tBinding.setName(name);
        }
        String type = node.getAttribute(Constant.TYPE);
        if (type != null) {
            tBinding.setType(type);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        List<TBindingOperation> operations = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_OPERATION)) {
                    TBindingOperation tBindingOperation = createTBindingOperation(child);
                    operations.add(tBindingOperation);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tBinding.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tBinding.setOperation(operations.toArray(new TBindingOperation[elements.size()]));
            tBinding.setAny(elements.toArray());
        }
        return tBinding;
    }

    private TDocumentation createTDocumentation(DOMNode node) {

        TDocumentation tDocumentation = new TDocumentation();
        tDocumentation.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            tDocumentation.setContent(elements.toArray(new String[elements.size()]));
        }
        return tDocumentation;
    }

    private TBindingOperation createTBindingOperation(DOMNode node) {

        TBindingOperation tBindingOperation = new TBindingOperation();
        tBindingOperation.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tBindingOperation.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> faults = new ArrayList<>();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String nodeName = child.getNodeName();
                if (nodeName.equalsIgnoreCase(Constant.WSDL_INPUT)) {
                    TBindingOperationMessage tBindingOperationMessage = createTBindingOperationMessage(child);
                    tBindingOperation.setInput(tBindingOperationMessage);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_OUTPUT)) {
                    TBindingOperationMessage tBindingOperationMessage = createTBindingOperationMessage(child);
                    tBindingOperation.setOutput(tBindingOperationMessage);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_FAULT)) {
                    TBindingOperationFault tBindingOperationFault = createTBindingOperationFault(child);
                    faults.add(tBindingOperationFault);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tBindingOperation.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tBindingOperation.setFault(faults.toArray(new TBindingOperationFault[faults.size()]));
            tBindingOperation.setAny(elements.toArray());
        }
        return tBindingOperation;
    }

    private TBindingOperationMessage createTBindingOperationMessage(DOMNode node) {

        TBindingOperationMessage tBindingOperationMessage = new TBindingOperationMessage();
        tBindingOperationMessage.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tBindingOperationMessage.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tBindingOperationMessage.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tBindingOperationMessage.setAny(elements.toArray());
        }
        return tBindingOperationMessage;
    }

    private TBindingOperationFault createTBindingOperationFault(DOMNode node) {

        TBindingOperationFault tBindingOperationFault = new TBindingOperationFault();
        tBindingOperationFault.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tBindingOperationFault.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tBindingOperationFault.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tBindingOperationFault.setAny(elements.toArray());
        }
        return tBindingOperationFault;
    }

    private TService createTService(DOMNode node) {

        TService tService = new TService();
        tService.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tService.setName(name);
        }
        List<DOMNode> children = node.getChildren();
        List<TPort> tports = new ArrayList<>();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String nodeName = child.getNodeName();
                if (nodeName.equalsIgnoreCase(Constant.WSDL_PORT)) {
                    TPort tPort = createTPort(child);
                    tports.add(tPort);
                } else if (nodeName.equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tService.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tService.setPort(tports.toArray(new TPort[tports.size()]));
            tService.setAny(elements.toArray());
        }
        return tService;
    }

    private TPort createTPort(DOMNode node) {

        TPort tPort = new TPort();
        tPort.elementNode((DOMElement) node);
        String name = node.getAttribute(Constant.NAME);
        if (name != null) {
            tPort.setName(name);
        }
        String binding = node.getAttribute(Constant.BINDING);
        if (binding != null) {
            tPort.setBinding(binding);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.WSDL_DOCUMENTATION)) {
                    TDocumentation tDocumentation = createTDocumentation(child);
                    tPort.setDocumentation(tDocumentation);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            tPort.setAny(elements.toArray());
        }
        return tPort;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String targetNamespace = element.getAttribute(Constant.TARGET_NAMESPACE);
        if (targetNamespace != null) {
            ((TDefinitions) node).setTargetNamespace(targetNamespace);
        }
        String name = element.getAttribute(Constant.NAME);
        if (name != null) {
            ((TDefinitions) node).setName(name);
        }
    }

    private String getAnyElement(DOMNode node) {
        //TODO: check whether need any other implementation for xs:any
        String xml = Utils.getInlineString(node, Boolean.FALSE);
        return xml;
    }
}
