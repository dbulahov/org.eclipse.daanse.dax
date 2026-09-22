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

import java.time.LocalDateTime;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;

/**
 * Record implementation of {@link DateTimeLiteral}.
 *
 * @param value
 *            the date/time value (no time zone; DAX date/time values are
 *            zone-less)
 */
public record DateTimeLiteralR(LocalDateTime value) implements DateTimeLiteral {

    public DateTimeLiteralR {
        Objects.requireNonNull(value, "value must not be null");
    }
}
