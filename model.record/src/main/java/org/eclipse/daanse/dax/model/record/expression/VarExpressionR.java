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

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.VarExpression;

/**
 * Record implementation of {@link VarExpression}.
 *
 * @param variables
 *            the {@code VAR} declarations, in order
 * @param returnExpression
 *            the expression after {@code RETURN}
 */
public record VarExpressionR(List<VariableDefinition> variables, DaxExpression returnExpression)
        implements VarExpression {

    public VarExpressionR {
        variables = List.copyOf(Objects.requireNonNull(variables, "variables must not be null"));
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("variables must not be empty");
        }
        Objects.requireNonNull(returnExpression, "returnExpression must not be null");
    }
}
