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

import org.eclipse.lemminx.customservice.synapse.expression.ExpressionValidator;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for semantic expression validation via ExpressionValidator.validate().
 * Covers function argument count/type validation, filter expression warnings,
 * and the warning flag preservation fix.
 */
public class ExpressionValidatorTest {

    // ===== Function argument count validation =====

    @Test
    public void testValidFunctionCallNoErrors() {
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper(\"hello\")");
        assertTrue(errors.isEmpty(), "toUpper with 1 string arg should produce no errors");
    }

    @Test
    public void testMultiOverloadSameArityAcceptsValidType() {
        // length has two overloads (string and array categories), both arity 1.
        // A string literal should be accepted by the string overload.
        List<ExpressionError> errors = ExpressionValidator.validate("length(\"hello\")");
        assertTrue(errors.isEmpty(), "length with string arg should match the string overload");
    }

    @Test
    public void testFunctionTooFewArguments() {
        List<ExpressionError> errors = ExpressionValidator.validate("subString(\"hello\")");
        assertFalse(errors.isEmpty(), "subString with 1 arg should produce an error");
        assertTrue(errors.get(0).getMessage().contains("expects 2-3 argument(s) but received 1"),
                "Error message should indicate expected vs actual arg count: " + errors.get(0).getMessage());
    }

    @Test
    public void testFunctionTooManyArguments() {
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper(\"a\", \"b\")");
        assertFalse(errors.isEmpty(), "toUpper with 2 args should produce an error");
        assertTrue(errors.get(0).getMessage().contains("expects 1 argument(s) but received 2"),
                "Error message should indicate 1 expected but 2 received: " + errors.get(0).getMessage());
    }

    @Test
    public void testFunctionOverloadBothAritiesValid() {
        List<ExpressionError> errors2 = ExpressionValidator.validate("subString(\"x\", 1)");
        assertTrue(errors2.isEmpty(), "subString with 2 args should be valid");

        List<ExpressionError> errors3 = ExpressionValidator.validate("subString(\"x\", 1, 3)");
        assertTrue(errors3.isEmpty(), "subString with 3 args should be valid");
    }

