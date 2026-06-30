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

package org.eclipse.lemminx.customservice.synapse.directoryTree.legacyBuilder.utils;

public enum ProjectType {
    ROOT_PROJECT("org.wso2.developerstudio.eclipse.mavenmultimodule.project.nature"),
    DATA_SERVICE_CONFIGS("org.wso2.developerstudio.eclipse.ds.project.nature"),
    ESB_CONFIGS("org.wso2.developerstudio.eclipse.esb.project.nature"),
    COMPOSITE_EXPORTER("org.wso2.developerstudio.eclipse.distribution.project.nature"),
    CONNECTOR_EXPORTER("org.wso2.developerstudio.eclipse.artifact.connector.project.nature"),
    DATA_SOURCE_CONFIGS("org.wso2.developerstudio.eclipse.datasource.project.nature"),
    MEDIATOR_PROJECT("org.wso2.developerstudio.eclipse.artifact.mediator.project.nature"),
    REGISTRY_RESOURCE("org.wso2.developerstudio.eclipse.general.project.nature"),
    DOCKER_EXPORTER("org.wso2.developerstudio.eclipse.docker.distribution.project.nature"),
    KUBERNETES_EXPORTER("org.wso2.developerstudio.eclipse.kubernetes.distribution.project.nature"),
    JAVA_LIBRARY_PROJECT("org.wso2.developerstudio.eclipse.java.library.project.nature");

    public String value;

    ProjectType(String value) {

        this.value = value;
    }
}
