package org.ssssssss.magicapi.idea.lang.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;
import org.ssssssss.magicapi.idea.lang.psi.MagicScriptElementTypes;

/**
 * Recursive descent parser for magic-script.
 *
 * <p>Statement and expression shapes — including operator precedence — mirror
 * {@code org.ssssssss.script.parsing.Parser} so that the PSI tree matches what
 * the runtime accepts. The parser is deliberately forgiving: on unexpected input
 * it reports an error and skips a token, which keeps completion and folding
 * usable while the user is typing.</p>
 */
public class MagicScriptParser implements PsiParser, MagicScriptElementTypes {

    /** Precedence levels, lowest first. */
    private static final String[][] BINARY_OPERATOR_LEVELS = {
            {"||"},
            {"&&"},
            {"|"},
            {"^"},
            {"&"},
            {"===", "==", "!==", "!=", "<>"},
            {"<", "<=", ">", ">=", "instanceof"},
            {"+", "-"},
            {"<<", ">>", ">>>"},
            {"*", "/", "%"}
    };

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        while (!builder.eof()) {
            int before = builder.getCurrentOffset();
            parseStatement(builder);
            if (builder.getCurrentOffset() == before) {
                builder.error("Unexpected token");
                builder.advanceLexer();
            }
        }
        marker.done((com.intellij.psi.tree.IElementType) SCRIPT);
        return builder.getTreeBuilt();
    }

    // ------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------

    private void parseStatement(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        if (type == null) {
            return;
        }
        if (type == MagicScriptTokenType.SEMICOLON) {
            builder.advanceLexer();
            return;
        }
        if (type == MagicScriptTokenType.LEFT_BRACE) {
            parseBlock(builder);
            return;
        }
        String text = builder.getTokenText();
        if (type == MagicScriptTokenType.IDENTIFIER && text != null) {
            switch (text) {
                case "import" -> parseImport(builder);
                case "var", "let", "const" -> parseVariableDeclaration(builder);
                case "if" -> parseIf(builder);
                case "for" -> parseFor(builder);
                case "while" -> parseWhile(builder);
                case "try" -> parseTry(builder);
                case "return" -> parseReturn(builder);
                case "exit" -> parseExit(builder);
                case "throw" -> parseThrow(builder);
                case "assert" -> parseAssert(builder);
                case "break", "continue" -> parseSimpleKeywordStatement(builder,
                        "break".equals(text) ? BREAK_STATEMENT : CONTINUE_STATEMENT);
                default -> parseExpressionStatement(builder);
            }
            return;
        }
        parseExpressionStatement(builder);
    }

    private void parseSimpleKeywordStatement(PsiBuilder builder, IElementType elementType) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        consumeSemicolon(builder);
        marker.done(elementType);
    }

    private void parseExpressionStatement(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        int before = builder.getCurrentOffset();
        parseExpression(builder);
        consumeSemicolon(builder);
        if (builder.getCurrentOffset() == before) {
            marker.drop();
            builder.error("Unexpected token");
            builder.advanceLexer();
            return;
        }
        marker.done(EXPRESSION_STATEMENT);
    }

    private void parseBlock(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        if (builder.getTokenType() != MagicScriptTokenType.LEFT_BRACE) {
            marker.drop();
            return;
        }
        builder.advanceLexer();
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_BRACE) {
            int before = builder.getCurrentOffset();
            parseStatement(builder);
            if (builder.getCurrentOffset() == before) {
                builder.error("Unexpected token");
                builder.advanceLexer();
            }
        }
        if (builder.getTokenType() == MagicScriptTokenType.RIGHT_BRACE) {
            builder.advanceLexer();
        } else {
            builder.error("Expected '}'");
        }
        marker.done(BLOCK_STATEMENT);
    }

    /**
     * {@code import log;}, {@code import 'java.util.Date' as Date;} or a bare
     * dotted class name {@code import com.alibaba.fastjson.JSON;}.
     *
     * <p>magic-script's {@code parseImport} loops over {@code Period} after an
     * unquoted identifier, so a dotted path is valid without quotes; a trailing
     * {@code .*} is accepted too.</p>
     */
    private void parseImport(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // import
        if (builder.getTokenType() == MagicScriptTokenType.STRING_LITERAL) {
            builder.advanceLexer();
        } else if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
            builder.advanceLexer();
            // dotted package / class name: com.alibaba.fastjson.JSON or java.util.*
            while (builder.getTokenType() == MagicScriptTokenType.DOT) {
                builder.advanceLexer();
                if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                    builder.advanceLexer();
                } else if (builder.getTokenType() == MagicScriptTokenType.OPERATION_SIGN
                        && "*".equals(builder.getTokenText())) {
                    builder.advanceLexer();
                    break;
                } else {
                    builder.error("Expected a class name segment");
                    break;
                }
            }
        } else {
            builder.error("Expected a module name or a class name");
        }
        if (isKeyword(builder, "as")) {
            builder.advanceLexer();
            if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                builder.advanceLexer();
            } else {
                builder.error("Expected an alias");
            }
        }
        consumeSemicolon(builder);
        marker.done(IMPORT_STATEMENT);
    }

    private void parseVariableDeclaration(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // var / let / const
        while (!builder.eof()) {
            if (builder.getTokenType() == MagicScriptTokenType.LEFT_BRACE
                    || builder.getTokenType() == MagicScriptTokenType.LEFT_BRACKET) {
                skipBalanced(builder);
            } else if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                builder.advanceLexer();
            } else {
                builder.error("Expected a variable name");
                break;
            }
            if (builder.getTokenType() == MagicScriptTokenType.ASSIGN) {
                builder.advanceLexer();
                parseExpression(builder);
            }
            if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
                continue;
            }
            break;
        }
        consumeSemicolon(builder);
        marker.done(VARIABLE_DECLARATION);
    }

    private void skipBalanced(PsiBuilder builder) {
        IElementType open = builder.getTokenType();
        IElementType close = open == MagicScriptTokenType.LEFT_BRACE
                ? MagicScriptTokenType.RIGHT_BRACE
                : MagicScriptTokenType.RIGHT_BRACKET;
        int depth = 0;
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == open) {
                depth++;
            } else if (type == close) {
                depth--;
                if (depth == 0) {
                    builder.advanceLexer();
                    return;
                }
            }
            builder.advanceLexer();
        }
    }

    private void parseIf(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // if
        parseParenthesizedCondition(builder);
        parseStatement(builder);
        while (isKeyword(builder, "else")) {
            PsiBuilder.Marker elseMarker = builder.mark();
            builder.advanceLexer(); // else
            if (isKeyword(builder, "if")) {
                parseIf(builder);
            } else {
                parseStatement(builder);
            }
            elseMarker.done(ELSE_CLAUSE);
        }
        marker.done(IF_STATEMENT);
    }

    /**
     * A condition of {@code if} / {@code while}. magic-script makes the parentheses
     * optional ({@code if a == 1 { } } is valid), but the body must always be a block.
     */
    private void parseParenthesizedCondition(PsiBuilder builder) {
        boolean parenthesized = builder.getTokenType() == MagicScriptTokenType.LEFT_PAREN;
        if (parenthesized) {
            builder.advanceLexer();
        }
        parseExpression(builder);
        if (parenthesized) {
            expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
        }
    }

    /**
     * {@code for (value in expr) { ... }} or {@code for (key, value in expr) { ... }}.
     *
     * <p>magic-script has no C style {@code for(;;)} header: the parser forces
     * {@code (} ... {@code in} ... {@code )} (Parser.java:510-536 in magic-script).</p>
     */
    private void parseFor(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // for
        if (builder.getTokenType() != MagicScriptTokenType.LEFT_PAREN) {
            builder.error("Expected '('");
            marker.done(FOR_STATEMENT);
            return;
        }
        builder.advanceLexer();
        // Loop variables: `value` or `key, value`.
        int loopVariables = 0;
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_PAREN) {
            int before = builder.getCurrentOffset();
            if (isKeyword(builder, "in")) {
                builder.advanceLexer();
                // The iterated expression.
                parseExpression(builder);
                break;
            }
            if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                PsiBuilder.Marker variable = builder.mark();
                builder.advanceLexer();
                variable.done(PARAMETER);
                loopVariables++;
            } else if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
            } else {
                builder.error("Expected a loop variable or 'in'");
                builder.advanceLexer();
            }
            if (builder.getCurrentOffset() == before) {
                builder.advanceLexer();
            }
        }
        if (loopVariables == 0) {
            builder.error("Expected loop variable");
        }
        expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
        parseStatement(builder);
        marker.done(FOR_STATEMENT);
    }

    private void parseWhile(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // while
        parseParenthesizedCondition(builder);
        parseStatement(builder);
        marker.done(WHILE_STATEMENT);
    }

    private void parseTry(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // try
        parseStatement(builder);
        if (isKeyword(builder, "catch")) {
            PsiBuilder.Marker catchMarker = builder.mark();
            builder.advanceLexer();
            if (builder.getTokenType() == MagicScriptTokenType.LEFT_PAREN) {
                builder.advanceLexer();
                if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                    builder.advanceLexer();
                }
                expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
            }
            parseStatement(builder);
            catchMarker.done(CATCH_CLAUSE);
        }
        if (isKeyword(builder, "finally")) {
            PsiBuilder.Marker finallyMarker = builder.mark();
            builder.advanceLexer();
            parseStatement(builder);
            finallyMarker.done(FINALLY_CLAUSE);
        }
        marker.done(TRY_STATEMENT);
    }

    private void parseReturn(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // return
        if (!atStatementEnd(builder)) {
            parseExpression(builder);
        }
        consumeSemicolon(builder);
        marker.done(RETURN_STATEMENT);
    }

    private void parseExit(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // exit
        if (!atStatementEnd(builder)) {
            while (true) {
                parseExpression(builder);
                if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                    builder.advanceLexer();
                    continue;
                }
                break;
            }
        }
        consumeSemicolon(builder);
        marker.done(EXIT_STATEMENT);
    }

    private void parseThrow(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        parseExpression(builder);
        consumeSemicolon(builder);
        marker.done(THROW_STATEMENT);
    }

    private void parseAssert(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // assert
        parseExpression(builder);
        if (builder.getTokenType() == MagicScriptTokenType.COLON) {
            builder.advanceLexer();
            while (true) {
                parseExpression(builder);
                if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                    builder.advanceLexer();
                    continue;
                }
                break;
            }
        }
        consumeSemicolon(builder);
        marker.done(ASSERT_STATEMENT);
    }

    // ------------------------------------------------------------------
    // Expressions
    // ------------------------------------------------------------------

    private void parseExpression(PsiBuilder builder) {
        parseAssignment(builder);
    }

    private void parseAssignment(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        parseTernary(builder);
        if (!isAssignmentOperator(builder)) {
            marker.drop();
            return;
        }
        builder.advanceLexer();
        parseAssignment(builder);
        marker.done(ASSIGNMENT_EXPRESSION);
    }

    private boolean isAssignmentOperator(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        String text = builder.getTokenText();
        if (type == MagicScriptTokenType.ASSIGN) {
            return true;
        }
        if (type == MagicScriptTokenType.OPERATION_SIGN && text != null) {
            return text.length() > 1 && text.endsWith("=");
        }
        return false;
    }

    private void parseTernary(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        parseBinary(builder, 0);
        if (builder.getTokenType() == MagicScriptTokenType.QUESTION) {
            builder.advanceLexer();
            parseExpression(builder);
            if (builder.getTokenType() == MagicScriptTokenType.COLON) {
                builder.advanceLexer();
                parseExpression(builder);
            } else {
                builder.error("Expected ':'");
            }
            marker.done(TERNARY_EXPRESSION);
        } else {
            marker.drop();
        }
    }

    private void parseBinary(PsiBuilder builder, int level) {
        if (level >= BINARY_OPERATOR_LEVELS.length) {
            parseUnary(builder);
            return;
        }
        String[] operators = BINARY_OPERATOR_LEVELS[level];
        PsiBuilder.Marker marker = builder.mark();
        parseBinary(builder, level + 1);
        while (!builder.eof() && matchesAny(builder, operators)) {
            builder.advanceLexer();
            parseBinary(builder, level + 1);
            marker.done(BINARY_EXPRESSION);
            marker = marker.precede();
        }
        marker.drop();
    }

    private boolean matchesAny(PsiBuilder builder, String[] operators) {
        IElementType type = builder.getTokenType();
        String text = builder.getTokenText();
        if (type == null || text == null) {
            return false;
        }
        if (type != MagicScriptTokenType.OPERATION_SIGN
                && type != MagicScriptTokenType.ASSIGN
                && !(type == MagicScriptTokenType.IDENTIFIER && "instanceof".equals(text))) {
            return false;
        }
        for (String operator : operators) {
            if (operator.equals(text)) {
                return true;
            }
        }
        return false;
    }

    private void parseUnary(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        if (type == null) {
            return;
        }
        if (isKeyword(builder, "async")) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            parseUnary(builder);
            marker.done(ASYNC_EXPRESSION);
            return;
        }
        if (type == MagicScriptTokenType.OPERATION_SIGN) {
            String text = builder.getTokenText();
            if ("!".equals(text) || "~".equals(text) || "++".equals(text)
                    || "--".equals(text) || "+".equals(text) || "-".equals(text)) {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                parseUnary(builder);
                marker.done(PREFIX_EXPRESSION);
                return;
            }
        }
        parsePostfix(builder);
    }

    private void parsePostfix(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        parsePrimary(builder);
        while (!builder.eof()) {
            IElementType type = builder.getTokenType();
            if (type == MagicScriptTokenType.DOT || type == MagicScriptTokenType.SAFE_DOT) {
                builder.advanceLexer();
                if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                    builder.advanceLexer();
                } else {
                    builder.error("Expected a field name");
                }
                marker.done(MEMBER_ACCESS_EXPRESSION);
            } else if (type == MagicScriptTokenType.LEFT_PAREN) {
                parseArgumentList(builder);
                marker.done(CALL_EXPRESSION);
            } else if (type == MagicScriptTokenType.LEFT_BRACKET) {
                builder.advanceLexer();
                parseExpression(builder);
                expect(builder, MagicScriptTokenType.RIGHT_BRACKET, "']'");
                marker.done(ARRAY_ACCESS_EXPRESSION);
            } else if (type == MagicScriptTokenType.DOUBLE_COLON) {
                builder.advanceLexer();
                if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                    builder.advanceLexer();
                }
                if (builder.getTokenType() == MagicScriptTokenType.LEFT_PAREN) {
                    parseArgumentList(builder);
                }
                marker.done(CLASS_CONVERTER_EXPRESSION);
            } else if (type == MagicScriptTokenType.OPERATION_SIGN && isIncrementOrDecrement(builder)) {
                builder.advanceLexer();
                marker.done(POSTFIX_EXPRESSION);
            } else {
                break;
            }
            marker = marker.precede();
        }
        marker.drop();
    }

    private static boolean isIncrementOrDecrement(PsiBuilder builder) {
        String text = builder.getTokenText();
        return "++".equals(text) || "--".equals(text);
    }

    private void parseArgumentList(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // (
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_PAREN) {
            int before = builder.getCurrentOffset();
            if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
                continue;
            }
            parseExpression(builder);
            if (builder.getCurrentOffset() == before) {
                builder.advanceLexer();
            }
        }
        expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
        marker.done(ARGUMENT_LIST);
    }

    private void parsePrimary(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        if (type == null) {
            return;
        }
        if (type == MagicScriptTokenType.LEFT_PAREN) {
            if (isLambdaAhead(builder)) {
                PsiBuilder.Marker marker = builder.mark();
                parseParameterList(builder);
                parseLambdaBody(builder);
                marker.done(LAMBDA_EXPRESSION);
                return;
            }
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            parseExpression(builder);
            expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
            marker.done(PARENTHESIZED_EXPRESSION);
            return;
        }
        if (type == MagicScriptTokenType.LEFT_BRACE) {
            parseMapLiteral(builder);
            return;
        }
        if (type == MagicScriptTokenType.LEFT_BRACKET) {
            parseListLiteral(builder);
            return;
        }
        if (type == MagicScriptTokenType.IDENTIFIER) {
            String text = builder.getTokenText();
            if ("new".equals(text)) {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                    builder.advanceLexer();
                }
                if (builder.getTokenType() == MagicScriptTokenType.LEFT_PAREN) {
                    parseArgumentList(builder);
                }
                marker.done(NEW_EXPRESSION);
                return;
            }
            if ("select".equals(text)) {
                parseSelect(builder);
                return;
            }
            if (builder.lookAhead(1) == MagicScriptTokenType.LAMBDA && isPlainIdentifier(text)) {
                PsiBuilder.Marker marker = builder.mark();
                builder.advanceLexer();
                builder.advanceLexer(); // =>
                parseLambdaBody(builder);
                marker.done(LAMBDA_EXPRESSION);
                return;
            }
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            marker.done(REFERENCE_EXPRESSION);
            return;
        }
        if (type == MagicScriptTokenType.SPREAD) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            parseExpression(builder);
            marker.done(SPREAD_EXPRESSION);
            return;
        }
        if (type == MagicScriptTokenType.EMBEDDED_CODE) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            marker.done(EMBEDDED_EXPRESSION);
            return;
        }
        if (isLiteral(type)) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            marker.done(LITERAL_EXPRESSION);
            return;
        }
        builder.error("Expected an expression");
        builder.advanceLexer();
    }

    /**
     * True when the current {@code (} opens a lambda parameter list, i.e.
     * {@code (a, b) => ...}. {@code lookAhead(0)} is the current token itself, so
     * the scan starts at 1.
     */
    private boolean isLambdaAhead(PsiBuilder builder) {
        int lookahead = 1;
        while (lookahead < 64) {
            IElementType type = builder.lookAhead(lookahead);
            if (type == null) {
                return false;
            }
            if (type == MagicScriptTokenType.RIGHT_PAREN) {
                return builder.lookAhead(lookahead + 1) == MagicScriptTokenType.LAMBDA;
            }
            if (type != MagicScriptTokenType.IDENTIFIER && type != MagicScriptTokenType.COMMA) {
                return false;
            }
            lookahead++;
        }
        return false;
    }

    private static boolean isPlainIdentifier(String text) {
        return text != null && !"new".equals(text) && !"async".equals(text) && !"select".equals(text);
    }

    private static boolean isLiteral(IElementType type) {
        return type == MagicScriptTokenType.INT_LITERAL
                || type == MagicScriptTokenType.LONG_LITERAL
                || type == MagicScriptTokenType.FLOAT_LITERAL
                || type == MagicScriptTokenType.DOUBLE_LITERAL
                || type == MagicScriptTokenType.DECIMAL_LITERAL
                || type == MagicScriptTokenType.BYTE_LITERAL
                || type == MagicScriptTokenType.SHORT_LITERAL
                || type == MagicScriptTokenType.STRING_LITERAL
                || type == MagicScriptTokenType.REGEXP_LITERAL;
    }

    private void parseParameterList(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // (
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_PAREN) {
            if (builder.getTokenType() == MagicScriptTokenType.IDENTIFIER) {
                PsiBuilder.Marker parameter = builder.mark();
                builder.advanceLexer();
                parameter.done(PARAMETER);
            } else {
                builder.advanceLexer();
                continue;
            }
            if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
            }
        }
        expect(builder, MagicScriptTokenType.RIGHT_PAREN, "')'");
        marker.done(PARAMETER_LIST);
        expect(builder, MagicScriptTokenType.LAMBDA, "'=>'");
    }

    private void parseLambdaBody(PsiBuilder builder) {
        if (builder.getTokenType() == MagicScriptTokenType.LEFT_BRACE) {
            parseBlock(builder);
        } else {
            parseExpression(builder);
        }
    }

    private void parseMapLiteral(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // {
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_BRACE) {
            int before = builder.getCurrentOffset();
            IElementType type = builder.getTokenType();
            if (type == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
                continue;
            }
            if (type == MagicScriptTokenType.SPREAD) {
                builder.advanceLexer();
                parseExpression(builder);
            } else if (type == MagicScriptTokenType.LEFT_BRACKET) {
                builder.advanceLexer();
                parseExpression(builder);
                expect(builder, MagicScriptTokenType.RIGHT_BRACKET, "']'");
                if (builder.getTokenType() == MagicScriptTokenType.COLON) {
                    builder.advanceLexer();
                    parseExpression(builder);
                }
            } else {
                if (type == MagicScriptTokenType.IDENTIFIER || type == MagicScriptTokenType.STRING_LITERAL) {
                    builder.advanceLexer();
                } else {
                    parseExpression(builder);
                }
                if (builder.getTokenType() == MagicScriptTokenType.COLON) {
                    builder.advanceLexer();
                    parseExpression(builder);
                }
            }
            if (builder.getCurrentOffset() == before) {
                builder.advanceLexer();
            }
        }
        expect(builder, MagicScriptTokenType.RIGHT_BRACE, "'}'");
        marker.done(MAP_LITERAL);
    }

    private void parseListLiteral(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // [
        while (!builder.eof() && builder.getTokenType() != MagicScriptTokenType.RIGHT_BRACKET) {
            int before = builder.getCurrentOffset();
            if (builder.getTokenType() == MagicScriptTokenType.COMMA) {
                builder.advanceLexer();
                continue;
            }
            parseExpression(builder);
            if (builder.getCurrentOffset() == before) {
                builder.advanceLexer();
            }
        }
        expect(builder, MagicScriptTokenType.RIGHT_BRACKET, "']'");
        marker.done(LIST_LITERAL);
    }

    private void parseSelect(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // select
        while (!builder.eof() && !isLinqTerminator(builder)) {
            int before = builder.getCurrentOffset();
            IElementType type = builder.getTokenType();
            if (type == MagicScriptTokenType.COMMA
                    || isKeyword(builder, "from") || isKeyword(builder, "join")
                    || isKeyword(builder, "left") || isKeyword(builder, "where")
                    || isKeyword(builder, "group") || isKeyword(builder, "by")
                    || isKeyword(builder, "having") || isKeyword(builder, "order")
                    || isKeyword(builder, "limit") || isKeyword(builder, "offset")
                    || isKeyword(builder, "on") || isKeyword(builder, "as")) {
                builder.advanceLexer();
                continue;
            }
            parseExpression(builder);
            if (builder.getCurrentOffset() == before) {
                builder.advanceLexer();
            }
        }
        marker.done(SELECT_EXPRESSION);
    }

    private boolean isLinqTerminator(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        return type == null
                || type == MagicScriptTokenType.SEMICOLON
                || type == MagicScriptTokenType.RIGHT_PAREN
                || type == MagicScriptTokenType.RIGHT_BRACE
                || type == MagicScriptTokenType.RIGHT_BRACKET;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isKeyword(PsiBuilder builder, String keyword) {
        return builder.getTokenType() == MagicScriptTokenType.IDENTIFIER
                && keyword.equals(builder.getTokenText());
    }

    private static boolean atStatementEnd(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        return type == null
                || type == MagicScriptTokenType.SEMICOLON
                || type == MagicScriptTokenType.RIGHT_BRACE
                || type == MagicScriptTokenType.RIGHT_PAREN
                || type == MagicScriptTokenType.RIGHT_BRACKET;
    }

    private static void consumeSemicolon(PsiBuilder builder) {
        if (builder.getTokenType() == MagicScriptTokenType.SEMICOLON) {
            builder.advanceLexer();
        }
    }

    private static void expect(PsiBuilder builder, IElementType type, String text) {
        if (builder.getTokenType() == type) {
            builder.advanceLexer();
        } else if (!builder.eof()) {
            builder.error("Expected " + text);
        }
    }
}
