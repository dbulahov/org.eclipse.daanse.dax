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
 * A binary comparison expression, e.g. {@code 5 < 6} or {@code [Amount] >=
 * 100}.
 */
public non-sealed interface BooleanExpression extends DaxExpression {

    /**
     * The comparison operators supported by DAX.
     */
    enum BooleanOperator {
        EQUAL, NOT_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL
    }

    /**
     * @return the left-hand operand
     */
    DaxExpression left();

    /**
     * @return the comparison operator
     */
    BooleanOperator operator();

    /**
     * @return the right-hand operand
     */
    DaxExpression right();
}
