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
package org.eclipse.daanse.dax.unparser.simple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.eclipse.daanse.dax.model.api.DaxStatement;
import org.eclipse.daanse.dax.parser.api.DaxParserException;
import org.eclipse.daanse.dax.parser.api.DaxParserProvider;
import org.eclipse.daanse.dax.parser.ccc.CCCDaxParserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Round-trip test: parse -&gt; unparse -&gt; parse -&gt; unparse.
 * <p>
 * The unparsed text must parse again, and the second unparse must be identical
 * to the first one (fixpoint). The first unparse may normalize the input (for
 * example quoting), so the fixpoint is asserted on the unparsed form, not on
 * the original DAX text.
 * </p>
 */
class RoundTripTest {

    private final SimpleUnparser unparser = new SimpleUnparser();

    /**
     * The CCC parser lives in a package that the parser.ccc bundle does not
     * export; this is a plain surefire test, and {@code tests.bnd} keeps the
     * parser packages out of the OSGi test fragment's imports.
     */
    private final DaxParserProvider parserProvider = new CCCDaxParserProvider();

    private DaxStatement parse(String dax) throws DaxParserException {
        return parserProvider.newParser(dax).parseDaxStatement();
    }

    @Test
    void testUnparsed() throws Exception {
        String dax = "EVALUATE 'Sales'";

        String unparsed = unparser.unparseDaxStatement(parse(dax));

        assertThat(unparsed)
                .isEqualTo("EVALUATE 'Sales'");
    }

    @Test
    void testRedundantParenthesesRemoved() throws Exception {
        String dax = "EVALUATE FILTER('Sales', (('Sales'[Amount] * 2) + 1) > (10))";

        assertThat(unparser.unparseDaxStatement(parse(dax)))
                .isEqualTo("EVALUATE FILTER('Sales', 'Sales'[Amount] * 2 + 1 > 10)");
    }

    @Test
    void testRequiredParenthesesKept() throws Exception {
        String dax = "EVALUATE ROW(\"x\", (1 + 2) * (3 - (4 - 5)) ^ 2)";

        assertThat(unparser.unparseDaxStatement(parse(dax)))
                .isEqualTo("EVALUATE ROW(\"x\", (1 + 2) * (3 - (4 - 5)) ^ 2)");
    }

    @Test
    void testDefineBlock() throws Exception {
        String dax = "DEFINE MEASURE 'Sales'[Total] = SUM('Sales'[Amount]), VAR __min = 1000 "
                + "EVALUATE 'Sales' ORDER BY 'Sales'[Amount] DESC, 'Sales'[Status] START AT (5, \"a\")";

        assertThat(unparser.unparseDaxStatement(parse(dax))).isEqualTo("""
                DEFINE
                    MEASURE 'Sales'[Total] = SUM('Sales'[Amount]),
                    VAR __min = 1000
                EVALUATE 'Sales' ORDER BY 'Sales'[Amount] DESC, 'Sales'[Status] ASC START AT (5, "a")""");
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "EVALUATE 'Sales'", //
            "EVALUATE 'Sales''s Data'", //
            "EVALUATE FILTER('Sales', 'Sales'[Status] = \"Say \"\"hi\"\"\")", //
            "EVALUATE FILTER('Sales', 'Sales'[Date] >= dt\"2024-01-31\" && 'Sales'[Date] < dt\"2024-02-01T10:30:00\")", //
            "EVALUATE FILTER('Sales', ('Sales'[A] = 1 || 'Sales'[B] = 2) && true)", //
            "EVALUATE FILTER('Sales', 'Sales'[A] || ('Sales'[B] && 'Sales'[C]))", //
            "EVALUATE FILTER('Sales', ('Sales'[A] = 1) = false)", //
            "EVALUATE FILTER('Sales', 'Sales'[Status] IN {\"A\", \"B\"})", //
            "EVALUATE {(1, \"a\", true), (2, \"b\", false)}", //
            "EVALUATE {1, 2.5, 3}", //
            "EVALUATE ROW(\"x\", 1 - (2 - 3), \"y\", (1 - 2) - 3, \"z\", 2 ^ (3 ^ 2))", //
            "EVALUATE ROW(\"x\", \"a\" & (1 + 2) & [Measure])", //
            "EVALUATE TOPN(10, 'Sales', 'Sales'[Amount], DESC)", //
            "EVALUATE FILTER('Sales', 'Sales'[Amount] > @MinAmount)", //
            "DEFINE @MinAmount = 1000 EVALUATE FILTER('Sales', 'Sales'[Amount] > @MinAmount)", //
            "DEFINE TABLE Top = TOPN(10, 'Product', 'Product'[Price], DESC) EVALUATE Top", //
            "DEFINE TABLE T = {1, 2}, TABLE U = FILTER(T, true) EVALUATE CALCULATETABLE(U, T)", //
            "DEFINE COLUMN 'Sales'[Category] = IF('Sales'[Amount] > 100, \"High\", \"Low\") EVALUATE 'Sales'", //
            "EVALUATE 'Product' ORDER BY 'Product'[ProductKey] START AT 50", //
            "EVALUATE 'Product' ORDER BY 'Product'[ProductKey] START AT ((1 + 2) * 3)", //
            "EVALUATE 'A' EVALUATE 'B' ORDER BY 'B'[X] DESC", //
            "EVALUATE VAR x = 1 RETURN ROW(\"Value\", x)", //
            "EVALUATE VAR x = 1 VAR y = x * 2 RETURN ROW(\"Value\", (VAR z = y RETURN z) + 1)", //
            "DEFINE MEASURE 'Sales'[M] = VAR t = SUM('Sales'[Amount]) RETURN t * 2 EVALUATE 'Sales'" //
    })
    void testRoundTrip(String dax) throws Exception {
        String first = unparser.unparseDaxStatement(parse(dax));

        assertThatCode(() -> parse(first)).as("unparsed text must parse again:%n%s", first)
                .doesNotThrowAnyException();
        String second = unparser.unparseDaxStatement(parse(first));

        assertThat(second).isEqualTo(first);
    }

}
