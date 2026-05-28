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
package org.eclipse.lemminx.customservice.synapse.parser;

import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.List;

public class Node {

    private Either<Range, List<Range>> range;
    private String value;
    private String key;
    private String displayValue;

    public Node() {

    }

    public Node(String value, Either<Range, List<Range>> range) {

        this.range = range;
        this.value = value;
    }

    public Node(String key, String value, Either<Range, List<Range>> range) {

        this.range = range;
        this.key = key;
        this.value = value;
    }

    public String getValue() {

        return value;
    }

    public String getKey() {

        return key;
    }

    public void setRange(Either<Range, List<Range>> range) {
        this.range = range;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return this.displayValue;
    }

    public Either<Range, List<Range>> getRange() {
        return this.range;
    }
}
