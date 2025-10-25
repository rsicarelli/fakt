---
name: fakt-docs-navigator
description: Intelligent navigator for Fakt's 80+ documentation files covering architecture, testing guidelines, Metro patterns, generic type handling, implementation roadmaps, and troubleshooting. Use when user asks about project concepts, patterns, guidelines, Metro alignment, testing standards, generic types, phase implementation, or needs reference to specific documentation.
allowed-tools: [Read, Grep, Glob]
---

# Fakt Documentation Knowledge Base Navigator

Intelligent router and navigator for Fakt's comprehensive documentation corpus (80+ Markdown files) covering compiler plugin architecture, testing standards, Metro alignment, and implementation roadmaps.

## Core Mission

This Skill provides **progressive disclosure** access to Fakt's extensive documentation without overwhelming the context window. It acts as an intelligent librarian, knowing exactly which document to consult for any given question.

## Documentation Structure

The knowledge base is organized into logical topic areas:

```
resources/docs/
├── validation/              # Testing and quality standards
│   ├── testing-guidelines.md    # THE ABSOLUTE STANDARD ⭐
│   ├── compilation-validation.md
│   └── type-safety-validation.md
├── development/             # Technical development guides
│   ├── metro-alignment.md       # Metro patterns ⭐
│   ├── kotlin-api-reference.md
│   ├── decision-tree.md
│   └── metro-fir-ir-specifications.md
├── implementation/          # Status and roadmap
│   ├── roadmap.md
│   ├── generics/            # Generic type implementation
│   │   ├── README.md
│   │   ├── ROADMAP.md
│   │   ├── phase1-core-infrastructure.md
│   │   ├── phase2-code-generation.md
│   │   └── phase3-testing-integration.md
│   ├── phase1-performance-dominance/
│   ├── phase2-idiomatic-kotlin/
│   └── phase3-kmp-dominance/
├── architecture/            # System architecture
│   ├── unified-ir-native.md
│   └── code-generation-strategies.md
├── patterns/                # Code patterns
│   ├── basic-fake-generation.md
│   ├── suspend-function-handling.md
│   └── complex-generics-strategy.md
├── analysis/                # Deep analysis docs
│   └── generic-scoping-analysis.md ⭐
├── troubleshooting/         # Issue resolution
│   └── common-issues.md
├── api/                     # API specifications
│   ├── annotations.md
│   ├── generated-api.md
│   └── specifications.md
├── multi-module/            # Multi-module setup
│   ├── README.md
│   ├── ARCHITECTURE-DECISION.md
│   └── TECHNICAL-REFERENCE.md
└── source_sets/             # KMP source set handling
    ├── README.md
    ├── ARCHITECTURE.md
    └── MIGRATION-GUIDE.md
```

## Instructions

### 1. Classify User Query

**Identify topic category from user's question:**

**Testing & Quality:**
- Keywords: "test", "BDD", "GIVEN-WHEN-THEN", "coverage", "validation"
- → Navigate to `validation/`

**Metro Patterns:**
- Keywords: "Metro", "alignment", "architectural patterns", "IrGenerationExtension"
- → Navigate to `development/metro-alignment.md`

**Generic Types:**
- Keywords: "generics", "type parameters", "T", "class-level", "method-level"
- → Navigate to `implementation/generics/` or `analysis/generic-scoping-analysis.md`

**Implementation Status:**
- Keywords: "status", "phase", "roadmap", "progress", "what's complete"
- → Navigate to `implementation/`

**Architecture:**
- Keywords: "architecture", "how does it work", "design", "FIR", "IR"
- → Navigate to `architecture/`

**Troubleshooting:**
- Keywords: "error", "issue", "problem", "not working", "how to fix"
- → Navigate to `troubleshooting/common-issues.md`

**Multi-module/KMP:**
- Keywords: "multi-module", "KMP", "commonMain", "source set", "modules"
- → Navigate to `multi-module/` or `source_sets/`

### 2. Navigate to Relevant Documentation

**Use file/directory names as navigation aids:**

```bash
# List available docs in category
ls resources/docs/{category}/

# Search for specific topic
grep -r "{keyword}" resources/docs/{category}/
```

**Progressive disclosure strategy:**
1. Identify the most relevant single file
2. Read that file first
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
→ Consult `validation/testing-guidelines.md`
→ Extract GIVEN-WHEN-THEN pattern for X
→ Provide test template

