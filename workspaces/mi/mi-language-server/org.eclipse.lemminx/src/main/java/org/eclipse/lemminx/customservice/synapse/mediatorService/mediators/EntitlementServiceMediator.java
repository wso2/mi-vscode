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
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.TagRanges;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementService;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.entitlement.EntitlementServiceClient;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitlementServiceMediator {
    private static final List<String> entitlementTagAttributes =
            List.of("callbackClassName", "client", "password", "entitlementServerURL", "username",
                    "thriftHost", "thriftPort", "onAcceptSequenceKey", "onRejectSequenceKey", "obligationsSequenceKey",
                    "adviceSequenceKey", "onAcceptSequenceType", "onRejectSequenceType", "obligationsSequenceType",
                    "adviceSequenceType", "description", "callbackHandler", "entitlementClientType");
    private static final List<String> onAcceptTagAttributes = List.of("onAcceptSequenceType");
    private static final List<String> onRejectTagAttributes = List.of("onRejectSequenceType");
    private static final List<String> adviceTagAttributes = List.of("adviceSequenceType");
    private static final List<String> obligationTagAttributes = List.of("obligationsSequenceType");

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              EntitlementService entitlementService,
                                                                                              List<String> dirtyFields) {
        String client = data.get("entitlementClientType").toString();
        switch (client) {
            case "SOAP - Basic Auth (WSO2 IS 4.0.0 or later)":
                data.put("client", "basicAuth");
                break;
            case "THRIFT":
                data.put("client", "thrift");
                break;
            case "SOAP - Authentication Admin (WSO2 IS 3.2.3 or earlier)":
                data.put("client", "soap");
                break;
            case "WSXACML":
                data.put("client", "wsXacml");
                break;
        }

        String callbackHandler = data.get("callbackHandler").toString();
        Object callbackClassName = data.get("callbackClassName");
        switch (callbackHandler) {
            case "UT":
                data.put("callbackClassName",
                        "org.wso2.carbon.identity.entitlement.mediator.callback.UTEntitlementCallbackHandler");
                break;
            case "X509":
                data.put("callbackClassName",
                        "org.wso2.carbon.identity.entitlement.mediator.callback.X509EntitlementCallbackHandler");
                break;
            case "SAML":
                data.put("callbackClassName",
                        "org.wso2.carbon.identity.entitlement.mediator.callback.SAMLEntitlementCallbackHandler");
                break;
            case "Kerberos":
                data.put("callbackClassName",
                        "org.wso2.carbon.identity.entitlement.mediator.callback.KerberosEntitlementCallbackHandler");
                break;
            case "Custom":
                data.put("callbackClassName", callbackClassName);
                break;
        }

        if (data.get("onAcceptSequenceType").equals("ANONYMOUS")) {
            data.remove("onAcceptSequenceKey");
        }
        if (data.get("onRejectSequenceType").equals("ANONYMOUS")) {
            data.remove("onRejectSequenceKey");
        }
        if (data.get("obligationsSequenceType").equals("ANONYMOUS")) {
            data.remove("obligationsSequenceKey");
        }
        if (data.get("adviceSequenceType").equals("ANONYMOUS")) {
            data.remove("adviceSequenceKey");
        }

        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }

        if (entitlementService == null) {
            data.put("newMediator", true);
            return Either.forLeft(data);
        }
        return Either.forRight(getEdits(data, entitlementService, dirtyFields));

    }

    public static Map<Range, Map<String, Object>> getEdits(Map<String, Object> data, EntitlementService entitlementService,
                                                           List<String> dirtyFields) {
        Map<Range, Map<String, Object>> edits = new HashMap<>();

        if (MediatorUtils.anyMatch(dirtyFields, entitlementTagAttributes)) {
            getEdit("edit", "entitlement", data, entitlementService, true, edits);
        }

        if (MediatorUtils.anyMatch(dirtyFields, onAcceptTagAttributes)) {
            Map<String, Object> onAcceptData = new HashMap<>(data);
            setEditSeqData(onAcceptData, entitlementService, "onAccept");
            if (onAcceptData.containsKey("needEdit") && (Boolean) onAcceptData.get("needEdit")) {
                getEdit("add", "onAccept", onAcceptData, entitlementService, false, edits);
            }
        }

        if (MediatorUtils.anyMatch(dirtyFields, onRejectTagAttributes)) {
            Map<String, Object> onRejectData = new HashMap<>(data);
            setEditSeqData(onRejectData, entitlementService, "onReject");
            if (onRejectData.containsKey("needEdit") && (Boolean) onRejectData.get("needEdit")) {
                getEdit("add", "onReject", onRejectData, entitlementService, false, edits);
            }
        }

        if (MediatorUtils.anyMatch(dirtyFields, adviceTagAttributes)) {
            Map<String, Object> adviceData = new HashMap<>(data);
            setEditSeqData(adviceData, entitlementService, "advice");
            if (adviceData.containsKey("needEdit") && (Boolean) adviceData.get("needEdit")) {
                getEdit("add", "advice", adviceData, entitlementService, false, edits);
            }
        }

        if (MediatorUtils.anyMatch(dirtyFields, obligationTagAttributes)) {
            Map<String, Object> obligationData = new HashMap<>(data);
            setEditSeqData(obligationData, entitlementService, "obligations");
            if (obligationData.containsKey("needEdit") && (Boolean) obligationData.get("needEdit")) {
                getEdit("add", "obligation", obligationData, entitlementService, false, edits);
            }
        }

        return edits;
    }

    private static void setEditSeqData(
            Map<String, Object> data,
            EntitlementService entitlementService,
            String key) {

        String attribute = getAttribute(entitlementService, key);
        Object sequenceType = data.get(key + "SequenceType");

        if ("REGISTRY_REFERENCE".equals(attribute) && "ANONYMOUS".equals(sequenceType)) {
            if (isAttributeValueEmpty(entitlementService, key)) {
                data.put("needEdit", true);
                data.put("addKey" + capitalizeFirstLetter(key), true);
            }
        } else if ("ANONYMOUS".equals(attribute) && "REGISTRY_REFERENCE".equals(sequenceType)) {
            data.put("needEdit", true);
            data.put("removeSequence", true);
        }
    }

    private static String getAttribute(EntitlementService entitlementService, String attributeType) {
        switch (attributeType) {
            case "onAccept":
                return entitlementService.getOnAcceptAttribute();
            case "onReject":
                return entitlementService.getOnRejectAttribute();
            case "advice":
                return entitlementService.getAdviceAttribute();
            case "obligations":
                return entitlementService.getObligationsAttribute();
            default:
                throw new IllegalArgumentException("Unknown attribute type: " + attributeType);
        }
    }

    private static boolean isAttributeValueEmpty(EntitlementService entitlementService, String attributeType) {
        switch (attributeType) {
            case "onAccept":
                return entitlementService.getOnAccept() == null || entitlementService.getOnAccept().getRange() == null;
            case "onReject":
                return entitlementService.getOnReject() == null || entitlementService.getOnReject().getRange() == null;
            case "advice":
                return entitlementService.getAdvice() == null || entitlementService.getAdvice().getRange() == null;
            case "obligations":
                return entitlementService.getObligations() == null || entitlementService.getObligations().getRange() == null;
            default:
                throw new IllegalArgumentException("Unknown attribute type: " + attributeType);
        }
    }


    private static void getEdit(String prefix,
                                String key,
                                Map<String, Object> data,
                                EntitlementService entitlementService,
                                boolean editStartTagOnly,
                                Map<Range, Map<String, Object>> edits) {
        Map<String, Object> dataCopy = new HashMap<>(data);
        if (!Boolean.TRUE.equals(data.get("removeSequence"))) {
            String editKey = prefix + capitalizeFirstLetter(key);
            dataCopy.put(editKey, true);
        }
        TagRanges ranges = getRanges(entitlementService, key);
        Range editRange = null;
        if (ranges != null) {
            editRange = new Range(
                    ranges.getStartTagRange().getStart(),
                    editStartTagOnly ? ranges.getStartTagRange().getEnd() :
                            (ranges.getEndTagRange() != null && ranges.getEndTagRange().getEnd() != null ?
                                    ranges.getEndTagRange().getEnd() : ranges.getStartTagRange().getEnd())
            );
        } else {
            TagRanges entitlementRange = entitlementService.getRange();
            if (entitlementRange != null) {
                editRange = new Range(
                        entitlementRange.getEndTagRange().getStart(),
                        entitlementRange.getEndTagRange().getStart()
                );
            }
        }
        edits.put(editRange, dataCopy);
    }

    private static TagRanges getRanges(EntitlementService entitlementService, String key) {
        switch (key) {
            case "entitlement":
                return entitlementService.getRange();
            case "onAccept":
                return entitlementService.getOnAccept().getRange();
            case "onReject":
                return entitlementService.getOnReject().getRange();
            case "advice":
                return entitlementService.getAdvice().getRange();
            case "obligations":
                return entitlementService.getObligations().getRange();
            default:
                throw new IllegalArgumentException("Unknown attribute type: " + key);
        }
    }

    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public static Map<String, Object> getDataFromST430(EntitlementService node) {

        Map<String, Object> data = new HashMap<>();

        EntitlementServiceClient client = node.getClient();
        switch (client) {
            case basicAuth:
                data.put("entitlementClientType", "SOAP - Basic Auth (WSO2 IS 4.0.0 or later)");
                break;
            case thrift:
                data.put("entitlementClientType", "THRIFT");
                break;
            case soap:
                data.put("entitlementClientType", "SOAP - Authentication Admin (WSO2 IS 3.2.3 or earlier)");
                break;
            case wsXacml:
                data.put("entitlementClientType", "WSXACML");
                break;
        }

        String callbackClassName = node.getCallbackClass();
        if ("org.wso2.carbon.identity.entitlement.mediator.callback.UTEntitlementCallbackHandler".equals(callbackClassName)) {
            data.put("callbackHandler", "UT");
        } else if ("org.wso2.carbon.identity.entitlement.mediator.callback.X509EntitlementCallbackHandler".equals(callbackClassName)) {
            data.put("callbackHandler", "X509");
        } else if ("org.wso2.carbon.identity.entitlement.mediator.callback.SAMLEntitlementCallbackHandler".equals(callbackClassName)) {
            data.put("callbackHandler", "SAML");
        } else if ("org.wso2.carbon.identity.entitlement.mediator.callback.KerberosEntitlementCallbackHandler".equals(callbackClassName)) {
            data.put("callbackHandler", "Kerberos");
        } else {
            data.put("callbackHandler", "Custom");
            data.put("callbackClassName", callbackClassName);
        }

        data.put("description", node.getDescription());
        data.put("client", node.getClient());
        data.put("password", node.getRemoteServicePassword());
        data.put("entitlementServerURL", node.getRemoteServiceUrl());
        data.put("username", node.getRemoteServiceUserName());
        data.put("thriftHost", node.getThriftHost());
        data.put("thriftPort", node.getThriftPort());
        data.put("onAcceptSequenceKey", node.getOnAcceptAttribute());
        data.put("onRejectSequenceKey", node.getOnRejectAttribute());
        data.put("obligationsSequenceKey", node.getObligationsAttribute());
        data.put("adviceSequenceKey", node.getAdviceAttribute());

        data.put("onAcceptSequenceType", node.getOnAcceptAttribute() != null ? "REGISTRY_REFERENCE" : "ANONYMOUS");
        data.put("onRejectSequenceType", node.getOnRejectAttribute() != null ? "REGISTRY_REFERENCE" : "ANONYMOUS");
        data.put("obligationsSequenceType", node.getObligationsAttribute() != null ? "REGISTRY_REFERENCE" : "ANONYMOUS");
        data.put("adviceSequenceType", node.getAdviceAttribute() != null ? "REGISTRY_REFERENCE" : "ANONYMOUS");
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));

        return data;
    }
}
