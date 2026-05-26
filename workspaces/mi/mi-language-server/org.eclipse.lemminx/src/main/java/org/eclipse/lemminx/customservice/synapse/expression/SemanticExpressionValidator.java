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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;
import org.eclipse.lemminx.util.synapse_expression.ExpressionLexer;
import org.eclipse.lemminx.util.synapse_expression.ExpressionParser;
import org.eclipse.lemminx.util.synapse_expression.ExpressionParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs semantic validation on Synapse expressions after ANTLR parsing succeeds.
 * Validates function argument counts and literal argument types using the FunctionRegistry.
 */
public class SemanticExpressionValidator extends ExpressionParserBaseVisitor<Void> {

    private final List<ExpressionError> errors = new ArrayList<>();
    private final FunctionRegistry registry;

    public SemanticExpressionValidator() {
        this.registry = FunctionRegistry.getInstance();
    }

    public List<ExpressionError> getErrors() {
        return errors;
    }

    @Override
    public Void visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
        Token funcToken = ctx.FUNCTIONS().getSymbol();
        String funcName = funcToken.getText();
        int line = funcToken.getLine();
        int charPos = funcToken.getCharPositionInLine();

        // Count arguments
        List<ExpressionParser.ExpressionContext> args = ctx.expression();
        int argCount = args != null ? args.size() : 0;

        if (!registry.hasFunction(funcName)) {
            // Unknown function — skip (ANTLR grammar already restricts FUNCTIONS tokens,
            // so this shouldn't normally happen)
            return visitChildren(ctx);
        }

        List<FunctionSignature> overloads = registry.getOverloads(funcName);
        boolean arityMatch = overloads.stream().anyMatch(s -> s.getArity() == argCount);

        if (!arityMatch) {
            addArityError(funcName, argCount, line, charPos, funcToken);
        } else if (args != null) {
            // Arity matches — check literal argument types against all overloads with this arity.
            // Accept the call if ANY overload is type-compatible (e.g., length has both
            // string and array overloads with arity 1).
            List<FunctionSignature> sameArityOverloads = overloads.stream()
                    .filter(s -> s.getArity() == argCount)
                    .collect(java.util.stream.Collectors.toList());
            if (sameArityOverloads.size() == 1) {
                validateArgumentTypes(funcName, sameArityOverloads.get(0), args, line, charPos);
            } else if (sameArityOverloads.size() > 1) {
                validateArgumentTypesMultiOverload(funcName, sameArityOverloads, args, line, charPos);
            }
        }

        // Also validate the suffix (secondary function call) if present
        ExpressionParser.FunctionCallSuffixContext suffix = ctx.functionCallSuffix();
        if (suffix != null && suffix.SECONDARY_FUNCTIONS() != null) {
            validateSecondaryFunction(suffix);
        }

