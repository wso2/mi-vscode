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

public class ConfigDetails {

    private String key;
    private String type;
    private String value;
    private Either<Range, List<Range>> range;

    public ConfigDetails(String key, String type, String value, Either<Range, List<Range>> range) {
        this.key = key;
        this.type = type;
        this.value = value;
        this.range = range;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Either<Range, List<Range>> getRange() {
        return range;
    }
}
