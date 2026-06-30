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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.eip.aggregate;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.Optional;

public class CorrelateOnOrCompleteConditionOrOnComplete extends STNode {

    Optional<AggregateCorrelateOn> correlateOn;
    Optional<AggregateCompleteCondition> completeCondition;
    Optional<AggregateOnComplete> onComplete;

    public CorrelateOnOrCompleteConditionOrOnComplete() {

        correlateOn = Optional.empty();
        completeCondition = Optional.empty();
        onComplete = Optional.empty();
    }

    public Optional<AggregateCorrelateOn> getCorrelateOn() {

        return correlateOn;
    }

    public void setCorrelateOn(Optional<AggregateCorrelateOn> correlateOn) {

        this.correlateOn = correlateOn;
    }

    public Optional<AggregateCompleteCondition> getCompleteCondition() {

        return completeCondition;
    }

    public void setCompleteCondition(Optional<AggregateCompleteCondition> completeCondition) {

        this.completeCondition = completeCondition;
    }

    public Optional<AggregateOnComplete> getOnComplete() {

        return onComplete;
    }

    public void setOnComplete(Optional<AggregateOnComplete> onComplete) {

        this.onComplete = onComplete;
    }
}
