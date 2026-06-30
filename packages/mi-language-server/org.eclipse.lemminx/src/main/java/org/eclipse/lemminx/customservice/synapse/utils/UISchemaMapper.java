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

package org.eclipse.lemminx.customservice.synapse.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connection;
import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.Namespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIConnector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpointParameters;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.eclipse.lemminx.customservice.synapse.utils.Utils.isExpression;

public class UISchemaMapper {

    public static JsonObject mapInputToUISchema(JsonObject data, JsonObject uiSchema) {
        JsonArray elements = uiSchema.getAsJsonArray("elements");
        processElements(data, elements);
        updateSchemaForEventIntegrations(uiSchema);
        return uiSchema;
    }

    private static void processElements(JsonObject data, JsonArray elements) {
        for (JsonElement element : elements) {
            JsonObject elementObj = element.getAsJsonObject();
            String elementType = elementObj.get("type").getAsString();
            if (elementType.equals("attributeGroup")) {
                JsonObject groupValue = elementObj.getAsJsonObject("value");
                JsonArray groupElements = groupValue.getAsJsonArray("elements");
                processElements(data, groupElements);
            } else {
                processElement(data, elementObj);
            }
        }
    }

    private static void processElement(JsonObject elementData, JsonObject elementObj) {
        JsonObject value = elementObj.getAsJsonObject("value");
        if (elementObj.get("type").getAsString().equals("table")) {
            String tableName = value.get("name").getAsString();
            if (elementData.has(tableName)) {
                JsonArray tableData;
                if (elementData.get(tableName).isJsonArray()) {
                    tableData = elementData.getAsJsonArray(tableName);
                } else {
                    tableData = generateTableDataForConnector(elementData.get(tableName).getAsString());
                }
                value.add("currentValue", tableData);
            }
        } else {
            if (value.has("name")) {
                String attributeName = value.get("name").getAsString();
                if (elementData.has(attributeName)) {
                    JsonElement currentValue = elementData.get(attributeName);
                    if (currentValue.isJsonPrimitive() && currentValue.getAsString().startsWith("'{") && currentValue.getAsString().endsWith("}'")) {
                        currentValue = new JsonPrimitive(
                                currentValue.getAsString().substring(1, currentValue.getAsString().length() - 1));
                    } else if (isCheckBox(value)) {
                        currentValue = new JsonPrimitive(currentValue.getAsBoolean());
                    } else if (currentValue.isJsonPrimitive()) {
                        String sanitizedValue = Utils.removeCDATATag(currentValue.getAsString());
                        currentValue = new JsonPrimitive(sanitizedValue);
                    }
                    value.add("currentValue", currentValue);
                }
            }
        }
    }

    private static boolean isCheckBox(JsonObject value) {

        if (value.has(Constant.INPUT_TYPE)) {
            return Constant.CHECK_BOX.equals(value.get(Constant.INPUT_TYPE).getAsString());
        }
        return false;
    }

