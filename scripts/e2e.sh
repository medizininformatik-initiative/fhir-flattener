#!/usr/bin/env bash
# End-to-end test of a built fhir-flattener image: starts the container and sends real HTTP requests to it.
# Usage: scripts/e2e.sh [IMAGE]   (default IMAGE: fhir-flattener:latest)
# Needs docker, curl and jq. PORT (default 8000) is the host port; STARTUP_TIMEOUT (default 120) is in seconds.
set -euo pipefail

IMAGE="${1:-fhir-flattener:latest}"
PORT="${PORT:-8000}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-120}"
BASE="http://localhost:${PORT}/fhir"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../e2e" && pwd)"
CONTAINER="fhir-flattener-e2e-$$"
WORK="$(mktemp -d)"
FAILURES=0

cleanup() {
  local status=$?
  if [ "$status" -ne 0 ] || [ "$FAILURES" -ne 0 ]; then
    echo "--- container log ---"
    docker logs "$CONTAINER" 2>&1 | tail -n 100 || true
  fi
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  rm -rf "$WORK"
}
trap cleanup EXIT

pass() { echo "PASS: $1"; }
fail() { echo "FAIL: $1"; FAILURES=$((FAILURES + 1)); }

# post FILE ACCEPT -> writes body to $WORK/body, prints the HTTP status
post_run() {
  curl -sS -o "$WORK/body" -w '%{http_code}' --max-time 600 \
    -H 'Content-Type: application/fhir+json' -H "Accept: $2" \
    --data-binary "@$DIR/$1" "$BASE/ViewDefinition/\$run"
}

docker run -d --name "$CONTAINER" -p "${PORT}:8000" "$IMAGE" >/dev/null

echo "Waiting up to ${STARTUP_TIMEOUT}s for $BASE/metadata"
deadline=$((SECONDS + STARTUP_TIMEOUT))
until curl -sf -o /dev/null "$BASE/metadata"; do
  if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER")" != "true" ]; then
    echo "FAIL: container exited before it answered"; exit 1
  fi
  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "FAIL: no answer from $BASE/metadata within ${STARTUP_TIMEOUT}s"; exit 1
  fi
  sleep 2
done

# 1. metadata returns a CapabilityStatement that announces $run
if curl -sf "$BASE/metadata" | jq -e '
    .resourceType == "CapabilityStatement"
    and ([.rest[].resource[] | select(.type == "ViewDefinition") | .operation[].name] | index("$run") != null)
  ' >/dev/null; then
  pass "metadata"
else
  fail "metadata: no CapabilityStatement with ViewDefinition/\$run"
fi

# 2. $run as CSV; Spark writes no header, and the row order is not fixed
status="$(post_run run-patients.json text/csv)"
if [ "$status" = 200 ] && diff <(sort "$WORK/body") <(sort "$DIR/expected/run-patients.csv"); then
  pass "\$run text/csv"
else
  fail "\$run text/csv (HTTP $status)"; cat "$WORK/body"
fi

# 3. $run as NDJSON; compare as sorted, key-sorted JSON objects
status="$(post_run run-patients.json application/x-ndjson)"
if [ "$status" = 200 ] && diff <(jq -cS . "$WORK/body" | sort) <(jq -cS . "$DIR/expected/run-patients.ndjson" | sort); then
  pass "\$run application/x-ndjson"
else
  fail "\$run application/x-ndjson (HTTP $status)"; cat "$WORK/body"
fi

# 4. $run without resources is rejected with an OperationOutcome
status="$(post_run run-without-resources.json text/csv)"
if [ "$status" = 500 ] && jq -e '
    .resourceType == "OperationOutcome"
    and (.issue[0].diagnostics | contains("Resources must be provided directly"))
  ' "$WORK/body" >/dev/null; then
  pass "\$run without resources"
else
  fail "\$run without resources (HTTP $status)"; cat "$WORK/body"
fi

if [ "$FAILURES" -ne 0 ]; then
  echo "$FAILURES check(s) failed"
  exit 1
fi
echo "All e2e checks passed"
