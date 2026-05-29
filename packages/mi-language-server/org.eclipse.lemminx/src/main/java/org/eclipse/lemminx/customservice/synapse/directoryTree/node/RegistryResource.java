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

package org.eclipse.lemminx.customservice.synapse.directoryTree.node;

public class RegistryResource {

    private FolderNode conf;
    private FolderNode gov;

    public FolderNode getConf() {

        return conf;
    }

    public void setConf(FolderNode conf) {

        this.conf = conf;
    }

    public FolderNode getGov() {

        return gov;
    }

    public void setGov(FolderNode gov) {

        this.gov = gov;
    }
}
