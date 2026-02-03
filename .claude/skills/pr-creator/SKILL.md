---
name: pr-creator
description: REQUIRED for all PR creation in this project. Creates professional PRs using project template, enforces Conventional Commits title format, BLOCKS AI attribution pollution, always creates drafts via gh CLI. Use when creating PR, opening PR, submitting PR, creating pull request, or user mentions "create pr", "open pr", "pr this", "submit pr", "make pr", "pull request", "create a pr", "open a pr", "push for review".
allowed-tools: Read, Bash, Grep, Glob, AskUserQuestion
---

# PR Creator

Creates professional pull requests with proper titles, populated templates, and always in draft mode.

## Core Mission

This skill automates PR creation following project conventions. It uses the existing `.github/pull_request_template.md`, enforces Conventional Commits title format, and **always creates PRs as drafts** to allow review before marking ready.

**Problem Solved:**
Manual PR creation is tedious and often results in incomplete descriptions or inconsistent titles. This skill ensures every PR has proper structure, meaningful descriptions, and follows project conventions.

## Instructions

### 1. Intercept PR Intent

**Detect when user wants to create a PR:**
- Direct: "create pr", "open pr", "make pr"
- Indirect: "pr this", "submit pr", "push for review"
- After work: "done, create a pr"

**Immediately activate and begin PR workflow.**

### 2. Gather Repository Information

**Collect context about current state:**

```bash
# Current branch name
git branch --show-current

# Verify not on main/master
git branch --show-current | grep -E '^(main|master)$' && echo "ERROR: Cannot PR from main"

# Check for remote tracking
git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || echo "No upstream"

# Commits in this branch (vs main)
git log main..HEAD --oneline

# Changed files summary
git diff main..HEAD --stat

# Detailed file list
git diff main..HEAD --name-only
```

**Block if:**
- On main/master branch
- No commits ahead of main
- Uncommitted changes exist (warn and offer to commit first)

### 3. Analyze Changes

**Determine PR characteristics:**

**Type** (from file changes):
- `feat` - New files in src/, new functionality
- `fix` - Changes to existing code fixing issues
- `docs` - Only .md files, docs/ changes
- `style` - Formatting, whitespace only
- `refactor` - Code restructure, same behavior
- `perf` - Performance improvements
- `test` - Test files only
- `build` - build.gradle.kts, Makefile, CI
- `ci` - .github/workflows/ changes
- `chore` - Maintenance, dependencies

