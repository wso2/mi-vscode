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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.condRouter;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class ConditionalRouter extends Mediator {

    ConditionalRouterConditionalRoute[] conditionalRoute;
    boolean continueAfter;
    String description;

    public ConditionalRouterConditionalRoute[] getConditionalRoute() {

        return conditionalRoute;
    }

    public void setConditionalRoute(ConditionalRouterConditionalRoute[] conditionalRoute) {

        this.conditionalRoute = conditionalRoute;
    }

    public boolean isContinueAfter() {

        return continueAfter;
    }

    public void setContinueAfter(boolean continueAfter) {

        this.continueAfter = continueAfter;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}