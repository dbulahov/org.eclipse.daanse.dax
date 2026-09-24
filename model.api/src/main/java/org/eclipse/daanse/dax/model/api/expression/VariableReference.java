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

/**
 * A reference to a variable declared by a {@code VAR} in scope, e.g. the
 * {@code x} in {@code VAR x = 1 RETURN ROW("Value", x)}. The variable may be
 * declared by an enclosing {@link VarExpression} or by a {@code DEFINE VAR}
 * clause.
 */
public non-sealed interface VariableReference extends DaxExpression {

    /**
     * @return the variable name as written at the reference
     */
    String name();
}
