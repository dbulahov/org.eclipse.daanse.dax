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
package org.eclipse.daanse.dax.parser.ccc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.parser.api.DaxParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaxParserWrapper implements org.eclipse.daanse.dax.parser.api.DaxParser {

    private static final Logger logger = LoggerFactory.getLogger(DaxParserWrapper.class);
    private DaxParser delegate;

    public DaxParserWrapper(CharSequence dax) throws DaxParserException {
        logger.debug("Creating DaxParserWrapper with dax length: {}",
                dax != null ? dax.length() : 0);

        if (dax == null) {
            logger.error("DAX statement is null");
            throw new DaxParserException("statement must not be null");
        } else if (dax.length() == 0) {
            logger.error("DAX statement is empty");
            throw new DaxParserException("statement must not be empty");
        }
        try {
            delegate = new DaxParser(dax);
            logger.debug("DaxParserWrapper created successfully");
        } catch (Exception e) {
            logger.error("Failed to create DaxParser delegate", e);
            throw new DaxParserException("Failed to create parser", e);
        }
    }

    @Override
    public DaxStatement parseDaxStatement() throws DaxParserException {
        logger.debug("Parsing DAX statement");
        try {
            delegate.DaxStatement();
            DaxStatement result = (DaxStatement) delegate.peekNode();
            logger.debug("Successfully parsed DAX statement with {} EVALUATE statement(s)",
                    result.evaluateStatements().size());
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse DAX statement", e);
            throw toDaxParserException(e);
        } finally {
            dump();
        }
    }

    @Override
    public EvaluateStatement parseEvaluateStatement() throws DaxParserException {
        logger.debug("Parsing EVALUATE statement");
        try {
            delegate.EvaluateStatement();
            EvaluateStatement result = (EvaluateStatement) delegate.peekNode();
            logger.debug("Successfully parsed EVALUATE statement");
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse EVALUATE statement", e);
            throw toDaxParserException(e);
        } finally {
            dump();
        }
    }

    @Override
    public DaxExpression parseExpression() throws DaxParserException {
        logger.debug("Parsing DAX expression");
        try {
            // ExpressionRoot requires EOF, so trailing garbage is a parse error
            delegate.ExpressionRoot();
            DaxExpression result = (DaxExpression) delegate.peekNode();
            logger.debug("Successfully parsed DAX expression: {}", result.getClass().getSimpleName());
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse DAX expression", e);
            throw toDaxParserException(e);
        } finally {
            dump();
        }
    }

    /**
     * Wraps a parse failure, carrying over the position of the offending token
     * when the underlying exception provides one.
     */
    private static DaxParserException toDaxParserException(Exception e) {
        if (e instanceof DaxParserException daxParserException) {
            return daxParserException;
        }
        Throwable cause = e;
        while (cause != null && !(cause instanceof ParseException)) {
            cause = cause.getCause();
        }
        if (cause instanceof ParseException parseException) {
            Node.TerminalNode token = parseException.getToken();
            if (token != null) {
                return new DaxParserException(e.getMessage(), e, token.getBeginLine(), token.getBeginColumn());
            }
        }
        return new DaxParserException(e);
    }

    /**
     * Logs the parsed AST at TRACE level. The tree is captured into a buffer
     * instead of going to {@code System.out}, so parsing stays silent unless
     * tracing is switched on.
     */
    private void dump() {
        if (!logger.isTraceEnabled()) {
            return;
        }
        Node root = delegate.rootNode();
        if (root == null) {
            return;
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream sink = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            root.dump("", sink);
        }
        logger.trace("Parsed AST:{}{}", System.lineSeparator(), buffer.toString(StandardCharsets.UTF_8));
    }
}
