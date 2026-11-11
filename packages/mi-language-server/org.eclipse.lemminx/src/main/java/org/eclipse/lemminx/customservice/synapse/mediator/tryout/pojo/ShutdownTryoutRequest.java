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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

import org.eclipse.lemminx.customservice.synapse.pojo.AbstractProjectRequest;

/**
 * Request to stop the Try-Out MI server, sent before the project is run or debugged so the ports are free.
 * The server is stopped even if another project is using it, and any paused Try-Out session is ended.
 */
public class ShutdownTryoutRequest extends AbstractProjectRequest {

}
