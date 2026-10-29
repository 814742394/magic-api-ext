# magic-api 脚本内置模块与函数速查（补全/文档数据源）

> 用途：为 IntelliJ IDEA 插件的补全、跳转、文档气泡提供结构化数据。
> 生成日期：2026-10-29
> 基准版本：magic-api **2.2.2**（本地源码 `/tmp/ma-probe`，commit `e1cdcca`）+ magic-script **1.9.0**（`~/.m2/.../magic-script-1.9.0-sources.jar` 解包至 `/tmp/ms-src`）+ 官方文档站 https://www.ssssssss.org/magic-api/
>
> **以源码为准、文档为辅**。凡文档与源码冲突处，正文用 `⚠️` 标注并以源码为准。
> 源码行号均相对 `/tmp/ma-probe/magic-api/src/main/java/org/ssssssss/magicapi/`，为便于阅读省略该前缀，写作 `modules/...:行号`。

---

## 0. 关键结论速览（给插件作者）

| 结论 | 依据 |
| --- | --- |
| 模块用 `@MagicModule("name")` 注册，注解全名是 `org.ssssssss.magicapi.core.annotation.MagicModule`（**不是** `@MagicAnnotation`） | `core/annotation/MagicModule.java:1-20` |
| 只有 8 个核心模块：`db`、`http`、`log`、`request`、`response`、`env`、`magic`，另有 `db` 之外的动态数据源写法。插件模块 `redis`/`mongo`/`es`/`nebula` 在 `magic-api-plugins/` 下 | `grep -rn "@MagicModule"` 全仓结果 |
| **不存在** `assert`、`import`、`java`、`i18n`、`class` 这些"模块" | 全仓无 `@MagicModule("assert"/"i18n"/...")`；`java.type` 无任何实现；`import`/`assert` 是**语法关键字**而非模块 |
| `assert` 是**语言关键字**（`assert expr : expr,...`），编译期生成 `Exit`，不是函数也不是模块 | `/tmp/ms-src/org/ssssssss/script/parsing/ast/statement/Assert.java:15-40`；`Parser.java:265 parseAssert()` |
| `log` 模块是**动态注册**的，类型是 `org.slf4j.Logger`，非 `@MagicModule` 类 | `magic-api-spring-boot-starter/.../MagicAPIAutoConfiguration.java:267-268` |
| `db` 是**默认自动导入**的模块（`autoImportModule` 默认值 = `db`），脚本里可直接用 `db.xxx` 而无需 `import db` | `core/config/MagicAPIProperties.java:46`，`MagicAPIAutoConfiguration.java:278-282` |
| `db` 同时是 `DynamicModule`，会按当前接口的"默认数据源"选项动态切换数据源实例 | `modules/db/SQLModule.java:735-741` 与 `getDynamicModule()` |

---

## 1. 内置模块总表

### 1.1 模块注册总表

| 静态名（脚本内写法） | 对应类 | 是否自动导入 | 获取方式 |
| --- | --- | --- | --- |
| `db` | `org.ssssssss.magicapi.modules.db.SQLModule` | ✅ 默认自动导入（`autoImportModule=db`） | `@MagicModule("db")`，`SQLModule.java:40-41` |
| `http` | `org.ssssssss.magicapi.modules.http.HttpModule` | ❌ 需 `import http;` | `@MagicModule("http")`，`HttpModule.java:22-23` |
| `log` | `org.slf4j.Logger`（动态） | ❌ 需 `import log;` | `MagicAPIAutoConfiguration.java:267-268` 的 `DynamicModuleImport` |
| `request` | `org.ssssssss.magicapi.modules.servlet.RequestModule` | ❌ 需 `import request;` | `@MagicModule("request")`，`RequestModule.java:22-23` |
| `response` | `org.ssssssss.magicapi.modules.servlet.ResponseModule` | ❌ 需 `import response;` | `@MagicModule("response")`，`ResponseModule.java:25-26` |
| `env` | `org.ssssssss.magicapi.modules.spring.EnvModule` | ❌ 需 `import env;` | `@MagicModule("env")`，`EnvModule.java:12-13` |
| `magic` | `org.ssssssss.magicapi.core.service.impl.DefaultMagicAPIService` | ❌ 需 `import magic;` | `@MagicModule("magic")`，`DefaultMagicAPIService.java:41-42` |
| `redis`（插件） | `org.ssssssss.magicapi.redis.RedisModule` | ❌ | `magic-api-plugins/magic-api-plugin-redis/.../RedisModule.java:23-24` |
| `mongo`（插件） | `org.ssssssss.magicapi.mongo.MongoModule` | ❌ | `.../plugin-mongo/.../MongoModule.java:29-30` |
| `elasticsearch`（插件） | `...elasticsearch.ElasticSearchModule` | ❌ | `.../plugin-elasticsearch/.../ElasticSearchModule.java:7-8` |
| `nebula`（插件） | `...nebula.NebulaModule` | ❌ | `.../plugin-nebula/.../NebulaModule.java:23-24` |

> 自动导入的可配置项为 `magic-api.auto-import-module`（逗号分隔），源码默认值 `"db"`：`core/config/MagicAPIProperties.java:46`。
> 自动导入生效逻辑：`MagicAPIAutoConfiguration.java:278-282`，对 `getModuleNames()` 过滤 `importModules` 后 `MagicScriptEngine.addDefaultImport(...)`。

补充：`request`、`response`、`env`、`http` 的 Bean 均在 `MagicModuleConfiguration` 中声明（`@Bean` + `@ConditionalOnMissingBean`），因此可被用户自定义实现替换。
来源：`magic-api/src/main/java/org/ssssssss/magicapi/core/configuration/MagicModuleConfiguration.java:129-198`（`redis`/`mongo` 等插件模块为独立 jar）。

