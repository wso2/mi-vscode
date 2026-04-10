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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry that maps open workspace folder URIs to their isolated
 * {@link ProjectContext} instances.
 *
 * <p>In a Multi-Root Workspace scenario, VS Code may have several MI projects
 * open simultaneously, each with its own MI version, connectors, and handlers.
 * {@code WorkspaceManager} is the single source of truth for resolving:
 * <pre>
 *   document URI  →  correct {@link ProjectContext}
 * </pre>
 *
 * <p>All operations are thread-safe; the underlying map is a
 * {@link ConcurrentHashMap} so concurrent LSP request threads can look up
 * contexts without external synchronization.
 *
 * <p><b>URI contract:</b> All URIs stored in and passed to this class must be
 * in normalized {@code file:///} format (e.g.
 * {@code file:///Users/me/ProjectA}). Callers are responsible for normalizing
 * URIs before invoking any method.
 */
public class WorkspaceManager {

    private static final Logger log = Logger.getLogger(WorkspaceManager.class.getName());

    /**
     * Map from project root URI to the {@link ProjectContext} for that project.
     * Uses {@link ConcurrentHashMap} for lock-free, thread-safe reads.
     *
     * <p>Key: normalized project root URI (e.g. {@code file:///Users/me/ProjectA})
     * <br>Value: the fully initialized {@link ProjectContext} for that root
     */
    private final Map<String, ProjectContext> projects = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Mutating operations
    // -------------------------------------------------------------------------

    /**
     * Registers a new {@link ProjectContext} for the given project root URI.
     *
     * <p>If a context is already registered for {@code projectUri}, a warning
     * is logged and the existing entry is <em>not</em> overwritten. Call
     * {@link #removeProject} first if you need to replace a context.
     *
     * @param projectUri the normalized root URI of the project
     *                   (e.g. {@code "file:///Users/me/ProjectA"})
     * @param context    the fully initialized {@link ProjectContext} to register
     */
    public void addProject(String projectUri, ProjectContext context) {

        if (projectUri == null || context == null) {
            log.log(Level.WARNING, "addProject called with null projectUri or context — ignoring.");
            return;
        }
        ProjectContext existing = projects.putIfAbsent(projectUri, context);
        if (existing != null) {
            log.log(Level.WARNING,
                    "A ProjectContext is already registered for URI: " + projectUri
                    + ". The existing context was NOT replaced. Call removeProject() first.");
        } else {
            log.log(Level.INFO, "Registered ProjectContext for: " + projectUri);
        }
    }

    /**
     * Removes and returns the {@link ProjectContext} for the given project root URI.
     *
     * <p>If no context is registered for {@code projectUri}, a warning is
     * logged and {@code null} is returned.
     *
     * @param projectUri the normalized root URI of the project to remove
     * @return the removed {@link ProjectContext}, or {@code null} if not found
     */
    public ProjectContext removeProject(String projectUri) {

        ProjectContext removed = projects.remove(projectUri);
        if (removed == null) {
            log.log(Level.WARNING,
                    "removeProject: no ProjectContext found for URI: " + projectUri);
        } else {
            log.log(Level.INFO, "Removed ProjectContext for: " + projectUri);
        }
        return removed;
    }

    // -------------------------------------------------------------------------
    // Query operations
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link ProjectContext} for an exact project root URI match.
     *
     * @param projectUri the normalized root URI of the project
     * @return the registered {@link ProjectContext}, or {@code null} if not found
     */
    public ProjectContext getProject(String projectUri) {

        return projects.get(projectUri);
    }

    /**
     * Resolves a document URI to the {@link ProjectContext} of the project it
     * belongs to, using a <em>longest-prefix match</em>.
     *
     * <p>Given a document URI such as
     * {@code file:///Users/me/ProjectA/src/main/synapse-config/api/MyAPI.xml},
     * this method iterates all registered project root URIs and returns the
     * context whose root URI is the longest prefix of the document URI. The
     * longest-prefix rule ensures correctness when one project root is nested
     * inside another.
     *
     * <p>Example:
     * <pre>
     *   Registered roots:
     *     file:///Users/me/ProjectA      → ContextA
     *     file:///Users/me/ProjectA/sub  → ContextB   (more specific)
     *
     *   getProjectForDocument("file:///Users/me/ProjectA/sub/foo.xml")
     *     → returns ContextB  (longest match)
     * </pre>
     *
     * @param documentUri the normalized URI of the document being processed
     * @return the best-matching {@link ProjectContext}, or {@code null} if no
     *         registered project contains the document
     */
    public ProjectContext getProjectForDocument(String documentUri) {

        if (documentUri == null) {
            return null;
        }

        ProjectContext bestMatch = null;
        int longestPrefixLength = -1;

        for (Map.Entry<String, ProjectContext> entry : projects.entrySet()) {
            String projectUri = entry.getKey();
            // Use separator check to avoid false matches (e.g. "project" matching "project2").
            if ((documentUri.startsWith(projectUri + "/") || documentUri.equals(projectUri))
                    && projectUri.length() > longestPrefixLength) {
                longestPrefixLength = projectUri.length();
                bestMatch = entry.getValue();
            }
        }

        if (bestMatch == null) {
            log.log(Level.WARNING,
                    "getProjectForDocument: no registered project contains document: " + documentUri);
        }
        return bestMatch;
    }

    /**
     * Returns an unmodifiable snapshot of all currently registered
     * {@link ProjectContext} instances.
     *
     * <p>The returned collection reflects the state of the registry at the
     * moment of the call. Subsequent additions or removals are not reflected.
     *
     * @return a collection of all registered contexts (never {@code null},
     *         may be empty)
     */
    public Collection<ProjectContext> getAllProjects() {

        // Return a true snapshot — not a live view — so callers can iterate safely
        // even if another thread adds/removes a project concurrently.
        return Collections.unmodifiableCollection(new ArrayList<>(projects.values()));
    }

    /**
     * Returns {@code true} if a {@link ProjectContext} is registered for the
     * given project root URI.
     *
     * @param projectUri the normalized root URI to query
     * @return {@code true} if the project is registered, {@code false} otherwise
     */
    public boolean hasProject(String projectUri) {

        return projects.containsKey(projectUri);
    }

    /**
     * Returns the number of {@link ProjectContext} instances currently registered.
     *
     * @return the project count (0 if no projects are registered)
     */
    public int getProjectCount() {

        return projects.size();
    }
}