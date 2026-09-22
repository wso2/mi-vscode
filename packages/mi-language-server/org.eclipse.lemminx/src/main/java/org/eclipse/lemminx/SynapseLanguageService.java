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
import org.eclipse.lemminx.customservice.synapse.pojo.HasProjectUri;
import org.eclipse.lemminx.customservice.synapse.pojo.ProjectUriRequest;
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
import org.eclipse.lemminx.customservice.synapse.inbound.conector.FetchInboundConnectorsParams;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorResponse;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorParam;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundEndpointInfo;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundInfoRequest;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.DependencyScanner;
import org.eclipse.lemminx.customservice.synapse.dependency.tree.pojo.DependencyTree;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.ServerLessTryoutHandler;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.TryOutManager;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.ShutdownTryoutRequest;
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
import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ArtifactFileScanner;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFinderFactory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynapseLanguageService implements ISynapseLanguageService {

    private static final Logger log = Logger.getLogger(SynapseLanguageService.class.getName());
    private static final CancelChecker NULL_CANCEL_CHECKER = new CancelChecker() {
        @Override
        public void checkCanceled() {
            // Do nothing.
        }
    };

    // Published once per process so callers with no DI path to the live server can resolve a document's ProjectContext.
    private static volatile WorkspaceManager workspaceManagerHolder;

    /**
     * Resolves the {@link ProjectContext} for a document URI (returning {@code null} if none is registered) for callers with no DI path to the live server, using the path-based {@link WorkspaceManager#getProjectForFile} lookup because disk-opened documents' URIs don't always match the client's spelling.
     */
    public static ProjectContext resolveProjectContext(String documentUri) {
        WorkspaceManager manager = workspaceManagerHolder;
        return manager != null && documentUri != null ? manager.getProjectForFile(documentUri) : null;
    }

    private XMLTextDocumentService xmlTextDocumentService;
    private XMLLanguageServer xmlLanguageServer;
    private SynapseLanguageClientAPI languageClient;
    private String extensionPath;
    private String miServerPath;
    private TryOutManager tryOutManager;
    // Serializes the stop-the-old-server/start-a-new-one handover in bindTryOutManager, since concurrent
    // try-out requests from two projects could otherwise both pass the "not mine" check.
    private final Object tryOutBindLock = new Object();
    // Resource finders for project roots the debug flow names but no ProjectContext owns, keyed by
    // normalized path; see unregisteredProjectResourceFinder for why these are held.
    private final Map<String, AbstractResourceFinder> unregisteredProjectFinders = new ConcurrentHashMap<>();
    private DynamicFieldsHandler dynamicFieldsHandler;
    private final URIResolverExtensionManager uriResolverExtensionManager;

    public SynapseLanguageService(XMLTextDocumentService xmlTextDocumentService, XMLLanguageServer xmlLanguageServer) {

        this.xmlTextDocumentService = xmlTextDocumentService;
        this.xmlLanguageServer = xmlLanguageServer;
        uriResolverExtensionManager = xmlLanguageServer.getXMLLanguageService().getResolverExtensionManager();
        this.dynamicFieldsHandler = new DynamicFieldsHandler();
        workspaceManagerHolder = xmlLanguageServer.getWorkspaceManager();
    }

    /**
     * Parses the {@code extensionPath}/{@code miServerPath} settings, split out from {@link #init} so {@code XMLLanguageServer} can call it before building this process's {@link ProjectContext}s (which need {@link #getMiServerPath()}).
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

    /**
     * Completes initialisation once {@code XMLLanguageServer} has registered every workspace project, no longer binding a default project or pre-loading state for just one.
     *
     * @param projectUri the client's {@code rootPath}; retained for logging only
     */
    public void init(String projectUri, Object settings, SynapseLanguageClientAPI languageClient) {

        this.languageClient = languageClient;
        applySettings(settings);
        int registered = xmlLanguageServer.getWorkspaceManager().getAllProjects().size();
        if (registered == 0) {
            log.log(Level.WARNING, "Language server initialized with no MI projects registered (rootPath: "
                    + projectUri + "). Requests will resolve to no project until one is added.");
        } else {
            log.log(Level.INFO, "Language server initialized with " + registered + " MI project(s) registered.");
        }
    }

    // -------------------------------------------------------------------------
    // Dispatch: resolves the ProjectContext for a request, or null — never another project's.
    // -------------------------------------------------------------------------

    /**
     * Resolves a {@link ProjectContext} from a document {@code file://} URI, returning {@code null} (by design, so the caller answers empty rather than with another project's data) when it matches none.
     */
    private ProjectContext resolveByUri(String documentUri) {
        return resolve(documentUri, "document URI", WorkspaceManager::getProjectForDocument);
    }

    /**
     * Shared body of the {@code resolveBy*} family: resolves {@code value} via {@code lookup}, logging blank vs. unmatched at different levels since both resolve to no project.
     *
     * @param value     the request field to resolve from
     * @param fieldName what that field is, for the log
     * @param lookup    the registry lookup to run
     * @return the resolved project, or {@code null} if the field is blank or matches none
     */
    private ProjectContext resolve(String value, String fieldName, ProjectLookup lookup) {
        if (StringUtils.isBlank(value)) {
            log.log(Level.FINE, "Request carried no " + fieldName + "; resolving to no project.");
            return null;
        }
        if (xmlLanguageServer == null) {
            return null;
        }
        ProjectContext context = lookup.find(xmlLanguageServer.getWorkspaceManager(), value);
        if (context == null) {
            // The lookup already logs the miss; this adds the facade-level consequence for the log.
            log.log(Level.WARNING, "No registered project for " + fieldName + ": " + value
                    + " — request will be answered with an empty result, not another project's data.");
        }
        return context;
    }

    /** A {@link WorkspaceManager} lookup, as used by {@link #resolve}. */
    @FunctionalInterface
    private interface ProjectLookup {
        ProjectContext find(WorkspaceManager manager, String value);
    }

    private ProjectContext resolve(TextDocumentIdentifier document) {
        return resolveByUri(document != null ? document.getUri() : null);
    }

    private ProjectContext resolve(DefinitionParams params) {
        return resolve(params != null ? params.getTextDocument() : null);
    }

    /**
     * Resolves a {@link ProjectContext} from a filesystem-path request field via {@link WorkspaceManager#getProjectForFile}, matching by normalized path rather than URI because Windows drive-letter percent-encoding otherwise fails to match for every document (a {@code file://} URI value is accepted too).
     *
     * @return the owning project, or {@code null} if the path is blank, unparseable, or outside every
     *         registered project
     */
    private ProjectContext resolveByPath(String filePath) {
        return resolve(filePath, "file path", WorkspaceManager::getProjectForFile);
    }

    /**
     * Resolves a {@link ProjectContext} from an explicit project root (an OS path or {@code file://} URI) via {@link WorkspaceManager#getProjectByPath(String)}, since RPCs send it as {@code WorkspaceFolder.uri.fsPath} rather than the registry's URI key.
     *
     * @return the named project, or {@code null} if {@code projectUri} is blank or names no
     *         registered project
     */
    private ProjectContext resolveByProjectUri(String projectUri) {
        return resolve(projectUri, "projectUri", WorkspaceManager::getProjectByPath);
    }

    /**
     * Null-safe overload for RPCs whose only project hint is the request's own {@code projectUri}, guarding {@code request == null} once instead of at every call site.
     *
     * @param request the request naming the project, may be null
     * @return the named project, or {@code null} if the request or its field names none
     */
    private ProjectContext resolveByProjectUri(HasProjectUri request) {
        return resolveByProjectUri(request != null ? request.getProjectUri() : null);
    }

    /**
     * Resolves a {@link ProjectContext} preferring an explicit document URI/path, falling back to a project root URI only when no document URI was supplied (an unmatched document URI is never retried against {@code projectUri}, to avoid answering from a project that doesn't own it).
     *
     * @return the resolved project, or {@code null} if neither field identifies one
     */
    private ProjectContext resolveByUriOrProjectUri(String documentUri, String projectUri) {
        if (StringUtils.isNotBlank(documentUri)) {
            return resolveByUri(documentUri);
        }
        return resolveByProjectUri(projectUri);
    }

    /** Null-safe overload of {@link #resolveByUriOrProjectUri(String, String)}. */
    private ProjectContext resolveByUriOrProjectUri(String documentUri, HasProjectUri request) {
        return resolveByUriOrProjectUri(documentUri, request != null ? request.getProjectUri() : null);
    }

    /**
     * Resolves a {@link ProjectContext} from a file path, falling back to the named project root when the path is unmatched (not just blank) — e.g. a try-out file extracted from a {@code .car} dependency, outside every project root but still tied to the requesting project's panel.
     *
     * @return the resolved project, or {@code null} if neither field identifies one
     */
    private ProjectContext resolveByPathOrNamedProject(String filePath, String projectUri) {
        ProjectContext context = resolveByPath(filePath);
        if (context == null && StringUtils.isNotBlank(projectUri)) {
            log.log(Level.INFO, "File is outside every registered project: " + filePath
                    + " — falling back to the project the request names: " + projectUri);
            context = resolveByProjectUri(projectUri);
        }
        return context;
    }

    /** Null-safe overload of {@link #resolveByPathOrNamedProject(String, String)}. */
    private ProjectContext resolveByPathOrNamedProject(String filePath, HasProjectUri request) {
        return resolveByPathOrNamedProject(filePath, request != null ? request.getProjectUri() : null);
    }

    /**
     * The {@link #resolveByPath} counterpart of {@link #resolveByUriOrProjectUri}: prefers a filesystem path field when present, falling back to an explicit project root for requests whose path is legitimately optional (blank by design).
     *
     * @return the resolved project, or {@code null} if neither field identifies one
     */
    private ProjectContext resolveByPathOrProjectUri(String filePath, String projectUri) {
        if (StringUtils.isNotBlank(filePath)) {
            return resolveByPath(filePath);
        }
        return resolveByProjectUri(projectUri);
    }

    /** Null-safe overload of {@link #resolveByPathOrProjectUri(String, String)}. */
    private ProjectContext resolveByPathOrProjectUri(String filePath, HasProjectUri request) {
        return resolveByPathOrProjectUri(filePath, request != null ? request.getProjectUri() : null);
    }

    /**
     * Resolves the single, process-global {@link TryOutManager} for {@code ctx}, taking over the shared single-port MI server (shutting down the currently bound manager) when it points at a different project.
     *
     * @param requestServerPath the initiating project's configured MI server path (may be blank/null);
     *                           used instead of the process-global {@link #miServerPath} when this call
     *                           is what creates a new {@link TryOutManager}, so the single shared server
     *                           launches the runtime the *initiating* project expects
     * @return the {@link TryOutManager} bound to {@code ctx}'s project, or {@code null} if {@code ctx}
     *         is {@code null} — the caller should then surface {@link #tryOutUnavailableMessage()}
     */
    private TryOutManager bindTryOutManager(ProjectContext ctx, String requestServerPath) {
        if (ctx == null) {
            return null;
        }
        synchronized (tryOutBindLock) {
            // Same project-root comparison as every other ownership check: a raw equals would read
            // two spellings of one folder as two projects.
            if (tryOutManager != null
                    && WorkspaceManager.isSameProjectPath(ctx.getProjectUri(), tryOutManager.getProjectUri())) {
                return tryOutManager;
            }
            if (tryOutManager != null) {
                // Take the shared server over from the project that currently holds it; shutdown() blocks
                // until the MI port is free, so the manager created below can bind it right away.
                log.log(Level.INFO, String.format(
                        "Stopping the try-out server of project '%s' to start one for project '%s'.",
                        tryOutManager.getProjectUri(), ctx.getProjectUri()));
                tryOutManager.shutdown();
            }
            String effectiveServerPath = StringUtils.isNotBlank(requestServerPath) ? requestServerPath : miServerPath;
            tryOutManager = new TryOutManager(ctx.getProjectUri(), effectiveServerPath, ctx.getProjectServerVersion(),
                    ctx.getConnectorHolder(), languageClient);
            return tryOutManager;
        }
    }

    /**
     * Explains why {@link #bindTryOutManager} declined, which now only happens for an unresolvable project.
     */
    private String tryOutUnavailableMessage() {
        return "This request does not identify an open MI project, so no try-out server could be "
                + "started. Reopen the file from its project folder and try again.";
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

        ProjectContext ctx = resolveByProjectUri(dbConnectionTestParams);
        if (ctx == null) {
            return CompletableFuture.completedFuture(new DBConnectionTestResponse(false));
        }
        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        boolean connectionStatus = dbConnectionTester.testDBConnection(dbConnectionTestParams.dbType,
                    dbConnectionTestParams.username, dbConnectionTestParams.password,
                    dbConnectionTestParams.host, dbConnectionTestParams.port, dbConnectionTestParams.dbName,
                    dbConnectionTestParams.url, dbConnectionTestParams.className,
                    ctx.getProjectUri());
        DBConnectionTestResponse response = new DBConnectionTestResponse(connectionStatus);
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<DBConnectionTestResponse> loadDriverAndTestConnection(DBConnectionTestParams request){
        ProjectContext ctx = resolveByProjectUri(request);
        if (ctx == null) {
            return CompletableFuture.completedFuture(new DBConnectionTestResponse(false));
        }
        DBConnectionTester dbConnectionTester = new DBConnectionTester();
        boolean connectionStatus = dbConnectionTester.testDBConnection(request.dbType,
                request.username, request.password,
                request.host, request.port, request.dbName,
                request.url, request.className, request.driverPath, ctx.getProjectUri());
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
            //
            // TODO(unrouted-request): a blank fileName resolves no ProjectContext, so validation runs
            // degraded without connector/dependent-artifact knowledge until the agent/copilot caller
            // sends an explicit projectUri (or, meanwhile, fileName).
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

    /**
     * Lists one project's artifacts (its own plus its {@code .car} dependencies) for the property-panel dropdowns, routed via the request's {@code projectUri}/document or, for the debug flow's {@code customProjectUri}, via {@link #unregisteredProjectResourceFinder} when that project isn't open in the workspace.
     */
    @Override
    public CompletableFuture<ResourceResponse> availableResources(ResourceParam param) {

        boolean isDebugFlow = StringUtils.isNotBlank(param.customProjectUri);
        ProjectContext ctx = isDebugFlow
                ? resolveByProjectUri(param.customProjectUri)
                : resolveByUriOrProjectUri(param.getDocumentUri(), param);
        String effectivePath = StringUtils.isNotBlank(param.projectPath) ? param.projectPath
                : isDebugFlow ? param.customProjectUri
                : ctx != null ? ctx.getProjectUri() : null;
        AbstractResourceFinder resourceFinder = ctx != null ? ctx.getResourceFinder()
                : isDebugFlow ? unregisteredProjectResourceFinder(effectivePath) : null;
        ResourceResponse response;
        if (resourceFinder == null) {
            response = new ResourceResponse();
        } else if (StringUtils.isNotBlank(param.dataServiceName)) {
            response = resourceFinder.getDataServiceOperations(effectivePath, param.dataServiceName);
        } else {
            response = resourceFinder.getAvailableResources(effectivePath, param.resourceType);
        }
        return CompletableFuture.supplyAsync(() -> response);
    }

    /**
     * A stand-in {@link AbstractResourceFinder}, cached per project root, that lets the debug flow list a project not open in the workspace by scanning its directory and loading its already-extracted {@code .car} dependencies from disk (downloading nothing, and never deleting on conflict, unlike the live {@code loadDependentResources} RPC).
     *
     * @param projectPath the project root to scan
     * @return a finder for {@code projectPath}, or {@code null} if it is blank
     */
    private AbstractResourceFinder unregisteredProjectResourceFinder(String projectPath) {

        if (StringUtils.isBlank(projectPath)) {
            return null;
        }
        // Keyed by the normalized root so two spellings of one project share a finder, but built from
        // the path as named, since re-spelling it would hash to a nonexistent dependency directory.
        return unregisteredProjectFinders.computeIfAbsent(WorkspaceManager.normalizeProjectPath(projectPath),
                key -> buildUnregisteredProjectResourceFinder(projectPath));
    }

    /** Builds the finder {@link #unregisteredProjectResourceFinder} caches, kept separate so the one-time work reads as a unit rather than a lambda inside the lookup. */
    private AbstractResourceFinder buildUnregisteredProjectResourceFinder(String projectPath) {

        log.log(Level.INFO, "Building a ResourceFinder for a project the debug flow named but that is "
                + "not registered: " + projectPath);
        AbstractResourceFinder finder =
                ResourceFinderFactory.getResourceFinder(Utils.isLegacyProject(projectPath), new ConnectorHolder());
        try {
            LoadDependentResourcesResponse result = finder.loadDependentResources(projectPath);
            log.log(Level.INFO, "Dependent resources for unregistered project " + projectPath + ": "
                    + result.getStatus() + " — " + result.getMessage());
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load dependent resources for unregistered project: " + projectPath
                    + " — only its own artifacts will be listed.", e);
        }
        return finder;
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
            ProjectContext ctx = resolveByProjectUri(request);
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
            ProjectContext ctx = resolveByProjectUri(request);
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

    @Override
    public CompletableFuture<List<String>> getRegistryFiles(TextDocumentIdentifier param) {

        ProjectContext ctx = resolve(param);
        List<String> registryFiles = ctx != null
                ? RegistryFileScanner.scanRegistryFiles(ctx.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> registryFiles);
    }

    @Override
    public CompletableFuture<List<String>> getResourceFiles(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        List<String> resourceFiles = ctx != null
                ? ResourceFileScanner.scanResourceFiles(ctx.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> resourceFiles);
    }

    @Override
    public CompletableFuture<List<ConfigurableEntry>> getConfigurableEntries(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        if (ctx == null) {
            return CompletableFuture.supplyAsync(ArrayList::new);
        }
        try {
            List<ConfigurableEntry> configurableEntries = ConfigParser.scanConfigurableEntries(ctx.getProjectUri());
            return CompletableFuture.supplyAsync(() -> configurableEntries);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error while scanning configurable entries.", e);
            return CompletableFuture.supplyAsync(() -> new ArrayList<>());
        }
    }

    @Override
    public CompletableFuture<List<String>> getResourceUsages(ResourceUsagesRequest resourceUsagesRequest) {

        // resourceFilePath is a filesystem path (the project explorer passes it straight to
        // Uri.file(..)), so it must be resolved as a path, not as a document URI.
        ProjectContext ctx = resolveByPath(resourceUsagesRequest.getResourceFilePath());
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

        ProjectContext ctx = resolveByUriOrProjectUri(param.documentPath, param);
        return CompletableFuture.supplyAsync(() -> ctx != null
                && ctx.getInboundConnectorHolder().saveInboundConnector(param.connectorName, param.uiSchema));
    }

    @Override
    public CompletableFuture<InboundConnectorResponse> getInboundConnectorSchema(InboundConnectorParam param) {

        // documentPath is only sent when editing an existing inbound endpoint (a new one sends only
        // connectorId), so routing on it alone resolved every "pick a connector" click to no project;
        // fall back to the project the caller named.
        ProjectContext ctx = resolveByPathOrProjectUri(param.documentPath, param);
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
    public CompletableFuture<JsonObject> getLocalInboundConnectors(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getInboundConnectorHolder().getLocalInboundConnectorList() : new JsonObject());
    }

    @Override
    public CompletableFuture<JsonObject> getConnectionUISchema(ConnectionUIParam param) {

        ProjectContext ctx = resolveByUriOrProjectUri(param.getDocumentUri(), param);
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
    public CompletableFuture<OverviewModel> getOverviewModel(ProjectUriRequest request) {
        ProjectContext ctx = resolveByProjectUri(request);
        OverviewModel overviewModel = ctx != null
                ? OverviewModelGenerator.getOverviewModel(ctx.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> overviewModel);
    }

    @Override
    public CompletableFuture<CheckDBDriverResponseParams> checkDBDriver(CheckDBDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams);
        CheckDBDriverResponseParams response = QueryGenerator.isDriverAvailableInClassPath(requestParams.className,
                ctx != null ? ctx.getProjectUri() : null);
        return CompletableFuture.supplyAsync(() -> response);
    }

    // The DB-driver group below resolves the project before mutating its driver classpath, since an
    // unresolved raw projectUri would otherwise silently create/mutate a phantom classloader entry.
    @Override
    public CompletableFuture<Boolean> addDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams);
        if (ctx == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        boolean isSuccess = QueryGenerator.addDriverToClassPath(requestParams.addDriverPath, requestParams.className,
                ctx.getProjectUri());
        return CompletableFuture.supplyAsync(() -> isSuccess);
    }

    @Override
    public CompletableFuture<Boolean> removeDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams);
        if (ctx == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        boolean response = QueryGenerator.removeDriverFromClassPath(requestParams.removeDriverPath,
                ctx.getProjectUri());
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<Boolean> modifyDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams);
        if (ctx == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        boolean response = QueryGenerator.modifyDriverInClassPath(requestParams.addDriverPath,
                requestParams.removeDriverPath, requestParams.className, ctx.getProjectUri());
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

        // documentUri is a filesystem path despite the name — MediatorHandler.generateSynapseConfig
        // does Files.exists(Path.of(documentUri)), and the client compares it to doc.uri.fsPath.
        ProjectContext ctx = resolveByPath(synapseConfigRequest.documentUri);
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

        ProjectContext ctx = resolveByPathOrNamedProject(request.getFile(), request);
        return CompletableFuture.supplyAsync(() -> {
            TryOutManager manager = bindTryOutManager(ctx, request.getServerPath());
            if (manager == null) {
                return new MediatorTryoutInfo(tryOutUnavailableMessage());
            }
            return manager.tryout(request);
        });
    }

    @Override
    public CompletableFuture<Boolean> shutDownTryoutServer(ShutdownTryoutRequest request) {

        // Only tears down the shared TryOutManager when the request's project root (matched via
        // WorkspaceManager.isSameProjectPath, not String.equals, to tolerate spelling differences and
        // without requiring the project to still be registered) matches the one currently bound, so an
        // unrelated or unproven shutdown call can't kill another project's try-out session or leak the server.
        return CompletableFuture.supplyAsync(() -> {
            // Same lock as bindTryOutManager, to avoid shutting down a manager another project just
            // bound or reading a half-published one.
            synchronized (tryOutBindLock) {
                if (tryOutManager == null) {
                    return true;
                }
                String requestProjectUri = request != null ? request.getProjectUri() : null;
                if (StringUtils.isBlank(requestProjectUri)) {
                    log.log(Level.WARNING, String.format(
                            "Ignoring a try-out shutdown request that carries no project; the server bound "
                                    + "to '%s' is left running because the request cannot be shown to own it.",
                            tryOutManager.getProjectUri()));
                    return true;
                }
                if (!WorkspaceManager.isSameProjectPath(requestProjectUri, tryOutManager.getProjectUri())) {
                    return true;
                }
                return tryOutManager.shutdown();
            }
        });
    }

    @Override
    public CompletableFuture<MediatorTryoutInfo> mediatorInputOutputSchema(MediatorTryoutRequest request) {

        // Schema generation is a lightweight, stateless read, so it's served directly from the resolved
        // project rather than the single rebindable TryOutManager, and never blocked by another project's try-out.
        ProjectContext ctx = resolveByPathOrNamedProject(request.getFile(), request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? new ServerLessTryoutHandler(ctx.getProjectUri(), ctx.getConnectorHolder()).handle(request)
                : new MediatorTryoutInfo("Project is not initialized"));
    }

    @Override
    public CompletableFuture<TestConnectionResponse> testConnectorConnection(TestConnectionRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            TryOutManager manager = bindTryOutManager(ctx, null);
            if (manager == null) {
                return new TestConnectionResponse(tryOutUnavailableMessage());
            }
            return manager.testConnectorConnection(request);
        });
    }

    @Override
    public CompletableFuture<OverviewPageDetailsResponse> getOverviewPageDetails(ProjectUriRequest request) {
        ProjectContext ctx = resolveByProjectUri(request);
        OverviewPageDetailsResponse response = ctx != null
                ? OverviewPage.getDetails(ctx.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> response);
    }

    // Unrouted by design: signatureHelp uses a static, project-independent catalogue, and
    // ExpressionCompletionsProvider (also reached with no ProjectContext to pass) resolves its own project.
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
        ProjectContext ctx = resolveByProjectUri(request);
        UpdateResponse response = ctx != null
                ? PomParser.updateProperty(ctx.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<UpdateResponse> updateDependency(UpdateDependencyRequest request) {
        ProjectContext ctx = resolveByProjectUri(request);
        UpdateResponse response = ctx != null
                ? PomParser.updateDependency(ctx.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<HelperPanelData> expressionHelperData(ExpressionParam param) {

        // documentUri is a filesystem path despite the name — ExpressionHelperProvider does
        // new File(documentUri), which throws InvalidPathException on a file:// URI.
        ProjectContext ctx = resolveByPath(param.getDocumentUri());
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getExpressionHelperProvider().getExpressionHelperData(param) : null);
    }

    @Override
    public CompletableFuture<UpdateResponse> updateConfigFile(UpdateConfigRequest request) {
        ProjectContext ctx = resolveByProjectUri(request);
        UpdateResponse response = ctx != null
                ? ConfigParser.updateConfigFile(ctx.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<String> updateConnectorDependencies(ProjectUriRequest request) {
        ProjectContext ctx = resolveByProjectUri(request);
        if (ctx == null) {
            return CompletableFuture.supplyAsync(() -> "Project is not initialized");
        }
        String statusMessage = DependencyDownloadManager.downloadDependencies(ctx.getProjectUri(),
                ctx.getConnectorHolder());
        ctx.updateConnectors();
        return CompletableFuture.supplyAsync(() -> statusMessage);
    }

    @Override
    public CompletableFuture<String> refetchIntegrationProjectDependencies(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        if (ctx == null) {
            return CompletableFuture.supplyAsync(() -> "Project is not initialized");
        }
        String projectUri = ctx.getProjectUri();
        log.info("Refetching integration project dependencies for project: " + projectUri);
        return CompletableFuture.supplyAsync(() -> DependencyDownloadManager.refetchIntegrationProjectDependencies(projectUri));
    }

    @Override
    public CompletableFuture<DependencyStatusResponse> getDependencyStatusList(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? DependencyDownloadManager.getDependencyStatusList(ctx.getProjectUri()) : null);
    }

    @Override
    public CompletableFuture<ConnectorDependencyResponse> getConnectorDependencies(
            ConnectorDependencyRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ConnectorConfigService.buildDependencyResponse(ctx.getProjectUri(),
                        request.connectorArtifactId, ctx.getConnectorHolder())
                : null);
    }

    @Override
    public CompletableFuture<Boolean> updateConnectorDependencyOverride(
            UpdateConnectorDependencyRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateDependencyOverride(ctx.getProjectUri(), request,
                        ctx.getConnectorHolder());
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

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return false;
            }
            try {
                ConnectorConfigService.resetDependencyOverrides(ctx.getProjectUri(), request);
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

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateConnectorFlags(ctx.getProjectUri(), request,
                        ctx.getConnectorHolder());
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

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return false;
            }
            try {
                ConnectorConfigService.updateGlobalConnectorFlags(ctx.getProjectUri(), request);
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

        ProjectContext ctx = resolveByProjectUri(request);
        if (ctx != null) {
            ConnectorConfigService.initIfAbsent(ctx.getProjectUri());
        }
    }

    @Override
    public CompletableFuture<LoadDependentResourcesResponse> loadDependentResources(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> {
            if (ctx == null) {
                return new LoadDependentResourcesResponse(LoadDependentResourcesResponse.STATUS_ERROR,
                        "Project is not initialized");
            }
            String projectUri = ctx.getProjectUri();
            log.info("Loading dependent resources for project: " + projectUri);
            LoadDependentResourcesResponse result = ctx.getResourceFinder().loadDependentResources(projectUri);
            ctx.updateConnectors();
            log.info("Dependent resources loaded successfully for project: " + projectUri);
            return result;
        });
    }

    @Override
    public CompletableFuture<ConnectorGeneratorResponse> generateConnector(ConnectorGenerateRequest connectorGenReq) {
        String filePath = null;
        try {
            ProjectContext ctx = resolveByProjectUri(connectorGenReq);
            if (ctx != null) {
                String projectUri = ctx.getProjectUri();
                String projectServerVersion = ctx.getProjectServerVersion();
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

    // Unrouted by design: the handler is stateless and its driver classloader is keyed by projectUri.
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

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ConnectorDownloadManager.downloadDriverForConnector(
                        ctx.getProjectUri(),
                        request.getConnectorName(),
                        request.getConnectionType(),
                        ctx.getConnectorHolder())
                : null);
    }

    @Override
    public CompletableFuture<DriverMavenCoordinatesResponse> getDriverMavenCoordinates(
            DriverMavenCoordinatesRequest request){
        // filePath is blank on first use, before the driver is downloaded, so routing on it alone
        // resolved that case to no project and broke the DB operation form's validation; fall back to
        // the project the caller named.
        ProjectContext ctx = resolveByPathOrProjectUri(request.getFilePath(), request);
        return CompletableFuture.supplyAsync(() -> ctx != null ? ConnectorDownloadManager.getDriverMavenCoordinates(
                request.getFilePath(),
                request.getConnectorName(),
                request.getConnectionType(),
                ctx.getConnectorHolder()
        ) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> updateMavenDeployPlugin(DeployPluginDetails pluginDetails) {

        ProjectContext ctx = resolveByProjectUri(pluginDetails);
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.addCarDeployPluginToPom(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE), pluginDetails) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> getMavenDeployPluginDetails(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.extractCarDeployPluginFields(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<TextEdit> removeMavenDeployPlugin(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.removeDeployPlugin(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<List<ConfigDetails>> getConfigurableList(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ConfigParser.getConfigDetails(ctx.getProjectUri()) : Collections.emptyList());
    }

    @Override
    public CompletableFuture<String> getLocalInboundEndpointsListForCopilot(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getInboundConnectorHolder().getLocalInboundEndpointsListForCopilot() : null);
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

        // connectorPath is the zip's path on disk — the loader opens it with new ZipFile(..).
        ProjectContext ctx = resolveByPath(connectorDetails.connectorPath);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getConnectorLoader().isDuplicateConnector(connectorDetails.connectorPath) : connectorDetails);
    }

    @Override
    public CompletableFuture<Either<ConnectorInfoResponse, String>> resolveConnector(UpdateDependencyRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (request.dependencies == null || request.dependencies.isEmpty()) {
                return Either.forRight("At least one dependency is required");
            }
            ProjectContext ctx = resolveByProjectUri(request);
            if (ctx == null) {
                return Either.forRight("Project is not initialized");
            }
            String projectUri = ctx.getProjectUri();

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
    public CompletableFuture<String> fetchInboundConnectors(FetchInboundConnectorsParams params) {

        ProjectContext ctx = resolveByProjectUri(params);
        String targetZipName = params != null ? params.zipFileName : null;
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getInboundConnectorHolder().getCustomInboundConnectors(targetZipName) : null);
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
