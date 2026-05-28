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

package org.eclipse.lemminx.synapse.expression;

import org.eclipse.lemminx.customservice.synapse.expression.FunctionRegistry;
import org.eclipse.lemminx.customservice.synapse.expression.FunctionSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FunctionRegistry: loading from functions.json, hardcoded overrides,
 * arity calculations, and usage string formatting.
 */
public class FunctionRegistryTest {

    private final FunctionRegistry registry = FunctionRegistry.getInstance();

    @Test
    public void testLoadsFromFunctionsJson() {
        assertTrue(registry.hasFunction("length"), "length should be loaded from functions.json");
        assertTrue(registry.hasFunction("toUpper"), "toUpper should be loaded");
        assertTrue(registry.hasFunction("toLower"), "toLower should be loaded");
        assertTrue(registry.hasFunction("abs"), "abs should be loaded");
        assertTrue(registry.hasFunction("now"), "now should be loaded");
    }

    @Test
    public void testUnknownFunctionReturnsFalse() {
        assertFalse(registry.hasFunction("nonExistentFunction"));
        assertFalse(registry.hasFunction(""));
        assertFalse(registry.hasFunction("fooBar123"));
    }

    @Test
    public void testGetOverloadsSubString() {
        List<FunctionSignature> overloads = registry.getOverloads("subString");
        assertNotNull(overloads);
        assertEquals(2, overloads.size(), "subString should have 2 overloads (2-arg and 3-arg)");
    }

    @Test
    public void testMinMaxArityOverloaded() {
        assertEquals(2, registry.getMinArity("subString"));
        assertEquals(3, registry.getMaxArity("subString"));
    }

    @Test
    public void testMinMaxAritySingleOverload() {
        assertEquals(1, registry.getMinArity("toUpper"));
        assertEquals(1, registry.getMaxArity("toUpper"));
    }

    @Test
    public void testMinMaxArityZeroArgs() {
        assertEquals(0, registry.getMinArity("now"));
        assertEquals(0, registry.getMaxArity("now"));
    }

    @Test
    public void testGetUsageStringSingle() {
        String usage = registry.getUsageString("toUpper");
        assertEquals("toUpper(string)", usage);
    }

    @Test
    public void testGetUsageStringOverloaded() {
        String usage = registry.getUsageString("subString");
        // Should show optional param in brackets: subString(string, integer, [integer])
        assertTrue(usage.contains("subString("), "Usage should start with function name");
        assertTrue(usage.contains("["), "Usage should show optional params in brackets");
    }

    @Test
    public void testGetUsageStringUnknown() {
        String usage = registry.getUsageString("unknownFunc");
        assertEquals("unknownFunc(...)", usage);
    }

    @Test
    public void testOverloadsReturnsEmptyListForUnknown() {
        List<FunctionSignature> overloads = registry.getOverloads("unknownFunc");
        assertNotNull(overloads, "Should return empty list, not null");
        assertTrue(overloads.isEmpty());
    }

    @Test
    public void testMinMaxArityUnknownFunction() {
        assertEquals(0, registry.getMinArity("unknownFunc"));
        assertEquals(0, registry.getMaxArity("unknownFunc"));
    }

    // ===== Hardcoded overrides =====

    @Test
    public void testHardcodedRoundOverloads() {
        List<FunctionSignature> overloads = registry.getOverloads("round");
        assertNotNull(overloads);
        assertEquals(2, overloads.size(), "round should have 2 overloads (1-arg and 2-arg)");
        boolean has1Arg = overloads.stream().anyMatch(s -> s.getArity() == 1);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        assertTrue(has1Arg, "round should have a 1-arg overload");
        assertTrue(has2Arg, "round should have a 2-arg overload");
    }

    @Test
    public void testHardcodedBooleanFix() {
        List<FunctionSignature> overloads = registry.getOverloads("boolean");
        assertNotNull(overloads);
        assertEquals(1, overloads.size());
        assertEquals(1, overloads.get(0).getArity(), "boolean should have arity 1 (not 0)");
        assertEquals("any", overloads.get(0).getParamTypes().get(0),
                "boolean param type should be 'any'");
    }

