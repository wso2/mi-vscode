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

package org.eclipse.lemminx.customservice.synapse.dataService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of per-project DB-driver classloaders.
 *
 * <p>The shared language-server process serves every open project, so a single process-wide
 * classloader would let one project's driver load evict or collide with another's (e.g. two
 * projects needing different versions of the same driver class). Each project therefore gets
 * its own {@link URLClassLoader}, keyed by its normalized project root ({@link #normalize}).
 */
public class DynamicClassLoader {

    private static final Map<String, ProjectDrivers> registry = new ConcurrentHashMap<>();

    /** Per-project driver classloader state. All mutation is synchronized on the instance. */
    private static final class ProjectDrivers {

        // Stable parent for every project's loader, rather than the calling thread's context
        // classloader (which varies unpredictably across the shared server's request-pool threads).
        private static final ClassLoader PARENT = DynamicClassLoader.class.getClassLoader();

        private URLClassLoader classLoader;
        // Keyed by canonical path rather than URL, since URL/File path equality is case-sensitive
        // and breaks deduplication and removal on case-insensitive filesystems (e.g. Windows).
        private final Map<String, URL> currentUrls = new HashMap<>();
        // Which of those jars actually existed on disk when the current loader was built.
        //
        // A URLClassLoader is only valid for the on-disk state it was constructed against: the JDK
        // pops each URL off its search path the first time it opens it, and an open that fails
        // (missing jar) drops the URL permanently, so that loader can never see the jar reappear.
        // Registering the jar again does not help either - the key is already in currentUrls, so
        // there is nothing to "change" and no rebuild is triggered. That is how deleting a driver
        // from deployment/libs and then re-adding it through the datasource wizard leaves
        // checkDBDriver reporting the driver as missing for the rest of the session.
        //
        // Tracking existence at build time turns that into something loader() can detect, and also
        // covers the reverse case (a jar deleted after the loader opened it) and jars dropped into
        // deployment/libs by hand, neither of which goes through updateJar at all.
        private Set<String> presentWhenBuilt = Set.of();

        synchronized void addDirectory(File jarDirectory) throws Exception {
            File[] jarFiles = jarDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                return;
            }
            boolean changed = false;
            for (File jarFile : jarFiles) {
                String jarKey = jarFile.getCanonicalPath();
                if (!currentUrls.containsKey(jarKey)) {
                    currentUrls.put(jarKey, jarFile.toURI().toURL());
                    changed = true;
                }
            }
            if (changed) {
                rebuild();
            }
        }

        synchronized void updateJar(File jarFile, boolean addJar) throws Exception {
            String jarKey = jarFile.getCanonicalPath();
            boolean changed;
            if (addJar) {
                changed = currentUrls.putIfAbsent(jarKey, jarFile.toURI().toURL()) == null;
            } else {
                changed = currentUrls.remove(jarKey) != null;
            }
            if (changed) {
                rebuild();
            }
        }

        synchronized URLClassLoader loader() {
            Set<String> present = presentJars();
            if (classLoader == null || !present.equals(presentWhenBuilt)) {
                rebuild(present);
            }
            return classLoader;
        }

        /** Canonical paths of the registered jars that are currently readable files on disk. */
        private Set<String> presentJars() {
            Set<String> present = new HashSet<>();
            for (String jarKey : currentUrls.keySet()) {
                if (new File(jarKey).isFile()) {
                    present.add(jarKey);
                }
            }
            return present;
        }

        private void rebuild() {
            rebuild(presentJars());
        }

        // Missing jars stay in the URL array: currentUrls is the record of what this project has
        // registered, and a URL the loader cannot open is simply skipped. Dropping them here would
        // instead lose them for good, since nothing re-registers a jar that only reappears on disk.
        private void rebuild(Set<String> present) {
            classLoader = new URLClassLoader(currentUrls.values().toArray(new URL[0]), PARENT);
            presentWhenBuilt = present;
        }
    }

    private static ProjectDrivers forProject(String projectKey) {
        return registry.computeIfAbsent(normalize(projectKey), key -> new ProjectDrivers());
    }

    /**
     * Add DB drivers in a folder to the given project's class loader (union — never evicts
     * drivers already loaded for this or any other project).
     *
     * @param projectKey   this project's root (URI or absolute path — see {@link #normalize})
     * @param jarDirectory path of the DB driver jars folder
     */
    public static void updateClassLoader(String projectKey, File jarDirectory) throws Exception {
        forProject(projectKey).addDirectory(jarDirectory);
    }

    /**
     * Add or remove a single DB driver from the given project's class path.
     *
     * @param projectKey this project's root (URI or absolute path — see {@link #normalize})
     * @param jarFile    path of the DB driver jar
     * @param addJar     whether the jar should be added or removed
     */
    public static void updateJarInClassLoader(String projectKey, File jarFile, boolean addJar) throws Exception {
        forProject(projectKey).updateJar(jarFile, addJar);
    }

    /**
     * @param projectKey this project's root (URI or absolute path — see {@link #normalize})
     * @return the class loader scoped to this project's DB drivers
     */
    public static URLClassLoader getClassLoader(String projectKey) {
        return forProject(projectKey).loader();
    }

    /**
     * Drops the registry entry for a project (e.g. on project close), so a later reopen rebuilds
     * cleanly instead of reusing a stale loader. Does not {@code close()} the discarded
     * {@link URLClassLoader} — a driver connection opened through it may still be in use.
     *
     * @param projectKey this project's root (URI or absolute path — see {@link #normalize})
     */
    public static void removeProject(String projectKey) {
        registry.remove(normalize(projectKey));
    }

    /**
     * Normalizes a project key so the same project always maps to the same registry entry
     * regardless of whether callers pass a {@code file://} URI or a plain OS path.
     */
    static String normalize(String projectKey) {
        if (projectKey == null) {
            return "";
        }
        try {
            Path path = looksLikeUri(projectKey) ? Paths.get(URI.create(projectKey)) : Paths.get(projectKey);
            try {
                return path.toRealPath().toString();
            } catch (IOException e) {
                // Path may not exist yet (e.g. in tests, or a project root not yet materialized on
                // disk) - fall back to a normalized absolute path so repeat calls still agree.
                return path.toAbsolutePath().normalize().toString();
            }
        } catch (Exception e) {
            return projectKey;
        }
    }

    private static boolean looksLikeUri(String key) {
        return key.startsWith("file:") || key.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }
}
