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

import org.eclipse.daanse.dax.model.api.expression.TableReference;

/**
 * Record implementation of {@link TableReference}.
 *
 * @param name
 *            the table name
 */
public record TableReferenceR(String name) implements TableReference {

    public TableReferenceR {
        Objects.requireNonNull(name, "name must not be null");
    }
}
