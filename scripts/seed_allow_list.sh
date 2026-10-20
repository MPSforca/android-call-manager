#!/usr/bin/env bash
# Seeds fake entries directly into the app's own Room database (allow_list table)
# on the connected device/emulator, for manually testing "allowlist a number that
# isn't a contact" without tapping through the UI for each one.
#
# Requires a debug build installed (run-as only works on debuggable apps).
#
# Usage:
#   ./scripts/seed_allow_list.sh                        # seeds a default fixture set
#   ./scripts/seed_allow_list.sh <number> [<name>]       # seeds one entry
#
# See README.md "Manual/interactive testing with fixtures and mocks" for how this
# fits alongside seeding contacts and the system call log.
set -euo pipefail

PKG="com.sforca.callmanager"
DB="databases/call-manager.db"

sql() {
  adb shell run-as "$PKG" sqlite3 "$DB" "$1"
}

insert_entry() {
  local number="$1" name="$2"
  local now_ms
  now_ms=$(($(date +%s) * 1000))
  local name_sql="NULL"
  if [[ -n "$name" ]]; then
    name_sql="'$(printf '%s' "$name" | sed "s/'/''/g")'"
  fi
  sql "INSERT OR REPLACE INTO allow_list (number, displayName, addedAt) VALUES ('$number', $name_sql, $now_ms);"
}

if [[ $# -ge 1 ]]; then
  number="$1"
  name="${2:-}"
  insert_entry "$number" "$name"
  echo "Allowlisted $number${name:+ ($name)}."
  exit 0
fi

# Default fixture: a mix of non-contact numbers (the case this feature is for)
# and one that matches a name, to eyeball the displayName fallback in the Lists tab.
insert_entry "+15557778888" "Allowlisted Non-Contact"
insert_entry "+15559990000" ""
insert_entry "+15551112222" "Pharmacy Auto-Line"

echo "Seeded default allow-list fixture (3 numbers). Check the Lists tab's Allow list section."
