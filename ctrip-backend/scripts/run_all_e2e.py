#!/usr/bin/env python3
"""统一运行 P0~P1.4 全部 API E2E。

用法：
    python -I ctrip-backend/scripts/run_all_e2e.py
    BASE_URL=http://localhost:8088/api/v1 python -I ctrip-backend/scripts/run_all_e2e.py

登录/注册限流（429）时等待 65 秒并最多重跑当前脚本 2 次。
每次运行限时 300 秒，超时终止子进程、报告已有输出并停止验收。
"""

import os
import subprocess
import sys
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SCRIPTS = [
    "e2e_p0.py",
    "e2e_admin_p11.py",
    "e2e_admin_p12.py",
    "e2e_admin_p13.py",
    "e2e_admin_p14.py",
    "e2e_admin_p3.py",
    "e2e_flash_sale.py",
]
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")
MAX_RETRIES = 2
RETRY_WAIT_SECONDS = 65
SCRIPT_TIMEOUT_SECONDS = 300


def print_script_output(output):
    for line in output.splitlines():
        print("  " + line, flush=True)


def run_script(script):
    path = os.path.join(ROOT, "ctrip-backend", "scripts", script)
    env = {**os.environ, "BASE_URL": BASE_URL}
    print(f"\n===== {script} =====", flush=True)
    result = subprocess.run(
        [sys.executable, "-I", "-u", "-X", "utf8", path],
        cwd=ROOT,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=SCRIPT_TIMEOUT_SECONDS,
    )
    output = result.stdout.decode("utf-8", errors="replace")
    print_script_output(output)
    return result.returncode, output


def main():
    print(f"BASE_URL={BASE_URL}")
    started = time.time()
    for script in SCRIPTS:
        for attempt in range(MAX_RETRIES + 1):
            script_started = time.time()
            try:
                code, output = run_script(script)
            except subprocess.TimeoutExpired as exc:
                print(f"\nTIMEOUT {script} ({SCRIPT_TIMEOUT_SECONDS}s)", flush=True)
                print_script_output((exc.stdout or b"").decode("utf-8", errors="replace"))
                return 1
            elapsed = time.time() - script_started
            if code == 0:
                print(f"PASS {script} ({elapsed:.1f}s)", flush=True)
                break
            if "429" in output and attempt < MAX_RETRIES:
                print(
                    f"429 detected in {script}, wait {RETRY_WAIT_SECONDS}s "
                    f"and retry ({attempt + 1}/{MAX_RETRIES})...",
                    flush=True,
                )
                time.sleep(RETRY_WAIT_SECONDS)
                continue
            print(f"\nFAIL {script} (exit {code})", flush=True)
            print("---- last output ----")
            print("\n".join(output.splitlines()[-40:]))
            return 1
    print(f"\nALL E2E PASS ({time.time() - started:.1f}s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
