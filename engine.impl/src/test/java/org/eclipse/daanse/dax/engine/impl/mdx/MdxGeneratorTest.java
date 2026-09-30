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

import org.eclipse.daanse.dax.engine.api.DaxType;
import org.eclipse.daanse.dax.engine.impl.TestModel;
import org.eclipse.daanse.dax.engine.impl.model.ModelColumn;
import org.eclipse.daanse.dax.engine.impl.model.ModelTable;
import org.eclipse.daanse.dax.engine.impl.model.TabularModel;
import org.eclipse.daanse.dax.engine.impl.mdx.MdxQuery.ValueSource.CellValue;
import org.eclipse.daanse.dax.engine.impl.mdx.MdxQuery.ValueSource.MemberName;
import org.eclipse.daanse.dax.engine.impl.plan.Binder;
import org.eclipse.daanse.dax.engine.impl.plan.Generate;
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
import org.eclipse.daanse.dax.engine.impl.plan.TopN;
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
    void calculateTableFiltersTheMeasuresOfItsIterators() throws Exception {
        assertThat(mdx("EVALUATE CALCULATETABLE(FILTER(VALUES('Markets'[Country]), [Sales] > 1), "
                + "'Product'[Category] = \"Bikes\")"))
                .isEqualTo("SELECT {} ON COLUMNS, Filter([Markets].[Country].Members, [Measures].[Sales] > 1) "
                        + "ON ROWS FROM [C] WHERE Filter([Product].[Category].Members, "
                        + "UCase([Product].CurrentMember.Name) = \"BIKES\")");
        assertThat(mdx("EVALUATE CALCULATETABLE(VALUES('Product'[Subcategory]), 'Product'[Category] = \"Bikes\")"))
                .isEqualTo("SELECT {} ON COLUMNS, Exists([Product].[Subcategory].Members, "
                        + "Filter([Product].[Category].Members, UCase([Product].CurrentMember.Name) = \"BIKES\")) "
                        + "ON ROWS FROM [C]");
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

    @Test
    void rowOfCalculateSumOfColumnByDottedLevel() throws Exception {
        assertThat(mdx("EVALUATE ROW(\"S\", CALCULATE(SUM('Product'[Product.Category])))"))
                .isEqualTo("WITH MEMBER [Measures].[DAX S] AS Sum(Existing [Product].[Category].Members, "
                        + "IIf(IsNumeric([Product].CurrentMember.Name), CDbl([Product].CurrentMember.Name), NULL)) "
                        + "SELECT {[Measures].[DAX S]} ON COLUMNS FROM [C]");
    }

    @Test
    void distinctCountOfColumnByGroups() throws Exception {
        assertThat(mdx("EVALUATE SUMMARIZECOLUMNS('Date'[Year], \"N\", DISTINCTCOUNT('Product'[Subcategory]))"))
                .isEqualTo("WITH MEMBER [Measures].[DAX N] AS IIf(Count(Existing [Product].[Subcategory].Members) = 0, "
                        + "NULL, Count(Existing [Product].[Subcategory].Members)) "
                        + "SELECT {[Measures].[DAX N]} ON COLUMNS, NON EMPTY [Date].[Year].Members ON ROWS FROM [C]");
    }

    @Test
    void generateTopNForEachRow() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE GENERATE(VALUES('Date'[Year]), TOPN(3, VALUES('Markets'[Country]), [Sales]))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.generate("[C]", (Generate) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {} ON COLUMNS, Generate([Date].[Year].Members, "
                + "CrossJoin({[Date].CurrentMember}, TopCount(NonEmpty([Markets].[Country].Members, "
                + "{[Measures].[Sales]}), 3, [Measures].[Sales]))) ON ROWS FROM [C]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new MemberName(1, 1));
        assertThat(plan.evaluates().get(0).table().columns()).extracting(c -> c.name())
                .containsExactly("Date[Year]", "Markets[Country]");
    }

    @Test
    void generateOfFilteredTableAndGroupingWithMeasures() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider().newParser(
                "EVALUATE GENERATE(KEEPFILTERS(FILTER(VALUES('Product'[Category]), 'Product'[Category] = \"Bikes\")), "
                        + "SUMMARIZECOLUMNS('Markets'[Country], 'Date'[Year], \"S\", [Sales]))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.generate("[C]", (Generate) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS, Generate(Filter("
                + "[Product].[Category].Members, UCase([Product].CurrentMember.Name) = \"BIKES\"), "
                + "CrossJoin({[Product].CurrentMember}, NonEmpty(CrossJoin([Markets].[Country].Members, "
                + "[Date].[Year].Members), {[Measures].[Sales]}))) ON ROWS FROM [C]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new MemberName(1, 1),
                new MemberName(2, 1), new CellValue(0));
    }

    @Test
    void generateOfRowComputesItForEachRow() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE GENERATE(VALUES('Product'[Category]), ROW(\"N\", ISBLANK([Sales])))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.generate("[C]", (Generate) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("WITH MEMBER [Measures].[DAX N] AS IsEmpty([Measures].[Sales]) "
                + "SELECT {[Measures].[DAX N]} ON COLUMNS, [Product].[Category].Members ON ROWS FROM [C]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new CellValue(0));
    }

    @Test
    void generateWithoutMeasuresIsASlicerOfItsTuples() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider().newParser("""
                EVALUATE CALCULATETABLE(ROW("S", [Sales]), KEEPFILTERS(GENERATE(KEEPFILTERS(VALUES('Date'[Year])),
                    FILTER(KEEPFILTERS(VALUES('Markets'[Country])), NOT(ISBLANK([Sales]))))))
                """).parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[C]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS FROM [C] WHERE "
                + "Generate([Date].[Year].Members, CrossJoin({[Date].CurrentMember}, "
                + "Filter([Markets].[Country].Members, NOT IsEmpty([Measures].[Sales]))))");
        assertThat(query.sources()).containsExactly(new CellValue(0));
    }

    @Test
    void orFunctionOfMeasuresFiltersTheGroups() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider().newParser("""
                EVALUATE TOPN(501, ADDCOLUMNS(KEEPFILTERS(FILTER(KEEPFILTERS(VALUES('Markets'[Country])),
                        OR(NOT(ISBLANK('Measures'[Sales])), NOT(ISBLANK('Measures'[Unit Sales]))))),
                    "S", 'Measures'[Sales], "U", 'Measures'[Unit Sales]), 'Markets'[Country], 1)
                ORDER BY 'Markets'[Country]
                """).parseDaxStatement());
        TopN topN = (TopN) plan.evaluates().get(0).table();
        MdxQuery query = MdxGenerator.summarize("[C]", (Summarize) topN.source());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales], [Measures].[Unit Sales]} ON COLUMNS, "
                + "Filter([Markets].[Country].Members, NOT IsEmpty([Measures].[Sales]) "
                + "OR NOT IsEmpty([Measures].[Unit Sales])) ON ROWS FROM [C]");
        assertThat(topN.columns()).extracting(c -> c.name()).containsExactly("Markets[Country]", "[S]", "[U]");
    }

    @Test
    void generateOfGenerateGoingDeeperInAHierarchyIsASlicer() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider().newParser("""
                EVALUATE CALCULATETABLE(ROW("S", 'Measures'[Sales], "U", 'Measures'[Unit Sales]),
                    KEEPFILTERS(GENERATE(KEEPFILTERS(GENERATE(KEEPFILTERS(VALUES('Product'[Category])),
                            VALUES('Date'[Year]))),
                        FILTER(KEEPFILTERS(VALUES('Product'[Subcategory])),
                            OR(NOT(ISBLANK('Measures'[Sales])), NOT(ISBLANK('Measures'[Unit Sales])))))))
                """).parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[C]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales], [Measures].[Unit Sales]} ON COLUMNS "
                + "FROM [C] WHERE Generate(Generate([Product].[Category].Members, "
                + "CrossJoin({[Product].CurrentMember}, [Date].[Year].Members)), "
                + "CrossJoin({[Date].CurrentMember}, Filter(Descendants([Product].CurrentMember, "
                + "[Product].[Subcategory]), NOT IsEmpty([Measures].[Sales]) "
                + "OR NOT IsEmpty([Measures].[Unit Sales]))))");
    }

    @Test
    void generateGoingDeeperInAHierarchyReadsTheOuterColumnFromTheAncestor() throws Exception {
        QueryPlan plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE GENERATE(VALUES('Date'[Year]), "
                        + "SUMMARIZECOLUMNS('Product'[Category], 'Product'[Subcategory], \"S\", [Sales]))")
                .parseDaxStatement());
        // not deeper: only other hierarchies of the outer table are its own
        assertThat(plan.evaluates().get(0).table().columns()).extracting(c -> c.name())
                .containsExactly("Date[Year]", "Product[Category]", "Product[Subcategory]", "[S]");
        plan = new Binder(TestModel.model(), Map.of()).bind(new CCCDaxParserProvider()
                .newParser("EVALUATE GENERATE(VALUES('Product'[Category]), "
                        + "SUMMARIZECOLUMNS('Product'[Subcategory], 'Date'[Year], \"S\", [Sales]))")
                .parseDaxStatement());
        MdxQuery query = MdxGenerator.generate("[C]", (Generate) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS, Generate("
                + "[Product].[Category].Members, NonEmpty(CrossJoin(Descendants([Product].CurrentMember, "
                + "[Product].[Subcategory]), [Date].[Year].Members), {[Measures].[Sales]})) ON ROWS FROM [C]");
        assertThat(query.sources()).containsExactly(new MemberName(0, 1), new MemberName(0, 2),
                new MemberName(1, 1), new CellValue(0));
    }

    @Test
    void generatesGoingDeeperLevelByLevel() throws Exception {
        ModelColumn product = new ModelColumn("Product", "Name", "[Product]", "[Product].[Name]", 3, DaxType.STRING);
        TabularModel model = new TabularModel("[Sales]",
                List.of(new ModelTable("Product", List.of(CATEGORY, SUBCATEGORY, product)),
                        new ModelTable("Date", List.of(YEAR))),
                List.of(SALES));
        QueryPlan plan = new Binder(model, Map.of()).bind(new CCCDaxParserProvider().newParser("""
                EVALUATE CALCULATETABLE(ROW("S", 'Measures'[Sales]),
                    KEEPFILTERS(GENERATE(
                        KEEPFILTERS(GENERATE(
                            KEEPFILTERS(GENERATE(KEEPFILTERS(VALUES('Date'[Year])), VALUES('Product'[Category]))),
                            VALUES('Product'[Subcategory]))),
                        FILTER(KEEPFILTERS(VALUES('Product'[Name])), NOT(ISBLANK('Measures'[Sales]))))))
                """).parseDaxStatement());
        MdxQuery query = MdxGenerator.summarize("[C]", (Summarize) plan.evaluates().get(0).table());
        assertThat(query.text()).isEqualTo("SELECT {[Measures].[Sales]} ON COLUMNS FROM [C] WHERE "
                + "Generate(Generate(Generate([Date].[Year].Members, "
                + "CrossJoin({[Date].CurrentMember}, [Product].[Category].Members)), "
                + "CrossJoin({[Date].CurrentMember}, Descendants([Product].CurrentMember, [Product].[Subcategory]))), "
                + "CrossJoin({[Date].CurrentMember}, Filter(Descendants([Product].CurrentMember, [Product].[Name]), "
                + "NOT IsEmpty([Measures].[Sales]))))");
    }
}
