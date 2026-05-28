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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.DefaultEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Format;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.Optimize;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;

public class DefaultEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        DefaultEndpoint defaultEndpoint = new DefaultEndpoint();
        defaultEndpoint.elementNode(element);
        populateAttributes(defaultEndpoint, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equalsIgnoreCase(Constant.ENABLE_SEC)) {
                    EndpointEnableSec enableSec = EndpointUtils.createEnableSec(node);
                    defaultEndpoint.setEnableSec(enableSec);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_RM)) {
                    EndpointEnableRM enableRM = EndpointUtils.createEnableRM(node);
                    defaultEndpoint.setEnableRM(enableRM);
                } else if (name.equalsIgnoreCase(Constant.ENABLE_ADDRESSING)) {
                    EndpointEnableAddressing enableAddressing = EndpointUtils.createEnableAddressing(node);
                    defaultEndpoint.setEnableAddressing(enableAddressing);
                } else if (name.equalsIgnoreCase(Constant.TIMEOUT)) {
                    EndpointTimeout timeout = EndpointUtils.createTimeout(node);
                    defaultEndpoint.setTimeout(timeout);
                } else if (name.equalsIgnoreCase(Constant.SUSPEND_ON_FAILURE)) {
                    EndpointSuspendOnFailure suspendOnFailure = EndpointUtils.createSuspendOnFailure(node);
                    defaultEndpoint.setSuspendOnFailure(suspendOnFailure);
                } else if (name.equalsIgnoreCase(Constant.MARK_FOR_SUSPENSION)) {
                    EndpointMarkForSuspension markForSuspension = EndpointUtils.createMarkForSuspension(node);
                    defaultEndpoint.setMarkForSuspension(markForSuspension);
                }
            }
        }
        return defaultEndpoint;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String format = element.getAttribute(Constant.FORMAT);
        Format formatEnum = Utils.getEnumFromValue(format, Format.class);
        if (formatEnum != null) {
            ((DefaultEndpoint) node).setFormat(formatEnum);
        }
        String optimize = element.getAttribute(Constant.OPTIMIZE);
        Optimize optimizeEnum = Utils.getEnumFromValue(optimize, Optimize.class);
        if (optimizeEnum != null) {
            ((DefaultEndpoint) node).setOptimize(optimizeEnum);
        }
        String encoding = element.getAttribute(Constant.ENCODING);
        if (encoding != null && !encoding.isEmpty()) {
            ((DefaultEndpoint) node).setEncoding(encoding);
        }
        String statistics = element.getAttribute(Constant.STATISTICS);
        if (statistics != null && !statistics.isEmpty()) {
            ((DefaultEndpoint) node).setStatistics(statistics);
        }
        String trace = element.getAttribute(Constant.TRACE);
        if (trace != null && !trace.isEmpty()) {
            ((DefaultEndpoint) node).setTrace(trace);
        }
    }
}
