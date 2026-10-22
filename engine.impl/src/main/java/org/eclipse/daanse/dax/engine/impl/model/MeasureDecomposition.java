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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.ParenthesesFunction;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.NumericLiteral;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;

/**
 * A calculated measure decomposed into the stored measures it is computed of,
 * the atoms: each of the measure's members or of another member of some
 * hierarchies, as {@code ([Profit], [Time].[Time].PrevMember)} gives it.
 * <p>
 * Where the measure is not BLANK is a formula of where the atoms are not,
 * true only where it is not: {@code a + b}, {@code a - b} and
 * {@code CoalesceEmpty(a, b)} where one of them is, {@code a * b} and
 * {@code a / b} where both are (BLANK of a denominator alone gives infinity
 * or BLANK, as the cube is set). A constant, {@code IIf}, a measure of another
 * cube and any other expression is not decomposed.
 * </p>
 * <p>
 * Of a set of members of a hierarchy the atoms are not taken of other members
 * of, the measure is computed of their aggregates: it is the same of the
 * members where they are not BLANK as of all of them.
 * </p>
 * <p>
 * {@code [Employees].CurrentMember.DataMember}, of a hierarchy with an all
 * member, takes the own rows of a parent of a parent-child hierarchy, some of
 * those of the member: an atom of it is not BLANK only where that of the
 * member is. Of the all member and of a calculated one it is no member, the
 * measure is BLANK there: of a set of members aggregated, as of all.
 * </p>
 *
 * @param atoms              the atoms, those of the data members of the
 *                           current ones as of the members: where they are not
 *                           BLANK, so are those
 * @param support            where it is not BLANK, of where the atoms are
 *                           not, the data members of the current ones as
 *                           atoms of their own
 * @param shiftedHierarchies the unique names of the hierarchies of whose other
 *                           members it takes atoms
 * @param currentHierarchies those and of the hierarchies whose current member
 *                           it takes the data member of
 */
