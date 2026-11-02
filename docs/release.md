# GitHub 发版流程

> 适用范围：本仓库的版本发布（GitHub Release + 自动构建）。
> 相关文档：[JetBrains Marketplace 发布流程](marketplace-publishing.md)

---

## 0. 一句话版本

```bash
# 改版本号 → 提交 → 打 tag 推送，剩下的 CI 自动做
sed -i '' 's/pluginVersion=0.1.4/pluginVersion=0.1.5/' gradle.properties
git add -A && git commit -m "Release 0.1.5"
git tag v0.1.5 && git push origin main --tags
```

推送 tag 后会自动完成：跑测试 → 构建插件包 → 创建 Release → 挂上 zip → 生成变更说明。

---

## 1. 完整发版步骤

### 1.1 更新版本号

`gradle.properties` 是**版本唯一来源**（`plugin.xml` 里的 `<version>` 由构建时注入，不要手改）：

```properties
pluginVersion=0.1.5
```

同时更新 `plugin.xml` 的 `<change-notes>`（用户在 IDE 里看到的更新说明），把新版本放在最前面。

### 1.2 本地验证

```bash
./gradlew test buildPlugin -PideaPath="/Applications/IntelliJ IDEA.app"
ls -lh build/distributions/
```

测试必须全绿再继续。

### 1.3 提交并打 tag

```bash
git add -A
git commit -m "Release 0.1.5"
git tag -a v0.1.5 -m "Magic API 0.1.5"
git push origin main --tags
```

### 1.4 等 CI

在仓库 **Actions** 页面观察：

| 工作流 | 职责 | 触发条件 |
| --- | --- | --- |
| **Build** | 测试 + 构建 + **创建 GitHub Release** | 推送 `main` 或 `v*` 标签 |
| **Publish to Marketplace** | 发布到 JetBrains Marketplace | 推送 `v*` 标签 |

通常 3–8 分钟（首次因下载 IDE SDK 会更久，之后走缓存）。

### 1.5 验收

```bash
curl -s https://api.github.com/repos/814742394/magic-api-ext/releases \
  | python3 -c "import json,sys; [print(r['tag_name'], [a['name'] for a in r['assets']]) for r in json.load(sys.stdin)]"
```

应看到 `v0.1.5` 且附件里有 `magic-api-idea-0.1.5.zip`。

Release 页面：`https://github.com/814742394/magic-api-ext/releases/tag/v0.1.5`

---

## 2. Release 的三种做法

| 方式 | 谁来做 | 点评 |
| --- | --- | --- |
| 网页手动创建 | 点鼠标 | 偶尔发版可以，但附件要手动传 |
| `gh release create` | 需装 gh CLI | 本机未安装 |
| **Actions 自动** | 打 tag 自动出 | ✅ 本项目采用，零手工 |

自动化的核心是 `.github/workflows/build.yml` 里这两个步骤（仅在 `v*` tag 上运行）：

```yaml
- name: Read plugin version
  if: startsWith(github.ref, 'refs/tags/v')
  id: version
  run: |
    VERSION=$(grep -E '^pluginVersion=' gradle.properties | cut -d= -f2)
    echo "value=$VERSION" >> "$GITHUB_OUTPUT"

- name: Create GitHub Release
  if: startsWith(github.ref, 'refs/tags/v')
  uses: softprops/action-gh-release@v2
  with:
    name: Magic API ${{ github.ref_name }}
    tag_name: ${{ github.ref_name }}
    files: build/distributions/*.zip
    generate_release_notes: true
    fail_on_unmatched_files: true
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

用 `GITHUB_TOKEN` 而不是个人令牌，无需任何额外配置。

---

## 3. 关键配置（缺一个就会失败）

### 3.1 `permissions: contents: write`

新版仓库的 `GITHUB_TOKEN` 默认**只读**，创建 Release 会失败：

```
⚠️ GitHub release failed with status: 403
{"message":"Resource not accessible by integration"}
```

修法是在 workflow 顶层加：

```yaml
permissions:
  contents: write
```

### 3.2 tag 触发必须在 `on.push` 里声明

```yaml
on:
  push:
    branches: [main]
    tags: ['v*']     # 少了这行，推 tag 不会触发 Build，也就没有 Release
