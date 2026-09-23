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

/**
 * A call to a DAX function, e.g. {@code NOW()} or {@code ALL('Sales')}.
 */
public non-sealed interface FunctionCall extends DaxExpression {

    /**
     * @return the name of the called function, e.g. {@code NOW}
     */
    String functionName();

    /**
     * @return the list of argument expressions, in order; empty when the
     *         function is called with no arguments
     */
    List<DaxExpression> arguments();
}
