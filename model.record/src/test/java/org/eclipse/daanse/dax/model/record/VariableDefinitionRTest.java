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

import java.math.BigDecimal;

import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.junit.jupiter.api.Test;

class VariableDefinitionRTest {

    private NumericLiteralR minAmount() {
        return new NumericLiteralR(new BigDecimal("1000"));
    }

    @Test
    void isAVariableDefinitionAndTransitivelyADefineClause() {
        VariableDefinitionR variable = new VariableDefinitionR("__minAmount", minAmount());

        assertThat(variable).isInstanceOf(VariableDefinition.class).isInstanceOf(DefineClause.class);
    }

    @Test
    void exposesTheGivenNameAndExpressionUnchanged() {
        DaxExpression expression = minAmount();

        VariableDefinitionR variable = new VariableDefinitionR("__minAmount", expression);

        assertThat(variable.name()).isEqualTo("__minAmount");
        assertThat(variable.expression()).isSameAs(expression);
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new VariableDefinitionR(null, minAmount()));
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new VariableDefinitionR("__minAmount", null));
    }

    @Test
    void definitionsWithEqualNameAndExpressionAreEqualAndHaveTheSameHashCode() {
        VariableDefinitionR a = new VariableDefinitionR("__minAmount", minAmount());
        VariableDefinitionR b = new VariableDefinitionR("__minAmount", minAmount());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void definitionsWithDifferentNamesAreNotEqual() {
        VariableDefinitionR a = new VariableDefinitionR("__minAmount", minAmount());
        VariableDefinitionR b = new VariableDefinitionR("__maxAmount", minAmount());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void definitionsWithDifferentExpressionsAreNotEqual() {
        VariableDefinitionR a = new VariableDefinitionR("__minAmount", minAmount());
        VariableDefinitionR b = new VariableDefinitionR("__minAmount", new NumericLiteralR(new BigDecimal("2000")));

        assertThat(a).isNotEqualTo(b);
    }
}
