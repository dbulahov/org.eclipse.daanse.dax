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
import org.eclipse.daanse.dax.model.api.expression.Entity;
import org.junit.jupiter.api.Test;

class EntityRTest {

    @Test
    void isAnEntityAndTransitivelyADaxExpression() {
        EntityR entity = new EntityR("Sales");

        assertThat(entity).isInstanceOf(Entity.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        EntityR entity = new EntityR("Sales");

        assertThat(entity.name()).isEqualTo("Sales");
    }

    @Test
    void allowsAnEmptyName() {
        EntityR entity = new EntityR("");

        assertThat(entity.name()).isEmpty();
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new EntityR(null));
    }

    @Test
    void entitiesWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        EntityR a = new EntityR("Sales");
        EntityR b = new EntityR("Sales");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void entitiesWithDifferentNamesAreNotEqual() {
        EntityR a = new EntityR("Sales");
        EntityR b = new EntityR("Customers");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringContainsTheName() {
        EntityR entity = new EntityR("Sales");

        assertThat(entity.toString()).contains("Sales");
    }
}
