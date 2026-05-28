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

package org.eclipse.lemminx.customservice.synapse.mediatorService.pojo;

import org.eclipse.lsp4j.Range;

import java.util.List;
import java.util.Map;

public class SynapseConfigRequest {

    public String documentUri;
    public Range range;
    public String mediatorType;
    public Map<String, Object> values;
    public List<String> dirtyFields;

}
