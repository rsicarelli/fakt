---
name: skill-creator
description: Creates new Claude Code Skills following best practices from migration patterns. Use when creating new Skills, converting slash commands to Skills, scaffolding Skill structure, or when user mentions "create skill", "new skill", "migrate command", or "skill from scratch". Enforces trigger-rich descriptions, progressive disclosure, and model-agnostic design.
allowed-tools: [Read, Write, Bash, Grep, Glob]
---

# Skill Creator - Meta-Skill for Skill Development

Creates well-structured Claude Code Skills following Fakt migration patterns and Gemini research best practices.

## Core Mission

This meta-Skill scaffolds new Skills with:
- Trigger-rich descriptions (<1024 chars)
- Progressive disclosure structure (SKILL.md + resources/)
- Model-agnostic instructions
- Activation test prompts
- Migration pattern compliance

## Instructions

### 1. Gather Skill Requirements

**Ask clarifying questions:**

**Required information:**
- [ ] **Skill name** (kebab-case, descriptive)
- [ ] **Core purpose** (what problem does it solve?)
- [ ] **Trigger keywords** (how will users invoke it?)
- [ ] **Category** (core-workflows, validation, analysis, etc.)
- [ ] **Source** (migrating from slash command OR new from scratch)

**Optional information:**
- [ ] Required tools (Read, Bash, etc.)
- [ ] Supporting files needed (scripts, docs)
- [ ] Complexity level (simple/medium/complex)
- [ ] Dependencies on other Skills

**Example dialogue:**
```
Q: "What's the Skill name?"
A: "kotlin-api-consultant"

Q: "What problem does it solve?"
A: "Validates Kotlin compiler API usage against source code"

Q: "How might users invoke it?"
A: "validate API", "check Kotlin API", "consult compiler source"

Q: "Category?"
A: "analysis"

Q: "Migrating from slash command or creating new?"
A: "Migrating from /consult-kotlin-api"
```

### 2. Read Source Material (If Migrating)

**If migrating from slash command:**

```bash
# Read the original command
cat .claude/commands/{command-name}.md

# Extract:
- allowed-tools
- Core logic/instructions
- Dependencies (scripts, docs)
- Arguments pattern
```

**Analysis checklist:**
- [ ] What are the explicit arguments? (need context extraction)
- [ ] What tools does it use?
- [ ] Are there supporting files?
- [ ] Does it specify model? (will lose this)
- [ ] What's the core workflow?

### 3. Craft Trigger-Rich Description

**Use description template:**

```
{What it does in detail}. Use when {trigger scenario 1}, {trigger scenario 2}, {trigger scenario 3}, or when user mentions "{keyword 1}", "{keyword 2}", "{keyword 3}", or "{related concept}" context.
```

**Follow description best practices:**

1. **Be specific** (not "helps with X", but "does Y by doing Z")
2. **Third person** (never "I can" or "you can")
3. **List triggers** (all synonyms user might say)
4. **Max 1024 chars** (use them!)
5. **Include "use when"** clause

**Example:**
```yaml
# ❌ Bad (too vague):
description: Validates Kotlin APIs

# ✅ Good (trigger-rich):
description: Validates Kotlin compiler API usage against source code, checking for deprecations, compatibility, and correct patterns. Use when validating API calls, checking compiler API compatibility, debugging API issues, or when user mentions "validate API", "check Kotlin API", "IrFactory", "IrPluginContext", "compiler API", or API class names.
```

**Validation:**
```python
# Check length
assert len(description) <= 1024, "Description too long"
assert len(description) > 100, "Description too vague"

# Check structure
assert "use when" in description.lower(), "Missing 'use when' clause"
assert not any(p in description.lower() for p in ["i can", "you can"]), "Not third person"
```

### 4. Design Progressive Disclosure Structure

**Determine file structure:**

**Simple Skill** (no supporting files):
```
skill-name/
└── SKILL.md  (<300 lines, all logic here)
```