---

### 1.2 `db` 模块（SQLModule）

类：`org.ssssssss.magicapi.modules.db.SQLModule`
来源：`modules/db/SQLModule.java`；文档 https://www.ssssssss.org/magic-api/pages/module/db/

方法签名省略了统一的第一个隐式参数 `RuntimeContext runtimeContext`（由脚本引擎自动注入，**补全时不要展示**）。

| 方法 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- |
| `select` | `sqlOrXml: String` | `List<Map<String,Object>>` | 查询列表结果 | :317 |
| `select` | `sqlOrXml: String, params: Map` | `List<Map<String,Object>>` | 带变量查询列表 | :326 |
| `selectInt` | `sqlOrXml: String[, params: Map]` | `Integer` | 查询 int（须一行一列） | :650, :659 |
| `selectOne` | `sqlOrXml: String[, params: Map]` | `Map<String,Object>` | 查询单个对象 | :675, :684 |
| `selectValue` | `sqlOrXml: String[, params: Map]` | `Object` | 查询单个值（须一行一列） | :706, :715 |
| `page` | `sqlOrXml: String[, params: Map]` | `Object` | 分页查询，分页条件**自动从请求参数获取** | :514, :524 |
| `page` | `sqlOrXml: String, limit: long, offset: long[, params: Map]` | `Object` | 分页，分页条件**手动传入** | :533, :544 |
| `page` | `countSqlOrXml: String, sqlOrXml: String[, params: Map]` | `Object` | 手动指定 count 语句 | :568, :578 |
| `page` | `count: int, sqlOrXml: String, limit: long, offset: long, params: Map` | `Object` | 手动指定总数 | :592 |
| `update` | `sqlOrXml: String[, params: Map]` | `int` | 执行增删改，返回受影响行数 | :356, :365 |
| `insert` | `sqlOrXml: String[, params: Map]` | `Object` | 插入，返回主键 | :385, :394 |
| `insert` | `sqlOrXml: String, primary: String[, params: Map]` | `Object` | 指定主键列插入 | :404, :414 |
| `batchUpdate` | `sql: String, args: List<Object[]>` | `int` | 批量执行，占位符与数组下标对应 | :435 |
| `batchUpdate` | `sql: String, batchSize: int, args: List<Object[]>` | `int` | 带批大小的批量执行 | :464 |
| `batchUpdate` | `sqls: List<String>` | `int` | 批量执行多条 SQL | :493 |
| `call` | `sqlOrXml: String` | `Object` | 调用存储过程（`#{in}` / `@{out, TYPE}` 语法） | :780 |
| `transaction` | `callback: Function` | `Object` | 自动事务，回调内异常自动回滚 | :215 |
| `transaction` | — | `Transaction` | 手动事务，返回对象含 `commit()` / `rollback()` | :232 |
| `cache` | `cacheName: String[, ttl: long]` | `SQLModule` | 使用缓存，**返回自身可链式** | :243, :260 |
| `deleteCache` | `cacheName: String` | `SQLModule` | 删除命名缓存 | :452 |
| `camel` / `pascal` / `lower` / `upper` / `normal` | — | `SQLModule` | 列名转换策略，链式 | :265, :270, :275, :280, :285 |
| `columnCase` | `name: String` | `SQLModule` | 指定列名转换器 | :290 |
| `table` | `tableName: String` | `NamedTable` | 进入单表操作 API | :724 |
| `getDataSourceName` | — | `String` | 当前数据源名 | :560 |

> `Transaction` 类只有 `commit()` / `rollback()` 两个方法：`modules/db/Transaction.java:29,37`。

#### 1.2.1 `db.table(...)` 返回的 `NamedTable`（单表 API，风格抄 MyBatis-Plus）

来源：`modules/db/table/NamedTable.java`、`modules/db/table/Where.java`；文档 db 页「单表操作API」小节。

| 方法 | 参数 | 返回 | 说明 | 源码行 |
| --- | --- | --- | --- | --- |
| `logic` | — | `NamedTable` | 启用逻辑删除（delete→update，select 拼 `logic_field <> logic_value`） | :112 |
| `withBlank` | — | `NamedTable` | 插入/修改时不过滤空值 | :118 |
| `primary` | `primary: String[, defaultValue: Object]` | `NamedTable` | 设置主键列 | :124, :130 |
| `column` | `property: String[, value: Object]` | `NamedTable` | 设置查询列 / 设置列值 | :151, :200 |
| `columns` | `String...` / `Collection<String>` | `NamedTable` | 批量设置列 | :158, :192 |
| `exclude` / `excludes` | `String` / `String...` / `List` | `NamedTable` | 排除列 | :168, :176, :184 |
| `orderBy` / `orderByDesc` | `property: String[, order: String]` | `NamedTable` | 排序 | :208, :215, :220 |
| `groupBy` | `String...` | `NamedTable` | 分组 | :225 |
| `where` | — | `Where` | 进入条件构造 | :146 |
| `insert` | `[data: Map]` | `Object`（主键） | 单条插入 | :231, :236 |
| `batchInsert` | `collection: Collection<Map>[, batchSize: int]` | `int` | 批量插入 | :338, :358 |
| `update` | `[data: Map][, isUpdateBlank: boolean]` | `int` | 更新 | :389, :394 |
| `save` | `[data: Map][, beforeQuery: boolean]` | `Object` | 有主键则 update，无则 insert | :287, :292, :326 |
| `delete` | — | `int` | 删除（配合 `logic()` 转更新） | :269 |
| `select` | — | `List<Map<String,Object>>` | 查询列表 | :363 |
| `page` | — | `Object` | 分页 | :375, :381 |
| `count` | — | `int` | 计数 | :442 |

