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
import org.eclipse.daanse.dax.model.api.expression.RowConstructor;

/**
 * Record implementation of {@link RowConstructor}.
 *
 * @param columns
 *            the list of column expressions
 */
public record RowConstructorR(List<DaxExpression> columns) implements RowConstructor {

    public RowConstructorR {
        Objects.requireNonNull(columns, "columns must not be null");
        columns = List.copyOf(columns);
    }
}
