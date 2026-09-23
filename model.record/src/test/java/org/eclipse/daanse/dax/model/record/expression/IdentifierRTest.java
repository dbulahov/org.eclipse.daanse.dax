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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.junit.jupiter.api.Test;

class IdentifierRTest {

    @Test
    void isAnIdentifierAndTransitivelyADaxExpression() {
        IdentifierR identifier = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Status")));

        assertThat(identifier).isInstanceOf(Identifier.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenPartsInOrder() {
        EntityR entity = new EntityR("Sales");
        ScalarR scalar = new ScalarR("Status");

        IdentifierR identifier = new IdentifierR(List.of(entity, scalar));

        assertThat(identifier.parts()).containsExactly(entity, scalar);
    }

    @Test
    void allowsAnEmptyPartsList() {
        IdentifierR identifier = new IdentifierR(List.of());

        assertThat(identifier.parts()).isEmpty();
    }

    @Test
    void rejectsANullPartsList() {
        assertThatNullPointerException().isThrownBy(() -> new IdentifierR(null));
    }

    @Test
    void copiesTheListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<DaxExpression> source = new ArrayList<>();
        source.add(new EntityR("Sales"));

        IdentifierR identifier = new IdentifierR(source);
        source.add(new ScalarR("Status"));

        assertThat(identifier.parts()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiablePartsList() {
        IdentifierR identifier = new IdentifierR(List.of(new EntityR("Sales")));

        assertThatThrownBy(() -> identifier.parts().add(new ScalarR("Status")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void identifiersWithEqualPartsAreEqualAndHaveTheSameHashCode() {
        IdentifierR a = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Status")));
        IdentifierR b = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Status")));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void identifiersWithDifferentPartsAreNotEqual() {
        IdentifierR a = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Status")));
        IdentifierR b = new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount")));

        assertThat(a).isNotEqualTo(b);
    }
}