**"What's the Metro pattern for Y?"**
→ Consult `development/metro-alignment.md`
→ Compare Metro vs Fakt approach
→ Show code examples

**"What's the status of generics?"**
→ Consult `implementation/generics/README.md`
→ Consult `analysis/generic-scoping-analysis.md`
→ Summarize current phase and limitations

**"Why is compilation failing?"**
→ Consult `troubleshooting/common-issues.md`
→ Match error pattern
→ Provide fix

**"How do multi-module projects work?"**
→ Consult `multi-module/README.md`
→ Reference `multi-module/ARCHITECTURE-DECISION.md`
→ Provide setup guide

### 5. Cross-Reference Related Topics

**When answering, suggest related documentation:**

Example: User asks about generics
→ Primary: `implementation/generics/README.md`
→ Also relevant: `analysis/generic-scoping-analysis.md`
→ Also relevant: `patterns/complex-generics-strategy.md`
→ Testing: `validation/type-safety-validation.md`

**Build knowledge graph in response:**
```
📚 Generic Type Handling Resources:

Primary:
- implementation/generics/README.md - Complete roadmap
- analysis/generic-scoping-analysis.md - Core challenge

Related:
- patterns/complex-generics-strategy.md - Code patterns
- validation/type-safety-validation.md - Testing approach

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
- ⭐ `validation/testing-guidelines.md` - THE ABSOLUTE STANDARD
- ⭐ `development/metro-alignment.md` - Architectural baseline
- ⭐ `analysis/generic-scoping-analysis.md` - Core Phase 2 challenge

**Frequently referenced:**
- `implementation/generics/README.md` - Generics roadmap
- `troubleshooting/common-issues.md` - Error resolution
- `architecture/unified-ir-native.md` - System architecture

**Specialized:**
- `multi-module/*` - Multi-module setup
- `source_sets/*` - KMP source set handling
- `implementation/phase*/*` - Phase-specific roadmaps

## Query Resolution Patterns

### Pattern 1: Direct Lookup
```
User: "What are the testing guidelines?"
→ Read: validation/testing-guidelines.md
→ Extract: Key principles
→ Return: Formatted summary
```

### Pattern 2: Multi-Document Synthesis
```
User: "How do generics work and what's the status?"
→ Read: implementation/generics/README.md (overview)
→ Read: analysis/generic-scoping-analysis.md (details)
→ Read: patterns/complex-generics-strategy.md (code)
→ Synthesize: Complete picture
```

### Pattern 3: Cross-Reference Navigation
```
User: "How to test generic interfaces?"
→ Read: validation/testing-guidelines.md (testing approach)
→ Read: implementation/generics/phase3-testing-integration.md (generics testing)
→ Read: patterns/complex-generics-strategy.md (examples)
→ Combine: Testing strategy for generics
```

### Pattern 4: Troubleshooting Path
```
User: "Why is my code not compiling?"
→ Read: troubleshooting/common-issues.md (error patterns)
→ If generic-related → Read: analysis/generic-scoping-analysis.md
→ If Metro-related → Read: development/metro-alignment.md
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

This knowledge base powers other Skills:
- **`kotlin-ir-debugger`** - References Metro and IR docs
- **`bdd-test-runner`** - References testing guidelines
- **`metro-pattern-validator`** - References Metro alignment docs
- **`generic-scoping-analyzer`** - References generic analysis docs

## Best Practices

1. **Navigate don't dump** - Find specific sections, don't read entire files
2. **Cross-reference** - Build connections between related docs
3. **Update awareness** - Note when docs seem outdated
4. **Progressive disclosure** - Load details only when needed
5. **Empower users** - Teach them the documentation structure

## Known Documentation Hotspots

**Most frequently accessed:**
1. `validation/testing-guidelines.md` - Referenced by all test-related queries
2. `development/metro-alignment.md` - Referenced for architecture decisions
3. `implementation/generics/README.md` - Referenced for generic type questions
4. `troubleshooting/common-issues.md` - Referenced for error resolution

**Specialized but critical:**
- `analysis/generic-scoping-analysis.md` - Deep Phase 2 understanding
- `multi-module/ARCHITECTURE-DECISION.md` - Multi-module strategy
- `source_sets/ARCHITECTURE.md` - KMP setup details
