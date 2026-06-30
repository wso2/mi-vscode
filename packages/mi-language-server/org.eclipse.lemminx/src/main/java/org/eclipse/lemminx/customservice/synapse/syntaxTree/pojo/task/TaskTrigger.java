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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.task;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class TaskTrigger extends STNode {

    String interval;
    String count;
    boolean once;
    String cron;

    public String getInterval() {

        return interval;
    }

    public void setInterval(String interval) {

        this.interval = interval;
    }

    public String getCount() {

        return count;
    }

    public void setCount(String count) {

        this.count = count;
    }

    public boolean getOnce() {

        return once;
    }

    public void setOnce(boolean once) {

        this.once = once;
    }

    public String getCron() {

        return cron;
    }

    public void setCron(String cron) {

        this.cron = cron;
    }
}
