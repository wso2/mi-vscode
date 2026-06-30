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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

public enum DeployedArtifactType {

    APIS("apis"),
    SEQUENCES("sequences"),
    ENDPOINTS("endpoints"),
    LOCAL_ENTRIES("local-entries"),
    TASKS("tasks"),
    MESSAGE_STORES("message-stores"),
    MESSAGE_PROCESSORS("message-processors"),
    INBOUND_ENDPOINTS("inbound-endpoints"),
    TEMPLATES("templates"),
    DATA_SERVICES("data-services"),
    DATA_SOURCES("data-sources");

    private final String value;

    DeployedArtifactType(String value) {

        this.value = value;
    }

    public String getValue() {

        return value;
    }
}
