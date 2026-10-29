package org.ssssssss.magicapi.idea.lang.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.ssssssss.magicapi.idea.lang.MagicScriptLanguage;

/**
 * Composite element types produced by the magic-script parser.
 */
public class MagicScriptElementType extends IElementType {

    public MagicScriptElementType(@NonNls String debugName) {
        super(debugName, MagicScriptLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "MagicScript:" + super.toString();
    }
}
