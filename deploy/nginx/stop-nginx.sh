#!/usr/bin/env bash
set -euo pipefail

BASE="$HOME/.local/ctrip-nginx"
STATE="$HOME/.local/state/ctrip-deploy"

if [ -x "$BASE/usr/sbin/nginx" ] && [ -f "$STATE/logs/nginx.pid" ]; then
  "$BASE/usr/sbin/nginx" -p "$STATE" -c "$STATE/conf/nginx.conf" -s stop 2>/dev/null || true
  echo "nginx 已停止"
else
  echo "nginx 未运行"
fi
