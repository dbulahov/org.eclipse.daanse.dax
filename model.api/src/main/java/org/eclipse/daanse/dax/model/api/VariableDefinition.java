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
package org.eclipse.daanse.dax.model.api;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * One {@code VAR} definition of a {@code DEFINE} clause, e.g.
 * {@code VAR __minAmount = 1000}.
 */
public non-sealed interface VariableDefinition extends DefineClause {

    /**
     * @return the name the variable is defined under, e.g.
     *         {@code __minAmount}
     */
    String name();

    /**
     * @return the expression the variable evaluates to
     */
    DaxExpression expression();
}
