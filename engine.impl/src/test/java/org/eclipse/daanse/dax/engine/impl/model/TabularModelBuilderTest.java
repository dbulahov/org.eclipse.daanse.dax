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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.dimension;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.hierarchy;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.level;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.measure;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.property;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.dax.engine.api.DaxType;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.element.StoredMeasure;
import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.ParenthesesFunction;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.NumericLiteral;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.junit.jupiter.api.Test;

class TabularModelBuilderTest {

    @Test
    void dimensionsBecomeTablesAndLevelsColumns() {
        CatalogReader reader = mock(CatalogReader.class);
        Cube cube = mock(Cube.class);
        when(cube.getName()).thenReturn("Sales");

        Dimension measures = dimension("Measures", true);
        Dimension product = dimension("Product", false);
        Dimension hidden = dimension("Hidden", false);
        when(hidden.isVisible()).thenReturn(false);
        when(reader.getCubeDimensions(cube)).thenReturn(List.of(measures, product, hidden));

        Hierarchy main = hierarchy("ProductHierarchy", "[Product].[ProductHierarchy]");
        Hierarchy alternative = hierarchy("Alt", "[Product].[Alt]");
        when(reader.getDimensionHierarchies(product)).thenReturn(List.of(main, alternative));
        List<Level> mainLevels = List.of(level("(All)", "[Product].[ProductHierarchy].[(All)]", 0),
                level("Category", "[Product].[ProductHierarchy].[Category]", 1), level("Subcategory", "[Product].[ProductHierarchy].[Subcategory]", 2));
        List<Level> alternativeLevels = List.of(level("Category", "[Product].[Alt].[Category]", 1));
        when(reader.getHierarchyLevels(main)).thenReturn(mainLevels);
        when(reader.getHierarchyLevels(alternative)).thenReturn(alternativeLevels);

        Member sales = measure("Sales Amount");
        Member secret = measure("Secret");
        when(secret.isVisible()).thenReturn(false);
        when(cube.getMeasures()).thenReturn(List.of(sales, secret));

        TabularModel model = TabularModelBuilder.build(reader, cube);

        assertThat(model.cube()).isEqualTo("[Sales]");
        assertThat(model.tables()).containsOnlyKeys("Product");
        assertThat(model.table("PRODUCT").orElseThrow().columns()).containsExactly(
                new ModelColumn("Product", "Product.ProductHierarchy.Category", "[Product].[ProductHierarchy]",
                        "[Product].[ProductHierarchy].[Category]", 1,
                        DaxType.STRING),
                new ModelColumn("Product", "Product.ProductHierarchy.Subcategory", "[Product].[ProductHierarchy]",
                        "[Product].[ProductHierarchy].[Subcategory]", 2,
                        DaxType.STRING),
                new ModelColumn("Product", "Product.Alt.Category", "[Product].[Alt]", "[Product].[Alt].[Category]", 1,
                        DaxType.STRING));
        assertThat(model.measures()).containsOnlyKeys("Sales Amount");
        assertThat(model.measure("sales amount")).contains(new ModelMeasure("Sales Amount", "[Measures].[Sales Amount]", true, DaxType.DECIMAL));
    }

    @Test
    void columnIsFoundByDottedLevelUniqueName() {
        ModelColumn category = new ModelColumn("Product", "Category", "[Product].[Product]",
                "[Product].[Product].[Category]", 1, DaxType.STRING);
        ModelTable table = new ModelTable("Product", List.of(category));

        assertThat(table.column("Product.Product.Category")).contains(category);
        assertThat(table.column("product.PRODUCT.category")).contains(category);
        assertThat(table.column("Category")).contains(category);
        assertThat(table.column("Product.Product.Subcategory")).isEmpty();
    }

