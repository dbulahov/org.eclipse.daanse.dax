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
import org.eclipse.daanse.dax.model.api.expression.Keyword;
import org.junit.jupiter.api.Test;

class KeywordRTest {

    @Test
    void isAKeywordAndTransitivelyADaxExpression() {
        KeywordR keyword = new KeywordR("DESC");

        assertThat(keyword).isInstanceOf(Keyword.class).isInstanceOf(DaxExpression.class);
    }

    @Test
    void exposesTheGivenNameUnchanged() {
        KeywordR keyword = new KeywordR("DESC");

        assertThat(keyword.name()).isEqualTo("DESC");
    }

    @Test
    void allowsAnEmptyName() {
        KeywordR keyword = new KeywordR("");

        assertThat(keyword.name()).isEmpty();
    }

    @Test
    void rejectsANullName() {
        assertThatNullPointerException().isThrownBy(() -> new KeywordR(null));
    }

    @Test
    void keywordsWithEqualNamesAreEqualAndHaveTheSameHashCode() {
        KeywordR a = new KeywordR("DESC");
        KeywordR b = new KeywordR("DESC");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void keywordsWithDifferentNamesAreNotEqual() {
        KeywordR a = new KeywordR("DESC");
        KeywordR b = new KeywordR("ASC");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toStringContainsTheName() {
        KeywordR keyword = new KeywordR("DESC");

        assertThat(keyword.toString()).contains("DESC");
    }
}
