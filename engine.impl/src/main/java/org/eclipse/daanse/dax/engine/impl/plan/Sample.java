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
package org.eclipse.daanse.dax.engine.impl.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.dax.engine.api.DaxColumn;
import org.eclipse.daanse.dax.engine.impl.plan.EvaluatePlan.SortKey;

/**
 * Rows of a table evenly spread over an order of its columns, as
 * {@code SAMPLE} gives them: all rows if there are not more than the count,
 * else the first, the last and those between them at equal steps. Computed
 * on the rows of its source, in the order.
 *
 * @param source the table
 * @param count  how many rows; none if 0 or less
 * @param keys   the order, by columns of the source; not empty
 */
public record Sample(TablePlan source, long count, List<SortKey> keys) implements TablePlan {

    public Sample {
        Objects.requireNonNull(source, "source");
        keys = List.copyOf(keys);
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("no keys");
        }
    }

    @Override
    public List<DaxColumn> columns() {
        return source.columns();
    }

    /**
     * @param rows the rows of the source
     * @return the sample of the rows, in order
     */
    public List<List<Object>> apply(List<List<Object>> rows) {
        Comparator<List<Object>> order = null;
        for (SortKey key : keys) {
            Comparator<List<Object>> byKey = (a, b) -> DaxValues.compare(a.get(key.column()), b.get(key.column()));
            if (!key.ascending()) {
                byKey = byKey.reversed();
            }
            order = order == null ? byKey : order.thenComparing(byKey);
        }
        List<List<Object>> sorted = new ArrayList<>(rows);
        sorted.sort(order);
        if (count <= 0) {
            return List.of();
        }
        if (count >= sorted.size()) {
            return sorted;
        }
        if (count == 1) {
            return sorted.subList(0, 1);
        }
        // the first and the last row, and those between at equal steps
        List<List<Object>> sample = new ArrayList<>();
        long last = sorted.size() - 1L;
        for (long i = 0; i < count; i++) {
            sample.add(sorted.get((int) Math.round((double) (i * last) / (count - 1))));
        }
        return sample;
    }
}