    @Test
    public void testHardcodedPropertyOverloads() {
        List<FunctionSignature> overloads = registry.getOverloads("property");
        assertNotNull(overloads);
        assertEquals(2, overloads.size(), "property should have 2 overloads (1-arg and 2-arg)");
        boolean has1Arg = overloads.stream().anyMatch(s -> s.getArity() == 1);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        assertTrue(has1Arg, "property should have a 1-arg overload");
        assertTrue(has2Arg, "property should have a 2-arg overload");
    }

    @Test
    public void testHardcodedUrlEncodeOverloads() {
        List<FunctionSignature> overloads = registry.getOverloads("urlEncode");
        assertNotNull(overloads);
        assertTrue(overloads.size() >= 2, "urlEncode should have at least 2 overloads");
    }

    @Test
    public void testParamTypesCorrectForReplace() {
        List<FunctionSignature> overloads = registry.getOverloads("replace");
        assertNotNull(overloads);
        assertFalse(overloads.isEmpty());
        FunctionSignature sig = overloads.get(0);
        assertEquals(3, sig.getArity());
        assertEquals(List.of("string", "string", "string"), sig.getParamTypes());
    }

    @Test
    public void testXpathHasTwoArgOverload() {
        List<FunctionSignature> overloads = registry.getOverloads("xpath");
        assertNotNull(overloads);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        assertTrue(has2Arg, "xpath should have a 2-arg overload");
    }

    @Test
    public void testRegistryHasTwoArgOverload() {
        List<FunctionSignature> overloads = registry.getOverloads("registry");
        assertNotNull(overloads);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        assertTrue(has2Arg, "registry should have a 2-arg overload");
    }

    // ===== P3-43: functions.json completeness =====

    @Test
    public void testFormatDateTimeOverloads() {
        List<FunctionSignature> overloads = registry.getOverloads("formatDateTime");
        assertNotNull(overloads);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        boolean has3Arg = overloads.stream().anyMatch(s -> s.getArity() == 3);
        assertTrue(has2Arg, "formatDateTime should have a 2-arg overload");
        assertTrue(has3Arg, "formatDateTime should have a 3-arg overload");
    }

    @Test
    public void testLogOverloads() {
        List<FunctionSignature> overloads = registry.getOverloads("log");
        assertNotNull(overloads);
        boolean has1Arg = overloads.stream().anyMatch(s -> s.getArity() == 1);
        boolean has2Arg = overloads.stream().anyMatch(s -> s.getArity() == 2);
        assertTrue(has1Arg, "log should have a 1-arg overload");
        assertTrue(has2Arg, "log should have a 2-arg overload");
    }

    @Test
    public void testRoundParamTypes() {
        List<FunctionSignature> overloads = registry.getOverloads("round");
        FunctionSignature oneArg = overloads.stream().filter(s -> s.getArity() == 1).findFirst().orElse(null);
        FunctionSignature twoArg = overloads.stream().filter(s -> s.getArity() == 2).findFirst().orElse(null);
        assertNotNull(oneArg);
        assertNotNull(twoArg);
        assertEquals("number", oneArg.getParamTypes().get(0));
        assertEquals("number", twoArg.getParamTypes().get(0));
        assertEquals("integer", twoArg.getParamTypes().get(1));
    }

    @Test
    public void testObjectAndArrayFunctionsLoaded() {
        assertTrue(registry.hasFunction("object"), "object should be loaded from functions.json");
        assertTrue(registry.hasFunction("array"), "array should be loaded from functions.json");
        assertEquals(1, registry.getMinArity("object"));
        assertEquals(1, registry.getMinArity("array"));
    }

    @Test
    public void testExistsFunctionLoaded() {
        assertTrue(registry.hasFunction("exists"), "exists should be loaded from functions.json");
        assertEquals(1, registry.getMinArity("exists"));
        assertEquals("any", registry.getOverloads("exists").get(0).getParamTypes().get(0));
    }
}
