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

import java.math.BigDecimal;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.junit.jupiter.api.Test;

class NumericLiteralRTest {

    @Test
    void isANumericLiteralAndTransitivelyALiteralAndADaxExpression() {
        NumericLiteralR literal = new NumericLiteralR(new BigDecimal("42"));

        assertThat(literal).isInstanceOf(NumericLiteral.class).isInstanceOf(Literal.class)
                .isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenValueUnchanged() {
        NumericLiteralR literal = new NumericLiteralR(new BigDecimal("3.14"));

        assertThat(literal.value()).isEqualTo(new BigDecimal("3.14"));
    }

    @Test
    void preservesTheSourcePrecisionInsteadOfNormalizingItAway() {
        // "3.10" and "3.1" are numerically equal but not the same BigDecimal
        // scale; the record must keep exactly what was given.
        NumericLiteralR literal = new NumericLiteralR(new BigDecimal("3.10"));

        assertThat(literal.value().toPlainString()).isEqualTo("3.10");
    }

    @Test
    void rejectsANullValue() {
        assertThatNullPointerException().isThrownBy(() -> new NumericLiteralR(null));
    }

    @Test
    void literalsWithEqualScaledValuesAreEqualAndHaveTheSameHashCode() {
        NumericLiteralR a = new NumericLiteralR(new BigDecimal("42"));
        NumericLiteralR b = new NumericLiteralR(new BigDecimal("42"));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void literalsWithDifferentValuesAreNotEqual() {
        NumericLiteralR a = new NumericLiteralR(new BigDecimal("42"));
        NumericLiteralR b = new NumericLiteralR(new BigDecimal("43"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void literalsWithTheSameNumericValueButDifferentScaleAreNotEqual() {
        // record equality delegates to BigDecimal.equals(), which is
        // scale-sensitive (unlike compareTo())
        NumericLiteralR a = new NumericLiteralR(new BigDecimal("3.1"));
        NumericLiteralR b = new NumericLiteralR(new BigDecimal("3.10"));

        assertThat(a).isNotEqualTo(b);
    }
}
