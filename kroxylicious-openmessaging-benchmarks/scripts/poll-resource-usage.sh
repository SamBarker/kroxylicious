#!/usr/bin/env bash
#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

set -euo pipefail

# Polls pod CPU and memory usage via kubectl top during a benchmark run.
# Intended to be started as a background process by run-benchmark.sh.
#
# Each poll appends a timestamped snapshot of resource usage for all pods
# matching the given label selector(s). Output is TSV for easy parsing.
#
# Usage: poll-resource-usage.sh <namespace> <output-dir> <interval-seconds> <label-selector> [<label-selector>...]

usage() {
    cat >&2 <<EOF
Usage: $(basename "$0") <namespace> <output-dir> <interval-seconds> <label-selector> [<label-selector>...]

Polls pod CPU and memory usage via 'kubectl top pod' and appends timestamped
snapshots to <output-dir>/resource-usage.tsv.

Arguments:
  namespace          Kubernetes namespace containing the pods
  output-dir         Directory to write resource-usage.tsv into
  interval-seconds   Polling interval in seconds
  label-selector     One or more -l selectors for kubectl top pod
EOF
    exit 1
}

if [[ $# -lt 4 ]]; then
    usage
fi

NAMESPACE="$1"
OUTPUT_DIR="$2"
INTERVAL="$3"
shift 3
LABEL_SELECTORS=("$@")

USAGE_FILE="${OUTPUT_DIR}/resource-usage.tsv"

mkdir -p "${OUTPUT_DIR}"

{
    echo "# resource-usage polling started"
    echo "# namespace=${NAMESPACE} interval=${INTERVAL}s selectors=${LABEL_SELECTORS[*]}"
    echo "# started=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    printf "timestamp\tpod\tcpu\tmemory\n"
} > "${USAGE_FILE}"

while true; do
    NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    for selector in "${LABEL_SELECTORS[@]}"; do
        # kubectl top pod outputs: NAME  CPU(cores)  MEMORY(bytes)
        # Skip the header line and emit TSV with timestamp prefix.
        kubectl top pod -n "${NAMESPACE}" -l "${selector}" --no-headers 2>/dev/null \
            | while read -r pod cpu mem; do
                printf "%s\t%s\t%s\t%s\n" "${NOW}" "${pod}" "${cpu}" "${mem}"
            done
    done >> "${USAGE_FILE}"
    sleep "${INTERVAL}"
done