`Where` 条件方法（全部返回 `Where`，可链式；均支持 `(column, value)` 与 `(condition: boolean, column, value)` 两种重载）：

| 方法 | 说明 | 源码行 |
| --- | --- | --- |
| `eq` | `=` / `==` | :74, :80 |
| `ne` | `<>` | :97, :103 |
| `lt` / `lte` | `<` / `<=` | :120, :133 |
| `gt` / `gte` | `>` / `>=` | :146, :159 |
| `in` / `notIn` | `in` / `not in` | :172, :196 |
| `like` / `notLike` | `like` / `not like` | :220, :233 |
| `isNull` / `isNotNull` | `is null` / `is not null` | :246, :262 |
| `notNull` / `notBlank` | 跳过 null / 空白条件 | :52, :57 |

> ⚠️ 文档 db 页只列了 `eq/ne/lt/gt/lte/gte/in/notIn/like/notLike`，源码另有 `isNull`/`isNotNull`/`notNull`/`notBlank`，**以源码为准**补充。
> 文档页末尾原话：「其它的自己摸索吧，API抄的mybatis-plus，文档写不动了」。

---

### 1.3 `request` 模块（RequestModule）

类：`org.ssssssss.magicapi.modules.servlet.RequestModule`
来源：`modules/servlet/RequestModule.java`；文档 https://www.ssssssss.org/magic-api/pages/module/request/

| 方法 | 静态 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- | --- |
| `getFile` | ✅ static | `name: String` | `MultipartFile` | 获取上传的单个文件（空文件返回 null） | :38 |
| `getFiles` | ✅ static | `name: String` | `List<MultipartFile>` | 获取上传文件集合 | :53 |
| `get` | ✅ static | — | `MagicHttpServletRequest` | 获取原生 Request 对象 | :65 |
| `getValues` | ❌ 实例 | `name: String` | `List<String>` | 获取数组型请求参数 | :83 |
| `getHeaders` | ❌ 实例 | `name: String` | `List<String>` | 获取同名 header 数组 | :98 |
| `getClientIP` | ❌ 实例 | `otherHeaderNames: String...` | `String` | 获取客户端真实 IP | :108 |

> ⚠️ 补全实现注意：`getFile`/`getFiles`/`get` 是 `static`，其余是实例方法。脚本里两者写法一致（`request.getFile(...)`），但反射元数据里 `Modifier.isStatic` 不同。

---

### 1.4 `response` 模块（ResponseModule）

类：`org.ssssssss.magicapi.modules.servlet.ResponseModule`
来源：`modules/servlet/ResponseModule.java`；文档 https://www.ssssssss.org/magic-api/pages/module/response/

| 方法 | 静态 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- | --- |
| `page` | ❌ | `total: long, values: List<Map<String,Object>>` | `Object` | 构造分页结果 | :55 |
| `json` | ❌ | `value: Object` | `ResponseEntity<Object>` | 直接返回 JSON，不再被包装 | :66 |
| `text` | ❌ | `text: String` | `ResponseEntity` | 输出纯文本 | :139 |
| `download` | ✅ static | `value: Object, filename: String` | `ResponseEntity<?>` | 文件下载 | :41 |
| `image` | ❌ | `value: Object, mime: String` | `ResponseEntity` | 输出图片（`image/png` 等） | :128 |
| `redirect` | ❌ | `url: String` | `NullValue` | 重定向（内部 `sendRedirect`） | :149 |
| `addHeader` | ❌ | `key: String, value: String` | `ResponseModule` | 追加响应头 | :74 |
| `setHeader` | ❌ | `key: String, value: String` | `ResponseModule` | 设置响应头 | :89 |
| `getOutputStream` | ❌ | — | `OutputStream` | 取输出流（之后应 `response.end()`） | :106 |
| `end` | ❌ | — | `NullValue` | 终止输出，框架不再包装返回值 | :113 |

> ⚠️ **冲突（重要）**：官方文档 response 页列出了 `addCookie` / `addCookies` 两个方法及其 `options`（`path/httpOnly/domain/maxAge`）参数，但**源码 2.2.2 中不存在**。
> 验证方式：`grep -i cookie ResponseModule.java` 无命中；对已发布 jar `magic-api-2.2.2.jar` 的 `ResponseModule.class` 做 `strings` 检查，方法名字符串仅含 `addHeader/setHeader/getOutputStream/download/page/json/image/text`，无 `addCookie`。
> 结论：`addCookie`/`addCookies` 是**文档残留/已移除**，插件补全**不应**提供这两个方法（若用户环境是旧版 2.x 则可能仍存在，可做成版本条件补全）。

---

### 1.5 `log` 模块（SLF4J Logger）

类型：`org.slf4j.Logger`（由 `DynamicModuleImport` 动态提供，logger 名称 = 当前脚本名）
来源：`magic-api-spring-boot-starter/src/main/java/org/ssssssss/magicapi/spring/boot/starter/MagicAPIAutoConfiguration.java:267-268`；文档 https://www.ssssssss.org/magic-api/pages/module/log/

```java
MagicResourceLoader.addModule("log",
    new DynamicModuleImport(Logger.class,
        context -> LoggerFactory.getLogger(Objects.toString(context.getScriptName(), "Unknown"))));
```

