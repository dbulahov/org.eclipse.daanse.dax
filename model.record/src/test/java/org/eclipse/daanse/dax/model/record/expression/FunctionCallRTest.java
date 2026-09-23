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
import org.eclipse.daanse.dax.model.api.expression.FunctionCall;
import org.junit.jupiter.api.Test;

class FunctionCallRTest {

    @Test
    void isAFunctionCallAndTransitivelyADaxExpression() {
        FunctionCallR call = new FunctionCallR("NOW", List.of());

        assertThat(call).isInstanceOf(FunctionCall.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenFunctionNameUnchanged() {
        FunctionCallR call = new FunctionCallR("NOW", List.of());

        assertThat(call.functionName()).isEqualTo("NOW");
    }

    @Test
    void allowsAnEmptyArgumentList() {
        FunctionCallR call = new FunctionCallR("NOW", List.of());

        assertThat(call.arguments()).isEmpty();
    }

    @Test
    void exposesTheGivenArgumentsInOrder() {
        EntityR first = new EntityR("Sales");
        StringLiteralR second = new StringLiteralR("Value1");

        FunctionCallR call = new FunctionCallR("ALL", List.of(first, second));

        assertThat(call.arguments()).containsExactly(first, second);
    }

    @Test
    void rejectsANullFunctionName() {
        assertThatNullPointerException().isThrownBy(() -> new FunctionCallR(null, List.of()));
    }

    @Test
    void rejectsANullArgumentsList() {
        assertThatNullPointerException().isThrownBy(() -> new FunctionCallR("NOW", null));
    }

    @Test
    void copiesTheArgumentsListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<DaxExpression> source = new ArrayList<>();
        source.add(new EntityR("Sales"));

        FunctionCallR call = new FunctionCallR("ALL", source);
        source.add(new EntityR("Customers"));

        assertThat(call.arguments()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiableArgumentsList() {
        FunctionCallR call = new FunctionCallR("ALL", List.of(new EntityR("Sales")));

        assertThatThrownBy(() -> call.arguments().add(new EntityR("Customers")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void callsWithEqualNameAndArgumentsAreEqualAndHaveTheSameHashCode() {
        FunctionCallR a = new FunctionCallR("ALL", List.of(new EntityR("Sales")));
        FunctionCallR b = new FunctionCallR("ALL", List.of(new EntityR("Sales")));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void callsWithDifferentFunctionNamesAreNotEqual() {
        FunctionCallR a = new FunctionCallR("ALL", List.of(new EntityR("Sales")));
        FunctionCallR b = new FunctionCallR("VALUES", List.of(new EntityR("Sales")));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void callsWithDifferentArgumentsAreNotEqual() {
        FunctionCallR a = new FunctionCallR("ALL", List.of(new EntityR("Sales")));
        FunctionCallR b = new FunctionCallR("ALL", List.of(new EntityR("Customers")));

        assertThat(a).isNotEqualTo(b);
    }
}
