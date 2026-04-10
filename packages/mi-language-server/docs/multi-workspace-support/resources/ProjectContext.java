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
import org.eclipse.lemminx.customservice.synapse.expression.ExpressionHelperProvider;
import org.eclipse.lemminx.customservice.synapse.inbound.conector.InboundConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.mediatorService.MediatorHandler;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.AbstractResourceFinder;
import org.eclipse.lemminx.customservice.synapse.resourceFinder.ResourceFinderFactory;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.nio.file.Path;
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
     */
    private boolean initialized = false;

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
        this.connectorHolder = ConnectorHolder.getInstance();
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
     *   <li>Resolves and stores the {@code synapseXsdPath}</li>
     * </ol>
     *
     * @param miServerPath   absolute path to the local MI server installation
     * @param languageClient the language-client proxy for sending notifications
     *                       back to the IDE
     * @throws Exception if any step in the initialization pipeline fails
     */
    public void initProject(String miServerPath, SynapseLanguageClientAPI languageClient) throws Exception {

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
        this.expressionHelperProvider = new ExpressionHelperProvider(projectUri);

        // 6. Create and load the resource finder.
        this.resourceFinder = ResourceFinderFactory.getResourceFinder(isLegacyProject);
        try {
            resourceFinder.loadDependentResources(projectUri);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to initialize ProjectContext for: " + projectUri + ". Error: " + e.getMessage());
        }

        // 7. Resolve the synapse XSD path for this project's MI version.
        this.synapseXsdPath = Utils.copyXSDFiles(projectUri);

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
