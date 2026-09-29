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
package org.eclipse.daanse.dax.engine.impl.mdx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.dax.engine.impl.TestModel.CATEGORY;
import static org.eclipse.daanse.dax.engine.impl.TestModel.MARKETS;
import static org.eclipse.daanse.dax.engine.impl.TestModel.SALES;
import static org.eclipse.daanse.dax.engine.impl.TestModel.SALES_AMOUNT;
import static org.eclipse.daanse.dax.engine.impl.TestModel.SUBCATEGORY;
import static org.eclipse.daanse.dax.engine.impl.TestModel.UNIT_SALES;
import static org.eclipse.daanse.dax.engine.impl.TestModel.YEAR;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.dax.engine.impl.TestModel;
import org.eclipse.daanse.dax.engine.impl.mdx.MdxQuery.ValueSource.CellValue;
import org.eclipse.daanse.dax.engine.impl.mdx.MdxQuery.ValueSource.MemberName;
import org.eclipse.daanse.dax.engine.impl.plan.Binder;
import org.eclipse.daanse.dax.engine.impl.plan.NamedMeasure;
import org.eclipse.daanse.dax.engine.impl.plan.QueryPlan;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Comparison;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Constant;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.IsBlank;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Logical;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.MeasureValue;
import org.eclipse.daanse.dax.engine.impl.plan.ScalarPlan.Not;
import org.eclipse.daanse.dax.engine.impl.plan.Summarize;
import org.eclipse.daanse.dax.model.api.expression.BooleanExpression.BooleanOperator;
import org.eclipse.daanse.dax.model.api.expression.LogicalExpression.LogicalOperator;
import org.eclipse.daanse.dax.parser.ccc.CCCDaxParserProvider;
import org.junit.jupiter.api.Test;

class MdxGeneratorTest {

