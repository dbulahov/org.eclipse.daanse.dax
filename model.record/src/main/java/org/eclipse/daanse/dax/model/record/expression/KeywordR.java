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

import org.eclipse.daanse.dax.model.api.expression.Keyword;

/**
 * Record implementation of {@link Keyword}.
 *
 * @param name
 *            the keyword text
 */
public record KeywordR(String name) implements Keyword {

    public KeywordR {
        Objects.requireNonNull(name, "name must not be null");
    }
}
