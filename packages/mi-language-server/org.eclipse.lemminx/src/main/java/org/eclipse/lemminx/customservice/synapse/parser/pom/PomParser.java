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
package org.eclipse.lemminx.customservice.synapse.parser.pom;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.parser.Constants;
import org.eclipse.lemminx.customservice.synapse.parser.DeployPluginDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.PropertyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdatePropertyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PomParser {

    private static final Logger LOGGER = Logger.getLogger(PomParser.class.getName());
    private static OverviewPageDetailsResponse pomDetailsResponse;
    private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static boolean hasDependencies = false;
    private static boolean hasProperties = false;

    public static void getPomDetails(String projectUri, OverviewPageDetailsResponse detailsResponse) {
        pomDetailsResponse = detailsResponse;
        extractPomContent(projectUri);
    }

    public static UpdateResponse updateProperty(String projectUri, UpdatePropertyRequest request) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            UpdateResponse updateResponse = new UpdateResponse();
            StringBuilder elementInString = new StringBuilder();
            List<String> pomContent = readPom(projectUri);
            assert pomContent != null;
            Element propertiesElement = null;
            Range initialRange = getPropertiesRange(pomContent);
            if (!hasProperties) {
                propertiesElement = document.createElement(Constants.PROPERTIES);
            }
            for (PropertyDetails property : request.properties) {
                if (property != null && property.getRange() != null) {
                    updateResponse.add(new TextEdit(property.getRange(),
                            elementToString(createPropertyElement(document, property))));
                } else {
                    if (propertiesElement != null) {
                        propertiesElement.appendChild(createPropertyElement(document, property));
                    } else {
                        elementInString.append(elementToString(createPropertyElement(document, property)));
                    }
                }
            }
            String value =
                    (propertiesElement != null) ? elementToString(propertiesElement) : elementInString.toString();
            if (StringUtils.isEmpty(value)) {
                return null;
            }
            // Add the new content inside the <properties> section
            updateResponse.add(new TextEdit(new Range(initialRange.getStart(), initialRange.getStart()), value));
            return updateResponse;
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error parsing the POM file : " + e.getMessage());
            return null;
        }
    }

    public static UpdateResponse updateDependency(String projectUri, UpdateDependencyRequest request) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            List<String> pomContent;
            UpdateResponse updateResponse= new UpdateResponse();
            Element dependenciesElement = null;
            StringBuilder elementInString = new StringBuilder();
            pomContent = readPom(projectUri);
            assert pomContent != null;
            Position initialRange = getDependenciesStartPosition(pomContent);
            if (!hasDependencies) {
                dependenciesElement = document.createElement(Constants.DEPENDENCIES);
            }
            for (DependencyDetails dependency : request.dependencies) {
                if (dependency.getRange() != null) {
                    updateResponse.add(new TextEdit(dependency.getRange(),
                            elementToString(createDependencyElement(document, dependency))));
                } else {
                    if (dependenciesElement != null) {
                        dependenciesElement.appendChild(createDependencyElement(document, dependency));
                    } else {
                        elementInString.append(elementToString(createDependencyElement(document, dependency)));
                    }
                }
            }
            String value;
            if (dependenciesElement != null) {
                value = elementToString(dependenciesElement);
            } else {
                value = elementInString.toString();
            }
            if (value == null) {
                return null;
            }
            updateResponse.add(new TextEdit(new Range(initialRange, initialRange), value));
            return updateResponse;
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error parsing the POM file : " + e.getMessage());
            return null;
        }
    }

   public static DeployPluginDetails addCarDeployPluginToPom(File pomFile, DeployPluginDetails pluginDetails) {
       try {
           String content = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()));
           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = factory.newDocumentBuilder();
           Document doc = builder.parse(new ByteArrayInputStream(content.getBytes()));
           doc.getDocumentElement().normalize();

           Document snippetDoc = builder.newDocument();
           Element plugin = snippetDoc.createElement(Constants.PLUGIN);
           appendTextElement(snippetDoc, plugin, Constant.GROUP_ID_KEY, "org.wso2.maven");
           appendTextElement(snippetDoc, plugin, Constant.ARTIFACT_ID, "maven-car-deploy-plugin");
           appendTextElement(snippetDoc, plugin, Constant.VERSION, "5.2.44");

           Element executions = snippetDoc.createElement("executions");
           Element execution = snippetDoc.createElement("execution");
           appendTextElement(snippetDoc, execution, Constant.ID, "car-deploy");
           appendTextElement(snippetDoc, execution, "phase", "deploy");
           Element goals = snippetDoc.createElement("goals");
           appendTextElement(snippetDoc, goals, "goal", "deploy-car");
           execution.appendChild(goals);
           executions.appendChild(execution);
           plugin.appendChild(executions);

           Element dependencies = snippetDoc.createElement(Constants.DEPENDENCIES);
           Element dependency = snippetDoc.createElement(Constant.DEPENDENCY);
           appendTextElement(snippetDoc, dependency, Constant.GROUP_ID_KEY, "com.sun.activation");
           appendTextElement(snippetDoc, dependency, Constant.ARTIFACT_ID, "javax.activation");
           appendTextElement(snippetDoc, dependency, Constant.VERSION, "1.2.0");
           dependencies.appendChild(dependency);
           plugin.appendChild(dependencies);

           Element configuration = snippetDoc.createElement("configuration");
           Element carbonServers = snippetDoc.createElement("carbonServers");
           Element carbonServer = snippetDoc.createElement("CarbonServer");

           appendTextElement(snippetDoc, carbonServer, "trustStorePath", pluginDetails.getTruststorePath());
           appendTextElement(snippetDoc, carbonServer, "trustStorePassword", pluginDetails.getTruststorePassword());
           appendTextElement(snippetDoc, carbonServer, "trustStoreType", pluginDetails.getTruststoreType());
           appendTextElement(snippetDoc, carbonServer, "serverUrl", pluginDetails.getServerUrl());
           appendTextElement(snippetDoc, carbonServer, "userName", pluginDetails.getUsername());
           appendTextElement(snippetDoc, carbonServer, "password", pluginDetails.getPassword());
           appendTextElement(snippetDoc, carbonServer, "serverType", pluginDetails.getServerType());
           appendTextElement(snippetDoc, carbonServer, "operation", "deploy");

           carbonServers.appendChild(carbonServer);
           configuration.appendChild(carbonServers);
           plugin.appendChild(configuration);

           Transformer transformer = TransformerFactory.newInstance().newTransformer();
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");
           transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-space", "4");
           transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

           StringWriter writer = new StringWriter();
           transformer.transform(new DOMSource(plugin), new StreamResult(writer));
           String pluginSnippet = writer.toString();

           List<String> pomContent = readPom(pomFile.getParentFile().getAbsolutePath());
           Range pluginRange = getDeployPluginRange(pomContent);
           if (pluginRange != null) {
               return new DeployPluginDetails(new TextEdit(new Range(pluginRange.getStart(),
                       new Position(pluginRange.getEnd().getLine(),
                       pluginRange.getEnd().getCharacter() + pluginSnippet.length() + 1)), pluginSnippet));
           } else {
               Position newPluginPosition = getNewPluginAddPosition(pomContent);
               return new DeployPluginDetails(new TextEdit(new Range(newPluginPosition,
                        new Position(newPluginPosition.getLine(), newPluginPosition.getCharacter())), pluginSnippet));
           }

       } catch (ParserConfigurationException | TransformerException e) {
           LOGGER.log(Level.SEVERE, "Error occurred while generating plugin configuration: " + e.getMessage());
       } catch (XMLStreamException | SAXException | IOException e) {
           LOGGER.log(Level.SEVERE, "Error occurred while reading pom configuration: " + e.getMessage());
       }
       return null;
   }

   public static DeployPluginDetails extractCarDeployPluginFields(File pomFile) {
       try {
           DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = factory.newDocumentBuilder();
           Document doc = builder.parse(pomFile);
           doc.getDocumentElement().normalize();

           NodeList plugins = doc.getElementsByTagName(Constants.PLUGIN);
           for (int i = 0; i < plugins.getLength(); i++) {
               Element plugin = (Element) plugins.item(i);
               String groupId = getTextValue(plugin, Constant.GROUP_ID_KEY);
               String artifactId = getTextValue(plugin, Constant.ARTIFACT_ID);

               if ("org.wso2.maven".equals(groupId) && "maven-car-deploy-plugin".equals(artifactId)) {
                   Element configuration = getFirstChildElementByTagName(plugin, "configuration");
                   Element carbonServers = getFirstChildElementByTagName(configuration, "carbonServers");
                   Element carbonServer = getFirstChildElementByTagName(carbonServers, "CarbonServer");

                   if (carbonServer != null) {
                       return new DeployPluginDetails(
                               getTextValue(carbonServer, "trustStorePath"),
                               getTextValue(carbonServer, "trustStorePassword"),
                               getTextValue(carbonServer, "trustStoreType"),
                               getTextValue(carbonServer, "serverUrl"),
                               getTextValue(carbonServer, "userName"),
                               getTextValue(carbonServer, "password"),
                               getTextValue(carbonServer, "serverType"));
                   }
               }
           }
       } catch (ParserConfigurationException | IOException | SAXException e) {
           LOGGER.log(Level.SEVERE, "Error extracting CAR deploy plugin configurations: " + e.getMessage());
       }
       return null;
   }

    public static Range getDeployPluginRange(List<String> pomContent) throws XMLStreamException {
        XMLStreamReader reader = getXMLReader(pomContent);
        boolean insidePlugin = false;
        boolean insideArtifactId = false;
        boolean isTargetPlugin = false;
        Position pluginStart = null;
        Position pluginEnd = null;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if (localName.equals(Constants.PLUGIN)) {
                    insidePlugin = true;
                    Location startLoc = reader.getLocation();
                    pluginStart = new Position(startLoc.getLineNumber(), startLoc.getColumnNumber());
                } else if (insidePlugin && localName.equals(Constants.ARTIFACT_ID)) {
                    insideArtifactId = true;
                }
            } else if (event == XMLStreamConstants.CHARACTERS) {
                if (insidePlugin && insideArtifactId) {
                    String text = reader.getText().trim();
                    if ("maven-car-deploy-plugin".equals(text)) {
                        isTargetPlugin = true;
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                String localName = reader.getLocalName();
                if (localName.equals(Constants.ARTIFACT_ID)) {
                    insideArtifactId = false;
                } else if (localName.equals(Constants.PLUGIN)) {
                    if (insidePlugin && isTargetPlugin) {
                        Location endLoc = reader.getLocation();
                        pluginEnd = new Position(endLoc.getLineNumber(), endLoc.getColumnNumber() + "</plugin>".length());
                        break;
                    }
                    insidePlugin = false;
                    isTargetPlugin = false;
                    pluginStart = null;
                }
            }
        }
        if (pluginStart != null && pluginEnd != null) {
            return new Range(pluginStart, pluginEnd);
        }
        return null;
    }

    public static TextEdit removeDeployPlugin(File pomFile) {
        try {
            Range pluginRange = getDeployPluginRange(readPom(pomFile.getParentFile().getAbsolutePath()));
            if (pluginRange != null) {
                return new TextEdit(pluginRange, StringUtils.EMPTY);
            }
        } catch (XMLStreamException e) {
            LOGGER.log(Level.SEVERE, "Error getting plugin range from POM file: " + e.getMessage());
        }
        Position defaultPosition = new Position(0,0);
        return new TextEdit(new Range(defaultPosition, defaultPosition), StringUtils.EMPTY);
    }

    private static List<String> readPom(String projectUri) {
        File pomFile = new File(projectUri + File.separator + Constants.POM_FILE);
        if (!isPomFileExist(pomFile)) {
            return null;
        }
        try {
            return Files.readAllLines(pomFile.toPath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error modifying the POM file: " + e.getMessage());
            return null;
        }
    }

    private static Range getPropertiesRange(List<String> pomContent) {
        if (pomContent == null || pomContent.isEmpty()) {
            return null;
        }
        XMLStreamReader reader = null;
        try {
            reader = getXMLReader(pomContent);
            Deque<String> elementStack = new ArrayDeque<>();
            int startLine = -1, startChar = -1;
            int endLine = -1, endChar = -1;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    elementStack.push(localName);

                    if (Constants.PROPERTIES.equals(localName) && elementStack.size() == 2 &&
                            Constant.PROJECT.equals(elementStack.peekLast())) {
                        hasProperties = true;
                        startLine = reader.getLocation().getLineNumber();
                        startChar = reader.getLocation().getColumnNumber() + localName.length() + 2; // After <properties>
                    }
                }

                if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();

                    if (Constants.PROPERTIES.equals(localName) && startLine != -1 && elementStack.size() == 2 &&
                            Constant.PROJECT.equals(elementStack.peekLast())) {
                        endLine = reader.getLocation().getLineNumber();
                        endChar = reader.getLocation().getColumnNumber(); // At </properties>
                        break;
                    }

                    elementStack.pop();
                }
            }

            if (startLine == -1 || endLine == -1 || startChar == -1 || endChar == -1) {
                return null;
            }

            Position start = new Position(startLine, startChar);
            Position end = new Position(endLine, endChar);
            return new Range(start, end);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the POM file: " + e.getMessage());
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException e) {
                    LOGGER.log(Level.WARNING, "Error closing XMLStreamReader: " + e.getMessage());
                }
            }
        }
    }

    private static Position getDependenciesStartPosition(List<String> pomContent) {
        try {
            XMLStreamReader reader = getXMLReader(pomContent);
            int depth = 0;
            boolean insideProject = false;
            Position propertiesEndPosition = null;

            while (reader.hasNext()) {
                int eventType = reader.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (localName.equals(Constant.PROJECT)) {
                        insideProject = true;
                        depth = 1;
                    } else if (insideProject) {
                        depth++;
                        if (localName.equals(Constants.DEPENDENCIES) && depth == 2) {
                            Location location = reader.getLocation();
                            hasDependencies = true;
                            int startLine = location.getLineNumber();
                            int startColumn = location.getColumnNumber();
                            int endColumn = startColumn + localName.length() + 2;
                            return new Position(startLine, endColumn);
                        }
                    }
                } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (localName.equals(Constants.PROPERTIES) && depth == 2) {
                        Location location = reader.getLocation();
                        hasDependencies = false;
                        int startLine = location.getLineNumber();
                        int startColumn = location.getColumnNumber();
                        int endColumn = startColumn + localName.length() + 4;
                        propertiesEndPosition = new Position(startLine, endColumn);
                    }
                    depth--;
                }
            }
            return propertiesEndPosition != null ? propertiesEndPosition : new Position(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the POM file: " + e.getMessage());
            return null;
        }
    }

    private static Element createPropertyElement(Document document, PropertyDetails propertyDetails) {
        Element property = document.createElement(propertyDetails.getName());
        property.setTextContent(propertyDetails.getValue());
        return property;
    }

    private static Element createDependencyElement(Document document, DependencyDetails dependencyDetails) {
        Element dependency = document.createElement(Constants.DEPENDENCY);
        Element groupId = document.createElement(Constants.GROUP_ID);
        groupId.setTextContent(dependencyDetails.getGroupId());
        dependency.appendChild(groupId);
        Element artifactId = document.createElement(Constants.ARTIFACT_ID);
        artifactId.setTextContent(dependencyDetails.getArtifact());
        dependency.appendChild(artifactId);
        Element version = document.createElement(Constants.VERSION);
        version.setTextContent(dependencyDetails.getVersion());
        dependency.appendChild(version);
        if (dependencyDetails.getType() != null) {
            Element type = document.createElement(Constants.TYPE);
            type.setTextContent(dependencyDetails.getType() );
            dependency.appendChild(type);
            Element exclusion = document.createElement(Constants.EXCLUSION);
            Element conGroupId = document.createElement(Constants.GROUP_ID);
            conGroupId.setTextContent("*");
            exclusion.appendChild(conGroupId);
            Element conArtifactId = document.createElement(Constants.ARTIFACT_ID);
            conArtifactId.setTextContent("*");
            exclusion.appendChild(conArtifactId);
            Element exclusions = document.createElement(Constants.EXCLUSIONS);
            exclusions.appendChild(exclusion);
            dependency.appendChild(exclusions);
        }
        return dependency;
    }

    private static void extractPomContent(String projectUri) {
        try {
            File pomFile = new File(projectUri + File.separator + Constants.POM_FILE);
            if (!isPomFileExist(pomFile)) {
                return;
            }
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            PluginHandler handler = new PluginHandler(pomDetailsResponse);
            saxParser.parse(pomFile, handler);
        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE, "Error configuring the parser for the POM file: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error accessing the POM file: " + e.getMessage());
        } catch (SAXException e) {
            LOGGER.log(Level.SEVERE, "Error parsing the POM file: " + e.getMessage());
        }
    }

    private static boolean isPomFileExist(File pomFile) {
        if (!pomFile.exists()) {
            LOGGER.log(Level.SEVERE, "POM file does not exist: " + pomFile.getAbsolutePath());
            return false;
        }
        return true;
    }

    private static String elementToString(Element element) {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, Constants.YES);
            transformer.setOutputProperty(OutputKeys.INDENT, Constants.YES);
            DOMSource domSource = new DOMSource(element);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(domSource, result);
            return writer.toString().trim();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing the XML element: " + e.getMessage());
            return null;
        }
    }

    private static Position getNewPluginAddPosition(List<String> pomContent) {
        try {
            XMLStreamReader reader = getXMLReader(pomContent);
            boolean inProfiles = false;
            boolean inProfile = false;
            boolean isDefaultProfile = false;
            boolean inId = false;
            boolean inPlugins = false;
            Position lastPluginEnd = null;
            int depth = 0;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String localName = reader.getLocalName();
                    if (localName.equals(Constant.PROFILES)) {
                        inProfiles = true;
                    } else if (inProfiles && localName.equals(Constant.PROFILE)) {
                        inProfile = true;
                        isDefaultProfile = false;
                    } else if (inProfile && localName.equals(Constant.ID)) {
                        inId = true;
                    } else if (inProfile && isDefaultProfile && localName.equals(Constants.PLUGINS)) {
                        inPlugins = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if (inId && inProfile) {
                        String idText = reader.getText().trim();
                        if (idText.equals(Constant.DEFAULT)) {
                            isDefaultProfile = true;
                        }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();
                    if (localName.equals(Constant.ID)) {
                        inId = false;
                    } else if (localName.equals(Constants.PLUGINS)) {
                        inPlugins = false;
                    } else if (localName.equals(Constant.PROFILE)) {
                        inProfile = false;
                    } else if (localName.equals(Constant.PROFILES)) {
                        inProfiles = false;
                    }
                    if (inPlugins && localName.equals(Constants.PLUGIN)) {
                        Location location = reader.getLocation();
                        lastPluginEnd = new Position(location.getLineNumber(), location.getColumnNumber() + localName.length() + 3);
                    }
                    depth--;
                }
            }
            return lastPluginEnd != null ? lastPluginEnd : new Position(0, 0);
        } catch (XMLStreamException e) {
            LOGGER.log(Level.SEVERE, "Error reading the POM file: " + e.getMessage());
            return null;
        }
    }

    private static void appendTextElement(Document doc, Element parent, String tag, String value) {
        Element elem = doc.createElement(tag);
        elem.setTextContent(value);
        parent.appendChild(elem);
    }

    private static String getTextValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private static Element getFirstChildElementByTagName(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        for (int i = 0; i < children.getLength(); i++) {
            Node item = children.item(i);
            if (item.getParentNode().equals(parent)) {
                return (Element) item;
            }
        }
        return null;
    }

    private static XMLStreamReader getXMLReader(List<String> pomContent) throws XMLStreamException {
        String xml = String.join("\n", pomContent);
        WstxInputFactory factory = new WstxInputFactory();
        return factory.createXMLStreamReader(new StringReader(xml));
    }
}
