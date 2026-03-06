---
name: skill-creator
description: Creates new Claude Code Skills following project patterns with trigger-rich descriptions and progressive disclosure. Use when creating new skills, converting commands to skills, scaffolding skill structure, or updating an existing skill's structure. Make sure to use this skill whenever a new skill needs to be created — it enforces the project's conventions for descriptions, body structure, allowed-tools, and progressive disclosure that are easy to get wrong.
allowed-tools: Read, Write, Bash, Grep, Glob
---

# Skill Creator

Scaffolds new Claude Code Skills with proper structure, trigger-rich descriptions, and progressive disclosure.

## Instructions

### 1. Gather Requirements

**Required:**
- [ ] Skill name (kebab-case)
- [ ] Core purpose (what problem does it solve?)
- [ ] Trigger keywords (how will users invoke it?)

**Optional:**
- [ ] Required tools (Read, Bash, etc.)
- [ ] Supporting files needed
- [ ] Dependencies on other skills

**If migrating from slash command:**
```bash
cat .claude/commands/{command-name}.md
# Extract: allowed-tools, core logic, dependencies, arguments
```

### 2. Craft Description

Descriptions must be **"pushy"** — they should actively combat under-triggering by covering edge cases and using assertive language. The goal is to make sure the skill gets activated whenever it's relevant.

**Template:**
```
{What it does}. Use when {scenario 1}, {scenario 2}, {scenario 3}, or {scenario 4}. Make sure to use this skill whenever {broader activation context} — {reason why this matters}.
```

**Rules:**
- Max 1024 chars
- Must include "Use when" clause with 3+ specific scenarios
- Must include "Make sure to use this skill whenever" for broader activation
- Third person (never "I can" or "you can")
- Be specific, not vague
- No quoted keyword lists — trust semantic matching
- Explain **why** the skill matters, not just what it does

**Example:**
```yaml
# Bad: Validates Kotlin APIs
# Mediocre: Validates Kotlin compiler API usage against source code. Use when validating API calls or checking compatibility.
# Good: Validates Kotlin compiler API usage against source code, checking deprecations and compatibility. Use when validating API calls, checking compiler API compatibility, verifying API hasn't been deprecated, or debugging API issues. Make sure to use this skill whenever compiler plugin code references Kotlin internal APIs — these APIs change frequently between versions and silent breakage is common.
```

### 3. Design Structure

**Simple (no supporting files):**
```
skill-name/
└── SKILL.md  (<300 lines)
```

**Medium (some references):**
```
skill-name/
├── SKILL.md            # <500 lines
└── resources/
    └── reference.md    # On-demand
```

**Complex (scripts + docs):**
```
skill-name/
├── SKILL.md
├── scripts/
└── resources/
```

**Rule:** If SKILL.md would exceed 500 lines, extract details to resources/.

### Writing Style: Explain the "Why"

Instructions should explain **why** each step exists, not just what to do. This helps the agent make better decisions when encountering edge cases.

```markdown
# Bad — just commands:
### 3. Run tests
Run `make test-sample`.

# Good — explains why:
### 3. Run tests
Run `make test-sample` to verify generated code compiles against the published plugin.
This catches issues that unit tests miss because it exercises the full compilation pipeline.
```

Keep instructions lean and imperative. Add context lines only where the "why" isn't obvious.

### 4. Generate SKILL.md

```yaml
---
name: {skill-name}
description: {description from step 2}
allowed-tools: {minimal set — only what's needed}
---

# {Skill Title}

{One-line purpose.}

## Instructions

### 1. {First Step}
{Details}

### 2. {Second Step}
{Details}

...

## Supporting Files
{List resources/ if any}

## Related Skills
{Skills this composes with}
```

**Allowed-tools guidelines:**
- Analysis: `Read, Grep, Glob`
- Execution: `Read, Bash, TaskCreate, TaskUpdate`
- Generation: `Read, Write, Grep, Glob`
- Complex: `Read, Write, Edit, Bash, Grep, Glob, TaskCreate, TaskUpdate`

### 5. Create Files

```bash
SKILL_NAME="{skill-name}"
mkdir -p ".claude/skills/${SKILL_NAME}/resources"
# Write SKILL.md and any supporting files
```

### 6. Verify

- [ ] SKILL.md has valid YAML frontmatter
- [ ] Description < 1024 chars
- [ ] Description includes "Use when" with 3+ scenarios
- [ ] Description includes "Make sure to use this skill whenever" (pushy tone)
- [ ] allowed-tools is minimal
- [ ] Instructions are numbered, clear, and explain "why" where non-obvious
- [ ] Supporting files documented with `## Supporting Files` if resources/ exists
- [ ] SKILL.md body < 500 lines (extract to resources/ if over)

### 7. Summary

```
SKILL CREATED: {skill-name}

Location: .claude/skills/{skill-name}/
Files: SKILL.md ({lines} lines), resources/ ({count} files)
Description: {description}
Tools: {allowed-tools}

Test by asking naturally: "{example prompt that should trigger this skill}"
```

## Templates

- **`templates/SKILL-template.md`** — Base SKILL.md structure
- **`templates/script-template.sh`** — Bash script template

## Related Skills

- **`docs-navigator`** — Access existing skill patterns
