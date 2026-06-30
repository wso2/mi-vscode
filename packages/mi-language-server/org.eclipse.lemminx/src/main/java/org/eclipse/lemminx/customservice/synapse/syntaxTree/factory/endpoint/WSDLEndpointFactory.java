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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl11Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.misc.Wsdl20Factory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.EnableDisable;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Format;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Optimize;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11.TDefinitions;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20.DescriptionType;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class WSDLEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        WSDLEndpoint wsdlEndpoint = new WSDLEndpoint();
        wsdlEndpoint.elementNode(element);
        populateAttributes(wsdlEndpoint, element);
        List<DOMNode> children = element.getChildren();
        if (children != null & !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.WSDL_DEFINITIONS)) {
                    Wsdl11Factory factory = new Wsdl11Factory();
                    TDefinitions definitions = (TDefinitions) factory.create((DOMElement) child);
                    wsdlEndpoint.setDefinitions(definitions);
                } else if (name.equalsIgnoreCase(Constant.WSDL_DESCRIPTION)) {
                    Wsdl20Factory factory = new Wsdl20Factory();
                    DescriptionType description = (DescriptionType) factory.create((DOMElement) child);
                    wsdlEndpoint.setDescription(description);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_SEC)) {
                    EndpointEnableSec enableSec = EndpointUtils.createEnableSec(child);
                    wsdlEndpoint.setEnableSec(enableSec);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_RM)) {
                    EndpointEnableRM enableRM = EndpointUtils.createEnableRM(child);
                    wsdlEndpoint.setEnableRM(enableRM);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_ADDRESSING)) {
                    EndpointEnableAddressing enableAddressing = EndpointUtils.createEnableAddressing(child);
                    wsdlEndpoint.setEnableAddressing(enableAddressing);
                } else if (name.equalsIgnoreCase(Constant.TIMEOUT)) {
                    EndpointTimeout timeout = EndpointUtils.createTimeout(child);
                    wsdlEndpoint.setTimeout(timeout);
                } else if (name.equalsIgnoreCase(Constant.SUSPEND_ON_FAILURE)) {
                    EndpointSuspendOnFailure suspendOnFailure = EndpointUtils.createSuspendOnFailure(child);
                    wsdlEndpoint.setSuspendOnFailure(suspendOnFailure);
                } else if (name.equalsIgnoreCase(Constant.MARK_FOR_SUSPENSION)) {
                    EndpointMarkForSuspension markForSuspension = EndpointUtils.createMarkForSuspension(child);
                    wsdlEndpoint.setMarkForSuspension(markForSuspension);
                }
            }
        }
        return wsdlEndpoint;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String format = element.getAttribute(Constant.FORMAT);
        Format formatEnum = Utils.getEnumFromValue(format, Format.class);
        ((WSDLEndpoint) node).setFormat(formatEnum);
        if (formatEnum != null) {
            ((WSDLEndpoint) node).setFormat(formatEnum);
        }
        String optimize = element.getAttribute(Constant.OPTIMIZE);
        Optimize optimizeEnum = Utils.getEnumFromValue(optimize, Optimize.class);
        if (optimizeEnum != null) {
            ((WSDLEndpoint) node).setOptimize(optimizeEnum);
        }
        String encoding = element.getAttribute(Constant.ENCODING);
        if (encoding != null && !encoding.isEmpty()) {
            ((WSDLEndpoint) node).setEncoding(encoding);
        }
        String statistics = element.getAttribute(Constant.STATISTICS);
        EnableDisable statisticsEnum = Utils.getEnumFromValue(statistics, EnableDisable.class);
        if (statisticsEnum != null) {
            ((WSDLEndpoint) node).setStatistics(statisticsEnum);
        }
        String trace = element.getAttribute(Constant.TRACE);
        EnableDisable traceEnum = Utils.getEnumFromValue(trace, EnableDisable.class);
        if (traceEnum != null) {
            ((WSDLEndpoint) node).setTrace(traceEnum);
        }
        String uri = element.getAttribute(Constant.URI);
        if (uri != null && !uri.isEmpty()) {
            ((WSDLEndpoint) node).setUri(uri);
        }
        String service = element.getAttribute(Constant.SERVICE);
        if (service != null && !service.isEmpty()) {
            ((WSDLEndpoint) node).setService(service);
        }
        String port = element.getAttribute(Constant.PORT);
        if (port != null && !port.isEmpty()) {
            ((WSDLEndpoint) node).setPort(port);
        }
    }
}
