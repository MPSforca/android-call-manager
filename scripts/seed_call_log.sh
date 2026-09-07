#!/usr/bin/env bash
# Seeds fake entries into the connected device/emulator's system call log
# (content://call_log/calls) for manually testing the Recent tab, in particular
# its per-number grouping and expandable call history.
#
# Usage:
#   ./scripts/seed_call_log.sh                  # seeds a default mixed fixture set
#   ./scripts/seed_call_log.sh <number> <count>  # seeds N calls from one number
#
# See README.md "Manual/interactive testing with fixtures and mocks" for the
# underlying `adb shell content insert` command and CallLog.Calls.TYPE values.
set -euo pipefail

insert_call() {
  local number="$1" type="$2" offset_seconds="$3"
  local date_ms=$(( ($(date +%s) - offset_seconds) * 1000 ))
  adb shell content insert --uri content://call_log/calls \
    --bind number:s:"$number" \
    --bind date:l:"$date_ms" \
    --bind duration:i:30 \
    --bind type:i:"$type" \
    --bind new:i:1 \
    >/dev/null
}

if [[ $# -eq 2 ]]; then
  number="$1"
  count="$2"
  for ((i = 0; i < count; i++)); do
    insert_call "$number" 1 $((i * 60))
  done
  echo "Seeded $count incoming calls from $number."
  exit 0
fi

if [[ $# -ne 0 ]]; then
  echo "Usage: $0 [<number> <count>]" >&2
  exit 1
fi

# Default fixture: a repeat caller (3 calls, to test grouping/expansion), a
# couple of one-off numbers, and a missed + outgoing call for variety.
insert_call "+15551234567" 1 0        # repeat caller, most recent
insert_call "+15551234567" 3 3600     # repeat caller, missed, 1h ago
insert_call "+15551234567" 1 86400    # repeat caller, 1 day ago
insert_call "+15559876543" 1 1800     # one-off, 30m ago
insert_call "+15550001111" 2 7200     # one-off outgoing, 2h ago
insert_call "+15550002222" 3 172800   # one-off missed, 2 days ago

echo "Seeded default call log fixture (3 calls from +15551234567, 3 one-off numbers)."
