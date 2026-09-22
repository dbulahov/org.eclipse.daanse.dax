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

import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression;
import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.junit.jupiter.api.Test;

class ArithmeticExpressionRTest {

    @Test
    void isAnArithmeticExpressionAndTransitivelyADaxExpression() {
        ArithmeticExpressionR expr = new ArithmeticExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(6)),
                ArithmeticOperator.ADD, new NumericLiteralR(java.math.BigDecimal.valueOf(5)));

        assertThat(expr).isInstanceOf(ArithmeticExpression.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenOperandsAndOperator() {
        DaxExpression left = new NumericLiteralR(java.math.BigDecimal.valueOf(6));
        DaxExpression right = new NumericLiteralR(java.math.BigDecimal.valueOf(5));

        ArithmeticExpressionR expr = new ArithmeticExpressionR(left, ArithmeticOperator.ADD, right);

        assertThat(expr.left()).isSameAs(left);
        assertThat(expr.operator()).isEqualTo(ArithmeticOperator.ADD);
        assertThat(expr.right()).isSameAs(right);
    }

    @Test
    void rejectsANullLeftOperand() {
        assertThatNullPointerException().isThrownBy(() -> new ArithmeticExpressionR(null, ArithmeticOperator.ADD,
                new NumericLiteralR(java.math.BigDecimal.ONE)));
    }

    @Test
    void rejectsANullOperator() {
        assertThatNullPointerException().isThrownBy(() -> new ArithmeticExpressionR(
                new NumericLiteralR(java.math.BigDecimal.ONE), null, new NumericLiteralR(java.math.BigDecimal.ONE)));
    }

    @Test
    void rejectsANullRightOperand() {
        assertThatNullPointerException().isThrownBy(() -> new ArithmeticExpressionR(
                new NumericLiteralR(java.math.BigDecimal.ONE), ArithmeticOperator.ADD, null));
    }

    @Test
    void expressionsWithEqualComponentsAreEqualAndHaveTheSameHashCode() {
        ArithmeticExpressionR a = new ArithmeticExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(3)),
                ArithmeticOperator.POWER, new NumericLiteralR(java.math.BigDecimal.valueOf(2)));
        ArithmeticExpressionR b = new ArithmeticExpressionR(new NumericLiteralR(java.math.BigDecimal.valueOf(3)),
                ArithmeticOperator.POWER, new NumericLiteralR(java.math.BigDecimal.valueOf(2)));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void expressionsWithDifferentOperatorsAreNotEqual() {
        DaxExpression left = new NumericLiteralR(java.math.BigDecimal.valueOf(6));
        DaxExpression right = new NumericLiteralR(java.math.BigDecimal.valueOf(2));

        ArithmeticExpressionR a = new ArithmeticExpressionR(left, ArithmeticOperator.MULTIPLY, right);
        ArithmeticExpressionR b = new ArithmeticExpressionR(left, ArithmeticOperator.DIVIDE, right);

        assertThat(a).isNotEqualTo(b);
    }
}
