# Working Preferences

## Coding standards

Detailed rules live in `.context/rules/` — follow them for all code changes:

| Rule file | Covers |
|---|---|
| [api-changes.md](../../.context/rules/api-changes.md) | When a formal proposal is required before changing public API |
| [performance.md](../../.context/rules/performance.md) | Performance-sensitive patterns to follow/avoid |
| [security-patterns.md](../../.context/rules/security-patterns.md) | Security coding requirements |
| [logging.md](../../.context/rules/logging.md) | Logging conventions |
| [documentation-requirements.md](../../.context/rules/documentation-requirements.md) | When and what to document |
| [pull-requests.md](../../.context/rules/pull-requests.md) | PR checklist |
| [testing.md](../../.context/rules/testing.md) | Test structure (Given/When/Then, executable specification approach) |
| [commits.md](../../.context/rules/commits.md) | Commit message format, DCO signoff, AI disclosure |

## Key conventions at a glance

- **API vs implementation** — `kroxylicious-api`, `kroxylicious-kms`, `kroxylicious-authorizer-api`, and the Kubernetes CRDs are public API. Changes require a proposal. Everything else is implementation.
- **YAML config is public API** — even though the backing Java classes are not.
- **Tests are executable specifications** — Given/When/Then structure; a failing test should say what *value* is broken.
- **Commits** — Conventional Commits format; `Assisted-by:` trailer when AI helped; DCO `Signed-off-by:` required.

## Sustainability and maintainability

This is an open-source project with a small maintainer team. Prefer:
- **Minimal, focused changes** — avoid scope creep in PRs
- **Clear, self-documenting code** — contributors shouldn't need to ask what something does
- **Separable modules** — don't create unnecessary coupling between modules; the monorepo structure should not mean monolithic design

## AI collaboration style

- Make the **minimal change** that solves the problem — no unrequested refactors, extra abstractions, or speculative features
- Follow existing code style and project conventions without being asked
- Read the relevant rule files before writing code in an unfamiliar area
- Prefer editing tools over full rewrites
- When a public API change would be needed, **flag it** rather than just making it — it requires a proposal
- Don't duplicate content that already exists in README.md or module READMEs
