# Fakt Compiler Plugin - Skills System

> **Specialized skills for Kotlin compiler plugin development**
> **Location**: `.claude/skills/`

## Available Skills (10 Total)

| Skill | Purpose |
|-------|---------|
| `bdd-test-runner` | Runs tests and checks GIVEN-WHEN-THEN naming compliance |
| `codegen` | Documents the code generation pipeline (model → builder → renderer) |
| `commit` | `/commit`: Conventional Commits with the AI co-author trailer |
| `compilation` | Validates generated fakes compile and diagnoses build failures |
| `feature-option` | Guides adding new @Fake annotation options (11 touchpoints) |
| `issue-creator` | Creates GitHub issues with auto-detected project context |
| `kotlin-api-consultant` | Keeps compiler API usage working across supported Kotlin versions |
| `pr` | `/pr`: draft PRs from the project template with the AI attribution footer |
| `sample-scaffolder` | Scaffolds new sample projects (JVM, KMP, Android) |
| `skill-creator` | Meta-skill for creating new skills |

## How Skills Work

Claude Code auto-activates skills based on the `description` field in each skill's YAML frontmatter, and any skill can be run by name as a slash command (`/commit`). Example prompts:

```
"Run tests and check BDD compliance"          → bdd-test-runner
"Compilation failed, help me debug"           → compilation
"Commit this"                                 → commit
"Create a PR for this branch"                 → pr
"Add a new option to @Fake"                   → feature-option
"How does the codegen pipeline work?"         → codegen
"Does this compiler API exist in Kotlin 2.2?" → kotlin-api-consultant
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
