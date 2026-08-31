/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicClassLoaderTest {

    @Test
    void twoProjectsGetIndependentLoaders(@TempDir Path tempDir) throws Exception {
        String projectA = tempDir.resolve("projectA").toString();
        String projectB = tempDir.resolve("projectB").toString();

        URLClassLoader loaderA = DynamicClassLoader.getClassLoader(projectA);
        URLClassLoader loaderB = DynamicClassLoader.getClassLoader(projectB);

        assertNotEquals(loaderA, loaderB, "Two different projects must not share a classloader instance");
    }

    @Test
    void loadingProjectBDriversDoesNotEvictProjectADrivers(@TempDir Path tempDir) throws Exception {
        String projectA = tempDir.resolve("projectA_" + System.nanoTime()).toString();
        String projectB = tempDir.resolve("projectB_" + System.nanoTime()).toString();

        Path jarADir = Files.createDirectories(Path.of(projectA, "libs"));
        Path jarBDir = Files.createDirectories(Path.of(projectB, "libs"));
        File jarA = createEmptyJar(jarADir.resolve("driver-a.jar"));
        File jarB = createEmptyJar(jarBDir.resolve("driver-b.jar"));

        DynamicClassLoader.updateClassLoader(projectA, jarADir.toFile());
        DynamicClassLoader.updateClassLoader(projectB, jarBDir.toFile());

        URL[] urlsA = DynamicClassLoader.getClassLoader(projectA).getURLs();
        URL[] urlsB = DynamicClassLoader.getClassLoader(projectB).getURLs();

        assertTrue(containsUrlFor(urlsA, jarA), "Project A's loader must still contain its own driver jar");
        assertTrue(containsUrlFor(urlsB, jarB), "Project B's loader must contain its own driver jar");
        assertTrue(!containsUrlFor(urlsA, jarB), "Project A's loader must not have been polluted by project B's jar");
        assertTrue(!containsUrlFor(urlsB, jarA), "Project B's loader must not have been polluted by project A's jar");
    }

    @Test
    void uriFormAndPathFormOfSameProjectNormalizeEqual(@TempDir Path tempDir) throws Exception {
        Path projectRoot = Files.createDirectories(tempDir.resolve("project"));
        String pathForm = projectRoot.toAbsolutePath().toString();
        String uriForm = projectRoot.toUri().toString();

        assertEquals(DynamicClassLoader.normalize(pathForm), DynamicClassLoader.normalize(uriForm),
                "URI form and absolute-path form of the same project root must key to the same registry entry");
    }

    @Test
    void removeProjectDropsTheRegistryEntry(@TempDir Path tempDir) throws Exception {
        String project = tempDir.resolve("removable").toString();

        URLClassLoader before = DynamicClassLoader.getClassLoader(project);
        DynamicClassLoader.removeProject(project);
        URLClassLoader after = DynamicClassLoader.getClassLoader(project);

        assertNotEquals(before, after, "A fresh loader must be created after the project entry is removed");
    }

    @Test
    void loaderIsRebuiltWhenADeletedJarIsRestored(@TempDir Path tempDir) throws Exception {
        String project = tempDir.resolve("restore_" + System.nanoTime()).toString();
        Path libs = Files.createDirectories(Path.of(project, "deployment", "libs"));
        File jar = createEmptyJar(libs.resolve("driver.jar"));

        DynamicClassLoader.updateClassLoader(project, libs.toFile());
        URLClassLoader seeded = DynamicClassLoader.getClassLoader(project);

        // The user clears deployment/libs by hand while the server is running.
        Files.delete(jar.toPath());
        URLClassLoader whileMissing = DynamicClassLoader.getClassLoader(project);
        assertNotSame(seeded, whileMissing, "A loader must not be reused across a jar disappearing from disk");

        // The datasource wizard copies the jar back to the same path and re-registers it. The key is
        // already known, so the add itself is a no-op - the rebuild has to come from the restored file.
        createEmptyJar(libs.resolve("driver.jar"));
        DynamicClassLoader.updateJarInClassLoader(project, jar, true);
        URLClassLoader afterRestore = DynamicClassLoader.getClassLoader(project);

        assertNotSame(whileMissing, afterRestore, "A loader built while the jar was missing has dropped that "
                + "URL for good, so a restored jar must be served by a rebuilt loader");
    }

    @Test
    void loaderIsStableWhileTheJarsOnDiskAreUnchanged(@TempDir Path tempDir) throws Exception {
        String project = tempDir.resolve("stable_" + System.nanoTime()).toString();
        Path libs = Files.createDirectories(Path.of(project, "deployment", "libs"));
        createEmptyJar(libs.resolve("driver.jar"));

        DynamicClassLoader.updateClassLoader(project, libs.toFile());

        assertSame(DynamicClassLoader.getClassLoader(project), DynamicClassLoader.getClassLoader(project),
                "An unchanged deployment/libs must keep handing back the same loader, so classes already "
                        + "loaded through it stay identity-comparable");
    }

    private File createEmptyJar(Path path) throws IOException {
        Files.write(path, new byte[]{0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        return path.toFile();
    }

    private boolean containsUrlFor(URL[] urls, File jar) throws Exception {
        URL target = jar.toURI().toURL();
        for (URL url : urls) {
            if (url.equals(target)) {
                return true;
            }
        }
        return false;
    }
}
