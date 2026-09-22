/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

import java.time.LocalDateTime;

/**
 * A date/time literal in {@code dt"..."} format, e.g.
 * {@code dt"2024-01-31T10:30:00"}.
 */
public non-sealed interface DateTimeLiteral extends Literal {

    /**
     * @return the date/time value (no time zone; DAX date/time values are
     *         zone-less)
     */
    LocalDateTime value();
}
