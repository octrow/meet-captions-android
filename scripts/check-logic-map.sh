#!/usr/bin/env bash
# docs/logic-map.md names real modules: a rename that forgets the map fails here.
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="${ROOT:-app/src/main/java/dev/octrow/meetcaptions}"
refs=$(grep '^Module:' docs/logic-map.md | grep -o '`[^`]*`' | tr -d '`')
[ -n "$refs" ] || { echo "no Module: lines in docs/logic-map.md"; exit 1; }
missing=0
for ref in $refs; do
  if [ -d "$ROOT/$ref" ] || ls "$ROOT/$ref".* >/dev/null 2>&1; then continue; fi
  echo "missing module: $ref"; missing=1
done
exit $missing
