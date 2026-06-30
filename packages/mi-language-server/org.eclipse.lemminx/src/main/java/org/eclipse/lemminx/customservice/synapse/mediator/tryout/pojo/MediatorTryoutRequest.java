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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MediatorTryoutRequest {

    private final String file;
    private String tryoutId;
    private final int line;
    private final int column;
    private final String contentType;
    private final String inputPayload;
    private final List<Property> queryParams;
    private final List<Property> pathParams;
    private final Edit[] edits;
    private MediatorInfo mediatorInfo;
    private boolean isIsolatedTryout;

    public MediatorTryoutRequest(String file, int line, int column, String inputPayload, Edit[] edits) {

        this(file, line, column, "application/json", inputPayload, edits);
    }

    public MediatorTryoutRequest(String file, int line, int column, String contentType, String inputPayload,
                                 Edit[] edits) {

        this.file = file;
        this.line = line;
        this.column = column;
        this.contentType = contentType;
        this.inputPayload = inputPayload;
        this.edits = edits;
        this.queryParams = new ArrayList<>();
        this.pathParams = new ArrayList<>();
    }

    public String getFile() {

        return file;
    }

    public int getLine() {

        return line;
    }

    public int getColumn() {

        return column;
    }

    public String getContentType() {

        return contentType;
    }

    public String getInputPayload() {

        return inputPayload;
    }

    public Edit[] getEdits() {

        if (edits == null) {
            return null;
        }
        return edits.clone();
    }

    public MediatorInfo getMediatorInfo() {

        return mediatorInfo;
    }

    public void setMediatorInfo(MediatorInfo mediatorInfo) {

        this.mediatorInfo = mediatorInfo;
    }

    public String getTryoutId() {

        return tryoutId;
    }

    public void setTryoutId(String tryoutId) {

        this.tryoutId = tryoutId;
    }

    public boolean isIsolatedTryout() {

        return isIsolatedTryout;
    }

    public List<Property> getQueryParams() {

        return queryParams;
    }

    public List<Property> getPathParams() {

        return pathParams;
    }

    @Override
    public String toString() {

        return "MediatorTryoutRequest{" +
                "file='" + file + '\'' +
                ", tryoutId='" + tryoutId + '\'' +
                ", line=" + line +
                ", column=" + column +
                ", inputPayload='" + inputPayload + '\'' +
                ", edits=" + Arrays.toString(edits) +
                ", mediatorInfo=" + mediatorInfo +
                ", isIsolatedTryout=" + isIsolatedTryout +
                '}';
    }
}
