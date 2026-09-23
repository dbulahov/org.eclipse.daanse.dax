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
package org.eclipse.daanse.dax.model.api;

/**
 * A single definition that may appear in the {@code DEFINE} clause of a
 * {@link DaxStatement}, e.g. a {@link MeasureDefinition}.
 * <p>
 * Sealed over the kinds of definitions supported by the parser.
 * </p>
 */
public sealed interface DefineClause
        permits MeasureDefinition, TableDefinition, ColumnDefinition, VariableDefinition, ParameterDefinition {

}
