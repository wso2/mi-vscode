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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.Callout;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.CalloutConfiguration;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.CalloutEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.CalloutSource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.CalloutTarget;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.SourceOrTargetOrConfiguration;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;
import java.util.Optional;

public class CallOutFactory extends AbstractMediatorFactory {

    private static final String CALL_OUT = "CallOut";

    @Override
    protected Mediator createSpecificMediator(DOMElement element) {

        Callout callOut = new Callout();
        callOut.elementNode(element);
        populateAttributes(callOut, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            SourceOrTargetOrConfiguration sourceOrTargetOrConfiguration = new SourceOrTargetOrConfiguration();
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.SOURCE)) {
                    CalloutSource source = createSource(child);
                    sourceOrTargetOrConfiguration.setSource(Optional.ofNullable(source));
                } else if (name.equalsIgnoreCase(Constant.TARGET)) {
                    CalloutTarget target = createTarget(child);
                    sourceOrTargetOrConfiguration.setTarget(Optional.ofNullable(target));
                } else if (name.equalsIgnoreCase(Constant.ENABLE_SEC)) {
                    CalloutEnableSec enableSec = createEnableSec(child);
                    sourceOrTargetOrConfiguration.setEnableSec(Optional.ofNullable(enableSec));
                } else if (name.equalsIgnoreCase(Constant.CONFIGURATION)) {
                    CalloutConfiguration configuration = createConfiguration(child);
                    sourceOrTargetOrConfiguration.setConfiguration(Optional.ofNullable(configuration));
                }
            }
            callOut.setSourceOrTargetOrConfiguration(sourceOrTargetOrConfiguration);
        }
        return callOut;
    }

    private CalloutSource createSource(DOMNode child) {

        CalloutSource source = new CalloutSource();
        source.elementNode((DOMElement) child);
        String xpath = child.getAttribute(Constant.XPATH);
        if (xpath != null) {
            source.setXpath(xpath);
        }
        String key = child.getAttribute(Constant.KEY);
        if (key != null) {
            source.setKey(key);
        }
        return source;
    }

    private CalloutTarget createTarget(DOMNode child) {

        CalloutTarget target = new CalloutTarget();
        target.elementNode((DOMElement) child);
        String xpath = child.getAttribute(Constant.XPATH);
        if (xpath != null) {
            target.setXpath(xpath);
        }
        String key = child.getAttribute(Constant.KEY);
        if (key != null) {
            target.setKey(key);
        }
        return target;
    }

    private CalloutEnableSec createEnableSec(DOMNode child) {

        CalloutEnableSec enableSec = new CalloutEnableSec();
        enableSec.elementNode((DOMElement) child);
        String policy = child.getAttribute(Constant.POLICY);
        if (policy != null) {
            enableSec.setPolicy(policy);
        }
        String outboundPolicy = child.getAttribute(Constant.OUTBOUND_POLICY);
        if (outboundPolicy != null) {
            enableSec.setOutboundPolicy(outboundPolicy);
        }
        String inboundPolicy = child.getAttribute(Constant.INBOUND_POLICY);
        if (inboundPolicy != null) {
            enableSec.setInboundPolicy(inboundPolicy);
        }
        return enableSec;
    }

    private CalloutConfiguration createConfiguration(DOMNode child) {

        CalloutConfiguration configuration = new CalloutConfiguration();
        configuration.elementNode((DOMElement) child);
        String axis2xml = child.getAttribute(Constant.AXIS2XML);
        if (axis2xml != null) {
            configuration.setAxis2Xml(axis2xml);
        }
        String repository = child.getAttribute(Constant.REPOSITORY);
        if (repository != null) {
            configuration.setRepository(repository);
        }
        return configuration;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String serviceURL = element.getAttribute(Constant.SERVICE_URL);
        if (serviceURL != null) {
            ((Callout) node).setServiceURL(serviceURL);
        }
        String action = element.getAttribute(Constant.ACTION);
        if (action != null) {
            ((Callout) node).setAction(action);
        }
        String initAxis2ClientOptions = element.getAttribute(Constant.INIT_AXIS2_CLIENT_OPTIONS);
        if (initAxis2ClientOptions != null) {
            ((Callout) node).setInitAxis2ClientOptions(Boolean.valueOf(initAxis2ClientOptions));
        }
        String endpointKey = element.getAttribute(Constant.ENDPOINT_KEY);
        if (endpointKey != null) {
            ((Callout) node).setEndpointKey(endpointKey);
        }
        String description = element.getAttribute(Constant.DESCRIPTION);
        if (description != null) {
            ((Callout) node).setDescription(description);
        }
        String traceFilter = element.getAttribute(Constant.TRACE_FILTER);
        if (traceFilter != null) {
            ((Callout) node).setTraceFilter(traceFilter);
        }
    }

    @Override
    public String getTagName() {

        return CALL_OUT;
    }

}
