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

/**
 * One {@code TABLE} definition of a {@code DEFINE} clause, e.g.
 * {@code TABLE TopProducts = TOPN(10, 'Product', ...)}.
 */
public non-sealed interface TableDefinition extends DefineClause {

    /**
     * @return the name the table is defined under, e.g. {@code TopProducts}
     */
    String name();

    /**
     * @return the table expression the definition evaluates to
     */
    DaxExpression expression();
}