**Scope** (from paths):
- `compiler` - compiler/src/**
- `fir` - compiler/src/**/fir/**
- `ir` - compiler/src/**/ir/**
- `generation` - compiler/src/**/generation/**
- `gradle` - gradle-plugin/**
- `annotations` - annotations/**
- `samples` - samples/**
- `docs` - docs/**, *.md

**Related Issues:**
- Extract from branch name: `fix/123-description` → #123
- Extract from commit messages: `Fixes #456`

### 4. Generate PR Title

**Apply Conventional Commits format:**

```
<type>[(scope)]: <description>
```

**Validation:**
- [ ] Type is valid
- [ ] Max 72 characters
- [ ] No period at end
- [ ] Imperative mood ("add" not "added")

**Examples:**
```
feat(compiler): add generic type parameter support
fix(fir): resolve annotation detection for nested interfaces
docs: update KMP multi-module guide
refactor(generation): restructure factory generation API
```

### 5. Populate PR Template

**Read project template:**
```bash
cat .github/pull_request_template.md
```

**Always use Standard format:**
```markdown
## Description
{2-3 sentences explaining what changed and why}

## Related Issue
Fixes #{issue}

## Type of Change
- [x] {Detected type}
- [ ] Breaking change (if applicable)

## Pre-Submission Checklist
- [x] Code formatted: `./gradlew spotlessApply`
- [x] Linter passing: `./gradlew lintKotlin`
- [x] Static analysis: `./gradlew detekt`
- [x] Tests added/updated and passing: `./gradlew test`
- [x] Generated code compiles (verified with sample project)
- [x] Updated documentation if needed

## Additional Context
{Key implementation details or decisions}
```

### 6. Validate Before Submission

**Check PR is ready:**

```bash
# Verify branch is pushed
git push -u origin $(git branch --show-current)

# Verify no uncommitted changes
git status --porcelain

# Optional: Run quick validation
make format 2>/dev/null || ./gradlew spotlessCheck
```

**Warn if:**
- Branch not pushed to remote
- Uncommitted changes exist
- Format check fails

### 7. Create Draft PR

**Always use draft mode:**

```bash
gh pr create \
  --draft \
  --title "<generated-title>" \
  --body "$(cat <<'EOF'
<populated-template>
EOF
)"
```

**Important flags:**
- `--draft` - Always create as draft (REQUIRED)
- `--title` - Validated Conventional Commits title
- `--body` - Populated template with proper depth

### 8. Output PR URL and Next Steps

**Success output:**

```
PR CREATED (Draft)

URL: https://github.com/rsicarelli/fakt/pull/123
Title: feat(compiler): add generic type parameter support
Status: Draft

Checklist before marking ready:
- [ ] Review the PR description for accuracy
- [ ] Ensure all CI checks pass
- [ ] Add reviewers if needed

To mark ready for review:
  gh pr ready 123

To view PR:
  gh pr view 123 --web
```

## Forbidden Patterns (CRITICAL)

**These MUST NEVER appear in PR title or description:**

```
FORBIDDEN - ALWAYS BLOCK

1. AI Attribution:
   "Generated with Claude Code"
   "Generated by Claude"
   "Created by AI"

2. Co-Author Lines:
   "Co-Authored-By: Claude"
   "Co-authored-by:.*noreply@anthropic.com"

3. Robot Emoji Attribution:
   Lines with robot emoji followed by "Generated"
```

**PRs should appear professional and human-authored.**

## Supporting Files

Progressive disclosure for detailed documentation:

- **`resources/pr-title-conventions.md`** - Full title format specification
- **`resources/pr-description-guide.md`** - How to write effective descriptions

## Related Skills

This skill composes with:
- **`git-commit-guardian`** - Ensures commits are clean before PR
- **`bdd-test-runner`** - Run tests before PR submission
- **`compilation-validator`** - Validate code compiles

This skill enables:
- Consistent PR quality across the project
- Automated changelog generation
- Clear review process with drafts

## Best Practices

1. **Always draft first** - Review before requesting reviews
2. **Match title to primary change** - Use most significant commit type
3. **Reference issues** - Link related GitHub issues
4. **Explain the why** - Description should explain reasoning
5. **No AI attribution** - Clean, professional PRs only
6. **Check CI before ready** - Ensure all checks pass first

## PR Type Reference

| Type | When to Use | Example Title |
|------|-------------|---------------|
| `feat` | New feature | `feat(dsl): add behavior configuration` |
| `fix` | Bug fix | `fix(ir): resolve type mismatch` |
| `docs` | Docs only | `docs: add KMP guide` |
| `style` | Formatting | `style: apply ktfmt` |
| `refactor` | Restructure | `refactor: extract analyzer` |
| `perf` | Performance | `perf: cache metadata` |
| `test` | Tests | `test: add generic tests` |
| `build` | Build system | `build: upgrade Kotlin` |
| `ci` | CI/CD | `ci: add KMP matrix` |
| `chore` | Maintenance | `chore: update deps` |

## Error Messages

### Not on Feature Branch
```
PR BLOCKED - Cannot create PR from main/master

Create a feature branch first:
  git checkout -b feat/your-feature
```

### No Commits
```
PR BLOCKED - No commits ahead of main

Make some changes and commit first.
Current branch is up to date with main.
```

### Uncommitted Changes
```
PR WARNING - Uncommitted changes detected

Files with changes:
  M src/main/kotlin/File.kt
  ? src/main/kotlin/NewFile.kt

Options:
1. Commit changes first (recommended)
2. Stash changes: git stash
3. Proceed anyway (changes won't be in PR)
```

### Title Too Long
```
PR BLOCKED - Title exceeds 72 characters

Current: 85 characters
Limit: 72 characters

Original: "feat(compiler): implement comprehensive generic type parameter support for all interfaces"
Suggested: "feat(compiler): add generic type support"
```

## Known Limitations

- Requires `gh` CLI to be installed and authenticated
- Cannot detect all issue references automatically
- Breaking change detection is heuristic-based
- Depth level must be explicitly chosen (by design)

## References

- **Project PR Template**: `.github/pull_request_template.md`
- **Conventional Commits**: https://www.conventionalcommits.org/
- **GitHub CLI**: https://cli.github.com/
