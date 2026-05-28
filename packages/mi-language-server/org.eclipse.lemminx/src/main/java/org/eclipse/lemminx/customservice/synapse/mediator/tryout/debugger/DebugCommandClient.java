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

import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DebugCommandClient {

    private static final Logger LOGGER = Logger.getLogger(DebugCommandClient.class.getName());

    private static final String HOST = TryOutConstants.LOCALHOST;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private int port = TryOutConstants.DEFAULT_DEBUGGER_COMMAND_PORT;
    private Socket socket;

    public void connect() {

        try {
            socket = new Socket(HOST, port);
            socket.setSoTimeout(5000); // Set a timeout for reading from the socket
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format("Failed to connect to the server using port: %d", port), e);
        }
    }

    public String sendCommand(String message) {

        try {
            return send(message);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send command", e);
        }
        return null;
    }

    private String send(String message) throws IOException {

        var outputStream = new BufferedOutputStream(socket.getOutputStream());

        message += "\n";

        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();

        var inputStream =
                new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        return inputStream.readLine();
    }

    public String sendCommandWithRetry(String message) {

        return sendCommandWithRetry(message, DEFAULT_MAX_RETRIES);
    }

    public String sendCommandWithRetry(String message, int maxRetries) {

        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return send(message);
            } catch (IOException e) {
                attempt++;
                LOGGER.log(Level.WARNING, String.format("Attempt %d failed to send command", attempt), e);
                if (attempt >= maxRetries) {
                    LOGGER.log(Level.SEVERE, "Max retries reached. Failed to send command", e);
                    return null;
                }
            }
        }
        return null;
    }

    public void sendResumeCommand() {

        sendCommand(TryOutConstants.RESUME_COMMAND);
    }

    public boolean isConnected() {

        if (socket == null) {
            return false;
        }
        return socket.isConnected();
    }

    public void close() throws IOException {

        if (socket != null) {
            socket.close();
        }
    }
}
