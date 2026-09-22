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

import java.time.LocalDateTime;

import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.junit.jupiter.api.Test;

class DateTimeLiteralRTest {

    @Test
    void isADateTimeLiteralAndTransitivelyALiteralAndADaxExpression() {
        DateTimeLiteralR literal = new DateTimeLiteralR(LocalDateTime.of(2024, 1, 31, 10, 30, 0));

        assertThat(literal).isInstanceOf(DateTimeLiteral.class).isInstanceOf(Literal.class)
                .isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenValueUnchanged() {
        LocalDateTime value = LocalDateTime.of(2024, 1, 31, 10, 30, 0);

        DateTimeLiteralR literal = new DateTimeLiteralR(value);

        assertThat(literal.value()).isEqualTo(value);
    }

    @Test
    void rejectsANullValue() {
        assertThatNullPointerException().isThrownBy(() -> new DateTimeLiteralR(null));
    }

    @Test
    void literalsWithEqualValuesAreEqualAndHaveTheSameHashCode() {
        DateTimeLiteralR a = new DateTimeLiteralR(LocalDateTime.of(2024, 1, 31, 10, 30, 0));
        DateTimeLiteralR b = new DateTimeLiteralR(LocalDateTime.of(2024, 1, 31, 10, 30, 0));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void literalsWithDifferentValuesAreNotEqual() {
        DateTimeLiteralR a = new DateTimeLiteralR(LocalDateTime.of(2024, 1, 31, 10, 30, 0));
        DateTimeLiteralR b = new DateTimeLiteralR(LocalDateTime.of(2024, 2, 1, 0, 0, 0));

        assertThat(a).isNotEqualTo(b);
    }
}
