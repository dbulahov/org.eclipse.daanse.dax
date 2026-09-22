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
import org.eclipse.daanse.dax.model.api.expression.StringExpression;
import org.eclipse.daanse.dax.model.api.expression.StringExpression.StringOperator;
import org.junit.jupiter.api.Test;

class StringExpressionRTest {

    @Test
    void isAStringExpressionAndTransitivelyADaxExpression() {
        StringExpressionR expr = new StringExpressionR(new StringLiteralR("a"), StringOperator.AND,
                new StringLiteralR("b"));

        assertThat(expr).isInstanceOf(StringExpression.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenOperandsAndOperator() {
        DaxExpression left = new StringLiteralR("a");
        DaxExpression right = new StringLiteralR("b");

        StringExpressionR expr = new StringExpressionR(left, StringOperator.AND, right);

        assertThat(expr.left()).isSameAs(left);
        assertThat(expr.operator()).isEqualTo(StringOperator.AND);
        assertThat(expr.right()).isSameAs(right);
    }

    @Test
    void rejectsANullLeftOperand() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StringExpressionR(null, StringOperator.AND, new StringLiteralR("b")));
    }

    @Test
    void rejectsANullOperator() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StringExpressionR(new StringLiteralR("a"), null, new StringLiteralR("b")));
    }

    @Test
    void rejectsANullRightOperand() {
        assertThatNullPointerException()
                .isThrownBy(() -> new StringExpressionR(new StringLiteralR("a"), StringOperator.AND, null));
    }

    @Test
    void expressionsWithEqualComponentsAreEqualAndHaveTheSameHashCode() {
        StringExpressionR a = new StringExpressionR(new StringLiteralR("a"), StringOperator.AND,
                new StringLiteralR("b"));
        StringExpressionR b = new StringExpressionR(new StringLiteralR("a"), StringOperator.AND,
                new StringLiteralR("b"));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void expressionsWithDifferentOperandsAreNotEqual() {
        StringExpressionR a = new StringExpressionR(new StringLiteralR("a"), StringOperator.AND,
                new StringLiteralR("b"));
        StringExpressionR b = new StringExpressionR(new StringLiteralR("a"), StringOperator.AND,
                new StringLiteralR("c"));

        assertThat(a).isNotEqualTo(b);
    }
}
