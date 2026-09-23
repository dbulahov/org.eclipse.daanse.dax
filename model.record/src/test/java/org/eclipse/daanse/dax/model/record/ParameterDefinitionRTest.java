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
import org.eclipse.daanse.dax.model.api.ParameterDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.junit.jupiter.api.Test;

class ParameterDefinitionRTest {

    private NumericLiteralR minAmount() {
        return new NumericLiteralR(new BigDecimal("1000"));
    }

    @Test
    void isAParameterDefinitionAndTransitivelyADefineClause() {
        ParameterDefinitionR parameter = new ParameterDefinitionR("MinAmount", minAmount());

        assertThat(parameter).isInstanceOf(ParameterDefinition.class).isInstanceOf(DefineClause.class);
    }

    @Test
    void exposesTheGivenNameAndExpressionUnchanged() {
        DaxExpression expression = minAmount();

        ParameterDefinitionR parameter = new ParameterDefinitionR("MinAmount", expression);

        assertThat(parameter.name()).isEqualTo("MinAmount");
        assertThat(parameter.expression()).isSameAs(expression);
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new ParameterDefinitionR(null, minAmount()));
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new ParameterDefinitionR("MinAmount", null));
    }

    @Test
    void definitionsWithEqualNameAndExpressionAreEqualAndHaveTheSameHashCode() {
        ParameterDefinitionR a = new ParameterDefinitionR("MinAmount", minAmount());
        ParameterDefinitionR b = new ParameterDefinitionR("MinAmount", minAmount());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void definitionsWithDifferentNamesAreNotEqual() {
        ParameterDefinitionR a = new ParameterDefinitionR("MinAmount", minAmount());
        ParameterDefinitionR b = new ParameterDefinitionR("MaxAmount", minAmount());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void definitionsWithDifferentExpressionsAreNotEqual() {
        ParameterDefinitionR a = new ParameterDefinitionR("MinAmount", minAmount());
        ParameterDefinitionR b = new ParameterDefinitionR("MinAmount", new NumericLiteralR(new BigDecimal("2000")));

        assertThat(a).isNotEqualTo(b);
    }
}
