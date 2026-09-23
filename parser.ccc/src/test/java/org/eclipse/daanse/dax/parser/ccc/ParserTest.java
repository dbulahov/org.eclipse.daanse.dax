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

import org.eclipse.daanse.dax.model.api.ColumnDefinition;
import org.eclipse.daanse.dax.model.api.DaxStatement;
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
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.eclipse.daanse.dax.model.api.expression.Parameter;
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.Scalar;
import org.eclipse.daanse.dax.model.api.expression.StringExpression;
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

    @Test
    void testBooleanExpression6() throws DaxParserException {
        String dax = "EVALUATE {6 <> 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        BooleanExpression booleanExpr = (BooleanExpression) expr;
        assertThat(booleanExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator.NOT_EQUAL);
        assertThat(booleanExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(booleanExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) booleanExpr.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testArithmeticExpression1() throws DaxParserException {
        String dax = "EVALUATE {6 + 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(arithmeticExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(arithmeticExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testArithmeticExpression2() throws DaxParserException {
        String dax = "EVALUATE {6 - 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MINUS);
        assertThat(arithmeticExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(arithmeticExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testArithmeticExpression3() throws DaxParserException {
        String dax = "EVALUATE {6 * 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(arithmeticExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(arithmeticExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testArithmeticExpression4() throws DaxParserException {
        String dax = "EVALUATE {6 / 2}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.DIVIDE);
        assertThat(arithmeticExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(arithmeticExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.right()).value()).isEqualTo(new BigDecimal("2"));
    }

    @Test
    void testArithmeticExpression5() throws DaxParserException {
        String dax = "EVALUATE {3 ^ 2}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.POWER);
        assertThat(arithmeticExpr.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.left()).value()).isEqualTo(new BigDecimal("3"));
        assertThat(arithmeticExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) arithmeticExpr.right()).value()).isEqualTo(new BigDecimal("2"));
    }

    @Test
    void testArithmeticExpression6() throws DaxParserException {
        String dax = "EVALUATE {\"a\" + \"b\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        ArithmeticExpression arithmeticExpr = (ArithmeticExpression) expr;
        assertThat(arithmeticExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(arithmeticExpr.left()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) arithmeticExpr.left()).value()).isEqualTo("a");
        assertThat(arithmeticExpr.right()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) arithmeticExpr.right()).value()).isEqualTo("b");
    }

    @Test
    void testChainedArithmeticExpressionIsLeftAssociative() throws DaxParserException {
        // 6 + 5 + 4 must build the left-associative tree (6 + 5) + 4, not
        // one flat three-operand node and not the right-associative
        // 6 + (5 + 4) grouping.
        String dax = "EVALUATE {6 + 5 + 4}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression outer = (ArithmeticExpression) expr;
        assertThat(outer.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);

        assertThat(outer.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) outer.right()).value()).isEqualTo(new BigDecimal("4"));

        assertThat(outer.left()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression inner = (ArithmeticExpression) outer.left();
        assertThat(inner.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(inner.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) inner.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(inner.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) inner.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testLogicalExpressionAnd() throws DaxParserException {
        String dax = "EVALUATE {5 < 6 && 6 > 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(LogicalExpression.class);
        LogicalExpression logicalExpr = (LogicalExpression) expr;
        assertThat(logicalExpr.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator.AND);
        assertThat(logicalExpr.left()).isInstanceOf(BooleanExpression.class);
        assertThat(logicalExpr.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    void testLogicalExpressionOr() throws DaxParserException {
        String dax = "EVALUATE {5 < 6 || 6 < 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(LogicalExpression.class);
        LogicalExpression logicalExpr = (LogicalExpression) expr;
        assertThat(logicalExpr.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator.OR);
        assertThat(logicalExpr.left()).isInstanceOf(BooleanExpression.class);
        assertThat(logicalExpr.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    void testLogicalExpressionWithBooleanLiteralOperands() throws DaxParserException {
        String dax = "EVALUATE {true && false}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        LogicalExpression logicalExpr = (LogicalExpression) expr;
        assertThat(logicalExpr.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator.AND);
        assertThat(logicalExpr.left()).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) logicalExpr.left()).value()).isTrue();
        assertThat(logicalExpr.right()).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) logicalExpr.right()).value()).isFalse();
    }

    @Test
    void testChainedLogicalExpressionIsLeftAssociative() throws DaxParserException {
        // true && true && false must build the left-associative tree
        // (true && true) && false, not one flat three-operand node.
        String dax = "EVALUATE {true && true && false}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(LogicalExpression.class);
        LogicalExpression outer = (LogicalExpression) expr;
        assertThat(outer.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator.AND);
        assertThat(outer.right()).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) outer.right()).value()).isFalse();

        assertThat(outer.left()).isInstanceOf(LogicalExpression.class);
        LogicalExpression inner = (LogicalExpression) outer.left();
        assertThat(inner.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator.AND);
        assertThat(inner.left()).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) inner.left()).value()).isTrue();
        assertThat(inner.right()).isInstanceOf(BooleanLiteral.class);
        assertThat(((BooleanLiteral) inner.right()).value()).isTrue();
    }

    @Test
    void testLogicalExpressionBindsLooserThanComparisonAndArithmetic() throws DaxParserException {
        // 6 + 5 < 20 && 1 = 1 must parse as ((6 + 5) < 20) && (1 = 1): both
        // arithmetic and comparison bind tighter than &&.
        String dax = "EVALUATE {6 + 5 < 20 && 1 = 1}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(LogicalExpression.class);
        LogicalExpression logicalExpr = (LogicalExpression) expr;

        assertThat(logicalExpr.left()).isInstanceOf(BooleanExpression.class);
        BooleanExpression leftComparison = (BooleanExpression) logicalExpr.left();
        assertThat(leftComparison.left()).isInstanceOf(ArithmeticExpression.class);

        assertThat(logicalExpr.right()).isInstanceOf(BooleanExpression.class);
    }

    @Test
    void testParenthesizedArithmeticExpressionOverridesPrecedence() throws DaxParserException {
        // 6 + (5 + 4) must parse as 6 + (5 + 4): the parens are a distinct
        // right-hand ArithmeticExpression node, not flattened away and not
        // corrupted by the delimiter tokens around the grouped sub-expression.
        String dax = "EVALUATE {6 + (5 + 4)}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression outer = (ArithmeticExpression) expr;
        assertThat(outer.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);

        assertThat(outer.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) outer.left()).value()).isEqualTo(new BigDecimal("6"));

        assertThat(outer.right()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression inner = (ArithmeticExpression) outer.right();
        assertThat(inner.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(inner.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) inner.left()).value()).isEqualTo(new BigDecimal("5"));
        assertThat(inner.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) inner.right()).value()).isEqualTo(new BigDecimal("4"));
    }

    @Test
    void testMultiplicationBindsTighterThanAdditionRegardlessOfOrder() throws DaxParserException {
        // 6 * 5 + 4: * is already first left-to-right, so this alone would
        // pass even without real precedence - kept for symmetry with the
        // next case, which is the one that actually proves it.
        String dax = "EVALUATE {6 * 5 + 4}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        assertThat(expr).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression outer = (ArithmeticExpression) expr;
        assertThat(outer.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(outer.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) outer.right()).value()).isEqualTo(new BigDecimal("4"));

        assertThat(outer.left()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression inner = (ArithmeticExpression) outer.left();
        assertThat(inner.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(((NumericLiteral) inner.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(((NumericLiteral) inner.right()).value()).isEqualTo(new BigDecimal("5"));

        // 4 + 6 * 5 is the real proof: * appears second left-to-right, but
        // must still bind tighter than +, giving 4 + (6 * 5), i.e. the
        // OUTER operator must be ADD, not MULTIPLY.
        String dax2 = "EVALUATE {4 + 6 * 5}";
        DaxStatement stmt2 = new DaxParserWrapper(dax2).parseDaxStatement();
        DaxExpression expr2 = getFirstExpression(stmt2.evaluateStatements().get(0));
        assertThat(expr2).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression outer2 = (ArithmeticExpression) expr2;
        assertThat(outer2.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(outer2.left()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) outer2.left()).value()).isEqualTo(new BigDecimal("4"));

        assertThat(outer2.right()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression inner2 = (ArithmeticExpression) outer2.right();
        assertThat(inner2.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(((NumericLiteral) inner2.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(((NumericLiteral) inner2.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testPowerBindsTighterThanMultiplicationAndAddition() throws DaxParserException {
        // 2 + 3 * 4 ^ 2 must parse as 2 + (3 * (4 ^ 2)): ^ tighter than *,
        // * tighter than +.
        String dax = "EVALUATE {2 + 3 * 4 ^ 2}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        DaxExpression expr = getFirstExpression(stmt.evaluateStatements().get(0));
        ArithmeticExpression add = (ArithmeticExpression) expr;
        assertThat(add.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(((NumericLiteral) add.left()).value()).isEqualTo(new BigDecimal("2"));

        ArithmeticExpression multiply = (ArithmeticExpression) add.right();
        assertThat(multiply.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(((NumericLiteral) multiply.left()).value()).isEqualTo(new BigDecimal("3"));

        ArithmeticExpression power = (ArithmeticExpression) multiply.right();
        assertThat(power.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.POWER);
        assertThat(((NumericLiteral) power.left()).value()).isEqualTo(new BigDecimal("4"));
        assertThat(((NumericLiteral) power.right()).value()).isEqualTo(new BigDecimal("2"));
    }

    @Test
    void testParenthesesOverridePrecedence() throws DaxParserException {
        // (4 + 6) * 5 must respect the explicit parens: MULTIPLY(ADD(4,6),
        // 5) = 50, not the precedence-driven ADD(4, MULTIPLY(6,5)) = 34 that
        // 4 + 6 * 5 (no parens) gives.
        String dax = "EVALUATE {(4 + 6) * 5}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        DaxExpression expr = getFirstExpression(stmt.evaluateStatements().get(0));
        assertThat(expr).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression outer = (ArithmeticExpression) expr;
        assertThat(outer.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(outer.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) outer.right()).value()).isEqualTo(new BigDecimal("5"));

        assertThat(outer.left()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression inner = (ArithmeticExpression) outer.left();
        assertThat(inner.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(((NumericLiteral) inner.left()).value()).isEqualTo(new BigDecimal("4"));
        assertThat(((NumericLiteral) inner.right()).value()).isEqualTo(new BigDecimal("6"));
    }

    @Test
    void testMultiColumnRowConstructorStillParsesAfterPrecedenceChange() throws DaxParserException {
        // Guards against the RowConstructor/PrimaryExpression grouping-parens
        // ambiguity: a row starting with LPAREN must still be recognized as
        // an explicit multi-column row when a real row follows (comma or
        // closing brace after the matching RPAREN), not misread as a bare
        // parenthesized arithmetic expression.
        String dax = "EVALUATE {(\"a\", 1), (\"b\", 2)}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        TableConstructor tc = (TableConstructor) stmt.evaluateStatements().get(0).tableExpression();
        assertThat(tc.rows()).hasSize(2);
        assertThat(tc.rows().get(0).columns()).hasSize(2);
        assertThat(tc.rows().get(1).columns()).hasSize(2);
    }

    @Test
    void testStringExpression1() throws DaxParserException {
        String dax = "EVALUATE {\"a\" & \"b\"}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        DaxExpression expr = getFirstExpression(evalStmt);
        StringExpression stringExpr = (StringExpression) expr;
        assertThat(stringExpr.operator()).isEqualTo(org.eclipse.daanse.dax.model.api.expression.StringExpression.StringOperator.AND);
        assertThat(stringExpr.left()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) stringExpr.left()).value()).isEqualTo("a");
        assertThat(stringExpr.right()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) stringExpr.right()).value()).isEqualTo("b");
    }

    @Test
    void testMixedAdditiveAndMultiplicativeChainFoldsLeftToRight() throws DaxParserException {
        // 4 + 6 * 5 + 1 - 2 * 4. + and - are ONE precedence level, folded
        // left to right (like every other chain in this grammar), with each
        // * nested only where it directly occurs - NOT "first + pulls
        // everything after it into one right-hand group". So the actual
        // tree is:
        //   ((4 + (6 * 5)) + 1) - (2 * 4)
        // i.e. outermost operator is the LAST one scanned (MINUS), not the
        // first (ADD). Both groupings evaluate to the same value (27), since
        // +/- are associative/commutative as a chain, but only this shape is
        // what the parser actually builds.
        String dax = "EVALUATE {4 + 6 * 5 + 1 - 2 * 4}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        DaxExpression expr = getFirstExpression(stmt.evaluateStatements().get(0));
        assertThat(expr).isInstanceOf(ArithmeticExpression.class);

        // Outermost: ... - (2 * 4)
        ArithmeticExpression minus = (ArithmeticExpression) expr;
        assertThat(minus.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MINUS);

        assertThat(minus.right()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression rightMultiply = (ArithmeticExpression) minus.right();
        assertThat(rightMultiply.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(((NumericLiteral) rightMultiply.left()).value()).isEqualTo(new BigDecimal("2"));
        assertThat(((NumericLiteral) rightMultiply.right()).value()).isEqualTo(new BigDecimal("4"));

        // (4 + (6 * 5)) + 1
        assertThat(minus.left()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression addOne = (ArithmeticExpression) minus.left();
        assertThat(addOne.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(((NumericLiteral) addOne.right()).value()).isEqualTo(new BigDecimal("1"));

        // 4 + (6 * 5)
        assertThat(addOne.left()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression addFour = (ArithmeticExpression) addOne.left();
        assertThat(addFour.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.ADD);
        assertThat(((NumericLiteral) addFour.left()).value()).isEqualTo(new BigDecimal("4"));

        assertThat(addFour.right()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression leftMultiply = (ArithmeticExpression) addFour.right();
        assertThat(leftMultiply.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(((NumericLiteral) leftMultiply.left()).value()).isEqualTo(new BigDecimal("6"));
        assertThat(((NumericLiteral) leftMultiply.right()).value()).isEqualTo(new BigDecimal("5"));
    }

    @Test
    void testParenthesesOverridePrecedenceInStringExpression() throws DaxParserException {
        // Without parens, "a" & "b" & "c" folds left-associatively:
        // ("a" & "b") & "c" - left is a StringExpression, right is a bare
        // literal. With parens around the right pair, "a" & ("b" & "c")
        // must flip that: left is now the bare literal and right is the
        // StringExpression, proving the parens are actually respected
        // inside a string expression's operand position, not just ignored.
        String dax = "EVALUATE {\"a\" & (\"b\" & \"c\")}";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        DaxExpression expr = getFirstExpression(stmt.evaluateStatements().get(0));
        assertThat(expr).isInstanceOf(StringExpression.class);
        StringExpression outer = (StringExpression) expr;
        assertThat(outer.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.StringExpression.StringOperator.AND);

        assertThat(outer.left()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) outer.left()).value()).isEqualTo("a");

        assertThat(outer.right()).isInstanceOf(StringExpression.class);
        StringExpression inner = (StringExpression) outer.right();
        assertThat(inner.operator())
                .isEqualTo(org.eclipse.daanse.dax.model.api.expression.StringExpression.StringOperator.AND);
        assertThat(inner.left()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) inner.left()).value()).isEqualTo("b");
        assertThat(inner.right()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) inner.right()).value()).isEqualTo("c");
    }

    @Test
    void testEntity1() throws DaxParserException {
        String dax = "EVALUATE 'Sales'";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.evaluateStatements()).hasSize(1);
        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Entity.class);
        Entity entity = (Entity) evalStmt.tableExpression();
        assertThat(entity.name()).isEqualTo("Sales");
    }

    @Test
    void testFunctionCallNoArguments() throws DaxParserException {
        String dax = "EVALUATE NOW()";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("NOW");
        assertThat(fc.arguments()).isEmpty();
    }


    @Test
    void testFunctionCallSingleArgument() throws DaxParserException {
        String dax = "EVALUATE ALL('Sales')";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("ALL");
        assertThat(fc.arguments()).hasSize(1);
        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        Entity entity = (Entity)fc.arguments().get(0);
        assertThat(entity.name()).isEqualToIgnoringCase("Sales");
    }

    @Test
    void testFunctionCallSingleArgument1() throws DaxParserException {
        String dax = "EVALUATE VALUES('Sales'[Status])";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("VALUES");
        assertThat(fc.arguments()).hasSize(1);
        assertThat(fc.arguments().get(0)).isInstanceOf(Identifier.class);
        Identifier identifier = (Identifier)fc.arguments().get(0);
        assertThat(identifier.parts()).hasSize(2);

        assertThat(identifier.parts().get(0)).isInstanceOf(Entity.class);
        Entity entity = (Entity)identifier.parts().get(0);
        assertThat(entity.name()).isEqualToIgnoringCase("Sales");

        assertThat(identifier.parts().get(1)).isInstanceOf(Scalar.class);
        Scalar scalar = (Scalar)identifier.parts().get(1);
        assertThat(scalar.name()).isEqualToIgnoringCase("Status");
    }


    @Test
    void testFunctionCallArguments() throws DaxParserException {
        String dax = "EVALUATE TOPN(10, 'Sales', 'Sales'[Amount], DESC)";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("TOPN");
        assertThat(fc.arguments()).hasSize(4);

        assertThat(fc.arguments().get(0)).isInstanceOf(NumericLiteral.class);
        NumericLiteral numericLiteral = (NumericLiteral)fc.arguments().get(0);
        assertThat(numericLiteral.value()).isEqualTo(new BigDecimal("10"));

        assertThat(fc.arguments().get(1)).isInstanceOf(Entity.class);
        Entity entity = (Entity)fc.arguments().get(1);
        assertThat(entity.name()).isEqualTo("Sales");


        assertThat(fc.arguments().get(2)).isInstanceOf(Identifier.class);
        Identifier identifier = (Identifier)fc.arguments().get(2);
        assertThat(identifier.parts()).hasSize(2);

        assertThat(identifier.parts().get(0)).isInstanceOf(Entity.class);
        entity = (Entity)identifier.parts().get(0);
        assertThat(entity.name()).isEqualToIgnoringCase("Sales");

        assertThat(identifier.parts().get(1)).isInstanceOf(Scalar.class);
        Scalar scalar = (Scalar)identifier.parts().get(1);
        assertThat(scalar.name()).isEqualToIgnoringCase("Amount");

        assertThat(fc.arguments().get(3)).isInstanceOf(Keyword.class);
        Keyword keyword = (Keyword)fc.arguments().get(3);
        assertThat(keyword.name()).isEqualTo("DESC");

    }

    @Test
    void testNestedFunctionCallArgument() throws DaxParserException {
        String dax = "EVALUATE SUMMARIZE('Sales', 'Product'[Category], \"Total\", SUM('Sales'[Amount]))";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("SUMMARIZE");
        assertThat(fc.arguments()).hasSize(4);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        Entity table = (Entity) fc.arguments().get(0);
        assertThat(table.name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(Identifier.class);
        Identifier groupByColumn = (Identifier) fc.arguments().get(1);
        assertThat(groupByColumn.parts()).hasSize(2);
        assertThat(groupByColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) groupByColumn.parts().get(0)).name()).isEqualToIgnoringCase("Product");
        assertThat(groupByColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) groupByColumn.parts().get(1)).name()).isEqualToIgnoringCase("Category");

        assertThat(fc.arguments().get(2)).isInstanceOf(StringLiteral.class);
        StringLiteral columnName = (StringLiteral) fc.arguments().get(2);
        assertThat(columnName.value()).isEqualTo("Total");

        assertThat(fc.arguments().get(3)).isInstanceOf(FunctionCall.class);
        FunctionCall sumCall = (FunctionCall) fc.arguments().get(3);
        assertThat(sumCall.functionName()).isEqualToIgnoringCase("SUM");
        assertThat(sumCall.arguments()).hasSize(1);

        assertThat(sumCall.arguments().get(0)).isInstanceOf(Identifier.class);
        Identifier sumColumn = (Identifier) sumCall.arguments().get(0);
        assertThat(sumColumn.parts()).hasSize(2);
        assertThat(sumColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) sumColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(sumColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) sumColumn.parts().get(1)).name()).isEqualToIgnoringCase("Amount");
    }

    @Test
    void testFunctionCallWithBooleanExpressionArgument() throws DaxParserException {
        String dax = """
                EVALUATE
                CALCULATETABLE(
                    'Sales',
                    'Date'[Year] = 2024
                )""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("CALCULATETABLE");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        Entity table = (Entity) fc.arguments().get(0);
        assertThat(table.name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression filter = (BooleanExpression) fc.arguments().get(1);
        assertThat(filter.operator()).isEqualTo(BooleanExpression.BooleanOperator.EQUAL);

        assertThat(filter.left()).isInstanceOf(Identifier.class);
        Identifier filterColumn = (Identifier) filter.left();
        assertThat(filterColumn.parts()).hasSize(2);
        assertThat(filterColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) filterColumn.parts().get(0)).name()).isEqualToIgnoringCase("Date");
        assertThat(filterColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) filterColumn.parts().get(1)).name()).isEqualToIgnoringCase("Year");

        assertThat(filter.right()).isInstanceOf(NumericLiteral.class);
        NumericLiteral filterValue = (NumericLiteral) filter.right();
        assertThat(filterValue.value()).isEqualTo(new BigDecimal("2024"));
    }

    @Test
    void testUnterminatedFunctionCallThrows() {
        // the call is missing its closing parenthesis
        String dax = "EVALUATE NOW(";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testMissingCommaBetweenFunctionCallArgumentsThrows() {
        // two arguments with no comma separating them
        String dax = "EVALUATE ALL('Sales' 'Product')";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testFunctionCallWithLeadingCommaThrows() {
        // a comma with no argument before it
        String dax = "EVALUATE NOW(,)";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testFunctionCallWithTrailingCommaThrows() {
        // a comma with no argument after it
        String dax = "EVALUATE ALL('Sales',)";
        assertThatExceptionOfType(DaxParserException.class)
                .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testEvaluateWithOrderByClause() throws DaxParserException {
        String dax = "EVALUATE 'Sales'\nORDER BY 'Sales'[OrderDate] DESC";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Entity.class);
        assertThat(((Entity) evalStmt.tableExpression()).name()).isEqualToIgnoringCase("Sales");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem item = evalStmt.orderBy().get(0);
        assertThat(item.direction()).isEqualTo(OrderByItem.SortDirection.DESC);

        assertThat(item.expression()).isInstanceOf(Identifier.class);
        Identifier orderByColumn = (Identifier) item.expression();
        assertThat(orderByColumn.parts()).hasSize(2);
        assertThat(orderByColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) orderByColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(orderByColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) orderByColumn.parts().get(1)).name()).isEqualToIgnoringCase("OrderDate");
    }

    @Test
    void testEvaluateWithoutOrderByClauseHasNoOrderByItems() throws DaxParserException {
        String dax = "EVALUATE 'Sales'";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.orderBy()).isEmpty();
    }

    @Test
    void testOrderByWithoutDirectionDefaultsToAscending() throws DaxParserException {
        String dax = "EVALUATE 'Sales' ORDER BY 'Sales'[OrderDate]";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.orderBy()).hasSize(1);
        assertThat(evalStmt.orderBy().get(0).direction()).isEqualTo(OrderByItem.SortDirection.ASC);
    }

    @Test
    void testOrderByWithMultipleItems() throws DaxParserException {
        String dax = "EVALUATE 'Sales' ORDER BY 'Sales'[OrderDate] DESC, 'Sales'[Amount]";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.orderBy()).hasSize(2);

        OrderByItem first = evalStmt.orderBy().get(0);
        assertThat(first.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertThat(first.expression()).isInstanceOf(Identifier.class);
        Identifier firstColumn = (Identifier) first.expression();
        assertThat(firstColumn.parts()).hasSize(2);
        assertThat(firstColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) firstColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(firstColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) firstColumn.parts().get(1)).name()).isEqualToIgnoringCase("OrderDate");

        OrderByItem second = evalStmt.orderBy().get(1);
        assertThat(second.direction()).isEqualTo(OrderByItem.SortDirection.ASC);
        assertThat(second.expression()).isInstanceOf(Identifier.class);
        Identifier secondColumn = (Identifier) second.expression();
        assertThat(secondColumn.parts()).hasSize(2);
        assertThat(secondColumn.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) secondColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(secondColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) secondColumn.parts().get(1)).name()).isEqualToIgnoringCase("Amount");
    }

    @Test
    void testOrderByWithBlanksFirstIsRejected() {
        // BLANKS FIRST is not part of the DAX ORDER BY grammar
        String dax = "EVALUATE 'Sales' ORDER BY [Amount] BLANKS FIRST";
        assertThatExceptionOfType(DaxParserException.class)
        .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testOrderByWithBlanksLastIsRejected() {
        // BLANKS LAST is not part of the DAX ORDER BY grammar
        String dax = "EVALUATE 'Sales' ORDER BY [Amount] BLANKS LAST";
        assertThatExceptionOfType(DaxParserException.class)
        .isThrownBy(() -> new DaxParserWrapper(dax).parseDaxStatement());
    }

    @Test
    void testOrderByWithUnqualifiedColumnReferences() throws DaxParserException {
        String dax = "EVALUATE 'Sales' ORDER BY [Year] DESC, [Amount] ASC";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Entity.class);
        assertThat(((Entity) evalStmt.tableExpression()).name()).isEqualToIgnoringCase("Sales");

        assertThat(evalStmt.orderBy()).hasSize(2);

        OrderByItem first = evalStmt.orderBy().get(0);
        assertThat(first.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertThat(first.expression()).isInstanceOf(Scalar.class);
        assertThat(((Scalar) first.expression()).name()).isEqualToIgnoringCase("Year");

        OrderByItem second = evalStmt.orderBy().get(1);
        assertThat(second.direction()).isEqualTo(OrderByItem.SortDirection.ASC);
        assertThat(second.expression()).isInstanceOf(Scalar.class);
        assertThat(((Scalar) second.expression()).name()).isEqualToIgnoringCase("Amount");
    }

    @Test
    void testFunctionCallTableExpressionWithOrderByClause() throws DaxParserException {
        String dax = """
                EVALUATE
                SUMMARIZE(
                    'Sales',
                    'Product'[Category],
                    "Total Sales", SUM('Sales'[Amount])
                )
                ORDER BY [Total Sales] DESC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("SUMMARIZE");
        assertThat(fc.arguments()).hasSize(4);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(Identifier.class);
        Identifier groupByColumn = (Identifier) fc.arguments().get(1);
        assertThat(groupByColumn.parts()).hasSize(2);
        assertThat(((Entity) groupByColumn.parts().get(0)).name()).isEqualToIgnoringCase("Product");
        assertThat(((Scalar) groupByColumn.parts().get(1)).name()).isEqualToIgnoringCase("Category");

        assertThat(fc.arguments().get(2)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(2)).value()).isEqualTo("Total Sales");

        assertThat(fc.arguments().get(3)).isInstanceOf(FunctionCall.class);
        FunctionCall sumCall = (FunctionCall) fc.arguments().get(3);
        assertThat(sumCall.functionName()).isEqualToIgnoringCase("SUM");
        assertThat(sumCall.arguments()).hasSize(1);
        Identifier sumColumn = (Identifier) sumCall.arguments().get(0);
        assertThat(((Entity) sumColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(((Scalar) sumColumn.parts().get(1)).name()).isEqualToIgnoringCase("Amount");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertThat(orderByItem.expression()).isInstanceOf(Scalar.class);
        assertThat(((Scalar) orderByItem.expression()).name()).isEqualToIgnoringCase("Total Sales");
    }

    @Test
    void testFunctionCallWithLogicalExpressionArgument() throws DaxParserException {
        String dax = "EVALUATE FILTER('Sales', 'Sales'[Amount] > 100 && 'Sales'[Region] <> \"North\")";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(LogicalExpression.class);
        LogicalExpression condition = (LogicalExpression) fc.arguments().get(1);
        assertThat(condition.operator()).isEqualTo(LogicalExpression.LogicalOperator.AND);

        assertThat(condition.left()).isInstanceOf(BooleanExpression.class);
        BooleanExpression amountFilter = (BooleanExpression) condition.left();
        assertThat(amountFilter.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);
        assertThat(amountFilter.left()).isInstanceOf(Identifier.class);
        Identifier amountColumn = (Identifier) amountFilter.left();
        assertThat(((Entity) amountColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(((Scalar) amountColumn.parts().get(1)).name()).isEqualToIgnoringCase("Amount");
        assertThat(amountFilter.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) amountFilter.right()).value()).isEqualTo(new BigDecimal("100"));

        assertThat(condition.right()).isInstanceOf(BooleanExpression.class);
        BooleanExpression regionFilter = (BooleanExpression) condition.right();
        assertThat(regionFilter.operator()).isEqualTo(BooleanExpression.BooleanOperator.NOT_EQUAL);
        assertThat(regionFilter.left()).isInstanceOf(Identifier.class);
        Identifier regionColumn = (Identifier) regionFilter.left();
        assertThat(((Entity) regionColumn.parts().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(((Scalar) regionColumn.parts().get(1)).name()).isEqualToIgnoringCase("Region");
        assertThat(regionFilter.right()).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) regionFilter.right()).value()).isEqualTo("North");
    }

    private void assertIsColumn(Identifier identifier, String entityName, String columnName) {
        assertThat(identifier.parts()).hasSize(2);
        assertThat(identifier.parts().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) identifier.parts().get(0)).name()).isEqualToIgnoringCase(entityName);
        assertThat(identifier.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) identifier.parts().get(1)).name()).isEqualToIgnoringCase(columnName);
    }

    @Test
    void testDefineClauseWithMeasures() throws DaxParserException {
        String dax = """
                DEFINE
                    MEASURE 'Sales'[Total Amount] = SUM('Sales'[Amount]),
                    MEASURE 'Sales'[Average Amount] = AVERAGE('Sales'[Amount]),
                    MEASURE 'Sales'[Order Count] = COUNTROWS('Sales')
                EVALUATE
                SUMMARIZECOLUMNS(
                    'Product'[Category],
                    "Total", [Total Amount],
                    "Average", [Average Amount],
                    "Orders", [Order Count]
                )
                ORDER BY [Total] DESC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(3);

        assertThat(stmt.defineClauses().get(0)).isInstanceOf(MeasureDefinition.class);
        MeasureDefinition totalAmount = (MeasureDefinition) stmt.defineClauses().get(0);
        assertIsColumn(totalAmount.name(), "Sales", "Total Amount");
        assertThat(totalAmount.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall totalAmountExpr = (FunctionCall) totalAmount.expression();
        assertThat(totalAmountExpr.functionName()).isEqualToIgnoringCase("SUM");
        assertIsColumn((Identifier) totalAmountExpr.arguments().get(0), "Sales", "Amount");

        assertThat(stmt.defineClauses().get(1)).isInstanceOf(MeasureDefinition.class);
        MeasureDefinition averageAmount = (MeasureDefinition) stmt.defineClauses().get(1);
        assertIsColumn(averageAmount.name(), "Sales", "Average Amount");
        assertThat(averageAmount.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall averageAmountExpr = (FunctionCall) averageAmount.expression();
        assertThat(averageAmountExpr.functionName()).isEqualToIgnoringCase("AVERAGE");
        assertIsColumn((Identifier) averageAmountExpr.arguments().get(0), "Sales", "Amount");

        assertThat(stmt.defineClauses().get(2)).isInstanceOf(MeasureDefinition.class);
        MeasureDefinition orderCount = (MeasureDefinition) stmt.defineClauses().get(2);
        assertIsColumn(orderCount.name(), "Sales", "Order Count");
        assertThat(orderCount.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall orderCountExpr = (FunctionCall) orderCount.expression();
        assertThat(orderCountExpr.functionName()).isEqualToIgnoringCase("COUNTROWS");
        assertThat(orderCountExpr.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) orderCountExpr.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("SUMMARIZECOLUMNS");
        assertThat(fc.arguments()).hasSize(7);

        assertIsColumn((Identifier) fc.arguments().get(0), "Product", "Category");

        assertThat(fc.arguments().get(1)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(1)).value()).isEqualTo("Total");
        assertThat(fc.arguments().get(2)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) fc.arguments().get(2)).name()).isEqualToIgnoringCase("Total Amount");

        assertThat(fc.arguments().get(3)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(3)).value()).isEqualTo("Average");
        assertThat(fc.arguments().get(4)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) fc.arguments().get(4)).name()).isEqualToIgnoringCase("Average Amount");

        assertThat(fc.arguments().get(5)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(5)).value()).isEqualTo("Orders");
        assertThat(fc.arguments().get(6)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) fc.arguments().get(6)).name()).isEqualToIgnoringCase("Order Count");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertThat(orderByItem.expression()).isInstanceOf(Scalar.class);
        assertThat(((Scalar) orderByItem.expression()).name()).isEqualToIgnoringCase("Total");
    }

    @Test
    void testDaxStatementWithoutDefineClauseHasNoMeasures() throws DaxParserException {
        String dax = "EVALUATE 'Sales'";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).isEmpty();
    }

    @Test
    void testTableDefinitionInDefineClause() throws DaxParserException {
        String dax = """
                DEFINE
                    TABLE TopProducts =
                        TOPN(
                            10,
                            'Product',
                            CALCULATE(SUM('Sales'[Amount])),
                            DESC
                        )
                EVALUATE
                TopProducts
                ORDER BY 'Product'[ProductName] ASC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(1);
        assertThat(stmt.defineClauses().get(0)).isInstanceOf(TableDefinition.class);
        TableDefinition topProducts = (TableDefinition) stmt.defineClauses().get(0);
        assertThat(topProducts.name()).isEqualToIgnoringCase("TopProducts");

        assertThat(topProducts.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall topn = (FunctionCall) topProducts.expression();
        assertThat(topn.functionName()).isEqualToIgnoringCase("TOPN");
        assertThat(topn.arguments()).hasSize(4);

        assertThat(topn.arguments().get(0)).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) topn.arguments().get(0)).value()).isEqualTo(new BigDecimal("10"));

        assertThat(topn.arguments().get(1)).isInstanceOf(Entity.class);
        assertThat(((Entity) topn.arguments().get(1)).name()).isEqualToIgnoringCase("Product");

        assertThat(topn.arguments().get(2)).isInstanceOf(FunctionCall.class);
        FunctionCall calculate = (FunctionCall) topn.arguments().get(2);
        assertThat(calculate.functionName()).isEqualToIgnoringCase("CALCULATE");
        assertThat(calculate.arguments()).hasSize(1);
        assertThat(calculate.arguments().get(0)).isInstanceOf(FunctionCall.class);
        FunctionCall sum = (FunctionCall) calculate.arguments().get(0);
        assertThat(sum.functionName()).isEqualToIgnoringCase("SUM");
        assertIsColumn((Identifier) sum.arguments().get(0), "Sales", "Amount");

        assertThat(topn.arguments().get(3)).isInstanceOf(Keyword.class);
        assertThat(((Keyword) topn.arguments().get(3)).name()).isEqualToIgnoringCase("DESC");

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Keyword.class);
        assertThat(((Keyword) evalStmt.tableExpression()).name()).isEqualToIgnoringCase("TopProducts");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.ASC);
        assertIsColumn((Identifier) orderByItem.expression(), "Product", "ProductName");
    }

    @Test
    void testColumnDefinitionInDefineClause() throws DaxParserException {
        String dax = """
                DEFINE
                    COLUMN 'Sales'[Amount Category] =
                        IF('Sales'[Amount] > 1000, "High", "Low")
                EVALUATE
                SUMMARIZE(
                    'Sales',
                    'Sales'[Amount Category],
                    "Total", SUM('Sales'[Amount])
                )""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(1);
        assertThat(stmt.defineClauses().get(0)).isInstanceOf(ColumnDefinition.class);
        ColumnDefinition amountCategory = (ColumnDefinition) stmt.defineClauses().get(0);
        assertIsColumn(amountCategory.name(), "Sales", "Amount Category");

        assertThat(amountCategory.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall ifCall = (FunctionCall) amountCategory.expression();
        assertThat(ifCall.functionName()).isEqualToIgnoringCase("IF");
        assertThat(ifCall.arguments()).hasSize(3);

        assertThat(ifCall.arguments().get(0)).isInstanceOf(BooleanExpression.class);
        BooleanExpression condition = (BooleanExpression) ifCall.arguments().get(0);
        assertThat(condition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);
        assertIsColumn((Identifier) condition.left(), "Sales", "Amount");
        assertThat(condition.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) condition.right()).value()).isEqualTo(new BigDecimal("1000"));

        assertThat(ifCall.arguments().get(1)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) ifCall.arguments().get(1)).value()).isEqualTo("High");
        assertThat(ifCall.arguments().get(2)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) ifCall.arguments().get(2)).value()).isEqualTo("Low");

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("SUMMARIZE");
        assertThat(fc.arguments()).hasSize(4);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(Identifier.class);
        assertIsColumn((Identifier) fc.arguments().get(1), "Sales", "Amount Category");

        assertThat(fc.arguments().get(2)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(2)).value()).isEqualTo("Total");

        assertThat(fc.arguments().get(3)).isInstanceOf(FunctionCall.class);
        FunctionCall sumCall = (FunctionCall) fc.arguments().get(3);
        assertThat(sumCall.functionName()).isEqualToIgnoringCase("SUM");
        assertIsColumn((Identifier) sumCall.arguments().get(0), "Sales", "Amount");
    }

    @Test
    void testDefineClauseWithAllDefinitionKinds() throws DaxParserException {
        String dax = """
                DEFINE
                    MEASURE 'Sales'[Total Amount] = SUM('Sales'[Amount]),
                    TABLE HighValueSales =
                        FILTER('Sales', 'Sales'[Amount] > 1000),
                    COLUMN 'Sales'[IsHigh] = 'Sales'[Amount] > 1000
                EVALUATE
                SUMMARIZECOLUMNS(
                    'Product'[Category],
                    "Total High Value", CALCULATE([Total Amount], HighValueSales)
                )""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(3);

        assertThat(stmt.defineClauses().get(0)).isInstanceOf(MeasureDefinition.class);
        MeasureDefinition totalAmount = (MeasureDefinition) stmt.defineClauses().get(0);
        assertIsColumn(totalAmount.name(), "Sales", "Total Amount");
        assertThat(totalAmount.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall sumCall = (FunctionCall) totalAmount.expression();
        assertThat(sumCall.functionName()).isEqualToIgnoringCase("SUM");
        assertIsColumn((Identifier) sumCall.arguments().get(0), "Sales", "Amount");

        assertThat(stmt.defineClauses().get(1)).isInstanceOf(TableDefinition.class);
        TableDefinition highValueSales = (TableDefinition) stmt.defineClauses().get(1);
        assertThat(highValueSales.name()).isEqualToIgnoringCase("HighValueSales");
        assertThat(highValueSales.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall filterCall = (FunctionCall) highValueSales.expression();
        assertThat(filterCall.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(filterCall.arguments()).hasSize(2);
        assertThat(filterCall.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) filterCall.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(filterCall.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression filterCondition = (BooleanExpression) filterCall.arguments().get(1);
        assertThat(filterCondition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);
        assertIsColumn((Identifier) filterCondition.left(), "Sales", "Amount");
        assertThat(((NumericLiteral) filterCondition.right()).value()).isEqualTo(new BigDecimal("1000"));

        assertThat(stmt.defineClauses().get(2)).isInstanceOf(ColumnDefinition.class);
        ColumnDefinition isHigh = (ColumnDefinition) stmt.defineClauses().get(2);
        assertIsColumn(isHigh.name(), "Sales", "IsHigh");
        assertThat(isHigh.expression()).isInstanceOf(BooleanExpression.class);
        BooleanExpression isHighCondition = (BooleanExpression) isHigh.expression();
        assertThat(isHighCondition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);
        assertIsColumn((Identifier) isHighCondition.left(), "Sales", "Amount");
        assertThat(((NumericLiteral) isHighCondition.right()).value()).isEqualTo(new BigDecimal("1000"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("SUMMARIZECOLUMNS");
        assertThat(fc.arguments()).hasSize(3);

        assertIsColumn((Identifier) fc.arguments().get(0), "Product", "Category");

        assertThat(fc.arguments().get(1)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) fc.arguments().get(1)).value()).isEqualTo("Total High Value");

        assertThat(fc.arguments().get(2)).isInstanceOf(FunctionCall.class);
        FunctionCall calculateCall = (FunctionCall) fc.arguments().get(2);
        assertThat(calculateCall.functionName()).isEqualToIgnoringCase("CALCULATE");
        assertThat(calculateCall.arguments()).hasSize(2);
        assertThat(calculateCall.arguments().get(0)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) calculateCall.arguments().get(0)).name()).isEqualToIgnoringCase("Total Amount");
        assertThat(calculateCall.arguments().get(1)).isInstanceOf(Keyword.class);
        assertThat(((Keyword) calculateCall.arguments().get(1)).name()).isEqualToIgnoringCase("HighValueSales");
    }

    @Test
    void testBooleanExpressionWithInOperator() throws DaxParserException {
        String dax = "EVALUATE FILTER('Markets', 'Markets'[Territory] IN {\"EMEA\", \"APAC\"})";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Markets");

        assertThat(fc.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression membership = (BooleanExpression) fc.arguments().get(1);
        assertThat(membership.operator()).isEqualTo(BooleanExpression.BooleanOperator.IN);
        assertIsColumn((Identifier) membership.left(), "Markets", "Territory");

        assertThat(membership.right()).isInstanceOf(TableConstructor.class);
        TableConstructor territories = (TableConstructor) membership.right();
        assertThat(territories.rows()).hasSize(2);
        assertThat(territories.rows().get(0).columns()).hasSize(1);
        assertThat(territories.rows().get(0).columns().get(0)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) territories.rows().get(0).columns().get(0)).value()).isEqualTo("EMEA");
        assertThat(territories.rows().get(1).columns().get(0)).isInstanceOf(StringLiteral.class);
        assertThat(((StringLiteral) territories.rows().get(1).columns().get(0)).value()).isEqualTo("APAC");
    }

    @Test
    void testVariableDefinitionInDefineClause() throws DaxParserException {
        String dax = """
                DEFINE
                    VAR __minAmount = 1000
                EVALUATE
                FILTER(
                    'Sales',
                    'Sales'[Amount] >= __minAmount
                )
                ORDER BY 'Sales'[Amount] DESC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(1);
        assertThat(stmt.defineClauses().get(0)).isInstanceOf(VariableDefinition.class);
        VariableDefinition minAmount = (VariableDefinition) stmt.defineClauses().get(0);
        assertThat(minAmount.name()).isEqualTo("__minAmount");
        assertThat(minAmount.expression()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) minAmount.expression()).value()).isEqualTo(new BigDecimal("1000"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression condition = (BooleanExpression) fc.arguments().get(1);
        assertThat(condition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN_OR_EQUAL);
        assertIsColumn((Identifier) condition.left(), "Sales", "Amount");
        assertThat(condition.right()).isInstanceOf(Keyword.class);
        assertThat(((Keyword) condition.right()).name()).isEqualTo("__minAmount");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertIsColumn((Identifier) orderByItem.expression(), "Sales", "Amount");
    }

    @Test
    void testVariableDefinitionReferencingAnotherVariable() throws DaxParserException {
        String dax = """
                DEFINE
                    VAR __baseAmount = 1000,
                    VAR __threshold = __baseAmount * 0.9
                EVALUATE
                FILTER(
                    'Sales',
                    'Sales'[Amount] >= __threshold
                )""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(2);

        assertThat(stmt.defineClauses().get(0)).isInstanceOf(VariableDefinition.class);
        VariableDefinition baseAmount = (VariableDefinition) stmt.defineClauses().get(0);
        assertThat(baseAmount.name()).isEqualTo("__baseAmount");
        assertThat(baseAmount.expression()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) baseAmount.expression()).value()).isEqualTo(new BigDecimal("1000"));

        assertThat(stmt.defineClauses().get(1)).isInstanceOf(VariableDefinition.class);
        VariableDefinition threshold = (VariableDefinition) stmt.defineClauses().get(1);
        assertThat(threshold.name()).isEqualTo("__threshold");
        assertThat(threshold.expression()).isInstanceOf(ArithmeticExpression.class);
        ArithmeticExpression thresholdExpr = (ArithmeticExpression) threshold.expression();
        assertThat(thresholdExpr.operator()).isEqualTo(ArithmeticExpression.ArithmeticOperator.MULTIPLY);
        assertThat(thresholdExpr.left()).isInstanceOf(Keyword.class);
        assertThat(((Keyword) thresholdExpr.left()).name()).isEqualTo("__baseAmount");
        assertThat(thresholdExpr.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) thresholdExpr.right()).value()).isEqualTo(new BigDecimal("0.9"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression condition = (BooleanExpression) fc.arguments().get(1);
        assertThat(condition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN_OR_EQUAL);
        assertIsColumn((Identifier) condition.left(), "Sales", "Amount");
        assertThat(condition.right()).isInstanceOf(Keyword.class);
        assertThat(((Keyword) condition.right()).name()).isEqualTo("__threshold");
    }

    @Test
    void testParameterDefinitionInDefineClause() throws DaxParserException {
        String dax = """
                DEFINE
                    @MinAmount = 1000
                EVALUATE
                FILTER(
                    'Sales',
                    'Sales'[Amount] >= @MinAmount
                )
                ORDER BY 'Sales'[Amount] DESC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(1);
        assertThat(stmt.defineClauses().get(0)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition minAmount = (ParameterDefinition) stmt.defineClauses().get(0);
        assertThat(minAmount.name()).isEqualTo("MinAmount");
        assertThat(minAmount.expression()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) minAmount.expression()).value()).isEqualTo(new BigDecimal("1000"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(FunctionCall.class);
        FunctionCall fc = (FunctionCall) evalStmt.tableExpression();
        assertThat(fc.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(fc.arguments()).hasSize(2);

        assertThat(fc.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) fc.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");

        assertThat(fc.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression condition = (BooleanExpression) fc.arguments().get(1);
        assertThat(condition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN_OR_EQUAL);
        assertIsColumn((Identifier) condition.left(), "Sales", "Amount");
        assertThat(condition.right()).isInstanceOf(Parameter.class);
        assertThat(((Parameter) condition.right()).name()).isEqualTo("MinAmount");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertIsColumn((Identifier) orderByItem.expression(), "Sales", "Amount");
    }

    @Test
    void testParameterAsEvaluateTableExpression() throws DaxParserException {
        String dax = """
                DEFINE
                    @HighValueSales =
                        FILTER('Sales', 'Sales'[Amount] > 1000)
                EVALUATE
                @HighValueSales
                ORDER BY 'Sales'[Amount] DESC""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(1);
        assertThat(stmt.defineClauses().get(0)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition highValueSales = (ParameterDefinition) stmt.defineClauses().get(0);
        assertThat(highValueSales.name()).isEqualTo("HighValueSales");

        assertThat(highValueSales.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall filterCall = (FunctionCall) highValueSales.expression();
        assertThat(filterCall.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(filterCall.arguments()).hasSize(2);
        assertThat(filterCall.arguments().get(0)).isInstanceOf(Entity.class);
        assertThat(((Entity) filterCall.arguments().get(0)).name()).isEqualToIgnoringCase("Sales");
        assertThat(filterCall.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression filterCondition = (BooleanExpression) filterCall.arguments().get(1);
        assertThat(filterCondition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);
        assertIsColumn((Identifier) filterCondition.left(), "Sales", "Amount");
        assertThat(((NumericLiteral) filterCondition.right()).value()).isEqualTo(new BigDecimal("1000"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Parameter.class);
        assertThat(((Parameter) evalStmt.tableExpression()).name()).isEqualTo("HighValueSales");

        assertThat(evalStmt.orderBy()).hasSize(1);
        OrderByItem orderByItem = evalStmt.orderBy().get(0);
        assertThat(orderByItem.direction()).isEqualTo(OrderByItem.SortDirection.DESC);
        assertIsColumn((Identifier) orderByItem.expression(), "Sales", "Amount");
    }

    @Test
    void testParameterDefinitionReferencingAnotherParameter() throws DaxParserException {
        String dax = """
                DEFINE
                    @TopN = 5,
                    @TopProducts =
                        TOPN(
                            @TopN,
                            'Product',
                            CALCULATE(SUM('Sales'[Amount])),
                            DESC
                        )
                EVALUATE
                @TopProducts""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(2);

        assertThat(stmt.defineClauses().get(0)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition topN = (ParameterDefinition) stmt.defineClauses().get(0);
        assertThat(topN.name()).isEqualTo("TopN");
        assertThat(topN.expression()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) topN.expression()).value()).isEqualTo(new BigDecimal("5"));

        assertThat(stmt.defineClauses().get(1)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition topProducts = (ParameterDefinition) stmt.defineClauses().get(1);
        assertThat(topProducts.name()).isEqualTo("TopProducts");

        assertThat(topProducts.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall topnCall = (FunctionCall) topProducts.expression();
        assertThat(topnCall.functionName()).isEqualToIgnoringCase("TOPN");
        assertThat(topnCall.arguments()).hasSize(4);

        assertThat(topnCall.arguments().get(0)).isInstanceOf(Parameter.class);
        assertThat(((Parameter) topnCall.arguments().get(0)).name()).isEqualTo("TopN");

        assertThat(topnCall.arguments().get(1)).isInstanceOf(Entity.class);
        assertThat(((Entity) topnCall.arguments().get(1)).name()).isEqualToIgnoringCase("Product");

        assertThat(topnCall.arguments().get(2)).isInstanceOf(FunctionCall.class);
        FunctionCall calculateCall = (FunctionCall) topnCall.arguments().get(2);
        assertThat(calculateCall.functionName()).isEqualToIgnoringCase("CALCULATE");
        assertThat(calculateCall.arguments()).hasSize(1);
        assertThat(calculateCall.arguments().get(0)).isInstanceOf(FunctionCall.class);
        FunctionCall sumCall = (FunctionCall) calculateCall.arguments().get(0);
        assertThat(sumCall.functionName()).isEqualToIgnoringCase("SUM");
        assertIsColumn((Identifier) sumCall.arguments().get(0), "Sales", "Amount");

        assertThat(topnCall.arguments().get(3)).isInstanceOf(Keyword.class);
        assertThat(((Keyword) topnCall.arguments().get(3)).name()).isEqualToIgnoringCase("DESC");

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Parameter.class);
        assertThat(((Parameter) evalStmt.tableExpression()).name()).isEqualTo("TopProducts");
    }

    @Test
    void testParameterWithColumnReference() throws DaxParserException {
        String dax = """
                DEFINE
                    @AllSales = 'Sales',
                    @FilteredSales =
                        FILTER(@AllSales, @AllSales[Amount] > 1000)
                EVALUATE
                @FilteredSales""";
        DaxStatement stmt = new DaxParserWrapper(dax).parseDaxStatement();

        assertThat(stmt.defineClauses()).hasSize(2);

        assertThat(stmt.defineClauses().get(0)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition allSales = (ParameterDefinition) stmt.defineClauses().get(0);
        assertThat(allSales.name()).isEqualTo("AllSales");
        assertThat(allSales.expression()).isInstanceOf(Entity.class);
        assertThat(((Entity) allSales.expression()).name()).isEqualToIgnoringCase("Sales");

        assertThat(stmt.defineClauses().get(1)).isInstanceOf(ParameterDefinition.class);
        ParameterDefinition filteredSales = (ParameterDefinition) stmt.defineClauses().get(1);
        assertThat(filteredSales.name()).isEqualTo("FilteredSales");

        assertThat(filteredSales.expression()).isInstanceOf(FunctionCall.class);
        FunctionCall filterCall = (FunctionCall) filteredSales.expression();
        assertThat(filterCall.functionName()).isEqualToIgnoringCase("FILTER");
        assertThat(filterCall.arguments()).hasSize(2);

        assertThat(filterCall.arguments().get(0)).isInstanceOf(Parameter.class);
        assertThat(((Parameter) filterCall.arguments().get(0)).name()).isEqualTo("AllSales");

        assertThat(filterCall.arguments().get(1)).isInstanceOf(BooleanExpression.class);
        BooleanExpression condition = (BooleanExpression) filterCall.arguments().get(1);
        assertThat(condition.operator()).isEqualTo(BooleanExpression.BooleanOperator.GREATER_THAN);

        assertThat(condition.left()).isInstanceOf(Identifier.class);
        Identifier amountColumn = (Identifier) condition.left();
        assertThat(amountColumn.parts()).hasSize(2);
        assertThat(amountColumn.parts().get(0)).isInstanceOf(Parameter.class);
        assertThat(((Parameter) amountColumn.parts().get(0)).name()).isEqualTo("AllSales");
        assertThat(amountColumn.parts().get(1)).isInstanceOf(Scalar.class);
        assertThat(((Scalar) amountColumn.parts().get(1)).name()).isEqualToIgnoringCase("Amount");

        assertThat(condition.right()).isInstanceOf(NumericLiteral.class);
        assertThat(((NumericLiteral) condition.right()).value()).isEqualTo(new BigDecimal("1000"));

        EvaluateStatement evalStmt = stmt.evaluateStatements().get(0);
        assertThat(evalStmt.tableExpression()).isInstanceOf(Parameter.class);
        assertThat(((Parameter) evalStmt.tableExpression()).name()).isEqualTo("FilteredSales");
    }

}
