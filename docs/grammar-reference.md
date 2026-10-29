# magic-script 语法参考（供手写递归下降解析器使用）

> 基准：本地源码 `/tmp/ms-probe`（`pom.xml` version **1.9.0**，最新 commit `8c2ec7d`「实现解构语法的剩余属性收集（...rest）功能」）。
> 本文所有结论都以源码为依据；凡与 README 不一致处标 ⚠️ 并以源码为准。
> 源码路径前缀统一省略为 `parsing/`、`ast/` 等，实际均为 `src/main/java/org/ssssssss/script/...`。
>
> **重要方法论说明**：本文不仅读了源码，还把 `/tmp/ms-probe` 用 `javac` 全量编译后，直接用 `Parser.parse()` 对上百个片段做了 AST 打印与执行验证（探针脚本见附录 A）。
> 文中标注「✅实测」的条目表示除读码外还经过程序验证；标注「读码」表示仅由源码逻辑推导。

---

## 0. 结论速览（实现解析器前必看）

| 结论 | 依据 |
| --- | --- |
| 语言**没有** `do-while`、`function`、`switch`、`class`、`typeof`、`delete`、`? :` 的 `?:` 简写、`??`、`**`、`in` 表达式 | `parsing/Parser.java:98-146` 的 `parseStatement()` 分派表里没有任何一处匹配这些词；实测 `do{}while()`/`function f(){}`/`switch` 均报错 ✅实测 |
| `for` **只有一种形态**：`for (x in expr)` / `for (k, v in expr)`。`for(;;)`、`for(x : y)` 均报错 | `Parser.java:510-536` 强制 `expect("in")`；实测 `for(;;)` → `期待 'an identifier'`，`for(x:y)` → `期待 'in'` ✅实测 |
| `var` **一次只能声明一个变量**，`var a = 1, b = 2` 报错；但解构 `var {a,b} = ...` / `var [a,b] = ...` 支持 | `Parser.java:478-489` 单 token 路径；实测 `var a = 1, b = 2` → `Expected a variable, field...` ✅实测 |
| `let`/`const` 是 `var` 的**完全同义别名**（同一段代码 `matchVarDefine()` → `parseVarDefine()`），仅 `const` 要求必须有初始值且禁止再赋值 | `Parser.java:185-187, 486-488`；`ast/binary/BinaryOperation.java:26-28`、`ast/UnaryOperation.java:29-31` ✅实测 |
| 语句分隔**不靠换行也不靠分号**，纯靠语法自洽；`;` 只是被无条件吃掉的分隔符（允许重复 `;;`），且**不能出现在语句开头** | `Parser.java:141-144`（`while (stream.match(";", true))` 在语句末尾）；实测 `;var a = 1` 报错 ✅实测 |
| 块 `{...}` 是**表达式位置**的 map 字面量，不是语句块；不存在裸块语句 | `Parser.java:857-858, 913-959`；实测 `{ var a = 1 }` → `期待 ':'` ✅实测 |
| `if`/`while`/`for` 的条件**不要求**括号（`if a {}` 合法），但**必须**有 `{}` 语句块 | `Parser.java:545-551, 498-507`（`parseExpression()` 后直接 `parseFunctionBody()`）；实测 `if(a) return 1` → `期待 '{'` ✅实测 |
| `and`/`or` **大小写不敏感**（`AND`/`And` 都算），但**不能做变量名**；`instanceof` **大小写敏感** | `parsing/Tokenizer.java:170-173` 用 `equalsIgnoreCase` 归入 `SqlAnd`/`SqlOr`；Tokenizer.java:168-169 用 `equals` 归入 `InstanceOf` ✅实测 |
| `<>` 是 SQL 风格不等于，**只能在 LINQ 内使用**（`inLinq=true`），普通表达式里用会编译期报错 | `TokenType.java:70`（`SqlNotEqual("<>","<>",true)`）+ `Parser.java:621-623` ✅实测 |
| `->` 与 `=>` 完全等价，都是 lambda | `Tokenizer.java:64`（`stream.match("=>",true) \|\| stream.match("->",true)`）✅实测 |
| 顶层 `return` **完全合法**（脚本本身就是一个方法体），`return;` 合法但 `return` 后紧跟 EOF 非法 | `MagicScript.java:105`（`parser.parse(script)` 直接产出方法体）；`Parser.java:576-583` ✅实测 |
| 存在文档未提的 **`assert expr : expr[,expr...]`** 语句，编译为「条件为假则 `exit`」 | `Parser.java:126-127, 265-280`；`ast/statement/Assert.java:12-41` ✅实测 |
| 存在文档未提的 **泛型化变量声明** `Type name = expr`（`matchTypeDefine()`），即 `String a = 'x'`、`Map m = {}` 合法；泛型类型给不了 | `Parser.java:132-139, 148-183` ✅实测 |
| 存在文档未提的 **`import 'x' as Name` 与 `import x.y.Z as Name` 两种都支持**，但 `import db as logger`（无引号模块名）**是错的**会解析成两条语句 | `Parser.java:294-342` + `isStringLiteral` 分支；实测 `import db as logger` → `Import` + `VariableDefine  as logger` ✅实测 |
| `select ... from ...` 是**表达式**，可直接作为右值；`from` 后的别名**必填**，否则 `LINQ中，别名是必须的` | `Parser.java:852-853, 734-763`；`ast/linq/LinqSelect.java:46-49` ✅实测 |
| 字符串模板是**反引号**`` ` `` + `${}`，只有反引号支持插值；`'`/`"`/`"""` 都是纯字符串 | `Tokenizer.java:115-154`；`ast/literal/StringLiteral.java:61-63` ✅实测 |
| ```` ```lang ... ``` ```` 内嵌脚本块解析成 `LanguageExpression`，是**表达式**不是语句 | `Tokenizer.java:92-113`（连续产出两个 `Language` token）；`Parser.java:889-891`（`new LanguageExpression(stream.consume(), stream.consume())`）✅实测 |
| 正则字面量 `/\d+/gim` 的识别**依赖前一个 token 类型**（上下文敏感），且前面不能是字面量 | `Tokenizer.java:321-350` ✅实测 |
| 数字 `0x`/`0b` 后**按值自动定类型**（byte→int→long），与 Java 不同；`0x1F` 实际是 `ByteLiteral` | `Tokenizer.java:274-288 autoNumberType()` ✅实测（`return 0x80` → `Integer 128`） |

---

## 1. 关键字完整列表

### 1.1 词法器层面的「硬关键字」

`Tokenizer.java:156-180` 的 `tokenizerIdentifier()` 会把标识符文本特判成下面几类 token（**这些词永远不可能是 `Identifier`**）：

| 文本 | 归入 TokenType | 大小写 | 源码 |
| --- | --- | --- | --- |
| `true` / `false` | `BooleanLiteral` | 敏感 | `Tokenizer.java:164-165` |
| `null` | `NullLiteral` | 敏感 | `Tokenizer.java:166-167` |
| `instanceof` | `InstanceOf` | **敏感**（`InstanceOf` 是普通标识符） | `Tokenizer.java:168-169` ✅实测 |
| `and` / `AND` / `And` … | `SqlAnd` | **不敏感**（`equalsIgnoreCase`） | `Tokenizer.java:170-171` ✅实测 |
| `or` / `OR` / `Or` … | `SqlOr` | **不敏感** | `Tokenizer.java:172-173` ✅实测 |

> 因此 `var and = 1` / `var or = 1` 会报「期待 'an identifier'」，而 `var And = 1` 同样报错（不敏感）——✅实测。
> 但 `var Import = 1`、`var InstanceOf = 1`、`var Select = 1` 全部合法 ——✅实测。

### 1.2 语句分派关键字（`Parser.java:98-146` 的处理顺序 + `KEYWORDS` 常量）

`Parser.java:60` 的 `KEYWORDS` 列表（决定「哪些词不能做变量名」）：

```
import as var let const return break continue if for in new
true false null else try catch finally async while exit and or throw
```

