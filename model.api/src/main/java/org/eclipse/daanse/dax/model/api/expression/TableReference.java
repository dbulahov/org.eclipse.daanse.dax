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
 * A reference to a table declared by a {@code DEFINE TABLE} clause, e.g. the
 * {@code TopProducts} in
 * {@code DEFINE TABLE TopProducts = TOPN(...) EVALUATE TopProducts}.
 */
public non-sealed interface TableReference extends DaxExpression {

    /**
     * @return the table name as written at the reference
     */
    String name();
}
