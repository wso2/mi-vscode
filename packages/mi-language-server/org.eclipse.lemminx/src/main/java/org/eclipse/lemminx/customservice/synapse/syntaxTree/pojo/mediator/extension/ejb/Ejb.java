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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.extension.ejb;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class Ejb extends Mediator {

    EjbArgs args;
    String beanstalk;
    String clazz;
    String sessionId;
    boolean remove;
    String method;
    String target;
    String jndiName;
    String id;
    boolean stateful;
    String description;
    String traceFilter;

    public Ejb() {
        setDisplayName("EJB");
    }

    public EjbArgs getArgs() {

        return args;
    }

    public void setArgs(EjbArgs args) {

        this.args = args;
    }

    public String getBeanstalk() {

        return beanstalk;
    }

    public void setBeanstalk(String beanstalk) {

        this.beanstalk = beanstalk;
    }

    public String getClazz() {

        return clazz;
    }

    public void setClazz(String clazz) {

        this.clazz = clazz;
    }

    public String getSessionId() {

        return sessionId;
    }

    public void setSessionId(String sessionId) {

        this.sessionId = sessionId;
    }

    public boolean isRemove() {

        return remove;
    }

    public void setRemove(boolean remove) {

        this.remove = remove;
    }

    public String getMethod() {

        return method;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public String getTarget() {

        return target;
    }

    public void setTarget(String target) {

        this.target = target;
    }

    public String getJndiName() {

        return jndiName;
    }

    public void setJndiName(String jndiName) {

        this.jndiName = jndiName;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public boolean isStateful() {

        return stateful;
    }

    public void setStateful(boolean stateful) {

        this.stateful = stateful;
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
