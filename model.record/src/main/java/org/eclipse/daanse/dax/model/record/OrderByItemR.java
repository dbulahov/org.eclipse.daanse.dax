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

import java.util.Objects;
import java.util.Optional;

import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.OrderByItem.SortDirection;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * Record implementation of {@link OrderByItem}.
 *
 * @param expression
 *            the expression to sort by
 * @param direction
 *            the sort direction
 * @param startAt
 *            the paging start value for this item
 */
public record OrderByItemR(DaxExpression expression, SortDirection direction, Optional<DaxExpression> startAt)
        implements OrderByItem {

    public OrderByItemR {
        Objects.requireNonNull(expression, "expression must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(startAt, "startAt must not be null");
    }

    /**
     * Convenience constructor for an {@code ORDER BY} item with no
     * {@code START AT} value.
     *
     * @param expression
     *            the expression to sort by
     * @param direction
     *            the sort direction
     */
    public OrderByItemR(DaxExpression expression, SortDirection direction) {
        this(expression, direction, Optional.empty());
    }
}
