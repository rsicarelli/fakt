# GitHub Labels Setup Guide

This document lists the labels that should be created in the GitHub repository to support the new issue template and contribution workflow.

## How to Create Labels

Go to: `https://github.com/rsicarelli/fakt/labels`

Click "New label" for each label below, using the specified name, description, and color.

---

## Labels to Create

### Contributor Discovery

#### `good first issue`
- **Color:** `#7057ff` (purple)
- **Description:** Perfect for new contributors - well-defined task with clear scope

#### `help wanted`
- **Color:** `#008672` (teal)
- **Description:** Community contributions encouraged - maintainers need help

#### `needs-investigation`
- **Color:** `#d876e3` (pink)
- **Description:** Requires research or analysis before implementation

---

### Feature Lifecycle

#### `idea`
- **Color:** `#a2eeef` (light blue)
- **Description:** Early brainstorming - not yet a formal feature request

#### `needs-discussion`
- **Color:** `#d4c5f9` (lavender)
- **Description:** Design feedback needed before proceeding

#### `approved`
- **Color:** `#0e8a16` (green)
- **Description:** Ready for implementation - design approved

---

### Priority

#### `priority: high`
- **Color:** `#d73a4a` (red)
- **Description:** Significant impact - should be addressed soon

#### `priority: medium`
- **Color:** `#fbca04` (yellow)
- **Description:** Nice to have - moderate importance

#### `priority: low`
- **Color:** `#e4e669` (light yellow)
- **Description:** Future consideration - low urgency

---

### Categories

#### `compiler`
- **Color:** `#1d76db` (dark blue)
- **Description:** Compiler plugin work (FIR/IR phases, code generation)

#### `gradle-plugin`
- **Color:** `#0366d6` (medium blue)
- **Description:** Gradle plugin integration and configuration

#### `documentation`
- **Color:** `#0075ca` (blue)
- **Description:** Documentation improvements (MkDocs, README, guides)

#### `performance`
- **Color:** `#c2e0c6` (light green)
- **Description:** Speed, efficiency, or build time improvements

---

## Existing Labels to Keep

The following labels already exist and should be preserved:
- `bug` - Something isn't working
- `enhancement` - New feature or request
- `needs-triage` - Needs initial review by maintainers

---

## Label Usage Guidelines

### For Maintainers

**When triaging issues:**
1. Apply category label (`compiler`, `gradle-plugin`, `documentation`, etc.)
2. Add priority if clear (`priority: high/medium/low`)
3. Add `good first issue` for beginner-friendly tasks
4. Add `help wanted` when community input is valuable
5. Add `needs-discussion` when design decisions are needed
6. Move from `idea` → `needs-discussion` → `approved` as proposals mature

**For feature requests:**
- Start with `enhancement` + `needs-triage` (auto-applied)
- Add `idea` for informal/early proposals
- Add `needs-discussion` if design questions exist
- Add `approved` when ready to implement
- Add relevant category label

**For bugs:**
- Start with `bug` + `needs-triage` (auto-applied)
- Add `priority: high` if critical/blocking
- Add `needs-investigation` if root cause is unclear
- Add category label once investigated

---

## Future Enhancements

Consider adding these labels later as needed:
- `breaking-change` - Requires major version bump
- `dependencies` - Dependency updates (could use Dependabot)
- `testing` - Test infrastructure or coverage improvements
- `ci/cd` - GitHub Actions workflow changes
- `security` - Security-related issues

---

**Note:** This file is for documentation only and should not be committed to the repository unless labels have been created.
