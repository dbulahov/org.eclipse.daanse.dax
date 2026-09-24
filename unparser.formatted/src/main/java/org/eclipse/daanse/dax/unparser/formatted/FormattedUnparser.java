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
package org.eclipse.daanse.dax.unparser.formatted;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.dax.model.api.ColumnDefinition;
import org.eclipse.daanse.dax.model.api.DefineClause;
import org.eclipse.daanse.dax.model.api.EvaluateStatement;
import org.eclipse.daanse.dax.model.api.MeasureDefinition;
import org.eclipse.daanse.dax.model.api.ParameterDefinition;
import org.eclipse.daanse.dax.model.api.TableDefinition;
import org.eclipse.daanse.dax.model.api.VariableDefinition;
import org.eclipse.daanse.dax.model.api.expression.BooleanLiteral;
import org.eclipse.daanse.dax.model.api.expression.DateTimeLiteral;
import org.eclipse.daanse.dax.model.api.expression.DaxExpression;
import org.eclipse.daanse.dax.model.api.expression.Entity;
import org.eclipse.daanse.dax.model.api.expression.FunctionCall;
import org.eclipse.daanse.dax.model.api.expression.Identifier;
import org.eclipse.daanse.dax.model.api.expression.Literal;
import org.eclipse.daanse.dax.model.api.expression.NumericLiteral;
import org.eclipse.daanse.dax.model.api.expression.Parameter;
import org.eclipse.daanse.dax.model.api.expression.Scalar;
import org.eclipse.daanse.dax.model.api.expression.StringLiteral;
import org.eclipse.daanse.dax.unparser.api.UnParser;
import org.eclipse.daanse.dax.unparser.api.base.AbstractUnParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Formatted {@link UnParser} implementation: indents the statement and (by
 * default) adds descriptive single-line comments for measures, tables,
 * columns, functions and literals.
 * <p>
 * Comments are emitted only in positions where they cannot swallow following
 * DAX code, so the output remains valid DAX: whole-line comments on their own
 * line, and trailing comments only after an expression that ends its clause.
 * A trailing comment is held back until the end of the line, so that nothing
 * written after the expression on the same line can end up inside it.
 * </p>
 * <p>
 * The behaviour is configurable through {@link Config}: {@code includeComments}
 * toggles the comments, {@code indentSize} sets the number of spaces per
 * indentation level. The configuration is applied on activation and on
 * modification.
 * </p>
 * <p>
 * The service is registered with the property
 * {@code dax.unparser.style=formatted}. Consumers that need exactly this
 * implementation select it with a target filter, for example
 * {@code @Reference(target = "(dax.unparser.style=formatted)")}.
 * </p>
 */
@Component(service = UnParser.class, property = { FormattedUnparser.STYLE_PROPERTY })
@Designate(ocd = FormattedUnparser.Config.class)
public class FormattedUnparser extends AbstractUnParser {

    /** Service property that identifies this unparser implementation. */
    public static final String STYLE_PROPERTY = "dax.unparser.style=formatted";

    /**
     * Configuration of the formatted unparser; used as the activation object of
     * the component, so it is an annotation type (component property type).
     */
    @ObjectClassDefinition()
    public @interface Config {
        /**
         * @return whether descriptive comments are included
         */
        @AttributeDefinition(name = "Include Comments", description = "Include descriptive comments for each element")
        boolean includeComments() default true;

        /**
         * @return the number of spaces per indentation level
         */
        @AttributeDefinition(name = "Indent Size", description = "Number of spaces for indentation")
        int indentSize() default 4;
    }

    /**
     * Description of each context a trailing comment may follow; every other
     * context is nested inside an expression or followed by more tokens.
     */
    private static final Map<String, String> TOP_LEVEL_CONTEXTS = Map.of( //
            CONTEXT_MEASURE, "measure calculation", //
            CONTEXT_COLUMN, "column calculation", //
            CONTEXT_TABLE, "table definition", //
            CONTEXT_VARIABLE, "variable value", //
            CONTEXT_PARAMETER, "parameter value", //
            CONTEXT_EVALUATE, "table to evaluate");

    private boolean includeComments = true;
    private int indentSize = 4;
    private int currentIndent;
    private String pendingLineComment;

    /**
     * Activates the component with the given configuration.
     *
     * @param config the configuration; the values are applied to the renderer
     */
    @Activate
    @Modified
    void activate(Config config) {
        this.includeComments = config.includeComments();
        this.indentSize = Math.max(0, config.indentSize());
    }

    @Override
    protected void beforeStatement(StringBuilder sb) {
        currentIndent = 0;
        pendingLineComment = null;
        if (includeComments) {
            sb.append("// DAX Query\n");
            sb.append("// Generated by Eclipse Daanse Formatted Unparser\n\n");
        }
    }

