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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.switchMediator;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Switch extends Mediator {

    SwitchCase[] _case;
    SwitchDefault _default;
    String source;
    String description;
    String traceFilter;

    public Switch() {
        setDisplayName("Switch");
    }

    public SwitchCase[] get_case() {

        return _case;
    }

    public void set_case(SwitchCase[] _case) {

        this._case = _case;
    }

    public SwitchDefault get_default() {

        return _default;
    }

    public void set_default(SwitchDefault _default) {

        this._default = _default;
    }

    public String getSource() {

        return source;
    }

    public void setSource(String source) {

        this.source = source;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}

