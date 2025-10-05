# Fakt Compiler Plugin - Slash Commands

> **Custom slash commands for Claude Code to streamline Fakt development**
> **Location**: `.claude/commands/`
> **Usage**: Type `/command-name` in Claude Code

## 🎯 Available Commands

### 🚀 Roadmap Execution

#### `/execute-roadmap [phase] [feature]`
**Execute Fakt extended roadmap with TDD RED-GREEN cycle**

- Analyzes current progress across all phases (Phase 1, 2, 3)
- Detects which feature you're working on
- Creates detailed TDD todo list (RED → GREEN cycle)
- Updates phase-specific CHANGELOGs
- Updates overall current-status.md
- Validates alignment with Metro patterns and testing standards

**Use Cases**:
- Starting work on roadmap features (final classes, singleton objects, data builders, etc.)
- Resuming after breaks
- Checking current phase and next steps
- Getting detailed TDD todo list for active feature
- Tracking progress across the entire roadmap

**Examples**:
```bash
# Auto-detect current phase and feature
/execute-roadmap

# Start/resume specific phase
/execute-roadmap phase1           # Performance Dominance
/execute-roadmap phase2           # Idiomatic Kotlin
/execute-roadmap phase3           # KMP Support

# Work on specific feature
/execute-roadmap phase1 final-classes
/execute-roadmap phase1 singleton-objects
/execute-roadmap phase1 top-level-functions
/execute-roadmap phase2 data-classes
/execute-roadmap phase2 sealed-hierarchies
/execute-roadmap phase2 flow-producers
/execute-roadmap phase3 kmp

# Reset a feature and start fresh
/execute-roadmap phase1 singleton-objects --reset
```

**Related Docs**:
- [Main Roadmap](../docs/implementation/roadmap.md)
- [Phase 1: Performance](../docs/implementation/phase1-performance-dominance/README.md)
- [Phase 2: Idiomatic Kotlin](../docs/implementation/phase2-idiomatic-kotlin/README.md)
- [Phase 3: KMP](../docs/implementation/phase3-kmp-dominance/README.md)

---

### 🚀 Generic Type Implementation

#### `/resume-and-update-generics [phase]`
**Resume and continue generic type implementation with TDD RED-GREEN cycle**

- Analyzes current progress in generic implementation
- Creates detailed TDD todo list (RED → GREEN cycle)
- Updates CHANGELOG with session tracking
- Validates alignment with Metro patterns and testing standards

**Use Cases**:
- Starting a new work session on generics
- Resuming after closing tabs/breaks
- Checking current phase and next steps
- Getting detailed TDD todo list

**Examples**:
```bash
# Auto-detect current phase and resume
/resume-and-update-generics

# Resume specific phase
/resume-and-update-generics phase1
/resume-and-update-generics phase2

# Reset and start fresh
/resume-and-update-generics --reset
```

**Related Docs**: [Generics Implementation](../docs/implementation/generics/)

---

#### `/plan-generic-implementation [strategy]`
**Deep research and strategic planning for generic type implementation**

- Comprehensive analysis of current Fakt infrastructure
- Evaluates implementation strategies (erasure, substitution, mvp, full)
- Creates detailed roadmap with IrTypeSubstitutor integration
- Metro pattern alignment validation

**Strategies**:
- `erasure` - Type erasure MVP (fastest)
- `substitution` - IR substitution approach
- `mvp` - Smart defaults strategy
- `full` - Full production strategy (recommended)

**Examples**:
```bash
# Get full production strategy
/plan-generic-implementation full

# Quick MVP approach
/plan-generic-implementation mvp
```

---

### 🔍 Analysis & Debugging

#### `/analyze-interface-structure <InterfaceName>`
**Deep structural analysis of @Fake annotated interfaces**

- Analyzes interface methods, properties, type parameters
- Identifies generic patterns and complexity
- Suggests optimal code generation strategy
- Validates Metro alignment

**Example**:
```bash
/analyze-interface-structure Repository
/analyze-interface-structure CacheService
```

---

#### `/analyze-generic-scoping [interface_name|all]`
**Analyzes generic type parameter scoping challenges**

- Detects class-level vs method-level generics
- Identifies scoping conflicts
- Suggests Phase 2A solutions
- Validates type parameter usage

**Example**:
```bash
/analyze-generic-scoping Repository
/analyze-generic-scoping all
```

---

#### `/analyze-compilation-error [--type=<error_type>] [--interface=<name>]`
**Systematic compilation error analysis and resolution**

- Categorizes compilation errors
- Provides specific fixes for generic-related errors
- Tracks error patterns
- Suggests preventive measures

**Example**:
```bash
/analyze-compilation-error --interface=Repository
/analyze-compilation-error --type=generic
```

---

#### `/debug-ir-generation <interface_name>`
**Step-by-step debugging of IR generation for interfaces**

- Traces IR generation process
- Validates type parameter handling
- Checks Metro pattern compliance
- Identifies generation issues

**Example**:
```bash
/debug-ir-generation Repository
/debug-ir-generation AsyncService
```

---

### ✅ Validation & Testing

#### `/validate-compilation [--interface=<name>|--all|--verbose]`
**Comprehensive compilation validation and type safety verification**

- Validates generated fakes compile
- Tests type safety at use-site
- Multi-stage validation (generation → structure → type-check)
- Performance benchmarking

**Example**:
```bash
/validate-compilation --all
/validate-compilation --interface=Repository --verbose
```

---

#### `/validate-metro-alignment [component_name]`
**Validates KtFakes implementation alignment with Metro patterns**

- Checks architectural alignment
- Validates IR generation patterns
- Tests two-phase compilation approach
- Ensures best practices compliance

**Example**:
```bash
/validate-metro-alignment
/validate-metro-alignment GenericIrSubstitutor
```