    @Test
    public void testFunctionZeroArgsWhenOneExpected() {
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper()");
        assertFalse(errors.isEmpty(), "toUpper with 0 args should produce an error");
        assertTrue(errors.get(0).getMessage().contains("expects 1 argument(s) but received 0"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testNowNoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("now()");
        assertTrue(errors.isEmpty(), "now() with 0 args should be valid");
    }

    @Test
    public void testNowWithArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("now(\"x\")");
        assertFalse(errors.isEmpty(), "now() with 1 arg should produce an error");
        assertTrue(errors.get(0).getMessage().contains("expects 0 argument(s) but received 1"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testReplaceMissingThirdArg() {
        List<ExpressionError> errors = ExpressionValidator.validate("replace(\"s\", \"old\")");
        assertFalse(errors.isEmpty(), "replace with 2 args should produce an error (expects 3)");
        assertTrue(errors.get(0).getMessage().contains("expects 3 argument(s) but received 2"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testPowMissingSecondArg() {
        List<ExpressionError> errors = ExpressionValidator.validate("pow(2)");
        assertFalse(errors.isEmpty(), "pow with 1 arg should produce an error (expects 2)");
        assertTrue(errors.get(0).getMessage().contains("expects 2 argument(s) but received 1"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testLengthTooManyArgs() {
        // length has two overloads (string and array categories) but both are arity 1
        List<ExpressionError> errors = ExpressionValidator.validate("length(\"a\", \"b\", \"c\")");
        assertFalse(errors.isEmpty(), "length with 3 args should produce an error");
    }

    // ===== Literal argument type validation =====

    @Test
    public void testStringLiteralWhereNumberExpected() {
        List<ExpressionError> errors = ExpressionValidator.validate("abs(\"hello\")");
        assertFalse(errors.isEmpty(), "abs with string literal should produce a type error");
        assertTrue(errors.get(0).getMessage().contains("expects argument 1 to be"),
                "Error message should indicate type mismatch: " + errors.get(0).getMessage());
    }

    @Test
    public void testNumberLiteralWhereStringExpected() {
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper(123)");
        assertFalse(errors.isEmpty(), "toUpper with number literal should produce a type error");
        assertTrue(errors.get(0).getMessage().contains("expects argument 1 to be string but received number"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testCompatibleNumberAndInteger() {
        // subString(string, integer[, integer]) — number literal 1 is compatible with integer
        List<ExpressionError> errors = ExpressionValidator.validate("subString(\"x\", 1)");
        assertTrue(errors.isEmpty(), "Number literal should be compatible with integer param type");
    }

    @Test
    public void testAnyTypeAcceptsAll() {
        // exists(expression) accepts "any" type
        List<ExpressionError> errorsStr = ExpressionValidator.validate("exists(\"hello\")");
        assertTrue(errorsStr.isEmpty(), "exists should accept string literal");

        List<ExpressionError> errorsNum = ExpressionValidator.validate("exists(42)");
        assertTrue(errorsNum.isEmpty(), "exists should accept number literal");

        List<ExpressionError> errorsBool = ExpressionValidator.validate("exists(true)");
        assertTrue(errorsBool.isEmpty(), "exists should accept boolean literal");
    }

    @Test
    public void testNonLiteralArgSkipsTypeCheck() {
        // abs(payload.price) — payload access is non-literal, type check skipped
        List<ExpressionError> errors = ExpressionValidator.validate("abs(payload.price)");
        assertTrue(errors.isEmpty(), "Non-literal arg should skip type checking");
    }

    @Test
    public void testBooleanLiteralWhereStringExpected() {
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper(true)");
        assertFalse(errors.isEmpty(), "toUpper with boolean literal should produce a type error");
        assertTrue(errors.get(0).getMessage().contains("expects argument 1 to be string but received boolean"),
                "Error message: " + errors.get(0).getMessage());
    }

    // ===== Warning flag preservation (C1 fix) =====

    @Test
    public void testWarningFlagPreserved() {
        // Filter expressions with issues produce warnings (not errors).
        // payload[?(@ > >)] has consecutive operators, triggers addFilterWarning with warning=true
        List<ExpressionError> errors = ExpressionValidator.validate("payload[?(@ > >)]");
        assertFalse(errors.isEmpty(), "Filter expression should produce at least one diagnostic");
        boolean hasWarning = errors.stream().anyMatch(ExpressionError::isWarning);
        assertTrue(hasWarning, "Filter expression warnings should have isWarning()=true");
    }

    // ===== Hardcoded function overrides =====

    @Test
    public void testRoundOneArg() {
        List<ExpressionError> errors = ExpressionValidator.validate("round(3.5)");
        assertTrue(errors.isEmpty(), "round with 1 arg should be valid");
    }

    @Test
    public void testRoundTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("round(3.14159, 2)");
        assertTrue(errors.isEmpty(), "round with 2 args should be valid");
    }

    @Test
    public void testBooleanOneArg() {
        // Tests the C4 fix: boolean was loaded with arity 0 from functions.json,
        // now corrected to arity 1 via hardcoded override
        List<ExpressionError> errors = ExpressionValidator.validate("boolean(\"true\")");
        assertTrue(errors.isEmpty(), "boolean with 1 arg should be valid (C4 fix)");
    }

    @Test
    public void testBooleanNoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("boolean()");
        assertFalse(errors.isEmpty(), "boolean with 0 args should produce an error");
        assertTrue(errors.get(0).getMessage().contains("expects 1 argument(s) but received 0"),
                "Error message: " + errors.get(0).getMessage());
    }

    @Test
    public void testPropertySecondaryFunctionOneArg() {
        // xpath("//foo").property("name") — secondary function with 1 arg
        List<ExpressionError> errors = ExpressionValidator.validate("xpath(\"//foo\").property(\"name\")");
        assertTrue(errors.isEmpty(), "property secondary function with 1 arg should be valid");
    }

    @Test
    public void testPropertySecondaryFunctionTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("xpath(\"//foo\").property(\"name\", \"scope\")");
        assertTrue(errors.isEmpty(), "property secondary function with 2 args should be valid");
    }

    // ===== Syntax vs semantic =====

    @Test
    public void testSyntaxErrorShortCircuitsSemanticValidation() {
        // Unclosed paren should produce syntax error, not semantic error
        List<ExpressionError> errors = ExpressionValidator.validate("toUpper(\"hello\"");
        assertFalse(errors.isEmpty(), "Unclosed paren should produce an error");
        // Syntax errors don't have the "expects N argument(s)" format
        assertFalse(errors.get(0).getMessage().contains("expects"),
                "Should be a syntax error, not a semantic argument count error");
    }

    @Test
    public void testValidComplexExpression() {
        // A complex but valid expression should produce no errors
        List<ExpressionError> errors = ExpressionValidator.validate(
                "subString(toUpper(payload.name), 0, length(payload.name))");
        assertTrue(errors.isEmpty(), "Complex valid expression should produce no errors");
    }

    // ===== P3-43: functions.json completeness integration tests =====

    @Test
    public void testFormatDateTimeTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate(
                "formatDateTime(\"2026-01-01\", \"yyyy-MM-dd\")");
        assertTrue(errors.isEmpty(), "formatDateTime with 2 args should be valid");
    }

    @Test
    public void testFormatDateTimeThreeArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate(
                "formatDateTime(\"2026-01-01\", \"yyyy-MM-dd\", \"dd/MM/yyyy\")");
        assertTrue(errors.isEmpty(), "formatDateTime with 3 args should be valid");
    }

    @Test
    public void testLogTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate("log(100, 10)");
        assertTrue(errors.isEmpty(), "log(number, number) should be valid");
    }

    @Test
    public void testRegistryTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate(
                "registry(\"conf:/path\", \"propKey\")");
        assertTrue(errors.isEmpty(), "registry(string, string) should be valid");
    }

    @Test
    public void testXpathTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate(
                "xpath(\"//element\", \"varName\")");
        assertTrue(errors.isEmpty(), "xpath(string, string) should be valid");
    }

    @Test
    public void testUrlEncodeTwoArgs() {
        List<ExpressionError> errors = ExpressionValidator.validate(
                "urlEncode(\"hello world\", \"UTF-8\")");
        assertTrue(errors.isEmpty(), "urlEncode(string, string) should be valid");
    }

    @Test
    public void testBooleanZeroArgsFails() {
        List<ExpressionError> errors = ExpressionValidator.validate("boolean()");
        assertFalse(errors.isEmpty(), "boolean() with no args should fail");
    }

    // ===== P3-40: Division by zero detection =====

    @Test
    public void testDivisionByLiteralZero() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload.x / 0");
        assertEquals(1, errors.size(), "Division by literal 0 should produce a warning");
        assertTrue(errors.get(0).getMessage().contains("Division by zero"));
        assertTrue(errors.get(0).isWarning(), "Division by zero should be a warning, not an error");
    }

    @Test
    public void testModuloByLiteralZero() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload.x % 0");
        assertEquals(1, errors.size(), "Modulo by literal 0 should produce a warning");
        assertTrue(errors.get(0).getMessage().contains("Modulo by zero"));
        assertTrue(errors.get(0).isWarning());
    }

    @Test
    public void testDivisionByNonZero() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload.x / 5");
        assertTrue(errors.isEmpty(), "Division by non-zero should produce no warnings");
    }

    @Test
    public void testDivisionByDynamicValue() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload.x / payload.y");
        assertTrue(errors.isEmpty(), "Division by dynamic value should not be flagged");
    }

    @Test
    public void testDivisionByZeroPointZero() {
        List<ExpressionError> errors = ExpressionValidator.validate("10 / 0.0");
        assertEquals(1, errors.size(), "Division by 0.0 should produce a warning");
        assertTrue(errors.get(0).getMessage().contains("Division by zero"));
    }

    // ===== Unary NOT operator support =====

    @Test
    public void testUnaryNotBoolean() {
        List<ExpressionError> errors = ExpressionValidator.validate("!true");
        assertTrue(errors.isEmpty(), "!true should be valid syntax");
    }

    @Test
    public void testUnaryNotPayloadAccess() {
        List<ExpressionError> errors = ExpressionValidator.validate("!payload.active");
        assertTrue(errors.isEmpty(), "!payload.active should be valid syntax");
    }

    @Test
    public void testUnaryNotParenthesizedExpression() {
        List<ExpressionError> errors = ExpressionValidator.validate("!(payload.x > 5)");
        assertTrue(errors.isEmpty(), "!(payload.x > 5) should be valid syntax");
    }

    @Test
    public void testDoubleNegation() {
        List<ExpressionError> errors = ExpressionValidator.validate("!!true");
        assertTrue(errors.isEmpty(), "!!true should be valid syntax");
    }

    // ===== Arithmetic operand type checking =====

    @Test
    public void testStringPlusNumberNoWarning() {
        // String concatenation with + is valid
        List<ExpressionError> errors = ExpressionValidator.validate("\"hello\" + 123");
        assertTrue(errors.isEmpty(), "String + number should be valid (string concatenation)");
    }

    @Test
    public void testStringMinusNumberWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("\"hello\" - 123");
        assertFalse(errors.isEmpty(), "String - number should produce a warning");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("cannot be applied to string")),
                "Should warn about string with minus operator");
        assertTrue(errors.stream().allMatch(ExpressionError::isWarning), "Should be a warning, not an error");
    }

    @Test
    public void testStringMultiplyWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("\"hello\" * 2");
        assertFalse(errors.isEmpty(), "String * number should produce a warning");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("cannot be applied to string")),
                "Should warn about string with multiply operator");
    }

    @Test
    public void testBooleanPlusNumberWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("true + 1");
        assertFalse(errors.isEmpty(), "Boolean + number should produce a warning");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("cannot be applied to boolean")),
                "Should warn about boolean with plus operator");
    }

    @Test
    public void testBooleanMinusBooleanWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("true - false");
        assertFalse(errors.isEmpty(), "Boolean - boolean should produce warnings");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("cannot be applied to boolean")),
                "Should warn about boolean with minus operator");
    }

    @Test
    public void testNumberPlusNumberNoWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("5 + 3");
        assertTrue(errors.isEmpty(), "Number + number should produce no warnings");
    }

    @Test
    public void testDynamicValuePlusStringNoWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload.x + \"str\"");
        assertTrue(errors.isEmpty(), "Dynamic value + string should not be flagged");
    }

    @Test
    public void testBooleanDivisionWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("true / 2");
        assertFalse(errors.isEmpty(), "Boolean / number should produce a warning");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("cannot be applied to boolean")),
                "Should warn about boolean with division operator");
    }

    // ===== Array index type validation =====

    @Test
    public void testFloatArrayIndexWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload[1.5]");
        assertFalse(errors.isEmpty(), "Float array index should produce a warning");
        assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("floating-point")),
                "Should warn about floating-point array index");
        assertTrue(errors.stream().allMatch(ExpressionError::isWarning), "Should be a warning, not an error");
    }

    @Test
    public void testIntegerArrayIndexNoWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload[1]");
        assertTrue(errors.isEmpty(), "Integer array index should produce no warnings");
    }

    @Test
    public void testZeroArrayIndexNoWarning() {
        List<ExpressionError> errors = ExpressionValidator.validate("payload[0]");
        assertTrue(errors.isEmpty(), "Zero array index should produce no warnings");
    }
}
