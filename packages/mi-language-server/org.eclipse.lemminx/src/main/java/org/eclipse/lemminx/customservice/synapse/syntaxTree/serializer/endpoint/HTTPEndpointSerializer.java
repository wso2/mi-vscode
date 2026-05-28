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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.endpoint;

import org.apache.axiom.om.OMElement;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EnableSecAndEnableRMAndEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttp;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthentication;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationBasicAuth;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauth;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthAuthorizationCode;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthClientCredentials;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthPasswordCredentials;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.http.EndpointHttpAuthenticationOauthRequestParameters;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.common.Parameter;

public class HTTPEndpointSerializer extends EndpointSerializer {

    @Override
    protected OMElement serializeSpecificEndpoint(NamedEndpoint endpoint) {

        EndpointHttp httpEndpoint = endpoint.getHttp();
        if (httpEndpoint == null) {
            handleException("Could not find the http endpoint.");
        }

        OMElement httpElement = serializeHttpEndpoint(httpEndpoint);

        return httpElement;
    }

    protected OMElement serializeHttpEndpoint(EndpointHttp httpEndpoint) {

        OMElement httpElement = fac.createOMElement("http", synNS);
        serializeEndpointConfigurations(httpElement, httpEndpoint);
        return httpElement;
    }

    private void serializeEndpointConfigurations(OMElement httpElement, EndpointHttp httpEndpoint) {

        serializeQOSProperties(httpElement, httpEndpoint);
        serializeCommonEndpointProperties(httpElement, httpEndpoint);
        serializeAuthenticationProperties(httpElement, httpEndpoint);
        serializeAttributes(httpElement, httpEndpoint);
    }

    private void serializeQOSProperties(OMElement httpElement, EndpointHttp httpEndpoint) {

        EnableSecAndEnableRMAndEnableAddressing configs = httpEndpoint.getEnableSecAndEnableRMAndEnableAddressing();
        if (configs != null) {
            if (configs.getEnableSec() != null && configs.getEnableSec().isPresent()) {
                OMElement enableSecElt = serializeEnableSec(configs.getEnableSec().get());
                httpElement.addChild(enableSecElt);
            }
            if (configs.getEnableRM() != null && configs.getEnableRM().isPresent()) {
                OMElement enableRMElt = serializeEnableRM(configs.getEnableRM().get());
                httpElement.addChild(enableRMElt);
            }
            if (configs.getEnableAddressing() != null && configs.getEnableAddressing().isPresent()) {
                OMElement enableAddressingElt = serializeEnableAddressing(configs.getEnableAddressing().get());
                httpElement.addChild(enableAddressingElt);
            }
        }
    }

    private void serializeCommonEndpointProperties(OMElement httpElement, EndpointHttp httpEndpoint) {

        EnableSecAndEnableRMAndEnableAddressing configs = httpEndpoint.getEnableSecAndEnableRMAndEnableAddressing();
        if (configs != null) {
            if (configs.getTimeout() != null && configs.getTimeout().isPresent()) {
                OMElement timeoutElt = serializeTimeout(configs.getTimeout().get());
                httpElement.addChild(timeoutElt);
            }
            if (configs.getSuspendOnFailure() != null && configs.getSuspendOnFailure().isPresent()) {
                OMElement suspendOnFailureElt = serializeSuspendOnFailure(configs.getSuspendOnFailure().get());
                httpElement.addChild(suspendOnFailureElt);
            }
            if (configs.getMarkForSuspension() != null && configs.getMarkForSuspension().isPresent()) {
                OMElement markForSuspensionElt = serializeMarkForSuspension(configs.getMarkForSuspension().get());
                httpElement.addChild(markForSuspensionElt);
            }
            if (configs.getRetryConfig() != null && configs.getRetryConfig().isPresent()) {
                OMElement retryConfigElt = serializeRetryConfig(configs.getRetryConfig().get());
                httpElement.addChild(retryConfigElt);
            }
        }
    }

    private void serializeAuthenticationProperties(OMElement httpElement, EndpointHttp httpEndpoint) {

        EnableSecAndEnableRMAndEnableAddressing configs = httpEndpoint.getEnableSecAndEnableRMAndEnableAddressing();
        if (configs != null && configs.getAuthentication() != null && configs.getAuthentication().isPresent()) {
            OMElement authenticationElt = fac.createOMElement("authentication", synNS);
            EndpointHttpAuthentication authentication = configs.getAuthentication().get();
            if (authentication.getBasicAuth() != null) {
                serializeBasicAuth(authenticationElt, authentication.getBasicAuth());
            } else if (authentication.getOauth() != null) {
                serializeOAuth(authenticationElt, authentication.getOauth());
            }
            httpElement.addChild(authenticationElt);
        }
    }

