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

package org.eclipse.lemminx.customservice.synapse.mediatorService;

import com.github.mustachejava.Mustache;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.SynapseConfigResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ai.AIConnector;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.InvalidMediator;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.UISchemaMapper;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorUtils.generateResponseVariableDefaultValue;
import static org.eclipse.lemminx.customservice.synapse.utils.UISchemaMapper.mapInputToUISchema;

public class MediatorHandler {

    private static final Logger LOGGER = Logger.getLogger(MediatorHandler.class.getName());
    private static final int MAX_DEPTH = 10;
    private JsonObject mediatorList;
    private JsonObject agentToolList;
    private Map<String, JsonObject> uiSchemaMap;
    private Map<String, Mustache> templateMap;
    private ConnectorHolder connectorHolder;
    private boolean isInitialized;
    private Gson gson;
    private String miServerVersion;
    private AIConnectorHandler aiConnectorHandler;
    private String projectUri;

    public void init(String projectUri, String projectServerVersion, ConnectorHolder connectorHolder) {

        try {
            this.miServerVersion = projectServerVersion;
            this.connectorHolder = connectorHolder;
            this.mediatorList = Utils.getMediatorList(projectServerVersion, connectorHolder);
            this.agentToolList = Utils.getAgentToolList(mediatorList, connectorHolder);
            gson = new Gson();
            this.aiConnectorHandler = new AIConnectorHandler(this, projectUri);
            this.projectUri = projectUri;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    String.format("Failed to load mediators for the MI server version: %s", projectServerVersion), e);
            LOGGER.warning(String.format("Falling back to default mediators (MI %s).", Constant.DEFAULT_MI_VERSION));
            try {
                this.mediatorList = Utils.getMediatorList(Constant.DEFAULT_MI_VERSION, connectorHolder);
            } catch (IOException ex) {
                // This should not happen
            }
        }
        this.templateMap = Utils.getTemplateMap("org/eclipse/lemminx/mediators/"
                + projectServerVersion.replace(".", "") + "/templates");
        this.uiSchemaMap = Utils.getUISchemaMap("org/eclipse/lemminx/mediators/"
                + projectServerVersion.replace(".", "") + "/ui-schemas");
        this.isInitialized = true;
    }

    public JsonObject getSupportedMediators(TextDocumentIdentifier documentIdentifier, Position position) {

        try {
            if (isRequestedForAgentTool(documentIdentifier.getUri(), position)) {
                return agentToolList;
            }
            DOMDocument document = Utils.getDOMDocument(new File(new URI(documentIdentifier.getUri())));
            List<String> lastMediators = Arrays.asList("send", "drop", "loopback", "respond");
            List<String> iterateMediators = Arrays.asList("iterate", "foreach");
            int offset = document.offsetAt(position);
            DOMNode currentNode = document.findNodeBefore(offset);
            DOMNode nextMediator = currentNode.getNextSibling();
            if (lastMediators.contains(currentNode.getNodeName())) {
                return new JsonObject();
            } else {
                if (isAddedAbove(currentNode, nextMediator, offset) || isIterateMediator(currentNode, iterateMediators)) {
                    return removeMediators(mediatorList, lastMediators);
                }
                return mediatorList;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while retrieving supported mediators.", e);
        }
        return null;
    }

    public SynapseConfigResponse generateSynapseConfig(String documentUri, Range range, String mediator,
                                                       Map<String, Object> data, List<String> dirtyFields) {

        try {
            boolean isUpdate = !range.getEnd().equals(range.getStart());
            STNode node = null;
            if (StringUtils.isNotEmpty(documentUri) && Files.exists(Path.of(documentUri))) {
                node = getMediatorNodeAtPosition(Utils.getDOMDocument(new File(documentUri)), range.getStart(),
                        isUpdate);
            }
            if (isRequestedForAgentTool(documentUri, range.getStart())) {
                return aiConnectorHandler.generateAgentToolConfig(documentUri, range, mediator, data, dirtyFields,
                        isUpdate);
            } else if (isAIConnector(node, mediator)) {
                return aiConnectorHandler.generateAIConnectorConfig(node, mediator, data, range);
            } else if (isConnector(node, mediator)) {
                return generateConnectorSynapseConfig(node, mediator, data, range);
            } else {
                return generateMediatorSynapseConfig(node, mediator, data, dirtyFields, range);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while generating Synapse configuration.", e);
        }
        return null;
    }

    private boolean isAIConnector(STNode node, String mediator) {

        return node instanceof AIConnector || (StringUtils.isNotEmpty(mediator) && mediator.startsWith("ai."));
    }

    private boolean isConnector(STNode node, String mediator) {

        return node instanceof Connector || mediator.contains(".");
    }

    private SynapseConfigResponse generateConnectorSynapseConfig(STNode node, String mediator, Map<String, Object> data,
                                                                 Range range) {
        ConnectorAction operation = getConnectorOperation(node, mediator);
        if (operation != null) {
            List<OperationParameter> parameters = operation.getParameters();
            Map<String, Object> connectorData = new HashMap<>();
            connectorData.put(Constant.TAG, operation.getTag());
            connectorData.put(Constant.CONFIG_KEY, data.get(Constant.CONFIG_KEY));
            List<Object> parameterData = new ArrayList<>();
            for (OperationParameter parameter : parameters) {
                if (data.containsKey(parameter.getName())) {
                    Map<String, Object> dataValue = processConnectorParameter(data.get(parameter.getName()));
                    parameterData.add(Map.of(Constant.NAME, parameter.getName(), Constant.VALUE, dataValue));
                }
            }

            // support for dynamically added parameters
            // for every value in data with key that starts with dyn_param_ but not
            // dyn_param_temp_ add to the parameterData
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getKey().startsWith("dyn_param_") && !entry.getKey().startsWith("dyn_param_temp_")
                        && entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                    String name = entry.getKey().substring(entry.getKey().lastIndexOf("_") + 1);
                    Map<String, Object> dataValue = processConnectorParameter(entry.getValue());
                    parameterData.add(Map.of(Constant.NAME, name, Constant.VALUE, dataValue));
                }
            }

            connectorData.put(Constant.PARAMETERS, parameterData);
            StringWriter writer = new StringWriter();
            String edit = templateMap.get(Constant.CONNECTOR).execute(writer, connectorData).toString();
            TextEdit textEdit = new TextEdit(range, edit);
            return new SynapseConfigResponse(textEdit);
        }
        return null;
    }

    protected Map<String, Object> processConnectorParameter(Object data) {
        Map<String, Object> dataValue = new HashMap<>();
        boolean isExpressionField = false;
        if (data instanceof String) {
            String dataStr = (String) data;
            String trimmedDataStr = dataStr.trim();
            if (trimmedDataStr.startsWith("{") && trimmedDataStr.endsWith("}")) {
                dataValue.put(Constant.VALUE, String.format("'%s'", trimmedDataStr));
            } else {
                dataValue.put(Constant.VALUE, String.format("%s", data));
            }
        } else if (data instanceof Boolean) {
            dataValue.put(Constant.VALUE, data);
        } else if (data instanceof Map) {
            dataValue = (Map) data;
            Object isExpressionObj = dataValue.get(Constant.IS_EXPRESSION);
            boolean isExpression = isExpressionObj == null ? false : (boolean) isExpressionObj;
            if (isExpression) {
                isExpressionField = true;
                dataValue.put(Constant.VALUE, String.format("{%s}", dataValue.get(Constant.VALUE)));
                dataValue.put(Constant.IS_EXPRESSION, true);
            } else {
                if (dataValue.get(Constant.VALUE).toString().startsWith("{") &&
                        dataValue.get(Constant.VALUE).toString().endsWith("}")) {
                    dataValue.put(Constant.VALUE, String.format("'%s'", dataValue.get(Constant.VALUE)));
                }
            }
        } else if (data instanceof List) {
            List<LinkedTreeMap> dataValueList = (List) data;
            StringBuilder dataValueStr = new StringBuilder("[");
            int i = 0;
            for (LinkedTreeMap dataValueItem : dataValueList) {
                // Separate only if another value exists
                if (i > 0) {
                    dataValueStr.append(",");
                }

                if (dataValueItem.get(Constant.PROPERTY_NAME) != null &&
                        dataValueItem.get(Constant.PROPERTY_VALUE) != null) {
                    Object propertyValue;
                    if (dataValueItem.get(Constant.PROPERTY_VALUE) instanceof LinkedTreeMap) {
                        LinkedTreeMap dataValueLinkedTree = (LinkedTreeMap) dataValueItem.get(Constant.PROPERTY_VALUE);
                        propertyValue = dataValueLinkedTree.get(Constant.VALUE);
                    } else {
                        propertyValue = dataValueItem.get(Constant.PROPERTY_VALUE);
                    }
                    dataValueStr.append(String.format("{\"%s\":\"%s\"}",
                            dataValueItem.get(Constant.PROPERTY_NAME), propertyValue));
                } else if (dataValueList.size() > 0) {
                    // get all the values in the list and append to the dataValueStr in the format
                    // ["value1","value2","value3"...]

                    // if count of value larger than 0
                    dataValueStr.append("[");
                    Iterator<Object> iterator = dataValueItem.keySet().iterator();
                    while (iterator.hasNext()) {
                        boolean keepAsObject = false;
                        Object key = iterator.next();
                        Object propertyValue;
                        if (dataValueItem.get(key) instanceof LinkedTreeMap) {
                            LinkedTreeMap dataValueLinkedTree = (LinkedTreeMap) dataValueItem.get(key);
                            propertyValue = dataValueLinkedTree.get(Constant.VALUE);
                        } else {
                            propertyValue = dataValueItem.get(key);
                        }

                        if (propertyValue instanceof List) {
                            keepAsObject = true;
                            propertyValue = buildNestedArray(propertyValue);
                        }

                        if (!keepAsObject) {
                            // encode double quotes in the value if present
                            if (propertyValue instanceof String) {
                                propertyValue = ((String) propertyValue).replace("\"", "\\\"");
                            }

                            dataValueStr.append(String.format("\"%s\"", propertyValue));
                        } else {
                            dataValueStr.append(String.format("%s", propertyValue));
                        }

                        // Separate only if another value exists
                        if (iterator.hasNext()) {
                            dataValueStr.append(",");
                        }
                    }
                    dataValueStr.append("]");
                }
                i++;
            }
            dataValueStr.append(']');
            dataValue.put(Constant.VALUE, String.format("%s", dataValueStr));
        }

        if (dataValue.get(Constant.VALUE) != null && dataValue.get(Constant.VALUE).toString().startsWith("<![CDATA[")) {
            dataValue.put(Constant.IS_CDATA, true);
            String value = dataValue.get(Constant.VALUE).toString().substring(9); // Remove <![CDATA[
            if (value.endsWith("]]>")) {
                value = value.substring(0, value.length() - 3); // Remove ]]>
            }
            dataValue.put(Constant.VALUE, value);
        }
        if (!isExpressionField && hasSpecialXmlCharacter(dataValue.get(Constant.VALUE))) {
            dataValue.put(Constant.IS_CDATA, true);
        }
        return dataValue;
    }

    private boolean hasSpecialXmlCharacter(Object o) {

        if (!(o instanceof String)) {
            return false;
        }
        String value = (String) o;
        return value.contains("&") || value.contains("<") || value.contains(">");
    }

    protected ConnectorAction getConnectorOperation(STNode node, String mediator) {

        String connectorName;
        String operation;
        if (node instanceof Connector) {
            connectorName = ((Connector) node).getConnectorName();
            operation = ((Connector) node).getMethod();
        } else {
            connectorName = mediator.split("\\.")[0];
            operation = getConnectorOperationName(mediator, connectorName);
        }
        if (!connectorHolder.exists(connectorName)) {
            return null;
        }
        org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector connectorMetadata =
                connectorHolder.getConnector(connectorName);
        return connectorMetadata.getAction(operation);
    }

    private SynapseConfigResponse generateMediatorSynapseConfig(STNode node, String mediator, Map<String, Object> data,
                                                                List<String> dirtyFields,
                                                                Range range)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {

        for (Map.Entry<String, JsonElement> entry : mediatorList.entrySet()) {
            if (Constant.AI.equalsIgnoreCase(entry.getKey())) {
                // Skip AI mediators as that is handled by {@link AIConnectorHandler}
                continue;
            }
            JsonArray mediatorsArray = getMediatorsArrayForCategory(entry.getValue());
            for (JsonElement mediatorElement : mediatorsArray) {
                JsonObject mediatorObject = mediatorElement.getAsJsonObject();
                if (mediator.equals(mediatorObject.get(Constant.TAG).getAsString())) {
                    String mediatorClass = mediatorObject.get(Constant.MEDIATOR_CLASS).getAsString();
                    String processingClass = mediatorObject.get(Constant.PROCESSING_CLASS).getAsString();
                    String processingMethod = mediatorObject.get(Constant.STORE_METHOD).getAsString();
                    if(mediator.equals(Constant.DATA_MAPPER) && data.get(Constant.NAME).toString().contains("gov:datamapper")){
                        String name = (String) data.get(Constant.NAME);
                        String dmName = name.substring(name.lastIndexOf("/") + 1);
                        String dmcPath = Path.of(projectUri, Constant.SRC, Constant.MAIN, Constant.WSO2MI,
                                Constant.RESOURCES, Constant.REGISTRY, Constant.GOV,
                                Constant.DATA_MAPPER, dmName + ".dmc").toString();
                        if(Files.exists(Path.of(dmcPath))){
                            data.put(Constant.NAME, data.get(Constant.NAME) + ".dmc");
                        }
                    }
                    Class<?> mediatorProcessor = Class.forName(processingClass);
                    Object processorInstance = mediatorProcessor.getDeclaredConstructor().newInstance();
                    Method processorMethod =
                            mediatorProcessor.getMethod(processingMethod, Map.class, Class.forName(mediatorClass),
                                    List.class);
                    if (!Class.forName(mediatorClass).isInstance(node)) {
                        node = null;
                    }
                    @SuppressWarnings("unchecked")
                    Either<Map<String, Object>, Map<Range, Map<String, Object>>>
                            processedData =
                            (Either<Map<String, Object>, Map<Range, Map<String, Object>>>) processorMethod.invoke(
                                    processorInstance, data, node, dirtyFields);
                    if (processedData.isLeft()) {
                        StringWriter writer = new StringWriter();
                        String edit =
                                templateMap.get(mediator).execute(writer, processedData.getLeft()).toString().trim();
                        TextEdit textEdit = new TextEdit(range, edit);
                        return new SynapseConfigResponse(textEdit);
                    } else {
                        Map<Range, Map<String, Object>> editsData = processedData.getRight();
                        SynapseConfigResponse edits = new SynapseConfigResponse();
                        for (Map.Entry<Range, Map<String, Object>> entry1 : editsData.entrySet()) {
                            StringWriter writer = new StringWriter();
                            String edit =
                                    templateMap.get(mediator).execute(writer, entry1.getValue()).toString().trim();
                            if (isBlankEdit(entry1.getKey(), edit)) {
                                continue;
                            }
                            TextEdit textEdit = new TextEdit(entry1.getKey(), edit);
                            edits.addTextEdit(textEdit);
                        }
                        return edits;
                    }
                }
            }
        }
        return null;
    }

    private boolean isBlankEdit(Range range, String edit) {

        if (range.getStart().getLine() == range.getEnd().getLine()) {
            return range.getStart().getCharacter() == range.getEnd().getCharacter() && edit.trim().isEmpty();
        }
        return false;
    }

    private JsonArray getMediatorsArrayForCategory(JsonElement value) {

        JsonElement listElements = value.getAsJsonObject().get(Constant.ITEMS);
        JsonArray mediatorsArray;
        if (listElements.isJsonArray()) {
            mediatorsArray = listElements.getAsJsonArray();
        } else {
            // Straighten the categorized connectors to a single array
            mediatorsArray = new JsonArray();
            listElements.getAsJsonObject().keySet().forEach(key -> {
                mediatorsArray.addAll(listElements.getAsJsonObject().getAsJsonArray(key));
            });
        }
        return mediatorsArray;
    }

    public JsonObject getUISchemaWithValues(TextDocumentIdentifier documentIdentifier, Position position) {

        try {
            DOMDocument document = Utils.getDOMDocumentFromPath(documentIdentifier.getUri());
            if (isRequestedForAgentTool(documentIdentifier.getUri(), position)) {
                return aiConnectorHandler.getToolSchemaWithValues(document, position);
            }
            STNode node = getMediatorNodeAtPosition(document, position, Boolean.TRUE);
            return getUISchemaForSTNode(node);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while updating UI schema with existing values.", e);
        }
        return null;
    }

    protected JsonObject getUISchemaForSTNode(STNode node) throws Exception {

        if (node != null) {
            if (node instanceof Connector) {
                return getUISchemaWithValuesForConnector(node);
            }
            return getUISchemaWithValuesForMediator(node);
        }
        return null;
    }

    private JsonObject getUISchemaWithValuesForConnector(STNode node) {

        Connector connector = (Connector) node;
        JsonObject uiSchema = getConnectorUiSchema(connector.getTag(), null, null);
        return UISchemaMapper.mapInputToUISchemaForConnector(connector, uiSchema);
    }

    private JsonObject getUISchemaWithValuesForMediator(STNode node)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException,
            InstantiationException {

        String mediatorName = node.getTag();
        for (Map.Entry<String, JsonElement> entry : mediatorList.entrySet()) {
            if (Constant.AI.equalsIgnoreCase(entry.getKey())) {
                // Skip AI mediators as that is handled by {@link AIConnectorHandler}
                continue;
            }
            JsonArray mediatorsArray = getMediatorsArrayForCategory(entry.getValue());
            for (JsonElement mediatorElement : mediatorsArray) {
                JsonObject mediator = mediatorElement.getAsJsonObject();
                if (mediatorName.equals(mediator.get(Constant.TAG).getAsString())) {
                    String mediatorClass = mediator.get(Constant.MEDIATOR_CLASS).getAsString();
                    String processingClass = mediator.get(Constant.PROCESSING_CLASS).getAsString();
                    String processingMethod = mediator.get(Constant.RETRIEVE_METHOD).getAsString();
                    Class<?> mediatorProcessor = Class.forName(processingClass);
                    Object processorInstance = mediatorProcessor.getDeclaredConstructor().newInstance();
                    Method processorMethod =
                            mediatorProcessor.getMethod(processingMethod, Class.forName(mediatorClass));
                    Object data = processorMethod.invoke(processorInstance, node);
                    JsonObject dataJson = gson.toJsonTree(data).getAsJsonObject();
                    return UISchemaMapper.mapInputToUISchema(dataJson,
                            findUISchema(mediatorName, dataJson.get(Constant.UI_SCHEMA_NAME)));
                }
            }
        }
        return uiSchemaMap.get(mediatorName);
    }

    protected JsonObject findUISchema(String mediatorName, JsonElement uiSchemaName) {

        if (uiSchemaName != null) {

            // Replace _ with : in the UI schema name to match the UI schema map key
            String uiSchemaNameValue = uiSchemaName.getAsString().replace("_", ":");
            if (uiSchemaMap.containsKey(uiSchemaNameValue)) {
                return uiSchemaMap.get(uiSchemaNameValue).deepCopy();
            }
        }
        return uiSchemaMap.get(mediatorName).deepCopy();
    }

    private STNode getMediatorNodeAtPosition(DOMDocument document, Position position, Boolean isUpdate)
            throws BadLocationException {

        position = new Position(position.getLine(), position.getCharacter() + (isUpdate ? 1 : 0));
        int offset = document.offsetAt(position);
        DOMNode node = document.findNodeAt(offset);
        if (node == null || (node instanceof DOMElement && ((DOMElement) node).getEndTagOpenOffset() == offset)) {
            return null;
        }

        STNode mediator = MediatorFactoryFinder.getInstance().getMediator(node);
        if (mediator != null && !(mediator instanceof InvalidMediator)) {
            return mediator;
        }

        return null;
    }

    private JsonObject removeMediators(JsonObject mediatorList, List<String> mediatorsToRemove) {

        JsonObject filteredMediators = new JsonObject();
        for (String key : mediatorList.keySet()) {
            JsonObject mediatorObject = mediatorList.getAsJsonObject(key);
            if (mediatorObject.has(Constant.IS_CONNECTOR) && mediatorObject.get(Constant.IS_CONNECTOR).getAsBoolean()) {
                filteredMediators.add(key, mediatorObject);
            } else {
                JsonArray mediatorsArray = mediatorObject.getAsJsonArray(Constant.ITEMS);
                JsonArray filteredArray = new JsonArray();
                for (JsonElement element : mediatorsArray) {
                    JsonObject mediator = element.getAsJsonObject();
                    String tag = mediator.get(Constant.TAG).getAsString();
                    if (!mediatorsToRemove.contains(tag)) {
                        filteredArray.add(mediator);
                    }
                }
                JsonObject items = new JsonObject();
                items.add(Constant.ITEMS, filteredArray);
                filteredMediators.add(key, items);
            }
        }
        return filteredMediators;
    }

    private boolean isIterateMediator(DOMNode currentNode, List<String> iterateMediators) {

        if (currentNode != null && iterateMediators != null) {
            DOMNode node = currentNode.getNodeName().equals("#text") ? currentNode.getParentNode() : currentNode;
            if (node != null) {
                DOMNode parentNode = node.getParentNode();
                if (parentNode != null && iterateMediators.contains(parentNode.getNodeName())) {
                    return true;
                } else {
                    DOMNode grandParentNode = parentNode != null ? parentNode.getParentNode() : null;
                    if (grandParentNode != null && iterateMediators.contains(grandParentNode.getNodeName())) {
                        return true;
                    } else {
                        DOMNode greatGrandParentNode = grandParentNode != null ? grandParentNode.getParentNode() : null;
                        return greatGrandParentNode != null && iterateMediators.contains(greatGrandParentNode.getNodeName());
                    }

                }
            }
        }
        return false;
    }

    private boolean isAddedAbove(DOMNode currentNode, DOMNode nextNode, int offset) {
        if (currentNode instanceof DOMElement) {
            DOMElement tagElement = (DOMElement) currentNode;
            if (!tagElement.isSelfClosed() && tagElement.isInInsideStartEndTag(offset)) {
                return currentNode.hasChildNodes();
            } else {
                return nextNode != null;
            }
        }
        return false;
    }

    public JsonObject getUiSchema(String mediatorName, TextDocumentIdentifier documentIdentifier, Position position) {

        if (documentIdentifier != null && isRequestedForAgentTool(documentIdentifier.getUri(), position)) {
            return aiConnectorHandler.getToolSchema(mediatorName);
        } else if (uiSchemaMap.containsKey(mediatorName)) {
            return uiSchemaMap.get(mediatorName);
        } else if (mediatorName.contains(".")) {
            return getConnectorUiSchema(mediatorName, documentIdentifier, position);
        }
        return null;
    }

    public JsonObject getUiSchema(String mediatorName) {

        return getUiSchema(mediatorName, null, null);
    }

    private boolean isRequestedForAgentTool(String documentUri, Position position) {

        if (documentUri == null || position == null) {
            return false;
        }
        try {
            String documentPath = Utils.getAbsolutePath(documentUri);
            if (StringUtils.isEmpty(documentPath) || Files.notExists(Path.of(documentPath))) {
                return false;
            }
            DOMDocument document = Utils.getDOMDocument(new File(documentPath));
            int offset = document.offsetAt(position);
            DOMNode currentNode = document.findNodeAt(offset);
            if (isInsideAiAgent(currentNode)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while checking if the tool schema is requested.", e);
        }
        return false;
    }

    private boolean isInsideAiAgent(DOMNode currentNode) {

        // Need to check for <ai.agent><tools><tool/></tools></ai.agent> structure
        if (currentNode == null || currentNode.getParentNode() == null) {
            return true;
        }

        // Check if the parent is <ai.agent/>
        if (Constant.AI_AGENT_TAG.equals(currentNode.getParentNode().getNodeName())) {
            return true;
        }

        // Check if the grand-parent is <ai.agent/>
        return currentNode.getParentNode().getParentNode() != null &&
                Constant.AI_AGENT_TAG.equals(currentNode.getParentNode().getParentNode().getNodeName());
    }

    private JsonObject getConnectorUiSchema(String mediatorName, TextDocumentIdentifier documentIdentifier, Position position) {

        String connectorName = mediatorName.split("\\.")[0];
        String operationName = getConnectorOperationName(mediatorName, connectorName);
        if (connectorHolder.exists(connectorName)) {
            org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector
                    connector = connectorHolder.getConnector(connectorName);
            ConnectorAction operation = connector.getAction(operationName);
            if (operation != null) {
                String uiSchemaPath = operation.getUiSchemaPath();
                try {
                    JsonObject uiSchemaObject = Utils.getJsonObject(Utils.readFile(new File(uiSchemaPath)));
                    boolean isTryoutSupported = Utils.compareVersions(miServerVersion, Constant.MI_430_VERSION) > 0;
                    uiSchemaObject.addProperty(Constant.CAN_TRY_OUT, isTryoutSupported);
                    JsonObject resultObject = new JsonObject();
                    if (isTryoutSupported && documentIdentifier != null && position != null) {
                        resultObject.addProperty(Constant.RESPONSE_VARIABLE, generateResponseVariableDefaultValue(documentIdentifier, position, connectorName, operationName));
                    }
                    return mapInputToUISchema(resultObject, uiSchemaObject);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error occurred while retrieving UI schema for connector operation.", e);
                }
            }

        }
        return null;
    }

    public boolean isInitialized() {

        return isInitialized;
    }

    public void reloadMediatorList(String projectServerVersion) {

        try {
            this.mediatorList = Utils.getMediatorList(projectServerVersion, connectorHolder);
            this.agentToolList = Utils.getAgentToolList(mediatorList, connectorHolder);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to reload mediators.", e);
        }
    }

    /**
     * Retrieves the Mustache template for the given mediator.
     *
     * @param key the key of the template
     * @return
     */
    protected Mustache getMustacheTemplate(String key) {

        return templateMap.get(key);
    }

    private String buildNestedArray(Object value) {

        return buildNestedArray(value, 0);
    }

    private String buildNestedArray(Object value, int depth) {

        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Maximum JSON nesting depth exceeded");
        }

        // Unwrap LinkedTreeMap layers
        while (value instanceof LinkedTreeMap) {
            LinkedTreeMap map = (LinkedTreeMap) value;
            if (map.get(Constant.VALUE) != null) {
                value = map.get(Constant.VALUE);
            } else {
                break;
            }
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(buildNestedArray(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        if (value instanceof String) {
            return "\"" + ((String) value).replace("\"", "\\\"") + "\"";
        }

        return String.valueOf(value);
    }
	
	private String getConnectorOperationName(String mediatorName, String connectorName) {

		String[] mediatorNameParts = mediatorName.split("\\.");
		// The legacy series of the utility module has its operations in the utility.operation.method format
        return (Constant.UTILITY.equalsIgnoreCase(connectorName) && mediatorNameParts.length == 3) ? 
			mediatorNameParts[1] + Constant.DOT + mediatorNameParts[2]: mediatorNameParts[1];
	}
}
