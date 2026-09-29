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
import java.util.Optional;

import org.eclipse.daanse.dax.engine.api.DaxColumn;
import org.eclipse.daanse.dax.engine.api.DaxExecutionException;
import org.eclipse.daanse.dax.engine.api.DaxType;

/**
 * A table with columns computed on each of its rows, as {@code ADDCOLUMNS}
 * gives it; computed on the rows of its source. Columns the cube computes are
 * added measures of a {@link Summarize} of the source, which the source has
 * after its first {@code width} columns.
 *
 * @param source the table
 * @param width  how many of the source's first columns are the result's; the
 *               others are computed for the added columns only
 * @param added  the added columns, on the rows of the source
 */
public record AddColumns(TablePlan source, int width, List<Added> added) implements TablePlan {

    public AddColumns {
        Objects.requireNonNull(source, "source");
        if (width < 0 || width > source.columns().size()) {
            throw new IllegalArgumentException("width " + width);
        }
        added = List.copyOf(added);
    }

    /**
     * An added column.
     *
     * @param name       the column name the query gives, without brackets
     * @param expression how to compute it on a row of the source
     * @param type       the type of its values
     */
    public record Added(String name, ScalarPlan expression, DaxType type) {

        public Added {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(type, "type");
        }
    }

    @Override
    public List<DaxColumn> columns() {
        List<DaxColumn> columns = new ArrayList<>(source.columns().subList(0, width));
        for (Added column : added) {
            columns.add(new DaxColumn("[" + column.name() + "]", Optional.empty(), column.type()));
        }
        return columns;
    }

    /**
     * @param rows the rows of the source
     * @return them with the added columns
     * @throws DaxExecutionException if a column cannot be computed on a row
     */
    public List<List<Object>> apply(List<List<Object>> rows) throws DaxExecutionException {
        List<List<Object>> result = new ArrayList<>(rows.size());
        for (List<Object> row : rows) {
            List<Object> extended = new ArrayList<>(row.subList(0, width));
            for (Added column : added) {
                extended.add(column.expression().evaluate(row));
            }
            result.add(extended);
        }
        return result;
    }
}
