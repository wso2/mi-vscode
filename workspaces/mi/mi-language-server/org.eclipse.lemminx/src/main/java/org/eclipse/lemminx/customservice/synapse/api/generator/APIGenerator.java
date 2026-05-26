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

package org.eclipse.lemminx.customservice.synapse.api.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.CommentMediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.APIResource;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.ApiVersionType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Loopback;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.Respond;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.MediaType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.PayloadFactoryFormat;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.payload.TemplateType;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.api.APISerializer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Source: https://github.com/wso2/carbon-mediation/blob/master/components/mediation-commons/src/main/java/org/wso2/carbon/mediation/commons/rest/api/swagger/APIGenerator.java

/**
 * This class will generate synapse Rest API configuration skeleton from swagger definition
 */
public class APIGenerator {

    private JsonObject swaggerJson;
    private String publishSwaggerPath;
    private static final Logger log = Logger.getLogger(APIGenerator.class.getName());

    public APIGenerator(JsonObject swaggerJson, String publishSwaggerPath) {

        this.swaggerJson = swaggerJson;
        this.publishSwaggerPath = publishSwaggerPath;
    }

    public String generateSynapseAPIXml() {

        String apiXml = null;
        try {
            API api = generateSynapseAPI();
            if (publishSwaggerPath != null) {
                api.setPublishSwagger(publishSwaggerPath);
            }
            apiXml = APISerializer.serializeAPI(api);
        } catch (APIGenException e) {
        }
        return apiXml;
    }

    /**
     * Generate API from provided swagger definition
     *
     * @return Generated API
     * @throws APIGenException
     */
    public API generateSynapseAPI() throws APIGenException {

        String apiContext;
        if (swaggerJson.get(SwaggerConstants.SERVERS) == null ||
                swaggerJson.get(SwaggerConstants.SERVERS).getAsJsonArray().size() == 0) {
            apiContext = SwaggerConstants.DEFAULT_CONTEXT;
        } else {
            JsonObject firstServer = swaggerJson.getAsJsonArray(SwaggerConstants.SERVERS).get(0).getAsJsonObject();
            // get the first path in the servers section
            String serversString = firstServer.get(SwaggerConstants.URL).getAsString();
            if (serversString.contains("{") && serversString.contains("}")) {
                // url is templated, need to resolve
                if (firstServer.has(SwaggerConstants.VARIABLES)) {
                    JsonObject variables = firstServer.get(SwaggerConstants.VARIABLES).getAsJsonObject();
                    serversString = replaceTemplates(serversString, variables);
                } else {
                    throw new APIGenException("Server url is templated, but variables cannot be found");
                }
            }
            try {
                URL url = new URL(serversString);
                apiContext = url.getPath();
            } catch (MalformedURLException e) {
                // url can be relative the place where the swagger is hosted.
                apiContext = serversString;
            }
            if (apiContext.isEmpty() || "/".equals(apiContext)) {
                apiContext = SwaggerConstants.DEFAULT_CONTEXT;
            }
            //cleanup context : remove ending '/'
            if (apiContext.lastIndexOf('/') == (apiContext.length() - 1)) {
                apiContext = apiContext.substring(0, apiContext.length() - 1);
            }
            // add leading / if not exists
            if (!apiContext.startsWith("/")) {
                apiContext = "/" + apiContext;
            }
        }

        if (swaggerJson.get(SwaggerConstants.INFO) == null) {
            throw new APIGenException("The \"info\" section of the swagger definition is mandatory for API generation");
        }
        JsonObject swaggerInfo = swaggerJson.getAsJsonObject(SwaggerConstants.INFO);
        if (swaggerInfo.get(SwaggerConstants.TITLE) == null ||
                swaggerInfo.get(SwaggerConstants.TITLE).getAsString().isEmpty()) {
            throw new APIGenException("The title of the swagger definition is mandatory for API generation");
        }

        String apiName = swaggerInfo.get(SwaggerConstants.TITLE).getAsString();

        // Extract version information
        ApiVersionType versionType = null;
        String version = "";
        JsonElement swaggerVersionElement = swaggerInfo.get(SwaggerConstants.VERSION);
        if (swaggerVersionElement != null && swaggerVersionElement.isJsonPrimitive() &&
                swaggerVersionElement.getAsJsonPrimitive().isString()) {
            version = swaggerVersionElement.getAsString();
            if (apiContext.endsWith(version)) {
                // If the base path ends with the version, then it will be considered as version-type=url
                versionType = ApiVersionType.url;
                //cleanup api context path : remove version from base path
                apiContext = apiContext.substring(0, apiContext.length() - version.length() - 1);
            } else {
                // otherwise context based version strategy
                versionType = ApiVersionType.context;
            }
        }

        // Create API
        API genAPI = new API();
        genAPI.setName(apiName);
        genAPI.setContext(apiContext);
        genAPI.setVersionType(versionType);
        genAPI.setVersion(version);

        if (swaggerJson.get(SwaggerConstants.PATHS) != null) {
            JsonObject pathsObj = swaggerJson.getAsJsonObject(SwaggerConstants.PATHS);
            for (Map.Entry<String, JsonElement> pathEntry : pathsObj.entrySet()) {
                if (pathEntry.getValue() instanceof JsonObject) {
                    createResource(pathEntry.getKey(), pathEntry.getValue().getAsJsonObject(), genAPI, null);
                }
            }
        }

        String apiElement = APISerializer.serializeAPI(genAPI);
        if (log.isLoggable(Level.FINE)) {
            log.info("API generation completed : " + genAPI.getName() + " API: " + apiElement);
        }
        return genAPI;
    }

