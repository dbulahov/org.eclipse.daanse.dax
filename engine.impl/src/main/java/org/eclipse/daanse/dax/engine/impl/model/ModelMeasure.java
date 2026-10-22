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
package org.eclipse.daanse.dax.engine.impl.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.dax.engine.api.DaxType;

/**
 * A measure of the cube.
 *
 * @param name       the measure name
 * @param uniqueName the unique name, e.g. {@code [Measures].[Sales Amount]}
 * @param stored     whether it aggregates the rows of a fact table, not a
 *                   calculated member: BLANK where they are not, so also
 *                   where it is BLANK of each part of the context
 * @param type       the type of its values, as CSDL declares it; a client
 *                   applies the measure's format string only to a typed
 *                   numeric column
 * @param decomposed for a calculated measure computed of aggregates of
 *                   stored measures, possibly of other members of some
 *                   hierarchies, and not BLANK where one of them is not, as
 *                   {@code [Store Sales] - [Store Cost]}: the unique names of
 *                   those hierarchies; empty for a stored measure or another
 *                   calculated one, see {@link MeasureDecomposition}
 */
public record ModelMeasure(String name, String uniqueName, boolean stored, DaxType type,
        Optional<Set<String>> decomposed) {

    public ModelMeasure {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(uniqueName, "uniqueName");
        Objects.requireNonNull(type, "type");
        decomposed = decomposed.map(Set::copyOf);
    }

    /** A measure not known to be computed of stored measures. */
    public ModelMeasure(String name, String uniqueName, boolean stored, DaxType type) {
        this(name, uniqueName, stored, type, Optional.empty());
    }

    /** A measure whose type is not known. */
    public ModelMeasure(String name, String uniqueName, boolean stored) {
        this(name, uniqueName, stored, DaxType.VARIANT);
    }

    /** A measure not known to be stored, whose type is not known. */
    public ModelMeasure(String name, String uniqueName) {
        this(name, uniqueName, false);
    }

    /**
     * @return whether it is stored or computed of stored measures, not BLANK
     *         where one of them is: of the members of a hierarchy it does not
     *         take others of where it is not BLANK, it is the same as of all
     *         of them
     */
    public boolean computedOfStored() {
        return stored || decomposed.isPresent();
    }

    /**
     * @return the unique names of the hierarchies of whose other members it
     *         takes stored measures
     */
    public Set<String> shiftedHierarchies() {
        return decomposed.orElse(Set.of());
    }
}
