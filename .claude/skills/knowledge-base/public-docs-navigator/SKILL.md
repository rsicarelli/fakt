---
name: public-docs-navigator
description: Navigate Fakt's public MkDocs documentation site (docs/) for users learning about Fakt. Use when users ask about documentation, guides, multi-module setup, code generation strategy, testing patterns, or need help understanding Fakt features. Provides quick navigation to reference docs, usage guides, multi-module documentation, and troubleshooting resources. Progressive disclosure - loads specific pages on demand rather than loading everything.
allowed-tools: [Read, Grep, Glob]
---

# Public Documentation Navigator

Navigate Fakt's public MkDocs documentation site efficiently. This skill helps locate and access user-facing documentation for developers learning to use Fakt.

## Core Mission

Provide fast, accurate navigation to Fakt's public documentation (29 files organized in MkDocs site) with intelligent search and cross-reference capabilities. Focus on helping users find the right documentation quickly without loading unnecessary content.

## Documentation Structure

```
docs/                                      # Public MkDocs site (29 files)
├── index.md                               # Homepage
├── introduction/                          # 5 files - Getting started
│   ├── overview.md                        # What is Fakt
│   ├── why-fakt.md                        # Why choose Fakt over alternatives
│   ├── installation.md                    # Setup instructions
│   ├── quick-start.md                     # First fake in 5 minutes
│   └── features.md                        # Comprehensive feature list
├── usage/                                 # 5 files - Core features
│   ├── basic-usage.md                     # Simple interface faking
│   ├── suspend-functions.md               # Coroutine support
│   ├── generics.md                        # Generic type handling
│   ├── properties.md                      # val/var properties
│   └── call-tracking.md                   # StateFlow counters
├── guides/                                # 3 files - Best practices
│   ├── testing-patterns.md                # GIVEN-WHEN-THEN, state-based testing
│   ├── migration.md                       # Migrate from MockK/Mockito
│   └── performance.md                     # Benchmarks, optimization
├── multi-module/                          # 6 files - KMP multi-module
│   ├── index.md                           # Overview & architecture
│   ├── getting-started.md                 # Step-by-step 15-min tutorial
│   ├── advanced.md                        # Platform detection, performance
│   ├── troubleshooting.md                 # 8+ common issues
│   ├── migration.md                       # Single → multi-module strategies
│   └── reference.md                       # FakeCollectorTask technical reference
├── reference/                             # 6 files - Technical reference
│   ├── api.md                             # Generated fake APIs
│   ├── codegen-strategy.md                # Why DSL + string-based generation
│   ├── fakes-over-mocks.md                # Performance analysis, KMP crisis
│   ├── configuration.md                   # fakt {} DSL configuration
│   ├── compatibility.md                   # Kotlin version compatibility
│   └── limitations.md                     # Current limitations, roadmap
├── samples/                               # 1 file
│   └── index.md                           # Sample projects overview
├── faq.md                                 # Frequently asked questions
├── troubleshooting.md                     # Common issues and solutions
├── contributing.md                        # How to contribute
└── changelog.md                           # Version history
```

## Instructions

### 1. Identify User Intent

**Analyze the user's question to determine the topic area**:

- **Getting Started**: installation, quick-start, first fake, setup
  → Navigate to `introduction/` folder

- **How to Use**: basic usage, suspend functions, generics, properties, call tracking
  → Navigate to `usage/` folder

- **Best Practices**: testing patterns, GIVEN-WHEN-THEN, migration from MockK/Mockito, performance
  → Navigate to `guides/` folder

- **Multi-Module**: KMP, cross-module fakes, collector modules, platform detection
  → Navigate to `multi-module/` folder (6 comprehensive files)

- **Technical Reference**: API details, code generation architecture, fakes vs mocks analysis, configuration options
  → Navigate to `reference/` folder

- **Troubleshooting**: Compilation errors, setup issues, IDE problems
  → Navigate to `troubleshooting.md` or `faq.md`

