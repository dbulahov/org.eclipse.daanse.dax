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
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.ParenthesesFunction;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.NumericLiteral;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;

/**
 * Decomposes a calculated measure into the stored measures it is computed of,
 * the atoms: each of the measure's members or of another member of some
 * hierarchies, as {@code ([Profit], [Time].[Time].PrevMember)} gives it.
 * <p>
 * Where the measure is not BLANK is a formula of where the atoms are not,
 * true only where it is not: {@code a + b}, {@code a - b} and
 * {@code CoalesceEmpty(a, b)} where one of them is, {@code a * b} and
 * {@code a / b} where both are (BLANK of a denominator alone gives infinity
 * or BLANK, as the cube is set). If that is true where any atom is not BLANK,
 * a set of members of a hierarchy the atoms are not taken of other members of
 * that keeps those where the measure is not BLANK keeps where the atoms are
 * not: the measure is the same of it as of all members.
 * </p>
 * <p>
 * A constant, {@code IIf}, a measure of another cube and any other
 * expression is not decomposed.
 * </p>
 */
final class MeasureDecomposition {

    private static final Set<String> NAVIGATIONS = Set.of("CurrentMember", "PrevMember", "NextMember", "Parent",
            "FirstChild", "LastChild", "FirstSibling", "LastSibling");

    /** Where a measure is not BLANK, of where its atoms are not. */
    private sealed interface Support {

        boolean holds(Set<String> atoms);
    }

    private record Atom(String key) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return atoms.contains(key);
        }
    }

    private record Any(List<Support> supports) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return supports.stream().anyMatch(s -> s.holds(atoms));
        }
    }

    private record All(List<Support> supports) implements Support {

        @Override
        public boolean holds(Set<String> atoms) {
            return supports.stream().allMatch(s -> s.holds(atoms));
        }
    }

    private final Map<String, Member> cubeMeasures;
    private final Set<String> visiting = new HashSet<>();
    private final Set<String> atoms = new LinkedHashSet<>();
    private final Set<String> hierarchies = new TreeSet<>();

    private MeasureDecomposition(Map<String, Member> cubeMeasures) {
        this.cubeMeasures = cubeMeasures;
    }

    /**
     * @param measure      a calculated measure
     * @param cubeMeasures the measures of the cube, by unique name
     * @return the unique names of the hierarchies of whose other members the
     *         measure takes the stored measures it is computed of, if it is
     *         not BLANK where one of them is not; empty if it is another
     */
    static Optional<Set<String>> shiftedHierarchies(Member measure, Map<String, Member> cubeMeasures) {
        MeasureDecomposition decomposition = new MeasureDecomposition(cubeMeasures);
        return decomposition.measure(measure, List.of())
                .filter(support -> !decomposition.atoms.isEmpty()
                        && decomposition.atoms.stream().allMatch(atom -> support.holds(Set.of(atom))))
                .map(support -> decomposition.hierarchies);
    }

    /**
     * @param shift the coordinates of the other members it is taken of
     */
    private Optional<Support> measure(Member measure, List<String> shift) {
        if (!measure.isCalculated()) {
            String atom = measure.getUniqueName() + shift;
            atoms.add(atom);
            return Optional.of(new Atom(atom));
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

    private Optional<Support> expression(Expression expression, List<String> shift) {
        return switch (expression) {
        case MemberExpression member when member.getMember().isMeasure()
                && cubeMeasures.containsKey(member.getMember().getUniqueName()) ->
            measure(cubeMeasures.get(member.getMember().getUniqueName()), shift);
        case ResolvedFunCall call -> call(call, shift);
        case null, default -> Optional.empty();
        };
    }

    private Optional<Support> call(ResolvedFunCall call, List<String> shift) {
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

    private Optional<List<Support>> arguments(ResolvedFunCall call, List<String> shift) {
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
    private Optional<Support> tuple(ResolvedFunCall tuple, List<String> shift) {
        Expression measure = null;
        List<String> shifted = new ArrayList<>(shift);
        for (Expression argument : tuple.getArgs()) {
            if (argument instanceof MemberExpression member && member.getMember().isMeasure()) {
                if (measure != null) {
                    return Optional.empty();
                }
                measure = argument;
                continue;
            }
            Optional<String> coordinate = coordinate(argument);
            if (coordinate.isEmpty()) {
                return Optional.empty();
            }
            shifted.add(coordinate.get());
        }
        return measure == null ? Optional.empty() : expression(measure, shifted);
    }

    /**
     * @return a member of a hierarchy, given or navigated to of the current
     *         one, as {@code [Time].[Time].CurrentMember.PrevMember}; empty if
     *         it is another expression
     */
    private Optional<String> coordinate(Expression expression) {
        return switch (expression) {
        case MemberExpression member when !member.getMember().isMeasure() -> {
            hierarchies.add(member.getMember().getHierarchy().getUniqueName());
            yield Optional.of(member.getMember().getUniqueName());
        }
        case HierarchyExpression hierarchy when !hierarchy.getHierarchy().getDimension().isMeasures() -> {
            hierarchies.add(hierarchy.getHierarchy().getUniqueName());
            yield Optional.of(hierarchy.getHierarchy().getUniqueName() + ".CurrentMember");
        }
        case ResolvedFunCall call when call.getArgCount() == 1
                && is(call.getOperationAtom(), PlainPropertyOperationAtom.class, NAVIGATIONS.toArray(String[]::new)) ->
            coordinate(call.getArg(0)).map(c -> c + "." + call.getOperationAtom().name());
        case ResolvedFunCall call when call.getArgCount() == 2
                && is(call.getOperationAtom(), MethodOperationAtom.class, "Lag", "Lead")
                && call.getArg(1) instanceof NumericLiteral offset ->
            coordinate(call.getArg(0)).map(c -> c + "." + call.getOperationAtom().name() + "("
                    + offset.getValue().stripTrailingZeros().toPlainString() + ")");
        case null, default -> Optional.empty();
        };
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
