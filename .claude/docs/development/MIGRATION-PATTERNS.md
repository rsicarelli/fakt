# Migration Patterns: Slash Commands → Skills API

> **Document Purpose**: Lessons learned and reusable patterns from migrating Fakt's slash commands to Skills
> **Status**: Living document - updated as migration progresses
> **Target Audience**: Future Skills developers, migration teams

## Key Learnings from Gemini Research

### 1. Progressive Disclosure is Your Superpower

**Problem Solved:**
Slash commands inject entire content into context (200+ lines)
Skills load metadata (~50 tokens), full content on-demand

**Pattern:**
```
Skill Directory:
├── SKILL.md           # <500 lines (core logic)
├── scripts/           # Executable helpers
│   └── *.sh
└── resources/         # Loaded on-demand
    ├── guide-1.md     # Progressive disclosure
    ├── guide-2.md
    └── patterns.md
```

**Benefit:**
- 80+ doc files available without token cost
- Only load what's needed when needed
- Context window efficiency: 30-50 tokens standby vs 2000+ tokens always

---

### 2. Description is 80% of Success

**Gemini Research Quote:**
> "The description field is the sole determinant for autonomous activation"

**Pattern: Trigger-Rich Descriptions**

❌ **Bad (too vague):**
```yaml
description: Debugs IR generation
```

✅ **Good (specific, trigger-rich):**
```yaml
description: Debugs Kotlin compiler Intermediate Representation (IR) generation for @Fake annotated interfaces. Use when analyzing IR, inspecting compiler output, debugging IR generation issues, troubleshooting interface generation, or when user mentions interface names with "debug", "IR", "intermediate representation", or "compiler generation" context.
```

**Key elements:**
1. **What it does** - "Debugs Kotlin compiler IR generation"
2. **When to use** - "Use when analyzing IR, inspecting..."
3. **Trigger keywords** - List all synonyms/variations users might say
4. **Third person** - Never "I can" or "You can", always descriptive

**Max length**: 1024 chars (use them all!)

---

### 3. Argument-Based Commands Need Context Instructions

**Challenge:**
Slash commands: `/debug-ir-generation <interface_name>`
Skills: No direct argument passing

**Pattern: Conversational Context Extraction**

```markdown
## Instructions

### 1. Identify Target Interface
- Extract interface name from user's recent messages
- Look for patterns: "debug AsyncService", "analyze UserRepository IR"
- If ambiguous or missing, ask: "Which interface would you like me to debug?"

### 2. Validate Before Proceeding
- Check interface exists with @Fake annotation
- Verify it's actually an interface (not class)
- Confirm correct module/source set

### 3. Execute With Extracted Context
- Use extracted name as argument to script/logic
- Handle edge cases gracefully
```

**Key insight**: Trust the model to extract context from conversation. Guide it with clear instructions on what to look for and how to handle ambiguity.

---

### 4. Model Field is Gone - Accept It

**Breaking Change:**
```yaml
# Slash command supported:
model: claude-sonnet-4-20250514

# Skills DO NOT support model specification
# ❌ This field is ignored/invalid
```

**Mitigation Strategies:**

**Strategy 1: Design Model-Agnostic**
- Write instructions that work on all models
- Test with Sonnet 4 AND Haiku
- Avoid model-specific capabilities

**Strategy 2: Session-Level Control**
```markdown
# In CLAUDE.md or project docs
For complex IR debugging, ensure you're using Claude Sonnet 4 or higher.
```

**Strategy 3: Hybrid Approach (Temporary)**
```markdown
# Keep critical slash commands that need specific models
# Use Skills for model-agnostic workflows
```

---

### 5. allowed-tools Pattern Preservation

**Good news**: `allowed-tools` migrates directly!

**Pattern:**
```yaml
# Slash command:
allowed-tools: Read, Grep, Glob, Bash(./gradlew:*), TodoWrite, Task

# Skill (exact same):
allowed-tools: [Read, Grep, Glob, Bash, TodoWrite]
```

**Benefit**: Fine-grained control over tool access without user permission prompts

**Best practice**: Be restrictive - only allow tools actually needed

---

### 6. Supporting Files = Progressive Disclosure Gold

**Pattern: Separate Concerns**

**Instead of monolithic SKILL.md:**
```markdown
SKILL.md (500+ lines of everything)
```