---

#### `/run-bdd-tests [pattern|all|compiler]`
**Execute BDD-compliant GIVEN-WHEN-THEN tests**

- Runs tests with vanilla JUnit5
- Validates GIVEN-WHEN-THEN naming
- Coverage validation
- Enforces testing standards

**Example**:
```bash
/run-bdd-tests all
/run-bdd-tests Generic*
/run-bdd-tests compiler
```

---

### 📚 Reference & Documentation

#### `/consult-kotlin-api <api_class_or_interface>`
**Query Kotlin compiler source for API validation**

- Looks up Kotlin compiler APIs
- Validates API compatibility
- Checks for deprecations
- Provides usage examples

**Example**:
```bash
/consult-kotlin-api IrTypeSubstitutor
/consult-kotlin-api IrTypeParameterRemapper
```

---

#### `/check-implementation-status [phase1|phase2|detailed|validation]`
**Monitor implementation progress and validate phase completion**

- Shows current implementation status
- Validates phase completion
- Tracks test pass rates
- Identifies blockers

**Example**:
```bash
/check-implementation-status
/check-implementation-status phase1
/check-implementation-status detailed
```

---

### 🛠️ Setup & Development

#### `/setup-development-environment [--full|--quick|--validate]`
**Complete development environment setup with IDE integration**

- Sets up IDE configuration
- Installs dependencies
- Configures tooling
- Validates setup

**Example**:
```bash
/setup-development-environment --quick
/setup-development-environment --full
/setup-development-environment --validate
```

---

#### `/analyze-and-test`
**Deep behavior analysis and comprehensive unit test generation**

- Analyzes code behavior
- Generates GIVEN-WHEN-THEN tests
- Follows Fakt testing standards
- Ensures coverage

---

## 🎯 Command Categories

### By Use Case

**Starting Generic Implementation**:
1. `/plan-generic-implementation full` - Get strategy
2. `/resume-and-update-generics` - Start first session

**Daily Development Session**:
1. `/resume-and-update-generics` - Resume where you left off
2. Follow TDD todo list (RED → GREEN)
3. Update CHANGELOG manually
4. `/validate-compilation --all` - Verify before ending

**Debugging Issues**:
1. `/debug-ir-generation <interface>` - Trace generation
2. `/analyze-compilation-error --interface=<name>` - Fix errors
3. `/consult-kotlin-api <api>` - Validate API usage

**Validation & Quality**:
1. `/validate-metro-alignment` - Check patterns
2. `/run-bdd-tests all` - Run tests
3. `/validate-compilation --verbose` - Type safety check

---

## 📊 Command Priority

### Must Use (Critical)

1. **`/resume-and-update-generics`** ⭐ - Every session start
2. **`/validate-metro-alignment`** - Before major decisions
3. **`/run-bdd-tests`** - After each GREEN cycle

### Should Use (Important)

4. `/validate-compilation` - Before phase completion
5. `/consult-kotlin-api` - When using new APIs
6. `/check-implementation-status` - Weekly progress check

### Nice to Have (Helpful)

7. `/analyze-interface-structure` - Understanding interfaces
8. `/debug-ir-generation` - When generation fails
9. `/analyze-compilation-error` - For specific errors

---

## 🔄 Typical Workflow

### Phase 1 Daily Session

```bash
# 1. Start session
/resume-and-update-generics

# Output: Todo list with RED-GREEN tasks

# 2. Follow TDD cycle
# ❌ RED: Write failing test
# ✅ GREEN: Implement to pass
# Run: ./gradlew :compiler:test

# 3. Validate alignment
/validate-metro-alignment GenericIrSubstitutor

# 4. Check tests
/run-bdd-tests Generic*

# 5. End session
# Update CHANGELOG.md manually
```

### Phase Completion

```bash
# 1. Check status
/check-implementation-status phase1

# 2. Validate compilation
/validate-compilation --all

# 3. Run full test suite
/run-bdd-tests all

# 4. Check Metro alignment
/validate-metro-alignment

# 5. Ready for next phase
/resume-and-update-generics phase2
```

---

## 🚨 Best Practices

### DO ✅

- Run `/resume-and-update-generics` at start of each session
- Follow TDD RED-GREEN cycle religiously
- Update CHANGELOG after each session
- Validate Metro alignment before architectural decisions
- Run tests after each GREEN cycle

### DON'T ❌

- Skip RED phase (write tests first!)
- Batch multiple GREEN cycles (commit often)
- Ignore command recommendations
- Skip validation before phase completion
- Forget to update CHANGELOG

---

## 📝 Creating Custom Commands

Commands are markdown files in `.claude/commands/` with this structure:

```markdown
# /command-name

> **Purpose**: Brief description
> **Usage**: `/command-name [args]`
> **Scope**: What this command does

## Command Overview
[Detailed description]

## Arguments
[Argument documentation]

## What This Command Does
[Step-by-step logic]

## Example Usage
[Examples]
```

**Naming Convention**: Use kebab-case (`analyze-interface-structure.md`)

---

## 🔗 Related Documentation

- [Generic Implementation Docs](../docs/implementation/generics/)
- [Testing Guidelines](../docs/validation/testing-guidelines.md)
- [Metro Alignment](../docs/development/metro-alignment.md)
- [CLAUDE.md](../../CLAUDE.md) - Main project documentation

---

## 📞 Getting Help

**Command not working?**
1. Check command exists: `ls .claude/commands/`
2. Read command documentation: `cat .claude/commands/<name>.md`
3. Verify arguments format
4. Check related documentation

**Want to add a new command?**
1. Create markdown file in `.claude/commands/`
2. Follow template structure
3. Update this README
4. Test command

---

Last Updated: January 2025
Total Commands: 15
