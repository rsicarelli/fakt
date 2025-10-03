# Specifications

This section contains API specifications, code generation strategies, and compile-time type handling documentation.

## Overview

Fakt's specification documents define the public API surface, code generation patterns, and type system handling strategies.

## Documentation in this Section

### API Design

- **[API_SPECIFICATIONS](API_SPECIFICATIONS.html)** - Complete API reference
  - `@Fake` annotation usage
  - Factory function patterns
  - Configuration DSL syntax
  - Working examples and patterns

### Code Generation

- **[CODE_GENERATION_STRATEGIES](CODE_GENERATION_STRATEGIES.html)** - Generation approaches
  - String-based vs IR-native generation
  - Implementation class patterns
  - DSL creation strategies

### Type System

- **[COMPILE_TIME_GENERIC_SOLUTIONS](COMPILE_TIME_GENERIC_SOLUTIONS.html)** - Generic type handling
  - Interface-level generics (`Repository<T>`)
  - Method-level generics (`fun <T> process()`)
  - Variance and constraints
  - Smart default value system

## Key Features

**Type Safety**: Compile-time validation and type-safe DSL generation
**Generics Support**: Handles complex generic scenarios
**Professional Output**: Idiomatic Kotlin code generation

## Related Sections

- [Architecture](architecture.html) - Understand the compilation phases
- [Implementation](implementation.html) - See current implementation status
- [Testing](testing.html) - Learn testing patterns for generated code
