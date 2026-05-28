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

package org.eclipse.lemminx.customservice.synapse.debugger.entity;

import java.util.Objects;

public class Breakpoint {

    int line;
    Integer column;

    public Breakpoint(int line) {

        this.line = line;
    }

    public Breakpoint(int line, Integer column) {

        this.line = line;
        this.column = column;
    }

    public int getLine() {

        return line;
    }

    public void setLine(int line) {

        this.line = line;
    }

    public Integer getColumn() {

        return column;
    }

    public void setColumn(Integer column) {

        this.column = column;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Breakpoint that = (Breakpoint) o;
        return line == that.line && Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {

        return Objects.hash(line, column);
    }

    @Override
    public String toString() {

        return "Breakpoint{" +
                "line=" + line +
                ", column=" + column +
                '}';
    }
}
