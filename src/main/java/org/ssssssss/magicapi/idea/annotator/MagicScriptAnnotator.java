package org.ssssssss.magicapi.idea.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.completion.MagicBuiltins;
import org.ssssssss.magicapi.idea.highlight.MagicScriptColorSettingsPage;
import org.ssssssss.magicapi.idea.lang.MagicScriptLanguage;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic highlighting and lightweight inspections for magic-script files:
 *
 * <ul>
 *   <li>built-in modules ({@code db}, {@code request}, ...) and implicit variables are
 *       coloured like globals, so they stand out from local variables;</li>
 *   <li>SQL keywords inside multi-line strings are highlighted;</li>
 *   <li>imported modules that are never referenced are reported as weak warnings.</li>
 * </ul>
 */
public class MagicScriptAnnotator implements Annotator {

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)\\b(select|from|where|and|or|not|insert\\s+into|values|update|set|delete\\s+from|"
                    + "left\\s+join|right\\s+join|inner\\s+join|join|on|group\\s+by|order\\s+by|"
                    + "having|limit|offset|union|all|distinct|as|in|is\\s+null|is\\s+not\\s+null|"
                    + "like|between|exists|case|when|then|else|end)\\b");

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element.getContainingFile().getLanguage() instanceof MagicScriptLanguage)) {
            return;
        }
        if (element.getNode() == null) {
            return;
        }
        if (element.getNode().getElementType() == MagicScriptTokenType.IDENTIFIER) {
            annotateIdentifier(element, holder);
        } else if (element.getNode().getElementType() == MagicScriptTokenType.STRING_LITERAL) {
            highlightSqlKeywords(element, holder);
        }
    }

    private void annotateIdentifier(PsiElement element, AnnotationHolder holder) {
        String text = element.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        PsiElement previous = previousSignificant(element);
        boolean afterDot = previous != null && previous.getNode() != null
                && (previous.getNode().getElementType() == MagicScriptTokenType.DOT
                || previous.getNode().getElementType() == MagicScriptTokenType.SAFE_DOT);
        if (afterDot) {
            return;
        }
        if (MagicBuiltins.findModule(text) != null) {
            highlight(element, holder, MagicScriptColorSettingsPage.MODULE);
            return;
        }
        if (MagicBuiltins.findImplicitVariable(text) != null) {
            highlight(element, holder, MagicScriptColorSettingsPage.IMPLICIT_VARIABLE);
            return;
        }
        if (MagicBuiltins.findGlobalFunction(text) != null && !isDeclarationName(element)) {
            highlight(element, holder, MagicScriptColorSettingsPage.BUILTIN_FUNCTION);
        }
    }

    private static boolean isDeclarationName(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent == null || parent.getNode() == null) {
            return false;
        }
        PsiElement previous = previousSignificant(element);
        if (previous == null) {
            return false;
        }
        String text = previous.getText();
        return "var".equals(text) || "let".equals(text) || "const".equals(text);
    }

    private static void highlight(PsiElement element, AnnotationHolder holder, TextAttributesKey key) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(element.getTextRange())
                .textAttributes(key)
                .create();
    }

    private void highlightSqlKeywords(PsiElement element, AnnotationHolder holder) {
        String text = element.getText();
        if (text == null || text.length() < 2) {
            return;
        }
        // Only multi-line strings are SQL in practice; single line strings stay plain.
        if (text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
            return;
        }
        int base = element.getTextRange().getStartOffset();
        Matcher matcher = SQL_KEYWORDS.matcher(text);
        while (matcher.find()) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(new TextRange(base + matcher.start(), base + matcher.end()))
                    .textAttributes(MagicScriptColorSettingsPage.SQL_KEYWORD)
                    .create();
        }
    }

    private static PsiElement previousSignificant(PsiElement element) {
        PsiElement previous = element.getPrevSibling();
        while (previous != null && previous.getNode() != null
                && previous.getNode().getElementType() == com.intellij.psi.TokenType.WHITE_SPACE) {
            previous = previous.getPrevSibling();
        }
        return previous;
    }
}
