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

package org.eclipse.lemminx.customservice.synapse.inbound.conector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.UiSchemaFlattener;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorVariableSchemaUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.parser.Node;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.UISchemaMapper;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser.getPomDetails;

public class InboundConnectorHolder {

    private static final Logger LOGGER = Logger.getLogger(InboundConnectorHolder.class.getName());
    public static final String INPUT_SCHEMA_FILE_SUFFIX = "_inputschema.json";
    private static InboundConnectorHolder instance;
    private String projectId;
    private String projectPath;
    private String tempFolderPath;
    // <Connector name, Connector ID> map
    private final HashMap<String, String> connectorIdMap;
    // <Connector ID, UI schema path> map
    private final HashMap<String, String> inboundConnectors;
    // <Connector ID, input schema path> map
    private final HashMap<String, String> inboundConnectorInputSchemas;
    private Map<String, JsonObject> localInboundConnectors;
    private JsonObject inboundConnectorListJson;
    private String projectRuntimeVersion;
    private String localInboundEndpointsListForCopilot;

    private Set<String> FALLBACK_TO_440 = Set.of(
            Constant.MI_470_VERSION,
            Constant.MI_460_VERSION,
            Constant.MI_450_VERSION
    );

    public InboundConnectorHolder() {

        this.inboundConnectors = new HashMap<>();
        this.connectorIdMap = new HashMap<>();
        this.inboundConnectorInputSchemas = new HashMap<>();
    }

    public static InboundConnectorHolder getInstance() {

        if (instance == null) {
            throw new IllegalStateException("InboundConnectorHolder has not yet been initialized");
        }
        return instance;
    }

    public void init(String projectPath, String projectRuntimeVersion) {

        if (projectPath == null) {
            LOGGER.log(Level.SEVERE, "Project path is null. Cannot initialize inbound connector holder.");
            return;
        }
        this.projectPath = projectPath;
        this.projectId = Utils.getHash(projectPath);
        // Maintain the original runtime version of the project as the 4.5.0 version has new inbound-endpoints
        // TODO: https://github.com/wso2/mi-vscode/issues/1331
        OverviewPageDetailsResponse pomDetailsResponse = new OverviewPageDetailsResponse();
        getPomDetails(projectPath, pomDetailsResponse);
        Node node = pomDetailsResponse.getPrimaryDetails().getRuntimeVersion();
        if (node != null && FALLBACK_TO_440.contains(node.getValue())) {
            this.projectRuntimeVersion = node.getValue();
        } else {
            this.projectRuntimeVersion = projectRuntimeVersion;
        }
        this.tempFolderPath = System.getProperty("user.home") + File.separator + ".wso2-mi" + File.separator +
                Constant.INBOUND_CONNECTORS + File.separator + new File(projectPath).getName() + "_" + projectId;
        String referenceRuntime = FALLBACK_TO_440.contains(this.projectRuntimeVersion) ? Constant.MI_440_VERSION
                                    : this.projectRuntimeVersion;
        this.localInboundConnectors = Utils.getUISchemaMap("org/eclipse/lemminx/inbound-endpoints/"
                + referenceRuntime.replace(".", StringUtils.EMPTY));
        getCustomInboundConnectors();
        loadInboundConnectors();
        this.localInboundEndpointsListForCopilot = generateInboundConnectorArray();
        instance = this;
    }

