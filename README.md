# Magic API — IntelliJ IDEA 插件

![Build](https://github.com/814742394/magic-api-ext/actions/workflows/build.yml/badge.svg)


为 [magic-api](https://www.ssssssss.org/magic-api/) 的接口脚本（[magic-script](https://gitee.com/ssssssss-team/magic-script) 语言）提供 IDE 支持：
语法高亮、内置模块补全、快速文档、代码折叠、结构视图。

目标 IDE：**IntelliJ IDEA 2026.1+**（`sinceBuild=261`，自带 JRE 21+）。

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

## 安装

从源码构建后安装：

```bash
# 使用本机已安装的 IDEA 作为编译 SDK（推荐，避免下载数百 MB）
./gradlew buildPlugin -PideaPath="/Applications/IntelliJ IDEA.app"

# 或用环境变量 / gradle.properties 里的 ideaPath 指定
export IDEA_PATH="/Applications/IntelliJ IDEA.app"
./gradlew buildPlugin

# 不指定时自动下载 IntelliJ IDEA Community 2026.1 作为 SDK
./gradlew buildPlugin

# 产物
build/distributions/magic-api-idea-0.1.2.zip
```

在 IDE 中：<kbd>Settings</kbd> → <kbd>Plugins</kbd> → ⚙️ → <kbd>Install Plugin from Disk…</kbd> → 选择上面的 zip → 重启。

调试运行时模式：

```bash
./gradlew runIde        # 启动一个装有本插件的沙箱 IDE
```

## 工程结构

```
src/main/java/org/ssssssss/magicapi/idea/
├── lang/                  语言定义、文件类型、词法/语法入口、关键字、图标
│   ├── lexer/             手写 LexerBase 实现
│   ├── parser/            递归下降 Parser（优先级与 magic-script 源码一致）
│   └── psi/               IElementType 常量表
├── highlight/             语法高亮 + 配色方案页
├── completion/            内置模块数据（MagicBuiltins）与补全贡献者
├── documentation/         快速文档渲染与 provider
├── folding/               折叠
├── structure/             结构视图
└── annotator/             语义高亮与轻量检查
src/main/resources/META-INF/plugin.xml
docs/                      调研产出：语法规范、内置模块速查
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

## 0.1.2

- 插件说明（Settings → Plugins 中的 Overview）与变更说明改为中文。

## 0.1.1 修复

- **关键字不高亮**：magic-script 词法器把 `var`/`import`/`return` 等一律输出为标识符，而解析器靠文本判断关键字。
  原实现直接用该词法器做高亮，导致整份文件没有任何关键字着色。现新增
  `MagicScriptHighlightingLexer` 在高亮层把保留字重标为 `KEYWORD`，解析器行为不受影响。
- **点号路径 import 报语法错误**：`import com.alibaba.fastjson.JSON;` 之前只吃了一个标识符，后面的 `.` 被当成语法错误。
  现按 magic-script `parseImport` 的规则循环消费 `.` 段，并支持 `java.util.*`。

## 已知限制

- 不做类型推断，因此 `var a = db.select(...)` 之后的 `a.` 不补全集合扩展方法（`map`/`filter`/…）。
- 不解析 magic-api 的服务端资源目录（`.ms` 文件的 `info.json` 分组/路径元数据），因为插件工作在纯文件模式下。
- 未实现「跳转到定义」和「重命名」，因为这些概念在 magic-script 里对应运行时注入的模块。

## 开发

```bash
./gradlew test          # 28 个测试：词法、语法、补全、文档、折叠
./gradlew verifyPlugin  # 插件兼容性校验（需联网）
```

测试使用平台提供的 `BasePlatformTestCase` 轻量夹具。编译/测试 SDK 的解析顺序：

1. `-PideaPath=...` 或 `gradle.properties` 中的 `ideaPath`
2. 环境变量 `IDEA_PATH`
3. 都没有时，下载 IntelliJ IDEA Community 2026.1

这样在 IDE 里直接打开工程也能构建，同时不把某台机器的绝对路径写进版本库。

## 持续集成

`.github/workflows/build.yml` 会在每次 push / PR 时用 JDK 21 + IntelliJ IDEA Community 2026.1
跑完整测试并构建插件包，产物作为 workflow artifact 上传（见仓库 Actions 页面）。

## 许可

MIT，与 magic-api / magic-script 一致。
