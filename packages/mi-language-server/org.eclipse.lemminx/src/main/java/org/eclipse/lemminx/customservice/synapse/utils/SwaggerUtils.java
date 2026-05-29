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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.eclipse.lemminx.customservice.synapse.api.generator.SwaggerConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SwaggerUtils {

    /**
     * Compares the paths of two OpenAPI specifications and returns whether they are equal.
     * <p>
     * This method extracts the {@link PathItem} mappings from both the generated and existing
     * OpenAPI specifications and delegates the comparison to {@code comparePaths}.
     * </p>
     *
     * @param openAPI1 the generated {@link OpenAPI} specification to compare.
     * @param openAPI2 the existing {@link OpenAPI} specification to compare against.
     * @return {@code true} if the paths in both OpenAPI specifications are equal, {@code false} otherwise.
     */
    public static boolean compareOpenAPIs(OpenAPI openAPI1, OpenAPI openAPI2) {
        return comparePaths(openAPI1.getPaths(), openAPI2.getPaths());
    }

    /**
     * Compares two maps of OpenAPI {@link PathItem} objects and determines if they are equal.
     * <p>
     * This method performs a comparison between the two provided path maps, typically extracted
     * from two OpenAPI specifications, to check if their path definitions are identical.
     * </p>
     *
     * @param paths1 the first map of path strings to {@link PathItem} objects.
     * @param paths2 the second map of path strings to {@link PathItem} objects.
     * @return {@code true} if both path maps are equal; {@code false} otherwise.
     */
    private static boolean comparePaths(Map<String, PathItem> paths1, Map<String, PathItem> paths2) {
        Map<String, String> normalizedPaths1 = normalizePaths(paths1);
        Map<String, String> normalizedPaths2 = normalizePaths(paths2);

        Set<String> allNormalizedPaths = new HashSet<>(normalizedPaths1.keySet());
        allNormalizedPaths.addAll(normalizedPaths2.keySet());

        if (allNormalizedPaths.size() != normalizedPaths2.size()) {
            return false;
        }
        for (String normalizedPath : allNormalizedPaths) {
            // Get the original paths using precomputed mappings
            String originalPath1 = normalizedPaths1.get(normalizedPath);
            String originalPath2 = normalizedPaths2.get(normalizedPath);

            if (originalPath1 == null || originalPath2 == null) {
                return false;
            }
            PathItem pathItem1 = paths1.get(originalPath1);
            PathItem pathItem2 = paths2.get(originalPath2);
            if (!compareMethods(pathItem1, pathItem2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes the path strings in the given map of OpenAPI {@link PathItem} objects.
     * <p>
     * This method replaces all path parameter placeholders (e.g., {@code {id}}) with a generic
     * placeholder {@code {}} to allow for structural comparison of paths without considering
     * parameter names. It returns a map where the key is the normalized path and the value
     * is the original path.
     * </p>
     *
     * @param paths the map of path strings to {@link PathItem} objects.
     * @return a map containing normalized path strings as keys and their corresponding original paths as values.
     */
    private static Map<String, String> normalizePaths(Map<String, PathItem> paths) {
        Map<String, String> normalizedPaths = new HashMap<>();

        for (String path : paths.keySet()) {
            String normalizedPath = path.replaceAll(SwaggerConstants.PATH_PARAMETER_REGEX, SwaggerConstants.NORMALIZED_PLACEHOLDER);
            normalizedPaths.put(normalizedPath, path); // Store normalized -> original mapping
        }
        return normalizedPaths;
    }

    /**
     * Compares the HTTP methods defined in two {@link PathItem} objects.
     * <p>
     * This method checks whether both {@link PathItem} instances define the same set of HTTP methods
     * (e.g., GET, POST, PUT) and can be used as part of a structural comparison of OpenAPI paths.
     * </p>
     *
     * @param p1 the first {@link PathItem} to compare.
     * @param p2 the second {@link PathItem} to compare.
     * @return {@code true} if both {@link PathItem} objects define the same HTTP methods; {@code false} otherwise.
     */
    private static boolean compareMethods(PathItem p1, PathItem p2) {
        if (p1 == p2) return true;
        if (p1 == null || p2 == null) return false;

        return compareOperation(p1.getGet(), p2.getGet()) &&
                compareOperation(p1.getPost(), p2.getPost()) &&
                compareOperation(p1.getPut(), p2.getPut()) &&
                compareOperation(p1.getDelete(), p2.getDelete()) &&
                compareOperation(p1.getPatch(), p2.getPatch()) &&
                compareOperation(p1.getOptions(), p2.getOptions()) &&
                compareOperation(p1.getHead(), p2.getHead());
    }

    /**
     * Compares two {@link Operation} objects from OpenAPI to check if they are equivalent.
     * <p>
     * This method compares the properties of two {@link Operation} instances (such as HTTP method,
     * parameters, responses, etc.) to determine if they define the same operation. It can be used
     * for comparing individual operations in a structural OpenAPI comparison.
     * </p>
     *
     * @param op1 the first {@link Operation} object to compare.
     * @param op2 the second {@link Operation} object to compare.
     * @return {@code true} if both {@link Operation} objects are equivalent; {@code false} otherwise.
     */
    private static boolean compareOperation(Operation op1, Operation op2) {
        if (op1 == op2) return true;
        if (op1 == null || op2 == null) return false;

        return compareParameters(op1.getParameters(), op2.getParameters());
    }

    /**
     * Compares two lists of {@link Parameter} objects to check if they are equivalent.
     * <p>
     * This method compares the parameters of two OpenAPI operations, checking for equality in
     * parameter names, types, and other relevant attributes. It ensures that both sets of parameters
     * are structurally and semantically identical.
     * </p>
     *
     * @param params1 the first list of {@link Parameter} objects to compare.
     * @param params2 the second list of {@link Parameter} objects to compare.
     * @return {@code true} if both lists of parameters are identical in structure and content; {@code false} otherwise.
     */
    private static boolean compareParameters(List<Parameter> params1, List<Parameter> params2) {
        if (params1 == params2) return true;

        // Filter only required parameters
        List<Parameter> requiredParams1 = new ArrayList<>();
        if (params1 != null) {
            requiredParams1 = params1.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getRequired()))
                    .sorted(Comparator.comparing(Parameter::getName))
                    .collect(Collectors.toList());
        }

        List<Parameter> requiredParams2 = new ArrayList<>();
        if (params2 != null) {
            requiredParams2 = params2.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getRequired()))
                    .sorted(Comparator.comparing(Parameter::getName))
                    .collect(Collectors.toList());
        }

        if (requiredParams1.size() != requiredParams2.size()) {
            return false;
        }

        for (int i = 0; i < requiredParams1.size(); i++) {
            Parameter p1 = requiredParams1.get(i);
            Parameter p2 = requiredParams2.get(i);
            if (!Objects.equals(p1.getName(), p2.getName()) ||
                    !Objects.equals(p1.getIn(), p2.getIn())) {
                return false;
            }
        }
        return true;
    }
}
