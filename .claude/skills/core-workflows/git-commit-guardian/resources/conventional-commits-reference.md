# Conventional Commits Reference

Quick reference for the Conventional Commits 1.0.0 specification.

## Format

```
<type>[optional scope][optional !]: <description>

[optional body]

[optional footer(s)]
```

## Types

| Type | Description | SemVer Impact |
|------|-------------|---------------|
| `feat` | New feature | MINOR |
| `fix` | Bug fix | PATCH |
| `docs` | Documentation only | - |
| `style` | Formatting, whitespace | - |
| `refactor` | Code change, no behavior change | - |
| `perf` | Performance improvement | PATCH |
| `test` | Adding/correcting tests | - |
| `build` | Build system, dependencies | - |
| `ci` | CI configuration | - |
| `chore` | Maintenance tasks | - |
| `revert` | Reverting previous commit | varies |

## Breaking Changes

Two ways to indicate breaking changes:

**1. Exclamation mark in header:**
```
feat(api)!: change factory signature to require config block
```

**2. Footer notation:**
```
feat(api): change factory signature to require config block

BREAKING CHANGE: fakeXxx() now requires configuration block.
Migrate: fakeXxx() → fakeXxx {}
```

Both trigger MAJOR version bump in semantic versioning.

## Scope

Optional, provides context:
```
feat(compiler): ...
fix(fir): ...
docs(api): ...
test(generation): ...
```

Common scopes for Fakt:
- `compiler` - Compiler plugin core
- `fir` - FIR phase
- `ir` - IR phase
- `generation` - Code generation
- `dsl` - Configuration DSL
- `gradle` - Gradle plugin
- `annotations` - @Fake annotation module
- `samples` - Sample projects
- `docs` - Documentation

## Subject Line Rules

1. **Maximum 72 characters** (aim for 50)
2. **No period at end**
3. **Imperative mood** ("add" not "added")
4. **Lowercase after colon** (unless proper noun)

```
feat(compiler): add generic type support
fix: resolve null pointer in type resolution
docs: update installation guide
```

## Body Rules

1. **Blank line** between subject and body
2. **Wrap at 80 characters**
3. **Explain what and why**, not how
4. **Use bullet points** for multiple items

```
feat(compiler): add generic type parameter support

This change implements handling for class-level generics in the IR
generation phase. Method-level generics use an identity function
pattern due to type erasure constraints.

Key changes:
- Extract generic bounds from IrTypeParameter
- Generate type-safe default values
- Add variance handling for covariant/contravariant types
```

## Footer Rules

**Issue references:**
```
Closes #123
Fixes #456
Resolves #789
```

**Breaking change (alternative to `!`):**
```
BREAKING CHANGE: description of breaking change
```

**Multiple footers:**
```
Closes #123
Reviewed-by: @username
BREAKING CHANGE: API signature changed
```

## Complete Examples

### Simple feature
```
feat: add user authentication
```

### Feature with scope
```
feat(auth): implement JWT token validation
```

### Bug fix with body
```
fix(compiler): resolve stack overflow in recursive types

The type resolver was not tracking visited types during recursive
resolution, causing infinite loops with self-referential generics.

Added visited set to break cycles.

Fixes #234
```

### Breaking change
```
feat(api)!: require explicit configuration for factory functions

BREAKING CHANGE: Factory functions no longer have implicit defaults.

Before: val fake = fakeUserService()
After:  val fake = fakeUserService {}

This change improves type safety and makes behavior explicit.

Migration guide: https://fakt.dev/migration/v2
```

### Documentation
```
docs: add troubleshooting section for KMP projects

Covers common issues:
- Source set configuration
- Expect/actual declarations
- Platform-specific fakes
```

### Revert
```
revert: feat(compiler): add generic type parameter support

This reverts commit abc123def456.

Reason: Causing compilation failures in multi-module projects.
Will re-implement with proper cross-module resolution.
```

## Anti-Patterns

**Avoid these:**

```
# Too vague
fix: bug fix
update: stuff
misc: various changes

# Wrong mood (not imperative)
feat: added new feature
fix: fixed the bug
docs: updated readme

# Period at end
feat: add feature.

# Too long subject
feat(compiler): implement comprehensive generic type parameter support for all interface method variations including bounds

# Wrong type
feature: add thing  (use "feat")
bugfix: fix thing   (use "fix")

# AI attribution (ALWAYS FORBIDDEN)
feat: add feature

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Quick Checklist

Before committing:
- [ ] Type is valid (`feat`, `fix`, `docs`, etc.)
- [ ] Subject ≤72 characters
- [ ] No period at subject end
- [ ] Imperative mood used
- [ ] Blank line before body (if body exists)
- [ ] Body wrapped at 80 chars
- [ ] Breaking changes marked
- [ ] NO AI attribution lines
- [ ] NO "Generated with Claude Code"
