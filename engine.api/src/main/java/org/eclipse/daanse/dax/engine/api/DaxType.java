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
package org.eclipse.daanse.dax.engine.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The types of DAX values, with the Java type that represents each. BLANK is
 * {@code null} in every type.
 */
public enum DaxType {

    /** Whole number, 64 bit. */
    INTEGER(Long.class),

    /** Fixed decimal number (currency). */
    DECIMAL(BigDecimal.class),

    /** Floating point number. */
    DOUBLE(Double.class),

    /** Text. */
    STRING(String.class),

    /** True/false. */
    BOOLEAN(Boolean.class),

    /** Date and time; a date alone has midnight as its time. */
    DATETIME(LocalDateTime.class),

    /** Binary data. */
    BINARY(byte[].class),

    /** Values of differing types, e.g. of {@code IF} with mixed branches. */
    VARIANT(Object.class);

    private final Class<?> javaType;

    DaxType(Class<?> javaType) {
        this.javaType = javaType;
    }

    /** @return the Java type of the values of this type */
    public Class<?> javaType() {
        return javaType;
    }

    /**
     * @param value a value, not {@code null}
     * @return the type whose Java type the value has; {@link #VARIANT} is
     *         never returned
     * @throws IllegalArgumentException if the value's type is none of these
     */
    public static DaxType of(Object value) {
        for (DaxType type : values()) {
            if (type != VARIANT && type.javaType.isInstance(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("not a DAX value: " + value.getClass().getName());
    }
}
