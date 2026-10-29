package org.ssssssss.magicapi.idea.lang;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Token types produced by {@link MagicScriptLexer}.
 *
 * <p>Leaf tokens are created without an explicit language so that they can be
 * attached to a PSI {@code LeafElement} lazily; composite element types are
 * created with @{@link MagicScriptLanguage} so that the parser can register
 * them in the {@link MagicScriptParserDefinition}.</p>
 */
public final class MagicScriptTokenType extends IElementType {

    public static final MagicScriptTokenType WHITE_SPACE = new MagicScriptTokenType("WHITE_SPACE");
    public static final MagicScriptTokenType LINE_COMMENT = new MagicScriptTokenType("LINE_COMMENT");
    public static final MagicScriptTokenType BLOCK_COMMENT = new MagicScriptTokenType("BLOCK_COMMENT");

    public static final MagicScriptTokenType IDENTIFIER = new MagicScriptTokenType("IDENTIFIER");
    public static final MagicScriptTokenType INT_LITERAL = new MagicScriptTokenType("INT_LITERAL");
    public static final MagicScriptTokenType LONG_LITERAL = new MagicScriptTokenType("LONG_LITERAL");
    public static final MagicScriptTokenType FLOAT_LITERAL = new MagicScriptTokenType("FLOAT_LITERAL");
    public static final MagicScriptTokenType DOUBLE_LITERAL = new MagicScriptTokenType("DOUBLE_LITERAL");
    public static final MagicScriptTokenType DECIMAL_LITERAL = new MagicScriptTokenType("DECIMAL_LITERAL");
    public static final MagicScriptTokenType BYTE_LITERAL = new MagicScriptTokenType("BYTE_LITERAL");
    public static final MagicScriptTokenType SHORT_LITERAL = new MagicScriptTokenType("SHORT_LITERAL");
    public static final MagicScriptTokenType STRING_LITERAL = new MagicScriptTokenType("STRING_LITERAL");
    public static final MagicScriptTokenType REGEXP_LITERAL = new MagicScriptTokenType("REGEXP_LITERAL");
    public static final MagicScriptTokenType EMBEDDED_CODE = new MagicScriptTokenType("EMBEDDED_CODE");

    public static final MagicScriptTokenType LEFT_PAREN = new MagicScriptTokenType("LEFT_PAREN");
    public static final MagicScriptTokenType RIGHT_PAREN = new MagicScriptTokenType("RIGHT_PAREN");
    public static final MagicScriptTokenType LEFT_BRACKET = new MagicScriptTokenType("LEFT_BRACKET");
    public static final MagicScriptTokenType RIGHT_BRACKET = new MagicScriptTokenType("RIGHT_BRACKET");
    public static final MagicScriptTokenType LEFT_BRACE = new MagicScriptTokenType("LEFT_BRACE");
    public static final MagicScriptTokenType RIGHT_BRACE = new MagicScriptTokenType("RIGHT_BRACE");
    public static final MagicScriptTokenType SEMICOLON = new MagicScriptTokenType("SEMICOLON");
    public static final MagicScriptTokenType COMMA = new MagicScriptTokenType("COMMA");
    public static final MagicScriptTokenType DOT = new MagicScriptTokenType("DOT");
    public static final MagicScriptTokenType SAFE_DOT = new MagicScriptTokenType("SAFE_DOT");
    public static final MagicScriptTokenType COLON = new MagicScriptTokenType("COLON");
    public static final MagicScriptTokenType DOUBLE_COLON = new MagicScriptTokenType("DOUBLE_COLON");
    public static final MagicScriptTokenType SPREAD = new MagicScriptTokenType("SPREAD");
    public static final MagicScriptTokenType LAMBDA = new MagicScriptTokenType("LAMBDA");
    public static final MagicScriptTokenType QUESTION = new MagicScriptTokenType("QUESTION");

    public static final MagicScriptTokenType ASSIGN = new MagicScriptTokenType("ASSIGN");
    public static final MagicScriptTokenType OPERATION_SIGN = new MagicScriptTokenType("OPERATION_SIGN");
    public static final MagicScriptTokenType BAD_CHARACTER = new MagicScriptTokenType("BAD_CHARACTER");

    private MagicScriptTokenType(@NotNull @NonNls String debugName) {
        super(debugName, null);
    }

    private MagicScriptTokenType(@NotNull @NonNls String debugName, MagicScriptLanguage language) {
        super(debugName, language);
    }
}
