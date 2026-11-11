/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared record of the running try-out MI server: who started it and what it deployed.
 *
 * Lets one window stop another window's server and remove its CAPPs and artifacts.
 */
public final class TryOutServerRecord {

    private static final Logger LOGGER = Logger.getLogger(TryOutServerRecord.class.getName());
    private static final Gson GSON = new Gson();
    // Start times read by different JVMs can differ slightly; a reused PID starts much later.
    private static final long START_TIME_TOLERANCE_MILLIS = 1000;

    private long serverPid;
    // Start time of the server process (epoch millis, 0 if unknown), to detect a reused PID.
    private long serverStartTime;
    private String serverPath;
    private String projectUri;
    private List<String> deployedFiles = new ArrayList<>();
    private List<String> deployedCapps = new ArrayList<>();

    TryOutServerRecord(long serverPid, long serverStartTime, String serverPath, String projectUri,
                       List<String> deployedFiles, List<String> deployedCapps) {

        this.serverPid = serverPid;
        this.serverStartTime = serverStartTime;
        this.serverPath = serverPath;
        this.projectUri = projectUri;
        this.deployedFiles = new ArrayList<>(deployedFiles);
        this.deployedCapps = new ArrayList<>(deployedCapps);
    }

    long getServerPid() {

        return serverPid;
    }

    long getServerStartTime() {

        return serverStartTime;
    }

    String getServerPath() {

        return serverPath;
    }

    String getProjectUri() {

        return projectUri;
    }

    /**
     * Whether the recorded MI server process is still running.
     */
    boolean isServerAlive() {

        return serverProcess().isPresent();
    }

    /**
     * The recorded server process, if it is still running. Empty if its PID now belongs to another process.
     */
    Optional<ProcessHandle> serverProcess() {

        if (serverPid <= 0) {
            return Optional.empty();
        }
        return ProcessHandle.of(serverPid).filter(ProcessHandle::isAlive).filter(this::matchesProcess);
    }

    private boolean matchesProcess(ProcessHandle process) {

        return matchesStartTime(startTimeOf(process));
    }

    /**
     * Whether this record is for the server process with the given PID and start time.
     */
    private boolean matchesServer(long pid, long startTime) {

        return serverPid == pid && matchesStartTime(startTime);
    }

    private boolean matchesStartTime(long startTime) {

        if (serverStartTime == 0 || startTime == 0) {
            // Start time unknown (e.g. a record from an older version): trust the PID.
            return true;
        }
        return Math.abs(startTime - serverStartTime) <= START_TIME_TOLERANCE_MILLIS;
    }

    /**
     * The process start time in epoch millis, or 0 if unknown. Read it while the process runs; the OS may not
     * report it after the process exits.
     */
    static long startTimeOf(ProcessHandle process) {

        return process.info().startInstant().map(Instant::toEpochMilli).orElse(0L);
    }

    /**
     * Removes the CAPPs and artifacts the recorded server deployed.
     */
    void deleteDeployedContent() {

        deleteFiles(deployedFiles);
        deleteFiles(deployedCapps);
    }

    private static void deleteFiles(List<String> filePaths) {

        if (filePaths == null) {
            return;
        }
        for (String filePath : filePaths) {
            try {
                Files.deleteIfExists(new File(filePath).toPath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, String.format("Error while deleting the try-out file: %s", filePath), e);
            }
        }
    }

    /**
     * @return the current record, or {@code null} if there is none or it can't be read
     */
    static synchronized TryOutServerRecord read() {

        Path recordFile = TryOutConstants.TRYOUT_SERVER_RECORD_FILE;
        if (!Files.exists(recordFile)) {
            return null;
        }
        try {
            return GSON.fromJson(Files.readString(recordFile, StandardCharsets.UTF_8), TryOutServerRecord.class);
        } catch (IOException | JsonParseException e) {
            LOGGER.log(Level.WARNING, "Error while reading the try-out server record", e);
            return null;
        }
    }

    /**
     * Writes the record atomically, so other windows never read a half-written file.
     */
    static synchronized void write(TryOutServerRecord record) {

        Path recordFile = TryOutConstants.TRYOUT_SERVER_RECORD_FILE;
        Path tempFile = null;
        try {
            Files.createDirectories(recordFile.getParent());
            tempFile = Files.createTempFile(recordFile.getParent(), "tryout-server", ".json.tmp");
            Files.writeString(tempFile, GSON.toJson(record), StandardCharsets.UTF_8);
            Files.move(tempFile, recordFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while writing the try-out server record", e);
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupError) {
                    LOGGER.log(Level.WARNING, "Error while deleting the temporary try-out server record", cleanupError);
                }
            }
        }
    }

    static synchronized void delete() {

        try {
            Files.deleteIfExists(TryOutConstants.TRYOUT_SERVER_RECORD_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error while deleting the try-out server record", e);
        }
    }

    /**
     * Deletes the record only if it is still for this server, not a newer one that reused the PID.
     */
    static synchronized void deleteIfServer(long serverPid, long serverStartTime) {

        TryOutServerRecord record = read();
        if (record != null && record.matchesServer(serverPid, serverStartTime)) {
            delete();
        }
    }

    /**
     * Whether the record is still for this server, i.e. no other window stopped it.
     */
    static synchronized boolean isRecordedServer(long serverPid, long serverStartTime) {

        TryOutServerRecord record = read();
        return record != null && record.matchesServer(serverPid, serverStartTime);
    }
}
