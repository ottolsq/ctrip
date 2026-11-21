#!/usr/bin/env python3
"""统一运行 P0~P1.4 全部 API E2E。

用法：
    python -I ctrip-backend/scripts/run_all_e2e.py
    BASE_URL=http://localhost:8088/api/v1 python -I ctrip-backend/scripts/run_all_e2e.py

登录/注册限流（429）时等待 65 秒并最多重跑当前脚本 2 次。
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
]
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080/api/v1")
MAX_RETRIES = 2
RETRY_WAIT_SECONDS = 65


def run_script(script):
    path = os.path.join(ROOT, "ctrip-backend", "scripts", script)
    env = {**os.environ, "BASE_URL": BASE_URL, "PYTHONUNBUFFERED": "1"}
    print(f"\n===== {script} =====", flush=True)
    lines = []
    process = subprocess.Popen(
        [sys.executable, "-I", path],
        cwd=ROOT,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    for line in process.stdout:
        print("  " + line, end="", flush=True)
        lines.append(line)
    return process.wait(), "".join(lines)


def main():
    print(f"BASE_URL={BASE_URL}")
    started = time.time()
    for script in SCRIPTS:
        for attempt in range(MAX_RETRIES + 1):
            script_started = time.time()
            code, output = run_script(script)
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
