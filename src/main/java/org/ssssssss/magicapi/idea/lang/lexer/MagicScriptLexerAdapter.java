package org.ssssssss.magicapi.idea.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;

/**
 * Adapter that exposes {@link MagicScriptLexer} through the {@link LexerBase}
 * contract expected by the platform.
 */
public class MagicScriptLexerAdapter extends LexerBase {

    private final MagicScriptLexer lexer = new MagicScriptLexer();
    private CharSequence buffer = "";
    private int end;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.end = endOffset;
        lexer.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    public int getState() {
        return lexer.getState();
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return lexer.getTokenType();
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
        return end;
    }
}