### 2. Quick Navigation Patterns

**Use Read tool for specific pages** (if you know exactly what the user needs):

```bash
# Getting started
Read docs/introduction/quick-start.md        # First fake in 5 minutes
Read docs/introduction/installation.md       # Setup guide

# Core features
Read docs/usage/basic-usage.md               # Simple interface faking
Read docs/usage/suspend-functions.md         # Coroutine support
Read docs/usage/generics.md                  # Generic types

# Multi-module (comprehensive)
Read docs/multi-module/index.md              # Architecture overview
Read docs/multi-module/getting-started.md    # Step-by-step tutorial
Read docs/multi-module/troubleshooting.md    # Common issues

# Reference (technical deep dives)
Read docs/reference/codegen-strategy.md      # Why DSL + string generation
Read docs/reference/fakes-over-mocks.md      # 1,683 lines, performance analysis
Read docs/reference/api.md                   # Generated API reference
```

**Use Grep tool for keyword searches** (when user's need is unclear):

```bash
# Search for specific topics
Grep "multi-module" docs/ -r --output_mode=files_with_matches
Grep "suspend function" docs/ -r --output_mode=content --head_limit=20
Grep "MockK.*Mockito" docs/ -r --output_mode=content
Grep "performance" docs/ -r --output_mode=files_with_matches

# Search with context
Grep "code generation" docs/ -r -C 3 --output_mode=content --head_limit=30
```

### 3. Progressive Disclosure Strategy

**DO NOT** load all documentation at once. Instead:

1. **First**, identify the likely section (introduction, usage, guides, multi-module, reference)
2. **Then**, use Grep to search within that section if needed
3. **Finally**, Read the specific file(s) relevant to the user's question

**Example workflow**:
```
User: "How do I set up multi-module support?"

1. Identify: Multi-module topic
2. Search: Grep "getting started" docs/multi-module/ --output_mode=content
3. Read: Read docs/multi-module/getting-started.md
4. Cross-reference: "For troubleshooting, see docs/multi-module/troubleshooting.md"
```

### 4. Handle Cross-References

**When a topic spans multiple files**, provide navigation:

- **Multi-module setup**:
  - Overview: `multi-module/index.md`
  - Tutorial: `multi-module/getting-started.md`
  - Troubleshooting: `multi-module/troubleshooting.md`

- **Code generation**:
  - Strategy: `reference/codegen-strategy.md`
  - Testing patterns: `guides/testing-patterns.md`

- **Fakes vs Mocks**:
  - Analysis: `reference/fakes-over-mocks.md`
  - Migration: `guides/migration.md`

- **Performance**:
  - Guide: `guides/performance.md`
  - Fakes vs Mocks: `reference/fakes-over-mocks.md` (performance metrics)

### 5. Special Topics

#### Multi-Module Documentation (6 Files)

**Comprehensive resource** (~7,000 lines total):
- `index.md` - Architecture, when to use, decision criteria
- `getting-started.md` - 15-minute tutorial with complete examples
- `advanced.md` - Platform detection algorithm, performance, publishing
- `troubleshooting.md` - 8+ common issues with step-by-step solutions
- `migration.md` - Single-module → multi-module strategies
- `reference.md` - FakeCollectorTask technical deep dive

**Keywords triggering multi-module**: "multi-module", "KMP", "cross-module", "collector", "platform detection", "commonTest"

#### Reference Documentation (Technical Deep Dives)

**Comprehensive technical references**:
- `codegen-strategy.md` (387 lines) - Why type-safe DSL + string generation
  - IR-native vs string-based comparison
  - KotlinPoet decision
  - Metro comparison

- `fakes-over-mocks.md` (1,683 lines) - Strategic analysis
  - "Fakes over mocks" philosophy
  - Google "Now in Android" case study
  - Performance metrics (1,300x slower mockkObject, 3x slower mock-maker-inline)
  - KMP multiplatform crisis
  - Kotlin-specific challenges

**Keywords**: "codegen", "code generation", "IR", "fakes over mocks", "performance", "MockK", "Mockito"

#### Getting Started Flow

**Recommended reading order for new users**:
1. `introduction/overview.md` - What is Fakt
2. `introduction/why-fakt.md` - Why choose Fakt
3. `introduction/installation.md` - Setup
4. `introduction/quick-start.md` - First fake
5. `usage/basic-usage.md` - Core features

### 6. Search Strategies

**When user's question is vague**:

```bash
# Broad topic search
Grep "keyword" docs/ -r --output_mode=files_with_matches

# Then narrow down
Grep "keyword" docs/{likely-section}/ -r --output_mode=content --head_limit=30
```

**When looking for examples**:

```bash
Grep "```kotlin" docs/ -r --output_mode=files_with_matches  # Find code examples
Grep "@Fake" docs/ -r -A 10 --output_mode=content          # Find annotation usage
```

**When looking for troubleshooting**:

```bash
Grep "error\|issue\|problem" docs/troubleshooting.md --output_mode=content
Grep "{user's error message}" docs/ -r --output_mode=content
```

## Related Skills

- **fakt-docs-navigator**: For internal contributor documentation (`.claude/docs/`, 66 files)
  - Use when discussing compiler architecture, Metro alignment, implementation details
  - This skill is for **external users**; that skill is for **contributors**

## Best Practices

1. **Start Specific**: If you know exactly what the user needs, Read the specific file directly
2. **Search Progressively**: Start with file list, then narrow to content
3. **Provide Context**: When referencing a file, explain what it contains
4. **Cross-Reference**: Link related documentation when relevant
5. **Respect Progressive Disclosure**: Don't load entire documentation tree unless needed

## Common Scenarios

### Scenario 1: New User Setup
```
User: "How do I get started with Fakt?"

Response:
1. Read docs/introduction/quick-start.md
2. Summarize: "@Fake annotation → auto-generated fakes → configure with DSL"
3. Offer: "For detailed installation, see docs/introduction/installation.md"
```

### Scenario 2: Multi-Module Question
```
User: "How do I use fakes across modules?"

Response:
1. Read docs/multi-module/index.md (architecture overview)
2. Summarize: Producer → Collector → Consumer pattern
3. Suggest: "For step-by-step setup, see docs/multi-module/getting-started.md"
```

### Scenario 3: Technical Deep Dive
```
User: "Why does Fakt use string-based generation instead of IR?"

Response:
1. Read docs/reference/codegen-strategy.md
2. Summarize: Transparency, debuggability, stability > marginal performance
3. Reference: Metro comparison, IR-native trade-offs
```

### Scenario 4: Troubleshooting
```
User: "My fakes aren't appearing in the IDE"

Response:
1. Grep "IDE.*fakes" docs/troubleshooting.md
2. Read relevant section
3. Provide: "./gradlew clean build, File → Invalidate Caches"
```

### Scenario 5: Performance Question
```
User: "Is Fakt faster than MockK?"

Response:
1. Read docs/reference/fakes-over-mocks.md (section on performance)
2. Highlight: mockkObject 1,300x slower, mock-maker-inline 3x slower
3. Reference: docs/guides/performance.md for benchmarks
```

## Notes

- **Total Documentation**: 29 files, ~20,000+ lines
- **Largest Files**:
  - `reference/fakes-over-mocks.md` (1,683 lines)
  - `multi-module/getting-started.md` (1,800 lines)
  - `multi-module/advanced.md` (1,500 lines)
- **Most Comprehensive Section**: Multi-module (6 files, ~7,000 lines total)
- **MkDocs Config**: `mkdocs.yml` (120 lines) - for navigation structure reference

- **Public vs Internal Docs**:
  - This skill: `docs/` (public MkDocs site for users)
  - fakt-docs-navigator: `.claude/docs/` (internal docs for contributors)
