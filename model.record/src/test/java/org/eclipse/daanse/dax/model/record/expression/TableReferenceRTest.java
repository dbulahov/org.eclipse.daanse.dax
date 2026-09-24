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

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.TableReference;
import org.junit.jupiter.api.Test;

class TableReferenceRTest {

    @Test
    void isATableReferenceAndTransitivelyADaxExpression() {
        TableReferenceR reference = new TableReferenceR("TopProducts");

        assertThat(reference).isInstanceOf(TableReference.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        TableReferenceR reference = new TableReferenceR("TopProducts");

        assertThat(reference.name()).isEqualTo("TopProducts");
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new TableReferenceR(null));
    }

    @Test
    void referencesWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        TableReferenceR a = new TableReferenceR("TopProducts");
        TableReferenceR b = new TableReferenceR("TopProducts");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void referencesWithDifferentNamesAreNotEqual() {
        TableReferenceR a = new TableReferenceR("TopProducts");
        TableReferenceR b = new TableReferenceR("HighValueSales");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void comparesNamesCaseSensitively() {
        // the record keeps the name as written; case-insensitive resolution
        // of tables is the parser's job
        TableReferenceR a = new TableReferenceR("TopProducts");
        TableReferenceR b = new TableReferenceR("TOPPRODUCTS");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void isNotEqualToAKeywordWithTheSameName() {
        assertThat(new TableReferenceR("TopProducts")).isNotEqualTo(new KeywordR("TopProducts"));
    }

    @Test
    void toStringContainsTheName() {
        TableReferenceR reference = new TableReferenceR("TopProducts");

        assertThat(reference.toString()).contains("TopProducts");
    }
}
