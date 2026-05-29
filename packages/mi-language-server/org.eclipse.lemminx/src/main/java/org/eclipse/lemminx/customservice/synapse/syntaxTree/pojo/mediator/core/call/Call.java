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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core.call;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Call extends Mediator {

    CallSource source;
    CallTarget target;
    NamedEndpoint endpoint;
    boolean blocking;
    String description;
    boolean initAxis2ClientOptions;
    String inlineEndpointXml;
    String traceFilter;

    public Call() {
        setDisplayName("Call Endpoint");
    }
    public CallSource getSource() {

        return source;
    }

    public void setSource(CallSource source) {

        this.source = source;
    }

    public CallTarget getTarget() {

        return target;
    }

    public void setTarget(CallTarget target) {

        this.target = target;
    }

    public NamedEndpoint getEndpoint() {

        return endpoint;
    }

    public void setEndpoint(NamedEndpoint endpoint) {

        this.endpoint = endpoint;
    }

    public boolean isBlocking() {

        return blocking;
    }

    public void setBlocking(boolean blocking) {

        this.blocking = blocking;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public boolean getInitAxis2ClientOptions() {

        return initAxis2ClientOptions;
    }

    public void setInitAxis2ClientOptions(boolean initAxis2ClientOptions) {

        this.initAxis2ClientOptions = initAxis2ClientOptions;
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
