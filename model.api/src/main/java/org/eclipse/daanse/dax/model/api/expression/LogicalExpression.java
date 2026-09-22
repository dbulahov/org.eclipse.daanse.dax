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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.dax.model.api.expression;

/**
 * A binary logical expression, e.g. {@code [Amount] > 100 && [Country] =
 * "US"}. DAX spells these operators {@code &&} (AND) and {@code ||} (OR).
 */
public non-sealed interface LogicalExpression extends DaxExpression {

    /**
     * The logical operators supported by DAX.
     */
    enum LogicalOperator {
        AND, OR
    }

    /**
     * @return the left-hand operand
     */
    DaxExpression left();

    /**
     * @return the logical operator
     */
    LogicalOperator operator();

    /**
     * @return the right-hand operand
     */
    DaxExpression right();
}
