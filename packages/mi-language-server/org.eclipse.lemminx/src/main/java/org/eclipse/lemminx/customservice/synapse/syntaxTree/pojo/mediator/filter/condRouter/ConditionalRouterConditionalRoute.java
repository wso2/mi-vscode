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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.targets.Target;

public class ConditionalRouterConditionalRoute extends STNode {

    ConditionalRouterConditionalRouteCondition condition;
    Target target;
    boolean asynchronous;
    boolean breakRoute;

    public ConditionalRouterConditionalRouteCondition getCondition() {

        return condition;
    }

    public void setCondition(ConditionalRouterConditionalRouteCondition condition) {

        this.condition = condition;
    }

    public Target getTarget() {

        return target;
    }

    public void setTarget(Target target) {

        this.target = target;
    }

    public boolean isAsynchronous() {

        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {

        this.asynchronous = asynchronous;
    }

    public boolean isBreakRoute() {

        return breakRoute;
    }

    public void setBreakRoute(boolean breakRoute) {

        this.breakRoute = breakRoute;
    }
}
