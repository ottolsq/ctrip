#!/usr/bin/env python3
"""限定盲盒秒杀 API 级端到端验证。

流程：管理员创建限定模板 → 用户 A 抢购并异步建单（PAID）→ 重复抢购拒绝
→ 用户 B 抢最后一单 → 用户 C 售罄 → 用户 A 开盒成功（验证偏好已保存）。

运行：
    python -I ctrip-backend/scripts/e2e_flash_sale.py
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
    return payload.get("data")


def call_optional(method, path, token=None):
    url = BASE + path
    req = urllib.request.Request(url, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=40) as response:
            return response.status, json.loads(response.read().decode())
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        return exc.code, json.loads(raw) if raw else {}


def login(credential, password):
    return call("POST", "/auth/login", {
        "credential": credential, "password": password
    })["accessToken"]


def register(prefix):
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"{prefix}_{suffix}@example.com"
    username = f"{prefix}{suffix}"
    for attempt in range(2):
        try:
            data = call("POST", "/auth/register/email", {
                "email": email, "password": "Test12345", "username": username
            }, expect=201)
            return data["accessToken"]
        except SystemExit as exc:
            if attempt == 0 and "429" in str(exc):
                print("注册限流，等待 65 秒后重试...")
                time.sleep(65)
                continue
            raise


def local_dt(hours):
    return (datetime.datetime.now() + datetime.timedelta(hours=hours)).strftime(LOCAL_DT)


def flash(token, template_id):
    return call("POST", "/blind-box/flash-sale", {
        "templateId": template_id,
        "departureCity": "杭州",
        "budgetLevel": "STANDARD",
        "theme": "美食"
    }, token=token)


def wait_order(token, order_no, expect_status="PAID", timeout=20):
    deadline = time.time() + timeout
    order = None
    while time.time() < deadline:
        code, payload = call_optional(
            "GET", f"/blind-box/orders/{order_no}", token)
        if code == 200:
            order = payload.get("data")
            if order and order.get("status") == expect_status:
                return order
        time.sleep(0.5)
    raise SystemExit(f"order {order_no} not {expect_status}: {order}")


def main():
    admin = login("admin@example.com", "Admin12345")
    suffix = ''.join(random.choices(string.ascii_lowercase, k=4))
    template = call("POST", "/admin/blind-box/templates", {
        "name": f"秒杀盲盒{suffix}",
        "type": "LIMITED",
        "price": 88.00,
        "stock": 2,
        "status": "ACTIVE",
        "ruleConfig": json.dumps({
            "activityStartTime": local_dt(-1),
            "activityEndTime": local_dt(2)
        })
    }, token=admin, expect=201)
    template_id = template["id"]

    user1 = register("flash1")
    user2 = register("flash2")
    user3 = register("flash3")

    first = flash(user1, template_id)
    assert first["success"] and first["status"] == "QUEUED", first
    order1 = wait_order(user1, first["orderNo"])
    admin_detail = call(
        "GET", f"/admin/blind-box/orders/{first['orderNo']}", token=admin)
    assert admin_detail["order"]["payMethod"] == "FLASH_SALE", admin_detail

    duplicate = flash(user1, template_id)
    assert not duplicate["success"] and duplicate["status"] == "DUPLICATED", duplicate

    second = flash(user2, template_id)
    assert second["success"], second
    wait_order(user2, second["orderNo"])

    sold_out = flash(user3, template_id)
    assert not sold_out["success"] and sold_out["status"] == "SOLD_OUT", sold_out

    call("POST", f"/blind-box/orders/{first['orderNo']}/open", token=user1)
    opened = None
    for _ in range(45):
        time.sleep(2)
        code, payload = call_optional(
            "GET", f"/blind-box/orders/{first['orderNo']}", user1)
        if code == 200 and payload.get("data", {}).get("status") == "OPENED":
            opened = payload["data"]
            break
    assert opened and opened["status"] == "OPENED", opened

    call("PUT", f"/admin/blind-box/templates/{template_id}",
         {"status": "INACTIVE"}, token=admin)
    print("E2E FLASH SALE PASS")


if __name__ == "__main__":
    sys.exit(main())
