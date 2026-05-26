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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class DriverShim implements Driver {

    private Driver driver;

    public DriverShim(Driver d) {

        this.driver = d;
    }

    public boolean acceptsURL(String u) throws SQLException {

        return this.driver.acceptsURL(u);
    }

    public Connection connect(String u, Properties p) throws SQLException {

        return this.driver.connect(u, p);
    }

    public int getMajorVersion() {

        return this.driver.getMajorVersion();
    }

    public int getMinorVersion() {

        return this.driver.getMinorVersion();
    }

    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {

        return this.driver.getPropertyInfo(u, p);
    }

    public boolean jdbcCompliant() {

        return this.driver.jdbcCompliant();
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {

        return null;
    }
}
