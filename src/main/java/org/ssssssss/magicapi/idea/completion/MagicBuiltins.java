package org.ssssssss.magicapi.idea.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * magic-api 内置模块 / 全局函数 / 隐式变量的静态数据注册表。
 *
 * <p>数据全部来自 {@code docs/magic-api-builtins.md}（基准版本 magic-api 2.2.2 +
 * magic-script 1.9.0，以源码为准）。本类只依赖 JDK，不引用任何 {@code com.intellij.*}
 * 类型，因此可以脱离 IDE 单独编译与测试。</p>
 *
 * <p>几条刻意的取舍：</p>
 * <ul>
 *   <li>所有方法签名已剔除脚本引擎自动注入的首个隐式参数
 *       {@code RuntimeContext runtimeContext}。</li>
 *   <li>{@code response} 模块<b>不</b>包含官方文档残留的 {@code addCookie}/{@code addCookies}
 *       （2.2.2 源码中不存在）。</li>
 *   <li>隐式变量<b>不</b>包含 {@code query}（常量已定义但从未注入）。</li>
 *   <li>{@code redis}/{@code mongo}/{@code elasticsearch}/{@code nebula} 属于插件模块，
 *       方法集由 {@code DynamicMethod}/{@code DynamicAttribute} 在运行时决定，静态扫描拿不到，
 *       故只登记模块本身、成员留空，并默认不启用。</li>
 * </ul>
 */
public final class MagicBuiltins {

    private MagicBuiltins() {
    }

    // ------------------------------------------------------------------
    // 核心模块（文档 1.2 ~ 1.8）
    // ------------------------------------------------------------------

    /** {@code db} 模块：默认自动导入，无需 {@code import db;}。 */
    public static final MagicModule DB = new MagicModule(
            "db",
            "org.ssssssss.magicapi.modules.db.SQLModule",
            "数据库操作模块：SQL/XML 查询、分页、事务、缓存与单表 API（动态模块，按接口默认数据源切换）",
            true,
            dbMembers());

    /** {@code request} 模块。 */
    public static final MagicModule REQUEST = new MagicModule(
            "request",
            "org.ssssssss.magicapi.modules.servlet.RequestModule",
            "获取原始请求的模块：上传文件、数组型参数、请求头与客户端真实 IP",
            false,
            requestMembers());

    /** {@code response} 模块。 */
    public static final MagicModule RESPONSE = new MagicModule(
            "response",
            "org.ssssssss.magicapi.modules.servlet.ResponseModule",
            "构造响应结果的模块：分页/JSON/文本/图片/文件下载、响应头与输出流",
            false,
            responseMembers());

    /** {@code env} 模块。 */
    public static final MagicModule ENV = new MagicModule(
            "env",
            "org.ssssssss.magicapi.modules.spring.EnvModule",
            "读取 Spring 配置项（application.yml / properties）的模块",
            false,
            envMembers());

    /** {@code magic} 模块。 */
    public static final MagicModule MAGIC = new MagicModule(
            "magic",
            "org.ssssssss.magicapi.core.service.impl.DefaultMagicAPIService",
            "调用其它 magic-api 接口或「函数」资源的模块",
            false,
            magicMembers());

    /** {@code http} 模块：链式 HTTP 客户端。 */
    public static final MagicModule HTTP = new MagicModule(
            "http",
            "org.ssssssss.magicapi.modules.http.HttpModule",
            "链式 HTTP 客户端模块，用于调用外部接口（以动词方法终结链）",
            false,
            httpMembers());

    /** {@code log} 模块：动态注册的 SLF4J Logger，没有 {@code @MagicModule} 注解，只能硬编码。 */
    public static final MagicModule LOG = new MagicModule(
            "log",
            "org.slf4j.Logger",
            "日志模块：按当前脚本名创建 SLF4J Logger（动态注册，无 @MagicModule 注解）",
            false,
            logMembers());

    /** 七个核心模块，顺序即 {@code import} 补全的建议排序。 */
    public static final List<MagicModule> CORE_MODULES =
            List.of(DB, REQUEST, RESPONSE, ENV, MAGIC, HTTP, LOG);

