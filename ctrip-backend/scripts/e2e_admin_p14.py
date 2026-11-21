#!/usr/bin/env python3
"""P1.4 运营内容与平台数据概览 API 级端到端验证。

前置条件：
- 后端已启动（默认 http://localhost:8080）；
- 已执行 P1 迁移与 admin/operator 种子账号 SQL；
- 已导入目的地示例数据（推荐位使用 destination id=1）。

运行：
    python -I ctrip-backend/scripts/e2e_admin_p14.py

覆盖：
- Banner 管理端 CRUD、公开接口时间窗与启停过滤；
- 推荐位管理端 CRUD、公开接口目标装配、未发布攻略跳过；
- Dashboard 聚合字段与 7 天序列；
- 管理端 ADMIN 权限、公开接口免登录。
"""

import datetime
import json
import os
import random
import string
import sys
import time
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")
LOCAL_DT = "%Y-%m-%dT%H:%M:%S"


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


def login(credential, password):
    data, _ = call("POST", "/auth/login", {"credential": credential, "password": password})
    return data["accessToken"]


def register(prefix):
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"{prefix}_{suffix}@example.com"
    username = f"{prefix}{suffix}"
    for attempt in range(2):
        try:
            token_data, _ = call("POST", "/auth/register/email", {
                "email": email,
                "password": "Test12345",
                "username": username
            }, expect=201)
            return email, token_data["accessToken"], username
        except SystemExit as exc:
            if attempt == 0 and "429" in str(exc):
                print("注册限流，等待 65 秒后重试...")
                time.sleep(65)
                continue
            raise


def local_dt(delta_hours):
    return (datetime.datetime.now() + datetime.timedelta(hours=delta_hours)).strftime(LOCAL_DT)


def banner_payload(title, start_at, end_at, status="ENABLED"):
    return {
        "title": title,
        "imageUrl": "https://img.example.com/e2e-banner.jpg",
        "linkUrl": "/destinations",
        "sortOrder": 1,
        "status": status,
        "startAt": start_at,
        "endAt": end_at
    }


def recommendation_payload(title, target_type, target_id, status="ENABLED"):
    return {
        "slot": "home_featured",
        "title": title,
        "targetType": target_type,
        "targetId": target_id,
        "sortOrder": 1,
        "status": status,
        "startAt": None,
        "endAt": None
    }


def public_ids(path):
    data, _ = call("GET", path)
    return {item["id"] for item in data}


def main():
    admin_token = login("admin@example.com", "Admin12345")
    operator_token = login("operator@example.com", "Operator12345")

    call("GET", "/admin/banners", token=operator_token, expect=403)
    call("GET", "/admin/recommendations", token=operator_token, expect=403)
    call("GET", "/admin/dashboard", token=operator_token, expect=403)

    _, user_token, username = register("ops")
    suffix = username[-6:]

    # 普通用户同样不得访问管理端
    call("GET", "/admin/banners", token=user_token, expect=403)
    call("GET", "/admin/recommendations", token=user_token, expect=403)
    call("GET", "/admin/dashboard", token=user_token, expect=403)

    current, _ = call("POST", "/admin/banners", banner_payload(
        f"E2E当前轮播{suffix}", local_dt(-1), local_dt(1)), token=admin_token, expect=201)
    future, _ = call("POST", "/admin/banners", banner_payload(
        f"E2E未来轮播{suffix}", local_dt(24), local_dt(48)), token=admin_token, expect=201)

    banner_list, _ = call("GET", "/banners")
    assert isinstance(banner_list, list), banner_list
    ids = {item["id"] for item in banner_list}
    assert current["id"] in ids, ids
    assert future["id"] not in ids, ids

    # 未使用的 slot 必须返回空数组（首页隐藏推荐区的依据）
    empty_recs, _ = call("GET", f"/recommendations?slot=p1_4_empty_{suffix}")
    assert empty_recs == [], empty_recs

    call("PUT", f"/admin/banners/{current['id']}", banner_payload(
        f"E2E当前轮播{suffix}", local_dt(-1), local_dt(1), status="DISABLED"),
        token=admin_token)
    assert current["id"] not in public_ids("/banners")

    call("DELETE", f"/admin/banners/{current['id']}", token=admin_token)
    call("DELETE", f"/admin/banners/{future['id']}", token=admin_token)
    assert current["id"] not in public_ids("/banners")

    destination_rec, _ = call("POST", "/admin/recommendations", recommendation_payload(
        f"E2E目的地推荐{suffix}", "DESTINATION", 1), token=admin_token, expect=201)
    public_recs, _ = call("GET", "/recommendations?slot=home_featured")
    destination_item = next(
        (item for item in public_recs if item["id"] == destination_rec["id"]), None)
    assert destination_item, public_recs
    assert destination_item["targetTitle"], destination_item
    assert destination_item["linkUrl"] == "/destinations/1", destination_item
    call("DELETE", f"/admin/recommendations/{destination_rec['id']}", token=admin_token)

    tag = f"{suffix}{''.join(random.choices(string.ascii_lowercase, k=4))}"
    guide, _ = call("POST", "/guides", {
        "title": f"E2E已发布攻略{tag}",
        "content": "<p>P1.4 推荐位测试</p>",
        "destinationId": 1
    }, token=user_token, expect=201)
    call("POST", f"/guides/{guide['id']}/submit", token=user_token)
    call("POST", f"/admin/guides/{guide['id']}/approve", token=admin_token)

    pending_guide, _ = call("POST", "/guides", {
        "title": f"E2E待审攻略{tag}",
        "content": "<p>未发布，不应出现在推荐</p>",
        "destinationId": 1
    }, token=user_token, expect=201)
    call("POST", f"/guides/{pending_guide['id']}/submit", token=user_token)

    published_rec, _ = call("POST", "/admin/recommendations", recommendation_payload(
        f"E2E攻略推荐{tag}", "GUIDE", guide["id"]), token=admin_token, expect=201)
    pending_rec, _ = call("POST", "/admin/recommendations", recommendation_payload(
        f"E2E待审推荐{tag}", "GUIDE", pending_guide["id"]), token=admin_token, expect=201)

    public_recs, _ = call("GET", "/recommendations?slot=home_featured")
    ids = {item["id"] for item in public_recs}
    assert published_rec["id"] in ids, public_recs
    assert pending_rec["id"] not in ids, public_recs

    call("DELETE", f"/admin/recommendations/{published_rec['id']}", token=admin_token)
    call("DELETE", f"/admin/recommendations/{pending_rec['id']}", token=admin_token)

    dashboard, _ = call("GET", "/admin/dashboard", token=admin_token)
    for field in ("totalUsers", "totalGuides", "totalOrders", "totalSales",
                  "bannerCount", "recommendationCount"):
        assert field in dashboard, dashboard
    assert len(dashboard["last7Days"]) == 7, dashboard
    assert float(dashboard["totalSales"]) >= 0, dashboard

    # 未登录的公开接口应可读
    call("GET", "/banners")
    call("GET", "/recommendations?slot=home_featured")

    print("E2E ADMIN P1.4 PASS")


if __name__ == "__main__":
    sys.exit(main())
