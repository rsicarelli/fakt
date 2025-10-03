# KtFakes Claude Code Documentation Structure

> **Purpose**: Comprehensive documentation and command structure optimized for Claude Code development workflow
> **Philosophy**: MAP (Minimum Awesome Product) - Production quality documentation
> **Testing Standard**: [📋 Testing Guidelines](docs/validation/testing-guidelines.md)

## 🎯 **Navigation Index**

### **📋 Core Implementation Status**
- **[📊 Current Status](docs/implementation/current-status.md)** - Phase 1 breakthrough complete, 75% critical issues resolved
- **[🚀 Implementation Roadmap](docs/implementation/roadmap.md)** - Phase 2 generic scoping architecture plan
- **[🔍 Generic Scoping Analysis](docs/analysis/generic-scoping-analysis.md)** - Deep dive into core architectural challenge

### **🏗️ Architecture & Development**
- **[⚙️ Metro Alignment](docs/development/metro-alignment.md)** - Following Metro architectural patterns
- **[📚 Kotlin API Reference](docs/development/kotlin-api-reference.md)** - IR API usage and validation

### **✅ Validation & Testing**
- **[📋 Testing Guidelines](docs/validation/testing-guidelines.md)** - THE ABSOLUTE TESTING STANDARD (GIVEN-WHEN-THEN)
- **[🔧 Compilation Validation](docs/validation/compilation-validation.md)** - Ensure generated code compiles
- **[🛡️ Type Safety Validation](docs/validation/type-safety-validation.md)** - Generic handling and type preservation

## 🔧 **Available Commands**

### **Core Development Commands**
```bash
/debug-ir-generation <interface>      # Step-by-step IR generation debug
/analyze-generic-scoping [interface]  # Deep analysis of generic type challenge
/check-implementation-status [phase]  # Monitor progress and validate milestones
/consult-kotlin-api <api>             # Validate Kotlin compiler API usage
```

### **Validation & Testing Commands**
```bash
/run-bdd-tests [pattern]              # Execute GIVEN-WHEN-THEN tests
/validate-metro-alignment [component] # Check Metro pattern compliance
```

## 📁 **Documentation Structure**

```
.claude/
├── README.md                         # This navigation index
├── docs/
│   ├── validation/                   # Testing and validation strategies
│   │   ├── testing-guidelines.md    # THE ABSOLUTE TESTING STANDARD ⭐
│   │   ├── compilation-validation.md # Generated code must compile
│   │   └── type-safety-validation.md # Generic handling validation
│   ├── development/                  # Technical development guides
│   │   ├── metro-alignment.md       # Metro architectural patterns
│   │   └── kotlin-api-reference.md  # IR API usage validation
│   ├── implementation/               # Status and roadmap tracking
│   │   ├── current-status.md        # Phase 1 breakthrough status ⭐
│   │   └── roadmap.md               # Phase 2 implementation plan ⭐
│   ├── analysis/                     # Deep technical analysis
│   │   └── generic-scoping-analysis.md # Core architectural challenge ⭐
│   ├── architecture/                 # [Future] Architecture docs
│   ├── examples/                     # [Future] Code examples and demos
│   └── api/                         # [Future] API specifications
└── commands/                         # Claude Code command definitions
    ├── debug-ir-generation.md       # IR generation debugging
    ├── analyze-generic-scoping.md   # Generic scoping analysis ⭐
    ├── check-implementation-status.md # Progress monitoring ⭐
    ├── run-bdd-tests.md             # GIVEN-WHEN-THEN test execution
    ├── consult-kotlin-api.md        # API validation
    └── validate-metro-alignment.md  # Metro pattern compliance
```

## 🎯 **Current Focus Areas**

### **✅ Completed (Phase 1 Breakthrough)**
- **Generic Type Parameter Detection** - `<T>` parameters preserved in method signatures
- **Smart Default Value System** - Zero TODO compilation blockers
- **Function Type Resolution** - Perfect `(T) -> R` lambda syntax generation
- **Testing Standards** - GIVEN-WHEN-THEN patterns enforced across all docs

### **🔍 Current Priority (Phase 2A)**
- **Generic Type Scoping Solution** - Dynamic casting with identity functions
- **Implementation Timeline** - 2-3 weeks for method-level generic support
- **Success Target** - 85% → 95% compilation success rate

### **🔮 Future Work (Phase 2B+)**
- **Generic Class Generation** - Full type safety for class-level generics
- **Technical Debt Cleanup** - Remove MVP placeholders
- **Advanced Features** - Import generation, call tracking, performance optimization

## 📊 **Success Metrics Dashboard**

### **Phase 1 Achievements**
- ✅ **Compilation Success Rate**: 60% → 85%
- ✅ **TODO Elimination**: 100% → 0%
- ✅ **Function Type Generation**: Perfect syntax
- ✅ **Infrastructure**: Production-ready build system

### **Phase 2 Targets**
- 🎯 **Compilation Success Rate**: 85% → 95%
- 🎯 **Generic Coverage**: All method-level generics working
- 🎯 **Type Safety**: Controlled Any? casting with safe defaults
- 🎯 **Developer Experience**: Clear patterns and documentation

## 🚀 **Quick Start for Development**

### **Understanding Current State**
1. **Read Status**: [📊 Current Status](docs/implementation/current-status.md)
2. **Check Progress**: `/check-implementation-status`
3. **Analyze Challenge**: [🔍 Generic Scoping](docs/analysis/generic-scoping-analysis.md)

### **Working on Phase 2A**
1. **Analyze Scoping**: `/analyze-generic-scoping AsyncDataService`
2. **Debug Generation**: `/debug-ir-generation <interface>`
3. **Validate Changes**: `/run-bdd-tests`
4. **Check Alignment**: `/validate-metro-alignment`

### **Testing Requirements**
1. **Follow Standards**: [📋 Testing Guidelines](docs/validation/testing-guidelines.md)
2. **Use GIVEN-WHEN-THEN**: Always uppercase, @TestInstance required
3. **Validate Compilation**: [🔧 Compilation Strategy](docs/validation/compilation-validation.md)
4. **Ensure Type Safety**: [🛡️ Type Validation](docs/validation/type-safety-validation.md)

## 🔗 **External References**

### **Original Project Documentation**
- **ktfake/docs/** - Original 18 technical documents
- **ktfake/CLAUDE.md** - Main project context file
- **Metro Framework** - Architectural inspiration source
- **Kotlin Compiler** - IR API reference implementation

### **Development Workflow**
- **Build**: `./gradlew :compiler:shadowJar`
- **Test**: `cd test-sample && ../gradlew compileKotlinJvm`
- **Validate**: `/check-implementation-status`

---

**This structure provides comprehensive support for KtFakes development with Claude Code, following MAP quality standards and focusing on the critical Phase 2 generic scoping implementation.**