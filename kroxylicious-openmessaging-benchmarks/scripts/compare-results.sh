#!/usr/bin/env bash
#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: compare-results.sh <baseline.json> <candidate.json>

Compare two OpenMessaging Benchmark result files and display a table
showing latency and throughput metrics side-by-side with deltas.

Arguments:
  baseline.json    Path to the baseline OMB result JSON file
  candidate.json   Path to the candidate OMB result JSON file

Options:
  --help           Show this help message and exit

Example:
  compare-results.sh results/baseline.json results/proxy-no-filters.json
EOF
}

if [[ $# -eq 0 ]]; then
    usage >&2
    exit 1
fi

if [[ "$1" == "--help" ]]; then
    usage
    exit 0
fi
