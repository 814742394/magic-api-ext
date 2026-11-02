# Magic API — IntelliJ IDEA 插件

![Build](https://github.com/814742394/magic-api-ext/actions/workflows/build.yml/badge.svg)
[![JetBrains Marketplace](https://img.shields.io/badge/Marketplace-Magic%20API-blue?logo=jetbrains)](https://plugins.jetbrains.com/plugin/35622-magic-api)
[![License](https://img.shields.io/github/license/814742394/magic-api-ext)](LICENSE)

为 [magic-api](https://www.ssssssss.org/magic-api/) 的接口脚本（[magic-script](https://gitee.com/ssssssss-team/magic-script) 语言）提供 IDE 支持：
语法高亮、内置模块补全、快速文档、代码折叠、结构视图。

目标 IDE：**IntelliJ IDEA 2026.1+**（`sinceBuild=261`，自带 JRE 21+）。

## 安装

### 从插件市场安装（推荐）

**方式一（IDE 内）**：打开 <kbd>Settings</kbd> → <kbd>Plugins</kbd> → <kbd>Marketplace</kbd>，
搜索 **Magic API**，点击 <kbd>Install</kbd>，重启 IDE。

**方式二（网页直达）**：打开插件主页，点 <kbd>Get</kbd>，或在 IDE 中选择 <kbd>Install to…</kbd>：

> <https://plugins.jetbrains.com/plugin/35622-magic-api>

安装后打开任意 `.ms` 文件即可生效，无需额外配置。

### 离线安装

从 [Releases](https://github.com/814742394/magic-api-ext/releases) 下载
`magic-api-idea-<version>.zip`，然后 <kbd>Settings</kbd> → <kbd>Plugins</kbd> → ⚙️ →
<kbd>Install Plugin from Disk…</kbd> → 选择 zip → 重启。

## 功能

| 能力 | 说明 |
| --- | --- |
| 文件类型 | `.ms` 文件自动识别为 MagicScript |
| 语法高亮 | 关键字（`var`/`import`/`return`/`if`/`exit`…）、各类数字字面量（`1b/1s/1l/1f/1d/1m`）、`'...'`/`"..."`/`"""SQL"""`、正则 `/\d+/g`、模板串 `` `a${b}` ``、内嵌语言块 ` ```lang ... ``` `、注释 |
| import 形式 | `import db;`（模块）、`import 'java.util.Date' as Date;`（引号）、`import com.alibaba.fastjson.JSON;`（点号路径）、`import java.util.*;`（通配） |
| 语义高亮 | 内置模块（`db`/`request`/...）、隐式变量（`path`/`header`/`cookie`/`session`/`body`）、内置函数；`"""..."""` 里的 SQL 关键字 |
| 补全 | `import ` 后的模块名；`db.` 等模块成员；表达式起始处的隐式变量 / 全局函数 / 语句关键字；`import db as d` 别名解析 |
| 快速文档 | 光标停在模块或成员上按 <kbd>F1</kbd>（macOS <kbd>Ctrl</kbd>+<kbd>J</kbd>）查看签名、返回值与中文说明 |
| 代码折叠 | 花括号块、多行字符串、内嵌脚本块、连续 `import`、连续行注释、Linq `select` |
| 结构视图 | <kbd>⌘</kbd>+<kbd>7</kbd> 查看 import / 变量声明 / lambda / return / exit / Linq 大纲 |
| 其它 | 注释切换（`//`、`/* */`）、括号匹配、配色方案页「MagicScript」 |

### 效果示例

```javascript
import log;
import com.alibaba.fastjson.JSON;

var id = path.id;                       // path/header/cookie/session/body 均可补全

var user = db.selectOne("""
    select id, name from user where id = #{id}
""");                                    // 多行 SQL 内的关键字单独着色

if (user == null) {
    exit 404, '用户不存在';               // 关键字与 exit 语句均有高亮
}

log.info('查询用户 {}', id);              // db./log. 等内置模块成员可补全
return { code: 1, data: user };
```

## 版本与更新记录

| 版本 | 主要变化 |
| --- | --- |
| **0.1.4** | 修复插件描述格式（Marketplace 要求以拉丁字符开头），成功上架 |
| 0.1.3 | 插件署名改为 `zhengXL`；构建不再依赖本机绝对路径；新增发布脚本 |
| 0.1.2 | 插件说明与变更说明改为中文 |
| 0.1.1 | 修复关键字不高亮；修复点号路径 `import` 被误报语法错误；支持 `import java.util.*;` |
| 0.1.0 | 首个版本：语法高亮、模块补全、快速文档、代码折叠、结构视图 |

完整变更说明见 [Releases](https://github.com/814742394/magic-api-ext/releases) 与
[`META-INF/plugin.xml`](src/main/resources/META-INF/plugin.xml) 的 `<change-notes>`。

## 工程结构

```
src/main/java/org/ssssssss/magicapi/idea/
├── lang/                  语言定义、文件类型、词法/语法入口、关键字、图标
│   ├── lexer/             手写 LexerBase 实现
│   ├── parser/            递归下降 Parser（优先级与 magic-script 源码一致）
│   └── psi/               IElementType 常量表
├── highlight/             语法高亮 + 配色方案页（含 KEYWORD 重标词法器）
├── completion/            内置模块数据（MagicBuiltins）与补全贡献者
├── documentation/         快速文档渲染与 provider
├── folding/               折叠
├── structure/             结构视图
└── annotator/             语义高亮与轻量检查
src/main/resources/META-INF/plugin.xml
docs/                      调研产出与流程文档（见下方「文档」一节）
samples/                   可直接打开的 .ms 示例脚本
```

## 数据来源与准确性

内置模块/函数数据以 **magic-api 2.2.2 源码** 为准、官方文档为辅，条目逐条标注了出处，见
[`docs/magic-api-builtins.md`](docs/magic-api-builtins.md)。语言语法（关键字、语句形态、优先级、
字面量）基于 **magic-script 1.9.0** 源码 + 实际解析验证，见 [`docs/grammar-reference.md`](docs/grammar-reference.md)。

几处**刻意做出的取舍**（与常见印象或文档不同）：

- **不提示 `query` 变量**：`Constants.java` 定义了该常量但从未注入上下文。
- **不提示 `response.addCookie/addCookies`**：文档残留，2.2.2 源码中不存在。
- **`redis`/`mongo`/`elasticsearch`/`nebula` 只给模块名、不给成员**：它们由 `DynamicMethod` 在运行时决定方法，静态扫描无法得知，编造会产生假补全。
- **`for` 只支持 `for (x in e)` / `for (k, v in e)`**：magic-script 没有 C 风格 `for(;;)`。
- **`if`/`while` 的括号可选**，`for` 的括号必需 —— 与运行时解析器一致。

## 文档

| 文档 | 内容 |
| --- | --- |
| [docs/marketplace-publishing.md](docs/marketplace-publishing.md) | JetBrains Marketplace 上架与更新流程（含描述格式校验等已知坑） |
| [docs/release.md](docs/release.md) | GitHub 发版流程（打 tag 自动出 Release、CI 配置要点） |
| [docs/grammar-reference.md](docs/grammar-reference.md) | magic-script 语法规范（基于 1.9.0 源码实测） |
| [docs/magic-api-builtins.md](docs/magic-api-builtins.md) | magic-api 内置模块与函数速查 |

## 已知限制

- 不做类型推断，因此 `var a = db.select(...)` 之后的 `a.` 不补全集合扩展方法（`map`/`filter`/…）。
- 不解析 magic-api 的服务端资源目录（`.ms` 文件的 `info.json` 分组/路径元数据），因为插件工作在纯文件模式下。
- 未实现「跳转到定义」和「重命名」，因为这些概念在 magic-script 里对应运行时注入的模块。

## 参与开发

```bash
# 构建（复用本机 IDEA 作为 SDK，避免下载数百 MB）
./gradlew buildPlugin -PideaPath="/Applications/IntelliJ IDEA.app"

# 运行全部测试（39 个：词法、语法、补全、文档、折叠、示例脚本）
./gradlew test -PideaPath="/Applications/IntelliJ IDEA.app"

# 启动一个装好本插件的沙箱 IDE，手工验证效果
./gradlew runIde -PideaPath="/Applications/IntelliJ IDEA.app"
```

编译/测试 SDK 的解析顺序：

1. `-PideaPath=...` 或 `gradle.properties` 中的 `ideaPath`
2. 环境变量 `IDEA_PATH`
3. 都没有时，自动下载 IntelliJ IDEA `2026.1.5`

测试使用平台提供的 `BasePlatformTestCase` 轻量夹具，覆盖词法、语法树、
补全、快速文档、折叠与真实示例脚本解析。

## 发版

推送 `v*` 标签即可自动完成测试、构建、创建 GitHub Release 并挂上插件包：

```bash
sed -i '' 's/^pluginVersion=.*/pluginVersion=0.1.5/' gradle.properties   # 递增版本
git add -A && git commit -m "Release 0.1.5"
git tag v0.1.5 && git push origin main --tags
```

上架到插件市场的详细步骤见 [docs/marketplace-publishing.md](docs/marketplace-publishing.md)。

## 许可

[MIT](LICENSE) — 与 magic-api / magic-script 一致。

## 反馈

- 问题与建议：<https://github.com/814742394/magic-api-ext/issues>
- 插件市场页面评分与评论：<https://plugins.jetbrains.com/plugin/35622-magic-api>
