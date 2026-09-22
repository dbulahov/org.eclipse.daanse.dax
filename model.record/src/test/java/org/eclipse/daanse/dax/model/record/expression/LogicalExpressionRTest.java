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
package org.eclipse.daanse.dax.model.record.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator;
import org.junit.jupiter.api.Test;

class LogicalExpressionRTest {

    @Test
    void isALogicalExpressionAndTransitivelyADaxExpression() {
        LogicalExpressionR expr = new LogicalExpressionR(new BooleanLiteralR(true), LogicalOperator.AND,
                new BooleanLiteralR(false));

        assertThat(expr).isInstanceOf(LogicalExpression.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenOperandsAndOperator() {
        DaxExpression left = new BooleanLiteralR(true);
        DaxExpression right = new BooleanLiteralR(false);

        LogicalExpressionR expr = new LogicalExpressionR(left, LogicalOperator.AND, right);

        assertThat(expr.left()).isSameAs(left);
        assertThat(expr.operator()).isEqualTo(LogicalOperator.AND);
        assertThat(expr.right()).isSameAs(right);
    }

    @Test
    void rejectsANullLeftOperand() {
        assertThatNullPointerException()
                .isThrownBy(() -> new LogicalExpressionR(null, LogicalOperator.AND, new BooleanLiteralR(true)));
    }

    @Test
    void rejectsANullOperator() {
        assertThatNullPointerException().isThrownBy(
                () -> new LogicalExpressionR(new BooleanLiteralR(true), null, new BooleanLiteralR(true)));
    }

    @Test
    void rejectsANullRightOperand() {
        assertThatNullPointerException()
                .isThrownBy(() -> new LogicalExpressionR(new BooleanLiteralR(true), LogicalOperator.AND, null));
    }

    @Test
    void expressionsWithEqualComponentsAreEqualAndHaveTheSameHashCode() {
        LogicalExpressionR a = new LogicalExpressionR(new BooleanLiteralR(true), LogicalOperator.OR,
                new BooleanLiteralR(false));
        LogicalExpressionR b = new LogicalExpressionR(new BooleanLiteralR(true), LogicalOperator.OR,
                new BooleanLiteralR(false));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void expressionsWithDifferentOperatorsAreNotEqual() {
        DaxExpression left = new BooleanLiteralR(true);
        DaxExpression right = new BooleanLiteralR(false);

        LogicalExpressionR a = new LogicalExpressionR(left, LogicalOperator.AND, right);
        LogicalExpressionR b = new LogicalExpressionR(left, LogicalOperator.OR, right);

        assertThat(a).isNotEqualTo(b);
    }
}
