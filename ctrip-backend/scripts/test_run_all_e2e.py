import contextlib
import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import run_all_e2e


class RunAllE2ETest(unittest.TestCase):
    def test_timeout_reports_partial_output_and_stops_without_retry(self):
        script = "slow_fixture.py"
        with tempfile.TemporaryDirectory() as root:
            scripts = Path(root, "ctrip-backend", "scripts")
            scripts.mkdir(parents=True)
            (scripts / script).write_text(
                "import time\nprint('429 fixture started', flush=True)\n"
                "time.sleep(2)\nprint('fixture finished')\n",
                encoding="utf-8",
            )
            output = io.StringIO()
            with (
                patch.object(run_all_e2e, "ROOT", root),
                patch.object(run_all_e2e, "SCRIPTS", [script, "next_fixture.py"]),
                patch.object(run_all_e2e, "SCRIPT_TIMEOUT_SECONDS", 1, create=True),
                patch.object(run_all_e2e.time, "sleep") as wait,
                contextlib.redirect_stdout(output),
            ):
                result = run_all_e2e.main()

        log = output.getvalue()
        self.assertEqual(result, 1, log)
        self.assertIn("TIMEOUT slow_fixture.py", log)
        self.assertIn("429 fixture started", log)
        self.assertNotIn("fixture finished", log)
        self.assertNotIn("next_fixture.py", log)
        wait.assert_not_called()

    def test_rate_limit_failure_still_retries(self):
        script = "rate_limit_fixture.py"
        with (
            patch.object(run_all_e2e, "SCRIPTS", [script]),
            patch.object(run_all_e2e, "run_script", side_effect=[(1, "429"), (0, "")]) as run,
            patch.object(run_all_e2e.time, "sleep") as wait,
            contextlib.redirect_stdout(io.StringIO()),
        ):
            self.assertEqual(run_all_e2e.main(), 0)

        self.assertEqual(run.call_count, 2)
        wait.assert_called_once_with(run_all_e2e.RETRY_WAIT_SECONDS)
