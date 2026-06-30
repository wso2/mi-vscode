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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Params;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Properties;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.Property;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class PropertyInjector {

    private static final Logger LOGGER = Logger.getLogger(PropertyInjector.class.getName());

    public static void injectProperties(MediatorInfo oldInfo, MediatorInfo newInfo, Consumer<JsonObject> sendCommand) {

        if (oldInfo == null || newInfo == null) {
            return;
        }
        MediatorInfo injectInfo = new MediatorInfo();

        // Update payload
        filterPayload(newInfo::getPayload, oldInfo::getPayload, injectInfo::setPayload);

        // Update different property collections
        filterPropertyCollection(newInfo::getVariables, oldInfo::getVariables, injectInfo::addVariable);
        filterPropertyCollection(newInfo.getProperties()::getSynapse, oldInfo.getProperties()::getSynapse,
                injectInfo::addSynapseProperty);
        filterPropertyCollection(newInfo.getProperties()::getAxis2, oldInfo.getProperties()::getAxis2,
                injectInfo::addAxis2Property);
        filterPropertyCollection(newInfo.getProperties()::getAxis2Client, oldInfo.getProperties()::getAxis2Client,
                injectInfo::addAxis2ClientProperty);
        filterPropertyCollection(newInfo.getProperties()::getAxis2Transport, oldInfo.getProperties()::getAxis2Transport,
                injectInfo::addAxis2TransportProperty);
        filterPropertyCollection(newInfo.getProperties()::getAxis2Operation, oldInfo.getProperties()::getAxis2Operation,
                injectInfo::addAxis2OperationProperty);
        filterPropertyCollection(newInfo::getHeaders, oldInfo::getHeaders, injectInfo::addHeader);
        injectProperties(injectInfo, sendCommand);
    }

    private static <T> void filterPayload(Supplier<T> newValueSupplier, Supplier<T> oldValueSupplier,
                                          Consumer<T> setter) {

        T newValue = newValueSupplier.get();
        T oldValue = oldValueSupplier.get();

        if (newValue != null && !newValue.equals(oldValue)) {
            setter.accept(newValue);
        }
    }

    private static <T> void filterPropertyCollection(Supplier<Collection<T>> newCollectionSupplier,
                                                     Supplier<Collection<T>> oldCollectionSupplier, Consumer<T> adder) {

        Collection<T> newCollection = newCollectionSupplier.get();
        Collection<T> oldCollection = oldCollectionSupplier.get();

        newCollection.stream()
                .filter(property -> !oldCollection.contains(property))
                .forEach(adder);
    }

    public static void injectProperties(MediatorInfo mediatorInfo, Consumer<JsonObject> sendCommand) {

        if (mediatorInfo == null) {
            return;
        }
        injectPayload(mediatorInfo.getPayload(), sendCommand);
        injectVariables(mediatorInfo.getVariables(), sendCommand);
        injectProperties(mediatorInfo.getProperties(), sendCommand);
        injectHeaders(mediatorInfo.getHeaders(), sendCommand);
        injectParams(mediatorInfo.getParams(), sendCommand);
    }

    private static void injectPayload(JsonPrimitive payload, Consumer<JsonObject> sendCommand) {

        if (payload == null || StringUtils.isEmpty(payload.getAsString())) {
            return;
        }
        injectProperty(sendCommand, TryOutConstants.ENVELOPE, payload.getAsString(),
                TryOutConstants.ENVELOPE, false);
    }

    private static void injectVariables(List<Property> variables, Consumer<JsonObject> sendCommand) {

        if (variables == null) {
            return;
        }
        for (Property variable : variables) {
            injectProperty(sendCommand, variable.getKey(), variable.getValue(), TryOutConstants.VARIABLE,
                    true);
        }
    }

    private static void injectProperties(Properties properties, Consumer<JsonObject> sendCommand) {

        if (properties == null) {
            return;
        }
        for (Property property : properties.getSynapse()) {
            injectProperty(sendCommand, property.getKey(), property.getValue(), TryOutConstants.DEFAULT,
                    false);
        }
        for (Property property : properties.getAxis2()) {
            injectProperty(sendCommand, property.getKey(), property.getValue(), TryOutConstants.AXIS2,
                    false);
        }
        for (Property property : properties.getAxis2Client()) {
            injectProperty(sendCommand, property.getKey(), property.getValue(), TryOutConstants.AXIS2_CLIENT,
                    false);
        }
        for (Property property : properties.getAxis2Transport()) {
            injectProperty(sendCommand, property.getKey(), property.getValue(), TryOutConstants.AXIS2_TRANSPORT,
                    false);
        }
        for (Property property : properties.getAxis2Operation()) {
            injectProperty(sendCommand, property.getKey(), property.getValue(), TryOutConstants.AXIS2_OPERATION,
                    false);
        }
    }

    private static void injectHeaders(List<Property> headers, Consumer<JsonObject> sendCommand) {

        if (headers == null) {
            return;
        }
        for (Property header : headers) {
            injectProperty(sendCommand, header.getKey(), header.getValue(), TryOutConstants.TRANSPORT, false);
        }
    }

    private static void injectParams(Params params, Consumer<JsonObject> sendCommand) {

        if (params == null) {
            return;
        }
        // TODO: Implement the params injection if needed
    }

    private static void injectProperty(Consumer<JsonObject> sendCommand, String key, String value, String context,
                                       boolean isVariable) {

        if (StringUtils.isEmpty(key) || value == null) {
            return;
        }
        JsonObject property = new JsonObject();
        property.addProperty(TryOutConstants.COMMAND, TryOutConstants.SET);
        property.addProperty(TryOutConstants.COMMAND_ARGUMENT,
                isVariable ? TryOutConstants.VARIABLE : TryOutConstants.PROPERTY);
        property.addProperty(TryOutConstants.CONTEXT, context);
        JsonObject propertyJson = new JsonObject();
        propertyJson.addProperty(isVariable ? TryOutConstants.VARIABLE_NAME : TryOutConstants.PROPERTY_NAME, key);
        propertyJson.addProperty(isVariable ? TryOutConstants.VARIABLE_VALUE : TryOutConstants.PROPERTY_VALUE, value);
        property.add(isVariable ? TryOutConstants.VARIABLE : TryOutConstants.PROPERTY, propertyJson);
        sendCommand.accept(property);
    }
}
