---
name: fakt-docs-navigator
description: Navigate Fakt's internal contributor documentation (.claude/docs/, 66 files across 18 directories) covering compiler plugin architecture, testing guidelines, Metro patterns, generic type handling, implementation roadmaps, FIR/IR design, and troubleshooting. Use when discussing internal implementation, compiler architecture, Metro alignment, testing standards, codegen v2, or contributor-level technical details. For public user documentation, use public-docs-navigator instead.
allowed-tools: [Read, Grep, Glob]
---

# Fakt Internal Documentation Navigator

Navigate Fakt's internal contributor documentation for deep technical understanding of compiler plugin architecture, implementation strategies, and development guidelines.

## Core Mission

Provide **progressive disclosure** access to Fakt's internal contributor documentation (66 files, 18 directories) without overwhelming context. This skill is for **contributors and maintainers** working on Fakt's internals. For **public user documentation**, use `public-docs-navigator` instead.

## Documentation Structure

Real location: `/Users/rsicarelli/Workspace/Personal/ktfakes-prototype/ktfake/.claude/docs/`

The knowledge base is organized into 18 directories (66 files total):

```
.claude/docs/                              # Internal contributor documentation
├── README.md                              # Master index
├── validation/                            # 4 files - Testing standards
│   ├── testing-guidelines.md             # THE ABSOLUTE STANDARD ⭐
│   ├── compilation-validation.md
│   ├── type-safety-validation.md
│   └── SKILLS-ACTIVATION-TESTS.md
├── development/                           # 5 files - Technical guides
│   ├── metro-alignment.md                # Metro patterns ⭐
│   ├── kotlin-api-reference.md
│   ├── decision-tree.md
│   ├── metro-fir-ir-specifications.md
│   └── MIGRATION-PATTERNS.md
├── implementation/                        # 4 files + generics/
│   ├── roadmap.md
│   ├── fir-full-support-plan.md
│   ├── RESUME-FIR-IMPLEMENTATION.md
│   └── generics/                          # 2 files
│       ├── test-matrix.md
│       └── technical-reference.md
├── architecture/                          # 4 files - System design
│   ├── unified-ir-native.md
│   ├── code-generation-strategies.md     # Codegen approach
│   ├── fir-ir-separation-decision.md
│   └── code-generation-approach.md
├── codegen-v2/                            # 2 files - New codegen
│   ├── README.md
│   └── ADR.md                            # Architecture Decision Record
├── kmp-multi-module/                      # 7 files - Multi-module (outdated approach)
│   ├── README.md                         # Deprecation notice
│   ├── ARCHITECTURE-DECISION.md
│   ├── COMPARISON-MATRIX.md
│   ├── CONVENTION-PLUGIN-BLUEPRINT.md
│   ├── FAQ.md
│   ├── IMPLEMENTATION-ROADMAP.md
│   └── TECHNICAL-REFERENCE.md
├── multi-module/                          # 1 file - Actual implementation
│   └── collector-task-implementation.md  # FakeCollectorTask details
├── patterns/                              # 3 files - Code patterns
│   ├── basic-fake-generation.md
│   ├── suspend-function-handling.md
│   └── complex-generics-strategy.md
├── analysis/                              # 2 files - Deep analysis
│   ├── test-coverage-analysis.md
│   └── generic-scoping-analysis.md       # ⭐ Core Phase 2 challenge
├── source_sets/                           # 4 files - KMP source sets
│   ├── README.md
│   ├── ARCHITECTURE.md
│   ├── CODE-PATTERNS.md
│   └── API-REFERENCE.md
├── api/                                   # 3 files - API specs
│   ├── specifications.md
│   ├── annotations.md
│   └── generated-api.md
├── troubleshooting/                       # 1 file
│   └── common-issues.md
├── contexts/                              # 3 files - User contexts
│   ├── kotlin-developers.md
│   ├── enterprise-teams.md
│   └── tdd-practitioners.md
├── examples/                              # 2 files - Working examples
│   ├── quick-start-demo.md
│   └── working-examples.md
├── research/                              # 1 file
│   └── gemini-deep-research-prompt-fake-testing-issues.md
├── future/                                # 1 file
│   └── explicit-backing-fields-refactoring.md
└── [15 root-level files]                 # Legacy/general docs
```