    // ------------------------------------------------------------------
    // 链式类型（文档 1.2.1）
    // ------------------------------------------------------------------

    /**
     * {@code db.table(...)} 的返回类型：单表操作 API（风格抄 MyBatis-Plus）。
     */
    public static final MagicModule NAMED_TABLE = new MagicModule(
            "NamedTable",
            "org.ssssssss.magicapi.modules.db.table.NamedTable",
            "db.table(...) 返回的单表操作对象，方法均返回自身，可链式调用",
            false,
            namedTableMembers());

    /**
     * {@code NamedTable.where()} 的返回类型：条件构造器。
     */
    public static final MagicModule WHERE = new MagicModule(
            "Where",
            "org.ssssssss.magicapi.modules.db.table.Where",
            "db.table(...).where() 返回的条件构造器，条件方法均返回自身，可链式调用",
            false,
            whereMembers());

    /**
     * 链式返回类型的查找表（按类型名索引），键为 {@code NamedTable} / {@code Where} 这类
     * 类型短名，也接受完整类名。
     */
    public static final List<MagicModule> CHAIN_TYPES = List.of(NAMED_TABLE, WHERE);

    // ------------------------------------------------------------------
    // 插件模块（文档 1.9，默认不启用）
    // ------------------------------------------------------------------

    /** 插件模块：需用户自行引入 {@code magic-api-plugins} 对应 jar，默认不参与补全。 */
    public static final List<MagicModule> OPTIONAL_PLUGIN_MODULES = List.of(
            new MagicModule("redis", "org.ssssssss.magicapi.redis.RedisModule",
                    "Redis 操作模块（插件）：方法由 DynamicMethod 动态提供，静态扫描无法枚举", false, List.of()),
            new MagicModule("mongo", "org.ssssssss.magicapi.mongo.MongoModule",
                    "MongoDB 操作模块（插件）：DynamicAttribute，先取库再取集合", false, List.of()),
            new MagicModule("elasticsearch", "org.ssssssss.magicapi.elasticsearch.ElasticSearchModule",
                    "ElasticSearch 操作模块（插件）：需单独引入插件 jar", false, List.of()),
            new MagicModule("nebula", "org.ssssssss.magicapi.nebula.NebulaModule",
                    "Nebula Graph 操作模块（插件）：需单独引入插件 jar", false, List.of())
    );

    // ------------------------------------------------------------------
    // 全局函数（文档 3.1）
    // ------------------------------------------------------------------

