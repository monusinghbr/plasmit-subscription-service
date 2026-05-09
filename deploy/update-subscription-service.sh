#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="plasmit-subscription-service.service"
BRANCH="${1:-main}"

cd "$(dirname "$0")/.."

UNIT_EXEC_START="$(systemctl show "${SERVICE_NAME}" -p ExecStart --value 2>/dev/null || true)"
if [[ -n "${UNIT_EXEC_START}" ]]; then
  echo "Systemd ExecStart: ${UNIT_EXEC_START}"
fi

echo "Updating $(pwd) from origin/${BRANCH}"
git fetch origin "${BRANCH}"
git checkout "${BRANCH}"
git pull --ff-only origin "${BRANCH}"

echo "Building subscription service"
./mvnw clean package -DskipTests

echo "Restarting ${SERVICE_NAME}"
sudo systemctl daemon-reload
sudo systemctl restart "${SERVICE_NAME}"
sudo systemctl status "${SERVICE_NAME}" --no-pager

echo "Checking port 8083"
ss -ltnp | grep ':8083 ' || true

echo "Latest deployed commit:"
git log -1 --oneline
