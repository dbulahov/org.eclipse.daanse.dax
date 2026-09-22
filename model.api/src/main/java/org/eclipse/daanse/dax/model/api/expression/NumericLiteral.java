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

import java.math.BigDecimal;

/**
 * A numeric literal, e.g. {@code 42} or {@code 3.14}.
 */
public non-sealed interface NumericLiteral extends Literal {

    /**
     * @return the numeric value, preserving the source precision
     */
    BigDecimal value();
}
