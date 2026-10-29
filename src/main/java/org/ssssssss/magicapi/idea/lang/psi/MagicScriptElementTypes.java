package org.ssssssss.magicapi.idea.lang.psi;

import org.ssssssss.magicapi.idea.lang.MagicScriptLanguage;
import org.ssssssss.magicapi.idea.lang.lexer.MagicScriptLexer;

/**
 * The composite element types of the magic-script AST.
 *
 * <p>Kept in one place so both the parser and the structure/folding extensions
 * can refer to them without circular dependencies.</p>
 */
public interface MagicScriptElementTypes {

    MagicScriptLanguage LANGUAGE = MagicScriptLanguage.INSTANCE;

    MagicScriptElementType SCRIPT = new MagicScriptElementType("SCRIPT");

    // Statements
    MagicScriptElementType STATEMENT = new MagicScriptElementType("STATEMENT");
    MagicScriptElementType EXPRESSION_STATEMENT = new MagicScriptElementType("EXPRESSION_STATEMENT");
    MagicScriptElementType VARIABLE_DECLARATION = new MagicScriptElementType("VARIABLE_DECLARATION");
    MagicScriptElementType IF_STATEMENT = new MagicScriptElementType("IF_STATEMENT");
    MagicScriptElementType ELSE_CLAUSE = new MagicScriptElementType("ELSE_CLAUSE");
    MagicScriptElementType FOR_STATEMENT = new MagicScriptElementType("FOR_STATEMENT");
    MagicScriptElementType WHILE_STATEMENT = new MagicScriptElementType("WHILE_STATEMENT");
    MagicScriptElementType TRY_STATEMENT = new MagicScriptElementType("TRY_STATEMENT");
    MagicScriptElementType CATCH_CLAUSE = new MagicScriptElementType("CATCH_CLAUSE");
    MagicScriptElementType FINALLY_CLAUSE = new MagicScriptElementType("FINALLY_CLAUSE");
    MagicScriptElementType IMPORT_STATEMENT = new MagicScriptElementType("IMPORT_STATEMENT");
    MagicScriptElementType RETURN_STATEMENT = new MagicScriptElementType("RETURN_STATEMENT");
    MagicScriptElementType EXIT_STATEMENT = new MagicScriptElementType("EXIT_STATEMENT");
    MagicScriptElementType THROW_STATEMENT = new MagicScriptElementType("THROW_STATEMENT");
    MagicScriptElementType ASSERT_STATEMENT = new MagicScriptElementType("ASSERT_STATEMENT");
    MagicScriptElementType BREAK_STATEMENT = new MagicScriptElementType("BREAK_STATEMENT");
    MagicScriptElementType CONTINUE_STATEMENT = new MagicScriptElementType("CONTINUE_STATEMENT");
    MagicScriptElementType BLOCK_STATEMENT = new MagicScriptElementType("BLOCK_STATEMENT");
    MagicScriptElementType LAMBDA_EXPRESSION = new MagicScriptElementType("LAMBDA_EXPRESSION");
    MagicScriptElementType PARAMETER_LIST = new MagicScriptElementType("PARAMETER_LIST");
    MagicScriptElementType PARAMETER = new MagicScriptElementType("PARAMETER");

    // Expressions
    MagicScriptElementType EXPRESSION = new MagicScriptElementType("EXPRESSION");
    MagicScriptElementType ASSIGNMENT_EXPRESSION = new MagicScriptElementType("ASSIGNMENT_EXPRESSION");
    MagicScriptElementType TERNARY_EXPRESSION = new MagicScriptElementType("TERNARY_EXPRESSION");
    MagicScriptElementType BINARY_EXPRESSION = new MagicScriptElementType("BINARY_EXPRESSION");
    MagicScriptElementType UNARY_EXPRESSION = new MagicScriptElementType("UNARY_EXPRESSION");
    MagicScriptElementType PREFIX_EXPRESSION = new MagicScriptElementType("PREFIX_EXPRESSION");
    MagicScriptElementType POSTFIX_EXPRESSION = new MagicScriptElementType("POSTFIX_EXPRESSION");
    MagicScriptElementType CALL_EXPRESSION = new MagicScriptElementType("CALL_EXPRESSION");
    MagicScriptElementType ARGUMENT_LIST = new MagicScriptElementType("ARGUMENT_LIST");
    MagicScriptElementType REFERENCE_EXPRESSION = new MagicScriptElementType("REFERENCE_EXPRESSION");
    MagicScriptElementType MEMBER_ACCESS_EXPRESSION = new MagicScriptElementType("MEMBER_ACCESS_EXPRESSION");
    MagicScriptElementType NEW_EXPRESSION = new MagicScriptElementType("NEW_EXPRESSION");
    MagicScriptElementType ARRAY_ACCESS_EXPRESSION = new MagicScriptElementType("ARRAY_ACCESS_EXPRESSION");
    MagicScriptElementType PARENTHESIZED_EXPRESSION = new MagicScriptElementType("PARENTHESIZED_EXPRESSION");
    MagicScriptElementType LITERAL_EXPRESSION = new MagicScriptElementType("LITERAL_EXPRESSION");
    MagicScriptElementType MAP_LITERAL = new MagicScriptElementType("MAP_LITERAL");
    MagicScriptElementType LIST_LITERAL = new MagicScriptElementType("LIST_LITERAL");
    MagicScriptElementType SELECT_EXPRESSION = new MagicScriptElementType("SELECT_EXPRESSION");
    MagicScriptElementType ASYNC_EXPRESSION = new MagicScriptElementType("ASYNC_EXPRESSION");
    MagicScriptElementType SPREAD_EXPRESSION = new MagicScriptElementType("SPREAD_EXPRESSION");
    MagicScriptElementType CLASS_CONVERTER_EXPRESSION = new MagicScriptElementType("CLASS_CONVERTER_EXPRESSION");
    MagicScriptElementType EMBEDDED_EXPRESSION = new MagicScriptElementType("EMBEDDED_EXPRESSION");
}
