#!/usr/bin/env bash
set -euo pipefail

BASE="$HOME/.local/ctrip-nginx"
if [ -x "$BASE/usr/sbin/nginx" ] && [ -f "$BASE/etc/nginx/mime.types" ]; then
  echo "nginx 已就绪：$("$BASE/usr/sbin/nginx" -v 2>&1)"
  exit 0
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cd "$TMP"
echo "==> 下载 Ubuntu nginx 包（无需 sudo）"
apt-get download nginx nginx-common >/dev/null

mkdir -p "$BASE"
find . -maxdepth 1 -name '*.deb' -exec dpkg-deb -x {} "$BASE" \;
echo "nginx 安装完成：$("$BASE/usr/sbin/nginx" -v 2>&1)"
