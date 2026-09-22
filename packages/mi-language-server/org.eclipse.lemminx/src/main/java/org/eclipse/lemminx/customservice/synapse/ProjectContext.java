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
 * Holds all per-project state for the MI Language Server, with one instance per open project root keyed by
 * project URI, identity fields set at construction, and service handlers eagerly initialized via
 * {@link #initProject}; the {@code TryOutManager} is deliberately excluded since it manages a single shared
 * MI Server process and stays a global concern in {@code SynapseLanguageService}.
 */
public class ProjectContext {

    private static final Logger log = Logger.getLogger(ProjectContext.class.getName());

    /**
     * Tracks whether {@link #initProject} has completed, deliberately kept {@code volatile} so this flag
     * acts as this object's publication fence — it must stay volatile and be set only as
     * {@link #initProject}'s last statement, so a thread that observes it {@code true} is guaranteed to
     * see every field below fully written.
     */
    private volatile boolean initialized = false;

    // -------------------------------------------------------------------------
    // Identity fields — set once at construction time and never changed.
    // -------------------------------------------------------------------------

    /**
     * The root folder URI of this project (e.g. {@code file:///Users/.../ProjectA}), used as the primary
     * key when looking up its context.
     */
    private final String projectUri;

    /**
     * Whether this project is a legacy (state-machine-based) MI project, which uses a different
     * activation and completion pathway than modern MI projects.
     */
    private final boolean isLegacyProject;

    /**
     * The WSO2 MI version string for this project (e.g. {@code "4.4.0"}), used to select the correct XSD
     * schemas, mediator descriptors, and feature toggles.
     */
    private final String projectServerVersion;

    // -------------------------------------------------------------------------
    // Schema field — resolved during initProject().
    // -------------------------------------------------------------------------

    /**
     * Path to this project's extracted root {@code synapse_config.xsd}, resolved during {@link #initProject}.
     */
    private Path synapseXsdPath;

    // -------------------------------------------------------------------------
    // Connector fields — eagerly initialized in the constructor.
    // -------------------------------------------------------------------------

    /**
     * Holds metadata and descriptors for this project's regular (outbound) connectors, initialized eagerly
     * so scanning can populate it right after construction.
     */
    private final ConnectorHolder connectorHolder;

    /**
     * Holds metadata and descriptors for this project's inbound connectors, initialized eagerly alongside
     * {@link #connectorHolder}.
     */
    private final InboundConnectorHolder inboundConnectorHolder;

    // -------------------------------------------------------------------------
    // Service handler fields — eagerly initialized via initProject().
    // -------------------------------------------------------------------------

    /**
     * Loads and refreshes connectors from this project's connector directory, using a concrete type
     * (Old vs New) that depends on {@link #isLegacyProject}.
     */
    private AbstractConnectorLoader connectorLoader;

    /**
     * Handles mediator completion proposals and hover information for this project, loading the mediator
     * descriptor set for {@link #projectServerVersion}.
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
     * Creates a new {@code ProjectContext} for the given project root, eagerly creating its
     * {@link ConnectorHolder} and {@link InboundConnectorHolder} while leaving other service handlers
     * {@code null} until {@link #initProject} is called.
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
     * Eagerly initializes all service handlers for this project, in dependency order mirroring
     * {@code SynapseLanguageService.init(...)}, and must be called exactly once immediately after
     * construction so that every getter is guaranteed non-null once it returns successfully.
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
     * schema directory this project's documents are already registered against, so the generated
     * {@code connectors.xsd} lands where the validation engine actually reads instead of an
     * independently re-extracted copy.
     *
     * @param miServerPath   absolute path to the local MI server installation
     * @param languageClient the language-client proxy for sending notifications back to the IDE
     * @param synapseXsdPath the schema directory already resolved for this project's documents, or
     *                       {@code null} to let this context extract its own
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

        // 8. Resolve the synapse XSD path for this project's MI version, reusing the caller-supplied
        // directory when given.
        this.synapseXsdPath = synapseXsdPath != null ? synapseXsdPath : Utils.copyXSDFiles(projectUri);

        // 9. Seed this project's DB-driver classloader from its own deployment/libs.
        try {
            DynamicClassLoader.updateClassLoader(projectUri, Path.of(projectUri, "deployment", "libs").toFile());
        } catch (Exception e) {
            log.log(Level.WARNING,
                    "Could not seed the DB-driver classloader from deployment/libs for: " + projectUri, e);
        }

        // 10. Load this project's connectors and pack the bundled HTTP connector, both through the
        // unguarded loadConnectors() since `initialized` is deliberately still false here.
        loadConnectors();
        packHttpConnector();

        // 11. Publish last on purpose: `initialized == true` must not become visible until every field,
        // including the connector loading above, is fully written. See the field's javadoc.
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
     * Whether {@link #initProject} has run to completion for this context — being registered in
     * {@code WorkspaceManager} does not imply this, so callers on notification handlers, batch loops, or
     * background refreshes should check it and skip the project if false, while per-call request
     * handlers can rely on the resulting {@link IllegalStateException}.
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
     * Reloads this project's outbound connectors from disk, refreshes its mediator descriptor list, and
     * regenerates {@code connectors.xsd} into its own {@link #synapseXsdPath}, scoped entirely to this
     * context's {@link #connectorHolder} so other open projects are unaffected.
     *
     * @throws IllegalStateException if {@link #initProject} has not been called
     */
    public void updateConnectors() {
        checkInitialized();
        loadConnectors();
    }

    /**
     * The body of {@link #updateConnectors} without the readiness guard, so {@link #initProject} can run
     * the initial connector load while {@code initialized} is still {@code false}; callers outside
     * {@link #initProject} must use {@link #updateConnectors} instead.
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
        inboundConnectorHolder.getCustomInboundConnectors(null);
    }

    /**
     * Packs the bundled HTTP connector into this project's connector download directory if this
     * project's MI version needs it and it isn't already there, mirroring the previous single-project
     * bootstrap for every registered project.
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
     * Throws {@link IllegalStateException} if {@link #initProject} has not been called yet, so
     * service-handler getters fail with a clear message instead of a downstream {@code NullPointerException}.
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
