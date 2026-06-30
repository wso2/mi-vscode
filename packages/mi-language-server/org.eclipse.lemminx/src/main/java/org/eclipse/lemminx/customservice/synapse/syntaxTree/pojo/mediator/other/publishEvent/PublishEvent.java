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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.other.publishEvent;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.Mediator;

public class PublishEvent extends Mediator {

    Object eventSink;
    Object streamName;
    Object streamVersion;
    boolean async;
    String timeout;
    PublishEventAttributes attributes;
    String description;

    public Object getEventSink() {

        return eventSink;
    }

    public void setEventSink(Object eventSink) {

        this.eventSink = eventSink;
    }

    public Object getStreamName() {

        return streamName;
    }

    public void setStreamName(Object streamName) {

        this.streamName = streamName;
    }

    public Object getStreamVersion() {

        return streamVersion;
    }

    public void setStreamVersion(Object streamVersion) {

        this.streamVersion = streamVersion;
    }

    public PublishEventAttributes getAttributes() {

        return attributes;
    }

    public void setAttributes(PublishEventAttributes attributes) {

        this.attributes = attributes;
    }

    public boolean isAsync() {

        return async;
    }

    public void setAsync(boolean async) {

        this.async = async;
    }

    public String getTimeout() {

        return timeout;
    }

    public void setTimeout(String timeout) {

        this.timeout = timeout;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}
