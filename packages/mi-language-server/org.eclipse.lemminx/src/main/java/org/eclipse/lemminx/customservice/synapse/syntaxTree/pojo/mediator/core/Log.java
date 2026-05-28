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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.core;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Log extends Mediator {

    MediatorProperty[] property;
    LogLevel level;
    String separator;
    LogCategory category;
    String description;
    String message;
    boolean logFullPayload;
    boolean logMessageID;
    String traceFilter;

    public Log() {
        setDisplayName("Log");
    }

    public MediatorProperty[] getProperty() {

        return property;
    }

    public void setProperty(MediatorProperty[] property) {

        this.property = property;
    }

    public LogLevel getLevel() {

        return level;
    }

    public void setLevel(LogLevel level) {

        this.level = level;
    }

    public String getSeparator() {

        return separator;
    }

    public void setSeparator(String separator) {

        this.separator = separator;
    }

    public LogCategory getCategory() {

        return category;
    }

    public void setCategory(LogCategory category) {

        this.category = category;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getMessage() {

        return message;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    public boolean isLogFullPayload() {

        return logFullPayload;
    }

    public void setLogFullPayload(boolean logFullPayload) {

        this.logFullPayload = logFullPayload;
    }

    public boolean isLogMessageID() {

        return logMessageID;
    }

    public void setLogMessageID(boolean logMessageID) {

        this.logMessageID = logMessageID;
    }

    public String getTraceFilter() {

        return traceFilter;
    }

    public void setTraceFilter(String traceFilter) {

        this.traceFilter = traceFilter;
    }
}
