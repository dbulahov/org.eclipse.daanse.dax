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
package org.eclipse.daanse.dax.engine.impl.mdx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.daanse.dax.engine.impl.mdx.MdxQuery.ValueSource;
import org.eclipse.daanse.dax.engine.impl.model.ModelColumn;
import org.eclipse.daanse.dax.engine.impl.plan.NamedMeasure;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.ColumnValue;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Comparison;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Constant;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.InList;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.IsBlank;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Logical;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.MeasureValue;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Not;
import org.eclipse.daanse.dax.engine.impl.plan.Filter;
import org.eclipse.daanse.dax.engine.impl.plan.Summarize;
import org.eclipse.daanse.dax.engine.impl.plan.TablePlan;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator;

/**
 * Translates plans into MDX.
 * <p>
 * A {@link Summarize} puts its measures on the columns axis and the cross join
 * of its hierarchies on the rows axis, {@code NON EMPTY} when it has measures.
 * Of the columns of one hierarchy only the deepest level goes on the axis; the
 * others are read from the ancestors of its members. Its condition filters
 * the rows set by MDX {@code Filter}, its top then by {@code TopCount} or
 * {@code BottomCount} of the groups where the measure is not empty. A result
 * expression other than a measure is a calculated member of {@code WITH},
 * e.g. {@code ISBLANK} of a measure is {@code IsEmpty} of it. Added measures
 * follow the measures on the columns axis; with them, {@code NonEmpty} of the
 * measures replaces {@code NON EMPTY}.
 * </p>
 * <p>
 * A filter table on hierarchies grouped by keeps the rows set by
 * {@code Exists}; the filter tables on others are the slicer, {@code WHERE}.
 * A filter table is the set of its members, filtered by a condition on their
 * names ignoring case, as DAX compares text.
 * </p>
 */
public final class MdxGenerator {

    private MdxGenerator() {
    }

    /**
     * @param cube      the cube name, as MDX writes it
     * @param summarize the plan
     * @return the MDX query computing the plan's table
     */
    public static MdxQuery summarize(String cube, Summarize summarize) {
        Map<String, ModelColumn> deepest = deepest(summarize.groupBy());
        List<String> hierarchies = new ArrayList<>(deepest.keySet());

        List<ValueSource> sources = new ArrayList<>();
        for (ModelColumn column : summarize.groupBy()) {
            sources.add(new ValueSource.MemberName(hierarchies.indexOf(column.hierarchy()), column.depth()));
        }
        StringBuilder mdx = new StringBuilder();
        StringJoiner measures = new StringJoiner(", ", "{", "}");
        // the members of the measures, not of the added ones, which keep no group
        StringJoiner keeping = new StringJoiner(", ", "{", "}");
        List<NamedMeasure> all = summarize.allMeasures();
        for (int i = 0; i < all.size(); i++) {
            NamedMeasure measure = all.get(i);
            String member;
            if (measure.expression() instanceof MeasureValue value) {
                member = value.measure().uniqueName();
            } else {
                member = "[Measures]." + MdxNames.quote("DAX " + measure.name());
                mdx.append(mdx.isEmpty() ? "WITH " : " ").append("MEMBER ").append(member).append(" AS ")
                        .append(condition(measure.expression()));
            }
            measures.add(member);
            if (i < summarize.measures().size()) {
                keeping.add(member);
            }
            sources.add(new ValueSource.CellValue(i));
        }
        if (!mdx.isEmpty()) {
            mdx.append(" ");
        }

        mdx.append("SELECT ").append(measures).append(" ON COLUMNS");
        String slicer = null;
        List<TablePlan> kept = new ArrayList<>();
        for (TablePlan filter : summarize.filters()) {
            if (Summarize.filterColumns(filter).stream().anyMatch(c -> deepest.containsKey(c.hierarchy()))) {
                kept.add(filter);
            } else {
                slicer = slicer == null ? filterSet(filter) : "CrossJoin(" + slicer + ", " + filterSet(filter) + ")";
            }
        }
        boolean rows = !deepest.isEmpty();
        if (rows) {
            String set = set(summarize);
            for (TablePlan filter : kept) {
                set = "Exists(" + set + ", " + filterSet(filter) + ")";
            }
            set = conditionAndTop(set, summarize);
            mdx.append(", ");
            if (!summarize.measures().isEmpty() && !summarize.added().isEmpty()) {
                // NON EMPTY of the axis would count the added measures too
                set = "NonEmpty(" + set + ", " + keeping + ")";
            } else if (!summarize.measures().isEmpty()) {
                mdx.append("NON EMPTY ");
            }
            mdx.append(set).append(" ON ROWS");
        }
        mdx.append(" FROM ").append(cube);
        if (slicer != null) {
            mdx.append(" WHERE ").append(slicer);
        }
        return new MdxQuery(mdx.toString(), sources, rows);
    }

    /** @return the cross join of the deepest level of each hierarchy grouped by */
    private static String set(Summarize summarize) {
        String set = null;
        for (ModelColumn column : deepest(summarize.groupBy()).values()) {
            String members = column.level() + ".Members";
            set = set == null ? members : "CrossJoin(" + set + ", " + members + ")";
        }
        return set;
    }

