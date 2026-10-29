package org.ssssssss.magicapi.idea.highlight;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.lang.MagicScriptKeywords;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;
import org.ssssssss.magicapi.idea.lang.lexer.MagicScriptLexer;

/**
 * Highlighting lexer: wraps the parser lexer and re-tags reserved words as
 * {@link MagicScriptTokenType#KEYWORD}.
 *
 * <p>magic-script does not reserve keywords at the token level — the reference
 * tokenizer emits {@code Identifier} for {@code var}, {@code import},
 * {@code return}, ... and the parser dispatches on the text. The parser lexer must
 * keep that behaviour so the parser can rely on token text, but the highlighter
 * needs a distinct token type to colour them, hence this thin decorator.</p>
 */
public class MagicScriptHighlightingLexer extends LexerBase {

    private final MagicScriptLexer lexer = new MagicScriptLexer();
    private CharSequence buffer = "";
    private int bufferEnd;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.bufferEnd = endOffset;
        lexer.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    public int getState() {
        return lexer.getState();
    }

    @Override
    public @Nullable IElementType getTokenType() {
        IElementType type = lexer.getTokenType();
        if (type != MagicScriptTokenType.IDENTIFIER) {
            return type;
        }
        String text = lexer.getTokenText();
        return text != null && MagicScriptKeywords.HIGHLIGHT.contains(text)
                ? MagicScriptTokenType.KEYWORD
                : type;
    }

    @Override
    public int getTokenStart() {
        return lexer.getTokenStart();
    }

    @Override
    public int getTokenEnd() {
        return lexer.getTokenEnd();
    }

    @Override
    public void advance() {
        lexer.advance();
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return bufferEnd;
    }
}
