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

public class BindingOperationType extends ExtensibleDocumentedType {

    Object otherAttributes;
    List<Object> inputOrOutputOrInfault;
    String ref;

    @Override
    public Object getOtherAttributes() {

        return otherAttributes;
    }

    @Override
    public void setOtherAttributes(Object otherAttributes) {

        this.otherAttributes = otherAttributes;
    }

    public List<Object> getInputOrOutputOrInfault() {

        return inputOrOutputOrInfault;
    }

    public void setInputOrOutputOrInfault(List<Object> inputOrOutputOrInfault) {

        this.inputOrOutputOrInfault = inputOrOutputOrInfault;
    }

    public String getRef() {

        return ref;
    }

    public void setRef(String ref) {

        this.ref = ref;
    }
}
