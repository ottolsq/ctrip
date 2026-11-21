#!/usr/bin/env python3
"""LIMITED 盲盒库存并发压测。

环境变量：
    BASE_URL     默认 http://localhost:8080/api/v1
    STOCK        默认 10
    CONCURRENCY  默认 20（2 * STOCK）

断言：并发下单成功数 == STOCK，剩余库存 == 0，失败请求均为 400。
"""

import concurrent.futures
import datetime
import json
import os
import random
import string
import sys
import threading
import time
import urllib.error
import urllib.request

BASE = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")
STOCK = int(os.environ.get("STOCK", "10"))
CONCURRENCY = int(os.environ.get("CONCURRENCY", str(STOCK * 2)))
LOCAL_DT = "%Y-%m-%dT%H:%M:%S"


def raw_call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=40) as response:
            code = response.status
            raw = response.read().decode()
    except urllib.error.HTTPError as exc:
        code = exc.code
        raw = exc.read().decode()
    except Exception as exc:  # noqa: BLE001 - 压测需要记录网络异常
        return 0, {"error": str(exc)}, time.perf_counter() - started
    payload = json.loads(raw) if raw else {}
    return code, payload, time.perf_counter() - started


def call(method, path, body=None, token=None, expect=200):
    for attempt in range(2):
        code, payload, _ = raw_call(method, path, body, token)
        if code == 429 and attempt == 0:
            print("429 限流，等待 65 秒后重试...")
            time.sleep(65)
            continue
        if code != expect:
            print(f"{method} {path} -> {code}: {json.dumps(payload, ensure_ascii=False)[:500]}")
            raise SystemExit(f"expected {expect}, got {code}")
        return payload.get("data")
    raise SystemExit("retry exhausted")


def register_with_retry():
    suffix = ''.join(random.choices(string.ascii_lowercase + string.digits, k=6))
    payload = {
        "email": f"stress_{suffix}@example.com",
        "password": "Test12345",
        "username": f"stress{suffix}"
    }
    data = call("POST", "/auth/register/email", payload, expect=201)
    return data["accessToken"]


def local_dt(hours):
    return (datetime.datetime.now() + datetime.timedelta(hours=hours)).strftime(LOCAL_DT)


def main():
    admin = call("POST", "/auth/login", {
        "credential": "admin@example.com", "password": "Admin12345"
    })["accessToken"]
    user_token = register_with_retry()

    name = f"压测限定盲盒{''.join(random.choices(string.ascii_lowercase, k=4))}"
    rule = json.dumps({
        "activityStartTime": local_dt(-1),
        "activityEndTime": local_dt(1)
    })
    template = call("POST", "/admin/blind-box/templates", {
        "name": name,
        "type": "LIMITED",
        "price": 99.00,
        "stock": STOCK,
        "status": "ACTIVE",
        "ruleConfig": rule
    }, token=admin, expect=201)
    template_id = template["id"]
    print(f"模板创建: id={template_id}, stock={template['stock']}, concurrency={CONCURRENCY}")

    barrier = threading.Barrier(CONCURRENCY)
    order_body = {
        "templateId": template_id,
        "departureCity": "杭州",
        "budgetLevel": "STANDARD",
        "theme": "美食"
    }

    def worker(_):
        barrier.wait()
        return raw_call("POST", "/blind-box/orders", order_body, user_token)

    wall_start = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENCY) as pool:
        results = list(pool.map(worker, range(CONCURRENCY)))
    wall = time.perf_counter() - wall_start

    success = sum(1 for code, _, _ in results if code == 201)
    failures = [(code, payload) for code, payload, _ in results if code != 201]
    durations = sorted(duration for _, _, duration in results)
    p95 = durations[max(0, int(len(durations) * 0.95) - 1)]
    rps = len(results) / wall if wall > 0 else 0

    stock_after = call("GET", f"/blind-box/{template_id}")["stock"]
    call("PUT", f"/admin/blind-box/templates/{template_id}",
         {"status": "INACTIVE"}, token=admin)

    print(f"requests={len(results)} success={success} failed={len(failures)}")
    print(f"wall={wall:.3f}s rps={rps:.1f} p95={p95 * 1000:.1f}ms stock_after={stock_after}")
    for code, payload in failures[:5]:
        print(f"  failure {code}: {json.dumps(payload, ensure_ascii=False)[:200]}")

    if success != STOCK:
        raise SystemExit(f"FAIL: expected {STOCK} successes, got {success}")
    if stock_after != 0:
        raise SystemExit(f"FAIL: expected stock 0, got {stock_after}")
    bad_codes = {code for code, _ in failures if code != 400}
    if bad_codes:
        raise SystemExit(f"FAIL: unexpected status codes {bad_codes}")

    print("STRESS PASS")


if __name__ == "__main__":
    sys.exit(main())
