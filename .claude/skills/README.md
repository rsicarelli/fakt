# Fakt Compiler Plugin - Skills System

> **Auto-activating specialized skills for Kotlin compiler plugin development**
> **Location**: `.claude/skills/`
> **Auto-Activation**: Configured via `skill-rules.json`

## 🎯 What are Skills?

Skills are specialized, reusable knowledge modules that provide deep expertise in specific domains of Fakt compiler plugin development. Unlike slash commands (which execute specific workflows), skills provide **comprehensive guidance** and can be **automatically suggested** based on your prompts and file context.

### Skills vs. Slash Commands

| Aspect | Skills | Slash Commands |
|--------|--------|----------------|
| **Purpose** | Domain expertise & guidance | Workflow execution |
| **Activation** | Auto-suggest based on context | Manual invocation (`/command`) |
| **Scope** | Broad, reusable knowledge | Specific, narrow workflow |
| **Integration** | Can be used by multiple commands | Standalone |
| **Example** | `kotlin-api-consultant` (API validation) | `/consult-kotlin-api <class>` (query specific API) |

**Best Practice**: Skills often **power** slash commands. Commands orchestrate workflows, skills provide the expertise.

---

## 🚀 Auto-Activation System

Skills automatically suggest themselves based on:

1. **Keyword Matching**: Your prompt contains specific keywords (e.g., "Kotlin API", "run tests", "generics")
2. **Intent Pattern Matching**: Regex patterns detect your intentions (e.g., "(check|validate).*?compilation")
3. **File Context**: Editing specific files triggers relevant skills (e.g., `*IrGenerator*.kt` → `kotlin-ir-debugger`)
4. **Priority Levels**: Critical skills always activate, low-priority only for explicit matches

### How Auto-Activation Works

```
┌─────────────────────────────────────────────────────────┐
│  1. USER PROMPT                                         │
│     "Check if IrGenerationExtension API changed"        │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  2. HOOK: skill-activation-prompt.sh                    │
│     • Reads .claude/skills/skill-rules.json             │
│     • Matches keywords: "IrGenerationExtension", "API"  │
│     • Matches intent: "(check|validate).*?API"          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  3. SKILL SUGGESTION (injected into Claude's context)   │
│                                                          │
│  🎯 FAKT SKILL ACTIVATION CHECK                         │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━               │
│  📚 RECOMMENDED SKILLS:                                 │
│    → kotlin-api-consultant (high priority)              │
│                                                          │
│  💡 TIP: Use Skill tool before proceeding               │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  4. CLAUDE USES SKILL                                   │
│     Loads kotlin-api-consultant skill knowledge         │
│     Provides API validation expertise                   │
└─────────────────────────────────────────────────────────┘
```

### Configuration: `skill-rules.json`

All trigger patterns are defined in `.claude/skills/skill-rules.json`:

```json
{
  "skills": {
    "kotlin-api-consultant": {
      "type": "domain",
      "enforcement": "suggest",
      "priority": "high",
      "promptTriggers": {
        "keywords": [
          "Kotlin API",
          "compiler API",
          "IrGenerationExtension",
          "API validation"
        ],
        "intentPatterns": [
          "(check|validate|verify).*?API",
        ]
      }
    }
  }
}
```

**Customization**: Edit `skill-rules.json` to add new triggers, adjust priorities, or modify keywords for your workflow.

---

## 📚 Available Skills (13 Total)

### 🔍 Analysis Skills (4)

#### 1. `kotlin-api-consultant`
**Purpose**: Queries Kotlin compiler source for API validation, compatibility checks, and architectural pattern validation

**Auto-Activates On**:
- Keywords: "Kotlin API", "compiler API", "IrGenerationExtension", "IrPluginContext", "API validation"
- Intent: "(check|validate).*?API"

**Use Cases**:
- Validating Kotlin compiler APIs before use
- Checking for breaking changes across Kotlin versions
- Finding usage patterns for specific APIs
- Detecting deprecated or @UnsafeApi markers

**Example**:
```
User: "Check if IrTypeParameter API is stable"
→ Auto-activates kotlin-api-consultant
→ Searches Kotlin source, checks annotations, reports compiler plugin usage patterns
```

---

#### 2. `generic-scoping-analyzer`
**Purpose**: Analyzes generic type parameter scoping challenges (class-level vs method-level)

**Auto-Activates On**:
- Keywords: "generic", "generics", "type parameter", "IrTypeParameter", "scoping"
- Intent: "(analyze|debug).*?generic", "Phase 2.*?generic"

