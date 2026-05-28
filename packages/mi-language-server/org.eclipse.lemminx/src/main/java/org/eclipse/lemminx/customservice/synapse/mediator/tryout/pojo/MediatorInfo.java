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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MediatorInfo {

    private JsonPrimitive payload;
    private List<Property> variables;
    private List<Property> headers;
    private Properties properties;
    private Params params;
    private List<Property> configs;

    public MediatorInfo() {

        payload = new JsonPrimitive(StringUtils.EMPTY);
        variables = new ArrayList<>();
        headers = new ArrayList<>();
        properties = new Properties();
        params = new Params();
        configs = new ArrayList<>();
    }

    public void addSynapseProperties(List<Property> properties) {

        this.properties.addSynapseProperties(properties);
    }

    public void addAxis2Properties(List<Property> properties) {

        this.properties.addAxis2Properties(properties);
    }

    public void addAxis2ClientProperties(List<Property> properties) {

        this.properties.addAxis2ClientProperties(properties);
    }

    public void addAxis2TransportProperties(List<Property> properties) {

        this.properties.addAxis2TransportProperties(properties);
        headers.addAll(properties);
    }

    public void addAxis2OperationProperties(List<Property> properties) {

        this.properties.addAxis2OperationProperties(properties);
    }

    public void addVariables(List<Property> properties) {

        variables.addAll(properties);
    }

    public void addSynapseProperty(Property property) {

        properties.addSynapseProperty(property);
    }

    public void addAxis2Property(Property property) {

        properties.addAxis2Property(property);
    }

    public void addAxis2ClientProperty(Property property) {

        properties.addAxis2ClientProperty(property);
    }

    public void addAxis2TransportProperty(Property property) {

        properties.addAxis2TransportProperty(property);
        headers.add(property);
    }

    public void addAxis2OperationProperty(Property property) {

        properties.addAxis2OperationProperty(property);
    }

    public void addVariable(Property property) {

        variables.add(property);
    }

    public void addHeader(String key, String value) {

        headers.add(new Property(key, value));
    }

    public void addHeader(Property property) {

        headers.add(property);
    }

    public void addQueryParam(String key, String value) {

        params.addQueryParam(new Property(key, value));
    }

    public void addUriParam(String key, String value) {

        params.addPathParam(new Property(key, value));
    }

    public void addFunctionParam(String key, String value) {

        params.addFunctionParam(new Property(key, value));
    }

    public void addConfig(Property config) {

        configs.add(config);
    }

    public void setPayload(JsonPrimitive payload) {

        this.payload = payload;
    }

    public JsonPrimitive getPayload() {

        return payload;
    }

    public List<Property> getSynapse() {

        return properties.getSynapse();
    }

    public List<Property> getAxis2() {

        return properties.getAxis2();
    }

    public List<Property> getAxis2Client() {

        return properties.getAxis2Client();
    }

    public List<Property> getAxis2Transport() {

        return properties.getAxis2Transport();
    }

    public List<Property> getAxis2Operation() {

        return properties.getAxis2Operation();
    }

    public List<Property> getVariables() {

        return variables;
    }

    public List<Property> getHeaders() {

        return headers;
    }

    public Properties getProperties() {

        return properties;
    }

    public Params getParams() {

        return params;
    }

    public void setParams(Params params) {

        this.params = params;
    }

    public void setVariables(List<Property> variables) {

        this.variables = variables;
    }

    public void setHeaders(List<Property> headers) {

        this.headers = headers;
    }

    public void setProperties(Properties properties) {

        this.properties = properties;
    }

    public List<Property> getConfigs() {

        return configs;
    }

    public void setConfigs(List<Property> configs) {

        this.configs = configs;
    }

    public MediatorInfo deepCopy() {

        MediatorInfo mediatorInfo = new MediatorInfo();
        mediatorInfo.setPayload(payload.deepCopy());
        mediatorInfo.setVariables(new ArrayList<>(variables));
        mediatorInfo.setProperties(properties.deepCopy());
        mediatorInfo.setHeaders(new ArrayList<>(headers));
        mediatorInfo.setParams(params.deepCopy());
        mediatorInfo.setConfigs(new ArrayList<>(configs));
        return mediatorInfo;
    }

    private void addHeaders(List<Property> headers) {

        this.headers.addAll(headers);
    }

    public void removeHeader(String name) {

        if (headers == null) {
            return;
        }
        for (Property header : headers) {
            if (header.getKey().equals(name)) {
                headers.remove(header);
                break;
            }
        }
    }

    public void removeVariable(String name) {

        if (variables == null) {
            return;
        }
        for (Property variable : variables) {
            if (variable.getKey().equals(name)) {
                variables.remove(variable);
                break;
            }
        }
    }

    @Override
    public String toString() {

        return "MediatorInfo{" +
                "payload=" + payload +
                ", variables=" + variables +
                ", headers=" + headers +
                ", properties=" + properties +
                ", params=" + params +
                '}';
    }
}
