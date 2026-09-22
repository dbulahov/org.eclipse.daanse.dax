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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.record.expression.StringLiteralR;
import org.eclipse.daanse.dax.model.record.expression.TableConstructorR;
import org.junit.jupiter.api.Test;

class DaxStatementRTest {

    private EvaluateStatementR evaluateStatement() {
        return new EvaluateStatementR(new TableConstructorR(List.of()));
    }

    @Test
    void isADaxStatement() {
        DaxStatementR statement = new DaxStatementR(List.of(evaluateStatement()));

        assertThat(statement).isInstanceOf(DaxStatement.class);
    }

    @Test
    void exposesTheGivenEvaluateStatementsInOrder() {
        EvaluateStatementR first = evaluateStatement();
        EvaluateStatementR second = new EvaluateStatementR(new StringLiteralR("Hello World"));

        DaxStatementR statement = new DaxStatementR(List.of(first, second));

        assertThat(statement.evaluateStatements()).containsExactly(first, second);
    }

    @Test
    void rejectsANullEvaluateStatementsList() {
        assertThatNullPointerException().isThrownBy(() -> new DaxStatementR(null));
    }

    @Test
    void copiesTheListSoLaterMutationOfTheSourceListIsNotReflected() {
        List<EvaluateStatement> source = new ArrayList<>();
        source.add(evaluateStatement());

        DaxStatementR statement = new DaxStatementR(source);
        source.add(evaluateStatement());

        assertThat(statement.evaluateStatements()).hasSize(1);
    }

    @Test
    void returnsAnUnmodifiableEvaluateStatementsList() {
        DaxStatementR statement = new DaxStatementR(List.of(evaluateStatement()));

        assertThatThrownBy(() -> statement.evaluateStatements().add(evaluateStatement()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void statementsWithEqualEvaluateStatementsAreEqualAndHaveTheSameHashCode() {
        EvaluateStatementR shared = evaluateStatement();

        DaxStatementR a = new DaxStatementR(List.of(shared));
        DaxStatementR b = new DaxStatementR(List.of(shared));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void statementsWithDifferentEvaluateStatementsAreNotEqual() {
        DaxStatementR a = new DaxStatementR(List.of(evaluateStatement()));
        DaxStatementR b = new DaxStatementR(List.of(evaluateStatement(), evaluateStatement()));

        assertThat(a).isNotEqualTo(b);
    }
}