因此 `log.<method>` 的补全集 == `org.slf4j.Logger` 的公开方法。常用：

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `trace/debug/info/warn/error` | `msg: String` | `void` | 打印日志 |
| `trace/debug/info/warn/error` | `msg: String, arg: Object` | `void` | 单参数，`{}` 占位 |
| `trace/debug/info/warn/error` | `msg: String, args: Object...` | `void` | 多参数 |
| `trace/debug/info/warn/error` | `msg: String, t: Throwable` | `void` | 带异常 |
| `isTraceEnabled/isDebugEnabled/isInfoEnabled/isWarnEnabled/isErrorEnabled` | — | `boolean` | 级别判断 |
| `getName` | — | `String` | logger 名（= 脚本路径） |

> 说明：`log` 没有 `@MagicModule` 注解，是从 `DynamicModuleImport` 注册的，源码中不存在 `LogModule` 类。插件若按注解扫描会漏掉它，需硬编码。

---

### 1.6 `env` 模块（EnvModule）

类：`org.ssssssss.magicapi.modules.spring.EnvModule`
来源：`modules/spring/EnvModule.java`；文档 https://www.ssssssss.org/magic-api/pages/module/env/

| 方法 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- |
| `get` | `key: String` | `String` | 取 Spring 配置项 | :18 |
| `get` | `key: String, defaultValue: String` | `String` | 取配置项，缺省用默认值 | :27 |

---

### 1.7 `magic` 模块（DefaultMagicAPIService）

类：`org.ssssssss.magicapi.core.service.impl.DefaultMagicAPIService`（实现 `MagicAPIService`）
来源：`core/service/impl/DefaultMagicAPIService.java`；文档 https://www.ssssssss.org/magic-api/pages/module/magic/

| 方法 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- |
| `call` | `method: String, path: String, parameters: Map` | `Object` | 调用另一接口，返回带 code/message 的包装结果 | :110 |
| `execute` | `method: String, path: String, parameters: Map` | `Object` | 调用另一接口，返回原始内容（不包装） | :91 |
| `invoke` | `path: String, parameters: Map` | `Object` | 调用「函数」资源 | :126 |

> ⚠️ 文档 magic 页把三个方法都写成入参含 `method`，实际 `invoke` 只有 `(path, parameters)`。
> 该类还有 `upload`、`download`、`push`、`processNotify` 等 public 方法，但属于内部 API，**不建议**进入补全列表（文档也未列）。

---

### 1.8 `http` 模块（HttpModule）

类：`org.ssssssss.magicapi.modules.http.HttpModule`
来源：`modules/http/HttpModule.java`；文档 https://www.ssssssss.org/magic-api/pages/module/http/
设计：链式 builder，除动词方法外均返回 `HttpModule` 自身。

| 方法 | 参数 | 返回值 | 说明 | 源码行 |
| --- | --- | --- | --- | --- |
| `connect` | `url: String` | `HttpModule` | 创建请求对象 | :46 |
| `param` | `key: String, values: Object...` / `values: Map` | `HttpModule` | URL 查询参数 | :51, :62 |
| `data` | `key: String, values: Object...` / `values: Map` | `HttpModule` | 表单参数 | :68, :79 |
| `header` | `key: String, value: String` / `values: Map` | `HttpModule` | 请求头 | :85, :92 |
| `method` | `method: HttpMethod` | `HttpModule` | 指定方法（默认 GET） | :101 |
| `body` | `requestBody: Object` | `HttpModule` | 请求体 | :107 |
| `entity` | `entity: HttpEntity<Object>` | `HttpModule` | 自定义 HttpEntity | :114 |
| `contentType` | `String` / `MediaType` | `HttpModule` | Content-Type | :120, :125 |
| `expectBytes` / `expectString` | — | `HttpModule` | 指定返回 `byte[]` / `String` | :131, :137 |
| `get/post/put/delete/head/options/trace/patch` | — | `ResponseEntity<?>` | 发请求，**终结链** | :143-185 |
| `execute` | — | `ResponseEntity<?>` | 按已设 method 执行 | :191 |

---

### 1.9 插件模块（按需安装）

| 模块 | 类 | 说明 |
| --- | --- | --- |
| `redis` | `org.ssssssss.magicapi.redis.RedisModule`（`implements DynamicMethod`） | 方法名由 `DynamicMethod` 动态提供，静态扫描拿不到 |
| `mongo` | `org.ssssssss.magicapi.mongo.MongoModule` | `DynamicAttribute`，`mongo.xxx` 先取库再取集合 |
| `elasticsearch` | `...elasticsearch.ElasticSearchModule` | — |
| `nebula` | `...nebula.NebulaModule` | — |

> 建议插件把这些做成"可选模块包"，默认关闭，避免在未安装插件的环境里产生假补全。

---

## 2. 脚本中可直接使用的隐式变量

来源：`core/config/Constants.java:36-63`（变量名常量）+ `core/web/RequestHandler.java:132-150`（注入点）+ `core/interceptor/DefaultResultProvider.java:31-32`（响应脚本变量）。

| 变量名 | 类型 | 注入位置 | 说明 |
| --- | --- | --- | --- |
| `path` | `Map<String, Object>` | `RequestHandler.java:147` | `URL` 上的路径变量，如 `/user/{id}` → `path.id` |
| `header` | `Map<String, Object>`（`LinkedCaseInsensitiveMap`，**大小写不敏感**） | `RequestHandler.java:143` | 全部请求头，`header.token` |
| `cookie` | `CookieContext extends HashMap<String,String>`（**大小写不敏感**） | `RequestHandler.java:139` | `cookie.JSESSIONID` |
| `session` | `SessionContext extends HashMap<String,Object>` | `RequestHandler.java:145` | 读写 session 属性；`get` 读属性、`put` 写属性 |
| `body` | `Object`（Map 或 List，取决于请求体） | `RequestHandler.java:150` | 整个请求体；`body.name`；数组体可直接遍历 |
| `query` | 常量已定义但**未注入** | `Constants.java:57`（仅常量） | ⚠️ 源码中 `VAR_NAME_QUERY` 除定义外无任何引用，脚本里拿不到 `query` 变量。文档也未提。补全**不要**提供 |
| `response` | `MagicHttpServletResponse` | `DefaultResultProvider.java:32` | **只在「响应脚本 / response script」中可用**，普通接口脚本里无此变量 |
| `request` | `MagicHttpServletRequest` | `DefaultResultProvider.java:31` | 同上，仅在响应脚本中 |

