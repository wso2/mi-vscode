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

public class CheckDBDriverResponseParams {

    private boolean isDriverAvailable;
    private String driverVersion;
    private String driverPath;

    public CheckDBDriverResponseParams(boolean isDriverAvailable, String driverVersion, String driverPath) {
        this.isDriverAvailable = isDriverAvailable;
        this.driverVersion = driverVersion;
        this.driverPath = driverPath;
    }

    public boolean isDriverAvailable() {
        return isDriverAvailable;
    }

    public String getDriverVersion() {
        return driverVersion;
    }
}
