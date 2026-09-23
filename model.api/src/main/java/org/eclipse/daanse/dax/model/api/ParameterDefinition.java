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
 * One parameter definition of a {@code DEFINE} clause, e.g.
 * {@code @MinAmount = 1000}.
 */
public non-sealed interface ParameterDefinition extends DefineClause {

    /**
     * @return the parameter name, without the leading {@code @}
     */
    String name();

    /**
     * @return the expression the parameter evaluates to
     */
    DaxExpression expression();
}
