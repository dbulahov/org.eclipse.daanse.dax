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
import org.eclipse.daanse.dax.model.api.expression.VariableReference;
import org.junit.jupiter.api.Test;

class VariableReferenceRTest {

    @Test
    void isAVariableReferenceAndTransitivelyADaxExpression() {
        VariableReferenceR reference = new VariableReferenceR("__minAmount");

        assertThat(reference).isInstanceOf(VariableReference.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        VariableReferenceR reference = new VariableReferenceR("__minAmount");

        assertThat(reference.name()).isEqualTo("__minAmount");
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new VariableReferenceR(null));
    }

    @Test
    void referencesWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        VariableReferenceR a = new VariableReferenceR("__minAmount");
        VariableReferenceR b = new VariableReferenceR("__minAmount");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void referencesWithDifferentNamesAreNotEqual() {
        VariableReferenceR a = new VariableReferenceR("__minAmount");
        VariableReferenceR b = new VariableReferenceR("__maxAmount");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void comparesNamesCaseSensitively() {
        // the record keeps the name as written; case-insensitive resolution
        // of variables is the parser's job
        VariableReferenceR a = new VariableReferenceR("__minAmount");
        VariableReferenceR b = new VariableReferenceR("__MINAMOUNT");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void isNotEqualToAKeywordWithTheSameName() {
        assertThat(new VariableReferenceR("__minAmount")).isNotEqualTo(new KeywordR("__minAmount"));
    }

    @Test
    void toStringContainsTheName() {
        VariableReferenceR reference = new VariableReferenceR("__minAmount");

        assertThat(reference.toString()).contains("__minAmount");
    }
}
