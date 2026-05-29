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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Sequence;

public class Throttle extends Mediator {

    ThrottlePolicies policies;
    Sequence onAccept;
    Sequence onReject;
    String id;
    String onAcceptAttribute;
    String onRejectAttribute;
    String description;
    String traceFilter;

    public Throttle() {
        setDisplayName("Throttle");
    }

    public ThrottlePolicies getPolicies() {

        return policies;
    }

    public void setPolicies(ThrottlePolicies policies) {

        this.policies = policies;
    }

    public Sequence getOnAccept() {

        return onAccept;
    }

    public void setOnAccept(Sequence onAccept) {

        this.onAccept = onAccept;
    }

    public Sequence getOnReject() {

        return onReject;
    }

    public void setOnReject(Sequence onReject) {

        this.onReject = onReject;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public String getOnAcceptAttribute() {

        return onAcceptAttribute;
    }

    public void setOnAcceptAttribute(String onAcceptAttribute) {

        this.onAcceptAttribute = onAcceptAttribute;
    }

    public String getOnRejectAttribute() {

        return onRejectAttribute;
    }

    public void setOnRejectAttribute(String onRejectAttribute) {

        this.onRejectAttribute = onRejectAttribute;
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
