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

if [[ $# -ne 2 ]]; then
    echo "Error: expected 2 arguments, got $#" >&2
    usage >&2
    exit 1
fi

BASELINE="$1"
CANDIDATE="$2"

for file in "$BASELINE" "$CANDIDATE"; do
    if [[ ! -f "$file" ]]; then
        echo "Error: file not found: $file" >&2
        exit 1
    fi
done

if ! command -v jq &>/dev/null; then
    echo "Error: jq is required but not found in PATH" >&2
    exit 1
fi

# Prints a comparison row: metric name, baseline value, candidate value, delta, and percentage change.
# Arguments: label, baseline_value, candidate_value
print_row() {
    local label="$1" baseline_val="$2" candidate_val="$3"
    local delta pct
    delta=$(echo "$candidate_val - $baseline_val" | bc -l)
    pct=$(echo "scale=1; if ($baseline_val != 0) $delta / $baseline_val * 100 else 0" | bc -l)
    # Format sign for delta and percentage
    local delta_sign="" pct_sign=""
    if (( $(echo "$delta > 0" | bc -l) )); then
        delta_sign="+"
    fi
    if (( $(echo "$pct > 0" | bc -l) )); then
        pct_sign="+"
    fi
    printf "  %-25s %12.2f %12.2f %12.2f (%s%.1f%%)\n" "$label" "$baseline_val" "$candidate_val" "$delta" "$pct_sign" "$pct"
}

echo ""
echo "Publish Latency (ms)"
printf "  %-25s %12s %12s %12s\n" "Metric" "Baseline" "Candidate" "Delta"
printf "  %-25s %12s %12s %12s\n" "-------------------------" "------------" "------------" "------------"

for metric in \
    "aggregatedPublishLatencyAvg:Avg" \
    "aggregatedPublishLatency50pct:p50" \
    "aggregatedPublishLatency95pct:p95" \
    "aggregatedPublishLatency99pct:p99" \
    "aggregatedPublishLatency999pct:p99.9"; do

    field="${metric%%:*}"
    label="${metric##*:}"
    baseline_val=$(jq ".$field" "$BASELINE")
    candidate_val=$(jq ".$field" "$CANDIDATE")
    print_row "$label" "$baseline_val" "$candidate_val"
done
