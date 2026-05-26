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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Send extends Mediator {

    NamedEndpoint endpoint;
    String receive;
    boolean buildmessage;
    String description;
    String inlineEndpointXml;
    String traceFilter;

    public Send() {
        setDisplayName("Send");
    }


    public NamedEndpoint getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(NamedEndpoint endpoint) {

        this.endpoint = endpoint;
    }

    public String getReceive() {

        return receive;
    }

    public void setReceive(String receive) {

        this.receive = receive;
    }

    public boolean isBuildmessage() {

        return buildmessage;
    }

    public void setBuildmessage(boolean buildmessage) {

        this.buildmessage = buildmessage;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getInlineEndpointXml() {
        return inlineEndpointXml;
    }

    public void setInlineEndpointXml(String inlineEndpointXml) {
        this.inlineEndpointXml = inlineEndpointXml;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