```

`branches` 和 `tags` 是并列关系：只写 `branches` 时，推 tag 不触发。

### 3.3 不要用 `secrets` 做步骤级 `if`

```yaml
if: ${{ secrets.PUBLISH_TOKEN != '' }}   # ❌ GitHub 直接判定整个 workflow 无效
```

表现是运行详情里 **jobs 数为 0**，日志为空。改用：

```yaml
- name: Check publish token
  id: token
  env:
    PUBLISH_TOKEN: ${{ secrets.PUBLISH_TOKEN }}
  run: |
    if [ -n "$PUBLISH_TOKEN" ]; then echo "present=true" >> "$GITHUB_OUTPUT";
    else echo "present=false" >> "$GITHUB_OUTPUT"; fi

- name: Publish
  if: steps.token.outputs.present == 'true'
```

### 3.4 缓存 IDE SDK

平台 SDK 约 1.5 GB，不缓存时每次构建都要重新下载。workflow 里已配置：

```yaml
- name: Cache IntelliJ Platform SDK
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches/modules-2
      ~/.gradle/caches/9.8.2/transforms
    key: intellij-sdk-${{ runner.os }}-${{ hashFiles('build.gradle.kts', 'gradle.properties') }}
```

---

## 4. 构建 SDK 的解析顺序

`build.gradle.kts` 按以下优先级选择编译用 IDE：

1. `-PideaPath=...` 或 `gradle.properties` 里的 `ideaPath`
2. 环境变量 `IDEA_PATH`
3. 都没有时 → `intellijIdea("2026.1.5")`（CI 走这条，需下载）

```bash
# 本地（有 IDEA，4 秒完成）
./gradlew buildPlugin -PideaPath="/Applications/IntelliJ IDEA.app"

# 模拟 CI（无本地 IDE，会下载 SDK）
./gradlew buildPlugin
```

### ⚠️ 不要再用 `create("IC", ...)`

IntelliJ IDEA Community 自 2025.3 起不再单独发布，旧写法会失败：

```
[org.jetbrains.intellij.platform] IntelliJ IDEA Community (IC) is no longer
published since 2025.3 (253), use: intellijIdea("2026.1.5")
```

另外注意版本号要用**完整三段**（`2026.1.5`）：`2026.1` 只有 POM、没有可下载的压缩包（tar.gz 返回 404）。

---

## 5. 撤销/重做一次发版

```bash
# 删本地 tag
git tag -d v0.1.5

# 删远端 tag（连带触发一次 Release 删除）
git push origin :refs/tags/v0.1.5

# 修正问题后重新打 tag
git tag -a v0.1.5 -m "Magic API 0.1.5"
git push origin refs/tags/v0.1.5
```

> Release 由 tag 驱动，**改 tag 内容后必须删掉重建**，只改 commit 而不动 tag 不会重新触发。

---

## 6. 网络问题（本机环境特有）

`github.com` 的 443 端口在部分网络下会间歇性连不上（TLS 被重置），但 `api.github.com` 正常。
表现为 `git push` 卡 75 秒后：

```
fatal: unable to access 'https://github.com/...': Failed to connect to github.com port 443
```

**临时绕过**（已验证可用）：

```bash
git push https://ghfast.top/https://github.com/814742394/magic-api-ext.git main --tags
```

**根治建议**：把本机公钥加到 GitHub（Settings → SSH and GPG keys），改用 SSH：

```bash
ssh-keygen -lf ~/.ssh/id_ed25519.pub     # 查看公钥指纹
cat ~/.ssh/id_ed25519.pub                # 把整行内容贴到 GitHub
git remote set-url origin git@github.com:814742394/magic-api-ext.git
```

> 代理地址属于第三方服务，仅用于推送时绕行，**不要写进 `.git/config`**（已确认当前 `origin` 是标准 GitHub 地址）。

---

## 7. 发版检查清单

- [ ] `pluginVersion` 已递增
- [ ] `plugin.xml` 的 `<change-notes>` 已补充本版内容
- [ ] 本地 `./gradlew test buildPlugin` 通过
- [ ] 已提交并推送到 `main`
- [ ] 已打 `v<version>` 标签并推送
- [ ] Actions 里 Build 显示 success
- [ ] Release 页面有 zip 附件
- [ ] 需要上架 Marketplace 时，继续执行 [marketplace-publishing.md](marketplace-publishing.md)
