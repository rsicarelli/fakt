# Fakt - Claude Code Documentation

> **Purpose**: Internal documentation for Claude Code development workflow
> **Testing Standard**: [Testing Guidelines](docs/development/validation/testing-guidelines.md)

Project instructions live in [`CLAUDE.md`](../CLAUDE.md) at the repository root.

## Documentation

### Core Documentation
- **[Project Overview](docs/README.md)** - Quick start and features
- **[Architecture](docs/implementation/architecture/ARCHITECTURE.md)** - FIR emission in the `FaktGenerateTask` worker; legacy in-process IR path
- **[KMP Optimization](docs/implementation/architecture/kmp-optimization-strategy.md)** - Multi-platform strategy
- **[Metadata Producer / FIR Emission](docs/implementation/architecture/metadata-producer-fir-emission.md)** - KMP producer/consumer emission
- **[v1.0 Sunset Handover](docs/implementation/v1-sunset-handover.md)** - Removing the legacy path (#150)
- **[Testing Guidelines](docs/development/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standard
- **[Troubleshooting](docs/troubleshooting/common-issues.md)** - Common issues and solutions

### Development Resources
- **[Kotlin API Reference](docs/development/kotlin-api-reference.md)** - Compiler source consultation
- **[Kotlin IR API](docs/development/kotlin-compiler-ir-api.md)** - IR API deep dive
- **[Cache-Correct KMP Research](docs/implementation/research/issue-79-cache-correct-kmp-research.md)** - Background for #79

## Skills and Commands

See the **[Skills README](skills/README.md)** for what each skill does.

| Category | Skills |
|----------|--------|
| **Compiler & codegen** | `codegen`, `feature-option`, `compilation`, `kotlin-api-consultant` |
| **Testing & samples** | `bdd-test-runner`, `sample-scaffolder` |
| **Git & GitHub** | `commit` (`/commit`), `pr` (`/pr`), `issue-creator` |
| **Meta** | `skill-creator` |

Custom command: `/release-notes <version>` (`commands/release-notes.md`).

## Structure

```
.claude/
├── README.md                      # This file
├── commands/
│   └── release-notes.md
├── docs/
│   ├── README.md                  # Project overview
│   ├── development/
│   │   ├── kotlin-api-reference.md
│   │   ├── kotlin-compiler-ir-api.md
│   │   └── validation/
│   │       └── testing-guidelines.md
│   ├── implementation/
│   │   ├── architecture/
│   │   │   ├── ARCHITECTURE.md
│   │   │   ├── kmp-optimization-strategy.md
│   │   │   └── metadata-producer-fir-emission.md
│   │   ├── research/
│   │   │   └── issue-79-cache-correct-kmp-research.md
│   │   └── v1-sunset-handover.md
│   └── troubleshooting/
│       └── common-issues.md
└── skills/                        # One directory per skill, each with a SKILL.md
    ├── README.md
    ├── bdd-test-runner/
    ├── codegen/
    ├── commit/
    ├── compilation/
    ├── feature-option/
    ├── issue-creator/
    ├── kotlin-api-consultant/
    ├── pr/
    ├── sample-scaffolder/
    └── skill-creator/
```

## Quick Commands

```bash
# Build and publish locally
make publish-local

# Test sample project
make test-sample

# Debug compiler output
make debug

# Format code
make format
```