public record MeasureDecomposition(Set<String> atoms, Support support, Set<String> shiftedHierarchies,
        Set<String> currentHierarchies) {

    public MeasureDecomposition {
        atoms = Set.copyOf(atoms);
        Objects.requireNonNull(support, "support");
        shiftedHierarchies = Set.copyOf(shiftedHierarchies);
        currentHierarchies = Set.copyOf(currentHierarchies);
    }

    /** Where a measure is not BLANK, of where its atoms are not. */
    public sealed interface Support {

        /** @return whether it is not BLANK where only the atoms are not */
        boolean holds(Set<String> atoms);
    }

    /** Where a stored measure, of the members of its key, is not BLANK. */
    public record Atom(String key) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return atoms.contains(key);
        }
    }

    /** Where one of them is. */
    public record Any(List<Support> supports) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return supports.stream().anyMatch(s -> s.holds(atoms));
        }
    }

    /** Where all of them are. */
    public record All(List<Support> supports) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return supports.stream().allMatch(s -> s.holds(atoms));
        }
    }

    /** @return whether the measure is not BLANK where the atom is not */
    public boolean notBlankWhere(String atom) {
        return support.holds(Set.of(atom));
    }

    /**
     * @param measure      a calculated measure
     * @param cubeMeasures the measures of the cube, by unique name
     * @return the measure decomposed; empty if it is not computed of stored
     *         measures that way
     */
    static Optional<MeasureDecomposition> of(Member measure, Map<String, Member> cubeMeasures) {
        Analysis analysis = new Analysis(cubeMeasures);
        return analysis.measure(measure, List.of()).filter(support -> !analysis.atoms.isEmpty())
                .map(support -> new MeasureDecomposition(analysis.atoms, support, analysis.shifted,
                        analysis.current));
    }

    /**
     * A member a stored measure is taken of.
     *
     * @param key        how it is navigated to
     * @param dataMember whether it is the data member of the current member
     */
    private record Coordinate(String key, boolean dataMember) {
    }

    private static final class Analysis {

        private static final Set<String> NAVIGATIONS = Set.of("PrevMember", "NextMember", "Parent", "FirstChild",
                "LastChild", "FirstSibling", "LastSibling");

        private final Map<String, Member> cubeMeasures;
        private final Set<String> visiting = new HashSet<>();
        private final Set<String> atoms = new LinkedHashSet<>();
        private final Set<String> shifted = new TreeSet<>();
        private final Set<String> current = new TreeSet<>();

        private Analysis(Map<String, Member> cubeMeasures) {
            this.cubeMeasures = cubeMeasures;
        }

        /**
         * @param shift the members it is taken of
         */
        private Optional<Support> measure(Member measure, List<Coordinate> shift) {
            if (!measure.isCalculated()) {
                atoms.add(key(measure, shift.stream().filter(c -> !c.dataMember()).toList()));
                return Optional.of(new Atom(key(measure, shift)));
            }
            if (!visiting.add(measure.getUniqueName())) {
                return Optional.empty();
            }
            try {
                return expression(measure.getExpression(), shift);
            } finally {
                visiting.remove(measure.getUniqueName());
            }
        }

        private static String key(Member measure, List<Coordinate> shift) {
            return shift.isEmpty() ? measure.getUniqueName()
                    : measure.getUniqueName() + " at " + String.join(", ", shift.stream().map(Coordinate::key).toList());
        }

        private Optional<Support> expression(Expression expression, List<Coordinate> shift) {
            return switch (expression) {
            case MemberExpression member when member.getMember().isMeasure()
                    && cubeMeasures.containsKey(member.getMember().getUniqueName()) ->
                measure(cubeMeasures.get(member.getMember().getUniqueName()), shift);
            case ResolvedFunCall call -> call(call, shift);
            case null, default -> Optional.empty();
            };
        }

        private Optional<Support> call(ResolvedFunCall call, List<Coordinate> shift) {
            OperationAtom atom = call.getOperationAtom();
            int count = call.getArgCount();
            if (count == 1 && (call.getFunDef() instanceof ParenthesesFunction
                    || is(atom, PrefixOperationAtom.class, "-", "+"))) {
                return expression(call.getArg(0), shift);
            }
            if (count > 1 && atom instanceof ParenthesesOperationAtom) {
                return tuple(call, shift);
            }
            if (count == 2 && is(atom, InfixOperationAtom.class, "+", "-")) {
                return arguments(call, shift).map(Any::new);
            }
            if (count == 2 && is(atom, InfixOperationAtom.class, "*", "/")) {
                return arguments(call, shift).map(All::new);
            }
            if (count > 0 && is(atom, FunctionOperationAtom.class, "CoalesceEmpty")) {
                return arguments(call, shift).map(Any::new);
            }
            return Optional.empty();
        }

        private Optional<List<Support>> arguments(ResolvedFunCall call, List<Coordinate> shift) {
            List<Support> supports = new ArrayList<>();
            for (Expression argument : call.getArgs()) {
                Optional<Support> support = expression(argument, shift);
                if (support.isEmpty()) {
                    return Optional.empty();
                }
                supports.add(support.get());
            }
            return Optional.of(supports);
        }

        /**
         * @return of a tuple of a measure and members of other hierarchies, the
         *         measure of those members
         */
        private Optional<Support> tuple(ResolvedFunCall tuple, List<Coordinate> shift) {
            Expression measure = null;
            List<Coordinate> shifted = new ArrayList<>(shift);
            for (Expression argument : tuple.getArgs()) {
                if (argument instanceof MemberExpression member && member.getMember().isMeasure()) {
                    if (measure != null) {
                        return Optional.empty();
                    }
                    measure = argument;
                    continue;
                }
                Optional<Coordinate> coordinate = coordinate(argument);
                if (coordinate.isEmpty()) {
                    return Optional.empty();
                }
                shifted.add(coordinate.get());
            }
            return measure == null ? Optional.empty() : expression(measure, shifted);
        }

        /**
         * @return the data member of the current member of a hierarchy with an
         *         all member, or another member it is navigated to; empty if
         *         it is another expression
         */
        private Optional<Coordinate> coordinate(Expression expression) {
            if (expression instanceof ResolvedFunCall call && call.getArgCount() == 1
                    && is(call.getOperationAtom(), PlainPropertyOperationAtom.class, "DataMember")) {
                Optional<Hierarchy> hierarchy = currentOf(call.getArg(0)).filter(Hierarchy::hasAll);
                hierarchy.ifPresent(h -> current.add(h.getUniqueName()));
                return hierarchy.map(h -> new Coordinate(h.getUniqueName() + ".DataMember", true));
            }
            return member(expression).map(key -> new Coordinate(key, false));
        }

        /**
         * @return a member of a hierarchy, given or navigated to of the current
         *         one, as {@code [Time].[Time].CurrentMember.PrevMember}; empty
         *         if it is another expression
         */
        private Optional<String> member(Expression expression) {
            Optional<Hierarchy> currentOf = currentOf(expression);
            if (currentOf.isPresent()) {
                shifted.add(currentOf.get().getUniqueName());
                current.add(currentOf.get().getUniqueName());
                return Optional.of(currentOf.get().getUniqueName());
            }
            return switch (expression) {
            case MemberExpression member when !member.getMember().isMeasure() -> {
                shifted.add(member.getMember().getHierarchy().getUniqueName());
                current.add(member.getMember().getHierarchy().getUniqueName());
                yield Optional.of(member.getMember().getUniqueName());
            }
            case ResolvedFunCall call when call.getArgCount() == 1
                    && is(call.getOperationAtom(), PlainPropertyOperationAtom.class, NAVIGATIONS.toArray(String[]::new)) ->
                member(call.getArg(0)).map(m -> m + "." + call.getOperationAtom().name());
            case ResolvedFunCall call when call.getArgCount() == 2
                    && is(call.getOperationAtom(), MethodOperationAtom.class, "Lag", "Lead")
                    && call.getArg(1) instanceof NumericLiteral offset ->
                member(call.getArg(0)).map(m -> m + "." + call.getOperationAtom().name() + "("
                        + offset.getValue().stripTrailingZeros().toPlainString() + ")");
            case null, default -> Optional.empty();
            };
        }

        /**
         * @return the hierarchy of an expression of its current member, as
         *         {@code [Time].[Time]} or {@code [Time].CurrentMember}; empty
         *         if it is another, or of the measures
         */
        private static Optional<Hierarchy> currentOf(Expression expression) {
            return switch (expression) {
            case HierarchyExpression hierarchy -> Optional.of(hierarchy.getHierarchy())
                    .filter(h -> !h.getDimension().isMeasures());
            case DimensionExpression dimension when !dimension.getDimension().isMeasures()
                    && dimension.getDimension().getHierarchies().size() == 1 ->
                Optional.of(dimension.getDimension().getHierarchies().getFirst());
            case ResolvedFunCall call when call.getArgCount() == 1
                    && is(call.getOperationAtom(), PlainPropertyOperationAtom.class, "CurrentMember") ->
                currentOf(call.getArg(0));
            case null, default -> Optional.empty();
            };
        }
    }

    private static boolean is(OperationAtom atom, Class<? extends OperationAtom> kind, String... names) {
        if (!kind.isInstance(atom)) {
            return false;
        }
        for (String name : names) {
            if (name.equalsIgnoreCase(atom.name())) {
                return true;
            }
        }
        return false;
    }
}