    @Test
    void measuresByColumnsOfTwoHierarchies() {
        MdxQuery query = MdxGenerator.summarize("[Sales]", new Summarize(List.of(CATEGORY, YEAR),
                List.of(new NamedMeasure("S", SALES_AMOUNT), new NamedMeasure("U", UNIT_SALES))));
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales Amount], [Measures].[Unit Sales]} ON COLUMNS, "
                + "NON EMPTY CrossJoin([Product].[Category].Members, [Date].[Year].Members) ON ROWS FROM [Sales]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new MemberName(1, 1), new CellValue(0),
                new CellValue(1));
        assertThat(query.rows()).isTrue();
    }

    @Test
    void onlyTheDeepestLevelOfAHierarchyGoesOnTheAxis() {
        MdxQuery query = MdxGenerator.summarize("[Sales]", new Summarize(List.of(CATEGORY, SUBCATEGORY), List.of()));
        assertThat(query.text()).isEqualTo("SELECT {} ON COLUMNS, [Product].[Subcategory].Members ON ROWS FROM [Sales]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new MemberName(0, 2));
    }

    @Test
    void measuresAloneHaveNoRowsAxis() {
        MdxQuery query = MdxGenerator.summarize("[Sales]",
                new Summarize(List.of(), List.of(new NamedMeasure("S", SALES_AMOUNT))));
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales Amount]} ON COLUMNS FROM [Sales]");
        assertThat(query.rows()).isFalse();
    }

    @Test
    void r10FilterBecomesFilterSet() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE FILTER(VALUES('Markets'[Country]), [Sales] > 100000)").parseDaxStatement());
        Summarize summarize = (Summarize) plan.evaluates().get(0).table();
        assertThat(summarize).isEqualTo(new Summarize(List.of(MARKETS), List.of(), Optional.of(
                new Comparison(BooleanOperator.GREATER_THAN, new MeasureValue(SALES), new Constant(100000L)))));

        MdxQuery query = MdxGenerator.summarize("[SteelWheelsSales]", summarize);
        assertThat(query.text()).isEqualTo("SELECT {} ON COLUMNS, "
                + "Filter([Markets].[Country].Members, [Measures].[Sales] > 100000) ON ROWS FROM [SteelWheelsSales]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1));
    }

    @Test
    void topNByMeasureBecomesTopCount() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE TOPN(10, VALUES('Markets'[Country]), [Sales])").parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[SteelWheelsSales]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {} ON COLUMNS, "
                + "TopCount(NonEmpty([Markets].[Country].Members, {[Measures].[Sales]}), 10, [Measures].[Sales]) ON ROWS "
                + "FROM [SteelWheelsSales]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1));
    }

    @Test
    void ascendingTopAfterTheConditionWithMeasures() {
        MdxQuery query = MdxGenerator.summarize("[Sales]", new Summarize(List.of(CATEGORY),
                List.of(new NamedMeasure("U", UNIT_SALES)),
                Optional.of(new Comparison(BooleanOperator.GREATER_THAN, new MeasureValue(SALES), new Constant(1L))),
                Optional.of(new Summarize.Top(3, SALES_AMOUNT, true))));
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Unit Sales]} ON COLUMNS, NON EMPTY BottomCount(NonEmpty("
                + "Filter([Product].[Category].Members, [Measures].[Sales] > 1), {[Measures].[Sales Amount]}), 3, "
                + "[Measures].[Sales Amount]) ON ROWS FROM [Sales]");
    }

    @Test
    void isBlankOfMeasureIsIsEmptyOfCalculatedMember() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider().newParser(
                "EVALUATE SUMMARIZECOLUMNS('Markets'[Country], \"S\", [Sales], \"No Sales\", ISBLANK([Sales]))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[SteelWheelsSales]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("WITH MEMBER [Measures].[DAX No Sales] AS IsEmpty([Measures].[Sales]) "
                + "SELECT {[Measures].[Sales], [Measures].[DAX No Sales]} ON COLUMNS, "
                + "NON EMPTY [Markets].[Country].Members ON ROWS FROM [SteelWheelsSales]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new CellValue(0), new CellValue(1));
    }

    @Test
    void rowOfIsBlankOfMeasures() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE ROW(\"a\", ISBLANK([Sales]), \"b\", NOT(ISBLANK('Measures'[Unit Sales])))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[Sales]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("WITH MEMBER [Measures].[DAX a] AS IsEmpty([Measures].[Sales]) "
                + "MEMBER [Measures].[DAX b] AS NOT IsEmpty([Measures].[Unit Sales]) "
                + "SELECT {[Measures].[DAX a], [Measures].[DAX b]} ON COLUMNS FROM [Sales]");
        assertThat(query.rows()).isFalse();
    }

    private static String mdx(String dax) throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of())
                .bind(new CCCDaxParserProvider().newParser(dax).parseDaxStatement());
        return MdxGenerator.summarize("[C]", (Summarize) plan.evaluates().get(0).table()).text();
    }

    @Test
    void filterTableOnOtherHierarchyIsTheSlicer() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Product'[Category], KEEPFILTERS(FILTER(VALUES('Markets'[Country]), "
                + "'Markets'[Country] IN {\"USA\", \"France\"})), \"S\", [Sales])"))
                .isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS, NON EMPTY [Product].[Category].Members ON ROWS FROM [C] "
                        + "WHERE Filter([Markets].[Country].Members, (UCase([Markets].CurrentMember.Name) = \"USA\" "
                        + "OR UCase([Markets].CurrentMember.Name) = \"FRANCE\"))");
    }

    @Test
    void filterTableOnHierarchyGroupedByKeepsTheRows() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Product'[Subcategory], "
                + "FILTER(VALUES('Product'[Category]), 'Product'[Category] = \"Bikes\"), \"S\", [Sales Amount])"))
                .isEqualTo("SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY Exists([Product].[Subcategory].Members, "
                        + "Filter([Product].[Category].Members, UCase([Product].CurrentMember.Name) = \"BIKES\")) "
                        + "ON ROWS FROM [C]");
    }

    @Test
    void filterTablesByMeasuresAndAncestors() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Date'[Year], TOPN(3, VALUES('Markets'[Country]), [Sales]), "
                + "KEEPFILTERS(FILTER('Product', 'Product'[Category] = \"Bikes\" && \"Road\" <> 'Product'[Subcategory])), "
                + "\"S\", [Sales Amount])"))
                .isEqualTo("SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY [Date].[Year].Members ON ROWS FROM [C] "
                        + "WHERE CrossJoin(TopCount(NonEmpty([Markets].[Country].Members, {[Measures].[Sales]}), 3, "
                        + "[Measures].[Sales]), Filter([Product].[Subcategory].Members, "
                        + "(UCase(Ancestor([Product].CurrentMember, [Product].[Category]).Name) = \"BIKES\") "
                        + "AND (\"ROAD\" <> UCase([Product].CurrentMember.Name))))");
    }

    @Test
    void filterTableByMeasureOrText() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Product'[Category], KEEPFILTERS(FILTER(VALUES('Markets'[Country]), "
                + "'Markets'[Country] = \"USA\" || [Sales] > 100)), \"S\", [Sales Amount])"))
                .isEqualTo("SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY [Product].[Category].Members "
                        + "ON ROWS FROM [C] WHERE Filter([Markets].[Country].Members, "
                        + "(UCase([Markets].CurrentMember.Name) = \"USA\") OR ([Measures].[Sales] > 100))");
    }

    @Test
    void filterTableOfAllValuesFiltersNothing() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Product'[Category], KEEPFILTERS(VALUES('Markets'[Country])), "
                + "'Markets', \"S\", [Sales])"))
                .isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS, NON EMPTY [Product].[Category].Members ON ROWS FROM [C]");
    }

    @Test
    void addedMeasuresKeepTheRows() throws Exception {
        assertThat(mdx("EVALUATE ADDCOLUMNS(VALUES('Markets'[Country]), \"S\", [Sales])"))
                .isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS, [Markets].[Country].Members ON ROWS FROM [C]");
        assertThat(mdx("EVALUATE ADDCOLUMNS(SUMMARIZECOLUMNS('Markets'[Country], \"S\", [Sales]), "
                + "\"Empty\", ISBLANK([Unit Sales]))"))
                .isEqualTo("WITH MEMBER [Measures].[DAX Empty] AS IsEmpty([Measures].[Unit Sales]) "
                        + "SELECT {[Measures].[Sales], [Measures].[DAX Empty]} ON COLUMNS, "
                        + "NonEmpty([Markets].[Country].Members, {[Measures].[Sales]}) ON ROWS FROM [C]");
    }

    @Test
    void conditionOfSeveralParts() {
        ScalarPlan condition = new Logical(LogicalOperator.OR,
                new Not(new IsBlank(new MeasureValue(UNIT_SALES))),
                new Logical(LogicalOperator.AND,
                        new Comparison(BooleanOperator.LESS_THAN_OR_EQUAL, new MeasureValue(SALES), new Constant(2.5)),
                        new Comparison(BooleanOperator.NOT_EQUAL, new Constant("a\"b"), new Constant(true))));
        assertThat(MdxGenerator.condition(condition)).isEqualTo("NOT IsEmpty([Measures].[Unit Sales]) OR "
                + "(([Measures].[Sales] <= 2.5) AND (\"a\"\"b\" <> TRUE))");
    }

    @Test
    void quotesNames() {
        assertThat(MdxNames.quote("a]b")).isEqualTo("[a]]b]");
    }
}
