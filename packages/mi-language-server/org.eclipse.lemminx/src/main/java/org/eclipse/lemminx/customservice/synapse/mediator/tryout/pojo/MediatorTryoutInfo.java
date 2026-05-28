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

import java.util.List;

public class MediatorTryoutInfo {

    private String id;
    private MediatorInfo input;
    private MediatorInfo output;
    private String error;

    public MediatorTryoutInfo() {

        input = new MediatorInfo();
        output = new MediatorInfo();
    }

    public MediatorTryoutInfo(String id, MediatorInfo input, MediatorInfo output) {

        this.input = input;
        this.output = output;
        this.id = id;
    }

    public MediatorTryoutInfo(String id, MediatorInfo input, String error) {

        this.id = id;
        this.input = input;
        this.error = error;
    }

    public MediatorTryoutInfo(MediatorInfo input, MediatorInfo output) {

        this.input = input;
        this.output = output;
    }

    public MediatorTryoutInfo(String error) {

        this.error = error;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public MediatorInfo getInput() {

        if (input != null) {
            return input.deepCopy();
        }
        return null;
    }

    public MediatorInfo getOutput() {

        if (output != null) {
            return output.deepCopy();
        }
        return null;
    }

    public void setError(String error) {

        this.error = error;
    }

    public String getError() {

        return error;
    }

    public void setInputPayload(JsonPrimitive jsonPrimitive) {

        input.setPayload(jsonPrimitive);
    }

    public void addInputSynapseProperties(Property property) {

        input.addSynapseProperty(property);
    }

    public void addInputAxis2Properties(Property property) {

        input.addAxis2Property(property);
    }

    public void addInputAxis2ClientProperties(Property property) {

        input.addAxis2ClientProperty(property);
    }

    public void addInputAxis2TransportProperties(Property property) {

        input.addAxis2TransportProperty(property);
    }

    public void addInputAxis2OperationProperties(Property property) {

        input.addAxis2OperationProperty(property);
    }

    public void setOutputPayload(JsonPrimitive jsonPrimitive) {

        output.setPayload(jsonPrimitive);
    }

    public void addOutputSynapseProperties(Property property) {

        output.addSynapseProperty(property);
    }

    public void addOutputAxis2Properties(Property property) {

        output.addAxis2Property(property);
    }

    public void addOutputAxis2ClientProperties(Property property) {

        output.addAxis2ClientProperty(property);
    }

    public void addOutputAxis2TransportProperties(Property property) {

        output.addAxis2TransportProperty(property);
    }

    public void addOutputAxis2OperationProperties(Property property) {

        output.addAxis2OperationProperty(property);
    }

    public void addOutputVariable(Property property) {

        output.addVariable(property);
    }

    public void addOutputVariable(String name, String value) {

        output.addVariable(new Property(name, value));
    }

    public void setInputConfigs(List<Property> configs) {

        input.setConfigs(configs);
    }

    public void setOutputConfigs(List<Property> configs) {

        output.setConfigs(configs);
    }

    public void removeOutputVariable(String name) {

        output.removeVariable(name);
    }

    public void addOutputHeader(Property property) {

        output.addHeader(property);
    }

    public void removeOutputHeader(String name) {

        output.removeHeader(name);
    }

    public void setInputParams(Params params) {

        input.setParams(params);
    }

    public void setOutputParams(Params params) {

        output.setParams(params);
    }

    public void replaceInputWithOutput() {

        input = output.deepCopy();
    }

    @Override
    public String toString() {

        return "MediatorTryoutInfo{" +
                "id='" + id + '\'' +
                ", input=" + input +
                ", output=" + output +
                ", error='" + error + '\'' +
                '}';
    }
}
