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

package org.eclipse.lemminx.customservice.synapse.schemagen.xsd;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The JsonSchemaArrayNode class represents a JSON Schema node for array types.
 * It extends the JsonSchemaNode class and provides methods to manage array items.
 */
public class JsonSchemaArrayNode extends JsonSchemaNode {

    private final ArrayNode items;

    /**
     * Constructs a new JsonSchemaArrayNode with the specified ID and parent node.
     *
     * @param id   the identifier for the JSON Schema node
     * @param node the parent JSON Schema object node
     */
    public JsonSchemaArrayNode(String id, ObjectNode node) {

        super(node, id + Utils.ARRAY_FIRST_ELEMENT_IDENTIFIER);
        items = JsonNodeFactory.instance.arrayNode();

        node.put(Utils.ID, id);
        node.put(Utils.TYPE, Utils.ARRAY);
        node.set(Utils.ITEMS, items);
    }

    /**
     * Adds an item to the array node of this JSON Schema array node.
     *
     * @param item the JSON Schema node to add as an item
     */
    public void addItem(JsonSchemaNode item) {

        items.add(item.getNode());
    }
}
