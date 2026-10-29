package org.ssssssss.magicapi.idea.lang;

import com.intellij.lang.PairedBraceMatcher;
import com.intellij.lang.BracePair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.lang.psi.MagicScriptElementTypes;

/**
 * Brace matching for {@code ()}, {@code []} and <code>{}</code>.
 */
public class MagicScriptBraceMatcher implements PairedBraceMatcher, MagicScriptElementTypes {

    private static final BracePair[] PAIRS = {
            new BracePair(MagicScriptTokenType.LEFT_PAREN, MagicScriptTokenType.RIGHT_PAREN, false),
            new BracePair(MagicScriptTokenType.LEFT_BRACKET, MagicScriptTokenType.RIGHT_BRACKET, false),
            new BracePair(MagicScriptTokenType.LEFT_BRACE, MagicScriptTokenType.RIGHT_BRACE, true),
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