    /** {@code @Function} 全局函数：脚本里直接 {@code f(...)} 调用，无需 import。 */
    public static final List<MagicFunction> GLOBAL_FUNCTIONS = List.of(
            // 字符串
            fn("is_blank", "is_blank(str: CharSequence)", "boolean", "判断字符串是否为空白"),
            fn("not_blank", "not_blank(str: CharSequence)", "boolean", "判断字符串是否非空白"),
            // 通用
            fn("uuid", "uuid()", "String", "生成随机 UUID"),
            fn("is_null", "is_null(value)", "boolean", "判断值是否为 null"),
            fn("not_null", "not_null(value)", "boolean", "判断值是否非 null"),
            // 输出
            fn("print", "print(value)", "void", "打印值（不换行）"),
            fn("println", "println(value)", "void", "打印值并换行"),
            fn("printf", "printf(format: String, args: Object...)", "void", "按格式串打印"),
            // 集合
            fn("range", "range(from: int, to: int)", "Iterator<Integer>", "生成闭区间整数序列，常用于 for 循环"),
            // 聚合
            fn("count", "count(value)", "int", "统计元素个数（Linq 聚合）"),
            fn("sum", "sum(value)", "Number", "对元素求和（Linq 聚合）"),
            fn("avg", "avg(value)", "Number", "求平均值（Linq 聚合）"),
            fn("max", "max(value)", "Object", "取最大值（Linq 聚合）"),
            fn("min", "min(value)", "Object", "取最小值（Linq 聚合）"),
            fn("group_concat", "group_concat(target, [separator: String])", "String", "分组拼接，分隔符默认 ,"),
            // 日期
            fn("now", "now()", "Date", "当前时间"),
            fn("current_timestamp", "current_timestamp()", "long", "当前秒级时间戳"),
            fn("current_timestamp_millis", "current_timestamp_millis()", "long", "当前毫秒时间戳"),
            fn("date_format", "date_format(target: Date|TemporalAccessor, [pattern: String])", "String",
                    "格式化日期，pattern 默认 yyyy-MM-dd HH:mm:ss"),
            // 空值
            fn("ifnull", "ifnull(target, trueValue)", "Object", "target 为 null 时取 trueValue"),
            // 数学
            fn("round", "round(target: Number, [scale: int])", "double", "四舍五入，scale 为小数位数"),
            fn("ceil", "ceil(target: Number)", "Number", "向上取整"),
            fn("floor", "floor(target: Number)", "Number", "向下取整"),
            fn("percent", "percent(target: Number, [scale: int])", "String", "转成百分比字符串"),
            // 数组
            fn("new_array", "new_array(size: int) | (componentType: Class, size: int) | (values: T...)", "Object",
                    "创建数组，支持三种重载"),
            fn("new_int_array", "new_int_array(size: int)", "int[]", "创建 int 数组"),
            fn("new_short_array", "new_short_array(size: int)", "short[]", "创建 short 数组"),
            fn("new_double_array", "new_double_array(size: int)", "double[]", "创建 double 数组"),
            fn("new_float_array", "new_float_array(size: int)", "float[]", "创建 float 数组"),
            fn("new_byte_array", "new_byte_array(size: int)", "byte[]", "创建 byte 数组"),
            fn("new_char_array", "new_char_array(size: int)", "char[]", "创建 char 数组"),
            fn("new_boolean_array", "new_boolean_array(size: int)", "boolean[]", "创建 boolean 数组"),
            fn("new_long_array", "new_long_array(size: int)", "long[]", "创建 long 数组")
    );

    // ------------------------------------------------------------------
    // 隐式变量（文档 2 / 2.2）
    // ------------------------------------------------------------------

    /** 普通接口脚本中可直接使用的隐式变量；<b>不含</b> {@code query}（源码从未注入）。 */
    public static final List<ImplicitVariable> IMPLICIT_VARIABLES = List.of(
            new ImplicitVariable("path", "Map<String, Object>",
                    "URL 路径变量，如 /user/{id} 可用 path.id"),
            new ImplicitVariable("header", "Map<String, Object>",
                    "全部请求头（LinkedCaseInsensitiveMap，大小写不敏感），如 header.token"),
            new ImplicitVariable("cookie", "CookieContext extends HashMap<String, String>",
                    "全部 Cookie（大小写不敏感），只读：get/size/isEmpty/containsKey，put 不会写回响应"),
            new ImplicitVariable("session", "SessionContext extends HashMap<String, Object>",
                    "会话属性读写代理：get 读属性、put 写属性"),
            new ImplicitVariable("body", "Object",
                    "整个请求体（Map 或 List），如 body.name；数组体可直接遍历")
    );

    /** 响应脚本（responseScript）专用变量（文档 2.2）。 */
    public static final List<ImplicitVariable> RESPONSE_SCRIPT_VARIABLES = List.of(
            new ImplicitVariable("code", "int", "响应码"),
            new ImplicitVariable("message", "String", "响应消息"),
            new ImplicitVariable("data", "Object", "业务数据"),
            new ImplicitVariable("apiInfo", "ApiInfo", "当前接口定义"),
            new ImplicitVariable("request", "MagicHttpServletRequest", "原生请求对象（仅响应脚本可用）"),
            new ImplicitVariable("response", "MagicHttpServletResponse", "原生响应对象（仅响应脚本可用）"),
            new ImplicitVariable("timestamp", "long", "当前毫秒时间戳"),
            new ImplicitVariable("requestTime", "long", "请求进入时间"),
            new ImplicitVariable("executeTime", "long", "执行耗时（毫秒）")
    );

