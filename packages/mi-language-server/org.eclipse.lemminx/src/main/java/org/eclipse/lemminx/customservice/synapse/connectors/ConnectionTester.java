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

package org.eclipse.lemminx.customservice.synapse.connectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionResponse;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.TryOutHandler;
import org.eclipse.lemminx.customservice.synapse.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.LocalEntry;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.ConnectorParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.LocalEntrySerializer;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.mediator.ConnectorSerializer;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lsp4j.Position;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionTester {

    private static final Logger LOGGER = Logger.getLogger(ConnectionTester.class.getName());
    private static final Path CONNECTOR_PROJECT_TEMP_PATH = Path.of(System.getProperty("user.home"), ".wso2-mi");
    private static final Path LOCAL_ENTRY_RELATIVE_PATH =
            Path.of("src", "main", "wso2mi", "artifacts", "local-entries");
    private String projectRoot;
    private final TryOutHandler tryOutHandler;
    private ConnectorHolder connectorHolder;

    public ConnectionTester(String projectRoot, TryOutHandler tryOutHandler, ConnectorHolder connectorHolder) {

        this.projectRoot = projectRoot;
        this.tryOutHandler = tryOutHandler;
        this.connectorHolder = connectorHolder;
    }

    public TestConnectionResponse testConnection(TestConnectionRequest request) {

        String connectorName = request.getConnectorName();
        if (!connectorHolder.exists(connectorName)) {
            return new TestConnectionResponse("Connector not found");
        }
        String connectionType = request.getConnectionType();
        LocalEntry localEntry = new LocalEntry();
        String key = updateAndGetLocalEntryKey(request);
        localEntry.setKey(key);

        Connector connector = connectorHolder.getConnector(connectorName);
        ConnectorAction initOperation = connector.getAction(Constant.INIT);
        if (initOperation == null) {
            return new TestConnectionResponse("Connection operation not found");
        }

        String connectionXml = getConnectionXml(connector, initOperation, request.getParameters(), connectionType);
        localEntry.setContent(connectionXml);
        String localEntryXml = LocalEntrySerializer.serializeLocalEntry(localEntry);

        Path tempProjectPath = CONNECTOR_PROJECT_TEMP_PATH.resolve(connectorName + "_" + key);
        try {
            if (!tempProjectPath.toFile().mkdirs()) {
                LOGGER.log(Level.WARNING, "Failed to create temp project directory: " + tempProjectPath);
                return new TestConnectionResponse("Failed to create temp project directory");
            }
            addPomFile(Path.of(projectRoot), tempProjectPath);
            createLocalEntryFile(tempProjectPath, localEntryXml, localEntry.getKey());

            ConnectorAction testConnectionOperation = connector.getAction(Constant.TEST_CONNECTION_OPERATION);
            if (testConnectionOperation == null) {
                return new TestConnectionResponse("Test connection operation not found");
            }

            org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector testConnection =
                    getTestConnectionOperation(connectorName, testConnectionOperation, localEntry.getKey());
            MediatorTryoutInfo info = getMediatorTryoutInfo(testConnection, tempProjectPath.toString());
            Boolean isValidConnection = checkIsValidConnection(info);
            if (isValidConnection != null) {
                return new TestConnectionResponse(isValidConnection);
            }
            return new TestConnectionResponse("Error while testing the connection");
        } catch (IOException | InvalidConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error while testing the connection", e);
            return new TestConnectionResponse("Error while testing the connection");
        } finally {
            try {
                Utils.deleteDirectory(tempProjectPath);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error while deleting the temp project directory", e);
            }
        }
    }

    private MediatorTryoutInfo getMediatorTryoutInfo(
            org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector testConnection,
            String tempProjectPath)
            throws IOException, InvalidConfigurationException {

        String apiPath = TryOutUtils.createAPI(testConnection, tempProjectPath.toString());
        Position position = TryOutUtils.getMediatorPosition(apiPath, 0, 0);
        MediatorTryoutRequest mediatorTryoutRequest =
                new MediatorTryoutRequest(apiPath, position.getLine(), position.getCharacter(),
                        "{}", null);
        Properties context = new Properties();
        context.setProperty(TryOutConstants.POST_CLEANUP, "true");
        context.setProperty(TryOutConstants.IS_CONNECTOR_TEST, "true");
        return tryOutHandler.handleIsolatedTryOut(tempProjectPath, mediatorTryoutRequest, false, context);
    }

    private void createLocalEntryFile(Path tempProjectPath, String localEntryXml, String localEntryKey)
            throws IOException {

        Path localEntryPath =
                tempProjectPath.resolve(LOCAL_ENTRY_RELATIVE_PATH).resolve(localEntryKey + ".xml");
        if (!localEntryPath.toFile().exists()) {
            localEntryPath.toFile().getParentFile().mkdirs();
        }
        Utils.writeToFile(localEntryPath.toString(), localEntryXml);
    }

    private String updateAndGetLocalEntryKey(TestConnectionRequest request) {

        String key = UUID.randomUUID().toString().replaceAll("-", "_");
        request.addParameter(Constant.NAME, key); // Replace the name with a unique key to avoid conflicts
        return key;
    }

    private String getConnectionXml(Connector connector, ConnectorAction initOperation,
                                    Map<String, Object> parameters, String connectionType) {

        org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector connection =
                new org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector();
        connection.setConnectorName(connector.getName());
        connection.setTag(initOperation.getTag());
        connection.setMethod(Constant.INIT);
        if (StringUtils.isNotEmpty(connectionType)) {
            connection.addParameter(new ConnectorParameter(Constant.CONNECTION_TYPE, connectionType));
        }
        if (parameters != null) {
            for (OperationParameter parameter : initOperation.getParameters()) {
                if (parameters.containsKey(parameter.getName())) {
                    ConnectorParameter connectorParameter = new ConnectorParameter();
                    connectorParameter.setName(parameter.getName());
                    if (parameters.get(parameter.getName()) instanceof Map) {
                        Map parameterMap = (Map) parameters.get(parameter.getName());
                        if (Boolean.parseBoolean(parameterMap.get(Constant.IS_EXPRESSION).toString())) {
                            connectorParameter.setExpression(parameterMap.get(Constant.VALUE).toString());
                        } else {
                            connectorParameter.setValue(parameterMap.get(Constant.VALUE).toString());
                        }
                    } else {
                        connectorParameter.setValue(parameters.get(parameter.getName()).toString());
                    }
                    connection.addParameter(connectorParameter);
                }
            }
        }
        ConnectorSerializer connectorSerializer = new ConnectorSerializer();
        return connectorSerializer.serializeMediator(null, connection).toString();
    }

    private org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector getTestConnectionOperation(
            String connectorName, ConnectorAction testConnectionOperation, String localEntryKey) {

        org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector testConnection =
                new org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.connector.Connector();
        testConnection.setConnectorName(connectorName);
        testConnection.setTag(testConnectionOperation.getTag());
        testConnection.setMethod(Constant.TEST_CONNECTION_OPERATION);
        testConnection.setConfigKey(localEntryKey);
        return testConnection;
    }

    private Boolean checkIsValidConnection(MediatorTryoutInfo info) {

        if (info != null) {
            MediatorInfo outputInfo = info.getOutput();
            if (outputInfo != null) {
                List<Property> synapseProperties = outputInfo.getSynapse();
                Property isValidConnection = synapseProperties.stream()
                        .filter(property -> Constant.IS_VALID_CONNECTION.equals(property.getKey()))
                        .findFirst()
                        .orElse(null);
                if (isValidConnection != null) {
                    return Boolean.parseBoolean(isValidConnection.getValue());
                }
            }
        }
        return null;
    }

    private void addPomFile(Path projectRoot, Path tempProjectPath) throws IOException {

        Path pomPath = projectRoot.resolve(Constant.POM);
        Path mavenWrapperPath = projectRoot.resolve("mvnw");
        Path dotMvnPath = projectRoot.resolve(".mvn");

        for (Path path : List.of(pomPath, mavenWrapperPath, dotMvnPath)) {
            if (path.toFile().exists()) {
                if (path.toFile().isDirectory()) {
                    Utils.copyFolder(path, tempProjectPath.resolve(path.getFileName()), null);
                } else {
                    Utils.copyFile(path.toString(), tempProjectPath.toString());
                }
            }
        }
    }
}
