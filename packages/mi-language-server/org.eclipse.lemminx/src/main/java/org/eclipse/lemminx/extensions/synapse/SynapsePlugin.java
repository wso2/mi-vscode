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

package org.eclipse.lemminx.extensions.synapse;

import org.eclipse.lemminx.extensions.synapse.codeactions.SynapseCodeActionParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lsp4j.InitializeParams;

/**
 * LemMinX extension plugin for WSO2 Micro Integrator Synapse XML validation.
 * Registers the SynapseDiagnosticsParticipant for semantic validation rules
 * that cannot be expressed in XSD schemas.
 */
public class SynapsePlugin implements IXMLExtension {

    private final IDiagnosticsParticipant diagnosticsParticipant;
    private final ICodeActionParticipant codeActionParticipant;

    public SynapsePlugin() {
        diagnosticsParticipant = new SynapseDiagnosticsParticipant();
        codeActionParticipant = new SynapseCodeActionParticipant();
    }

    @Override
    public void start(InitializeParams params, XMLExtensionsRegistry registry) {
        registry.registerDiagnosticsParticipant(diagnosticsParticipant);
        registry.registerCodeActionParticipant(codeActionParticipant);
    }

    @Override
    public void stop(XMLExtensionsRegistry registry) {
        registry.unregisterDiagnosticsParticipant(diagnosticsParticipant);
        registry.unregisterCodeActionParticipant(codeActionParticipant);
    }
}
