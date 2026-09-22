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

import org.eclipse.daanse.dax.model.api.expression.BooleanExpression;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.junit.jupiter.api.Test;

class BooleanExpressionRTest {

    @Test
    void isABooleanExpressionAndTransitivelyADaxExpression() {
        BooleanExpressionR expr = new BooleanExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(5)),
                BooleanOperator.LESS_THAN, new NumericLiteralR(java.math.BigDecimal.valueOf(6)));

        assertThat(expr).isInstanceOf(BooleanExpression.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenOperandsAndOperator() {
        DaxExpression left = new NumericLiteralR(java.math.BigDecimal.valueOf(5));
        DaxExpression right = new NumericLiteralR(java.math.BigDecimal.valueOf(6));

        BooleanExpressionR expr = new BooleanExpressionR(left, BooleanOperator.LESS_THAN, right);

        assertThat(expr.left()).isSameAs(left);
        assertThat(expr.operator()).isEqualTo(BooleanOperator.LESS_THAN);
        assertThat(expr.right()).isSameAs(right);
    }

    @Test
    void rejectsANullLeftOperand() {
        assertThatNullPointerException().isThrownBy(() -> new BooleanExpressionR(null, BooleanOperator.EQUAL,
                new NumericLiteralR(java.math.BigDecimal.ONE)));
    }

    @Test
    void rejectsANullOperator() {
        assertThatNullPointerException().isThrownBy(() -> new BooleanExpressionR(
                new NumericLiteralR(java.math.BigDecimal.ONE), null, new NumericLiteralR(java.math.BigDecimal.ONE)));
    }

    @Test
    void rejectsANullRightOperand() {
        assertThatNullPointerException().isThrownBy(() -> new BooleanExpressionR(
                new NumericLiteralR(java.math.BigDecimal.ONE), BooleanOperator.EQUAL, null));
    }

    @Test
    void expressionsWithEqualComponentsAreEqualAndHaveTheSameHashCode() {
        BooleanExpressionR a = new BooleanExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(6)),
                BooleanOperator.EQUAL, new NumericLiteralR(java.math.BigDecimal.valueOf(6)));
        BooleanExpressionR b = new BooleanExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(6)),
                BooleanOperator.EQUAL, new NumericLiteralR(java.math.BigDecimal.valueOf(6)));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void expressionsWithDifferentOperatorsAreNotEqual() {
        DaxExpression left = new NumericLiteralR(java.math.BigDecimal.valueOf(6));
        DaxExpression right = new NumericLiteralR(java.math.BigDecimal.valueOf(6));

        BooleanExpressionR a = new BooleanExpressionR(left, BooleanOperator.GREATER_THAN_OR_EQUAL, right);
        BooleanExpressionR b = new BooleanExpressionR(left, BooleanOperator.LESS_THAN_OR_EQUAL, right);

        assertThat(a).isNotEqualTo(b);
    }
}
