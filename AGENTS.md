# Agent Rules

## Start with the wiki

At the start of each task, check `.context/wiki/index.md` to decide
whether wiki context is needed before acting. Don't read the wiki in
full. Use the index and follow links only when they are relevant to
the task.

## Update the wiki

After completing a task, offer to update the wiki if the task yielded durable knowledge that could benefit future work, then wait for user approval. This includes new processes, architecture decisions, or insights that go beyond the immediate task.

## Primary documentation

See the following for comprehensive project context:
- [README.md](README.md) — Architecture, security model, testing philosophy, user personas
- [DEV_GUIDE.md](DEV_GUIDE.md) — Build commands, Maven profiles, IDE setup, CI

## Coding rules

Rules for all agents live in `.context/rules/`:
- [api-changes.md](.context/rules/api-changes.md) — When proposals are required
- [performance.md](.context/rules/performance.md) — Performance-sensitive patterns
- [security-patterns.md](.context/rules/security-patterns.md) — Security coding requirements
- [logging.md](.context/rules/logging.md) — Logging conventions
- [documentation-requirements.md](.context/rules/documentation-requirements.md) — When to write docs
- [pull-requests.md](.context/rules/pull-requests.md) — PR checklist requirements
- [testing.md](.context/rules/testing.md) — Test structure conventions
- [commits.md](.context/rules/commits.md) — Commit conventions

## Commit conventions

When creating commits, follow these conventions:

**Commit Message Format:**
- Use Conventional Commits: `<type>(<scope>): <description>`
- Types: `feat`, `fix`, `docs`, `build`, `chore`, `refactor`, `perf`, `test`, `ci`
- Keep subject line under 72 characters

**AI Disclosure Requirement:**
When AI assists with code changes, add an `Assisted-by:` trailer:
- Format: `Assisted-by: <Tool> <model-name> <noreply@vendor.com>`
- Placement: After commit body, before `Signed-off-by:` trailer

**DCO Signoff:**
All commits require `Signed-off-by:` trailer (auto-added by git hook).

**Example:**
```
feat(filters): add request throttling filter

Implements configurable rate limiting at the filter level with
per-client quotas and burst handling.

Assisted-by: Claude Sonnet 4.5 <noreply@anthropic.com>
Signed-off-by: Jane Developer <jane@example.com>
```

## Module-specific context

See module README.md files for detailed context:
- [kroxylicious-api/README.md](kroxylicious-api/README.md) — Filter API
- [kroxylicious-runtime/README.md](kroxylicious-runtime/README.md) — Proxy runtime
- [kroxylicious-filters/README.md](kroxylicious-filters/README.md) — End-user filters
- [kroxylicious-kms/README.md](kroxylicious-kms/README.md) — KMS API
- [kroxylicious-authorizer-api/README.md](kroxylicious-authorizer-api/README.md) — Authoriser API
- [kroxylicious-kubernetes/kroxylicious-kubernetes-api/README.md](kroxylicious-kubernetes/kroxylicious-kubernetes-api/README.md) — Kubernetes CRDs
- [kroxylicious-kubernetes/kroxylicious-operator/README.md](kroxylicious-kubernetes/kroxylicious-operator/README.md) — Kubernetes operator
- [kroxylicious-kubernetes/kroxylicious-admission/README.md](kroxylicious-kubernetes/kroxylicious-admission/README.md) — Admission webhook
- [kroxylicious-docs/README.md](kroxylicious-docs/README.md) — End-user documentation
