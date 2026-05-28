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
package org.eclipse.lemminx.customservice.synapse.parser;

public class Constants {

    private Constants() { }

    public static final String SRC = "src";
    public static final String MAIN = "main";
    public static final String WSO2_MI = "wso2mi";
    public static final String RESOURCES = "resources";
    public static final String CONF = "conf";
    public static final String CONFIG_FILE = "config.properties";
    public static final String POM_FILE = "pom.xml";
    public static final String DEPENDENCY = "dependency";
    public static final String PLUGINS = "plugins";
    public static final String PLUGIN = "plugin";
    public static final String REPOSITORY = "repository";
    public static final String PLUGIN_REPOSITORY = "pluginRepository";
    public static final String PROPERTIES = "properties";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String EXCLUSIONS = "exclusions";
    public static final String EXCLUSION = "exclusion";

    public static final String VERSION = "version";

    public static final String NAME = "name";

    public static final String TYPE = "type";
    public static final String TEST_SERVER_TYPE = "testServerType";
    public static final String PRO_TEST_SERVER_TYPE = "test.server.type";
    public static final String TEST_SERVER_HOST = "testServerHost";
    public static final String PRO_TEST_SERVER_HOST = "test.server.host";
    public static final String TEST_SERVER_PORT = "testServerPort";
    public static final String PRO_TEST_SERVER_PORT = "test.server.port";
    public static final String TEST_SERVER_PATH = "testServerPath";
    public static final String PRO_TEST_SERVER_PATH = "test.server.path";
    public static final String TEST_SERVER_VERSION = "testServerVersion";
    public static final String PRO_TEST_SERVER_VERSION = "test.server.version";
    public static final String TEST_SERVER_DOWNLOAD_LINK = "testServerDownloadLink";
    public static final String SKIP_TEST = "mavenTestSkip";
    public static final String MAVEN_SKIP_TEST = "maven.test.skip";
    public static final String VSCODE_CAR_PLUGIN = "vscode-car-plugin";
    public static final String MI_CONTAINER_CONFIG_MAPPER = "mi-container-config-mapper";
    public static final String SYNAPSE_UNIT_TEST_MAVEN_PLUGIN = "synapse-unit-test-maven-plugin";
    public static final String GROUP_ID = "groupId";
    public static final String PACKAGING = "packaging";
    public static final String DESCRIPTION = "description";
    public static final String FULL_RANGE = "fullRange";

    public static final String ZIP = "zip";
    public static final String CAR = "car";
    public static final String DOCKER_FILE_BASE_IMAGE = "dockerfile.base.image";
    public static final String FAT_CAR_ENABLE = "fat.car.enable";
    public static final String VERSIONED_DEPLOYMENT = "versionedDeployment";
    public static final String CIPHER_TOOL_ENABLE = "ciphertool.enable";
    public static final String KEY_STORE_ALIAS = "keystore.alias";
    public static final String KEY_STORE_PASSWORD = "keystore.password";
    public static final String KEY_STORE_NAME = "keystore.name";
    public static final String KEY_STORE_TYPE = "keystore.type";
    public static final String PROJECT_RUNTIME_VERSION = "project.runtime.version";
    public static final String CAR_PLUGIN_VERSION = "car.plugin.version";
    public static final String DOCKER_MAVEN_PLUGIN = "docker-maven-plugin";
    public static final String HASH = "#";
    public static final String COLON = ":";
    public static final String NEW_LINE = "\n";
    public static final String DEPENDENCIES = "dependencies";
    public static final String LINE_SEPARATOR = "line.separator";
    public static final String EMPTY = "";
    public static final String DEPENDENCY_END_TAG = "</dependency>";
    public static final String DEPENDENCIES_END_TAG = "</dependencies>";
    public static final String DEPENDENCIES_START_TAG = "<dependencies>";
    public static final String PROPERTIES_END_TAG = "</properties>";
    public static final String YES = "yes";
    public static final String PROJECT_RUNTIME_VERSION_CONSTANT = "${project.runtime.version}";
    public static final String PROJECT_TEST_SERVER_VERSION_CONSTANT_WITH_ESCAPE = "\\$\\{test.server.version\\}";
    public static final String PROJECT_TEST_SERVER_VERSION_CONSTANT = "${test.server.version}";
    public static final String PROJECT_VERSION_CONSTANT = "${project.version}";
    public static final String PROJECT_ARTIFACT_ID_CONSTANT = "${project.artifactId}";
}
