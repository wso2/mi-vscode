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

package org.eclipse.lemminx.customservice.synapse.pojo;

/**
 * Implemented by every {@code synapse/*} request that names the project it belongs to.
 *
 * <p>These requests had each declared their own {@code projectUri} field independently, in several
 * different access styles (public field, private field with accessors, package-private with
 * accessors), so a caller had to know which style a given request class used. One accessor shape
 * lets the language service resolve the project the same way for all of them, through a single
 * null-safe {@code resolveByProjectUri(HasProjectUri)}.
 *
 * <p>The field remains optional: a null, blank or unmatched {@code projectUri} resolves to no
 * project and the RPC answers with an empty or failed result, never with another project's data.
 */
public interface HasProjectUri {

    /**
     * Returns the project root this request belongs to.
     *
     * <p>The VS Code extension sends this as {@code WorkspaceFolder.uri.fsPath} — an absolute
     * filesystem path rather than a {@code file://} URI — though both forms are accepted by the
     * resolvers, which normalize either.
     *
     * @return the project root, or {@code null}/blank if the request does not name one
     */
    String getProjectUri();
}
