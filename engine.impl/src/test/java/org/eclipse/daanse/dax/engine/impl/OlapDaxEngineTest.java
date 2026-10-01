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
package org.eclipse.daanse.dax.engine.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.cellSet;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.dimension;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.hierarchy;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.level;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.measure;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.member;
import static org.eclipse.daanse.dax.engine.impl.OlapMocks.property;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.dax.engine.api.DaxCancelledException;
import org.eclipse.daanse.dax.engine.api.DaxColumn;
import org.eclipse.daanse.dax.engine.api.DaxQueryStatement;
import org.eclipse.daanse.dax.engine.api.DaxResult;
import org.eclipse.daanse.dax.engine.api.DaxSemanticException;
import org.eclipse.daanse.dax.engine.api.DaxSyntaxException;
import org.eclipse.daanse.dax.engine.api.DaxTable;
import org.eclipse.daanse.dax.parser.ccc.CCCDaxParserProvider;
import org.eclipse.daanse.olap.api.catalog.CatalogReader;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.execution.Statement;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OlapDaxEngineTest {

    private final OlapDaxEngine engine = new OlapDaxEngine(new CCCDaxParserProvider());
    private final Connection connection = mock(Connection.class);
    private final CatalogReader reader = mock(CatalogReader.class);
    private final Cube cube = mock(Cube.class);
    private final Statement olapStatement = mock(Statement.class);
    private final Level category = level("Category", "[Product].[ProductHierarchy].[Category]", 1);
    private final Level subcategory = level("Subcategory", "[Product].[ProductHierarchy].[Subcategory]", 2);

    @BeforeEach
    void catalog() {
        when(connection.getCatalogReader()).thenReturn(reader);
        when(connection.createStatement()).thenReturn(olapStatement);
        when(reader.getCubes()).thenReturn(List.of(cube));
        when(cube.getName()).thenReturn("Sales");
        Dimension product = dimension("Product", false);
        Dimension measures = dimension("Measures", true);
        when(reader.getCubeDimensions(cube)).thenReturn(List.of(measures, product));
        Hierarchy hierarchy = hierarchy("ProductHierarchy", "[Product].[ProductHierarchy]");
        when(reader.getDimensionHierarchies(product)).thenReturn(List.of(hierarchy));
        when(reader.getHierarchyLevels(hierarchy)).thenReturn(List.of(category, subcategory));
        Member salesAmount = measure("Sales Amount");
        when(cube.getMeasures()).thenReturn(List.of(salesAmount));
    }

    @AfterEach
    void close() {
        engine.close();
    }

    private List<List<Object>> rows(DaxTable table) throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        while (table.next()) {
            List<Object> row = new ArrayList<>();
            for (int c = 0; c < table.columns().size(); c++) {
                row.add(table.getObject(c));
            }
            rows.add(row);
        }
        return rows;
    }

    @Test
    void executesSeveralEvaluatesThroughMdx() throws Exception {
        Member bikes = member("Bikes", category, null);
        Member clothes = member("Clothes", category, null);
        List<List<Member>> positions = List.of(List.of(member("Road", subcategory, bikes)),
                List.of(member("Mountain", subcategory, bikes)), List.of(member("Caps", subcategory, clothes)));
        CellSet cellSet = cellSet(positions, new Object[][] { { 10.0 }, { 30 }, { 20.5 } });
        when(olapStatement.executeQuery(
                "SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY [Product].[ProductHierarchy].[Subcategory].Members ON ROWS FROM [Sales]"))
                .thenReturn(cellSet);

        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Category], 'Product'[Product.ProductHierarchy.Subcategory], "Sales", [Sales Amount])
                        ORDER BY [Sales] DESC
                        EVALUATE {1}
                        """)) {
            DaxTable first = result.nextTable();
            assertThat(first.columns()).extracting(DaxColumn::name)
                    .containsExactly("Product[Product.ProductHierarchy.Category]", "Product[Product.ProductHierarchy.Subcategory]", "[Sales]");
            assertThat(rows(first)).containsExactly(List.of("Bikes", "Mountain", 30L), List.of("Clothes", "Caps", 20.5),
                    List.of("Bikes", "Road", 10.0));

            DaxTable second = result.nextTable();
            assertThat(rows(second)).containsExactly(List.of(1L));
            assertThatIllegalStateException().isThrownBy(first::next);

            assertThat(result.nextTable()).isNull();
        }
        verify(olapStatement).close();
    }

    @Test
    void readsLevelPropertiesAndMakesTheirValuesDistinct() throws Exception {
        Property[] properties = { property("Color", false) };
        when(subcategory.getProperties()).thenReturn(properties);
        Member bikes = member("Bikes", category, null);
        Member clothes = member("Clothes", category, null);
        Member road = member("Road", subcategory, bikes);
        Member mountain = member("Mountain", subcategory, bikes);
        Member caps = member("Caps", subcategory, clothes);
        when(road.getPropertyValue("Color")).thenReturn("Red");
        when(mountain.getPropertyValue("Color")).thenReturn("Red");
        when(caps.getPropertyValue("Color")).thenReturn("Blue");
        CellSet cellSet = cellSet(List.of(List.of(road), List.of(mountain), List.of(caps)),
                new Object[][] { {}, {}, {} });
        when(olapStatement.executeQuery("SELECT {} ON COLUMNS, Filter([Product].[ProductHierarchy].[Subcategory].Members, "
                + "NOT IsEmpty([Measures].[Sales Amount])) ON ROWS FROM [Sales]")).thenReturn(cellSet);

        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE FILTER(KEEPFILTERS(VALUES('Product'[Product.ProductHierarchy.Subcategory.Color])),
                            NOT(ISBLANK('Measures'[Sales Amount])))
                        ORDER BY 'Product'[Product.ProductHierarchy.Subcategory.Color]
                        """)) {
            DaxTable table = result.nextTable();
            assertThat(table.columns()).extracting(DaxColumn::name)
                    .containsExactly("Product[Product.ProductHierarchy.Subcategory.Color]");
            assertThat(rows(table)).containsExactly(List.of("Blue"), List.of("Red"));
        }
    }

    @Test
    void computesMeasuresByPropertyValuesOverTheirMembers() throws Exception {
        Property[] properties = { property("Color", false) };
        when(subcategory.getProperties()).thenReturn(properties);
        Member bikes = member("Bikes", category, null);
        Member clothes = member("Clothes", category, null);
        Member road = member("Road", subcategory, bikes);
        Member mountain = member("Mountain", subcategory, bikes);
        Member caps = member("Caps", subcategory, clothes);
        when(road.getPropertyValue("Color")).thenReturn("Red");
        when(mountain.getPropertyValue("Color")).thenReturn("Red");
        when(caps.getPropertyValue("Color")).thenReturn("Blue");
        CellSet values = cellSet(List.of(List.of(road), List.of(mountain), List.of(caps)),
                new Object[][] { {}, {}, {} });
        when(olapStatement.executeQuery(
                "SELECT {} ON COLUMNS, [Product].[ProductHierarchy].[Subcategory].Members ON ROWS FROM [Sales]"))
                .thenReturn(values);
        Member red = member("DAX group 1", subcategory, null);
        Member blue = member("DAX group 2", subcategory, null);
        CellSet groups = cellSet(List.of(List.of(red), List.of(blue)), new Object[][] { { 40.0 }, { 20.5 } });
        String color = "[Product].[ProductHierarchy].CurrentMember.Properties(\"Color\")";
        when(olapStatement.executeQuery("WITH MEMBER [Product].[ProductHierarchy].[DAX group 1] AS Aggregate(Filter("
                + "[Product].[ProductHierarchy].[Subcategory].Members, " + color + " = \"Red\")) "
                + "MEMBER [Product].[ProductHierarchy].[DAX group 2] AS Aggregate(Filter("
                + "[Product].[ProductHierarchy].[Subcategory].Members, " + color + " = \"Blue\")) "
                + "SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY {[Product].[ProductHierarchy].[DAX group 1], "
                + "[Product].[ProductHierarchy].[DAX group 2]} ON ROWS FROM [Sales]")).thenReturn(groups);

        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Subcategory.Color],
                            "S", [Sales Amount])
                        ORDER BY 'Product'[Product.ProductHierarchy.Subcategory.Color]
                        """)) {
            assertThat(rows(result.nextTable())).containsExactly(List.of("Blue", 20.5), List.of("Red", 40.0));
        }
    }

    @Test
    void groupsByNumericPropertyValuesOfTheirType() throws Exception {
        Property[] properties = { property("Sqft", false, Property.Datatype.TYPE_INTEGER) };
        when(subcategory.getProperties()).thenReturn(properties);
        Member bikes = member("Bikes", category, null);
        Member road = member("Road", subcategory, bikes);
        Member mountain = member("Mountain", subcategory, bikes);
        when(road.getPropertyValue("Sqft")).thenReturn(2678);
        when(mountain.getPropertyValue("Sqft")).thenReturn(5624);
        CellSet values = cellSet(List.of(List.of(road), List.of(mountain)), new Object[][] { {}, {} });
        when(olapStatement.executeQuery(
                "SELECT {} ON COLUMNS, [Product].[ProductHierarchy].[Subcategory].Members ON ROWS FROM [Sales]"))
                .thenReturn(values);
        CellSet groups = cellSet(List.of(List.of(member("DAX group 1", subcategory, null)),
                List.of(member("DAX group 2", subcategory, null))), new Object[][] { { 40.0 }, { 20.5 } });
        String sqft = "[Product].[ProductHierarchy].CurrentMember.Properties(\"Sqft\")";
        when(olapStatement.executeQuery("WITH MEMBER [Product].[ProductHierarchy].[DAX group 1] AS Aggregate(Filter("
                + "[Product].[ProductHierarchy].[Subcategory].Members, " + sqft + " = 2678)) "
                + "MEMBER [Product].[ProductHierarchy].[DAX group 2] AS Aggregate(Filter("
                + "[Product].[ProductHierarchy].[Subcategory].Members, " + sqft + " = 5624)) "
                + "SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY {[Product].[ProductHierarchy].[DAX group 1], "
                + "[Product].[ProductHierarchy].[DAX group 2]} ON ROWS FROM [Sales]")).thenReturn(groups);

        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Subcategory.Sqft],
                            "S", [Sales Amount])
                        """)) {
            assertThat(rows(result.nextTable())).containsExactly(List.of(2678L, 40.0), List.of(5624L, 20.5));
        }
    }

    @Test
    void filtersTheRowsOfTheMdxResult() throws Exception {
        Member bikes = member("Bikes", category, null);
        Member clothes = member("Clothes", category, null);
        List<List<Member>> positions = List.of(List.of(bikes), List.of(clothes));
        CellSet cellSet = cellSet(positions, new Object[][] { { 40.0 }, { 20.5 } });
        when(olapStatement.executeQuery(
                "SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY [Product].[ProductHierarchy].[Category].Members ON ROWS FROM [Sales]"))
                .thenReturn(cellSet);

        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        statement.setParameter("least", 30L);
        try (statement; DaxResult result = statement.execute("""
                        EVALUATE FILTER(SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Category], "Sales", [Sales Amount]),
                            [Sales] > @least)
                        """)) {
            assertThat(rows(result.nextTable())).containsExactly(List.of("Bikes", 40.0));
        }
    }

    @Test
    void topNByMeasureRunsTopCount() throws Exception {
        CellSet cellSet = cellSet(List.of(List.of(member("Bikes", category, null))), new Object[0][0]);
        when(olapStatement.executeQuery("SELECT {} ON COLUMNS, TopCount(NonEmpty([Product].[ProductHierarchy].[Category].Members, "
                + "{[Measures].[Sales Amount]}), 1, [Measures].[Sales Amount]) ON ROWS FROM [Sales]")).thenReturn(cellSet);
        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("EVALUATE TOPN(1, VALUES('Product'[Product.ProductHierarchy.Category]), [Sales Amount])")) {
            assertThat(rows(result.nextTable())).containsExactly(List.of("Bikes"));
        }
    }

    @Test
    void isBlankOfMeasureRunsIsEmpty() throws Exception {
        CellSet cellSet = cellSet(List.of(List.of(member("Bikes", category, null)),
                List.of(member("Clothes", category, null))), new Object[][] { { true }, { false } });
        when(olapStatement.executeQuery("WITH MEMBER [Measures].[DAX Empty] AS IsEmpty([Measures].[Sales Amount]) "
                + "SELECT {[Measures].[DAX Empty]} ON COLUMNS, NON EMPTY [Product].[ProductHierarchy].[Category].Members ON ROWS FROM [Sales]"))
                .thenReturn(cellSet);
        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute(
                        "EVALUATE SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Category], \"Empty\", ISBLANK([Sales Amount]))")) {
            assertThat(rows(result.nextTable())).containsExactly(List.of("Bikes", true), List.of("Clothes", false));
        }
    }

    @Test
    void filterTableKeepsTheGroupsRelatedToIt() throws Exception {
        CellSet cellSet = cellSet(List.of(List.of(member("Road", subcategory, member("Bikes", category, null)))),
                new Object[][] { { 10.0 } });
        when(olapStatement.executeQuery("SELECT {[Measures].[Sales Amount]} ON COLUMNS, NON EMPTY Exists("
                + "[Product].[ProductHierarchy].[Subcategory].Members, Filter([Product].[ProductHierarchy].[Category].Members, "
                + "UCase([Product].[ProductHierarchy].CurrentMember.Name) = \"BIKES\")) ON ROWS FROM [Sales]")).thenReturn(cellSet);
        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE SUMMARIZECOLUMNS('Product'[Product.ProductHierarchy.Subcategory],
                            KEEPFILTERS(FILTER(VALUES('Product'[Product.ProductHierarchy.Category]), 'Product'[Product.ProductHierarchy.Category] = "bikes")),
                            "S", [Sales Amount])
                        """)) {
            assertThat(rows(result.nextTable())).containsExactly(List.of("Road", 10.0));
        }
    }

    @Test
    void addColumnsOfMeasuresAndColumns() throws Exception {
        CellSet cellSet = cellSet(List.of(List.of(member("Bikes", category, null)),
                List.of(member("Clothes", category, null))), new Object[][] { { 40.0 }, { null } });
        when(olapStatement.executeQuery(
                "SELECT {[Measures].[Sales Amount]} ON COLUMNS, [Product].[ProductHierarchy].[Category].Members ON ROWS FROM [Sales]"))
                .thenReturn(cellSet);
        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("""
                        EVALUATE ADDCOLUMNS(VALUES('Product'[Product.ProductHierarchy.Category]), "S", [Sales Amount],
                            "Bikes", 'Product'[Product.ProductHierarchy.Category] = "bikes")
                        """)) {
            DaxTable table = result.nextTable();
            assertThat(table.columns()).extracting(DaxColumn::name).containsExactly("Product[Product.ProductHierarchy.Category]", "[S]",
                    "[Bikes]");
            assertThat(rows(table)).containsExactly(List.of("Bikes", 40.0, true),
                    java.util.Arrays.asList("Clothes", null, false));
        }
    }

    @Test
    void measuresAloneAnswerNoRowWhenBlank() throws Exception {
        CellSet cellSet = cellSet(null, new Object[][] { { null } });
        when(olapStatement.executeQuery(anyString())).thenReturn(cellSet);
        try (DaxQueryStatement statement = engine.createStatement(connection, Map.of());
                DaxResult result = statement.execute("EVALUATE ROW(\"Total\", [Sales Amount])")) {
            assertThat(rows(result.nextTable())).isEmpty();
        }
    }

    @Test
    void syntaxErrorHasItsPosition() {
        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        assertThatThrownBy(() -> statement.execute("EVALUATE SUMMARIZECOLUMNS(")).isInstanceOfSatisfying(
                DaxSyntaxException.class, e -> assertThat(e.getMessage()).contains("1:"));
    }

    @Test
    void errorInAnyEvaluateFailsBeforeAnythingRuns() {
        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        assertThatThrownBy(() -> statement.execute("EVALUATE 'Product' EVALUATE 'Nope'"))
                .isInstanceOf(DaxSemanticException.class).hasMessage("the table 'Nope' does not exist");
        verify(connection, never()).createStatement();
    }

    @Test
    void severalCubesNeedTheCubeNamed() throws Exception {
        Cube other = mock(Cube.class);
        when(other.getName()).thenReturn("Other");
        when(reader.getCubes()).thenReturn(List.of(other, cube));
        assertThatThrownBy(() -> engine.createStatement(connection, Map.of()).execute("EVALUATE {1}"))
                .isInstanceOf(DaxSemanticException.class).hasMessageContaining("2 cubes");
        try (DaxResult result = engine.createStatement(connection, Map.of("CUBE", "sales")).execute("EVALUATE {1}")) {
            assertThat(result.nextTable()).isNotNull();
        }
    }

    @Test
    void cancelStopsTheRunningMdx() throws Exception {
        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        when(olapStatement.executeQuery(anyString())).thenAnswer(invocation -> {
            statement.cancel(); // as another thread would, while the MDX runs
            verify(olapStatement).cancel();
            throw new IllegalStateException("interrupted");
        });
        DaxResult result = statement.execute("EVALUATE 'Product'");
        assertThatThrownBy(result::nextTable).isInstanceOfSatisfying(DaxCancelledException.class,
                e -> assertThat(e.reason()).isEqualTo(DaxCancelledException.Reason.CANCELLED));
    }

    @Test
    void timeoutStopsTheRunningMdx() throws Exception {
        CountDownLatch cancelled = new CountDownLatch(1);
        doAnswer(invocation -> {
            cancelled.countDown();
            return null;
        }).when(olapStatement).cancel();
        when(olapStatement.executeQuery(anyString())).thenAnswer(invocation -> {
            assertThat(cancelled.await(5, TimeUnit.SECONDS)).isTrue();
            throw new IllegalStateException("interrupted");
        });
        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        statement.setTimeout(Duration.ofMillis(50));
        DaxResult result = statement.execute("EVALUATE 'Product'");
        assertThatThrownBy(result::nextTable).isInstanceOfSatisfying(DaxCancelledException.class,
                e -> assertThat(e.reason()).isEqualTo(DaxCancelledException.Reason.TIMEOUT));
    }

    @Test
    void parametersMustBeDaxValues() {
        DaxQueryStatement statement = engine.createStatement(connection, Map.of());
        statement.setParameter("p", 1L);
        statement.setParameter("blank", null);
        assertThatThrownBy(() -> statement.setParameter("p", 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
