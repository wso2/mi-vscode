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
import org.apache.commons.lang3.NotImplementedException;

public class DebugInfo implements IDebugInfo {

    String mediatorPosition;
    boolean isValid = Boolean.TRUE;
    String error;

    @Override
    public boolean isValid() {

        return isValid;
    }

    @Override
    public void setValid(boolean valid) {

        isValid = valid;
    }

    @Override
    public String getError() {

        return error;
    }

    @Override
    public void setError(String error) {

        this.error = error;
    }

    @Override
    public JsonElement toJson() {

        throw new NotImplementedException("toJson method is not implemented");
    }

    @Override
    public void setMediatorPosition(String mediatorPosition) {

        this.mediatorPosition = mediatorPosition;
    }

    @Override
    public String getMediatorPosition() {

        return mediatorPosition;
    }

    @Override
    public IDebugInfo clone() throws CloneNotSupportedException {

        return (IDebugInfo) super.clone();
    }

    @Override
    public String toString() {

        return "DebugInfo{" +
                "mediatorPosition='" + mediatorPosition + '\'' +
                ", isValid=" + isValid +
                ", error='" + error + '\'' +
                '}';
    }
}
