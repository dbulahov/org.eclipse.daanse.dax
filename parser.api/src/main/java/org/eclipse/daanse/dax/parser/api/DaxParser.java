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

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;

public interface DaxParser {

    /**
     * Parses a complete DAX query: an optional DEFINE block followed by one
     * or more EVALUATE statements, up to the end of the input.
     *
     * @return the parsed query
     * @throws DaxParserException on syntax errors, carrying the position of
     *         the offending token when available
     */
    DaxStatement parseDaxStatement() throws DaxParserException;

    /**
     * Parses a single EVALUATE statement (without requiring the end of the
     * input afterwards).
     *
     * @return the parsed EVALUATE statement
     * @throws DaxParserException on syntax errors
     */
    EvaluateStatement parseEvaluateStatement() throws DaxParserException;

    /**
     * Parses a stand-alone DAX expression; the whole input must be consumed.
     *
     * @return the parsed expression
     * @throws DaxParserException on syntax errors
     */
    DaxExpression parseExpression() throws DaxParserException;
}