**Medium Skill** (some supporting files):
```
skill-name/
├── SKILL.md            # <500 lines core logic
└── resources/
    ├── reference.md    # Detailed reference (on-demand)
    └── examples.md     # Code examples (on-demand)
```

**Complex Skill** (scripts + docs):
```
skill-name/
├── SKILL.md            # <500 lines core logic
├── scripts/
│   ├── validate.sh     # Executable helper
│   └── analyze.py
└── resources/
    ├── patterns.md     # Loaded on-demand
    ├── troubleshooting.md
    └── metro-reference.md
```

**Rule**: If SKILL.md would exceed 500 lines, extract to resources/

### 5. Generate Skill Directory Structure

**Execute scaffolding:**

```bash
# Determine category
CATEGORY="{category}"  # core-workflows, validation, analysis, etc.
SKILL_NAME="{skill-name}"

# Create structure
mkdir -p "claude2/skills/${CATEGORY}/${SKILL_NAME}/{scripts,resources}"

echo "✅ Created: claude2/skills/${CATEGORY}/${SKILL_NAME}/"
```

### 6. Write SKILL.md from Template

**Use template from resources/skill-template.md:**

```yaml
---
name: {skill-name}
description: {trigger-rich description from step 3}
allowed-tools: [{minimal set of tools needed}]
---

# {Skill Title}

{One-line mission statement}

## Core Mission

{2-3 sentences explaining purpose and value}

## Instructions

### 1. {First Major Step}

{Detailed substeps}

### 2. {Second Major Step}

{Detailed substeps}

...

## Supporting Files

{List resources/ files and when they're loaded}

## Related Skills

{Skills this composes with}

## Best Practices

1. {Practice 1}
2. {Practice 2}
...

## Known Limitations

{Current constraints or Phase limitations}
```

**Key sections:**
- **Instructions**: Numbered, explicit, handle edge cases
- **Supporting Files**: Document progressive disclosure
- **Related Skills**: Enable composition
- **Best Practices**: Guide model behavior

### 7. Handle Argument-Based Conversion

**If migrating command with arguments:**

**Slash command pattern:**
```bash
/command-name <argument> [optional]
```

**Skill pattern (context extraction):**
```markdown
## Instructions

### 1. Extract {Argument} from Context

**Look for in user's messages:**
- Direct mention: "command for {value}"
- Contextual: "{value} isn't working"
- Pattern: "analyze {value}"

**If ambiguous or missing:**
- Ask: "Which {argument} would you like me to {action}?"
- Do NOT proceed with assumptions

### 2. Validate {Argument}

**Checks:**
- {Validation 1}
- {Validation 2}

**If invalid:**
- Report error clearly
- Suggest corrections
```

**Example (from kotlin-ir-debugger):**
```markdown
### 1. Identify Target Interface
- Extract interface name from user's recent messages
- Look for: "debug AsyncService", "analyze UserRepository IR"
- If missing, ask: "Which interface would you like me to debug?"
```

### 8. Define Minimal allowed-tools

**Be restrictive:**

```yaml
# ❌ Too permissive:
allowed-tools: [Read, Write, Edit, Bash, Grep, Glob, TodoWrite, Task, WebFetch]

# ✅ Minimal (only what's needed):
allowed-tools: [Read, Grep, Bash]
```

**Common tool sets:**

**Analysis Skills:**
```yaml
allowed-tools: [Read, Grep, Glob]
```

**Execution Skills:**
```yaml
allowed-tools: [Read, Bash, TodoWrite]
```

**Generation Skills:**
```yaml
allowed-tools: [Read, Write, Grep, Glob]
```

**Complex Workflows:**
```yaml
allowed-tools: [Read, Write, Bash, Grep, Glob, TodoWrite]
```

### 9. Create Activation Test Prompts

**Generate 5-10 test prompts:**

**Template:**
```markdown
## Skill: {skill-name}

### Test Prompts (Should Activate)

**Direct invocation:**
1. "{direct command phrase}"
2. "{variation 1}"

**Contextual triggers:**
3. "{contextual phrase with keywords}"
4. "{problem-solving context}"

**Synonyms:**
5. "{using synonym 1}"
6. "{using synonym 2}"

### Negative Tests (Should NOT Activate)

**Wrong domain:**
- "{phrase that should trigger different Skill}"
- "{general question not Skill-specific}"
```

