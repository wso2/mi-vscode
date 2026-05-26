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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.endpoint;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.factory.AbstractFactory;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointRetryConfig;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EnableSecAndEnableRMAndEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthentication;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationBasicAuth;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauth;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthAuthorizationCode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthClientCredentials;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthPasswordCredentials;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthRequestParameters;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.HttpMethod;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;
import org.eclipse.lemminx.customservice.synapse.utils.Utils;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;

import java.util.List;
import java.util.Optional;

public class HttpEndpointFactory extends AbstractFactory {

    @Override
    public STNode create(DOMElement element) {

        EndpointHttp httpEndpoint = new EndpointHttp();
        httpEndpoint.elementNode(element);
        populateAttributes(httpEndpoint, element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            EnableSecAndEnableRMAndEnableAddressing configs = new EnableSecAndEnableRMAndEnableAddressing();
            for (DOMNode node : children) {
                String name = node.getNodeName();
                if (name.equalsIgnoreCase(Constant.ENABLE_SEC)) {
                    EndpointEnableSec enableSec = EndpointUtils.createEnableSec(node);
                    configs.setEnableSec(Optional.ofNullable(enableSec));
                } else if (name.equalsIgnoreCase(Constant.ENABLE_RM)) {
                    EndpointEnableRM enableRM = EndpointUtils.createEnableRM(node);
                    configs.setEnableRM(Optional.ofNullable(enableRM));
                } else if (name.equalsIgnoreCase(Constant.ENABLE_ADDRESSING)) {
                    EndpointEnableAddressing enableAddressing = EndpointUtils.createEnableAddressing(node);
                    configs.setEnableAddressing(Optional.ofNullable(enableAddressing));
                } else if (name.equalsIgnoreCase(Constant.TIMEOUT)) {
                    EndpointTimeout timeout = EndpointUtils.createTimeout(node);
                    configs.setTimeout(Optional.ofNullable(timeout));
                } else if (name.equalsIgnoreCase(Constant.SUSPEND_ON_FAILURE)) {
                    EndpointSuspendOnFailure suspendOnFailure = EndpointUtils.createSuspendOnFailure(node);
                    configs.setSuspendOnFailure(Optional.ofNullable(suspendOnFailure));
                } else if (name.equalsIgnoreCase(Constant.MARK_FOR_SUSPENSION)) {
                    EndpointMarkForSuspension markForSuspension = EndpointUtils.createMarkForSuspension(node);
                    configs.setMarkForSuspension(Optional.ofNullable(markForSuspension));
                } else if (name.equalsIgnoreCase(Constant.AUTHENTICATION)) {
                    EndpointHttpAuthentication authentication = createAuthentication(node);
                    configs.setAuthentication(Optional.ofNullable(authentication));
                } else if (Constant.RETRY_CONFIG.equalsIgnoreCase(name)) {
                    EndpointRetryConfig retryConfig = EndpointUtils.createRetryConfig(node);
                    configs.setRetryConfig(Optional.ofNullable(retryConfig));
                }
            }
            httpEndpoint.setEnableSecAndEnableRMAndEnableAddressing(configs);
        }
        return httpEndpoint;
    }

