---
allowed-tools: Read, Grep, Glob, Bash(find:*), TodoWrite, Task
argument-hint: <InterfaceName> (required - name of interface to analyze structure)
description: Deep structural analysis of @Fake annotated interfaces for generation planning
model: claude-sonnet-4-20250514
---

# 🔬 Interface Structure Deep Analysis

**Comprehensive @Fake interface analysis with generation complexity assessment**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/analysis/generic-scoping-analysis.md` - Generic type challenge understanding
- `.claude/docs/patterns/complex-generics-strategy.md` - Advanced generic handling patterns
- `.claude/docs/validation/type-safety-validation.md` - Type system validation approach
- `.claude/docs/patterns/ir-generation-flow.md` - Generation pipeline integration
- `.claude/docs/troubleshooting/common-issues.md` - Known structural issue patterns
- Real interface definitions in source code for analysis

**🏆 ANALYSIS BASELINE:**
- Method signature complexity assessment
- Generic type parameter scoping analysis
- Suspend function pattern detection
- Property type resolution planning

## Command Overview

**Usage**: `/analyze-interface-structure <InterfaceName>`
**Example**: `/analyze-interface-structure UserService`

Analyzes the structural characteristics of an interface to understand:
- Method signatures and parameter types
- Property definitions and types
- Generic type parameters and constraints
- Suspend function usage patterns
- Complex type relationships

## Command Implementation

### Input Analysis
```bash
# 1. Locate interface definition
find . -name "*.kt" -exec grep -l "interface $INTERFACE_NAME" {} \;

# 2. Extract interface details
grep -A 20 "interface $INTERFACE_NAME" <interface_file>

# 3. Identify @Fake annotation presence
grep -B 5 "interface $INTERFACE_NAME" <interface_file> | grep "@Fake"
```

### Structural Analysis

#### Method Analysis
```kotlin
// Extract method signatures
grep -E "^\s*(suspend\s+)?fun\s+" <interface_file> | while read method; do
    echo "Method: $method"
    # Analyze:
    # - Return type complexity
    # - Parameter count and types
    # - Suspend modifier presence
    # - Generic type parameters
done
```

#### Property Analysis
```kotlin
// Extract property definitions
grep -E "^\s*val\s+|^\s*var\s+" <interface_file> | while read property; do
    echo "Property: $property"
    # Analyze:
    # - Type complexity
    # - Nullability
    # - Generic type usage
done
```

#### Generic Analysis
```kotlin
// Extract type parameters
grep -E "<.*>" <interface_file> | while read generic; do
    echo "Generic: $generic"
    # Analyze:
    # - Interface-level generics
    # - Method-level generics
    # - Type constraints
    # - Variance annotations
done
```

### Analysis Output

#### Basic Structure Report
```
Interface Analysis Report: UserService
=====================================

📋 Interface Overview:
- Name: UserService
- Package: com.example.service
- @Fake annotation: ✅ Present
- Type parameters: <T : Entity>

📋 Methods (3 total):
1. suspend fun getUser(id: String): Result<User>
   - Suspend: ✅
   - Generics: Result<User>
   - Parameters: id: String
   - Complexity: Medium (suspend + generic return)

2. fun updateUser(user: User): Boolean
   - Suspend: ❌
   - Generics: ❌
   - Parameters: user: User
   - Complexity: Low

3. fun <T> processData(data: T): T
   - Suspend: ❌
   - Generics: ✅ Method-level <T>
   - Parameters: data: T
   - Complexity: High (method-level generic)

📋 Properties (2 total):
1. val currentUser: User?
   - Type: User?
   - Nullable: ✅
   - Complexity: Low

2. val isActive: Boolean
   - Type: Boolean
   - Nullable: ❌
   - Complexity: Low
```

#### Generation Complexity Assessment
```
📋 Generation Complexity Analysis:
- Overall Complexity: HIGH
- Challenges Identified:
  ✅ Method-level generics detected (<T> in processData)
  ✅ Suspend functions present
  ✅ Generic return types (Result<User>)
  ⚠️  Nullable properties require null handling