**Total**: 66 files across 18 directories

## Instructions

### 1. Classify User Query

**Identify topic category from user's question:**

**Testing & Quality:**
- Keywords: "test", "BDD", "GIVEN-WHEN-THEN", "coverage", "validation"
- → Navigate to `.claude/docs/validation/`

**Metro Patterns:**
- Keywords: "Metro", "alignment", "architectural patterns", "IrGenerationExtension"
- → Navigate to `.claude/docs/development/metro-alignment.md`

**Generic Types:**
- Keywords: "generics", "type parameters", "T", "class-level", "method-level"
- → Navigate to `.claude/docs/implementation/generics/` or `.claude/docs/analysis/generic-scoping-analysis.md`

**Implementation Status:**
- Keywords: "status", "phase", "roadmap", "progress", "what's complete"
- → Navigate to `.claude/docs/implementation/`

**Architecture & Codegen:**
- Keywords: "architecture", "how does it work", "design", "FIR", "IR", "codegen", "generation strategy"
- → Navigate to `.claude/docs/architecture/` or `.claude/docs/codegen-v2/`

**Troubleshooting:**
- Keywords: "error", "issue", "problem", "not working", "how to fix"
- → Navigate to `.claude/docs/troubleshooting/common-issues.md`

**Multi-module/KMP:**
- Keywords: "multi-module", "KMP", "commonMain", "source set", "modules", "collector task"
- → Navigate to `.claude/docs/multi-module/` (actual implementation) or `.claude/docs/kmp-multi-module/` (outdated design docs)

### 2. Navigate to Relevant Documentation

**Use Read and Grep tools with real .claude/docs/ paths:**

```bash
# Read specific doc
Read .claude/docs/validation/testing-guidelines.md
Read .claude/docs/development/metro-alignment.md

# List available docs in category
Glob ".claude/docs/{category}/*.md"

# Search for specific topic
Grep "{keyword}" .claude/docs/{category}/ -r --output_mode=files_with_matches
Grep "{keyword}" .claude/docs/ -r --output_mode=content --head_limit=30
```

**Progressive disclosure strategy:**
1. Identify the most relevant single file
2. Read that file first using Read tool
3. If more detail needed, follow references within
4. Load additional files only as necessary

### 3. Extract and Synthesize Information

**When reading documentation:**

1. **Focus on user's specific question** - don't dump entire document
2. **Extract relevant sections** - quote specific parts
3. **Provide context** - explain why this doc is authoritative
4. **Reference location** - tell user where to find more

**Example response format:**
```
Based on `.claude/docs/validation/testing-guidelines.md` (THE ABSOLUTE STANDARD):

[Relevant extracted content]

📚 Full reference: `.claude/docs/validation/testing-guidelines.md`
```

### 4. Handle Common Query Patterns

**"How do I test X?"**
→ Read `.claude/docs/validation/testing-guidelines.md`
→ Extract GIVEN-WHEN-THEN pattern for X
→ Provide test template

**"What's the Metro pattern for Y?"**
→ Read `.claude/docs/development/metro-alignment.md`
→ Compare Metro vs Fakt approach
→ Show code examples

**"What's the status of generics?"**
→ Read `.claude/docs/implementation/generics/technical-reference.md`
→ Read `.claude/docs/analysis/generic-scoping-analysis.md`
→ Summarize current phase and limitations

**"Why is compilation failing?"**
→ Read `.claude/docs/troubleshooting/common-issues.md`
→ Match error pattern
→ Provide fix

