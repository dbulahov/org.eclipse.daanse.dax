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
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.junit.jupiter.api.Test;

class RowConstructorRTest {

    @Test
    void isARowConstructor() {
        RowConstructorR row = new RowConstructorR(List.of(new StringLiteralR("Hello World")));

        assertThat(row).isInstanceOf(RowConstructor.class);
    }

    @Test
    void exposesTheGivenColumnsInOrder() {
        StringLiteralR first = new StringLiteralR("Value1");
        StringLiteralR second = new StringLiteralR("Value2");

        RowConstructorR row = new RowConstructorR(List.of(first, second));

        assertThat(row.columns()).containsExactly(first, second);
    }

    @Test
    void allowsAnEmptyColumnList() {
        RowConstructorR row = new RowConstructorR(List.of());

        assertThat(row.columns()).isEmpty();
    }

    @Test
    void rejectsANullColumnsList() {
        assertThatNullPointerException().isThrownBy(() -> new RowConstructorR(null));
    }

    @Test
    void copiesTheListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<DaxExpression> source = new ArrayList<>();
        source.add(new StringLiteralR("Value1"));

        RowConstructorR row = new RowConstructorR(source);
        source.add(new StringLiteralR("Value2"));

        assertThat(row.columns()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiableColumnsList() {
        RowConstructorR row = new RowConstructorR(List.of(new StringLiteralR("Value1")));

        assertThatThrownBy(() -> row.columns().add(new StringLiteralR("Value2")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rowsWithEqualColumnsAreEqualAndHaveTheSameHashCode() {
        RowConstructorR a = new RowConstructorR(List.of(new StringLiteralR("Value1")));
        RowConstructorR b = new RowConstructorR(List.of(new StringLiteralR("Value1")));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void rowsWithDifferentColumnsAreNotEqual() {
        RowConstructorR a = new RowConstructorR(List.of(new StringLiteralR("Value1")));
        RowConstructorR b = new RowConstructorR(List.of(new StringLiteralR("Value2")));

        assertThat(a).isNotEqualTo(b);
    }
}
