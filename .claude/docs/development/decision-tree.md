# Decision Tree - KtFakes Development Context

> **Purpose**: Navigate to the right documentation and commands based on your development context
> **Status**: Master navigation for all KtFakes development scenarios
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🌳 **START HERE - Identify Your Context**

### **🚨 NEW TO KTFAKES?**
- 👋 **First time user?** → [📋 Quick Start Demo](../examples/quick-start-demo.md)
- 📚 **Need overview?** → [📋 Project README](../README.md)
- 🎯 **Want examples?** → [📋 Working Examples](../examples/working-examples.md)

---

## 🎯 **CHOOSE YOUR DEVELOPMENT SCENARIO**

### **🏗️ Architecture & Design Questions**
```
❓ Understanding the system architecture?
   → [📋 Unified IR-Native Architecture](../architecture/unified-ir-native.md)

❓ Comparing code generation approaches?
   → [📋 Code Generation Strategies](../architecture/code-generation-strategies.md)

❓ Learning from Metro patterns?
   → [📋 Metro Alignment](metro-alignment.md)

❓ Understanding generic challenges?
   → [📋 Generic Scoping Analysis](../analysis/generic-scoping-analysis.md)
```

### **🔧 IR Generation & FIR Development**
```
❓ Debugging IR generation step-by-step?
   → 🔧 /debug-ir-generation <interface>

❓ Understanding Kotlin compiler APIs?
   → [📋 Kotlin Compiler IR API Guide](kotlin-compiler-ir-api.md)

❓ Working with FIR to IR pipeline?
   → [📋 Metro FIR IR Specifications](metro-fir-ir-specifications.md)

❓ Type resolution issues?
   → 🔧 /analyze-generic-scoping <interface>
```

### **🧪 TDD & Testing Workflow**
```
❓ Following TDD practices?
   → [📋 TDD Practitioners Context](../contexts/tdd-practitioners.md)

❓ Running BDD tests correctly?
   → 🔧 /run-bdd-tests <pattern>

❓ Validating compilation?
   → 🔧 /validate-compilation --interface=<name>

❓ Understanding testing standards?
   → [📋 Testing Guidelines](../validation/testing-guidelines.md) ⭐ ABSOLUTE STANDARD
```

### **⚡ Performance & Benchmarking**
```
❓ Measuring compilation performance?
   → 🔧 /benchmark-compilation-time

❓ Validating generated code performance?
   → [📋 Type Safety Validation](../validation/type-safety-validation.md)

❓ Ensuring compilation safety?
   → [📋 Compilation Validation](../validation/compilation-validation.md)
```

### **🎯 Usage Patterns & Implementation**
```
❓ Basic fake generation patterns?
   → [📋 Basic Fake Generation](../patterns/basic-fake-generation.md)

❓ Working with suspend functions?
   → [📋 Suspend Function Handling](../patterns/suspend-function-handling.md)

❓ Complex generic strategies?
   → [📋 Complex Generics Strategy](../patterns/complex-generics-strategy.md)

❓ Multi-module scenarios?
   → [📋 Multi-Interface Projects](../patterns/multi-interface-projects.md)
```

### **🚨 Debugging & Issues**
```
❓ Compilation errors?
   → 🔧 /analyze-compilation-error --interface=<name>

❓ IR generation problems?
   → [📋 Compiler Plugin Debugging](../patterns/compiler-plugin-debugging.md)

❓ Common problems?
   → [📋 Common Issues & Solutions](../troubleshooting/common-issues.md)

❓ Generic scoping debug?
   → [📋 Generic Scoping Debug](../analysis/generic-scoping-analysis.md)
```

### **📚 Learning & Reference**
```
❓ API specifications?
   → [📋 API Specifications](../api/specifications.md)

❓ Generated API reference?
   → [📋 Generated API Reference](../api/generated-api.md)

❓ Annotation usage?
   → [📋 Annotations Reference](../api/annotations.md)

❓ Metro pattern alignment?
   → [📋 Metro Alignment Guide](metro-alignment.md)
```

---

## 🏢 **USER TYPE CONTEXTS**

### **👨‍💻 Kotlin Developer**
```
✅ Experienced with Kotlin
✅ Want type-safe testing
✅ Moving from MockK/Mockito

→ [📋 Kotlin Developers Context](../contexts/kotlin-developers.md)
```

### **🧪 TDD Practitioner**
```
✅ Test-first development
✅ Red-Green-Refactor cycle
✅ Quality-focused workflow

→ [📋 TDD Practitioners Context](../contexts/tdd-practitioners.md)
```

