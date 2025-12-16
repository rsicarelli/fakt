# KtFakes Claude Code Documentation Structure

> **Purpose**: Comprehensive documentation and skills system optimized for Claude Code development workflow
> **Philosophy**: MAP (Minimum Awesome Product) - Production quality documentation
> **Testing Standard**: [📋 Testing Guidelines](docs/validation/testing-guidelines.md)

## 🎯 **Navigation Index**

### **🌳 Start Here**
- **[🌳 Decision Tree](docs/development/decision-tree.md)** - Navigate to the right documentation based on your context

### **🏗️ Architecture & Implementation**
- **[📐 Architecture Overview](docs/implementation/architecture/ARCHITECTURE.md)** - Overall Fakt architecture
- **[� Gradle Plugin](docs/implementation/architecture/gradle-plugin.md)** - Plugin implementation and extension DSL
- **[⚡ Compiler Optimizations](docs/implementation/architecture/compiler-optimizations.md)** - Caching and incremental compilation
- **[🎯 KMP Optimization Strategy](docs/implementation/architecture/kmp-optimization-strategy.md)** - Multi-platform optimization

### **💻 Code Generation**
- **[🚀 Codegen V2](docs/implementation/codegen-v2/README.md)** - Type-safe code generation DSL (production-ready)
- **[🎨 Basic Fake Generation](docs/implementation/patterns/basic-fake-generation.md)** - Core fake generation patterns
- **[⚡ Suspend Functions](docs/implementation/patterns/suspend-function-handling.md)** - Coroutine support

### **🧬 Generic Type Handling**
- **[� Technical Reference](docs/implementation/generics/technical-reference.md)** - IrTypeSubstitutor deep dive
- **[🎯 Complex Generics Strategy](docs/implementation/generics/complex-generics-strategy.md)** - Advanced generic handling

### **📦 Multi-Module & Source Sets**
- **[🔧 Collector Task](docs/implementation/multi-module/collector-task-implementation.md)** - FakeCollectorTask implementation
- **[📂 Source Sets Guide](docs/implementation/source_sets/README.md)** - KMP source set handling

### **📋 API & Specifications**
- **[📝 API Specifications](docs/implementation/api/specifications.md)** - API contracts and interfaces
- **[�️ Annotations](docs/implementation/api/annotations.md)** - @Fake annotation system
- **[🔍 Generated API](docs/implementation/api/generated-api.md)** - Generated code structure

### **🔬 Development Resources**
- **[⚙️ Metro Alignment](docs/development/metro-alignment.md)** - Following Metro architectural patterns
- **[📚 Kotlin API Reference](docs/development/kotlin-api-reference.md)** - IR API usage and validation
- **[🔧 Kotlin Compiler IR API](docs/development/kotlin-compiler-ir-api.md)** - Compiler API deep dive
- **[📐 Metro FIR/IR Specs](docs/development/metro-fir-ir-specifications.md)** - Metro framework specifications

### **✅ Validation & Testing**
- **[📋 Testing Guidelines](docs/development/validation/testing-guidelines.md)** - THE ABSOLUTE TESTING STANDARD (GIVEN-WHEN-THEN)
- **[🔧 Compilation Validation](docs/development/validation/compilation-validation.md)** - Ensure generated code compiles
- **[🛡️ Type Safety Validation](docs/development/validation/type-safety-validation.md)** - Generic handling and type preservation
- **[🧪 Skills Activation Tests](docs/development/validation/SKILLS-ACTIVATION-TESTS.md)** - 40+ test prompts for Skills

### **📚 Examples & Contexts**
- **[🚀 Quick Start Demo](docs/development/examples/quick-start-demo.md)** - Get started quickly
- **[💡 Working Examples](docs/development/examples/working-examples.md)** - Real-world examples
- **[👨‍💻 For Kotlin Developers](docs/development/contexts/kotlin-developers.md)** - Context for Kotlin devs
- **[🧪 For TDD Practitioners](docs/development/contexts/tdd-practitioners.md)** - Context for TDD users

### **🔮 Future Vision Documents**
- **[🎯 Gradle Plugin Vision](docs/development/future/gradle-plugin-vision.md)** - Future enhancement proposals
- **[⚡ Performance Optimization Vision](docs/development/future/performance-optimization-vision.md)** - Future optimization ideas
- **[🔧 Explicit Backing Fields](docs/development/future/explicit-backing-fields-refactoring.md)** - Refactoring proposal

### **🛠️ Troubleshooting**
- **[⚠️ Common Issues](docs/troubleshooting/common-issues.md)** - Solutions to frequent problems

