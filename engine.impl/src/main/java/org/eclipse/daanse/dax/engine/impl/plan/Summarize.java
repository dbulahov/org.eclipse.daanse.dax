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
import org.eclipse.daanse.dax.engine.api.DaxType;
import org.eclipse.daanse.dax.engine.impl.model.ModelColumn;
import org.eclipse.daanse.dax.engine.impl.model.ModelMeasure;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator;

/**
 * Groups the cube by columns and computes measures per group, as
 * {@code SUMMARIZECOLUMNS} does; a table reference is the grouping by all its
 * columns without measures.
 * <p>
 * With measures, groups whose measures are all BLANK are left out. Without
 * measures, every combination of the columns' values is a group. With a
 * condition, only the groups for which the cube computes it TRUE are kept, as
 * {@code FILTER} by measures keeps them. With a top, only the first groups by a
 * measure are kept, of those where it is not BLANK, as MDX {@code TopCount}
 * keeps them; ties beyond the count are not.
 * </p>
 * <p>
 * Filter tables, as of {@code SUMMARIZECOLUMNS}, filter the cube before
 * grouping: a filter on hierarchies grouped by keeps the groups related to its
 * rows, one on others restricts what the measures compute. Each is a
 * {@link Summarize} without measures and filter tables, or a {@link Filter} of
 * one by its columns.
 * </p>
 * <p>
 * Added measures, as of {@code ADDCOLUMNS}, are computed for each group after
 * the measures; unlike them, they keep no group.
 * </p>
 *
 * @param groupBy   the columns to group by; the first result columns
 * @param measures  the measures; the result columns after the group-by columns
 * @param condition the condition on the groups, of constants,
 *                  {@link ScalarPlan.MeasureValue}s and the text of the
 *                  columns grouped by; empty to keep all
 * @param top       the first groups to keep, after the condition; empty to keep
 *                  all
 * @param filters   the filter tables
 * @param added     the added measures; the last result columns
 */
public record Summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures, Optional<ScalarPlan> condition,
        Optional<Top> top, List<TablePlan> filters, List<NamedMeasure> added) implements TablePlan {

    public Summarize {
        groupBy = List.copyOf(groupBy);
        measures = List.copyOf(measures);
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(top, "top");
        filters = List.copyOf(filters);
        added = List.copyOf(added);
    }

    /** A grouping without added measures. */
    public Summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures, Optional<ScalarPlan> condition,
            Optional<Top> top, List<TablePlan> filters) {
        this(groupBy, measures, condition, top, filters, List.of());
    }

    /** A grouping without filter tables. */
    public Summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures, Optional<ScalarPlan> condition,
            Optional<Top> top) {
        this(groupBy, measures, condition, top, List.of());
    }

    /** A grouping without top. */
    public Summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures, Optional<ScalarPlan> condition) {
        this(groupBy, measures, condition, Optional.empty());
    }

    /** A grouping keeping all groups. */
    public Summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures) {
        this(groupBy, measures, Optional.empty());
    }

    /** @return this grouping keeping only the groups it keeps for which the condition is TRUE too */
    public Summarize filtered(ScalarPlan condition) {
        ScalarPlan both = this.condition
                .<ScalarPlan>map(c -> new ScalarPlan.Logical(LogicalOperator.AND, c, condition)).orElse(condition);
        return new Summarize(groupBy, measures, Optional.of(both), top, filters, added);
    }

    /** @return this grouping keeping only the first groups by a measure */
    public Summarize topped(Top top) {
        return new Summarize(groupBy, measures, condition, Optional.of(top), filters, added);
    }

    /** @return this grouping computing the measures too, as its last columns */
    public Summarize withAdded(List<NamedMeasure> more) {
        List<NamedMeasure> all = new ArrayList<>(added);
        all.addAll(more);
        return new Summarize(groupBy, measures, condition, top, filters, all);
    }

    /** @return the measures and then the added measures, as the result columns after the groups */
    public List<NamedMeasure> allMeasures() {
        List<NamedMeasure> all = new ArrayList<>(measures);
        all.addAll(added);
        return all;
    }

    /**
     * @param filter a filter table
     * @return the columns of the grouping at its bottom
     */
    public static List<ModelColumn> filterColumns(TablePlan filter) {
        return switch (filter) {
        case Summarize summarize -> summarize.groupBy();
        case Filter f -> filterColumns(f.source());
        default -> throw new IllegalArgumentException("no filter table: " + filter);
        };
    }

    /**
     * The first groups by a measure.
     *
     * @param count     how many groups
     * @param measure   the measure to order by
     * @param ascending whether the first are the smallest, as MDX
     *                  {@code BottomCount} gives them; else the largest
     */
    public record Top(long count, ModelMeasure measure, boolean ascending) {

        public Top {
            Objects.requireNonNull(measure, "measure");
        }
    }

    @Override
    public List<DaxColumn> columns() {
        List<DaxColumn> columns = new ArrayList<>();
        for (ModelColumn column : groupBy) {
            columns.add(new DaxColumn(column.daxName(), Optional.of(column.table()), column.type()));
        }
        for (NamedMeasure measure : allMeasures()) {
            columns.add(new DaxColumn("[" + measure.name() + "]", Optional.empty(), DaxType.VARIANT));
        }
        return columns;
    }
}
