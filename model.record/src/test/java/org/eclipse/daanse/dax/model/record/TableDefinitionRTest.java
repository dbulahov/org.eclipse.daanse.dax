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

import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.TableDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.expression.EntityR;
import org.eclipse.daanse.dax.model.record.expression.FunctionCallR;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.junit.jupiter.api.Test;

class TableDefinitionRTest {

    private FunctionCallR topTenProducts() {
        return new FunctionCallR("TOPN", List.of(new NumericLiteralR(new BigDecimal("10")), new EntityR("Product")));
    }

    @Test
    void isATableDefinitionAndTransitivelyADefineClause() {
        TableDefinitionR table = new TableDefinitionR("TopProducts", topTenProducts());

        assertThat(table).isInstanceOf(TableDefinition.class).isInstanceOf(DefineClause.class);
    }

    @Test
    void exposesTheGivenNameAndExpressionUnchanged() {
        DaxExpression expression = topTenProducts();

        TableDefinitionR table = new TableDefinitionR("TopProducts", expression);

        assertThat(table.name()).isEqualTo("TopProducts");
        assertThat(table.expression()).isSameAs(expression);
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new TableDefinitionR(null, topTenProducts()));
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new TableDefinitionR("TopProducts", null));
    }

    @Test
    void definitionsWithEqualNameAndExpressionAreEqualAndHaveTheSameHashCode() {
        TableDefinitionR a = new TableDefinitionR("TopProducts", topTenProducts());
        TableDefinitionR b = new TableDefinitionR("TopProducts", topTenProducts());

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void definitionsWithDifferentNamesAreNotEqual() {
        TableDefinitionR a = new TableDefinitionR("TopProducts", topTenProducts());
        TableDefinitionR b = new TableDefinitionR("BottomProducts", topTenProducts());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void definitionsWithDifferentExpressionsAreNotEqual() {
        TableDefinitionR a = new TableDefinitionR("TopProducts", topTenProducts());
        TableDefinitionR b = new TableDefinitionR("TopProducts", new EntityR("Product"));

        assertThat(a).isNotEqualTo(b);
    }
}
