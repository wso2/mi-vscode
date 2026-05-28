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

package org.eclipse.lemminx.customservice.synapse.mediatorService.pojo;

import org.eclipse.lsp4j.TextEdit;

import java.util.ArrayList;
import java.util.List;

public class SynapseConfigResponse {

    private List<TextEdit> textEdits;

    public SynapseConfigResponse() {

        textEdits = new ArrayList<>();
    }

    public SynapseConfigResponse(TextEdit edit) {

        textEdits = new ArrayList<>();
        addTextEdit(edit);
    }

    public List<TextEdit> getTextEdits() {

        sort();
        return textEdits;
    }

    public void addTextEdit(TextEdit edit) {

        if (edit != null) {
            textEdits.add(edit);
        }
    }

    public void sort() {

        textEdits.sort((o1, o2) -> {
            if (o1.getRange().getStart().getLine() == o2.getRange().getStart().getLine()) {
                return o2.getRange().getStart().getCharacter() - o1.getRange().getStart().getCharacter();
            }
            return o2.getRange().getStart().getLine() - o1.getRange().getStart().getLine();
        });
    }

    @Override
    public String toString() {

        return "SynapseConfigResponse{" +
                "textEdits=" + textEdits +
                '}';
    }
}
