# Fakt - Claude Code Documentation

> **Purpose**: Internal documentation for Claude Code development workflow
> **Testing Standard**: [Testing Guidelines](docs/development/validation/testing-guidelines.md)

## Documentation

### Core Documentation
- **[Project Overview](docs/README.md)** - Quick start and features
- **[Architecture](docs/implementation/architecture/ARCHITECTURE.md)** - FIR→IR two-phase design
- **[KMP Optimization](docs/implementation/architecture/kmp-optimization-strategy.md)** - Multi-platform strategy
- **[Testing Guidelines](docs/development/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standard
- **[Troubleshooting](docs/troubleshooting/common-issues.md)** - Common issues and solutions

### Development Resources
- **[Kotlin API Reference](docs/development/kotlin-api-reference.md)** - Compiler source consultation
- **[Kotlin IR API](docs/development/kotlin-compiler-ir-api.md)** - IR API deep dive

## Skills

See **[Skills README](skills/README.md)** for the complete skills system.

### Available Skills

| Category | Skills |
|----------|--------|
| **Analysis** | `kotlin-api-consultant`, `interface-analyzer`, `compilation-error-analyzer` |
| **Core Workflows** | `bdd-test-runner`, `behavior-analyzer-tester`, `git-commit-guardian`, `pr-creator` |
| **Validation** | `compilation-validator`, `compiler-architecture-validator`, `implementation-tracker` |
| **Knowledge Base** | `fakt-docs-navigator`, `public-docs-navigator` |
| **Development** | `skill-creator` |

## Structure

```
.claude/
├── README.md              # This file
├── docs/
│   ├── README.md          # Project overview
│   ├── development/
│   │   ├── kotlin-api-reference.md
│   │   ├── kotlin-compiler-ir-api.md
│   │   └── validation/
│   │       └── testing-guidelines.md
│   ├── implementation/
│   │   └── architecture/
│   │       ├── ARCHITECTURE.md
│   │       └── kmp-optimization-strategy.md
│   └── troubleshooting/
│       └── common-issues.md
├── skills/                # Claude Code skills
│   ├── analysis/
│   ├── core-workflows/
│   ├── development/
│   ├── knowledge-base/
│   └── validation/
└── hooks/                 # Claude Code hooks
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