    @Test
    void levelPropertiesAreColumnsAfterTheirLevel() {
        CatalogReader reader = mock(CatalogReader.class);
        Cube cube = mock(Cube.class);
        when(cube.getName()).thenReturn("Sales");
        Dimension customers = dimension("Customers", false);
        when(reader.getCubeDimensions(cube)).thenReturn(List.of(customers));
        Hierarchy hierarchy = hierarchy("Customers", "[Customers].[Customers]");
        when(reader.getDimensionHierarchies(customers)).thenReturn(List.of(hierarchy));
        Level city = level("City", "[Customers].[Customers].[City]", 1);
        Level name = level("Name", "[Customers].[Customers].[Name]", 2);
        // internal ones are left out; those of the same name of two levels and one named as a level are not
        Property[] cityProperties = { property("Population", false, Property.Datatype.TYPE_INTEGER),
                property("Area", false, Property.Datatype.TYPE_NUMERIC), property("$key", false),
                property("Key", true), property("Code", false) };
        Property[] nameProperties = { property("Gender", false), property("Code", false), property("City", false) };
        when(city.getProperties()).thenReturn(cityProperties);
        when(name.getProperties()).thenReturn(nameProperties);
        when(reader.getHierarchyLevels(hierarchy)).thenReturn(List.of(city, name));
        when(cube.getMeasures()).thenReturn(List.of());

        TabularModel model = TabularModelBuilder.build(reader, cube);

        assertThat(model.table("Customers").orElseThrow().columns()).containsExactly(
                new ModelColumn("Customers", "Customers.Customers.City", "[Customers].[Customers]",
                        "[Customers].[Customers].[City]", 1, DaxType.STRING),
                new ModelColumn("Customers", "Customers.Customers.City.Population", "[Customers].[Customers]",
                        "[Customers].[Customers].[City]", 1, DaxType.INTEGER, Optional.of("Population")),
                new ModelColumn("Customers", "Customers.Customers.City.Area", "[Customers].[Customers]",
                        "[Customers].[Customers].[City]", 1, DaxType.DOUBLE, Optional.of("Area")),
                new ModelColumn("Customers", "Customers.Customers.City.Code", "[Customers].[Customers]",
                        "[Customers].[Customers].[City]", 1, DaxType.STRING, Optional.of("Code")),
                new ModelColumn("Customers", "Customers.Customers.Name", "[Customers].[Customers]",
                        "[Customers].[Customers].[Name]", 2, DaxType.STRING),
                new ModelColumn("Customers", "Customers.Customers.Name.Gender", "[Customers].[Customers]",
                        "[Customers].[Customers].[Name]", 2, DaxType.STRING, Optional.of("Gender")),
                new ModelColumn("Customers", "Customers.Customers.Name.Code", "[Customers].[Customers]",
                        "[Customers].[Customers].[Name]", 2, DaxType.STRING, Optional.of("Code")),
                new ModelColumn("Customers", "Customers.Customers.Name.City", "[Customers].[Customers]",
                        "[Customers].[Customers].[Name]", 2, DaxType.STRING, Optional.of("City")));
        ModelTable table = model.table("Customers").orElseThrow();
        assertThat(table.column("customers.customers.name.gender")).map(ModelColumn::property)
                .contains(Optional.of("Gender"));
        assertThat(table.column("Customers.Customers.City")).map(ModelColumn::property).contains(Optional.empty());
    }

    @Test
    void measuresAreTypedAsCsdlDeclaresThem() {
        assertThat(TabularModelBuilder.measureType(stored("sum", Optional.of(DataTypeJdbc.INTEGER))))
                .isEqualTo(DaxType.DECIMAL);
        assertThat(TabularModelBuilder.measureType(stored("count", Optional.empty()))).isEqualTo(DaxType.INTEGER);
        assertThat(TabularModelBuilder.measureType(stored("avg", Optional.of(DataTypeJdbc.INTEGER))))
                .isEqualTo(DaxType.DOUBLE);
        assertThat(TabularModelBuilder.measureType(stored("max", Optional.of(DataTypeJdbc.INTEGER))))
                .isEqualTo(DaxType.INTEGER);
        assertThat(TabularModelBuilder.measureType(measure("Calculated"))).isEqualTo(DaxType.DECIMAL);
    }

    private static StoredMeasure stored(String aggregator, Optional<DataTypeJdbc> dataType) {
        StoredMeasure measure = mock(StoredMeasure.class);
        when(measure.getAggregateFunction()).thenReturn(aggregator);
        when(measure.getDataType()).thenReturn(dataType);
        return measure;
    }

    @Test
    void sumsAndDifferencesOfStoredMeasuresAreNotBlankWhereOneOfThemIsNot() {
        Member a = storedMeasure("A");
        Member b = storedMeasure("B");
        Member c = storedMeasure("C");
        Member profit = calculated("Profit", infix("-", member(a), member(b)));
        Map<String, Member> cube = cube(a, b, c, profit);

        assertNotBlankWhereEach(decomposed(profit, cube), "[Measures].[A]", "[Measures].[B]");
        assertNotBlankWhereEach(decomposed(calculated("S", infix("-", infix("+", member(a), member(b)), member(c))),
                cube), "[Measures].[A]", "[Measures].[B]", "[Measures].[C]");
        assertNotBlankWhereEach(decomposed(calculated("Negated", call(new PrefixOperationAtom("-"),
                mock(FunctionDefinition.class), member(a))), cube), "[Measures].[A]");
        assertNotBlankWhereEach(decomposed(calculated("Parenthesized", call(new ParenthesesOperationAtom(),
                mock(ParenthesesFunction.class), infix("+", member(a), member(b)))), cube),
                "[Measures].[A]", "[Measures].[B]");
        // of another decomposed one
        assertNotBlankWhereEach(decomposed(calculated("Total", infix("+", member(profit), member(c))), cube),
                "[Measures].[A]", "[Measures].[B]", "[Measures].[C]");
        assertNotBlankWhereEach(decomposed(calculated("First", call(new FunctionOperationAtom("CoalesceEmpty"),
                mock(FunctionDefinition.class), member(a), member(b))), cube), "[Measures].[A]", "[Measures].[B]");
    }

