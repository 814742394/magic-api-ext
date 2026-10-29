package org.ssssssss.magicapi.idea.lang;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * File type for magic-api interface scripts (`.ms`).
 *
 * <p>magic-api stores every interface as a `.ms` file under
 * {@code magic-api.resource.location}; see
 * {@code AbstractPathMagicResourceStorage#getSuffix()} in magic-api.</p>
 */
public final class MagicScriptFileType extends LanguageFileType {

    public static final MagicScriptFileType INSTANCE = new MagicScriptFileType();

    public static final String DEFAULT_EXTENSION = "ms";

    private MagicScriptFileType() {
        super(MagicScriptLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "MagicScript";
    }

    @Override
    public @NotNull String getDescription() {
        return "magic-api interface script";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    public @Nullable Icon getIcon() {
        return MagicScriptIcons.FILE;
    }
}
