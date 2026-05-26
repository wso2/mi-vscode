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

package org.eclipse.lemminx.synapse;

import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for Utils.getServerVersion() NPE fix.
 * Verifies that unmapped MI versions return a default rather than null.
 */
public class UtilsServerVersionTest {

    @TempDir
    Path tempDir;

    private void writePomWithVersion(String version) throws IOException {
        String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>com.example</groupId>\n"
                + "  <artifactId>test-project</artifactId>\n"
                + "  <version>1.0.0</version>\n"
                + "  <properties>\n"
                + "    <project.runtime.version>" + version + "</project.runtime.version>\n"
                + "  </properties>\n"
                + "</project>";
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);
    }

    @Test
    public void testUnmappedVersionReturnsDefault() throws Exception {
        writePomWithVersion("9.9.9");
        String result = Utils.getServerVersion(tempDir.toString(), Constant.DEFAULT_MI_VERSION);
        assertNotNull(result, "getServerVersion should never return null");
        assertEquals(Constant.DEFAULT_MI_VERSION, result,
                "Unmapped version should fall back to DEFAULT_MI_VERSION");
    }

    @Test
    public void testKnown440Version() throws Exception {
        writePomWithVersion("4.4.0");
        String result = Utils.getServerVersion(tempDir.toString(), Constant.DEFAULT_MI_VERSION);
        assertNotNull(result);
        // 4.4.0 is in MI_SUPPORTED_VERSION_MAP, should map to itself or its mapping
        assertEquals(Constant.MI_SUPPORTED_VERSION_MAP.get("4.4.0"), result);
    }

    @Test
    public void testKnown430Version() throws Exception {
        writePomWithVersion("4.3.0");
        String result = Utils.getServerVersion(tempDir.toString(), Constant.DEFAULT_MI_VERSION);
        assertNotNull(result);
        assertEquals(Constant.MI_SUPPORTED_VERSION_MAP.get("4.3.0"), result);
    }

    @Test
    public void testNoPomReturnsDefault() {
        // Non-existent project path with no pom.xml
        String result = Utils.getServerVersion("/nonexistent/path/12345", Constant.DEFAULT_MI_VERSION);
        assertNotNull(result, "Should return default, not null");
        String expected = Constant.MI_SUPPORTED_VERSION_MAP.get(Constant.DEFAULT_MI_VERSION);
        assertEquals(expected, result, "Should return the mapped default MI version");
    }

    @Test
    public void testBelowMinimumReturns430() throws Exception {
        writePomWithVersion("4.2.0");
        String result = Utils.getServerVersion(tempDir.toString(), Constant.DEFAULT_MI_VERSION);
        assertNotNull(result);
        // Version below 4.3.0 minimum should return 4.3.0
        assertEquals(Constant.MI_SUPPORTED_VERSION_MAP.get(Constant.MI_430_VERSION), result);
    }
}
