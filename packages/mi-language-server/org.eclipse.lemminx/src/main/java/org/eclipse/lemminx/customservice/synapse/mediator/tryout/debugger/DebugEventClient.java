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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DebugEventClient extends Thread {

    private static final Logger LOGGER = Logger.getLogger(DebugEventClient.class.getName());
    private static final String HOST = TryOutConstants.LOCALHOST;
    private int port = TryOutConstants.DEFAULT_DEBUGGER_EVENT_PORT;
    private Socket socket;
    private final BlockingQueue<String> eventQueue;
    private final BreakpointEventProcessor breakpointEventProcessor;
    private boolean isDebuggerActive = false;

    public DebugEventClient(BreakpointEventProcessor breakpointEventProcessor) {

        this.eventQueue = new ArrayBlockingQueue<>(10);
        this.breakpointEventProcessor = breakpointEventProcessor;
    }

    public void connect() {

        try {
            socket = new Socket(HOST, port);
            socket.setReceiveBufferSize(65536);
            isDebuggerActive = true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to connect to the server using port: %d", port), e);
        }
    }

    @Override
    public void run() {

        Thread eventListener = new Thread(new DebugEventListener());
        eventListener.start();
        listenForEvent();
    }

    private void listenForEvent() {

        while (isDebuggerActive) {
            try {
                String event = eventQueue.take();
                Gson gson = new Gson();
                JsonObject eventJson = gson.fromJson(event, JsonObject.class);
                if (eventJson.has(TryOutConstants.EVENT) && eventJson.get(TryOutConstants.EVENT) != null &&
                        TryOutConstants.BREAKPOINT.equals(eventJson.get(TryOutConstants.EVENT).getAsString())) {
                    breakpointEventProcessor.process(eventJson);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.SEVERE, "Failed to listen for events", e);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred while listening the event", e);
            }
        }
    }

    public boolean isConnected() {

        if (socket == null) {
            return false;
        }
        return socket.isConnected();
    }

    public void close() throws IOException {

        isDebuggerActive = false;
        if (socket != null) {
            socket.close();
        }
    }

    public void clearEventQueue() {

        eventQueue.clear();
    }

    private class DebugEventListener implements Runnable {

        @Override
        public void run() {

            while (isDebuggerActive) {
                listen();
            }
        }

        public void listen() {

            try {
                InputStream inputStream = socket.getInputStream();
                byte[] tempBuffer = new byte[1024];
                StringBuilder buffer = new StringBuilder();
                int bytesRead;

                while ((bytesRead = inputStream.read(tempBuffer)) != -1) {
                    String receivedData = new String(tempBuffer, 0, bytesRead, StandardCharsets.UTF_8);
                    buffer.append(receivedData);

                    int delimiterIndex;
                    while ((delimiterIndex = buffer.indexOf("\n")) != -1) {
                        String event = buffer.substring(0, delimiterIndex).trim();
                        buffer.delete(0, delimiterIndex + 1);
                        eventQueue.offer(event);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to listen for events", e);
            }
        }
    }
}
