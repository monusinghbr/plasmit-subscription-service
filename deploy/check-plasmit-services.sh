#!/usr/bin/env bash
set -euo pipefail

SERVICES=(
  plasmit-auth.service
  plasmit-super-admin.service
  plasmit-subscription-service.service
  plasmit-hospital-core.service
  plasmit-patient-hospital.service
  plasmit-appointment-hospital.service
  plasmit-lab-hospital.service
  plasmit-billing-hospital.service
  plasmit-prescription-hospital.service
  plasmit-pharmacy-hospital.service
  plasmit-pharmacy-sales-hospital.service
)

echo "Auto-start status:"
systemctl is-enabled "${SERVICES[@]}"

echo
echo "Runtime status:"
systemctl --no-pager --full status "${SERVICES[@]}"
