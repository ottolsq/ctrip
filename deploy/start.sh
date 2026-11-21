#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT/deploy/run"
JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot}"
JAR="$(ls "$ROOT"/ctrip-backend/target/backend-*.jar 2>/dev/null | head -1 || true)"
DIST="$ROOT/ctrip-frontend/dist"

if [ -z "$JAR" ] || [ ! -d "$DIST" ]; then
  echo "未找到 jar 或 dist，请先执行：bash deploy/build.sh" >&2
  exit 1
fi

mkdir -p "$RUN_DIR"

if curl -sf "http://localhost:8081/api/v1/banners" >/dev/null 2>&1; then
  echo "==> 后端 8081 已在运行"
else
  echo "==> 启动后端 jar（8081）"
  nohup "$JAVA_HOME/bin/java" -jar "$JAR" --server.port=8081 \
    > "$RUN_DIR/backend.log" 2>&1 &
fi

for _ in $(seq 1 60); do
  if curl -sf "http://localhost:8081/api/v1/banners" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! curl -sf "http://localhost:8081/api/v1/banners" >/dev/null 2>&1; then
  echo "后端启动失败，日志：$RUN_DIR/backend.log" >&2
  tail -30 "$RUN_DIR/backend.log" >&2 || true
  exit 1
fi

WSL_ROOT="$(echo "$ROOT" | sed -E 's#^/([a-zA-Z])/#/mnt/\L\1/#')"
WSL_DIST="$(echo "$DIST" | sed -E 's#^/([a-zA-Z])/#/mnt/\L\1/#')"
echo "==> 启动 WSL nginx（8088）"
wsl.exe -d Ubuntu-24.04 -- bash "$WSL_ROOT/deploy/nginx/start-nginx.sh" "$WSL_DIST"

for _ in $(seq 1 20); do
  if curl -sf "http://localhost:8088/" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! curl -sf "http://localhost:8088/" >/dev/null 2>&1; then
  echo "nginx 启动失败" >&2
  exit 1
fi

echo "==> 部署完成"
echo "前端: http://localhost:8088/"
echo "接口: http://localhost:8088/api/v1"
echo "后端: http://localhost:8081/"
