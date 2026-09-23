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
 * A bare unquoted word standing alone as a value, e.g. the {@code DESC} in
 * {@code TOPN(10, 'Sales', 'Sales'[Amount], DESC)}.
 */
public non-sealed interface Keyword extends DaxExpression {

    /**
     * @return the keyword text, e.g. {@code DESC}
     */
    String name();
}
