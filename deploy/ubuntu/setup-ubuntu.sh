#!/usr/bin/env bash
set -euo pipefail

. /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
    echo 'This setup script is for Ubuntu. See HUONG-DAN.md.' >&2
    exit 1
fi

sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless curl ca-certificates
if ! command -v tailscale >/dev/null 2>&1; then
    installer=$(mktemp)
    trap 'rm -f -- "$installer"' EXIT
    curl -fsSL https://tailscale.com/install.sh -o "$installer"
    sudo sh "$installer"
fi
sudo systemctl enable --now tailscaled
echo 'Open the URL below in a browser and sign in to your own Tailscale account.'
sudo tailscale up
echo 'Ubuntu Tailscale IPv4:'
tailscale ip -4
echo 'Next: bash start-server.sh'
