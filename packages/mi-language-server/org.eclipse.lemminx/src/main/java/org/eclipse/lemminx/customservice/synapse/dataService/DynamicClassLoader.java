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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class DynamicClassLoader {

    private static URLClassLoader classLoader;
    private static final Object lock = new Object();
    private static Map<String, URL> currentUrls = new HashMap<>();

    /**
     * Add DB drivers in a folder to the class loader
     *
     * @param jarDirectory path of the DB driver jars folder
     */
    public static void updateClassLoader(File jarDirectory) throws Exception {
        synchronized (lock) {
            File[] jarFiles = jarDirectory.listFiles((dir1, name) -> name.endsWith(".jar"));

            if (jarFiles == null || jarFiles.length == 0) {
                return;
            }

            Map<String, URL> newUrls = new HashMap<>();
            for (File jarFile : jarFiles) {
                newUrls.put(jarFile.getCanonicalPath(), jarFile.toURI().toURL());
            }

            if (!currentUrls.keySet().equals(newUrls.keySet())) {
                classLoader = new URLClassLoader(newUrls.values().toArray(new URL[0]),
                        Thread.currentThread().getContextClassLoader());
                currentUrls = newUrls;
            }
        }
    }

    /**
     * Add or remove a DB driver from the class path
     *
     * @param jarFile path of the DB driver jar
     * @param addJar whether the jar should be added or removed
     */
    public static void updateJarInClassLoader(File jarFile, boolean addJar) throws Exception {
        synchronized (lock) {
            String jarKey = jarFile.getCanonicalPath();
            Map<String, URL> updatedUrls = new HashMap<>(currentUrls);
            if (addJar) {
                updatedUrls.putIfAbsent(jarKey, jarFile.toURI().toURL());
            } else {
                updatedUrls.remove(jarKey);
            }
            if (!updatedUrls.keySet().equals(currentUrls.keySet())) {
                classLoader = new URLClassLoader(updatedUrls.values().toArray(new URL[0]),
                        Thread.currentThread().getContextClassLoader());
                currentUrls = updatedUrls;
            }
        }
    }

    public static URLClassLoader getClassLoader() {
        return classLoader;
    }
}
