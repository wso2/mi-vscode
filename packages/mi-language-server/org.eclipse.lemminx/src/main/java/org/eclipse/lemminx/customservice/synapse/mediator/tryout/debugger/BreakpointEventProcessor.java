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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.debugger;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lemminx.customservice.synapse.debugger.entity.debuginfo.IDebugInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BreakpointEventProcessor {

    private static final Logger LOGGER = Logger.getLogger(BreakpointEventProcessor.class.getName());
    private static final List<String>
            PROPERTY_CONTEXTS = List.of("synapse", "axis2", "axis2-client", "transport", "operation", "variable");
    private final DebugCommandClient commandClient;
    private final Object lock;
    private boolean isListeningForStepOver = false;
    private boolean isInputFetched = false;
    private boolean isDone = false;
    private boolean isFault = false;
    private List<String> inputResponse;
    private List<String> outputResponse;
    private final List<JsonObject> activeBreakpoints;
    private IDebugInfo faultSequenceBreakpoint;

    public BreakpointEventProcessor(DebugCommandClient commandClient, Object lock, List<JsonObject> activeBreakpoints) {

        this.commandClient = commandClient;
        this.activeBreakpoints = activeBreakpoints;
        this.lock = lock;
    }

    public void process(JsonObject eventData) {

        if (!TryOutUtils.isExpectedBreakpoint(eventData, activeBreakpoints)) {
            LOGGER.warning("Event data does not match any active breakpoints: " + eventData);
            commandClient.sendResumeCommand();
            return;
        }
        List<String> properties = getProperties();
        if (isDone) {
            return;
        }
        if (TryOutUtils.isExpectedBreakpoint(eventData, faultSequenceBreakpoint.toJson().getAsJsonObject())) {
            isFault = true;
            isDone = true;
        }
        if (!isListeningForStepOver) {
            inputResponse = Collections.unmodifiableList(properties);
            synchronized (lock) {
                isListeningForStepOver = true;
                isInputFetched = true;
                lock.notifyAll();
            }
        } else {
            outputResponse = Collections.unmodifiableList(properties);
            synchronized (lock) {
                isDone = true;
                isListeningForStepOver = false;
                lock.notifyAll();
            }
            commandClient.sendResumeCommand();
        }
    }

    private List<String> getProperties() {

        List<String> properties = new ArrayList<>();
        for (String context : PROPERTY_CONTEXTS) {
            String property = getProperty(context);
            properties.add(property);
        }
        return properties;
    }

    private String getProperty(String context) {

        JsonObject property = new JsonObject();
        property.addProperty(TryOutConstants.COMMAND, TryOutConstants.GET);
        property.addProperty(TryOutConstants.COMMAND_ARGUMENT,
                TryOutConstants.VARIABLE.equals(context) ? TryOutConstants.VARIABLES : TryOutConstants.PROPERTIES);
        property.addProperty(TryOutConstants.CONTEXT, context);

        try {
            return commandClient.sendCommandWithRetry(property.toString());
        } catch (Exception e) {
            LOGGER.warning("Error occurred while fetching the property.");
        }
        return StringUtils.EMPTY;
    }

    public boolean isDone() {

        return isDone;
    }

    public List<String> getInputResponse() {

        if (inputResponse == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(inputResponse);
    }

    public List<String> getOutputResponse() {

        if (outputResponse == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(outputResponse);
    }

    public void setFaultSequenceBreakpoint(IDebugInfo faultSequenceBreakpoint) {

        this.faultSequenceBreakpoint = faultSequenceBreakpoint;
    }

    public boolean isFault() {

        return isFault;
    }

    public boolean isInputFetched() {

        return isInputFetched;
    }

    public void reset() {

        isDone = false;
        isInputFetched = false;
        isListeningForStepOver = false;
        inputResponse = null;
        outputResponse = null;
        isFault = false;
    }
}
