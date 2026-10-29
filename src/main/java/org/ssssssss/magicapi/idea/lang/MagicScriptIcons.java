package org.ssssssss.magicapi.idea.lang;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Plugin icons. All are loaded from SVG resources so they scale with the UI.
 */
public final class MagicScriptIcons {

    public static final Icon FILE = load("/icons/magicapi.svg");
    public static final Icon MODULE = load("/icons/module.svg");
    public static final Icon FUNCTION = load("/icons/function.svg");
    public static final Icon VARIABLE = load("/icons/variable.svg");

    private MagicScriptIcons() {
    }

    private static @NotNull Icon load(@NotNull String path) {
        return IconLoader.getIcon(path, MagicScriptIcons.class);
    }
}
