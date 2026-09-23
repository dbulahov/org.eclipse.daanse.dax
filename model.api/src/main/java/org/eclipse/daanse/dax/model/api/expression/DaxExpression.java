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
 * Root interface of all DAX expressions.
 * <p>
 * The interface is sealed over the expression kinds produced by the parser
 * </p>
 */
public sealed interface DaxExpression permits Literal, TableConstructor, BooleanExpression, ArithmeticExpression,
        LogicalExpression, StringExpression, Entity, FunctionCall, Identifier, Scalar, Keyword, Parameter {

}
