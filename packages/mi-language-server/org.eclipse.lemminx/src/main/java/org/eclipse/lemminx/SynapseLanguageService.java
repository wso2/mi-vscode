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

package org.eclipse.lemminx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.ISynapseLanguageService;
import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.CodeDiagnosticRequest;
import org.eclipse.lemminx.customservice.synapse.ProjectContext;
import org.eclipse.lemminx.customservice.synapse.WorkspaceManager;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.IsEqualSwaggersParam;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateAPIResponse;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateSwaggerParam;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateSwaggerResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorReader;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectionUIParam;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connections;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorParam;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionFinder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.Connector;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorDetails;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoDto;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.ConnectorInfoRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.generate.ConnectorGenerateRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.generate.ConnectorGeneratorResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionResponse;
import org.eclipse.lemminx.customservice.synapse.dataService.DynamicClassLoader;
import org.eclipse.lemminx.customservice.synapse.dataService.QueryGenerator;
import org.eclipse.lemminx.customservice.synapse.dataService.CheckDBDriverRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.CheckDBDriverResponseParams;
import org.eclipse.lemminx.customservice.synapse.dataService.MappingsGenRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.ModifyDriverRequestParams;
import org.eclipse.lemminx.customservice.synapse.dataService.QueryGenRequestParams;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTestParams;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTestResponse;
import org.eclipse.lemminx.customservice.synapse.db.DBConnectionTester;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.StepOverInfo;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.OverviewModelGenerator;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.OverviewModel;
import org.eclipse.lemminx.customservice.synapse.driver.DriverDownloadRequest;
import org.eclipse.lemminx.customservice.synapse.driver.DriverMavenCoordinatesResponse;
import org.eclipse.lemminx.customservice.synapse.driver.DriverMavenCoordinatesRequest;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionSignatureProvider;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionValidator;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionCompletionsProvider;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionValidationResponse;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.HelperPanelData;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorResponse;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorParam;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundEndpointInfo;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundInfoRequest;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyScanner;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.TryOutManager;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.AIConnectorHandler;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MediatorRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.SynapseConfigRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.SynapseConfigResponse;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.UISchemaRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MCPToolRequest;
import org.eclipse.lemminx.customservice.synapse.mediatorService.pojo.MCPToolResponse;
import org.eclipse.lemminx.customservice.synapse.parser.ConfigDetails;
import org.eclipse.lemminx.customservice.synapse.parser.Constants;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyStatusResponse;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DeployPluginDetails;
import org.eclipse.lemminx.customservice.synapse.parser.DependencyDownloadManager;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPage;
import org.eclipse.lemminx.customservice.synapse.parser.OverviewPageDetailsResponse;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateConfigRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdatePropertyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.UpdateResponse;
import org.eclipse.lemminx.customservice.synapse.parser.config.ConfigParser;
import org.eclipse.lemminx.customservice.synapse.parser.config.ConfigurableEntry;
import org.eclipse.lemminx.customservice.synapse.parser.pom.PomParser;
import org.eclipse.lemminx.customservice.synapse.parser.ConnectorDownloadManager;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorConfigService;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ConnectorDependencyResponse;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.ResetConnectorDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.UpdateConnectorDependencyRequest;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.UpdateConnectorFlagsRequest;
import org.eclipse.lemminx.customservice.synapse.parser.connectorConfig.UpdateGlobalConnectorFlagsRequest;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ArtifactFileScanner;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.RegistryFileScanner;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointInfoResponse;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointsRequest;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.BreakpointValidity;
import org.eclipse.lemminx.customservice.synapse.debugger.DebuggerHelper;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.ValidationResponse;
import org.eclipse.lemminx.customservice.synapse.api.generator.pojo.GenerateAPIParam;
import org.eclipse.lemminx.customservice.synapse.api.generator.RestApiAdmin;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFileScanner;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceUsageFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceUsagesRequest;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceParam;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.LoadDependentResourcesResponse;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.pojo.ResourceResponse;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.definition.SynapseDefinitionProvider;
import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryMapResponse;
import org.eclipse.lemminx.customservice.synapse.directoryTree.DirectoryTreeBuilder;
import org.eclipse.lemminx.customservice.synapse.dynamic.db.DynamicField;
import org.eclipse.lemminx.customservice.synapse.dynamic.db.DynamicFieldsHandler;
import org.eclipse.lemminx.customservice.synapse.dynamic.db.GetDynamicFieldsRequest;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.FileType;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenFromContentRequest;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenRequest;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGenResponse;
import org.eclipse.lemminx.customservice.synapse.schemagen.util.SchemaGeneratorHelper;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeGenerator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.SyntaxTreeResponse;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.ArtifactTypeResponse;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.utils.ExtendedLocation;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.customservice.synapse.idp.PdfToImagesRequest;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.extensions.synapse.SynapseDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.completion.ICompletionResponse;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.uriresolver.URIResolverExtensionManager;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.wso2.mi.tool.connector.tools.generator.grpc.GRPCConnectorGenerator;
import org.wso2.mi.tool.connector.tools.generator.openapi.ConnectorGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RPC facade for all {@code synapse/*} requests. This class does not own per-project state itself —
 * every method {@link #resolve resolves} the {@link ProjectContext} for the request (from a document
 * URI in its parameters, when one is present) and delegates to that context's handlers, so documents
 * from different open MI projects are served by their own isolated connector/mediator/resource state.
 *
 * <p>Requests whose parameters carry no resolvable document/folder URI fall back to the
 * {@link #defaultContext} — the first project registered at {@code initialize} time (the sole project,
 * for today's single-root client). Adding an explicit {@code projectUri} to those few request types so
 * they too can target a specific project in a multi-root workspace is left to the client-side follow-up
 * work, per the multi-project execution plan.
 */
public class SynapseLanguageService implements ISynapseLanguageService {

    private static final Logger log = Logger.getLogger(SynapseLanguageService.class.getName());
    private static final CancelChecker NULL_CANCEL_CHECKER = new CancelChecker() {
        @Override
        public void checkCanceled() {
            // Do nothing.
        }
    };

    // Published once init() loads deps; read through so RPC re-loads are reflected automatically.
    private static volatile AbstractResourceFinderHolder loadedResourceFinderHolder;

    /**
     * Dependent integration-project resources loaded at LS init for the default project. Empty before
     * init runs (e.g. unit tests that exercise participants directly).
     */
    public static Map<String, ResourceResponse> getLoadedDependentResources() {
        AbstractResourceFinderHolder holder = loadedResourceFinderHolder;
        return holder != null ? holder.getDependentResourcesMap() : Collections.emptyMap();
    }

    /**
     * Publishes a pre-loaded finder so {@link #getLoadedDependentResources()} sees its map. Called
     * internally by {@link #init} and by tests that exercise cross-project reference validation
     * without going through a full LS initialisation.
     */
    public static void setLoadedResourceFinder(org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder finder) {
        loadedResourceFinderHolder = finder != null ? finder::getDependentResourcesMap : null;
    }

    @FunctionalInterface
    private interface AbstractResourceFinderHolder {
        Map<String, ResourceResponse> getDependentResourcesMap();
    }

    // Published once per process so static-context-only callers (e.g. IDiagnosticsParticipant, which
    // has no DI path to the live server instance) can resolve a document's ProjectContext.
    private static volatile WorkspaceManager workspaceManagerHolder;

    /**
     * Resolves the {@link ProjectContext} for a document URI, for callers with no DI path to the live
     * {@code XMLLanguageServer}/{@code WorkspaceManager} (e.g. {@code SynapseDiagnosticsParticipant}).
     * Returns {@code null} if no project is registered for the document (or none has initialized yet).
     */
    public static ProjectContext resolveProjectContext(String documentUri) {
        WorkspaceManager manager = workspaceManagerHolder;
        return manager != null && documentUri != null ? manager.getProjectForDocument(documentUri) : null;
    }

    private XMLTextDocumentService xmlTextDocumentService;
    private XMLLanguageServer xmlLanguageServer;
    private SynapseLanguageClientAPI languageClient;
    private String extensionPath;
    private String miServerPath;
    private TryOutManager tryOutManager;
    private DynamicFieldsHandler dynamicFieldsHandler;
    private final URIResolverExtensionManager uriResolverExtensionManager;

    /**
     * The project this facade falls back to for RPCs whose parameters carry no resolvable document
     * URI. Populated from {@link XMLLanguageServer#getDefaultProjectContext()} during {@link #init}.
     */
    private ProjectContext defaultContext;

    public SynapseLanguageService(XMLTextDocumentService xmlTextDocumentService, XMLLanguageServer xmlLanguageServer) {

        this.xmlTextDocumentService = xmlTextDocumentService;
        this.xmlLanguageServer = xmlLanguageServer;
        uriResolverExtensionManager = xmlLanguageServer.getXMLLanguageService().getResolverExtensionManager();
        this.dynamicFieldsHandler = new DynamicFieldsHandler();
        workspaceManagerHolder = xmlLanguageServer.getWorkspaceManager();
    }

    /**
     * Parses the {@code extensionPath}/{@code miServerPath} settings. Split out from {@link #init} so
     * {@code XMLLanguageServer} can call it before building this process's {@link ProjectContext}s
     * (which need {@link #getMiServerPath()}), then call {@link #init} afterwards to pick up the
     * resulting default context.
     */
    public void applySettings(Object settings) {
        if (settings instanceof JsonObject) {
            JsonObject json = (JsonObject) settings;
            if (json.has("extensionPath")) {
                extensionPath = json.get("extensionPath").getAsString();
            }
            if (json.has("miServerPath")) {
                miServerPath = json.get("miServerPath").getAsString();
            }
        }
    }

    public void init(String projectUri, Object settings, SynapseLanguageClientAPI languageClient) {

        this.languageClient = languageClient;
        applySettings(settings);
        if (projectUri == null) {
            log.log(Level.SEVERE, "Project path is null. Language server initialization failed.");
            return;
        }
        ProjectContext context = xmlLanguageServer.getDefaultProjectContext();
        if (context == null) {
            log.log(Level.SEVERE, "No ProjectContext could be resolved for: " + projectUri);
            return;
        }
        this.defaultContext = context;
        try {
            DynamicClassLoader.updateClassLoader(Path.of(projectUri, "deployment", "libs").toFile());
            this.tryOutManager = new TryOutManager(projectUri, miServerPath, context.getProjectServerVersion(),
                    context.getConnectorHolder(), languageClient);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while updating class loader for DB drivers.", e);
        }
        setLoadedResourceFinder(context.getResourceFinder());
    }

    // -------------------------------------------------------------------------
    // Dispatch — resolves the ProjectContext for a request, falling back to
    // defaultContext when the request carries no resolvable document URI.
    // -------------------------------------------------------------------------

    private ProjectContext resolveByUri(String documentUri) {
        if (StringUtils.isNotBlank(documentUri) && xmlLanguageServer != null) {
            ProjectContext context = xmlLanguageServer.getWorkspaceManager().getProjectForDocument(documentUri);
            if (context != null) {
                return context;
            }
        }
        return defaultContext;
    }

    private ProjectContext resolve(TextDocumentIdentifier document) {
        return resolveByUri(document != null ? document.getUri() : null);
    }

    private ProjectContext resolve(DefinitionParams params) {
        return resolve(params != null ? params.getTextDocument() : null);
    }

    @Override
    public CompletableFuture<SyntaxTreeResponse> syntaxTree(TextDocumentIdentifier param) {

        ProjectContext ctx = resolve(param);
        return xmlTextDocumentService.computeDOMAsync(param, (xmlDocument, cancelChecker) -> {
            SyntaxTreeGenerator generator = new SyntaxTreeGenerator();
            generator.setProjectPath(ctx != null ? ctx.getProjectUri() : null);
            return generator.getSyntaxTree(xmlDocument);
        });
    }

    @Override
    public CompletableFuture<DBConnectionTestResponse> testDBConnection(DBConnectionTestParams dbConnectionTestParams) {

        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        boolean connectionStatus = dbConnectionTester.testDBConnection(dbConnectionTestParams.dbType,
                    dbConnectionTestParams.username, dbConnectionTestParams.password,
                    dbConnectionTestParams.host, dbConnectionTestParams.port, dbConnectionTestParams.dbName,
                    dbConnectionTestParams.url, dbConnectionTestParams.className);
        DBConnectionTestResponse response = new DBConnectionTestResponse(connectionStatus);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<DBConnectionTestResponse> loadDriverAndTestConnection(DBConnectionTestParams request){
        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        boolean connectionStatus = dbConnectionTester.testDBConnection(request.dbType,
                request.username, request.password,
                request.host, request.port, request.dbName,
                request.url, request.className, request.driverPath);
        DBConnectionTestResponse response = new DBConnectionTestResponse(connectionStatus);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<PublishDiagnosticsParams> diagnostic(TextDocumentIdentifier param) {

        return xmlTextDocumentService.computeDOMAsync(param, (xmlDocument, cancelChecker) -> {
            cancelChecker.checkCanceled();
            return doDiagnostics(xmlDocument, cancelChecker);
        });
    }

    private PublishDiagnosticsParams doDiagnostics(DOMDocument xmlDocument, CancelChecker cancelChecker) {

        SharedSettings sharedSettings = xmlTextDocumentService.getSharedSettings();
        XMLValidationSettings validationSettingsForUri = sharedSettings != null
                ? sharedSettings.getValidationSettings().getValidationSettings(xmlDocument.getDocumentURI())
                : null;
        List<Diagnostic> diagnostics = xmlLanguageServer.getXMLLanguageService().doDiagnostics(xmlDocument,
                validationSettingsForUri,
                Collections.emptyMap(), cancelChecker);
        return new PublishDiagnosticsParams(xmlDocument.getDocumentURI(), diagnostics);
    }

    @Override
    public CompletableFuture<PublishDiagnosticsParams> codeDiagnostic(CodeDiagnosticRequest param) {

        return CompletableFuture.supplyAsync(() -> {
            // Use the real file path (when supplied) as the document URI. Several diagnostics are
            // gated on the document path — e.g. SynapseExpressionValidator only runs for files under
            // src/main/wso2mi/artifacts — so the literal "temp" fallback would silently drop them.
            // Treat a blank fileName as missing, otherwise an unusable URI would skip those checks.
            String uri = StringUtils.isBlank(param.getFileName()) ? "temp" : param.getFileName();
            // Opt-out (default off) for cross-file reference checks: the agent validates a file
            // before its referenced siblings are written, so those checks would fire spuriously.
            // Set/clear around doDiagnostics on this thread; the editor never sets it.
            try {
                SynapseDiagnosticsParticipant.setSkipCrossFileValidation(param.isSkipCrossFileValidation());
                DOMDocument xmlDocument = Utils.getDOMDocument(param.getCode(), uri, uriResolverExtensionManager);
                return doDiagnostics(xmlDocument, NULL_CANCEL_CHECKER);
            } finally {
                SynapseDiagnosticsParticipant.clearSkipCrossFileValidation();
            }
        });
    }

    @Override
    public CompletableFuture<ExpressionValidationResponse> expressionValidation(ExpressionParam param) {

        return CompletableFuture.supplyAsync(() -> {
            String expression = param.getExpression();
            if (expression.startsWith("${") && expression.endsWith("}")) {
                expression = expression.substring(2, expression.length() - 1);
            }
            List<ExpressionError> errors = ExpressionValidator.validate(expression);
            return new ExpressionValidationResponse(errors.isEmpty(), errors);
        });
    }

    @Override
    public CompletableFuture<DirectoryMapResponse> directoryTree(WorkspaceFolder param) {

        DirectoryMapResponse response = DirectoryTreeBuilder.buildDirectoryTree(param);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<ExtendedLocation> definition(
            DefinitionParams params) {

        log.log(Level.INFO, "Processing definition request for document: " + params.getTextDocument().getUri());
        ProjectContext ctx = resolve(params);
        return xmlTextDocumentService.computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
            Map<String, ResourceResponse> dependentResourcesMap = ctx != null
                    ? ctx.getResourceFinder().getDependentResourcesMap() : Collections.emptyMap();

            return SynapseDefinitionProvider.definition(xmlDocument, params.getPosition(),
                    ctx != null ? ctx.getProjectUri() : null, cancelChecker, dependentResourcesMap);
        });
    }

    @Override
    public CompletableFuture<ResourceResponse> availableResources(ResourceParam param) {

        ProjectContext ctx = StringUtils.isNotBlank(param.customProjectUri)
                ? resolveByUri(param.customProjectUri) : defaultContext;
        String effectivePath = StringUtils.isNotBlank(param.projectPath) ? param.projectPath
                : StringUtils.isNotBlank(param.customProjectUri) ? param.customProjectUri
                : ctx != null ? ctx.getProjectUri() : null;
        ResourceResponse response = ctx != null
                ? ctx.getResourceFinder().getAvailableResources(effectivePath, param.resourceType)
                : new ResourceResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<Either3<ConnectorResponse, Connector, Boolean>> availableConnectors(ConnectorParam param) {

        return CompletableFuture.supplyAsync(() -> {
            ProjectContext ctx = resolve(param.documentIdentifier);
            ConnectorHolder holder = ctx != null ? ctx.getConnectorHolder() : new ConnectorHolder();
            if (param.connectorName != null && !param.connectorName.isEmpty()) {
                Connector connector = holder.getConnector(param.connectorName);
                if (connector == null) {
                    return Either3.forThird(Boolean.FALSE);
                }
                return Either3.forSecond(connector);
            }
            return Either3.forFirst(new ConnectorResponse(holder.getConnectors()));
        });
    }

    @Override
    public CompletableFuture<Either<ConnectorInfoDto, String>> getConnectorInfo(ConnectorInfoRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (StringUtils.isAnyBlank(request.groupId, request.artifactId, request.version)) {
                return Either.forRight("groupId, artifactId, and version are required");
            }
            ProjectContext ctx = defaultContext;
            if (ctx == null) {
                return Either.forRight("Project is not initialized");
            }
            String projectUri = ctx.getProjectUri();

            File extractDir;
            File zipFile;
            try {
                ResolvedArtifact artifact = downloadAndExtractArtifact(
                        projectUri, request.groupId, request.artifactId, request.version);
                extractDir = artifact.extractDir;
                zipFile = artifact.zipFile;
            } catch (IOException e) {
                log.log(Level.WARNING, "Error resolving connector: " + request.artifactId, e);
                return Either.forRight("Error resolving " + request.artifactId + ": " + e.getMessage());
            } catch (IllegalStateException e) {
                return Either.forRight(e.getMessage());
            }

            ConnectorReader connectorReader = new ConnectorReader();
            Connector connector = connectorReader.readConnector(extractDir.getAbsolutePath(), projectUri);
            if (connector == null) {
                return Either.forRight("Failed to read connector metadata: " + request.artifactId);
            }

            // Copilot lookups are read-only with respect to the project's
            // ConnectorHolder — we return metadata but do NOT register the
            // connector as a project dependency. The zip path is still set on
            // the DTO so the caller can reach the cached file if it needs to.
            connector.setConnectorZipPath(zipFile.getAbsolutePath());
            return Either.forLeft(ConnectorInfoDto.from(connector));
        });
    }

    @Override
    public CompletableFuture<Either<InboundEndpointInfo, String>> getInboundInfo(InboundInfoRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            ProjectContext ctx = defaultContext;
            InboundConnectorHolder inboundConnectorHolder = ctx != null ? ctx.getInboundConnectorHolder() : null;
            // Bundled lookup first — no download needed.
            if (StringUtils.isNotBlank(request.id)) {
                InboundEndpointInfo bundled = inboundConnectorHolder != null
                        ? inboundConnectorHolder.getBundledInboundEndpoint(request.id) : null;
                if (bundled != null) {
                    return Either.forLeft(bundled);
                }
                if (StringUtils.isAnyBlank(request.groupId, request.artifactId, request.version)) {
                    return Either.forRight("Bundled inbound not found for id: " + request.id);
                }
            }

            if (StringUtils.isAnyBlank(request.groupId, request.artifactId, request.version)) {
                return Either.forRight(
                        "Provide either id (for bundled inbound) or {groupId, artifactId, version}");
            }
            if (ctx == null) {
                return Either.forRight("Project is not initialized");
            }

            File extractDir;
            try {
                ResolvedArtifact artifact = downloadAndExtractArtifact(
                        ctx.getProjectUri(), request.groupId, request.artifactId, request.version);
                extractDir = artifact.extractDir;
            } catch (IOException e) {
                log.log(Level.WARNING, "Error resolving inbound: " + request.artifactId, e);
                return Either.forRight("Error resolving " + request.artifactId + ": " + e.getMessage());
            } catch (IllegalStateException e) {
                return Either.forRight(e.getMessage());
            }

            File uiSchemaFile = extractDir.toPath()
                    .resolve(Constant.RESOURCES).resolve(Constant.UI_SCHEMA_JSON).toFile();
            if (!uiSchemaFile.exists()) {
                return Either.forRight("resources/uischema.json not found in " + request.artifactId);
            }
            try {
                String schemaString = Utils.readFile(uiSchemaFile);
                JsonObject schemaJson = Utils.getJsonObject(schemaString);
                if (schemaJson == null || !schemaJson.has(Constant.NAME)) {
                    return Either.forRight("Invalid inbound uischema in " + request.artifactId);
                }
                String inboundName = schemaJson.get(Constant.NAME).getAsString();
                ctx.getInboundConnectorHolder().saveInboundConnector(inboundName, schemaString);
                return Either.forLeft(
                        InboundConnectorHolder.buildInboundEndpointInfo(schemaJson, "downloaded"));
            } catch (IOException e) {
                log.log(Level.WARNING, "Error reading inbound uischema: " + request.artifactId, e);
                return Either.forRight("Error reading " + request.artifactId + ": " + e.getMessage());
            }
        });
    }

    /**
     * Resolves a Maven artifact to a local extracted directory. Downloads from
     * WSO2 Nexus (or copies from the local {@code .m2} repo) if the zip isn't
     * already cached, then extracts if needed. The cache lives under a
     * machine-wide {@code ~/.wso2-mi/copilot/cache/artifacts/<MI-version>/}
     * directory and is partitioned by a sanitised {@code groupId} segment so
     * two artifacts that share an artifactId+version across different groupIds
     * don't collide. This is intentionally separate from the per-project
     * {@code ~/.wso2-mi/connectors/<projectId>/} cache so Copilot lookups for
     * connectors that are NOT in the project's pom don't pollute the project's
     * connector list (scanned by {@code NewProjectConnectorLoader}) or get
     * evicted by {@code ConnectorDownloadManager.deleteRemovedConnectors}.
     *
     * @throws IllegalStateException if the download fails to produce a zip file.
     * @throws IOException on extract/download I/O errors.
     */
    private ResolvedArtifact downloadAndExtractArtifact(String projectUri, String groupId, String artifactId,
            String version) throws IOException {

        // Use the raw pom.xml runtime version (not the schema-mapped projectServerVersion)
        // so the cache folder reflects the user's actual MI runtime: a 4.5.0 project
        // caches under .../copilot/cache/artifacts/4.5.0/ instead of being collapsed to
        // 4.4.0 by MI_SUPPORTED_VERSION_MAP (which is only meant for XSD schema selection).
        String miVersion = Utils.getRawRuntimeVersion(projectUri, Constant.DEFAULT_MI_VERSION);
        // Partition the cache by a sanitised groupId so two artifacts that share an
        // artifactId+version across different groupIds don't overwrite each other.
        // The download/extract helpers always name files <artifactId>-<version>, so
        // disambiguating via a parent directory is what keeps the cache collision-free.
        String safeGroupId = groupId.replaceAll("[^a-zA-Z0-9._-]", "_");
        File directory = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI,
                Constant.COPILOT, Constant.CACHE, Constant.ARTIFACTS, miVersion).toFile();
        File downloadDir = Path.of(directory.getAbsolutePath(), Constant.DOWNLOADED, safeGroupId).toFile();
        File extractDir = Path.of(directory.getAbsolutePath(), Constant.EXTRACTED, safeGroupId).toFile();
        downloadDir.mkdirs();
        extractDir.mkdirs();

        File zipFile = new File(downloadDir,
                artifactId + "-" + version + Constant.ZIP_EXTENSION);
        if (!zipFile.exists()) {
            File localCopy = Utils.getDependencyFromLocalRepo(groupId, artifactId, version,
                    Constant.ZIP_EXTENSION_NO_DOT);
            if (localCopy != null) {
                Utils.copyFile(localCopy.getPath(), downloadDir.getPath());
            } else {
                try {
                    Utils.downloadConnector(groupId, artifactId, version, downloadDir,
                            Constant.ZIP_EXTENSION_NO_DOT, projectUri);
                } catch (FileNotFoundException notFound) {
                    // 404 from the Maven repo — coordinates don't resolve to an artifact.
                    throw new IllegalStateException("Artifact not found on WSO2 Nexus: "
                            + groupId + ":" + artifactId + ":" + version
                            + ". Verify the groupId, artifactId, and version are correct.");
                } catch (IOException ioe) {
                    // Non-404 HTTP error or local IO failure — surface a clean message
                    // instead of letting the raw IOException propagate to a generic catch.
                    throw new IllegalStateException("Failed to download artifact "
                            + groupId + ":" + artifactId + ":" + version + ": " + ioe.getMessage(), ioe);
                }
            }
        }
        if (!zipFile.exists()) {
            throw new IllegalStateException("Failed to download artifact: "
                    + groupId + ":" + artifactId + ":" + version);
        }

        File extractedFolder = new File(extractDir, artifactId + "-" + version);
        if (!extractedFolder.exists()) {
            Utils.extractZip(zipFile, extractedFolder);
        }
        return new ResolvedArtifact(zipFile, extractedFolder);
    }

    private static final class ResolvedArtifact {

        final File zipFile;
        final File extractDir;

        ResolvedArtifact(File zipFile, File extractDir) {

            this.zipFile = zipFile;
            this.extractDir = extractDir;
        }
    }

    /**
     * Reloads the default project's connectors. Used as a fallback by {@code XMLWorkspaceService} when
     * a watched connector {@code .zip} change can't be resolved to a specific registered project.
     */
    public void updateConnectors() {

        if (defaultContext != null) {
            defaultContext.updateConnectors();
        }
    }

    /**
     * Reloads the default project's inbound connectors. Same fallback role as {@link #updateConnectors()}.
     */
    public void updateInboundConnectors() {

        if (defaultContext != null) {
            defaultContext.updateInboundConnectors();
        }
    }

    @Override
    public CompletableFuture<List<String>> getRegistryFiles(TextDocumentIdentifier param) {

        ProjectContext ctx = resolve(param);
        List<String> registryFiles = ctx != null
                ? RegistryFileScanner.scanRegistryFiles(ctx.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> registryFiles);
    }

    @Override
    public CompletableFuture<List<String>> getResourceFiles() {

        List<String> resourceFiles = defaultContext != null
                ? ResourceFileScanner.scanResourceFiles(defaultContext.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> resourceFiles);
    }

    @Override
    public CompletableFuture<List<ConfigurableEntry>> getConfigurableEntries() {

        if (defaultContext == null) {
            return CompletableFuture.supplyAsync(ArrayList::new);
        }
        try {
            List<ConfigurableEntry> configurableEntries = ConfigParser.scanConfigurableEntries(defaultContext.getProjectUri());
            return CompletableFuture.supplyAsync(() -> configurableEntries);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error while scanning configurable entries.", e);
            return CompletableFuture.supplyAsync(() -> new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<List<String>> getResourceUsages(ResourceUsagesRequest resourceUsagesRequest) {

        ProjectContext ctx = resolveByUri(resourceUsagesRequest.getResourceFilePath());
        List<String> resourceUsagesProjectIdentifiers = ctx != null
                ? ResourceUsageFinder.findResourceUsagesProjectIdentifiers(ctx.getProjectUri(),
                        resourceUsagesRequest.getResourceFilePath(), ctx.getConnectorHolder(), ctx.isLegacyProject())
                : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> resourceUsagesProjectIdentifiers);
    }

    @Override
    public CompletableFuture<List<String>> getArtifactFiles(TextDocumentIdentifier param) {

        ProjectContext ctx = resolve(param);
        List<String> artifactFiles = ctx != null
                ? ArtifactFileScanner.scanArtifactFiles(ctx.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> artifactFiles);
    }

    @Override
    public CompletableFuture<BreakpointInfoResponse> getBreakpointInfo(BreakpointsRequest breakPointRequest) {

        DebuggerHelper debuggerHelper = new DebuggerHelper(breakPointRequest.filePath);
        List<JsonElement> debugInfoJsonList = debuggerHelper.generateDebugInfoJson(breakPointRequest.breakpoints);
        BreakpointInfoResponse breakpointInfoResponse = new BreakpointInfoResponse(debugInfoJsonList);
        return CompletableFuture.supplyAsync(() -> breakpointInfoResponse);
    }

    @Override
    public CompletableFuture<ValidationResponse> validateBreakpoints(BreakpointsRequest breakPointRequest) {

        DebuggerHelper debuggerHelper = new DebuggerHelper(breakPointRequest.filePath);
        List<BreakpointValidity> validityList = debuggerHelper.validateBreakpoints(breakPointRequest.breakpoints);
        ValidationResponse validationResponse = new ValidationResponse(validityList);
        return CompletableFuture.supplyAsync(() -> validationResponse);
    }

    @Override
    public CompletableFuture<StepOverInfo> stepOverBreakpoint(BreakpointsRequest breakPointRequest) {

        DebuggerHelper debuggerHelper = new DebuggerHelper(breakPointRequest.filePath);
        StepOverInfo stepOverInfo = debuggerHelper.getStepOverBreakpoints(breakPointRequest.breakpoint);
        return CompletableFuture.supplyAsync(() -> stepOverInfo);
    }

    @Override
    public CompletableFuture<Either<Connections, Map<String, Connections>>> connectorConnections(ConnectorParam param) {

        ProjectContext ctx = resolve(param.documentIdentifier);
        Either<Connections, Map<String, Connections>> connections = ctx != null
                ? ConnectionFinder.findConnections(ctx.getProjectUri(), param.connectorName, ctx.getConnectorHolder(),
                        ctx.isLegacyProject())
                : Either.forLeft(new Connections());
        return CompletableFuture.supplyAsync(() -> connections);
    }

    @Override
    public CompletableFuture<SchemaGenResponse> generateSchema(SchemaGenRequest schemaGenRequest) {

        SchemaGeneratorHelper schemaGenerate = new SchemaGeneratorHelper();
        FileType fileType = FileType.valueOf(schemaGenRequest.type);
        String schema = schemaGenerate.getSchemaContent(fileType, schemaGenRequest.filePath, schemaGenRequest.delimiter);
        SchemaGenResponse schemaGenResponse = new SchemaGenResponse(schema);
        return CompletableFuture.supplyAsync(() -> schemaGenResponse);
    }

    @Override
    public CompletableFuture<SchemaGenResponse> generateSchemaFromContent(SchemaGenFromContentRequest schemaGenRequest) {

        SchemaGeneratorHelper schemaGenerate = new SchemaGeneratorHelper();
        FileType fileType = FileType.valueOf(schemaGenRequest.type);
        String schema = schemaGenerate.getSchemaFromContent(fileType,
                schemaGenRequest.fileContent, schemaGenRequest.delimiter);
        SchemaGenResponse schemaGenResponse = new SchemaGenResponse(schema);
        return CompletableFuture.supplyAsync(() -> schemaGenResponse);
    }

    @Override
    public CompletableFuture<GenerateAPIResponse> generateAPI(GenerateAPIParam param) {

        RestApiAdmin generator = new RestApiAdmin();
        GenerateAPIResponse apiXml = generator.createAPI(param);
        return CompletableFuture.supplyAsync(() -> apiXml);
    }

    @Override
    public CompletableFuture<GenerateSwaggerResponse> swaggerFromAPI(GenerateSwaggerParam param) {

        RestApiAdmin generator = new RestApiAdmin();
        return CompletableFuture.supplyAsync(() -> generator.generateSwaggerFromAPI(param));
    }

    @Override
    public CompletableFuture<Boolean> isEqualSwaggers(IsEqualSwaggersParam param) {

        RestApiAdmin generator = new RestApiAdmin();
        return CompletableFuture.supplyAsync(() -> generator.isEqualSwaggers(param));
    }

    @Override
    public CompletableFuture<Boolean> saveInboundConnectorSchema(InboundConnectorParam param) {

        ProjectContext ctx = resolveByUri(param.documentPath);
        return CompletableFuture.supplyAsync(() -> ctx != null
                && ctx.getInboundConnectorHolder().saveInboundConnector(param.connectorName, param.uiSchema));
    }

    @Override
    public CompletableFuture<InboundConnectorResponse> getInboundConnectorSchema(InboundConnectorParam param) {

        ProjectContext ctx = resolveByUri(param.documentPath);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return null;
            }
            if (param.connectorId != null) {
                return ctx.getInboundConnectorHolder().getInboundConnectorSchemaFromId(param.connectorId);
            } else {
                return ctx.getInboundConnectorHolder().getInboundConnectorSchema(new File(param.documentPath));
            }
        });
    }

    @Override
    public CompletableFuture<JsonObject> getLocalInboundConnectors() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? defaultContext.getInboundConnectorHolder().getLocalInboundConnectorList() : new JsonObject());
    }

    @Override
    public CompletableFuture<JsonObject> getConnectionUISchema(ConnectionUIParam param) {

        ProjectContext ctx = resolveByUri(param.getDocumentUri());
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getConnectionHandler().getConnectionUISchema(param) : new JsonObject());
    }

    @Override
    public CompletableFuture<DependencyTree> dependencyTree(TextDocumentIdentifier param) {

        ProjectContext ctx = resolve(param);
        DependencyTree dependencyTree;
        if (ctx != null) {
            DependencyScanner dependencyScanner = new DependencyScanner(ctx.getProjectUri());
            dependencyTree = dependencyScanner.analyzeArtifact(param.getUri());
        } else {
            dependencyTree = null;
        }
        return CompletableFuture.supplyAsync(() -> dependencyTree);
    }

    @Override
    public CompletableFuture<OverviewModel> getOverviewModel() {
        OverviewModel overviewModel = defaultContext != null
                ? OverviewModelGenerator.getOverviewModel(defaultContext.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> overviewModel);
    }

    @Override
    public CompletableFuture<CheckDBDriverResponseParams> checkDBDriver(CheckDBDriverRequestParams requestParams) {
        CheckDBDriverResponseParams response = QueryGenerator.isDriverAvailableInClassPath(requestParams.className,
                defaultContext != null ? defaultContext.getProjectUri() : null);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<Boolean> addDBDriver(ModifyDriverRequestParams requestParams) {
        boolean isSuccess = QueryGenerator.addDriverToClassPath(requestParams.addDriverPath, requestParams.className);
        return CompletableFuture.supplyAsync(() -> isSuccess);
    }

    @Override
    public CompletableFuture<Boolean> removeDBDriver(ModifyDriverRequestParams requestParams) {
        boolean response = QueryGenerator.removeDriverFromClassPath(requestParams.removeDriverPath);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<Boolean> modifyDBDriver(ModifyDriverRequestParams requestParams) {
        boolean response = QueryGenerator.modifyDriverInClassPath(requestParams.addDriverPath,
                requestParams.removeDriverPath, requestParams.className);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<String> generateQueries(QueryGenRequestParams requestParams) {
        String xmlContent = QueryGenerator.generateDSSQueries(requestParams);
        return CompletableFuture.supplyAsync(() -> xmlContent);
    }

    @Override
    public CompletableFuture<Map<String, List<Boolean>>> fetchTables(QueryGenRequestParams requestParams) {
        Map<String, List<Boolean>> tableList = QueryGenerator.getTableList(requestParams);
        return CompletableFuture.supplyAsync(() -> tableList);
    }

    @Override
    public CompletableFuture<DirectoryMapResponse> getProjectExplorerModel(WorkspaceFolder param) {

        DirectoryMapResponse response = DirectoryTreeBuilder.getProjectExplorerModel(param);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<List<String>> getProjectIntegrationType(WorkspaceFolder param) {

        List<String> response = OverviewPage.getProjectIntegrationType(param);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<JsonObject> getMediators(MediatorRequest mediatorRequest) {

        ProjectContext ctx = resolve(mediatorRequest.documentIdentifier);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getMediatorHandler().getSupportedMediators(mediatorRequest.documentIdentifier, mediatorRequest.position)
                : new JsonObject());
    }

    @Override
    public CompletableFuture<JsonObject> getMediatorUISchema(UISchemaRequest uiSchemaRequest) {

        ProjectContext ctx = resolve(uiSchemaRequest.documentIdentifier);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getMediatorHandler().getUiSchema(uiSchemaRequest.mediatorType, uiSchemaRequest.documentIdentifier,
                        uiSchemaRequest.position)
                : new JsonObject());
    }

    @Override
    public CompletableFuture<SynapseConfigResponse> generateSynapseConfig(SynapseConfigRequest synapseConfigRequest) {

        ProjectContext ctx = resolveByUri(synapseConfigRequest.documentUri);
        return CompletableFuture.supplyAsync(
                () -> ctx != null ? ctx.getMediatorHandler().generateSynapseConfig(synapseConfigRequest.documentUri,
                        synapseConfigRequest.range, synapseConfigRequest.mediatorType, synapseConfigRequest.values,
                        synapseConfigRequest.dirtyFields) : null);
    }

    @Override
    public CompletableFuture<JsonObject> getMediatorUISchemaWithValues(MediatorRequest mediatorRequest) {

        ProjectContext ctx = resolve(mediatorRequest.documentIdentifier);
        return CompletableFuture.supplyAsync(
                () -> ctx != null ? ctx.getMediatorHandler().getUISchemaWithValues(mediatorRequest.documentIdentifier,
                        mediatorRequest.position) : new JsonObject());
    }

    @Override
    public CompletableFuture<MediatorTryoutInfo> tryOutMediator(MediatorTryoutRequest request) {

        return CompletableFuture.supplyAsync(() -> tryOutManager.tryout(request));
    }

    @Override
    public CompletableFuture<Boolean> shutDownTryoutServer() {

        return CompletableFuture.supplyAsync(() -> Boolean.valueOf(tryOutManager.shutdown()));
    }

    @Override
    public CompletableFuture<MediatorTryoutInfo> mediatorInputOutputSchema(MediatorTryoutRequest request) {

        return CompletableFuture.supplyAsync(() -> tryOutManager.getInputOutputSchema(request));
    }

    @Override
    public CompletableFuture<TestConnectionResponse> testConnectorConnection(TestConnectionRequest request) {

        return CompletableFuture.supplyAsync(() -> tryOutManager.testConnectorConnection(request));
    }

    @Override
    public CompletableFuture<OverviewPageDetailsResponse> getOverviewPageDetails() {
        OverviewPageDetailsResponse response = defaultContext != null
                ? OverviewPage.getDetails(defaultContext.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<ICompletionResponse> expressionCompletion(ExpressionParam param) {

        return CompletableFuture.supplyAsync(() -> ExpressionCompletionsProvider.getCompletions(param));
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(ExpressionParam params) {

        return CompletableFuture.supplyAsync(() -> ExpressionSignatureProvider.getFunctionSignatures(params));
    }

    @Override
    public CompletableFuture<UpdateResponse> updateProperty(UpdatePropertyRequest request) {
        UpdateResponse response = defaultContext != null
                ? PomParser.updateProperty(defaultContext.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<UpdateResponse> updateDependency(UpdateDependencyRequest request) {
        UpdateResponse response = defaultContext != null
                ? PomParser.updateDependency(defaultContext.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<HelperPanelData> expressionHelperData(ExpressionParam param) {

        ProjectContext ctx = resolveByUri(param.getDocumentUri());
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getExpressionHelperProvider().getExpressionHelperData(param) : null);
    }

    @Override
    public CompletableFuture<UpdateResponse> updateConfigFile(UpdateConfigRequest request) {
        UpdateResponse response = defaultContext != null
                ? ConfigParser.updateConfigFile(defaultContext.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<String> updateConnectorDependencies() {
        if (defaultContext == null) {
            return CompletableFuture.supplyAsync(() -> "Project is not initialized");
        }
        String statusMessage = DependencyDownloadManager.downloadDependencies(defaultContext.getProjectUri(),
                defaultContext.getConnectorHolder());
        defaultContext.updateConnectors();
        return CompletableFuture.supplyAsync(() -> statusMessage);
    }

    @Override
    public CompletableFuture<String> refetchIntegrationProjectDependencies() {

        if (defaultContext == null) {
            return CompletableFuture.supplyAsync(() -> "Project is not initialized");
        }
        String projectUri = defaultContext.getProjectUri();
        log.info("Refetching integration project dependencies for project: " + projectUri);
        return CompletableFuture.supplyAsync(() -> DependencyDownloadManager.refetchIntegrationProjectDependencies(projectUri));
    }

    @Override
    public CompletableFuture<DependencyStatusResponse> getDependencyStatusList() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? DependencyDownloadManager.getDependencyStatusList(defaultContext.getProjectUri()) : null);
    }

    @Override
    public CompletableFuture<ConnectorDependencyResponse> getConnectorDependencies(
            ConnectorDependencyRequest request) {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? ConnectorConfigService.buildDependencyResponse(defaultContext.getProjectUri(),
                        request.connectorArtifactId, defaultContext.getConnectorHolder())
                : null);
    }

    @Override
    public CompletableFuture<Boolean> updateConnectorDependencyOverride(
            UpdateConnectorDependencyRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (defaultContext == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateDependencyOverride(defaultContext.getProjectUri(), request,
                        defaultContext.getConnectorHolder());
                return true;
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "Invalid request to updateConnectorDependencyOverride: " + e.getMessage());
                return false;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to update connector dependency override: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> resetConnectorDependencyOverrides(
            ResetConnectorDependencyRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (defaultContext == null) {
                return false;
            }
            try {
                ConnectorConfigService.resetDependencyOverrides(defaultContext.getProjectUri(), request);
                return true;
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "Invalid request to resetConnectorDependencyOverrides: " + e.getMessage());
                return false;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to reset connector dependency overrides: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateConnectorFlags(UpdateConnectorFlagsRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (defaultContext == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateConnectorFlags(defaultContext.getProjectUri(), request,
                        defaultContext.getConnectorHolder());
                return true;
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "Invalid request to updateConnectorFlags: " + e.getMessage());
                return false;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to update connector flags: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateGlobalConnectorFlags(UpdateGlobalConnectorFlagsRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (defaultContext == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateGlobalConnectorFlags(defaultContext.getProjectUri(), request);
                return true;
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "Invalid request to updateGlobalConnectorFlags: " + e.getMessage());
                return false;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to update root connector config: " + e.getMessage(), e);
                return false;
            }
        });
    }

    @Override
    public void initConnectorConfig(ConnectorDependencyRequest request) {

        if (defaultContext != null) {
            ConnectorConfigService.initIfAbsent(defaultContext.getProjectUri());
        }
    }

    @Override
    public CompletableFuture<LoadDependentResourcesResponse> loadDependentResources() {

        return CompletableFuture.supplyAsync(() -> {
            if (defaultContext == null) {
                return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_ERROR,
                        "Project is not initialized");
            }
            String projectUri = defaultContext.getProjectUri();
            log.info("Loading dependent resources for project: " + projectUri);
            LoadDependentResourcesResponse result = defaultContext.getResourceFinder().loadDependentResources(projectUri);
            defaultContext.updateConnectors();
            log.info("Dependent resources loaded successfully for project: " + projectUri);
            return result;
        });
    }

    @Override
    public CompletableFuture<ConnectorGeneratorResponse> generateConnector(ConnectorGenerateRequest connectorGenReq) {
        String filePath = null;
        try {
            if (defaultContext != null) {
                String projectUri = defaultContext.getProjectUri();
                String projectServerVersion = defaultContext.getProjectServerVersion();
                if (connectorGenReq.openAPIPath.endsWith(".proto")) {
                    filePath = GRPCConnectorGenerator.generateConnector(connectorGenReq.openAPIPath,
                            connectorGenReq.connectorProjectPath, projectServerVersion, projectUri);
                } else {
                    filePath = ConnectorGenerator.generateConnector(connectorGenReq.openAPIPath,
                            connectorGenReq.connectorProjectPath, projectServerVersion, projectUri);
                }
            }
        } catch (Exception e) {
            String errorMsg = "Error occurred while generating the connector: " + e.getMessage();
            log.log(Level.SEVERE, errorMsg, e);
            ConnectorGeneratorResponse errorResponse = new ConnectorGeneratorResponse(false, null, errorMsg);
            return CompletableFuture.supplyAsync(() -> errorResponse);
        }
        ConnectorGeneratorResponse response = new ConnectorGeneratorResponse(filePath != null, filePath);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<ArtifactTypeResponse> getArtifactType(TextDocumentIdentifier artifactIdentifier) {

        return CompletableFuture.supplyAsync(() -> SyntaxTreeGenerator.getArtifactType(artifactIdentifier.getUri()));
    }

    @Override
    public CompletableFuture<Map<String, List<DynamicField>>> getDynamicFields(GetDynamicFieldsRequest request) {

        return CompletableFuture.supplyAsync(() -> dynamicFieldsHandler.handleDynamicFieldsRequest(request).getFields());
    }

    @Override
    public CompletableFuture<List<String>> getStoredProcedures(QueryGenRequestParams request) {

        return CompletableFuture.supplyAsync(() -> dynamicFieldsHandler.getStoredProcedures(request));
    }

    @Override
    public CompletableFuture<String> downloadDriverForConnector(DriverDownloadRequest request) {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? ConnectorDownloadManager.downloadDriverForConnector(
                        defaultContext.getProjectUri(),
                        request.getConnectorName(),
                        request.getConnectionType(),
                        defaultContext.getConnectorHolder())
                : null);
    }

    @Override
    public CompletableFuture<DriverMavenCoordinatesResponse> getDriverMavenCoordinates(
            DriverMavenCoordinatesRequest request){
        ProjectContext ctx = resolveByUri(request.getFilePath());
        return CompletableFuture.supplyAsync(() -> ctx != null ? ConnectorDownloadManager.getDriverMavenCoordinates(
                request.getFilePath(),
                request.getConnectorName(),
                request.getConnectionType(),
                ctx.getConnectorHolder()
        ) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> updateMavenDeployPlugin(DeployPluginDetails pluginDetails) {

        return CompletableFuture.supplyAsync(() -> defaultContext != null ? PomParser.addCarDeployPluginToPom(
                new File(defaultContext.getProjectUri() + File.separator + Constants.POM_FILE), pluginDetails) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> getMavenDeployPluginDetails() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null ? PomParser.extractCarDeployPluginFields(
                new File(defaultContext.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<TextEdit> removeMavenDeployPlugin() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null ? PomParser.removeDeployPlugin(
                new File(defaultContext.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<List<ConfigDetails>> getConfigurableList() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? ConfigParser.getConfigDetails(defaultContext.getProjectUri()) : Collections.emptyList());
    }

    @Override
    public CompletableFuture<String> getLocalInboundEndpointsListForCopilot() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? defaultContext.getInboundConnectorHolder().getLocalInboundEndpointsListForCopilot() : null);
    }

    @Override
    public CompletableFuture<List<String>> pdfToImagesBase64(PdfToImagesRequest param) {

    	return CompletableFuture.supplyAsync(() -> Utils.pdfToImage(param.getBase64()));
    }

    @Override
    public CompletableFuture<List<List<Object>>> getInputOutputMappings(MappingsGenRequestParams param) {

        return CompletableFuture.supplyAsync(() -> Constant.INPUT.equals(param.type) ?
                QueryGenerator.getInputMappings(param.query) : QueryGenerator.getOutputMappings(param));
    }

    @Override
    public CompletableFuture<MCPToolResponse> getMCPTools(MCPToolRequest param) {

        log.log(Level.INFO, "Fetching MCP tools for connection: {}", param.connectionName);
        ProjectContext ctx = resolveByUri(param.documentUri);
        if (ctx == null) {
            return CompletableFuture.supplyAsync(() -> null);
        }
        Connections connections = ConnectionFinder.findConnections(ctx.getProjectUri(), Constant.LOWERCASE_AI,
                ctx.getConnectorHolder(), ctx.isLegacyProject()).getLeft();
        AIConnectorHandler aiConnectorHandler = new AIConnectorHandler(ctx.getMediatorHandler(), ctx.getProjectUri());
        log.log(Level.INFO, "Initialized AI connector handler for MCP tools fetch");
        return CompletableFuture.supplyAsync(
                () -> aiConnectorHandler.fetchMcpTools(param.documentUri, param.range, connections.getConnections(),
                        param.connectionName));
    }

    @Override
    public CompletableFuture<ConnectorDetails> isDuplicateConnector(ConnectorDetails connectorDetails) {

        ProjectContext ctx = resolveByUri(connectorDetails.connectorPath);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getConnectorLoader().isDuplicateConnector(connectorDetails.connectorPath) : connectorDetails);
    }

    @Override
    public CompletableFuture<Either<ConnectorInfoResponse, String>> resolveConnector(UpdateDependencyRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (request.dependencies == null || request.dependencies.isEmpty()) {
                return Either.forRight("At least one dependency is required");
            }
            if (defaultContext == null) {
                return Either.forRight("Project is not initialized");
            }
            String projectUri = defaultContext.getProjectUri();

            List<Connector> resolvedConnectors = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (DependencyDetails dep : request.dependencies) {
                if (StringUtils.isAnyBlank(dep.getGroupId(), dep.getArtifact(), dep.getVersion())) {
                    errors.add("Skipping dependency with missing groupId, artifact, or version");
                    continue;
                }
                try {
                    ResolvedArtifact artifact = downloadAndExtractArtifact(
                            projectUri, dep.getGroupId(), dep.getArtifact(), dep.getVersion());
                    ConnectorReader connectorReader = new ConnectorReader();
                    Connector connector = connectorReader.readConnector(
                            artifact.extractDir.getAbsolutePath(), projectUri);
                    if (connector != null) {
                        connector.setConnectorZipPath(artifact.zipFile.getAbsolutePath());
                        resolvedConnectors.add(connector);
                    } else {
                        errors.add("Failed to read connector metadata: " + dep.getArtifact());
                    }
                } catch (IllegalStateException e) {
                    errors.add(e.getMessage());
                } catch (IOException e) {
                    log.log(Level.WARNING, "Error resolving connector: " + dep.getArtifact(), e);
                    errors.add("Error resolving " + dep.getArtifact() + ": " + e.getMessage());
                }
            }

            if (resolvedConnectors.isEmpty() && !errors.isEmpty()) {
                return Either.forRight(String.join("; ", errors));
            }
            List<ConnectorInfoDto> dtos = new ArrayList<>(resolvedConnectors.size());
            for (Connector c : resolvedConnectors) {
                dtos.add(ConnectorInfoDto.from(c));
            }
            return Either.forLeft(new ConnectorInfoResponse(dtos));
        });
    }

    @Override
    public CompletableFuture<String> fetchInboundConnectors() {

        return CompletableFuture.supplyAsync(() -> defaultContext != null
                ? defaultContext.getInboundConnectorHolder().getCustomInboundConnectors() : null);
    }

    public String getProjectUri() {
        return defaultContext != null ? defaultContext.getProjectUri() : null;
    }

    public ConnectorHolder getConnectorHolder() {

        return defaultContext != null ? defaultContext.getConnectorHolder() : null;
    }

    public String getExtensionPath() {

        return extensionPath;
    }

    public String getMiServerPath() {

        return miServerPath;
    }

    public void dispose() {

        if (tryOutManager != null) {
            tryOutManager.shutdown();
        }
    }
}
