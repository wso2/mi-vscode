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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl11;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.STNode;

import java.util.Optional;

public class AnyTopLevelOptionalElement extends STNode {

    Optional<TImport> _import;
    Optional<TTypes> types;
    Optional<TMessage> message;
    Optional<TPortType> portType;
    Optional<TBinding> binding;
    Optional<TService> service;

    public Optional<TImport> get_import() {

        return _import;
    }

    public void set_import(Optional<TImport> _import) {

        this._import = _import;
    }

    public Optional<TTypes> getTypes() {

        return types;
    }

    public void setTypes(Optional<TTypes> types) {

        this.types = types;
    }

    public Optional<TMessage> getMessage() {

        return message;
    }

    public void setMessage(Optional<TMessage> message) {

        this.message = message;
    }

    public Optional<TPortType> getPortType() {

        return portType;
    }

    public void setPortType(Optional<TPortType> portType) {

        this.portType = portType;
    }

    public Optional<TBinding> getBinding() {

        return binding;
    }

    public void setBinding(Optional<TBinding> binding) {

        this.binding = binding;
    }

    public Optional<TService> getService() {

        return service;
    }

    public void setService(Optional<TService> service) {

        this.service = service;
    }
}
