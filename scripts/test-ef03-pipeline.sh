#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
limit="${1:-1}"
if [[ ! "$limit" =~ ^[1-9][0-9]*$ ]]; then
  echo 'Usage: bash scripts/test-ef03-pipeline.sh [positive row count; default 1]' >&2
  exit 2
fi
umask 077
mkdir -p .local/ef03-tests/logs
run_log=$(mktemp .local/ef03-tests/logs/run-XXXXXX.log)
printf 'Test log: %s\n' "$run_log"
mvn -o -Dskip.installnodenpm -Dskip.npm -Dtest=Ef03PipelineLiveTest \
  -Def03.live=true -Def03.limit="$limit" test 2>&1 | tee "$run_log"
