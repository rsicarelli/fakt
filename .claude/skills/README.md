# Fakt Compiler Plugin - Skills System

> **Specialized skills for Kotlin compiler plugin development**
> **Location**: `.claude/skills/`

## Available Skills (15 Total)

| Skill | Purpose |
|-------|---------|
| `behavior-analyzer-tester` | Deep behavior analysis and GIVEN-WHEN-THEN test generation |
| `bdd-test-runner` | Executes and validates BDD-compliant tests |
| `codegen` | Documents the code generation pipeline (model → builder → renderer) |
| `compilation` | Validates compilation and diagnoses build failures |
| `compiler-architecture-validator` | Validates compiler plugin best practices |
| `docs-navigator` | Navigates internal + public documentation |
| `feature-option` | Guides adding new @Fake annotation options (11 touchpoints) |
| `git-commit-guardian` | Enforces Conventional Commits, blocks AI attribution |
| `interface-analyzer` | Deep structural analysis of @Fake interfaces |
| `issue-creator` | Creates GitHub issues with auto-detected project context |
| `kotlin-api-consultant` | Validates Kotlin compiler API usage |
| `pr-creator` | Creates professional draft PRs |
| `sample-scaffolder` | Scaffolds new sample projects (JVM, KMP, Android) |
| `skill-creator` | Meta-skill for creating new skills |
| `workflow` | Orchestrates full development pipeline |

## How Skills Work

Claude Code auto-activates skills based on the `description` field in each skill's YAML frontmatter. Write natural prompts and relevant skills are suggested automatically:

```
"Run tests and check BDD compliance"          → bdd-test-runner
"Compilation failed, help me debug"           → compilation
"Create a PR for this branch"                 → pr-creator
"Analyze the UserService interface"           → interface-analyzer
"Start working on the new feature"            → workflow
"Add a new option to @Fake"                   → feature-option
"How does the codegen pipeline work?"         → codegen
"Create a new sample project"                 → sample-scaffolder
"Create an issue for this bug"                → issue-creator
```

## Skill Structure

```
.claude/skills/{skill-name}/
├── SKILL.md              # Main skill file (required)
└── resources/            # Supporting files (optional, loaded on-demand)
```

### SKILL.md Format

```yaml
---
name: skill-name
description: What it does. Use when {scenarios}.
allowed-tools: Read, Grep, Glob
---

# Skill Title

## Instructions
### 1. First Step
### 2. Second Step

## Supporting Files
## Related Skills
```

## Creating New Skills

Use the `skill-creator` skill. Key requirements:
1. Description < 1024 chars with "Use when" clause
2. Minimal allowed-tools (only what's needed)
3. Progressive disclosure (extract to resources/ if > 500 lines)
