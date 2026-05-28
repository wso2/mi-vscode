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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.rewrite;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.And;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Equal;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Not;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators.Or;

public class RewriteRewriteruleCondition extends STNode {

    And and;
    Or or;
    Equal equal;
    Not not;
    String condition;

    public And getAnd() {

        return and;
    }

    public void setAnd(And and) {

        this.and = and;
    }

    public Or getOr() {

        return or;
    }

    public void setOr(Or or) {

        this.or = or;
    }

    public Equal getEqual() {

        return equal;
    }

    public void setEqual(Equal equal) {

        this.equal = equal;
    }

    public Not getNot() {

        return not;
    }

    public void setNot(Not not) {

        this.not = not;
    }

    public String getCondition() {

        return condition;
    }

    public void setCondition(String condition) {

        this.condition = condition;
    }
}
