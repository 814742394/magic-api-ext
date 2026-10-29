package org.ssssssss.magicapi.idea.lang;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.lang.lexer.MagicScriptLexer;
import org.ssssssss.magicapi.idea.lang.parser.MagicScriptParser;
import org.ssssssss.magicapi.idea.lang.psi.MagicScriptElementTypes;

/**
 * Parser definition wiring the lexer and parser into the platform.
 */
public class MagicScriptParserDefinition implements ParserDefinition, MagicScriptElementTypes {

    public static final IFileElementType FILE = new IFileElementType(MagicScriptLanguage.INSTANCE);

    public static final TokenSet COMMENTS = TokenSet.create(
            MagicScriptTokenType.LINE_COMMENT,
            MagicScriptTokenType.BLOCK_COMMENT
    );

    public static final TokenSet STRING_LITERALS = TokenSet.create(
            MagicScriptTokenType.STRING_LITERAL
    );

    public static final TokenSet WHITESPACES = TokenSet.create(
            com.intellij.psi.TokenType.WHITE_SPACE
    );

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new MagicScriptLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new MagicScriptParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRING_LITERALS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return new MagicScriptPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new MagicScriptFile(viewProvider);
    }

    /**
     * Default PSI element for every composite node; completion, folding and
     * structure view all work off the AST, so no per-node classes are needed.
     */
    public static class MagicScriptPsiElement extends ASTWrapperPsiElement {
        public MagicScriptPsiElement(@NotNull ASTNode node) {
            super(node);
        }

        @Override
        public void accept(@NotNull PsiElementVisitor visitor) {
            visitor.visitElement(this);
        }
    }

    public static class MagicScriptFile extends PsiFileBase {
        public MagicScriptFile(@NotNull FileViewProvider viewProvider) {
            super(viewProvider, MagicScriptLanguage.INSTANCE);
        }

        @Override
        public @NotNull FileType getFileType() {
            return MagicScriptFileType.INSTANCE;
        }

        @Override
        public String toString() {
            return "MagicScriptFile:" + getName();
        }
    }
}
