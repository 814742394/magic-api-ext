package org.ssssssss.magicapi.idea.completion;

/**
 * 脚本中可直接使用的隐式变量（由框架在请求进入时注入上下文，用户无需声明）。
 *
 * @param name        变量名，例如 {@code "path"}
 * @param type        静态类型（或最接近的 Java 类型），例如 {@code "Map<String, Object>"}
 * @param description 中文一句话说明
 */
public record ImplicitVariable(String name, String type, String description) {

    public ImplicitVariable {
        name = name == null ? "" : name;
        type = type == null ? "" : type;
        description = description == null ? "" : description;
    }
}
