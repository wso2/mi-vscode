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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.builder;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class BuilderMessageBuilder extends STNode {

    String contentType;
    String clazz;
    String formatterClass;

    public String getContentType() {

        return contentType;
    }

    public void setContentType(String contentType) {

        this.contentType = contentType;
    }

    public String getClazz() {

        return clazz;
    }

    public void setClazz(String clazz) {

        this.clazz = clazz;
    }

    public String getFormatterClass() {

        return formatterClass;
    }

    public void setFormatterClass(String formatterClass) {

        this.formatterClass = formatterClass;
    }
}