    private static String conditionAndTop(String set, Summarize summarize) {
        if (summarize.condition().isPresent()) {
            set = "Filter(" + set + ", " + condition(summarize.condition().get(), summarize.groupBy()) + ")";
        }
        if (summarize.top().isPresent()) {
            Summarize.Top top = summarize.top().get();
            String measure = top.measure().uniqueName();
            set = (top.ascending() ? "BottomCount(" : "TopCount(") + "NonEmpty(" + set + ", {" + measure + "}), "
                    + top.count() + ", " + measure + ")";
        }
        return set;
    }

    /** @return the set of the members of a filter table, see {@link Summarize#filters()} */
    private static String filterSet(TablePlan filter) {
        return switch (filter) {
        case Summarize summarize -> conditionAndTop(set(summarize), summarize);
        case Filter f -> "Filter(" + filterSet(f.source()) + ", "
                + condition(f.condition(), Summarize.filterColumns(f.source())) + ")";
        default -> throw new IllegalArgumentException("no filter table: " + filter);
        };
    }

    /** @return the deepest column of each hierarchy, in order of first appearance */
    private static Map<String, ModelColumn> deepest(List<ModelColumn> columns) {
        Map<String, ModelColumn> deepest = new LinkedHashMap<>();
        for (ModelColumn column : columns) {
            deepest.merge(column.hierarchy(), column, (a, b) -> b.depth() > a.depth() ? b : a);
        }
        return deepest;
    }

    /**
     * @param plan an expression of measures and non-BLANK constants, as
     *             {@link Summarize#condition()} has; {@code ISBLANK} of a
     *             measure is {@code IsEmpty}
     * @return it as an MDX expression
     */
    static String condition(ScalarPlan plan) {
        return condition(plan, List.of());
    }

    /**
     * @param plan    a condition as {@link #condition(ScalarPlan)} takes, or on
     *                the text of columns
     * @param columns the columns a {@link ColumnValue} refers to, each the name
     *                of a member of the row or of its ancestor
     * @return it as an MDX expression
     */
    static String condition(ScalarPlan plan, List<ModelColumn> columns) {
        return switch (plan) {
        case MeasureValue measure -> measure.measure().uniqueName();
        case Constant constant -> literal(constant.value());
        case ColumnValue column -> "UCase(" + memberName(columns.get(column.column()), columns) + ")";
        case InList in -> {
            StringJoiner any = new StringJoiner(" OR ", "(", ")").setEmptyValue("(1 = 0)");
            for (Object value : in.values()) {
                // a member's name is never BLANK
                if (value != null) {
                    any.add(condition(in.value(), columns) + " = " + literal(value.toString().toUpperCase(Locale.ROOT)));
                }
            }
            yield any.toString();
        }
        case Comparison comparison -> term(comparison.left(), comparison, columns) + " " + switch (comparison.operator()) {
            case EQUAL -> "=";
            case NOT_EQUAL -> "<>";
            case LESS_THAN -> "<";
            case LESS_THAN_OR_EQUAL -> "<=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case IN -> throw new IllegalArgumentException("IN is an InList");
            } + " " + term(comparison.right(), comparison, columns);
        case Logical logical -> operand(logical.left(), columns)
                + (logical.operator() == LogicalOperator.AND ? " AND " : " OR ") + operand(logical.right(), columns);
        case Not not -> "NOT " + operand(not.operand(), columns);
        case IsBlank isBlank -> "IsEmpty(" + condition(isBlank.operand(), columns) + ")";
        };
    }

    /** @return an operand of a comparison; text compared with a column in upper case, as the column is */
    private static String term(ScalarPlan plan, Comparison comparison, List<ModelColumn> columns) {
        boolean withColumn = comparison.left() instanceof ColumnValue || comparison.right() instanceof ColumnValue;
        if (withColumn && plan instanceof Constant constant && constant.value() instanceof String text) {
            return literal(text.toUpperCase(Locale.ROOT));
        }
        return condition(plan, columns);
    }

    /** @return the name of the member of the column: the current one, or its ancestor at the column's level */
    private static String memberName(ModelColumn column, List<ModelColumn> columns) {
        String current = column.hierarchy() + ".CurrentMember";
        if (deepest(columns).get(column.hierarchy()).depth() == column.depth()) {
            return current + ".Name";
        }
        return "Ancestor(" + current + ", " + column.level() + ").Name";
    }

    private static String operand(ScalarPlan plan, List<ModelColumn> columns) {
        String mdx = condition(plan, columns);
        return plan instanceof Logical || plan instanceof Comparison ? "(" + mdx + ")" : mdx;
    }

    private static String literal(Object value) {
        return switch (value) {
        case String string -> "\"" + string.replace("\"", "\"\"") + "\"";
        case Boolean bool -> bool ? "TRUE" : "FALSE";
        case BigDecimal decimal -> decimal.toPlainString();
        case Number number -> number.toString();
        default -> throw new IllegalArgumentException("no MDX literal for " + value);
        };
    }
}
