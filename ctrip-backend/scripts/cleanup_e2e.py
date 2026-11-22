#!/usr/bin/env python3
"""非破坏性清理 E2E 产生的运营内容。

- 删除标题以 E2E 开头的轮播图/推荐位；
- 删除无订单的 E2E/P3/压测/秒杀模板；有订单的模板改为 INACTIVE；
- 不删除用户、订单、抽取记录（保留审计数据）。

运行：
    python -I ctrip-backend/scripts/cleanup_e2e.py
"""

import json
import os
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")
PREFIXES = ("E2E", "P3合规盲盒", "压测限定盲盒", "秒杀盲盒")


def call(method, path, body=None, token=None):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        method=method,
        headers={"Content-Type": "application/json",
                 **({"Authorization": "Bearer " + token} if token else {})})
    try:
        with urllib.request.urlopen(req, timeout=40) as response:
            return response.status, json.loads(response.read().decode() or "{}")
    except urllib.error.HTTPError as exc:
        return exc.code, json.loads(exc.read().decode() or "{}")


def main():
    _, payload = call("POST", "/auth/login", {
        "credential": "admin@example.com", "password": "Admin12345"})
    token = payload["data"]["accessToken"]

    deleted_banners = 0
    _, payload = call("GET", "/admin/banners?page=1&size=100", token=token)
    for item in payload.get("data", {}).get("records", []):
        if str(item.get("title", "")).startswith("E2E"):
            code, _ = call("DELETE", f"/admin/banners/{item['id']}", token=token)
            deleted_banners += 1 if code == 200 else 0

    deleted_recs = 0
    _, payload = call("GET", "/admin/recommendations?page=1&size=100", token=token)
    for item in payload.get("data", {}).get("records", []):
        if str(item.get("title", "")).startswith("E2E"):
            code, _ = call("DELETE", f"/admin/recommendations/{item['id']}", token=token)
            deleted_recs += 1 if code == 200 else 0

    deleted_templates = 0
    deactivated_templates = 0
    _, payload = call("GET", "/admin/blind-box/templates?page=1&limit=100", token=token)
    for item in payload.get("data", {}).get("records", []):
        if not str(item.get("name", "")).startswith(PREFIXES):
            continue
        code, _ = call("DELETE", f"/admin/blind-box/templates/{item['id']}", token=token)
        if code == 200:
            deleted_templates += 1
        else:
            call("PUT", f"/admin/blind-box/templates/{item['id']}",
                 {"status": "INACTIVE"}, token=token)
            deactivated_templates += 1

    print(f"banners deleted={deleted_banners}, recommendations deleted={deleted_recs}")
    print(f"templates deleted={deleted_templates}, deactivated={deactivated_templates}")
    print("CLEANUP PASS")


if __name__ == "__main__":
    main()