> 注意：`assert` 被注释掉了（`throw"/*, "assert"*/`），所以 `assert` **可以**做变量名 ✅实测（`var assert = 1` 通过，`var x = assert(1)` 解析为 `FunctionCall`）。
> `select`、`from`、`where`、`group`、`by`、`having`、`order`、`join`、`left`、`on`、`limit`、`offset`、`desc`、`asc` **不在** `KEYWORDS` 里，只属于 `LINQ_KEYWORDS`（`Parser.java:61`），可以做变量名 ✅实测（`var select = 1` 通过，但 `var y = select` 会报错，见 §7.3）。

### 1.3 逐个关键字的支持情况

| 关键字 | 支持 | 具体语义 | 源码依据 |
| --- | --- | --- | --- |
| `var` | ✅ | 声明变量，可无初值；单变量 | `Parser.java:106, 185-187, 430-490` |
| `let` | ✅ | 与 `var` 完全等价（无任何额外语义/块级作用域） | `Parser.java:186`（同一条 `match`） ✅实测 |
| `const` | ✅ | 与 `var` 同路径；**必须有初始值**，且编译期禁止再赋值/`++`/`--` | `Parser.java:449,465,479,486-488`；`BinaryOperation.java:26-28`；`UnaryOperation.java:29-31` ✅实测 |
| `if` | ✅ | `if <expr> { … }`，可 `else if` / `else`；括号可选 | `Parser.java:545-574` |
| `else` | ✅ | 只能紧跟 `if` 块之后；`else if` 会被拆成嵌套 `IfStatement`；`else` 与 `if` 之间**不允许分号**（⚠️ `if(a){}; else{}` 报错） | `Parser.java:555-570` ✅实测 |
| `for` | ✅ 但只有 `for (x in e)` / `for (k, v in e)` | 强制 `(` + `in`；不支持 C 风格与 `for(x:y)` | `Parser.java:510-536` ✅实测 |
| `in` | ✅ | 仅作为 `for` 的组成部分被 `expect("in")` 消费；**不是运算符**（`1 in [1,2]` 报错） | `Parser.java:529` ✅实测 |
| `while` | ✅ | `while <expr> { … }`，括号可选，必须有 `{}` | `Parser.java:498-508` |
| `do` | ❌ **不支持** | 不在 `KEYWORDS`，也不在分派表，被当普通标识符 | `Parser.java:60, 98-146` ✅实测 `do{}while(a)` 报错 |
| `function` | ❌ **不支持** | 函数只能写成 lambda（`=>` / `->`） | 同上 ✅实测 |
| `return` | ✅ | `return` / `return expr`；`return` 后是 `;` 或 `}` 时无返回值（**EOF 不算**，以裸 `return` 结尾会报错）；**顶层合法** | `Parser.java:576-583`；`MagicScript.java:105` ✅实测 |
| `break` | ✅ | 裸 `break`，无标签 | `Parser.java:122-123` |
| `continue` | ✅ | 裸 `continue`，无标签 | `Parser.java:116-117` |
| `exit` | ✅ | `exit e1, e2, …`（**至少一个**表达式，`exit` 单独出现报错；`exit(200)` 因括号表达式巧合通过，`exit(1,2)` 报错） | `Parser.java:124-125, 250-257` ✅实测 |
| `try` / `catch` / `finally` | ✅ | `catch` 的捕获变量括号**可选**；`try(...)` 支持 try-with-resources 变体 | `Parser.java:344-399` ✅实测 |
| `throw` | ✅ | `throw expr`，只接受**单个**表达式 | `Parser.java:128-129, 259-263` ✅实测 `throw 1, 2` 报错 |
| `import` | ✅ | 三种形态，见 §2.10 | `Parser.java:294-342` |
| `as` | ✅ | 仅用于 `import`（有引号形态） | `Parser.java:321-324` |
| `new` | ✅ | `new Type(args)`，**必须带参数括号** | `Parser.java:980-982, 415-428` ✅实测 `new Date` 报错 |
| `async` | ✅ | `async <方法调用/函数调用/lambda>`，产出 `AsyncCall`（返回 `Future`） | `Parser.java:118-119, 282-292` ✅实测 |
| `and` / `or` | ✅ | SQL 风格逻辑与/或，与 `&&`/`||` **同级不同组**（见 §3） | `Parser.java:35-36`；`Tokenizer.java:170-173` |
| `assert` | ✅ **（文档未提）** | `assert cond : e1[, e2...]`；也接受 `assert cond`（此时解析失败回退成两条表达式语句，**不是** assert） | `Parser.java:126-127, 265-280`；`Assert.java:12-41` ✅实测 |
| `select`/`from`/`where`/`group`/`by`/`having`/`order`/`join`/`left`/`on`/`limit`/`offset`/`desc`/`asc` | ✅（仅 LINQ 上下文） | 见 §2.11 | `Parser.java:61, 734-841` |
| `switch`/`case`/`default` | ❌ | 同 `do` | ✅实测 |
| `class`/`interface` | ❌ | 同 `do` | ✅实测 |
| `typeof`/`delete`/`void`/`yield` | ❌ | 同 `do` | ✅实测 |
| `this`/`super` | ❌ | 普通标识符（可用作变量名） | 读码 |
| `assert` 之外的 `debugger`/`with` 等 | ❌ | 同 `do` | 读码 |

---

## 2. 语句形式（伪 BNF）

顶层结构：**一个语句序列，整体就是一个方法体**（`MagicScript.java:105` → `Parser.parse(source)` 直接返回 `List<Node>` 作为方法体；`Parser.java:77-90`）。

```bnf
Program        ::= Statement* EOF

Statement      ::= ImportStmt
                 | VarDefineStmt
                 | IfStmt
                 | ReturnStmt
                 | ForStmt
                 | WhileStmt
                 | ContinueStmt
                 | AsyncStmt
                 | TryStmt
                 | BreakStmt
                 | ExitStmt
                 | AssertStmt
                 | ThrowStmt
                 | TypedVarDefineStmt          /* Type name = expr，靠回溯识别 */
                 | ExpressionStmt

/* 注意：分号是"语句尾部的可选垃圾"，可重复。
   Parser.java:141-144: while (stream.match(";", true)) ;
   且语句开头不允许出现分号（实测 `;var a = 1` 报错）。 */
```

### 2.1 变量声明

```bnf
VarDefineStmt  ::= ("var" | "let" | "const") VarTarget ( "=" Expression )?
VarTarget      ::= Identifier
                 | "{" DestructureList "}"        /* map 解构 */
                 | "[" DestructureList "]"        /* list 解构 */
DestructureList::= DestructureItem ("," DestructureItem)*
DestructureItem::= Identifier | "..." Identifier  /* rest，最多一个，位置任意 */
```

要点（`Parser.java:430-490`）：

* **单变量**：`var a`、`var a = expr`。`const a` 无初始值 → `const修饰的变量需要给初始值` ✅实测。
* **不支持逗号多变量**：`var a = 1, b = 2` 报错 ✅实测（`parseVarDefine` 读完一个 `Identifier` 和可选 `= expr` 就返回，`,` 留给表达式解析器 → 报错）。
* **解构**：`var {a, b} = expr`（map 解构，按**属性名**取）、`var [a, b] = expr`（list 解构，按**下标**取）。
  * rest：`var {a, ...rest} = obj`、`var [o, p, ...q] = arr`；**只能有一个 rest**，多了报 `解构语法中只能有一个剩余属性`（`Parser.java:441-443`）。
  * map 解构的 rest 生成「剩余属性对象」，list 解构的 rest 生成「剩余元素列表」（`ast/statement/VariableDestructuringDefine.java:65-97`）。
  * 解构的 `=` 是**可选**的（`stream.match(Assignment, true)` 不用 expect，`Parser.java:464`），`var {a,b} c` 也能通过，语义上把 `c` 当右值 ✅实测。**解析器照抄这一宽松行为即可**。
  * 不支持 `{a: x}` 重命名（实测 `var {a: x} = m` → `期待 '{', 获得 ':'`）。⚠️ 与 JS 不同。
  * 不允许空解构 `var {} = m` / `var [] = m`（要求至少一个 Identifier）✅实测。

### 2.2 赋值

