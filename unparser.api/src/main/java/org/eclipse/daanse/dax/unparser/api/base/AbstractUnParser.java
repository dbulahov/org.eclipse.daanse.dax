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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.dax.unparser.api.base;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.dax.model.api.ColumnDefinition;
import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.ParameterDefinition;
import org.eclipse.daanse.dax.model.api.TableDefinition;
import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanLiteral;
import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Entity;
import org.eclipse.daanse.dax.model.api.expression.FunctionCall;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.eclipse.daanse.dax.model.api.expression.Keyword;
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.eclipse.daanse.dax.model.api.expression.Parameter;
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.Scalar;
import org.eclipse.daanse.dax.model.api.expression.StringExpression;
import org.eclipse.daanse.dax.model.api.expression.StringLiteral;
import org.eclipse.daanse.dax.model.api.expression.TableConstructor;
import org.eclipse.daanse.dax.unparser.api.UnParser;

/**
 * Template base class for {@link UnParser} implementations.
 * <p>
 * This class contains the complete, exhaustive traversal of the DAX model
 * ({@link DaxStatement} query root, sealed {@link DefineClause} and
 * {@link DaxExpression} hierarchies) implemented with pattern-matching
 * {@code switch} statements, so the compiler enforces that every permitted
 * subtype is handled. It also centralizes DAX lexical concerns:
 * </p>
 * <ul>
 * <li>identifier quoting by the written form of each part: quoted table
 * references as {@code 'table'} (with {@code '} escaped as {@code ''}),
 * bracketed member references as {@code [part]} (with {@code ]} escaped as
 * {@code ]]}), bare names verbatim,</li>
 * <li>string literal escaping ({@code "} as {@code ""}),</li>
 * <li>deterministic date/time literal rendering as
 * {@code dt"yyyy-MM-dd"} or {@code dt"yyyy-MM-ddTHH:mm:ss"} (the ISO form
 * the parser accepts),</li>
 * <li>precedence-aware minimal parenthesization of nested binary and unary
 * expressions.</li>
 * </ul>
 * <p>
 * Subclasses customize the output through protected hook methods (for
 * example {@link #beforeStatement(StringBuilder)},
 * {@link #beforeDefineClause(StringBuilder, DefineClause)},
 * {@link #orderByItemSeparator()}) or by overriding the protected
 * {@code append*} rendering methods. The rendering methods receive an
 * optional {@code context} string describing the surrounding construct that
 * decorating implementations may use for generated comments; it may be
 * {@code null}.
 * </p>
 * <p>
 * Note: names of {@code TABLE} and {@code VAR} definitions are stored as
 * plain names in the model and are emitted verbatim; {@code MEASURE} and
 * {@code COLUMN} names are {@link Identifier}s and are quoted like any other
 * identifier.
 * </p>
 */
public abstract class AbstractUnParser implements UnParser {

