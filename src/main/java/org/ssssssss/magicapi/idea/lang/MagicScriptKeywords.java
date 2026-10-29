package org.ssssssss.magicapi.idea.lang;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Keywords of magic-script.
 *
 * <p>Mirrors {@code org.ssssssss.script.parsing.Parser#KEYWORDS} plus the words
 * the parser dispatches on directly ({@code assert}, {@code select},
 * {@code instanceof}) and the Linq clause words.</p>
 */
public final class MagicScriptKeywords {

    /** Words that start a statement. */
    public static final Set<String> STATEMENT_KEYWORDS = Set.of(
            "import", "var", "let", "const", "if", "else", "for", "in", "while",
            "return", "break", "continue", "exit", "assert", "try", "catch",
            "finally", "throw", "async", "new", "select"
    );

    /** All reserved words recognised by the language. */
    public static final Set<String> ALL = Set.of(
            "import", "as", "var", "let", "const", "return", "break", "continue",
            "if", "for", "in", "new", "true", "false", "null", "else", "try",
            "catch", "finally", "async", "while", "exit", "and", "or", "throw",
            "assert", "instanceof", "select", "from", "join", "left", "right",
            "group", "by", "having", "where", "on", "limit", "offset", "order",
            "desc", "asc"
    );

    /**
     * Words the magic-script parser special-cases but that are not reserved:
     * {@code and}/{@code or} are also valid member names after a dot.
     */
    public static final Set<String> LINQ_CLAUSES = Set.of(
            "from", "join", "left", "group", "by", "having", "where", "on",
            "limit", "offset", "order", "desc", "asc"
    );

    /** Reserved words that are also valid at the start of an expression. */
    public static final Set<String> EXPRESSION_KEYWORDS = Set.of(
            "true", "false", "null", "new", "async", "select"
    );

    private static final List<String> OPERATORS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "===", "!==", "++", "--", "+=", "-=",
            "*=", "/=", "%=", "&=", "|=", "^=", "&&", "||", "==", "!=", "<=",
            ">=", "<<", ">>", "<>", "::", "=>", "+", "-", "*", "/", "%", "&",
            "|", "^", "!", "~", "<", ">", "="
    );

    private MagicScriptKeywords() {
    }

    public static boolean isKeyword(@NotNull String word) {
        return ALL.contains(word);
    }

    /**
     * Operators ordered by descending length so that the lexer matches the
     * longest one first ({@code >>>} before {@code >>} before {@code >}).
     */
    public static List<String> operators() {
        return OPERATORS;
    }
}
