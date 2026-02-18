#!/usr/bin/env bash
#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: collect-results.sh [options]

Collect OpenMessaging Benchmark results and metadata.

Options:
  --generate-run-metadata <dir>  Generate run-metadata.json in the given directory
  --help                         Show this help message and exit

Example:
  collect-results.sh --generate-run-metadata ./results/2024-01-15
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

if [[ "$1" == "--generate-run-metadata" ]]; then
    if [[ $# -lt 2 ]]; then
        echo "Error: --generate-run-metadata requires a directory argument" >&2
        exit 1
    fi

    OUTPUT_DIR="$2"
    mkdir -p "$OUTPUT_DIR"

    if ! command -v jq &>/dev/null; then
        echo "Error: jq is required but not found in PATH" >&2
        exit 1
    fi

    GIT_COMMIT=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
    GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
    TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    jq -n \
        --arg commit "$GIT_COMMIT" \
        --arg branch "$GIT_BRANCH" \
        --arg timestamp "$TIMESTAMP" \
        '{gitCommit: $commit, gitBranch: $branch, timestamp: $timestamp}' \
        > "$OUTPUT_DIR/run-metadata.json"

    echo "Generated $OUTPUT_DIR/run-metadata.json"
    exit 0
fi

echo "Error: unknown option: $1" >&2
usage >&2
exit 1
