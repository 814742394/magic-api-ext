package org.ssssssss.magicapi.idea.completion;

import java.util.List;

/**
 * magic-api 内置模块（脚本里以静态名访问，例如 {@code db.select(...)}）。
 *
 * @param name         脚本里的静态名，即 {@code @MagicModule("...")} 的值，例如 {@code "db"}
 * @param typeName     对应的 Java 类全名，例如
 *                     {@code "org.ssssssss.magicapi.modules.db.SQLModule"}
 * @param description  中文一句话说明
 * @param autoImported 是否无需 {@code import} 即可直接使用（默认只有 {@code db} 为 {@code true}，
 *                     由 {@code magic-api.auto-import-module} 配置决定）
 * @param members      该模块暴露给脚本的方法
 */
public record MagicModule(
        String name,
        String typeName,
        String description,
        boolean autoImported,
        List<MagicFunction> members) {

    public MagicModule {
        name = name == null ? "" : name;
        typeName = typeName == null ? "" : typeName;
        description = description == null ? "" : description;
        // 保持不可变：调用方即使传进来可变 List，也不会影响注册表内容。
        members = members == null ? List.of() : List.copyOf(members);
    }

    /**
     * 按名字查找成员方法（同名重载返回第一个）。
     *
     * @return 找到的方法，找不到返回 {@code null}
     */
    public MagicFunction findMember(String memberName) {
        for (MagicFunction member : members) {
            if (member.name().equals(memberName)) {
                return member;
            }
        }
        return null;
    }

    /**
     * 该模块的所有同名重载。
     */
    public List<MagicFunction> findOverloads(String memberName) {
        return members.stream()
                .filter(m -> m.name().equals(memberName))
                .toList();
    }

    /**
     * 脚本里需要显式 import 时使用的前缀，例如 {@code "import db;"}；自动导入的模块返回空串。
     */
    public String importHint() {
        return autoImported ? "" : "import " + name + ";";
    }
}
