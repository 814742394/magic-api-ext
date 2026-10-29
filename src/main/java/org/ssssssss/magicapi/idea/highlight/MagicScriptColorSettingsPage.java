package org.ssssssss.magicapi.idea.highlight;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * Colour settings page for magic-script, so users can tune every created key.
 */
public class MagicScriptColorSettingsPage implements com.intellij.openapi.options.colors.ColorSettingsPage {

    public static final TextAttributesKey MODULE = createTextAttributesKey(
            "MAGIC_SCRIPT_MODULE", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey BUILTIN_FUNCTION = createTextAttributesKey(
            "MAGIC_SCRIPT_BUILTIN_FUNCTION", DefaultLanguageHighlighterColors.STATIC_METHOD);
    public static final TextAttributesKey IMPLICIT_VARIABLE = createTextAttributesKey(
            "MAGIC_SCRIPT_IMPLICIT_VARIABLE", DefaultLanguageHighlighterColors.GLOBAL_VARIABLE);
    public static final TextAttributesKey SQL_KEYWORD = createTextAttributesKey(
            "MAGIC_SCRIPT_SQL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey SQL_PARAMETER = createTextAttributesKey(
            "MAGIC_SCRIPT_SQL_PARAMETER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey EMBEDDED_CODE = createTextAttributesKey(
            "MAGIC_SCRIPT_EMBEDDED_CODE", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR);

    private static final AttributesDescriptor[] DESCRIPTORS = {
            new AttributesDescriptor("Keyword", MagicScriptSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Built-in module", MODULE),
            new AttributesDescriptor("Built-in function", BUILTIN_FUNCTION),
            new AttributesDescriptor("Implicit variable (path, header, ...)", IMPLICIT_VARIABLE),
            new AttributesDescriptor("Number", MagicScriptSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", MagicScriptSyntaxHighlighter.STRING),
            new AttributesDescriptor("SQL keyword inside a multi-line string", SQL_KEYWORD),
            new AttributesDescriptor("SQL parameter (#{...}, ${...})", SQL_PARAMETER),
            new AttributesDescriptor("Embedded script block (```lang ... ```)", EMBEDDED_CODE),
            new AttributesDescriptor("Line comment", MagicScriptSyntaxHighlighter.LINE_COMMENT),
            new AttributesDescriptor("Block comment", MagicScriptSyntaxHighlighter.BLOCK_COMMENT),
            new AttributesDescriptor("Operation sign", MagicScriptSyntaxHighlighter.OPERATION_SIGN),
            new AttributesDescriptor("Braces and brackets", MagicScriptSyntaxHighlighter.BRACES),
            new AttributesDescriptor("Bad character", HighlighterColors.BAD_CHARACTER),
    };

    private static final String DEMO = """
            // magic-api interface
            import log;
            import 'java.util.Date' as Date;

            var id = path.id;
            var sql = \"\"\"
                select * from user where id = #{id}
            \"\"\";
            var user = db.selectOne(sql);
            log.info('loaded {}', user);

            if (user == null) {
                exit 404, 'not found';
            }
            return db.page('select * from user');
            """;

    @Override
    public @NotNull String getDisplayName() {
        return "MagicScript";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public @NotNull AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public @NotNull ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new MagicScriptSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return DEMO;
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
