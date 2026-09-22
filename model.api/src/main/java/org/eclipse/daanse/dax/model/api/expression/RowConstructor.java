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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.dax.model.api.expression;

import java.util.List;

/**
 * Represents a row constructor within a table constructor.
 *
 * <pre>
 * Example:
 * ("Value1", 1, TRUE)
 * </pre>
 * <p>
 * This type is deliberately NOT part of the sealed {@link DaxExpression}
 * hierarchy: a row constructor is a structural building block that only
 * occurs inside a {@link TableConstructor} and can never stand alone as an
 * expression, so it must not appear where a {@code DaxExpression} is
 * expected.
 * </p>
 */
public interface RowConstructor {

    /**
     * @return the list of column expressions
     */
    List<DaxExpression> columns();
}
