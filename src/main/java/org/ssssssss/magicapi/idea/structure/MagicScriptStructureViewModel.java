package org.ssssssss.magicapi.idea.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;
import org.ssssssss.magicapi.idea.lang.psi.MagicScriptElementTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure view model for magic-script files.
 *
 * <p>Shows the outline a magic-api author cares about: the imports, every variable
 * declaration, every top-level lambda and the statements that produce the
 * response ({@code return}, {@code exit}), plus Linq {@code select} blocks.</p>
 */
public class MagicScriptStructureViewModel extends TextEditorBasedStructureViewModel {

    private final PsiFile myFile;

    public MagicScriptStructureViewModel(@NotNull PsiFile file) {
        super(file);
        this.myFile = file;
    }

    @Override
    public @NotNull StructureViewTreeElement getRoot() {
        return new MagicScriptStructureViewElement(myFile);
    }

    @Override
    public Sorter @NotNull [] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    protected Class<?> @NotNull [] getSuitableClasses() {
        return new Class<?>[]{PsiFile.class};
    }

    /**
     * Tree element backed by a PSI element; children are computed from the AST.
     */
    static final class MagicScriptStructureViewElement implements StructureViewTreeElement {

        private final PsiElement element;

        MagicScriptStructureViewElement(@NotNull PsiElement element) {
            this.element = element;
        }

        @Override
        public Object getValue() {
            return element;
        }

        @Override
        public @NotNull com.intellij.navigation.ItemPresentation getPresentation() {
            return new MagicScriptItemPresentation(element);
        }

        @Override
        public com.intellij.ide.util.treeView.smartTree.TreeElement @NotNull [] getChildren() {
            List<StructureViewTreeElement> children = new ArrayList<>();
            PsiElement child = element.getFirstChild();
            while (child != null) {
                if (isInteresting(child)) {
                    children.add(new MagicScriptStructureViewElement(child));
                } else if (child.getFirstChild() != null) {
                    // Look one level down for statements inside a script/block.
                    PsiElement grandChild = child.getFirstChild();
                    while (grandChild != null) {
                        if (isInteresting(grandChild)) {
                            children.add(new MagicScriptStructureViewElement(grandChild));
                        }
                        grandChild = grandChild.getNextSibling();
                    }
                }
                child = child.getNextSibling();
            }
            return children.toArray(StructureViewTreeElement.EMPTY_ARRAY);
        }

        private static boolean isInteresting(PsiElement element) {
            ASTNode node = element.getNode();
            if (node == null) {
                return false;
            }
            IElementType type = node.getElementType();
            return type == MagicScriptElementTypes.IMPORT_STATEMENT
                    || type == MagicScriptElementTypes.VARIABLE_DECLARATION
                    || type == MagicScriptElementTypes.RETURN_STATEMENT
                    || type == MagicScriptElementTypes.EXIT_STATEMENT
                    || type == MagicScriptElementTypes.SELECT_EXPRESSION
                    || type == MagicScriptElementTypes.LAMBDA_EXPRESSION;
        }

        @Override
        public void navigate(boolean requestFocus) {
            if (element instanceof com.intellij.pom.Navigatable navigatable) {
                navigatable.navigate(requestFocus);
            }
        }

        @Override
        public boolean canNavigate() {
            return element instanceof com.intellij.pom.Navigatable;
        }

        @Override
        public boolean canNavigateToSource() {
            return canNavigate();
        }
    }

    /**
     * Presentation: a short label per element kind plus the source line.
     */
    static final class MagicScriptItemPresentation implements com.intellij.navigation.ItemPresentation {

        private final PsiElement element;

        MagicScriptItemPresentation(@NotNull PsiElement element) {
            this.element = element;
        }

        @Override
        public String getPresentableText() {
            String text = element.getText();
            if (text == null) {
                return "";
            }
            String firstLine = text.split("\\R", 2)[0].strip();
            if (firstLine.length() > 60) {
                firstLine = firstLine.substring(0, 57) + "...";
            }
            return firstLine;
        }

        @Override
        public String getLocationString() {
            if (element.getNode() == null) {
                return null;
            }
            IElementType type = element.getNode().getElementType();
            if (type == MagicScriptElementTypes.IMPORT_STATEMENT) {
                return "import";
            }
            if (type == MagicScriptElementTypes.VARIABLE_DECLARATION) {
                return "variable";
            }
            if (type == MagicScriptElementTypes.RETURN_STATEMENT) {
                return "return";
            }
            if (type == MagicScriptElementTypes.EXIT_STATEMENT) {
                return "exit";
            }
            if (type == MagicScriptElementTypes.SELECT_EXPRESSION) {
                return "linq";
            }
            if (type == MagicScriptElementTypes.LAMBDA_EXPRESSION) {
                return "lambda";
            }
            return null;
        }

        @Override
        public javax.swing.Icon getIcon(boolean unused) {
            if (element.getNode() == null) {
                return null;
            }
            IElementType type = element.getNode().getElementType();
            if (type == MagicScriptElementTypes.IMPORT_STATEMENT) {
                return org.ssssssss.magicapi.idea.lang.MagicScriptIcons.MODULE;
            }
            if (type == MagicScriptElementTypes.LAMBDA_EXPRESSION) {
                return org.ssssssss.magicapi.idea.lang.MagicScriptIcons.FUNCTION;
            }
            return org.ssssssss.magicapi.idea.lang.MagicScriptIcons.VARIABLE;
        }
    }
}
