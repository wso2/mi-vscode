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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;

public class Iterate extends Mediator {

    Target target;
    boolean sequential;
    boolean continueParent;
    String expression;
    boolean preservePayload;
    String attachPath;
    String id;
    String description;
    String traceFilter;

    public Iterate() {
        setDisplayName("Iterate");
    }

    public Target getTarget() {

        return target;
    }

    public void setTarget(Target target) {

        this.target = target;
    }

    public boolean isSequential() {

        return sequential;
    }

    public void setSequential(boolean sequential) {

        this.sequential = sequential;
    }

    public boolean isContinueParent() {

        return continueParent;
    }

    public void setContinueParent(boolean continueParent) {

        this.continueParent = continueParent;
    }

    public String getExpression() {

        return expression;
    }

    public void setExpression(String expression) {

        this.expression = expression;
    }

    public boolean isPreservePayload() {

        return preservePayload;
    }

    public void setPreservePayload(boolean preservePayload) {

        this.preservePayload = preservePayload;
    }

    public String getAttachPath() {

        return attachPath;
    }

    public void setAttachPath(String attachPath) {

        this.attachPath = attachPath;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