**Use Cases**:
- Understanding class-level vs method-level generic differences
- Planning Phase 2A/2B generic implementation
- Debugging generic type resolution issues
- Analyzing variance and bounds

**Resources**: `resources/phase2a-solution-patterns.md`, `resources/scoping-analysis-guide.md`

---

#### 3. `compilation-error-analyzer`
**Purpose**: Systematic compilation error diagnosis and resolution

**Auto-Activates On**:
- Keywords: "compilation error", "compile error", "unresolved reference", "type mismatch"
- Intent: "(analyze|debug|fix).*?compilation.*?error"

**Use Cases**:
- Diagnosing why generated code won't compile
- Categorizing error types (missing imports, type mismatches, etc.)
- Providing specific fixes for common error patterns
- Tracking error patterns across interfaces

**Resources**: `resources/troubleshooting-workflows.md`

---

#### 4. `interface-analyzer`
**Purpose**: Deep structural analysis of @Fake annotated interfaces

**Auto-Activates On**:
- Keywords: "analyze interface", "interface structure", "@Fake interface"
- Intent: "analyze.*?interface.*?structure"

**Use Cases**:
- Examining method signatures and property definitions
- Checking generic type parameters complexity
- Assessing generation strategy recommendations
- Understanding suspend function patterns

---

### ⚙️ Core Workflows (3)

#### 5. `bdd-test-runner`
**Purpose**: Executes BDD-compliant GIVEN-WHEN-THEN tests with vanilla JUnit5

**Auto-Activates On**:
- Keywords: "run tests", "execute tests", "BDD", "GIVEN-WHEN-THEN", "test coverage"
- Intent: "(run|execute).*?test", "(check|validate).*?test.*?compliance"

**Use Cases**:
- Running tests with compliance validation (no "should" pattern)
- Coverage analysis and gap identification
- Production testing pattern verification
- Test failure diagnostics

**Resources**: `resources/testing-guidelines-reference.md`, `resources/metro-testing-patterns.md`

---

#### 6. `kotlin-ir-debugger`
**Purpose**: Step-by-step IR generation debugging and pattern validation

**Auto-Activates On**:
- Keywords: "IR generation", "debug IR", "IR nodes", "IR debugger"
- Intent: "debug.*?IR.*?generation", "IR.*?(issue|problem)"

**Use Cases**:
- Debugging IR generation for specific interfaces
- Inspecting IR node structure
- Validating IR generation patterns
- Troubleshooting IrFactory usage

**Priority**: **CRITICAL** (always activates for IR debugging)

---

#### 7. `behavior-analyzer-tester`
**Purpose**: Deep behavior analysis and comprehensive unit test generation

**Auto-Activates On**:
- Keywords: "generate tests", "create tests", "test generation", "analyze behavior"
- Intent: "generate.*?test", "(create|add).*?test.*?for"

**Use Cases**:
- Generating GIVEN-WHEN-THEN tests for new features
- Analyzing code behavior for test coverage
- Creating unit tests following project standards
- Improving test coverage systematically

---

### ✅ Validation (3)

#### 8. `compilation-validator`
**Purpose**: Production-grade compilation validation ensuring zero errors

**Auto-Activates On**:
- Keywords: "validate compilation", "check compilation", "generated code errors", "type safety"
- Intent: "(validate|verify|check).*?compilation", "(does|will).*?compile"

**Use Cases**:
- Multi-stage compilation validation (generation → structure → type-check)
- Type safety verification
- Smart defaults validation
- Configuration DSL type checking

**Priority**: **CRITICAL** (compilation must always work)

**Resources**: `resources/compilation-patterns.md`, `resources/type-safety-guide.md`

---

#### 9. `compiler-architecture-validator`
**Purpose**: Validates Fakt implementation follows compiler plugin best practices and architectural patterns

**Auto-Activates On**:
- Keywords: "architecture", "validate architecture", "compiler plugin patterns", "two-phase compilation"
- Intent: "(validate|verify|check).*?(architecture|pattern|structure)"

**Use Cases**:
- Verifying two-phase FIR → IR compilation structure
- Checking CompilerPluginRegistrar patterns
- Validating context-driven generation
- Ensuring IrGenerationExtension patterns

**Priority**: **HIGH** (architecture quality is critical)

---

#### 10. `implementation-tracker`
**Purpose**: Monitors implementation progress and validates phase completion

**Auto-Activates On**:
- Keywords: "implementation status", "check status", "progress", "phase status"
- Intent: "(check|show).*?status", "phase.*?completion"

