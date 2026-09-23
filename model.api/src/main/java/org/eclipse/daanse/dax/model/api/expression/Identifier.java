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

import java.util.List;

/**
 * A fully qualified reference to a column of a table (entity), e.g.
 * {@code 'Sales'[Status]}, made up of the chain of parts that qualify it -
 * currently an {@link Entity} followed by a {@link Scalar}.
 */
public non-sealed interface Identifier extends DaxExpression {

    /**
     * @return the parts making up this identifier, in order, e.g.
     *         {@code [Entity("Sales"), Scalar("Status")]} for
     *         {@code 'Sales'[Status]}
     */
    List<DaxExpression> parts();
}
