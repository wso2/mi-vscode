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

package org.eclipse.lemminx.customservice.synapse.expression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.Functions;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.HelperPanelData;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.HelperPanelItem;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.ServerLessTryoutHandler;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Edit;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Properties;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionHelperProvider {

    private static final Map<Properties.Type, String> PROPERTIES_SECOND_LEVEL = Map.of(
            Properties.Type.SYNAPSE, "properties.synapse",
            Properties.Type.AXIS2, "properties.axis2");
    private static final Map<Params.Type, String> PARAMS_SECOND_LEVEL = Map.of(
            Params.Type.QUERY, "params.queryParams",
            Params.Type.PATH, "params.pathParams",
            Params.Type.FUNC, "params.functionParams");
    private final ServerLessTryoutHandler tryoutHandler;
    private final String projectPath;

    public ExpressionHelperProvider(String projectPath) {

        this.projectPath = projectPath;
        this.tryoutHandler = new ServerLessTryoutHandler(projectPath);
    }

    /**
     * Get the expression helper panel data for the requested mediator.
     *
     * @param param ExpressionParam
     * @return HelperPanelData
     */
    public HelperPanelData getExpressionHelperData(ExpressionParam param) {

        if (!ExpressionCompletionUtils.isValidRequest(param)) {
            return getBasicHelperData();
        }
        if (param.getNeedLastMediator()) {
            return getLastMediatorHelperData(param);
        }
        try {
            boolean isNewMediator = isNewMediator(param);
            String payload =
                    ExpressionCompletionUtils.getInputPayload(projectPath, param.getDocumentUri(), param.getPosition());
            if (param.getPosition() == null) {
                return getBasicHelperData();
            }
            MediatorTryoutRequest request =
                    new MediatorTryoutRequest(param.getDocumentUri(), param.getPosition().getLine(),
                            param.getPosition().getCharacter(), payload, null);
            MediatorTryoutInfo tryoutInfo = getMediatorTryoutInfo(request, isNewMediator);
            MediatorInfo propsData = isNewMediator ? tryoutInfo.getInput() : tryoutInfo.getInput();
            return createHelperData(propsData, ExpressionCompletionUtils.getFunctions());
        } catch (BadLocationException | IOException e) {
            return getBasicHelperData();
        }
    }

    /**
     * Get the expression helper panel data for the last mediator of the requested mediation sequence.
     *
     * @param param ExpressionParam
     * @return HelperPanelData
     */
    public HelperPanelData getLastMediatorHelperData(ExpressionParam param) {

        if (!ExpressionCompletionUtils.isValidRequest(param)) {
            return getBasicHelperData();
        }
        try {
            Path documentTruePath = Path.of(Utils.getAbsolutePath(param.getDocumentUri()));
            Position position =
                    ExpressionCompletionUtils.getLastMediatorPosition(documentTruePath.toString(), param.getPosition());
            return getExpressionHelperData(new ExpressionParam(documentTruePath.toString(), position));
        } catch (IOException e) {
            return getBasicHelperData();
        }
    }

    private boolean isNewMediator(ExpressionParam param) throws IOException, BadLocationException {

        Position mediatorPosition =
                ExpressionCompletionUtils.getMediatorPosition(param.getDocumentUri(), param.getPosition());
        return mediatorPosition != param.getPosition();
    }

    private HelperPanelData getBasicHelperData(String payload) {

        HelperPanelData helperData = getBasicHelperData();
        helperData.setPayload(createDataList(new JsonPrimitive(payload)));
        return helperData;
    }

    private HelperPanelData getBasicHelperData() {

        HelperPanelData helperData = new HelperPanelData();
        List<Property> configurables = ExpressionCompletionUtils.getConfigs(projectPath);
        helperData.setConfigs(createDataList(configurables, ExpressionConstants.CONFIGS));
        setFunctions(helperData, ExpressionCompletionUtils.getFunctions());
        return helperData;
    }

    private MediatorTryoutInfo getMediatorTryoutInfo(MediatorTryoutRequest request, boolean isNewMediator) {

        //If it is a new mediator, add a dummy log mediator to get the tryout info
        if (isNewMediator) {
            Position position = new Position(request.getLine(), request.getColumn());
            Edit edit = new Edit("<log />", new Range(position, position));
            Edit[] editArray = new Edit[1];
            editArray[0] = edit;
            request = new MediatorTryoutRequest(request.getFile(), request.getLine(), request.getColumn() + 1,
                    request.getInputPayload(), editArray);
        }
        MediatorTryoutInfo info = tryoutHandler.handle(request);
        List<Property> configurables = ExpressionCompletionUtils.getConfigs(projectPath);
        info.setInputConfigs(configurables);
        info.setOutputConfigs(configurables);
        return info;
    }

    private HelperPanelData createHelperData(MediatorInfo propsData, Map<String, Functions> functions) {

        HelperPanelData helperData = new HelperPanelData();
        setFunctions(helperData, functions);
        helperData.setVariables(createDataList(propsData.getVariables(), ExpressionConstants.VARS));
        helperData.setPayload(createDataList(propsData.getPayload()));
        helperData.setProperties(createDataList(propsData.getProperties()));
        helperData.setParams(createDataList(propsData.getParams()));
        helperData.setConfigs(createDataList(propsData.getConfigs(), ExpressionConstants.CONFIGS));
        helperData.setHeaders(createDataList(propsData.getHeaders(), ExpressionConstants.HEADERS));
        return helperData;
    }

    private void setFunctions(HelperPanelData helperData, Map<String, Functions> functions) {

        Map<String, Functions> clonedFunctionsMap = new HashMap<>();
        for (Map.Entry<String, Functions> entry : functions.entrySet()) {
            String key = entry.getKey();
            Functions value = entry.getValue();
            clonedFunctionsMap.put(key, value.deepCopy());
        }
        helperData.setFunctions(clonedFunctionsMap);
    }

    private List<CompletionItem> createDataList(Properties attributes) {

        List<CompletionItem> dataList = new ArrayList<>();

        for (Map.Entry<Properties.Type, String> entry : PROPERTIES_SECOND_LEVEL.entrySet()) {
            String label = Utils.toCamelCase(entry.getKey().toString());
            HelperPanelItem item = new HelperPanelItem(label, entry.getValue());
            item.addChildren(createDataList(attributes.getPropertiesByType(entry.getKey()), entry.getValue()));
            dataList.add(item);
        }
        return dataList;
    }

    private List<CompletionItem> createDataList(Params params) {

        List<CompletionItem> dataList = new ArrayList<>();
        for (Map.Entry<Params.Type, String> entry : PARAMS_SECOND_LEVEL.entrySet()) {
            String label = Utils.toCamelCase(entry.getKey().toString());
            HelperPanelItem item = new HelperPanelItem(label, entry.getValue());
            item.addChildren(createDataList(params.getPropertiesByType(entry.getKey()), entry.getValue()));
            dataList.add(item);
        }
        return dataList;
    }

    private List<CompletionItem> createDataList(JsonPrimitive payload) {

        List<CompletionItem> dataList = new ArrayList<>();
        if (payload != null) {
            JsonElement element = Utils.getJsonElement(payload.getAsString());
            if (element != null) {
                CompletionItem item = new HelperPanelItem(ExpressionConstants.PAYLOAD, ExpressionConstants.PAYLOAD);
                ((HelperPanelItem) item).addChildren(addJsonChildren(element, ExpressionConstants.PAYLOAD));
                dataList.add(item);
            }
        }
        return dataList;
    }

    private List<CompletionItem> createDataList(List<Property> properties, String expressionPrefix) {

        List<CompletionItem> dataList = new ArrayList<>();
        for (Property variable : properties) {
            String variableName = variable.getKey();
            String referenceValue = variableName.contains(Constant.DOT) || variableName.contains(Constant.SPACE) ?
                    "[\"" + variableName + "\"]" : variableName;
            String expression = expressionPrefix + Constant.DOT + referenceValue;
            HelperPanelItem item = new HelperPanelItem(variable.getKey(), expression);
            String value = variable.getValue();
            if (variable.getProperties() != null && !variable.getProperties().isEmpty()) {
                item.addChildren(createDataList(variable.getProperties(), expression));
            } else if (value != null) {
                item.addChildren(addJsonChildren(Utils.getJsonElement(value), expression));
            }
            dataList.add(item);
        }
        return dataList;
    }

    private List<CompletionItem> addJsonChildren(JsonElement jsonObject, String expressionPrefix) {

        List<CompletionItem> dataList = new ArrayList<>();
        if (jsonObject != null) {
            if (jsonObject.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : jsonObject.getAsJsonObject().entrySet()) {
                    String expressionSuffix = getExpressionSuffix(entry.getKey());
                    String expression = expressionPrefix + expressionSuffix;
                    HelperPanelItem item = new HelperPanelItem(entry.getKey(), expression);
                    JsonElement value = entry.getValue();
                    item.addChildren(addJsonChildren(value, expression));
                    dataList.add(item);
                }
            } else if (jsonObject.isJsonArray()) {
                JsonArray jsonArray = jsonObject.getAsJsonArray();
                expressionPrefix = expressionPrefix + ExpressionConstants.ARRAY_COMPLETION_INSERT_TEXT;
                HelperPanelItem item =
                        new HelperPanelItem(ExpressionConstants.ARRAY_COMPLETION_LABEL, expressionPrefix);
                item.addChildren(addJsonChildren(jsonArray.get(0), expressionPrefix));
                dataList.add(item);
            }
        }
        return dataList;
    }

    private String getExpressionSuffix(String key) {

        if (key.contains(Constant.DOT) || key.contains(StringUtils.SPACE)) {
            return "[\"" + key + "\"]";
        }
        return Constant.DOT + key;
    }
}
