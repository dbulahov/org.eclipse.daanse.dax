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
package org.eclipse.daanse.dax.model.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;

import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.eclipse.daanse.dax.model.record.expression.EntityR;
import org.eclipse.daanse.dax.model.record.expression.FunctionCallR;
import org.eclipse.daanse.dax.model.record.expression.IdentifierR;
import org.eclipse.daanse.dax.model.record.expression.ScalarR;
import org.junit.jupiter.api.Test;

class MeasureDefinitionRTest {

    private IdentifierR totalAmount() {
        return new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Total Amount")));
    }

    private FunctionCallR sumOfAmount() {
        return new FunctionCallR("SUM",
                List.of(new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount")))));
    }

    @Test
    void isAMeasureDefinitionAndTransitivelyADefineClause() {
        MeasureDefinitionR measure = new MeasureDefinitionR(totalAmount(), sumOfAmount());

        assertThat(measure).isInstanceOf(MeasureDefinition.class).isInstanceOf(DefineClause.class);
    }

    @Test
    void exposesTheGivenNameAndExpressionUnchanged() {
        Identifier name = totalAmount();
        DaxExpression expression = sumOfAmount();

        MeasureDefinitionR measure = new MeasureDefinitionR(name, expression);

        assertThat(measure.name()).isSameAs(name);
        assertThat(measure.expression()).isSameAs(expression);
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new MeasureDefinitionR(null, sumOfAmount()));
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new MeasureDefinitionR(totalAmount(), null));
    }

    @Test
    void definitionsWithEqualNameAndExpressionAreEqualAndHaveTheSameHashCode() {
        MeasureDefinitionR a = new MeasureDefinitionR(totalAmount(), sumOfAmount());
        MeasureDefinitionR b = new MeasureDefinitionR(totalAmount(), sumOfAmount());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void definitionsWithDifferentNamesAreNotEqual() {
        MeasureDefinitionR a = new MeasureDefinitionR(totalAmount(), sumOfAmount());
        MeasureDefinitionR b = new MeasureDefinitionR(
                new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Average Amount"))), sumOfAmount());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void definitionsWithDifferentExpressionsAreNotEqual() {
        MeasureDefinitionR a = new MeasureDefinitionR(totalAmount(), sumOfAmount());
        MeasureDefinitionR b = new MeasureDefinitionR(totalAmount(),
                new FunctionCallR("COUNTROWS", List.of(new EntityR("Sales"))));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void convenienceConstructorBuildsTheEntityAndScalarIdentifier() {
        MeasureDefinitionR measure = new MeasureDefinitionR("Sales", "Total Amount", sumOfAmount());

        assertThat(measure.name().parts()).containsExactly(new EntityR("Sales"), new ScalarR("Total Amount"));
        assertThat(measure).isEqualTo(new MeasureDefinitionR(totalAmount(), sumOfAmount()));
    }

    @Test
    void convenienceConstructorRejectsNullParts() {
        assertThatNullPointerException().isThrownBy(() -> new MeasureDefinitionR(null, "Total Amount", sumOfAmount()));
        assertThatNullPointerException().isThrownBy(() -> new MeasureDefinitionR("Sales", null, sumOfAmount()));
        assertThatNullPointerException().isThrownBy(() -> new MeasureDefinitionR("Sales", "Total Amount", null));
    }
}
