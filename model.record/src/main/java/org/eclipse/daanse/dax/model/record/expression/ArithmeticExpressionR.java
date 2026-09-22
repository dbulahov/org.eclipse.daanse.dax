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

import java.util.Objects;

import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression;
import org.eclipse.daanse.dax.model.api.expression.ArithmeticExpression.ArithmeticOperator;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

/**
 * Record implementation of {@link ArithmeticExpression}.
 *
 * @param left
 *            the left-hand operand
 * @param operator
 *            the arithmetic operator
 * @param right
 *            the right-hand operand
 */
public record ArithmeticExpressionR(DaxExpression left, ArithmeticOperator operator, DaxExpression right)
        implements ArithmeticExpression {

    public ArithmeticExpressionR {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(right, "right must not be null");
    }
}
