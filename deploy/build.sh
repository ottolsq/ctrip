#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-25.0.5.8-hotspot}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> 打包后端"
cd "$ROOT/ctrip-backend"
./mvnw -q -DskipTests package

echo "==> 构建前端"
cd "$ROOT/ctrip-frontend"
npm run build

JAR="$(ls "$ROOT"/ctrip-backend/target/backend-*.jar | head -1)"
echo "==> 构建完成"
echo "jar : $JAR"
echo "dist: $ROOT/ctrip-frontend/dist"
