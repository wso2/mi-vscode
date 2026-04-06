/**
 * Copyright (c) 2026 WSO2 LLC. (http://www.wso2.org).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.SynapseLanguageService;
import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.XMLTextDocumentService;
import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.SchemaGenerate;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorAction;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.OperationParameter;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Tests for multi-root workspace validation using the XML engine and
 * file-association mechanism.
 *
 * <p>Test 1 ({@link #multiRootIsolation()}) verifies that a single
 * {@code XMLLanguageService} instance validates two isolated projects
 * (MI 4.3.0 and MI 4.4.0) using per-project XSD file associations.</p>
 *
 * <p>Test 2 ({@link #dynamicConnectorSchemaUpdate()}) verifies that
 * dynamically generated connector schemas are picked up by the validation
 * engine without a server restart.</p>
 */
public class CleanMultiRootValidationTest {

	private Path tempDirA;
	private Path tempDirB;

	@BeforeEach
	public void setUp() throws Exception {
		tempDirA = Files.createTempDirectory("project-a");
		tempDirB = Files.createTempDirectory("project-b");
		tempDirA.toFile().deleteOnExit();
		tempDirB.toFile().deleteOnExit();

		// Project A — MI 4.3.0
		String pomA = "<project><properties>"
				+ "<project.runtime.version>4.3.0</project.runtime.version>"
				+ "</properties></project>";
		Files.write(tempDirA.resolve("pom.xml"), pomA.getBytes(StandardCharsets.UTF_8));
		Files.createDirectories(tempDirA.resolve("src"));

		// Project B — MI 4.4.0
		String pomB = "<project><properties>"
				+ "<project.runtime.version>4.4.0</project.runtime.version>"
				+ "</properties></project>";
		Files.write(tempDirB.resolve("pom.xml"), pomB.getBytes(StandardCharsets.UTF_8));
		Files.createDirectories(tempDirB.resolve("src"));
	}

	// ---------------------------------------------------------------------------
	// Test 1 — One XMLLanguageService validates multiple projects
	// ---------------------------------------------------------------------------

	@Test
	public void multiRootIsolation() throws Exception {
		MockXMLLanguageServer server = createMultiRootServer();

		// The <variable> mediator was introduced in MI 4.4.0 — invalid in MI 4.3.0
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"testSeq\">\n"
				+ "    <variable name=\"myVar\" type=\"STRING\" value=\"test\"/>\n"
				+ "</sequence>";

		// Open the same XML content under both project trees
		String uriA = openDocument(server, tempDirA, "src/sequence.xml", xml, 1);
		String uriB = openDocument(server, tempDirB, "src/sequence.xml", xml, 1);

		Thread.sleep(1500);

		// Retrieve published diagnostics
		PublishDiagnosticsParams diagA = findDiagnosticsForUri(server.getPublishDiagnostics(), uriA);
		PublishDiagnosticsParams diagB = findDiagnosticsForUri(server.getPublishDiagnostics(), uriB);

		assertNotNull(diagA, "Diagnostics for Project A missing");
		assertNotNull(diagB, "Diagnostics for Project B missing");

		// Project A (4.3.0) — 'variable' is unknown → at least 1 error
		assertEquals(1, diagA.getDiagnostics().size(),
				"Project-A (4.3.0) should report 1 error for unknown 'variable' mediator");

		// Project B (4.4.0) — 'variable' is valid → 0 errors
		assertEquals(0, diagB.getDiagnostics().size(),
				"Project-B (4.4.0) should report 0 errors for valid 'variable' mediator");
	}

	// ---------------------------------------------------------------------------
	// Test 2 — Dynamically changed content (connector schema) is also validated
	// ---------------------------------------------------------------------------

	@Test
	public void dynamicConnectorSchemaUpdate() throws Exception {
		MockXMLLanguageServer server = createMultiRootServer();

		// Open a placeholder document so the server has an active document for Project A
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"testSeq\">\n"
				+ "    <variable name=\"myVar\" type=\"STRING\" value=\"test\"/>\n"
				+ "</sequence>";
		String uriA = openDocument(server, tempDirA, "src/sequence.xml", xml, 1);

		Thread.sleep(1500);

		// Simulate downloading a Salesforce connector
		ConnectorHolder holder = ConnectorHolder.getInstance();
		holder.clearConnectors();
		holder.addConnector(createFakeSalesforceConnector());

		// Generate the connector schema inside both workspace schema directories
		@SuppressWarnings("unchecked")
		Map<String, Path> resolvedSchemas = getWorkspaceSchemas(server);

		Path schemaPathA = resolvedSchemas.get(tempDirA.toUri().toString());
		assertNotNull(schemaPathA, "Schema path for Project A should be resolved");

		SchemaGenerate.generate(holder,
				schemaPathA.resolve("mediators").resolve("connectors.xsd").toString());

		// Verify generated schema contains the expected element
		String schemaContent = Files.readString(
				schemaPathA.resolve("mediators").resolve("connectors.xsd"));
		assertTrue(schemaContent.contains("<xs:element name=\"salesforce.create\">"),
				"Schema must contain salesforce.create element");

		// Send a didChange with XML that uses the connector
		String connectorXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<sequence xmlns=\"http://ws.apache.org/ns/synapse\" name=\"salesforceSeq\">\n"
				+ "    <salesforce.create>\n"
				+ "        <sobjectType>Account</sobjectType>\n"
				+ "    </salesforce.create>\n"
				+ "</sequence>";

		server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(
				new VersionedTextDocumentIdentifier(uriA, 2),
				List.of(new TextDocumentContentChangeEvent(connectorXml))));

