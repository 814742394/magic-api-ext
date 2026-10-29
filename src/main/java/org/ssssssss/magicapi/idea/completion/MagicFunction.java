package org.ssssssss.magicapi.idea.completion;

/**
 * magic-api 脚本中的一个可调用函数/方法。
 *
 * <p>{@code signature} 已经剔除脚本引擎自动注入的首个隐式参数
 * （{@code RuntimeContext runtimeContext}），只保留用户在脚本里实际能看到、
 * 能填写的那部分，例如 {@code "select(sqlOrXml: String)"}。</p>
 *
 * @param name        方法名，例如 {@code "select"}；重载之间同名
 * @param signature   展示用签名（含方法名与形参），例如 {@code "select(sqlOrXml: String[, params: Map])"}
 * @param returnType  返回值类型，例如 {@code "List<Map<String,Object>>"}
 * @param description 中文一句话说明
 * @param chainable   返回值是否可继续链式调用（返回自身或另一个链式类型，如
 *                    {@code db.cache(...)}、{@code db.table(...).where()}）
 */
public record MagicFunction(
        String name,
        String signature,
        String returnType,
        String description,
        boolean chainable) {

    public MagicFunction {
        name = name == null ? "" : name;
        signature = signature == null ? "" : signature;
        returnType = returnType == null ? "" : returnType;
        description = description == null ? "" : description;
    }

    /**
     * 构造一个非链式函数（大多数方法都是这种）。
     */
    public static MagicFunction of(String name, String signature, String returnType, String description) {
        return new MagicFunction(name, signature, returnType, description, false);
    }

    /**
     * 构造一个可继续链式调用的函数。
     */
    public static MagicFunction chain(String name, String signature, String returnType, String description) {
        return new MagicFunction(name, signature, returnType, description, true);
    }

    /**
     * 是否为无参方法（签名形如 {@code "transaction()"}）。
     */
    public boolean hasNoParameters() {
        int open = signature.indexOf('(');
        int close = signature.lastIndexOf(')');
        return open >= 0 && close > open && close - open == 1;
    }
}