    /**
     * Resolve templated URLs. Ex: https://{customerId}.saas-app.com:{port}/v2/gggg
     *
     * @param input     Input template URL.
     * @param variables OpenAPI variables definition.
     * @return Resolved URL.
     * @throws APIGenException Error occurred while replacing the template values.
     */
    private String replaceTemplates(String input, JsonObject variables) throws APIGenException {

        Matcher m = Pattern.compile(SwaggerConstants.TEMPLATE_REGEX).matcher(input);
        while (m.find()) {
            String temp = m.group(1);
            if (variables.has(temp) && variables.get(temp).getAsJsonObject().has(SwaggerConstants.DEFAULT_VALUE)) {
                String realValue =
                        variables.get(temp).getAsJsonObject().get(SwaggerConstants.DEFAULT_VALUE).getAsString();
                input = input.replace("{" + temp + "}", realValue);
            } else {
                throw new APIGenException("Variables cannot be found to replace the value " + "{" + temp + "}");
            }
        }
        return input;
    }

    /**
     * Generate API from provided swagger definition referring to the old API.
     *
     * @param existingAPI old API
     * @return Generated API
     * @throws APIGenException
     */
    public API generateSynapseAPI(API existingAPI) throws APIGenException {

        String apiContext;
        if (swaggerJson.get(SwaggerConstants.SERVERS) == null ||
                swaggerJson.get(SwaggerConstants.SERVERS).getAsJsonArray().size() == 0) {
            apiContext = SwaggerConstants.DEFAULT_CONTEXT;
        } else {
            // get the first path in the servers section
            String serversString =
                    swaggerJson.getAsJsonArray(SwaggerConstants.SERVERS).get(0).getAsJsonObject()
                            .get(SwaggerConstants.URL).getAsString();
            try {
                URL url = new URL(serversString);
                apiContext = url.getPath();
            } catch (MalformedURLException e) {
                // url can be relative the place where the swagger is hosted.
                apiContext = serversString;
            }
        }
        //cleanup context : remove ending '/'
        if (apiContext.lastIndexOf('/') == (apiContext.length() - 1)) {
            apiContext = apiContext.substring(0, apiContext.length() - 1);
        }
        // add leading / if not exists
        if (!apiContext.startsWith("/")) {
            apiContext = "/" + apiContext;
        }

        if (swaggerJson.get(SwaggerConstants.INFO) == null) {
            throw new APIGenException("The \"info\" section of the swagger definition is mandatory for API generation");
        }
        JsonObject swaggerInfo = swaggerJson.getAsJsonObject(SwaggerConstants.INFO);
        if (swaggerInfo.get(SwaggerConstants.TITLE) == null ||
                swaggerInfo.get(SwaggerConstants.TITLE).getAsString().isEmpty()) {
            throw new APIGenException("The title of the swagger definition is mandatory for API generation");
        }

        String apiName = swaggerInfo.get(SwaggerConstants.TITLE).getAsString();

        // Extract version information
        ApiVersionType versionType = null;
        String version = "";
        JsonElement swaggerVersionElement = swaggerInfo.get(SwaggerConstants.VERSION);
        if (swaggerVersionElement != null && swaggerVersionElement.isJsonPrimitive() &&
                swaggerVersionElement.getAsJsonPrimitive().isString()) {
            version = swaggerVersionElement.getAsString();
            if (apiContext.endsWith(version)) {
                // If the base path ends with the version, then it will be considered as version-type=url
                versionType = ApiVersionType.url;
                //cleanup api context path : remove version from base path
                apiContext = apiContext.substring(0, apiContext.length() - version.length() - 1);
            } else {
                // otherwise context based version strategy
                versionType = ApiVersionType.context;
            }
        }

        // Create API
        API genAPI = new API();
        genAPI.setName(apiName);
        genAPI.setContext(apiContext);
        genAPI.setVersionType(versionType);
        genAPI.setVersion(version);

        if (swaggerJson.get(SwaggerConstants.PATHS) != null) {
            JsonObject pathsObj = swaggerJson.getAsJsonObject(SwaggerConstants.PATHS);
            for (Map.Entry<String, JsonElement> pathEntry : pathsObj.entrySet()) {
                if (pathEntry.getValue() instanceof JsonObject) {
                    createResource(pathEntry.getKey(), pathEntry.getValue().getAsJsonObject(), genAPI, existingAPI);
                }
            }
        }

        String apiElement = APISerializer.serializeAPI(genAPI);
        if (log.isLoggable(Level.FINE)) {
            log.info("API generation completed : " + genAPI.getName() + " API: " + apiElement);
        }
        return genAPI;
    }

