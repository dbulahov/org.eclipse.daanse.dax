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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.daanse.dax.engine.api.DaxColumn;
import org.eclipse.daanse.dax.engine.api.DaxExecutionException;
import org.eclipse.daanse.dax.engine.api.DaxSemanticException;
import org.eclipse.daanse.dax.engine.api.DaxType;
import org.eclipse.daanse.dax.engine.impl.model.ModelColumn;
import org.eclipse.daanse.dax.engine.impl.model.ModelMeasure;
import org.eclipse.daanse.dax.engine.impl.model.ModelTable;
import org.eclipse.daanse.dax.engine.impl.model.TabularModel;
import org.eclipse.daanse.dax.engine.impl.plan.EvaluatePlan.SortKey;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.ColumnValue;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Comparison;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Constant;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.InList;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.IsBlank;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Logical;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.MeasureValue;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Not;
import org.eclipse.daanse.dax.model.api.ColumnDefinition;
import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.ParameterDefinition;
import org.eclipse.daanse.dax.model.api.TableDefinition;
import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator;
import org.eclipse.daanse.dax.model.api.expression.BooleanLiteral;
import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Entity;
import org.eclipse.daanse.dax.model.api.expression.FunctionCall;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.eclipse.daanse.dax.model.api.expression.Keyword;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.eclipse.daanse.dax.model.api.expression.Parameter;
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.Scalar;
import org.eclipse.daanse.dax.model.api.expression.StringLiteral;
import org.eclipse.daanse.dax.model.api.expression.TableConstructor;
import org.eclipse.daanse.dax.model.api.expression.VariableReference;

/**
 * Binds a parsed query to a {@link TabularModel}: resolves its names and
 * turns it into a {@link QueryPlan}. Every error of the query is found here,
 * before anything is computed.
 * <p>
 * Supported so far: table references, table constructors and {@code ROW} of
 * constants, {@code ROW} and {@code SUMMARIZECOLUMNS} of columns and measure
 * references, {@code DISTINCT} of a column or of a supported table,
 * {@code VALUES} of a column or a table name, {@code FILTER} of a supported
 * table by a condition on its columns and, computed by the cube, on measures,
 * {@code TOPN} of a supported table by its columns and measures, filter tables
 * of {@code SUMMARIZECOLUMNS} of these, {@code KEEPFILTERS} of a filter table
 * or of the table {@code FILTER}, {@code TOPN} or {@code ADDCOLUMNS} iterates, e.g. of
 * {@code VALUES}, {@code ADDCOLUMNS} of a supported table by expressions of
 * its columns, or of measures computed by the cube, the comparisons, {@code IN} a table
 * constructor, {@code &&}, {@code ||}, {@code NOT} and {@code ISBLANK} of
 * constants and columns, constant {@code VAR} and parameter definitions, and {@code ORDER BY} result columns. Anything else
 * fails as not supported yet.
 * </p>
 */
public final class Binder {

    private final TabularModel model;
    private final Map<String, Object> parameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Object> variables = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * @param model      the model the query refers to
     * @param parameters the values of the query parameters by name, without
     *                   {@code @}; a value is {@code null} for BLANK
     */
    public Binder(TabularModel model, Map<String, Object> parameters) {
        this.model = model;
        this.parameters.putAll(parameters);
    }

    /**
     * @param statement the parsed query
     * @return its plan
     * @throws DaxSemanticException if the query does not fit the model or uses
     *                              what is not supported yet
     */
    public QueryPlan bind(DaxStatement statement) throws DaxSemanticException {
        for (DefineClause clause : statement.defineClauses()) {
            define(clause);
        }
        List<EvaluatePlan> evaluates = new ArrayList<>();
        for (EvaluateStatement evaluate : statement.evaluateStatements()) {
            TablePlan table = table(evaluate.tableExpression());
            evaluates.add(new EvaluatePlan(table, orderBy(evaluate.orderBy(), table.columns())));
        }
        return new QueryPlan(evaluates);
    }

    private void define(DefineClause clause) throws DaxSemanticException {
        switch (clause) {
        case VariableDefinition variable -> variables.put(variable.name(), constant(variable.expression()));
        case ParameterDefinition parameter -> {
            // a value the caller gives overrides the query's own
            if (!parameters.containsKey(parameter.name())) {
                parameters.put(parameter.name(), constant(parameter.expression()));
            }
        }
        case MeasureDefinition m -> throw notSupported("DEFINE MEASURE");
        case TableDefinition t -> throw notSupported("DEFINE TABLE");
        case ColumnDefinition c -> throw notSupported("DEFINE COLUMN");
        }
    }

    // --- tables

