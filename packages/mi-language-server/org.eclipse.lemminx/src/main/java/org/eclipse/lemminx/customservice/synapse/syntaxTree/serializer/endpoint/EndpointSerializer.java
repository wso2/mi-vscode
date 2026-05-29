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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.EndpointSession;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.NamedEndpoint;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableAddressing;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableRM;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointEnableSec;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointMarkForSuspension;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointParameter;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointProperty;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointRetryConfig;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointSuspendOnFailure;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.endpoint.common.EndpointTimeout;
import org.eclipse.lemminx.customservice.synapse.syntaxTree.serializer.InvalidConfigurationException;
import org.eclipse.lemminx.customservice.synapse.utils.Constant;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class EndpointSerializer {

    private static Logger log = Logger.getLogger(EndpointSerializer.class.getName());

    protected static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static final OMNamespace synNS = Constant.SYNAPSE_OMNAMESPACE;
    protected static final OMNamespace nullNS
            = fac.createOMNamespace(Constant.EMPTY_STRING, "");

    public static OMElement serializeEndpoint(NamedEndpoint endpoint) {

        EndpointSerializer endpointSerializer = getEndpointSerializer(endpoint);
        OMElement endpointElt = null;
        if (endpointSerializer != null) {
            if (endpointSerializer instanceof ReferenceEndpointSerializer ||
                    endpointSerializer instanceof TemplateEndpointSerializer) {
                return endpointSerializer.serializeSpecificEndpoint(endpoint);
            }
            endpointElt = fac.createOMElement("endpoint", synNS);
            if (endpoint.getName() != null) {
                endpointElt.addAttribute("name", endpoint.getName(), nullNS);
            } else {
                handleException("Endpoint name is required.");
            }
            OMElement childElement = endpointSerializer.serializeSpecificEndpoint(endpoint);
            endpointElt.addChild(childElement);
            serializeEndpointProperties(endpointElt, endpoint.getProperty());
            serializeEndpointParameters(endpointElt, endpoint.getParameter());
            if (endpoint.getDescription() != null) {
                OMElement descriptionElement = fac.createOMElement("description", synNS);
                descriptionElement.setText(endpoint.getDescription());
                endpointElt.addChild(descriptionElement);
            }
        }
        return endpointElt;
    }

    protected abstract OMElement serializeSpecificEndpoint(NamedEndpoint endpoint);

    private static EndpointSerializer getEndpointSerializer(NamedEndpoint endpoint) {

        switch (endpoint.getType()) {
            case NAMED_ENDPOINT:
                return new ReferenceEndpointSerializer();
            case HTTP_ENDPOINT:
                return new HTTPEndpointSerializer();
            case ADDRESS_ENDPOINT:
                return new AddressEndpointSerializer();
            case WSDL_ENDPOINT:
                return new WSDLEndpointSerializer();
            case DEFAULT_ENDPOINT:
                return new DefaultEndpointSerializer();
            case RECIPIENT_LIST_ENDPOINT:
                return new RecipientListEndpointSerializer();
            case LOAD_BALANCE_ENDPOINT:
                return new LoadBalanceEndpointSerializer();
            case FAIL_OVER_ENDPOINT:
                return new FailoverEndpointSerializer();
            case TEMPLATE_ENDPOINT:
                return new TemplateEndpointSerializer();
            default:
                handleException("Invalid endpoint type");
                return null;
        }
    }

    protected static final void serializeEndpointProperties(OMElement endpointElement, EndpointProperty[] properties) {

        if (properties != null) {
            for (EndpointProperty property : properties) {
                OMElement propertyElement = fac.createOMElement("property", synNS);
                if (property.getName() != null) {
                    propertyElement.addAttribute("name", property.getName(), nullNS);
                } else {
                    handleException("Endpoint property name is required.");
                }
                if (property.getValue() != null) {
                    propertyElement.addAttribute("value", property.getValue(), nullNS);
                }
                if (property.getScope() != null) {
                    propertyElement.addAttribute("scope", property.getScope().getValue(), nullNS);
                }
                endpointElement.addChild(propertyElement);
            }
        }
    }

    protected static final void serializeEndpointParameters(OMElement endpointElement, EndpointParameter[] parameters) {

        if (parameters != null) {
            for (EndpointParameter parameter : parameters) {
                OMElement parameterElement = fac.createOMElement("parameter", synNS);
                String paramNSPrefix = parameter.getParamNamespacePrefix();
                if (paramNSPrefix != null) {
                    String uri = parameter.getNamespaces().get("xmlns:" + paramNSPrefix);
                    if (uri != null) {
                        parameterElement.setNamespace(fac.createOMNamespace(uri, paramNSPrefix));
                    } else {
                        handleException("Namespace:" + paramNSPrefix + " is not defined in the parameter");
                    }
                }
                if (parameter.getName() != null) {
                    parameterElement.addAttribute("name", parameter.getName(), nullNS);
                } else {
                    handleException("Endpoint parameter name is required.");
                }
                if (parameter.getValue() != null) {
                    parameterElement.addAttribute("value", parameter.getValue(), nullNS);
                }
                endpointElement.addChild(parameterElement);
            }
        }
    }

    protected OMElement serializeSuspendOnFailure(EndpointSuspendOnFailure suspendOnFailure) {

        OMElement suspendOnFailureElement = fac.createOMElement("suspendOnFailure", synNS);
        if (suspendOnFailure.getErrorCodes() != null) {
            OMElement errorCodesElement = fac.createOMElement("errorCodes", synNS);
            errorCodesElement.setText(suspendOnFailure.getErrorCodes().getTextNode());
            suspendOnFailureElement.addChild(errorCodesElement);
        }
        if (suspendOnFailure.getInitialDuration() != null) {
            OMElement initialDurationElement = fac.createOMElement("initialDuration", synNS);
            initialDurationElement.setText(suspendOnFailure.getInitialDuration().getTextNode());
            suspendOnFailureElement.addChild(initialDurationElement);
        }
        if (suspendOnFailure.getProgressionFactor() != null) {
            OMElement progressionFactorElement = fac.createOMElement("progressionFactor", synNS);
            progressionFactorElement.setText(suspendOnFailure.getProgressionFactor().getTextNode());
            suspendOnFailureElement.addChild(progressionFactorElement);
        }
        if (suspendOnFailure.getMaximumDuration() != null) {
            OMElement maximumDurationElement = fac.createOMElement("maximumDuration", synNS);
            maximumDurationElement.setText(suspendOnFailure.getMaximumDuration().getTextNode());
            suspendOnFailureElement.addChild(maximumDurationElement);
        }
        return suspendOnFailureElement;
    }

    protected OMElement serializeMarkForSuspension(EndpointMarkForSuspension markForSuspension) {

        OMElement markForSuspensionElement = fac.createOMElement("markForSuspension", synNS);
        if (markForSuspension.getErrorCodes() != null) {
            OMElement errorCodesElement = fac.createOMElement("errorCodes", synNS);
            errorCodesElement.setText(markForSuspension.getErrorCodes().getTextNode());
            markForSuspensionElement.addChild(errorCodesElement);
        }
        if (markForSuspension.getRetriesBeforeSuspension() != null) {
            OMElement retriesBeforeSuspensionElement = fac.createOMElement("retriesBeforeSuspension", synNS);
            retriesBeforeSuspensionElement.setText(markForSuspension.getRetriesBeforeSuspension().getTextNode());
            markForSuspensionElement.addChild(retriesBeforeSuspensionElement);
        }
        if (markForSuspension.getRetryDelay() != null) {
            OMElement retryDelayElement = fac.createOMElement("retryDelay", synNS);
            retryDelayElement.setText(markForSuspension.getRetryDelay().getTextNode());
            markForSuspensionElement.addChild(retryDelayElement);
        }
        return markForSuspensionElement;
    }

    protected OMElement serializeTimeout(EndpointTimeout timeout) {

        OMElement timeoutElement = fac.createOMElement("timeout", synNS);
        if (timeout.getDuration() != null) {
            OMElement durationElement = fac.createOMElement("duration", synNS);
            durationElement.setText(timeout.getDuration().getTextNode());
            timeoutElement.addChild(durationElement);
        }
        if (timeout.getResponseAction() != null) {
            OMElement responseActionElement = fac.createOMElement("responseAction", synNS);
            responseActionElement.setText(timeout.getResponseAction().getTextNode());
            timeoutElement.addChild(responseActionElement);
        }
        return timeoutElement;
    }

    protected OMElement serializeRetryConfig(EndpointRetryConfig retryConfig) {

        OMElement retryConfigElement = fac.createOMElement("retryConfig", synNS);
        if (retryConfig.getEnabledErrorCodes() != null) {
            OMElement enabledErrorCodesElement = fac.createOMElement("enabledErrorCodes", synNS);
            enabledErrorCodesElement.setText(retryConfig.getEnabledErrorCodes().getTextNode());
            retryConfigElement.addChild(enabledErrorCodesElement);
        }
        if (retryConfig.getDisabledErrorCodes() != null) {
            OMElement disabledErrorCodesElement = fac.createOMElement("disabledErrorCodes", synNS);
            disabledErrorCodesElement.setText(retryConfig.getDisabledErrorCodes().getTextNode());
            retryConfigElement.addChild(disabledErrorCodesElement);
        }
        return retryConfigElement;
    }

    protected OMElement serializeEnableAddressing(EndpointEnableAddressing enableAddressing) {

        OMElement enableAddressingElement = fac.createOMElement("enableAddressing", synNS);
        if (enableAddressing.isSeparateListener()) {
            enableAddressingElement.addAttribute("separateListener", "true", nullNS);
        }
        if (enableAddressing.getVersion() != null) {
            enableAddressingElement.addAttribute("version", enableAddressing.getVersion().getValue(), nullNS);
        }
        return enableAddressingElement;
    }

    protected OMElement serializeEnableSec(EndpointEnableSec enableSec) {

        OMElement enableSecElement = fac.createOMElement("enableSec", synNS);
        if (enableSec.getPolicy() != null) {
            enableSecElement.addAttribute("policy", enableSec.getPolicy(), nullNS);
        } else if (enableSec.getInboundPolicy() != null && enableSec.getOutboundPolicy() != null) {
            enableSecElement.addAttribute("inboundPolicy", enableSec.getInboundPolicy(), nullNS);
            enableSecElement.addAttribute("outboundPolicy", enableSec.getOutboundPolicy(), nullNS);
        } else {
            handleException("Invalid security configuration");
        }
        return enableSecElement;
    }

    protected OMElement serializeEnableRM(EndpointEnableRM enableRM) {

        OMElement enableRMElement = fac.createOMElement("enableRM", synNS);
        if (enableRM.getPolicy() != null) {
            enableRMElement.addAttribute("policy", enableRM.getPolicy(), nullNS);
        }
        return enableRMElement;
    }

    protected OMElement serializeSession(EndpointSession session) {

        OMElement sessionElement = fac.createOMElement("session", synNS);
        if (session.getSessionTimeout() >= 0) {
            OMElement sessionTimeoutElement = fac.createOMElement("sessionTimeout", synNS);
            sessionTimeoutElement.setText(String.valueOf(session.getSessionTimeout()));
            sessionElement.addChild(sessionTimeoutElement);
        }
        if (session.getType() != null) {
            sessionElement.addAttribute("type", session.getType().name(), nullNS);
        }
        return sessionElement;
    }

    protected static void handleException(String s) {

        log.log(Level.SEVERE, s);
        throw new InvalidConfigurationException(s);
    }

}
