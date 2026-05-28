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
 * The JsonSchemaOneOfNode class represents a JSON Schema node for the "oneOf" keyword.
 * It extends the JsonSchemaNode class and provides methods to manage the "oneOf" schema.
 */
public class JsonSchemaOneOfNode extends JsonSchemaNode {

    /**
     * Constructs a new JsonSchemaOneOfNode with an empty JSON object node.
     */
    public JsonSchemaOneOfNode() {

        node = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Constructs a new JsonSchemaOneOfNode with the specified JSON object node and name.
     *
     * @param node the JSON object node
     * @param name the name of the JSON Schema node
     */
    public JsonSchemaOneOfNode(ObjectNode node, String name) {

        super(node);
        this.name = name;
    }

    /**
     * Sets the JSON array node representing the content of oneOf.
     *
     * @param oneOf the JSON array node containing objects
     */
    public void setOneOf(ArrayNode oneOf) {

        node.set(Utils.ONE_OF, oneOf);
    }
}
