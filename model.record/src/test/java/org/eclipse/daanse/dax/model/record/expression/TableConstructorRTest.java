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

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.TableConstructor;
import org.eclipse.daanse.dax.model.record.DaxStatementR;
import org.eclipse.daanse.dax.model.record.EvaluateStatementR;
import org.junit.jupiter.api.Test;

class TableConstructorRTest {

    @Test
    void isATableConstructor() {
        TableConstructorR table = new TableConstructorR(List.of(new RowConstructorR(List.of())));

        assertThat(table).isInstanceOf(TableConstructor.class);
    }

    @Test
    void exposesTheGivenRowsInOrder() {
        RowConstructorR first = new RowConstructorR(List.of(new StringLiteralR("Value1")));
        RowConstructorR second = new RowConstructorR(List.of(new StringLiteralR("Value2")));

        TableConstructorR table = new TableConstructorR(List.of(first, second));

        assertThat(table.rows()).containsExactly(first, second);
    }

    @Test
    void allowsAnEmptyRowList() {
        TableConstructorR table = new TableConstructorR(List.of());

        assertThat(table.rows()).isEmpty();
    }

    @Test
    void rejectsANullRowsList() {
        assertThatNullPointerException().isThrownBy(() -> new TableConstructorR(null));
    }

    @Test
    void copiesTheListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<RowConstructor> source = new ArrayList<>();
        source.add(new RowConstructorR(List.of()));

        TableConstructorR table = new TableConstructorR(source);
        source.add(new RowConstructorR(List.of()));

        assertThat(table.rows()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiableRowsList() {
        TableConstructorR table = new TableConstructorR(List.of(new RowConstructorR(List.of())));

        assertThatThrownBy(() -> table.rows().add(new RowConstructorR(List.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void tablesWithEqualRowsAreEqualAndHaveTheSameHashCode() {
        TableConstructorR a = new TableConstructorR(List.of(new RowConstructorR(List.of())));
        TableConstructorR b = new TableConstructorR(List.of(new RowConstructorR(List.of())));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void tablesWithDifferentRowsAreNotEqual() {
        TableConstructorR a = new TableConstructorR(List.of(new RowConstructorR(List.of())));
        TableConstructorR b = new TableConstructorR(List.of());

        assertThat(a).isNotEqualTo(b);
    }

    /**
     * Builds the record tree by hand for {@code EVALUATE {"Hello World"}} and
     * checks it has exactly the shape the parser module produces for that
     * query, i.e. that the records wire together correctly end to end.
     */
    @Test
    void buildsTheTreeForAnEvaluateOfASingleColumnSingleRowTable() {
        DaxStatement statement = new DaxStatementR(
                List.of(new EvaluateStatementR(new TableConstructorR(List.of(new RowConstructorR(
                        List.of(new StringLiteralR("Hello World"))))))));

        EvaluateStatement evaluateStatement = statement.evaluateStatements().get(0);
        DaxExpression tableExpression = evaluateStatement.tableExpression();
        assertThat(tableExpression).isInstanceOf(TableConstructor.class);

        TableConstructor table = (TableConstructor) tableExpression;
        assertThat(table.rows()).hasSize(1);

        RowConstructor row = table.rows().get(0);
        assertThat(row.columns()).hasSize(1);
        assertThat(row.columns().get(0)).isInstanceOf(StringLiteralR.class)
                .extracting(column -> ((StringLiteralR) column).value()).isEqualTo("Hello World");
    }
}
