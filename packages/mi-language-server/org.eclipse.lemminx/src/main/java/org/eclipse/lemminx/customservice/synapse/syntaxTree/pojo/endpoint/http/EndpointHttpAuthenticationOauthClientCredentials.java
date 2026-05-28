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

public class EndpointHttpAuthenticationOauthClientCredentials extends STNode {

    STNode clientId;
    STNode clientSecret;
    STNode refreshToken;
    STNode tokenUrl;
    EndpointHttpAuthenticationOauthRequestParameters requestParameters;
    STNode authMode;

    public STNode getClientId() {

        return clientId;
    }

    public void setClientId(STNode clientId) {

        this.clientId = clientId;
    }

    public STNode getClientSecret() {

        return clientSecret;
    }

    public void setClientSecret(STNode clientSecret) {

        this.clientSecret = clientSecret;
    }

    public STNode getRefreshToken() {

        return refreshToken;
    }

    public void setRefreshToken(STNode refreshToken) {

        this.refreshToken = refreshToken;
    }

    public STNode getTokenUrl() {

        return tokenUrl;
    }

    public void setTokenUrl(STNode tokenUrl) {

        this.tokenUrl = tokenUrl;
    }

    public EndpointHttpAuthenticationOauthRequestParameters getRequestParameters() {

        return requestParameters;
    }

    public void setRequestParameters(EndpointHttpAuthenticationOauthRequestParameters requestParameters) {

        this.requestParameters = requestParameters;
    }

    public STNode getAuthMode() {

        return authMode;
    }

    public void setAuthMode(STNode authMode) {

        this.authMode = authMode;
    }
}
