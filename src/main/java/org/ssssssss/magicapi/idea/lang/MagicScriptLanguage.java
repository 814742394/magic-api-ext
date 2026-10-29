package org.ssssssss.magicapi.idea.lang;

import com.intellij.lang.Language;

/**
 * The magic-script language used by magic-api interfaces (`.ms` resources).
 */
public final class MagicScriptLanguage extends Language {

    public static final MagicScriptLanguage INSTANCE = new MagicScriptLanguage();

    private MagicScriptLanguage() {
        super("MagicScript");
    }

    @Override
    public String getDisplayName() {
        return "MagicScript";
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }
}
