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

import org.eclipse.daanse.dax.model.api.expression.RowConstructor;
import org.eclipse.daanse.dax.model.api.expression.TableConstructor;

/**
 * Record implementation of {@link TableConstructor}.
 *
 * @param rows
 *            the list of row constructors
 */
public record TableConstructorR(List<RowConstructor> rows) implements TableConstructor {

    public TableConstructorR {
        Objects.requireNonNull(rows, "rows must not be null");
        rows = List.copyOf(rows);
    }
}
