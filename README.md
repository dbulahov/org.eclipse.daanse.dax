# Eclipse Daanse DAX Language Processing

Complete DAX (Data Analysis Expressions) language processing framework with advanced parsing, abstract syntax tree modeling, and code generation capabilities.

## Overview

This project provides comprehensive support for DAX statement types including EVALUATE, DEFINE, and calculated expressions with pluggable parser implementations. The implementation follows the same architecture as the Eclipse Daanse MDX project, with clear separation between API, implementation, and parser modules.

## Modules

### model.api
Core API definitions for DAX language model. Provides interfaces and contracts for representing DAX statements, expressions, and query components.

**Key interfaces:**
- `DaxStatement` - Root interface for all DAX statements (EVALUATE, DEFINE)
- `DaxExpression` - Root interface for all expressions
- `Define` - Interface for DEFINE clauses (MEASURE, TABLE, COLUMN, VAR)
- `OrderByClause`, `StartAtClause` - Query modifiers

### model.record
Record-based implementation of the DAX model API. Provides immutable Java record classes for representing DAX statements, expressions, and query components with efficient memory usage and strong type safety.

**Features:**
- Immutable record-based implementations
- Zero-overhead abstractions
- Type-safe representations

Note: a `model.emf` module also exists in the source tree but is currently excluded from the build and out of sync with the model API (pending decision).

### parser.ccc
DAX parser implementation using CongoCC (Congo Compiler Compiler). This module provides a generated parser for DAX queries that produces API-based model objects.

**Features:**
- CongoCC-based parser generator
- Case-insensitive lexing
- Support for DAX syntax including:
    - DEFINE/EVALUATE statements
    - Variable declarations (VAR)
    - Measures, tables, and calculated columns
    - ORDER BY with ASC/DESC and BLANKS FIRST/LAST
    - START AT ... SKIP ...
    - All standard operators and expressions
- Function calls with flexible naming (as per DAX specification)
- Date/time literals in dt"..." format
- Quoted identifiers with brackets `[Column Name]`
- Table names with single quotes `'Table Name'`

### unparser.api
API definitions for DAX unparsing services. Defines the interface for converting DAX model objects back to DAX query strings.

### unparser.simple
Simple unparser implementation that converts DAX model objects to compact DAX query strings with minimal formatting.

**Features:**
- Compact output
- Minimal whitespace
- OSGi component

### unparser.formatted
Enhanced unparser implementation that produces well-formatted, commented DAX query strings.

**Features:**
- Descriptive comments for each element type:
    - `// Measure: SalesTotal` - for measures
    - `// Table: FilteredSales` - for tables
    - `// Column: Sales[Amount]` - for columns
    - `// Function: SUM` - for function calls
    - `// Numeric value` - for numeric literals
- Configurable indentation
- Multi-line formatting for function arguments
- Optional comment generation
- OSGi component with configuration support

### transform.api
API definitions for DAX transformation services (DAX → MDX). Defines the contracts for the dialect sniffer, the model binder, and the converter service, together with the result types (MDX text plus column plan).

### transform.mdx
DAX → MDX transformation implementation. Translates DAX queries into MDX statements executable by the Daanse OLAP engine.

**Features:**
- Dialect sniffer that recognizes DAX input
- Model binder that resolves tables, columns, and measures against the Daanse OLAP catalog
- Converter service producing MDX text plus a column plan for result mapping
- Transformation rules R-01..R-16 (EVALUATE, SUMMARIZECOLUMNS, filters, TOPN, ORDER BY, START AT, DEFINE MEASURE, CALCULATE, variables, subtotals, basic time intelligence)
- Unsupported or unresolvable constructs are rejected with stable error codes (`DAX-UNSUP-*`, `DAX-BIND-*`, `DAX-TYPE-*`)

## Concept & Architecture

The concept and planning behind the DAX support — including the evaluated implementation paths and the rationale for the DAX → MDX transformation approach — is documented in the planning series of the olap repository: `org.eclipse.daanse.olap/docs/planung/dax/`, especially `04-weg1-dax-zu-mdx.md`.

## Grammar Structure

The parser is organized into modular grammar files:

- **Grammar.ccc** - Main grammar configuration and entry point
- **Lexer.inc.ccc** - Lexical analysis rules (tokens, keywords, operators)
- **Expressions.inc.ccc** - Expression parsing rules
- **Statements.inc.ccc** - Statement parsing rules (DEFINE, EVALUATE)

## DAX Language Support

The parser supports core DAX language features:

### Statements
- `EVALUATE <table-expression>` - Primary query statement
- `DEFINE` - Define measures, variables, tables, and columns
- `ORDER BY` - Sort results with optional blank handling
- `START AT` - Skip rows and pagination

### Expressions
- **Literals**: Numbers, strings, dates (dt"..."), booleans
- **Identifiers**: Column references `[Column]`, table references `'Table'[Column]`
- **Operators**: Arithmetic (+, -, *, /), Comparison (=, <>, <, >, <=, >=), Logical (AND, OR, NOT)
- **Function calls**: Flexible function names (does not hardcode function list)

### Example DAX Query
```dax
DEFINE
    MEASURE Sales[Total] = SUM(Sales[Amount])
    VAR MinDate = MIN(Sales[Date])
    RETURN
EVALUATE
    SUMMARIZE(
        Sales,
        Sales[Product],
        "Total Sales", [Total]
    )
ORDER BY [Total] DESC
```

## Building

```bash
mvn clean install
```

The CongoCC parser will be generated during the build process from the grammar files in `parser.ccc/src/main/ccc/`.

## Design Decisions

### No Function Keyword List
Following the user's requirement, the parser does NOT include a hardcoded list of DAX function names. Function names are treated as flexible identifiers, allowing the parser to work with any DAX function (built-in or custom) without grammar updates.

### Architecture
The project follows the proven architecture of Eclipse Daanse MDX:
- Clean separation between API and implementation
- Sealed interfaces for type safety
- Record-based implementations for immutability
- Generated parser from declarative grammar

## References

- DAX syntax reference: https://learn.microsoft.com/en-us/dax/dax-syntax-reference
- Eclipse Daanse project: https://github.com/eclipse-daanse
- Based on grammar from: dax.g4 (ANTLR4 DAX grammar)

## License

Eclipse Public License 2.0 (EPL-2.0)
