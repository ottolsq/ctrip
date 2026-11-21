#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "用法：start-nginx.sh <frontend-dist-dir>" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST="$1"
BASE="$HOME/.local/ctrip-nginx"
STATE="$HOME/.local/state/ctrip-deploy"

bash "$SCRIPT_DIR/setup-nginx.sh"

mkdir -p "$STATE/conf" "$STATE/html" "$STATE/logs" \
  "$STATE/tmp/client_body" "$STATE/tmp/proxy" "$STATE/tmp/fastcgi" \
  "$STATE/tmp/uwsgi" "$STATE/tmp/scgi"

rm -rf "$STATE/html"/*
cp -r "$DIST"/. "$STATE/html/"
cp "$BASE/etc/nginx/mime.types" "$STATE/conf/mime.types"
sed "s|\${PREFIX}|$STATE|g" "$SCRIPT_DIR/nginx.conf.template" > "$STATE/conf/nginx.conf"

if [ -f "$STATE/logs/nginx.pid" ]; then
  "$BASE/usr/sbin/nginx" -p "$STATE" -c "$STATE/conf/nginx.conf" -s stop 2>/dev/null || true
  sleep 0.5
fi

"$BASE/usr/sbin/nginx" -p "$STATE" -c "$STATE/conf/nginx.conf"
echo "nginx 已启动：http://localhost:8088（prefix=$STATE）"
