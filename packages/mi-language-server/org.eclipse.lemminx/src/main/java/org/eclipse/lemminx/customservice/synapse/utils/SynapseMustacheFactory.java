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

package org.eclipse.lemminx.customservice.synapse.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

public class SynapseMustacheFactory extends DefaultMustacheFactory {

    @Override
    public void encode(String value, Writer writer) {

        String escapedValue = Utils.escapeXML(value);
        try {
            writer.write(escapedValue);
        } catch (IOException e) {
            throw new MustacheException("Failed to write encode value: " + value);
        }
    }
}
