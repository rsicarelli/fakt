# Fakt Compiler Plugin - Skills System

> **Specialized skills for Kotlin compiler plugin development**
> **Location**: `.claude/skills/`

## What are Skills?

Skills are specialized, reusable knowledge modules that provide deep expertise in specific domains of Fakt compiler plugin development. They provide **comprehensive guidance** and are **automatically suggested** by Claude Code based on their description field.

## How Skills Work

Claude Code's native auto-activation system uses the `description` field in each skill's YAML frontmatter to determine when to suggest the skill. Write trigger-rich descriptions that include:

- What the skill does
- When to use it ("Use when...")
- Keywords that should trigger activation

Example:
```yaml
---
name: compilation-validator
description: Validates compilation of generated Fakt fake code ensuring zero errors. Use when validating compilation, checking generated code, or when user mentions "validate compilation", "compile fakes", or "type safety".
allowed-tools: Read, Bash, Grep, Glob
---
```

## Available Skills (13 Total)

| Skill | Purpose |
|-------|---------|
| `kotlin-api-consultant` | Validates Kotlin compiler API usage against source code |
| `compilation-error-analyzer` | Systematic compilation error diagnosis and resolution |
| `interface-analyzer` | Deep structural analysis of @Fake annotated interfaces |
| `bdd-test-runner` | Executes BDD-compliant GIVEN-WHEN-THEN tests |
| `behavior-analyzer-tester` | Deep behavior analysis and test generation |
| `git-commit-guardian` | Enforces Conventional Commits format |
| `pr-creator` | Creates professional PRs using project template |
| `compilation-validator` | Production-grade compilation validation |
| `compiler-architecture-validator` | Validates compiler plugin best practices |
| `implementation-tracker` | Monitors implementation progress |
| `fakt-docs-navigator` | Navigate internal contributor documentation |
| `public-docs-navigator` | Navigate public MkDocs documentation |
| `skill-creator` | Meta-skill for creating new skills |

## Skill Structure

Each skill follows this structure:

```
.claude/skills/{skill-name}/
├── SKILL.md              # Main skill file (required)
└── resources/            # Supporting files (optional)
    ├── reference.md
    └── examples.md
```

### SKILL.md Format

```yaml
---
name: skill-name
description: Rich description with trigger keywords. Use when...
allowed-tools: Read, Grep, Glob, Bash
---

# Skill Title

Brief overview

## Core Mission
What this skill does

## Instructions
Step-by-step guidance

## Supporting Files
- resources/reference.md - Detailed reference
```

## Using Skills

### Automatic Activation

Simply write natural prompts and Claude Code will auto-suggest relevant skills based on their descriptions:

```
"Run tests and check BDD compliance"
→ Activates: bdd-test-runner

"Check if IrFactory API changed"
→ Activates: kotlin-api-consultant

"Create a PR for this branch"
→ Activates: pr-creator
```

### Manual Invocation

Use the slash command to invoke a skill directly:

```
/pr-creator
/git-commit-guardian
```

Or ask Claude to use a specific skill:

```
"Use the kotlin-api-consultant skill to check this API"
"Apply git-commit-guardian guidelines"
```

## Creating New Skills

Use the `skill-creator` skill for guidance on creating new skills. Key requirements:

1. **Trigger-rich description** - Include "Use when..." clause and relevant keywords
2. **Proper frontmatter** - Use comma-separated format for allowed-tools (not brackets)
3. **Progressive disclosure** - Keep SKILL.md under 500 lines, use resources/ for details

## Related Documentation

- [CLAUDE.md](../../CLAUDE.md) - Main project documentation
- [Testing Guidelines](../docs/development/validation/testing-guidelines.md) - BDD standards
