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
package org.eclipse.daanse.dax.model.api.expression;

import java.util.List;

import org.eclipse.daanse.dax.model.api.VariableDefinition;

/**
 * An expression that declares local variables and evaluates to its
 * {@code RETURN} expression, e.g.
 * {@code VAR x = 1 RETURN ROW("Value", x)}.
 */
public non-sealed interface VarExpression extends DaxExpression {

    /**
     * @return the {@code VAR} declarations, in order; never empty
     */
    List<VariableDefinition> variables();

    /**
     * @return the expression after {@code RETURN}
     */
    DaxExpression returnExpression();
}