    private TablePlan table(DaxExpression expression) throws DaxSemanticException {
        return switch (expression) {
        case Entity entity -> tableReference(entity.name());
        case Keyword keyword -> tableReference(keyword.name());
        case TableConstructor constructor -> tableConstructor(constructor);
        case FunctionCall call -> switch (call.functionName().toUpperCase(Locale.ROOT)) {
            case "ROW" -> row(call.arguments());
            case "SUMMARIZECOLUMNS" -> summarizeColumns(call.arguments());
            case "DISTINCT" -> distinct(call.arguments());
            case "VALUES" -> values(call.arguments());
            case "FILTER" -> filter(call.arguments());
            case "TOPN" -> topN(call.arguments());
            case "ADDCOLUMNS" -> addColumns(call.arguments());
            case "KEEPFILTERS" -> throw new DaxSemanticException("KEEPFILTERS can only be used as a filter table, "
                    + "e.g. of SUMMARIZECOLUMNS, or as the table FILTER, TOPN or ADDCOLUMNS iterates");
            default -> throw notSupported("the table function " + call.functionName());
            };
        default -> throw notSupported("a table expression of kind " + kind(expression));
        };
    }

    private TablePlan tableReference(String name) throws DaxSemanticException {
        ModelTable table = model.table(name)
                .orElseThrow(() -> new DaxSemanticException("the table '" + name + "' does not exist"));
        return summarize(table.columns(), List.of());
    }

