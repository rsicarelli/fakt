---
name: pr
description: Creates draft pull requests from the project PR template with Conventional Commits titles and an AI attribution footer. Use when creating or opening pull requests, running /pr, pushing a branch for review, or preparing changes for merge. Make sure to use this skill whenever a pull request needs to be created — it is the project's PR workflow and replaces the default one.
allowed-tools: Read, Bash, Grep, Glob, AskUserQuestion
---

# PR

Creates pull requests — always as drafts, from the project template, crediting AI co-authorship.

## Instructions

### 1. Gather Repository State

```bash
# Current branch
git branch --show-current

# Verify not on main/master
git branch --show-current | grep -E '^(main|master)$' && echo "ERROR: Cannot PR from main"

# Check remote tracking
git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || echo "No upstream"

# Commits vs main
git log main..HEAD --oneline

# Changed files
git diff main..HEAD --stat
```

**Block if:**
- On main/master
- No commits ahead of main
- Uncommitted changes (warn, offer to commit first)

### 2. Analyze Changes

**Determine type from files:**
- `feat` — New functionality
- `fix` — Bug fixes
- `docs` — Only .md, docs/ changes
- `refactor` — Restructure, same behavior
- `test` — Test files only
- `build` — build.gradle.kts, Makefile, CI
- `chore` — Maintenance

**Determine scope from paths:** use the scope table in the `commit` skill (`compiler`, `codegen`, `gradle`, `annotations`, `samples`, `claude`, `ci`).

### 3. Generate PR Title

```
<type>[(scope)]: <description>
```

- [ ] Max 72 characters
- [ ] No trailing period
- [ ] Imperative mood

### 4. Populate PR Template

```bash
cat .github/pull_request_template.md
```

Fill template with analysis from step 2.

### 5. Add the AI Attribution Footer

When Claude wrote or co-wrote the change, end the description with an attribution line after the template:

```
🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

If the session already supplies attribution lines for pull requests, use those verbatim instead. Keep the title free of attribution.

### 6. Push and Create Draft PR

```bash
# Push branch
git push -u origin $(git branch --show-current)

# Create draft PR (if `gh` isn't available, use the session's GitHub tool with the same draft, title and body)
gh pr create \
  --draft \
  --title "<title>" \
  --body "$(cat <<'EOF'
<populated template>
EOF
)"
```

### 7. Output

```
PR CREATED (Draft)

URL: {url}
Title: {title}

To mark ready: gh pr ready {number}
To view: gh pr view {number} --web
```

## Error Messages

**Not on feature branch:**
```
PR BLOCKED — Cannot create PR from main/master
Create a feature branch first: git checkout -b feat/your-feature
```

**No commits:**
```
PR BLOCKED — No commits ahead of main
```

**Title too long:**
```
PR BLOCKED — Title exceeds 72 characters (currently: {n})
Suggested: "{shortened}"
```

## Related Skills

- **`commit`** — Clean commits before PR
- **`bdd-test-runner`** — Run tests before PR
