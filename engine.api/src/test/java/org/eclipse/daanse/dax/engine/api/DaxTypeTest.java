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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class DaxTypeTest {

    @Test
    void typeOfValue() {
        assertThat(DaxType.of(1L)).isEqualTo(DaxType.INTEGER);
        assertThat(DaxType.of(BigDecimal.ONE)).isEqualTo(DaxType.DECIMAL);
        assertThat(DaxType.of(1.5d)).isEqualTo(DaxType.DOUBLE);
        assertThat(DaxType.of("a")).isEqualTo(DaxType.STRING);
        assertThat(DaxType.of(true)).isEqualTo(DaxType.BOOLEAN);
        assertThat(DaxType.of(LocalDateTime.of(2026, 1, 1, 0, 0))).isEqualTo(DaxType.DATETIME);
        assertThat(DaxType.of(new byte[] { 1 })).isEqualTo(DaxType.BINARY);
    }

    @Test
    void rejectsNonDaxValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> DaxType.of(1));
    }
}
