/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.dax.unparser.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.DaxStatementR;
import org.eclipse.daanse.dax.model.record.EvaluateStatementR;
import org.eclipse.daanse.dax.model.record.MeasureDefinitionR;
import org.eclipse.daanse.dax.model.record.OrderByItemR;
import org.eclipse.daanse.dax.model.record.expression.ArithmeticExpressionR;
import org.eclipse.daanse.dax.model.record.expression.BooleanExpressionR;
import org.eclipse.daanse.dax.model.record.expression.DateTimeLiteralR;
import org.eclipse.daanse.dax.model.record.expression.EntityR;
import org.eclipse.daanse.dax.model.record.expression.FunctionCallR;
import org.eclipse.daanse.dax.model.record.expression.IdentifierR;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.dax.model.record.expression.ParameterR;
import org.eclipse.daanse.dax.model.record.expression.RowConstructorR;
import org.eclipse.daanse.dax.model.record.expression.ScalarR;
import org.eclipse.daanse.dax.model.record.expression.StringLiteralR;
import org.eclipse.daanse.dax.model.record.expression.TableConstructorR;
import org.junit.jupiter.api.Test;

class SimpleUnparserTest {

    private final SimpleUnparser unparser = new SimpleUnparser();

    private static DaxStatement evaluate(DaxExpression expression) {
        return new DaxStatementR(
                List.of(new EvaluateStatementR(expression, List.of())));
    }

    @Test
    void testSimpleEvaluateStatement() {
        // CREATE: EVALUATE 'Sales'[Amount]
        DaxExpression tableRef = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount")));

        String result = unparser.unparseDaxStatement(evaluate(tableRef));

        assertThat(result).isEqualTo("EVALUATE 'Sales'[Amount]");
    }

    @Test
    void testEvaluateWithFunction() {
        // CREATE: EVALUATE SUM('Sales'[Amount])
        DaxExpression columnRef = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount")));
        DaxExpression sumFunc = new FunctionCallR("SUM", List.of(columnRef));

        String result = unparser.unparseDaxStatement(evaluate(sumFunc));

        assertThat(result).isEqualTo("EVALUATE SUM('Sales'[Amount])");
    }

