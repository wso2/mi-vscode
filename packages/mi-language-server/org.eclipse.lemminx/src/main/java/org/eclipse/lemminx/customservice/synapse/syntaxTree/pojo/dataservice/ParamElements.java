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

package org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.dataservice;

import java.util.Optional;

public class ParamElements {

    Optional<ParamValidateCustom> validateCustom;
    Optional<ParamValidateLength> validateLength;
    Optional<ParamValidatePattern> validatePattern;
    Optional<ParamValidateLongRange> validateLongRange;
    Optional<ParamValidateDoubleRange> validateDoubleRange;

    public Optional<ParamValidateCustom> getValidateCustom() {

        return validateCustom;
    }

    public void setValidateCustom(Optional<ParamValidateCustom> validateCustom) {

        this.validateCustom = validateCustom;
    }

    public Optional<ParamValidateLength> getValidateLength() {

        return validateLength;
    }

    public void setValidateLength(Optional<ParamValidateLength> validateLength) {

        this.validateLength = validateLength;
    }

    public Optional<ParamValidatePattern> getValidatePattern() {

        return validatePattern;
    }

    public void setValidatePattern(Optional<ParamValidatePattern> validatePattern) {

        this.validatePattern = validatePattern;
    }

    public Optional<ParamValidateLongRange> getValidateLongRange() {

        return validateLongRange;
    }

    public void setValidateLongRange(Optional<ParamValidateLongRange> validateLongRange) {

        this.validateLongRange = validateLongRange;
    }

    public Optional<ParamValidateDoubleRange> getValidateDoubleRange() {

        return validateDoubleRange;
    }

    public void setValidateDoubleRange(Optional<ParamValidateDoubleRange> validateDoubleRange) {

        this.validateDoubleRange = validateDoubleRange;
    }
}
