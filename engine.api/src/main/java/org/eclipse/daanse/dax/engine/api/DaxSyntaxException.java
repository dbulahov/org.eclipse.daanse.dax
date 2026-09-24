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
 * The query text is not valid DAX.
 */
public final class DaxSyntaxException extends DaxException {

    private static final long serialVersionUID = 1L;

    /** Line or column of an error whose position is not known. */
    public static final int UNKNOWN_POSITION = -1;

    private final int line;
    private final int column;

    /**
     * @param message the description of the error
     * @param line    the 1-based line of the error, or {@link #UNKNOWN_POSITION}
     * @param column  the 1-based column of the error, or
     *                {@link #UNKNOWN_POSITION}
     * @param cause   the parser's failure; may be {@code null}
     */
    public DaxSyntaxException(String message, int line, int column, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
    }

    /** @return the 1-based line of the error, or {@link #UNKNOWN_POSITION} */
    public int line() {
        return line;
    }

    /** @return the 1-based column of the error, or {@link #UNKNOWN_POSITION} */
    public int column() {
        return column;
    }
}