    private static JsonArray generateTableDataForConnector(String tableFieldCDATA) {

        String tableFieldValue = Utils.removeCDATATag(tableFieldCDATA);
        JsonArray result = new JsonArray();
        JsonArray tableValues = Utils.getJsonArray(tableFieldValue);
        for (JsonElement tableValue : tableValues) {
            String fieldName = "";
            String fieldValue = "";
            if (tableValue instanceof JsonObject) {
                Set<Map.Entry<String, JsonElement>> entries = ((JsonObject) tableValue).entrySet();
                Map.Entry<String, JsonElement> entry = entries.iterator().next();
                fieldName = entry.getKey();
                JsonElement fieldValueElement = entry.getValue();
                if (fieldValueElement.isJsonPrimitive() && fieldValueElement.getAsJsonPrimitive().isString()) {
                    fieldValue = fieldValueElement.getAsString();
                }
            } else if (tableValue instanceof JsonArray) {
                JsonArray tableValueArray = (JsonArray) tableValue;

                if (tableValueArray.size() == 2 && tableValueArray.get(1).isJsonArray()) {

                    JsonElement tableValueElement = tableValueArray.get(0);
                    fieldName = (tableValueElement != null && tableValueElement.isJsonPrimitive()) ?
                            tableValueElement.getAsString() : "";

                    JsonArray nestedArray = tableValueArray.get(1).getAsJsonArray();
                    JsonArray transformedArray = transformNestedArray(nestedArray);

                    JsonArray tableDataRow = new JsonArray();
                    tableDataRow.add(fieldName);
                    tableDataRow.add(transformedArray);

                    result.add(tableDataRow);
                    continue;
                }

                if (tableValueArray.size() == 2) {
                    fieldName = tableValueArray.get(0).getAsString();
                    fieldValue = tableValueArray.get(1).getAsString();
                } else {
                    // support for param manager with 3 fields
                    JsonArray tableDataRow = new JsonArray();
                    JsonObject rowInfo;

                    for (int j = 0; j < tableValueArray.size(); j++) {
                        fieldName = tableValueArray.get(j).getAsString();
                        if (j == 0) {
                            tableDataRow.add(fieldName);
                        } else {
                            rowInfo = new JsonObject();
                            fieldValue = tableValueArray.get(j).getAsString().trim();
                            rowInfo.add("isExpression", new JsonPrimitive(isExpression(fieldValue)));
                            rowInfo.add("value", new JsonPrimitive(fieldValue));
                            rowInfo.add("namespaces", new JsonArray());
                            tableDataRow.add(rowInfo);
                        }
                    }
                    result.add(tableDataRow);
                    continue;
                }
            }
            if (StringUtils.isNotEmpty(fieldName)) {
                JsonArray tableDataRow = new JsonArray();
                JsonObject rowInfo = new JsonObject();

                rowInfo.add("isExpression", new JsonPrimitive(isExpression(fieldValue)));
                rowInfo.add("value", new JsonPrimitive(fieldValue));
                rowInfo.add("namespaces", new JsonArray());

                tableDataRow.add(fieldName);
                tableDataRow.add(rowInfo);
                tableDataRow.add(rowInfo);
                result.add(tableDataRow);
            }
        }
        return result;
    }

    public static JsonObject mapInputToUISchemaForConnector(Connector connector, JsonObject uiSchema) {

        JsonObject data = new JsonObject();
        connector.getParameters().forEach(parameter -> {
            String name = parameter.getName();
            if (parameter.getIsExpression()) {
                JsonObject expression = getConnectorExpressionParam(parameter);
                data.add(name, expression);
            } else {
                data.addProperty(name, parameter.getValue());
            }
        });
        if (connector instanceof AIConnector) {
            addAIConnectionsData(data, (AIConnector) connector);
        } else {
            data.addProperty(Constant.CONFIG_REF, connector.getConfigKey());
        }
        return mapInputToUISchema(data, uiSchema);
    }

    private static void addAIConnectionsData(JsonObject data, AIConnector connector) {

        Map<String, Connection> connectionMap = connector.getConnections();
        if (connectionMap == null && connectionMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            String fieldKey = Constant.AI_CONNECTION_TO_DISPLAY_NAME_MAP.inverse().get(entry.getKey());
            Connection connection = entry.getValue();
            if (fieldKey != null && connection != null) {
                data.addProperty(fieldKey, connection.getName());
            }
        }
    }

    private static JsonObject getConnectorExpressionParam(ConnectorParameter parameter) {

        List<Namespace> namespaces = MediatorUtils.transformNamespaces(parameter.getNamespaces());
        JsonArray namespacesJson = new Gson().toJsonTree(namespaces).getAsJsonArray();
        JsonObject expression = new JsonObject();
        String expressionValue = parameter.getExpression();
        Pattern pattern = Pattern.compile("\\{.*}");
        if (expressionValue != null && pattern.matcher(expressionValue).matches()) {
            expressionValue = expressionValue.substring(1, expressionValue.length() - 1);
        }
        expression.addProperty(Constant.VALUE, expressionValue);
        expression.add(Constant.NAMESPACES, namespacesJson);
        expression.addProperty(Constant.IS_EXPRESSION, true);
        return expression;
    }

