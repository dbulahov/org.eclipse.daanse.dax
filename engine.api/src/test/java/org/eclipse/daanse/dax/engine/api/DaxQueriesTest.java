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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DaxQueriesTest {

    @ParameterizedTest
    @ValueSource(strings = { "EVALUATE 'Sales'", "evaluate Sales", "DEFINE MEASURE Sales[X] = 1 EVALUATE Sales",
            "  \n\tEVALUATE Sales", "EVALUATE{1}", "EVALUATE('Sales')", "// comment\nEVALUATE Sales",
            "-- comment\r\nDEFINE VAR x = 1 EVALUATE {x}", "/* a\n b */ EVALUATE Sales", "﻿EVALUATE Sales",
            "EVALUATE" })
    void recognizesDax(String text) {
        assertThat(DaxQueries.isDaxQuery(text)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "SELECT {} ON 0 FROM [Sales]", "WITH MEMBER [Measures].[x] AS 1 SELECT FROM [Sales]",
            "SELECT * FROM $system.DISCOVER_SCHEMA_ROWSETS", "DRILLTHROUGH SELECT FROM [Sales]", "EVALUATEX Sales",
            "EVALUATE_1", "DEFINED", "/* EVALUATE */ SELECT FROM [Sales]", "// EVALUATE", "/* EVALUATE", "", "   " })
    void rejectsOtherStatements(String text) {
        assertThat(DaxQueries.isDaxQuery(text)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void rejectsNull() {
        assertThat(DaxQueries.isDaxQuery(null)).isFalse();
    }
}
