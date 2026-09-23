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

import java.util.Objects;

import org.eclipse.daanse.dax.model.api.expression.Parameter;

/**
 * Record implementation of {@link Parameter}.
 *
 * @param name
 *            the parameter name, without the leading {@code @}
 */
public record ParameterR(String name) implements Parameter {

    public ParameterR {
        Objects.requireNonNull(name, "name must not be null");
    }
}
