package org.ssssssss.magicapi.idea.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.lang.MagicScriptIcons;
import org.ssssssss.magicapi.idea.lang.MagicScriptLanguage;

import javax.swing.Icon;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Completion for magic-api scripts.
 *
 * <p>Three contexts are handled, following the decision tree in
 * {@code docs/magic-api-builtins.md}:</p>
 * <ol>
 *   <li>{@code import <module>} — the built-in modules of magic-api;</li>
 *   <li>{@code <module>.<cursor>} — the members of that module;</li>
 *   <li>expression start — implicit variables, global functions and keywords.</li>
 * </ol>
 *
 * <p>Everything is derived from the text before the caret; no index is touched,
 * so completion stays fast and works in scratch files too.</p>
 */
public class MagicScriptCompletionContributor extends CompletionContributor {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("\\bimport\\s+([A-Za-z_]\\w*)?$");
    private static final Pattern MEMBER_PATTERN =
            Pattern.compile("([A-Za-z_@][\\w@]*)\\s*(?:\\.|\\?\\.)\\s*([A-Za-z_]\\w*)?$");
    private static final Pattern ALIAS_PATTERN =
            Pattern.compile("\\bimport\\s+(\\w+)\\s+as\\s+(\\w+)");

    private static final Icon MODULE_ICON = MagicScriptIcons.MODULE;
    private static final Icon FUNCTION_ICON = MagicScriptIcons.FUNCTION;
    private static final Icon VARIABLE_ICON = MagicScriptIcons.VARIABLE;

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        PsiElement position = parameters.getPosition();
        PsiFile file = position.getContainingFile();
        if (file == null || !(file.getLanguage() instanceof MagicScriptLanguage)) {
            return;
        }
        // The platform inserts a dummy identifier at the caret when completion is
        // invoked programmatically; strip it so that both the context detection and
        // the prefix matcher see what the user actually typed.
        String text = file.getText();
        int offset = Math.min(parameters.getOffset(), text.length());
        String before = stripDummyIdentifier(text.substring(0, offset));
        boolean inString = insideStringLiteral(before, before.length());
        result = result.withPrefixMatcher(identifierPrefix(before));

        Matcher importMatcher = IMPORT_PATTERN.matcher(before);
        if (importMatcher.find() && !inString) {
            addModules(result);
            return;
        }

        Matcher memberMatcher = MEMBER_PATTERN.matcher(before);
        if (memberMatcher.find() && !inString) {
            String qualifier = resolveAlias(memberMatcher.group(1), before);
            MagicModule module = MagicBuiltins.findModule(qualifier);
            if (module != null) {
                addMembers(result, module);
                return;
            }
            MagicModule chainType = MagicBuiltins.findChainType(qualifier);
            if (chainType != null) {
                addMembers(result, chainType);
            }
            return;
        }

