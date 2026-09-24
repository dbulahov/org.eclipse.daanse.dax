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

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;

/**
 * Record implementation of {@link DaxStatement}.
 *
 * @param defineClauses
 *            the definitions of the DEFINE clause, in source order
 * @param evaluateStatements
 *            the EVALUATE statements of the query, in source order; never
 *            empty
 */
public record DaxStatementR(List<DefineClause> defineClauses, List<EvaluateStatement> evaluateStatements)
        implements DaxStatement {

    public DaxStatementR {
        Objects.requireNonNull(defineClauses, "defineClauses must not be null");
        Objects.requireNonNull(evaluateStatements, "evaluateStatements must not be null");
        defineClauses = List.copyOf(defineClauses);
        evaluateStatements = List.copyOf(evaluateStatements);
    }

    /**
     * Convenience constructor for a DAX statement with no {@code DEFINE}
     * clause.
     *
     * @param evaluateStatements
     *            the EVALUATE statements of the query, in source order
     */
    public DaxStatementR(List<EvaluateStatement> evaluateStatements) {
        this(List.of(), evaluateStatements);
    }
}
