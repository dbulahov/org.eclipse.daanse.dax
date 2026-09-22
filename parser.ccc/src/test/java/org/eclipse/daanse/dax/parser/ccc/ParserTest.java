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
package org.eclipse.daanse.dax.parser.ccc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanLiteral;
import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.TableConstructor;
import org.eclipse.daanse.dax.parser.api.DaxParserException;
import org.eclipse.daanse.dax.parser.ccc.tree.StringLiteral;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;

/**
 * Tests for parsing .
 */
@RequireServiceComponentRuntime
class ParserTest {

    private DaxExpression getFirstExpression(EvaluateStatement evalStmt) {
        TableConstructor tc = (TableConstructor) evalStmt.tableExpression();
        assertThat(tc.rows()).hasSize(1);
        RowConstructor row = tc.rows().get(0);
        assertThat(row.columns()).hasSize(1);
        return row.columns().get(0);
    }

    @Test
    void testStringLiteral() throws DaxParserException {
        String dax = "EVALUATE {\"Hello World\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(StringLiteral.class);
        StringLiteral literal = (StringLiteral) expr;
        assertThat(literal.value()).isEqualTo("Hello World");
    }

    @Test
    void testDoubleEvaluateStringLiteral() throws DaxParserException {
        String dax = "EVALUATE {\"Hello World1\"} EVALUATE {\"Hello World2\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(StringLiteral.class);
        StringLiteral literal = (StringLiteral) expr;
        assertThat(literal.value()).isEqualTo("Hello World1");

        evalStmt = stmt.evaluateStatements().get(1);
        expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(StringLiteral.class);
        literal = (StringLiteral) expr;
        assertThat(literal.value()).isEqualTo("Hello World2");
    }

    @Test
    void testStringLiteralWithEscapedQuotes() throws DaxParserException {
        String dax = "EVALUATE {\"Say \"\"Hello\"\"\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        StringLiteral literal = (StringLiteral) expr;
        assertThat(literal.value()).isEqualTo("Say \"Hello\"");
    }

    @Test
    void testConstructorRejectsNullInput() {
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(null))
                .withMessage("statement must not be null");
    }

    @Test
    void testConstructorRejectsEmptyInput() {
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(""))
                .withMessage("statement must not be empty");
    }

    @Test
    void testMissingEvaluateKeywordThrows() {
        String dax = "{\"Hello World\"}";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testUnterminatedTableConstructorThrows() {
        String dax = "EVALUATE {\"Hello World\"";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testEmptyTableConstructorThrows() {
        // a table constructor requires at least one row; {} is not valid DAX
        String dax = "EVALUATE {}";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testUnterminatedStringLiteralThrows() {
        String dax = "EVALUATE {\"Hello World}";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testTrailingGarbageAfterDaxStatementThrows() {
        // parseDaxStatement() requires the whole input to be consumed
        String dax = "EVALUATE {\"Hello World\"} garbage";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testTrailingGarbageAfterExpressionThrows() {
        // parseExpression() requires the whole input to be consumed
        String dax = "\"Hello World\" \"Goodbye World\"";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseExpression());
    }

    @Test
    void testStandaloneEvaluateStatementDoesNotRequireEndOfInput() throws DaxParserException {
        // unlike parseDaxStatement(), parseEvaluateStatement() may leave
        // trailing input unconsumed
        String dax = "EVALUATE {\"Hello World\"} EVALUATE {\"Goodbye World\"}";
        EvaluateStatement evalStmt = new DaxParserWrapper(dax).parseEvaluateStatement();

        DaxExpression expr = getFirstExpression(evalStmt);
        StringLiteral literal = (StringLiteral) expr;
        assertThat(literal.value()).isEqualTo("Hello World");
    }

    @Test
    void testSyntaxErrorReportsThePositionOfTheOffendingToken() {
        String dax = "EVALUATE {\"First\"}\nEVALUATE }";

        DaxParserException exception = catchThrowableOfType(DaxParserException.class,
                () -> new DaxParserWrapper(dax).parseDaxStatement());

        assertThat(exception).isNotNull();
        assertThat(exception.line()).isEqualTo(2);
        assertThat(exception.column()).isNotEqualTo(DaxParserException.UNKNOWN_POSITION);
    }

    @Test
    void testNumericLiteral() throws DaxParserException {
        String dax = "EVALUATE {10}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(NumericLiteral.class);
        NumericLiteral literal = (NumericLiteral) expr;
        assertThat(literal.value()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    void testNumericLiteralWithFraction() throws DaxParserException {
        String dax = "EVALUATE {3.14}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(NumericLiteral.class);
        NumericLiteral literal = (NumericLiteral) expr;
        assertThat(literal.value()).isEqualTo(new BigDecimal("3.14"));
    }

    @Test
    void testBooleanLiteral() throws DaxParserException {
        String dax = "EVALUATE {true}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(BooleanLiteral.class);
        BooleanLiteral literal = (BooleanLiteral) expr;
        assertTrue(literal.value());
    }

    @Test
    void testBooleanLiteralFalse() throws DaxParserException {
        String dax = "EVALUATE {false}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(BooleanLiteral.class);
        BooleanLiteral literal = (BooleanLiteral) expr;
        assertThat(literal.value()).isFalse();
    }

    @Test
    void testDateTimeLiteral() throws DaxParserException {
        String dax = "EVALUATE {dt\"2026-09-22\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(DateTimeLiteral.class);
        DateTimeLiteral literal = (DateTimeLiteral) expr;
        assertThat(literal.value()).isEqualTo(LocalDate.of(2026, 9, 22).atStartOfDay());
    }

    @Test
    void testDateTimeLiteralWithTime() throws DaxParserException {
        String dax = "EVALUATE {dt\"2024-01-31T10:30:00\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(DateTimeLiteral.class);
        DateTimeLiteral literal = (DateTimeLiteral) expr;
        assertThat(literal.value()).isEqualTo(LocalDateTime.of(2024, 1, 31, 10, 30, 0));
    }

    @Test
    void testBooleanExpression1() throws DaxParserException {
        String dax = "EVALUATE {5 < 6}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.LESS_THAN);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("5"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void testBooleanExpression2() throws DaxParserException {
        String dax = "EVALUATE {6 > 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.GREATER_THAN);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testBooleanExpression3() throws DaxParserException {
        String dax = "EVALUATE {6 = 6}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.EQUAL);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void testBooleanExpression4() throws DaxParserException {
        String dax = "EVALUATE {6 >= 6}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.GREATER_THAN_OR_EQUAL);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void testBooleanExpression5() throws DaxParserException {
        String dax = "EVALUATE {6 <= 6}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.LESS_THAN_OR_EQUAL);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("6"));
    }

}
