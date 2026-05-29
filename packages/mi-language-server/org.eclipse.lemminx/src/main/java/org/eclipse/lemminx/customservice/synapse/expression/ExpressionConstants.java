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

import java.util.List;

public class ExpressionConstants {

    public static final String EXPRESSION_PREFIX = "${";
    public static final List<String> OPERATORS_CHARS =
            List.of("+", "-", "*", "/", "?", ":", ">", "<", ">= ", "<=", "==", "!=", "&&", "||", "and", "or",
                    "=", "&", "|");
    public static final String LABEL = "label";
    public static final String INSERT_TEXT = "insertText";
    public static final String DETAIL = "details";
    public static final String CATEGORY = "category";
    public static final String SIGNATURE = "signature";
    public static final String VARS = "vars";
    public static final String PROPERTIES = "properties";
    public static final String PROPS = "props";
    public static final String PARAMS = "params";
    public static final String AXIS2 = "axis2";
    public static final String AXIS2_CLIENT = "axis2Client";
    public static final String AXIS2_TRANSPORT = "axis2Transport";
    public static final String AXIS2_OPERATION = "axis2Operation";
    public static final String SYNAPSE = "synapse";
    public static final String HEADERS = "headers";
    public static final String PAYLOAD = "payload";
    public static final List<String> ATTRIBUTES_SECOND_LEVEL = List.of(SYNAPSE, AXIS2);
    public static final String QUERY_PARAMS = "queryParams";
    public static final String PATH_PARAMS = "pathParams";
    public static final String FUNCTION_PARAMS = "functionParams";
    public static final String CONFIGS = "configs";
    public static final List<String> ROOT_LEVEL_TOKENS =
            List.of(VARS, PROPERTIES, PROPS, PARAMS, HEADERS, PAYLOAD, CONFIGS);
    public static final List<String> PARAMS_SECOND_LEVEL = List.of(QUERY_PARAMS, PATH_PARAMS, FUNCTION_PARAMS);
    public static final String OBJECT = "Object";
    public static final String ITEMS = "items";
    public static final String SORT_TEXT = "sortText";
    public static final String ARRAY_COMPLETION_INSERT_TEXT = "[0]";
    public static final String ARRAY_COMPLETION_LABEL = "[]";

    private ExpressionConstants() {

    }
}
