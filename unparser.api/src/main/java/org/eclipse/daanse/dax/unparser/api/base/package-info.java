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
 * Reusable base classes for {@link org.eclipse.daanse.dax.unparser.api.UnParser}
 * implementations.
 * <p>
 * {@link org.eclipse.daanse.dax.unparser.api.base.AbstractUnParser} implements
 * the complete, exhaustive traversal of the sealed DAX model hierarchy and the
 * central DAX quoting rules. Implementation bundles extend it and override only
 * their formatting-specific hooks, so no traversal logic has to be duplicated
 * across unparser bundles.
 * </p>
 *
 * @since 1.0
 */
@org.osgi.annotation.bundle.Export
package org.eclipse.daanse.dax.unparser.api.base;
