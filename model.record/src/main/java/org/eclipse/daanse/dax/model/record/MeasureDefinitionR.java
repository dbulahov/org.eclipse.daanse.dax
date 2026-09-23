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

import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Identifier;

/**
 * Record implementation of {@link MeasureDefinition}.
 *
 * @param name
 *            the fully qualified name the measure is defined on
 * @param expression
 *            the expression the measure evaluates to
 */
public record MeasureDefinitionR(Identifier name, DaxExpression expression) implements MeasureDefinition {

    public MeasureDefinitionR {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(expression, "expression must not be null");
    }
}
