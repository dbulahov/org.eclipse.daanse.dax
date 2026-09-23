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
import org.eclipse.daanse.dax.model.api.expression.Parameter;
import org.junit.jupiter.api.Test;

class ParameterRTest {

    @Test
    void isAParameterAndTransitivelyADaxExpression() {
        ParameterR parameter = new ParameterR("MinAmount");

        assertThat(parameter).isInstanceOf(Parameter.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        ParameterR parameter = new ParameterR("MinAmount");

        assertThat(parameter.name()).isEqualTo("MinAmount");
    }

    @Test
    void allowsAnEmptyName() {
        ParameterR parameter = new ParameterR("");

        assertThat(parameter.name()).isEmpty();
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new ParameterR(null));
    }

    @Test
    void parametersWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        ParameterR a = new ParameterR("MinAmount");
        ParameterR b = new ParameterR("MinAmount");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void parametersWithDifferentNamesAreNotEqual() {
        ParameterR a = new ParameterR("MinAmount");
        ParameterR b = new ParameterR("MaxAmount");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringContainsTheName() {
        ParameterR parameter = new ParameterR("MinAmount");

        assertThat(parameter.toString()).contains("MinAmount");
    }
}
