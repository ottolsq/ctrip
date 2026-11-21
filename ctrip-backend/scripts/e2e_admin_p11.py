#!/usr/bin/env python3
"""P1.1 管理端 API 级端到端验证。

前置条件：
- 后端已启动（默认 http://localhost:8080）；
- 已执行 P1 迁移与 admin/operator 种子账号 SQL。

运行：
    python -I ctrip-backend/scripts/e2e_admin_p11.py

覆盖：
- 管理员登录与用户列表；
- 注册普通用户 → 管理员按关键词检索 → 封禁；
- 封禁后旧 access token 立即 401；
- 解封后恢复原状态并重新登录；
- 内容运营访问 ADMIN 接口返回 403（方法级安全生效）。
"""

import json
import os
import random
import string
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")


def call(method, path, body=None, token=None, expect=200):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            code = response.status
            raw = response.read().decode()
            payload = json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        code = exc.code
        raw = exc.read().decode()
        payload = json.loads(raw) if raw else {}
    print(f"{method} {path} -> {code}")
    if code != expect:
        print("RESPONSE:", json.dumps(payload, ensure_ascii=False)[:1200])
        raise SystemExit(f"expected {expect}, got {code}: {path}")
    if expect < 400 and payload.get("success") is not True:
        print("RESPONSE:", json.dumps(payload, ensure_ascii=False)[:1200])
        raise SystemExit(f"unexpected failure: {path}")
    return payload.get("data"), payload


def main():
    admin_token, _ = call("POST", "/auth/login", {
        "credential": "admin@example.com", "password": "Admin12345"
    })
    admin_token = admin_token["accessToken"]
    page, _ = call("GET", "/admin/users?page=1&size=10", token=admin_token)
    assert "records" in page, page

    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"admin_e2e_{suffix}@example.com"
    username = f"adminE2E{suffix}"
    user_token_data, _ = call("POST", "/auth/register/email", {
        "email": email, "password": "Test12345", "username": username
    }, expect=201)
    user_token = user_token_data["accessToken"]

    search, _ = call(
        "GET", f"/admin/users?page=1&size=10&keyword={username}", token=admin_token)
    matches = [u for u in search["records"] if u["username"] == username]
    assert matches, search
    user_id = matches[0]["id"]

    detail, _ = call("GET", f"/admin/users/{user_id}", token=admin_token)
    assert detail["guideCount"] == 0 and detail["orderCount"] == 0, detail

    call("POST", f"/admin/users/{user_id}/ban", token=admin_token)
    _, payload = call("GET", "/users/me", token=user_token, expect=401)
    assert "已被封禁" in (payload.get("error") or ""), payload

    call("POST", f"/admin/users/{user_id}/unban", token=admin_token)
    relogin, _ = call("POST", "/auth/login", {
        "credential": email, "password": "Test12345"
    })
    me, _ = call("GET", "/users/me", token=relogin["accessToken"])
    assert me["role"] == "USER", me

    operator_token_data, _ = call("POST", "/auth/login", {
        "credential": "operator@example.com", "password": "Operator12345"
    })
    operator_token = operator_token_data["accessToken"]
    call("GET", "/admin/users", token=operator_token, expect=403)
    call("GET", "/admin/blind-box/templates", token=operator_token, expect=403)

    print("E2E ADMIN P1.1 PASS")


if __name__ == "__main__":
    sys.exit(main())