    @Test
    void productsAndQuotientsAreBlankWhereOneOfThemIs() {
        Member a = storedMeasure("A");
        Member b = storedMeasure("B");
        Map<String, Member> cube = cube(a, b);

        for (String operator : new String[] { "/", "*" }) {
            MeasureDecomposition decomposition = decomposed(calculated("M", infix(operator, member(a), member(b))),
                    cube).orElseThrow();
            assertThat(decomposition.atoms()).containsExactlyInAnyOrder("[Measures].[A]", "[Measures].[B]");
            assertThat(decomposition.notBlankWhere("[Measures].[A]")).isFalse();
            assertThat(decomposition.support().holds(Set.of("[Measures].[A]", "[Measures].[B]"))).isTrue();
        }
    }

    @Test
    void otherMeasuresAreNotDecomposed() {
        Member a = storedMeasure("A");
        Member b = storedMeasure("B");
        Member c = storedMeasure("C");
        Map<String, Member> cube = cube(a, b, c);

        // of a constant or a condition
        Expression one = mock(Expression.class);
        assertThat(decomposed(calculated("Plus one", infix("+", member(a), one)), cube)).isEmpty();
        assertThat(decomposed(calculated("Conditional", call(new FunctionOperationAtom("IIf"),
                mock(FunctionDefinition.class), member(a), member(b), member(c))), cube)).isEmpty();
        // of a measure of another cube
        assertThat(decomposed(calculated("Foreign", infix("+", member(a), member(storedMeasure("Other")))), cube))
                .isEmpty();
        // of itself
        Member cyclic = measure("Cyclic");
        when(cyclic.isCalculated()).thenReturn(true);
        ResolvedFunCall itself = infix("+", member(a), member(cyclic));
        when(cyclic.getExpression()).thenReturn(itself);
        assertThat(decomposed(cyclic, cube(a, cyclic))).isEmpty();
    }

    @Test
    void measuresOfOtherMembersAreDecomposedWithTheirHierarchies() {
        Member sales = storedMeasure("Store Sales");
        Member cost = storedMeasure("Store Cost");
        Member profit = calculated("Profit", infix("-", member(sales), member(cost)));
        Hierarchy time = hierarchy("Time", "[Time].[Time]");
        Dimension timeDimension = dimension("Time", false);
        when(time.getDimension()).thenReturn(timeDimension);
        Expression previous = navigation("PrevMember", navigation("CurrentMember", current(time)));
        Member lastPeriod = calculated("Profit last Period", call(new FunctionOperationAtom("CoalesceEmpty"),
                mock(FunctionDefinition.class), tuple(member(profit), previous), member(profit)));
        Member growth = calculated("Profit Growth",
                infix("/", infix("-", member(profit), member(lastPeriod)), member(lastPeriod)));
        Map<String, Member> cube = cube(sales, cost, profit, lastPeriod, growth);

        // BLANK where Profit is, of the current and of the previous period
        MeasureDecomposition decomposition = decomposed(growth, cube).orElseThrow();
        assertNotBlankWhereEach(Optional.of(decomposition), "[Measures].[Store Sales]", "[Measures].[Store Cost]",
                "[Measures].[Store Sales] at [Time].[Time].PrevMember",
                "[Measures].[Store Cost] at [Time].[Time].PrevMember");
        assertThat(decomposition.shiftedHierarchies()).containsExactly("[Time].[Time]");
        assertThat(decomposed(calculated("Lagged", infix("+", member(profit), tuple(member(profit),
                call(new MethodOperationAtom("Lag"), mock(FunctionDefinition.class), current(time), number(2))))),
                cube).orElseThrow().atoms()).contains("[Measures].[Store Sales] at [Time].[Time].Lag(2)");
        // not BLANK where Profit of the previous period alone is not
        assertThat(decomposed(calculated("Share", infix("/", member(profit), tuple(member(profit), previous))), cube)
                .orElseThrow().notBlankWhere("[Measures].[Store Sales] at [Time].[Time].PrevMember")).isFalse();
        // of another expression of a member
        assertThat(decomposed(calculated("Other", infix("+", member(profit), tuple(member(profit),
                call(new FunctionOperationAtom("StrToMember"), mock(FunctionDefinition.class), current(time))))),
                cube)).isEmpty();
    }

