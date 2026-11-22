#!/usr/bin/env python3
"""P0 用户端 API 级端到端验证。

前置条件：
- 后端已在 http://localhost:8080 运行；
- MySQL/Redis/RabbitMQ 已启动，数据库已导入 schema 与示例数据。

运行：
    python -I ctrip-backend/scripts/e2e_p0.py

可通过 BASE_URL 覆盖后端地址，例如：
    BASE_URL=http://localhost:8080/api/v1 python -I scripts/e2e_p0.py
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


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=80) as response:
            code = response.status
            payload = json.loads(response.read().decode())
    except urllib.error.HTTPError as exc:
        code = exc.code
        raw = exc.read().decode()
        payload = json.loads(raw) if raw else {}
    print(f"{method} {path} -> {code}")
    if code not in (200, 201) or not payload.get("success"):
        print("RESPONSE:", json.dumps(payload, ensure_ascii=False)[:1200])
        raise SystemExit(1)
    return payload["data"]


def main():
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    email = f"e2e_{suffix}@example.com"
    token_data = call(
        "POST", "/auth/register/email",
        {"email": email, "password": "Test12345", "username": f"e2e{suffix}"})
    token = token_data["accessToken"]
    print("AUTH OK", email)

    guides = call("GET", "/guides?page=1&limit=5", token=token)
    assert guides["records"], "no guides"
    guide_id = guides["records"][0]["id"]
    assert call("GET", f"/collections/status?targetType=GUIDE&targetId={guide_id}",
                token=token)["collected"] is False
    call("POST", f"/guides/{guide_id}/collect", token=token)
    assert call("GET", f"/collections/status?targetType=GUIDE&targetId={guide_id}",
                token=token)["collected"] is True
    collections = call("GET", "/collections?page=1&size=10&type=GUIDE", token=token)
    assert any(item["contentType"] == "GUIDE" and item["contentId"] == guide_id
               for item in collections["records"]), collections
    call("DELETE", f"/guides/{guide_id}/collect", token=token)
    assert call("GET", f"/collections/status?targetType=GUIDE&targetId={guide_id}",
                token=token)["collected"] is False
    print("COLLECTION GUIDE OK")

    today = datetime.date.today()
    itinerary = call("POST", "/itineraries", {
        "title": "E2E 行程",
        "destinationId": 1,
        "startDate": str(today + datetime.timedelta(days=1)),
        "endDate": str(today + datetime.timedelta(days=3)),
    }, token=token)
    itinerary_id = itinerary["id"]
    assert itinerary["status"] == "DRAFT" and itinerary["isOwner"] is True
    assert call("PUT", f"/itineraries/{itinerary_id}", {"status": "PUBLISHED"},
                token=token)["status"] == "PUBLISHED"
    share_code = call("POST", f"/itineraries/{itinerary_id}/share", token=token)
    assert isinstance(share_code, str) and len(share_code) == 6, share_code
    shared = call("GET", f"/itineraries/share/{share_code}")
    assert shared["isOwner"] is False, shared
    assert "userId" not in shared, shared.keys()
    assert isinstance(shared.get("days"), list)
    assert call("PUT", f"/itineraries/{itinerary_id}", {"status": "ARCHIVED"},
                token=token)["status"] == "ARCHIVED"
    assert call("PUT", f"/itineraries/{itinerary_id}", {"status": "PUBLISHED"},
                token=token)["status"] == "PUBLISHED"
    call("POST", f"/itineraries/{itinerary_id}/collection?add=true", token=token)
    itinerary_collections = call(
        "GET", "/collections?page=1&size=10&type=ITINERARY", token=token)
    assert any(item["contentType"] == "ITINERARY" and item["contentId"] == itinerary_id
               for item in itinerary_collections["records"]), itinerary_collections
    call("DELETE", f"/collections/ITINERARY/{itinerary_id}", token=token)
    print("ITINERARY SHARE/ARCHIVE/COLLECT OK, code=", share_code)

    templates = call("GET", "/blind-box?page=1&limit=20", token=token)
    daily = [item for item in templates["records"] if item["type"] == "DAILY"]
    assert daily, templates
    template_id = daily[0]["id"]
    order = call("POST", "/blind-box/orders", {
        "templateId": template_id,
        "departureCity": "杭州",
        "budgetLevel": "STANDARD",
        "theme": "美食",
    }, token=token)
    order_no = order["orderNo"]
    call("POST", "/payments/blind-box/callback",
         {"orderNo": order_no, "payMethod": "ALIPAY"}, token=token)
    opened = call("POST", f"/blind-box/orders/{order_no}/open", token=token)
    assert opened["status"] in ("PROCESSING", "OPENED"), opened
    final = None
    for _ in range(45):
        time.sleep(2)
        final = call("GET", f"/blind-box/orders/{order_no}", token=token)
        if final["status"] == "OPENED":
            break
    assert final and final["status"] == "OPENED", final
    result = call("GET", f"/blind-box/orders/{order_no}/result", token=token)
    assert result, result
    print("BLINDBOX OPEN OK, order=", order_no,
          "result_keys=", list(result.keys())[:8])

    imported = call(
        "POST", f"/blind-box/orders/{order_no}/import-itinerary", token=token)
    assert imported and imported.get("days"), imported
    itinerary_id = imported["id"]
    again = call(
        "POST", f"/blind-box/orders/{order_no}/import-itinerary", token=token)
    assert again["id"] == itinerary_id, (again, imported)
    itinerary = call("GET", f"/itineraries/{itinerary_id}", token=token)
    assert itinerary.get("days"), itinerary
    assert any(day.get("items") for day in itinerary["days"]), itinerary
    print("BLINDBOX IMPORT OK, itinerary=", itinerary_id,
          "days=", len(itinerary["days"]))
    print("E2E PASS")


if __name__ == "__main__":
    sys.exit(main())
