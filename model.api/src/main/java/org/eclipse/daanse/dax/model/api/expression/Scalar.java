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
package org.eclipse.daanse.dax.model.api.expression;

/**
 * A scalar (column) reference in square brackets, e.g. {@code [Status]}.
 * Typically appears as one of the {@link Identifier#parts()} of a fully
 * qualified column reference such as {@code 'Sales'[Status]}.
 */
public non-sealed interface Scalar extends DaxExpression {

    /**
     * @return the column name, without the surrounding square brackets
     */
    String name();
}