    @Test
    void testDefineMeasure() {
        // CREATE: DEFINE MEASURE 'Sales'[Total] = SUM('Sales'[Amount]) EVALUATE 'Sales'
        DaxExpression columnRef = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount")));
        DaxExpression sumFunc = new FunctionCallR("SUM", List.of(columnRef));
        MeasureDefinition measureClause = new MeasureDefinitionR(
                new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Total"))), sumFunc);
        EvaluateStatement evaluateStatement = new EvaluateStatementR(
                new IdentifierR(List.of(new EntityR("Sales"))));
        DaxStatement defStmt = new DaxStatementR(List.of(evaluateStatement), List.of(measureClause));

        String result = unparser.unparseDaxStatement(defStmt);

        assertThat(result).isEqualTo("""
                DEFINE
                    MEASURE 'Sales'[Total] = SUM('Sales'[Amount])
                EVALUATE 'Sales'""");
    }

    @Test
    void testSeveralEvaluateStatements() {
        // CREATE: EVALUATE 'Sales' EVALUATE 'Product'
        DaxStatement stmt = new DaxStatementR(
                List.of(new EvaluateStatementR(new IdentifierR(List.of(new EntityR("Sales")))),
                        new EvaluateStatementR(new IdentifierR(List.of(new EntityR("Product"))))),
                List.of());

        String result = unparser.unparseDaxStatement(stmt);

        assertThat(result).isEqualTo("""
                EVALUATE 'Sales'
                EVALUATE 'Product'""");
    }

    @Test
    void testNumericLiteral() {
        String result = unparser.unparseDaxStatement(evaluate(new NumericLiteralR(new BigDecimal("42"))));

        assertThat(result).isEqualTo("EVALUATE 42");
    }

    @Test
    void testStringLiteralEscapesEmbeddedQuotes() {
        String result = unparser.unparseDaxStatement(evaluate(new StringLiteralR("He said \"hi\"")));

        assertThat(result).isEqualTo("EVALUATE \"He said \"\"hi\"\"\"");
    }

    @Test
    void testBinaryExpressionWithoutRedundantParentheses() {
        // CREATE: EVALUATE 10 + 20
        DaxExpression binExpr = new ArithmeticExpressionR(new NumericLiteralR(new BigDecimal("10")),
                ArithmeticExpression.ArithmeticOperator.ADD, new NumericLiteralR(new BigDecimal("20")));

        String result = unparser.unparseDaxStatement(evaluate(binExpr));

        assertThat(result).isEqualTo("EVALUATE 10 + 20");
    }

    @Test
    void testBinaryExpressionParenthesizesLowerPrecedenceOperand() {
        // CREATE: EVALUATE (1 + 2) * 3
        DaxExpression sum = new ArithmeticExpressionR(new NumericLiteralR(BigDecimal.ONE),
                ArithmeticExpression.ArithmeticOperator.ADD, new NumericLiteralR(new BigDecimal("2")));
        DaxExpression product = new ArithmeticExpressionR(sum, ArithmeticExpression.ArithmeticOperator.MULTIPLY,
                new NumericLiteralR(new BigDecimal("3")));

        String result = unparser.unparseDaxStatement(evaluate(product));

        assertThat(result).isEqualTo("EVALUATE (1 + 2) * 3");
    }

    @Test
    void testOrderByClause() {
        // CREATE: EVALUATE 'Sales' ORDER BY [Amount] DESC
        DaxExpression tableRef = new IdentifierR(List.of(new EntityR("Sales")));
        OrderByItem orderItem = new OrderByItemR(new IdentifierR(List.of(new ScalarR("Amount"))),
                OrderByItem.SortDirection.DESC);
        List<OrderByItem> orderBy = List.of(orderItem);
        DaxStatement stmt = new DaxStatementR(List.of(new EvaluateStatementR(tableRef, orderBy)), List.of());

        String result = unparser.unparseDaxStatement(stmt);

        assertThat(result).isEqualTo("EVALUATE 'Sales' ORDER BY [Amount] DESC");
    }

    @Test
    void testOrderByWithStartAtValues() {
        // CREATE: EVALUATE 'Sales' ORDER BY [Year] DESC, [Region] ASC START AT (2024, @Region)
        List<OrderByItem> orderBy = List.of(
                new OrderByItemR(new IdentifierR(List.of(new ScalarR("Year"))), OrderByItem.SortDirection.DESC,
                        Optional.of(new NumericLiteralR(new BigDecimal("2024")))),
                new OrderByItemR(new IdentifierR(List.of(new ScalarR("Region"))), OrderByItem.SortDirection.ASC,
                        Optional.of(new ParameterR("Region"))));
        DaxStatement stmt = new DaxStatementR(
                List.of(new EvaluateStatementR(new IdentifierR(List.of(new EntityR("Sales"))), orderBy)));

        String result = unparser.unparseDaxStatement(stmt);

        // the parser requires the parenthesized list form for several values
        assertThat(result).isEqualTo("EVALUATE 'Sales' ORDER BY [Year] DESC, [Region] ASC START AT (2024, @Region)");
    }

    @Test
    void testParameterReferenceInExpression() {
        // CREATE: EVALUATE FILTER('Sales', 'Sales'[Year] = @Year)
        DaxExpression predicate = new BooleanExpressionR(
                new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Year"))),
                BooleanExpression.BooleanOperator.EQUAL, new ParameterR("Year"));
        DaxExpression filter = new FunctionCallR("FILTER",
                List.of(new IdentifierR(List.of(new EntityR("Sales"))), predicate));

        String result = unparser.unparseDaxStatement(evaluate(filter));

        assertThat(result).isEqualTo("EVALUATE FILTER('Sales', 'Sales'[Year] = @Year)");
    }


    @Test
    void testBareIdentifierStaysBare() {
        String result = unparser.unparseDaxStatement(evaluate(new IdentifierR(List.of(new EntityR("Sales")))));

        assertThat(result).isEqualTo("EVALUATE 'Sales'");
    }

    @Test
    void testDateTimeLiteralRendersDeterministicDaxLiteral() {
        String dateOnly = unparser
                .unparseDaxStatement(evaluate(new DateTimeLiteralR(LocalDateTime.of(2024, 1, 15, 0, 0))));
        String dateTime = unparser
                .unparseDaxStatement(evaluate(new DateTimeLiteralR(LocalDateTime.of(2024, 1, 15, 10, 30, 5))));

        assertThat(dateOnly).isEqualTo("EVALUATE dt\"2024-01-15\"");
        assertThat(dateTime).isEqualTo("EVALUATE dt\"2024-01-15T10:30:05\"");
    }

    @Test
    void testTableConstructorIsNotDropped() {
        // single-column rows are bare values, multi-column rows are (a, b)
        DaxExpression singleColumn = new TableConstructorR(
                List.of(new RowConstructorR(List.of(new NumericLiteralR(BigDecimal.ONE))),
                        new RowConstructorR(List.of(new NumericLiteralR(new BigDecimal("2"))))));
        DaxExpression multiColumn = new TableConstructorR(List.of(
                new RowConstructorR(List.of(new NumericLiteralR(BigDecimal.ONE), new StringLiteralR("a"))),
                new RowConstructorR(List.of(new NumericLiteralR(new BigDecimal("2")), new StringLiteralR("b")))));

        assertThat(unparser.unparseDaxStatement(evaluate(singleColumn))).isEqualTo("EVALUATE {1, 2}");
        assertThat(unparser.unparseDaxStatement(evaluate(multiColumn)))
                .isEqualTo("EVALUATE {(1, \"a\"), (2, \"b\")}");
    }
}
