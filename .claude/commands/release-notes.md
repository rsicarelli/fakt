---
description: Generate improved release notes for a Fakt version
allowed-tools: Bash(gh:*)
---

Generate improved release notes for Fakt version **v$ARGUMENTS**.

## Steps

1. Fetch the current release notes: `gh release view v$ARGUMENTS --repo rsicarelli/fakt`
2. Read each PR mentioned in "What's Changed" to understand the changes: `gh pr view {number} --repo rsicarelli/fakt --json title,body`
3. Categorize changes:
   - **User-facing features** → New Features
   - **Breaking changes** → Breaking Changes (include migration path)
   - **Performance/DX improvements** → Improvements
   - **Internal refactors, CI, chores** → Keep only in "What's Changed", don't highlight

## Output Format

```
## Breaking Changes

- Description with migration path (e.g., `old` → `new`).

## New Features

- Feature description focusing on user benefit.

## Improvements

- Improvement description.

## What's Changed
* (keep existing PR links)

**Full Changelog**: (keep existing link)
```

## Rules

- Simple `-` bullet lists, no bold sub-headings per item
- Text-only, no code samples unless essential for migration
- Focus on user benefits, not implementation details
- Omit empty sections
- Always preserve "What's Changed" and "Full Changelog"

After generating, ask "Should I update the release?" and if approved, run `gh release edit v$ARGUMENTS --repo rsicarelli/fakt --notes "..."`.