    private EndpointHttpAuthentication createAuthentication(DOMNode node) {

        EndpointHttpAuthentication authentication = new EndpointHttpAuthentication();
        authentication.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.OAUTH)) {
                    EndpointHttpAuthenticationOauth oauth = createOauth(child);
                    authentication.setOauth(oauth);
                } else if (name.equalsIgnoreCase(Constant.BASIC_AUTH)) {
                    EndpointHttpAuthenticationBasicAuth basicAuth = createBasicAuth(child);
                    authentication.setBasicAuth(basicAuth);
                }
            }
        }
        return authentication;
    }

    private EndpointHttpAuthenticationBasicAuth createBasicAuth(DOMNode element) {

        EndpointHttpAuthenticationBasicAuth basicAuth = new EndpointHttpAuthenticationBasicAuth();
        basicAuth.elementNode((DOMElement) element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.USERNAME)) {
                    STNode username = new STNode();
                    username.elementNode((DOMElement) child);
                    basicAuth.setUsername(username);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.PASSWORD)) {
                    STNode password = new STNode();
                    password.elementNode((DOMElement) child);
                    basicAuth.setPassword(password);
                }
            }
        }
        return basicAuth;
    }

    private EndpointHttpAuthenticationOauth createOauth(DOMNode element) {

        EndpointHttpAuthenticationOauth oauth = new EndpointHttpAuthenticationOauth();
        oauth.elementNode((DOMElement) element);
        List<DOMNode> children = element.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                if (child.getNodeName().equalsIgnoreCase(Constant.AUTHORIZATION_CODE)) {
                    EndpointHttpAuthenticationOauthAuthorizationCode authorizationCode = createAuthorizationCode(child);
                    oauth.setAuthorizationCode(authorizationCode);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.CLIENT_CREDENTIALS)) {
                    EndpointHttpAuthenticationOauthClientCredentials clientCredentials = createClientCredentials(child);
                    oauth.setClientCredentials(clientCredentials);
                } else if (child.getNodeName().equalsIgnoreCase(Constant.PASSWORD_CREDENTIALS)) {
                    EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials =
                            createPasswordCredentials(child);
                    oauth.setPasswordCredentials(passwordCredentials);
                }
            }
        }
        return oauth;
    }

    private EndpointHttpAuthenticationOauthAuthorizationCode createAuthorizationCode(DOMNode node) {

        EndpointHttpAuthenticationOauthAuthorizationCode authorizationCode =
                new EndpointHttpAuthenticationOauthAuthorizationCode();
        authorizationCode.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.CLIENT_ID)) {
                    STNode clientId = new STNode();
                    clientId.elementNode((DOMElement) child);
                    authorizationCode.setClientId(clientId);
                } else if (name.equalsIgnoreCase(Constant.CLIENT_SECRET)) {
                    STNode clientSecret = new STNode();
                    clientSecret.elementNode((DOMElement) child);
                    authorizationCode.setClientSecret(clientSecret);
                } else if (name.equalsIgnoreCase(Constant.REFRESH_TOKEN)) {
                    STNode refreshToken = new STNode();
                    refreshToken.elementNode((DOMElement) child);
                    authorizationCode.setRefreshToken(refreshToken);
                } else if (name.equalsIgnoreCase(Constant.TOKEN_URL)) {
                    STNode tokenUrl = new STNode();
                    tokenUrl.elementNode((DOMElement) child);
                    authorizationCode.setTokenUrl(tokenUrl);
                } else if (name.equalsIgnoreCase(Constant.REQUEST_PARAMETERS)) {
                    EndpointHttpAuthenticationOauthRequestParameters requestParameters =
                            EndpointUtils.createOauthRequestParameters(child);
                    requestParameters.elementNode((DOMElement) child);
                    authorizationCode.setRequestParameters(requestParameters);
                } else if (name.equalsIgnoreCase(Constant.AUTH_MODE)) {
                    STNode authMode = new STNode();
                    authMode.elementNode((DOMElement) child);
                    authorizationCode.setAuthMode(authMode);
                }
            }
        }
        return authorizationCode;
    }

    private EndpointHttpAuthenticationOauthClientCredentials createClientCredentials(DOMNode node) {

        EndpointHttpAuthenticationOauthClientCredentials clientCredentials =
                new EndpointHttpAuthenticationOauthClientCredentials();
        clientCredentials.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.CLIENT_ID)) {
                    STNode clientId = new STNode();
                    clientId.elementNode((DOMElement) child);
                    clientCredentials.setClientId(clientId);
                } else if (name.equalsIgnoreCase(Constant.CLIENT_SECRET)) {
                    STNode clientSecret = new STNode();
                    clientSecret.elementNode((DOMElement) child);
                    clientCredentials.setClientSecret(clientSecret);
                } else if (name.equalsIgnoreCase(Constant.REFRESH_TOKEN)) {
                    STNode refreshToken = new STNode();
                    refreshToken.elementNode((DOMElement) child);
                    clientCredentials.setRefreshToken(refreshToken);
                } else if (name.equalsIgnoreCase(Constant.TOKEN_URL)) {
                    STNode tokenUrl = new STNode();
                    tokenUrl.elementNode((DOMElement) child);
                    clientCredentials.setTokenUrl(tokenUrl);
                } else if (name.equalsIgnoreCase(Constant.REQUEST_PARAMETERS)) {
                    EndpointHttpAuthenticationOauthRequestParameters requestParameters =
                            EndpointUtils.createOauthRequestParameters(child);
                    requestParameters.elementNode((DOMElement) child);
                    clientCredentials.setRequestParameters(requestParameters);
                } else if (name.equalsIgnoreCase(Constant.AUTH_MODE)) {
                    STNode authMode = new STNode();
                    authMode.elementNode((DOMElement) child);
                    clientCredentials.setAuthMode(authMode);
                }
            }
        }
        return clientCredentials;
    }

    private EndpointHttpAuthenticationOauthPasswordCredentials createPasswordCredentials(DOMNode node) {

        EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials =
                new EndpointHttpAuthenticationOauthPasswordCredentials();
        passwordCredentials.elementNode((DOMElement) node);
        List<DOMNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (DOMNode child : children) {
                String name = child.getNodeName();
                if (name.equalsIgnoreCase(Constant.USERNAME)) {
                    STNode username = new STNode();
                    username.elementNode((DOMElement) child);
                    passwordCredentials.setUsername(username);
                } else if (name.equalsIgnoreCase(Constant.PASSWORD)) {
                    STNode password = new STNode();
                    password.elementNode((DOMElement) child);
                    passwordCredentials.setPassword(password);
                } else if (name.equalsIgnoreCase(Constant.CLIENT_ID)) {
                    STNode clientId = new STNode();
                    clientId.elementNode((DOMElement) child);
                    passwordCredentials.setClientId(clientId);
                } else if (name.equalsIgnoreCase(Constant.CLIENT_SECRET)) {
                    STNode clientSecret = new STNode();
                    clientSecret.elementNode((DOMElement) child);
                    passwordCredentials.setClientSecret(clientSecret);
                } else if (name.equalsIgnoreCase(Constant.REFRESH_TOKEN)) {
                    STNode refreshToken = new STNode();
                    refreshToken.elementNode((DOMElement) child);
                    passwordCredentials.setRefreshToken(refreshToken);
                } else if (name.equalsIgnoreCase(Constant.TOKEN_URL)) {
                    STNode tokenUrl = new STNode();
                    tokenUrl.elementNode((DOMElement) child);
                    passwordCredentials.setTokenUrl(tokenUrl);
                } else if (name.equalsIgnoreCase(Constant.REQUEST_PARAMETERS)) {
                    EndpointHttpAuthenticationOauthRequestParameters requestParameters =
                            EndpointUtils.createOauthRequestParameters(child);
                    passwordCredentials.setRequestParameters(requestParameters);
                } else if (name.equalsIgnoreCase(Constant.AUTH_MODE)) {
                    STNode authMode = new STNode();
                    authMode.elementNode((DOMElement) child);
                    passwordCredentials.setAuthMode(authMode);
                }
            }
        }
        return passwordCredentials;
    }

    @Override
    public void populateAttributes(STNode node, DOMElement element) {

        String uriTemplate = element.getAttribute(Constant.URI_TEMPLATE);
        if (uriTemplate != null && !uriTemplate.isEmpty()) {
            ((EndpointHttp) node).setUriTemplate(uriTemplate);
        }
        String method = element.getAttribute(Constant.METHOD);
        HttpMethod methodEnum = Utils.getEnumFromValue(method, HttpMethod.class);
        if (methodEnum != null) {
            ((EndpointHttp) node).setMethod(methodEnum);
        }
        String statistics = element.getAttribute(Constant.STATISTICS);
        if (statistics != null && !statistics.isEmpty()) {
            ((EndpointHttp) node).setStatistics(statistics);
        }
        String trace = element.getAttribute(Constant.TRACE);
        if (trace != null && !trace.isEmpty()) {
            ((EndpointHttp) node).setTrace(trace);
        }
    }
}
