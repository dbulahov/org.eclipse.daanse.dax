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

import org.eclipse.daanse.dax.model.api.expression.BooleanLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.junit.jupiter.api.Test;

class BooleanLiteralRTest {

    @Test
    void isABooleanLiteralAndTransitivelyALiteralAndADaxExpression() {
        BooleanLiteralR literal = new BooleanLiteralR(true);

        assertThat(literal).isInstanceOf(BooleanLiteral.class).isInstanceOf(Literal.class)
                .isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenTrueValue() {
        assertThat(new BooleanLiteralR(true).value()).isTrue();
    }

    @Test
    void exposesTheGivenFalseValue() {
        assertThat(new BooleanLiteralR(false).value()).isFalse();
    }

    @Test
    void literalsWithEqualValuesAreEqualAndHaveTheSameHashCode() {
        BooleanLiteralR a = new BooleanLiteralR(true);
        BooleanLiteralR b = new BooleanLiteralR(true);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void literalsWithDifferentValuesAreNotEqual() {
        BooleanLiteralR a = new BooleanLiteralR(true);
        BooleanLiteralR b = new BooleanLiteralR(false);

        assertThat(a).isNotEqualTo(b);
    }
}
