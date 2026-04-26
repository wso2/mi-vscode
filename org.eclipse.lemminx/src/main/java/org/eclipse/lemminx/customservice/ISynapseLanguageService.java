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

package org.eclipse.lemminx.customservice;

import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.CodeDiagnosticRequest;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.IsEqualSwaggersParam;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateAPIResponse;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateSwaggerParam;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateSwaggerResponse;

import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectionUIParam;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connections;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorParam;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorDetails;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateAPIParam;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoDto;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.generate.ConnectorGenerateRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.generate.ConnectorGeneratorResponse;
import org.eclipse.lemminx.customservice.synapse.dataService.CheckDBDriverRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.CheckDBDriverResponseParams;
import org.eclipse.lemminx.customservice.synapse.dataService.MappingsGenRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.ModifyDriverRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.QueryGenRequestParams;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTestParams;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTestResponse;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointInfoResponse;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointsRequest;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.ValidationResponse;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.OverviewModel;
import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryMapResponse;
import org.eclipse.lemminx.customservice.synapse.driver.DriverDownloadRequest;
import org.eclipse.lemminx.customservice.synapse.driver.DriverMavenCoordinatesResponse;
import org.eclipse.lemminx.customservice.synapse.driver.DriverMavenCoordinatesRequest;
import org.eclipse.lemminx.customservice.synapse.dynamic.db.DynamicField;
import org.eclipse.lemminx.customservice.synapse.dynamic.db.GetDynamicFieldsRequest;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionValidationResponse;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.HelperPanelData;
import org.eclipse.lemminx.customservice.synapse.idp.PdfToImagesRequest;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorResponse;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorParam;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundEndpointInfo;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundInfoRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MediatorRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.SynapseConfigRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.SynapseConfigResponse;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.UISchemaRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MCPToolRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MCPToolResponse;
import org.eclipse.lemminx.customservice.synapse.parser.ConfigDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyStatusResponse;
import org.eclipse.lemminx.customservice.synapse.parser.DeployPluginDetails;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateConfigRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdatePropertyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateResponse;
import org.eclipse.lemminx.customservice.synapse.parser.config.ConfigurableEntry;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceUsagesRequest;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceParam;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.LoadDependentResourcesResponse;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenFromContentRequest;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenRequest;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeResponse;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.ArtifactTypeResponse;
import org.eclipse.lemminx.customservice.synapse.utils.ExtendedLocation;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@JsonSegment("synapse")
public interface ISynapseLanguageService {

    @JsonRequest
    CompletableFuture<SyntaxTreeResponse> syntaxTree(TextDocumentIdentifier param);

    @JsonRequest
    CompletableFuture<DBConnectionTestResponse> loadDriverAndTestConnection(DBConnectionTestParams request);

    @JsonRequest
    CompletableFuture<PublishDiagnosticsParams> diagnostic(TextDocumentIdentifier param);

    @JsonRequest
    CompletableFuture<PublishDiagnosticsParams> codeDiagnostic(CodeDiagnosticRequest param);

    @JsonRequest
    CompletableFuture<ExpressionValidationResponse> expressionValidation(ExpressionParam param);

    @JsonRequest
    CompletableFuture<DirectoryMapResponse> directoryTree(WorkspaceFolder param);

    @JsonRequest
    CompletableFuture<ExtendedLocation> definition(DefinitionParams params);

    @JsonRequest
    CompletableFuture<ResourceResponse> availableResources(ResourceParam param);

    @JsonRequest
    CompletableFuture<Either3<ConnectorResponse, Connector, Boolean>> availableConnectors(ConnectorParam param);

    @JsonRequest
    CompletableFuture<List<String>> getRegistryFiles(TextDocumentIdentifier param);

    @JsonRequest
    CompletableFuture<List<String>> getResourceFiles();

    @JsonRequest
    CompletableFuture<List<ConfigurableEntry>> getConfigurableEntries();

