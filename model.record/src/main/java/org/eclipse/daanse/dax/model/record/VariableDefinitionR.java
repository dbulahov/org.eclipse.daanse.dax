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

import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * Record implementation of {@link VariableDefinition}.
 *
 * @param name
 *            the name the variable is defined under
 * @param expression
 *            the expression the variable evaluates to
 */
public record VariableDefinitionR(String name, DaxExpression expression) implements VariableDefinition {

    public VariableDefinitionR {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(expression, "expression must not be null");
    }
}