```bnf
ExpressionStmt ::= Expression            /* 赋值在表达式层，见 §3.1 */
```

```js
a = 1
a.b = 1                // MemberAccess 作为 lvalue（VariableSetter）
a[0] = 1               // MapOrArrayAccess 作为 lvalue
a += 1                 // 复合赋值：编译期展开为 a = a + 1
obj?.x = 1             // 语法合法；但运行期 null 目标仍会报 target is null ✅实测
```

* 赋值是**右结合**的（`parseBinaryOperator` 对同一层用 `while` 左折叠，但 `AssigmentOperation` 的特判让它实际表现为右结合；`MagicScriptCompiler.java:186-194` 对 `a = b = 1` 有专门处理）。
* ⚠️ **`var x = a = b = 1` 语法通过但运行期报「赋值目标应为变量」** ✅实测（`a = b` 先被折叠，再整体赋给 a）。写解析器时按「赋值二元算子 + 右结合」实现即可，无需特判。
* 复合赋值 `+= -= *= /= %= <<= >>= >>>= &= |= ^=` 全部支持（`Parser.java:34` + `BinaryOperation.java:50-133`）。

### 2.3 if / else

```bnf
IfStmt      ::= "if" Expression Block ("else" "if" Expression Block)* ("else" Block)?
Block       ::= "{" Statement* "}"
```

* 条件**不需要**括号：`if a { }` 合法 ✅实测；`if (a) { }` 也合法。
* **必须**有 `{}` 块：`if(a) return 1` → `期待 '{'` ✅实测。
* `else if` 在 AST 上是**嵌套 `IfStatement`**（`Parser.java:556-563`，`elseIfs` 列表）。
* ⚠️ `else` 前**不允许分号**：`if(a){}; else {}` 报错（`;` 在 `parseStatement` 尾部被吃，之后 `else` 变成新语句 → `literal cannot be used alone`）✅实测。
* 空块 `{}` 合法 ✅实测。

### 2.4 for（唯一形态）

```bnf
ForStmt     ::= "for" "(" Identifier ( "," Identifier )? "in" Expression ")" Block
```

* **只有** `for (value in expr)` 与 `for (key, value in expr)`（`Parser.java:510-536`）：
  * 单变量 = 遍历值（list 的元素 / map 的值）；
  * 双变量 = 前者是索引/键，后者是值。
* `for` 后**必须**有 `(` 和 `)`（与 `if`/`while` 不同！），实测 `for x in y {}` → `期待 ')'`。
* 不支持 `for(;;)`、`for(x : y)`、`for(var i = 0; ...)` ✅实测（`for(var i=0;...)` 会先报「变量名不能定义为关键字」，因为 `var` 被当变量名）。
* 循环变量名不能是关键字（`Parser.java:516,522` 的 `checkKeyword`）。
* 循环体**必须**是 `{}` 块。

### 2.5 while

```bnf
WhileStmt   ::= "while" Expression Block
```

* 括号可选（`while a { }` 合法 ✅实测），块必填。
* `break;` / `continue;` 在体内有效（`ast/statement/WhileStatement.java:30-46` 用 `markLabel` + `exitLabel`）。

### 2.6 无 do-while

```bnf
/* DoWhileStmt ::= "do" Block "while" "(" Expression ")"   —— 不存在 */
```

`do` 不在 `KEYWORDS`，也不在 `parseStatement` 分派表 → 被当普通标识符 ✅实测。

### 2.7 return / break / continue

```bnf
ReturnStmt   ::= "return" ( Expression )?     /* 无值时后必须是 ";"、"}" 或 EOF */
BreakStmt    ::= "break"
ContinueStmt ::= "continue"
```

* `Parser.java:576-583`：`return` 后用 `stream.match(false, Semicolon, RightCurly)` **前瞻**判断有无返回值（注意：**不**判断 EOF，所以文件末尾裸写 `return` 会报错）✅实测。
* 顶层 `return` 合法，脚本整体即方法体。
* 只有一个 `Return` 且其值是单个变量时，`MagicScript.compile()` 会走「简单取值」快速路径（`MagicScript.java:140-146`），这是实现细节，不影响语法。

### 2.8 exit / throw / assert

```bnf
ExitStmt     ::= "exit" Expression ( "," Expression )*
ThrowStmt    ::= "throw" Expression
AssertStmt   ::= "assert" Expression ":" Expression ( "," Expression )*
```

* `exit`（`Parser.java:250-257`）：`do-while` 结构，**至少一个**表达式；`exit;` 报错 ✅实测。
* ⚠️ `exit(200)` 恰好能通过，但那不是「调用语法」，而是 `parseExpression()` 把 `(200)` 当成了**括号表达式**；`exit(1,2)` 会因为逗号表达式不存在而报 `期待 ')'` ✅实测。所以多参数必须去掉括号：`exit 200, 'ok'`。
* `throw`（`Parser.java:259-263`）：只能一个表达式，`throw 1, 2` 报错 ✅实测。
* `assert`（`Parser.java:265-280`）：⚠️ **用 try/catch 回溯**实现——先按 `assert cond : e1, e2` 解析，失败则 `resetIndex` 退回普通表达式。所以 `assert a`（无冒号）**不会**被识别为 assert，而是两条表达式语句（`VariableAccess assert` + `VariableAccess a`）✅实测。
* `Assert` 的语义是「条件为假 → 相当于 `exit` 列出后续表达式」（`Assert.java:34-40`），常用来 `assert cond : 500, 'msg'`。

### 2.9 try / catch / finally

```bnf
TryStmt     ::= "try" TryResources? Block
                ( "catch" ( "(" Identifier ")" )? Block )?
                ( "finally" Block )?

TryResources::= "(" ( ( VarDefineStmt | TypedVarDefineStmt ) ";"? )* ")"
```

* `catch` 的**捕获变量括号可省略**（`try{…}catch{…}` 合法，`Parser.java:385-389`）✅实测。但省略后 `exceptionVarNode == null`，编译期会走「无 catch 分支」判定（`TryStatement.java:59`），⚠️ **实测 `try{throw 'x'}catch{return 'caught'}` 的异常未被捕获**（异常穿透）。写解析器时按语法接受即可，但文档里应提示用户写 `catch(e)`。
* try-with-resources（`Parser.java:348-377`）：括号内**只允许变量定义**（`var x = …` 或 `Type x = …`），用 `;` 分隔也允许不加分隔符；其它形式报 `try 括号中只允许写赋值语句` ✅实测。资源在 finally 里自动 `close()`（`TryStatement.java:28-38, 48-55`）。
* `finally` 只允许一个（第二个 `finally` 报错）✅实测。

### 2.10 import

```bnf
ImportStmt  ::= "import" ImportTarget ( "as" Identifier )?
ImportTarget::= StringLiteral
              | Identifier ( "." ( Identifier | "*" ) )*
```

实际形态（`isStringLiteral` 是源码里的局部变量名，决定走类加载还是模块加载）：

| 写法 | 最终 `isStringLiteral` | 绑定到的变量名 | 后端调用 |
| --- | --- | --- | --- |
| `import 'java.lang.System'` | true | 取最后一段 `System`（`Parser.java:326-335`） | `loadClass` |
| `import 'java.lang.System' as System` | true | `System` | `loadClass` |
| `import java.lang.System` | true（循环中置位，`Parser.java:306`） | 同上，取最后一段 | `loadClass` |
| `import java.io.*` / `import 'java.text.*'` | true | 不绑定变量（`isImportPackage()` 为真，`Import.java:30-32`） | `context.addImport("java.io.")`，只影响后续自动导包 |
| `import db`（无点号、无引号） | **false** | `db` | `loadModule("db")`，导入**模块** |
| `import '@my/functions' as f` | true，且以 `@` 开头 | 必须显式 `as` | `loadFunction("my/functions")`；不带 `as` 报 `Expected as` ✅实测 |

