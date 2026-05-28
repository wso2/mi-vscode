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

package org.eclipse.lemminx.customservice.synapse.mediatorService.mediators;

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.advanced.Transaction;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionMediator {
    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Transaction transaction,
                                                                                              List<String> dirtyFields) {
        if (data.containsKey("action") && data.get("action") instanceof String) {
            String action = (String) data.get("action");
            switch (action) {
                case "Commit transaction":
                    data.put("action", "commit");
                    break;
                case "Fault if no transaction":
                    data.put("action", "fault-if-no-tx");
                    break;
                case "Initiate new transaction":
                    data.put("action", "new");
                    break;
                case "Resume transaction":
                    data.put("action", "resume");
                    break;
                case "Suspend transaction":
                    data.put("action", "suspend");
                    break;
                case "Rollback transaction":
                    data.put("action", "rollback");
                    break;
                case "Use existing or initiate transaction":
                    data.put("action", "use-existing-or-new");
                    break;
                default:
                    data.put("action", "");
                    break;
            }
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Transaction node) {

        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        if (node.getAction() != null) {
            switch (node.getAction()) {
                case COMMIT:
                    data.put("action", "Commit transaction");
                    break;
                case FAULT_IF_NO_TX:
                    data.put("action", "Fault if no transaction");
                    break;
                case NEW:
                    data.put("action", "Initiate new transaction");
                    break;
                case RESUME:
                    data.put("action", "Resume transaction");
                    break;
                case SUSPEND:
                    data.put("action", "Suspend transaction");
                    break;
                case ROLLBACK:
                    data.put("action", "Rollback transaction");
                    break;
                case USE_EXISTING_OR_NEW:
                    data.put("action", "Use existing or initiate transaction");
                    break;
                default:
                    data.put("action", "");
                    break;
            }
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
