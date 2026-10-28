#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
if ! command -v java >/dev/null 2>&1 || ! command -v tailscale >/dev/null 2>&1; then
    echo 'Run bash setup-ubuntu.sh first.' >&2
    exit 1
fi
java_version=$(java -version 2>&1)
if [[ ! "$java_version" =~ version\ \"([0-9]+) ]] || (( BASH_REMATCH[1] < 21 )); then
    echo 'Java 21 or newer is required. Run bash setup-ubuntu.sh.' >&2
    exit 1
fi
tailscale_ip=$(tailscale ip -4)
if [[ -z "$tailscale_ip" ]]; then
    echo 'No Tailscale IPv4 address. Run sudo tailscale up first.' >&2
    exit 1
fi
echo "Ubuntu Tailscale IP: $tailscale_ip"
echo 'Default game port: 5555. Keep this terminal open while playing; Ctrl+C stops the server.'
cd -- "$script_dir"
# Bind only to Tailscale, so the game is not served on the home LAN/public interface.
exec java -Xms64m -Xmx512m "-Dtankops.config=$script_dir/config.properties" \
    "-Dtankops.bindAddress=$tailscale_ip" -jar "$script_dir/tankops-server.jar"