### **📝 Migration & Patterns**
- **[🔄 Migration Patterns](docs/development/MIGRATION-PATTERNS.md)** - Skills migration patterns

## 🔧 **Available Skills**

Fakt uses Claude Code Skills for autonomous, context-aware development assistance. Skills activate automatically based on conversation context.

### **Core Workflows (Tier 1)**
- **kotlin-ir-debugger** - Debug Kotlin compiler IR generation for @Fake interfaces
- **bdd-test-runner** - Execute and validate GIVEN-WHEN-THEN tests with vanilla JUnit5
- **behavior-analyzer-tester** - Deep behavior analysis and comprehensive unit test generation

### **Validation (Tier 2)**
- **metro-pattern-validator** - Validate Fakt implementation alignment with Metro patterns
- **compilation-validator** - Validate generated code compiles without errors
- **implementation-tracker** - Monitor KtFakes implementation progress across phases

### **Analysis (Tier 3)**
- **kotlin-api-consultant** - Query Kotlin compiler source for API validation and Metro alignment
- **interface-analyzer** - Deep structural analysis of @Fake interfaces for generation planning
- **compilation-error-analyzer** - Systematic compilation error diagnosis and resolution
- **generic-scoping-analyzer** - Analyze generic type parameter scoping challenges with Phase 2A/2B solutions

### **Knowledge Base**
- **fakt-docs-navigator** - Intelligent navigator for 80+ documentation files

### **Development Tools**
- **skill-creator** - Creates new Claude Code Skills following best practices

## 📁 **Documentation Structure**

```
.claude/
├── README.md                         # This navigation index
├── skills/                           # ⭐ Claude Code Skills (12 total)
│   ├── core-workflows/               # Tier 1: Essential development workflows
│   │   ├── kotlin-ir-debugger/
│   │   ├── bdd-test-runner/
│   │   └── behavior-analyzer-tester/
│   ├── validation/                   # Tier 2: Validation and tracking
│   │   ├── metro-pattern-validator/
│   │   ├── compilation-validator/
│   │   └── implementation-tracker/
│   ├── analysis/                     # Tier 3: Deep analysis capabilities
│   │   ├── kotlin-api-consultant/
│   │   ├── interface-analyzer/
│   │   ├── compilation-error-analyzer/
│   │   └── generic-scoping-analyzer/
│   ├── knowledge-base/               # Documentation navigation
│   │   └── fakt-docs-navigator/
│   └── development/                  # Development utilities
│       └── skill-creator/
├── docs/
│   ├── development/                  # Development resources and guides
│   │   ├── decision-tree.md         # 🌳 Master navigation guide
│   │   ├── metro-alignment.md       # Metro architectural patterns
│   │   ├── kotlin-api-reference.md  # IR API usage validation
│   │   ├── kotlin-compiler-ir-api.md # Compiler API deep dive
│   │   ├── metro-fir-ir-specifications.md
│   │   ├── MIGRATION-PATTERNS.md    # Skills migration patterns
│   │   ├── validation/              # Testing and validation
│   │   │   ├── testing-guidelines.md    # THE ABSOLUTE TESTING STANDARD ⭐
│   │   │   ├── compilation-validation.md
│   │   │   ├── type-safety-validation.md
│   │   │   └── SKILLS-ACTIVATION-TESTS.md
│   │   ├── examples/                # Quick start and demos
│   │   │   ├── quick-start-demo.md
│   │   │   └── working-examples.md
│   │   ├── contexts/                # Persona-based guides
│   │   │   ├── kotlin-developers.md
│   │   │   └── tdd-practitioners.md
│   │   └── future/                  # Future vision documents
│   │       ├── gradle-plugin-vision.md
│   │       ├── performance-optimization-vision.md
│   │       └── explicit-backing-fields-refactoring.md
│   ├── implementation/              # Implementation details
│   │   ├── architecture/            # Core architecture docs
│   │   │   ├── ARCHITECTURE.md      # ⭐ Main architecture overview
│   │   │   ├── gradle-plugin.md
│   │   │   ├── compiler-optimizations.md
│   │   │   └── kmp-optimization-strategy.md
│   │   ├── codegen-v2/              # Code generation DSL
│   │   │   ├── README.md            # ⭐ Codegen V2 overview
│   │   │   └── ADR.md               # Architecture decisions
│   │   ├── patterns/                # Implementation patterns
│   │   │   ├── basic-fake-generation.md
│   │   │   └── suspend-function-handling.md
│   │   ├── generics/                # Generic type handling
│   │   │   ├── technical-reference.md
│   │   │   └── complex-generics-strategy.md
│   │   ├── multi-module/            # Multi-module support
│   │   │   └── collector-task-implementation.md
│   │   ├── source_sets/             # KMP source sets
│   │   │   ├── README.md
│   │   │   ├── ARCHITECTURE.md
│   │   │   ├── API-REFERENCE.md
│   │   │   └── CODE-PATTERNS.md
│   │   └── api/                     # API specifications
│   │       ├── specifications.md
│   │       ├── annotations.md
│   │       └── generated-api.md
│   └── troubleshooting/             # Problem resolution
│       └── common-issues.md
```

