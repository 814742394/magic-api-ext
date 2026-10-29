package org.ssssssss.magicapi.idea.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.completion.ImplicitVariable;
import org.ssssssss.magicapi.idea.completion.MagicBuiltins;
import org.ssssssss.magicapi.idea.completion.MagicFunction;
import org.ssssssss.magicapi.idea.completion.MagicModule;
import org.ssssssss.magicapi.idea.lang.MagicScriptLanguage;

/**
 * Quick documentation (Ctrl+Q / F1) for magic-api built-in modules, their
 * members and the implicit request variables.
 *
 * <p>The element under the caret is inspected textually: for {@code db.select}
 * the qualifier resolves to the {@code db} module and the selector to
 * {@code select}. No index is involved.</p>
 */
public class MagicScriptDocumentationProvider extends AbstractDocumentationProvider {

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file,
                                                             @Nullable PsiElement contextElement, int targetOffset) {
        return contextElement;
    }

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        PsiElement target = element != null ? element : originalElement;
        if (target == null || !(target.getContainingFile().getLanguage() instanceof MagicScriptLanguage)) {
            return null;
        }
        String fileText = target.getContainingFile().getText();
        int offset = target.getTextOffset();
        String word = identifierAt(fileText, offset);
        if (word == null) {
            return null;
        }

        String qualifier = qualifierBefore(fileText, offset);
        if (qualifier != null) {
            String resolved = resolveAlias(qualifier, fileText);
            MagicModule module = MagicBuiltins.findModule(resolved);
            if (module == null) {
                module = MagicBuiltins.findChainType(resolved);
            }
            if (module != null) {
                for (MagicFunction member : module.findOverloads(word)) {
                    return MagicBuiltinDocumentation.html(member, module);
                }
            }
            return null;
        }

        MagicModule module = MagicBuiltins.findModule(word);
        if (module != null) {
            return MagicBuiltinDocumentation.html(module);
        }
        ImplicitVariable variable = MagicBuiltins.findImplicitVariable(word);
        if (variable != null) {
            return MagicBuiltinDocumentation.html(variable);
        }
        MagicFunction function = MagicBuiltins.findGlobalFunction(word);
        if (function != null) {
            return MagicBuiltinDocumentation.html(function, null);
        }
        return null;
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        if (element == null || !(element.getContainingFile().getLanguage() instanceof MagicScriptLanguage)) {
            return null;
        }
        String text = element.getContainingFile().getText();
        int offset = element.getTextOffset();
        String word = identifierAt(text, offset);
        if (word == null) {
            return null;
        }
        MagicModule module = MagicBuiltins.findModule(word);
        if (module != null) {
            return "magic-api module " + module.name() + " : " + module.typeName();
        }
        MagicFunction function = MagicBuiltins.findGlobalFunction(word);
        if (function != null) {
            return function.signature();
        }
        return null;
    }

    /**
     * Word starting at {@code offset}, or {@code null} when the offset is not on an identifier.
     */
    static @Nullable String identifierAt(String text, int offset) {
        if (offset < 0 || offset >= text.length()) {
            return null;
        }
        int start = offset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        int end = offset;
        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        return start == end ? null : text.substring(start, end);
    }

    /**
     * If the identifier at {@code offset} is the selector of a member access, the
     * qualifier text (e.g. {@code db} in {@code db.select}).
     */
    static @Nullable String qualifierBefore(String text, int offset) {
        int i = offset - 1;
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return null;
        }
        if (text.charAt(i) == '.') {
            i--;
            if (i >= 0 && text.charAt(i) == '?') {
                i--;
            }
        } else {
            return null;
        }
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
            i--;
        }
        int end = i + 1;
        while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) {
            i--;
        }
        int start = i + 1;
        return start >= end ? null : text.substring(start, end);
    }

    private static String resolveAlias(String qualifier, String fileText) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\bimport\\s+(\\w+)\\s+as\\s+" + java.util.regex.Pattern.quote(qualifier))
                .matcher(fileText);
        return matcher.find() ? matcher.group(1) : qualifier;
    }
}
