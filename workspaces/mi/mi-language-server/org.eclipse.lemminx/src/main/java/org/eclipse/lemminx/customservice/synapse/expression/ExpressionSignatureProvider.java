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

import org.eclipse.lemminx.customservice.synapse.expression.pojo.ExpressionParam;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.FunctionCompletionItem;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.FunctionInfo;
import org.eclipse.lemminx.customservice.synapse.expression.pojo.Functions;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ExpressionSignatureProvider {

    private static final Logger LOGGER = Logger.getLogger(ExpressionSignatureProvider.class.getName());
    private static Map<String, List<SignatureInformation>> functionSignatures = new HashMap<>();

    static {
        loadFunctionSignatures();
    }

    private ExpressionSignatureProvider() {

    }

    private static void loadFunctionSignatures() {

        Map<String, Functions> functionCompletionItems = ExpressionCompletionUtils.getFunctions();
        for (Map.Entry<String, Functions> entry : functionCompletionItems.entrySet()) {
            for (CompletionItem completionItem : entry.getValue().getItems()) {
                SignatureInformation signatureInformation = ((FunctionCompletionItem) completionItem).getSignature();
                String label = completionItem.getLabel().substring(0, completionItem.getLabel().indexOf('('));
                if (functionSignatures.containsKey(label)) {
                    functionSignatures.get(label).add(signatureInformation);
                } else {
                    List<SignatureInformation> signatures = new ArrayList<>();
                    signatures.add(signatureInformation);
                    functionSignatures.put(label, signatures);
                }
            }
        }
    }

    public static SignatureHelp getFunctionSignatures(ExpressionParam params) {

        return getFunctionSignatures(params.getExpression(), params.getOffset());

    }

    private static SignatureHelp getFunctionSignatures(String expression, int expressionOffset) {

        FunctionInfo currentFunctionInfo = findCurrentFunction(expression, expressionOffset);
        if (currentFunctionInfo != null) {
            List<SignatureInformation> signatureInformation = functionSignatures.get(currentFunctionInfo.getName());
            if (signatureInformation != null) {
                int activeSignature =
                        findActiveSignature(signatureInformation, currentFunctionInfo.getCurrentParameterIndex());
                return new SignatureHelp(signatureInformation, activeSignature,
                        currentFunctionInfo.getCurrentParameterIndex());
            }
        }
        return null;
    }

    private static int findActiveSignature(List<SignatureInformation> signatureInformation, int currentParameterIndex) {

        int activeSignature = 0;
        for (int i = 0; i < signatureInformation.size(); i++) {
            if (signatureInformation.get(i).getParameters().size() > currentParameterIndex) {
                activeSignature = i;
                break;
            }
        }
        return activeSignature;
    }

    public static FunctionInfo findCurrentFunction(String input, int cursorPosition) {

        Deque<FunctionInfo> functionDeque = new ArrayDeque<>();

        for (int i = 0; i < cursorPosition; i++) {
            char c = input.charAt(i);

            if (c == '(') {
                // Identify the function name before '('
                int start = i - 1;
                while (start >= 0 && (Character.isLetterOrDigit(input.charAt(start)) || input.charAt(start) == '_')) {
                    start--;
                }
                start++;

                String functionName = input.substring(start, i).trim();
                functionDeque.push(new FunctionInfo(functionName, 0));
            } else if (c == ')') {
                if (!functionDeque.isEmpty()) {
                    functionDeque.pop();
                }
            } else if (c == ',' && !functionDeque.isEmpty()) {
                // Increment parameter index of the current function context
                functionDeque.peek().incrementParameterIndex();
            }
        }

        // If the stack is not empty, we are inside a function
        if (!functionDeque.isEmpty()) {
            FunctionInfo currentContext = functionDeque.peek();
            return new FunctionInfo(currentContext.getName(), currentContext.getCurrentParameterIndex());
        }

        return null;
    }
}
