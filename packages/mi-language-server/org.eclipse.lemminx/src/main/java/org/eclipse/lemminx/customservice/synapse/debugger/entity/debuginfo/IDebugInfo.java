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

package org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo;

import com.google.gson.JsonElement;

public interface IDebugInfo extends Cloneable {

    void setMediatorPosition(String mediatorPosition);

    String getMediatorPosition();

    boolean isValid();

    void setValid(boolean valid);

    String getError();

    void setError(String error);

    JsonElement toJson();

    IDebugInfo clone() throws CloneNotSupportedException;
}
