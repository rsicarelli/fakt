---
name: commit
description: Creates project commits — Conventional Commits format, message quality (72 char subject, 80 char body), staged-change checks, and the AI co-author trailer. Use when committing changes, running /commit, creating commit messages, or reviewing commit message quality. Make sure to use this skill whenever a git commit is being created — it is the project's commit workflow and replaces the default one.
allowed-tools: Read, Bash, Grep, Glob
---

# Commit

Creates commits that follow the project's Conventional Commits standard and credit AI co-authorship.

## Instructions

### 1. Intercept Commit Intent

Detect: "commit", "git commit", "commit changes", "push this", "done, commit it".
Activate immediately before any commit action.

### 2. Analyze Staged Changes

```bash
git diff --cached --name-only
git diff --cached

# Check for sensitive files (BLOCK if found)
git diff --cached --name-only | grep -E '\.(env|pem|key|credentials)$'
```

**Determine commit type from changes:**
- `feat` — New feature files
- `fix` — Bug fixes
- `docs` — Only docs/comments
- `refactor` — Restructure, same behavior
- `test` — Test additions/fixes
- `build` — Build system, dependencies
- `chore` — Maintenance

**Scopes** (pick from the paths changed; omit for cross-cutting changes):

| Scope | Paths |
|-------|-------|
| `compiler` | `compiler/`, `compiler-api/` |
| `codegen` | `codegen-runtime/` |
| `gradle` | `gradle-plugin/`, `build-logic/` |
| `annotations` | `annotations/` |
| `samples` | `samples/` |
| `claude` | `.claude/`, `CLAUDE.md` |
| `ci` | `.github/` |
| `deps` | dependency bumps (`chore(deps): ...`) |
| `release` | release tooling |

Documentation-only changes use the `docs` type (`docs(gradle): ...` when scoped to one module).

### 3. Validate Commit Message

**Required format:**
```
<type>[(scope)]: <description>

[optional body]

[optional footer]
```

**Validation rules:**
- [ ] Valid type: `feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert`
- [ ] Subject ≤72 characters
- [ ] No trailing period
- [ ] Imperative mood ("add" not "added")
- [ ] Blank line between subject and body
- [ ] Body lines wrapped at 80 characters

### 4. Add the AI Co-Author Trailer

When Claude wrote or co-wrote the change, end the message with a `Co-Authored-By` trailer after a blank line:

```
Co-Authored-By: Claude <model name> <noreply@anthropic.com>
```

If the session already supplies attribution lines for commits, use those verbatim instead. One trailer per commit; no other attribution text is needed.

### 5. Execute Commit

**Only after all validations pass:**

```bash
git commit -m "$(cat <<'EOF'
<validated commit message>
EOF
)"
```

**Verify:**
```bash
git log -1 --format="%H %s"
```

### 6. Handle Edge Cases

**Multiple logical changes** — recommend splitting:
```
1. refactor(codegen): extract default-value lookup to its own strategy
2. feat(codegen): add a default for kotlin.time.Duration
3. test(codegen): cover the Duration default
```

## Error Messages

**Invalid Type:**
```
COMMIT BLOCKED — Invalid Type
"update" is not valid. Use: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
```

**Subject Too Long:**
```
COMMIT BLOCKED — Subject exceeds 72 characters (currently: {n})
Suggested: "{shortened version}"
```

## Related Skills

- **`bdd-test-runner`** — Run tests before committing
- **`compilation`** — Validate code compiles before committing
