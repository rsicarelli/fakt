---
name: issue-creator
description: Creates well-structured GitHub issues using gh CLI with auto-detected project context (version, platform, module). Supports bug reports, feature requests, and internal tasks following repo templates. Use when creating a GitHub issue, reporting a bug, requesting a feature, logging a task, documenting a TODO as an issue, or recording a bug found during development. Make sure to use this skill whenever an issue needs to be created — it auto-detects project context and applies the correct template.
allowed-tools: Read, Bash, Grep, Glob, AskUserQuestion
---

# Issue Creator

Creates GitHub issues with proper context using `gh issue create` and the repository's issue templates.

## Instructions

### 1. Determine Issue Type

Use `AskUserQuestion` to select:
- **Bug report** — something broken, unexpected behavior
- **Feature request** — new capability or enhancement
- **Task/chore** — internal work, refactoring, documentation

### 2. Auto-Detect Project Context

Read project metadata before creating the issue:

```bash
# Version
grep "^version=" gradle.properties

# Kotlin version
grep "^kotlin " gradle/libs.versions.toml

# Current branch and recent changes
git branch --show-current
git log --oneline -5
```

### 3. Create Issue by Type

#### Bug Report

Title prefix: `[Bug]: `. Auto-labels: `bug`, `needs-triage`.

Template fields (from `.github/ISSUE_TEMPLATE/bug_report.yml`):
- **Fakt Version** (auto-detected from `gradle.properties`)
- **Kotlin Version** (auto-detected from `libs.versions.toml`)
- **Platform** — KMP / JVM only / Android only
- **Gradle Version** (optional)
- **KMP Targets** (optional, if KMP)
- **Module Setup** — Single-module / Multi-module
- **Bug Description** — structured: what's happening, steps to reproduce, code example, error logs

```bash
gh issue create \
  --title "[Bug]: {description}" \
  --label "bug,needs-triage" \
  --body "$(cat <<'EOF'
### Fakt Version
{version}

### Kotlin Version
{kotlinVersion}

### Platform
{platform}

### Bug Description

**What's happening:**
{description}

**Steps to reproduce:**
1. ...

**Code example:**
```kotlin
// minimal reproduction
```

**Error/build logs:**
```
{logs}
```
EOF
)"
```

#### Feature Request

Title prefix: `[Feature]: `. Auto-labels: `enhancement`, `needs-triage`.

Template fields (from `.github/ISSUE_TEMPLATE/feature_request.yml`):
- **Problem description** — what problem does this solve
- **Proposed solution** — Kotlin code showing desired API
- **Alternatives considered** (optional)
- **Platform** (optional)

```bash
gh issue create \
  --title "[Feature]: {description}" \
  --label "enhancement,needs-triage" \
  --body "$(cat <<'EOF'
### Problem Description
{problem}

### Proposed Solution
```kotlin
{proposedApi}
```

### Alternatives Considered
{alternatives}
EOF
)"
```

#### Task/Chore

No template required (internal use). Use conventional title format.

```bash
gh issue create \
  --title "{type}: {description}" \
  --label "{labels}" \
  --body "$(cat <<'EOF'
## Context
{context}

## Scope
{scope}

## Checklist
- [ ] {step1}
- [ ] {step2}
EOF
)"
```

### 4. Available Labels

**Auto-applied by templates:**
- `bug` + `needs-triage` (bug reports)
- `enhancement` + `needs-triage` (feature requests)

**Category labels:**
- `compiler`, `gradle-plugin`, `documentation`, `performance`, `testing`, `ci/cd`

**Priority labels:**
- `priority: high`, `priority: medium`, `priority: low`

**Contributor labels:**
- `good first issue`, `help wanted`, `needs-investigation`, `needs-discussion`

**Lifecycle labels:**
- `idea`, `approved`, `breaking-change`, `dependencies`, `security`

> **Note**: Category and lifecycle labels must exist in the repository before use. Run `gh label list` to check available labels, or `gh label create` to add missing ones. Only `bug`, `enhancement`, and `needs-triage` are auto-created by issue templates.

### 5. Add Context from Codebase

When the issue relates to specific code, gather relevant context:

```bash
# Find affected files
# Read error messages from recent builds
# Check if related issues exist
gh issue list --label "{label}" --state open
```

Include file paths, function names, and module affected in the issue body.

> **Note**: GitHub issue templates use YAML form schema. When creating via `gh issue create --body`, the structured fields render as markdown headers in the created issue.

### 6. Output

After creating the issue, display:
- Issue URL
- Issue number
- Labels applied
- Assigned template

## Related Skills

- `pr-creator` — analogous GitHub creation workflow
- `workflow` — can create issues for discovered bugs during development
- `compilation` — provides error context for bug reports
