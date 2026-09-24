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

import java.util.Objects;

/**
 * The query was stopped before it completed.
 */
public final class DaxCancelledException extends DaxException {

    private static final long serialVersionUID = 1L;

    /** Why the query was stopped. */
    public enum Reason {
        /** {@link DaxQueryStatement#cancel()} was called. */
        CANCELLED,
        /** The statement's timeout elapsed. */
        TIMEOUT
    }

    private final Reason reason;

    public DaxCancelledException(Reason reason) {
        super(Objects.requireNonNull(reason, "reason") == Reason.TIMEOUT ? "query timed out" : "query cancelled");
        this.reason = reason;
    }

    /** @return why the query was stopped */
    public Reason reason() {
        return reason;
    }
}
