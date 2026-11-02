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

import { createFolderStructure, copyDockerResources, copyMavenWrapper } from '@wso2/mi-migration-core';
export { createFolderStructure, copyDockerResources, copyMavenWrapper };
import * as fs from 'fs';
import * as path from 'path';
import { ExtensionContext, Uri, Webview } from "vscode";
import { getInboundEndpointdXml, GetInboundTemplatesArgs } from './template-engine/mustach-templates/inboundEndpoints';
import { getRegistryResource } from './template-engine/mustach-templates/registryResources';
import { getMessageProcessorXml, MessageProcessorTemplateArgs } from './template-engine/mustach-templates/MessageProcessor';
import { getProxyServiceXml, ProxyServiceTemplateArgs } from './template-engine/mustach-templates/ProxyService';
import { GetTaskTemplatesArgs, getTaskXml } from './template-engine/mustach-templates/tasks';
import { getMessageStoreXml, GetMessageStoreTemplatesArgs } from './template-engine/mustach-templates/messageStore';
import { getEditTemplateXml, getTemplateXml, TemplateArgs } from './template-engine/mustach-templates/Template';
import { getHttpEndpointXml, HttpEndpointArgs } from './template-engine/mustach-templates/HttpEndpoint';
import { getAddressEndpointXml, AddressEndpointArgs } from './template-engine/mustach-templates/AddressEndpoint';
import { getWsdlEndpointXml, WsdlEndpointArgs } from './template-engine/mustach-templates/WsdlEndpoint';
import { getDefaultEndpointXml, DefaultEndpointArgs } from './template-engine/mustach-templates/DefaultEndpoint';
import { GetLoadBalanceEPTemplatesArgs, getLoadBalanceEPXml } from './template-engine/mustach-templates/loadBalanceEndpoint';
import { GetFailoverEPTemplatesArgs, getFailoverEPXml } from './template-engine/mustach-templates/failoverEndpoint';
import { GetRecipientEPTemplatesArgs, getRecipientEPXml } from './template-engine/mustach-templates/recipientEndpoint';
import { GetTemplateEPTemplatesArgs, getTemplateEPXml } from './template-engine/mustach-templates/templateEndpoint';
import { APIResourceArgs, getAPIResourceXml } from './template-engine/mustach-templates/API';
import { getDataServiceXml, getDataSourceXml, DataServiceArgs, Datasource } from './template-engine/mustach-templates/DataService';
const isDevMode = process.env.WEB_VIEW_WATCH_MODE === "true";

export function getComposerJSFiles(context: ExtensionContext, componentName: string, webView: Webview): string[] {
	console.debug('Getting JS files for component:', componentName);
	const filePath = path.join(context.extensionPath, 'resources', 'jslibs', componentName + '.js');
	console.debug('Resolved file path:', filePath);
	
	// In dev mode, ensure URL path uses forward slashes for Windows compatibility
	const devPath = isDevMode 
		? new URL(componentName + '.js', process.env.WEB_VIEW_DEV_HOST).toString()
		: webView.asWebviewUri(Uri.file(filePath)).toString();

	return [
		devPath,
		isDevMode ? 'http://localhost:8097' : '' // For React Dev Tools
	];
}


export function removeMavenWrapper(targetPath: string) {
	const mavenWrapperDir = path.join(targetPath, '.mvn');
	const mvnwCmd = path.join(targetPath, 'mvnw.cmd');
	const mvnw = path.join(targetPath, 'mvnw');

	if (fs.existsSync(mvnwCmd)) {
		fs.unlinkSync(mvnwCmd);
	}
	if (fs.existsSync(mvnw)) {
		fs.unlinkSync(mvnw);
	}
	if (fs.existsSync(mavenWrapperDir)) {
		fs.rmSync(mavenWrapperDir, { recursive: true, force: true });
	}
}

export function createGitignoreFile(targetPath: string): Promise<void> {
	return new Promise((resolve, reject) => {
		const gitignorePath = path.join(targetPath, '.gitignore');

		// Common .gitignore patterns
		const gitignoreContent = `
.wso2mi/
.env
##############################
## Java
##############################
.mtj.tmp/
*.class
*.jar
*.war
*.ear
*.nar
hs_err_pid*
replay_pid*

##############################
## Maven
##############################
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
pom.xml.bak
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar
mvnw
mvnw.cmd

##############################
## Visual Studio Code
##############################
.vscode/
.code-workspace

##############################
## OS X
##############################
.DS_Store

##############################
## Miscellaneous
##############################
*.log
		`;

		fs.writeFile(gitignorePath, gitignoreContent, (err) => {
			if (err) {
				reject(err);
				return;
			}
			console.log('.gitignore file created');
			resolve();
		});
	});
}

export function getInboundEndpointXmlWrapper(props: GetInboundTemplatesArgs) {
	return getInboundEndpointdXml(props);
}

export function getRegistryResourceContent(type: string, resourceName: string, roles: string | undefined) {
	return getRegistryResource(type, resourceName, roles);
}

export function getMessageProcessorXmlWrapper(props: MessageProcessorTemplateArgs) {
	return getMessageProcessorXml(props);
}

export function getProxyServiceXmlWrapper(props: ProxyServiceTemplateArgs) {
	return getProxyServiceXml(props);
}

export function getTaskXmlWrapper(data: GetTaskTemplatesArgs) {
	return getTaskXml(data);
}

export function getLoadBalanceXmlWrapper(data: GetLoadBalanceEPTemplatesArgs) {
	return getLoadBalanceEPXml(data);
}

export function getFailoverXmlWrapper(data: GetFailoverEPTemplatesArgs) {
	return getFailoverEPXml(data);
}

export function getRecipientXmlWrapper(data: GetRecipientEPTemplatesArgs) {
	return getRecipientEPXml(data);
}

export function getTemplateEndpointXmlWrapper(data: GetTemplateEPTemplatesArgs) {
	return getTemplateEPXml(data);
}

export function getMessageStoreXmlWrapper(props: GetMessageStoreTemplatesArgs) {
	return getMessageStoreXml(props);
}

export function getTemplateXmlWrapper(props: TemplateArgs) {
	return getTemplateXml(props);
}

export function getEditTemplateXmlWrapper(props: TemplateArgs) {
	return getEditTemplateXml(props);
}

export function getHttpEndpointXmlWrapper(props: HttpEndpointArgs) {
	return getHttpEndpointXml(props);
}

export function getAddressEndpointXmlWrapper(props: AddressEndpointArgs) {
	return getAddressEndpointXml(props);
}

export function getWsdlEndpointXmlWrapper(props: WsdlEndpointArgs) {
	return getWsdlEndpointXml(props);
}

export function getDefaultEndpointXmlWrapper(props: DefaultEndpointArgs) {
	return getDefaultEndpointXml(props);
}

export function getAPIResourceXmlWrapper(props: APIResourceArgs) {
	return getAPIResourceXml(props);
}

export function getDataServiceXmlWrapper(props: DataServiceArgs) {
	return getDataServiceXml(props);
}

export function getDssDataSourceXmlWrapper(props: Datasource) {
	return getDataSourceXml(props);
}