另外：

- **所有 RequestParam / 表单参数会以「同名变量」直接注入上下文**：`RequestHandler.java:134 context.putMapIntoContext(requestEntity.getParameters())`。
  即 `?name=abc&age=49` 可直接写 `name`、`age`。
- **路径变量同时以同名变量注入**：`RequestHandler.java:136 context.putMapIntoContext(requestEntity.getPathVariables())`。
  所以 `/user/{id}` 既可用 `path.id` 也可直接用 `id`。
  文档的注意点：若同时有 `?id=2`，`id` 取 RequestParam 值，需用 `path.id` 消歧（https://www.ssssssss.org/magic-api/pages/quick/param/）。
- 若脚本自定义变量与参数变量同名，**自定义变量优先**（文档 https://www.ssssssss.org/magic-api/pages/quick/param/ 「注意事项」）。
- 可在接口上配置「包装请求参数」选项，把全部参数塞进指定名字的变量：`RequestHandler.java:126-128`（`Options.WRAP_REQUEST_PARAMETERS`）。
- 可配置「默认数据源」：`RequestHandler.java:130-132`，会以 `Options.DEFAULT_DATA_SOURCE` 的值注入上下文，并被 `SQLModule.getDynamicModule()` 消费（`SQLModule.java:735-741`）。

### 2.1 `CookieContext` / `SessionContext` 的方法面

| 类型 | 可用方法 | 说明 | 源码 |
| --- | --- | --- | --- |
| `CookieContext` | `get(String)`、`size()`、`isEmpty()`、`containsKey()`…（继承 `HashMap<String,String>`） | `get` 被重写为大小写不敏感的 cookie 查找，**不读 map 本身**；`put` 不会写回响应 Cookie | `core/context/CookieContext.java` |
| `SessionContext` | `get(String)`、`put(String, Object)`（继承 `HashMap<String,Object>`） | `get`→`session.getAttribute`，`put`→`session.setAttribute`，是真正的读写代理 | `core/context/SessionContext.java` |

> ⚠️ 补全提示：`cookie` 上**不要**提示 `put/remove/clear` 之类写操作（无效）；`session` 上 `put` 是有效的。

### 2.2 响应脚本专用变量（`responseScript`）

当配置了响应脚本时，`DefaultResultProvider.buildResult(...)` 会新建一个上下文并注入：

| 变量 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `int` | 响应码 |
| `message` | `String` | 响应消息 |
| `data` | `Object` | 业务数据 |
| `apiInfo` | `ApiInfo` | 当前接口定义 |
| `request` | `MagicHttpServletRequest` | 原生请求 |
| `response` | `MagicHttpServletResponse` | 原生响应 |
| `timestamp` | `long` | 当前毫秒时间戳 |
| `requestTime` | `long` | 请求进入时间 |
| `executeTime` | `long` | 执行耗时（ms） |

来源：`core/interceptor/DefaultResultProvider.java:26-37`。

---

## 3. 常用全局函数 / 工具

### 3.1 `@Function` 全局函数（magic-script 内置，脚本里直接 `f(...)` 调用，无需 import）

标注：`org.ssssssss.script.annotation.Function`，通过 `JavaReflection.getFunctions()` 收集（`MagicScriptEngine.java:50-52`）。
来源：`/tmp/ms-src/org/ssssssss/script/functions/`

| 分类 | 函数 | 签名 | 说明 | 文件 |
| --- | --- | --- | --- | --- |
| 字符串 | `is_blank` | `(str: CharSequence) → boolean` | 是否空白 | `StringFunctions.java:16` |
| 字符串 | `not_blank` | `(str: CharSequence) → boolean` | 是否非空白 | `StringFunctions.java:10` |
| 通用 | `uuid` | `() → String` | UUID | `MagicScriptFunctions.java:12` |
| 通用 | `is_null` | `(value) → boolean` | 是否 null | `MagicScriptFunctions.java:24` |
| 通用 | `not_null` | `(value) → boolean` | 是否非 null | `MagicScriptFunctions.java:18` |
| 输出 | `print` / `println` | `(value) → void` | 打印 | `MagicScriptFunctions.java:30,36` |
| 输出 | `printf` | `(format: String, args: Object...) → void` | 格式化打印 | `MagicScriptFunctions.java:42` |
| 集合 | `range` | `(from: int, to: int) → Iterator<Integer>` | 闭区间整数序列，用于 for | `CollectionFunctions.java:15` |
| 聚合 | `count` | `(value) → int` | 计数（Linq） | `linq/AggregationFunctions.java:20` |
| 聚合 | `sum` / `avg` / `max` / `min` | `(value) → Number/Object` | 求和/均值/最大/最小 | `linq/AggregationFunctions.java:52,88,35,71` |
| 聚合 | `group_concat` | `(target, [separator: String]) → String` | 分组拼接，默认 `,` | `linq/AggregationFunctions.java:107,117` |
| 日期 | `now` | `() → Date` | 当前时间 | `linq/LinqFunctions.java:46` |
| 日期 | `current_timestamp` | `() → long` | 当前秒级时间戳 | `linq/LinqFunctions.java:52` |
| 日期 | `current_timestamp_millis` | `() → long` | 当前毫秒时间戳 | `linq/LinqFunctions.java:58` |
| 日期 | `date_format` | `(target: Date/TemporalAccessor, [pattern]) → String` | 格式化，默认 `yyyy-MM-dd HH:mm:ss` | `linq/LinqFunctions.java:25,32,39` |
| 空值 | `ifnull` | `(target, trueValue) → Object` | target 为 null 时取 trueValue | `linq/LinqFunctions.java:18` |
| 数学 | `round` | `(target: Number, [scale: int]) → double` | 四舍五入 | `linq/MathFunctions.java:11,18` |
| 数学 | `ceil` / `floor` | `(target: Number) → Number` | 上/下取整 | `linq/MathFunctions.java:24,30` |
| 数学 | `percent` | `(target: Number, [scale: int]) → String` | 百分比 | `linq/MathFunctions.java:36,43` |
| 数组 | `new_array` | 多重载：`(size: int)` / `(componentType: Class, size: int)` / `(values: T...)` | 创建数组 | `ArrayFunctions.java:60,66,73-121` |
| 数组 | `new_int_array` / `new_short_array` / `new_double_array` / `new_float_array` / `new_byte_array` / `new_char_array` / `new_boolean_array` / `new_long_array` | `(size: int) → T[]` | 创建基本类型数组 | `ArrayFunctions.java:12-54` |