**Use Cases**:
- Checking current implementation phase and status
- Monitoring test pass rates and metrics
- Validating milestone completion
- Identifying blockers and gaps

---

### 🛠️ Development & Knowledge (3)

#### 11. `skill-creator`
**Purpose**: Meta-skill for creating new Claude Code skills

**Auto-Activates On**:
- Keywords: "create skill", "new skill", "add skill", "skill development"
- Intent: "(create|add|build).*?skill"

**Use Cases**:
- Creating new skills following best practices
- Converting slash commands to skills
- Scaffolding skill structure with templates
- Following progressive disclosure patterns

**Priority**: **LOW** (only for explicit skill creation requests)

---

#### 12. `public-docs-navigator`
**Purpose**: Navigate Fakt's public MkDocs documentation site for user guides and feature documentation

**Auto-Activates On**:
- Keywords: "documentation", "docs", "public docs", "mkdocs", "getting started", "multi-module", "codegen strategy"
- Intent: "(show|find|where).*?doc", "how.*?use.*?fakt", "(explain|show).*?(codegen|multi-module)"

**Use Cases**:
- Finding user-facing documentation (installation, quick start, guides)
- Accessing multi-module setup tutorials and troubleshooting
- Referencing code generation strategy and architecture decisions
- Learning about fakes vs mocks performance analysis

**Documentation Coverage**:
- 29 files in docs/ (public MkDocs site)
- Introduction (5 files): overview, why-fakt, installation, quick-start, features
- Usage (5 files): basic-usage, suspend-functions, generics, properties, call-tracking
- Guides (3 files): testing-patterns, migration, performance
- Multi-Module (6 files): comprehensive KMP multi-module documentation
- Reference (6 files): API, codegen-strategy, fakes-over-mocks, configuration, compatibility, limitations

**Priority**: **MEDIUM** (proactively helps users find relevant documentation)

---

#### 13. `fakt-docs-navigator`
**Purpose**: Navigate Fakt's internal contributor documentation for compiler architecture and implementation details

**Auto-Activates On**:
- Keywords: "internal docs", "contributor docs", ".claude/docs", "testing guidelines", "Metro alignment", "compiler architecture"
- Intent: "(find|where|show).*?(internal|contributor).*?doc", "(testing guidelines|Metro alignment)"

**Use Cases**:
- Accessing internal testing guidelines and BDD standards
- Finding Metro alignment patterns and architectural decisions
- Navigating implementation roadmaps and phase documentation
- Referencing FIR/IR implementation details and codegen v2 architecture

**Documentation Coverage**:
- 70+ files across 3 main directories in .claude/docs/ (internal contributor docs)
- development/validation/ (4 files): testing-guidelines.md (THE ABSOLUTE STANDARD), compilation-validation.md
- development/ (12 files): metro-alignment.md, kotlin-api-reference.md, decision-tree.md, contexts/, examples/, future/
- implementation/ (7 subdirs): architecture/, codegen-v2/, generics/, patterns/, multi-module/, source_sets/, api/
- troubleshooting/ (1 file): common-issues.md

**Priority**: **MEDIUM** (helps contributors navigate internal documentation)

---

## 📖 Using Skills

### Auto-Activation (Recommended)

Simply write natural prompts and skills will auto-suggest:

```
✅ "Run tests and check BDD compliance"
→ Auto-activates: bdd-test-runner

✅ "Debug IR generation for UserService"
→ Auto-activates: kotlin-ir-debugger

✅ "Check if IrFactory API changed"
→ Auto-activates: kotlin-api-consultant
```

### Manual Invocation

Ask Claude to apply the skill guidelines:

```
"Apply git-commit-guardian guidelines"
"Use kotlin-api-consultant skill"
"Follow the bdd-test-runner skill"
```

Claude will automatically read the `.claude/skills/{category}/{skill-name}/SKILL.md` file and apply the guidelines.

**Important**: These are guideline-based skills, NOT registered slash commands or Skill tool invocations. They work by Claude reading and following the SKILL.md documentation.

---

## 🎨 Skill Structure

Each skill follows this structure:

```
.claude/skills/{category}/{skill-name}/
├── SKILL.md              # Main skill file (<500 lines recommended)
└── resources/            # Progressive disclosure (optional)
    ├── topic-1.md
    ├── topic-2.md
    └── complete-examples.md
```

### SKILL.md Format

