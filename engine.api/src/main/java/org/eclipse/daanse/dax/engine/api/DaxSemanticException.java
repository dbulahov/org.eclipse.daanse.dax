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
 * The query is valid DAX but does not fit the catalog, e.g. it refers to an
 * unknown table, column or measure, or calls a function with wrong arguments.
 */
public final class DaxSemanticException extends DaxException {

    private static final long serialVersionUID = 1L;

    public DaxSemanticException(String message) {
        super(message);
    }

    public DaxSemanticException(String message, Throwable cause) {
        super(message, cause);
    }
}
