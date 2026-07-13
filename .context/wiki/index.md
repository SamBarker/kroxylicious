# Kroxylicious — Agent Wiki

Concise context for AI agents working in this repository.
Read this file first; follow links only when relevant to the current task.

## Contents

- [project.md](project.md) — Where to find authoritative project documentation
- [preferences.md](preferences.md) — Working standards, conventions, and collaboration style

## Quick orientation

Kroxylicious is an open-source, Kafka-protocol-aware proxy written in Java (Netty).
This is a **monorepo** covering the various tools that make the proxy functional — the proxy runtime,
a Kubernetes operator, filter plugins, KMS integrations, and end-user documentation.
These modules are intentionally separable: if or when splitting them out makes maintenance easier,
the architecture should support that.

## Agent context structure

Agent context in this repository follows a two-tier layout:

```
.context/        # Shared, tool-neutral context — readable by any agent
  wiki/          # Project knowledge, conventions, decisions
  rules/         # Coding rules applicable to all agents

.claude/         # Claude-specific config only (settings, hooks, memory)
.cursor/         # Cursor-specific config only
```

**Principle:** if a rule, convention, or decision is tool-neutral, it lives under `.context/`.
Tool-specific directories (`.claude/`, `.cursor/`, etc.) are for configuration that only that
tool understands — settings files, hooks, memory stores. They should not contain knowledge
that another tool could benefit from.

Tool-specific directories may symlink into `.context/` so their tool can discover shared content
via its native path, but `.context/` is the source of truth.