源码细节（`Parser.java:294-342`）：
* 无引号且无点号 → `isStringLiteral=false` → 走 `loadModule`（模块导入）。
* 无引号但有点号 → 循环里 `isStringLiteral = true`，拼接 Span 文本；`.*` 结尾时跳出。
* 引号形态且 `packageName.startsWith("@")` 时**必须有 `as`**，否则报 `Expected as`（`Parser.java:327-334`）✅实测。
* 变量名不能是关键字（`checkKeyword`，`Parser.java:322-324`）✅实测 `import 'x' as var` 报错。

⚠️ **`import db as logger`（无引号模块名 + as）是错误写法**：`as` 只在 `isStringLiteral` 分支里被消费，所以会被解析成 `import db` 加一条 `as logger` 语句（后者被 `matchTypeDefine` 当成 `Type name` 形式的声明）✅实测。README 的写法示例 `import log as logger;` 因此是**误导性的**——正确写法是 `import log;`（变量名 `log`）或给模块名加引号（但引号形态走 `loadClass` 不走 `loadModule`，见下）。

> 补充（✅实测）：`import mymod` 后 `mymod.hi()` 可用；`import 'mymod'` 会走 `loadClass` 返回 `null` → 运行期 `对象为空`。所以「模块」和「类」的导入必须靠**有没有引号**区分，解析器只需正确区分这两种分支，语义交给后端。

### 2.11 LINQ（`select` 表达式）

不是语句，是**表达式**，但语法量大，单独列出（`Parser.java:734-841`）。

```bnf
LinqSelect  ::= "select" LinqFieldList "from" LinqField
                LinqJoin*
                ( "where" Expression )?
                ( "group" "by" Expression ( "," Expression )* )?
                ( "having" Expression )?
                ( "order" "by" LinqOrderItem ( "," LinqOrderItem )* )?
                ( "limit" Expression ( "offset" Expression )? )?

LinqFieldList ::= LinqField ( "," LinqField )*
LinqField     ::= Expression ( Identifier )?        /* 别名必填（from 处） */
LinqOrderItem ::= Expression ( "asc" | "desc" )?
LinqJoin      ::= ( "left" )? "join" LinqField "on" Expression
```

关键规则：
* **`select`/`from`/`where`/`group`/`by`/`having`/`order`/`join`/`left`/`on`/`limit`/`offset`/`asc`/`desc` 全部大小写不敏感**（`Parser.java:735,742,747,754,767,779,784,821,826` 全是 `ignoreCase=true`，LINQ 关键字判断用 `stream.match(false, true, "desc", "asc")`）✅实测 `SELECT a FROM t t1 WHERE ...` 通过。
* **`from` 后面的别名是必填的**，否则 `LinqSelect` 构造期抛 `LINQ中，别名是必须的`（`LinqSelect.java:46-49`）✅实测。
* `select` 后面可以用 `*`（`WholeLiteral`，仅 LINQ 内，`Parser.java:887-888`），`t.*` 也可（`MemberAccess(whole=true)`，`Parser.java:1038-1040`）。
* 字段别名是可选的（`select a b` → 别名 b）；但 `*` 后跟别名会报 `* 后边不能跟别名`。
* LINQ 内 `=` 表示**相等比较**而非赋值（`BinaryOperation.java:33`：`linqLevel == 0 ? AssigmentOperation : EqualOperation`），且 `@` 运算符和 `<>` 合法。
* `limit` 必须**先于** `offset`；`offset` 只在 `limit` 之后才被识别（`Parser.java:754-760`）。
* ⚠️ `offset` 单独出现（无 limit）不被识别，会变成后续语句。

### 2.12 函数 / lambda 定义

```bnf
LambdaExpr   ::= LambdaParams "=>" LambdaBody
               | LambdaParams "->" LambdaBody
LambdaParams ::= Identifier                       /* 单参数可省括号 */
               | "(" ( Identifier ( "," Identifier )* ( "," )? )? ")"
LambdaBody   ::= Expression                       /* 隐式 return */
               | Block                            /* 显式 return */
```

* 没有 `function` 关键字，没有具名函数声明；**函数值只能赋给变量**：`var f = (a) => a + 1`。
* `=>` 和 `->` 完全等价（`Tokenizer.java:64`）。
* 体为 `{...}` 时是语句块；体为表达式时自动包 `return`（`Parser.java:696-722`）。
* ⚠️ **`() => {}` 的 `{}` 被解析成「空 map 字面量」而非空语句块**：`Parser.java:701` 先尝试 `parseExpression()`，`{}` 恰好是合法 map → 得到 `LambdaFunction{ Return{ MapLiteral } }` ✅实测（`var f = () => {}` 调用返回空 `LinkedHashMap`）。这是**真实行为**，写解析器若追求 100% 兼容必须照抄「先试表达式、失败再试块」的顺序。
* `(a,b,) => 1` 尾部逗号允许 ✅实测。
* `((a,b))=>a` 报错 ✅实测（只允许单层括号）。

### 2.13 块 / 表达式语句

* **不存在裸块语句**：`{ ... }` 在语句位置会被当 map 字面量（`Parser.java:857-858`），所以 `{ var a = 1 }` 报 `期待 ':'` ✅实测；`{ a: 1 }` 单独成句会报 `literal cannot be used alone`（`Parser.java:92-96` 的 `validateNode`）。
* 表达式语句几乎任意表达式都可以，但**字面量不能单独成句**（`literal cannot be used alone`）。实测：`1 + 2` ✅、`[1,2,3].size()` ✅、`"abc"` ❌、`{a:1}` ❌。
* 单表达式脚本（整个文件只有一个表达式）会被自动 `return`（`MagicScript.java:162-168`）✅实测 `1 + 2` 返回 3。

---

## 3. 表达式与运算符优先级

### 3.1 优先级表（从**低**到**高**）

来源：`Parser.java:32-45` 的 `BINARY_OPERATOR_PRECEDENCE`。`parseBinaryOperator(precedence, level, …)` 的递归是「下标小 = 外层 = 优先级低」，所以**下标越大结合越紧**。

| 下标 | 运算符 | 结合 | 备注 |
| --- | --- | --- | --- |
| （最外层，非二元） | `? :` | 右结合 | `parseTernaryOperator`；三元比所有二元都低 |
| 0 | `=` | 见下注 | 赋值 |
| 1 | `>>>=` `>>=` `<<=` `^=` `\|=` `&=` `%=` `/=` `*=` `-=` `+=` | 左 | 复合赋值 |
| 2 | `\|\|` `or` | 左 | `or` 与 `\|\|` **同组**、同级 |
| 3 | `&&` `and` | 左 | `and` 与 `&&` **同组**、同级 |
| 4 | `\|` | 左 | 位或 |
| 5 | `^` | 左 | 位异或 |
| 6 | `&` | 左 | 位与 |
| 7 | `===` `==` `!==` `!=` `<>` | 左 | `<>` 仅 LINQ 可用 |
| 8 | `<` `<=` `>` `>=` `instanceof` | 左 | `instanceof` 与比较**同级** |
| 9 | `+` `-` | 左 | 加减 |
| 10 | `<<` `>>` `>>>` | 左 | 移位 |
| 11 | `*` `/` `%` | 左 | 乘除模 |
| 更紧（非二元） | `!` `~` `-` `+` `++` `--`（前缀）、`++` `--`（后缀） | — | 一元，见 §3.3 |
| 最紧 | `.` `?.` `[]` `()` `::` | 左 | 后缀链，见 §3.4 |

⚠️ **三个反直觉点（都以实测校正为准）**：

1. **移位比加减结合更紧**（下标 10 > 9），与 Java/C 相反：

   | 表达式 | magic-script | Java |
   | --- | --- | --- |
   | `1 << 2 + 3` | `(1 << 2) + 3 = 7` ✅实测 | `1 << (2+3) = 32` |
   | `1 + 2 << 3` | `1 + (2 << 3) = 17` ✅实测 | `(1+2) << 3 = 24` |
   | `2 * 3 << 1` | `(2*3) << 1 = 12` ✅实测（`*` 下标 11 更紧） | 12（同） |

2. **位运算 `& | ^` 夹在逻辑与比较之间**（下标 4/5/6，在比较 7、8 之下）：`1 == 1 & 2` 解析成 `(1==1) & 2`，运行期报「操作符 `&` 不支持 (Boolean, Integer)」✅实测。不要「按直觉」把位运算放到比较之后。

