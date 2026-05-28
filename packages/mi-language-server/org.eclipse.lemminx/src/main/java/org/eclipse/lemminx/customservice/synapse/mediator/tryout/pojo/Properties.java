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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Properties {

    private List<Property> synapse;
    private List<Property> axis2;
    private List<Property> axis2Client;
    private List<Property> axis2Transport;
    private List<Property> axis2Operation;

    public Properties() {

        synapse = new ArrayList<>();
        axis2 = new ArrayList<>();
        axis2Client = new ArrayList<>();
        axis2Transport = new ArrayList<>();
        axis2Operation = new ArrayList<>();
    }

    public void addSynapseProperties(List<Property> properties) {

        synapse.addAll(properties);
    }

    public void addAxis2Properties(List<Property> properties) {

        axis2.addAll(properties);
    }

    public void addAxis2ClientProperties(List<Property> properties) {

        axis2Client.addAll(properties);
    }

    public void addAxis2TransportProperties(List<Property> properties) {

        axis2Transport.addAll(properties);
    }

    public void addAxis2OperationProperties(List<Property> properties) {

        axis2Operation.addAll(properties);
    }

    public void addSynapseProperty(Property property) {

        synapse.add(property);
    }

    public void addAxis2Property(Property property) {

        axis2.add(property);
    }

    public void addAxis2ClientProperty(Property property) {

        axis2Client.add(property);
    }

    public void addAxis2TransportProperty(Property property) {

        axis2Transport.add(property);
    }

    public void addAxis2OperationProperty(Property property) {

        axis2Operation.add(property);
    }

    public List<Property> getSynapse() {

        return Collections.unmodifiableList(synapse);
    }

    public List<Property> getAxis2() {

        return Collections.unmodifiableList(axis2);
    }

    public List<Property> getAxis2Client() {

        return Collections.unmodifiableList(axis2Client);
    }

    public List<Property> getAxis2Transport() {

        return Collections.unmodifiableList(axis2Transport);
    }

    public List<Property> getAxis2Operation() {

        return Collections.unmodifiableList(axis2Operation);
    }

    public List<Property> getPropertiesByType(Type type) {

        if (type == null) {
            return null;
        }
        switch (type) {
            case SYNAPSE:
                return synapse;
            case AXIS2:
                return axis2;
            case AXIS2_CLIENT:
                return axis2Client;
            case AXIS2_TRANSPORT:
                return axis2Transport;
            case AXIS2_OPERATION:
                return axis2Operation;
            default:
                return null;
        }
    }

    public Properties deepCopy() {

        Properties properties = new Properties();
        properties.addSynapseProperties(new ArrayList<>(synapse));
        properties.addAxis2Properties(new ArrayList<>(axis2));
        properties.addAxis2ClientProperties(new ArrayList<>(axis2Client));
        properties.addAxis2TransportProperties(new ArrayList<>(axis2Transport));
        properties.addAxis2OperationProperties(new ArrayList<>(axis2Operation));
        return properties;
    }

    @Override
    public String toString() {

        return "Properties{" +
                "synapse=" + synapse +
                ", axis2=" + axis2 +
                ", axis2Client=" + axis2Client +
                ", axis2Transport=" + axis2Transport +
                ", axis2Operation=" + axis2Operation +
                '}';
    }

    public enum Type {
        SYNAPSE,
        AXIS2,
        AXIS2_CLIENT,
        AXIS2_TRANSPORT,
        AXIS2_OPERATION
    }
}
