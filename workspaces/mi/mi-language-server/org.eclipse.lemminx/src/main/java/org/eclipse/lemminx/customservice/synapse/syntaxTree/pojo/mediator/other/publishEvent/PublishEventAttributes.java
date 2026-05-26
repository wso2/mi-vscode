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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class PublishEventAttributes extends STNode {

    PublishEventAttributesMeta meta;
    PublishEventAttributesCorrelation correlation;
    PublishEventAttributesPayload payload;
    PublishEventAttributesArbitrary arbitrary;

    public PublishEventAttributesMeta getMeta() {

        return meta;
    }

    public void setMeta(PublishEventAttributesMeta meta) {

        this.meta = meta;
    }

    public PublishEventAttributesCorrelation getCorrelation() {

        return correlation;
    }

    public void setCorrelation(PublishEventAttributesCorrelation correlation) {

        this.correlation = correlation;
    }

    public PublishEventAttributesPayload getPayload() {

        return payload;
    }

    public void setPayload(PublishEventAttributesPayload payload) {

        this.payload = payload;
    }

    public PublishEventAttributesArbitrary getArbitrary() {

        return arbitrary;
    }

    public void setArbitrary(PublishEventAttributesArbitrary arbitrary) {

        this.arbitrary = arbitrary;
    }
}
