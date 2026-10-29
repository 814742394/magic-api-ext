package org.ssssssss.magicapi.idea;

import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.ssssssss.magicapi.idea.documentation.MagicScriptDocumentationProvider;
import org.ssssssss.magicapi.idea.lang.MagicScriptFileType;

import java.util.Arrays;
import java.util.List;

/**
 * Feature level tests: completion, documentation and folding must work against the
 * real platform in a light fixture.
 */
public class FeatureTest extends BasePlatformTestCase {

    private PsiFile createFile(String text) {
        return PsiFileFactory.getInstance(getProject())
                .createFileFromText("demo.ms", MagicScriptFileType.INSTANCE, text);
    }

    private List<String> completionAt(String textWithCaret) {
        int caret = textWithCaret.indexOf("<caret>");
        String text = textWithCaret.replace("<caret>", "");
        myFixture.configureByText(MagicScriptFileType.INSTANCE, text);
        myFixture.getEditor().getCaretModel().moveToOffset(caret);
        com.intellij.codeInsight.lookup.LookupElement[] elements =
                myFixture.complete(com.intellij.codeInsight.completion.CompletionType.BASIC);
        if (elements == null) {
            return List.of();
        }
        return Arrays.stream(elements)
                .map(e -> e.getLookupString())
                .toList();
    }

    public void testImportCompletionListsModules() {
        List<String> completions = completionAt("import <caret>");
        assertTrue("db must be suggested, got " + completions, completions.contains("db"));
        assertTrue("request must be suggested, got " + completions, completions.contains("request"));
        assertTrue("log must be suggested, got " + completions, completions.contains("log"));
        assertFalse("query is not a module, got " + completions, completions.contains("query"));
        assertFalse("assert is a keyword, not a module, got " + completions, completions.contains("assert"));
    }

    public void testModuleMemberCompletion() {
        List<String> completions = completionAt("var x = db.<caret>");
        assertTrue("db.select must be suggested, got " + completions, completions.contains("select"));
        assertTrue("db.page must be suggested, got " + completions, completions.contains("page"));
        assertTrue("db.transaction must be suggested, got " + completions, completions.contains("transaction"));
    }

    public void testImportAliasCompletesMembers() {
        List<String> completions = completionAt("import db as d;\nvar x = d.<caret>");
        assertTrue("alias must resolve to db members, got " + completions, completions.contains("select"));
    }

    public void testImplicitVariableCompletion() {
        List<String> completions = completionAt("var id = <caret>");
        assertTrue("path must be suggested, got " + completions, completions.contains("path"));
        assertTrue("header must be suggested, got " + completions, completions.contains("header"));
        assertFalse("query is never injected, got " + completions, completions.contains("query"));
    }

    /**
     * With a unique prefix the platform inserts the only match directly and
     * {@code complete()} returns {@code null}; the inserted text is what matters.
     */
    public void testImplicitVariableAutoInsert() {
        myFixture.configureByText(MagicScriptFileType.INSTANCE, "var id = pa");
        myFixture.getEditor().getCaretModel().moveToOffset(myFixture.getFile().getTextLength());
        myFixture.complete(com.intellij.codeInsight.completion.CompletionType.BASIC);
        assertTrue("completion must insert 'path', got " + myFixture.getEditor().getDocument().getText(),
                myFixture.getEditor().getDocument().getText().contains("path"));
    }

    public void testDocumentationForModule() {
        PsiFile file = createFile("return db.select('select 1');");
        MagicScriptDocumentationProvider provider = new MagicScriptDocumentationProvider();
        com.intellij.psi.PsiElement element = file.findElementAt(7);
        assertNotNull(element);
        String doc = provider.generateDoc(element, element);
        assertNotNull("db.select must have documentation", doc);
        assertTrue(doc, doc.contains("select"));
    }

    public void testFoldingForMultiLineStringAndBlock() {
        myFixture.configureByText(MagicScriptFileType.INSTANCE, """
                var sql = \"\"\"
                  select * from t
                \"\"\";
                if (true) {
                  return sql;
                }
                """);
        PsiFile file = myFixture.getFile();
        Document document = myFixture.getEditor().getDocument();
        FoldingBuilder builder = LanguageFolding.INSTANCE.forLanguage(file.getLanguage());
        assertNotNull("MagicScript must have a folding builder", builder);
        FoldingDescriptor[] descriptors = LanguageFolding.buildFoldingDescriptors(builder, file, document, false);
        assertTrue("multi-line string and block must fold, got " + descriptors.length,
                descriptors.length >= 2);
        assertTrue("multi-line string must fold to a \"\"\"...\"\"\" placeholder",
                Arrays.stream(descriptors).anyMatch(d -> d.getPlaceholderText() != null
                        && d.getPlaceholderText().contains("\"\"\"")));
    }

    public void testFoldingImportRun() {
        myFixture.configureByText(MagicScriptFileType.INSTANCE, """
                import log;
                import request;
                import response;
                return 1;
                """);
        PsiFile file = myFixture.getFile();
        Document document = myFixture.getEditor().getDocument();
        FoldingBuilder builder = LanguageFolding.INSTANCE.forLanguage(file.getLanguage());
        FoldingDescriptor[] descriptors = LanguageFolding.buildFoldingDescriptors(builder, file, document, false);
        assertTrue("three imports must fold into one region, got " + descriptors.length,
                Arrays.stream(descriptors).anyMatch(d -> "import (3)".equals(d.getPlaceholderText())));
    }
}
