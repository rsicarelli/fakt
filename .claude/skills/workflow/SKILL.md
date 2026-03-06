---
name: workflow
description: Orchestrates multi-step Fakt development workflows — analyze, implement, test, validate, commit, PR. Use when starting a new feature, fixing a bug end-to-end, following the full development cycle, or when a task requires coordinating multiple steps like publish-local + test + format + commit. Make sure to use this skill whenever a development task spans more than one step — it ensures the correct order of operations and prevents skipping validation steps.
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, TaskCreate, TaskUpdate, AskUserQuestion
---

# Fakt Development Workflow Orchestrator

Coordinates the full development pipeline by chaining specialized skills in the right order.

## Instructions

### 1. Determine Workflow Scope

**Use AskUserQuestion to understand the task:**

```
Question: "What kind of workflow do you need?"
Options:
- Full feature (analyze → implement → test → validate → commit → PR)
- Bug fix (diagnose → fix → test → validate → commit)
- Test only (analyze → generate tests → run tests)
- Validate only (build → validate compilation → check types)
```

### 2. Gather Context

**Before starting any phase:**
- What interface(s) are involved?
- What module? (compiler, samples, gradle-plugin)
- Is this a new feature or change to existing?
- Any related issues/PRs?

### 3. Execute Phases

**Phase 1: Analyze** (if applicable)
- Use `interface-analyzer` skill patterns for @Fake interfaces
- For new @Fake options: use `feature-option` skill for the 11-layer walkthrough
- Read relevant source code
- Identify complexity and generation strategy
- Create TaskCreate entries for each deliverable

**Phase 2: Implement**
- Write code following CLAUDE.md conventions
- Use modular design (analysis → generation → output)
- Keep FIR and IR phases separate
- Build incrementally: `make publish-local`

**Phase 3: Test**
- Write GIVEN-WHEN-THEN tests first (TDD)
- Use `bdd-test-runner` skill patterns
- Run: `make test-sample`
- Validate BDD compliance

**Phase 3.5: Sample** (optional, for new features)
- Use `sample-scaffolder` skill to create/update demonstration project
- Verify feature works end-to-end in sample context

**Phase 4: Validate**
- Use `compilation` skill patterns
- Check generated code compiles across platforms
- Verify type safety, smart defaults, DSL typing
- Run: `make quick-test`

**Phase 5: Commit**
- Use `git-commit-guardian` skill patterns
- Format first: `make format`
- Conventional Commits format
- No AI attribution

**Phase 6: PR** (if requested)
- Use `pr-creator` skill patterns
- Always draft mode
- Populated template

### 4. Track Progress

Create tasks for each phase that applies:

```
TaskCreate: "Analyze {interface} structure" → in_progress → completed
TaskCreate: "Implement {feature}" → in_progress → completed
TaskCreate: "Write GIVEN-WHEN-THEN tests" → in_progress → completed
TaskCreate: "Validate compilation" → in_progress → completed
TaskCreate: "Commit changes" → in_progress → completed
TaskCreate: "Create PR" → in_progress → completed
```

Update status as you progress through each phase.

### 5. Handle Failures

**If compilation fails:**
- Switch to `compilation` skill diagnostic mode
- Fix issues before proceeding to next phase
- Re-validate after fixes

**If tests fail:**
- Diagnose failure cause
- Fix code or test
- Re-run validation

**If format fails:**
- Run `make format` and re-stage

### 6. Report Summary

At the end of the workflow:

```
WORKFLOW COMPLETE

Phases completed:
- [x] Analyze — {summary}
- [x] Implement — {files changed}
- [x] Test — {count} tests passing
- [x] Validate — compilation OK
- [x] Commit — {commit hash}: {message}
- [x] PR — {url} (draft)

Next steps: {if any}
```

## Workflow Shortcuts

**`make` commands for common operations:**
- `make publish-local` — Build + publish plugin
- `make test-sample` — Test KMP sample
- `make quick-test` — Rebuild + test (no cache)
- `make full-rebuild` — Clean + rebuild everything
- `make format` — Format code
- `make debug` — Show compiler logs

## Related Skills

This orchestrator coordinates:
- **`interface-analyzer`** — Phase 1: Analyze
- **`feature-option`** — Phase 1: Analyze (for new @Fake options)
- **`compilation`** — Phase 4: Validate
- **`bdd-test-runner`** — Phase 3: Test
- **`sample-scaffolder`** — Phase 3.5: Sample (optional)
- **`git-commit-guardian`** — Phase 5: Commit
- **`pr-creator`** — Phase 6: PR
