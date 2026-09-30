---
name: docs-navigator
description: Navigates Fakt documentation — both internal contributor docs (.claude/docs/) and public MkDocs site (docs/). Use when looking for docs about architecture, testing, code generation, setup, troubleshooting, or any project convention. Make sure to use this skill whenever you need to find documentation — it knows the full doc tree structure and avoids wasting time searching in the wrong locations.
allowed-tools: Read, Grep, Glob
---

# Fakt Documentation Navigator

Navigate both internal contributor docs and public user-facing documentation efficiently.

## Instructions

### 1. Identify Doc Target

**Determine which documentation set:**
- **Internal** (`.claude/docs/`) — Architecture, compiler internals, validation, contributor guides
- **Public** (`docs/`) — User guides, installation, features, multi-module setup, FAQ

**If unclear:** Start with internal for implementation questions, public for usage questions.

### 2. Internal Documentation (`.claude/docs/`)

Quick navigation by topic (`.claude/docs/README.md` is the index):

**Testing & Quality**
- `.claude/docs/development/validation/testing-guidelines.md` — the project's testing standard

**Architecture & Design**
- `.claude/docs/implementation/architecture/ARCHITECTURE.md` — Main system overview
- `.claude/docs/implementation/architecture/kmp-optimization-strategy.md` — KMP optimization
- `.claude/docs/implementation/architecture/metadata-producer-fir-emission.md` — FIR emission metadata
- `.claude/docs/implementation/v1-sunset-handover.md` — v1.0 legacy-path removal (#150)
- `.claude/docs/implementation/research/issue-79-cache-correct-kmp-research.md` — cache-correct KMP research

**API Reference**
- `.claude/docs/development/kotlin-api-reference.md` — Kotlin compiler API reference
- `.claude/docs/development/kotlin-compiler-ir-api.md` — IR API details

**Troubleshooting**
- `.claude/docs/troubleshooting/common-issues.md` — Common problems and solutions

### 3. Public Documentation (`docs/`)

The `nav:` section of `mkdocs.yml` is the source of truth for this tree.

- `docs/index.md`, `docs/why-fakt.md`, `docs/compatibility.md`
- **Get started** (`docs/get-started/`) — `index.md`, `features.md`, `snapshots.md`
- **User guide** (`docs/user-guide/`) — `usage.md`, `generated-code-reference.md`, `plugin-configuration.md`, `immutable-vs-mutable.md`, `multi-module.md`, `test-fixtures.md`, `platform-support.md`, `testing-patterns.md`, `migration-from-mocks.md`, `performance.md`, `known-issues.md`
- **Help** (`docs/help/`) — `faq.md`, `troubleshooting.md`
- **Examples** — `docs/examples/index.md`

### 4. Search Strategy

**When topic is clear:** Read the specific file directly.

**When topic is vague:**
```bash
# Search internal docs
Grep "keyword" .claude/docs/ --output_mode=files_with_matches

# Search public docs
Grep "keyword" docs/ --output_mode=files_with_matches

# Then narrow down
Grep "keyword" docs/{likely-section}/ --output_mode=content --head_limit=30
```

### 5. Cross-References

When a topic spans docs, provide navigation:
- **Code generation**: `docs/user-guide/generated-code-reference.md` + `.claude/docs/implementation/architecture/ARCHITECTURE.md`
- **Multi-module**: `docs/user-guide/multi-module.md` + `.claude/docs/implementation/architecture/kmp-optimization-strategy.md`
- **Testing**: `docs/user-guide/testing-patterns.md` + `.claude/docs/development/validation/testing-guidelines.md`
- **Performance**: `docs/user-guide/performance.md`

## Notes

- All internal paths are relative to project root (`.claude/docs/`)
- All public paths are relative to project root (`docs/`)
- Progressive disclosure: read specific files on demand, don't load everything
- For test-related questions, the testing guidelines are the authority
