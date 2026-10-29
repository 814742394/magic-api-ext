#!/usr/bin/env bash
#
# 发布到 JetBrains Marketplace。
#
# 用法：
#   ./scripts/publish.sh                       # 交互式输入令牌（不回显、不进 shell 历史）
#   PUBLISH_TOKEN=perm:xxx ./scripts/publish.sh    # 从环境变量读取
#
# 令牌只存在于这个进程的环境里，不会写入任何文件，也不会出现在 shell 历史中。

set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -z "${PUBLISH_TOKEN:-}" ]]; then
  echo "JetBrains Marketplace 永久令牌：https://plugins.jetbrains.com/author/me/tokens"
  # -s 不回显；read 到的值只存在于内存
  read -r -s -p "粘贴令牌后回车（不会显示）: " PUBLISH_TOKEN
  echo
  export PUBLISH_TOKEN
fi

if [[ -z "${PUBLISH_TOKEN:-}" ]]; then
  echo "错误：没有提供令牌" >&2
  exit 1
fi

# 沿用本机已安装的 IDEA 作为 SDK，避免下载数百 MB
IDEA_PATH_DEFAULT="/Applications/IntelliJ IDEA.app"
ARGS=()
if [[ -d "$IDEA_PATH_DEFAULT" ]]; then
  ARGS+=("-PideaPath=$IDEA_PATH_DEFAULT")
fi

echo "==> 运行测试"
./gradlew ${ARGS[@]+"${ARGS[@]}"} test --no-daemon --console=plain

echo "==> 构建插件包"
./gradlew ${ARGS[@]+"${ARGS[@]}"} buildPlugin --no-daemon --console=plain

echo "==> 发布到 Marketplace"
./gradlew ${ARGS[@]+"${ARGS[@]}"} publishPlugin --no-daemon --console=plain

echo
echo "完成。审核状态见 https://plugins.jetbrains.com/author/me"