		Thread.sleep(1500);

		// After schema regeneration the connector element should be recognized
		PublishDiagnosticsParams newDiagA = findDiagnosticsForUri(
				server.getPublishDiagnostics(), uriA);

		assertNotNull(newDiagA, "Diagnostics for Project A missing after connector update");
		assertEquals(0, newDiagA.getDiagnostics().size(),
				"'salesforce.create' should be recognized after dynamic schema generation");
	}

	// ---------------------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------------------

	/**
	 * Creates a {@link MockXMLLanguageServer} initialized with two workspace
	 * folders ({@code tempDirA} and {@code tempDirB}) and a mocked
	 * {@link SynapseLanguageService} whose {@code init()} is a no-op.
	 */
	private MockXMLLanguageServer createMultiRootServer() throws Exception {
		MockXMLLanguageServer server = new MockXMLLanguageServer();

		// Stub SynapseLanguageService to bypass its global init logic
		XMLTextDocumentService tds =
				(XMLTextDocumentService) server.getTextDocumentService();
		SynapseLanguageService stubSynapseService = new SynapseLanguageService(tds, server) {
			@Override
			public void init(String projectUri, Object settings,
					SynapseLanguageClientAPI languageClient) {
				// no-op
			}
		};

		java.lang.reflect.Field synapseField =
				XMLLanguageServer.class.getDeclaredField("synapseLanguageService");
		synapseField.setAccessible(true);
		synapseField.set(server, stubSynapseService);

		// Build InitializeParams with both workspace folders
		InitializeParams params = new InitializeParams();

		WorkspaceFolder wfA = new WorkspaceFolder();
		wfA.setUri(tempDirA.toUri().toString());
		wfA.setName("project-a");

		WorkspaceFolder wfB = new WorkspaceFolder();
		wfB.setUri(tempDirB.toUri().toString());
		wfB.setName("project-b");

		params.setWorkspaceFolders(Arrays.asList(wfA, wfB));

		JsonObject initOptions = new JsonObject();
		JsonObject settingsObj = new JsonObject();
		JsonObject xmlObj = new JsonObject();
		JsonObject validationObj = new JsonObject();
		validationObj.addProperty("noGrammar", "ignore");
		xmlObj.add("validation", validationObj);
		settingsObj.add("xml", xmlObj);
		initOptions.add("settings", settingsObj);
		params.setInitializationOptions(initOptions);

		server.initialize(params).join();
		return server;
	}

	/**
	 * Opens a document under the given project directory and returns the
	 * document URI.
	 */
	private static String openDocument(MockXMLLanguageServer server,
			Path projectDir, String relativePath, String content, int version) {
		String uri = projectDir.resolve(relativePath).toUri().toString();
		TextDocumentItem doc = new TextDocumentItem(uri, "xml", version, content);
		server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(doc));
		return uri;
	}

	/**
	 * Finds the <em>last</em> published diagnostics for the given URI.
	 */
	private static PublishDiagnosticsParams findDiagnosticsForUri(
			List<PublishDiagnosticsParams> allDiagnostics, String uri) {
		PublishDiagnosticsParams result = null;
		for (PublishDiagnosticsParams p : allDiagnostics) {
			if (p.getUri().equals(uri)) {
				result = p;
			}
		}
		return result;
	}

	/**
	 * Creates a minimal fake Salesforce connector with a single
	 * {@code salesforce.create} action for testing purposes.
	 */
	private static Connector createFakeSalesforceConnector() {
		Connector connector = new Connector();
		connector.setName("salesforce");
		connector.setDisplayName("Salesforce Connector");

		ConnectorAction action = new ConnectorAction();
		action.setTag("salesforce.create");
		action.setHidden(false);

		OperationParameter param = new OperationParameter("sobjectType", "Type of SObject");
		action.setParameters(List.of(param));
		connector.setActions(List.of(action));

		return connector;
	}

	/**
	 * Reflectively accesses the {@code workspaceSchemas} field to obtain the
	 * per-project schema directories resolved during initialization.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Path> getWorkspaceSchemas(
			MockXMLLanguageServer server) throws Exception {
		java.lang.reflect.Field field =
				XMLLanguageServer.class.getDeclaredField("workspaceSchemas");
		field.setAccessible(true);
		return (Map<String, Path>) field.get(server);
	}
}