    private static final DateTimeFormatter DATE_ONLY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss",
            Locale.ROOT);

    // Binding strength of the expression levels, mirroring the grammar of the
    // parser (Expressions.inc.ccc): a higher value binds tighter. Note that
    // && and || share one level there, and comparisons are non-associative.
    private static final int PREC_LOGICAL = 1;
    private static final int PREC_COMPARISON = 2;
    private static final int PREC_STRING = 3;
    private static final int PREC_ADDITIVE = 4;
    private static final int PREC_MULTIPLICATIVE = 5;
    private static final int PREC_POWER = 6;
    private static final int PREC_PRIMARY = 7;

    @Override
    public final String unparseDaxStatement(DaxStatement daxStatement) {
        StringBuilder sb = new StringBuilder();
        beforeStatement(sb);
        boolean needsSeparator = false;
        if (!daxStatement.defineClauses().isEmpty()) {
            unparseDefineBlock(sb, daxStatement.defineClauses());
            needsSeparator = true;
        }
        for (EvaluateStatement evaluate : daxStatement.evaluateStatements()) {
            if (needsSeparator) {
                sb.append('\n');
            }
            unparseEvaluateStatement(sb, evaluate);
            needsSeparator = true;
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // hooks
    // ---------------------------------------------------------------------

    /**
     * Called once before anything of the statement is rendered. Does nothing
     * by default.
     *
     * @param sb the output
     */
    protected void beforeStatement(StringBuilder sb) {
    }

    /**
     * Called before each clause of the {@code DEFINE} block, after the
     * separating comma of the previous clause. Starts a new, indented line by
     * default.
     *
     * @param sb     the output
     * @param clause the clause about to be rendered
     */
    protected void beforeDefineClause(StringBuilder sb, DefineClause clause) {
        sb.append("\n    ");
    }

    /**
     * Called before each {@code EVALUATE} statement. Does nothing by default.
     *
     * @param sb       the output
     * @param evaluate the statement about to be rendered
     */
    protected void beforeEvaluateStatement(StringBuilder sb, EvaluateStatement evaluate) {
    }

    /**
     * @return the separator between two {@code ORDER BY} items
     */
    protected String orderByItemSeparator() {
        return ", ";
    }

    /**
     * @return the separator between two function arguments, row values or
     *         {@code START AT} values
     */
    protected String argumentSeparator() {
        return ", ";
    }

    // ---------------------------------------------------------------------
    // statements
    // ---------------------------------------------------------------------

    protected void unparseDefineBlock(StringBuilder sb, List<DefineClause> defineClauses) {
        sb.append("DEFINE");
        boolean first = true;
        for (DefineClause clause : defineClauses) {
            if (!first) {
                sb.append(',');
            }
            beforeDefineClause(sb, clause);
            appendDefineClause(sb, clause);
            first = false;
        }
    }

    protected void appendDefineClause(StringBuilder sb, DefineClause clause) {
        switch (clause) {
        case MeasureDefinition measure -> {
            sb.append("MEASURE ");
            appendIdentifier(sb, measure.name(), "measure name");
            sb.append(" = ");
            appendExpression(sb, measure.expression(), "measure expression");
        }
        case ColumnDefinition column -> {
            sb.append("COLUMN ");
            appendIdentifier(sb, column.name(), "column name");
            sb.append(" = ");
            appendExpression(sb, column.expression(), "column expression");
        }
        case TableDefinition table -> {
            sb.append("TABLE ").append(table.name()).append(" = ");
            appendExpression(sb, table.expression(), "table expression");
        }
        case VariableDefinition variable -> {
            sb.append("VAR ").append(variable.name()).append(" = ");
            appendExpression(sb, variable.expression(), "variable expression");
        }
        case ParameterDefinition parameter -> {
            sb.append('@').append(parameter.name()).append(" = ");
            appendExpression(sb, parameter.expression(), "parameter expression");
        }
        }
    }

    protected void unparseEvaluateStatement(StringBuilder sb, EvaluateStatement evaluate) {
        beforeEvaluateStatement(sb, evaluate);
        sb.append("EVALUATE ");
        appendExpression(sb, evaluate.tableExpression(), "evaluate expression");
        List<OrderByItem> orderBy = evaluate.orderBy();
        if (orderBy.isEmpty()) {
            return;
        }
        sb.append(" ORDER BY ");
        List<DaxExpression> startAtValues = new ArrayList<>();
        for (int i = 0; i < orderBy.size(); i++) {
            if (i > 0) {
                sb.append(orderByItemSeparator());
            }
            OrderByItem item = orderBy.get(i);
            appendExpression(sb, item.expression(), "order by expression");
            sb.append(' ').append(item.direction().name());
            item.startAt().ifPresent(startAtValues::add);
        }
        if (!startAtValues.isEmpty()) {
            appendStartAt(sb, startAtValues);
        }
    }

    /**
     * Renders the {@code START AT} clause. The parser requires a parenthesized
     * list for more than one value; a single value is written bare unless it
     * starts with a parenthesis itself, which the parser would mistake for
     * the list form.
     */
    protected void appendStartAt(StringBuilder sb, List<DaxExpression> values) {
        sb.append(" START AT ");
        StringBuilder rendered = new StringBuilder();
        appendExpressionList(rendered, values, "start at value");
        if (values.size() == 1 && rendered.charAt(0) != '(') {
            sb.append(rendered);
        } else {
            sb.append('(').append(rendered).append(')');
        }
    }

    // ---------------------------------------------------------------------
    // expressions
    // ---------------------------------------------------------------------

    /**
     * Renders an expression in a position that accepts a full expression
     * (no surrounding operator), so no parentheses are needed.
     *
     * @param context describes the surrounding construct, may be {@code null}
     */
    protected void appendExpression(StringBuilder sb, DaxExpression expression, String context) {
        switch (expression) {
        case Literal literal -> appendLiteral(sb, literal, context);
        case TableConstructor table -> appendTableConstructor(sb, table, context);
        case FunctionCall call -> appendFunctionCall(sb, call, context);
        case Identifier identifier -> appendIdentifier(sb, identifier, context);
        case Entity entity -> appendEntity(sb, entity);
        case Scalar scalar -> appendScalar(sb, scalar);
        case Parameter parameter -> sb.append('@').append(parameter.name());
        case Keyword keyword -> sb.append(keyword.name());
        case ArithmeticExpression arithmetic -> appendBinary(sb, arithmetic.left(),
                arithmeticOperator(arithmetic.operator()), arithmetic.right(), precedence(arithmetic), false);
        case StringExpression string -> appendBinary(sb, string.left(), "&", string.right(), PREC_STRING, false);
        case BooleanExpression bool -> appendBinary(sb, bool.left(), booleanOperator(bool.operator()), bool.right(),
                PREC_COMPARISON, true);
        case LogicalExpression logical -> appendBinary(sb, logical.left(),
                logical.operator() == LogicalExpression.LogicalOperator.AND ? "&&" : "||", logical.right(),
                PREC_LOGICAL, false);
        }
    }

    /**
     * Renders a binary expression, parenthesizing an operand only where the
     * parser would otherwise build a different tree: when it binds weaker
     * than the operator, or equally strong on the right side (all binary
     * operators are left-associative) or on either side of a non-associative
     * operator.
     */
    protected void appendBinary(StringBuilder sb, DaxExpression left, String operator, DaxExpression right,
            int precedence, boolean nonAssociative) {
        appendOperand(sb, left, precedence, nonAssociative);
        sb.append(' ').append(operator).append(' ');
        appendOperand(sb, right, precedence, true);
    }

    private void appendOperand(StringBuilder sb, DaxExpression operand, int parentPrecedence, boolean parenOnEqual) {
        int operandPrecedence = precedence(operand);
        boolean parens = operandPrecedence < parentPrecedence
                || (parenOnEqual && operandPrecedence == parentPrecedence);
        if (parens) {
            sb.append('(');
        }
        appendExpression(sb, operand, "operand");
        if (parens) {
            sb.append(')');
        }
    }

    protected static int precedence(DaxExpression expression) {
        return switch (expression) {
        case LogicalExpression e -> PREC_LOGICAL;
        case BooleanExpression e -> PREC_COMPARISON;
        case StringExpression e -> PREC_STRING;
        case ArithmeticExpression e -> switch (e.operator()) {
        case ADD, MINUS -> PREC_ADDITIVE;
        case MULTIPLY, DIVIDE -> PREC_MULTIPLICATIVE;
        case POWER -> PREC_POWER;
        };
        default -> PREC_PRIMARY;
        };
    }

    protected static String arithmeticOperator(ArithmeticExpression.ArithmeticOperator operator) {
        return switch (operator) {
        case ADD -> "+";
        case MINUS -> "-";
        case MULTIPLY -> "*";
        case DIVIDE -> "/";
        case POWER -> "^";
        };
    }

    protected static String booleanOperator(BooleanExpression.BooleanOperator operator) {
        return switch (operator) {
        case EQUAL -> "=";
        case NOT_EQUAL -> "<>";
        case LESS_THAN -> "<";
        case LESS_THAN_OR_EQUAL -> "<=";
        case GREATER_THAN -> ">";
        case GREATER_THAN_OR_EQUAL -> ">=";
        case IN -> "IN";
        };
    }

    protected void appendLiteral(StringBuilder sb, Literal literal, String context) {
        switch (literal) {
        case StringLiteral string -> sb.append('"').append(string.value().replace("\"", "\"\"")).append('"');
        case NumericLiteral numeric -> sb.append(numeric.value().toPlainString());
        case BooleanLiteral bool -> sb.append(bool.value() ? "true" : "false");
        case DateTimeLiteral dateTime -> appendDateTime(sb, dateTime.value());
        }
    }

    protected void appendDateTime(StringBuilder sb, LocalDateTime value) {
        sb.append("dt\"");
        if (value.toLocalTime().toSecondOfDay() == 0 && value.getNano() == 0) {
            sb.append(DATE_ONLY_FORMAT.format(value));
        } else {
            sb.append(DATE_TIME_FORMAT.format(value));
        }
        sb.append('"');
    }

    protected void appendTableConstructor(StringBuilder sb, TableConstructor table, String context) {
        sb.append('{');
        boolean first = true;
        for (RowConstructor row : table.rows()) {
            if (!first) {
                sb.append(argumentSeparator());
            }
            List<DaxExpression> columns = row.columns();
            if (columns.size() == 1) {
                appendExpression(sb, columns.get(0), "table constructor value");
            } else {
                sb.append('(');
                appendExpressionList(sb, columns, "table constructor value");
                sb.append(')');
            }
            first = false;
        }
        sb.append('}');
    }

    protected void appendFunctionCall(StringBuilder sb, FunctionCall call, String context) {
        sb.append(call.functionName()).append('(');
        appendExpressionList(sb, call.arguments(), "argument of " + call.functionName());
        sb.append(')');
    }

    protected void appendExpressionList(StringBuilder sb, List<DaxExpression> expressions, String context) {
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) {
                sb.append(argumentSeparator());
            }
            appendExpression(sb, expressions.get(i), context);
        }
    }

    protected void appendIdentifier(StringBuilder sb, Identifier identifier, String context) {
        for (DaxExpression part : identifier.parts()) {
            appendExpression(sb, part, context);
        }
    }

    protected void appendEntity(StringBuilder sb, Entity entity) {
        sb.append('\'').append(entity.name().replace("'", "''")).append('\'');
    }

    protected void appendScalar(StringBuilder sb, Scalar scalar) {
        sb.append('[').append(scalar.name().replace("]", "]]")).append(']');
    }
}
