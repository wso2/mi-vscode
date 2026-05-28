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

import org.eclipse.lemminx.customservice.synapse.syntaxTree.pojo.mediator.transformation.Datamapper;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatamapperMediator {
    private static final String NAME_REGEX = "(gov|resources):/?datamapper/(.*)";
    private static final String CONFIG_REGEX = "((gov|resources):/?datamapper/([^/]+))(?:/.*)?\\.dmc";

    public static Either<Map<String, Object>, Map<Range, Map<String, Object>>> processData430(Map<String, Object> data,
                                                                                              Datamapper datamapper,
                                                                                              List<String> dirtyFields) {

        Pattern pattern = Pattern.compile(NAME_REGEX);
        Matcher matcher = pattern.matcher((String) data.get("name"));
        if (matcher.find()) {
            String datamapperName = matcher.group(2);
            if (datamapperName.endsWith(".dmc")) {
                //Removing the .dmc extension from the datamapper name
                String datamapperBaseName = data.get("name").toString()
                        .substring(0, data.get("name").toString().length() - 4);
                String configurationLocalPath = datamapperBaseName + ".dmc";
                String inputSchemaLocalPath = datamapperBaseName + "_inputSchema.json";
                String outputSchemaLocalPath = datamapperBaseName + "_outputSchema.json";
                data.put("configurationLocalPath", configurationLocalPath);
                data.put("inputSchemaLocalPath", inputSchemaLocalPath);
                data.put("outputSchemaLocalPath", outputSchemaLocalPath);
            }  else {
                String configurationLocalPath = data.get("name") + "/" + datamapperName + ".dmc";
                String inputSchemaLocalPath = data.get("name") + "/" + datamapperName + "_inputSchema.json";
                String outputSchemaLocalPath = data.get("name") + "/" + datamapperName + "_outputSchema.json";
                data.put("configurationLocalPath", configurationLocalPath);
                data.put("inputSchemaLocalPath", inputSchemaLocalPath);
                data.put("outputSchemaLocalPath", outputSchemaLocalPath);
            }
        }
        if (data.containsKey("traceFilter") && Boolean.parseBoolean(String.valueOf(data.get("traceFilter")))) {
            data.put("traceFilter", true);
        } else {
            data.put("traceFilter", false);
        }
        return Either.forLeft(data);

    }

    public static Map<String, Object> getDataFromST430(Datamapper node) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", node.getDescription());
        data.put("inputType", node.getInputType());
        data.put("outputType", node.getOutputType());
        String configPath = node.getConfig();
        if (configPath != null) {
            Pattern pattern = Pattern.compile(CONFIG_REGEX);
            Matcher matcher = pattern.matcher(configPath);
            if (matcher.find()) {
                data.put("name", matcher.group(1));
            }
        }
        data.put("traceFilter", "enable".equals(node.getTraceFilter()));
        return data;
    }
}