3. **`=` 与 `==` 在 LINQ 内同层**：见 §3.2。

> 注：`=` 虽在最外层，但 `parseBinaryOperator` 对同层用 `while` 左折叠，而 `AssigmentOperation` 的编译期特判（`MagicScriptCompiler.java:186-194`）让它表现成**右结合**；`a = b ? 1 : 2` 会被特判成 `a = (b ? 1 : 2)`（`Parser.java:599-603` 把三元塞进赋值的右操作数）✅实测。

### 3.2 LINQ 内的优先级表（不同！）

`Parser.java:46-58` 的 `LINQ_BINARY_OPERATOR_PRECEDENCE` 与普通表只有**一处语义差异**：

| 差异 | 普通表 | LINQ 表 |
| --- | --- | --- |
| `=` 的含义 | 下标 0，单独一层，**赋值** | 与 `==` `===` `!=` `!==` **同组**（下标 6），全部当**相等比较** |
| 乘除模 | 下标 11：`* / %` | 下标 10：`/ * %`（同层，仅数组字面顺序不同，无语义差异） |

> 因为同层运算符由同一个 `while` 左折叠，数组内元素顺序只影响 `stream.match(false, operators)` 的匹配次序，不改变结果。实现时可以直接复用同一张表，只在 `linqLevel > 0` 时把 `=` 编译成 `EqualOperation`（`ast/BinaryOperation.java:33`）。

### 3.3 一元与后缀

```bnf
UnaryExpr   ::= ( "!" | "~" | "-" | "+" | "++" | "--" ) UnaryExpr
              | PostfixExpr
PostfixExpr ::= Primary ( "++" | "--" )?          /* 仅当 Primary 是 lvalue */
Primary     ::= Literal
              | Identifier PostfixChain
              | "(" LambdaOrParen ")"
              | "{" MapEntries "}"
              | "[" ListItems "]"
              | "..." Expression                   /* Spread */
              | "new" Identifier AccessChain "(" Args ")" PostfixChain?
              | "async" AsyncTarget
              | "select" ...                       /* LINQ，见 §2.11 */
              | "```" LanguageName Content "```"   /* LanguageExpression */
```

* 前缀运算符：`!` `~` `-` `+` `++` `--`（`Parser.java:59` 的 `UNARY_OPERATORS`，注意**没有** `typeof`/`delete`）。
* `-` 直接作用于数字字面量时会被折叠成负值（`Parser.java:643-646` 的 `useNeg()`），所以 `-1` 就是 `IntegerLiteral(1)`，AST 里没有 `UnaryOperation` ✅实测。
* 后缀 `++`/`--` 只在 operand 实现 `VariableSetter` 时接受（`Parser.java:685-689` → `VariableAccess` / `MemberAccess` / `MapOrArrayAccess`），因此 `1++`、`f(a)++` 报错 ✅实测。**`(a)++` 也不支持**（带括号后不再是 `VariableSetter`）✅实测。

### 3.4 后缀访问链（`parseAccessOrCall`）

`Parser.java:998-1046`，一个 `while` 循环依次处理五种后缀，**可任意混排**：

```bnf
PostfixChain ::= ( Access )*
Access       ::= "." Identifier                      /* MemberAccess */
               | "?." Identifier                     /* MemberAccess(optional=true) */
               | "." "*"                             /* 仅 LINQ：MemberAccess(whole=true) */
               | "[" Expression "]"                  /* MapOrArrayAccess */
               | "(" ArgList ")"                     /* FunctionCall 或 MethodCall */
               | "::" Identifier ( "(" ArgList ")" )? /* ClassConverter，参数可选 */
ArgList      ::= ( Expression ( "," Expression )* ( "," )? )?
```

要点：
* `?.` **只能**接 Identifier（不能 `?.[0]`、不能 `f?.()`）✅实测报错。optional 会向整条链传播（`MemberAccess.isOptional()` 递归判断，`MemberAccess.java:26-28`）。
* `::` 的转换名是**裸 Identifier**，可带可选参数列表：`"123"::int`、`"abc"::int(111)`、`"2020-01-01"::date('yyyy-MM-dd')`。连续的 `::` 左结合（`a::int::string`）。
* `::` 的优先级**高于**所有二元运算符、**低于**一元：`-1::int` = `-(1::int)` ✅实测；`a + 1::int` = `a + (1::int)` ✅实测。
* ⚠️ **调用后不能立即再调用**：`f(1)(2)` 报 `Expected a variable, field or method.`（因为 `FunctionCall` 既不是 `VariableAccess`/`MapOrArrayAccess` 也不是 `MemberAccess`）✅实测。`a.b(1)(2)` 同样报错。`f[0](2)` 可以（`MapOrArrayAccess` 允许）。
* `(` 分支的判定：`target instanceof VariableAccess || MapOrArrayAccess` → `FunctionCall`；`target instanceof MemberAccess` → `MethodCall`；否则报错。
* 后缀可以跨换行（`a\n.b`、`a\n?.b` 都合法）✅实测 —— 这是**自动分号插入陷阱**之一，见 §7.2。

### 3.5 Spread `...`

```bnf
SpreadExpr ::= "..." Expression
```

出现位置（均由 `parseUnaryOperator`/`parseAccessOrCallOrLiteral` 入口处统一处理）：
* 函数/方法实参：`f(...arr)`、`obj.m(...arr)`（`Parser.java:847-848`）
* list 元素：`[1, ...arr, 2]`（`Parser.java:961-974`，元素走 `parseExpression` 所以自然支持）
* map 值：`{...m, k: v}`（`Parser.java:920-931`，要求 `...` 前一个 token 是 `{` 或 `,`）
* 赋值右侧任意表达式位置：`var x = ...[1,2]` 也通过 ✅实测

编译期包成 `SpreadValue`（`ast/statement/Spread.java:26-33`），调用点通过 `arguments.stream().anyMatch(it -> it instanceof Spread)` 打「有展开参数」标记。

### 3.6 三元 / 空安全 / 其他

| 特性 | 支持 | 说明 |
| --- | --- | --- |
| `a ? b : c` | ✅ | 右结合；`a ? b ? c : d : e` 解析为 `a ? (b?c:d) : e` ✅实测 |
| `a ?? b`（空值合并） | ❌ | 报错 ✅实测 |
| `a ?: b`（Elvis） | ❌ | 报错 ✅实测 |
| `?.` 空安全链 | ✅ | 见 §3.4 |
| `!` 空断言后缀 | ❌ | 只有前缀 `!` |
| `in` 表达式 | ❌ | 仅 `for ... in` |
| `instanceof` | ✅ | 右侧是**表达式**（通常是类名 Identifier，也可是变量/字符串），编译为 `Class` 强转（`InstanceofOperation.java:24-29`）；`x instanceof 'java.lang.String'` 语法通过但运行期 ClassCastException ✅实测 |
| `**` 幂 | ❌ | 报错 ✅实测（被解析成两个 `*`） |

---

## 4. 字面量细节

### 4.1 数字

```bnf
Number ::= Hex | Binary | Decimal
Hex    ::= "0" ("x"|"X") HexDigit (HexDigit|"_")* ("L"|"l")?
Binary ::= "0" ("b"|"B") ("0"|"1"|"_")* ("L"|"l")?
Decimal::= Digit (Digit|"_")* ( "." (Digit|"_")* )? Suffix?
Suffix ::= "b"|"B" | "s"|"S" | "l"|"L" | "f"|"F" | "d"|"D" | "m"|"M"
```

源码：`Tokenizer.java:201-288`。