> 文档对应页：聚合函数 https://www.ssssssss.org/magic-api/pages/function/aggregation/ ；其余函数页在同一 `/pages/function/` 目录。

### 3.2 扩展方法（挂在具体类型上，`obj.xxx()` 形式的补全）

来源：`/tmp/ms-src/org/ssssssss/script/functions/*Extension.java`

| 接收者类型 | 扩展类 | 代表方法 |
| --- | --- | --- |
| `List` / `Collection` / 数组 | `StreamExtension` | `map`、`filter`、`find`、`findIndex`、`each`、`sort`、`reserve`、`shuffle`、`distinct`、`join`、`group`、`skip`、`limit`、`concat`、`toMap`、`push`、`first`、`max`、`min`、`sum`、`avg` |
| `String` | `StringExtension` | `is_blank`、`not_blank` 等 |
| `Map` | `MapExtension` | map 相关便捷方法 |
| `Number` | `NumberExtension` | 数值扩展 |
| `Date` | `DateExtension` | `format(target, pattern)` |
| `TemporalAccessor` | `TemporalAccessorExtension` | `format(target, pattern)` |
| `Pattern` | `PatternExtension` | 正则相关 |
| `Class` | `ClassExtension` | 类反射相关 |

> `StreamExtension.java:143-486` 是补全 `list.` 之后候选的主要来源（`map/filter/find/each/sort/distinct/join/group/skip/limit/...`）。

### 3.3 语言级"工具"（不是模块，别当成 import 目标）

来源：https://www.ssssssss.org/magic-api/pages/base/script/

| 形式 | 语义 | 备注 |
| --- | --- | --- |
| `import 'java.lang.System' as System;` | 导入 Java 类 | 支持 `import 'java.text.*'`（1.3.4+） |
| `import 'javax.sql.DataSource' as ds;` | 从 Spring 容器取 Bean | 按类型注入 |
| `import log;` / `import log as logger;` | 导入已注册模块 | 模块名即 `@MagicModule` 的值 |
| `import 'xxx'` 与 `import xxx` | 带引号=Java 类/包；不带引号=模块名 | 补全时按两者分流 |
| `assert expr : code, message, data` | 断言，失败等价 `exit` | `Assert.java:15-40`；**关键字**，出现在语句开头 |
| `exit code[, message[, data]]` | 立即退出并返回 | 文档 base/script |
| `async expr` | 异步执行，返回 `Future` | `var v = async db.select(...)` |
| `::int` / `::double` / `::date('yyyy-MM-dd')` / `::sql('blob')` | 类型转换 | 支持默认值 `::int(0)` |
| `` ```javascript ... ``` `` | 内嵌其它脚本语言 | 3 个反引号包裹，结果是一个可调用对象 |
| `new Foo(...)` | 创建对象（须先 import 该类） | 不支持 `.*` 形式 new |

> ⚠️ **不存在 `java.type(...)`**。全仓（magic-api 2.2.2 + magic-script 1.9.0）搜索 `java.type` / `"java"` 模块注册均无命中。若其它资料提到，属于误传。

### 3.4 用户自定义函数（「函数」资源）

magic-api 提供独立的「函数」资源类型（`FunctionInfo` + `FunctionMagicDynamicRegistry`），可被 `magic.invoke(path, params)` 调用。
来源：`magic-api/src/main/java/org/ssssssss/magicapi/function/service/FunctionMagicDynamicRegistry.java:22-71`、`function/model/FunctionInfo.java`。

> 补全价值：`magic.invoke('` 后应提示工程内的函数列表（需插件侧读取 `magic-api` 的函数资源目录，如 `function/` 目录下的脚本文件）。

---

## 4. 补全决策树（给 IDEA 插件）

### 4.1 输入 `import ` 之后（模块名 vs Java 类）

```
用户输入 "import <光标>"
├─ 光标紧跟在引号内：import '<光标>' 或 import '...<光标>'
│   └─ 提示 Java 类名 / 包名（含 ".*" 通配形式）
│       候选来源：项目 classpath + 常见包（java.lang.*, java.util.*, java.text.*, org.springframework.*）
│       ⚠️ 不要提示模块名
│
└─ 光标不在引号内：import <光标>
    └─ 提示「已注册模块名」
        必选（核心 7 个，不含 db 也可提示，因 db 自动导入但仍可显式 import）：
          db      → SQLModule
          http    → HttpModule
          log     → org.slf4j.Logger
          request → RequestModule
          response→ ResponseModule
          env     → EnvModule
          magic   → DefaultMagicAPIService
        可选（检测到插件 jar 才提示）：
          redis / mongo / elasticsearch / nebula
        ❌ 不要提示：assert / import / java / i18n / class（不存在这些模块）
```

