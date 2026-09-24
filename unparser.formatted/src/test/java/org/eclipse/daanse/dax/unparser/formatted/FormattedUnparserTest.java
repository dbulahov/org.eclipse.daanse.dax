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
package org.eclipse.daanse.dax.unparser.formatted;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.DaxStatementR;
import org.eclipse.daanse.dax.model.record.EvaluateStatementR;
import org.eclipse.daanse.dax.model.record.MeasureDefinitionR;
import org.eclipse.daanse.dax.model.record.OrderByItemR;
import org.eclipse.daanse.dax.model.record.expression.BooleanExpressionR;
import org.eclipse.daanse.dax.model.record.expression.FunctionCallR;
import org.eclipse.daanse.dax.model.record.expression.IdentifierR;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.dax.model.record.expression.ParameterR;
import org.eclipse.daanse.dax.model.record.expression.RowConstructorR;
import org.eclipse.daanse.dax.model.record.expression.StringLiteralR;
import org.eclipse.daanse.dax.model.record.expression.TableConstructorR;
import org.junit.jupiter.api.Test;

class FormattedUnparserTest {

    private final FormattedUnparser unparser = new FormattedUnparser();

    /**
     * Creates an unparser that has been activated with the given configuration,
     * exactly as SCR would do it (the configuration is the component property
     * type of the component).
     */
    private static FormattedUnparser configured(boolean includeComments, int indentSize) {
        FormattedUnparser configuredUnparser = new FormattedUnparser();
        configuredUnparser.activate(new FormattedUnparser.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return FormattedUnparser.Config.class;
            }

            @Override
            public boolean includeComments() {
                return includeComments;
            }

            @Override
            public int indentSize() {
                return indentSize;
            }
        });
        return configuredUnparser;
    }

    private static DaxStatement evaluate(DaxExpression expression) {
        return new DaxStatementR(List.of(),
                List.of(new EvaluateStatementR(expression, List.of())));
    }

    @Test
    void testSimpleEvaluateStatement_WithComments() {
        DaxExpression tableRef = IdentifierR.ofColumn("Sales", "Amount");

        String result = unparser.unparseDaxStatement(evaluate(tableRef));

        assertThat(result)
                .isNotNull()
                .contains("// DAX Query")
                .contains("// EVALUATE statement")
                .contains("EVALUATE")
                .contains("'Sales'[Amount]")
                .contains("// Table[Column] reference");
    }

    @Test
    void testFunctionCall_WithComments() {
        DaxExpression columnRef = IdentifierR.ofColumn("Sales", "Amount");
        DaxExpression sumFunc = new FunctionCallR("SUM", List.of(columnRef));

        String result = unparser.unparseDaxStatement(evaluate(sumFunc));

        assertThat(result)
                .contains("// Function: SUM with 1 argument(s)")
                .contains("SUM('Sales'[Amount])");
    }

    @Test
    void testDefineMeasure_WithComments() {
        DaxExpression columnRef = IdentifierR.ofColumn("Sales", "Amount");
        DaxExpression sumFunc = new FunctionCallR("SUM", List.of(columnRef));
        MeasureDefinition measureClause = new MeasureDefinitionR("Sales", "Total", sumFunc);

        DaxStatement defStmt = new DaxStatementR(List.of(measureClause),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"))));

        String result = unparser.unparseDaxStatement(defStmt);

        assertThat(result)
                .contains("// DEFINE block")
                .contains("// Measure: Sales[Total]")
                .contains("MEASURE 'Sales'[Total] =")
                .contains("// Function: SUM");
    }

    @Test
    void testNumericLiteral_WithComment() {
        MeasureDefinition measureClause = new MeasureDefinitionR("Sales", "MyMeasure",
                new NumericLiteralR(new BigDecimal("42")));

        DaxStatement defStmt = new DaxStatementR(List.of(measureClause),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"))));

        String result = unparser.unparseDaxStatement(defStmt);

        assertThat(result).contains("42 // Numeric value for measure calculation");
    }

    @Test
    void testNestedExpression_HasNoCommentsInsideTheExpression() {
        // comments must never appear inside an expression, they would swallow the
        // rest of the line and produce invalid DAX
        DaxExpression comparison = new BooleanExpressionR(IdentifierR.ofMeasure("Amount"),
                BooleanExpression.BooleanOperator.GREATER_THAN, new NumericLiteralR(new BigDecimal("100")));
        DaxExpression filterFunc = new FunctionCallR("FILTER",
                List.of(IdentifierR.ofTable("Sales"), comparison));

        String result = unparser.unparseDaxStatement(evaluate(filterFunc));

        assertThat(result)
                .contains("// Function: FILTER with 2 argument(s)")
                .contains("FILTER('Sales', [Amount] > 100)")
                .doesNotContain("[Amount] // ")
                .doesNotContain("100 // ");
    }

    @Test
    void testTableConstructorAndTrailingClauses() {
        DaxExpression table = new TableConstructorR(List.of(
                new RowConstructorR(List.of(new NumericLiteralR(BigDecimal.ONE), new StringLiteralR("a")))));

        String result = unparser.unparseDaxStatement(evaluate(table));

        assertThat(result).contains("{(1, \"a\")}");
    }

    @Test
    void testStartAtClause_WithComment() {
        // EVALUATE 'Sales' ORDER BY [Year] DESC START AT @StartYear
        OrderByItem year = new OrderByItemR(IdentifierR.ofMeasure("Year"), OrderByItem.SortDirection.DESC,
                Optional.of(new ParameterR("StartYear")));
        DaxStatement stmt = new DaxStatementR(List.of(),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"), List.of(year))));

        String result = unparser.unparseDaxStatement(stmt);

        assertThat(result)
                .contains("ORDER BY [Year] DESC")
                .contains("// Pagination: start at 1 sort key value(s)")
                .contains("START AT @StartYear");
    }

    @Test
    void testConfiguration_CommentsDisabledAndCustomIndent() {
        DaxExpression columnRef = IdentifierR.ofColumn("Sales", "Amount");
        DaxExpression sumFunc = new FunctionCallR("SUM", List.of(columnRef));
        MeasureDefinition measureClause = new MeasureDefinitionR("Sales", "Total", sumFunc);
        DaxStatement defStmt = new DaxStatementR(List.of(measureClause),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"))));

        String result = configured(false, 2).unparseDaxStatement(defStmt);

        // indent of 2 spaces per level
        assertThat(result).isEqualTo("DEFINE\n"
                + "  MEASURE 'Sales'[Total] = SUM('Sales'[Amount])\n"
                + "\n"
                + "EVALUATE\n"
                + "  'Sales'");
        assertThat(result).doesNotContain("//");
    }

    @Test
    void testConfiguration_CommentsEnabledWithCustomIndent() {
        DaxExpression tableRef = IdentifierR.ofTable("Sales");

        String result = configured(true, 8).unparseDaxStatement(evaluate(tableRef));

        assertThat(result)
                .contains("// DAX Query")
                .contains("\n        'Sales'");
    }

    @Test
    void testDefineClauses_EachOnItsOwnLineWithoutCommas() {
        // DAX separates definitions by whitespace; a trailing comment ends each line
        MeasureDefinition first = new MeasureDefinitionR("Sales", "A", new NumericLiteralR(BigDecimal.ONE));
        MeasureDefinition second = new MeasureDefinitionR("Sales", "B", new NumericLiteralR(new BigDecimal("2")));
        DaxStatement defStmt = new DaxStatementR(List.of(first, second),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"))));

        String result = unparser.unparseDaxStatement(defStmt);

        assertThat(result)
                .contains("MEASURE 'Sales'[A] = 1 // Numeric value for measure calculation\n")
                .contains("MEASURE 'Sales'[B] = 2 // Numeric value for measure calculation\n");
    }

    @Test
    void testOrderByAndStartAt_WithoutComments() {
        OrderByItem year = new OrderByItemR(IdentifierR.ofMeasure("Year"), OrderByItem.SortDirection.DESC,
                Optional.of(new NumericLiteralR(new BigDecimal("2024"))));
        OrderByItem region = new OrderByItemR(IdentifierR.ofMeasure("Region"), OrderByItem.SortDirection.ASC,
                Optional.of(new StringLiteralR("EU")));
        DaxStatement stmt = new DaxStatementR(List.of(),
                List.of(new EvaluateStatementR(IdentifierR.ofTable("Sales"), List.of(year, region))));

        String result = configured(false, 4).unparseDaxStatement(stmt);

        assertThat(result).isEqualTo("""
                EVALUATE
                    'Sales'
                ORDER BY [Year] DESC,
                    [Region] ASC
                START AT (2024, "EU")""");
    }
}
