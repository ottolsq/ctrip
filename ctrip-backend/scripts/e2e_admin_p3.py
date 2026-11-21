#!/usr/bin/env python3
"""P3 合规能力 API 级端到端验证。

前置条件：
- 后端已启动；
- 已执行 docs/SQL/ctrip_p3_compliance.sql；
- admin 种子账号可用。

运行：
    python -I ctrip-backend/scripts/e2e_admin_p3.py

覆盖：
- 概率公示 70/25/5、合计 100、保底 10、版本 v1；
- 开盒后用户抽取记录含档位/目的地/版本；
- 管理员按用户检索记录；
- 普通用户访问管理员记录 403。
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


def login(credential, password):
    data = call("POST", "/auth/login", {"credential": credential, "password": password})
    return data["accessToken"]


def register(prefix):
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"{prefix}_{suffix}@example.com"
    username = f"{prefix}{suffix}"
    for attempt in range(2):
        try:
            data = call("POST", "/auth/register/email", {
                "email": email, "password": "Test12345", "username": username
            }, expect=201)
            return data["accessToken"], username
        except SystemExit as exc:
            if attempt == 0 and "429" in str(exc):
                print("注册限流，等待 65 秒后重试...")
                time.sleep(65)
                continue
            raise


def local_dt(hours):
    return (datetime.datetime.now() + datetime.timedelta(hours=hours)).strftime(LOCAL_DT)


def main():
    admin_token = login("admin@example.com", "Admin12345")
    user_token, username = register("p3")

    template = call("POST", "/admin/blind-box/templates", {
        "name": f"P3合规盲盒{username[-6:]}",
        "type": "DAILY",
        "price": 88.00,
        "stock": -1,
        "status": "ACTIVE"
    }, token=admin_token, expect=201)
    template_id = template["id"]

    probabilities = call("GET", f"/blind-box/{template_id}/probabilities")
    tiers = probabilities["tiers"]
    assert len(tiers) == 3, probabilities
    percentages = {item["tier"]: float(item["probability"]) for item in tiers}
    assert percentages == {"COMMON": 70.0, "RARE": 25.0, "LEGENDARY": 5.0}, percentages
    assert abs(sum(percentages.values()) - 100.0) < 0.001
    assert probabilities["guaranteeCount"] == 10, probabilities
    assert probabilities["probabilityVersion"] == "v1", probabilities
    assert all(item["destinations"] for item in tiers), tiers

    order = call("POST", "/blind-box/orders", {
        "templateId": template_id,
        "departureCity": "杭州",
        "budgetLevel": "STANDARD",
        "theme": "美食"
    }, token=user_token, expect=201)
    order_no = order["orderNo"]

    call("POST", "/payments/blind-box/callback",
         {"orderNo": order_no, "payMethod": "ALIPAY"})
    call("POST", f"/blind-box/orders/{order_no}/open", token=user_token)

    final = None
    for _ in range(45):
        time.sleep(2)
        final = call("GET", f"/blind-box/orders/{order_no}", token=user_token)
        if final["status"] == "OPENED":
            break
    assert final and final["status"] == "OPENED", final

    mine = call("GET", "/blind-box/draw-records?page=1&size=20", token=user_token)
    record = next(
        (item for item in mine["records"] if item["orderNo"] == order_no), None)
    assert record, mine
    assert record["tier"] in ("COMMON", "RARE", "LEGENDARY"), record
    assert record["destination"], record
    assert record["probabilityVersion"] == "v1", record

    admin_records = call(
        "GET", f"/admin/blind-box/draw-records?page=1&size=20&userId={record['userId']}",
        token=admin_token)
    assert any(item["orderNo"] == order_no for item in admin_records["records"]), admin_records
    admin_item = next(item for item in admin_records["records"] if item["orderNo"] == order_no)
    assert admin_item["userName"] and admin_item["templateName"], admin_item

    call("GET", "/admin/blind-box/draw-records", token=user_token, expect=403)
    call("PUT", f"/admin/blind-box/templates/{template_id}",
         {"status": "INACTIVE"}, token=admin_token)

    print("E2E ADMIN P3 PASS")


if __name__ == "__main__":
    sys.exit(main())
