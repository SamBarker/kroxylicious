#!/usr/bin/env bash
#
# Copyright Kroxylicious Authors.
#
# Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
#

set -uo pipefail

# Orthogonal throughput + connection sweep for proxy CPU sizing model fitting.
#
# Produces data to fit:  CPU_mc = a + b × throughput_MB_per_s + c × connections
#
# Phase 1: fix connections (4 producers/topic, 1 consumer/subscription), sweep total rate.
# Phase 2: fix throughput (8000 msg/s total), sweep producers per topic.
#
# Together the two phases decouple per-byte AES-GCM overhead (b) from per-connection
# overhead (c) so both coefficients can be estimated independently via OLS regression.
#
# Runs all three CPU allocations (1-core, 2-core, 4-core) in sequence, each with a full
# deploy + teardown cycle. Allocation profiles in helm/kroxylicious-benchmark/allocations/
# set CPU resources and workerThreadCount together so Netty IO threads scale with the
# allocation rather than defaulting to 2×node CPUs.
#
# Usage: scripts/sizing-sweep.sh [--cluster-overrides <file>] [--output-dir <dir>] [--set <k=v>]
#
# Run from: kroxylicious-openmessaging-benchmarks/
#
# Analyse results:
#   uv run scripts/analyze-cpu-coefficient.py --ols-fit <output-dir>
#
# Estimated runtime: ~33 probes × 10 min + ~45 min infrastructure ≈ 6 hours

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

cd "${MODULE_DIR}"

usage() {
    cat >&2 <<EOF
Usage: $(basename "$0") [--cluster-overrides <file>] [--output-dir <dir>] [--set <key=value>]

Orthogonal throughput + connection sweep to fit a proxy CPU sizing model.

  Phase 1: vary total rate (4 producers/topic, 1 consumer/subscription)
  Phase 2: vary producers/topic (8000 msg/s total, 1 consumer/subscription)

  Runs all three CPU allocations (1-core, 2-core, 4-core) in sequence.
  Allocation profiles (helm/kroxylicious-benchmark/allocations/) set CPU resources
  and Netty workerThreadCount together.

Options:
  --cluster-overrides <file>  Helm values for cluster-specific settings
  --output-dir <dir>          Root directory for all results
                              (default: results/sizing-sweep-<timestamp>)
  --set <key=value>           Pass a Helm --set override to run-benchmark.sh (repeatable)
  -h, --help                  Show this help

Analyse completed results:
  uv run scripts/analyze-cpu-coefficient.py --ols-fit <output-dir>
EOF
    exit 1
}

CLUSTER_OVERRIDES=""
OUTPUT_DIR=""
HELM_SET_ARGS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        --cluster-overrides) CLUSTER_OVERRIDES="$2"; shift 2 ;;
        --output-dir)        OUTPUT_DIR="$2";        shift 2 ;;
        --set)               HELM_SET_ARGS+=("$2");  shift 2 ;;
        -h|--help)           usage ;;
        *) echo "Error: unknown argument '$1'" >&2; usage ;;
    esac
done

if [[ -n "${CLUSTER_OVERRIDES}" && ! -f "${CLUSTER_OVERRIDES}" ]]; then
    echo "Error: --cluster-overrides file not found: ${CLUSTER_OVERRIDES}" >&2
    exit 1
fi

RUN_ID="$(date +%Y%m%d-%H%M%S)"
OUTPUT_DIR="${OUTPUT_DIR:-results/sizing-sweep-${RUN_ID}}"
mkdir -p "${OUTPUT_DIR}"

exec > >(tee "${OUTPUT_DIR}/suite.log") 2>&1

echo "=========================================="
echo "CPU sizing sweep — ${RUN_ID}"
echo "Results: ${OUTPUT_DIR}"
echo "Cluster overrides: ${CLUSTER_OVERRIDES:-none}"
echo "Started: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "=========================================="

FAILED_STEPS=()

run_step() {
    local name="$1"
    shift
    echo ""
    echo "--- ${name} ---"
    echo "Started: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    if "$@"; then
        echo "Completed: $(date -u +%Y-%m-%dT%H:%M:%SZ) — ${name}"
        return 0
    else
        echo "FAILED: $(date -u +%Y-%m-%dT%H:%M:%SZ) — ${name}" >&2
        FAILED_STEPS+=("${name}")
        return 1
    fi
}

SCENARIO="encryption"
WORKLOAD="10topics-1kb"

# Phase 1: vary total rate, fixed producers-per-topic and consumers-per-subscription
PHASE1_RATES=(5000 10000 15000 20000 25000 30000)
PHASE1_PRODUCERS_PER_TOPIC=4
PHASE1_CONSUMERS_PER_SUB=1

