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
import org.apache.commons.lang3.StringUtils;

/**
 * The JsonSchemaObjectNode class represents a JSON Schema node for object types.
 * It extends the JsonSchemaNode class and provides methods to manage object properties and required elements.
 */
public class JsonSchemaObjectNode extends JsonSchemaNode {

    private final ObjectNode properties;
    private final ArrayNode required;

    /**
     * Constructs a new JsonSchemaObjectNode with the specified ID.
     *
     * @param id the identifier for the JSON Schema node
     */
    public JsonSchemaObjectNode(String id) {

        super(id, Utils.OBJECT);
        properties = JsonNodeFactory.instance.objectNode();
        required = JsonNodeFactory.instance.arrayNode();
        node.set(Utils.PROPERTIES, properties);
    }

    /**
     * Constructs a new JsonSchemaObjectNode with the specified JSON object node, ID, and name.
     *
     * @param node the JSON object node
     * @param id   the identifier for the JSON Schema node
     * @param name the name of the JSON Schema node
     */
    public JsonSchemaObjectNode(ObjectNode node, String id, String name) {

        super(node, id, Utils.OBJECT, name);
        properties = JsonNodeFactory.instance.objectNode();
        required = JsonNodeFactory.instance.arrayNode();
        this.node.set(Utils.PROPERTIES, properties);
    }

    /**
     * Updates the JSON Schema object node, optionally adding a title.
     *
     * @param addTitle flag indicating whether to add a title
     */
    public void update(boolean addTitle) {

        if (!required.isEmpty()) {
            node.set(Utils.REQUIRED, required);
        }
        if (addTitle && StringUtils.isNotEmpty(name)) {
            node.put(Utils.TITLE, name);
        }
    }

    /**
     * Adds a property to this JSON Schema object node.
     *
     * @param name     the name of the property
     * @param property the JSON Schema node representing the property
     */
    public void addProperty(String name, ObjectNode property) {

        properties.set(name, property);
    }

    /**
     * Adds a required element to this JSON Schema object node.
     *
     * @param itemName the name of the required element
     */
    public void addRequiredElement(String itemName) {

        required.add(itemName);
    }
}