    @JsonRequest
    CompletableFuture<List<String>> getArtifactFiles(TextDocumentIdentifier param);

    @JsonRequest
    CompletableFuture<BreakpointInfoResponse> getBreakpointInfo(BreakpointsRequest breakPointRequest);

    @JsonRequest
    CompletableFuture<ValidationResponse> validateBreakpoints(BreakpointsRequest breakPointRequest);

    @JsonRequest
    CompletableFuture<StepOverInfo> stepOverBreakpoint(BreakpointsRequest breakPointRequest);

    @JsonRequest
    CompletableFuture<Either<Connections, Map<String, Connections>>> connectorConnections(ConnectorParam param);

    @JsonRequest
    public CompletableFuture<List<String>> getResourceUsages(ResourceUsagesRequest resourceUsagesRequest);

    @JsonRequest
    CompletableFuture<SchemaGenResponse> generateSchema(SchemaGenRequest schemaGenRequest);

    @JsonRequest
    CompletableFuture<SchemaGenResponse> generateSchemaFromContent(SchemaGenFromContentRequest schemaGenRequest);

    @JsonRequest
    CompletableFuture<GenerateAPIResponse> generateAPI(GenerateAPIParam param);

    @JsonRequest
    CompletableFuture<GenerateSwaggerResponse> swaggerFromAPI(GenerateSwaggerParam param);

    @JsonRequest
    CompletableFuture<Boolean> isEqualSwaggers(IsEqualSwaggersParam param);

    @JsonRequest
    CompletableFuture<DBConnectionTestResponse> testDBConnection(DBConnectionTestParams dbConnectionTestParams);

    @JsonRequest
    CompletableFuture<Boolean> saveInboundConnectorSchema(InboundConnectorParam param);

    @JsonRequest
    CompletableFuture<InboundConnectorResponse> getInboundConnectorSchema(InboundConnectorParam param);

    @JsonRequest
    CompletableFuture<JsonObject> getLocalInboundConnectors();

    @JsonRequest
    CompletableFuture<JsonObject> getConnectionUISchema(ConnectionUIParam param);

    @JsonRequest
    CompletableFuture<DependencyTree> dependencyTree(TextDocumentIdentifier param);

    @JsonRequest
    CompletableFuture<OverviewModel> getOverviewModel();

    @JsonRequest
    CompletableFuture<CheckDBDriverResponseParams> checkDBDriver(CheckDBDriverRequestParams requestParams);

    @JsonRequest
    CompletableFuture<Boolean> addDBDriver(ModifyDriverRequestParams requestParams);

    @JsonRequest
    CompletableFuture<Boolean> removeDBDriver(ModifyDriverRequestParams requestParams);

    @JsonRequest
    CompletableFuture<Boolean> modifyDBDriver(ModifyDriverRequestParams requestParams);

    @JsonRequest
    CompletableFuture<String> generateQueries(QueryGenRequestParams requestParams);

    @JsonRequest
    CompletableFuture<Map<String, List<Boolean>>> fetchTables(QueryGenRequestParams requestParams);

    @JsonRequest
    CompletableFuture<DirectoryMapResponse> getProjectExplorerModel(WorkspaceFolder param);

    @JsonRequest
    CompletableFuture<List<String>> getProjectIntegrationType(WorkspaceFolder param);

    @JsonRequest
    CompletableFuture<JsonObject> getMediators(MediatorRequest mediatorRequest);

    @JsonRequest
    CompletableFuture<JsonObject> getMediatorUISchema(UISchemaRequest uiSchemaRequest);

    @JsonRequest
    CompletableFuture<SynapseConfigResponse> generateSynapseConfig(SynapseConfigRequest synapseConfigRequest);

    @JsonRequest
    CompletableFuture<JsonObject> getMediatorUISchemaWithValues(MediatorRequest mediatorRequest);

