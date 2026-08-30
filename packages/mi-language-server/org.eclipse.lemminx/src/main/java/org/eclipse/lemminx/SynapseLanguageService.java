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

public class SynapseLanguageService implements ISynapseLanguageService {

    private static final Logger log = Logger.getLogger(SynapseLanguageService.class.getName());
    private static final CancelChecker NULL_CANCEL_CHECKER = new CancelChecker() {
        @Override
        public void checkCanceled() {
            // Do nothing.
        }
    };

    // Published once per process so static-context-only callers (e.g. IDiagnosticsParticipant, which
    // has no DI path to the live server instance) can resolve a document's ProjectContext.
    private static volatile WorkspaceManager workspaceManagerHolder;

    /**
     * Resolves the {@link ProjectContext} for a document URI, for callers with no DI path to the live
     * {@code XMLLanguageServer}/{@code WorkspaceManager} (e.g. {@code SynapseDiagnosticsParticipant},
     * {@code SyntaxTreeUtils}). Returns {@code null} if no project is registered for the document (or
     * none has initialized yet).
     *
     * <p>This resolves through {@link WorkspaceManager#getProjectForFile} — the path-based lookup —
     * rather than {@link WorkspaceManager#getProjectForDocument}, because the URIs reaching here are
     * <em>not</em> always the ones the client sent. A {@link org.eclipse.lemminx.dom.DOMDocument} that
     * lemminx opened from disk itself carries {@code path.toUri().toString()}
     * ({@code Utils.getDOMDocument(File)}), which on Windows spells the drive-letter colon differently
     * from the workspace-folder URIs the registry is keyed by ({@code c:} vs. {@code c%3A}) and so can
     * never prefix-match one. The consequence is silent and severe rather than a visible error: callers
     * fall back to a default {@code MediatorFactoryFinder} with an empty {@code ConnectorHolder}, so
     * every connector call ({@code http.get}, …) parses as an {@code InvalidMediator}.
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
    // Serializes the stop-the-old-server/start-a-new-one handover in bindTryOutManager. Try-out
    // requests are served on the common pool, so two projects can ask to bind at once; without this,
    // both would pass the "not mine" check and launch a server for the single shared MI port.
    private final Object tryOutBindLock = new Object();
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
     * Parses the {@code extensionPath}/{@code miServerPath} settings. Split out from {@link #init} so
     * {@code XMLLanguageServer} can call it before building this process's {@link ProjectContext}s
     * (which need {@link #getMiServerPath()}), then call {@link #init} afterwards.
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
     * Completes initialisation once {@code XMLLanguageServer} has registered every workspace project.
     *
     * <p>This no longer captures a default project, and no longer pre-binds anything to one:
     * <ul>
     *   <li>the shared {@link TryOutManager} is created lazily, per request, by
     *       {@link #bindTryOutManager(ProjectContext, String)}, which binds it to whichever project
     *       actually asked;</li>
     *   <li>each project's DB-driver classloader is seeded from its own {@code deployment/libs} by
     *       {@link ProjectContext#initProject}, so every registered project gets one — not just the
     *       first, which is all this method could ever have done.</li>
     * </ul>
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
    // Dispatch — resolves the ProjectContext for a request, falling back to
    // null when the request carries no resolvable project reference. Never another project.
    // -------------------------------------------------------------------------

    /**
     * Resolves a {@link ProjectContext} from a document {@code file://} URI, or {@code null} when the
     * document belongs to no registered project.
     *
     * <p>Returning {@code null} for an <i>unmatched</i> URI is the point: the caller answers with an
     * empty result rather than another project's data. See the class javadoc.
     */
    private ProjectContext resolveByUri(String documentUri) {
        if (StringUtils.isBlank(documentUri)) {
            log.log(Level.FINE, "Request carried no document URI; resolving to no project.");
            return null;
        }
        if (xmlLanguageServer == null) {
            return null;
        }
        ProjectContext context = xmlLanguageServer.getWorkspaceManager().getProjectForDocument(documentUri);
        if (context == null) {
            // getProjectForDocument already logs the miss; add the facade-level consequence so the
            // pair reads as one story in the log.
            log.log(Level.WARNING, "No registered project for document: " + documentUri
                    + " — request will be answered with an empty result, not another project's data.");
        }
        return context;
    }