    private void serializeBasicAuth(OMElement httpElement, EndpointHttpAuthenticationBasicAuth basicAuth) {

        OMElement basicAuthElt = fac.createOMElement("basicAuth", synNS);
        if (basicAuth.getUsername() != null) {
            OMElement usernameElt = fac.createOMElement("username", synNS);
            usernameElt.setText(basicAuth.getUsername().getTextNode());
            basicAuthElt.addChild(usernameElt);
        }
        if (basicAuth.getPassword() != null) {
            OMElement passwordElt = fac.createOMElement("password", synNS);
            passwordElt.setText(basicAuth.getPassword().getTextNode());
            basicAuthElt.addChild(passwordElt);
        }
        httpElement.addChild(basicAuthElt);
    }

    private void serializeOAuth(OMElement httpElement, EndpointHttpAuthenticationOauth oauth) {

        OMElement oauthElt = fac.createOMElement("oauth", synNS);
        if (oauth.getAuthorizationCode() != null) {
            OMElement authCodeElt = fac.createOMElement("authorizationCode", synNS);
            EndpointHttpAuthenticationOauthAuthorizationCode authCode = oauth.getAuthorizationCode();
            serializeOauthParams(authCodeElt, authCode);
            oauthElt.addChild(authCodeElt);
        } else if (oauth.getClientCredentials() != null) {
            OMElement clientCredentialsElt = fac.createOMElement("clientCredentials", synNS);
            EndpointHttpAuthenticationOauthClientCredentials clientCredentials = oauth.getClientCredentials();
            serializeOauthParams(clientCredentialsElt, clientCredentials);
            oauthElt.addChild(clientCredentialsElt);
        } else if (oauth.getPasswordCredentials() != null) {
            OMElement passwordCredentialsElt = serializePasswordCredentials(oauth.getPasswordCredentials());
            oauthElt.addChild(passwordCredentialsElt);
        }
        httpElement.addChild(oauthElt);
    }

    private OMElement serializePasswordCredentials(EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials) {

        OMElement passwordCredentialsElt = fac.createOMElement("passwordCredentials", synNS);
        if (passwordCredentials.getUsername() != null) {
            OMElement usernameElt = fac.createOMElement("username", synNS);
            usernameElt.setText(passwordCredentials.getUsername().getTextNode());
            passwordCredentialsElt.addChild(usernameElt);
        }
        if (passwordCredentials.getPassword() != null) {
            OMElement passwordElt = fac.createOMElement("password", synNS);
            passwordElt.setText(passwordCredentials.getPassword().getTextNode());
            passwordCredentialsElt.addChild(passwordElt);
        }
        serializeOauthParams(passwordCredentialsElt, passwordCredentials);
        return passwordCredentialsElt;
    }

    private void serializeOauthParams(OMElement authorizationCodeElt,
                                      EndpointHttpAuthenticationOauthAuthorizationCode authorizationCode) {

        if (authorizationCode.getClientId() != null) {
            OMElement clientIdElt = fac.createOMElement("clientId", synNS);
            clientIdElt.setText(authorizationCode.getClientId().getTextNode());
            authorizationCodeElt.addChild(clientIdElt);
        }
        if (authorizationCode.getClientSecret() != null) {
            OMElement clientSecretElt = fac.createOMElement("clientSecret", synNS);
            clientSecretElt.setText(authorizationCode.getClientSecret().getTextNode());
            authorizationCodeElt.addChild(clientSecretElt);
        }
        if (authorizationCode.getRefreshToken() != null) {
            OMElement refreshTokenElt = fac.createOMElement("refreshToken", synNS);
            refreshTokenElt.setText(authorizationCode.getRefreshToken().getTextNode());
            authorizationCodeElt.addChild(refreshTokenElt);
        }
        if (authorizationCode.getTokenUrl() != null) {
            OMElement tokenUrlElt = fac.createOMElement("tokenUrl", synNS);
            tokenUrlElt.setText(authorizationCode.getTokenUrl().getTextNode());
            authorizationCodeElt.addChild(tokenUrlElt);
        }
        if (authorizationCode.getRequestParameters() != null) {
            OMElement requestParametersElt = serializeRequestParameters(authorizationCode.getRequestParameters());
            authorizationCodeElt.addChild(requestParametersElt);
        }
        if (authorizationCode.getAuthMode() != null) {
            OMElement authModeElt = fac.createOMElement("authMode", synNS);
            authModeElt.setText(authorizationCode.getAuthMode().getTextNode());
            authorizationCodeElt.addChild(authModeElt);
        }
    }

