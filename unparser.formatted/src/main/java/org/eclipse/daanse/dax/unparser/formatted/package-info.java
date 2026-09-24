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
/**
 * Formatted, commenting implementation of the DAX unparser.
 * <p>
 * {@link org.eclipse.daanse.dax.unparser.formatted.FormattedUnparser} extends
 * {@link org.eclipse.daanse.dax.unparser.api.base.AbstractUnParser}, indents the
 * statement and adds descriptive comments. It is configurable through
 * {@link org.eclipse.daanse.dax.unparser.formatted.FormattedUnparser.Config} and
 * registers an {@link org.eclipse.daanse.dax.unparser.api.UnParser} service with
 * the property {@code dax.unparser.style=formatted}.
 * </p>
 * <p>
 * This package is an implementation detail of the bundle and is deliberately
 * <em>not</em> exported.
 * </p>
 */
package org.eclipse.daanse.dax.unparser.formatted;
