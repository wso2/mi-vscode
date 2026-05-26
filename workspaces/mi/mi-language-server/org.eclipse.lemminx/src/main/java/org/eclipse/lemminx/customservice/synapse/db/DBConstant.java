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

package org.eclipse.lemminx.customservice.synapse.db;

public class DBConstant {

    public static class DBTypes {

        public static final String DB_TYPE_MYSQL = "mysql";
        public static final String DB_TYPE_ORACLE = "oracle";
        public static final String DB_TYPE_ORACLE_CONN = "oracle:thin";
        public static final String DB_TYPE_MSSQL = "mssql";
        public static final String DB_TYPE_POSTGRESSQL = "postgresql";
        public static final String DB_TYPE_DERBY = "apachederby";
        public static final String DB_TYPE_DERBY_CONN = "derby";
        public static final String DB_TYPE_MSSQL_CONN = "sqlserver";
        public static final String DB_URL_JDBC_BASE = "jdbc:";
        public static final String DB_TYPE_H2 = "h2";
    }

    public static class DBUrlParams {

        public static final String DB_URL_JDBC_BASE = "jdbc:";
        public static final String DB_DRIVER_URL_BASE = "jar:file:";
        public static final String DB_DRIVER_JAR_BASE = "/dbdrivers/";
        public static final String DB_URL_JDBC_SUFFIX = "!/";
    }

    public static class DBDrivers {

        public static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
        public static final String DERBY_CLIENT_DRIVER = "org.apache.derby.jdbc.ClientDriver";
        public static final String MS_SQL_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        public static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
        public static final String DB2_DRIVER = "com.ibm.db2.jcc.DB2Driver";
        public static final String HSQL_DRIVER = "org.hsqldb.jdbcDriver";
        public static final String INFORMIX_DRIVER = "com.informix.jdbc.IfxDriver";
        public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";
        public static final String H2_DRIVER = "org.h2.Driver";
    }
}
