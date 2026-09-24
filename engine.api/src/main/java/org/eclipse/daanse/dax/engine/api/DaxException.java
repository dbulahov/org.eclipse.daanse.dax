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
 * Failure of a DAX query. The subclass tells what went wrong, so a caller can
 * map it, e.g. to an XMLA error code.
 */
public abstract sealed class DaxException extends Exception
        permits DaxSyntaxException, DaxSemanticException, DaxExecutionException, DaxCancelledException {

    private static final long serialVersionUID = 1L;

    protected DaxException(String message) {
        super(message);
    }

    protected DaxException(String message, Throwable cause) {
        super(message, cause);
    }
}