    private ProjectContext resolve(TextDocumentIdentifier document) {
        return resolveByUri(document != null ? document.getUri() : null);
    }

    private ProjectContext resolve(DefinitionParams params) {
        return resolve(params != null ? params.getTextDocument() : null);
    }

    /**
     * Resolves a {@link ProjectContext} from a request field that carries a filesystem path (e.g.
     * {@link MediatorTryoutRequest#getFile()}) by matching normalized {@link java.nio.file.Path}s via
     * {@link WorkspaceManager#getProjectForFile}.
     *
     * <p>Use this — not {@link #resolveByUri} — for any field the handler itself dereferences as a
     * path ({@code new File(..)}, {@code Path.of(..)}, {@code new ZipFile(..)}). Routing such a field
     * through {@code resolveByUri} resolves every request to no project at all.
     *
     * <p><b>Do not "fix" this by converting the path to a URI and delegating to {@link #resolveByUri}.</b>
     * That was the previous implementation and it resolved to no project for <em>every</em> document on
     * Windows: the registry is keyed by the workspace-folder URIs exactly as the client sent them, and
     * VS Code percent-encodes the drive-letter colon ({@code file:///c%3A/Users/...}) where
     * {@code Path.toUri()} does not ({@code file:///c:/Users/...}), so the prefix match could never
     * hit. Comparing as paths removes URI spelling from the equation entirely. Note that a URI-based
     * test harness can hide this — Node's {@code pathToFileURL} emits the same unencoded spelling
     * Java does, so a probe keyed that way matches the buggy form and reports success.
     *
     * <p>A value that is already a {@code file://} URI is accepted too, so this is safe for the fields
     * whose callers are inconsistent about which of the two forms they send.
     *
     * @return the owning project, or {@code null} if the path is blank, unparseable, or outside every
     *         registered project
     */
    private ProjectContext resolveByPath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            log.log(Level.FINE, "Request carried no file path; resolving to no project.");
            return null;
        }
        if (xmlLanguageServer == null) {
            return null;
        }
        ProjectContext context = xmlLanguageServer.getWorkspaceManager().getProjectForFile(filePath);
        if (context == null) {
            // getProjectForFile already logs the miss; add the facade-level consequence so the pair
            // reads as one story in the log.
            log.log(Level.WARNING, "No registered project for file: " + filePath
                    + " — request will be answered with an empty result, not another project's data.");
        }
        return context;
    }

    /**
     * Resolves a {@link ProjectContext} from an explicit project root (e.g. the {@code projectUri}
     * field on RPCs that carry no document to resolve a project from). The VS Code extension sends
     * this field as {@code WorkspaceFolder.uri.fsPath} — an absolute filesystem path, not the
     * {@code file://} URI {@link WorkspaceManager} registers projects under — so this resolves via
     * {@link WorkspaceManager#getProjectByPath(String)}, which matches on each context's own
     * {@link ProjectContext#getProjectUri()} instead of the registry key. Both an OS path and a
     * {@code file://} URI are accepted, since that method normalizes either form.
     *
     * @return the named project, or {@code null} if {@code projectUri} is blank or names no
     *         registered project
     */
    private ProjectContext resolveByProjectUri(String projectUri) {
        if (StringUtils.isBlank(projectUri)) {
            log.log(Level.FINE, "Request carried no projectUri; resolving to no project.");
            return null;
        }
        if (xmlLanguageServer == null) {
            return null;
        }
        ProjectContext context = xmlLanguageServer.getWorkspaceManager().getProjectByPath(projectUri);
        if (context == null) {
            log.log(Level.WARNING, "No registered project matches projectUri: " + projectUri
                    + " — request will be answered with an empty result, not another project's data.");
        }
        return context;
    }

    /**
     * Resolves a {@link ProjectContext} preferring an explicit document URI/path when present, and
     * falling back to an explicit project root URI.
     *
     * <p>When a document URI is supplied but matches no project, this deliberately does <i>not</i>
     * retry with {@code projectUri}: a document that belongs to no open project is a different
     * condition from one that was never named, and silently widening the search is how a request ends
     * up answered by a project that does not own the document.
     *
     * @return the resolved project, or {@code null} if neither field identifies one
     */
    private ProjectContext resolveByUriOrProjectUri(String documentUri, String projectUri) {
        if (StringUtils.isNotBlank(documentUri)) {
            return resolveByUri(documentUri);
        }
        return resolveByProjectUri(projectUri);
    }

    /**
     * The {@link #resolveByPath} counterpart of {@link #resolveByUriOrProjectUri}: prefers a
     * filesystem path field when the request carries one, and falls back to an explicit project root.
     *
     * <p>Use this for requests whose path field is <em>legitimately optional</em> — a path that is
     * blank by design, not by omission. Routing such a request on the path alone resolves it to no
     * project exactly in the case it was meant to serve.
     *
     * @return the resolved project, or {@code null} if neither field identifies one
     */
    private ProjectContext resolveByPathOrProjectUri(String filePath, String projectUri) {
        if (StringUtils.isNotBlank(filePath)) {
            return resolveByPath(filePath);
        }
        return resolveByProjectUri(projectUri);
    }

    /**
     * Resolves the single, process-global {@link TryOutManager} for {@code ctx}, (re)binding it to
     * {@code ctx}'s project when it currently points elsewhere.
     *
     * <p>The underlying MI server process is a single-port, single-instance resource (see the multi
     * project execution plan's Phase 4), so binding a second project means taking that resource over:
     * the currently bound manager is shut down first — stopping its MI server even when a try-out is
     * running on it — and the requesting project gets a freshly launched one. Trying out a mediator in
     * a second project therefore always proceeds; it never fails with "another project is already
     * active", which left the user with no way forward but to hunt down the other project's panel.
     *
     * @param requestServerPath the initiating project's configured MI server path (may be blank/null);
     *                           used instead of the process-global {@link #miServerPath} when this call
     *                           is what creates a new {@link TryOutManager}, so the single shared server
     *                           launches the runtime the *initiating* project expects
     * <p>There is no manager to hand back when {@code ctx} is {@code null}: without a project there is
     * no runtime version, connector set or {@code deployment/libs} to launch against. Callers surface
     * that via {@link #tryOutUnavailableMessage()}.
     *
     * @return the {@link TryOutManager} bound to {@code ctx}'s project, or {@code null} if {@code ctx}
     *         is {@code null} — the caller should then surface {@link #tryOutUnavailableMessage()}
     */
    private TryOutManager bindTryOutManager(ProjectContext ctx, String requestServerPath) {
        if (ctx == null) {
            return null;
        }
        synchronized (tryOutBindLock) {
            if (tryOutManager != null && ctx.getProjectUri().equals(tryOutManager.getProjectUri())) {
                return tryOutManager;
            }
            if (tryOutManager != null) {
                // Take the shared server over from the project that currently holds it. shutdown() blocks
                // until the MI port is actually free, so the manager created below can bind it right away.
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
     * Explains why {@link #bindTryOutManager} declined. Its only remaining cause is an unresolvable
     * project — another project holding the shared server is taken over rather than refused.
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

        ProjectContext ctx = resolveByProjectUri(dbConnectionTestParams.projectUri);
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
        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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
            // TODO(unrouted-request): with a blank fileName this request identifies no project, so the
            // diagnostics participants resolve no ProjectContext and validate without connector or
            // dependent-artifact knowledge. That is correct-but-degraded rather than wrong; the fix is
            // for the agent/copilot caller to send an explicit projectUri, since it always knows which
            // project it is generating for. Until then, prefer sending fileName.
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
     * Lists the artifacts of one project — its own plus those its {@code .car} dependencies
     * contribute — for the key dropdowns in the property panels.
     *
     * <p>Routing must land on the requesting project's {@link ProjectContext}, because the dependent
     * artifacts live in that context's {@code ResourceFinder} and nowhere else. Both the explicit
     * {@code projectUri} and the originating document are honoured so a client that supplies either
     * one is routed correctly; a request identifying no project returns an empty
     * {@link ResourceResponse} rather than another project's artifact list.
     *
     * <p>{@code customProjectUri} is the debug-flow override: the debugger asks for a project that
     * may not be open in the workspace at all, so it names the directory to scan directly. It is a
     * project root path, hence {@link #resolveByProjectUri} rather than {@link #resolveByUri}.
     *
     * <p>Except for that override and the legacy {@code projectPath} field, the scanned directory is
     * taken from the resolved context, so the directory walked and the dependency map merged into the
     * result always belong to the same project.
     */
    @Override
    public CompletableFuture<ResourceResponse> availableResources(ResourceParam param) {

        ProjectContext ctx = StringUtils.isNotBlank(param.customProjectUri)
                ? resolveByProjectUri(param.customProjectUri)
                : resolveByUriOrProjectUri(param.getDocumentUri(), param.projectUri);
        String effectivePath = StringUtils.isNotBlank(param.projectPath) ? param.projectPath
                : StringUtils.isNotBlank(param.customProjectUri) ? param.customProjectUri
                : ctx != null ? ctx.getProjectUri() : null;
        ResourceResponse response;
        if (ctx == null) {
            response = new ResourceResponse();
        } else if (StringUtils.isNotBlank(param.dataServiceName)) {
            response = ctx.getResourceFinder().getDataServiceOperations(effectivePath, param.dataServiceName);
        } else {
            response = ctx.getResourceFinder().getAvailableResources(effectivePath, param.resourceType);
        }
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
            ProjectContext ctx = resolveByProjectUri(request.projectUri);
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
            ProjectContext ctx = resolveByProjectUri(request.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        List<String> resourceFiles = ctx != null
                ? ResourceFileScanner.scanResourceFiles(ctx.getProjectUri()) : Collections.emptyList();
        return CompletableFuture.supplyAsync(() -> resourceFiles);
    }

    @Override
    public CompletableFuture<List<ConfigurableEntry>> getConfigurableEntries(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
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

        ProjectContext ctx = resolveByUriOrProjectUri(param.documentPath, param.projectUri);
        return CompletableFuture.supplyAsync(() -> ctx != null
                && ctx.getInboundConnectorHolder().saveInboundConnector(param.connectorName, param.uiSchema));
    }

    @Override
    public CompletableFuture<InboundConnectorResponse> getInboundConnectorSchema(InboundConnectorParam param) {

        // documentPath is a filesystem path — the handler below does new File(param.documentPath) —
        // but it is only sent when an *existing* inbound endpoint is being edited. Creating a new
        // event integration sends connectorId alone, so routing on documentPath alone resolved every
        // "pick a connector" click to no project: the handler returned null and the form silently
        // stayed on the connector list. Fall back to the project the caller named.
        ProjectContext ctx = resolveByPathOrProjectUri(param.documentPath, param.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getInboundConnectorHolder().getLocalInboundConnectorList() : new JsonObject());
    }

    @Override
    public CompletableFuture<JsonObject> getConnectionUISchema(ConnectionUIParam param) {

        ProjectContext ctx = resolveByUriOrProjectUri(param.getDocumentUri(), param.getProjectUri());
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
        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        OverviewModel overviewModel = ctx != null
                ? OverviewModelGenerator.getOverviewModel(ctx.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> overviewModel);
    }

    @Override
    public CompletableFuture<CheckDBDriverResponseParams> checkDBDriver(CheckDBDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams.projectUri);
        CheckDBDriverResponseParams response = QueryGenerator.isDriverAvailableInClassPath(requestParams.className,
                ctx != null ? ctx.getProjectUri() : null);
        return CompletableFuture.supplyAsync(() -> response);
    }

    // The DB-driver group below mutates a project's driver classpath, so the project it names must be
    // resolved before it is used, never passed through raw: DynamicClassLoader keys its registry by
    // whatever string it is handed, so an unresolvable projectUri would silently create and mutate a
    // phantom entry instead of failing. Resolving first turns that into an honest false.
    @Override
    public CompletableFuture<Boolean> addDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams.projectUri);
        if (ctx == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        boolean isSuccess = QueryGenerator.addDriverToClassPath(requestParams.addDriverPath, requestParams.className,
                ctx.getProjectUri());
        return CompletableFuture.supplyAsync(() -> isSuccess);
    }

    @Override
    public CompletableFuture<Boolean> removeDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams.projectUri);
        if (ctx == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        boolean response = QueryGenerator.removeDriverFromClassPath(requestParams.removeDriverPath,
                ctx.getProjectUri());
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<Boolean> modifyDBDriver(ModifyDriverRequestParams requestParams) {
        ProjectContext ctx = resolveByProjectUri(requestParams.projectUri);
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

        ProjectContext ctx = resolveByPath(request.getFile());
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

        // Only tear down the shared TryOutManager if it's still bound to the requesting project (or the
        // request carries no project, for older clients) - otherwise an unrelated project's shutdown call
        // (e.g. before its own build/run) would kill another project's active try-out session.
        //
        // The ownership check compares project roots through WorkspaceManager.isSameProjectPath rather
        // than String.equals: the client sends WorkspaceFolder.uri.fsPath while the manager holds the
        // context's own projectUri, and a difference in format or drive-letter case between two spellings
        // of the same folder would otherwise read as "a different project" — declining the shutdown and
        // leaking the MI server process. It deliberately does not require the project to still be
        // registered, so a folder removed from the workspace can still shut its own try-out down.
        return CompletableFuture.supplyAsync(() -> {
            // Same lock as bindTryOutManager: without it this can shut down a manager another project
            // has just bound, or read a half-published one.
            synchronized (tryOutBindLock) {
                if (tryOutManager == null) {
                    return true;
                }
                String requestProjectUri = request != null ? request.getProjectUri() : null;
                if (StringUtils.isNotBlank(requestProjectUri)
                        && !WorkspaceManager.isSameProjectPath(requestProjectUri, tryOutManager.getProjectUri())) {
                    return true;
                }
                return tryOutManager.shutdown();
            }
        });
    }

    @Override
    public CompletableFuture<MediatorTryoutInfo> mediatorInputOutputSchema(MediatorTryoutRequest request) {

        // Schema generation here is a lightweight, stateless read (no shared MI server involved), so it
        // is served directly from the resolved project rather than going through the single rebindable
        // TryOutManager — it should never be blocked by another project's active try-out session.
        ProjectContext ctx = resolveByPath(request.getFile());
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? new ServerLessTryoutHandler(ctx.getProjectUri(), ctx.getConnectorHolder()).handle(request)
                : new MediatorTryoutInfo("Project is not initialized"));
    }

    @Override
    public CompletableFuture<TestConnectionResponse> testConnectorConnection(TestConnectionRequest request) {

        ProjectContext ctx = resolveByProjectUri(request.getProjectUri());
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
        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        OverviewPageDetailsResponse response = ctx != null
                ? OverviewPage.getDetails(ctx.getProjectUri()) : null;
        return CompletableFuture.supplyAsync(() -> response);
    }

    // TODO(unrouted-request): expressionCompletion and signatureHelp carry a documentUri but never
    // resolve a ProjectContext from it — the providers below read the file directly and derive what
    // they need from the path. They therefore ignore per-project connector and dependency state, so a
    // connector operation available in one open project is offered in all of them. Route these through
    // resolveByPath and pass the context to the providers.
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
        ProjectContext ctx = resolveByProjectUri(request.projectUri);
        UpdateResponse response = ctx != null
                ? PomParser.updateProperty(ctx.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<UpdateResponse> updateDependency(UpdateDependencyRequest request) {
        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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
        ProjectContext ctx = resolveByProjectUri(request.projectUri);
        UpdateResponse response = ctx != null
                ? ConfigParser.updateConfigFile(ctx.getProjectUri(), request) : new UpdateResponse();
        return CompletableFuture.supplyAsync(() -> response);
    }

    @Override
    public CompletableFuture<String> updateConnectorDependencies(ProjectUriRequest request) {
        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
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

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        if (ctx == null) {
            return CompletableFuture.supplyAsync(() -> "Project is not initialized");
        }
        String projectUri = ctx.getProjectUri();
        log.info("Refetching integration project dependencies for project: " + projectUri);
        return CompletableFuture.supplyAsync(() -> DependencyDownloadManager.refetchIntegrationProjectDependencies(projectUri));
    }

    @Override
    public CompletableFuture<DependencyStatusResponse> getDependencyStatusList(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? DependencyDownloadManager.getDependencyStatusList(ctx.getProjectUri()) : null);
    }

    @Override
    public CompletableFuture<ConnectorDependencyResponse> getConnectorDependencies(
            ConnectorDependencyRequest request) {

        ProjectContext ctx = resolveByProjectUri(request.projectUri);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ConnectorConfigService.buildDependencyResponse(ctx.getProjectUri(),
                        request.connectorArtifactId, ctx.getConnectorHolder())
                : null);
    }

    @Override
    public CompletableFuture<Boolean> updateConnectorDependencyOverride(
            UpdateConnectorDependencyRequest request) {

        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request.projectUri);
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

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        if (ctx != null) {
            ConnectorConfigService.initIfAbsent(ctx.getProjectUri());
        }
    }

    @Override
    public CompletableFuture<LoadDependentResourcesResponse> loadDependentResources(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
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
            ProjectContext ctx = resolveByProjectUri(connectorGenReq.projectUri);
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

    // TODO(unrouted-request): getDynamicFields, getStoredProcedures and fetchTables are served by one
    // process-global dynamicFieldsHandler shared by every open project, so neither its state nor its
    // caches are per-project. Give each ProjectContext its own handler, or key this one by project.
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

        ProjectContext ctx = resolveByProjectUri(request.getProjectUri());
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
        // filePath is the JDBC driver's path on disk, taken from a connection parameter — and it is
        // blank whenever the driver has not been downloaded yet, which is the main reason to ask for
        // the coordinates at all (ConnectorDownloadManager then reads them from the connector's
        // descriptor.yml instead). Routing on it alone therefore resolved the first-use case, where a
        // connection carries neither a driverPath nor stored coordinates, to no project: the handler
        // returned null and the caller's whole connection-validation step failed, leaving the DB
        // operation form's table and query fields empty. Fall back to the project the caller named.
        ProjectContext ctx = resolveByPathOrProjectUri(request.getFilePath(), request.getProjectUri());
        return CompletableFuture.supplyAsync(() -> ctx != null ? ConnectorDownloadManager.getDriverMavenCoordinates(
                request.getFilePath(),
                request.getConnectorName(),
                request.getConnectionType(),
                ctx.getConnectorHolder()
        ) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> updateMavenDeployPlugin(DeployPluginDetails pluginDetails) {

        ProjectContext ctx = resolveByProjectUri(pluginDetails.getProjectUri());
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.addCarDeployPluginToPom(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE), pluginDetails) : null);
    }

    @Override
    public CompletableFuture<DeployPluginDetails> getMavenDeployPluginDetails(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.extractCarDeployPluginFields(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<TextEdit> removeMavenDeployPlugin(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null ? PomParser.removeDeployPlugin(
                new File(ctx.getProjectUri() + File.separator + Constants.POM_FILE)) : null);
    }

    @Override
    public CompletableFuture<List<ConfigDetails>> getConfigurableList(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ConfigParser.getConfigDetails(ctx.getProjectUri()) : Collections.emptyList());
    }

    @Override
    public CompletableFuture<String> getLocalInboundEndpointsListForCopilot(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
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
            ProjectContext ctx = resolveByProjectUri(request.projectUri);
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
    public CompletableFuture<String> fetchInboundConnectors(ProjectUriRequest request) {

        ProjectContext ctx = resolveByProjectUri(request != null ? request.projectUri : null);
        return CompletableFuture.supplyAsync(() -> ctx != null
                ? ctx.getInboundConnectorHolder().getCustomInboundConnectors() : null);
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