**Use this structure:**
```
kotlin-ir-debugger/
├── SKILL.md                    # Core logic (300 lines)
├── resources/
│   ├── interface-analysis-patterns.md    # Loaded when analyzing
│   ├── metro-patterns-reference.md       # Loaded for Metro questions
│   ├── generic-type-handling.md          # Loaded for generic issues
│   └── troubleshooting-guide.md          # Loaded when errors occur
```

**Instructions in SKILL.md:**
```markdown
### 4. Analyze Interface Structure
For detailed analysis patterns → consult `resources/interface-analysis-patterns.md`

### 5. Check Metro Alignment
For Metro patterns → consult `resources/metro-patterns-reference.md`
```

**Model loads files on-demand based on context**

---

### 7. Knowledge Base Pattern (80+ Docs)

**Challenge**: How to provide access to 80+ documentation files without loading all into context?

**Solution: Navigator Skill**

```yaml
name: fakt-docs-navigator
description: Intelligent navigator for 80+ docs covering architecture, testing, Metro patterns, generics...
```

**Pattern:**
1. **Skill = Librarian, not Library**
   - Doesn't contain docs
   - Knows HOW to find docs
   - Loads specific doc when needed

2. **Information Architecture Matters**
   - Organize docs in logical subdirectories
   - Use descriptive file names
   - Model navigates by directory/file structure

3. **Symlink to Existing Docs**
   ```bash
   cd resources/
   ln -s ../../../.claude/docs docs
   ```

4. **Instructions = Navigation Logic**
   ```markdown
   ## Instructions

   ### 1. Classify User Query
   - Testing? → validation/
   - Metro? → development/metro-alignment.md
   - Generics? → implementation/generics/

   ### 2. Navigate to Relevant Doc
   ### 3. Extract Specific Section
   ### 4. Cross-Reference Related Topics
   ```

**Result**: All 80+ docs accessible, but only loaded as needed

---

### 8. Composability Pattern

**Gemini insight**: Skills can chain together autonomously

**Pattern: Single Responsibility Skills**

❌ **Anti-pattern (monolithic):**
```
mega-validator Skill:
- Validates Metro patterns
- Validates compilation
- Validates types
- Validates tests
- ...everything
```

✅ **Best practice (composable):**
```
metro-pattern-validator:
  - Single responsibility: Metro alignment

compilation-validator:
  - Single responsibility: Code compilation

type-safety-validator:
  - Single responsibility: Type safety
```

**Benefit**: Model can compose these autonomously

**Example prompt:**
```
User: "Validate the IR generator completely"

Claude autonomously:
1. Activates metro-pattern-validator
2. Then compilation-validator
3. Then type-safety-validator
4. Synthesizes results
```

---

### 9. Error Handling & User Confirmation

**Pattern: Explicit Confirmation for Destructive Ops**

```markdown
## Instructions

### [For destructive operations]

1. First, analyze request and create plan
2. Write plan to temporary file
3. Display plan to user
4. Ask: "This is destructive. Respond 'yes' to proceed."
5. **DO NOT** proceed unless user responds exactly "yes"
6. If not "yes", abort and await instructions
```

**Script pattern:**
```python
# In supporting scripts
if not user_confirmed:
    print("⚠️ Operation not confirmed. Aborting.")
    sys.exit(0)
```

**Defense-in-depth mindset**: Autonomous agent + filesystem access = potential damage. Guard aggressively.

---

### 10. Activation Latency Trade-Off

**Reality Check:**
- Slash commands: <1s (direct injection)
- Skills: 3-5s (reasoning + tool call)

**Acceptance Pattern:**
```
User expectation management:
- Document the latency in onboarding
- Frame as "AI is thinking about best approach"
- Provide visual feedback (in terminal)
```

**Mitigation:**
- Write clear, unambiguous descriptions (reduces reasoning time)
- Keep SKILL.md concise (faster to load)
- Use supporting files for details (lazy load)

**When to keep slash command:**
- High-frequency, time-sensitive operations
- Simple text snippets
- Deterministic, no-thinking-required tasks

---

## Conversion Checklist

For each slash command → Skill migration:

### Pre-Migration
- [ ] Read slash command completely
- [ ] Identify core intent (what problem does it solve?)
- [ ] List trigger keywords/phrases users might say
- [ ] Identify all dependencies (scripts, docs, tools)
- [ ] Note if it uses `model` field (will lose this)

