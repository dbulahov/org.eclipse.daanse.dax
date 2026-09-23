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

import java.util.Optional;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * One item of an {@code ORDER BY} clause, e.g. {@code 'Sales'[OrderDate]
 * DESC}.
 */
public interface OrderByItem {

    /**
     * @return the expression to sort by
     */
    DaxExpression expression();

    /**
     * @return the sort direction; {@link SortDirection#ASC} when none is
     *         given explicitly
     */
    SortDirection direction();

    /**
     * @return the paging start value for this item, e.g. the {@code 50} in
     *         {@code ORDER BY 'Product'[ProductKey] ASC START AT 50}; empty
     *         when the {@code ORDER BY} clause has no {@code START AT}, or
     *         has fewer {@code START AT} values than {@code ORDER BY} items
     */
    Optional<DaxExpression> startAt();

    /**
     * The direction an {@link OrderByItem} sorts by.
     */
    enum SortDirection {
        ASC, DESC
    }
}
