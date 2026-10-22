/**
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// This file is the source of truth for the root pom.xml template used during
// migration. It is intentionally kept in sync with
// packages/mi-extension/src/util/templates.ts — do not diverge the template
// logic between the two.

import * as fs from 'fs';
import * as path from 'path';
import { parseStringPromise } from 'xml2js';

export const LATEST_CAR_PLUGIN_VERSION = '5.4.21';
const RUNTIME_VERSION_450 = '4.5.0';

// ---------------------------------------------------------------------------
// Helpers (mirror of extension's onboardingUtils / templates)
// ---------------------------------------------------------------------------

function compareVersions(v1: string, v2: string): number {
    const getVersionNumbers = (str: string): string => {
        const match = str.match(/(\d+(\.\d+)*)/);
        return match ? match[0] : '0';
    };
    const parts1 = getVersionNumbers(v1).split('.').map(p => parseInt(p, 10));
    const parts2 = getVersionNumbers(v2).split('.').map(p => parseInt(p, 10));
    for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
        const p1 = parts1[i] || 0;
        const p2 = parts2[i] || 0;
        if (p1 > p2) return 1;
        if (p1 < p2) return -1;
    }
    return 0;
}

function isConsolidatedProject(filePath: string): boolean {
    try {
        if (!filePath) return false;
        const pomPath = path.join(filePath, 'pom.xml');
        if (!fs.existsSync(pomPath)) return false;
        const pomContent = fs.readFileSync(pomPath, 'utf-8');
        const match = pomContent.match(
            /<is\.consolidated\.project>\s*(true|false)\s*<\/is\.consolidated\.project>/i
        );
        return match ? match[1].toLowerCase() === 'true' : false;
    } catch {
        return false;
    }
}

interface PomInfo {
    groupId: string;
    artifactId: string;
    version: string;
}

async function getPomInfoFromFile(pomPath: string): Promise<PomInfo> {
    const xmlContent = fs.readFileSync(pomPath, 'utf-8');
    const parsed = await parseStringPromise(xmlContent, { explicitArray: false });
    const project = parsed.project;
    return {
        groupId: project.groupId || project.parent?.groupId,
        artifactId: project.artifactId,
        version: project.version || project.parent?.version,
    };
}

// ---------------------------------------------------------------------------
// Root pom.xml template — exact mirror of the extension's rootPomXmlContent
// ---------------------------------------------------------------------------

export const rootPomXmlContent = async (
    projectName: string,
    groupID: string,
    artifactID: string,
    projectUuid: string,
    version: string,
    miVersion: string,
    initialDependencies: string,
    directory?: string
): Promise<string> => {
    const addDeploymentType = compareVersions(miVersion, RUNTIME_VERSION_450) >= 0;
    const indent = addDeploymentType ? '      ' : '  ';
    let parentGroupID: string | undefined;
    let parentArtifactID: string | undefined;
    let parentVersion: string | undefined;
    let isModule = false;

    if (directory) {
        if (isConsolidatedProject(directory)) {
            const parentPomInfo = await getPomInfoFromFile(path.join(directory, 'pom.xml'));
            parentGroupID = parentPomInfo.groupId;
            parentArtifactID = parentPomInfo.artifactId;
            parentVersion = parentPomInfo.version;
            isModule = true;
        }
    }

    const dockerProfile = `
      <profile>
        <id>docker</id>
        <build>
          <plugins>
              <!-- Compile and build the class mediator jars -->
              <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-compiler-plugin</artifactId>
              <executions>
                <execution>
                  <id>default-compile</id>
                  <phase>generate-sources</phase>
                  <goals>
                    <goal>compile</goal>
                  </goals>
                </execution>
              </executions>
              <configuration>
                <source>1.8</source>
                <target>1.8</target>
              </configuration>
            </plugin>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-jar-plugin</artifactId>
              <configuration>
                <skipIfEmpty>true</skipIfEmpty>
              </configuration>
              <executions>
                <execution>
                  <id>default-jar</id>
                  <phase>generate-sources</phase>
                  <goals>
                    <goal>jar</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>
            <!-- Build the Carbon Application -->
            <plugin>
              <groupId>org.wso2.maven</groupId>
              <artifactId>vscode-car-plugin</artifactId>
              <version>\${car.plugin.version}</version>
              <extensions>true</extensions>
              <executions>
                <execution>
                  <phase>generate-sources</phase>
                  <goals>
                    <goal>car</goal>
                  </goals>
                  <configuration></configuration>
                </execution>
              </executions>
            </plugin>
            <!-- Download dependency jars to the deployment/libs folder -->
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-dependency-plugin</artifactId>
              <version>3.5.0</version>
              <executions>
                <execution>
                  <phase>process-resources</phase>
                  <goals>
                    <goal>copy-dependencies</goal>
                  </goals>
                  <configuration>
                    <outputDirectory>\${basedir}/deployment/libs</outputDirectory>
                    <excludeTransitive>true</excludeTransitive>
                    <!-- exclude dependencies which already available in MI -->
                    <excludeGroupIds>org.apache.synapse,org.apache.axis2</excludeGroupIds>
                    <excludeTypes>zip</excludeTypes>
                  </configuration>
                </execution>
              </executions>
            </plugin>
            <!-- Run config mapper to transform configuration files -->
            <plugin>
              <groupId>org.wso2.maven</groupId>
              <artifactId>mi-container-config-mapper</artifactId>
              <version>5.4.22</version>
              <extensions>true</extensions>
              <executions>
                <execution>
                  <id>config-mapper-parser</id>
                  <phase>generate-resources</phase>
                  <goals>
                    <goal>config-mapper-parser</goal>
                  </goals>
                  <configuration>
                    <miVersion>\${project.runtime.version}</miVersion>
                    <executeCipherTool>\${ciphertool.enable}</executeCipherTool>
                    <keystoreName>\${keystore.name}</keystoreName>
                    <keystoreAlias>\${keystore.alias}</keystoreAlias>
                    <keystoreType>\${keystore.type}</keystoreType>
                    <keystorePassword>\${keystore.password}</keystorePassword>
                    <projectLocation>\${project.basedir}</projectLocation>
                  </configuration>
                </execution>
              </executions>
              <configuration/>
            </plugin>
            <plugin>
              <artifactId>maven-antrun-plugin</artifactId>
              <version>3.0.0</version>
              <extensions>true</extensions>
              <executions>
                <execution>
                  <id>antrun-edit</id>
                  <phase>process-resources</phase>
                  <goals>
                    <goal>run</goal>
                  </goals>
                  <configuration>
                    <target>
                      <copy todir="\${basedir}/target/tmp_docker/CompositeApps">
                        <fileset dir="\${basedir}/target">
                          <include name="*.car"/>
                        </fileset>
                      </copy>
                    </target>
                  </configuration>
                </execution>
              </executions>
              <configuration/>
            </plugin>
            <!-- Build docker image -->
            <plugin>
              <groupId>io.fabric8</groupId>
              <artifactId>docker-maven-plugin</artifactId>
              <version>0.45.0</version>
              <extensions>true</extensions>
              <executions>
                <execution>
                  <id>docker-build</id>
                  <phase>package</phase>
                  <goals>
                    <goal>build</goal>
                  </goals>
                  <configuration>
                    <images>
                      <image>
                        <name>\${dockerfile.name}</name>
                        <build>
                          <from>\${dockerfile.base.image}</from>
                          <dockerFile>\${basedir}/target/tmp_docker/Dockerfile</dockerFile>
                          <args>
                            <BASE_IMAGE>\${dockerfile.base.image}</BASE_IMAGE>
                          </args>
                          <useDefaultExcludes>false</useDefaultExcludes>
                        </build>
                      </image>
                    </images>
                    <authConfig>
                      <username>\${dockerfile.pull.username}</username>
                      <password>\${dockerfile.pull.password}</password>
                    </authConfig>
                    <verbose>true</verbose>
                  </configuration>
                </execution>
              </executions>
              <configuration/>
            </plugin>
          </plugins>
        </build>
        <properties>
          <server.type>\${test.server.type}</server.type>
          <server.host>\${test.server.host}</server.host>
          <server.port>\${test.server.port}</server.port>
          <server.path>\${test.server.path}</server.path>
          <server.version>\${test.server.version}</server.version>
          <server.download.link>\${test.server.download.link}</server.download.link>
        </properties>
      </profile>`;

    let properties = `${indent}<projectType>integration-project</projectType>
      <uuid>${projectUuid}</uuid>
      <!-- <archiveLocation>configure a custom target directory for CApp</archiveLocation> -->
      <keystore.type>JKS</keystore.type>
      <keystore.name>wso2carbon.jks</keystore.name>
      <keystore.password>wso2carbon</keystore.password>
      <keystore.alias>wso2carbon</keystore.alias>
      <fat.car.enable>false</fat.car.enable>
      <ciphertool.enable>true</ciphertool.enable>
      <maven.compiler.source>1.8</maven.compiler.source>
      <maven.compiler.target>1.8</maven.compiler.target>
      <project.scm.id>integration-project</project.scm.id>
      <project.runtime.version>${miVersion}</project.runtime.version>
      <dockerfile.base.image>wso2/wso2mi:\${project.runtime.version}</dockerfile.base.image>
      <dockerfile.name>${artifactID.toLowerCase() + ':' + version}</dockerfile.name>
      <car.plugin.version>${LATEST_CAR_PLUGIN_VERSION}</car.plugin.version>
      <test.server.type>local</test.server.type>
      <test.server.host>localhost</test.server.host>
      <test.server.port>9008</test.server.port>
      <test.server.path>/</test.server.path>
      <test.server.version>\${project.runtime.version}</test.server.version>
      <testServerDownloadLink></testServerDownloadLink>
      <maven.test.skip>false</maven.test.skip>
      <testServerDownloadLink></testServerDownloadLink>`;

    if (addDeploymentType) {
        properties = `  <versionedDeployment>false</versionedDeployment>\n` + properties;
    }

    return `<?xml version="1.0" encoding="UTF-8"?>
  <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    ${isModule ? `<parent>
      <groupId>${parentGroupID}</groupId>
      <artifactId>${parentArtifactID}</artifactId>
      <version>${parentVersion}</version>
    </parent>
    <artifactId>${artifactID}</artifactId>` :
        `<groupId>${groupID}</groupId>
    <artifactId>${artifactID}</artifactId>
    <version>${version}</version>`}
    <packaging>pom</packaging>
    <name>${projectName}</name>
    <description>${projectName}</description>
    <repositories>
      <repository>
          <id>wso2-nexus</id>
          <name>WSO2 internal Repository</name>
          <url>https://maven.wso2.org/nexus/content/groups/wso2-public/</url>
          <releases>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
            <checksumPolicy>ignore</checksumPolicy>
          </releases>
      </repository>
      <repository>
          <id>wso2.releases</id>
          <name>WSO2 internal Repository</name>
          <url>https://maven.wso2.org/nexus/content/repositories/releases/</url>
          <releases>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
            <checksumPolicy>ignore</checksumPolicy>
          </releases>
      </repository>
      <repository>
          <id>wso2.snapshots</id>
          <name>Apache Snapshot Repository</name>
          <url>https://maven.wso2.org/nexus/content/repositories/snapshots/</url>
          <snapshots>
            <enabled>true</enabled>
            <updatePolicy>daily</updatePolicy>
          </snapshots>
          <releases>
            <enabled>false</enabled>
          </releases>
      </repository>
    </repositories>
    <pluginRepositories>
      <pluginRepository>
        <id>wso2.snapshots</id>
        <name>Apache Snapshot Repository</name>
        <url>https://maven.wso2.org/nexus/content/repositories/snapshots/</url>
        <snapshots>
          <enabled>true</enabled>
          <updatePolicy>daily</updatePolicy>
        </snapshots>
        <releases>
          <enabled>false</enabled>
        </releases>
      </pluginRepository>
      <pluginRepository>
        <releases>
          <enabled>true</enabled>
          <updatePolicy>daily</updatePolicy>
          <checksumPolicy>ignore</checksumPolicy>
        </releases>
        <id>wso2-nexus</id>
        <url>https://maven.wso2.org/nexus/content/groups/wso2-public/</url>
      </pluginRepository>
    </pluginRepositories>
    <profiles>
      <profile>
        <id>default</id>
        <activation>
          <activeByDefault>true</activeByDefault>
        </activation>
        <build>
          <plugins>
            <!-- Download dependency jars to the deployment/libs folder -->
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-dependency-plugin</artifactId>
              <version>3.5.0</version>
              <executions>
                <execution>
                  <phase>process-resources</phase>
                  <goals>
                    <goal>copy-dependencies</goal>
                  </goals>
                  <configuration>
                    <outputDirectory>\${basedir}/deployment/libs</outputDirectory>
                    <excludeTransitive>true</excludeTransitive>
                    <!-- exclude dependencies which already available in MI -->
                    <excludeGroupIds>org.apache.synapse,org.apache.axis2</excludeGroupIds>
                    <excludeTypes>zip,car</excludeTypes>
                  </configuration>
                </execution>
              </executions>
            </plugin>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-compiler-plugin</artifactId>
              <configuration>
                <source>1.8</source>
                <target>1.8</target>
              </configuration>
            </plugin>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-jar-plugin</artifactId>
              <configuration>
                <skipIfEmpty>true</skipIfEmpty>
              </configuration>
              <executions>
                <execution>
                  <phase>compile</phase>
                  <id>default-jar</id>
                  <goals>
                    <goal>jar</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>
            <plugin>
              <groupId>org.wso2.maven</groupId>
              <artifactId>vscode-car-plugin</artifactId>
              <version>\${car.plugin.version}</version>
              <extensions>true</extensions>
              <executions>
                <execution>
                  <phase>compile</phase>
                  <goals>
                    <goal>car</goal>
                  </goals>
                  <configuration/>
                </execution>
              </executions>
            </plugin>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-install-plugin</artifactId>
              <version>2.5.2</version>
              <executions>
                <execution>
                  <id>install-car</id>
                  <phase>compile</phase>
                  <goals>
                    <goal>install-file</goal>
                  </goals>
                  <configuration>
                    <packaging>car</packaging>
                    <artifactId>\${project.artifactId}</artifactId>
                    <groupId>\${project.groupId}</groupId>
                    <version>\${project.version}</version>
                    <file>\${project.build.directory}/\${project.artifactId}_\${project.version}.car</file>
                    <!-- Use the following configuration when archiveLocation is configured -->
                    <!-- <file>\${archiveLocation}/\${project.artifactId}_\${project.version}.car</file> -->
                  </configuration>
                </execution>
              </executions>
            </plugin>
          </plugins>
        </build>
        <properties>
          <server.type>\${test.server.type}</server.type>
          <server.host>\${test.server.host}</server.host>
          <server.port>\${test.server.port}</server.port>
          <server.path>\${test.server.path}</server.path>
          <server.version>\${test.server.version}</server.version>
          <server.download.link>\${test.server.download.link}</server.download.link>
        </properties>
      </profile>
      <profile>
        <id>test</id>
        <build/>
        <properties>
          <server.type>\${testServerType}</server.type>
          <server.host>\${testServerHost}</server.host>
          <server.port>\${testServerPort}</server.port>
          <server.path>\${testServerPath}</server.path>
        </properties>
      </profile>
      ${!isModule ? dockerProfile : ''}
    </profiles>
    <build>
      <plugins>
        <plugin>
          <groupId>org.wso2.maven</groupId>
          <artifactId>synapse-unit-test-maven-plugin</artifactId>
          <version>5.4.15</version>
          <executions>
            <execution>
              <id>synapse-unit-test</id>
              <phase>test</phase>
              <goals>
                <goal>synapse-unit-test</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <server>
              <testServerType>\${server.type}</testServerType>
              <testServerHost>\${server.host}</testServerHost>
              <testServerPort>\${server.port}</testServerPort>
              <testServerPath>\${server.path}</testServerPath>
              <testServerVersion>\${server.version}</testServerVersion>
              <testServerDownloadLink>\${server.download.link}</testServerDownloadLink>
            </server>
            <testCasesFilePath>\${project.basedir}/src/test/wso2mi/\${testFile}</testCasesFilePath>
            <mavenTestSkip>\${maven.test.skip}</mavenTestSkip>
          </configuration>
        </plugin>
      </plugins>
    </build>
    <properties>
    ${properties}
    </properties>
    ${initialDependencies}
  </project>`;
};

export const dockerfileContent = () =>
    `ARG BASE_IMAGE\nFROM \${BASE_IMAGE}\nCOPY CompositeApps/*.car \${WSO2_SERVER_HOME}/repository/deployment/server/carbonapps/\nCOPY resources/wso2carbon.jks \${WSO2_SERVER_HOME}/repository/resources/security/wso2carbon.jks\nCOPY resources/client-truststore.jks \${WSO2_SERVER_HOME}/repository/resources/security/client-truststore.jks\n# COPY libs/*.jar \${WSO2_SERVER_HOME}/lib/`;