# Phase 2: vary producers-per-topic, fixed total rate and consumers-per-subscription
PHASE2_PRODS_PER_TOPIC=(1 2 4 8 16)
PHASE2_TOTAL_RATE=8000
PHASE2_CONSUMERS_PER_SUB=1

TOTAL_PROBES=$(( ${#PHASE1_RATES[@]} + ${#PHASE2_PRODS_PER_TOPIC[@]} ))

CLUSTER_OVERRIDES_ARG=()
[[ -n "${CLUSTER_OVERRIDES}" ]] && CLUSTER_OVERRIDES_ARG=(--cluster-overrides "${CLUSTER_OVERRIDES}")

CORE_LABELS=(1core 2core 4core)

for ci in 0 1 2; do
    LABEL="${CORE_LABELS[$ci]}"
    ALLOC_DIR="${OUTPUT_DIR}/${LABEL}"
    ALLOC_PROFILE="${SCRIPT_DIR}/../helm/kroxylicious-benchmark/allocations/${LABEL}.yaml"

    echo ""
    echo "=========================================="
    echo "Allocation: ${LABEL}"
    echo "=========================================="

    # args applied to every probe in this allocation
    ALLOC_SET_ARGS=(
        --profile "${ALLOC_PROFILE}"
        --set kafka.replicationFactor=1
        --set kafka.minInSyncReplicas=1
    )
    for set_arg in "${HELM_SET_ARGS[@]+"${HELM_SET_ARGS[@]}"}"; do
        ALLOC_SET_ARGS+=(--set "${set_arg}")
    done

    probe_num=0
    alloc_failed=false

    # --- Phase 1: throughput sweep ---
    for rate in "${PHASE1_RATES[@]}"; do
        rate_label="$(printf 'rate-%05d' "${rate}")"
        probe_dir="${ALLOC_DIR}/phase1/${rate_label}"

        rb_args=(
            --producer-rate "${rate}"
            --producers-per-topic "${PHASE1_PRODUCERS_PER_TOPIC}"
            --consumers-per-subscription "${PHASE1_CONSUMERS_PER_SUB}"
            ${CLUSTER_OVERRIDES_ARG[@]+"${CLUSTER_OVERRIDES_ARG[@]}"}
            "${ALLOC_SET_ARGS[@]}"
        )
        [[ $probe_num -gt 0 ]]                     && rb_args+=(--skip-deploy)
        [[ $probe_num -lt $((TOTAL_PROBES - 1)) ]] && rb_args+=(--skip-teardown)

        if ! run_step "${LABEL}-phase1-${rate_label}" \
                "${SCRIPT_DIR}/run-benchmark.sh" "${rb_args[@]}" \
                "${SCENARIO}" "${WORKLOAD}" "${probe_dir}"; then
            alloc_failed=true
            break
        fi
        probe_num=$(( probe_num + 1 ))
    done

    if [[ "${alloc_failed}" == "true" ]]; then
        continue
    fi

    # --- Phase 2: connection sweep ---
    for prods in "${PHASE2_PRODS_PER_TOPIC[@]}"; do
        prods_label="$(printf 'prods-per-topic-%02d' "${prods}")"
        probe_dir="${ALLOC_DIR}/phase2/${prods_label}"

        rb_args=(
            --producer-rate "${PHASE2_TOTAL_RATE}"
            --producers-per-topic "${prods}"
            --consumers-per-subscription "${PHASE2_CONSUMERS_PER_SUB}"
            ${CLUSTER_OVERRIDES_ARG[@]+"${CLUSTER_OVERRIDES_ARG[@]}"}
            "${ALLOC_SET_ARGS[@]}"
            --skip-deploy
        )
        [[ $probe_num -lt $((TOTAL_PROBES - 1)) ]] && rb_args+=(--skip-teardown)

        if ! run_step "${LABEL}-phase2-${prods_label}" \
                "${SCRIPT_DIR}/run-benchmark.sh" "${rb_args[@]}" \
                "${SCENARIO}" "${WORKLOAD}" "${probe_dir}"; then
            alloc_failed=true
            break
        fi
        probe_num=$(( probe_num + 1 ))
    done
done

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
echo "=========================================="
echo "Suite complete: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "Results: ${OUTPUT_DIR}"
echo "=========================================="

if [[ ${#FAILED_STEPS[@]} -gt 0 ]]; then
    echo ""
    echo "Failed steps:"
    for step in ${FAILED_STEPS[@]+"${FAILED_STEPS[@]}"}; do
        echo "  - ${step}"
    done
    exit 1
fi

echo ""
echo "All steps succeeded."
echo ""
echo "Analyse with:"
echo "  uv run scripts/analyze-cpu-coefficient.py --ols-fit ${OUTPUT_DIR}"
