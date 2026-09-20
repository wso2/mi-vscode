/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.eclipse.lemminx.customservice.synapse;

import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.AbstractConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionHandler;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.NewProjectConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.connectors.OldProjectConnectorLoader;
import org.eclipse.lemminx.customservice.synapse.connectors.SchemaGenerate;
import org.eclipse.lemminx.customservice.synapse.dataService.DynamicClassLoader;
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionHelperProvider;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFinderFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.mediators.MediatorFactoryFinder;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds all per-project state required by the MI Language Server for a single
 * workspace folder/project. In a Multi-Root Workspace scenario, one instance
 * of {@code ProjectContext} is created per open project root, and all language
 * features (completions, hover, validation, connectors, etc.) are resolved
 * through the context that corresponds to the document being processed.
 *
 * <p>Instances are stored in a map keyed by the project root URI inside the
 * language server (e.g. {@code SynapseLanguageClientAPI} or its successor
 * manager class) so that requests for any document are dispatched to the
 * correct context.
 *
 * <p>Immutable identity fields (projectUri, isLegacyProject, projectServerVersion)
 * are set in the constructor and cannot be changed afterwards. All service
 * handler fields are eagerly initialized via the {@link #initProject} method,
 * which must be called immediately after construction. No setters are provided
 * — Early Initialization ensures everything is ready before the context is
 * used, eliminating loading delays during coding.
 *
 * <p><b>Note:</b> The {@code TryOutManager} is intentionally excluded from
 * this class. It manages a heavy background MI Server process that binds to
 * a specific network port, so only one instance can run at a time across all
 * projects. It remains a separate global concern managed by
 * {@code SynapseLanguageService}.
 */
public class ProjectContext {

    private static final Logger log = Logger.getLogger(ProjectContext.class.getName());

    /**
     * Tracks whether {@link #initProject} has completed successfully.
     * Used by service-handler getters to fail fast with a clear message
     * instead of returning {@code null}.
     *
     * <p><b>{@code volatile} on purpose — this flag is also this object's publication fence.</b>
     * {@code XMLLanguageServer.addProjectContext} registers a context in the shared
     * {@code WorkspaceManager} map <em>before</em> initializing it, and {@code didChangeWorkspaceFolders}
     * does so on the notification thread while request threads are already being served. So another
     * thread can be holding this reference while the fields below are still being written.
     * {@link java.util.concurrent.ConcurrentHashMap} only orders writes made <em>before</em> the
     * {@code put}; it says nothing about the ones {@link #initProject} makes afterwards. Reading this
     * volatile flag is what gives a reader the happens-before edge, so a thread that sees {@code true}
     * is guaranteed to see every field below fully written.
     *
     * <p>Two rules keep that guarantee, and both must hold together:
     * <ul>
     *   <li>the field stays {@code volatile} — otherwise a reader could observe {@code true} next to a
     *       still-null handler and get an NPE instead of the {@link IllegalStateException}
     *       {@link #checkInitialized} promises;</li>
     *   <li>the write stays the <em>last</em> statement of {@link #initProject} — never move it earlier
     *       to let some later init step past {@link #checkInitialized}. Call the unguarded internal
     *       helper from that step instead, the way {@link #loadConnectors} exists for
     *       {@link #updateConnectors}.</li>
     * </ul>
     */
    private volatile boolean initialized = false;

    // -------------------------------------------------------------------------
    // Identity fields — set once at construction time and never changed.
    // -------------------------------------------------------------------------

    /**
     * The root folder URI of this project (e.g. {@code file:///Users/.../ProjectA}).
     * Used as the primary key when looking up the context for a given document URI.
     */
    private final String projectUri;

    /**
     * Whether this project is a <em>legacy</em> (state-machine-based) MI project.
     * Legacy projects use a different activation and completion pathway compared
     * to modern MI projects.
     */
    private final boolean isLegacyProject;

    /**
     * The WSO2 MI version string associated with this project
     * (e.g. {@code "4.3.0"}, {@code "4.4.0"}). Used to select the correct
     * XSD schemas, mediator descriptors, and feature toggles.
     */
    private final String projectServerVersion;

    // -------------------------------------------------------------------------
    // Schema field — resolved during initProject().
    // -------------------------------------------------------------------------

    /**
     * Path to the extracted root {@code synapse_config.xsd} for this specific
     * project. Resolved during {@link #initProject} by extracting the
     * version-specific XSD bundle for this project.
     */
    private Path synapseXsdPath;

    // -------------------------------------------------------------------------
    // Connector fields — eagerly initialized in the constructor.
    // -------------------------------------------------------------------------

    /**
     * Holds metadata and descriptors for all regular (outbound) connectors
     * discovered for this project. Initialized eagerly so that connector
     * scanning can populate it immediately after construction.
     */
    private final ConnectorHolder connectorHolder;

    /**
     * Holds metadata and descriptors for all inbound connectors discovered
     * for this project. Initialized eagerly alongside {@link #connectorHolder}.
     */
    private final InboundConnectorHolder inboundConnectorHolder;

    // -------------------------------------------------------------------------
    // Service handler fields — eagerly initialized via initProject().
    // -------------------------------------------------------------------------

    /**
     * Responsible for loading and refreshing connectors from the project's
     * connector directory. The concrete type (Old vs New) depends on
     * {@link #isLegacyProject}.
     */
    private AbstractConnectorLoader connectorLoader;

    /**
     * Handles completion proposals and hover information for Synapse mediators
     * within this project. Depends on {@link #projectServerVersion} to load
     * the correct mediator descriptor set.
     */
    private MediatorHandler mediatorHandler;

    /**
     * Provides completion and documentation support for {@code ${}} expression
     * syntax (e.g. payload-factory, data-mapper expressions) within this project.
     */
    private ExpressionHelperProvider expressionHelperProvider;

    /**
     * Manages named connection artifacts (e.g. connector local-entries) for
     * this project, enabling connection-aware completions and validations.
     */
    private ConnectionHandler connectionHandler;

    /**
     * Locates and resolves project-internal resources (endpoints, sequences,
     * message-stores, etc.) referenced by documents in this project.
     */
    private AbstractResourceFinder resourceFinder;

    /**
     * Finds the correct mediator factory for a given syntax-tree node, scoped to this
     * project's MI version and connector holder.
     */
    private MediatorFactoryFinder mediatorFactory;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code ProjectContext} for the given project root.
     *
     * <p>The {@link ConnectorHolder} and {@link InboundConnectorHolder} are
     * created eagerly so that connector-scanning routines can start populating
     * them immediately. All other service handlers remain {@code null} until
     * {@link #initProject} is called.
     *
     * @param projectUri           root folder URI of the project
     *                             (e.g. {@code "file:///Users/.../ProjectA"})
     * @param isLegacyProject      {@code true} if this is a state-machine based
     *                             (legacy) MI project
     * @param projectServerVersion the MI version string for this project
     *                             (e.g. {@code "4.3.0"})
     */
    public ProjectContext(String projectUri, boolean isLegacyProject, String projectServerVersion) {

        this.projectUri = projectUri;
        this.isLegacyProject = isLegacyProject;
        this.projectServerVersion = projectServerVersion;

        // Eagerly initialize connector holders so scanning can begin immediately.
        this.connectorHolder = new ConnectorHolder();
        this.inboundConnectorHolder = new InboundConnectorHolder();
    }

    // -------------------------------------------------------------------------
    // Early Initialization
    // -------------------------------------------------------------------------

    /**
     * Eagerly initializes all service handlers for this project in the correct
     * dependency order, mirroring the initialization sequence from
     * {@code SynapseLanguageService.init(...)}.
     *
     * <p>This method must be called exactly once, immediately after construction,
     * before the context is registered for use. After this call returns
     * successfully, every getter is guaranteed to return a non-null, fully
     * initialized instance — eliminating any lazy-loading delays during coding.
     *
     * <p><b>Initialization order:</b>
     * <ol>
     *   <li>{@link InboundConnectorHolder#init} — loads inbound connector metadata</li>
     *   <li>{@link AbstractConnectorLoader} — instantiates the correct loader
     *       (Old vs New) and calls {@code init(projectUri)}</li>
     *   <li>{@link MediatorHandler} — loads mediator descriptors for this version</li>
     *   <li>{@link ConnectionHandler} — indexes named connections</li>
     *   <li>{@link ExpressionHelperProvider} — prepares expression helpers</li>
     *   <li>{@link AbstractResourceFinder} — discovers and indexes dependent
     *       resources (endpoints, sequences, etc.)</li>
     *   <li>{@link MediatorFactoryFinder} — builds the per-context factory finder</li>
     *   <li>Resolves and stores the {@code synapseXsdPath}</li>
     *   <li>{@link DynamicClassLoader} — seeds this project's DB-driver classloader
     *       from its own {@code deployment/libs}</li>
     *   <li>{@link #loadConnectors} and {@link #packHttpConnector} — loads this project's
     *       connectors and regenerates its {@code connectors.xsd}</li>
     *   <li>Sets {@link #initialized}, publishing the finished context to other threads</li>
     * </ol>
     *
     * <p>Only the last step makes this context usable: until it runs, and it runs only if no earlier
     * step threw, {@link #checkInitialized} rejects every service-handler getter. The context is
     * already registered in {@code WorkspaceManager} by then (see
     * {@code XMLLanguageServer.addProjectContext}), so other threads can hold a reference to it
     * throughout — which is why that final write is what carries the whole object across to them.
     *
     * @param miServerPath   absolute path to the local MI server installation
     * @param languageClient the language-client proxy for sending notifications
     *                       back to the IDE
     * @throws Exception if any step in the initialization pipeline fails
     */
    public void initProject(String miServerPath, SynapseLanguageClientAPI languageClient) throws Exception {
        initProject(miServerPath, languageClient, null);
    }

    /**
     * Same as {@link #initProject(String, SynapseLanguageClientAPI)}, but lets the caller supply the
     * schema directory this project's documents were already registered against (e.g. via LemMinX file
     * associations or the XML catalog), so this context's generated {@code connectors.xsd} lands in the
     * SAME directory the validation engine actually reads — not an independently re-extracted copy.
     *
     * @param miServerPath   absolute path to the local MI server installation
     * @param languageClient the language-client proxy for sending notifications back to the IDE
     * @param synapseXsdPath the schema directory already resolved for this project's document
     *                       associations, or {@code null} to let this context extract its own (e.g. for
     *                       callers that never registered one, such as tests)
     * @throws Exception if any step in the initialization pipeline fails
     */
    public void initProject(String miServerPath, SynapseLanguageClientAPI languageClient, Path synapseXsdPath)
            throws Exception {

        log.log(Level.INFO, "Initializing ProjectContext for: " + projectUri);

        // 1. Initialize inbound connector metadata.
        inboundConnectorHolder.init(projectUri, projectServerVersion);

        // 2. Instantiate the correct connector loader based on project type.
        if (isLegacyProject) {
            this.connectorLoader = new OldProjectConnectorLoader(languageClient, connectorHolder);
        } else {
            this.connectorLoader = new NewProjectConnectorLoader(languageClient, connectorHolder,
                    inboundConnectorHolder);
        }
        connectorLoader.init(projectUri);

        // 3. Initialize the mediator handler with version-specific descriptors.
        this.mediatorHandler = new MediatorHandler();
        mediatorHandler.init(projectUri, projectServerVersion, connectorHolder);

        // 4. Initialize the connection handler.
        this.connectionHandler = new ConnectionHandler();
        connectionHandler.init(connectorHolder);

        // 5. Create the expression helper provider.
        this.expressionHelperProvider = new ExpressionHelperProvider(projectUri, connectorHolder);

        // 6. Create and load the resource finder.
        this.resourceFinder = ResourceFinderFactory.getResourceFinder(isLegacyProject, connectorHolder);
        try {
            resourceFinder.loadDependentResources(projectUri);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to initialize ProjectContext for: " + projectUri + ". Error: " + e.getMessage());
        }

        // 7. Build the per-context mediator factory finder.
        this.mediatorFactory = new MediatorFactoryFinder(projectServerVersion, projectUri, connectorHolder);

        // 8. Resolve the synapse XSD path for this project's MI version — reuse the caller-supplied
        // directory when given, so generated schemas land where the validation engine already looks.
        this.synapseXsdPath = synapseXsdPath != null ? synapseXsdPath : Utils.copyXSDFiles(projectUri);

        // 9. Seed this project's DB-driver classloader from its own deployment/libs, so drivers already on
        // disk stay visible to checkDBDriver/testDBConnection.
        try {
            DynamicClassLoader.updateClassLoader(projectUri, Path.of(projectUri, "deployment", "libs").toFile());
        } catch (Exception e) {
            log.log(Level.WARNING,
                    "Could not seed the DB-driver classloader from deployment/libs for: " + projectUri, e);
        }

        // 10. Load this project's connectors now that the loader and XSD path are ready, and pack the
        // bundled HTTP connector in if this project's MI version needs it. Both go through the
        // unguarded loadConnectors() rather than the public updateConnectors(), because `initialized`
        // is deliberately still false here — see step 11.
        loadConnectors();
        packHttpConnector();

        // 11. Publish. This is the last statement on purpose: `initialized == true` is what tells every
        // other thread this context is both fully wired and fully loaded, so it must not become true
        // while any work remains — not even the connector loading above, which would otherwise hand a
        // concurrent reader a context whose ConnectorHolder is still filling and whose connector calls
        // therefore parse as InvalidMediator. See the field's javadoc for the visibility half.
        this.initialized = true;

        log.log(Level.INFO, "ProjectContext initialized successfully for: " + projectUri);
    }

    // -------------------------------------------------------------------------
    // Getters — identity fields
    // -------------------------------------------------------------------------

    /**
     * Returns the root folder URI of this project.
     *
     * @return the project root URI (never {@code null})
     */
    public String getProjectUri() {
        return projectUri;
    }

    /**
     * Returns whether this is a legacy (state-machine-based) MI project.
     *
     * @return {@code true} for legacy projects
     */
    public boolean isLegacyProject() {
        return isLegacyProject;
    }

    /**
     * Returns the WSO2 MI version string associated with this project.
     *
     * @return the project server version (e.g. {@code "4.3.0"})
     */
    public String getProjectServerVersion() {
        return projectServerVersion;
    }

    /**
     * Whether {@link #initProject} has run to completion for this context, i.e. whether the service
     * handlers below are safe to use.
     *
     * <p>Being registered in {@code WorkspaceManager} does <em>not</em> imply this. Registration and
     * initialization are deliberately separate (see {@code XMLLanguageServer.addProjectContext}), so a
     * context resolved from the registry may be one that is still initializing on another thread, or
     * one whose initialization threw. In both cases every getter below throws
     * {@link IllegalStateException}.
     *
     * <p>Check this before touching a service handler anywhere an exception would be swallowed or
     * would abandon unrelated work — LSP notification handlers, batch loops, background refreshes —
     * and skip that project instead. Request handlers that resolve a project per call can rely on the
     * exception surfacing as an error response.
     *
     * @return {@code true} once initialization has fully succeeded
     */
    public boolean isInitialized() {
        return initialized;
    }

    // -------------------------------------------------------------------------
    // Getter — synapseXsdPath (no setter; resolved in initProject())
    // -------------------------------------------------------------------------

    /**
     * Returns the path to the extracted root {@code synapse_config.xsd} for
     * this project.
     *
     * @return the XSD path (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public Path getSynapseXsdPath() {
        checkInitialized();
        return synapseXsdPath;
    }

    // -------------------------------------------------------------------------
    // Getters — connector holders (no setters; initialized in constructor)
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link ConnectorHolder} for this project's regular (outbound)
     * connectors.
     *
     * @return the connector holder (never {@code null})
     */
    public ConnectorHolder getConnectorHolder() {
        return connectorHolder;
    }

    /**
     * Returns the {@link InboundConnectorHolder} for this project's inbound
     * connectors.
     *
     * @return the inbound connector holder (never {@code null})
     */
    public InboundConnectorHolder getInboundConnectorHolder() {
        return inboundConnectorHolder;
    }

    // -------------------------------------------------------------------------
    // Getters — service handlers (no setters; initialized in initProject())
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link AbstractConnectorLoader} responsible for loading
     * connectors for this project.
     *
     * @return the connector loader (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public AbstractConnectorLoader getConnectorLoader() {
        checkInitialized();
        return connectorLoader;
    }

    /**
     * Returns the {@link MediatorHandler} that provides completion and hover
     * support for Synapse mediators in this project.
     *
     * @return the mediator handler (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public MediatorHandler getMediatorHandler() {
        checkInitialized();
        return mediatorHandler;
    }

    /**
     * Returns the {@link ExpressionHelperProvider} that handles {@code ${}}
     * expression completions and documentation for this project.
     *
     * @return the expression helper provider (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public ExpressionHelperProvider getExpressionHelperProvider() {
        checkInitialized();
        return expressionHelperProvider;
    }

    /**
     * Returns the {@link ConnectionHandler} that manages named connections for
     * this project.
     *
     * @return the connection handler (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public ConnectionHandler getConnectionHandler() {
        checkInitialized();
        return connectionHandler;
    }

    /**
     * Returns the {@link AbstractResourceFinder} that locates project-internal
     * resources (endpoints, sequences, etc.) for this project.
     *
     * @return the resource finder (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public AbstractResourceFinder getResourceFinder() {
        checkInitialized();
        return resourceFinder;
    }

    /**
     * Returns the {@link MediatorFactoryFinder} scoped to this project's MI version and
     * connector holder.
     *
     * @return the mediator factory finder (non-null after {@link #initProject})
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public MediatorFactoryFinder getMediatorFactory() {
        checkInitialized();
        return mediatorFactory;
    }

    // -------------------------------------------------------------------------
    // Connector refresh — invoked when this project's connector/inbound .zip
    // files change on disk.
    // -------------------------------------------------------------------------

    /**
     * Reloads this project's outbound connectors from disk, refreshes its mediator
     * descriptor list, and regenerates {@code connectors.xsd} into this project's own
     * {@link #synapseXsdPath}. Scoped entirely to this context's {@link #connectorHolder},
     * so it never affects any other open project.
     *
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public void updateConnectors() {
        checkInitialized();
        loadConnectors();
    }

    /**
     * The body of {@link #updateConnectors} without the readiness guard.
     *
     * <p>Exists so {@link #initProject} can run the initial connector load as part of bringing this
     * context up, at which point {@code initialized} is deliberately still {@code false} and
     * {@link #checkInitialized} would reject the call. Depends only on {@link #connectorLoader},
     * {@link #mediatorHandler}, {@link #synapseXsdPath} and {@link #connectorHolder}, all of which are
     * already assigned by then.
     *
     * <p>Callers outside {@link #initProject} must use {@link #updateConnectors} instead, so that a
     * context which never finished initializing fails with a clear {@link IllegalStateException}
     * rather than an NPE somewhere in the loader.
     */
    private void loadConnectors() {
        connectorLoader.loadConnector();
        if (mediatorHandler.isInitialized()) {
            mediatorHandler.reloadMediatorList(projectServerVersion);
        }
        String connectorPath = synapseXsdPath.resolve("mediators").resolve("connectors.xsd").toString();
        SchemaGenerate.generate(connectorHolder, connectorPath);
    }

    /**
     * Reloads this project's inbound connectors from disk.
     *
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public void updateInboundConnectors() {
        checkInitialized();
        inboundConnectorHolder.getCustomInboundConnectors();
    }

    /**
     * Packs the bundled HTTP connector into this project's connector download directory, if this
     * project's MI version needs it and it isn't already there. Mirrors the previous single-project
     * bootstrap so every registered project (not just the first one) gets the built-in HTTP connector.
     */
    private void packHttpConnector() {

        if (Utils.compareVersions(projectServerVersion, Constant.MI_440_VERSION) < 0
                || !Utils.hasDependency(projectUri, Constant.HTTP_CONNECTOR_ARTIFACT_ID)) {
            return;
        }
        String projectId = new File(projectUri).getName() + "_" + Utils.getHash(projectUri);
        String connectorDownloadPath = Path.of(System.getProperty(Constant.USER_HOME), Constant.WSO2_MI,
                Constant.CONNECTORS, projectId, Constant.DOWNLOADED).toString();
        File connectorDownloadFolder = new File(connectorDownloadPath);
        if (!connectorDownloadFolder.exists()) {
            boolean isDirectoryCreationSuccessful = connectorDownloadFolder.mkdirs();
            if (!isDirectoryCreationSuccessful) {
                log.log(Level.SEVERE, "Error occurred while creating directory: " + connectorDownloadFolder);
            }
        } else {
            File[] matchingFiles = connectorDownloadFolder.listFiles((dir, name) ->
                    name.startsWith("mi-connector-http") && name.endsWith(".zip"));
            if (matchingFiles != null && matchingFiles.length > 0) {
                return;
            }
        }
        try {
            InputStream inputStream = ProjectContext.class.getResourceAsStream(
                    "/org/eclipse/lemminx/connectors/mi-connector-http-1.0.0.zip");
            if (inputStream == null) {
                throw new FileNotFoundException("HTTP connector not found.");
            }
            Path httpConnectorPath = Paths.get(connectorDownloadPath, "mi-connector-http-1.0.0.zip");
            Files.copy(inputStream, httpConnectorPath, StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            // Unguarded: this only runs from initProject, where `initialized` is still false.
            loadConnectors();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while packing the HTTP connector to the project. ", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Throws {@link IllegalStateException} if {@link #initProject} has not
     * been called yet. Guards service-handler getters so that callers get a
     * clear error message instead of a downstream {@code NullPointerException}.
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "ProjectContext not initialized. Call initProject() first. Project: " + projectUri);
        }
    }

    // -------------------------------------------------------------------------
    // Object overrides
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable representation of this context, primarily for
     * logging and debugging purposes.
     *
     * @return a string in the format {@code ProjectContext{uri=..., version=..., legacy=...}}
     */
    @Override
    public String toString() {
        return "ProjectContext{" +
                "projectUri='" + projectUri + '\'' +
                ", projectServerVersion='" + projectServerVersion + '\'' +
                ", isLegacyProject=" + isLegacyProject +
                '}';
    }
}