**"How do multi-module projects work?"**
→ Read `.claude/docs/multi-module/collector-task-implementation.md` (actual implementation)
→ Or read `.claude/docs/kmp-multi-module/README.md` (outdated design, has deprecation notice)
→ Provide setup guide

**"What's the code generation strategy?"**
→ Read `.claude/docs/architecture/code-generation-approach.md` (authoritative)
→ Or read `.claude/docs/architecture/code-generation-strategies.md`
→ Or read `.claude/docs/codegen-v2/ADR.md` (Architecture Decision Record)

### 5. Cross-Reference Related Topics

**When answering, suggest related documentation:**

Example: User asks about generics
→ Primary: `.claude/docs/implementation/generics/technical-reference.md`
→ Also relevant: `.claude/docs/analysis/generic-scoping-analysis.md`
→ Also relevant: `.claude/docs/patterns/complex-generics-strategy.md`
→ Testing: `.claude/docs/validation/type-safety-validation.md`

**Build knowledge graph in response:**
```
📚 Generic Type Handling Resources:

Primary:
- .claude/docs/implementation/generics/technical-reference.md - Technical details
- .claude/docs/analysis/generic-scoping-analysis.md - Core challenge analysis

Related:
- .claude/docs/patterns/complex-generics-strategy.md - Code patterns
- .claude/docs/validation/type-safety-validation.md - Testing approach

Status:
- Phase 1: Complete ✅ (method-level generics)
- Phase 2: In Progress 🚧 (class-level generics)
```

### 6. Handle Missing or Outdated Information

**If documentation doesn't exist:**
```
⚠️ Documentation gap identified

This topic is not yet documented in the knowledge base.
I can help based on:
1. Code analysis (reading actual implementation)
2. Metro source comparison
3. Kotlin compiler API reference

Would you like me to:
- Analyze the codebase for this topic?
- Create documentation draft?
- Consult related existing docs?
```

**If documentation seems outdated:**
```
📋 Note: This documentation may be outdated

Doc says: {old info}
Code shows: {current implementation}

I recommend:
1. Verify against actual code
2. Update documentation
3. Trust implementation over docs
```

### 7. Provide Actionable Guidance

**Don't just quote docs - provide actionable next steps:**

**User asks: "How do I implement X?"**

Response pattern:
1. **Concept** - Extract from architecture docs
2. **Pattern** - Extract from patterns docs
3. **Testing** - Extract from testing guidelines
4. **Example** - Reference working examples
5. **Next steps** - Clear action items

### 8. Maintain Documentation Index

**As you navigate, build mental map of documentation:**

**Critical docs (always relevant):**
- ⭐ `.claude/docs/validation/testing-guidelines.md` - THE ABSOLUTE STANDARD
- ⭐ `.claude/docs/development/metro-alignment.md` - Architectural baseline
- ⭐ `.claude/docs/analysis/generic-scoping-analysis.md` - Core Phase 2 challenge

**Frequently referenced:**
- `.claude/docs/implementation/generics/technical-reference.md` - Generics details
- `.claude/docs/troubleshooting/common-issues.md` - Error resolution
- `.claude/docs/architecture/unified-ir-native.md` - System architecture
- `.claude/docs/architecture/code-generation-approach.md` - Codegen strategy (authoritative)
- `.claude/docs/codegen-v2/ADR.md` - Architecture Decision Record

**Specialized:**
- `.claude/docs/multi-module/collector-task-implementation.md` - Actual multi-module impl
- `.claude/docs/kmp-multi-module/*` - Multi-module design docs (outdated, has deprecation notice)
- `.claude/docs/source_sets/*` - KMP source set handling
- `.claude/docs/patterns/*` - Code patterns (basic, suspend, generics)

## Query Resolution Patterns

### Pattern 1: Direct Lookup
```
User: "What are the testing guidelines?"
→ Read .claude/docs/validation/testing-guidelines.md
→ Extract: Key principles
→ Return: Formatted summary
```

