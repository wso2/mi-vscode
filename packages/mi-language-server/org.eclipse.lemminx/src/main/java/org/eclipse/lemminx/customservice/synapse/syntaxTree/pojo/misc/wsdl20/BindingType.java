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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.misc.wsdl20;

import java.util.List;

public class BindingType extends ExtensibleDocumentedType {

    Object otherAttributes;
    List<Object> operationOrFaultOrAny;
    String name;
    String type;
    String _interface;

    @Override
    public Object getOtherAttributes() {

        return otherAttributes;
    }

    @Override
    public void setOtherAttributes(Object otherAttributes) {

        this.otherAttributes = otherAttributes;
    }

    public List<Object> getOperationOrFaultOrAny() {

        return operationOrFaultOrAny;
    }

    public void setOperationOrFaultOrAny(List<Object> operationOrFaultOrAny) {

        this.operationOrFaultOrAny = operationOrFaultOrAny;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public String get_interface() {

        return _interface;
    }

    public void set_interface(String _interface) {

        this._interface = _interface;
    }
}
