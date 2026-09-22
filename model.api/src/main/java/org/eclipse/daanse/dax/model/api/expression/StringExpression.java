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
 * A binary string expression, e.g. {@code "a" & "b"}. DAX has a single
 * string operator, {@code &}, which concatenates its operands.
 */
public non-sealed interface StringExpression extends DaxExpression {

    /**
     * The string operators supported by DAX.
     */
    enum StringOperator {
        AND
    }

    /**
     * @return the left-hand operand
     */
    DaxExpression left();

    /**
     * @return the string operator
     */
    StringOperator operator();

    /**
     * @return the right-hand operand
     */
    DaxExpression right();
}