### Pattern 2: Multi-Document Synthesis
```
User: "How do generics work and what's the status?"
→ Read .claude/docs/implementation/generics/technical-reference.md (details)
→ Read .claude/docs/analysis/generic-scoping-analysis.md (analysis)
→ Read .claude/docs/patterns/complex-generics-strategy.md (code patterns)
→ Synthesize: Complete picture
```

### Pattern 3: Cross-Reference Navigation
```
User: "How to test generic interfaces?"
→ Read .claude/docs/validation/testing-guidelines.md (testing approach)
→ Read .claude/docs/implementation/generics/test-matrix.md (generics testing)
→ Read .claude/docs/patterns/complex-generics-strategy.md (examples)
→ Combine: Testing strategy for generics
```

### Pattern 4: Troubleshooting Path
```
User: "Why is my code not compiling?"
→ Read .claude/docs/troubleshooting/common-issues.md (error patterns)
→ If generic-related → Read .claude/docs/analysis/generic-scoping-analysis.md
→ If Metro-related → Read .claude/docs/development/metro-alignment.md
→ Diagnose: Root cause + fix
```

## Documentation Quality Principles

When using documentation:

1. **Trust hierarchy:**
   - Code > Documentation > Assumptions
   - If doc contradicts code, verify with code

2. **Progressive detail:**
   - Start with README/overview files
   - Drill down only as needed
   - Don't load everything upfront

3. **Context preservation:**
   - Quote file paths in responses
   - Enable user to find docs themselves
   - Build user's mental model of structure

4. **Actionable output:**
   - Don't just quote - interpret
   - Provide next steps
   - Reference related Skills

## Related Skills

- **`public-docs-navigator`** - For public user documentation (docs/ MkDocs site). Use that skill for user-facing guides, multi-module setup tutorials, getting started, etc.
- **`kotlin-ir-debugger`** - References Metro and IR docs from this skill
- **`bdd-test-runner`** - References testing guidelines from this skill
- **`generic-scoping-analyzer`** - References generic analysis docs from this skill
- **`kotlin-api-consultant`** - Consults Kotlin API reference docs

**Division of responsibility**:
- This skill (fakt-docs-navigator): Internal contributor docs (.claude/docs/, 66 files)
- public-docs-navigator: External user docs (docs/, 29 files MkDocs site)

## Best Practices

1. **Navigate don't dump** - Find specific sections, don't read entire files
2. **Cross-reference** - Build connections between related docs
3. **Update awareness** - Note when docs seem outdated
4. **Progressive disclosure** - Load details only when needed
5. **Empower users** - Teach them the documentation structure

## Known Documentation Hotspots

**Most frequently accessed:**
1. `.claude/docs/validation/testing-guidelines.md` - THE ABSOLUTE STANDARD, referenced by all test queries
2. `.claude/docs/development/metro-alignment.md` - Referenced for architecture decisions and patterns
3. `.claude/docs/implementation/generics/technical-reference.md` - Referenced for generic type questions
4. `.claude/docs/troubleshooting/common-issues.md` - Referenced for error resolution
5. `.claude/docs/architecture/code-generation-approach.md` - Authoritative codegen strategy

**Specialized but critical:**
- `.claude/docs/analysis/generic-scoping-analysis.md` - Deep Phase 2 understanding
- `.claude/docs/codegen-v2/ADR.md` - Architecture Decision Record for new codegen
- `.claude/docs/multi-module/collector-task-implementation.md` - Actual multi-module implementation
- `.claude/docs/kmp-multi-module/README.md` - Multi-module design research (has deprecation notice)
- `.claude/docs/source_sets/ARCHITECTURE.md` - KMP source set handling

**Note on multi-module**:
- `.claude/docs/multi-module/` - Actual implementation (FakeCollectorTask)
- `.claude/docs/kmp-multi-module/` - Outdated design docs (custom source sets approach NOT implemented)
