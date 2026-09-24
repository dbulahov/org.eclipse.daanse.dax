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

import org.eclipse.daanse.dax.model.api.expression.VariableReference;

/**
 * Record implementation of {@link VariableReference}.
 *
 * @param name
 *            the variable name
 */
public record VariableReferenceR(String name) implements VariableReference {

    public VariableReferenceR {
        Objects.requireNonNull(name, "name must not be null");
    }
}
