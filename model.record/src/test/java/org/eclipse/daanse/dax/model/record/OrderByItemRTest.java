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
import java.util.Optional;

import org.eclipse.daanse.dax.model.api.OrderByItem;
import org.eclipse.daanse.dax.model.api.OrderByItem.SortDirection;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.expression.EntityR;
import org.eclipse.daanse.dax.model.record.expression.IdentifierR;
import org.eclipse.daanse.dax.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.dax.model.record.expression.ScalarR;
import org.junit.jupiter.api.Test;

class OrderByItemRTest {

    private IdentifierR productKey() {
        return new IdentifierR(List.of(new EntityR("Product"), new ScalarR("ProductKey")));
    }

    @Test
    void isAnOrderByItem() {
        OrderByItemR item = new OrderByItemR(productKey(), SortDirection.ASC);

        assertThat(item).isInstanceOf(OrderByItem.class);
    }

    @Test
    void exposesTheGivenExpressionAndDirectionUnchanged() {
        DaxExpression expression = productKey();

        OrderByItemR item = new OrderByItemR(expression, SortDirection.DESC);

        assertThat(item.expression()).isSameAs(expression);
        assertThat(item.direction()).isEqualTo(SortDirection.DESC);
    }

    @Test
    void hasNoStartAtValueWhenConstructedWithoutOne() {
        OrderByItemR item = new OrderByItemR(productKey(), SortDirection.ASC);

        assertThat(item.startAt()).isEmpty();
    }

    @Test
    void exposesTheGivenStartAtValueUnchanged() {
        NumericLiteralR startAt = new NumericLiteralR(new BigDecimal("50"));

        OrderByItemR item = new OrderByItemR(productKey(), SortDirection.ASC, Optional.of(startAt));

        assertThat(item.startAt()).contains(startAt);
    }

    @Test
    void rejectsANullExpression() {
        assertThatNullPointerException().isThrownBy(() -> new OrderByItemR(null, SortDirection.ASC));
    }

    @Test
    void rejectsANullDirection() {
        assertThatNullPointerException().isThrownBy(() -> new OrderByItemR(productKey(), null));
    }

    @Test
    void rejectsANullStartAtOptional() {
        assertThatNullPointerException().isThrownBy(() -> new OrderByItemR(productKey(), SortDirection.ASC, null));
    }

    @Test
    void itemsWithEqualFieldsAreEqualAndHaveTheSameHashCode() {
        OrderByItemR a = new OrderByItemR(productKey(), SortDirection.ASC);
        OrderByItemR b = new OrderByItemR(productKey(), SortDirection.ASC);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void itemsWithDifferentDirectionsAreNotEqual() {
        OrderByItemR a = new OrderByItemR(productKey(), SortDirection.ASC);
        OrderByItemR b = new OrderByItemR(productKey(), SortDirection.DESC);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void itemsWithDifferentStartAtValuesAreNotEqual() {
        OrderByItemR a = new OrderByItemR(productKey(), SortDirection.ASC);
        OrderByItemR b = new OrderByItemR(productKey(), SortDirection.ASC,
                Optional.of(new NumericLiteralR(new BigDecimal("50"))));

        assertThat(a).isNotEqualTo(b);
    }
}
