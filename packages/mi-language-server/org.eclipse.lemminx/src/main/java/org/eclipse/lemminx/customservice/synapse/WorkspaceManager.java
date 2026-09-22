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

import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe registry mapping open workspace folder URIs, normalized to {@code file:///} format, to their isolated {@link ProjectContext} instances.
 */
public class WorkspaceManager {

    private static final Logger log = Logger.getLogger(WorkspaceManager.class.getName());

    /**
     * Map from normalized project root URI to the {@link ProjectContext} for that project, backed by a {@link ConcurrentHashMap} for lock-free reads.
     */
    private final Map<String, ProjectContext> projects = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Mutating operations
    // -------------------------------------------------------------------------

    /**
     * Registers a {@link ProjectContext} for the given project root URI, without overwriting an existing entry (call {@link #removeProject} first to replace one).
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
     * Removes and returns the {@link ProjectContext} registered for the given project root URI, or {@code null} if none is registered.
     *
     * @param projectUri the normalized root URI of the project to remove
     * @return the removed {@link ProjectContext}, or {@code null} if not found
     */
    public ProjectContext removeProject(String projectUri) {
        
        if (projectUri == null) {
            log.log(Level.WARNING, "removeProject called with null projectUri \u2014 ignoring.");
            return null;
        }

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
     * Resolves a {@link ProjectContext} by matching {@code projectPath} against each context's own filesystem path rather than the {@code file://} URI registry keys, which callers must use for RPC fields carrying a raw {@code fsPath} that would never match a key in {@link #projects}.
     *
     * @param projectPath the project root as an absolute filesystem path (or a {@code file://} URI,
     *                     which is normalized the same way)
     * @return the matching {@link ProjectContext}, or {@code null} if none matches
     */
    public ProjectContext getProjectByPath(String projectPath) {

        if (projectPath == null) {
            return null;
        }
        for (ProjectContext context : projects.values()) {
            if (isSameProjectPath(projectPath, context.getProjectUri())) {
                return context;
            }
        }
        return null;
    }

    /**
     * Whether two project roots denote the same project, tolerating format differences such as {@code file://} URI vs. OS path, redundant separators, and Windows drive-letter/case, since a raw {@link String#equals} would silently treat two spellings of the same folder as different projects.
     *
     * @return {@code true} if both resolve to the same normalized path; {@code false} if either is
     *         {@code null}
     */
    public static boolean isSameProjectPath(String pathA, String pathB) {

        if (pathA == null || pathB == null) {
            return false;
        }
        String normalizedA = normalizeProjectPath(pathA);
        String normalizedB = normalizeProjectPath(pathB);
        return normalizedA != null && normalizedA.equalsIgnoreCase(normalizedB);
    }

    /**
     * Normalizes a project root — a filesystem path or a {@code file://} URI — to the one canonical absolute path string that anything keying, hashing, or comparing projects should derive from.
     *
     * @param path the project root, as a filesystem path or a {@code file://} URI
     * @return the normalized absolute path, or {@code path} itself if it cannot be parsed
     */
    public static String normalizeProjectPath(String path) {

        if (path == null) {
            return null;
        }
        Path normalized = toComparablePath(path);
        return normalized != null ? normalized.toString() : path;
    }

    /**
     * Converts a filesystem path or a {@code file://} URI to an absolute, normalized {@link Path}, since comparing as {@link Path}s (rather than URI strings) avoids the inconsistent encoding of Windows drive letters (e.g. {@code c%3A} vs. {@code c:}).
     *
     * @return the normalized absolute path, or {@code null} if {@code pathOrUri} is {@code null} or
     *         cannot be parsed as a path
     */
    private static Path toComparablePath(String pathOrUri) {

        if (pathOrUri == null) {
            return null;
        }
        try {
            return Paths.get(Utils.getAbsolutePath(pathOrUri)).toAbsolutePath().normalize();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to normalize path for comparison: " + pathOrUri, e);
            return null;
        }
    }

    /**
     * Resolves the {@link ProjectContext} that owns a file via a longest-prefix, whole-path-element match over normalized project roots (so {@code .../Order2} isn't matched by root {@code .../Order}); callers with a path rather than a document URI should use this instead of {@link #getProjectForDocument(String)} to avoid URI-encoding mismatches.
     *
     * @param filePath the file's absolute filesystem path, or its {@code file://} URI
     * @return the owning {@link ProjectContext}, or {@code null} if no registered project contains it
     */
    public ProjectContext getProjectForFile(String filePath) {

        Path file = toComparablePath(filePath);
        if (file == null) {
            log.log(Level.WARNING, "getProjectForFile called with an unusable path \u2014 returning null: "
                    + filePath);
            return null;
        }

        ProjectContext bestMatch = null;
        int longestPrefixLength = -1;

        for (ProjectContext context : projects.values()) {
            Path root = toComparablePath(context.getProjectUri());
            // Path.startsWith compares whole name elements case-insensitively on Windows, matching isSameProjectPath's tolerance.
            if (root != null && file.startsWith(root) && root.getNameCount() > longestPrefixLength) {
                longestPrefixLength = root.getNameCount();
                bestMatch = context;
            }
        }

        if (bestMatch == null) {
            log.log(Level.WARNING, "getProjectForFile: no registered project contains file: " + file);
        }
        return bestMatch;
    }

    /**
     * Resolves a document URI to the owning {@link ProjectContext} by delegating to {@link #getProjectForFile(String)}'s longest-prefix match, avoiding a raw URI-key comparison that would miss on spelling differences like Windows {@code c%3A} vs. {@code c:}.
     *
     * @param documentUri the URI of the document being processed
     * @return the best-matching {@link ProjectContext}, or {@code null} if no
     *         registered project contains the document
     */
    public ProjectContext getProjectForDocument(String documentUri) {

        return getProjectForFile(documentUri);
    }

    /**
     * Returns an unmodifiable snapshot of all currently registered {@link ProjectContext} instances, reflecting the registry only at the moment of the call.
     *
     * @return a collection of all registered contexts (never {@code null},
     *         may be empty)
     */
    public Collection<ProjectContext> getAllProjects() {

        // Return a true snapshot, not a live view, so callers can iterate safely during concurrent modification.
        return Collections.unmodifiableCollection(new ArrayList<>(projects.values()));
    }

}