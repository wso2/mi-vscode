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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.DbMediator;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.KeyAttribute;

public class DbMediatorConnectionPool extends STNode {

    KeyAttribute dsName;
    KeyAttribute icClass;
    KeyAttribute driver;
    KeyAttribute url;
    KeyAttribute user;
    KeyAttribute password;
    DbMediatorConnectionPoolProperty[] property;

    public KeyAttribute getDsName() {

        return dsName;
    }

    public void setDsName(KeyAttribute dsName) {

        this.dsName = dsName;
    }

    public KeyAttribute getIcClass() {

        return icClass;
    }

    public void setIcClass(KeyAttribute icClass) {

        this.icClass = icClass;
    }

    public KeyAttribute getDriver() {

        return driver;
    }

    public void setDriver(KeyAttribute driver) {

        this.driver = driver;
    }

    public KeyAttribute getUrl() {

        return url;
    }

    public void setUrl(KeyAttribute url) {

        this.url = url;
    }

    public KeyAttribute getUser() {

        return user;
    }

    public void setUser(KeyAttribute user) {

        this.user = user;
    }

    public KeyAttribute getPassword() {

        return password;
    }

    public void setPassword(KeyAttribute password) {

        this.password = password;
    }

    public DbMediatorConnectionPoolProperty[] getProperty() {

        return property;
    }

    public void setProperty(DbMediatorConnectionPoolProperty[] property) {

        this.property = property;
    }
}