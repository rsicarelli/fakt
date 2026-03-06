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

Quick navigation by topic:

**Testing & Quality**
- `.claude/docs/development/validation/testing-guidelines.md` — THE ABSOLUTE TESTING STANDARD
- `.claude/docs/development/validation/compilation-validation.md` — Generated code validation
- `.claude/docs/development/validation/type-safety-validation.md` — Generic type testing

**Architecture & Design**
- `.claude/docs/implementation/architecture/ARCHITECTURE.md` — Main system overview
- `.claude/docs/implementation/architecture/gradle-plugin.md` — Plugin implementation
- `.claude/docs/implementation/architecture/compiler-optimizations.md` — Caching strategy
- `.claude/docs/implementation/architecture/kmp-optimization-strategy.md` — KMP optimization

**Code Generation**
- `.claude/docs/implementation/codegen-v2/README.md` — Production-ready DSL
- `.claude/docs/implementation/patterns/basic-fake-generation.md` — Core generation
- `.claude/docs/implementation/patterns/suspend-function-handling.md` — Coroutines

**Generic Types**
- `.claude/docs/implementation/generics/technical-reference.md` — IrTypeSubstitutor
- `.claude/docs/implementation/generics/complex-generics-strategy.md` — Advanced strategies

**Multi-Module & KMP**
- `.claude/docs/implementation/multi-module/collector-task-implementation.md` — FakeCollectorTask
- `.claude/docs/implementation/source_sets/README.md` — Source sets guide

**API Reference**
- `.claude/docs/development/kotlin-api-reference.md` — Kotlin compiler API reference
- `.claude/docs/development/kotlin-compiler-ir-api.md` — IR API details

**Troubleshooting**
- `.claude/docs/troubleshooting/common-issues.md` — Common problems and solutions

### 3. Public Documentation (`docs/`)

Quick navigation by topic:

**Getting Started** (`docs/introduction/`)
- `overview.md` — What is Fakt
- `why-fakt.md` — Why choose Fakt
- `installation.md` — Setup instructions
- `quick-start.md` — First fake in 5 minutes
- `features.md` — Feature list

**Core Usage** (`docs/usage/`)
- `basic-usage.md` — Simple interface faking
- `suspend-functions.md` — Coroutine support
- `generics.md` — Generic type handling
- `properties.md` — val/var properties
- `call-tracking.md` — StateFlow counters

**Best Practices** (`docs/guides/`)
- `testing-patterns.md` — GIVEN-WHEN-THEN patterns
- `migration.md` — Migrate from MockK/Mockito
- `performance.md` — Benchmarks

**Multi-Module** (`docs/multi-module/`) — 6 files, ~7000 lines
- `index.md` — Architecture overview
- `getting-started.md` — Step-by-step tutorial
- `troubleshooting.md` — Common issues

**Reference** (`docs/reference/`)
- `api.md` — Generated fake APIs
- `codegen-strategy.md` — Why DSL + string-based generation
- `fakes-over-mocks.md` — Performance analysis (1,683 lines)
- `configuration.md` — fakt {} DSL configuration
- `compatibility.md` — Kotlin version compatibility
- `limitations.md` — Current limitations

**Other**
- `docs/faq.md` — Frequently asked questions
- `docs/troubleshooting.md` — Common issues and solutions
- `docs/contributing.md` — How to contribute

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
- **Code generation**: `reference/codegen-strategy.md` + `.claude/docs/implementation/codegen-v2/README.md`
- **Multi-module**: `docs/multi-module/` + `.claude/docs/implementation/multi-module/`
- **Testing**: `docs/guides/testing-patterns.md` + `.claude/docs/development/validation/testing-guidelines.md`
- **Performance**: `docs/guides/performance.md` + `docs/reference/fakes-over-mocks.md`

## Notes

- All internal paths are relative to project root (`.claude/docs/`)
- All public paths are relative to project root (`docs/`)
- Progressive disclosure: read specific files on demand, don't load everything
- Testing guidelines is THE ABSOLUTE STANDARD for all test-related questions
