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

import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.record.expression.StringLiteralR;
import org.junit.jupiter.api.Test;

class EvaluateStatementRTest {

    @Test
    void isAnEvaluateStatement() {
        EvaluateStatementR statement = new EvaluateStatementR(new StringLiteralR("Hello World"));

        assertThat(statement).isInstanceOf(EvaluateStatement.class);
    }

    @Test
    void exposesTheGivenTableExpression() {
        DaxExpression expression = new StringLiteralR("Hello World");

        EvaluateStatementR statement = new EvaluateStatementR(expression);

        assertThat(statement.tableExpression()).isSameAs(expression);
    }

    @Test
    void rejectsANullTableExpression() {
        assertThatNullPointerException().isThrownBy(() -> new EvaluateStatementR(null));
    }

    @Test
    void statementsWithEqualTableExpressionsAreEqualAndHaveTheSameHashCode() {
        EvaluateStatementR a = new EvaluateStatementR(new StringLiteralR("Hello World"));
        EvaluateStatementR b = new EvaluateStatementR(new StringLiteralR("Hello World"));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void statementsWithDifferentTableExpressionsAreNotEqual() {
        EvaluateStatementR a = new EvaluateStatementR(new StringLiteralR("Hello World"));
        EvaluateStatementR b = new EvaluateStatementR(new StringLiteralR("Goodbye World"));

        assertThat(a).isNotEqualTo(b);
    }
}