* **后缀**（大小写均可）：`b/B`→`byte`、`s/S`→`short`、`l/L`→`long`、`f/F`→`float`、`d/D`→`double`、`m/M`→`BigDecimal`、无后缀→`int`（`123` 实际是 `IntegerLiteral`）。
* `1b` 若值超 `byte` 范围不报错，运行期 `Byte.parseByte` 失败才对（`ByteLiteral.java:26-33`）——实测 `200b` 词法通过 ✅。
* **`0x`/`0b` 自动定型**（`autoNumberType`）：值 ≤ `Byte.MAX_VALUE` → `ByteLiteral`，≤ `Integer.MAX_VALUE` → `IntegerLiteral`，否则 `LongLiteral`。
  * 实测：`0x1F` → `ByteLiteral`；`0x80` → `IntegerLiteral`；`0x7FFFFFFF` → `IntegerLiteral`；`0x80000000` → `LongLiteral`。**与 Java 的「0x 后默认 int」不同** ✅实测（`return 0x80` → `Integer 128`）。
  * `0x`/`0b` 后带 `L` 后缀直接是 `LongLiteral`。
  * `0b` 后接非 0/1 会截断（`0b2` 只剩 `0b`，然后报错）✅实测。
* **下划线分隔**：整数、小数、十六进制、二进制都允许，且**位置随意**（`1_0`、`0x_1`、`1__0`、`1_` 都能通过词法）✅实测；解析成数值时才 `replace("_","")`。
* `1.` 合法（`DoubleLiteral`）✅实测；`.5` **不合法**（必须以数字开头）✅实测。
* 无科学计数法：`1e10` 被解析成 `1` + 标识符 `e10` ✅实测。
* `1.2.3` 报错（`.` 后必须是标识符）✅实测。
* 超过 long 范围的十进制整数在词法阶段不报错，编译期 `NumberFormatException`（`0xZZ` 当场抛 `NumberFormatException`）✅实测。

### 4.2 字符串

```bnf
String ::= "'" (Char|Escape)* "'"
         | '"' (Char|Escape)* '"'
         | '"""' Any* '"""'              /* 多行文本块 */
         | '`' TemplatePart* '`'         /* 模板字符串 */
Escape ::= "\\" | "\n" | "\r" | "\t" | '\"' | "\\'"
```

源码：`Tokenizer.java:290-319`（普通/三引号）、`Tokenizer.java:115-154`（模板）、`StringLiteral.java:65-88`（反转义）。