**依据**：模块名到类的映射来自 `@MagicModule` 与 `DynamicModuleImport` 注册（`MagicAPIAutoConfiguration.java:267-276`）；`import` 的两种语义见 https://www.ssssssss.org/magic-api/pages/base/script/ 「Import导入」。

### 4.2 输入 `import <mod> as ` 之后

```
import db as <光标>   → 不提示（as 后是用户自定义变量名）
```
仅给一个空模板，或在文档里说明"变量名自取"。

### 4.3 输入 `<module>.` 之后（成员补全）

```
<identifier>.<光标>
├─ identifier 是模块名或模块别名
│   ├─ db        → 1.2 表：select/selectInt/selectOne/selectValue/page/update/
│   │              insert/call/batchUpdate/transaction/cache/deleteCache/
│   │              camel/pascal/lower/upper/normal/columnCase/table
│   ├─ request   → getFile/getFiles/get/getValues/getHeaders/getClientIP
│   ├─ response  → page/json/text/download/image/redirect/addHeader/setHeader/
│   │              getOutputStream/end          ❌ 无 addCookie/addCookies
│   ├─ env       → get（2 个重载）
│   ├─ magic     → call/execute/invoke
│   ├─ http      → connect/param/data/header/method/body/entity/contentType/
│   │              expectBytes/expectString/get/post/put/delete/head/options/trace/patch/execute
│   └─ log       → SLF4J Logger 全量方法（trace/debug/info/warn/error/isXxxEnabled/getName）
│
└─ identifier 不是模块名 → 走普通类型成员补全（见 4.6）
```

**参数提示**：每个方法需展示"去隐式参数"后的签名。`db`/`db.table()`/`db.where()` 的第一个 `RuntimeContext` 参数由引擎注入，**必须从补全签名里剔除**（源码 `SQLModule.java:317` 等均有该首参）。

### 4.4 输入 `db.` 的场景化提示（重点）

| 触发 | 建议排序 / 提示 |
| --- | --- |
| `db.page(` | 首选「自动分页」重载 `page(sqlOrXml)` / `page(sqlOrXml, params)`；次选手动 `page(sql, limit, offset)`；再次 `page(countSql, sql)` |
| `db.page('...')` 后跟 `)` | 提示：分页参数默认从请求参数 `pageNo`/`pageSize` 自动读取（见下 4.5 配置项） |
| `db.select(` | `select(sql)` → `List<Map<String,Object>>`；若非 SQL 而是 XML 片段，提示 `sqlOrXml` 可传 `<select>...` |
| `db.selectInt(` / `db.selectValue(` | 文档气泡强调「必须保证结果返回一行一列」 |
| `db.insert(` | 提示第二参数 `primary`（主键列，可省略） |
| `db.transaction(` | 无参 → 返回 `Transaction`，链式提示 `.commit()` / `.rollback()`；有参 → 回调 `()=>{...}` |
| `db.cache(` | 返回 `SQLModule`，继续提示链式方法；第二参 `ttl` 单位**毫秒** |
| `db.call(` | 文档气泡给出 `#{in}` / `@{out, java.sql.Types}` / `@{param(value), TYPE}` 三种占位符语法 |
| `db.table('t').` | 切到 `NamedTable` 成员表（1.2.1） |
| `db.table('t').where().` | 切到 `Where` 成员表（`eq/ne/lt/lte/gt/gte/in/notIn/like/notLike/isNull/isNotNull/notNull/notBlank`） |
| `db.table('t').insert({` | 若插件能读表结构，提示列名 |
| `db.` 后输入 `select` 的返回 `.map(` | 转到 `StreamExtension` 的 `List` 扩展方法 |

### 4.5 需要读取的 magic-api 配置项（影响补全行为）

| 配置 | 默认值 | 对补全的影响 | 源码 |
| --- | --- | --- | --- |
| `magic-api.auto-import-module` | `db` | 决定哪些模块「无需 import 即可直接用」，据此决定 `<module>.` 在未 import 时是否也补全 | `core/config/MagicAPIProperties.java:46` |
| `magic-api.auto-import-package` | 空 | 额外自动导包，影响 `import '<类名>'` 的候选 | `MagicAPIAutoConfiguration.java:283-285` |
| 接口级「默认数据源」 | — | `db` 会在运行时切换数据源实例，但**类型不变**，补全无需特殊处理 | `SQLModule.java:735-741` |
| 接口级「包装请求参数」 | — | 会多出一个用户命名的 Map 变量，脚本补全无法预知名字，不做特殊处理 | `RequestHandler.java:126-128` |

### 4.6 隐式变量与普通表达式补全

```
光标处是表达式起始（非 "import " 之后、非 "." 之后）
├─ 优先提示 1) 隐式变量 2) 脚本局部变量 3) 全局函数
│   隐式变量：path, header, cookie, session, body
│             （+ request/response 仅当当前文件是「响应脚本」）
│             ❌ 不要提示 query
│   路由参数：当前接口 URL 模板里的 {xxx} 变量名（如 {id} → id）
│   请求参数：接口配置里定义的参数名（等同 RequestParam，可直接当变量用）
│   全局函数：3.1 表（uuid/is_blank/range/sum/ifnull/date_format/now/...）
│   模块名：db（自动导入）、以及文件中已 import 的模块别名
│
└─ identifier 已知类型
    ├─ List/数组 → StreamExtension 方法（map/filter/find/join/group/...）
    ├─ String    → StringExtension
    ├─ Map       → MapExtension
    ├─ Date      → DateExtension（format 等）
    ├─ cookie    → 只读 get/size/isEmpty/containsKey；❌ 不提示 put/remove
    └─ session   → 额外提示 put(key, value)（会写回 HttpSession）
```

