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
package org.eclipse.daanse.dax.model.record;

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * Record implementation of {@link EvaluateStatement}.
 *
 * @param tableExpression
 *            the table expression to evaluate
 * @param orderBy
 *            the {@code ORDER BY} items, in order
 */
public record EvaluateStatementR(DaxExpression tableExpression, List<OrderByItem> orderBy)
        implements EvaluateStatement {

    public EvaluateStatementR {
        Objects.requireNonNull(tableExpression, "tableExpression must not be null");
        Objects.requireNonNull(orderBy, "orderBy must not be null");
        orderBy = List.copyOf(orderBy);
    }

    /**
     * Convenience constructor for an {@code EVALUATE} statement with no
     * {@code ORDER BY} clause.
     *
     * @param tableExpression
     *            the table expression to evaluate
     */
    public EvaluateStatementR(DaxExpression tableExpression) {
        this(tableExpression, List.of());
    }
}
