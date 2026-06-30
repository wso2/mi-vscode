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

/**
 * The JsonSchemaElementValueNode class represents a JSON Schema node for element values.
 * It extends the JsonSchemaNode class and provides methods to manage element value nodes.
 */
public class JsonSchemaElementValueNode extends JsonSchemaNode {

    /**
     * Constructs a new JsonSchemaElementValueNode with the specified ID and type.
     *
     * @param id   the identifier for the JSON Schema node
     * @param type the type of the JSON Schema node
     */
    public JsonSchemaElementValueNode(String id, String type) {

        super(id + Utils.ID_VALUE_SEPERATOR + Utils.ELEMENT_VALUE, type);
    }
}
