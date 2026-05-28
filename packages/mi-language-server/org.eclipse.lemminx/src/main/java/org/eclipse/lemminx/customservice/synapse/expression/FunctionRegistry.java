/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.eclipse.lemminx.customservice.synapse.expression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton registry that loads function signatures from functions.json and provides
 * lookup by function name. Supports overloaded functions (multiple signatures per name).
 */
public class FunctionRegistry {

    private static final Logger LOGGER = Logger.getLogger(FunctionRegistry.class.getName());
    private static final String FUNCTIONS_JSON_PATH = "org/eclipse/lemminx/expression/functions.json";

    private static FunctionRegistry instance;
    private final Map<String, List<FunctionSignature>> functionMap;

    private FunctionRegistry() {
        functionMap = new HashMap<>();
        loadFunctions();
    }

    public static synchronized FunctionRegistry getInstance() {
        if (instance == null) {
            instance = new FunctionRegistry();
        }
        return instance;
    }

    /**
     * Returns the list of overloaded signatures for a given function name,
     * or an empty list if the function is not found.
     */
    public List<FunctionSignature> getOverloads(String functionName) {
        return functionMap.getOrDefault(functionName, Collections.emptyList());
    }

    /**
     * Returns true if the function is known.
     */
    public boolean hasFunction(String functionName) {
        return functionMap.containsKey(functionName);
    }

    /**
     * Returns a human-readable usage string for a function, e.g., "subString(string, integer[, integer])".
     */
    public String getUsageString(String functionName) {
        List<FunctionSignature> overloads = getOverloads(functionName);
        if (overloads.isEmpty()) {
            return functionName + "(...)";
        }
        if (overloads.size() == 1) {
            FunctionSignature sig = overloads.get(0);
            return functionName + "(" + String.join(", ", sig.getParamTypes()) + ")";
        }
        // Multiple overloads: show min-args version with optional params from max-args
        int minArity = overloads.stream().mapToInt(FunctionSignature::getArity).min().orElse(0);
        int maxArity = overloads.stream().mapToInt(FunctionSignature::getArity).max().orElse(0);
        FunctionSignature maxSig = overloads.stream()
                .filter(s -> s.getArity() == maxArity)
                .findFirst().orElse(overloads.get(0));

        StringBuilder sb = new StringBuilder(functionName);
        sb.append("(");
        List<String> params = maxSig.getParamTypes();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            if (i >= minArity) sb.append("[");
            sb.append(params.get(i));
            if (i >= minArity) sb.append("]");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Returns the minimum arity across all overloads for the given function.
     */
    public int getMinArity(String functionName) {
        return getOverloads(functionName).stream()
                .mapToInt(FunctionSignature::getArity)
                .min().orElse(0);
    }

    /**
     * Returns the maximum arity across all overloads for the given function.
     */
    public int getMaxArity(String functionName) {
        return getOverloads(functionName).stream()
                .mapToInt(FunctionSignature::getArity)
                .max().orElse(0);
    }

    private void loadFunctions() {
        try (InputStream is = FunctionRegistry.class.getClassLoader().getResourceAsStream(FUNCTIONS_JSON_PATH)) {
            if (is == null) {
                LOGGER.log(Level.WARNING, "Could not find functions.json at: " + FUNCTIONS_JSON_PATH);
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (Map.Entry<String, JsonElement> category : root.entrySet()) {
                JsonObject categoryObj = category.getValue().getAsJsonObject();
                if (!categoryObj.has(ExpressionConstants.ITEMS)) continue;
                for (JsonElement itemElem : categoryObj.getAsJsonArray(ExpressionConstants.ITEMS)) {
                    JsonObject item = itemElem.getAsJsonObject();
                    String label = item.get(ExpressionConstants.LABEL).getAsString();
                    String funcName = extractFunctionName(label);
                    String details = item.has(ExpressionConstants.DETAIL)
                            ? item.get(ExpressionConstants.DETAIL).getAsString() : "";

                    List<String> paramTypes = new ArrayList<>();
                    if (item.has(ExpressionConstants.SIGNATURE)) {
                        JsonObject signature = item.getAsJsonObject(ExpressionConstants.SIGNATURE);
                        for (Map.Entry<String, JsonElement> param : signature.entrySet()) {
                            paramTypes.add(param.getValue().getAsString());
                        }
                    }

                    FunctionSignature sig = new FunctionSignature(funcName, paramTypes, details);
                    functionMap.computeIfAbsent(funcName, k -> new ArrayList<>()).add(sig);
                }
            }

            // Add 'property' secondary function (1-2 args) — this is a SECONDARY_FUNCTIONS token
            // in the ANTLR grammar, not a regular FUNCTION, so it's not in functions.json.
            if (!functionMap.containsKey("property")) {
                List<FunctionSignature> propSigs = new ArrayList<>();
                propSigs.add(new FunctionSignature("property", List.of("string"),
                        "Get property value"));
                propSigs.add(new FunctionSignature("property", List.of("string", "string"),
                        "Get property value with scope"));
                functionMap.put("property", propSigs);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading functions.json", e);
        }
    }

    private static String extractFunctionName(String label) {
        int parenIndex = label.indexOf('(');
        if (parenIndex > 0) {
            return label.substring(0, parenIndex).trim();
        }
        return label.trim();
    }
}
