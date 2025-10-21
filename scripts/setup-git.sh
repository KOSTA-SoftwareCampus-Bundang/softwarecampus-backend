#!/usr/bin/env bash
set -euo pipefail

# 레포 루트에서 실행하세요.

git config commit.template .gitmessage
git config core.hooksPath .githooks

echo "설정 완료:"
echo "- commit.template -> .gitmessage"
echo "- core.hooksPath -> .githooks"
echo "권장: 에디터 연동 (예: VS Code) -> git config core.editor \"code --wait\""