* 单引号 `'...'`、双引号 `"..."` 等价；**不允许换行**（报 `定义的字符串不能换行`）✅实测。
* 三引号 `"""..."""` 允许换行，用于 SQL 文本块；**不做 trim/dedent**，内容原样保留（含首尾换行）✅实测（`""" \n select ... \n """` 结果带前后换行）。
* 转义只认：`\\` `\n` `\r` `\t` `\"` `\'`；其它 `\x` 原样保留反斜杠（`StringLiteral.java:70-85`）。
* **模板字符串是反引号**`` ` ``：`` `hello ${name}, ${a+b}` ``。`${...}` 内部走**完整的 `parseExpression()`**（`Parser.java:898-910`），可嵌套模板（`` `a${`b${c}`}d` `` 合法 ✅实测）。
* ⚠️ `${}` 内的解析是**独立 TokenStream**（`Tokenizer.java:130-137` 用 `tokenizer(stream, …, "}")` 截断），**括号计数很弱**：`` `${ {a:1} }` `` / `` `${ f({a:1}) }` `` 会报错 ✅实测。写解析器时可以直接按「找到配对的 `}`」实现（比原实现更宽松是安全的）。
* `"${a}"`、`'${a}'`、`"""${a}"""` 都是**纯字符串**，不插值 ✅实测。
* 空模板 `` `` `` 合法（返回空串）✅实测。

### 4.3 正则

```bnf
Regex ::= "/" RegexBody "/" Flag*
Flag  ::= "g" | "i" | "m" | "s" | "u" | "y"
```

源码：`Tokenizer.java:321-416`、`ast/literal/RegexpLiteral.java`。

* **上下文敏感**：只有当「前一个 token」是 `,` `;` `:` `}` `[` `(` `=` `!=` `===` `!==` `==` `&&` `||` `and` `or` `<>` `?` `=>`/`->` `!` 之一，或者**当前位于 token 流最开头**时，`/` 才会被尝试当正则起始；且前一个 token 若是 `LiteralToken` 一律不试（`Tokenizer.java:322-350`）✅实测。
* 因此 `var x = 1; /a/` 能识别（前一个是 `;`），但 `return /a/` **不能**（前一个是标识符 `return`）✅实测。**这是原实现的缺陷**，写解析器时建议按「是否处于「期望操作数」的位置」来判断，比原实现更正确。
* flags：`g`（仅记位 1，不影响 Pattern）、`i`→`CASE_INSENSITIVE`、`m`→`MULTILINE`、`s`→`DOTALL`、`u`→`UNICODE_CHARACTER_CLASS`、`y`→位 16（`RegexpLiteral.java:17-34`）。
* 字符类 `[...]` 内允许 `/`（`deep` 计数，`Tokenizer.java:366-395`）；转义 `\/` 支持；遇到换行放弃匹配并回退（此时 `/` 变成除法）。
* 未闭合报 `Missing ']'` 或回退为除法。

### 4.4 内嵌脚本块 ```` ```lang ... ``` ````

```bnf
LanguageBlock ::= "```" LanguageName Content "```"
```

* 词法器一次产出**两个** `Language` token（语言名 + 内容），`Parser.java:889-891` 连续 `consume()` 两次构造 `LanguageExpression` ✅实测。
* 语言名必须是合法标识符（`Tokenizer.java:96-111`），否则报「```后需要标识类型」。
* 是**表达式**，可赋值：`var x = ```sql select 1 ``` `；也可直接当语句（表达式语句）✅实测。
* 内容不解析（原样交给运行时，`LanguageExpression.java:15-30`）。

### 4.5 list / map

```bnf
ListLiteral ::= "[" ( ListItem ( "," ListItem )* ( "," )? )? "]"
ListItem    ::= Expression | SpreadExpr

MapLiteral  ::= "{" ( MapEntry ( "," MapEntry )* ( "," )? )? "}"
MapEntry    ::= MapKey ":" Expression
              | MapKey                          /* 简写：{a}、{"k"} */
              | SpreadExpr
MapKey      ::= Identifier | StringLiteral | "[" Expression "]"
```

源码：`Parser.java:913-974`。

* list：`[]` 空、`[1,2,]` 尾逗号、`[...arr]` 展开都支持 ✅实测。
* map key 三种写法（`Parser.java:932-940`）：
  1. 裸标识符 → 当字符串 key（`{a:1}`、`{if:1}`、`{return:1}` 都可以，**不检查关键字**）✅实测；
  2. 字符串字面量 → `{"k":1}`、`{'k':1}`、`{"""k""":1}`；
  3. `[expr]` → 动态 key（`{[k]:v}`）。
* **数字 key 不支持**：`{1:'a'}` 报 `期待 'an identifier'` ✅实测。
* ⚠️ **`true`/`null`/`and`/`or`/`instanceof` 不能做裸 key**（它们是特殊 token 不是 Identifier）：`{true:1}` 报错 ✅实测。
* 简写（value 省略）：`{a}` → `{a: a}`；`{"k"}` → `{"k": "k"}`（注意不是变量！`Parser.java:942-948`）；`{[k]}` → key 与 value 都是 `k` 的变量 ✅实测。
* map 展开：`{a:1, ...m, c:2}`，要求 `...` 紧跟 `{` 或 `,`（`Parser.java:920-931`）。
* 两种字面量都实现 `Literal`，因此**不能单独成句**（`literal cannot be used alone`）✅实测。

---

## 5. 注释

```bnf
LineComment  ::= "//" .* ( "\n" | EOF )
BlockComment ::= "/*" .*? "*/"
```

* 只支持 `//` 与 `/* */`（`Tokenizer.java:182-199`）。**不支持 `#` 注释**，`#` 会触发 `Unknown token` ✅实测。
* 注释默认**不进 token 流**（`matchComment=false`，`Tokenizer.java:17-21`）；`Parser` 用的是默认值，所以注释在任何位置都等于空白。
* 因此注释可以出现在**任何两个 token 之间**，包括运算符两侧、换行处：`var x = 1 /* c */ + 2` ✅实测。
* ⚠️ 未闭合的 `/*` 不报错（`skipUntil("*/")` 到文件尾即返回），实测 `var x = 1 /* unterminated` 解析成功 ✅实测。
* `//` 到行尾（`skipLine`）。
* 特殊标记：`MagicScript.DEBUG_MARK = "!# DEBUG\r\n"`，仅当源文本**以它开头**时被剥离并开启 debug（`MagicScript.java:39, 103-104`）——这是文件头标记，不是注释语法。

---

## 6. 常见写法示例

> 以下 13 个代码块全部抽出来交给本地编译的 `Parser.parse()` 跑过，**13/13 语法通过**。
> 其中 9 块（§6.1–6.5、6.8 的第二个 try、6.10 两块）在裸引擎上 `execute()` 也跑通并核对了返回值；
> 剩下 4 块（§6.6、6.7、6.9 及 §6.8 的第一个 try）依赖 magic-api 的 `db` / `log` 模块，裸引擎里会报「对象为空 / 找不到模块：db」——这是**环境缺少模块**，不是语法问题。
> 另外注意：`import db` 在 magic-api 中通常可省略（`auto-import-module=db` 自动导入）。

### 6.1 变量声明（var / let / const / 带类型）

```js
var count = 0
let name = 'magic'
const MAX = 100
var list = [1, 2, 3]
var map = {a: 1, b: 'two'}
var empty                     // 无初值，值为 null

String s = 'hello'            // 也支持 Type name = expr 形式（类型仅作占位）
Map cfg = {}
```

### 6.2 if / else if / else

```js
var score = 85
if(score >= 90) {
    return 'A'
} else if(score >= 80) {
    return 'B'
} else {
    return 'C'
}
```
（`if` 的条件括号可省略：`if score >= 90 { ... }` 同样合法。）

### 6.3 for in（集合 / Map / 区间）

```js
var list = ['a', 'b', 'c']
for(index, item in list) {
    System.out.println(index + ':' + item)
}

var map = {k1: 1, k2: 2}
for(key, value in map) {
    System.out.println(key + '=' + value)
}

var sum = 0
for(v in range(0, 100)) {      // range 含首含尾
    sum = sum + v
}
return sum
```

### 6.4 lambda（`=>` / `->`，隐式 return）

```js
var add = (a, b) => a + b
var inc = x -> x + 1
var pick = () => 42

var calc = (a, b) -> {
    var sum = a + b
    if(sum > 10) { return sum }
    return sum * 2
}
return calc(add(1, 2), inc(3))
```

### 6.5 map / list 字面量（含解构、展开、动态 key）

```js
var key = 'dynamic'
var m = {
    name: 'tom',
    [key]: 1,                  // 动态 key
    nested: {a: [1, 2, 3]}
}
var l = [1, 2, ...m.nested.a, 9]

var {name, nested} = m         // map 解构（按属性名）
var [first, second, ...rest] = l  // list 解构 + rest
return name + ':' + first
```

### 6.6 import（类 / 模块 / 静态导入 / 通配）

```js
import 'java.lang.System' as System       // 引号形态 → 加载 Java 类
import java.util.Date as Date             // 无引号带点号 → 也是类
import java.io.*                          // 通配：加入自动导包
import 'java.text.*'
import db                                 // 无点号无引号 → 加载模块，变量名 db

return new Date()
```
> ⚠️ 不要写 `import db as logger`（见 §2.10）。模块要改名只能 `import 'db' as logger`——但那会走类加载分支。

### 6.7 exit（提前返回并带状态码）

```js
var user = db.selectOne('select * from sys_user where id = #{id}')
if(user == null) {
    exit 404, '用户不存在'
}
return user
```
（`exit` 会抛 `MagicExitException`，被 `MagicScript.execute` 捕获后作为脚本返回值。）

### 6.8 try / catch / finally + throw

```js
try {
    var r = 1 / 0
    return r
} catch(e) {
    log.error('计算出错: ' + e.message)
    return -1
} finally {
    log.info('done')
}
```
```js
try {
    throw 'custom error'
} catch(e) {
    return 'caught'
}
```

### 6.9 SQL 多行字符串（三引号）

```js
var sql = """
    select id, name, age
    from sys_user
    where id = #{id}
      and name like concat('%', #{name}, '%')
    order by id desc
"""
return db.select(sql)
```
（三引号不做 dedent，缩进会原样进入 SQL；模板字符串 `` `...` `` 也能写 SQL，但 `${}` 会被当表达式插值——SQL 里的 `${}` 需注意。）

### 6.10 链式调用 / Linq

```js
var list = [1, 2, 3, 4, 5]
var result = list
    .map(e => e * 2)
    .filter(e => e > 4)
    .size()
return result
```
```js
var users = [{id: 1, name: 'A'}, {id: 2, name: 'B'}]
var rows = select
        u.id,
        u.name n,
        count(u.id) cnt
    from users u
    where u.id > 0
    group by u.id
    order by u.id desc
    limit 10
return rows
```
```js
return (select * from [{a: 1}, {a: 2}] t where t.a > 1).size()
```

---

## 7. 解析器实现注意点

### 7.1 关键字大小写

| 词 | 规则 | 依据 |
| --- | --- | --- |
| `and` / `or` | **不敏感**（`AND`/`And` 均可，且都进 `SqlAnd`/`SqlOr`） | `Tokenizer.java:170-173` ✅实测 |
| `instanceof` | **敏感**（`InstanceOf`/`INSTANCEOF` 是普通标识符） | `Tokenizer.java:168-169` ✅实测 |
| `true`/`false`/`null` | 敏感 | `Tokenizer.java:164-167` ✅实测 |
| 语句关键字（`var`/`if`/`for`/`in`/`else`/`try`/`async`/`exit`/…） | **全部敏感**（`TokenStream.match(String)` 默认 `ignoreCase=false`） | `TokenStream.java:221-233,240-242` ✅实测（`IF(a){}` 报错、`for(x IN y)` 报错、`ELSE` 报错） |
| LINQ 关键字 | **不敏感**（`select`/`from`/`where`/`group`/`by`/`having`/`order`/`join`/`left`/`on`/`limit`/`offset`/`asc`/`desc`） | `Parser.java:735-841` 全部 `ignoreCase=true` ✅实测 |
| `desc`/`asc` | 不敏感且**只是「无副作用尝试」**：`stream.match(false, true, "desc", "asc")` 匹配不上就当默认 asc | `Parser.java:784-788` |

> 实现建议：`and`/`or` 在**词法层**归一化（把文本转小写后判定），`instanceof` 用精确匹配，其余语句关键字用精确匹配，LINQ 关键字用 `equalsIgnoreCase`。

### 7.2 语句分隔：没有分号推断，但有「换行陷阱」

* **不需要分号**：连续语句靠每个 `parseStatement` 各自消费到语法闭合（`Parser.java:141-144` 只是**额外**吃掉尾随 `;`）。
* `;` 可以重复（`;;`），但**不能出现在语句开头**（`;var a = 1` 报错）✅实测。
* 因此 `var a = 1 var b = 2` 是合法的两条语句 ✅实测。
* **但换行不终止表达式**，因为词法器丢弃了换行信息（`Tokenizer` 不产出换行 token）。所以下面的写法会被粘在一起：

```js
var x = 1
[a]           // 被解析成 1[a]（MapOrArrayAccess）
var y = a
(b)           // 被解析成 a(b)（FunctionCall）
var z = f
(1)           // 被解析成 f(1)
```
✅以上三例均实测「粘住」。

实现建议：如果目标是与 magic-script **完全兼容**，就照抄（不做 ASI）；如果要更友好，可在 `[`/`(` 前引入「同一行」检查——**但这会与上游行为不一致**，建议至少在文档/报错信息里提示。

### 7.3 回溯点（递归下降必须预留 resetIndex 的地方）

原实现用 `TokenStream.makeIndex()/resetIndex()` 做回溯，共 5 处，缺一不可：

1. `parseStatement` 的 `matchTypeDefine()`（`Parser.java:131-139`）：是否为 `Type name = expr`。
   * 判定逻辑（`Parser.java:148-183`）：先吃一个 Identifier；若下一个也是 Identifier → 是类型声明；若「下一个 token 起点 == 前一个 token 终点」（紧邻，如 `Map{...}`）→ 不是；否则再看是否是 `Identifier { … }` / `Identifier [ … ]` 解构形态。
   * ⚠️ 副作用：`alpha beta` 会被解析成「类型 alpha + 变量 beta」的声明 ✅实测；`var x = 1\nreturn a` 中 `return` 是关键字不会被误判。
2. `parseUnaryOperator` 的 `"("` 分支（`Parser.java:649-682`）：先试着当 lambda 参数表，失败则回退按普通括号表达式解析。
   * 注意：失败时是**抛异常再 catch**（`Parser.java:704`），不是布尔返回——手写解析器应改为「先探查 `( params ) =>/->`」。
3. `createStringLiteral` 的模板字符串（`Parser.java:898-910`）：切换 TokenStream 递归解析 `${}`。
4. `parseAssert`（`Parser.java:265-280`）：整体 try/catch 回退。
5. `parseLambdaBody`（`Parser.java:696-722`）：先试表达式体，异常则试 `{...}` 块体，再不行按单条语句。

### 7.4 `<>` 运算符

* 词法：`SqlNotEqual("<>")`，`inLinq = true`（`TokenType.java:70`）。
* 语法：在比较层（级别 8）与 `!=` 同级。
* **普通表达式里用会编译期报错**：`Parser.java:621-623` → `<xxx> 只能在Linq中使用` ✅实测 `var x = a <> b` 报错。
* 所以解析器应保留该 token，在 `linqLevel == 0` 时抛出等价诊断。

### 7.5 `->` 也是 lambda

`Tokenizer.java:64`：`if (stream.match("=>", true) || stream.match("->", true))` → 统一产出 `TokenType.Lambda`。二者**完全无差别**，不要区分。

### 7.6 顶层 return 与「表达式脚本」

* 顶层 `return` 合法（脚本即方法体），`return;` 合法（`Parser.java:578` 前瞻 `;` 或 `}`）。
* ⚠️ **文件末尾裸写 `return` 会报错**：前瞻只看 `;`/`}`，EOF 时走 `parseExpression()` 报错 ✅实测。
* 若整个脚本**只有一个表达式**节点，会被自动包 `return`（`MagicScript.java:162-168`），因此 `1 + 2` 作为完整脚本返回 3 ✅实测。
* 字面量单独成句在**顶层**被禁止（`Parser.java:92-96` 的 `validateNode`），但**块内**同样禁止（`parseFunctionBody` 也调 `validateNode`）✅实测。

### 7.7 NewExpression 形式

```bnf
NewExpr ::= "new" Identifier AccessChain? "(" ArgList ")"
```

* `Parser.java:980-982`：`new` 在**标识符位置**被识别（`parseAccessOrCall(Identifier, true)`），随后调用 `parseNewExpression`。
* 内部先解析成 `MethodCall`/`FunctionCall`，再重包成 `NewStatement`（`Parser.java:415-428`）。
* **参数括号必须存在**：`new Date` 报 `Expected MethodCall or FunctionCall or LambdaFunction` ✅实测。
* 支持点号类名：`new java.util.Date()`、`new a.b.C(1)` ✅实测；不支持泛型尖括号（`new ArrayList<String>()` 报错）✅实测。
* `new` 之后的结果可以继续后缀：`new Foo(1).bar()`、`new Foo(1)[0]`、`new Foo()?.bar` ✅实测。
* `new` 只在**标识符位置**特判，作为**成员名**时是普通标识符：`a.new`、`m.new` 合法 ✅实测；但 `new.new` 报错（`new` 后必须跟 Identifier，而 `new` 本身不是 Identifier）✅实测。

### 7.8 其它易踩点

| 点 | 行为 | 依据 |
| --- | --- | --- |
| `if`/`while` 条件可省括号，`for` 不可省 | 见 §2.3/2.4/2.5 | `Parser.java:498-536` ✅实测 |
| 语句体必须是 `{}` | `if(a) x()` 报错 | ✅实测 |
| 不存在裸块 | `{...}` 永远是 map | ✅实测 |
| `select` 作变量名可以，但 `var y = select` 会当成 LINQ 起始 | 因为在 `Identifier` 位置特判了 `select` | `Parser.java:852-853` ✅实测 |
| `async` 位置特判 | 表达式位置的 `async` 一定进 `parseAsync()`，所以 `var x = async` 报错 | `Parser.java:850-851` ✅实测 |
| `exit`/`import` 作变量名 | `var exit = 1` 合法（语句位置才特判），但 `var x = exit` 里 `exit` 是普通标识符 | ✅实测 |
| 括号表达式不产生节点 | `(a)` 直接返回 `a`；`(1+2).toString()` 合法 | `Parser.java:679-682` ✅实测 |
| 逗号表达式不存在 | `(1,2)` 报错 | ✅实测 |
| `instanceof` 与 `<` `>` **同级**（同一下标 8） | `a instanceof Integer == true` 解析为 `(a instanceof Integer) == true`（✅实测）；`a < b == c` 也是左折叠，`1 < 2 < 3` 会得到 `(1<2) < 3` 并在运行期比较 boolean 与数字 | `Parser.java:41` ✅实测 |
| 字符串/数字字面量紧邻 | `1.toString()` 被解析成 `1.` + `toString()` | ✅实测 |
| `?.` 后必须 Identifier | `a?.[0]`、`f?.()` 报错 | `Parser.java:1041` ✅实测 |
| spread 只能有一个 rest（解构） | 报 `解构语法中只能有一个剩余属性` | `Parser.java:441-443` |
| 解构里 rest 位置任意 | `{a, ...b, c}` 合法（c 会被丢掉） | `Parser.java:439-457` ✅实测 |

---

## 附录 A：本文所用的验证方法

为了不「凭感觉读码」，把源码全量编译后写了一个 AST 探针：

```bash
# 全量编译（199 个源文件，无外部依赖，只需 JDK）
find /tmp/ms-probe/src/main/java -name '*.java' > /tmp/srcs.txt
javac -nowarn -encoding UTF-8 -d /tmp/msout @/tmp/srcs.txt

# 探针：解析并反射打印 AST
java -cp /tmp/msout:/tmp/gprobe/out Probe <cases.txt>     # 每个用例用一行 "-----" 分隔
java -cp /tmp/msout:/tmp/gprobe/out Exec  <cases.txt>     # 执行并打印返回值
```

探针 `/tmp/gprobe/Probe.java` 对每个片段执行 `new Parser().parse(src)`，再用反射逐层打印 AST 节点树（含 Span 文本）；`/tmp/gprobe/Exec.java` 用 `MagicScript.create(src, null).execute(new MagicScriptContext())` 跑真实语义。文中「✅实测」的结论都来自这套流程，共验证了 ~300 个片段。

> 注：探针只是验证工具，不属于交付物；仓库里没有新增任何文件，只有 `docs/grammar-reference.md`。

## 附录 B：与前文（README）的差异清单

| README 说法 | 源码事实 |
| --- | --- |
| 关键字表只列了 `var/if/for/in/continue/break/return/exit/try/catch/finally/import/as/new/true/false/null/async` | 实际还有 `let`/`const`/`else`/`while`/`and`/`or`/`throw`/`assert`，以及 LINQ 的 14 个关键字 |
| `import log as logger` | `as` 只在有引号的类/函数导入分支生效；模块导入写 `import log` 即可，`as` 会被解析成另一条语句 |
| 「当前 for 循环只支持两种，循环集合或 Map」 | 语法上只有 `for(x in e)` / `for(k,v in e)` 一种形态，靠 `e` 的类型决定迭代方式 |
| 类型表未提 `0x`/`0b`/下划线 | `0x`/`0b` 支持且**自动定型**（byte/int/long），下划线任意位置允许 |
| 未提 `assert`、`Type name = expr`、try-with-resources、`<>`、```` ``` ```` 语言块 | 全部实现（见 §2.8、§2.1、§2.9、§3.1、§4.4） |
| 「lambda: `()=>expr`、`(param1,param2)=>{...}`」 | 还支持单参数省括号 `x => x+1`，且 `->` 与 `=>` 等价 |