**Add to claude2/SKILLS-ACTIVATION-TESTS.md**

### 10. Document in Migration Log

**Create entry in MIGRATION-PATTERNS.md:**

```markdown
## Skill: {skill-name}

**Migrated from**: {slash command or "created new"}
**Date**: {date}
**Complexity**: {simple/medium/complex}

**Key Decisions:**
- {Decision 1}
- {Decision 2}

**Challenges:**
- {Challenge 1 and solution}

**Learnings:**
- {Pattern learned}
```

### 11. Output Skill Summary

**Provide user with complete summary:**

```
✅ SKILL CREATED: {skill-name}

📁 Location:
claude2/skills/{category}/{skill-name}/

📝 Files Created:
- SKILL.md ({X} lines)
- resources/ ({count} files)
- scripts/ ({count} files)

🎯 Description (trigger keywords):
{description}

🛠️ Tools Allowed:
{allowed-tools}

🧪 Test Prompts:
1. "{test prompt 1}"
2. "{test prompt 2}"
...

📋 Next Steps:
1. Review SKILL.md for accuracy
2. Test activation with prompts
3. Refine description if activation fails
4. Document learnings in MIGRATION-PATTERNS.md

🔗 Related Skills:
- {related-skill-1}
- {related-skill-2}
```

## Supporting Files

**Templates:**
- `templates/SKILL-template.md` - Base SKILL.md structure
- `templates/resource-template.md` - Supporting file template
- `templates/script-template.sh` - Bash script template

**References:**
- `resources/description-best-practices.md` - Crafting trigger-rich descriptions
- `resources/progressive-disclosure-guide.md` - When to extract to resources/
- `resources/migration-checklist.md` - Step-by-step migration guide

## Related Skills

This Skill uses:
- **`fakt-docs-navigator`** - Access migration patterns and best practices
- **`behavior-analyzer-tester`** - Generate activation test cases

This Skill enables:
- All future Skill development
- Consistent Skill quality
- Fast migration velocity

## Best Practices

1. **Always ask for requirements first** - don't assume
2. **Validate description length** - max 1024 chars
3. **Test activation immediately** - create test prompts
4. **Document patterns** - update MIGRATION-PATTERNS.md
5. **Progressive disclosure** - extract resources/ if >500 lines

## Skill Creation Workflow

### Quick Skill (Simple, no dependencies)
```
User: "Create skill to validate import statements"
→ Gather: name, triggers, tools
→ Generate: Simple structure (SKILL.md only)
→ Test: 5 activation prompts
→ Time: ~30 minutes
```

### Standard Skill (Medium complexity)
```
User: "Migrate /analyze-interface-structure to Skill"
→ Read: Source slash command
→ Extract: Logic, tools, dependencies
→ Convert: Arguments to context extraction
→ Generate: SKILL.md + resources/
→ Test: 10 activation prompts
→ Time: ~2 hours
```

### Complex Skill (Scripts, multiple resources)
```
User: "Create multi-module validator Skill"
→ Gather: Full requirements
→ Design: Progressive disclosure structure
→ Generate: SKILL.md + scripts/ + resources/
→ Test: 15 activation prompts + edge cases
→ Document: Migration patterns
→ Time: ~4 hours
```

## Known Patterns

**From successful migrations:**

1. **IR Debugger Pattern** - Complex analysis with Metro validation
2. **Test Runner Pattern** - Execution + compliance validation
3. **Knowledge Navigator Pattern** - 80+ docs with intelligent routing
4. **Behavior Analyzer Pattern** - Deep analysis + generation

**Consult**: `resources/skill-archetypes.md` for pattern library

## Meta Note

This Skill is self-improving:
- Document new patterns as discovered
- Update templates based on learnings
- Refine description based on activation tests
- Iterate on best practices

**Current Status**: v1.0 - Created during Phase 1 migration (Day 1)
