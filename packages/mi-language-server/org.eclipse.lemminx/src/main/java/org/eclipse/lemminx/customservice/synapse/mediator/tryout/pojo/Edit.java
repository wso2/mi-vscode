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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo;

import org.eclipse.lsp4j.Range;

public class Edit {

    private String text;
    private Range range;

    public Edit(String text, Range range) {

        this.text = text;
        this.range = range;
    }

    public String getText() {

        return text;
    }

    public void setText(String text) {

        this.text = text;
    }

    public Range getRange() {

        return range;
    }

    public void setRange(Range range) {

        this.range = range;
    }
}