    @JsonRequest
    CompletableFuture<MediatorTryoutInfo> tryOutMediator(MediatorTryoutRequest request);

    @JsonRequest
    CompletableFuture<Boolean> shutDownTryoutServer();

    @JsonRequest
    CompletableFuture<MediatorTryoutInfo> mediatorInputOutputSchema(MediatorTryoutRequest request);

    @JsonRequest
    CompletableFuture<OverviewPageDetailsResponse> getOverviewPageDetails();

    @JsonRequest
    CompletableFuture<UpdateResponse> updateProperty(UpdatePropertyRequest request);

    @JsonRequest
    CompletableFuture<UpdateResponse> updateDependency(UpdateDependencyRequest request);

    @JsonRequest
    CompletableFuture<UpdateResponse> updateConfigFile(UpdateConfigRequest request);

    @JsonRequest
    CompletableFuture<String> updateConnectorDependencies();

    @JsonRequest
    CompletableFuture<String> refetchIntegrationProjectDependencies();

    @JsonRequest
    CompletableFuture<LoadDependentResourcesResponse> loadDependentResources();

    @JsonRequest
    CompletableFuture<TestConnectionResponse> testConnectorConnection(TestConnectionRequest request);

    @JsonRequest
    CompletableFuture<ICompletionResponse> expressionCompletion(ExpressionParam param);

    @JsonRequest
    CompletableFuture<SignatureHelp> signatureHelp(ExpressionParam params);

    @JsonRequest
    CompletableFuture<HelperPanelData> expressionHelperData(ExpressionParam param);

    @JsonRequest
    CompletableFuture<ConnectorGeneratorResponse> generateConnector(ConnectorGenerateRequest schemaGenRequest);

    @JsonRequest
    CompletableFuture<ArtifactTypeResponse> getArtifactType(TextDocumentIdentifier artifactIdentifier);

    @JsonRequest
    CompletableFuture<Map<String, List<DynamicField>>> getDynamicFields(GetDynamicFieldsRequest request);

    @JsonRequest
    CompletableFuture<List<String>> getStoredProcedures(QueryGenRequestParams requestParams);

    @JsonRequest
    CompletableFuture<String> downloadDriverForConnector(DriverDownloadRequest request);

    @JsonRequest
    CompletableFuture<DriverMavenCoordinatesResponse> getDriverMavenCoordinates(
            DriverMavenCoordinatesRequest request);

    @JsonRequest
    CompletableFuture<DeployPluginDetails> updateMavenDeployPlugin(DeployPluginDetails request);

    @JsonRequest
    CompletableFuture<TextEdit> removeMavenDeployPlugin();

    @JsonRequest
    CompletableFuture<DeployPluginDetails> getMavenDeployPluginDetails();

    @JsonRequest
    CompletableFuture<List<ConfigDetails>> getConfigurableList();

    @JsonRequest
    CompletableFuture<String> getLocalInboundEndpointsListForCopilot();

    @JsonRequest
    CompletableFuture<List<String>> pdfToImagesBase64(PdfToImagesRequest request);

    @JsonRequest
    CompletableFuture<DependencyStatusResponse> getDependencyStatusList();

    @JsonRequest
    CompletableFuture<List<List<Object>>> getInputOutputMappings(MappingsGenRequestParams param);
  
    @JsonRequest
    CompletableFuture<MCPToolResponse> getMCPTools(MCPToolRequest param);

    @JsonRequest
    CompletableFuture<Either<ConnectorInfoResponse, String>> resolveConnector(UpdateDependencyRequest request);

    @JsonRequest
    CompletableFuture<ConnectorDetails> isDuplicateConnector(ConnectorDetails request);

    @JsonRequest
    CompletableFuture<Either<ConnectorInfoDto, String>> getConnectorInfo(ConnectorInfoRequest request);

    @JsonRequest
    CompletableFuture<Either<InboundEndpointInfo, String>> getInboundInfo(InboundInfoRequest request);
}
