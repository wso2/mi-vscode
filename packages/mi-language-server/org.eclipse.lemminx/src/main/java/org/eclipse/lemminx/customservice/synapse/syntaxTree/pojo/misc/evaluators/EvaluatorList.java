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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.evaluators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.Optional;

public class EvaluatorList extends STNode {

    Optional<And> and;
    Optional<Or> or;
    Optional<Equal> equal;
    Optional<Not> not;

    public Optional<And> getAnd() {

        return and;
    }

    public void setAnd(Optional<And> and) {

        this.and = and;
    }

    public Optional<Or> getOr() {

        return or;
    }

    public void setOr(Optional<Or> or) {

        this.or = or;
    }

    public Optional<Equal> getEqual() {

        return equal;
    }

    public void setEqual(Optional<Equal> equal) {

        this.equal = equal;
    }

    public Optional<Not> getNot() {

        return not;
    }

    public void setNot(Optional<Not> not) {

        this.not = not;
    }
}