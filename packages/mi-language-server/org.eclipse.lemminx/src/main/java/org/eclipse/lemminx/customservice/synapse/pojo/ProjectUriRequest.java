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

package org.eclipse.lemminx.customservice.synapse.pojo;

/**
 * Minimal request carrying only the project root URI, for {@code synapse/*} RPCs that have no
 * document URI to resolve a project from. Optional and backward-compatible: a null/blank/unmatched
 * {@code projectUri} falls back to the facade's {@code defaultContext}.
 */
public class ProjectUriRequest {

    public String projectUri;
}
