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
package org.eclipse.daanse.dax.parser.api;

public class DaxParserException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Value used for {@link #line()} and {@link #column()} when no position
     * information is available.
     */
    public static final int UNKNOWN_POSITION = -1;

    private final int line;
    private final int column;

    @SuppressWarnings("unused")
    private DaxParserException() {
        super("");
        this.line = UNKNOWN_POSITION;
        this.column = UNKNOWN_POSITION;
    }

    public DaxParserException(String message) {
        super(message);
        this.line = UNKNOWN_POSITION;
        this.column = UNKNOWN_POSITION;
    }

    public DaxParserException(String message, Throwable throwable) {
        super(message, throwable);
        this.line = UNKNOWN_POSITION;
        this.column = UNKNOWN_POSITION;
    }

    public DaxParserException(Throwable throwable) {
        super(throwable);
        this.line = UNKNOWN_POSITION;
        this.column = UNKNOWN_POSITION;
    }

    public DaxParserException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public DaxParserException(String message, Throwable throwable, int line, int column) {
        super(message, throwable);
        this.line = line;
        this.column = column;
    }

    /**
     * The 1-based line of the input where the parse error was detected, or
     * {@link #UNKNOWN_POSITION} if not available.
     */
    public int line() {
        return line;
    }

    /**
     * The 1-based column of the input where the parse error was detected, or
     * {@link #UNKNOWN_POSITION} if not available.
     */
    public int column() {
        return column;
    }

}
