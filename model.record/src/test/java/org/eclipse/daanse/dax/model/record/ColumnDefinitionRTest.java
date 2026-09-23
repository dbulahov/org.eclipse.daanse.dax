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
import java.util.List;

import org.eclipse.daanse.dax.model.api.ColumnDefinition;
import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.eclipse.daanse.dax.model.record.expression.EntityR;
import org.eclipse.daanse.dax.model.record.expression.FunctionCallR;
import org.eclipse.daanse.dax.model.record.expression.IdentifierR;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.dax.model.record.expression.ScalarR;
import org.eclipse.daanse.dax.model.record.expression.StringLiteralR;
import org.junit.jupiter.api.Test;

class ColumnDefinitionRTest {

    private IdentifierR amountCategory() {
        return new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Amount Category")));
    }

    private FunctionCallR ifHighOrLow() {
        return new FunctionCallR("IF", List.of(new NumericLiteralR(new BigDecimal("1000")),
                new StringLiteralR("High"), new StringLiteralR("Low")));
    }

    @Test
    void isAColumnDefinitionAndTransitivelyADefineClause() {
        ColumnDefinitionR column = new ColumnDefinitionR(amountCategory(), ifHighOrLow());

        assertThat(column).isInstanceOf(ColumnDefinition.class).isInstanceOf(DefineClause.class);
    }

    @Test
    void exposesTheGivenNameAndExpressionUnchanged() {
        Identifier name = amountCategory();
        DaxExpression expression = ifHighOrLow();

        ColumnDefinitionR column = new ColumnDefinitionR(name, expression);

        assertThat(column.name()).isSameAs(name);
        assertThat(column.expression()).isSameAs(expression);
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new ColumnDefinitionR(null, ifHighOrLow()));
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new ColumnDefinitionR(amountCategory(), null));
    }

    @Test
    void definitionsWithEqualNameAndExpressionAreEqualAndHaveTheSameHashCode() {
        ColumnDefinitionR a = new ColumnDefinitionR(amountCategory(), ifHighOrLow());
        ColumnDefinitionR b = new ColumnDefinitionR(amountCategory(), ifHighOrLow());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void definitionsWithDifferentNamesAreNotEqual() {
        ColumnDefinitionR a = new ColumnDefinitionR(amountCategory(), ifHighOrLow());
        ColumnDefinitionR b = new ColumnDefinitionR(
                new IdentifierR(List.of(new EntityR("Sales"), new ScalarR("Region Category"))), ifHighOrLow());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void definitionsWithDifferentExpressionsAreNotEqual() {
        ColumnDefinitionR a = new ColumnDefinitionR(amountCategory(), ifHighOrLow());
        ColumnDefinitionR b = new ColumnDefinitionR(amountCategory(), new StringLiteralR("High"));

        assertThat(a).isNotEqualTo(b);
    }
}
