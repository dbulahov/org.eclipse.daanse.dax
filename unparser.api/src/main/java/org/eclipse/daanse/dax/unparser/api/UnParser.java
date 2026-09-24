/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.dax.unparser.api;

import org.eclipse.daanse.dax.model.api.DaxStatement;

/**
 * Service that converts a DAX statement model back into its textual DAX
 * representation.
 * <p>
 * Multiple implementations may be registered at the same time. Each
 * implementation identifies itself with the service property
 * {@code dax.unparser.style} (for example {@code simple} or
 * {@code formatted}). Consumers that need a specific implementation should
 * use a target filter, e.g.
 * {@code @Reference(target = "(dax.unparser.style=simple)")}.
 * </p>
 */
public interface UnParser {

    /**
     * Convert a DAX statement model to its string representation.
     *
     * @param daxStatement the DAX statement to unparse
     * @return the DAX query string
     */
    String unparseDaxStatement(DaxStatement daxStatement);
}
