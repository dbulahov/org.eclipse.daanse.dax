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

import java.math.BigDecimal;
import java.util.Objects;

import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;

/**
 * Record implementation of {@link NumericLiteral}.
 *
 * @param value
 *            the numeric value, preserving the source precision
 */
public record NumericLiteralR(BigDecimal value) implements NumericLiteral {

    public NumericLiteralR {
        Objects.requireNonNull(value, "value must not be null");
    }
}
