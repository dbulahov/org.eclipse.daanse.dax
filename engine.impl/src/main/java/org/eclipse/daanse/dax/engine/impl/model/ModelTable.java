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
package org.eclipse.daanse.dax.engine.impl.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.daanse.dax.engine.impl.mdx.MdxNames;

/**
 * A table of the {@link TabularModel}: a dimension of the cube.
 *
 * @param name    the table name
 * @param columns the columns, in the order of the dimension's hierarchies and
 *                levels
 */
public record ModelTable(String name, List<ModelColumn> columns) {

    public ModelTable {
        Objects.requireNonNull(name, "name");
        columns = List.copyOf(columns);
    }

    /**
     * @param name the column name, or the unique name of its level written with
     *             dots, e.g. {@code Product.Product.Category}
     * @return the column of that name, ignoring case as DAX does
     */
    public Optional<ModelColumn> column(String name) {
        Optional<ModelColumn> column = columns.stream().filter(c -> c.name().equalsIgnoreCase(name)).findFirst();
        if (column.isPresent() || name.indexOf('.') < 0) {
            return column;
        }
        String level = MdxNames.uniqueName(name);
        return columns.stream().filter(c -> c.level().equalsIgnoreCase(level)).findFirst();
    }
}
