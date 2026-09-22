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
 * A constant value in a DAX expression.
 * <p>
 * Sealed over the literal kinds supported by the parser:
 * {@link NumericLiteral}, {@link StringLiteral}, {@link BooleanLiteral} and
 * {@link DateTimeLiteral}. Literals are also valid {@link StartAtValue START
 * AT values}.
 * </p>
 */
public sealed interface Literal extends DaxExpression
        permits StringLiteral {

}
