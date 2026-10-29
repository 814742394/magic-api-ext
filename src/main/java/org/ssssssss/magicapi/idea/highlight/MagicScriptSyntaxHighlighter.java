package org.ssssssss.magicapi.idea.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Token to colour mapping for magic-script.
 */
public class MagicScriptSyntaxHighlighter extends SyntaxHighlighterBase {

    public static final TextAttributesKey KEYWORD = createTextAttributesKey(
            "MAGIC_SCRIPT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey NUMBER = createTextAttributesKey(
            "MAGIC_SCRIPT_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = createTextAttributesKey(
            "MAGIC_SCRIPT_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey LINE_COMMENT = createTextAttributesKey(
            "MAGIC_SCRIPT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BLOCK_COMMENT = createTextAttributesKey(
            "MAGIC_SCRIPT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey OPERATION_SIGN = createTextAttributesKey(
            "MAGIC_SCRIPT_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey BRACES = createTextAttributesKey(
            "MAGIC_SCRIPT_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey BRACKETS = createTextAttributesKey(
            "MAGIC_SCRIPT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey PARENTHESES = createTextAttributesKey(
            "MAGIC_SCRIPT_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey DOT = createTextAttributesKey(
            "MAGIC_SCRIPT_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey COMMA = createTextAttributesKey(
            "MAGIC_SCRIPT_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey SEMICOLON = createTextAttributesKey(
            "MAGIC_SCRIPT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey(
            "MAGIC_SCRIPT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final Map<IElementType, TextAttributesKey> KEYS = new HashMap<>();

    static {
        KEYS.put(MagicScriptTokenType.LINE_COMMENT, LINE_COMMENT);
        KEYS.put(MagicScriptTokenType.BLOCK_COMMENT, BLOCK_COMMENT);
        KEYS.put(MagicScriptTokenType.STRING_LITERAL, STRING);
        KEYS.put(MagicScriptTokenType.REGEXP_LITERAL, STRING);
        KEYS.put(MagicScriptTokenType.EMBEDDED_CODE, MagicScriptColorSettingsPage.EMBEDDED_CODE);
        KEYS.put(MagicScriptTokenType.INT_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.LONG_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.FLOAT_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.DOUBLE_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.DECIMAL_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.BYTE_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.SHORT_LITERAL, NUMBER);
        KEYS.put(MagicScriptTokenType.OPERATION_SIGN, OPERATION_SIGN);
        KEYS.put(MagicScriptTokenType.ASSIGN, OPERATION_SIGN);
        KEYS.put(MagicScriptTokenType.LAMBDA, OPERATION_SIGN);
        KEYS.put(MagicScriptTokenType.SPREAD, OPERATION_SIGN);
        KEYS.put(MagicScriptTokenType.QUESTION, OPERATION_SIGN);
        KEYS.put(MagicScriptTokenType.LEFT_BRACE, BRACES);
        KEYS.put(MagicScriptTokenType.RIGHT_BRACE, BRACES);
        KEYS.put(MagicScriptTokenType.LEFT_BRACKET, BRACKETS);
        KEYS.put(MagicScriptTokenType.RIGHT_BRACKET, BRACKETS);
        KEYS.put(MagicScriptTokenType.LEFT_PAREN, PARENTHESES);
        KEYS.put(MagicScriptTokenType.RIGHT_PAREN, PARENTHESES);
        KEYS.put(MagicScriptTokenType.DOT, DOT);
        KEYS.put(MagicScriptTokenType.SAFE_DOT, DOT);
        KEYS.put(MagicScriptTokenType.COMMA, COMMA);
        KEYS.put(MagicScriptTokenType.SEMICOLON, SEMICOLON);
        KEYS.put(MagicScriptTokenType.KEYWORD, KEYWORD);
        KEYS.put(MagicScriptTokenType.BAD_CHARACTER, BAD_CHARACTER);
        KEYS.put(TokenType.BAD_CHARACTER, BAD_CHARACTER);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        // Keywords are plain identifiers to the parser; the decorator tags them.
        return new MagicScriptHighlightingLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        TextAttributesKey key = KEYS.get(tokenType);
        return key == null ? EMPTY_ARRAY : pack(key);
    }

    private static final TextAttributesKey[] EMPTY_ARRAY = new TextAttributesKey[0];
}
