# Commit Message Patterns for Fakt

Project-specific examples and patterns for the Fakt compiler plugin.

## Fakt-Specific Scopes

| Scope | When to Use |
|-------|-------------|
| `compiler` | Core compiler plugin changes |
| `fir` | FIR phase (Frontend IR) |
| `ir` | IR phase (Intermediate Representation) |
| `generation` | Code generation components |
| `analysis` | Interface analysis |
| `dsl` | Configuration DSL |
| `types` | Type resolution |
| `gradle` | Gradle plugin |
| `annotations` | @Fake annotation module |
| `samples` | Sample projects |
| `docs` | Documentation |
| `ci` | GitHub Actions, CI configuration |

## Good Examples by Category

### Features (feat)

```
feat(compiler): add @Fake annotation detection in FIR phase
```

```
feat(generation): implement factory function generation

Generates fakeXxx {} factory functions for @Fake interfaces.
Factory returns configured FakeXxxImpl instance.
```

```
feat(dsl): add configure block for behavior customization

Allows users to configure fake behavior per-method:
fakeUserService {
    getUser { User("test") }
}
```

```
feat(ir): support suspend function generation

Handles suspend modifiers in interface methods.
Generated implementation properly marks functions as suspend.

Closes #45
```

### Bug Fixes (fix)

```
fix(fir): resolve annotation detection for nested interfaces
```

```
fix(types): handle nullable generic bounds correctly

Generic bounds like T : Comparable<T>? were not preserving
nullability in generated code.
```

```
fix(generation): prevent duplicate imports in generated files

Deduplicates import statements when multiple methods return
same types from different packages.

Fixes #67
```

### Documentation (docs)

```
docs: add KMP multi-module setup guide
```

```
docs(api): document fakeXxx factory function signature
```

```
docs: update README with Gradle 8.x compatibility notes
```

### Refactoring (refactor)

```
refactor(compiler): extract InterfaceAnalyzer to dedicated file
```

```
refactor(generation): separate implementation and factory generators

Improves separation of concerns and testability.
Each generator now has single responsibility.
```

```
refactor: migrate from buildDir to layout.buildDirectory

buildDir is deprecated in Gradle 8+.
```

### Performance (perf)

```
perf(analysis): cache interface metadata during compilation
```

```
perf(generation): batch IR node creation

Reduces compilation overhead by batching IrFactory calls.
```

### Tests (test)

```
test(generation): add generic bounds test cases
```

```
test: add GIVEN-WHEN-THEN tests for suspend functions

Validates suspend modifier preservation in generated fakes.
```

```
test(compiler): add compilation validation for KMP targets

Ensures generated code compiles on JVM, JS, and Native.
```

### Build (build)

```
build: upgrade Kotlin to 2.1.0
```

```
build(gradle): add shadowJar configuration for compiler plugin
```

```
build: configure spotless for ktfmt formatting
```

### CI (ci)

```
ci: add KMP matrix to GitHub Actions
```

```
ci: configure automatic version bumping on release
```

### Chores (chore)

```
chore: update .gitignore for generated files
```

```
chore: bump version to 1.0.0-alpha04
```

```
chore(deps): update kotlinx-coroutines to 1.8.0
```

### Reverts (revert)

```
revert: feat(ir): support variance annotations

This reverts commit abc123.
Variance handling needs redesign for cross-module support.
```

## Bad Examples (NEVER DO)

### AI Attribution (ALWAYS FORBIDDEN)

```
feat(compiler): add generic support

Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Vague Messages

```
fix: stuff

update things

misc changes

WIP
```

### Wrong Mood

```
feat: added generic support          # Should be "add"
fix: fixed the null pointer          # Should be "fix"
docs: updated the readme             # Should be "update"
```

### Too Long

```
feat(compiler): implement comprehensive generic type parameter support including class-level bounds, method-level bounds, variance annotations, and cross-module type resolution
```

Should be:
```
feat(compiler): add generic type parameter support

Implements class-level and method-level generic handling:
- Class-level bounds extracted from IrTypeParameter
- Method-level generics use identity function pattern
- Variance annotations preserved in generated code
- Cross-module types resolved via FqName lookup
```

### Missing Type

```
add generic support                  # Missing type prefix
generic support added               # Missing type, wrong mood
```

### Period at End

```
feat: add generic support.           # Remove period
```

### Invalid Type

```
feature: add thing                   # Use "feat"
bugfix: fix thing                    # Use "fix"
update: change thing                 # Use appropriate type
```

## Commit Message Templates

### Simple Change
```
<type>(<scope>): <description>
```

### Change with Body
```
<type>(<scope>): <description>

<body explaining what and why>
```

### Change with Issue Reference
```
<type>(<scope>): <description>

<body>

Closes #<issue>
```

### Breaking Change
```
<type>(<scope>)!: <description>

<body>

BREAKING CHANGE: <description of breaking change>

Migration: <how to migrate>
```

## Pre-Commit Checklist

Before every commit:

1. **Type**: Is it valid? (`feat`, `fix`, `docs`, etc.)
2. **Scope**: Is it appropriate for Fakt? (see scope table)
3. **Subject**:
   - Under 72 characters?
   - No period at end?
   - Imperative mood?
4. **Body** (if present):
   - Blank line after subject?
   - Wrapped at 80 chars?
   - Explains what and why?
5. **Footer** (if present):
   - Issue references correct?
   - Breaking change documented?
6. **CRITICAL**:
   - NO "Generated with Claude Code"?
   - NO "Co-Authored-By: Claude"?
   - NO AI attribution of any kind?

## Character Count Guide

```
         1         2         3         4         5         6         7
123456789012345678901234567890123456789012345678901234567890123456789012
                                                  |50 char   |72 char
```

Aim for 50, max 72 for subject line.
