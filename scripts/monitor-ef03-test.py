#!/usr/bin/env python3
"""Read an EF03 test run's retained SQLite database without changing it."""
import argparse
import json
import sqlite3
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run", nargs="?", type=Path, help="run directory; defaults to latest run")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    runs = list((root / ".local/ef03-tests").glob("ef03_test_*/run.json"))
    folder = args.run or (max(runs, key=lambda p: p.stat().st_mtime).parent if runs else None)
    if folder is None:
        parser.error("No test run exists yet")
    print(f"Run: {folder}")
    run_file = folder / "run.json"
    if run_file.exists():
        run = json.loads(run_file.read_text())
        print(f"Camunda process: {run['processId']} | planned rows: {run['rows']}")
    database = folder / "test.sqlite"
    with sqlite3.connect(database.resolve().as_uri() + "?mode=ro", uri=True, timeout=10) as db:
        print("\nReceipt status:")
        for status, count in db.execute("select processing_status, count(*) from debt_paper_push_record group by processing_status"):
            print(f"  {status}: {count}")
        print("\nLast 10 requests (reference | receipt status | paper ID | Camunda instance):")
        for row in db.execute("select reference_id, processing_status, execution_paper_id, process_instance_key from debt_paper_push_record order by id desc limit 10"):
            print("  " + " | ".join(str(value) if value is not None else "-" for value in row))
        print("\nRecent failures:")
        for reference, reason in db.execute("select reference_id, failure_reason from debt_paper_push_record where failure_reason is not null order by id desc limit 5"):
            print(f"  {reference}: {reason}")
    summary = folder / "summary.json"
    print("\n" + (summary.read_text() if summary.exists() else "Final summary is not available yet; inspect results.jsonl and the run log."))


if __name__ == "__main__":
    main()
