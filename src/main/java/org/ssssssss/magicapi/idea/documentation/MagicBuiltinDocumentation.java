package org.ssssssss.magicapi.idea.documentation;

import org.jetbrains.annotations.NotNull;
import org.ssssssss.magicapi.idea.completion.ImplicitVariable;
import org.ssssssss.magicapi.idea.completion.MagicBuiltins;
import org.ssssssss.magicapi.idea.completion.MagicFunction;
import org.ssssssss.magicapi.idea.completion.MagicModule;

import java.util.List;

/**
 * 把 {@link MagicFunction} / {@link MagicModule} / {@link ImplicitVariable} 渲染成
 * 编辑器 quick doc 用的 HTML 片段。
 *
 * <p>本类只依赖 JDK 与 JetBrains 注解，不引用 {@code com.intellij.*}，因此可脱离 IDE 编译。
 * 所有来自数据表的文本（方法名、签名、类型、说明）都经过 {@link #escape(String)} 处理，
 * 避免 {@code List<Map<String,Object>>} 这类尖括号被 HTML 当成标签吞掉。</p>
 */
public final class MagicBuiltinDocumentation {

    private static final String HINT_AUTO_IMPORTED = "magic-api 内置，无需 import";
    private static final String HINT_IMPLICIT = "magic-api 隐式变量，由框架在请求进入时注入，无需 import";

    private MagicBuiltinDocumentation() {
    }

    // ------------------------------------------------------------------
    // 函数
    // ------------------------------------------------------------------

    /**
     * 渲染一个模块成员方法（或全局函数）的快速文档。
     *
     * @param f     要展示的函数
     * @param owner 所属模块；全局函数/隐式变量场景可传 {@code null}，此时按「全局函数」渲染
     * @return HTML 片段
     */
    public static @NotNull String html(@NotNull MagicFunction f, MagicModule owner) {
        StringBuilder sb = new StringBuilder(512);

        sb.append("<div class=\"definition\"><b>");
        if (owner != null) {
            sb.append(escape(owner.name())).append('.');
        }
        sb.append(escape(f.name())).append("</b></div>");

        sb.append("<pre>").append(escape(f.signature())).append("</pre>");
        sb.append("<div class=\"content\">");

        if (!f.description().isEmpty()) {
            sb.append("<p>").append(escape(f.description())).append("</p>");
        }

        sb.append("<p>返回值：<code>").append(escape(f.returnType())).append("</code></p>");

        if (f.chainable()) {
            sb.append("<p>可链式调用：该方法返回对象自身或链式类型，可继续用 <code>.</code> 访问后续方法。</p>");
        }

        sb.append("<p class=\"hint\">").append(escape(importHint(owner))).append("</p>");
        sb.append("</div>");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 模块
    // ------------------------------------------------------------------

    /**
     * 渲染一个模块的快速文档：模块名、类型全名、说明、import 提示与成员一览。
     */
    public static @NotNull String html(@NotNull MagicModule m) {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("<div class=\"definition\"><b>").append(escape(m.name())).append("</b></div>");
        sb.append("<p>类型：<code>").append(escape(m.typeName())).append("</code></p>");
        if (!m.description().isEmpty()) {
            sb.append("<p>").append(escape(m.description())).append("</p>");
        }
        sb.append("<p class=\"hint\">").append(escape(importHint(m))).append("</p>");

        List<MagicFunction> members = m.members();
        if (!members.isEmpty()) {
            sb.append("<div class=\"content\"><p>可用方法：</p><ul>");
            for (MagicFunction member : members) {
                sb.append("<li><code>").append(escape(member.signature())).append("</code>");
                if (!member.description().isEmpty()) {
                    sb.append(" — ").append(escape(member.description()));
                }
                sb.append("</li>");
            }
            sb.append("</ul></div>");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 隐式变量
    // ------------------------------------------------------------------

    /**
     * 渲染一个隐式变量的快速文档：变量名、类型、注入说明与 import 提示。
     */
    public static @NotNull String html(@NotNull ImplicitVariable v) {
        StringBuilder sb = new StringBuilder(320);

        sb.append("<div class=\"definition\"><b>").append(escape(v.name())).append("</b></div>");
        sb.append("<p>类型：<code>").append(escape(v.type())).append("</code></p>");
        if (!v.description().isEmpty()) {
            sb.append("<p>").append(escape(v.description())).append("</p>");
        }
        sb.append("<p class=\"hint\">").append(escape(HINT_IMPLICIT)).append("</p>");
        return sb.toString();
    }


    // ------------------------------------------------------------------
    // 补全列表用的单行纯文本摘要（不含 HTML，供 LookupElement 的灰字使用）
    // ------------------------------------------------------------------

    /**
     * 模块的单行摘要，形如 {@code 类型全名 · 说明}，自动导入的模块追加标注。
     *
     * @return 摘要文本，模块为空说明时仍返回类型名（不返回 {@code null}，避免调用方判空）
     */
    public static @NotNull String lookupDescription(@NotNull MagicModule module) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(module.typeName());
        if (!module.description().isEmpty()) {
            sb.append(" · ").append(module.description());
        }
        if (module.autoImported()) {
            sb.append("（自动导入）");
        }
        return sb.toString();
    }

    /**
     * 成员方法的单行摘要，形如 {@code 签名 → 返回值 · 说明}。
     */
    public static @NotNull String lookupDescription(@NotNull MagicFunction function) {
        StringBuilder sb = new StringBuilder(96);
        sb.append(function.signature()).append(" → ").append(function.returnType());
        if (!function.description().isEmpty()) {
            sb.append(" · ").append(function.description());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /**
     * 依据所属对象给出「是否需要 import」的一行提示。
     *
     * <ul>
     *   <li>自动导入的模块（{@code db}）→ 无需 import</li>
     *   <li>可 import 的核心/插件模块 → {@code 需 import xxx;}</li>
     *   <li>链式类型（{@code NamedTable} / {@code Where}）→ 由 {@code db.table(...)} 返回，无需 import</li>
     *   <li>{@code null}（全局函数）→ 无需 import</li>
     * </ul>
     */
    private static @NotNull String importHint(MagicModule owner) {
        if (owner == null) {
            return HINT_AUTO_IMPORTED;
        }
        if (owner.autoImported()) {
            return HINT_AUTO_IMPORTED;
        }
        // NamedTable / Where 这类链式类型不是 import 目标，不能给出 "需 import NamedTable;" 的错误指引。
        if (MagicBuiltins.findModule(owner.name()) == null && MagicBuiltins.findChainType(owner.name()) != null) {
            return "由 db.table(...) 返回的链式对象，无需 import";
        }
        return "需 " + owner.importHint();
    }

    /**
     * HTML 转义：{@code &}、{@code <}、{@code >} 必须处理，{@code "} 与 {@code '} 一并转义以便安全嵌入属性。
     */
    static @NotNull String escape(@NotNull String text) {
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
