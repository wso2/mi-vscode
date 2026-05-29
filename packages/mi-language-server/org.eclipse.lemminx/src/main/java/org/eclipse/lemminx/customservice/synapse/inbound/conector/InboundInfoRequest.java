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

package org.eclipse.lemminx.customservice.synapse.inbound.conector;

/**
 * Request payload for {@code synapse/getInboundInfo}. Supports two identification
 * modes:
 * <ul>
 *   <li>Bundled inbound: set {@link #id} to the built-in identifier (e.g. {@code "http"},
 *   {@code "jms"}, {@code "file"}). No download occurs.</li>
 *   <li>Downloaded inbound: set {@link #groupId}, {@link #artifactId}, {@link #version}
 *   to Maven coordinates of an {@code mi-inbound-*} artifact. Downloaded, extracted,
 *   and parsed as needed.</li>
 * </ul>
 * When {@code id} is provided but doesn't match a bundled inbound, the endpoint
 * falls through to the Maven path if coordinates are also provided.
 */
public class InboundInfoRequest {

    public String id;
    public String groupId;
    public String artifactId;
    public String version;
}