    private void loadInboundConnectors() {

        File folder = new File(tempFolderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isHidden()) {
                        loadInboundConnector(file);
                    }
                }
            }
        }
    }

    public synchronized String getCustomInboundConnectors() {

        boolean hasFailure = false;
        InputStream inputStream = JsonLoader.class
                .getResourceAsStream("/org/eclipse/lemminx/inbound-endpoints/inbound_endpoints_"
                        + this.projectRuntimeVersion.replace(".", StringUtils.EMPTY) + Constant.JSON_FILE_EXT);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        this.inboundConnectorListJson = JsonParser.parseReader(reader).getAsJsonObject();
        Path resourcesPath = Path.of(this.projectPath, Constant.SRC, Constant.MAIN, Constant.WSO2MI,
                Constant.RESOURCES);
        for (String dirName : new String[]{Constant.INBOUND_ENDPOINTS, Constant.INBOUND_CONNECTORS_DIR}) {
            File extractFolder = new File(resourcesPath.resolve(dirName).toString());
            if (importInboundConnectorsFromDirectory(extractFolder)) {
                hasFailure = true;
            }
        }
        return hasFailure ? "Failed to import the inbound-endpoint" : "success";
    }

    private boolean importInboundConnectorsFromDirectory(File extractFolder) {

        boolean hasFailure = false;
        List<File> inboundConnectorZips = getInboundConnectorZips(extractFolder);
        for (File zip : inboundConnectorZips) {
            String zipName = zip.getName().replace(Constant.DOT + "zip", StringUtils.EMPTY);
            File extractToFolder = new File(extractFolder.getAbsolutePath() + File.separator + zipName);
            try {
                Utils.extractZip(zip, extractToFolder);
                String schema = Utils.readFile(extractToFolder.toPath().resolve(Constant.RESOURCES)
                        .resolve(Constant.UI_SCHEMA_JSON).toFile());
                JsonObject connectorSchema = Utils.getJsonObject(schema);
                String connectorName = connectorSchema.get(Constant.NAME).getAsString();
                String connectorId = connectorSchema.get(Constant.ID) != null ?
                        connectorSchema.get(Constant.ID).getAsString() : StringUtils.EMPTY;
                File inputSchemaFile = extractToFolder.toPath().resolve(Constant.RESOURCES)
                        .resolve(Constant.INPUT_SCHEMA_JSON).toFile();
                String inputSchema = inputSchemaFile.exists() ? Utils.readFile(inputSchemaFile) : null;

                if (saveInboundConnector(connectorName, schema)) {
                    if (StringUtils.isNotBlank(inputSchema)) {
                        saveInboundConnectorInputSchema(connectorName, connectorId, inputSchema);
                    }
                    JsonArray connectorArray = this.inboundConnectorListJson.getAsJsonArray(Constant.INBOUND_CONNECTOR_DATA);
                    if (!isConnectorAlreadyListed(connectorArray, connectorId)) {
                        JsonObject newConnector = new JsonObject();
                        newConnector.addProperty(Constant.NAME, connectorSchema.get(Constant.TITLE) != null ?
                                connectorSchema.get(Constant.TITLE).getAsString() : StringUtils.EMPTY);
                        newConnector.addProperty(Constant.ID, connectorId);
                        newConnector.addProperty(Constant.DESCRIPTION, connectorSchema.get(Constant.DESCRIPTION) != null ?
                                connectorSchema.get(Constant.DESCRIPTION).getAsString() : StringUtils.EMPTY);
                        newConnector.addProperty(Constant.TYPE, Constant.INBOUND_DASH_ENDPOINT);
                        connectorArray.add(newConnector);
                    }
                } else {
                    hasFailure = true;
                    LOGGER.log(Level.SEVERE, "Failed to import custom inbound-endpoint:" + zipName
                            + ". Invalid or missing uischema.");
                }
            } catch (Exception e) {
                hasFailure = true;
                LOGGER.log(Level.SEVERE, "Failed to import custom inbound-endpoint:" + zipName, e);
            }
            if (extractToFolder.exists() && extractToFolder.isDirectory()) {
                try {
                    Utils.deleteDirectory(extractToFolder.toPath());
                } catch (IOException e) {
                    hasFailure = true;
                    LOGGER.log(Level.SEVERE, "Failed to delete extracted inbound-endpoint:" + zipName, e);
                }
            }
        }
        return hasFailure;
    }

    private boolean isConnectorAlreadyListed(JsonArray connectorArray, String connectorId) {

        if (connectorId == null || connectorId.isEmpty()) {
            return false;
        }
        for (JsonElement element : connectorArray) {
            JsonObject connector = element.getAsJsonObject();
            if (connector.has(Constant.ID) && connectorId.equals(connector.get(Constant.ID).getAsString())) {
                return true;
            }
        }
        return false;
    }

    private List<File> getInboundConnectorZips(File extractFolder) {

        List<File> inboundConnectorZips = new ArrayList<>();
        if (extractFolder.exists() && extractFolder.isDirectory()) {
            File[] files = extractFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (Utils.isZipFile(f)) {
                        inboundConnectorZips.add(f);
                    }
                }
            }
        }
        return inboundConnectorZips;
    }

    private void loadInboundConnector(File file) {

        String fileName = file.getName();
        // Input schema files are loaded together with their uischema, skip them here.
        if (fileName.endsWith(INPUT_SCHEMA_FILE_SUFFIX)) {
            return;
        }
        try {
            String connectorName = fileName.replace(Constant.JSON_FILE_EXT, StringUtils.EMPTY);
            String uiSchema = Utils.readFile(file);
            JsonObject inboundConnector = Utils.getJsonObject(uiSchema);
            if (inboundConnector == null || !inboundConnector.has(Constant.ID)) {
                return;
            }
            String id = inboundConnector.get(Constant.ID).getAsString();
            connectorIdMap.put(connectorName, id);
            inboundConnectors.put(id, file.getAbsolutePath());
            File inputSchemaFile = Path.of(tempFolderPath, connectorName + INPUT_SCHEMA_FILE_SUFFIX).toFile();
            if (inputSchemaFile.exists()) {
                inboundConnectorInputSchemas.put(id, inputSchemaFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while loading inbound connector schema from file", e);
        }
    }

    public Boolean saveInboundConnector(String connectorName, String uiSchema) {

        // Save inbound connector
        JsonObject inboundConnector = Utils.getJsonObject(uiSchema);
        if (inboundConnector == null || !inboundConnector.has(Constant.ID)) {
            return false;
        }
        String id = inboundConnector.get(Constant.ID).getAsString();
        Path filePath = Path.of(tempFolderPath, connectorName + ".json");
        if (saveToFile(filePath.toFile(), uiSchema)) {
            connectorIdMap.put(connectorName, id);
            inboundConnectors.put(id, filePath.toString());
            return true;
        }
        return false;
    }

    /**
     * Persists the input schema shipped by an inbound connector (its
     * {@code resources/inputschema.json}) next to the uischema in the per-project
     * temp folder and registers it against the connector id.
     *
     * @param connectorName the connector name (uischema {@code name})
     * @param id            the connector id (uischema {@code id})
     * @param inputSchema   the raw input schema JSON
     */
    public void saveInboundConnectorInputSchema(String connectorName, String id, String inputSchema) {

        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(inputSchema)) {
            return;
        }
        Path filePath = Path.of(tempFolderPath, connectorName + INPUT_SCHEMA_FILE_SUFFIX);
        if (saveToFile(filePath.toFile(), inputSchema)) {
            inboundConnectorInputSchemas.put(id, filePath.toString());
        }
    }

    /**
     * Returns the input schema of the inbound connector with the given id as a
     * {@link Property} tree (built the same way as regular connector output schemas),
     * or {@code null} if no input schema is registered.
     */
    public Property getInboundConnectorInputSchema(String id) {

        String inputSchemaPath = inboundConnectorInputSchemas.get(id);
        if (StringUtils.isEmpty(inputSchemaPath)) {
            return null;
        }
        try {
            String inputSchema = Utils.readFile(new File(inputSchemaPath));
            JsonObject inputSchemaJson = Utils.getJsonObject(inputSchema);
            return ConnectorVariableSchemaUtils.buildSchemaProperty(inputSchemaJson);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while reading inbound connector input schema from file", e);
        }
        return null;
    }

    public InboundConnectorResponse getInboundConnectorSchema(File inboundEPFile) {

        try {
            DOMDocument inboundEPElement = Utils.getDOMDocument(inboundEPFile);
            if (inboundEPElement != null) {
                InboundEndpoint ib =
                        (InboundEndpoint) SyntaxTreeGenerator.buildTree(inboundEPElement.getDocumentElement());
                if (ib != null) {
                    String id = getIdFromInboundEP(ib);
                    if (id != null) {
                        InboundConnectorResponse response = getInboundConnectorSchemaFromId(id);
                        if (response.getUiSchema() != null) {
                            JsonObject schemaWithValues =
                                    UISchemaMapper.mapInputToUISchemaForInboundEndpoint(ib, response.getUiSchema());
                            response.setUiSchema(schemaWithValues);
                        }
                        return response;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while reading inbound endpoint file", e);
        }
        return new InboundConnectorResponse();
    }

    private String getIdFromInboundEP(InboundEndpoint ib) {

        String id = ib.getProtocol();
        if (id == null) {
            id = ib.getClazz();
        }
        return id;
    }

    public JsonObject getLocalInboundConnectorList() {

        return inboundConnectorListJson;
    }

    public String getLocalInboundEndpointsListForCopilot() {

        return localInboundEndpointsListForCopilot;
    }

    public InboundConnectorResponse getInboundConnectorSchema(String connectorName) {

        InboundConnectorResponse inboundConnector = new InboundConnectorResponse();
        inboundConnector.connectorName = connectorName;
        String connectorId = connectorIdMap.get(connectorName);
        return getInboundConnectorSchemaFromId(connectorId);
    }

    public InboundConnectorResponse getInboundConnectorSchemaFromId(String connectorId) {

        InboundConnectorResponse inboundConnector = new InboundConnectorResponse();

        if (localInboundConnectors.containsKey(connectorId)) {
            inboundConnector.uiSchema = localInboundConnectors.get(connectorId);
            return inboundConnector;
        }

        String uiSchemaPath = inboundConnectors.get(connectorId);
        if (uiSchemaPath == null) {
            return inboundConnector;
        }
        try {
            String uiSchema = Utils.readFile(new File(uiSchemaPath));
            JsonObject inboundConnectorJson = Utils.getJsonObject(uiSchema);
            inboundConnector.uiSchema = inboundConnectorJson;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while reading inbound connector schema from file", e);
        }
        return inboundConnector;
    }

    public boolean saveToFile(File file, String text) {

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                int chunkSize = 8192; // 8KB chunks
                int length = text.length();
                for (int i = 0; i < length; i += chunkSize) {
                    if (i + chunkSize > length) {
                        writer.write(text.substring(i));
                    } else {
                        writer.write(text.substring(i, i + chunkSize));
                    }
                }
            }
            return Boolean.TRUE;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving inbound connector schema to file", e);
        }
        return Boolean.FALSE;
    }

    public void setProjectPath(String projectPath) {

        this.projectPath = projectPath;
    }

    public String generateInboundConnectorArray() {

        String localInboundConnectorList = StringUtils.EMPTY;
        String metadataJson = this.inboundConnectorListJson.toString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode inboundConnectorMetadata = null;
        try {
            inboundConnectorMetadata = mapper.readTree(metadataJson);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse inbound-endpoint metadata JSON.", e);
            return localInboundConnectorList;
        }
        ArrayNode connectorArray = mapper.createArrayNode();

        for (JsonNode inboundConnectorData : inboundConnectorMetadata.path(Constant.INBOUND_CONNECTOR_DATA)) {
            if (!Constant.BUILTIN_INBOUND_ENDPOINT.equals(inboundConnectorData.path(Constant.TYPE).asText())) {
                continue;
            }
            String id = inboundConnectorData.path(Constant.ID).asText();
            String connectorName = id;
            String description = inboundConnectorData.path(Constant.DESCRIPTION).asText(StringUtils.EMPTY);

            InputStream inputStream = JsonLoader.class.getResourceAsStream("/org/eclipse/lemminx/inbound-endpoints/"
                            + this.projectRuntimeVersion.replace(".", StringUtils.EMPTY) + "/" + id + Constant.JSON_FILE_EXT);
            if (inputStream == null) {
                continue;
            }
            JsonNode inboundConnectorUISchema = null;
            try {
                inboundConnectorUISchema = mapper.readTree(inputStream);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to read or parse the inbound-endpoint UI schema JSON.", e);
                return localInboundConnectorList;
            }
            ObjectNode inboundConnectorObject = mapper.createObjectNode();

            inboundConnectorObject.put(Constant.CONNECTOR_NAME, connectorName);
            inboundConnectorObject.put(Constant.DESCRIPTION, description);
            inboundConnectorObject.put(Constant.CONNECTOR_TYPE, Constant.EVENT_INTEGRATION);

            ObjectNode versionNode = mapper.createObjectNode();
            ArrayNode operationsArray = mapper.createArrayNode();
            ObjectNode initOperation = mapper.createObjectNode();
            initOperation.put(Constant.NAME, Constant.INIT);
            ArrayNode parametersArray = mapper.createArrayNode();

            for (JsonNode group : inboundConnectorUISchema.path(Constant.ELEMENTS)) {
                JsonNode elements = group.path(Constant.VALUE).path(Constant.ELEMENTS);
                for (JsonNode element : elements) {
                    if (!Constant.ATTRIBUTE.equals(element.path(Constant.TYPE).asText())) {
                        continue;
                    }

                    JsonNode val = element.path(Constant.VALUE);
                    ObjectNode param = mapper.createObjectNode();

                    String paramName = val.path(Constant.NAME).asText();
                    String inputType = val.path(Constant.INPUT_TYPE).asText();
                    String displayName = val.path(Constant.DISPLAY_NAME).asText();
                    boolean required = Constant.TRUE.equals(val.path(Constant.REQUIRED).asText());
                    JsonNode defaultValue = val.has(Constant.DEFAULT_VALUE) ? val.get(Constant.DEFAULT_VALUE) : NullNode.getInstance();

                    param.put(Constant.NAME, paramName);
                    param.put(Constant.TYPE, inputType);
                    param.put(Constant.REQUIRED, required);
                    param.set(Constant.DEFAULT_VALUE, defaultValue);

                    StringBuilder descBuilder = new StringBuilder(displayName);
                    if (Constant.COMBO.equals(inputType) && val.has(Constant.COMBO_VALUES)) {
                        List<String> values = new ArrayList<>();
                        for (JsonNode item : val.path(Constant.COMBO_VALUES)) {
                            values.add(item.asText());
                        }
                        descBuilder.append(" - supported values: ").append(values.toString());
                    }
                    param.put(Constant.DESCRIPTION, descBuilder.toString());

                    parametersArray.add(param);
                }
            }

            initOperation.set(Constant.PARAMETERS, parametersArray);
            operationsArray.add(initOperation);
            versionNode.set(Constant.OPERATIONS, operationsArray);
            inboundConnectorObject.set(Constant.VERSION, versionNode);

            connectorArray.add(inboundConnectorObject);
        }

        try {
            localInboundConnectorList = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(connectorArray);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Failed to serialize inbound-endpoint metadata to a JSON string.", e);
        }
        return localInboundConnectorList;
    }

    /**
     * Returns the {@link InboundEndpointInfo} for a bundled inbound endpoint
     * (those shipped inside the LS jar, e.g. {@code http}, {@code jms},
     * {@code file}), or {@code null} if no bundled schema is registered under
     * the given id. No disk I/O is performed.
     */
    public InboundEndpointInfo getBundledInboundEndpoint(String id) {

        if (id == null || localInboundConnectors == null) {
            return null;
        }
        JsonObject schema = localInboundConnectors.get(id);
        if (schema == null) {
            return null;
        }
        return buildInboundEndpointInfo(schema, "bundled");
    }

    /**
     * Builds an {@link InboundEndpointInfo} from an already-parsed uischema JSON
     * object. Callers that have just downloaded and parsed a {@code mi-inbound-*}
     * artifact use this to produce the response payload with
     * {@code source = "downloaded"}.
     */
    public static InboundEndpointInfo buildInboundEndpointInfo(JsonObject schema, String source) {

        InboundEndpointInfo info = new InboundEndpointInfo();
        info.setSource(source);
        if (schema.has(Constant.NAME)) {
            info.setName(schema.get(Constant.NAME).getAsString());
        }
        if (schema.has(Constant.ID)) {
            info.setId(schema.get(Constant.ID).getAsString());
        }
        if (schema.has(Constant.TITLE)) {
            info.setDisplayName(schema.get(Constant.TITLE).getAsString());
        }
        if (schema.has(Constant.HELP)) {
            info.setDescription(schema.get(Constant.HELP).getAsString());
        } else if (schema.has(Constant.DESCRIPTION)) {
            info.setDescription(schema.get(Constant.DESCRIPTION).getAsString());
        }
        if (schema.has(Constant.TYPE)) {
            info.setType(schema.get(Constant.TYPE).getAsString());
        }
        if (schema.has(Constant.ELEMENTS) && schema.get(Constant.ELEMENTS).isJsonArray()) {
            info.setParameters(UiSchemaFlattener.flatten(schema.getAsJsonArray(Constant.ELEMENTS)));
        }
        return info;
    }
}
