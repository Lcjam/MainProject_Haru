#!/bin/sh
set -eu

runner=/opt/haru-migrator/bin/haru-database-migrations
mode=${1:-auto}

: "${HARU_DB_URL:?HARU_DB_URL is required}"
: "${HARU_DATABASE:?HARU_DATABASE is required}"
: "${HARU_DB_USER:?HARU_DB_USER is required}"
: "${HARU_DB_PASSWORD:?HARU_DB_PASSWORD is required}"

run_migrator() {
  command_name=$1
  exec "$runner" "$command_name" \
    --url "$HARU_DB_URL" \
    --database "$HARU_DATABASE" \
    --user "$HARU_DB_USER"
}

case "$mode" in
  init|migrate)
    run_migrator "$mode"
    ;;
  auto)
    migration_log=$(mktemp)
    set +e
    "$runner" migrate \
      --url "$HARU_DB_URL" \
      --database "$HARU_DATABASE" \
      --user "$HARU_DB_USER" >"$migration_log" 2>&1
    migration_status=$?
    set -e
    cat "$migration_log"

    if [ "$migration_status" -eq 0 ]; then
      rm -f "$migration_log"
      exit 0
    fi

    if grep -Fq "Target database does not exist; use init only for a new empty database" "$migration_log"; then
      rm -f "$migration_log"
      run_migrator init
    fi

    rm -f "$migration_log"
    exit "$migration_status"
    ;;
  *)
    echo "Unsupported migration mode: $mode (expected auto, init, or migrate)" >&2
    exit 64
    ;;
esac
