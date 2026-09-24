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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.VarExpression;
import org.eclipse.daanse.dax.model.record.VariableDefinitionR;
import org.junit.jupiter.api.Test;

class VarExpressionRTest {

    private VariableDefinitionR variable(String name, int value) {
        return new VariableDefinitionR(name, new NumericLiteralR(new BigDecimal(value)));
    }

    private FunctionCallR row(String variableName) {
        return new FunctionCallR("ROW", List.of(new StringLiteralR("Value"), new VariableReferenceR(variableName)));
    }

    @Test
    void isAVarExpressionAndTransitivelyADaxExpression() {
        VarExpressionR expression = new VarExpressionR(List.of(variable("x", 1)), row("x"));

        assertThat(expression).isInstanceOf(VarExpression.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenVariablesInOrderAndTheReturnExpressionUnchanged() {
        VariableDefinitionR x = variable("x", 1);
        VariableDefinitionR y = variable("y", 2);
        DaxExpression returnExpression = row("y");

        VarExpressionR expression = new VarExpressionR(List.of(x, y), returnExpression);

        assertThat(expression.variables()).containsExactly(x, y);
        assertThat(expression.returnExpression()).isSameAs(returnExpression);
    }

    @Test
    void rejectsANullVariablesList() {
        assertThatNullPointerException().isThrownBy(() -> new VarExpressionR(null, row("x")));
    }

    @Test
    void rejectsAnEmptyVariablesList() {
        // VAR ... RETURN needs at least one VAR
        assertThatIllegalArgumentException().isThrownBy(() -> new VarExpressionR(List.of(), row("x")));
    }

    @Test
    void rejectsANullVariable() {
        List<VariableDefinition> variables = new ArrayList<>();
        variables.add(null);

        assertThatNullPointerException().isThrownBy(() -> new VarExpressionR(variables, row("x")));
    }

    @Test
    void rejectsANullReturnExpression() {
        assertThatNullPointerException().isThrownBy(() -> new VarExpressionR(List.of(variable("x", 1)), null));
    }

    @Test
    void copiesTheVariablesListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<VariableDefinition> source = new ArrayList<>();
        source.add(variable("x", 1));

        VarExpressionR expression = new VarExpressionR(source, row("x"));
        source.add(variable("y", 2));

        assertThat(expression.variables()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiableVariablesList() {
        VarExpressionR expression = new VarExpressionR(List.of(variable("x", 1)), row("x"));

        assertThatThrownBy(() -> expression.variables().add(variable("y", 2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mayBeNestedAsTheReturnExpression() {
        VarExpressionR inner = new VarExpressionR(List.of(variable("y", 2)), row("y"));

        VarExpressionR outer = new VarExpressionR(List.of(variable("x", 1)), inner);

        assertThat(outer.returnExpression()).isSameAs(inner);
    }

    @Test
    void expressionsWithEqualVariablesAndReturnExpressionAreEqualAndHaveTheSameHashCode() {
        VarExpressionR a = new VarExpressionR(List.of(variable("x", 1)), row("x"));
        VarExpressionR b = new VarExpressionR(List.of(variable("x", 1)), row("x"));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void expressionsWithDifferentVariablesAreNotEqual() {
        VarExpressionR a = new VarExpressionR(List.of(variable("x", 1)), row("x"));
        VarExpressionR b = new VarExpressionR(List.of(variable("x", 2)), row("x"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void expressionsWithDifferentVariableOrderAreNotEqual() {
        VarExpressionR a = new VarExpressionR(List.of(variable("x", 1), variable("y", 2)), row("x"));
        VarExpressionR b = new VarExpressionR(List.of(variable("y", 2), variable("x", 1)), row("x"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void expressionsWithDifferentReturnExpressionsAreNotEqual() {
        VarExpressionR a = new VarExpressionR(List.of(variable("x", 1)), row("x"));
        VarExpressionR b = new VarExpressionR(List.of(variable("x", 1)), new VariableReferenceR("x"));

        assertThat(a).isNotEqualTo(b);
    }
}
