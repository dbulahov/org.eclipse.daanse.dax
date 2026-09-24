/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

import org.eclipse.daanse.dax.unparser.api.UnParser;
import org.eclipse.daanse.dax.unparser.api.base.AbstractUnParser;
import org.osgi.service.component.annotations.Component;

/**
 * Minimal {@link UnParser} implementation: renders a DAX statement without
 * comments, using the plain layout provided by {@link AbstractUnParser}.
 * <p>
 * The output is intended to be re-parsable: identifiers, string literals and
 * date/time literals are quoted and escaped by the base class, and parentheses
 * are emitted only where operator precedence requires them.
 * </p>
 * <p>
 * The service is registered with the property
 * {@code dax.unparser.style=simple}. Consumers that need exactly this
 * implementation select it with a target filter, for example
 * {@code @Reference(target = "(dax.unparser.style=simple)")} or
 * {@code @Reference(service = UnParser.class, target = "(dax.unparser.style=simple)")}.
 * </p>
 */
@Component(service = UnParser.class, property = { SimpleUnparser.STYLE_PROPERTY })
public class SimpleUnparser extends AbstractUnParser {

    /** Service property that identifies this unparser implementation. */
    public static final String STYLE_PROPERTY = "dax.unparser.style=simple";

    // The complete rendering logic is inherited from AbstractUnParser.
}