### Skill Creation
- [ ] Create directory: `claude2/skills/{category}/{skill-name}/`
- [ ] Write SKILL.md (<500 lines core logic)
- [ ] Extract supporting files to `resources/`
- [ ] Move scripts to `scripts/`
- [ ] Define `allowed-tools` (minimal set needed)
- [ ] Craft trigger-rich description (use full 1024 chars)

### Testing
- [ ] Create 5-10 test prompts (see SKILLS-ACTIVATION-TESTS.md)
- [ ] Test autonomous activation
- [ ] Verify Skill follows instructions correctly
- [ ] Check latency (<5s acceptable, <10s limit)
- [ ] Refine description if activation fails

### Documentation
- [ ] Document in migration log
- [ ] Update SKILLS-ACTIVATION-TESTS.md
- [ ] Note any issues/learnings
- [ ] Update this MIGRATION-PATTERNS.md if new pattern discovered

---

## Common Pitfalls

### Pitfall 1: Description Too Generic
**Symptom**: Skill rarely activates
**Cause**: "Helps with testing" - too vague
**Fix**: Add specific triggers: "run tests", "BDD", "GIVEN-WHEN-THEN", "coverage"

### Pitfall 2: SKILL.md Too Large
**Symptom**: Slow activation, context bloat
**Cause**: Putting all documentation in SKILL.md
**Fix**: Extract to resources/, load on-demand

### Pitfall 3: Missing User Confirmation
**Symptom**: Accidental destructive operations
**Cause**: Skill proceeds without explicit approval
**Fix**: Add confirmation step in instructions

### Pitfall 4: Unclear Instructions
**Symptom**: Model doesn't follow workflow correctly
**Cause**: Ambiguous or missing steps
**Fix**: Be explicit, use numbered steps, handle edge cases

### Pitfall 5: Not Testing Activation
**Symptom**: Skill exists but never activates
**Cause**: Skipped activation test suite
**Fix**: Create test prompts, iterate on description

---

## Migration Velocity Lessons

**Week 1 Goal: 3 Skills (PoC)**
- kotlin-ir-debugger (most critical)
- bdd-test-runner (high usage)
- fakt-docs-navigator (enables others)

**Pattern Recognition:** After first 3 Skills, subsequent ones are faster (established patterns)

**Time Estimates per Skill:**
- Simple (no arguments, few tools): 1-2 hours
- Medium (arguments, multiple tools): 2-4 hours
- Complex (many dependencies, large docs): 4-8 hours

**Batch Processing:** Group similar Skills to leverage pattern reuse

---

## Tools & Automation

### Skill Scaffolding Script
```bash
#!/bin/bash
# create-skill.sh <category> <skill-name>

CATEGORY=$1
SKILL_NAME=$2

mkdir -p "claude2/skills/${CATEGORY}/${SKILL_NAME}/{scripts,resources}"
cat > "claude2/skills/${CATEGORY}/${SKILL_NAME}/SKILL.md" <<EOF
---
name: ${SKILL_NAME}
description: [TODO: Fill with trigger-rich description]
allowed-tools: [Read, Grep, Glob]
---

# ${SKILL_NAME}

[TODO: Core instructions]

## Instructions

### 1. [TODO: First step]
EOF

echo "✅ Skill scaffolded: claude2/skills/${CATEGORY}/${SKILL_NAME}/"
```

### Description Validation
```python
# validate-description.py
def validate_skill_description(description):
    checks = {
        "length": len(description) <= 1024,
        "third_person": not any(word in description.lower() for word in ["i can", "you can"]),
        "has_use_when": "use when" in description.lower(),
        "specific": len(description) > 100  # Not too vague
    }
    return all(checks.values()), checks
```

---

## Next Patterns to Discover

As migration continues, document:
- [ ] Multi-Skill pipeline patterns
- [ ] Skill-to-Skill communication patterns
- [ ] State management across Skill invocations
- [ ] Error recovery patterns
- [ ] Performance optimization patterns

---

## References

- Gemini Deep Research: `/Users/rsicarelli/Downloads/Claude Skills API Migration Research.md`
- Official Skills Docs: https://docs.claude.com/en/docs/claude-code/skills
- Open Source Examples: obra/superpowers, ruvnet/claude-flow
- Fakt Slash Commands: `.claude/commands/README.md`