        addImplicitVariables(result);
        addGlobalFunctions(result);
        addModules(result, false);
        if (atStatementStart(before)) {
            addStatementKeywords(result);
        }
    }

    private static String stripDummyIdentifier(String text) {
        return text.replace(com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "");
    }

    /**
     * The identifier fragment directly before the caret, which becomes the prefix
     * matcher used to filter the elements added below.
     */
    private static String identifierPrefix(String before) {
        int end = before.length();
        int start = end;
        while (start > 0 && Character.isJavaIdentifierPart(before.charAt(start - 1))) {
            start--;
        }
        return before.substring(start, end);
    }

    /** Resolves {@code import db as d} so that {@code d.} completes the {@code db} members. */
    private static String resolveAlias(String qualifier, String before) {
        Matcher matcher = ALIAS_PATTERN.matcher(before);
        while (matcher.find()) {
            if (matcher.group(2).equals(qualifier)) {
                return matcher.group(1);
            }
        }
        return qualifier;
    }

    private static boolean insideStringLiteral(String text, int offset) {
        boolean inQuoted = false;
        boolean inTemplate = false;
        for (int i = 0; i < offset; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '"' && !inTemplate) {
                inQuoted = !inQuoted;
            } else if (c == '`') {
                inTemplate = !inTemplate;
            }
        }
        return inQuoted || inTemplate;
    }

    private static boolean atStatementStart(String before) {
        String trimmed = before.stripTrailing();
        if (trimmed.isEmpty()) {
            return true;
        }
        char last = trimmed.charAt(trimmed.length() - 1);
        return last == ';' || last == '{' || last == '}' || last == '\n'
                || trimmed.endsWith("=>") || trimmed.endsWith("else");
    }

    private static void addModules(CompletionResultSet result) {
        addModules(result, true);
    }

    private static void addModules(CompletionResultSet result, boolean explicitImport) {
        for (MagicModule module : MagicBuiltins.CORE_MODULES) {
            LookupElementBuilder element = LookupElementBuilder.create(module.name())
                    .withPresentableText(module.name())
                    .withIcon(MODULE_ICON)
                    .withTypeText(shortTypeName(module.typeName()), true)
                    .withTailText(module.autoImported() ? "  自动导入" : "  需 import", true)
                    .withLookupStrings(List.of(module.typeName(), shortTypeName(module.typeName())));
            result.addElement(element.withTailText("  " + module.description(), true));
        }
        if (explicitImport) {
            // Plugin modules are opt-in: only suggest them once the user imported one,
            // otherwise the completion would advertise modules their server may not have.
            for (MagicModule module : MagicBuiltins.OPTIONAL_PLUGIN_MODULES) {
                result.addElement(LookupElementBuilder.create(module.name())
                        .withPresentableText(module.name())
                        .withIcon(MODULE_ICON)
                        .withTypeText(shortTypeName(module.typeName()), true)
                        .withTailText("  插件模块", true));
            }
        }
    }

    private static void addMembers(CompletionResultSet result, MagicModule module) {
        for (MagicFunction function : module.members()) {
            LookupElementBuilder element = LookupElementBuilder.create(function.name())
                    .withPresentableText(function.name())
                    .withIcon(FUNCTION_ICON)
                    .withTypeText(shortTypeName(function.returnType()), true)
                    .withTailText(function.chainable() ? "  链式" : "", true)
                    .withLookupStrings(List.of(function.name(), function.signature()));
            result.addElement(element.withTailText("  " + function.description(), true));
        }
    }

    private static void addImplicitVariables(CompletionResultSet result) {
        for (ImplicitVariable variable : MagicBuiltins.IMPLICIT_VARIABLES) {
            result.addElement(LookupElementBuilder.create(variable.name())
                    .withPresentableText(variable.name())
                    .withIcon(VARIABLE_ICON)
                    .withTypeText(variable.type(), true)
                    .withTailText("  " + variable.description(), true));
        }
    }

    private static void addGlobalFunctions(CompletionResultSet result) {
        for (MagicFunction function : MagicBuiltins.GLOBAL_FUNCTIONS) {
            LookupElementBuilder element = LookupElementBuilder.create(function.name())
                    .withPresentableText(function.name())
                    .withIcon(FUNCTION_ICON)
                    .withTypeText(shortTypeName(function.returnType()), true)
                    .withTailText("  " + function.signature(), true);
            result.addElement(element);
        }
    }

    private static void addStatementKeywords(CompletionResultSet result) {
        for (String keyword : MagicBuiltins.STATEMENT_KEYWORDS) {
            result.addElement(LookupElementBuilder.create(keyword)
                    .withPresentableText(keyword)
                    .withTypeText("keyword", true));
        }
    }

    static String shortTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return "";
        }
        int generic = typeName.indexOf('<');
        if (generic > 0) {
            return typeName.substring(0, generic)
                    + typeName.substring(generic);
        }
        int dot = typeName.lastIndexOf('.');
        return dot >= 0 && dot < typeName.length() - 1 ? typeName.substring(dot + 1) : typeName;
    }
}
