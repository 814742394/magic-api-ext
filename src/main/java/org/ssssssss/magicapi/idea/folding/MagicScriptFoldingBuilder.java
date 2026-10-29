package org.ssssssss.magicapi.idea.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ssssssss.magicapi.idea.lang.MagicScriptTokenType;
import org.ssssssss.magicapi.idea.lang.psi.MagicScriptElementTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * magic-script 代码折叠。
 *
 * <p>折叠区域一律「默认不折叠」（{@link #isCollapsedByDefault} 恒返回 {@code false}），
 * 只负责把可折叠的区间注册给平台，折叠状态由用户通过 gutter 自行控制。</p>
 *
 * <p>支持的区域：</p>
 * <ul>
 *   <li>花括号块：{@code BLOCK_STATEMENT}、{@code MAP_LITERAL}、
 *       {@code LAMBDA_EXPRESSION}（lambda 的块体，折叠范围是内层
 *       {@code BLOCK_STATEMENT} 的花括号）；</li>
 *   <li>跨行的字符串字面量 {@code STRING_LITERAL}，典型的是
 *       <code>"""select ..."""</code> 形式的 SQL；</li>
 *   <li>内嵌脚本块 {@code EMBEDDED_CODE}，折叠成 <code>```lang```</code> 占位；</li>
 *   <li>连续的 {@code import} 语句，合并成一个区域，占位 {@code import (N)}；</li>
 *   <li>连续 {@code >=2} 行的行注释；</li>
 *   <li>Linq 的 {@code SELECT_EXPRESSION}。</li>
 * </ul>
 *
 * <p>实现上只沿 AST 逐层遍历（{@link ASTNode#getFirstChildNode()} /
 * {@link ASTNode#getTreeNext()}），不触发任何索引，因此可以在任意线程安全地运行。</p>
 */
public class MagicScriptFoldingBuilder extends FoldingBuilderEx {

    /** 花括号复合节点。 */
    private static final TokenSet BRACE_BLOCKS = TokenSet.create(
            MagicScriptElementTypes.BLOCK_STATEMENT,
            MagicScriptElementTypes.MAP_LITERAL
    );

    /** 至少两行连续行注释才折叠。 */
    private static final int MIN_COMMENT_RUN = 2;

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root,
                                                         @NotNull Document document,
                                                         boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        // 同一区间只注册一次，避免 lambda 的块体与内层 BLOCK_STATEMENT 产生重复区域
        Set<TextRange> seen = new HashSet<>();
        ASTNode node = root.getNode();
        if (node != null) {
            collect(node, descriptors, seen);
        }
        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    /**
     * 遍历一个复合节点的直接子节点；连续 import / 连续行注释在「兄弟节点」层面处理，
     * 所以这里必须按兄弟链扫描而不是简单的逐节点递归。
     */
    private void collect(@NotNull ASTNode parent,
                         @NotNull List<FoldingDescriptor> out,
                         @NotNull Set<TextRange> seen) {
        ASTNode child = parent.getFirstChildNode();
        while (child != null) {
            if (isImport(child)) {
                child = foldImportRun(child, out, seen);
                continue;
            }
            if (isLineComment(child)) {
                child = foldCommentRun(child, out, seen);
                continue;
            }
            foldSingle(child, out, seen);
            child = child.getTreeNext();
        }
    }

    /** 处理单个节点自身的折叠，并继续向下递归。 */
    private void foldSingle(@NotNull ASTNode node,
                            @NotNull List<FoldingDescriptor> out,
                            @NotNull Set<TextRange> seen) {
        IElementType type = node.getElementType();

        if (type == MagicScriptTokenType.STRING_LITERAL) {
            // 只有真正跨行的字符串才值得折叠（单行字符串折叠后就没有内容了）
            if (hasLineBreak(node.getText()) && node.getTextLength() > 6) {
                add(out, seen, node, node.getTextRange(), stringPlaceholder(node.getText()));
            }
            return;
        }

        if (type == MagicScriptTokenType.EMBEDDED_CODE) {
            if (node.getTextLength() > 6) {
                add(out, seen, node, node.getTextRange(), embeddedPlaceholder(node.getText()));
            }
            return;
        }

        if (type == MagicScriptElementTypes.SELECT_EXPRESSION) {
            if (node.getTextLength() > 6) {
                add(out, seen, node, node.getTextRange(), "select ...");
            }
            // 继续递归：select 内部还可能出现块和注释
        }

        TextRange braces = braceRange(node);
        if (braces != null && braces.getLength() > 2) {
            add(out, seen, node, braces, "{...}");
        }

        collect(node, out, seen);
    }

    /**
     * 计算节点上真正属于「块」的那对花括号区间。
     *
     * <p>对 {@code BLOCK_STATEMENT} / {@code MAP_LITERAL} 要求首尾直接子节点分别是
     * {@code {} 和 {@code }}；这样即使文件正在编辑、右花括号缺失，也不会把内部
     * 其它 map 的右括号当成自己的。</p>
     */
    private static @Nullable TextRange braceRange(@NotNull ASTNode node) {
        IElementType type = node.getElementType();
        if (BRACE_BLOCKS.contains(type)) {
            return braceRangeOfBlock(node);
        }
        if (type == MagicScriptElementTypes.LAMBDA_EXPRESSION) {
            // lambda 的块体在语法树里是内层 BLOCK_STATEMENT，花括号范围直接取它
            for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                if (child.getElementType() == MagicScriptElementTypes.BLOCK_STATEMENT) {
                    return braceRangeOfBlock(child);
                }
            }
        }
        return null;
    }

    private static @Nullable TextRange braceRangeOfBlock(@NotNull ASTNode block) {
        ASTNode first = block.getFirstChildNode();
        ASTNode last = block.getLastChildNode();
        if (first == null || last == null) {
            return null;
        }
        if (first.getElementType() != MagicScriptTokenType.LEFT_BRACE
                || last.getElementType() != MagicScriptTokenType.RIGHT_BRACE) {
            return null;
        }
        return new TextRange(first.getStartOffset(), last.getStartOffset() + last.getTextLength());
    }

    /**
     * 把连续的 import 语句折叠成一个区域，返回该连续段之后的第一个节点，
     * 便于外层继续扫描。
     */
    private static @Nullable ASTNode foldImportRun(@NotNull ASTNode first,
                                                   @NotNull List<FoldingDescriptor> out,
                                                   @NotNull Set<TextRange> seen) {
        ASTNode last = first;
        int count = 1;
        ASTNode after = nextSignificant(last);
        while (after != null && isImport(after)) {
            last = after;
            count++;
            after = nextSignificant(last);
        }
        if (count > 1) {
            TextRange range = new TextRange(first.getStartOffset(),
                    last.getStartOffset() + last.getTextLength());
            add(out, seen, first, range, "import (" + count + ")");
        }
        return after;
    }

    /**
     * 折叠连续的行注释；中间隔着空行就认为不属于同一段，返回该段之后的节点。
     */
    private static @Nullable ASTNode foldCommentRun(@NotNull ASTNode first,
                                                    @NotNull List<FoldingDescriptor> out,
                                                    @NotNull Set<TextRange> seen) {
        ASTNode last = first;
        int count = 1;
        ASTNode after = nextSignificant(last);
        while (after != null && isLineComment(after) && continuesRun(last, after)) {
            last = after;
            count++;
            after = nextSignificant(last);
        }
        if (count >= MIN_COMMENT_RUN) {
            TextRange range = new TextRange(first.getStartOffset(),
                    last.getStartOffset() + last.getTextLength());
            add(out, seen, first, range, "// ... (" + count + " 行)");
        }
        return after;
    }

    /** 两个行注释之间是否只隔了一个换行（没有空行）。 */
    private static boolean continuesRun(@NotNull ASTNode previous, @NotNull ASTNode next) {
        ASTNode node = previous.getTreeNext();
        int lineBreaks = 0;
        while (node != null && node != next) {
            String text = node.getText();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lineBreaks++;
                }
            }
            if (lineBreaks > 1) {
                return false;
            }
            node = node.getTreeNext();
        }
        return true;
    }

    private static @Nullable ASTNode nextSignificant(@NotNull ASTNode node) {
        ASTNode next = node.getTreeNext();
        while (next != null && next.getElementType() == TokenType.WHITE_SPACE) {
            next = next.getTreeNext();
        }
        return next;
    }

    private static boolean isImport(@NotNull ASTNode node) {
        return node.getElementType() == MagicScriptElementTypes.IMPORT_STATEMENT;
    }

    private static boolean isLineComment(@NotNull ASTNode node) {
        return node.getElementType() == MagicScriptTokenType.LINE_COMMENT;
    }

    /** 注册区域；同一区间重复注册会被平台判为重叠加并报错，这里直接去重。 */
    private static void add(@NotNull List<FoldingDescriptor> out,
                            @NotNull Set<TextRange> seen,
                            @NotNull ASTNode node,
                            @NotNull TextRange range,
                            @NotNull String placeholder) {
        if (range.getLength() <= 0 || !seen.add(range)) {
            return;
        }
        out.add(new FoldingDescriptor(node, range, null, placeholder));
    }

    private static boolean hasLineBreak(@Nullable String text) {
        return text != null && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0);
    }

    /** 多行字符串的占位：三引号字符串用 <code>"""..."""</code>，模板串用反引号。 */
    private static @NotNull String stringPlaceholder(@NotNull String text) {
        if (text.startsWith("\"\"\"")) {
            return "\"\"\"...\"\"\"";
        }
        if (text.startsWith("`")) {
            return "`...`";
        }
        return "...";
    }

    /**
     * 内嵌脚本块 <code>```lang ... ```</code> 折叠成 <code>```lang```</code>；
     * 拿不到语言名时退化成 <code>```...```</code>。
     */
    private static @NotNull String embeddedPlaceholder(@NotNull String text) {
        String language = "";
        if (text.startsWith("```")) {
            int end = text.indexOf('\n');
            String head = end < 0 ? text.substring(3) : text.substring(3, end);
            head = head.strip();
            if (head.endsWith("```")) {
                head = head.substring(0, head.length() - 3).strip();
            }
            language = head;
        }
        return "```" + language + "```";
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
        IElementType type = node.getElementType();
        if (type == MagicScriptTokenType.STRING_LITERAL) {
            return stringPlaceholder(node.getText());
        }
        if (type == MagicScriptTokenType.EMBEDDED_CODE) {
            return embeddedPlaceholder(node.getText());
        }
        if (type == MagicScriptElementTypes.SELECT_EXPRESSION) {
            return "select ...";
        }
        if (type == MagicScriptElementTypes.IMPORT_STATEMENT) {
            return "import ...";
        }
        if (type == MagicScriptTokenType.LINE_COMMENT) {
            return "// ...";
        }
        return "{...}";
    }

    /**
     * 单参数版本仍被 {@code FoldingBuilder} 声明为抽象方法，必须实现；
     * 平台真正取占位文本时走的是上面带 {@link TextRange} 的重载，这里直接委托给它。
     */
    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return getPlaceholderText(node, node.getTextRange());
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