    // ------------------------------------------------------------------
    // 语句关键字（文档 4.7）
    // ------------------------------------------------------------------

    /**
     * 语句开头 / 行首补全的关键字集合。注意 {@code assert} 虽未列入
     * {@code Parser.KEYWORDS} 常量，但由 {@code parseAssert()} 单独处理，仍是合法关键字。
     */
    public static final List<String> STATEMENT_KEYWORDS = List.of(
            "var", "let", "const", "if", "else", "for", "in", "while",
            "break", "continue", "return", "exit", "assert", "try", "catch",
            "finally", "throw", "import", "as", "new", "async", "instanceof",
            "true", "false", "null", "and", "or"
    );

    // ------------------------------------------------------------------
    // 索引
    // ------------------------------------------------------------------

    private static final Map<String, MagicModule> MODULE_INDEX = indexModules(CORE_MODULES);

    private static final Map<String, MagicModule> OPTIONAL_MODULE_INDEX =
            Collections.unmodifiableMap(indexModules(OPTIONAL_PLUGIN_MODULES));

    private static final Map<String, MagicModule> CHAIN_TYPE_INDEX = indexChainTypes();

    private static final Map<String, MagicFunction> GLOBAL_FUNCTION_INDEX = indexFunctions(GLOBAL_FUNCTIONS);

    private static final Map<String, ImplicitVariable> IMPLICIT_VARIABLE_INDEX =
            indexVariables(IMPLICIT_VARIABLES, RESPONSE_SCRIPT_VARIABLES);

    // ------------------------------------------------------------------
    // 查找辅助
    // ------------------------------------------------------------------

    /**
     * 按脚本静态名查找核心模块，例如 {@code findModule("db")}。
     *
     * <p>插件模块不在其中，需用 {@link #findOptionalModule(String)}。</p>
     *
     * @return 找到的模块，找不到返回 {@code null}
     */
    public static MagicModule findModule(String name) {
        return name == null ? null : MODULE_INDEX.get(name);
    }

    /**
     * 按名字查找插件模块（{@code redis}/{@code mongo}/{@code elasticsearch}/{@code nebula}）。
     */
    public static MagicModule findOptionalModule(String name) {
        return name == null ? null : OPTIONAL_MODULE_INDEX.get(name);
    }

    /**
     * 按类型短名或完整类名查找链式返回类型，例如 {@code findChainType("NamedTable")}。
     */
    public static MagicModule findChainType(String typeName) {
        if (typeName == null) {
            return null;
        }
        MagicModule module = CHAIN_TYPE_INDEX.get(typeName);
        if (module == null) {
            // 允许传 "java.util.List" 这类与链式类型无关的返回值，直接判空。
            module = CHAIN_TYPE_INDEX.get(simpleName(typeName));
        }
        return module;
    }

    /** 核心模块名，按 {@code import} 补全的推荐顺序。 */
    public static List<String> moduleNames() {
        return moduleNameList();
    }

    /** 插件模块名。 */
    public static List<String> optionalModuleNames() {
        return optionalModuleNameList();
    }

    /**
     * 按名字查找全局函数（{@code @Function}，无需 import）。
     *
     * @return 找到的函数，找不到返回 {@code null}
     */
    public static MagicFunction findGlobalFunction(String name) {
        return name == null ? null : GLOBAL_FUNCTION_INDEX.get(name);
    }

    /**
     * 按名字查找隐式变量：先查普通脚本变量（path/header/cookie/session/body），
     * 再查响应脚本变量（code/message/data/...，仅响应脚本可用）。
     *
     * @return 找到的变量，找不到返回 {@code null}
     */
    public static ImplicitVariable findImplicitVariable(String name) {
        return name == null ? null : IMPLICIT_VARIABLE_INDEX.get(name);
    }

