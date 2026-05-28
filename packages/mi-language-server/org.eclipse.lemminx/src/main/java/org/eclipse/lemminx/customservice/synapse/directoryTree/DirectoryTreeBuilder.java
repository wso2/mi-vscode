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

package org.eclipse.lemminx.customservice.synapse.directoryTree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParser;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.APINode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.APIResource;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.AdvancedNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.ConnectionNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.FileNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.FolderNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.Node;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.RegistryNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.SequenceNode;
import org.eclipse.lemminx.customservice.synapse.directoryTree.node.TestFolder;
import org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder.LegacyDirectoryTreeBuilder;
import org.eclipse.lemminx.customservice.synapse.directoryTree.utils.DirectoryTreeUtils;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageProcessor;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.MessageStore;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.inbound.InboundEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.template.Template;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lsp4j.WorkspaceFolder;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryTreeBuilder {

    private static final Logger LOGGER = Logger.getLogger(DirectoryTreeBuilder.class.getName());
    private static final String MAIN = "main";
    private static final String WSO2MI = "wso2mi";
    private static final String RESOURCES = "resources";
    private static final String JAVA = "java";
    private static String projectPath;
    private static String mainSequence;
    private static List<String> artifactResourcePaths = new ArrayList<>();

    public static DirectoryMapResponse buildDirectoryTree(WorkspaceFolder projectFolder) {

        //Support old project structure
        if (DirectoryTreeUtils.isLegacyProject(projectFolder)) {
            return LegacyDirectoryTreeBuilder.buildDirectoryTree(projectFolder);
        }
        try {
            String encodedPath = projectFolder.getUri();
            if (encodedPath.startsWith(Constant.FILE_PREFIX)) {
                encodedPath = encodedPath.substring(7);
            }
            projectPath = URLDecoder.decode(encodedPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.log(Level.SEVERE, "Could not decode the file path.", e);
        }
        Tree directoryTree = null;
        if (projectPath != null) {
            String projectType = DirectoryTreeUtils.getProjectType(projectPath);
            if (Constant.INTEGRATION_PROJECT.equalsIgnoreCase(projectType)) {
                updateMainSequence();
                directoryTree = new IntegrationDirectoryTree(projectPath, projectType);
                analyzeIntegrationProject((IntegrationDirectoryTree) directoryTree);
                ((IntegrationDirectoryTree) directoryTree).sort();
            } else if (Constant.DOCKER_PROJECT.equalsIgnoreCase(projectType) || Constant.KUBERNETES_PROJECT.
                    equalsIgnoreCase(projectType)) {
                directoryTree = new DistributionDirectoryTree(projectPath, projectType);
                analyzeDistributionProject((DistributionDirectoryTree) directoryTree);
            }
        }

        DirectoryMapResponse directoryMapResponse = new DirectoryMapResponse(directoryTree);
        return directoryMapResponse;
    }

    /**
     * Generate model for the project explorer
     *
     * @param projectFolder project folder path
     *
     * @return project explorer structure
     */
    public static DirectoryMapResponse getProjectExplorerModel(WorkspaceFolder projectFolder) {
        DirectoryMapResponse directoryMap = buildDirectoryTree(projectFolder);
        if (directoryMap.getDirectoryMap() == null) {
            LOGGER.log(Level.SEVERE, "Error occurred while building directory tree.");
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(directoryMap.getDirectoryMap().getAsJsonObject().toString());
            removeGitkeepFiles(root);
            JsonNode artifacts = root.path(Constant.SRC).path(MAIN).path(WSO2MI).path(Constant.ARTIFACTS);
            JsonNode resources = root.path(Constant.SRC).path(MAIN).path(WSO2MI).path(Constant.RESOURCES);
            ObjectNode newArtifacts = mapper.createObjectNode();

            newArtifacts.set("APIs", artifacts.path(Constant.APIS));
            newArtifacts.set("Event Integrations", artifacts.path(Constant.INBOUNDENDPOINTS));
            newArtifacts.set("Automations", artifacts.path(Constant.TASKS));
            newArtifacts.set("Data Services", artifacts.path(Constant.DATA_SERVICES));

            ObjectNode otherArtifacts = newArtifacts.putObject("Other Artifacts");
            otherArtifacts.set("Sequences", artifacts.path(Constant.SEQUENCES));
            otherArtifacts.set("Connections", artifacts.path(Constant.CONNECTIONS));
            otherArtifacts.set("Data Sources", artifacts.path(Constant.DATA_SOURCES));

            ArrayNode classMediatorArray = mapper.createArrayNode();
            JsonNode mediatorFolders = root.path(Constant.SRC).path(MAIN).path(JAVA).path(Constant.FOLDERS);
            extractClassMediators(mediatorFolders, classMediatorArray);
            otherArtifacts.set("Class Mediators", classMediatorArray);

            ArrayNode ballerinaModulesArray = mapper.createArrayNode();
            JsonNode moduleFolders = root.path(Constant.SRC).path(MAIN).path(Constant.BALLERINA).path(Constant.FOLDERS);
            extractBallerinaModules(moduleFolders, ballerinaModulesArray);
            otherArtifacts.set("Ballerina Modules", ballerinaModulesArray);

            otherArtifacts.set("Endpoints", artifacts.path(Constant.ENDPOINTS));
            otherArtifacts.set("Proxy Services", artifacts.path(Constant.PROXYSERVICES));
            otherArtifacts.set("Message Stores", artifacts.path(Constant.MESSAGE_STORES));
            otherArtifacts.set("Message Processors", artifacts.path(Constant.MESSAGE_PROCESSORS));
            otherArtifacts.set("Local Entries", artifacts.path(Constant.LOCALENTRIES));
            otherArtifacts.set("Templates", artifacts.path(Constant.TEMPLATES));

            JsonNode registryFolders = root.path(Constant.SRC).path(MAIN).path(WSO2MI).path(Constant.RESOURCES)
                    .path(Constant.REGISTRY).path(Constant.GOV).path(Constant.FOLDERS);
            JsonNode newResources = resources.path(Constant.NEW_RESOURCES);
            JsonNode newResourcesFolders = newResources.path(Constant.FOLDERS);
            newArtifacts.set("Resources", newResources);

            ArrayNode dataMapperConfigs = getDataMapperConfigs(mapper, registryFolders, newResourcesFolders);

            if (dataMapperConfigs.isEmpty()) {
                otherArtifacts.set("Data Mappers", mapper.createArrayNode());
            } else {
                otherArtifacts.set("Data Mappers", dataMapperConfigs);
            }

            ((ObjectNode) root.path(Constant.SRC).path(MAIN).path(WSO2MI)).set(Constant.ARTIFACTS, newArtifacts);
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonNodeAsString = objectMapper.writeValueAsString(root);
            directoryMap.setDirectoryMap(JsonParser.parseString(jsonNodeAsString));
            return directoryMap;

        } catch (JsonProcessingException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while building the project explorer directory tree.", e);
            return null;
        }
    }

    /**
     * Recursively removes .gitkeep files from the JSON node tree structure.
     * This method traverses both object and array nodes, identifying and removing
     * any nodes that represent .gitkeep files.
     *
     * @param node the JSON node to process
     */
    private static void removeGitkeepFiles(JsonNode node) {

        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fieldsToRemove = new ArrayList<>();

            obj.fieldNames().forEachRemaining(field -> {
                JsonNode child = obj.get(field);
                if (child.isObject() && child.has(Constant.NAME) &&
                        Constant.GITKEEP.equals(child.get(Constant.NAME).asText())) {
                    fieldsToRemove.add(field);
                } else {
                    removeGitkeepFiles(child);
                }
            });
            fieldsToRemove.forEach(obj::remove);
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            List<Integer> itemsToRemove = new ArrayList<>();
            for (int index = 0; index < arrayNode.size(); index++) {
                JsonNode child = arrayNode.get(index);
                if (child.isObject() && child.has(Constant.NAME) &&
                        Constant.GITKEEP.equals(child.get(Constant.NAME).asText())) {
                    itemsToRemove.add(index);
                } else {
                    removeGitkeepFiles(child);
                }
            }
            for (int index = itemsToRemove.size() - 1; index >= 0; index--) {
                arrayNode.remove(itemsToRemove.get(index));
            }
        }
    }

    private static ArrayNode getDataMapperConfigs(ObjectMapper mapper, JsonNode registryFolders, JsonNode newResourcesFolders) {

        ArrayNode dataMappersNode = mapper.createArrayNode();
        for (JsonNode resourceFolder : newResourcesFolders) {
            if (Constant.DATA_MAPPER.equals(resourceFolder.path(Constant.NAME).asText())) {
                dataMappersNode.addAll((ArrayNode) resourceFolder.path(Constant.FOLDERS));
                break;
            }
        }
        for (JsonNode registryFolder : registryFolders) {
            if (Constant.DATA_MAPPER.equals(registryFolder.path(Constant.NAME).asText())) {
                dataMappersNode.addAll((ArrayNode) registryFolder.path(Constant.FOLDERS));
                break;
            }
        }
        return dataMappersNode;
    }

    public static List<String> getProjectIdentifiers(WorkspaceFolder projectFolder, List<String> filePaths) {

        List<String> result = new ArrayList<>();
        DirectoryMapResponse directoryMap = buildDirectoryTree(projectFolder);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(directoryMap.getDirectoryMap().getAsJsonObject().toString());
            JsonNode artifacts = root.path(Constant.SRC).path(MAIN).path(WSO2MI).path(Constant.ARTIFACTS);
            artifacts.fields().forEachRemaining(entry -> {
                String artifactType = entry.getKey();
                JsonNode artifactEntries = entry.getValue();
                if (artifactEntries.isArray()) {
                    for (JsonNode artifactEntryNode: artifactEntries) {
                        String path = artifactEntryNode.path(Constant.PATH).asText();
                        if (filePaths.contains(path)) {
                            result.add(artifactType + File.separator +
                                    path.substring(path.lastIndexOf(File.separator) + 1).split("\\.")[0]);
                        }
                    }
                }
            });
            return result;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private static void updateMainSequence() {

        mainSequence = null;
        Path pomPath = Path.of(projectPath, "pom.xml");
        File pomFile = pomPath.toFile();
        if (pomFile.exists()) {
            try {
                DOMDocument pom = Utils.getDOMDocument(pomFile);
                DOMNode profiles = Utils.getChildNodeByName(pom.getDocumentElement(), Constant.PROFILES);
                List<DOMNode> profileList = profiles.getChildren();
                profileList.forEach(profile -> {
                    DOMNode profileId = Utils.getChildNodeByName(profile, Constant.ID);
                    if (profileId != null && Constant.DEFAULT.equals(Utils.getInlineString(profileId.getFirstChild()))) {
                        DOMNode properties = Utils.getChildNodeByName(profile, Constant.PROPERTIES);
                        if (properties != null) {
                            DOMNode mainSequenceNode = Utils.getChildNodeByName(properties, Constant.MAIN_SEQUENCE);
                            if (mainSequenceNode != null) {
                                mainSequence = Utils.getInlineString(mainSequenceNode.getFirstChild());
                                return;
                            }
                        }
                    }
                });
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not read project pom file");
            }
        }
    }

    private static void analyzeIntegrationProject(IntegrationDirectoryTree directoryTree) {

        artifactResourcePaths = new ArrayList<>();
        analyzeArtifacts(directoryTree);
        analyzeResources(directoryTree);
        analyzeJavaProjects(directoryTree);
        analyzeBallerinaProjects(directoryTree);
        analyzeTestsFolder(directoryTree);
    }

    private static void analyzeDistributionProject(DistributionDirectoryTree directoryTree) {

        artifactResourcePaths = new ArrayList<>();
        File folder = new File(projectPath);
        if (folder != null && folder.exists() && !folder.isHidden()) {
            String folderName = folder.getName();
            FolderNode folderNode = new FolderNode(folderName, projectPath);
            traverseFolder(folderNode, null);
            directoryTree.setFolders(folderNode.getFolders());
            directoryTree.setFiles(folderNode.getFiles());
        }
    }

    private static void analyzeArtifacts(IntegrationDirectoryTree directoryTree) {

        String artifactsPath = projectPath + File.separator + Constant.SRC + File.separator + MAIN
                + File.separator + WSO2MI + File.separator + "artifacts";
        File folder = new File(artifactsPath);
        File[] listOfFiles = folder.listFiles(File::isDirectory);
        if (listOfFiles != null) {
            for (File subFolder : listOfFiles) {
                try {
                    if (subFolder.isDirectory()) {
                        String type = getType(subFolder.getName());
                        analyzeByType(directoryTree, subFolder, type);
                    }
                } catch (SecurityException e) {
                    LOGGER.log(Level.WARNING, "No read access to the file.", e);
                }
            }
        }
    }

    private static String getType(String name) {

        String name1 = Utils.removeHyphen(name);
        name1 = Utils.pluralToSingular(name1);
        name1 = name1.substring(0, 1).toUpperCase() + name1.substring(1);
        return name1;
    }

    private static void analyzeByType(IntegrationDirectoryTree directoryTree, File folder, String type) {

        try {
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile() && !file.isHidden()) {
                        String name = file.getName();
                        String path = file.getAbsolutePath();
                        Node advancedComponent = createEsbComponent(type, name, path);
                        if (advancedComponent != null) {
                            try {
                                String methodName;
                                if (advancedComponent instanceof ConnectionNode) {
                                    methodName = "addConnection";
                                } else {
                                    methodName = "add" + type;
                                }
                                Method method = directoryTree.getClass().getMethod(methodName, Node.class);
                                method.invoke(directoryTree, advancedComponent);
                            } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException |
                                     InvocationTargetException e) {
                                LOGGER.log(Level.WARNING, "Error while trying to execute method.", e);
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "No read access to the file.", e);
        }
    }

    private static void analyzeResources(IntegrationDirectoryTree directoryTree) {

        analyzeRegistryResources(directoryTree);
        analyzeConnectorResources(directoryTree);
        analyzeMetadataResources(directoryTree);
        analyzeNewResources(directoryTree);
    }

    private static void analyzeNewResources(IntegrationDirectoryTree directoryTree) {

        String registryPath = projectPath + File.separator + Constant.SRC + File.separator +
                MAIN + File.separator + WSO2MI + File.separator + RESOURCES;
        File folder = new File(registryPath);
        if (folder != null && folder.exists()) {
            if (!folder.isHidden()) {
                String folderName = folder.getName();
                FolderNode resourceFolderNode = new FolderNode(folderName, registryPath);
                traverseFolder(resourceFolderNode, directoryTree);
                directoryTree.getResources().setNewResources(resourceFolderNode);
            }
        }
    }

    private static void analyzeRegistryResources(IntegrationDirectoryTree directoryTree) {

        analyzeRegistryByType(directoryTree, Constant.GOV);
        analyzeRegistryByType(directoryTree, Constant.CONF);
    }

    private static void analyzeRegistryByType(IntegrationDirectoryTree directoryTree, String type) {

        String registryPath = projectPath + File.separator + Constant.SRC + File.separator +
                MAIN + File.separator + WSO2MI + File.separator + RESOURCES +
                File.separator + Constant.REGISTRY + File.separator + type;
        File folder = new File(registryPath);
        if (folder != null && folder.exists()) {
            if (!folder.isHidden()) {
                String folderName = folder.getName();
                FolderNode registryFolderNode = new FolderNode(folderName, registryPath);
                traverseFolder(registryFolderNode, directoryTree);
                if (Constant.GOV.equalsIgnoreCase(type)) {
                    directoryTree.getResources().getRegistry().setGov(registryFolderNode);
                } else if (Constant.CONF.equalsIgnoreCase(type)) {
                    directoryTree.getResources().getRegistry().setConf(registryFolderNode);
                }
            }
        }
    }

    private static void analyzeConnectorResources(IntegrationDirectoryTree directoryTree) {

        String connectorPath = projectPath + File.separator + Constant.SRC + File.separator + MAIN
                + File.separator + WSO2MI + File.separator + RESOURCES + File.separator + "connectors";
        File folder = new File(connectorPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (Utils.isZipFile(file) && !file.isHidden()) {
                    String name = file.getName();
                    String path = file.getAbsolutePath();
                    Node resource = new Node("connector", name, path);
                    directoryTree.getResources().addConnector(resource);
                }
            }
        }
    }

    private static void analyzeMetadataResources(IntegrationDirectoryTree directoryTree) {

        String metadataPath = projectPath + File.separator + Constant.SRC + File.separator + MAIN +
                File.separator + WSO2MI + File.separator + RESOURCES +
                File.separator + "metadata";
        File folder = new File(metadataPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && !file.isHidden()) {
                    String name = file.getName();
                    String path = file.getAbsolutePath();
                    Node resource = new Node("metadata", name, path);
                    directoryTree.getResources().addMetadata(resource);
                }
            }
        }
    }

    private static void analyzeJavaProjects(IntegrationDirectoryTree directoryTree) {

        String javaPath =
                projectPath + File.separator + Constant.SRC + File.separator + MAIN +
                        File.separator + JAVA;
        File folder = new File(javaPath);
        if (folder != null && folder.exists()) {
            if (!folder.isHidden()) {
                String folderName = folder.getName();
                FolderNode javaFolderNode = new FolderNode(folderName, javaPath);
                traverseFolder(javaFolderNode, null);
                directoryTree.setJava(javaFolderNode);
            }
        }
    }

    private static void analyzeBallerinaProjects(IntegrationDirectoryTree directoryTree) {

        String ballerinaPath = projectPath + File.separator + Constant.SRC + File.separator + MAIN +
                        File.separator + Constant.BALLERINA;
        File folder = new File(ballerinaPath);
        if (folder.exists() && !folder.isHidden()) {
            String folderName = folder.getName();
            FolderNode ballerinaFolderNode = new FolderNode(folderName, ballerinaPath);
            traverseFolder(ballerinaFolderNode, null);
            directoryTree.setBallerina(ballerinaFolderNode);
        }
    }

    private static void analyzeTestsFolder(IntegrationDirectoryTree directoryTree) {

        TestFolder testFolder = new TestFolder();
        String testsPath = projectPath + File.separator + Constant.SRC + File.separator + "test";
        analyzeSubTestFolder(testsPath, WSO2MI, testFolder::setWso2mi);
        analyzeSubTestFolder(testsPath, JAVA, testFolder::setJava);
        directoryTree.setTests(testFolder);
    }

    private static void analyzeSubTestFolder(String testPath, String testName, Consumer<FolderNode> setter) {

        File subFolder = new File(testPath + File.separator + testName);
        if (subFolder != null && subFolder.exists() && !subFolder.isHidden()) {
            String folderName = subFolder.getName();
            FolderNode testsFolderNode = new FolderNode(folderName, testPath);
            traverseFolder(testsFolderNode, null);
            setter.accept(testsFolderNode);
        }
    }

    private static void traverseFolder(FolderNode folderNode, IntegrationDirectoryTree directoryTree) {

        File[] listOfFiles = folderNode.listFiles();
        for (File file : listOfFiles) {
            if (Utils.isRegistryPropertiesFile(file)) {
                continue;
            }
            if (file.getAbsolutePath().endsWith(Path.of(Constant.RESOURCES, Constant.ARTIFACT_XML).toString()) ||
                    file.getAbsolutePath().endsWith(Path.of(Constant.RESOURCES, Constant.REGISTRY, Constant.ARTIFACT_XML).toString())) {
                continue;
            }
            if (file.isFile() && !file.isHidden()) {
                String name = file.getName();
                String filePath = file.getAbsolutePath();
                FileNode fileNodeComponent = new FileNode(name, filePath);
                folderNode.addFile(fileNodeComponent);
                if (directoryTree != null) {
                    if (!artifactResourcePaths.contains(filePath)) {
                        addResourceToIntegrationTree(directoryTree, filePath);
                        artifactResourcePaths.add(filePath);
                    }
                }
            } else if (file.isDirectory() && !file.isHidden()) {
                String name = file.getName();
                String folderPath = file.getAbsolutePath();
                FolderNode subFolderNode = new FolderNode(name, folderPath);
                folderNode.addFolder(subFolderNode);
                traverseFolder(subFolderNode, directoryTree);
            }
        }
    }

    private static void addResourceToIntegrationTree(IntegrationDirectoryTree directoryTree, String path) {

        if (path.endsWith(".xml")) {
            try {
                File file = new File(path);
                DOMDocument domDocument = Utils.getDOMDocument(file);
                DOMElement rootElement = Utils.getRootElementFromConfigXml(domDocument);

                if (rootElement != null) {
                    String type = rootElement.getNodeName();
                    String name = file.getName();
                    type = getType(type);
                    Node regNode = createRegistryNode(name, type, path);
                    String methodName = "add" + type;
                    Method method = directoryTree.getClass().getMethod
                            (methodName, Node.class);
                    method.invoke(directoryTree, regNode);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error while reading file content", e);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "Error while trying to execute method.", e);
            }
        }
    }

    private static Node createRegistryNode(String name, String type, String path) {

        StringBuilder key = new StringBuilder();
        if (path.contains(Constant.GOV)) {
            key.append(Constant.GOV);
        } else {
            key.append(Constant.CONF);
        }
        String key1 = path.substring(path.indexOf(key.toString()) + key.length() + 1,
                path.lastIndexOf(File.separator) + 1);
        key.append(":");
        key.append(key1);
        Node node = createEsbComponent(type, name, path);
        Node registry = new RegistryNode(node, key.toString());
        return registry;
    }

    private static Node createEsbComponent(String type, String name, String path) {

        String artifactName;
        String nodeType = Utils.addUnderscoreBetweenWords(type).toUpperCase();
        try {
            artifactName = getArtifactName(type, path);
        } catch (IOException e) {
            //Could not read artifact name. Ignoring the file as it is invalid.
            Node invalidNode = new Node(nodeType, name, path);
            invalidNode.setFaulty(Boolean.TRUE);
            return invalidNode;
        }
        if (artifactName == null) artifactName = name;
        Node component = new Node(nodeType, artifactName, path);
        setSubType(component, type, path);
        if (Constant.API.equalsIgnoreCase(type) || Constant.SEQUENCE.equalsIgnoreCase(type) ||
                Constant.PROXY_SERVICE.equalsIgnoreCase(type) || Constant.INBOUND_ENDPOINT.equalsIgnoreCase(type)) {
            AdvancedNode advancedNode = createAdvancedEsbComponent(component, type, path);
            return advancedNode;
        } else if (Constant.LOCAL_ENTRY.equalsIgnoreCase(type)) {
            Node localEntry = createLocalEntry(component, path);
            return localEntry;
        }

        return component;
    }

    private static void setSubType(Node component, String type, String path) {

        File file = new File(path);
        try {
            DOMDocument domDocument = Utils.getDOMDocument(file);
            STNode stNode = SyntaxTreeGenerator.buildTree(domDocument.getDocumentElement());
            switch (type) {
                case "Endpoint":
                    NamedEndpoint endpoint = (NamedEndpoint) stNode;
                    String endpointType = endpoint.getType().name();
                    component.setSubType(endpointType);
                    break;
                case "Template":
                    Template template = (Template) stNode;
                    String templateType = template.getType().name();
                    component.setSubType(templateType);
                    break;
                case "MessageProcessor":
                    MessageProcessor messageProcessor = (MessageProcessor) stNode;
                    String mpType = messageProcessor.getType().name();
                    component.setSubType(mpType);
                    break;
                case "MessageStore":
                    MessageStore messageStore = (MessageStore) stNode;
                    String messageStoreType = messageStore.getType().name();
                    component.setSubType(messageStoreType);
                    break;
                case "InboundEndpoint":
                    InboundEndpoint inboundEndpoint = (InboundEndpoint) stNode;
                    String ibType = inboundEndpoint.getType();
                    component.setSubType(ibType);
                    break;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while reading artifact", e);
        }
    }

    private static AdvancedNode createAdvancedEsbComponent(Node component, String type, String path) {

        AdvancedNode advancedNode;
        switch (type.toLowerCase()) {
            case Constant.API:
                String context = getApiContext(path);
                advancedNode = new APINode(component);
                ((APINode) advancedNode).setContext(context);
                break;
            case Constant.SEQUENCE:
                advancedNode = new SequenceNode(component);
                if (mainSequence != null) {
                    if (mainSequence.equalsIgnoreCase(advancedNode.getName())) {
                        ((SequenceNode) advancedNode).setMainSequence(Boolean.TRUE);
                    }
                }
                break;
            default:
                advancedNode = new AdvancedNode(component);
        }
        File file = new File(path);
        if (file.isFile() && !file.isHidden()) {
            DOMDocument domDocument = null;
            try {
                domDocument = Utils.getDOMDocument(file);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error while reading file content", e);
            }
            DOMElement rootElement = Utils.getRootElementFromConfigXml(domDocument);
            if (Constant.API.equalsIgnoreCase(type)) {
                addResources(rootElement, advancedNode);
            }
        }
        return advancedNode;
    }

    private static Node createLocalEntry(Node component, String path) {

        File file = new File(path);
        try {
            DOMDocument domDocument = Utils.getDOMDocument(file);
            if (domDocument != null) {
                DOMElement rootElement = domDocument.getDocumentElement();
                String key = rootElement.getAttribute(Constant.KEY);
                DOMElement childElement = Utils.getFirstElement(rootElement);
                if (childElement != null) {
                    String entryTag = childElement.getNodeName();
                    Pattern pattern = Pattern.compile("(.*)\\.init");
                    Matcher matcher = pattern.matcher(entryTag);
                    if (matcher.find()) {
                        String connectorName = matcher.group(1);
                        String connectionType = getConnectionType(childElement);
                        ConnectionNode connectionNode = new ConnectionNode(key, path, connectorName, connectionType);
                        return connectionNode;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while reading file content", e);
        }
        return component;
    }

    private static String getConnectionType(DOMElement element) {

        List<DOMNode> children = element.getChildren();
        if (children != null) {
            for (DOMNode child : children) {
                String nodeName = child.getNodeName();
                if ("connectionType".equals(nodeName)) {
                    String connectionType = Utils.getInlineString(child.getFirstChild());
                    return connectionType;
                }
            }
        }
        return null;
    }

    private static String getApiContext(String path) {

        File file = new File(path);
        DOMDocument domDocument = null;
        try {
            domDocument = Utils.getDOMDocument(file);
            DOMNode node = Utils.getChildNodeByName(domDocument, Constant.API);
            if (node != null) {
                String context = node.getAttribute(Constant.CONTEXT);
                return context;
            }
        } catch (IOException e) {
            //ignore
        }
        return null;
    }

    private static String getArtifactName(String type, String path) throws IOException {

        File file = new File(path);
        DOMDocument domDocument = Utils.getDOMDocument(file);

        String tag = getArtifactTag(type);

        DOMNode node = Utils.getChildNodeByName(domDocument, tag);
        if (node != null) {
            if (Constant.API.equalsIgnoreCase(type)) {
                return getApiArtifactName(node);
            } else {
                return getNonApiArtifactName(node);
            }
        } else {
            throw new IOException("Invalid artifact in the artifact folder: " + type);
        }
    }

    private static String getApiArtifactName(DOMNode node) {

        StringBuilder name = new StringBuilder();
        name.append(node.getAttribute(Constant.NAME));
        if (node.hasAttribute(Constant.VERSION)) {
            name.append(":v").append(node.getAttribute(Constant.VERSION));
        }
        return name.toString();
    }

    private static String getNonApiArtifactName(DOMNode node) {

        String name = node.getAttribute(Constant.NAME);
        if (name == null) {
            name = node.getAttribute(Constant.KEY);
            if (name == null) {
                DOMNode nameElt = Utils.getChildNodeByName(node, Constant.NAME);
                if (nameElt != null) {
                    name = Utils.getInlineString(nameElt.getFirstChild());
                }
            }
        }
        return name;
    }

    private static String getArtifactTag(String type) {

        if (Constant.PROXY_SERVICE.equalsIgnoreCase(type)) {
            return Constant.PROXY;
        } else if (Constant.DATA_SERVICE.equalsIgnoreCase(type)) {
            return Constant.DATA;
        }
        return type;
    }

    private static void addResources(DOMElement rootElement, AdvancedNode advancedNode) {

        List<DOMNode> apiChildren = rootElement.getChildren();
        for (DOMNode child : apiChildren) {
            String name = child.getNodeName();
            if (Constant.RESOURCE.equalsIgnoreCase(name)) {
                String methods = child.getAttribute(Constant.METHODS);
                String uriTemplate = child.getAttribute(Constant.URI_TEMPLATE);
                String urlMapping = child.getAttribute(Constant.URL_MAPPING);
                APIResource resource = new APIResource(methods, uriTemplate, urlMapping);
                ((APINode) advancedNode).addResource(resource);
            }
        }
    }

    private static void extractClassMediators(JsonNode mediatorFolders, ArrayNode classMediatorArray) {
        for (JsonNode classMediatorFolder : mediatorFolders) {
            if (classMediatorFolder.has(Constant.FILES)) {
                classMediatorArray.addAll((ArrayNode) classMediatorFolder.path(Constant.FILES));
            }

            if (classMediatorFolder.has(Constant.FOLDERS)) {
                extractClassMediators(classMediatorFolder.path(Constant.FOLDERS), classMediatorArray);
            }
        }
    }

    private static void extractBallerinaModules(JsonNode moduleFolders, ArrayNode ballerinaModuleArray) {
        for (JsonNode ballerinaModuleFolder : moduleFolders) {
            if (ballerinaModuleFolder.has(Constant.FILES)) {
                ballerinaModuleArray.addAll((ArrayNode) ballerinaModuleFolder.path(Constant.FILES));
            }

            if (ballerinaModuleFolder.has(Constant.FOLDERS)) {
                extractBallerinaModules(ballerinaModuleFolder.path(Constant.FOLDERS), ballerinaModuleArray);
            }
        }
    }
}
