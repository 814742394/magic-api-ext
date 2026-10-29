package org.ssssssss.magicapi.idea.lang.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;

/**
 * Hand written lexer for magic-script.
 *
 * <p>Token rules follow {@code org.ssssssss.script.parsing.Tokenizer}:</p>
 * <ul>
 *   <li>{@code """..."""} strings may span lines (used for SQL);</li>
 *   <li>{@code ```lang ... ```} embedded script blocks are a single token;</li>
 *   <li>backtick templates, including {@code ${...}}, are a single token;</li>
 *   <li>regular expression literals are only recognised where an expression starts;</li>
 *   <li>identifiers may start with {@code @}.</li>
 * </ul>
 *
 * <p>The lexer is restartable: it only looks backwards from the token start, so
 * incremental re-lexing from any offset produces the same tokens.</p>
 */
public class MagicScriptLexer extends LexerBase {

    private static final String[] MULTI_CHAR_OPERATORS = {
            ">>>=", "===", "!==", "<<=", ">>=", ">>>", "==", "!=", "<=", ">=",
            "&&", "||", "&=", "|=", "^=", "+=", "-=", "*=", "/=", "%=", "++",
            "--", "<<", ">>", "<>", "::", "=>", "->"
    };

    private CharSequence buffer = "";
    private int bufferStart;
    private int endOffset;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.bufferStart = Math.max(0, startOffset);
        this.endOffset = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.tokenType = null;
        locateToken();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (tokenEnd >= endOffset) {
            tokenType = null;
            tokenStart = endOffset;
            tokenEnd = endOffset;
            return;
        }
        tokenStart = tokenEnd;
        locateToken();
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    // ------------------------------------------------------------------
    // Scanning
    // ------------------------------------------------------------------

    private void locateToken() {
        if (tokenStart >= endOffset) {
            tokenEnd = endOffset;
            tokenType = null;
            return;
        }
        char c = buffer.charAt(tokenStart);

        if (isWhitespace(c)) {
            int i = tokenStart + 1;
            while (i < endOffset && isWhitespace(buffer.charAt(i))) {
                i++;
            }
            setToken(i, TokenType.WHITE_SPACE);
            return;
        }

        if (c == '/' && tokenStart + 1 < endOffset) {
            char n = buffer.charAt(tokenStart + 1);
            if (n == '/') {
                setToken(scanLineComment(), MagicScriptTokenType.LINE_COMMENT);
                return;
            }
            if (n == '*') {
                setToken(scanBlockComment(), MagicScriptTokenType.BLOCK_COMMENT);
                return;
            }
            if (isRegexAllowed()) {
                int end = scanRegexEnd();
                if (end > 0) {
                    setToken(end, MagicScriptTokenType.REGEXP_LITERAL);
                    return;
                }
            }
        }

        if (c == '"' || c == '\'') {
            setToken(scanQuoted(c), MagicScriptTokenType.STRING_LITERAL);
            return;
        }

        if (c == '`') {
            if (matchAt(tokenStart, "```")) {
                setToken(scanEmbeddedLanguage(), MagicScriptTokenType.EMBEDDED_CODE);
                return;
            }
            setToken(scanTemplate(), MagicScriptTokenType.STRING_LITERAL);
            return;
        }

        if (isIdentifierStart(c)) {
            int i = tokenStart + 1;
            while (i < endOffset && isIdentifierPart(buffer.charAt(i))) {
                i++;
            }
            setToken(i, MagicScriptTokenType.IDENTIFIER);
            return;
        }

        if (isDigit(c) || (c == '.' && tokenStart + 1 < endOffset && isDigit(buffer.charAt(tokenStart + 1)))) {
            setToken(scanNumber(), null);
            tokenType = numberTypeOf(tokenStart, tokenEnd);
            return;
        }

        if (c == '.' && matchAt(tokenStart, "...")) {
            setToken(tokenStart + 3, MagicScriptTokenType.SPREAD);
            return;
        }

        if (c == '?' && tokenStart + 1 < endOffset && buffer.charAt(tokenStart + 1) == '.') {
            setToken(tokenStart + 2, MagicScriptTokenType.SAFE_DOT);
            return;
        }

        for (String operator : MULTI_CHAR_OPERATORS) {
            if (matchAt(tokenStart, operator)) {
                setToken(tokenStart + operator.length(), operatorType(operator));
                return;
            }
        }

        setToken(tokenStart + 1, singleCharType(c));
    }

