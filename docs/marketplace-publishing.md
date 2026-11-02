# JetBrains Marketplace 发布流程

> 适用范围：本仓库的 IDEA 插件（`org.ssssssss.magicapi.idea`）。
> 本文覆盖从「首次上架」到「日常更新」的完整流程，并记录已经踩过的坑。
>
> 相关文档：[GitHub 发版流程](release.md)

---

## 0. 两种发布路径怎么选

| 路径 | 适用场景 | 是否需要令牌 |
| --- | --- | --- |
| **网页上传** | 首次创建插件、偶尔发版 | 不需要 |
| **命令行 / CI** | 日常更新、自动化发版 | 需要永久令牌 |

**首次发布必须走网页上传**——JetBrains 要求插件先通过网页创建，之后才能用 API 更新。对不存在的插件调用 `publishPlugin` 会报 `Plugin not found`。

---

## 1. 首次上架（只需做一次）

### 1.1 准备插件包

```bash
./gradlew buildPlugin -PideaPath="/Applications/IntelliJ IDEA.app"
# 产物：build/distributions/magic-api-idea-<version>.zip
```

### 1.2 网页创建插件

打开 <https://plugins.jetbrains.com/author/me> → **Upload plugin**。

**第 1 步：Vendor Details**

| 字段 | 建议值 | 说明 |
| --- | --- | --- |
| Public Vendor Name | `zhengXL` | 对外显示，与 `plugin.xml` 的 `<vendor>` 保持一致 |
| Vendor ID | 留空 | 只影响 `/vendor/<id>` 主页 URL，**一旦设定不可更改** |
| Vendor Status | `Non-trader` | 个人免费插件选此项；选 Trader 需填商业信息 |
| Email | 你的邮箱 | 审核结果发送到这里 |
| 显示邮箱勾选框 | 不勾 | 勾选后邮箱公开，易被抓取 |

**第 2 步：Plugin Details**

| 字段 | 建议值 |
| --- | --- |
| Plugin file | `build/distributions/magic-api-idea-<version>.zip` |
| License | `MIT license` |
| Source code URL | `https://github.com/814742394/magic-api-ext` |
| Tags | `JavaScript`、`Programming Language`（可加自定义 `magic-api`、`magic-script`） |
| Channel | 留空（= Stable） |
| Plugin contains ads | **不勾** |
| Add a "Hidden" flag | **不勾**（勾了会从公开列表隐藏，别人搜不到） |

### 1.3 提交后

- 状态变为 **Pending review**，人工审核通常 1–3 个工作日
- 审核期间**搜不到、插件页 404 都是正常的**
- 结果发到上面填的邮箱

---

## 2. 日常更新（已上架之后）

### 2.1 直接开始（推荐）

```bash
cd /Users/zhengxl/dev/stone/magic-api-ext
./scripts/publish.sh
```

脚本行为：

1. 提示 `粘贴令牌后回车（不会显示）` —— 输入不回显、不进 shell 历史
2. 依次运行 `test` → `buildPlugin` → `publishPlugin`
3. 令牌只存在于该进程内存，不落盘

### 2.2 手动方式

```bash
export PUBLISH_TOKEN="perm:xxxxxxxxxxxxxxxx"
./gradlew publishPlugin -PideaPath="/Applications/IntelliJ IDEA.app"
unset PUBLISH_TOKEN   # 用完立即清除
```

令牌也可放在**仓库外**的 `~/.gradle/gradle.properties`：

```properties
publishToken=perm:xxxxxxxxxxxxxxxx
```

### 2.3 通过 GitHub Actions 发布

推送 `v*` 标签时 `.github/workflows/publish.yml` 自动运行。需要在
**Settings → Secrets and variables → Actions** 配置：

| Secret | 必填 | 用途 |
| --- | --- | --- |
| `PUBLISH_TOKEN` | 是 | 永久令牌；**未配置时工作流自动跳过并打印 notice**，不会报错 |
| `CERTIFICATE_CHAIN` | 否 | 签名证书链 |
| `PRIVATE_KEY` | 否 | 签名私钥 |
| `PRIVATE_KEY_PASSWORD` | 否 | 私钥口令 |

签名开关由**仓库变量** `ENABLE_SIGNING=true` 控制（不能用 secret 做 `if` 条件，见第 4 节）。
不签名也能通过审核，签名的作用是让用户能校验包未被篡改。

---

## 3. 版本管理规则

**版本号来自 `gradle.properties` 的 `pluginVersion`，每次发布必须递增，否则上传被拒。**

