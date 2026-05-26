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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class Resource extends STNode {

    STNode description;
    CallQuery callQuery;
    String path;
    String method;
    boolean disableStreaming;
    boolean returnRequestStatus;

    public STNode getDescription() {

        return description;
    }

    public void setDescription(STNode description) {

        this.description = description;
    }

    public CallQuery getCallQuery() {

        return callQuery;
    }

    public void setCallQuery(CallQuery callQuery) {

        this.callQuery = callQuery;
    }

    public String getPath() {

        return path;
    }

    public void setPath(String path) {

        this.path = path;
    }

    public String getMethod() {

        return method;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public boolean isDisableStreaming() {

        return disableStreaming;
    }

    public void setDisableStreaming(boolean disableStreaming) {

        this.disableStreaming = disableStreaming;
    }

    public boolean isReturnRequestStatus() {

        return returnRequestStatus;
    }

    public void setReturnRequestStatus(boolean returnRequestStatus) {

        this.returnRequestStatus = returnRequestStatus;
    }
}
