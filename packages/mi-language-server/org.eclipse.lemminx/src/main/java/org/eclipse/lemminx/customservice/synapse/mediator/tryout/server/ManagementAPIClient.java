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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.DeployedArtifactType;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ManagementAPIClient {

    private static final Logger LOGGER = Logger.getLogger(ManagementAPIClient.class.getName());
    private static final int DEFAULT_PORT = 9164;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String SERVER_SHUTDOWN_PAYLOAD = "{ \"status\": \"shutdown\" }";
    private ObjectMapper objectMapper;
    private HttpClient client;
    private static final String HOST = TryOutConstants.LOCALHOST;
    // Time out every call, so an unresponsive MI can't block try-out forever.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private int port = DEFAULT_PORT;
    private volatile String accessToken;

    public ManagementAPIClient() {

        try {
            objectMapper = new ObjectMapper();
            init();
            connect();
        } catch (NoSuchAlgorithmException | KeyManagementException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Failed to initialize the client: " + e.getMessage());
        }
    }

    public void init() throws NoSuchAlgorithmException, KeyManagementException {

        // Create SSL context that ignores certificate verification
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {

                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // No need to check client certificate
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // No need to check server certificate
            }
        }}, new SecureRandom());

        // Create HttpClient with SSL context
        client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .sslContext(sslContext)
                .build();
    }

    public void connect() throws InterruptedException {

        connect(REQUEST_TIMEOUT);
    }

    private void connect(Duration timeout) throws InterruptedException {

        try {

            // Create Basic Auth token
            String token = Base64.getEncoder().encodeToString(
                    (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + token;

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .timeout(timeout)
                    .uri(URI.create(String.format("https://%s:%d/management/login", HOST, port)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authHeader)
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // Parse the response to get the access token
            JsonNode responseBody = objectMapper.readTree(response.body());
            accessToken = responseBody.get("AccessToken").asText();
        } catch (IOException e) {
            LOGGER.severe("Failed to connect to the server: " + e.getMessage());
        }
    }

    public void shutdown() throws InterruptedException {

        try {;
            HttpRequest request = HttpRequest.newBuilder()
                .timeout(REQUEST_TIMEOUT)
                    .uri(URI.create(String.format("https://%s:%d/management/server", HOST, port)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(SERVER_SHUTDOWN_PAYLOAD))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            LOGGER.severe("Failed to connect to the server: " + e.getMessage());
        }
    }

    /**
     * Sends a GET with the current token. On a 401, logs in again and retries once. The retry is per call, so
     * one failed login doesn't stop later calls (or other threads) from refreshing the token.
     *
     * @param path    the management API path, e.g. {@code applications}
     * @param timeout the time allowed for the whole call, including the login and retry
     * @return the response of the last attempt
     */
    private HttpResponse<String> getWithLogin(String path, Duration timeout) throws IOException, InterruptedException {

        long deadline = System.nanoTime() + timeout.toNanos();
        HttpResponse<String> response = client.send(buildGet(path, timeout), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            connect(remainingUntil(deadline));
            response = client.send(buildGet(path, remainingUntil(deadline)), HttpResponse.BodyHandlers.ofString());
        }
        return response;
    }

    private static Duration remainingUntil(long deadlineNanos) throws HttpTimeoutException {

        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            throw new HttpTimeoutException("The management API request timed out");
        }
        return Duration.ofNanos(remaining);
    }

    private HttpRequest buildGet(String path, Duration timeout) {

        return HttpRequest.newBuilder()
                .timeout(timeout)
                .uri(URI.create(String.format("https://%s:%d/management/%s", HOST, port, path)))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
    }

    public List<String> getDeployedCapps() throws IOException, InterruptedException {

        HttpResponse<String> response = getWithLogin("applications", REQUEST_TIMEOUT);
        if (response.statusCode() == 200) {
            return extractDeployedCapps(response.body());
        }
        throw new IOException("Failed to list the deployed Carbon Applications. Status: " + response.statusCode());
    }

    /**
     * Deployed CAPPs and their artifacts. A list, since MI can run several versions of one CAPP name.
     */
    public List<DeployedCapp> getDeployedCappsWithArtifacts() throws IOException, InterruptedException {

        HttpResponse<String> response = getWithLogin("applications", REQUEST_TIMEOUT);
        if (response.statusCode() == 200) {
            return extractDeployedCappsWithArtifacts(response.body());
        }
        throw new IOException("Failed to list the deployed Carbon Applications. Status: " + response.statusCode());
    }

    /**
     * The {@code activeList} of an applications response. MI omits it (and sends an {@code error}) when it can't
     * list the CAPPs, so a missing list is a failed lookup, not "nothing deployed".
     */
    private static JsonArray parseActiveCappList(String body) throws IOException {

        JsonObject jsonObject = Utils.getJsonObject(body);
        if (jsonObject == null || !jsonObject.has("activeList") || !jsonObject.get("activeList").isJsonArray()) {
            throw new IOException("Failed to list the deployed Carbon Applications. Response: " + body);
        }
        return jsonObject.getAsJsonArray("activeList");
    }

    private List<DeployedCapp> extractDeployedCappsWithArtifacts(String body) throws IOException {

        List<DeployedCapp> capps = new ArrayList<>();
        JsonArray activeList = parseActiveCappList(body);
        for (int i = 0; i < activeList.size(); i++) {
            JsonObject capp = activeList.get(i).getAsJsonObject();
            String name = capp.get("name").getAsString();
            String version = capp.has("version") && !capp.get("version").isJsonNull()
                    ? capp.get("version").getAsString() : null;
            List<CappArtifact> artifacts = new ArrayList<>();
            JsonArray artifactList = capp.has("artifacts") && capp.get("artifacts").isJsonArray()
                    ? capp.getAsJsonArray("artifacts") : new JsonArray();
            for (int j = 0; j < artifactList.size(); j++) {
                JsonObject artifact = artifactList.get(j).getAsJsonObject();
                if (artifact.has("name") && artifact.has("type")) {
                    artifacts.add(new CappArtifact(artifact.get("name").getAsString(),
                            artifact.get("type").getAsString()));
                }
            }
            capps.add(new DeployedCapp(name, version, artifacts));
        }
        return capps;
    }

    private List<String> extractDeployedCapps(String body) throws IOException {

        JsonArray activeList = parseActiveCappList(body);
        List<String> capps = new ArrayList<>();
        for (int i = 0; i < activeList.size(); i++) {
            capps.add(activeList.get(i).getAsJsonObject().get("name").getAsString());
        }
        return capps;
    }

    public List<DeployedArtifact> getArtifacts(DeployedArtifactType type) throws IOException, InterruptedException {

        return getArtifacts(type, REQUEST_TIMEOUT);
    }

    public List<DeployedArtifact> getArtifacts(DeployedArtifactType type, Duration timeout)
            throws IOException, InterruptedException {

        HttpResponse<String> response = getWithLogin(type.getValue(), timeout);
        if (response.statusCode() == 200) {
            return extractDeployedArtifacts(response.body());
        }
        LOGGER.severe("Failed to get artifacts: " + response.body());
        return Collections.emptyList();
    }

    public List<DeployedArtifact> extractDeployedArtifacts(String jsonString) throws IOException {

        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode listNode = rootNode.get("list");

        List<DeployedArtifact> nameUrlPairs = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode node : listNode) {
                String name = node.get("name").asText();
                String url = null;
                if (node.has("url")) {
                    url = node.get("url").asText();
                }
                nameUrlPairs.add(new DeployedArtifact(name, url));
            }
        }
        return nameUrlPairs;
    }

    public boolean deployCAPP(File capp) {

        try {
            String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
            HttpRequest request = HttpRequest.newBuilder()
                .timeout(REQUEST_TIMEOUT)
                    .uri(URI.create(String.format("https://%s:%d/management/applications", HOST, port)))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(buildMultipartBody(boundary, Path.of(capp.getAbsolutePath())))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().contains("Successfully added Carbon Application");
        } catch (Exception e) {
            return false;
        }
    }

    public static HttpRequest.BodyPublisher buildMultipartBody(String boundary, Path filePath) throws IOException {

        var byteArrays = new ArrayList<byte[]>();

        String filename = filePath.getFileName().toString();
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        byteArrays.add(partHeader.getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(filePath));
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    /**
     * A deployed CAPP.
     */
    public static class DeployedCapp {

        private final String name;
        private final String version;
        private final List<CappArtifact> artifacts;

        DeployedCapp(String name, String version, List<CappArtifact> artifacts) {

            this.name = name;
            this.version = version;
            this.artifacts = artifacts;
        }

        public String getName() {

            return name;
        }

        public String getVersion() {

            return version;
        }

        public List<CappArtifact> getArtifacts() {

            return artifacts;
        }
    }

    /**
     * An artifact in a CAPP: its name and type (e.g. {@code api}, {@code sequence}).
     */
    public static class CappArtifact {

        private final String name;
        private final String type;

        CappArtifact(String name, String type) {

            this.name = name;
            this.type = type;
        }

        public String getName() {

            return name;
        }

        public String getType() {

            return type;
        }
    }

    public static class DeployedArtifact {

        private final String name;
        private final String url;

        public DeployedArtifact(String name, String url) {

            this.name = name;
            this.url = url;
        }

        public String getName() {

            return name;
        }

        public String getUrl() {

            return url;
        }

        @Override
        public String toString() {

            return "DeployedArtifact{" +
                    "name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