    private void setToken(int end, IElementType type) {
        this.tokenEnd = Math.min(Math.max(end, tokenStart + 1), endOffset);
        this.tokenType = type;
    }

    private static IElementType singleCharType(char c) {
        return switch (c) {
            case '(' -> MagicScriptTokenType.LEFT_PAREN;
            case ')' -> MagicScriptTokenType.RIGHT_PAREN;
            case '[' -> MagicScriptTokenType.LEFT_BRACKET;
            case ']' -> MagicScriptTokenType.RIGHT_BRACKET;
            case '{' -> MagicScriptTokenType.LEFT_BRACE;
            case '}' -> MagicScriptTokenType.RIGHT_BRACE;
            case ';' -> MagicScriptTokenType.SEMICOLON;
            case ',' -> MagicScriptTokenType.COMMA;
            case '.' -> MagicScriptTokenType.DOT;
            case ':' -> MagicScriptTokenType.COLON;
            case '?' -> MagicScriptTokenType.QUESTION;
            case '=' -> MagicScriptTokenType.ASSIGN;
            case '+', '-', '*', '/', '%', '!', '~', '<', '>', '&', '|', '^' ->
                    MagicScriptTokenType.OPERATION_SIGN;
            default -> MagicScriptTokenType.BAD_CHARACTER;
        };
    }

    private static IElementType operatorType(String operator) {
        return switch (operator) {
            case "=>", "->" -> MagicScriptTokenType.LAMBDA;
            case "::" -> MagicScriptTokenType.DOUBLE_COLON;
            default -> MagicScriptTokenType.OPERATION_SIGN;
        };
    }

