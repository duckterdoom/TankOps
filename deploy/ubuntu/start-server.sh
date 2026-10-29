#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
java_bin="$script_dir/runtime/bin/java"
if [[ ! -x "$java_bin" ]]; then java_bin=$(command -v java || true); fi
if [[ -z "$java_bin" ]]; then
    echo 'No Java runtime. Use a native Ubuntu bundle or run bash setup-ubuntu.sh first.' >&2
    exit 1
fi
java_version=$("$java_bin" -version 2>&1)
if [[ ! "$java_version" =~ version\ \"([0-9]+) ]] || (( BASH_REMATCH[1] < 21 )); then
    echo 'Java 21 or newer is required.' >&2
    exit 1
fi
if [[ "${1:-}" == --local && $# == 1 ]]; then
    bind_address=127.0.0.1
elif [[ $# -gt 0 ]]; then
    echo 'Usage: bash start-server.sh [--local]' >&2; exit 1
else
    command -v tailscale >/dev/null 2>&1 || { echo 'Install Tailscale or use --local for Pinggy.' >&2; exit 1; }
    bind_address=$(tailscale ip -4)
    [[ -n "$bind_address" ]] || { echo 'Run sudo tailscale up first.' >&2; exit 1; }
fi
echo "Server bind address: $bind_address"
echo 'Keep this terminal open while playing; Ctrl+C stops the server.'
cd -- "$script_dir"
exec "$java_bin" -Xms64m -Xmx512m "-Dtankops.config=$script_dir/config.properties" \
    "-Dtankops.bindAddress=$bind_address" -jar "$script_dir/tankops-server.jar"