    private TablePlan tableConstructor(TableConstructor constructor) throws DaxSemanticException {
        List<List<Object>> rows = new ArrayList<>();
        int width = -1;
        for (RowConstructor row : constructor.rows()) {
            if (width >= 0 && row.columns().size() != width) {
                throw new DaxSemanticException("the rows of a table constructor must have the same number of values");
            }
            width = row.columns().size();
            List<Object> values = new ArrayList<>();
            for (DaxExpression value : row.columns()) {
                values.add(constant(value));
            }
            rows.add(values);
        }
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= Math.max(width, 0); i++) {
            names.add(width == 1 ? "Value" : "Value" + i);
        }
        return constantTable(names, rows);
    }

    private TablePlan row(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.isEmpty() || arguments.size() % 2 != 0) {
            throw new DaxSemanticException("ROW takes pairs of a name and an expression");
        }
        List<String> names = new ArrayList<>();
        List<DaxExpression> expressions = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i += 2) {
            names.add(columnName(arguments.get(i), "ROW"));
            expressions.add(arguments.get(i + 1));
        }
        List<NamedMeasure> measures = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            Optional<ScalarPlan> onCube = cubeExpression(expressions.get(i), "ROW");
            if (onCube.isPresent()) {
                measures.add(new NamedMeasure(names.get(i), onCube.get()));
            }
        }
        if (measures.size() == expressions.size()) {
            return summarize(List.of(), measures);
        }
        if (!measures.isEmpty()) {
            throw notSupported("ROW mixing measures and other expressions");
        }
        List<Object> values = new ArrayList<>();
        for (DaxExpression expression : expressions) {
            values.add(constant(expression));
        }
        return constantTable(names, List.of(values));
    }

    private TablePlan summarizeColumns(List<DaxExpression> arguments) throws DaxSemanticException {
        List<ModelColumn> groupBy = new ArrayList<>();
        List<TablePlan> filters = new ArrayList<>();
        int i = 0;
        for (; i < arguments.size() && !(arguments.get(i) instanceof StringLiteral); i++) {
            DaxExpression argument = arguments.get(i);
            if (argument instanceof Identifier identifier) {
                groupBy.add(column(identifier));
            } else {
                filterTable(argument).ifPresent(filters::add);
            }
        }
        if ((arguments.size() - i) % 2 != 0) {
            throw new DaxSemanticException("SUMMARIZECOLUMNS takes pairs of a name and an expression after its columns");
        }
        List<NamedMeasure> measures = new ArrayList<>();
        for (; i < arguments.size(); i += 2) {
            String name = columnName(arguments.get(i), "SUMMARIZECOLUMNS");
            DaxExpression expression = arguments.get(i + 1);
            ScalarPlan onCube = cubeExpression(expression, "SUMMARIZECOLUMNS")
                    .orElseThrow(() -> notSupported("SUMMARIZECOLUMNS with an expression of kind " + kind(expression)
                            + " of no measure"));
            measures.add(new NamedMeasure(name, onCube));
        }
        Summarize summarize = (Summarize) summarize(groupBy, measures);
        if (filters.isEmpty()) {
            return summarize;
        }
        checkFilters(summarize, filters);
        return new Summarize(summarize.groupBy(), measures, Optional.empty(), Optional.empty(), filters);
    }

    /**
     * @return the plan of a filter table of SUMMARIZECOLUMNS; empty if it
     *         filters nothing, as all values of columns
     */
    private Optional<TablePlan> filterTable(DaxExpression argument) throws DaxSemanticException {
        TablePlan table = table(withoutKeepFilters(argument));
        if (!filterTableOnCube(table)) {
            throw notSupported("SUMMARIZECOLUMNS with a filter table of kind " + table.getClass().getSimpleName()
                    + " or with conditions other than on the text of columns and on measures");
        }
        if (table instanceof Summarize summarize && summarize.condition().isEmpty() && summarize.top().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(table);
    }

    /**
     * @return the table KEEPFILTERS is of, as a filter table or the table of an
     *         iterator; the argument if it is no KEEPFILTERS
     */
    private static DaxExpression withoutKeepFilters(DaxExpression argument) throws DaxSemanticException {
        // without outer filters there is nothing KEEPFILTERS keeps: it is its table
        while (argument instanceof FunctionCall call && call.functionName().equalsIgnoreCase("KEEPFILTERS")) {
            if (call.arguments().size() != 1) {
                throw new DaxSemanticException("KEEPFILTERS takes one table");
            }
            argument = call.arguments().get(0);
        }
        return argument;
    }

    /** @return whether the cube computes the table as a filter, see {@link Summarize#filters()} */
    private static boolean filterTableOnCube(TablePlan table) {
        return switch (table) {
        case Summarize summarize -> !summarize.groupBy().isEmpty() && summarize.measures().isEmpty()
                && summarize.filters().isEmpty();
        case Filter filter -> filterTableOnCube(filter.source()) && onMembers(filter.condition());
        default -> false;
        };
    }

    /** @return whether the cube computes the condition on the names of members, see {@code MdxGenerator} */
    private static boolean onMembers(ScalarPlan plan) {
        return switch (plan) {
        case Comparison comparison -> (comparison.left() instanceof ColumnValue
                || comparison.left() instanceof Constant c && c.value() instanceof String)
                && (comparison.right() instanceof ColumnValue
                        || comparison.right() instanceof Constant c && c.value() instanceof String);
        case InList in -> in.value() instanceof ColumnValue
                && in.values().stream().allMatch(v -> v == null || v instanceof String);
        case Logical logical -> onMembers(logical.left()) && onMembers(logical.right());
        case Not not -> onMembers(not.operand());
        default -> false;
        };
    }

    /**
     * A filter on hierarchies not grouped by goes to the slicer, which takes one
     * filter per hierarchy; one on hierarchies grouped by keeps the groups, and
     * with measures it must not be deeper than the groups, or they would compute
     * too much.
     */
    private static void checkFilters(Summarize summarize, List<TablePlan> filters) throws DaxSemanticException {
        Map<String, Integer> grouped = new TreeMap<>();
        for (ModelColumn column : summarize.groupBy()) {
            grouped.merge(column.hierarchy(), column.depth(), Math::max);
        }
        Set<String> sliced = new LinkedHashSet<>();
        for (TablePlan filter : filters) {
            List<ModelColumn> columns = Summarize.filterColumns(filter);
            Set<String> hierarchies = new LinkedHashSet<>();
            columns.forEach(c -> hierarchies.add(c.hierarchy()));
            if (hierarchies.stream().noneMatch(grouped::containsKey)) {
                for (String hierarchy : hierarchies) {
                    if (!sliced.add(hierarchy)) {
                        throw notSupported("several filter tables on the hierarchy " + hierarchy);
                    }
                }
            } else if (grouped.keySet().containsAll(hierarchies)) {
                for (ModelColumn column : columns) {
                    if (!summarize.measures().isEmpty() && column.depth() > grouped.get(column.hierarchy())) {
                        throw notSupported("a filter table on " + column.daxName()
                                + ", deeper than SUMMARIZECOLUMNS groups by");
                    }
                }
            } else {
                throw notSupported("a filter table on hierarchies grouped by and others");
            }
        }
    }

    /**
     * @return the expression as the cube computes it, e.g. a measure or
     *         {@code ISBLANK} of one; empty if it uses no measure
     */
    private Optional<ScalarPlan> cubeExpression(DaxExpression expression, String function)
            throws DaxSemanticException {
        Optional<ModelMeasure> measure = measureReference(expression);
        if (measure.isPresent()) {
            return Optional.of(new MeasureValue(measure.get()));
        }
        // no columns: a name is a measure
        ScalarPlan plan = scalar(expression, List.of());
        if (!usesMeasure(plan)) {
            return Optional.empty();
        }
        if (!onCube(plan)) {
            throw notSupported(function + " with a measure in an expression with IN, BLANK or dates");
        }
        return Optional.of(plan);
    }

    private TablePlan distinct(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.size() != 1) {
            throw new DaxSemanticException("DISTINCT takes one column or table");
        }
        DaxExpression argument = arguments.get(0);
        if (argument instanceof Identifier identifier) {
            // the values of a column: a grouping by it alone
            return summarize(List.of(column(identifier)), List.of());
        }
        return switch (table(argument)) {
        // a grouping has one row per group already
        case Summarize summarize -> summarize;
        // so has a filtered one: only a grouping is filtered after binding
        case Filter filter -> filter;
        case TopN topN -> topN;
        // added columns depend on the rest of the row
        case AddColumns addColumns -> addColumns;
        case ConstantTable constant ->
            new ConstantTable(constant.columns(), new ArrayList<>(new LinkedHashSet<>(constant.rows())));
        };
    }

    private TablePlan values(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.size() != 1) {
            throw new DaxSemanticException("VALUES takes one column or table name");
        }
        // the cube has no rows breaking a relationship, so no blank row is added
        return switch (arguments.get(0)) {
        case Identifier identifier -> summarize(List.of(column(identifier)), List.of());
        case Entity entity -> tableReference(entity.name());
        case Keyword keyword -> tableReference(keyword.name());
        default -> throw new DaxSemanticException("VALUES takes a column or table name, not an expression of kind "
                + kind(arguments.get(0)));
        };
    }

    private TablePlan filter(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.size() != 2) {
            throw new DaxSemanticException("FILTER takes a table and a condition");
        }
        TablePlan source = table(withoutKeepFilters(arguments.get(0)));
        // the parts of a condition that are and-ed are computed apart: those on
        // measures by the cube, the others on the rows it answers
        List<ScalarPlan> onRows = new ArrayList<>();
        List<ScalarPlan> onCube = new ArrayList<>();
        for (ScalarPlan part : conjuncts(scalar(arguments.get(1), source.columns()))) {
            (usesMeasure(part) ? onCube : onRows).add(part);
        }
        if (!onCube.isEmpty()) {
            ScalarPlan condition = and(onCube);
            // a part on measures may compare the text of columns grouped by too, e.g. by ||
            if (!onCube(condition, groupedColumns(source))) {
                throw notSupported("FILTER by a condition joining measures with columns other than grouped by "
                        + "and compared with text, with IN of other than text, BLANK or dates other than by &&");
            }
            source = filteredByCube(source, condition);
        }
        if (onRows.isEmpty()) {
            return source;
        }
        Filter filter = new Filter(source, and(onRows));
        if (source instanceof ConstantTable constant) {
            // nothing to query: filtered right away
            try {
                return new ConstantTable(constant.columns(), filter.apply(constant.rows()));
            } catch (DaxExecutionException e) {
                throw new DaxSemanticException(e.getMessage(), e);
            }
        }
        return filter;
    }

    private static TablePlan filteredByCube(TablePlan table, ScalarPlan condition) throws DaxSemanticException {
        return switch (table) {
        case Summarize summarize when summarize.groupBy().isEmpty() -> throw notSupported("FILTER by a measure of ROW");
        case Summarize summarize when summarize.top().isPresent() ->
            throw notSupported("FILTER of TOPN by a measure");
        // the filter tables filter within SUMMARIZECOLUMNS only, not the measures of FILTER
        case Summarize summarize when !summarize.filters().isEmpty() ->
            throw notSupported("FILTER by a measure of SUMMARIZECOLUMNS with filter tables");
        case Summarize summarize -> summarize.filtered(condition);
        case Filter filter -> new Filter(filteredByCube(filter.source(), condition), filter.condition());
        // added columns keep the rows: filtered before
        case AddColumns addColumns ->
            new AddColumns(filteredByCube(addColumns.source(), condition), addColumns.width(), addColumns.added());
        case TopN topN -> throw notSupported("FILTER of TOPN by a measure");
        case ConstantTable constant -> throw notSupported("FILTER of a table constructor by a measure");
        };
    }

    private TablePlan topN(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.size() < 3) {
            throw new DaxSemanticException("TOPN takes a number of rows, a table and expressions to order by");
        }
        if (!(constant(arguments.get(0)) instanceof Number count)) {
            throw new DaxSemanticException("TOPN takes a number of rows");
        }
        TablePlan source = table(withoutKeepFilters(arguments.get(1)));
        List<SortKey> keys = new ArrayList<>();
        List<Summarize.Top> byMeasure = new ArrayList<>();
        for (int i = 2; i < arguments.size(); i++) {
            DaxExpression expression = arguments.get(i);
            // descending unless told
            boolean ascending = false;
            if (i + 1 < arguments.size() && isOrder(arguments.get(i + 1))) {
                ascending = ascending(arguments.get(++i));
            }
            switch (scalar(expression, source.columns())) {
            case ColumnValue column -> keys.add(new SortKey(column.column(), ascending));
            case MeasureValue measure -> byMeasure.add(new Summarize.Top(count.longValue(), measure.measure(), ascending));
            default -> throw notSupported("TOPN by an expression of kind " + kind(expression));
            }
        }
        if (!byMeasure.isEmpty()) {
            // computed by the cube, as MDX TopCount
            if (byMeasure.size() > 1 || !keys.isEmpty()) {
                throw notSupported("TOPN by a measure and other expressions");
            }
            if (!(source instanceof Summarize summarize) || summarize.groupBy().isEmpty()
                    || summarize.top().isPresent()) {
                throw notSupported("TOPN by a measure of other than a grouping of columns, e.g. VALUES");
            }
            if (!summarize.filters().isEmpty()) {
                throw notSupported("TOPN by a measure of SUMMARIZECOLUMNS with filter tables");
            }
            return summarize.topped(byMeasure.get(0));
        }
        TopN topN = new TopN(source, count.longValue(), keys);
        if (source instanceof ConstantTable constant) {
            // nothing to query: computed right away
            return new ConstantTable(topN.columns(), topN.apply(constant.rows()));
        }
        return topN;
    }

    private TablePlan addColumns(List<DaxExpression> arguments) throws DaxSemanticException {
        if (arguments.size() < 3 || arguments.size() % 2 == 0) {
            throw new DaxSemanticException("ADDCOLUMNS takes a table and pairs of a name and an expression");
        }
        TablePlan source = table(withoutKeepFilters(arguments.get(0)));
        List<DaxColumn> columns = source.columns();
        int width = columns.size();
        List<AddColumns.Added> added = new ArrayList<>();
        List<NamedMeasure> onCube = new ArrayList<>();
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 1; i < arguments.size(); i += 2) {
            String name = columnName(arguments.get(i), "ADDCOLUMNS");
            // a column of a table has its name in brackets after the table's
            String bracketed = ("[" + name + "]").toLowerCase(Locale.ROOT);
            if (columns.stream().anyMatch(c -> c.name().toLowerCase(Locale.ROOT).endsWith(bracketed))
                    || !names.add(name)) {
                throw new DaxSemanticException("ADDCOLUMNS: the column [" + name + "] exists already");
            }
            ScalarPlan expression = scalar(arguments.get(i + 1), columns);
            if (usesMeasure(expression)) {
                if (!onCube(expression)) {
                    throw notSupported("ADDCOLUMNS with a measure in an expression with columns, IN, BLANK or dates");
                }
                // computed by the cube, for each row
                onCube.add(new NamedMeasure(name, expression));
                added.add(new AddColumns.Added(name, new ColumnValue(width + onCube.size() - 1), DaxType.VARIANT));
            } else {
                added.add(new AddColumns.Added(name, expression, type(expression, columns)));
            }
        }
        if (!onCube.isEmpty()) {
            source = withAdded(source, onCube);
            if (onCube.size() == added.size()) {
                // the cube's columns are the last ones, in order
                return source;
            }
        }
        AddColumns addColumns = new AddColumns(source, width, added);
        if (source instanceof ConstantTable constant) {
            // nothing to query: computed right away
            try {
                return new ConstantTable(addColumns.columns(), addColumns.apply(constant.rows()));
            } catch (DaxExecutionException e) {
                throw new DaxSemanticException(e.getMessage(), e);
            }
        }
        return addColumns;
    }

    /** @return the table computing the measures too, as its last columns, for the rows it has */
    private static TablePlan withAdded(TablePlan table, List<NamedMeasure> measures) throws DaxSemanticException {
        return switch (table) {
        case Summarize summarize when summarize.groupBy().isEmpty() ->
            throw notSupported("ADDCOLUMNS by a measure of ROW");
        // the filter tables filter within SUMMARIZECOLUMNS only, not the measures of ADDCOLUMNS
        case Summarize summarize when !summarize.filters().isEmpty() ->
            throw notSupported("ADDCOLUMNS by a measure of SUMMARIZECOLUMNS with filter tables");
        case Summarize summarize -> summarize.withAdded(measures);
        // the columns are added after those the filter and the order refer to
        case Filter filter -> new Filter(withAdded(filter.source(), measures), filter.condition());
        case TopN topN -> new TopN(withAdded(topN.source(), measures), topN.count(), topN.keys());
        case AddColumns addColumns -> throw notSupported("ADDCOLUMNS by a measure of ADDCOLUMNS computing columns");
        case ConstantTable constant -> throw notSupported("ADDCOLUMNS of a table constructor by a measure");
        };
    }

    /** @return the type of the values of the expression on rows of the columns */
    private static DaxType type(ScalarPlan plan, List<DaxColumn> columns) {
        return switch (plan) {
        case Constant constant -> constant.value() == null ? DaxType.VARIANT : DaxType.of(constant.value());
        case ColumnValue column -> columns.get(column.column()).type();
        case MeasureValue measure -> DaxType.VARIANT;
        case Comparison comparison -> DaxType.BOOLEAN;
        case InList in -> DaxType.BOOLEAN;
        case Logical logical -> DaxType.BOOLEAN;
        case Not not -> DaxType.BOOLEAN;
        case IsBlank isBlank -> DaxType.BOOLEAN;
        };
    }

    /** @return whether the argument is an order of TOPN: ASC, DESC, TRUE, FALSE, 0 or 1 */
    private static boolean isOrder(DaxExpression argument) {
        return switch (argument) {
        case Keyword keyword -> List.of("ASC", "DESC", "TRUE", "FALSE").contains(keyword.name().toUpperCase(Locale.ROOT));
        case BooleanLiteral bool -> true;
        case NumericLiteral number -> number.value().signum() == 0 || number.value().compareTo(BigDecimal.ONE) == 0;
        case FunctionCall call -> call.arguments().isEmpty()
                && List.of("TRUE", "FALSE").contains(call.functionName().toUpperCase(Locale.ROOT));
        default -> false;
        };
    }

    private static boolean ascending(DaxExpression order) {
        return switch (order) {
        case Keyword keyword -> List.of("ASC", "TRUE").contains(keyword.name().toUpperCase(Locale.ROOT));
        case BooleanLiteral bool -> bool.value();
        case NumericLiteral number -> number.value().signum() != 0;
        case FunctionCall call -> call.functionName().equalsIgnoreCase("TRUE");
        default -> throw new IllegalArgumentException("no order: " + order);
        };
    }

    /** @return the parts of the condition joined by {@code &&} */
    private static List<ScalarPlan> conjuncts(ScalarPlan condition) {
        if (condition instanceof Logical logical && logical.operator() == LogicalOperator.AND) {
            List<ScalarPlan> parts = new ArrayList<>(conjuncts(logical.left()));
            parts.addAll(conjuncts(logical.right()));
            return parts;
        }
        return List.of(condition);
    }

    private static ScalarPlan and(List<ScalarPlan> parts) {
        ScalarPlan joined = parts.get(0);
        for (ScalarPlan part : parts.subList(1, parts.size())) {
            joined = new Logical(LogicalOperator.AND, joined, part);
        }
        return joined;
    }

    private static boolean usesMeasure(ScalarPlan plan) {
        return switch (plan) {
        case MeasureValue measure -> true;
        case Constant constant -> false;
        case ColumnValue column -> false;
        case Comparison comparison -> usesMeasure(comparison.left()) || usesMeasure(comparison.right());
        case InList in -> usesMeasure(in.value());
        case Logical logical -> usesMeasure(logical.left()) || usesMeasure(logical.right());
        case Not not -> usesMeasure(not.operand());
        case IsBlank isBlank -> usesMeasure(isBlank.operand());
        };
    }

    /** @return whether the cube can compute the condition, see {@code MdxGenerator} */
    private static boolean onCube(ScalarPlan plan) {
        return onCube(plan, 0);
    }

    /**
     * @param columns how many first columns of the rows are the columns a
     *                grouping groups by, whose text the cube compares by the
     *                names of members
     * @return whether the cube computes the condition, see {@code MdxGenerator}
     */
    private static boolean onCube(ScalarPlan plan, int columns) {
        return switch (plan) {
        case MeasureValue measure -> true;
        case Constant constant -> constant.value() instanceof Number || constant.value() instanceof String
                || constant.value() instanceof Boolean;
        case ColumnValue column -> false;
        case InList in -> text(in.value(), columns)
                && in.values().stream().allMatch(v -> v == null || v instanceof String);
        case Comparison comparison when comparison.left() instanceof ColumnValue
                || comparison.right() instanceof ColumnValue ->
            text(comparison.left(), columns) && text(comparison.right(), columns);
        case Comparison comparison -> onCube(comparison.left(), columns) && onCube(comparison.right(), columns);
        case Logical logical -> onCube(logical.left(), columns) && onCube(logical.right(), columns);
        case Not not -> onCube(not.operand(), columns);
        case IsBlank isBlank -> isBlank.operand() instanceof MeasureValue;
        };
    }

    /** @return whether the plan is text the cube compares: a column grouped by or a text constant */
    private static boolean text(ScalarPlan plan, int columns) {
        return plan instanceof ColumnValue column ? column.column() < columns
                : plan instanceof Constant constant && constant.value() instanceof String;
    }

    /**
     * @return how many first columns of the table are those the grouping it is
     *         computed of groups by; 0 if none is
     */
    private static int groupedColumns(TablePlan table) {
        return switch (table) {
        case Summarize summarize -> summarize.groupBy().size();
        // these keep the first columns of their source
        case Filter filter -> groupedColumns(filter.source());
        case TopN topN -> groupedColumns(topN.source());
        case AddColumns addColumns -> Math.min(addColumns.width(), groupedColumns(addColumns.source()));
        case ConstantTable constant -> 0;
        };
    }

    private TablePlan summarize(List<ModelColumn> groupBy, List<NamedMeasure> measures) throws DaxSemanticException {
        Set<ModelColumn> distinct = new LinkedHashSet<>(groupBy);
        if (measures.isEmpty()) {
            // Without measures nothing tells which combinations of two hierarchies of one
            // table exist, and the cross product would invent rows.
            Map<String, Set<String>> hierarchiesByTable = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (ModelColumn column : distinct) {
                hierarchiesByTable.computeIfAbsent(column.table(), t -> new LinkedHashSet<>()).add(column.hierarchy());
            }
            for (Map.Entry<String, Set<String>> entry : hierarchiesByTable.entrySet()) {
                if (entry.getValue().size() > 1) {
                    throw notSupported("columns of several hierarchies of the table '" + entry.getKey()
                            + "' without a measure");
                }
            }
        }
        return new Summarize(List.copyOf(distinct), measures);
    }

    private static TablePlan constantTable(List<String> names, List<List<Object>> rows) {
        List<DaxColumn> columns = new ArrayList<>();
        for (int c = 0; c < names.size(); c++) {
            Set<DaxType> types = EnumSet.noneOf(DaxType.class);
            for (List<Object> row : rows) {
                if (row.get(c) != null) {
                    types.add(DaxType.of(row.get(c)));
                }
            }
            DaxType type;
            if (types.size() == 1) {
                type = types.iterator().next();
            } else if (types.equals(EnumSet.of(DaxType.INTEGER, DaxType.DOUBLE))) {
                type = DaxType.DOUBLE;
                for (List<Object> row : rows) {
                    if (row.get(c) instanceof Long whole) {
                        row.set(c, whole.doubleValue());
                    }
                }
            } else {
                type = DaxType.VARIANT;
            }
            columns.add(new DaxColumn("[" + names.get(c) + "]", Optional.empty(), type));
        }
        return new ConstantTable(columns, rows);
    }

    // --- references

    private ModelColumn column(Identifier identifier) throws DaxSemanticException {
        String[] name = tableAndName(identifier);
        ModelTable table = model.table(name[0])
                .orElseThrow(() -> new DaxSemanticException("the table '" + name[0] + "' does not exist"));
        return table.column(name[1]).orElseThrow(
                () -> new DaxSemanticException("the column '" + name[0] + "'[" + name[1] + "] does not exist"));
    }

    /**
     * @return the measure the expression refers to, as {@code [Measure]} or
     *         {@code 'Table'[Measure]}; empty if it is no such reference
     */
    private Optional<ModelMeasure> measureReference(DaxExpression expression) throws DaxSemanticException {
        if (expression instanceof Scalar scalar) {
            return Optional.of(model.measure(scalar.name())
                    .orElseThrow(() -> new DaxSemanticException("the measure [" + scalar.name() + "] does not exist")));
        }
        if (expression instanceof Identifier identifier) {
            String[] name = tableAndName(identifier);
            Optional<ModelMeasure> measure = model.measure(name[1]);
            if (measure.isEmpty()) {
                throw new DaxSemanticException(
                        "'" + name[0] + "'[" + name[1] + "] is no measure; only measures are supported here yet");
            }
            return measure;
        }
        return Optional.empty();
    }

    private static String[] tableAndName(Identifier identifier) throws DaxSemanticException {
        List<DaxExpression> parts = identifier.parts();
        if (parts.size() == 2 && parts.get(1) instanceof Scalar column) {
            if (parts.get(0) instanceof Entity table) {
                return new String[] { table.name(), column.name() };
            }
            if (parts.get(0) instanceof Keyword table) {
                return new String[] { table.name(), column.name() };
            }
        }
        throw notSupported("the reference " + identifier);
    }

    /** @return the name of the referenced column as a result column has it, e.g. {@code Product[Category]} */
    private static String qualifiedName(Identifier identifier) throws DaxSemanticException {
        String[] name = tableAndName(identifier);
        return name[0] + "[" + name[1] + "]";
    }

    private static String columnName(DaxExpression expression, String function) throws DaxSemanticException {
        if (expression instanceof StringLiteral name) {
            return name.value();
        }
        throw new DaxSemanticException(function + " takes a string as column name");
    }

    // --- scalars

    private Object constant(DaxExpression expression) throws DaxSemanticException {
        try {
            return scalar(expression, null).evaluate(List.of());
        } catch (DaxExecutionException e) {
            throw new DaxSemanticException(e.getMessage(), e);
        }
    }

    /**
     * @param row the columns of the row the expression is computed on;
     *            {@code null} where a constant is expected
     */
    private ScalarPlan scalar(DaxExpression expression, List<DaxColumn> row) throws DaxSemanticException {
        return switch (expression) {
        case NumericLiteral number -> new Constant(number(number.value()));
        case StringLiteral string -> new Constant(string.value());
        case BooleanLiteral bool -> new Constant(bool.value());
        case DateTimeLiteral dateTime -> new Constant(dateTime.value());
        case Keyword keyword when keyword.name().equalsIgnoreCase("TRUE") -> new Constant(Boolean.TRUE);
        case Keyword keyword when keyword.name().equalsIgnoreCase("FALSE") -> new Constant(Boolean.FALSE);
        case FunctionCall call when call.functionName().equalsIgnoreCase("ISBLANK") ->
            new IsBlank(scalar(onlyArgument(call, "ISBLANK takes one value"), row));
        case FunctionCall call when call.functionName().equalsIgnoreCase("NOT") ->
            new Not(scalar(onlyArgument(call, "NOT takes one value"), row));
        case FunctionCall call when call.arguments().isEmpty() -> switch (call.functionName().toUpperCase(Locale.ROOT)) {
            case "BLANK" -> new Constant(null);
            case "TRUE" -> new Constant(Boolean.TRUE);
            case "FALSE" -> new Constant(Boolean.FALSE);
            default -> throw notSupported("the function " + call.functionName() + where(row));
            };
        case BooleanExpression comparison when comparison.operator() == BooleanOperator.IN -> in(comparison, row);
        case BooleanExpression comparison -> new Comparison(comparison.operator(), scalar(comparison.left(), row),
                scalar(comparison.right(), row));
        case LogicalExpression logical -> new Logical(logical.operator(), scalar(logical.left(), row),
                scalar(logical.right(), row));
        case Parameter parameter -> {
            if (!parameters.containsKey(parameter.name())) {
                throw new DaxSemanticException("the parameter @" + parameter.name() + " has no value");
            }
            yield new Constant(parameters.get(parameter.name()));
        }
        case VariableReference variable -> {
            if (!variables.containsKey(variable.name())) {
                throw new DaxSemanticException("the variable " + variable.name() + " is not defined");
            }
            yield new Constant(variables.get(variable.name()));
        }
        case Identifier identifier when row != null -> {
            // 'Measures'[Sales] is a measure, unless the row has such a column
            String name = qualifiedName(identifier);
            Optional<ModelMeasure> measure = model.measure(tableAndName(identifier)[1]);
            yield indexOf(row, name) < 0 && measure.isPresent() ? new MeasureValue(measure.get())
                    : columnValue(name, row);
        }
        case Scalar scalar when row != null -> {
            // [S] is a column of the row first, e.g. of SUMMARIZECOLUMNS, then a measure
            Optional<ModelMeasure> measure = model.measure(scalar.name());
            yield indexOf(row, "[" + scalar.name() + "]") < 0 && measure.isPresent()
                    ? new MeasureValue(measure.get())
                    : columnValue("[" + scalar.name() + "]", row);
        }
        default -> throw notSupported("an expression of kind " + kind(expression) + where(row));
        };
    }

    private ScalarPlan in(BooleanExpression in, List<DaxColumn> row) throws DaxSemanticException {
        if (!(in.right() instanceof TableConstructor constructor)) {
            throw notSupported("IN an expression of kind " + kind(in.right()));
        }
        TablePlan list = tableConstructor(constructor);
        if (list.columns().size() != 1 || in.left() instanceof RowConstructor) {
            throw notSupported("IN with several columns");
        }
        List<Object> values = new ArrayList<>();
        for (List<Object> value : ((ConstantTable) list).rows()) {
            values.add(value.get(0));
        }
        return new InList(scalar(in.left(), row), values);
    }

    private static ScalarPlan columnValue(String name, List<DaxColumn> row) throws DaxSemanticException {
        int index = indexOf(row, name);
        if (index < 0) {
            throw new DaxSemanticException("the column " + name + " is not in the table the condition is computed on");
        }
        return new ColumnValue(index);
    }

    private static DaxExpression onlyArgument(FunctionCall call, String message) throws DaxSemanticException {
        if (call.arguments().size() != 1) {
            throw new DaxSemanticException(message);
        }
        return call.arguments().get(0);
    }

    private static String where(List<DaxColumn> row) {
        return row == null ? " where a constant is expected" : " in a row condition";
    }

    private static Object number(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0 && stripped.toBigInteger().bitLength() < 64) {
            return stripped.longValueExact();
        }
        return value.doubleValue();
    }

    // --- order

    private static List<SortKey> orderBy(List<OrderByItem> items, List<DaxColumn> columns) throws DaxSemanticException {
        List<SortKey> keys = new ArrayList<>();
        for (OrderByItem item : items) {
            if (item.startAt().isPresent()) {
                throw notSupported("START AT");
            }
            String name = switch (item.expression()) {
            case Scalar scalar -> "[" + scalar.name() + "]";
            case Identifier identifier -> qualifiedName(identifier);
            default -> throw notSupported("ORDER BY an expression of kind " + kind(item.expression()));
            };
            int index = indexOf(columns, name);
            if (index < 0) {
                throw new DaxSemanticException("ORDER BY " + name + ": no such column in the result");
            }
            keys.add(new SortKey(index, item.direction() != OrderByItem.SortDirection.DESC));
        }
        return keys;
    }

    private static int indexOf(List<DaxColumn> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).name().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    // --- errors

    private static DaxSemanticException notSupported(String what) {
        return new DaxSemanticException(what + " is not supported yet");
    }

    /** @return the name of the model interface the expression implements */
    private static String kind(DaxExpression expression) {
        for (Class<?> type = expression.getClass(); type != null; type = type.getSuperclass()) {
            for (Class<?> implemented : type.getInterfaces()) {
                if (DaxExpression.class.isAssignableFrom(implemented) && implemented != DaxExpression.class) {
                    return implemented.getSimpleName();
                }
            }
        }
        return expression.getClass().getSimpleName();
    }
}
