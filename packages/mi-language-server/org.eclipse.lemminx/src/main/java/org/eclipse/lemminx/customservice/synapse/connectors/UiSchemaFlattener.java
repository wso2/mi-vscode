/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.customservice.synapse.connectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens a connector/inbound uischema {@code elements} tree into a flat list
 * of {@link OperationParameter}. Recurses into {@code attributeGroup} entries
 * and skips hidden attributes. Used for connection parameter enumeration.
 */
public final class UiSchemaFlattener {

    private UiSchemaFlattener() {
    }

    /**
     * Walks the given {@code elements} JSON array and returns every non-hidden
     * {@code attribute} as an {@link OperationParameter}, including its default
     * value from the uischema when present.
     */
    public static List<OperationParameter> flatten(JsonArray elements) {

        List<OperationParameter> out = new ArrayList<>();
        if (elements != null) {
            walk(elements, out);
        }
        return out;
    }

    private static void walk(JsonArray elements, List<OperationParameter> out) {

        for (JsonElement raw : elements) {
            if (!raw.isJsonObject()) {
                continue;
            }
            JsonObject element = raw.getAsJsonObject();
            String type = element.has(Constant.TYPE) ? element.get(Constant.TYPE).getAsString() : null;
            if (type == null || !element.has(Constant.VALUE) || !element.get(Constant.VALUE).isJsonObject()) {
                continue;
            }
            JsonObject value = element.getAsJsonObject(Constant.VALUE);
            if (Constant.ATTRIBUTE_GROUP.equals(type)) {
                if (value.has(Constant.ELEMENTS) && value.get(Constant.ELEMENTS).isJsonArray()) {
                    walk(value.getAsJsonArray(Constant.ELEMENTS), out);
                }
            } else if (Constant.ATTRIBUTE.equals(type)) {
                if (value.has(Constant.HIDDEN) && value.get(Constant.HIDDEN).getAsBoolean()) {
                    continue;
                }
                String name = value.has(Constant.NAME) ? value.get(Constant.NAME).getAsString() : null;
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                String description;
                if (value.has(Constant.HELP_TIP)) {
                    description = value.get(Constant.HELP_TIP).getAsString();
                } else if (value.has(Constant.DISPLAY_NAME)) {
                    description = value.get(Constant.DISPLAY_NAME).getAsString();
                } else {
                    description = StringUtils.EMPTY;
                }
                boolean required = false;
                if (value.has(Constant.REQUIRED)) {
                    JsonElement req = value.get(Constant.REQUIRED);
                    if (req.isJsonPrimitive()) {
                        required = Boolean.parseBoolean(req.getAsString());
                    }
                }
                OperationParameter param = new OperationParameter(name, description, required);
                param.setXsdType(mapInputTypeToXsd(
                        value.has(Constant.INPUT_TYPE) ? value.get(Constant.INPUT_TYPE).getAsString() : null));
                if (value.has(Constant.DEFAULT_VALUE)) {
                    JsonElement def = value.get(Constant.DEFAULT_VALUE);
                    if (def.isJsonPrimitive()) {
                        param.setDefaultValue(def.getAsString());
                    }
                }
                out.add(param);
            }
        }
    }

    private static String mapInputTypeToXsd(String inputType) {

        if (inputType == null) {
            return "xs:string";
        }
        switch (inputType) {
            case "number":
            case "integer":
                return "integerOrExpression";
            case "boolean":
            case "booleanOrExpression":
            case "checkbox":
                return "xs:boolean";
            default:
                return "xs:string";
        }
    }
}