    @Override
    protected void unparseDefineBlock(StringBuilder sb, List<DefineClause> clauses) {
        if (includeComments) {
            sb.append("// DEFINE block - declares measures, tables, columns, and variables\n");
        }
        sb.append("DEFINE\n");

        currentIndent++;
        for (DefineClause clause : clauses) {
            indent(sb);
            beforeDefineClause(sb, clause);
            appendDefineClause(sb, clause);
            endLine(sb);
        }
        currentIndent--;
    }

    @Override
    protected void beforeDefineClause(StringBuilder sb, DefineClause clause) {
        if (!includeComments) {
            return;
        }
        switch (clause) {
        case MeasureDefinition md -> sb.append("// Measure: ").append(describe(md.name()));
        case TableDefinition td -> sb.append("// Table: ").append(td.name());
        case ColumnDefinition cd -> sb.append("// Column: ").append(describe(cd.name()));
        case VariableDefinition vd -> sb.append("// Variable: ").append(vd.name());
        case ParameterDefinition pd -> sb.append("// Parameter: @").append(pd.name());
        }
        sb.append('\n');
        indent(sb);
    }

    @Override
    protected void unparseEvaluateStatement(StringBuilder sb, EvaluateStatement es) {
        if (includeComments) {
            sb.append("// EVALUATE statement - returns the result table\n");
        }
        sb.append("EVALUATE\n");

        currentIndent++;
        indent(sb);
        appendExpression(sb, es.tableExpression(), CONTEXT_EVALUATE);
        currentIndent--;

        appendOrderBy(sb, es);
        flushLineComment(sb);
    }

    @Override
    protected void beforeOrderByClause(StringBuilder sb) {
        endLine(sb);
        if (includeComments) {
            sb.append("// Sort order specification\n");
        }
    }

    @Override
    protected String orderByItemSeparator() {
        return ",\n" + " ".repeat(indentSize);
    }

    @Override
    protected void beforeStartAtClause(StringBuilder sb, List<DaxExpression> values) {
        sb.append('\n');
        if (includeComments) {
            sb.append("// Pagination: start at ").append(values.size()).append(" sort key value(s)\n");
        }
    }

    @Override
    protected void appendLiteral(StringBuilder sb, Literal literal, String context) {
        super.appendLiteral(sb, literal, context);
        if (commentsFor(context)) {
            pendingLineComment = switch (literal) {
            case NumericLiteral n -> "Numeric value for " + TOP_LEVEL_CONTEXTS.get(context);
            case StringLiteral s -> "String literal";
            case BooleanLiteral b -> "Boolean value";
            case DateTimeLiteral d -> "Date/time literal";
            };
        }
    }

    @Override
    protected void appendIdentifier(StringBuilder sb, Identifier id, String context) {
        super.appendIdentifier(sb, id, context);
        if (commentsFor(context)) {
            if (id.parts().size() > 1) {
                pendingLineComment = "Table[Column] reference";
            } else if (id.parts().get(0) instanceof Entity) {
                pendingLineComment = "Table reference";
            } else {
                pendingLineComment = "Column reference";
            }
        }
    }

    @Override
    protected void appendFunctionCall(StringBuilder sb, FunctionCall fc, String context) {
        if (commentsFor(context)) {
            // The comment goes on its own line so that it cannot swallow the call.
            sb.append("// Function: ").append(fc.functionName());
            if (!fc.arguments().isEmpty()) {
                sb.append(" with ").append(fc.arguments().size()).append(" argument(s)");
            }
            sb.append('\n');
            indent(sb);
        }
        super.appendFunctionCall(sb, fc, context);
    }

    /**
     * @param context the rendering context, may be {@code null}
     * @return whether a comment may be emitted here; comments are only safe
     *         for an expression that is not nested inside another expression
     *         and ends its clause
     */
    private boolean commentsFor(String context) {
        return includeComments && context != null && TOP_LEVEL_CONTEXTS.containsKey(context);
    }

    /**
     * @return the identifier in comment form, e.g. {@code Sales[Total]}
     */
    private static String describe(Identifier id) {
        StringBuilder sb = new StringBuilder();
        for (DaxExpression part : id.parts()) {
            switch (part) {
            case Entity e -> sb.append(e.name());
            case Scalar s -> sb.append('[').append(s.name()).append(']');
            case Parameter p -> sb.append('@').append(p.name());
            default -> sb.append(part);
            }
        }
        return sb.toString();
    }

    private void flushLineComment(StringBuilder sb) {
        if (pendingLineComment != null) {
            sb.append(" // ").append(pendingLineComment);
            pendingLineComment = null;
        }
    }

    private void endLine(StringBuilder sb) {
        flushLineComment(sb);
        sb.append('\n');
    }

    private void indent(StringBuilder sb) {
        sb.append(" ".repeat(currentIndent * indentSize));
    }
}
