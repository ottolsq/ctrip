#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WSL_ROOT="$(echo "$ROOT" | sed -E 's#^/([a-zA-Z])/#/mnt/\L\1/#')"

echo "==> 停止 WSL nginx"
wsl.exe -d Ubuntu-24.04 -- bash "$WSL_ROOT/deploy/nginx/stop-nginx.sh" || true

echo "==> 停止后端 8081"
PID="$(netstat -ano | grep -E 'TCP.*:8081 .*LISTENING' | awk '{print $5}' | head -1 || true)"
if [ -n "${PID:-}" ]; then
  taskkill //PID "$PID" //F >/dev/null 2>&1 || true
  echo "已停止 PID $PID"
else
  echo "8081 未在监听"
fi
