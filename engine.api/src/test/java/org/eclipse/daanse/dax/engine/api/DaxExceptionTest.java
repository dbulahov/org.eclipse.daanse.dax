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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DaxExceptionTest {

    @Test
    void syntaxExceptionKeepsPosition() {
        DaxSyntaxException e = new DaxSyntaxException("unexpected token", 3, 7, null);
        assertThat(e.line()).isEqualTo(3);
        assertThat(e.column()).isEqualTo(7);
        assertThat(e).hasMessage("unexpected token");
    }

    @Test
    void cancelledExceptionTellsReason() {
        assertThat(new DaxCancelledException(DaxCancelledException.Reason.TIMEOUT).reason())
                .isEqualTo(DaxCancelledException.Reason.TIMEOUT);
        assertThat(new DaxCancelledException(DaxCancelledException.Reason.CANCELLED)).hasMessage("query cancelled");
    }

    @Test
    void columnRequiresParts() {
        assertThatNullPointerException().isThrownBy(() -> new DaxColumn(null, Optional.empty(), DaxType.STRING, false));
        assertThatNullPointerException().isThrownBy(() -> new DaxColumn("[x]", null, DaxType.STRING, false));
    }
}