建议同时更新 `plugin.xml` 的 `<change-notes>`，它是用户在 IDE 里看到的「What's New」：

```xml
<change-notes><![CDATA[
    <b>0.1.5</b>
    <ul>
        <li>修复：……</li>
        <li>新增：……</li>
    </ul>
]]></change-notes>
```

渠道 `intellijPlatform.publishing.channels`：

| 值 | 可见性 |
| --- | --- |
| `default` | 所有用户（当前使用） |
| `beta` / `eap` | 仅主动订阅该渠道的用户 |

兼容范围：`sinceBuild = 261`、`untilBuild = null`（覆盖 IDEA 2026.1 及以后）。

---

## 4. 已踩过的坑（务必先读）

### 4.1 描述必须以拉丁字符开头

```
Invalid plugin descriptor 'description'. The plugin description must start with
Latin characters and have at least 40 characters.
```

`<description>` 开头是中文就会被打回。**修法**：把英文摘要放最前面，中文详细说明紧随其后。

```xml
<description><![CDATA[
    Language support for magic-api interface scripts (.ms files written in
    magic-script): syntax highlighting, completion, quick documentation ...

    <b>为 magic-api 接口脚本提供完整的 IDE 支持。</b>
    ...（中文分节内容）
]]></description>
```

提交前可先用脚本自检（去标签后判断首字符与长度）：

```bash
python3 - <<'PY'
import re
s = open('src/main/resources/META-INF/plugin.xml').read()
raw = re.search(r'<description><!\[CDATA\[(.*?)\]\]></description>', s, re.S).group(1)
plain = re.sub(r'\s+', ' ', re.sub(r'<[^>]+>', '', raw)).strip()
print('首字符拉丁:', plain[0].isascii(), '| 长度:', len(plain), '| 通过:', plain[0].isascii() and len(plain) >= 40)
PY
```

### 4.2 `secrets` 不能用于步骤级 `if`

```yaml
# ❌ 整个 workflow 被判为无效，jobs 数为 0
if: ${{ secrets.CERTIFICATE_CHAIN != '' }}
```

GitHub 不允许在 `if:` 中引用 `secrets`。**修法**：改用仓库变量 `vars.ENABLE_SIGNING`，
或在 `run` 里判断环境变量（本项目采用后者判断 `PUBLISH_TOKEN` 是否存在）。

### 4.3 审核期间不要重复推送

`approve=false` 时再次 `publishPlugin` 会覆盖待审内容、重新排队。
等本次审核通过再发下一个版本。

### 4.4 令牌泄露

令牌等同于账号发布权限，泄露后可被他人以你的名义上架版本。**令牌只能出现在**：

- 终端交互输入（`scripts/publish.sh`）
- 仓库外的 `~/.gradle/gradle.properties`
- GitHub Secrets

**不要**出现在截图、聊天、issue、提交、`.env` 或任何会被提交的文件里。
一旦泄露，立即到 <https://plugins.jetbrains.com/author/me/tokens> **撤销**并重新生成。

---

## 5. 状态自查

### 5.1 命令行查询（权威）

```bash
# 把 xmlId 换成你的插件 ID
curl -s 'https://plugins.jetbrains.com/api/plugins?xmlId=org.ssssssss.magicapi.idea&family=INTELLIJ' \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print({k:d.get(k) for k in ['id','name','approve','isHidden','isBlocked','hasUnapprovedUpdate']})"
```

| 字段 | 含义 |
| --- | --- |
| `approve: false` | ⏳ 待审核（此时搜索不到是正常的） |
| `approve: true` | ✅ 已通过，公开可见 |
| `hasUnapprovedUpdate: true` | 有已上传待审的新版本 |
| `isHidden: true` | 被隐藏，**搜不到** |
| `isBlocked: true` | 被封禁 |

### 5.2 判断是否真的上线

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://plugins.jetbrains.com/plugin/<id>-magic-api
# 404 = 还没上线（审核中）；200 = 已上线
```

搜索接口在审核期间返回 0 条结果是**预期行为**，不代表上传失败。

---

## 6. 发布后检查清单

- [ ] Marketplace 页面的描述、图标、变更说明显示正常
- [ ] 在 IDEA 内通过 *Settings → Plugins → Marketplace* 能搜到并安装
- [ ] 安装后 `.ms` 文件高亮、补全、文档均正常
- [ ] 插件 ID 与 `plugin.xml` 一致（不一致会导致「已安装但不生效」）
- [ ] `pluginVersion` 已递增，`<change-notes>` 已更新
- [ ] Git 仓库已打对应 tag 并在 GitHub 出了 Release（见 [release.md](release.md)）
