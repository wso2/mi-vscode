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

package org.eclipse.lemminx.customservice.synapse.mediatorService.mediators;

import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.callout.*;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils.transformNamespaces;

public class CalloutMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Callout callout,
                                                                                              List<String> dirtyFields) {
        boolean xpathPayload = "XPATH".equals(data.get("payloadType"));
        boolean propertyPayload = "PROPERTY".equals(data.get("payloadType"));
        boolean envelopePayload = "ENVELOPE".equals(data.get("payloadType"));
        boolean xpathTarget = "XPATH".equals(data.get("resultType"));
        boolean propertyTarget = "PROPERTY".equals(data.get("resultType"));

        String targetProperty = data.get("resultContextProperty") != null ? (String) data.get("resultContextProperty") : "";
        boolean securityEnabled = "TRUE".equals(data.get("securityType"));
        boolean configurationEnabled = data.get("pathToAxis2Repository") != null || data.get("pathToAxis2Xml") != null;
        boolean policies = "TRUE".equals(data.get("policies"));

        if (xpathTarget) {
            data.put("targetMessageXPath", data.get("resultMessageXPath"));
        }

        data.putAll(Map.of(
                "xpathPayload", xpathPayload,
                "propertyPayload", propertyPayload,
                "envelopePayload", envelopePayload,
                "xpathTarget", xpathTarget,
                "propertyTarget", propertyTarget,
                "securityEnabled", securityEnabled,
                "configurationEnabled", configurationEnabled,
                "targetProperty", targetProperty,
                "policies", policies
        ));

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        return Either.forLeft(data);
    }


    public static Map<String, Object> getDataFromST430(Callout node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        if (node.getServiceURL() != null) {
            data.put("endpointType", "URL");
            data.put("serviceURL", node.getServiceURL());
        }
        if (node.getEndpointKey() != null) {
            data.put("endpointType", "AddressEndpoint");
            data.put("addressEndpoint", node.getEndpointKey());
        }
        if (node.getAction() != null) {
            data.put("soapAction", node.getAction());
        }

        SourceOrTargetOrConfiguration sourceOrTargetOrConfiguration = node.getSourceOrTargetOrConfiguration();
        if (sourceOrTargetOrConfiguration != null) {

            if (sourceOrTargetOrConfiguration.getConfiguration().isPresent()) {
                CalloutConfiguration calloutConfiguration = sourceOrTargetOrConfiguration.getConfiguration().get();
                if (calloutConfiguration.getRepository() != null) {
                    data.put("pathToAxis2Repository", calloutConfiguration.getRepository());
                }
                if (calloutConfiguration.getAxis2Xml() != null) {
                    data.put("pathToAxis2xml", calloutConfiguration.getAxis2Xml());
                }
            }

            if (sourceOrTargetOrConfiguration.getSource().isPresent()) {
                CalloutSource source = sourceOrTargetOrConfiguration.getSource().get();
                if (source.getKey() != null) {
                    data.put("payloadType", "PROPERTY");
                    data.put("payloadProperty", source.getKey());
                } else if (source.getXpath() != null) {
                    data.put("payloadType", "XPATH");
                    data.put("payloadMessageXPath", Map.of(
                            "isExpression", true,
                            "value", source.getXpath(),
                            "namespaces", MediatorUtils.transformNamespaces(source.getNamespaces())
                    ));
                } else {
                    data.put("payloadType", "ENVELOPE");
                }
            }

            if (sourceOrTargetOrConfiguration.getTarget().isPresent()) {
                CalloutTarget target = sourceOrTargetOrConfiguration.getTarget().get();
                if (target.getKey() != null) {
                    data.put("resultType", "PROPERTY");
                    data.put("resultContextProperty", target.getKey());
                } else if (target.getXpath() != null) {
                    data.put("resultType", "XPATH");
                    data.put("resultMessageXPath", Map.of(
                            "isExpression", true,
                            "value", target.getXpath(),
                            "namespaces", transformNamespaces(target.getNamespaces())
                    ));
                }
            }

            data.put("securityType", "FALSE");
            if (sourceOrTargetOrConfiguration.getEnableSec().isPresent()) {
                CalloutEnableSec enableSec = sourceOrTargetOrConfiguration.getEnableSec().get();
                data.put("securityType", "TRUE");
                if (enableSec.getPolicy() != null) {
                    data.put("policies", "FALSE");
                    data.put("policyKey", enableSec.getPolicy());
                } else {
                    data.put("policies", "TRUE");
                    if (enableSec.getInboundPolicy() != null) {
                        data.put("inboundPolicyKey", enableSec.getInboundPolicy());
                    }
                    if (enableSec.getOutboundPolicy() != null) {
                        data.put("outboundPolicyKey", enableSec.getOutboundPolicy());
                    }
                }
            }
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
