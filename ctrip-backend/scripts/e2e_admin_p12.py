#!/usr/bin/env python3
"""P1.2 内容运营 API 级端到端验证。

前置条件：
- 后端已启动（默认 http://localhost:8080）；
- 已执行 P1 迁移与 admin/operator 种子账号 SQL。

运行：
    python -I ctrip-backend/scripts/e2e_admin_p12.py

覆盖：
- 作者创建攻略 → 提交审核 → 公共不可见；
- 运营驳回（带理由）→ 作者可见驳回理由；
- 作者修改后重新提交 → 运营通过 → 公共可见；
- 编辑已发布攻略 → 回到审核中且公共不可见；
- 评论发表 → 运营检索/删除；
- 内容数据统计；
- 普通用户访问管理端 403。
"""

import json
import os
import random
import string
import sys
import urllib.error
import urllib.parse
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
        with urllib.request.urlopen(req, timeout=40) as response:
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


def register(prefix):
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"{prefix}_{suffix}@example.com"
    token_data, _ = call("POST", "/auth/register/email", {
        "email": email,
        "password": "Test12345",
        "username": f"{prefix}{suffix}"
    }, expect=201)
    return email, token_data["accessToken"], f"{prefix}{suffix}"


def login(credential, password):
    data, _ = call("POST", "/auth/login", {"credential": credential, "password": password})
    return data["accessToken"]


def find_mine(token, guide_id):
    mine, _ = call("GET", "/guides/mine?page=1&size=100", token=token)
    return next((item for item in mine["records"] if item["id"] == guide_id), None)


def main():
    _, author_token, author_name = register("author")
    title = f"E2E审核攻略{author_name[-6:]}"

    guide, _ = call("POST", "/guides", {
        "title": title,
        "content": "<p>初始内容</p>",
        "destinationId": 1
    }, token=author_token, expect=201)
    guide_id = guide["id"]
    assert guide["status"] == "DRAFT", guide

    submitted, _ = call("POST", f"/guides/{guide_id}/submit", token=author_token)
    assert submitted["status"] == "PENDING", submitted
    call("GET", f"/guides/{guide_id}", token=author_token, expect=404)

    operator_token = login("operator@example.com", "Operator12345")
    pending, _ = call(
        "GET", f"/admin/guides?page=1&size=10&status=PENDING&keyword={urllib.parse.quote(title)}",
        token=operator_token)
    assert any(item["id"] == guide_id for item in pending["records"]), pending

    reason = "内容不完整，请补充交通与住宿信息"
    call("POST", f"/admin/guides/{guide_id}/reject", {"reason": reason}, token=operator_token)
    mine = find_mine(author_token, guide_id)
    assert mine and mine["status"] == "REJECTED", mine
    assert mine["rejectReason"] == reason, mine

    updated, _ = call("PUT", f"/guides/{guide_id}", {
        "title": title,
        "content": "<p>补充后的完整内容</p>"
    }, token=author_token)
    assert updated["status"] == "DRAFT", updated
    call("POST", f"/guides/{guide_id}/submit", token=author_token)
    call("POST", f"/admin/guides/{guide_id}/approve", token=operator_token)

    public_guide, _ = call("GET", f"/guides/{guide_id}", token=author_token)
    assert public_guide["status"] == "PUBLISHED", public_guide
    assert find_mine(author_token, guide_id)["status"] == "PUBLISHED"

    editing, _ = call("PUT", f"/guides/{guide_id}", {
        "content": "<p>已发布后的修改内容</p>"
    }, token=author_token)
    assert editing["status"] == "PENDING", editing
    call("GET", f"/guides/{guide_id}", token=author_token, expect=404)
    call("POST", f"/admin/guides/{guide_id}/approve", token=operator_token)
    call("GET", f"/guides/{guide_id}", token=author_token)

    _, user_token, _ = register("commenter")
    comment, _ = call("POST", f"/guides/{guide_id}/comments", {
        "content": "这是一条 E2E 测试评论"
    }, token=user_token)
    comment_id = comment["id"]

    comments, _ = call(
        "GET", f"/admin/comments?page=1&size=10&guideId={guide_id}",
        token=operator_token)
    assert any(item["id"] == comment_id for item in comments["records"]), comments
    call("DELETE", f"/admin/comments/{comment_id}", token=operator_token)
    after, _ = call(
        "GET", f"/admin/comments?page=1&size=10&guideId={guide_id}",
        token=operator_token)
    assert all(item["id"] != comment_id for item in after["records"]), after

    stats, _ = call("GET", "/admin/content/stats", token=operator_token)
    assert "totalGuides" in stats and "topGuides" in stats, stats
    assert len(stats["last7Days"]) == 7, stats

    call("GET", "/admin/guides", token=author_token, expect=403)

    print("E2E ADMIN P1.2 PASS")


if __name__ == "__main__":
    sys.exit(main())
