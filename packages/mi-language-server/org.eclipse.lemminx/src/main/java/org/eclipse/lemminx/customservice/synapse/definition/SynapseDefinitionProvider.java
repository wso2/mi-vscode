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

package org.eclipse.lemminx.customservice.synapse.definition;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.utils.ConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.ExtendedLocation;
import org.eclipse.lemminx.customservice.synapse.utils.LegacyConfigFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.utils.Utils.hasResourceForKey;

public class SynapseDefinitionProvider {

    private static final Logger LOGGER = Logger.getLogger(SynapseDefinitionProvider.class.getName());

    private static List<String> sequenceAttributes = List.of("inSequence", "outSequence", "faultSequence", "sequence"
            , "onAccept", "onReject", "obligation", "advice", "onError");
    private static List<String> endpointAttributes = List.of("endpointKey", "targetEndpoint", "endpoint");

    private static final Map<String, String> TYPE_TO_SINGULAR =
            Map.of("endpoints", "endpoint",
                    "sequences", "sequence",
                    "templates", "template",
                    "message-stores", "messageStore",
                    "data-services", "dataService",
                    "local-entries", "localEntry");

    public static ExtendedLocation definition(DOMDocument document, Position position, String projectPath,
                                              CancelChecker cancelChecker, Map<String, ResourceResponse> dependentResources) {

        cancelChecker.checkCanceled();
        ExtendedLocation result = new ExtendedLocation(null, null, false);

        int offset;
        try {
            offset = document.offsetAt(position);
        } catch (BadLocationException e) {
            LOGGER.log(Level.WARNING, "Error while reading file content", e);
            return result;
        }

        KeyAndTypeHolder keyAndType = getKeyAndType(document, offset);
        if (!keyAndType.isNull()) {
            Boolean isLegacyProject = Utils.isLegacyProject(projectPath);

            String path = null;
            try {
                if (isLegacyProject) {
                    path = LegacyConfigFinder.findEsbComponentPath(keyAndType.getKey(), keyAndType.getType(),
                            projectPath);
                } else {
                    path = ConfigFinder.findEsbComponentPath(keyAndType.getKey(), keyAndType.getType(),
                            projectPath);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error while reading file content", e);
            }
            if (path == null) {
                String singularType = TYPE_TO_SINGULAR.get(keyAndType.getType());
                ResourceResponse resourceResponse = dependentResources.get(singularType);
                if (hasResourceForKey(keyAndType.getKey(), resourceResponse)) {
                    return new ExtendedLocation(null, null, true);
                }
            } else {
                Range range = getDefinitionRange(path);
                return new ExtendedLocation(path, range, false);
            }
        }
        return result;
    }

    private static KeyAndTypeHolder getKeyAndType(DOMDocument document, int offset) {

        DOMAttr clickedAttr = document.findAttrAt(offset);
        if (clickedAttr != null && clickedAttr.getNodeAttrName().getEnd() <= offset) {
            DOMNode node = document.findNodeAt(offset);
            String type = null;
            String key = null;
            if (Constant.ENDPOINT.equalsIgnoreCase(node.getNodeName()) || Constant.SEQUENCE.equalsIgnoreCase(node.getNodeName())) {
                type = node.getNodeName().toLowerCase() + "s";
                key = node.getAttribute(Constant.KEY);
            } else if (Constant.CALL_TEMPLATE.equalsIgnoreCase(node.getNodeName())) {
                type = "templates";
                key = node.getAttribute(Constant.TARGET);
            } else if ("tool".equals(node.getNodeName())) {
                type = "templates";
                key = node.getAttribute(Constant.TEMPLATE);
            } else if (Constant.STORE.equalsIgnoreCase(node.getNodeName())) {
                type = "message-stores";
                key = node.getAttribute(Constant.MESSAGE_STORE);
            } else if ("dataServiceCall".equals(node.getNodeName())) {
                type = "data-services";
                key = node.getAttribute(Constant.SERVICE_NAME);
            } else {
                if (sequenceAttributes.contains(clickedAttr.getNodeName())) {
                    type = "sequences";
                    key = clickedAttr.getNodeValue();
                } else if (endpointAttributes.contains(clickedAttr.getNodeName())) {
                    type = "endpoints";
                    key = clickedAttr.getNodeValue();
                } else if ("configKey".equals(clickedAttr.getNodeName())) {
                    key = clickedAttr.getNodeValue();
                    type = "local-entries";
                } else {
                    String attKey = clickedAttr.getNodeValue();
                    if (attKey != null) {
                        if (attKey.contains(Constant.GOV_REGISTRY_PREFIX) || attKey.contains(Constant.CONF_REGISTRY_PREFIX)) {
                            key = attKey;
                        }
                    }
                }
            }
            KeyAndTypeHolder keyAndTypeHolder = new KeyAndTypeHolder(key, type);
            return keyAndTypeHolder;
        }
        return new KeyAndTypeHolder(null, null);
    }

    private static Range getDefinitionRange(String path) {

        File file = new File(path);
        Range range;
        try {
            DOMDocument document = Utils.getDOMDocument(file);
            DOMNode node = Utils.getRootElementFromConfigXml(document);
            if (node == null) {
                return new Range(new Position(0, 0), new Position(0, 1));
            }
            Position start = document.positionAt(node.getStart());
            Position end = document.positionAt(node.getEnd());
            range = new Range(start, end);
        } catch (IOException | BadLocationException e) {
            range = new Range(new Position(0, 0), new Position(0, 1));
        }
        return range;
    }

    private static class KeyAndTypeHolder {

        private String key;
        private String type;

        public KeyAndTypeHolder(String key, String type) {

            this.key = key;
            this.type = type;
        }

        public String getKey() {

            return key;
        }

        public String getType() {

            return type;
        }

        public boolean isNull() {

            return key == null && type == null;
        }
    }
}
