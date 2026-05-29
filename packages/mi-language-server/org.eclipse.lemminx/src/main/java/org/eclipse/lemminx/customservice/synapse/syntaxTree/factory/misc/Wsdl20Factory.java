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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.BindingFaultType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.BindingOperationFaultType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.BindingOperationMessageType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.BindingOperationType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.BindingType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.DescriptionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.DocumentationType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.EndpointType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.ImportType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.IncludeType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.InterfaceFaultType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.InterfaceOperationType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.InterfaceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.MessageRefFaultType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.MessageRefType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.ServiceType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.TypesType;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;

public class Wsdl20Factory extends AbstractFactory {

    //TODO: This one is not supported currently in MI. Need to check the implementation.
    @Override
    public STNode create(DOMElement element) {

        DescriptionType description = new DescriptionType();
        description.elementNode(element);
        populateAttributes(description, element);
        List<DOMNode> children = element.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase("import")) {
                    ImportType importType = createImportType(child);
                    elements.add(importType);
                } else if (name.equalsIgnoreCase("include")) {
                    IncludeType includeType = createIncludeType(child);
                    elements.add(includeType);
                } else if (name.equalsIgnoreCase("types")) {
                    TypesType typesType = createTypesType(child);
                    elements.add(typesType);
                } else if (name.equalsIgnoreCase("interface")) {
                    InterfaceType interfaceType = createInterfaceType(child);
                    elements.add(interfaceType);
                } else if (name.equalsIgnoreCase("binding")) {
                    BindingType bindingType = createBindingType(child);
                    elements.add(bindingType);
                } else if (name.equalsIgnoreCase("service")) {
                    ServiceType serviceType = createServiceType(child);
                    elements.add(serviceType);
                } else if (name.equalsIgnoreCase("documentation")) {
                    DocumentationType documentationType = createDocumentationType(child);
                    elements.add(documentationType);
                }
            }
            description.setImportOrIncludeOrTypes(elements);
        }
        return description;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String targetNamespace = element.getAttribute("targetNamespace");
        if (targetNamespace != null) {
            ((DescriptionType) node).setTargetNamespace(targetNamespace);
        }
    }

    private ImportType createImportType(DOMNode child) {

        ImportType importType = new ImportType();
        importType.elementNode((DOMElement) child);
        String namespace = child.getAttribute("namespace");
        if (namespace != null) {
            importType.setNamespace(namespace);
        }
        String location = child.getAttribute("location");
        if (location != null) {
            importType.setLocation(location);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String xml = getAnyElement(node);
                elements.add(xml);
            }
            importType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return importType;
    }

    private IncludeType createIncludeType(DOMNode child) {

        IncludeType includeType = new IncludeType();
        includeType.elementNode((DOMElement) child);
        String location = child.getAttribute("location");
        if (location != null) {
            includeType.setLocation(location);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String xml = getAnyElement(node);
                elements.add(xml);
            }
            includeType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return includeType;
    }

    private TypesType createTypesType(DOMNode child) {

        TypesType typesType = new TypesType();
        typesType.elementNode((DOMElement) child);
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String xml = getAnyElement(node);
                elements.add(xml);
            }
            typesType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return typesType;
    }

    private InterfaceType createInterfaceType(DOMNode child) {

        InterfaceType interfaceType = new InterfaceType();
        interfaceType.elementNode((DOMElement) child);
        String name = child.getAttribute("name");
        if (name != null) {
            interfaceType.setName(name);
        }
        String _extends = child.getAttribute("extends");
        if (_extends != null) {
            interfaceType.set_extends(_extends);
        }
        String styleDefault = child.getAttribute("styleDefault");
        if (styleDefault != null) {
            interfaceType.setStyleDefault(styleDefault);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name1 = node.getNodeName();
                if (name1.equalsIgnoreCase("operation")) {
                    InterfaceOperationType interfaceOperationType = createInterfaceOperationType(node);
                    elements.add(interfaceOperationType);
                } else if (name1.equalsIgnoreCase("fault")) {
                    InterfaceFaultType interfaceFaultType = createInterfaceFaultType(node);
                    elements.add(interfaceFaultType);
                }
            }
            interfaceType.setOperationOrFaultOrAny(elements);
        }
        return interfaceType;
    }

    private InterfaceOperationType createInterfaceOperationType(DOMNode node) {

        InterfaceOperationType interfaceOperationType = new InterfaceOperationType();
        interfaceOperationType.elementNode((DOMElement) node);
        String name = node.getAttribute("name");
        if (name != null) {
            interfaceOperationType.setName(name);
        }
        String pattern = node.getAttribute("pattern");
        if (pattern != null) {
            interfaceOperationType.setPattern(pattern);
        }
        String safe = node.getAttribute("safe");
        if (safe != null) {
            interfaceOperationType.setSafe(Boolean.parseBoolean(safe));
        }
        String style = node.getAttribute("style");
        if (style != null) {
            interfaceOperationType.setStyle(style);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name1 = child.getNodeName();
                if (name1.equalsIgnoreCase("input")) {
                    MessageRefType input = createMessageRefType(child);
                    elements.add(input);
                } else if (name1.equalsIgnoreCase("output")) {
                    MessageRefType output = createMessageRefType(child);
                    elements.add(output);
                } else if (name1.equalsIgnoreCase("infault")) {
                    MessageRefFaultType infault = createMessageRefFaultType(child);
                    elements.add(infault);
                } else if (name1.equalsIgnoreCase("outfault")) {
                    MessageRefFaultType outfault = createMessageRefFaultType(child);
                    elements.add(outfault);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            interfaceOperationType.setInputOrOutputOrInfault(elements);
        }
        return interfaceOperationType;
    }

    private MessageRefType createMessageRefType(DOMNode child) {

        MessageRefType messageRefType = new MessageRefType();
        messageRefType.elementNode((DOMElement) child);
        String messageLabel = child.getAttribute("messageLabel");
        if (messageLabel != null) {
            messageRefType.setMessageLabel(messageLabel);
        }
        String element = child.getAttribute("element");
        if (element != null) {
            messageRefType.setElement(element);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String xml = getAnyElement(node);
                elements.add(xml);
            }
            messageRefType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return messageRefType;
    }

    private MessageRefFaultType createMessageRefFaultType(DOMNode child) {

        MessageRefFaultType messageRefFaultType = new MessageRefFaultType();
        messageRefFaultType.elementNode((DOMElement) child);
        String ref = child.getAttribute("ref");
        if (ref != null) {
            messageRefFaultType.setRef(ref);
        }
        String messageLabel = child.getAttribute("messageLabel");
        if (messageLabel != null) {
            messageRefFaultType.setMessageLabel(messageLabel);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String xml = getAnyElement(node);
                elements.add(xml);
            }
            messageRefFaultType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return messageRefFaultType;
    }

    private InterfaceFaultType createInterfaceFaultType(DOMNode node) {

        InterfaceFaultType interfaceFaultType = new InterfaceFaultType();
        interfaceFaultType.elementNode((DOMElement) node);
        String name = node.getAttribute("name");
        if (name != null) {
            interfaceFaultType.setName(name);
        }
        String element = node.getAttribute("element");
        if (element != null) {
            interfaceFaultType.setElement(element);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            interfaceFaultType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return interfaceFaultType;
    }

    private BindingType createBindingType(DOMNode child) {

        BindingType bindingType = new BindingType();
        bindingType.elementNode((DOMElement) child);
        String name = child.getAttribute("name");
        if (name != null) {
            bindingType.setName(name);
        }
        String type = child.getAttribute("type");
        if (type != null) {
            bindingType.setType(type);
        }
        String _interface = child.getAttribute("interface");
        if (_interface != null) {
            bindingType.set_interface(_interface);
        }
        List<DOMNode> children = child.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name1 = node.getNodeName();
                if (name1.equalsIgnoreCase("operation")) {
                    BindingOperationType bindingOperationType = createBindingOperationType(node);
                    elements.add(bindingOperationType);
                } else if (name1.equalsIgnoreCase("fault")) {
                    BindingFaultType bindingFaultType = createBindingFaultType(node);
                    elements.add(bindingFaultType);
                }
            }
            bindingType.setOperationOrFaultOrAny(elements);
        }
        return bindingType;
    }

    private BindingOperationType createBindingOperationType(DOMNode node) {

        BindingOperationType bindingOperationType = new BindingOperationType();
        bindingOperationType.elementNode((DOMElement) node);
        String ref = node.getAttribute("ref");
        if (ref != null) {
            bindingOperationType.setRef(ref);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase("input")) {
                    BindingOperationMessageType input = createBindingOperationMessageType(child);
                    elements.add(input);
                } else if (name.equalsIgnoreCase("output")) {
                    BindingOperationMessageType output = createBindingOperationMessageType(child);
                    elements.add(output);
                } else if (name.equalsIgnoreCase("infault")) {
                    BindingOperationFaultType infault = createBindingOperationFaultType(child);
                    elements.add(infault);
                } else if (name.equalsIgnoreCase("outfault")) {
                    BindingOperationFaultType outfault = createBindingOperationFaultType(child);
                    elements.add(outfault);
                } else {
                    String xml = getAnyElement(child);
                    elements.add(xml);
                }
            }
            bindingOperationType.setInputOrOutputOrInfault(elements);
        }
        return bindingOperationType;
    }

    private BindingOperationMessageType createBindingOperationMessageType(DOMNode node) {

        BindingOperationMessageType bindingOperationMessageType = new BindingOperationMessageType();
        bindingOperationMessageType.elementNode((DOMElement) node);
        String messageLabel = node.getAttribute("messageLabel");
        if (messageLabel != null) {
            bindingOperationMessageType.setMessageLabel(messageLabel);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            bindingOperationMessageType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return bindingOperationMessageType;
    }

    private BindingOperationFaultType createBindingOperationFaultType(DOMNode node) {

        BindingOperationFaultType bindingOperationFaultType = new BindingOperationFaultType();
        bindingOperationFaultType.elementNode((DOMElement) node);
        String ref = node.getAttribute("ref");
        if (ref != null) {
            bindingOperationFaultType.setRef(ref);
        }
        String messageLabel = node.getAttribute("messageLabel");
        if (messageLabel != null) {
            bindingOperationFaultType.setMessageLabel(messageLabel);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            bindingOperationFaultType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return bindingOperationFaultType;
    }

    private BindingFaultType createBindingFaultType(DOMNode node) {

        BindingFaultType bindingFaultType = new BindingFaultType();
        bindingFaultType.elementNode((DOMElement) node);
        String ref = node.getAttribute("ref");
        if (ref != null) {
            bindingFaultType.setRef(ref);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            bindingFaultType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return bindingFaultType;
    }

    private ServiceType createServiceType(DOMNode node) {

        ServiceType serviceType = new ServiceType();
        serviceType.elementNode((DOMElement) node);
        String name = node.getAttribute("name");
        if (name != null) {
            serviceType.setName(name);
        }
        String _interface = node.getAttribute("interface");
        if (_interface != null) {
            serviceType.set_interface(_interface);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name1 = child.getNodeName();
                if (name1.equalsIgnoreCase("endpoint")) {
                    EndpointType endpointType = createEndpointType(child);
                    elements.add(endpointType);
                }
            }
            serviceType.setEndpointOrAny(elements.toArray(new EndpointType[elements.size()]));
        }
        return serviceType;
    }

    private EndpointType createEndpointType(DOMNode node) {

        EndpointType endpointType = new EndpointType();
        endpointType.elementNode((DOMElement) node);
        String name = node.getAttribute("name");
        if (name != null) {
            endpointType.setName(name);
        }
        String binding = node.getAttribute("binding");
        if (binding != null) {
            endpointType.setBinding(binding);
        }
        String address = node.getAttribute("address");
        if (address != null) {
            endpointType.setAddress(address);
        }
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            endpointType.setAny(elements.toArray(new Object[elements.size()]));
        }
        return endpointType;
    }

    private DocumentationType createDocumentationType(DOMNode node) {

        DocumentationType documentationType = new DocumentationType();
        documentationType.elementNode((DOMElement) node);
        //TODO: handle otherAttributes
        List<DOMNode> children = node.getChildren();
        List<Object> elements = new ArrayList<>();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String xml = getAnyElement(child);
                elements.add(xml);
            }
            documentationType.setContent(elements.toArray(new Object[elements.size()]));
        }
        return documentationType;
    }

    private String getAnyElement(DOMNode node) {
        //TODO: check whether need any other implementation for xs:any
        String xml = Utils.getInlineString(node, Boolean.FALSE);
        return xml;
    }
}
