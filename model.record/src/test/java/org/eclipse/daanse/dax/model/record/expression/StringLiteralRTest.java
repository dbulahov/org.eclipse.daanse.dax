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
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.eclipse.daanse.dax.model.api.expression.StringLiteral;
import org.junit.jupiter.api.Test;

class StringLiteralRTest {

    @Test
    void isAStringLiteralAndTransitivelyALiteralAndADaxExpression() {
        StringLiteralR literal = new StringLiteralR("Hello World");

        assertThat(literal).isInstanceOf(StringLiteral.class).isInstanceOf(Literal.class)
                .isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenValueUnchanged() {
        StringLiteralR literal = new StringLiteralR("Hello World");

        assertThat(literal.value()).isEqualTo("Hello World");
    }

    @Test
    void allowsAnEmptyValue() {
        StringLiteralR literal = new StringLiteralR("");

        assertThat(literal.value()).isEmpty();
    }

    @Test
    void rejectsANullValue() {
        assertThatNullPointerException().isThrownBy(() -> new StringLiteralR(null));
    }

    @Test
    void literalsWithEqualValuesAreEqualAndHaveTheSameHashCode() {
        StringLiteralR a = new StringLiteralR("Hello World");
        StringLiteralR b = new StringLiteralR("Hello World");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void literalsWithDifferentValuesAreNotEqual() {
        StringLiteralR a = new StringLiteralR("Hello World");
        StringLiteralR b = new StringLiteralR("Goodbye World");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringContainsTheValue() {
        StringLiteralR literal = new StringLiteralR("Hello World");

        assertThat(literal.toString()).contains("Hello World");
    }
}
