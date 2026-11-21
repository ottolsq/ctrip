#!/usr/bin/env python3
"""P1.3 盲盒后台管理 API 级端到端验证。

前置条件：
- 后端已启动（默认 http://localhost:8080）；
- 已执行 P1 迁移与 admin 种子账号 SQL；
- MySQL / Redis / RabbitMQ 正常运行（退款库存恢复经由 MQ 异步完成）。

运行：
    python -I ctrip-backend/scripts/e2e_admin_p13.py

覆盖：
- DAILY 模板库存强制 -1；LIMITED 模板活动窗口校验；
- 非法 LIMITED 模板（缺窗口/库存 0）被拒绝；
- 窗口内下单库存 5→4；窗口外购买被拒绝；
- 管理端订单列表/详情；退款后库存回到 5（仅恢复一次）；
- 重复退款、删除有订单模板被拒绝；
- 普通用户访问管理端 403。
"""

import datetime
import json
import os
import random
import string
import sys
import time
import urllib.error
import urllib.parse
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


def login(credential, password):
    data, _ = call("POST", "/auth/login", {"credential": credential, "password": password})
    return data["accessToken"]


def local_dt(delta_days):
    return (datetime.datetime.now() + datetime.timedelta(days=delta_days)).strftime(LOCAL_DT)


def rule_config(start_delta, end_delta):
    return json.dumps({
        "activityStartTime": local_dt(start_delta),
        "activityEndTime": local_dt(end_delta)
    })


def create_template(admin_token, name, type_, stock, rule=None, price=88.00):
    body = {
        "name": name,
        "type": type_,
        "price": price,
        "stock": stock,
        "status": "ACTIVE"
    }
    if rule is not None:
        body["ruleConfig"] = rule
    template, _ = call("POST", "/admin/blind-box/templates", body,
                       token=admin_token, expect=201)
    return template


def get_template(template_id):
    template, _ = call("GET", f"/blind-box/{template_id}")
    return template


def wait_stock(template_id, expected, timeout=40):
    deadline = time.time() + timeout
    stock = None
    while time.time() < deadline:
        stock = get_template(template_id)["stock"]
        if stock == expected:
            break
        time.sleep(2)
    assert stock == expected, f"expected stock {expected}, got {stock}"
    return stock


def main():
    admin_token = login("admin@example.com", "Admin12345")
    _, user_token, username = register("bbx")
    suffix = username[-6:]

    operator_token = login("operator@example.com", "Operator12345")
    call("GET", "/admin/blind-box/templates", token=operator_token, expect=403)
    call("GET", "/admin/blind-box/orders", token=operator_token, expect=403)

    daily = create_template(
        admin_token, f"E2E日常盲盒{suffix}", "DAILY", 999)
    assert daily["stock"] == -1, daily
    assert isinstance(daily["createdAt"], str), daily["createdAt"]

    # 模拟前端编辑 DAILY：payload 仍带 stock=-1，不得报 400
    daily_updated, _ = call("PUT", f"/admin/blind-box/templates/{daily['id']}", {
        "name": f"E2E日常盲盒{suffix}改名",
        "type": "DAILY",
        "price": 66.00,
        "stock": -1,
        "status": "ACTIVE"
    }, token=admin_token)
    assert daily_updated["stock"] == -1, daily_updated

    limited = create_template(
        admin_token, f"E2E限定盲盒{suffix}", "LIMITED", 5,
        rule=rule_config(-1, 1))
    limited_id = limited["id"]
    assert limited["stock"] == 5, limited

    call("POST", "/admin/blind-box/templates", {
        "name": f"E2E无窗口{suffix}", "type": "LIMITED", "price": 88,
        "stock": 5, "status": "ACTIVE"
    }, token=admin_token, expect=400)
    call("POST", "/admin/blind-box/templates", {
        "name": f"E2E零库存{suffix}", "type": "LIMITED", "price": 88,
        "stock": 0, "status": "ACTIVE", "ruleConfig": rule_config(-1, 1)
    }, token=admin_token, expect=400)

    assert get_template(limited_id)["stock"] == 5

    order, _ = call("POST", "/blind-box/orders", {
        "templateId": limited_id,
        "departureCity": "杭州",
        "budgetLevel": "STANDARD",
        "theme": "美食"
    }, token=user_token, expect=201)
    order_no = order["orderNo"]
    assert get_template(limited_id)["stock"] == 4

    # 编辑库存未变化时只提交名称，不得用陈旧库存覆盖并发扣减
    limited_updated, _ = call("PUT", f"/admin/blind-box/templates/{limited_id}", {
        "name": f"E2E限定盲盒{suffix}改名"
    }, token=admin_token)
    assert limited_updated["stock"] == 4, limited_updated

    expired = create_template(
        admin_token, f"E2E过期盲盒{suffix}", "LIMITED", 2,
        rule=rule_config(-3, -2))
    call("POST", "/blind-box/orders", {
        "templateId": expired["id"],
        "departureCity": "杭州",
        "budgetLevel": "STANDARD"
    }, token=user_token, expect=400)
    assert get_template(expired["id"])["stock"] == 2

    future = create_template(
        admin_token, f"E2E未开始盲盒{suffix}", "LIMITED", 1,
        rule=rule_config(2, 3))
    call("POST", "/blind-box/orders", {
        "templateId": future["id"],
        "departureCity": "杭州",
        "budgetLevel": "STANDARD"
    }, token=user_token, expect=400)
    assert get_template(future["id"])["stock"] == 1

    call("POST", "/payments/blind-box/callback",
         {"orderNo": order_no, "payMethod": "ALIPAY"})

    orders, _ = call(
        "GET", f"/admin/blind-box/orders?page=1&size=10&orderNo={order_no}",
        token=admin_token)
    listed = next(item for item in orders["records"] if item["orderNo"] == order_no)
    assert listed["userName"] and listed["templateName"], listed
    assert isinstance(listed["createdAt"], str), listed["createdAt"]

    detail, _ = call("GET", f"/admin/blind-box/orders/{order_no}", token=admin_token)
    assert detail["order"]["status"] == "PAID", detail
    assert detail["result"] is None, detail

    call("POST", f"/admin/blind-box/orders/{order_no}/retry-open",
         token=admin_token, expect=400)

    # 开盒后按 OPENED 退款，详情保留结果
    call("POST", f"/blind-box/orders/{order_no}/open", token=user_token)
    opened = None
    for _ in range(45):
        time.sleep(2)
        opened, _ = call("GET", f"/blind-box/orders/{order_no}", token=user_token)
        if opened["status"] == "OPENED":
            break
    assert opened and opened["status"] == "OPENED", opened

    reason = "E2E 退款规定原因"
    call("POST", f"/admin/blind-box/orders/{order_no}/refund",
         {"reason": reason}, token=admin_token)

    detail, _ = call("GET", f"/admin/blind-box/orders/{order_no}", token=admin_token)
    assert detail["order"]["status"] == "REFUNDED", detail
    assert detail["order"]["refundReason"] == reason, detail
    assert detail["order"]["refundedAt"], detail
    assert detail["result"] is not None and detail["result"]["destination"], detail

    wait_stock(limited_id, 5)
    call("POST", f"/admin/blind-box/orders/{order_no}/refund",
         {"reason": reason}, token=admin_token, expect=400)
    wait_stock(limited_id, 5)

    call("DELETE", f"/admin/blind-box/templates/{limited_id}",
         token=admin_token, expect=400)

    call("GET", "/admin/blind-box/orders", token=user_token, expect=403)

    print("E2E ADMIN P1.3 PASS")


if __name__ == "__main__":
    sys.exit(main())
