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

package org.eclipse.lemminx.customservice.synapse.mediator.tryout;

import org.eclipse.lemminx.customservice.SynapseLanguageClientAPI;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectionTester;
import org.eclipse.lemminx.customservice.synapse.connectors.ConnectorHolder;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionRequest;
import org.eclipse.lemminx.customservice.synapse.connectors.entity.TestConnectionResponse;
import org.eclipse.lemminx.customservice.synapse.mediator.TryOutConstants;
import org.eclipse.lemminx.customservice.synapse.mediator.schema.generate.ServerLessTryoutHandler;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutInfo;
import org.eclipse.lemminx.customservice.synapse.mediator.tryout.pojo.MediatorTryoutRequest;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TryOutManager {

    // Reassigned by rebind, so volatile for other request threads.
    private volatile String projectRoot;
    private final String miServerPath;
    private final TryOutHandler tryOutHandler;
    private volatile IsolatedTryOutHandler isolatedTryOutHandler;
    private volatile ServerLessTryoutHandler serverLessTryoutHandler;
    private volatile ConnectionTester connectionTester;
    // Requests that have bound this manager and not finished yet.
    private final AtomicInteger requestsInFlight = new AtomicInteger();

    public TryOutManager(String projectRoot, String miServerPath, String miVersion, ConnectorHolder connectorHolder,
                         SynapseLanguageClientAPI languageClient) {

        this.projectRoot = projectRoot;
        this.miServerPath = miServerPath;
        tryOutHandler = new TryOutHandler(projectRoot, miServerPath, languageClient);
        isolatedTryOutHandler = new IsolatedTryOutHandler(tryOutHandler, projectRoot, miVersion, connectorHolder);
        serverLessTryoutHandler = new ServerLessTryoutHandler(projectRoot, connectorHolder);
        connectionTester = new ConnectionTester(projectRoot, tryOutHandler, connectorHolder);
    }

    /**
     * The project root this manager is currently bound to.
     */
    public String getProjectUri() {

        return projectRoot;
    }

    /**
     * Whether a try-out session is running or paused. If so, don't rebind or shut down.
     */
    public boolean isActive() {

        return requestsInFlight.get() > 0 || tryOutHandler.hasPendingSession();
    }

    /**
     * Marks a request as using this manager, so it can't be rebound or shut down meanwhile.
     * Call while holding the bind lock, and pair with {@link #release()}.
     */
    public void acquire() {

        requestsInFlight.incrementAndGet();
    }

    public void release() {

        requestsInFlight.decrementAndGet();
    }

    /**
     * Whether the MI server is running, so a project switch can {@link #rebind} instead of restarting.
     */
    public boolean isServerAlive() {

        return tryOutHandler.isServerAlive();
    }

    /**
     * Whether the running server can be reused for another project: same server path, and that project's
     * {@code .env} has the same values or none.
     */
    public boolean canRebindTo(String candidateMiServerPath, String candidateProjectUri) {

        return Objects.equals(this.miServerPath, candidateMiServerPath) && canServeEnvOf(candidateProjectUri);
    }

    /**
     * Whether the running server's env fits {@code projectUri}'s {@code .env}: same values, or none.
     */
    public boolean canServeEnvOf(String projectUri) {

        return tryOutHandler.canServeEnvOf(projectUri);
    }

    /**
     * Switches to another project without restarting the server. The old project is undeployed.
     */
    public void rebind(String newProjectRoot, String miVersion, ConnectorHolder connectorHolder) {

        tryOutHandler.rebind(newProjectRoot);
        this.projectRoot = newProjectRoot;
        this.isolatedTryOutHandler =
                new IsolatedTryOutHandler(tryOutHandler, newProjectRoot, miVersion, connectorHolder);
        this.connectionTester = new ConnectionTester(newProjectRoot, tryOutHandler, connectorHolder);
        this.serverLessTryoutHandler = new ServerLessTryoutHandler(newProjectRoot, connectorHolder);
    }

    /**
     * Runs the try-out, unless a shutdown request was running when it arrived or came in after it.
     *
     * @param arrivalToken {@link TryOutHandler#tryOutArrivalToken()} when the request arrived
     */
    public final MediatorTryoutInfo tryout(MediatorTryoutRequest request, long arrivalToken) {

        return tryOutHandler.runUnlessShutDownSince(arrivalToken,
                () -> request.isIsolatedTryout()
                        ? isolatedTryOutHandler.tryOut(request)
                        : tryOutHandler.handle(request),
                () -> new MediatorTryoutInfo(TryOutConstants.SERVER_STOPPED_WHILE_WAITING_ERROR));
    }

    public final MediatorTryoutInfo getInputOutputSchema(MediatorTryoutRequest request) {

        return serverLessTryoutHandler.handle(request);
    }

    /**
     * Tests the connection, unless a shutdown request was running when it arrived or came in after it.
     *
     * @param arrivalToken {@link TryOutHandler#tryOutArrivalToken()} when the request arrived
     */
    public final TestConnectionResponse testConnectorConnection(TestConnectionRequest request, long arrivalToken) {

        return tryOutHandler.runUnlessShutDownSince(arrivalToken, () -> connectionTester.testConnection(request),
                () -> new TestConnectionResponse(TryOutConstants.SERVER_STOPPED_WHILE_WAITING_ERROR));
    }

    public boolean shutdown() {

        return tryOutHandler.resetAndShutDown();
    }


    /**
     * Kills the process using the try-out server's ports, even if this window didn't start it.
     */
    public boolean killOccupyingServer() {

        return tryOutHandler.killOccupyingServer();
    }

    /**
     * Removes the CAPPs that conflicted with the last try-out deployment.
     */
    public boolean removeConflictingCapps() {

        return tryOutHandler.removeConflictingCapps();
    }

    /**
     * Drops the closing panel's paused session, if any, so it doesn't block other projects.
     */
    public void discardActiveSession(String panelId) {

        tryOutHandler.discardActiveSession(panelId);
    }
}
