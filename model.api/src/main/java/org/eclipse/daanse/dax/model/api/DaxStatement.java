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
package org.eclipse.daanse.dax.model.api;

import java.util.List;

/**
 * A complete DAX query statement, following the DAX query grammar:
 *
 * EVALUATE expression+
 */
public interface DaxStatement {

    /**
     * @return the EVALUATE statements of the query
     */
    List<EvaluateStatement> evaluateStatements();
}