📋 Recommended Generation Strategy:
1. Use unified IR-native approach
2. Method-level generics require Phase 2A casting
3. Suspend functions fully supported
4. Nullable types need proper default values
```

#### Type Resolution Analysis
```
📋 Type Resolution Requirements:
- Simple types: String, Boolean ✅
- Complex types: User, Result<User> ✅
- Generic types: T (method-level) ⚠️ Needs casting
- Nullable types: User? ✅

📋 Default Value Strategy:
- String → ""
- Boolean → false
- User → null (nullable)
- Result<User> → Result.success("")
- T → identity function with casting
```

### Integration with Generation Pipeline

#### IR Analysis Integration
```kotlin
@Test
fun `GIVEN complex interface WHEN analyzing structure THEN should identify all components`() = runTest {
    // Given
    val interfaceStructure = analyzeInterface("UserService")

    // When
    val analysis = structuralAnalyzer.analyze(interfaceStructure)

    // Then
    assertEquals(3, analysis.methods.size)
    assertEquals(2, analysis.properties.size)
    assertTrue(analysis.hasMethodLevelGenerics)
    assertTrue(analysis.hasSuspendFunctions)
    assertEquals(GenerationComplexity.HIGH, analysis.complexity)
}
```

#### Generation Planning
```kotlin
@Test
fun `GIVEN analyzed interface WHEN planning generation THEN should create appropriate strategy`() = runTest {
    // Given
    val analysis = analyzeInterface("UserService")

    // When
    val strategy = generationPlanner.createStrategy(analysis)

    // Then
    assertEquals(StrategyType.UNIFIED_IR_NATIVE, strategy.type)
    assertTrue(strategy.requiresPhase2ACasting)
    assertTrue(strategy.supportsSuspendFunctions)
    assertEquals(4, strategy.requiredDefaultValues.size)
}
```

### Command Workflow

#### Step 1: Interface Discovery
```bash
# Find interface files
find . -path "*/src/*/kotlin/*" -name "*.kt" -exec grep -l "@Fake" {} \;

# Extract interface names
grep -h "@Fake" <files> -A 1 | grep "interface" | sed 's/.*interface \([A-Za-z0-9_]*\).*/\1/'
```

#### Step 2: Structural Extraction
```bash
# For each interface, extract complete structure
for interface in $interfaces; do
    echo "Analyzing $interface..."
    analyze_interface_structure "$interface"
done
```

#### Step 3: Generation Assessment
```bash
# Assess generation complexity and requirements
assess_generation_complexity <interface_analysis>

# Provide recommendations
recommend_generation_strategy <complexity_assessment>
```

### Error Handling

#### Common Issues
```
❌ Interface not found:
   → Check interface name spelling
   → Verify @Fake annotation presence
   → Ensure interface is in Kotlin source

❌ Complex generics detected:
   → Method-level generics require Phase 2A
   → Consider interface-level generics instead
   → Review generic scoping analysis

❌ Unsupported patterns:
   → Inline functions not supported
   → Operator overloading limited support
   → Extension functions not applicable
```

### Related Documentation

- **[📋 Generic Scoping Analysis](.claude/docs/analysis/generic-scoping-analysis.md)** - Deep dive into generic challenges
- **[📋 Type Safety Validation](.claude/docs/validation/type-safety-validation.md)** - Type system testing
- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN patterns

### Example Usage

```bash
# Basic interface analysis
/analyze-interface-structure UserService

# Complex interface with generics
/analyze-interface-structure Repository<User>

# Suspend function analysis
/analyze-interface-structure AsyncDataService

# Multi-interface analysis
for interface in UserService DataService AuthService; do
    /analyze-interface-structure $interface
done
```

---

**This command provides comprehensive interface structure analysis to inform generation strategy decisions and identify potential challenges before implementation.**