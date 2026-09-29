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
package org.eclipse.daanse.dax.engine.impl.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.engine.api.DaxColumn;
import org.eclipse.daanse.dax.engine.api.DaxExecutionException;

/**
 * The rows of a table for which a condition is TRUE, as {@code FILTER} gives
 * them; computed on the rows of its source.
 *
 * @param source    the table to filter
 * @param condition the condition, on the rows of the source
 */
public record Filter(TablePlan source, ScalarPlan condition) implements TablePlan {

    public Filter {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(condition, "condition");
    }

    @Override
    public List<DaxColumn> columns() {
        return source.columns();
    }

    /**
     * @param rows the rows of the source
     * @return those for which the condition is TRUE, in their order
     * @throws DaxExecutionException if the condition cannot be computed on a row
     */
    public List<List<Object>> apply(List<List<Object>> rows) throws DaxExecutionException {
        List<List<Object>> kept = new ArrayList<>();
        for (List<Object> row : rows) {
            if (ScalarPlan.logical(condition.evaluate(row), "FILTER")) {
                kept.add(row);
            }
        }
        return kept;
    }
}