    private int scanLineComment() {
        int i = tokenStart + 2;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (c == '\n' || c == '\r') {
                break;
            }
            i++;
        }
        return i;
    }

    private int scanBlockComment() {
        int i = tokenStart + 2;
        while (i + 1 < endOffset) {
            if (buffer.charAt(i) == '*' && buffer.charAt(i + 1) == '/') {
                return i + 2;
            }
            i++;
        }
        return endOffset;
    }

    private int scanQuoted(char quote) {
        boolean triple = quote == '"' && matchAt(tokenStart, "\"\"\"");
        int width = triple ? 3 : 1;
        int i = tokenStart + width;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (triple) {
                if (c == '"' && matchAt(i, "\"\"\"")) {
                    return i + 3;
                }
            } else {
                if (c == quote) {
                    return i + 1;
                }
                if (c == '\n' || c == '\r') {
                    return i;
                }
            }
            i++;
        }
        return endOffset;
    }

    private int scanEmbeddedLanguage() {
        int closing = find("```", tokenStart + 3);
        return closing < 0 ? endOffset : closing + 3;
    }

    private int scanTemplate() {
        int i = tokenStart + 1;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '`') {
                return i + 1;
            }
            if (c == '$' && i + 1 < endOffset && buffer.charAt(i + 1) == '{') {
                int end = scanBalancedFrom(i + 2);
                if (end < 0) {
                    return endOffset;
                }
                i = end;
                continue;
            }
            i++;
        }
        return endOffset;
    }

    /**
     * Scans from {@code from} (just past an already consumed {@code {}), returning
     * the offset just past the balancing brace, or {@code -1} when unbalanced.
     */
    private int scanBalancedFrom(int from) {
        int depth = 1;
        int i = from;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (c == '\'' || c == '"') {
                i = scanQuoted(c);
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
            i++;
        }
        return -1;
    }

    private int scanRegexEnd() {
        int i = tokenStart + 1;
        boolean inClass = false;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '\n' || c == '\r') {
                return -1;
            }
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                i++;
                while (i < endOffset && isRegexFlag(buffer.charAt(i))) {
                    i++;
                }
                return i;
            }
            i++;
        }
        return -1;
    }

    private int scanNumber() {
        int i = tokenStart;
        if (buffer.charAt(i) == '0' && i + 1 < endOffset) {
            char n = buffer.charAt(i + 1);
            if (n == 'x' || n == 'X') {
                i += 2;
                while (i < endOffset && (isHexDigit(buffer.charAt(i)) || buffer.charAt(i) == '_')) {
                    i++;
                }
                return consumeNumericSuffix(i);
            }
            if (n == 'b' || n == 'B') {
                i += 2;
                while (i < endOffset && (buffer.charAt(i) == '0' || buffer.charAt(i) == '1')) {
                    i++;
                }
                return consumeNumericSuffix(i);
            }
        }
        while (i < endOffset && (isDigit(buffer.charAt(i)) || buffer.charAt(i) == '_')) {
            i++;
        }
        if (i < endOffset && buffer.charAt(i) == '.') {
            i++;
            while (i < endOffset && (isDigit(buffer.charAt(i)) || buffer.charAt(i) == '_')) {
                i++;
            }
        }
        if (i < endOffset && (buffer.charAt(i) == 'e' || buffer.charAt(i) == 'E')) {
            int save = i;
            i++;
            if (i < endOffset && (buffer.charAt(i) == '+' || buffer.charAt(i) == '-')) {
                i++;
            }
            int digitsStart = i;
            while (i < endOffset && isDigit(buffer.charAt(i))) {
                i++;
            }
            if (i == digitsStart) {
                i = save;
            }
        }
        return consumeNumericSuffix(i);
    }

    private int consumeNumericSuffix(int i) {
        if (i < endOffset && isNumericSuffix(buffer.charAt(i))) {
            return i + 1;
        }
        return i;
    }

    private IElementType numberTypeOf(int start, int end) {
        if (end <= start) {
            return MagicScriptTokenType.INT_LITERAL;
        }
        char last = buffer.charAt(end - 1);
        boolean floatish = false;
        for (int i = start; i < end - 1; i++) {
            char c = buffer.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                floatish = true;
                break;
            }
        }
        return switch (Character.toLowerCase(last)) {
            case 'b' -> MagicScriptTokenType.BYTE_LITERAL;
            case 's' -> MagicScriptTokenType.SHORT_LITERAL;
            case 'l' -> MagicScriptTokenType.LONG_LITERAL;
            case 'm' -> MagicScriptTokenType.DECIMAL_LITERAL;
            case 'f' -> MagicScriptTokenType.FLOAT_LITERAL;
            case 'd' -> MagicScriptTokenType.DOUBLE_LITERAL;
            default -> floatish ? MagicScriptTokenType.DOUBLE_LITERAL : MagicScriptTokenType.INT_LITERAL;
        };
    }

    // ------------------------------------------------------------------
    // Regex disambiguation
    // ------------------------------------------------------------------

    /**
     * A {@code /} starts a regex when the preceding significant character can not
     * end an expression — the same heuristic the reference tokenizer applies to
     * the previous token.
     */
    private boolean isRegexAllowed() {
        int i = tokenStart - 1;
        while (i >= bufferStart) {
            char c = buffer.charAt(i);
            if (isWhitespace(c)) {
                i--;
                continue;
            }
            break;
        }
        if (i < bufferStart) {
            return true;
        }
        char c = buffer.charAt(i);
        return switch (c) {
            case '(', '[', '{', ',', ';', ':', '?', '=', '!', '+', '-', '*', '%', '&', '|', '^', '~', '<', '>', '/' -> true;
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // Character helpers
    // ------------------------------------------------------------------

    private boolean matchAt(int offset, String text) {
        if (offset < 0 || offset + text.length() > endOffset) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (buffer.charAt(offset + i) != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int find(String text, int from) {
        int last = endOffset - text.length();
        for (int i = Math.max(from, 0); i <= last; i++) {
            if (matchAt(i, text)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '\f' || c == '\u000B';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c) || c == '@';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private static boolean isRegexFlag(char c) {
        return c == 'g' || c == 'i' || c == 'm' || c == 's' || c == 'u' || c == 'y';
    }

    private static boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isNumericSuffix(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'b', 's', 'l', 'f', 'd', 'm' -> true;
            default -> false;
        };
    }
}