    @Test
    void measuresOfTheDataMemberOfTheCurrentMemberAreOfItsOwnRows() {
        Member salary = storedMeasure("Org Salary");
        Hierarchy employees = hierarchy("Employees", "[Employees].[Employees]");
        Dimension dimension = dimension("Employees", false);
        when(employees.getDimension()).thenReturn(dimension);
        when(employees.hasAll()).thenReturn(true);
        Member employeeSalary = calculated("Employee Salary", tuple(
                navigation("DataMember", navigation("CurrentMember", current(employees))), member(salary)));

        // where Org Salary is BLANK so is it, not where it is not
        MeasureDecomposition decomposition = decomposed(employeeSalary, cube(salary, employeeSalary)).orElseThrow();
        assertThat(decomposition.atoms()).containsExactly("[Measures].[Org Salary]");
        assertThat(decomposition.notBlankWhere("[Measures].[Org Salary]")).isFalse();
        assertThat(decomposition.shiftedHierarchies()).isEmpty();
        assertThat(decomposition.currentHierarchies()).containsExactly("[Employees].[Employees]");
        // the data member of a member of a hierarchy without all member is of some rows
        when(employees.hasAll()).thenReturn(false);
        assertThat(decomposed(employeeSalary, cube(salary, employeeSalary))).isEmpty();
    }

    private static void assertNotBlankWhereEach(Optional<MeasureDecomposition> decomposition, String... atoms) {
        assertThat(decomposition).isPresent();
        assertThat(decomposition.get().atoms()).containsExactlyInAnyOrder(atoms);
        for (String atom : atoms) {
            assertThat(decomposition.get().notBlankWhere(atom)).as(atom).isTrue();
        }
    }

    private static Optional<MeasureDecomposition> decomposed(Member measure, Map<String, Member> cube) {
        return MeasureDecomposition.of(measure, cube);
    }

    private static Map<String, Member> cube(Member... measures) {
        Map<String, Member> cube = new HashMap<>();
        for (Member measure : measures) {
            cube.put(measure.getUniqueName(), measure);
        }
        return cube;
    }

    private static Member storedMeasure(String name) {
        Member member = measure(name);
        when(member.isMeasure()).thenReturn(true);
        return member;
    }

    private static Member calculated(String name, Expression expression) {
        Member member = storedMeasure(name);
        when(member.isCalculated()).thenReturn(true);
        when(member.getExpression()).thenReturn(expression);
        return member;
    }

    private static MemberExpression member(Member member) {
        when(member.isMeasure()).thenReturn(true);
        MemberExpression expression = mock(MemberExpression.class);
        when(expression.getMember()).thenReturn(member);
        return expression;
    }

    private static Expression current(Hierarchy hierarchy) {
        HierarchyExpression expression = mock(HierarchyExpression.class,
                withSettings().extraInterfaces(Expression.class));
        when(expression.getHierarchy()).thenReturn(hierarchy);
        return (Expression) expression;
    }

    private static Expression number(int value) {
        NumericLiteral literal = mock(NumericLiteral.class, withSettings().extraInterfaces(Expression.class));
        when(literal.getValue()).thenReturn(BigDecimal.valueOf(value));
        return (Expression) literal;
    }

    private static ResolvedFunCall navigation(String name, Expression of) {
        return call(new PlainPropertyOperationAtom(name), mock(FunctionDefinition.class), of);
    }

    private static ResolvedFunCall tuple(Expression... members) {
        return call(new ParenthesesOperationAtom(), mock(FunctionDefinition.class), members);
    }

    private static ResolvedFunCall infix(String operator, Expression left, Expression right) {
        return call(new InfixOperationAtom(operator), mock(FunctionDefinition.class), left, right);
    }

    private static ResolvedFunCall call(OperationAtom atom, FunctionDefinition function, Expression... args) {
        ResolvedFunCall call = mock(ResolvedFunCall.class);
        when(call.getOperationAtom()).thenReturn(atom);
        when(call.getFunDef()).thenReturn(function);
        when(call.getArgCount()).thenReturn(args.length);
        when(call.getArgs()).thenReturn(args);
        for (int i = 0; i < args.length; i++) {
            when(call.getArg(i)).thenReturn(args[i]);
        }
        return call;
    }
}