### **🏢 Enterprise Team**
```
✅ Multi-module projects
✅ Large scale development
✅ Team coordination needs

→ [📋 Enterprise Teams Context](../contexts/enterprise-teams.md)
```

### **🔧 Compiler Plugin Developer**
```
✅ Extending KtFakes
✅ Understanding internals
✅ Contributing to project

→ [📋 Compiler Plugin Devs Context](../contexts/compiler-plugin-devs.md)
```

---

## ⚡ **QUICK COMMAND DECISION TREE**

### **Need to Debug? Choose Your Issue:**
```
📊 Check project status        → /check-implementation-status
🔍 Debug IR generation        → /debug-ir-generation <interface>
🧪 Run specific tests         → /run-bdd-tests <pattern>
⚡ Setup environment          → /setup-development-environment
🔧 Validate compilation       → /validate-compilation
📋 Analyze interface          → /analyze-interface-structure <interface>
🎯 Generic scoping issues     → /analyze-generic-scoping <interface>
🏗️ Check Metro alignment      → /validate-metro-alignment
📚 Consult Kotlin APIs        → /consult-kotlin-api <class>
🚨 Analyze compilation error  → /analyze-compilation-error
```

### **Working on Feature? Choose Your Focus:**
```
🏗️ New interface patterns      → Basic Fake Generation + Working Examples
⚡ Suspend function support    → Suspend Function Handling + Type Safety
🎯 Generic type improvements   → Generic Scoping Analysis + Complex Generics
🧪 Testing improvements       → Testing Guidelines + TDD Context
📊 Performance optimization   → Compilation Validation + Benchmarking
```

---

## 🔄 **WORKFLOW DECISION PATHS**

### **Development Workflow**
```
1. 🎯 Identify Context → Use this decision tree
2. 📚 Read Relevant Docs → Follow specific guides
3. 🔧 Use Commands → Execute with recommended tools
4. ✅ Validate → Check with testing guidelines
```

### **Problem-Solving Workflow**
```
1. 🚨 Identify Issue → Common Issues or Debug Commands
2. 🔍 Diagnose → Specific analysis commands
3. 🔧 Fix → Metro alignment + Kotlin API reference
4. ✅ Verify → Compilation validation + BDD tests
```

### **Learning Workflow**
```
1. 📚 Foundation → Quick Start + Working Examples
2. 🎯 Specialization → User Context + Patterns
3. 🔧 Practice → Commands + Real Development
4. 🏆 Mastery → Architecture + Contributing
```

---

## 📋 **DECISION CRITERIA**

### **📊 Status Assessment**
- **Phase 1 Complete?** → Current Status + Implementation Roadmap
- **Testing Infrastructure?** → Testing Guidelines + BDD Tests
- **Architecture Questions?** → Unified IR-Native + Metro Alignment

### **🎯 Feature Development**
- **Basic Features?** → Basic Fake Generation + Working Examples
- **Advanced Features?** → Complex Generics + Suspend Functions
- **Enterprise Features?** → Multi-Interface + Cross-Module

### **🚨 Problem Resolution**
- **Compilation Issues?** → Compilation Validation + Error Analysis
- **Type Safety Issues?** → Type Safety Validation + Generic Scoping
- **Performance Issues?** → Benchmarking + Optimization

---

## 🔗 **MASTER REFERENCE LINKS**

### **📋 Essential Documentation**
- **[🌟 Testing Guidelines](../validation/testing-guidelines.md)** - THE ABSOLUTE STANDARD
- **[🏗️ Architecture Overview](../architecture/unified-ir-native.md)** - Technical foundation
- **[📊 Current Status](../implementation/current-status.md)** - Where we are now
- **[🎯 Quick Start](../examples/quick-start-demo.md)** - Get started fast

### **⚡ Most Used Commands**
- **🔧 /debug-ir-generation** - Debug code generation
- **🧪 /run-bdd-tests** - Execute tests
- **✅ /validate-compilation** - Check compilation
- **📊 /check-implementation-status** - Project status

### **🎯 Context-Specific Entry Points**
- **[👨‍💻 Kotlin Developer](../contexts/kotlin-developers.md)** - Type-safe testing
- **[🧪 TDD Practitioner](../contexts/tdd-practitioners.md)** - Test-first development
- **[🏢 Enterprise Team](../contexts/enterprise-teams.md)** - Large-scale usage
- **[🔧 Plugin Developer](../contexts/compiler-plugin-devs.md)** - Extending KtFakes

---

**This decision tree ensures you always find the right documentation and commands for your specific KtFakes development context. Start with your scenario above and follow the guided path.** 🌳