    /** 仅在响应脚本中可用的变量。 */
    public static ImplicitVariable findResponseScriptVariable(String name) {
        if (name == null) {
            return null;
        }
        for (ImplicitVariable variable : RESPONSE_SCRIPT_VARIABLES) {
            if (variable.name().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    /** 是否为语句关键字（文档 4.7）。 */
    public static boolean isStatementKeyword(String word) {
        return word != null && STATEMENT_KEYWORD_INDEX.containsKey(word);
    }

    private static final Map<String, Boolean> STATEMENT_KEYWORD_INDEX = keywordIndex();

    // ------------------------------------------------------------------
    // 数据构造
    // ------------------------------------------------------------------

    private static List<MagicFunction> dbMembers() {
        return List.of(
                fn("select", "select(sqlOrXml: String)", "List<Map<String,Object>>", "查询列表结果"),
                fn("select", "select(sqlOrXml: String, params: Map)", "List<Map<String,Object>>", "带变量的查询列表结果"),
                fn("selectInt", "selectInt(sqlOrXml: String[, params: Map])", "Integer",
                        "查询 int，须保证结果只有一行一列"),
                fn("selectOne", "selectOne(sqlOrXml: String[, params: Map])", "Map<String,Object>", "查询单个对象"),
                fn("selectValue", "selectValue(sqlOrXml: String[, params: Map])", "Object",
                        "查询单个值，须保证结果只有一行一列"),
                fn("page", "page(sqlOrXml: String[, params: Map])", "Object",
                        "分页查询，分页参数自动从请求参数 pageNo/pageSize 获取"),
                fn("page", "page(sqlOrXml: String, limit: long, offset: long[, params: Map])", "Object",
                        "分页查询，分页条件手动传入"),
                fn("page", "page(countSqlOrXml: String, sqlOrXml: String[, params: Map])", "Object",
                        "分页查询，手动指定 count 语句"),
                fn("page", "page(count: int, sqlOrXml: String, limit: long, offset: long, params: Map)", "Object",
                        "分页查询，手动指定总数"),
                fn("update", "update(sqlOrXml: String[, params: Map])", "int", "执行增删改，返回受影响行数"),
                fn("insert", "insert(sqlOrXml: String[, params: Map])", "Object", "插入，返回主键"),
                fn("insert", "insert(sqlOrXml: String, primary: String[, params: Map])", "Object",
                        "指定主键列插入，返回主键"),
                fn("batchUpdate", "batchUpdate(sql: String, args: List<Object[]>)", "int",
                        "批量执行，占位符与数组下标对应"),
                fn("batchUpdate", "batchUpdate(sql: String, batchSize: int, args: List<Object[]>)", "int",
                        "带批大小的批量执行"),
                fn("batchUpdate", "batchUpdate(sqls: List<String>)", "int", "批量执行多条 SQL"),
                fn("call", "call(sqlOrXml: String)", "Object",
                        "调用存储过程，占位符语法 #{in} / @{out, java.sql.Types}"),
                fn("transaction", "transaction(callback: Function)", "Object",
                        "自动事务，回调内抛异常自动回滚"),
                fn("transaction", "transaction()", "Transaction",
                        "手动事务，返回对象含 commit() / rollback()"),
                chain("cache", "cache(cacheName: String[, ttl: long])", "SQLModule",
                        "使用命名缓存（ttl 单位毫秒），返回自身可继续链式"),
                chain("deleteCache", "deleteCache(cacheName: String)", "SQLModule",
                        "删除命名缓存，返回自身可继续链式"),
                chain("camel", "camel()", "SQLModule", "列名转小驼峰，返回自身可继续链式"),
                chain("pascal", "pascal()", "SQLModule", "列名转大驼峰，返回自身可继续链式"),
                chain("lower", "lower()", "SQLModule", "列名转小写，返回自身可继续链式"),
                chain("upper", "upper()", "SQLModule", "列名转大写，返回自身可继续链式"),
                chain("normal", "normal()", "SQLModule", "列名保持原样，返回自身可继续链式"),
                chain("columnCase", "columnCase(name: String)", "SQLModule",
                        "指定列名转换器，返回自身可继续链式"),
                chain("table", "table(tableName: String)", "NamedTable",
                        "进入单表操作 API，返回 NamedTable，可继续 .where()/.insert() 等"),
                fn("getDataSourceName", "getDataSourceName()", "String", "当前数据源名")
        );
    }

    private static List<MagicFunction> namedTableMembers() {
        return List.of(
                chain("logic", "logic()", "NamedTable",
                        "启用逻辑删除：delete 转 update，select 追加 logic_field <> logic_value"),
                chain("withBlank", "withBlank()", "NamedTable", "插入/修改时不过滤空值"),
                chain("primary", "primary(primary: String[, defaultValue: Object])", "NamedTable", "设置主键列"),
                chain("column", "column(property: String[, value: Object])", "NamedTable",
                        "设置查询列，或设置列值"),
                chain("columns", "columns(columns: String... | Collection<String>)", "NamedTable", "批量设置列"),
                chain("exclude", "exclude(property: String)", "NamedTable", "排除单个列"),
                chain("excludes", "excludes(properties: String... | List)", "NamedTable", "排除多个列"),
                chain("orderBy", "orderBy(property: String[, order: String])", "NamedTable",
                        "升序排序（order 可写 asc/desc）"),
                chain("orderByDesc", "orderByDesc(property: String[, order: String])", "NamedTable", "降序排序"),
                chain("groupBy", "groupBy(properties: String...)", "NamedTable", "分组"),
                chain("where", "where()", "Where", "进入条件构造，返回 Where，可继续 .eq()/.like() 等"),
                fn("insert", "insert([data: Map])", "Object", "单条插入，返回主键"),
                fn("batchInsert", "batchInsert(collection: Collection<Map>[, batchSize: int])", "int", "批量插入"),
                fn("update", "update([data: Map][, isUpdateBlank: boolean])", "int", "更新，返回受影响行数"),
                fn("save", "save([data: Map][, beforeQuery: boolean])", "Object",
                        "有主键则 update，无主键则 insert"),
                fn("delete", "delete()", "int", "删除（配合 logic() 转为更新）"),
                fn("select", "select()", "List<Map<String,Object>>", "查询列表"),
                fn("page", "page()", "Object", "分页查询"),
                fn("count", "count()", "int", "计数")
        );
    }

    private static List<MagicFunction> whereMembers() {
        return List.of(
                chain("eq", "eq(column: String, value: Object)", "Where", "等于（= / ==）"),
                chain("ne", "ne(column: String, value: Object)", "Where", "不等于（<>）"),
                chain("lt", "lt(column: String, value: Object)", "Where", "小于（<）"),
                chain("lte", "lte(column: String, value: Object)", "Where", "小于等于（<=）"),
                chain("gt", "gt(column: String, value: Object)", "Where", "大于（>）"),
                chain("gte", "gte(column: String, value: Object)", "Where", "大于等于（>=）"),
                chain("in", "in(column: String, value: Collection)", "Where", "in 条件（value 可为集合或数组）"),
                chain("notIn", "notIn(column: String, value: Collection)", "Where", "not in 条件"),
                chain("like", "like(column: String, value: Object)", "Where", "like 模糊匹配"),
                chain("notLike", "notLike(column: String, value: Object)", "Where", "not like 模糊匹配"),
                chain("isNull", "isNull(column: String)", "Where", "is null"),
                chain("isNotNull", "isNotNull(column: String)", "Where", "is not null"),
                chain("notNull", "notNull(column: String, value: Object)", "Where", "值非 null 时才拼该条件"),
                chain("notBlank", "notBlank(column: String, value: Object)", "Where", "值非空白时才拼该条件")
        );
    }

    private static List<MagicFunction> requestMembers() {
        return List.of(
                fn("getFile", "getFile(name: String)", "MultipartFile", "获取上传的单个文件（无文件时返回 null），静态方法"),
                fn("getFiles", "getFiles(name: String)", "List<MultipartFile>", "获取上传的文件集合，静态方法"),
                fn("get", "get()", "MagicHttpServletRequest", "获取原生请求对象，静态方法"),
                fn("getValues", "getValues(name: String)", "List<String>", "获取数组型请求参数"),
                fn("getHeaders", "getHeaders(name: String)", "List<String>", "获取同名请求头的所有值"),
                fn("getClientIP", "getClientIP(otherHeaderNames: String...)", "String", "获取客户端真实 IP")
        );
    }

    private static List<MagicFunction> responseMembers() {
        return List.of(
                fn("page", "page(total: long, values: List<Map<String,Object>>)", "Object", "构造分页结果"),
                fn("json", "json(value: Object)", "ResponseEntity<Object>", "直接返回 JSON，不再被框架包装"),
                fn("text", "text(text: String)", "ResponseEntity", "输出纯文本"),
                fn("download", "download(value: Object, filename: String)", "ResponseEntity<?>",
                        "文件下载，静态方法"),
                fn("image", "image(value: Object, mime: String)", "ResponseEntity",
                        "输出图片（mime 如 image/png）"),
                fn("redirect", "redirect(url: String)", "NullValue", "重定向到指定 URL"),
                chain("addHeader", "addHeader(key: String, value: String)", "ResponseModule", "追加响应头，返回自身"),
                chain("setHeader", "setHeader(key: String, value: String)", "ResponseModule", "设置响应头，返回自身"),
                fn("getOutputStream", "getOutputStream()", "OutputStream",
                        "获取输出流（之后应调用 response.end()）"),
                fn("end", "end()", "NullValue", "终止输出，框架不再包装返回值")
        );
    }

    private static List<MagicFunction> envMembers() {
        return List.of(
                fn("get", "get(key: String)", "String", "读取 Spring 配置项"),
                fn("get", "get(key: String, defaultValue: String)", "String", "读取配置项，缺失时用默认值")
        );
    }

    private static List<MagicFunction> magicMembers() {
        return List.of(
                fn("call", "call(method: String, path: String, parameters: Map)", "Object",
                        "调用另一个接口，返回带 code/message 的包装结果"),
                fn("execute", "execute(method: String, path: String, parameters: Map)", "Object",
                        "调用另一个接口，返回原始内容（不包装）"),
                fn("invoke", "invoke(path: String, parameters: Map)", "Object", "调用「函数」资源")
        );
    }

    private static List<MagicFunction> httpMembers() {
        return List.of(
                chain("connect", "connect(url: String)", "HttpModule", "创建请求对象，返回自身"),
                chain("param", "param(key: String, values: Object...)", "HttpModule", "URL 查询参数，返回自身"),
                chain("param", "param(values: Map)", "HttpModule", "批量 URL 查询参数，返回自身"),
                chain("data", "data(key: String, values: Object...)", "HttpModule", "表单参数，返回自身"),
                chain("data", "data(values: Map)", "HttpModule", "批量表单参数，返回自身"),
                chain("header", "header(key: String, value: String)", "HttpModule", "请求头，返回自身"),
                chain("header", "header(values: Map)", "HttpModule", "批量请求头，返回自身"),
                chain("method", "method(method: HttpMethod)", "HttpModule", "指定请求方法（默认 GET），返回自身"),
                chain("body", "body(requestBody: Object)", "HttpModule", "设置请求体，返回自身"),
                chain("entity", "entity(entity: HttpEntity<Object>)", "HttpModule", "自定义 HttpEntity，返回自身"),
                chain("contentType", "contentType(contentType: String | MediaType)", "HttpModule",
                        "设置 Content-Type，返回自身"),
                chain("expectBytes", "expectBytes()", "HttpModule", "指定返回 byte[]，返回自身"),
                chain("expectString", "expectString()", "HttpModule", "指定返回 String，返回自身"),
                fn("get", "get()", "ResponseEntity<?>", "发送 GET 请求（终结链）"),
                fn("post", "post()", "ResponseEntity<?>", "发送 POST 请求（终结链）"),
                fn("put", "put()", "ResponseEntity<?>", "发送 PUT 请求（终结链）"),
                fn("delete", "delete()", "ResponseEntity<?>", "发送 DELETE 请求（终结链）"),
                fn("head", "head()", "ResponseEntity<?>", "发送 HEAD 请求（终结链）"),
                fn("options", "options()", "ResponseEntity<?>", "发送 OPTIONS 请求（终结链）"),
                fn("trace", "trace()", "ResponseEntity<?>", "发送 TRACE 请求（终结链）"),
                fn("patch", "patch()", "ResponseEntity<?>", "发送 PATCH 请求（终结链）"),
                fn("execute", "execute()", "ResponseEntity<?>", "按已设置的 method 执行（终结链）")
        );
    }

    private static List<MagicFunction> logMembers() {
        List<MagicFunction> members = new ArrayList<>();
        for (String level : List.of("trace", "debug", "info", "warn", "error")) {
            members.add(fn(level,
                    level + "(msg: String[, arg: Object | args: Object... | t: Throwable])",
                    "void",
                    "打印 " + level.toUpperCase() + " 日志，{} 占位，支持单参/多参/带异常四种重载"));
        }
        for (String level : List.of("Trace", "Debug", "Info", "Warn", "Error")) {
            members.add(fn("is" + level + "Enabled",
                    "is" + level + "Enabled()",
                    "boolean",
                    "判断 " + level.toLowerCase() + " 级别是否开启"));
        }
        members.add(fn("getName", "getName()", "String", "logger 名（即当前脚本路径）"));
        return members;
    }

    // ------------------------------------------------------------------
    // 索引构造
    // ------------------------------------------------------------------

    private static Map<String, MagicModule> indexModules(List<MagicModule> modules) {
        Map<String, MagicModule> index = new LinkedHashMap<>();
        for (MagicModule module : modules) {
            index.put(module.name(), module);
        }
        return Collections.unmodifiableMap(index);
    }

    private static Map<String, MagicModule> indexChainTypes() {
        Map<String, MagicModule> index = new LinkedHashMap<>();
        for (MagicModule module : CHAIN_TYPES) {
            index.put(module.name(), module);
        }
        return Collections.unmodifiableMap(index);
    }

    private static Map<String, MagicFunction> indexFunctions(List<MagicFunction> functions) {
        Map<String, MagicFunction> index = new LinkedHashMap<>();
        for (MagicFunction function : functions) {
            // 同名重载保留第一条，补全展示时再按 name 聚合。
            index.putIfAbsent(function.name(), function);
        }
        return Collections.unmodifiableMap(index);
    }

    @SafeVarargs
    private static Map<String, ImplicitVariable> indexVariables(List<ImplicitVariable>... groups) {
        Map<String, ImplicitVariable> index = new LinkedHashMap<>();
        for (List<ImplicitVariable> group : groups) {
            for (ImplicitVariable variable : group) {
                index.putIfAbsent(variable.name(), variable);
            }
        }
        return Collections.unmodifiableMap(index);
    }

    private static Map<String, Boolean> keywordIndex() {
        Map<String, Boolean> index = new LinkedHashMap<>();
        for (String keyword : STATEMENT_KEYWORDS) {
            index.put(keyword, Boolean.TRUE);
        }
        return Collections.unmodifiableMap(index);
    }

    private static List<String> moduleNameList() {
        List<String> names = new ArrayList<>(CORE_MODULES.size());
        for (MagicModule module : CORE_MODULES) {
            names.add(module.name());
        }
        return List.copyOf(names);
    }

    private static List<String> optionalModuleNameList() {
        List<String> names = new ArrayList<>(OPTIONAL_PLUGIN_MODULES.size());
        for (MagicModule module : OPTIONAL_PLUGIN_MODULES) {
            names.add(module.name());
        }
        return List.copyOf(names);
    }

    /** 取类名最后一段，用于把返回值类型映射成链式类型键。 */
    private static String simpleName(String typeName) {
        int index = typeName.lastIndexOf('.');
        return index < 0 ? typeName : typeName.substring(index + 1);
    }

    // ------------------------------------------------------------------
    // 小工具
    // ------------------------------------------------------------------

    private static MagicFunction fn(String name, String signature, String returnType, String description) {
        return MagicFunction.of(name, signature, returnType, description);
    }

    private static MagicFunction chain(String name, String signature, String returnType, String description) {
        return MagicFunction.chain(name, signature, returnType, description);
    }
}
