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

/**
 * For each row of a table, the rows of a grouping computed for it, as
 * {@code GENERATE} gives them: each row of the outer table joined with the
 * rows of the inner one; an outer row the inner one has no rows for is left
 * out. Computed by the cube, as MDX {@code Generate}: the inner grouping's
 * groups, condition, top and measures are computed for the members of each
 * outer row.
 * <p>
 * Without columns to group by, the inner grouping is one row of its measures
 * for each outer row.
 * </p>
 *
 * @param outer the outer table: a grouping without measures, or a filter of one
 *              by its columns, as a filter table is (see
 *              {@link Summarize#filters()}); its columns come first
 * @param inner the inner grouping, without filter tables, of other hierarchies
 *              or of levels of those of the outer table: of a deeper level the
 *              members under the outer one, of one not deeper the outer one's
 *              ancestor; its columns come after
 */
public record Generate(TablePlan outer, Summarize inner) implements TablePlan {

    public Generate {
        Objects.requireNonNull(outer, "outer");
        Objects.requireNonNull(inner, "inner");
    }

    @Override
    public List<DaxColumn> columns() {
        List<DaxColumn> columns = new ArrayList<>(outer.columns());
        columns.addAll(inner.columns());
        return columns;
    }
}
