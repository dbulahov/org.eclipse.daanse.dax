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
 * Tells DAX queries from other statement texts, e.g. MDX, so a caller that
 * receives both can dispatch them.
 */
public final class DaxQueries {

    private DaxQueries() {
    }

    /**
     * @param text a statement text
     * @return whether the text starts, after whitespace and comments, with the
     *         keyword {@code EVALUATE} or {@code DEFINE}
     */
    public static boolean isDaxQuery(CharSequence text) {
        if (text == null) {
            return false;
        }
        String s = text.toString();
        int start = skipWhitespaceAndComments(s);
        int end = start;
        while (end < s.length() && isIdentifierPart(s.charAt(end))) {
            end++;
        }
        String keyword = s.substring(start, end);
        return "EVALUATE".equalsIgnoreCase(keyword) || "DEFINE".equalsIgnoreCase(keyword);
    }

    private static int skipWhitespaceAndComments(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '\uFEFF') {
                i++;
            } else if (s.startsWith("//", i) || s.startsWith("--", i)) {
                while (i < s.length() && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
                    i++;
                }
            } else if (s.startsWith("/*", i)) {
                int close = s.indexOf("*/", i + 2);
                i = close < 0 ? s.length() : close + 2;
            } else {
                break;
            }
        }
        return i;
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
