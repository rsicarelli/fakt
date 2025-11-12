# Fakt Documentation Navigator

## Description
Navigate Fakt's internal contributor documentation efficiently using the updated folder structure.

## Usage
Activate when the user asks:
- "Where can I find docs about X?"
- "How do I navigate the Fakt docs?"
- "What documentation exists for Y?"
- "Show me docs about Z"

## Quick Navigation by Topic

### Testing & Quality
- `docs/development/validation/testing-guidelines.md` - THE ABSOLUTE TESTING STANDARD
- `docs/development/validation/compilation-validation.md` - Ensure generated code compiles
- `docs/development/validation/type-safety-validation.md` - Generic type testing

### Architecture & Design
- `docs/implementation/architecture/ARCHITECTURE.md` - Main system overview
- `docs/implementation/architecture/gradle-plugin.md` - Plugin implementation
- `docs/implementation/architecture/compiler-optimizations.md` - Caching strategy
- `docs/development/metro-alignment.md` - Metro patterns

### Code Generation
- `docs/implementation/codegen-v2/README.md` - Production-ready DSL
- `docs/implementation/patterns/basic-fake-generation.md` - Core generation
- `docs/implementation/patterns/suspend-function-handling.md` - Coroutines

### Generic Types
- `docs/implementation/generics/technical-reference.md` - IrTypeSubstitutor
- `docs/implementation/generics/complex-generics-strategy.md` - Advanced strategies

### Multi-Module & KMP
- `docs/implementation/multi-module/collector-task-implementation.md` - FakeCollectorTask
- `docs/implementation/source_sets/README.md` - Source sets guide
- `docs/implementation/architecture/kmp-optimization-strategy.md` - KMP optimization

### Getting Started
- `docs/development/decision-tree.md` - Context-based navigation
- `docs/development/examples/quick-start-demo.md` - Quick start
- `docs/development/examples/working-examples.md` - Working examples

### Troubleshooting
- `docs/troubleshooting/common-issues.md` - Common problems and solutions

## Notes

- All paths are relative to `.claude/docs/`
- Decision tree is the master navigation document
- Testing guidelines is THE ABSOLUTE STANDARD
- Architecture docs are in `implementation/architecture/`
- Validation docs moved to `development/validation/`
- Old status tracking docs (current-status.md, roadmap.md, phase1-*) were removed