> 变量优先级依据：文档「如果脚本自定义变量和参数变量冲突，自定义变量优先」https://www.ssssssss.org/magic-api/pages/quick/param/ 。故排序应为：局部变量 > 请求/路径参数 > 隐式变量 > 全局函数。

### 4.7 语句关键字补全（避免误判为函数）

在语句开头 / 行首补全时，关键字集合应包含：

`var`、`let`、`const`、`if`、`else`、`for`、`in`、`while`、`break`、`continue`、`return`、`exit`、`assert`、`try`、`catch`、`finally`、`throw`、`import`、`as`、`new`、`async`、`instanceof`、`true`、`false`、`null`、`and`、`or`

来源：`/tmp/ms-src/org/ssssssss/script/parsing/Parser.java:60`（`KEYWORDS` 列表）+ https://www.ssssssss.org/magic-api/pages/base/script/ 「关键字」表。
注意 `Parser.java:60` 里 `assert` 在注释中被排除出 `KEYWORDS` 常量，但它由 `parseAssert()`（`Parser.java:265`）单独处理，仍是合法关键字 —— 补全应包含它。

---

## 5. 待确认 / 已知风险清单

| 项 | 现状 | 建议 |
| --- | --- | --- |
| 文档 `response.addCookie/addCookies` | 源码 2.2.2 无此方法（文档残留） | 不做补全；如需兼容旧版本，按 magic-api 版本号条件启用 |
| `query` 隐式变量 | `Constants.java:57` 定义但从未注入 | 不补全 |
| `i18n` 模块 | 2.2.2 中不存在（仅有 console 前端的 i18n 资源） | 不补全 |
| `redis`/`mongo`/`es`/`nebula` | 属于 `magic-api-plugins`，需单独引入 | 可选包，默认关闭 |
| `db` 的首个 `RuntimeContext` 参数 | 引擎注入，非用户可见 | 补全时剔除 |
| 插件/动态模块方法 | `DynamicMethod`/`DynamicAttribute` 使方法集运行时决定（如 `redis`） | 静态补全只能给已知子集，其余走"任意属性"兜底 |

---

## 6. 来源索引

**官方文档**

- 脚本语法（import/关键字/内嵌语言/Linq/exit/assert）：https://www.ssssssss.org/magic-api/pages/base/script/
- db 模块：https://www.ssssssss.org/magic-api/pages/module/db/
- request 模块：https://www.ssssssss.org/magic-api/pages/module/request/
- response 模块：https://www.ssssssss.org/magic-api/pages/module/response/
- log 模块：https://www.ssssssss.org/magic-api/pages/module/log/
- env 模块：https://www.ssssssss.org/magic-api/pages/module/env/
- magic 模块：https://www.ssssssss.org/magic-api/pages/module/magic/
- http 模块：https://www.ssssssss.org/magic-api/pages/module/http/
- 请求参数获取（path/param/header/cookie/session）：https://www.ssssssss.org/magic-api/pages/quick/param/
- 聚合函数：https://www.ssssssss.org/magic-api/pages/function/aggregation/

> 说明：`https://www.ssssssss.org/magic-api/pages/module/` 索引页返回 **404**；模块页面的完整清单从各模块页侧边栏抓取，实际只有 `db/env/http/log/magic/request/response` 七个。`pages/module/assert/`、`/i18n/`、`/import/`、`/java/` 均为 404。

**源码（magic-api 2.2.2，`/tmp/ma-probe`）**

- `magic-api/src/main/java/org/ssssssss/magicapi/core/annotation/MagicModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/db/SQLModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/db/table/NamedTable.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/db/table/Where.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/db/Transaction.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/servlet/RequestModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/servlet/ResponseModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/spring/EnvModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/modules/http/HttpModule.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/service/impl/DefaultMagicAPIService.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/config/Constants.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/config/MagicAPIProperties.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/web/RequestHandler.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/context/CookieContext.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/context/SessionContext.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/interceptor/DefaultResultProvider.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/core/configuration/MagicModuleConfiguration.java`
- `magic-api/src/main/java/org/ssssssss/magicapi/function/service/FunctionMagicDynamicRegistry.java`
- `magic-api-spring-boot-starter/src/main/java/org/ssssssss/magicapi/spring/boot/starter/MagicAPIAutoConfiguration.java`

**源码（magic-script 1.9.0，`/tmp/ms-src`）**

- `org/ssssssss/script/MagicScriptEngine.java`
- `org/ssssssss/script/MagicResourceLoader.java`
- `org/ssssssss/script/parsing/Parser.java`
- `org/ssssssss/script/parsing/ast/statement/Assert.java`
- `org/ssssssss/script/functions/MagicScriptFunctions.java`
- `org/ssssssss/script/functions/StringFunctions.java`
- `org/ssssssss/script/functions/CollectionFunctions.java`
- `org/ssssssss/script/functions/ArrayFunctions.java`
- `org/ssssssss/script/functions/StreamExtension.java`
- `org/ssssssss/script/functions/linq/LinqFunctions.java`
- `org/ssssssss/script/functions/linq/AggregationFunctions.java`
- `org/ssssssss/script/functions/linq/MathFunctions.java`