## 🎯 **Key Documentation Highlights**

### **🌟 Must-Read Documents**
1. **[🌳 Decision Tree](docs/development/decision-tree.md)** - Start here! Navigate based on your context
2. **[📐 Architecture Overview](docs/implementation/architecture/ARCHITECTURE.md)** - System architecture and design
3. **[🚀 Codegen V2](docs/implementation/codegen-v2/README.md)** - Type-safe code generation (production-ready)
4. **[📋 Testing Guidelines](docs/development/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standard
5. **[� Collector Task](docs/implementation/multi-module/collector-task-implementation.md)** - Multi-module implementation

### **📚 Learning Paths**

**New to Fakt?**
1. [Quick Start Demo](docs/development/examples/quick-start-demo.md)
2. [Working Examples](docs/development/examples/working-examples.md)
3. [Basic Fake Generation](docs/implementation/patterns/basic-fake-generation.md)

**Working with Generics?**
1. [Technical Reference](docs/implementation/generics/technical-reference.md)
2. [Complex Generics Strategy](docs/implementation/generics/complex-generics-strategy.md)

**Building Multi-Module Support?**
1. [Collector Task Implementation](docs/implementation/multi-module/collector-task-implementation.md)
2. [Source Sets Guide](docs/implementation/source_sets/README.md)
3. [KMP Optimization Strategy](docs/implementation/architecture/kmp-optimization-strategy.md)

**Understanding Compiler Integration?**
1. [Gradle Plugin](docs/implementation/architecture/gradle-plugin.md)
2. [Compiler Optimizations](docs/implementation/architecture/compiler-optimizations.md)
3. [Metro Alignment](docs/development/metro-alignment.md)

## 🚀 **Quick Start for Development**

### **First Time Setup**
1. **Navigate**: [🌳 Decision Tree](docs/development/decision-tree.md) - Find your context
2. **Quick Start**: [🚀 Quick Start Demo](docs/development/examples/quick-start-demo.md)
3. **Examples**: [� Working Examples](docs/development/examples/working-examples.md)

### **Development Workflow**
1. **Debug IR**: `/debug-ir-generation <interface>`
2. **Run Tests**: `/run-bdd-tests`
3. **Validate Metro**: `/validate-metro-alignment`
4. **Check Compilation**: `/validate-compilation`

### **Testing Requirements**
1. **Follow Standards**: [📋 Testing Guidelines](docs/development/validation/testing-guidelines.md)
2. **Use GIVEN-WHEN-THEN**: Always uppercase, @TestInstance required
3. **Validate Compilation**: [🔧 Compilation Validation](docs/development/validation/compilation-validation.md)
4. **Ensure Type Safety**: [🛡️ Type Safety](docs/development/validation/type-safety-validation.md)

## 🔗 **External References**

### **Project Documentation**
- **[fakt/docs/](../docs/)** - User-facing documentation (MkDocs)
- **[fakt/CLAUDE.md](../CLAUDE.md)** - Main project context file
- **[Metro Framework](https://github.com/kotlinx/metro)** - Architectural inspiration
- **[Kotlin Compiler](https://github.com/JetBrains/kotlin)** - IR API reference

### **Development Commands**
```bash
# Build compiler plugin
./gradlew :compiler:shadowJar

# Run tests
./gradlew :compiler:test

# Test in sample project
cd test-sample && ../gradlew compileKotlinJvm

# Build documentation
mkdocs serve
```

### **Skill Commands**
- `/debug-ir-generation` - Debug IR generation for interface
- `/run-bdd-tests` - Execute GIVEN-WHEN-THEN tests
- `/validate-metro-alignment` - Check Metro pattern compliance
- `/validate-compilation` - Ensure generated code compiles

---

**This documentation structure supports comprehensive Fakt development with Claude Code, organized by implementation concerns and providing clear learning paths for different development contexts.**