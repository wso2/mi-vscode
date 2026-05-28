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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.filter.throttle;

public class ControlAccessType implements AccessType {

    private int maximumCount;
    private int unitTime;
    private int prohibitTimePeriod;

    public int getMaximumCount() {

        return maximumCount;
    }

    public void setMaximumCount(int maximumCount) {

        this.maximumCount = maximumCount;
    }

    public int getUnitTime() {

        return unitTime;
    }

    public void setUnitTime(int unitTime) {

        this.unitTime = unitTime;
    }

    public int getProhibitTimePeriod() {

        return prohibitTimePeriod;
    }

    public void setProhibitTimePeriod(int prohibitTimePeriod) {

        this.prohibitTimePeriod = prohibitTimePeriod;
    }
}
