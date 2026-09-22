/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.dax.model.api.expression;

import java.util.List;

/**
 * Represents a table constructor expression.
 *
 * <pre>
 * Example:
 * {
 *   ("Value1", 1, TRUE),
 *   ("Value2", 2, FALSE)
 * }
 * </pre>
 */
public non-sealed interface TableConstructor extends DaxExpression {

    /**
     * @return the list of row constructors
     */
    List<RowConstructor> rows();
}
