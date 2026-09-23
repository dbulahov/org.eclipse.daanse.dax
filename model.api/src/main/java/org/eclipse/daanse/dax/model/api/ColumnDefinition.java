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
 *   dbulahov - initial
 */
package org.eclipse.daanse.dax.model.api;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Identifier;

/**
 * One {@code COLUMN} definition of a {@code DEFINE} clause, e.g.
 * {@code COLUMN 'Sales'[Amount Category] = IF('Sales'[Amount] > 1000,
 * "High", "Low")}.
 */
public non-sealed interface ColumnDefinition extends DefineClause {

    /**
     * @return the fully qualified name the column is defined on, e.g.
     *         {@code 'Sales'[Amount Category]}
     */
    Identifier name();

    /**
     * @return the expression the column evaluates to
     */
    DaxExpression expression();
}
