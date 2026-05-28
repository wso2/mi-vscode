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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.failover.EndpointFailover;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.loadbalance.EndpointLoadbalance;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.recipientList.EndpointRecipientlist;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.wsdl.WSDLEndpoint;

public class Endpoint extends STNode {

    DefaultEndpoint _default;
    EndpointHttp http;
    EndpointAddress address;
    WSDLEndpoint wsdl;
    EndpointLoadbalance loadbalance;
    EndpointSession session;
    EndpointFailover failover;
    EndpointRecipientlist recipientlist;
    EndpointProperty[] property;
    EndpointParameter[] parameter;
    String description;
    String key;
    String keyExpression;
    String template;
    String uri;
    EndpointType type;

    public DefaultEndpoint get_default() {

        return _default;
    }

    public void set_default(DefaultEndpoint _default) {

        this._default = _default;
    }

    public EndpointHttp getHttp() {

        return http;
    }

    public void setHttp(EndpointHttp http) {

        this.http = http;
    }

    public EndpointAddress getAddress() {

        return address;
    }

    public void setAddress(EndpointAddress address) {

        this.address = address;
    }

    public WSDLEndpoint getWsdl() {

        return wsdl;
    }

    public void setWsdl(WSDLEndpoint wsdl) {

        this.wsdl = wsdl;
    }

    public EndpointLoadbalance getLoadbalance() {

        return loadbalance;
    }

    public void setLoadbalance(EndpointLoadbalance loadbalance) {

        this.loadbalance = loadbalance;
    }

    public EndpointSession getSession() {

        return session;
    }

    public void setSession(EndpointSession session) {

        this.session = session;
    }

    public EndpointFailover getFailover() {

        return failover;
    }

    public void setFailover(EndpointFailover failover) {

        this.failover = failover;
    }

    public EndpointRecipientlist getRecipientlist() {

        return recipientlist;
    }

    public void setRecipientlist(EndpointRecipientlist recipientlist) {

        this.recipientlist = recipientlist;
    }

    public EndpointProperty[] getProperty() {

        return property;
    }

    public void setProperty(EndpointProperty[] property) {

        this.property = property;
    }

    public EndpointParameter[] getParameter() {

        return parameter;
    }

    public void setParameter(EndpointParameter[] parameter) {

        this.parameter = parameter;
    }

    public String getKey() {

        return key;
    }

    public void setKey(String key) {

        this.key = key;
    }

    public String getKeyExpression() {

        return keyExpression;
    }

    public void setKeyExpression(String keyExpression) {

        this.keyExpression = keyExpression;
    }

    public String getTemplate() {

        return template;
    }

    public void setTemplate(String template) {

        this.template = template;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public EndpointType getType() {

        return type;
    }

    public void setType(EndpointType type) {

        this.type = type;
    }
}
