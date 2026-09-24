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

/**
 * Evaluating the query failed, e.g. a conversion the data makes invalid or a
 * failure of the underlying database.
 */
public final class DaxExecutionException extends DaxException {

    private static final long serialVersionUID = 1L;

    public DaxExecutionException(String message) {
        super(message);
    }

    public DaxExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
