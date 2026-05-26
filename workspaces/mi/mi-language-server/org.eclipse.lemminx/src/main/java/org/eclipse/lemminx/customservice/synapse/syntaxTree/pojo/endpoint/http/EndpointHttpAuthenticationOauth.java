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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

public class EndpointHttpAuthenticationOauth extends STNode {

    EndpointHttpAuthenticationOauthAuthorizationCode authorizationCode;
    EndpointHttpAuthenticationOauthClientCredentials clientCredentials;
    EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials;

    public EndpointHttpAuthenticationOauthAuthorizationCode getAuthorizationCode() {

        return authorizationCode;
    }

    public void setAuthorizationCode(EndpointHttpAuthenticationOauthAuthorizationCode authorizationCode) {

        this.authorizationCode = authorizationCode;
    }

    public EndpointHttpAuthenticationOauthClientCredentials getClientCredentials() {

        return clientCredentials;
    }

    public void setClientCredentials(EndpointHttpAuthenticationOauthClientCredentials clientCredentials) {

        this.clientCredentials = clientCredentials;
    }

    public EndpointHttpAuthenticationOauthPasswordCredentials getPasswordCredentials() {

        return passwordCredentials;
    }

    public void setPasswordCredentials(EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials) {

        this.passwordCredentials = passwordCredentials;
    }
}
