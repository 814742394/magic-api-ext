package org.ssssssss.magicapi.idea;

import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.ssssssss.magicapi.idea.lang.MagicScriptFileType;

import java.util.Arrays;
import java.util.List;

/**
 * End-to-end check against the real sample scripts shipped in {@code samples/}.
 */
public class SampleFilesTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }

    private List<VirtualFile> sampleFiles() {
        VirtualFile dir = myFixture.findFileInTempDir("samples");
        assertNotNull("samples directory must be copied into the test fixture", dir);
        VirtualFile[] children = dir.getChildren();
        assertTrue("samples must not be empty", children.length > 0);
        return Arrays.asList(children);
    }

    public void testSamplesParseWithoutErrors() {
        myFixture.copyDirectoryToProject("samples", "samples");
        for (VirtualFile file : sampleFiles()) {
            PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
            assertNotNull(psiFile);
            assertEquals("must be recognised as MagicScript: " + file.getName(),
                    MagicScriptFileType.INSTANCE, psiFile.getFileType());
            PsiElement[] errors = PsiTreeUtil.collectElements(psiFile, element ->
                    element.getNode() != null
                            && element.getNode().getElementType() == com.intellij.psi.TokenType.ERROR_ELEMENT);
            assertEquals("parse errors in " + file.getName() + ": " + Arrays.toString(
                    Arrays.stream(errors).map(PsiElement::getText).toArray()),
                    0, errors.length);
        }
    }

    public void testSamplesHaveFoldingRegions() {
        myFixture.copyDirectoryToProject("samples", "samples");
        for (VirtualFile file : sampleFiles()) {
            PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
            assertNotNull(psiFile);
            Document document = com.intellij.psi.PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
            assertNotNull(document);
            FoldingBuilder builder = LanguageFolding.INSTANCE.forLanguage(psiFile.getLanguage());
            FoldingDescriptor[] descriptors = LanguageFolding.buildFoldingDescriptors(builder, psiFile, document, false);
            assertTrue("expected folding regions in " + file.getName(), descriptors.length > 0);
        }
    }
}