    public static JsonObject mapInputToUISchemaForInboundEndpoint(InboundEndpoint ib, JsonObject uiSchema) {

        JsonObject data = new JsonObject();
        data.addProperty(Constant.NAME, ib.getName());
        data.addProperty(Constant.SEQUENCE, ib.getSequence());
        data.addProperty(Constant.PROTOCOL, ib.getProtocol());
        data.addProperty(Constant.CLASS, ib.getClazz());
        data.addProperty(Constant.ON_ERROR, ib.getOnError());
        data.addProperty(Constant.SUSPEND, ib.isSuspend());
        data.addProperty(Constant.STATISTICS, ib.getStatistics() != null ? ib.getStatistics().toString() : null);
        data.addProperty(Constant.TRACE, ib.getTrace() != null ? ib.getTrace().toString() : null);
        InboundEndpointParameters[] parametersList = ib.getParameters();
        if (parametersList != null) {
            InboundEndpointParameters parameters = parametersList[0];
            if (parameters != null) {
                Parameter[] parametersArray = parameters.getParameter();
                if (parametersArray != null) {
                    for (int i = 0; i < parametersArray.length; i++) {
                        Parameter parameter = parametersArray[i];
                        data.addProperty(parameter.getName(), parameter.getContent());
                    }
                }
            }
        }
        return mapInputToUISchema(data, uiSchema);
    }

    private static void updateSchemaForEventIntegrations(JsonObject schema) {
        List<String> inboundEndpointTypes = List.of(Constant.INBUILT_INBOUND_ENDPOINT, Constant.EVENT_INTEGRATION);
        if (!schema.has(Constant.TYPE) || !inboundEndpointTypes.contains(schema.get(Constant.TYPE).getAsString())) {
            return;
        }

        JsonArray groups = schema.getAsJsonArray(Constant.ELEMENTS);

        for (JsonElement groupElem : groups) {
            JsonObject groupObj = groupElem.getAsJsonObject();
            JsonObject groupValue = groupObj.getAsJsonObject(Constant.VALUE);
            if (!Constant.GENERIC.equals(groupValue.get(Constant.GROUP_NAME).getAsString())) {
                continue;
            }

            JsonArray attributes = groupValue.getAsJsonArray(Constant.ELEMENTS);
            for (JsonElement attrElem : attributes) {
                JsonObject attrObj = attrElem.getAsJsonObject();
                if (!Constant.ATTRIBUTE.equals(attrObj.get(Constant.TYPE).getAsString())) {
                    continue;
                }

                JsonObject value = attrObj.getAsJsonObject(Constant.VALUE);
                if (Constant.SEQUENCE.equals(value.get(Constant.NAME).getAsString())) {
                    value.remove(Constant.ENABLE_CONDITION);
                }
                if (Constant.ON_ERROR.equals(value.get(Constant.NAME).getAsString())) {
                    value.remove(Constant.ENABLE_CONDITION);
                }
                if (Constant.GENERATE_SEQUENCES.equals(value.get(Constant.NAME).getAsString())) {
                    value.addProperty(Constant.CURRENT_VALUE, false);
                    value.addProperty(Constant.HIDDEN, true);
                }
            }
        }
    }

    private static JsonArray transformNestedArray(JsonArray array) {

        JsonArray transformed = new JsonArray();

        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) {
                transformed.add(new JsonArray());
                continue;
            }

            if (element.isJsonArray()) {
                transformed.add(transformNestedArray(element.getAsJsonArray()));
            } else {
                JsonArray singleValueArray = new JsonArray();
                singleValueArray.add(element.isJsonPrimitive() ? element.getAsString() : element.toString());
                transformed.add(singleValueArray);
            }
        }
        return transformed;
    }
}
