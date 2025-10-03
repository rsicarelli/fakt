# Implementation

This section documents Fakt's implementation progress, roadmap, decisions, and technical deep-dives.

## Overview

Fakt is in **MAP (Minimum Awesome Product)** stage - not just working, but production-quality and delightful. This section tracks progress, decisions, and technical implementation details.

## Documentation in this Section

### Status & Progress

- **[CURRENT_STATUS](CURRENT_STATUS.html)** - Real-time implementation status
  - What works (production-ready)
  - What doesn't work (known limitations)
  - What's in progress (current focus)
  - Progress metrics (honest percentages)

- **[IMPLEMENTATION_ROADMAP](IMPLEMENTATION_ROADMAP.html)** - MAP-focused roadmap
  - Critical priorities
  - Enhancement opportunities
  - Phase breakdown
  - Quality gates

### Technical Decisions

- **[IMPLEMENTATION_DECISION](IMPLEMENTATION_DECISION.html)** - Key architectural decisions
  - IR-native vs string-based generation
  - Two-phase FIR → IR approach
  - Metro pattern alignment
  - DSL design choices

### Generic Type Implementation

- **[GENERIC_IMPLEMENTATION_PROGRESS](GENERIC_IMPLEMENTATION_PROGRESS.html)** - Generics status
- **[GENERIC_TYPE_SCOPING_ANALYSIS](GENERIC_TYPE_SCOPING_ANALYSIS.html)** - Scoping challenges
- **[COMPILE_TIME_EXAMPLES](COMPILE_TIME_EXAMPLES.html)** - Practical examples
- **[FINAL_COMPILE_TIME_SOLUTION](FINAL_COMPILE_TIME_SOLUTION.html)** - Final approach

### Kotlin Compiler APIs

- **[KOTLIN_COMPILER_IR_API_GUIDE](KOTLIN_COMPILER_IR_API_GUIDE.html)** - IR API reference
  - IrElement hierarchy
  - Type system navigation
  - Symbol resolution
  - Common patterns

## Development Philosophy

**MAP over MVP**: Build awesome products that compete on developer experience
**Quality First**: Professional code quality matching MockK/Mockito-Kotlin
**Honest Progress**: Real metrics, documented limitations
**Metro Alignment**: Follow proven compiler plugin patterns

## Related Sections

- [Architecture](architecture.html) - Architectural foundation
- [Specifications](specifications.html) - What we're implementing
- [Testing](testing.html) - How we validate implementation
