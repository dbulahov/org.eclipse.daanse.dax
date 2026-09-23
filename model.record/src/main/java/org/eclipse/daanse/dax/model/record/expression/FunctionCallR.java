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

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.FunctionCall;

/**
 * Record implementation of {@link FunctionCall}.
 *
 * @param functionName
 *            the name of the called function
 * @param arguments
 *            the list of argument expressions
 */
public record FunctionCallR(String functionName, List<DaxExpression> arguments) implements FunctionCall {

    public FunctionCallR {
        Objects.requireNonNull(functionName, "functionName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        arguments = List.copyOf(arguments);
    }
}