```markdown
---
name: skill-name
description: Rich description with ALL trigger keywords. Use when...
allowed-tools: [Read, Grep, Glob, Bash, WebFetch]
---

# Skill Title

Brief overview

## Core Mission
What this skill does

## Instructions
Step-by-step guidance

## Supporting Files
- resources/topic-1.md - Details on topic 1
- resources/topic-2.md - Details on topic 2

## Related Skills
- other-skill-name - Composes with this skill

## Best Practices
Key recommendations
```

### Progressive Disclosure (500-Line Rule)

**Anthropic Best Practice**: Keep SKILL.md under 500 lines, use resources for deep dives.

**Benefits**:
- Stays under Claude's context limits
- Loads incrementally as needed
- Easier to maintain and navigate
- Faster initial loading

**Example**:
```
kotlin-api-consultant/
├── SKILL.md (411 lines - overview + navigation)
└── resources/
    ├── api-lookup-patterns.md (detailed search strategies)
    ├── compiler-api-usage.md (comprehensive examples)
    └── breaking-changes-catalog.md (version-specific changes)
```

---

## ⚙️ Configuration & Customization

### Editing Triggers

Edit `.claude/skills/skill-rules.json` to customize when skills activate:

```json
{
  "skills": {
    "your-skill-name": {
      "type": "domain",              // or "guardrail"
      "enforcement": "suggest",       // or "block", "warn"
      "priority": "high",             // critical | high | medium | low
      "promptTriggers": {
        "keywords": [
          "keyword1",
          "keyword2"
        ],
        "intentPatterns": [
          "(action|verb).*?pattern",
          "regex.*?pattern"
        ]
      },
      "fileTriggers": {
        "pathPatterns": [
          "compiler/src/**/*.kt",
          "**/*Generator*.kt"
        ],
        "contentPatterns": [
          "import org\\.jetbrains\\.kotlin\\.ir\\."
        ]
      }
    }
  }
}
```

### Adding New Skills

1. **Create skill directory**: `.claude/skills/{category}/{skill-name}/`
2. **Write SKILL.md**: Follow template format
3. **Add to skill-rules.json**: Define triggers
4. **Test activation**: Try prompts that should trigger it
5. **Refine triggers**: Adjust based on actual usage

See `skill-creator` skill for detailed guidance.

---

## 🔧 Troubleshooting

### Skill Not Activating

1. **Check keywords**: Does your prompt contain trigger keywords?
2. **Check intent patterns**: Does regex match your prompt?
3. **Check priority**: Low-priority skills only activate for explicit matches
4. **Check skill-rules.json**: Is skill properly registered?

### Hook Not Running

1. **Check .claude/settings.json**: Is UserPromptSubmit hook registered?
2. **Check hook permissions**: Is .sh file executable? (`chmod +x`)
3. **Check dependencies**: Run `cd .claude/hooks && npm install`
4. **Check logs**: Look for hook errors in Claude Code output

### Skill Not Loading

1. **Check SKILL.md format**: Valid YAML frontmatter?
2. **Check file location**: Must be in `.claude/skills/{category}/{name}/SKILL.md`
3. **Check allowed-tools**: Proper array format `[Tool1, Tool2]`

---

## 📊 Skills by Priority

### Critical (Always Activate)
- `kotlin-ir-debugger` - IR generation is core functionality
- `compilation-validator` - Generated code must compile
- `compiler-architecture-validator` - Architecture quality is critical

### High (Activate for Most Matches)
- `kotlin-api-consultant` - API validation is frequent
- `bdd-test-runner` - Testing is constant
- `generic-scoping-analyzer` - Generics are major challenge
- `compilation-error-analyzer` - Errors need systematic diagnosis

### Medium (Activate for Clear Matches)
- `interface-analyzer` - Structural analysis is occasional
- `behavior-analyzer-tester` - Test generation is periodic
- `implementation-tracker` - Status checks are intermittent
- `public-docs-navigator` - Proactively helps with user documentation
- `fakt-docs-navigator` - Helps contributors navigate internal docs

### Low (Activate Only for Explicit Requests)
- `skill-creator` - Skill creation is rare

---

## 🔗 Related Documentation

- [Commands README](../commands/README.md) - Slash commands that use skills
- [CLAUDE.md](../../CLAUDE.md) - Main project documentation with skills overview
- [skill-rules.json](./skill-rules.json) - Trigger configuration
- [Testing Guidelines](../docs/development/validation/testing-guidelines.md) - BDD standards

---

**Last Updated**: January 2025
**Total Skills**: 13 (4 Analysis + 3 Core Workflows + 3 Validation + 3 Development/Knowledge)
**Auto-Activation**: Enabled via UserPromptSubmit hook
**Configuration**: `.claude/skills/skill-rules.json`
