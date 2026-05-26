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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.MediatorProperty;

public class EndpointProperty extends MediatorProperty {

    EndpointPropertyScope scope;

    public EndpointProperty(MediatorProperty mediatorProperty) {

        setName(mediatorProperty.getName());
        setValue(mediatorProperty.getValue());
        setExpression(mediatorProperty.getExpression());
        setOtherAttributes(mediatorProperty.getOtherAttributes());
        setContent(mediatorProperty.getContent());
    }

    public EndpointPropertyScope getScope() {

        return scope;
    }

    public void setScope(EndpointPropertyScope scope) {

        this.scope = scope;
    }
}