        return visitChildren(ctx);
    }

    /**
     * Validates arity for secondary (chained) function calls like xpath("//foo").property("name").
     * Type validation is intentionally omitted: the only secondary function is 'property',
     * whose args are always strings (name and optional scope), so type mismatches are unlikely.
     */
    private void validateSecondaryFunction(ExpressionParser.FunctionCallSuffixContext ctx) {
        Token funcToken = ctx.SECONDARY_FUNCTIONS().getSymbol();
        String funcName = funcToken.getText();
        int line = funcToken.getLine();
        int charPos = funcToken.getCharPositionInLine();

        List<ExpressionParser.ExpressionContext> args = ctx.expression();
        int argCount = args != null ? args.size() : 0;

        if (registry.hasFunction(funcName)) {
            List<FunctionSignature> overloads = registry.getOverloads(funcName);
            boolean arityMatch = overloads.stream().anyMatch(s -> s.getArity() == argCount);
            if (!arityMatch) {
                addArityError(funcName, argCount, line, charPos, funcToken);
            }
        }
    }

    private void addArityError(String funcName, int argCount, int line, int charPos, Token funcToken) {
        int minArity = registry.getMinArity(funcName);
        int maxArity = registry.getMaxArity(funcName);
        String usage = registry.getUsageString(funcName);
        String expectedStr;
        if (minArity == maxArity) {
            expectedStr = String.valueOf(minArity);
        } else {
            expectedStr = minArity + "-" + maxArity;
        }
        String message = String.format(
                "Function '%s' expects %s argument(s) but received %d. Usage: %s",
                funcName, expectedStr, argCount, usage);
        errors.add(new ExpressionError(line, charPos, message, funcToken, null));
    }

    private void validateArgumentTypes(String funcName, FunctionSignature sig,
                                       List<ExpressionParser.ExpressionContext> args,
                                       int funcLine, int funcCharPos) {
        List<String> expectedTypes = sig.getParamTypes();
        for (int i = 0; i < args.size() && i < expectedTypes.size(); i++) {
            String expectedType = expectedTypes.get(i);
            if ("any".equals(expectedType)) {
                continue; // Accept any type
            }
            ExpressionParser.ExpressionContext argExpr = args.get(i);
            String literalType = getLiteralType(argExpr);
            if (literalType == null) {
                continue; // Not a literal — skip type checking
            }
            if (!isTypeCompatible(expectedType, literalType)) {
                Token argToken = argExpr.getStart();
                String message = String.format(
                        "Function '%s' expects argument %d to be %s but received %s literal '%s'.",
                        funcName, i + 1, expectedType, literalType, argExpr.getText());
                errors.add(new ExpressionError(argToken.getLine(), argToken.getCharPositionInLine(),
                        message, argToken, null));
            }
        }
    }

    /**
     * Validates argument types when multiple overloads share the same arity.
     * Accepts the call if ANY overload is fully type-compatible with the literal args.
     * Only reports an error if NO overload matches.
     */
    private void validateArgumentTypesMultiOverload(String funcName, List<FunctionSignature> overloads,
                                                    List<ExpressionParser.ExpressionContext> args,
                                                    int funcLine, int funcCharPos) {
        // Collect literal types for each arg position (null = non-literal, skip)
        String[] literalTypes = new String[args.size()];
        boolean hasAnyLiteral = false;
        for (int i = 0; i < args.size(); i++) {
            literalTypes[i] = getLiteralType(args.get(i));
            if (literalTypes[i] != null) {
                hasAnyLiteral = true;
            }
        }
        if (!hasAnyLiteral) {
            return; // No literal args to type-check
        }

        // Check if any overload is fully compatible
        for (FunctionSignature sig : overloads) {
            if (isOverloadCompatible(sig, literalTypes)) {
                return; // Found a matching overload
            }
        }

        // No overload matched — report errors against the first overload for context
        validateArgumentTypes(funcName, overloads.get(0), args, funcLine, funcCharPos);
    }

    private boolean isOverloadCompatible(FunctionSignature sig, String[] literalTypes) {
        List<String> expectedTypes = sig.getParamTypes();
        for (int i = 0; i < literalTypes.length && i < expectedTypes.size(); i++) {
            if (literalTypes[i] == null) {
                continue; // Non-literal, skip
            }
            if ("any".equals(expectedTypes.get(i))) {
                continue;
            }
            if (!isTypeCompatible(expectedTypes.get(i), literalTypes[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Void visitArithmeticExpression(ExpressionParser.ArithmeticExpressionContext ctx) {
        List<ExpressionParser.TermContext> terms = ctx.term();
        if (terms != null && terms.size() > 1) {
            for (int i = 1; i < terms.size(); i++) {
                Token opToken = findArithmeticOperatorBetween(ctx, terms.get(i - 1), terms.get(i));
                if (opToken == null) continue;
                checkArithmeticOperandTypes(opToken, terms.get(i - 1), terms.get(i));
            }
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitTerm(ExpressionParser.TermContext ctx) {
        List<ExpressionParser.FactorContext> factors = ctx.factor();
        if (factors != null && factors.size() > 1) {
            // term : factor ( (ASTERISK | DIV | MODULO) factor )*
            // Operators are interleaved between factors in the child list.
            for (int i = 1; i < factors.size(); i++) {
                // Find the operator token between factor[i-1] and factor[i]
                Token opToken = findOperatorBetween(ctx, factors.get(i - 1), factors.get(i));
                if (opToken != null && (opToken.getType() == ExpressionLexer.DIV
                        || opToken.getType() == ExpressionLexer.MODULO)) {
                    ExpressionParser.FactorContext divisor = factors.get(i);
                    if (isLiteralZero(divisor)) {
                        String op = opToken.getType() == ExpressionLexer.DIV ? "Division" : "Modulo";
                        Token token = divisor.getStart();
                        ExpressionError error = new ExpressionError(token.getLine(),
                                token.getCharPositionInLine(),
                                op + " by zero: divisor is literal 0.", token, null);
                        error.setWarning(true);
                        errors.add(error);
                    }
                }
                // Check operand types for *, /, %
                if (opToken != null) {
                    checkMultiplicativeOperandTypes(opToken, factors.get(i - 1), factors.get(i));
                }
            }
        }
        return visitChildren(ctx);
    }

    /**
     * Finds the operator token between two factor contexts within a term.
     */
    private Token findOperatorBetween(ExpressionParser.TermContext term,
                                       ExpressionParser.FactorContext left,
                                       ExpressionParser.FactorContext right) {
        int leftEnd = left.getStop().getTokenIndex();
        int rightStart = right.getStart().getTokenIndex();
        for (int i = 0; i < term.getChildCount(); i++) {
            if (term.getChild(i) instanceof TerminalNode) {
                Token t = ((TerminalNode) term.getChild(i)).getSymbol();
                if (t.getTokenIndex() > leftEnd && t.getTokenIndex() < rightStart) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a factor is a literal number with value 0 (e.g., 0, 0.0, 0.00).
     */
    private boolean isLiteralZero(ExpressionParser.FactorContext factor) {
        if (factor.literal() == null) return false;
        ExpressionParser.LiteralContext lit = factor.literal();
        if (lit.NUMBER() == null) return false;
        try {
            double value = Double.parseDouble(lit.NUMBER().getText());
            return value == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public Void visitFilterExpression(ExpressionParser.FilterExpressionContext ctx) {
        List<ExpressionParser.FilterComponentContext> components = ctx.filterComponent();
        if (components == null || components.size() < 2) {
            return visitChildren(ctx);
        }

        int lastOperatorIndex = -1;
        boolean lastWasComparison = false;
        boolean lastWasLogical = false;

        for (int i = 0; i < components.size(); i++) {
            ExpressionParser.FilterComponentContext comp = components.get(i);
            int opType = getFilterOperatorType(comp);

            if (opType == 1) { // comparison operator
                if (lastWasComparison && lastOperatorIndex == i - 1) {
                    addFilterWarning(comp,
                            "Possible incomplete filter expression: consecutive comparison operators without "
                                    + "an operand between them.");
                }
                lastWasComparison = true;
                lastWasLogical = false;
                lastOperatorIndex = i;
            } else if (opType == 2) { // logical binary operator (AND, OR)
                if (lastWasComparison && lastOperatorIndex == i - 1) {
                    addFilterWarning(comp,
                            "Possible incomplete filter expression: logical operator immediately after "
                                    + "comparison operator. Missing operand?");
                }
                if (lastWasLogical && lastOperatorIndex == i - 1) {
                    addFilterWarning(comp,
                            "Possible incomplete filter expression: consecutive logical operators without "
                                    + "an operand between them.");
                }
                lastWasComparison = false;
                lastWasLogical = true;
                lastOperatorIndex = i;
            } else {
                lastWasComparison = false;
                lastWasLogical = false;
            }
        }

        // Check for trailing operator
        if (lastOperatorIndex == components.size() - 1) {
            ExpressionParser.FilterComponentContext lastComp = components.get(lastOperatorIndex);
            if (lastWasComparison) {
                addFilterWarning(lastComp,
                        "Possible incomplete filter expression: ends with comparison operator without "
                                + "a right-hand operand.");
            } else if (lastWasLogical) {
                addFilterWarning(lastComp,
                        "Possible incomplete filter expression: ends with logical operator without "
                                + "a right-hand operand.");
            }
        }

        return visitChildren(ctx);
    }

    /**
     * Returns the operator type for a filter component:
     * 0 = not an operator, 1 = comparison, 2 = logical binary (AND/OR)
     */
    private int getFilterOperatorType(ExpressionParser.FilterComponentContext comp) {
        ExpressionParser.StringOrOperatorContext soo = comp.stringOrOperator();
        if (soo == null) {
            return 0;
        }
        if (soo.getChildCount() != 1 || !(soo.getChild(0) instanceof TerminalNode)) {
            return 0;
        }
        int tokenType = ((TerminalNode) soo.getChild(0)).getSymbol().getType();
        switch (tokenType) {
            case ExpressionLexer.GT:
            case ExpressionLexer.LT:
            case ExpressionLexer.GTE:
            case ExpressionLexer.LTE:
            case ExpressionLexer.EQ:
            case ExpressionLexer.NEQ:
                return 1;
            case ExpressionLexer.AND:
            case ExpressionLexer.OR:
                return 2;
            default:
                return 0;
        }
    }

    private void addFilterWarning(ExpressionParser.FilterComponentContext comp, String message) {
        Token token = comp.getStart();
        ExpressionError error = new ExpressionError(token.getLine(), token.getCharPositionInLine(),
                message, token, null);
        error.setWarning(true);
        errors.add(error);
    }

    @Override
    public Void visitArrayIndex(ExpressionParser.ArrayIndexContext ctx) {
        if (ctx.NUMBER() != null) {
            String text = ctx.NUMBER().getText();
            if (text.contains(".")) {
                Token token = ctx.NUMBER().getSymbol();
                ExpressionError error = new ExpressionError(token.getLine(),
                        token.getCharPositionInLine(),
                        "Array index should be an integer, but found floating-point number '" + text + "'.",
                        token, null);
                error.setWarning(true);
                errors.add(error);
            }
        }
        return visitChildren(ctx);
    }

    /**
     * Checks operand types for additive operators (+ and -).
     * For '-': string and boolean literals are invalid operands.
     * For '+': boolean literals are invalid (string concatenation with + is allowed).
     */
    private void checkArithmeticOperandTypes(Token opToken,
                                              ExpressionParser.TermContext left,
                                              ExpressionParser.TermContext right) {
        boolean isMinus = opToken.getType() == ExpressionLexer.MINUS;
        String opSymbol = isMinus ? "-" : "+";

        String leftType = getTermLiteralType(left);
        String rightType = getTermLiteralType(right);

        if (leftType != null && !isValidAdditiveOperand(leftType, isMinus)) {
            addArithmeticTypeWarning(opSymbol, leftType, left.getStart());
        }
        if (rightType != null && !isValidAdditiveOperand(rightType, isMinus)) {
            addArithmeticTypeWarning(opSymbol, rightType, right.getStart());
        }
    }

    /**
     * Checks operand types for multiplicative operators (*, /, %).
     * String and boolean literals are invalid operands for these operators.
     */
    private void checkMultiplicativeOperandTypes(Token opToken,
                                                  ExpressionParser.FactorContext left,
                                                  ExpressionParser.FactorContext right) {
        String opSymbol = opToken.getText();
        String leftType = getFactorLiteralType(left);
        String rightType = getFactorLiteralType(right);

        if (leftType != null && !isNumericType(leftType)) {
            addArithmeticTypeWarning(opSymbol, leftType, left.getStart());
        }
        if (rightType != null && !isNumericType(rightType)) {
            addArithmeticTypeWarning(opSymbol, rightType, right.getStart());
        }
    }

    private boolean isValidAdditiveOperand(String literalType, boolean isMinus) {
        if (isMinus) {
            // For '-', only number is valid
            return isNumericType(literalType);
        }
        // For '+', number and string are valid (string concatenation)
        return isNumericType(literalType) || "string".equals(literalType);
    }

    private boolean isNumericType(String type) {
        return "number".equals(type) || "integer".equals(type);
    }

    private void addArithmeticTypeWarning(String operator, String literalType, Token token) {
        ExpressionError error = new ExpressionError(token.getLine(), token.getCharPositionInLine(),
                "Arithmetic operator '" + operator + "' cannot be applied to " + literalType + " literal.",
                token, null);
        error.setWarning(true);
        errors.add(error);
    }

    /**
     * Finds the operator token between two term contexts within an arithmeticExpression.
     */
    private Token findArithmeticOperatorBetween(ExpressionParser.ArithmeticExpressionContext arith,
                                                 ExpressionParser.TermContext left,
                                                 ExpressionParser.TermContext right) {
        int leftEnd = left.getStop().getTokenIndex();
        int rightStart = right.getStart().getTokenIndex();
        for (int i = 0; i < arith.getChildCount(); i++) {
            if (arith.getChild(i) instanceof TerminalNode) {
                Token t = ((TerminalNode) arith.getChild(i)).getSymbol();
                if (t.getTokenIndex() > leftEnd && t.getTokenIndex() < rightStart) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Returns the literal type of a term if it is a single literal factor, or null otherwise.
     */
    private String getTermLiteralType(ExpressionParser.TermContext term) {
        List<ExpressionParser.FactorContext> factors = term.factor();
        if (factors == null || factors.size() != 1) return null;
        return getFactorLiteralType(factors.get(0));
    }

    /**
     * Returns the literal type of a factor if it is a direct literal, or null otherwise.
     */
    private String getFactorLiteralType(ExpressionParser.FactorContext factor) {
        ExpressionParser.LiteralContext literal = factor.literal();
        if (literal == null) return null;
        if (literal.STRING_LITERAL() != null) return "string";
        if (literal.NUMBER() != null) return "number";
        if (literal.BOOLEAN_LITERAL() != null) return "boolean";
        if (literal.arrayLiteral() != null) return "array";
        if (literal.NULL_LITERAL() != null) return "null";
        return null;
    }

    /**
     * Returns the type of a literal expression, or null if not a simple literal.
     */
    private String getLiteralType(ExpressionParser.ExpressionContext expr) {
        // Navigate through the expression hierarchy to reach the literal
        // expression -> comparisonExpression -> logicalExpression -> arithmeticExpression -> term -> factor -> literal
        if (expr == null) return null;

        ExpressionParser.ComparisonExpressionContext comp = expr.comparisonExpression();
        if (comp == null) return null;

        List<ExpressionParser.LogicalExpressionContext> logicals = comp.logicalExpression();
        if (logicals == null || logicals.size() != 1) return null;

        ExpressionParser.LogicalExpressionContext logical = logicals.get(0);
        // logicalExpression has a single arithmeticExpression (not a list)
        ExpressionParser.ArithmeticExpressionContext arith = logical.arithmeticExpression();
        if (arith == null) return null;

        List<ExpressionParser.TermContext> terms = arith.term();
        if (terms == null || terms.size() != 1) return null;

        ExpressionParser.TermContext termCtx = terms.get(0);
        List<ExpressionParser.FactorContext> factors = termCtx.factor();
        if (factors == null || factors.size() != 1) return null;

        ExpressionParser.FactorContext factor = factors.get(0);
        ExpressionParser.LiteralContext literal = factor.literal();
        if (literal == null) return null;

        if (literal.STRING_LITERAL() != null) return "string";
        if (literal.NUMBER() != null) return "number";
        if (literal.BOOLEAN_LITERAL() != null) return "boolean";
        if (literal.arrayLiteral() != null) return "array";
        if (literal.NULL_LITERAL() != null) return "null";
        return null;
    }

    /**
     * Checks if the actual literal type is compatible with the expected parameter type.
     */
    private boolean isTypeCompatible(String expectedType, String actualType) {
        if (expectedType.equals(actualType)) return true;
        // number is compatible with integer and vice versa
        if (("number".equals(expectedType) || "integer".equals(expectedType)) &&
                ("number".equals(actualType) || "integer".equals(actualType))) {
            return true;
        }
        return false;
    }
}
