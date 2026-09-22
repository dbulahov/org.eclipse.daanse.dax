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
 * A binary arithmetic expression, e.g. {@code 6 + 5} or {@code 3 ^ 2}.
 */
public non-sealed interface ArithmeticExpression extends DaxExpression {

    /**
     * The arithmetic operators supported by DAX.
     */
    enum ArithmeticOperator {
        ADD, MINUS, MULTIPLY, DIVIDE, POWER
    }

    /**
     * @return the left-hand operand
     */
    DaxExpression left();

    /**
     * @return the arithmetic operator
     */
    ArithmeticOperator operator();

    /**
     * @return the right-hand operand
     */
    DaxExpression right();
}