    private void serializeOauthParams(OMElement clientCredentialsElt,
                                      EndpointHttpAuthenticationOauthClientCredentials clientCredentials) {

        if (clientCredentials.getClientId() != null) {
            OMElement clientIdElt = fac.createOMElement("clientId", synNS);
            clientIdElt.setText(clientCredentials.getClientId().getTextNode());
            clientCredentialsElt.addChild(clientIdElt);
        }
        if (clientCredentials.getClientSecret() != null) {
            OMElement clientSecretElt = fac.createOMElement("clientSecret", synNS);
            clientSecretElt.setText(clientCredentials.getClientSecret().getTextNode());
            clientCredentialsElt.addChild(clientSecretElt);
        }
        if (clientCredentials.getRefreshToken() != null) {
            OMElement refreshTokenElt = fac.createOMElement("refreshToken", synNS);
            refreshTokenElt.setText(clientCredentials.getRefreshToken().getTextNode());
            clientCredentialsElt.addChild(refreshTokenElt);
        }
        if (clientCredentials.getTokenUrl() != null) {
            OMElement tokenUrlElt = fac.createOMElement("tokenUrl", synNS);
            tokenUrlElt.setText(clientCredentials.getTokenUrl().getTextNode());
            clientCredentialsElt.addChild(tokenUrlElt);
        }
        if (clientCredentials.getRequestParameters() != null) {
            OMElement requestParametersElt = serializeRequestParameters(clientCredentials.getRequestParameters());
            clientCredentialsElt.addChild(requestParametersElt);
        }
        if (clientCredentials.getAuthMode() != null) {
            OMElement authModeElt = fac.createOMElement("authMode", synNS);
            authModeElt.setText(clientCredentials.getAuthMode().getTextNode());
            clientCredentialsElt.addChild(authModeElt);
        }
    }

    private void serializeOauthParams(OMElement passwordCredentialsElt,
                                      EndpointHttpAuthenticationOauthPasswordCredentials passwordCredentials) {

        if (passwordCredentials.getClientId() != null) {
            OMElement clientIdElt = fac.createOMElement("clientId", synNS);
            clientIdElt.setText(passwordCredentials.getClientId().getTextNode());
            passwordCredentialsElt.addChild(clientIdElt);
        }
        if (passwordCredentials.getClientSecret() != null) {
            OMElement clientSecretElt = fac.createOMElement("clientSecret", synNS);
            clientSecretElt.setText(passwordCredentials.getClientSecret().getTextNode());
            passwordCredentialsElt.addChild(clientSecretElt);
        }
        if (passwordCredentials.getRefreshToken() != null) {
            OMElement refreshTokenElt = fac.createOMElement("refreshToken", synNS);
            refreshTokenElt.setText(passwordCredentials.getRefreshToken().getTextNode());
            passwordCredentialsElt.addChild(refreshTokenElt);
        }
        if (passwordCredentials.getTokenUrl() != null) {
            OMElement tokenUrlElt = fac.createOMElement("tokenUrl", synNS);
            tokenUrlElt.setText(passwordCredentials.getTokenUrl().getTextNode());
            passwordCredentialsElt.addChild(tokenUrlElt);
        }
        if (passwordCredentials.getRequestParameters() != null) {
            OMElement requestParametersElt = serializeRequestParameters(passwordCredentials.getRequestParameters());
            passwordCredentialsElt.addChild(requestParametersElt);
        }
        if (passwordCredentials.getAuthMode() != null) {
            OMElement authModeElt = fac.createOMElement("authMode", synNS);
            authModeElt.setText(passwordCredentials.getAuthMode().getTextNode());
            passwordCredentialsElt.addChild(authModeElt);
        }
    }

    private OMElement serializeRequestParameters(EndpointHttpAuthenticationOauthRequestParameters requestParameters) {

        OMElement requestParametersElt = fac.createOMElement("requestParameters", synNS);
        Parameter[] parameters = requestParameters.getParameter();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                OMElement parameterElt = fac.createOMElement("parameter", synNS);
                if (parameter.getName() != null) {
                    parameterElt.addAttribute("name", parameter.getName(), nullNS);
                }
                if (parameter.getTextNode() != null) {
                    parameterElt.setText(parameter.getTextNode());
                }
                requestParametersElt.addChild(parameterElt);
            }
        }
        return requestParametersElt;
    }

    private void serializeAttributes(OMElement endpointElement, EndpointHttp httpEndpoint) {

        if (httpEndpoint.getUriTemplate() != null) {
            endpointElement.addAttribute("uri-template", httpEndpoint.getUriTemplate(), nullNS);
        }
        if (httpEndpoint.getMethod() != null) {
            endpointElement.addAttribute("method", httpEndpoint.getMethod().name(), nullNS);
        }
        if (httpEndpoint.getStatistics() != null) {
            endpointElement.addAttribute("statistics", httpEndpoint.getStatistics(), nullNS);
        }
        if (httpEndpoint.getTrace() != null) {
            endpointElement.addAttribute("trace", httpEndpoint.getTrace(), nullNS);
        }
    }
}
