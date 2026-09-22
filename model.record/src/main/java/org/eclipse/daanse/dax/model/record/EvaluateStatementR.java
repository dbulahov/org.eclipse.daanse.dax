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

import java.util.Objects;

import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * Record implementation of {@link EvaluateStatement}.
 *
 * @param tableExpression
 *            the table expression to evaluate
 */
public record EvaluateStatementR(DaxExpression tableExpression) implements EvaluateStatement {

    public EvaluateStatementR {
        Objects.requireNonNull(tableExpression, "tableExpression must not be null");
    }
}
