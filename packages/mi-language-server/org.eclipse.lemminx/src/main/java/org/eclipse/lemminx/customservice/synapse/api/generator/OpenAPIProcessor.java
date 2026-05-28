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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.parser.AdvanceDetails;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.API;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.api.ApiVersionType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Source: https://github.com/wso2/carbon-mediation/blob/master/components/mediation-commons/src/main/java/org/wso2/carbon/mediation/commons/rest/api/swagger/OpenAPIProcessor.java

/**
 * This class will generate the OAS 3.0 definition of the given synapse API.
 */
public class OpenAPIProcessor {

    private API api;
    private static final Logger log = Logger.getLogger(OpenAPIProcessor.class.getName());

    public OpenAPIProcessor(final API api) {

        this.api = api;
    }

    /**
     * Generate the OAS 3.0 definition for a given API.
     *
     * @param isJSON response data type JSON / YAML.
     * @return OpenAPI definition as string.
     */
    public String getOpenAPISpecification(final boolean isJSON, final String hostname, final int port) {

        final OpenAPI openAPI = new OpenAPI();
        addInfoSection(openAPI);
        addServersSection(openAPI, hostname, port, StringUtils.EMPTY);
        // Re-use the previous implementation to get resource details of the API.
        final Map<String, Object> dataMap = GenericApiObjectDefinition.getPathMap(api);
        Paths paths = new Paths();

        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            PathItem pathItem = new PathItem();
            Map<String, Object> methodMap = (Map<String, Object>) entry.getValue();
            populateParameters(pathItem, methodMap);
            paths.put(entry.getKey(), pathItem);
        }
        openAPI.setPaths(paths);
        try {
            if (isJSON) {
                return Json.mapper().writeValueAsString(openAPI);
            }
            return Yaml.mapper().writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.log(Level.SEVERE, "Error occurred while creating the output JAML/JSON");
            return null;
        }
    }

    /**
     * Add path / query / body parameter details to the OpenApi.
     *
     * @param operation Operation object.
     * @param paramType Type of the parameter ( path / query / body ).
     * @param paramName Name of the parameter.
     */
    private void addPathQueryAndBodyParams(Operation operation, String paramType, String paramName) {

        switch (paramType) {
            case SwaggerConstants.PARAMETER_IN_PATH:
                PathParameter pathParameter = new PathParameter();
                pathParameter.setName(paramName);
                pathParameter.setSchema(new StringSchema());
                operation.addParametersItem(pathParameter);
                break;
            case SwaggerConstants.PARAMETER_IN_QUERY:
                QueryParameter queryParameter = new QueryParameter();
                queryParameter.setName(paramName);
                queryParameter.setSchema(new StringSchema());
                operation.addParametersItem(queryParameter);
                break;
            case SwaggerConstants.PARAMETER_IN_BODY:
                RequestBody requestBody = new RequestBody();
                requestBody.description("Sample Payload");
                requestBody.setRequired(false);

                // Add json media type for the request body
                MediaType jsonMediaType = new MediaType();
                Schema bodySchema = new Schema();
                bodySchema.setType("object");
                Map<String, Schema> inputProperties = new HashMap<>();
                ObjectSchema objectSchema = new ObjectSchema();
                bodySchema.setProperties(inputProperties);
                inputProperties.put("payload", objectSchema);
                jsonMediaType.setSchema(bodySchema);
                Content content = new Content();
                content.addMediaType("application/json", jsonMediaType);

                // Add xml media type for the request body
                MediaType xmlMediaType = new MediaType();
                Schema xmlBodySchema = new Schema();
                xmlBodySchema.setType("object");
                xmlBodySchema.setXml(new XML());
                xmlBodySchema.getXml().setName("payload");
                xmlMediaType.setSchema(xmlBodySchema);
                content.addMediaType("application/xml", xmlMediaType);
                requestBody.setContent(content);
                operation.setRequestBody(requestBody);
                break;
        }
    }

    /**
     * Update server details in the OpenApi definition.
     *
     * @param openAPI OpenApi object.
     */
    private void updateServersSection(OpenAPI openAPI) {

        List<Server> servers = openAPI.getServers();
        if (servers.size() > 0) {

            String basePath;
            if (ApiVersionType.url.equals(api.getVersionType())) {
                basePath = api.getContext() + "/" + api.getVersion();
            } else {
                basePath = api.getContext();
            }

            // When servers are already configured check for context change only
            String urlString;
            for (Server server : servers) {
                urlString = server.getUrl();
                try {
                    URL url = new URL(urlString);
                    String apiContext = url.getPath();
                    if (!basePath.equals(apiContext)) {
                        server.setUrl(urlString.replace(apiContext, basePath));
                    }
                } catch (MalformedURLException e) {
                    // URL is relative to the host
                    if (!basePath.equals(urlString)) {
                        server.setUrl(basePath);
                    }
                }
            }
        } else {
            addServersSection(openAPI, SwaggerConstants.DEFAULT_HOST, SwaggerConstants.DEFAULT_HTTP_PORT, StringUtils.EMPTY);
        }
    }

    /**
     * Add server details to the OpenApi definition.
     *
     * @param openAPI OpenApi object.
     */
    private void addServersSection(OpenAPI openAPI, String hostname, int port, String projectPath) {

        String serverVersionPath = api.getContext().startsWith("/") ? "" : "/";
        if (StringUtils.isNotBlank(projectPath)){
            OverviewPageDetailsResponse overviewPageDetailsResponse = new OverviewPageDetailsResponse();
            PomParser.getPomDetails(projectPath, overviewPageDetailsResponse);
            AdvanceDetails advanceDetails = overviewPageDetailsResponse.getBuildDetails().getAdvanceDetails();
            serverVersionPath = "/" + advanceDetails.getProjectGroupId().getValue() + "/" + advanceDetails.getProjectArtifactId().getValue()
                    + "/" + overviewPageDetailsResponse.getPrimaryDetails().getProjectVersion().getValue()
                    + serverVersionPath;
        }

        String basePath;
        if (ApiVersionType.url.equals(api.getVersionType())) {
            basePath = api.getContext() + "/" + api.getVersion();
        } else {
            basePath = api.getContext();
        }
        String httpHost, httpsHost;
        int offset = SwaggerConstants.DEFAULT_HTTP_PORT - port;
        if (StringUtils.isNotBlank(api.getHostname()) && !"-1".equals(api.getPort())) {
            httpHost = api.getHostname() + ":" + port;
            httpsHost = api.getHostname() + ":" + (SwaggerConstants.DEFAULT_HTTPS_PORT + offset);
        } else if (StringUtils.isNotBlank(hostname)) {
            httpHost = hostname + ":" + port;
            httpsHost = hostname + ":" + (SwaggerConstants.DEFAULT_HTTPS_PORT + offset);
        } else {
            httpHost = SwaggerConstants.DEFAULT_HOST + ":" + port;
            httpsHost = SwaggerConstants.DEFAULT_HOST + ":" + (SwaggerConstants.DEFAULT_HTTPS_PORT + offset);
        }
        Server httpServer = new Server();
        httpServer.setUrl(SwaggerConstants.PROTOCOL_HTTP + "://" + httpHost + serverVersionPath + basePath);
        Server httpsServer = new Server();
        httpsServer.setUrl(SwaggerConstants.PROTOCOL_HTTPS + "://" + httpsHost + serverVersionPath + basePath);
        openAPI.setServers(Arrays.asList(httpServer, httpsServer));
    }

    /**
     * Update info details of the OpenApi definition.
     *
     * @param openAPI OpenApi object.
     */
    private void updateInfoSection(OpenAPI openAPI) {

        final Info info = openAPI.getInfo();
        info.setTitle(api.getName());
        if (StringUtils.isEmpty(info.getDescription())) {
            info.setDescription(api.getDescription() == null ? ("API Definition of " + api.getName()) :
                    api.getDescription());
        }
        info.setVersion((api.getVersion() != null && !api.getVersion().equals("")) ? api.getVersion() : "1.0.0");
        openAPI.setInfo(info);
    }

    /**
     * Add info details of the OpenApi definition.
     *
     * @param openAPI OpenApi object.
     */
    private void addInfoSection(OpenAPI openAPI) {

        final Info info = new Info();
        info.setTitle(api.getName());
        info.setDescription(api.getDescription() == null ? ("API Definition of " + api.getName()) :
                api.getDescription());
        info.setVersion((api.getVersion() != null && !api.getVersion().equals("")) ? api.getVersion() : "1.0.0");
        openAPI.setInfo(info);
    }

    /**
     * Add the default response ( since we cannot define it ) and pathItems to path map
     *
     * @param pathItem    PathItem contains the resource details.
     * @param operation   Operation object that contains params (path, query and body)
     * @param methodEntry Context fof the resource.
     */
    private void addDefaultResponseAndPathItem(PathItem pathItem, Operation operation, Map.Entry methodEntry) {

        ApiResponses apiResponses = new ApiResponses();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("Default response");
        apiResponses.addApiResponse("default", apiResponse);
        operation.setResponses(apiResponses);

        switch ((String) methodEntry.getKey()) {
            case SwaggerConstants.OPERATION_HTTP_GET:
                pathItem.setGet(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_POST:
                pathItem.setPost(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_DELETE:
                pathItem.setDelete(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_PUT:
                pathItem.setPut(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_HEAD:
                pathItem.setHead(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_PATCH:
                pathItem.setPatch(operation);
                break;
            case SwaggerConstants.OPERATION_HTTP_OPTIONS:
                pathItem.setOptions(operation);
                break;
        }
    }

    /**
     * Add the default request body
     *
     * @param operation   Operation object that contains params (path, query and body)
     * @param methodEntry Context fof the resource.
     */
    private void addDefaultRequestBody(Operation operation, Map.Entry methodEntry) {

        if (operation.getRequestBody() == null) {
            switch ((String) methodEntry.getKey()) {
                case SwaggerConstants.OPERATION_HTTP_POST:
                case SwaggerConstants.OPERATION_HTTP_PUT:
                case SwaggerConstants.OPERATION_HTTP_PATCH:
                    RequestBody requestBody = new RequestBody();
                    requestBody.description("Sample Payload");
                    requestBody.setRequired(false);

                    MediaType mediaType = new MediaType();
                    Schema bodySchema = new Schema();
                    bodySchema.setType("object");

                    Map<String, Schema> inputProperties = new HashMap<>();
                    ObjectSchema objectSchema = new ObjectSchema();

                    bodySchema.setProperties(inputProperties);
                    inputProperties.put("payload", objectSchema);
                    mediaType.setSchema(bodySchema);
                    Content content = new Content();
                    content.addMediaType("application/json", mediaType);
                    requestBody.setContent(content);
                    operation.setRequestBody(requestBody);
                    break;
            }
        }
    }

    /**
     * Update a given swagger definition of the Synapse API.
     *
     * @param existingSwagger swagger definition needs to be updated.
     * @param isJSONIn        input swagger data type JSON / YAML.
     * @param isJSONOut       output swagger data type JSON / YAML.
     * @return updated swagger definition as string.
     */
    public String getUpdatedSwaggerFromApi(String existingSwagger, boolean isJSONIn, boolean isJSONOut,
                                           String hostname, int port, String projectPath)
            throws APIGenException {

        if (api == null) {
            throw new APIGenException("Provided API is null");
        }
        if (StringUtils.isEmpty(existingSwagger)) {
            throw new APIGenException("Provided swagger definition is empty");
        }

        if (isJSONIn) {
            JsonNode jsonNodeTree = null;
            try {
                jsonNodeTree = new ObjectMapper().readTree(existingSwagger);
                existingSwagger = new YAMLMapper().writeValueAsString(jsonNodeTree);
            } catch (JsonProcessingException e) {
                throw new APIGenException("Error occurred while converting the swagger to YAML format", e);
            }
        }
        OpenAPIV3Parser apiv3Parser = new OpenAPIV3Parser();
        SwaggerParseResult swaggerParseResult = apiv3Parser.readContents(existingSwagger);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();

        Paths paths = openAPI.getPaths();
        Paths newPaths = new Paths();

        final Map<String, Object> dataMap = GenericApiObjectDefinition.getPathMap(api);
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            boolean pathItemExists = false;
            PathItem pathItem;
            if (paths.containsKey(entry.getKey())) {
                pathItem = paths.get(entry.getKey());
                pathItemExists = true;
            } else {
                pathItem = new PathItem();
            }
            Map<String, Object> methodMap = (Map<String, Object>) entry.getValue();
            List<String> newMethodsList = new ArrayList<>();
            for (Map.Entry<String, Object> methodEntry : methodMap.entrySet()) {
                Operation operation = null;
                boolean operationExists = false;
                if (pathItemExists) {
                    newMethodsList.add(methodEntry.getKey());
                    switch (methodEntry.getKey()) {
                        case SwaggerConstants.OPERATION_HTTP_GET:
                            operation = pathItem.getGet();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_POST:
                            operation = pathItem.getPost();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_DELETE:
                            operation = pathItem.getDelete();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_PUT:
                            operation = pathItem.getPut();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_HEAD:
                            operation = pathItem.getHead();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_PATCH:
                            operation = pathItem.getPatch();
                            break;
                        case SwaggerConstants.OPERATION_HTTP_OPTIONS:
                            operation = pathItem.getOptions();
                    }
                }
                if (operation == null) {
                    operation = new Operation();
                } else {
                    operationExists = true;
                }
                Object[] paramArr =
                        (Object[]) ((Map<String, Object>) methodEntry.getValue()).get(SwaggerConstants.PARAMETERS);
                if (operationExists) {
                    List<Parameter> parameters = operation.getParameters();
                    List<Parameter> newParameter = new ArrayList<>();
                    if (paramArr != null && paramArr.length > 0) {
                        for (Object o : paramArr) {
                            String paramType = (String) ((Map<String, Object>) o).get(SwaggerConstants.PARAMETER_IN);
                            String paramName = (String) ((Map<String, Object>) o).get(SwaggerConstants.PARAMETER_NAME);
                            Optional<Parameter> existing = null;
                            switch (paramType) {
                                case SwaggerConstants.PARAMETER_IN_PATH:
                                    existing = parameters.stream()
                                            .filter(c -> c.getName().equals(paramName) && c instanceof PathParameter)
                                            .findFirst();
                                    break;
                                case SwaggerConstants.PARAMETER_IN_QUERY:
                                    existing =
                                            parameters.stream().filter(c -> c.getName().equals(paramName) &&
                                                    c instanceof QueryParameter).findFirst();
                                    break;
                            }
                            if (existing == null || !existing.isPresent()) {
                                // if we found parameter do not update
                                updatePathQueryAndBodyParams(operation, paramType, paramName, newParameter);
                            } else {
                                newParameter.add(existing.get());
                            }
                            updateDefaultResponseAndPathItem(pathItem, operation, methodEntry, operationExists);
                        }
                    } else {
                        // no parameters defined ( default resource in the API )
                        updateDefaultResponseAndPathItem(pathItem, operation, methodEntry, operationExists);
                    }
                    // remove deleted parameters from swagger
                    if (newParameter.size() > 0) {
                        parameters.removeIf(c -> !newParameter.contains(c) && (c instanceof PathParameter));
                    }
                } else {
                    populateParameters(pathItem, methodMap);
                }

            }
            if (pathItemExists) {
                // Remove additional methods
                List<String> allMethodsList =
                        Arrays.asList(new String[]{"get", "post", "put", "delete", "head", "options", "patch"});
                List<String> differences = allMethodsList.stream()
                        .filter(element -> !newMethodsList.contains(element))
                        .collect(Collectors.toList());
                for (String method : differences) {
                    switch (method) {
                        case SwaggerConstants.OPERATION_HTTP_GET:
                            pathItem.setGet(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_POST:
                            pathItem.setPost(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_DELETE:
                            pathItem.setDelete(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_PUT:
                            pathItem.setPut(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_HEAD:
                            pathItem.setHead(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_PATCH:
                            pathItem.setPatch(null);
                            break;
                        case SwaggerConstants.OPERATION_HTTP_OPTIONS:
                            pathItem.setOptions(null);
                            break;
                    }
                }
            }
            newPaths.put(entry.getKey(), pathItem);
        }
        // Adding the new path map
        openAPI.setPaths(newPaths);
        updateInfoSection(openAPI);
        addServersSection(openAPI, hostname, port, projectPath);

        try {
            if (isJSONOut) {
                return Json.mapper().writeValueAsString(openAPI);
            }
            return Yaml.mapper().writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            throw new APIGenException("Error occurred while creating the output JAML/JSON", e);
        }
    }

    /**
     * Update path / query / body parameter in the existing OpenApi.
     *
     * @param operation     Operation object.
     * @param paramType     Type of the parameter ( path / query / body ).
     * @param paramName     Name of the parameter.
     * @param newParameters List of new parameters.
     */
    private void updatePathQueryAndBodyParams(Operation operation, String paramType, String paramName,
                                              List<Parameter> newParameters) {

        switch (paramType) {
            case SwaggerConstants.PARAMETER_IN_PATH:
                PathParameter pathParameter = new PathParameter();
                pathParameter.setName(paramName);
                pathParameter.setSchema(new StringSchema());
                operation.addParametersItem(pathParameter);
                newParameters.add(pathParameter);
                break;
            case SwaggerConstants.PARAMETER_IN_QUERY:
                QueryParameter queryParameter = new QueryParameter();
                queryParameter.setName(paramName);
                queryParameter.setSchema(new StringSchema());
                operation.addParametersItem(queryParameter);
                newParameters.add(queryParameter);
                break;
            case SwaggerConstants.PARAMETER_IN_BODY:
                // if body schema exists do not modify
                if (operation.getRequestBody() == null) {
                    RequestBody requestBody = new RequestBody();
                    requestBody.description("Sample Payload");
                    requestBody.setRequired(false);

                    MediaType mediaType = new MediaType();
                    Schema bodySchema = new Schema();
                    bodySchema.setType("object");

                    Map<String, Schema> inputProperties = new HashMap<>();
                    ObjectSchema objectSchema = new ObjectSchema();

                    bodySchema.setProperties(inputProperties);
                    inputProperties.put("payload", objectSchema);
                    mediaType.setSchema(bodySchema);
                    Content content = new Content();
                    content.addMediaType("application/json", mediaType);
                    requestBody.setContent(content);
                    operation.setRequestBody(requestBody);
                }
                break;
        }
    }

    /**
     * Add the default response ( since we cannot define it ) and pathItems to path map
     *
     * @param pathItem          PathItem contains the resource details.
     * @param operation         Operation object that contains params (path, query and body)
     * @param methodEntry       Context fof the resource.
     * @param isOperationExists Operation already exists in pathItem.
     */
    private void updateDefaultResponseAndPathItem(PathItem pathItem, Operation operation, Map.Entry methodEntry,
                                                  boolean isOperationExists) {

        if (operation.getResponses() == null) {
            ApiResponses apiResponses = new ApiResponses();
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setDescription("Default response");
            apiResponses.addApiResponse("default", apiResponse);
            operation.setResponses(apiResponses);
        }

        if (isOperationExists) {
            switch ((String) methodEntry.getKey()) {
                case SwaggerConstants.OPERATION_HTTP_GET:
                    pathItem.setGet(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_POST:
                    pathItem.setPost(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_DELETE:
                    pathItem.setDelete(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_PUT:
                    pathItem.setPut(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_HEAD:
                    pathItem.setHead(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_PATCH:
                    pathItem.setPatch(operation);
                    break;
                case SwaggerConstants.OPERATION_HTTP_OPTIONS:
                    pathItem.setOptions(operation);
                    break;
            }
        }
    }

    /**
     * Populate parameters of the given API resource.
     *
     * @param pathItem  OpenApi PathItem object.
     * @param methodMap methods of the API resource.
     */
    private void populateParameters(PathItem pathItem, Map<String, Object> methodMap) {

        for (Map.Entry<String, Object> methodEntry : methodMap.entrySet()) {
            Operation operation = new Operation();
            Object[] paramArr =
                    (Object[]) ((Map<String, Object>) methodEntry.getValue()).get(SwaggerConstants.PARAMETERS);
            if (paramArr != null && paramArr.length > 0) {
                for (Object o : paramArr) {
                    String paramType = (String) ((Map<String, Object>) o).get(SwaggerConstants.PARAMETER_IN);
                    String paramName = (String) ((Map<String, Object>) o).get(SwaggerConstants.PARAMETER_NAME);
                    addPathQueryAndBodyParams(operation, paramType, paramName);
                    addDefaultResponseAndPathItem(pathItem, operation, methodEntry);
                }
            } else {
                // no parameters defined ( default resource in the API )
                addDefaultResponseAndPathItem(pathItem, operation, methodEntry);
                addDefaultRequestBody(operation, methodEntry);
            }
        }
    }
}
