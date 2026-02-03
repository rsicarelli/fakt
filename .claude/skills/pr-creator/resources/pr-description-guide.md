# PR Description Guide

How to write effective PR descriptions at each depth level.

## Depth Levels Overview

| Level | When to Use | Description Length |
|-------|-------------|-------------------|
| **Quick** | Typos, small docs, trivial fixes | 1 sentence |
| **Standard** | Most PRs, features, bug fixes | 2-3 sentences |
| **Comprehensive** | Major features, breaking changes | Multiple paragraphs |

## Level 1: Quick

**Use for:**
- Typo fixes
- Documentation updates
- Single-line changes
- Dependency bumps
- Formatting-only changes

**Template:**
```markdown
## Description
Brief one-liner explaining the change.

## Type of Change
- [x] Documentation / Bug fix / etc.

## Pre-Submission Checklist
- [x] Code formatted
- [x] Tests passing
```

**Example:**
```markdown
## Description
Fix typo in installation guide ("configuraiton" → "configuration").

## Type of Change
- [x] Documentation

## Pre-Submission Checklist
- [x] Code formatted
- [x] Tests passing
```

## Level 2: Standard

**Use for:**
- Most features
- Bug fixes
- Refactoring
- Test additions
- Non-breaking changes

**Template:**
```markdown
## Description
2-3 sentences explaining what changed and why. Focus on the problem
solved and the approach taken.

## Related Issue
Fixes #123

## Type of Change
- [x] New feature / Bug fix / Refactor / etc.

## Pre-Submission Checklist
- [x] Code formatted: `./gradlew spotlessApply`
- [x] Linter passing: `./gradlew lintKotlin`
- [x] Static analysis: `./gradlew detekt`
- [x] Tests added/updated and passing: `./gradlew test`
- [x] Generated code compiles (verified with sample project)
- [x] Updated documentation if needed

## Additional Context
Key implementation details, decisions made, or notes for reviewers.
```

**Example:**
```markdown
## Description
Adds support for suspend functions in @Fake interfaces. The generated
fake implementation properly marks functions as suspend and handles
coroutine context correctly.

## Related Issue
Fixes #45

## Type of Change
- [x] New feature

## Pre-Submission Checklist
- [x] Code formatted: `./gradlew spotlessApply`
- [x] Linter passing: `./gradlew lintKotlin`
- [x] Static analysis: `./gradlew detekt`
- [x] Tests added/updated and passing: `./gradlew test`
- [x] Generated code compiles (verified with sample project)
- [x] Updated documentation if needed

## Additional Context
Suspend modifier is detected in FIR phase and propagated to IR generation.
Uses `IrSimpleFunction.isSuspend` property for detection.
```

## Level 3: Comprehensive

**Use for:**
- Major features
- Breaking changes
- Architectural changes
- Multi-module changes
- Anything requiring migration

**Template:**
```markdown
## Description

### Problem
What problem does this PR solve? Why is this change needed?

### Solution
How does this PR solve the problem? What approach was taken?

### Key Changes
- Change 1: explanation
- Change 2: explanation
- Change 3: explanation

### Design Decisions
Any important decisions and their rationale.

## Related Issue
Fixes #123
Relates to #456, #789

## Type of Change
- [x] New feature
- [x] Breaking change

## Pre-Submission Checklist
- [x] Code formatted: `./gradlew spotlessApply`
- [x] Linter passing: `./gradlew lintKotlin`
- [x] Static analysis: `./gradlew detekt`
- [x] Tests added/updated and passing: `./gradlew test`
- [x] Generated code compiles (verified with sample project)
- [x] Updated documentation if needed
- [x] No breaking changes OR breaking changes documented

**Shortcut (if using Makefile):** `make format && make test`

## Additional Context

### Architecture
How this fits into the overall system architecture.

### Performance
Any performance considerations or benchmarks.

### Future Work
Related improvements planned for future PRs.

### Screenshots/Examples
If applicable, include visual examples or code snippets.

## Breaking Changes

### What Changed
Description of breaking changes.

### Migration Guide
Step-by-step instructions to migrate:

1. Step one
2. Step two
3. Step three

### Before/After
```kotlin
// Before
val fake = fakeUserService()

// After
val fake = fakeUserService {
    // configuration required
}
```
```

**Example:**
```markdown
## Description

### Problem
The current factory function API uses implicit defaults which can lead
to unexpected behavior. Users don't realize their fakes have default
implementations that may not match their test expectations.

### Solution
Require explicit configuration block for factory functions. This makes
the API more explicit and prevents confusion about default behavior.

### Key Changes
- `fakeXxx()` now requires `{}` block (even if empty)
- Default behaviors are only applied when explicitly configured
- Added `FakeConfig.defaults()` for opt-in default behavior

### Design Decisions
- Chose required block over optional flag for explicitness
- Aligned with Kotlin DSL patterns used in kotlinx.serialization
- Considered backwards compatibility layer but decided clean break better

## Related Issue
Fixes #78
Relates to #45, #67

## Type of Change
- [x] New feature
- [x] Breaking change

## Pre-Submission Checklist
- [x] Code formatted: `./gradlew spotlessApply`
- [x] Linter passing: `./gradlew lintKotlin`
- [x] Static analysis: `./gradlew detekt`
- [x] Tests added/updated and passing: `./gradlew test`
- [x] Generated code compiles (verified with sample project)
- [x] Updated documentation if needed
- [x] No breaking changes OR breaking changes documented

## Additional Context

### Architecture
Factory generation now always emits a trailing lambda parameter.
The `FakeConfig` class handles default value application.

### Performance
No measurable impact on compilation time.

### Future Work
- Add `@FakeDefaults` annotation for default configuration
- Support for partial defaults via `configure {}` syntax

## Breaking Changes

### What Changed
Factory functions now require configuration block.

### Migration Guide
1. Find all `fakeXxx()` calls
2. Add empty block: `fakeXxx {}`
3. Or use defaults: `fakeXxx { defaults() }`

### Before/After
```kotlin
// Before (implicit defaults)
val fake = fakeUserService()

// After (explicit)
val fake = fakeUserService {}

// Or with defaults
val fake = fakeUserService {
    defaults()
}
```
```

## Writing Tips

### Do

- **Explain the why**: Why is this change needed?
- **Be specific**: What exactly changed?
- **Link issues**: Reference related GitHub issues
- **Include examples**: Show code when helpful
- **Note for reviewers**: Highlight tricky areas

### Don't

- **Be vague**: "Fixed stuff" or "Updates"
- **Assume context**: Explain for future readers
- **Skip checklist**: Verify each item
- **Include AI attribution**: Keep it professional
- **Overwrite template**: Use the project template

## Checklist Items Explained

| Item | What It Means |
|------|---------------|
| Code formatted | Ran `./gradlew spotlessApply` |
| Linter passing | Ran `./gradlew lintKotlin` with no errors |
| Static analysis | Ran `./gradlew detekt` with no issues |
| Tests passing | Ran `./gradlew test` successfully |
| Generated code compiles | Tested with sample project build |
| Documentation updated | Updated docs if behavior changed |
| Breaking changes documented | Added migration guide if breaking |

## Forbidden Content

**Never include in PR descriptions:**

- "Generated with Claude Code"
- "Co-Authored-By: Claude"
- Any AI attribution
- Robot emoji attribution

PRs should appear professionally authored.
