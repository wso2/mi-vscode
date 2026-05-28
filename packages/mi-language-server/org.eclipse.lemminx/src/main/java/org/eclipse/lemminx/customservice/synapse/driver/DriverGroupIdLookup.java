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

package org.eclipse.lemminx.customservice.synapse.driver;

import java.util.HashMap;
import java.util.Map;

public class DriverGroupIdLookup {

    private static final Map<String, String> DRIVER_GROUP_IDS = new HashMap<>();

    static {
        // PostgreSQL
        DRIVER_GROUP_IDS.put("postgresql", "org.postgresql");
        DRIVER_GROUP_IDS.put("pgjdbc-ng", "com.impossibl.pgjdbc-ng");

        // MySQL
        DRIVER_GROUP_IDS.put("mysql-connector-java", "mysql");
        DRIVER_GROUP_IDS.put("mysql-connector-j", "com.mysql");
        DRIVER_GROUP_IDS.put("mariadb-java-client", "org.mariadb.jdbc");

        // SQL Server
        DRIVER_GROUP_IDS.put("mssql-jdbc", "com.microsoft.sqlserver");
        DRIVER_GROUP_IDS.put("jtds", "net.sourceforge.jtds");

        // Oracle
        DRIVER_GROUP_IDS.put("simplefan", "com.oracle.database.ha");
        DRIVER_GROUP_IDS.put("ojdbc", "com.oracle.database.jdbc");

        // DB2
        DRIVER_GROUP_IDS.put("jcc", "com.ibm.db2");
        DRIVER_GROUP_IDS.put("db2jcc", "com.ibm.db2.jcc");
    }

    public static String getGroupIdFromArtifactId(String artifactId) {

        String key = artifactId.toLowerCase();

        // Direct matches
        if (DRIVER_GROUP_IDS.containsKey(key)) {
            return DRIVER_GROUP_IDS.get(key);
        }

        // Pattern matching
        for (String pattern : DRIVER_GROUP_IDS.keySet()) {
            if (key.contains(pattern)) {
                return DRIVER_GROUP_IDS.get(pattern);
            }
        }
        return "unknown";
    }

    public static String getGroupIdFromFileName(String fileName) {

        String baseName = fileName.replace(".jar", "").toLowerCase();

        // Remove version part to get cleaner artifact ID
        if (baseName.matches(".*\\d+.*")) {
            baseName = baseName.replaceAll("-\\d+.*", "");
        }

        return getGroupIdFromArtifactId(baseName);
    }
}

