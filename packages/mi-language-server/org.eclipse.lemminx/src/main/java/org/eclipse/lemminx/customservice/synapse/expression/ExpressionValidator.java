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

package org.eclipse.lemminx.customservice.synapse.expression;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionError;
import org.eclipse.lemminx.util.synapse_expression.ExpressionLexer;
import org.eclipse.lemminx.util.synapse_expression.ExpressionParser;

import java.util.ArrayList;
import java.util.List;

public class ExpressionValidator {

    public static List<ExpressionError> validate(String expression) {

        List<ExpressionError> errors = new ArrayList<>();
        CharStream input = CharStreams.fromString(expression);
        ExpressionLexer lexer = new ExpressionLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        parser.setErrorHandler(new DefaultErrorStrategy());
        parser.removeErrorListeners();
        SyntaxErrorListener errorListener = new SyntaxErrorListener();
        parser.addErrorListener(errorListener);

        ExpressionParser.ExpressionContext tree = parser.expression();

        if (errorListener.hasErrors()) {
            for (ExpressionError err : errorListener.getErrors()) {
                errors.add(new ExpressionError(expression, err.getLine(), err.getCharPositionInLine(),
                        err.getMessage(), err.getOffendingSymbol(), err.getException()));
            }
        } else {
            // Run semantic validation when syntax is correct
            SemanticExpressionValidator semanticValidator = new SemanticExpressionValidator();
            semanticValidator.visit(tree);
            for (ExpressionError err : semanticValidator.getErrors()) {
                errors.add(new ExpressionError(expression, err));
            }
        }
        return errors;
    }
}