    /**
     * Function to create resource from swagger definition.
     *
     * @param path        path of the resource
     * @param resourceObj json representation of resource
     * @param genAPI      generated API
     * @param existingAPI old API
     */
    private void createResource(String path, JsonObject resourceObj, API genAPI, API existingAPI)
            throws APIGenException {

        boolean noneURLStyleAdded = false;
        List<APIResource> resources = new ArrayList<>();
        if (existingAPI != null) {
            for (APIResource resource : existingAPI.getResource()) {
                String resourceMapping;
                if (resource.getUrlMapping() != null) {
                    resourceMapping = resource.getUrlMapping();
                } else {
                    resourceMapping = resource.getUriTemplate();
                }
                // Getting all the resources whose path matches
                if (path.equals(resourceMapping)) {
                    resources.add(resource);
                }
            }
        }
        int i = 0;
        // Same number is assigned to all the method in the same resource of the existing API
        HashMap<String, Integer> methodMapping = new HashMap<>();
        HashMap<Integer, APIResource> createdResources = new HashMap<>();
        for (APIResource resource : resources) {
            for (String method : resource.getMethods()) {
                methodMapping.put(method, i);
            }
            i++;
        }

        List<APIResource> generatedResources;
        if (genAPI.getResource() != null) {
            generatedResources = new ArrayList<>(Arrays.asList(genAPI.getResource()));
        } else {
            generatedResources = new ArrayList<>();
        }
        for (Map.Entry<String, JsonElement> methodEntry : resourceObj.entrySet()) {
            // Skip the parameters section at the path level
            if (SwaggerConstants.PARAMETERS.equals(methodEntry.getKey())) {
                continue;
            }
            if (log.isLoggable(Level.FINE)) {
                log.info("Generating resource for path : " + path + ", method : " + methodEntry.getKey());
            }

            String methodName = methodEntry.getKey().toUpperCase();
            if (methodMapping.containsKey(methodName)) {
                APIResource createdResource = createdResources.get(methodMapping.get(methodName));
                // Check if a resource was created for another method belongs to the same resource.
                if (createdResource != null) {
                    createdResource.addMethod(methodName);
                    continue;
                }
            }

            // Create a new resource for each method.
            APIResource resource = new APIResource();
            resource.addMethod(methodName);

            // Identify URL Mapping and template and create relevant helper
            Matcher matcher = SwaggerConstants.PATH_PARAMETER_PATTERN.matcher(path);
            ArrayList<String> pathParamList = new ArrayList<>();
            while (matcher.find()) {
                pathParamList.add(matcher.group(1));
            }
            if (pathParamList.isEmpty()) {
                // if the path is '/' then it should have none URL style
                if (!"/".equals(path) || noneURLStyleAdded) {
                    resource.setUrlMapping(path);
                }
                if ("/".equals(path)) {
                    noneURLStyleAdded = true;
                }
            } else {
                resource.setUriTemplate(path);
            }

            resource.setInSequence(APIGenerator.getDefaultInSequence(pathParamList));
            generatedResources.add(resource);

            if (methodMapping.containsKey(methodName)) {
                createdResources.put(methodMapping.get(methodName), resource);
            }
        }
        genAPI.setResource(generatedResources.toArray(new APIResource[generatedResources.size()]));

    }

    /**
     * Function to create default in sequence
     *
     * @return template API in-sequence
     */
    private static Sequence getDefaultInSequence(List<String> pathParams) {

        Sequence defaultInSeq = new Sequence();

        CommentMediator generatedComment = new CommentMediator();
        generatedComment.setCommentText("This is generated API skeleton.");
        defaultInSeq.addToMediatorList(generatedComment);

        if (pathParams != null && pathParams.size() > 0) {
            // Create populate properties reading path parameters
            for (String param : pathParams) {
                Property propertyMediator = new Property();
                propertyMediator.setExpression("get-property('uri.var." + param + "')");
                propertyMediator.setName(param);
                defaultInSeq.addToMediatorList(propertyMediator);
            }
        }

        CommentMediator logicGoesHereComment = new CommentMediator();
        logicGoesHereComment.setCommentText("Business Logic Goes Here");
        defaultInSeq.addToMediatorList(logicGoesHereComment);

        PayloadFactory defaultPayload = new PayloadFactory();
        defaultPayload.setTemplateType(TemplateType.DEFAULT);
        defaultPayload.setMediaType(MediaType.json);
        PayloadFactoryFormat defaultPayloadFormat = new PayloadFactoryFormat();
        defaultPayloadFormat.setContent("{\"Response\" : \"Sample Response\"}");
        defaultPayload.setFormat(defaultPayloadFormat);
        defaultInSeq.addToMediatorList(defaultPayload);

        defaultInSeq.addToMediatorList(new Respond());
        return defaultInSeq;
    }
}
