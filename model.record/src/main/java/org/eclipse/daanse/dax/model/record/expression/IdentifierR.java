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
import org.eclipse.daanse.dax.model.api.expression.Identifier;

/**
 * Record implementation of {@link Identifier}.
 *
 * @param parts
 *            the parts making up this identifier, in order
 */
public record IdentifierR(List<DaxExpression> parts) implements Identifier {

    public IdentifierR {
        Objects.requireNonNull(parts, "parts must not be null");
        parts = List.copyOf(parts);
    }

    /**
     * @param table the table name, without quotes
     * @return the table reference {@code 'table'}
     */
    public static IdentifierR ofTable(String table) {
        return new IdentifierR(List.of(new EntityR(table)));
    }

    /**
     * @param table  the table name, without quotes
     * @param column the column name, without brackets
     * @return the fully qualified column reference {@code 'table'[column]}
     */
    public static IdentifierR ofColumn(String table, String column) {
        return new IdentifierR(List.of(new EntityR(table), new ScalarR(column)));
    }

    /**
     * @param measure the measure (or column) name, without brackets
     * @return the unqualified reference {@code [measure]}
     */
    public static IdentifierR ofMeasure(String measure) {
        return new IdentifierR(List.of(new ScalarR(measure)));
    }
}
