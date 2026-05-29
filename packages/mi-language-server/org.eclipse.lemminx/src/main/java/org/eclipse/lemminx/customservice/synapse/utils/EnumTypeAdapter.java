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

package org.eclipse.lemminx.customservice.synapse.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class EnumTypeAdapter implements JsonSerializer<Enum<?>>, JsonDeserializer<Enum<?>> {

    @Override
    public Enum<?> deserialize(JsonElement jsonElement, Type type,
                               JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        return Utils.getEnumFromValue(jsonElement.getAsString(), (Class<Enum>) type);
    }

    @Override
    public JsonElement serialize(Enum<?> anEnum, Type type, JsonSerializationContext jsonSerializationContext) {

        try {
            Method method = anEnum.getClass().getDeclaredMethod("getValue");
            String value = (String) method.invoke(anEnum);
            return jsonSerializationContext.serialize(value);
        } catch (Exception e) {
        }
        return jsonSerializationContext.serialize(anEnum.name());
    }
}
