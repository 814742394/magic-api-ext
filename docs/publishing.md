# 发布到 JetBrains Marketplace

## 一次性准备

1. **登录并创建插件**

   打开 <https://plugins.jetbrains.com/author/me> → **Upload plugin** → 选择
   `build/distributions/magic-api-idea-<version>.zip`，首次上传时填好：

   - 插件 ID：`org.ssssssss.magicapi.idea`（必须与 `plugin.xml` 一致，之后不可更改）
   - 名称：`Magic API`
   - 分类：Languages / Programming Languages 或 Frameworks

   首次上传后插件进入 **Pending review**，JetBrains 人工审核通常需要 1–3 个工作日。

2. **获取永久令牌**

   在 <https://plugins.jetbrains.com/author/me/tokens> 生成 **Permanent token**，
   形如 `perm:xxxxx`。这是一个账户级密钥，**不要写进仓库**。

## 发布方式

### 本地发布（推荐先把令牌放进环境变量）

```bash
export PUBLISH_TOKEN="perm:xxxxxxxxxxxxxxxx"

./gradlew publishPlugin -PideaPath="/Applications/IntelliJ IDEA.app"
```

令牌也可以放在仓库外的 `~/.gradle/gradle.properties`：

```properties
publishToken=perm:xxxxxxxxxxxxxxxx
```

### 通过 CI 发布

`.github/workflows/publish.yml` 会在推送 `v*` 标签时自动发布，
需要在仓库的 **Settings → Secrets and variables → Actions** 中新增：

| Secret | 值 |
| --- | --- |
| `PUBLISH_TOKEN` | 上面生成的永久令牌 |
| `CERTIFICATE_CHAIN` | 可选，签名用证书链 |
| `PRIVATE_KEY` | 可选，签名用私钥 |
| `PRIVATE_KEY_PASSWORD` | 可选，私钥口令 |

未配置签名证书时跳过 `signPlugin`，Marketplace 仍接受未签名包。

## 版本与渠道

- 版本号来自 `gradle.properties` 的 `pluginVersion`，**每次发布必须递增**，否则上传会被拒绝。
- `intellijPlatform.publishing.channels` 当前为 `default`（稳定渠道，所有用户可见）。
  预发布版本可改为 `listOf("eap")` 或 `listOf("beta")`。
- `sinceBuild = 261`、`untilBuild = null`（不限上界），覆盖 IDEA 2026.1 及以后。

## 发布后的检查清单

- [ ] Marketplace 页面的描述、图标、变更说明显示正常
- [ ] 在 IDEA 内通过 *Plugins → Marketplace* 搜索到并能安装
- [ ] 安装后 `.ms` 文件能正确高亮与补全
- [ ] `plugin.xml` 的 `<change-notes>` 已记录本次版本变更
