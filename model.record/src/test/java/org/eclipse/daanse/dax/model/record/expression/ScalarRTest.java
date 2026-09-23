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
import org.eclipse.daanse.dax.model.api.expression.Scalar;
import org.junit.jupiter.api.Test;

class ScalarRTest {

    @Test
    void isAScalarAndTransitivelyADaxExpression() {
        ScalarR scalar = new ScalarR("Status");

        assertThat(scalar).isInstanceOf(Scalar.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        ScalarR scalar = new ScalarR("Status");

        assertThat(scalar.name()).isEqualTo("Status");
    }

    @Test
    void allowsAnEmptyName() {
        ScalarR scalar = new ScalarR("");

        assertThat(scalar.name()).isEmpty();
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new ScalarR(null));
    }

    @Test
    void scalarsWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        ScalarR a = new ScalarR("Status");
        ScalarR b = new ScalarR("Status");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void scalarsWithDifferentNamesAreNotEqual() {
        ScalarR a = new ScalarR("Status");
        ScalarR b = new ScalarR("Amount");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringContainsTheName() {
        ScalarR scalar = new ScalarR("Status");

        assertThat(scalar.toString()).contains("Status");
    }
}
