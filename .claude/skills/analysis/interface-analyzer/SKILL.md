---
name: interface-analyzer
description: Deep structural analysis of @Fake annotated interfaces examining method signatures, property definitions, generic type parameters, suspend functions, complexity assessment, and generation strategy recommendations. Use when analyzing interface structure, examining method signatures, checking generic patterns, assessing generation complexity, or when user mentions "analyze interface", "interface structure", "check methods", "assess complexity", interface names, or "generation strategy".
allowed-tools: [Read, Grep, Glob, Bash]
---

# Interface Structure Deep Analyzer

Comprehensive @Fake interface structural analysis with generation complexity assessment and strategy recommendations.

## Core Mission

Analyzes the structural characteristics of @Fake annotated interfaces to understand:
- Method signatures and parameter types
- Property definitions and types
- Generic type parameters and constraints
- Suspend function usage patterns
- Complex type relationships
- Generation complexity and recommended strategy

## Instructions

### 1. Identify Target Interface

**Extract from conversation:**
- Interface name from user's message
- Look for patterns: "analyze UserService", "check AsyncDataService structure", "examine Repository interface"
- Common targets: Service interfaces, Repository interfaces, Data access interfaces

**If unclear or missing:**
```
Ask: "Which interface would you like me to analyze?"
Suggest: Check recent @Fake interfaces | Analyze all | Specific name
```

### 2. Locate Interface Definition

**Search in source code:**
```bash
# Find interface file
find . -path "*/src/*/kotlin/*" -name "*.kt" -exec grep -l "interface ${INTERFACE_NAME}" {} \;

# Common locations:
# - src/commonMain/kotlin/ (KMP)
# - src/main/kotlin/ (JVM)
# - src/test/kotlin/ or src/commonTest/kotlin/ (test interfaces)
```

**Verify @Fake annotation:**
```bash
# Check for @Fake annotation
grep -B 5 "interface ${INTERFACE_NAME}" ${INTERFACE_FILE} | grep "@Fake"
```

**If not found:**
```
❌ ERROR: Interface '${INTERFACE_NAME}' not found

💡 Suggestions:
1. Check spelling (case-sensitive)
2. Verify interface exists in source
3. Check if @Fake annotation is present
4. Try: find . -name "*.kt" -exec grep -l "interface.*Service" {} \;
```

### 3. Extract Interface Definition

**Read interface file:**
```bash
Read ${INTERFACE_FILE}
```

**Extract complete interface:**
```kotlin
// Look for pattern:
@Fake
interface ${INTERFACE_NAME}<Generic Parameters> : SuperType {
    // Properties
    // Methods
    // Nested declarations
}
```

**Parse key components:**
- [ ] Package declaration
- [ ] Imports (for type resolution)
- [ ] @Fake annotation presence
- [ ] Interface name
- [ ] Generic type parameters (if any)
- [ ] Supertype(s) (if any)
- [ ] Property declarations
- [ ] Method declarations
- [ ] Nested types/interfaces

### 4. Analyze Method Signatures

**Extract all methods:**

**For each method:**
```kotlin
fun methodName(param: Type): ReturnType
suspend fun asyncMethod(param: Type): ReturnType
fun <T> genericMethod(data: T): T
```

**Analyze each method:**

**Signature structure:**
```
📋 METHOD: ${method_name}

Signature: ${full_signature}

Components:
- Modifiers: suspend? | operator? | infix?
- Method-level generics: <T, R>? | none
- Parameters: (name: Type, ...)
- Return type: ReturnType
- Nullability: nullable? | non-null?
```

**Complexity indicators:**
- **Low**: Simple types (String, Int, Boolean), no generics
- **Medium**: Complex types (User, Result<T>), suspend functions
- **High**: Method-level generics, function types, complex constraints

**Example analysis:**
```
📋 METHOD: getUser

Signature: suspend fun getUser(id: String): Result<User>

Components:
- Modifiers: suspend ✅
- Method-level generics: none
- Parameters: (id: String)
- Return type: Result<User>
- Nullability: non-null

Complexity: MEDIUM
Reason: Suspend function + generic return type (Result<User>)
Strategy: Supported in Phase 1 ✅
```

### 5. Analyze Property Definitions

**Extract all properties:**

**For each property:**
```kotlin
val readOnlyProp: Type
var mutableProp: Type
val nullableProp: Type?
```

**Analyze each property:**

```
📋 PROPERTY: ${property_name}

Declaration: ${full_declaration}

Components:
- Mutability: val (read-only) | var (mutable)
- Type: ${type}
- Nullability: nullable? | non-null?
- Getter/Setter: custom? | default?

Default value strategy: ${default}
```

**Default value mapping:**
```
String → ""
Int, Long → 0
Boolean → false
Nullable (Type?) → null
Collections → emptyList() / emptySet() / emptyMap()
Complex types → null or Type()
```

**Example analysis:**
```
📋 PROPERTY: currentUser

Declaration: val currentUser: User?

Components:
- Mutability: val (read-only)
- Type: User
- Nullability: nullable ✅
- Getter/Setter: default

Default value strategy: null
Complexity: LOW
```

### 6. Analyze Generic Type Parameters

**Classify generic patterns:**

**Interface-level generics:**
```kotlin
interface Repository<T> {
    fun save(item: T): T
}

📋 GENERIC ANALYSIS: Repository<T>

Classification: Interface-level generic
Type parameters: T (class-level)
Scope: Available throughout interface
Methods using T: save (parameter and return)

Phase 1 Status: ⚠️ Type erasure (T becomes Any)
Phase 2B Solution: Generic fake class FakeRepository<T>
```

**Method-level generics:**
```kotlin
interface DataService {
    fun <T> process(data: T): T
}

📋 GENERIC ANALYSIS: DataService

Classification: Method-level generic
Type parameters: none (interface), T (method-level)
Scope: T only accessible within process() method

Phase 1 Status: ❌ Scoping challenge
Phase 2A Solution: Identity function + dynamic casting
```

**Mixed generics:**
```kotlin
interface CacheService<K, V> {
    fun get(key: K): V?
    fun <R : V> compute(key: K, fn: (K) -> R): R
}

📋 GENERIC ANALYSIS: CacheService<K, V>

Classification: Mixed (interface + method level)
Interface parameters: K, V
Method parameters: R (with constraint R : V)

Complexity: HIGH
Phase 2A: Handle method-level R
Phase 2B: Handle interface-level K, V
```

**Generic complexity scoring:**
```
No generics: LOW
Interface-level only: MEDIUM (Phase 2B)
Method-level only: MEDIUM (Phase 2A)
Mixed generics: HIGH (Phase 2A + 2B)
Complex constraints: VERY HIGH (Phase 3)
```

### 7. Detect Special Patterns

**Suspend functions:**
```kotlin
suspend fun fetchData(): Result<Data>

✅ PATTERN: Suspend function
Support: Phase 1 (fully supported)
Generation: Behavior property must also be suspend
```

**Function types:**
```kotlin
fun onClick(handler: (Event) -> Unit)

✅ PATTERN: Function type parameter
Support: Phase 1 (fully supported)
Generation: Smart default = empty lambda { }
```

**Nullable types:**
```kotlin
fun findUser(id: String): User?

✅ PATTERN: Nullable return type
Support: Phase 1 (fully supported)
Default: null
```

**Collections:**
```kotlin
fun getAllUsers(): List<User>

✅ PATTERN: Collection return type
Support: Phase 1 (fully supported)
Default: emptyList()
```

### 8. Assess Generation Complexity

**Generate complexity report:**

```
═══════════════════════════════════════════════════
📊 INTERFACE STRUCTURE ANALYSIS: ${INTERFACE_NAME}
═══════════════════════════════════════════════════

📋 OVERVIEW:
- Name: ${INTERFACE_NAME}
- Package: ${package}
- @Fake annotation: ✅ Present | ❌ Missing
- Type parameters: ${generic_params} | none
- Supertypes: ${supertypes} | none

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 METHODS (${method_count} total):

1. ${method_name_1}
   Signature: ${full_signature}
   Complexity: ${LOW|MEDIUM|HIGH}
   Reason: ${explanation}
   Support: Phase 1 ✅ | Phase 2A ⚠️ | Phase 2B 🔮

2. ${method_name_2}
   ...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 PROPERTIES (${property_count} total):

1. ${property_name_1}
   Type: ${type}
   Nullable: ${yes|no}
   Default: ${default_value}
   Complexity: ${LOW|MEDIUM}

2. ${property_name_2}
   ...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 GENERIC TYPE ANALYSIS:

Classification: ${NONE|INTERFACE|METHOD|MIXED}
Parameters: ${list}
Scoping: ${description}
Complexity: ${LOW|MEDIUM|HIGH|VERY HIGH}

Phase Support:
- Phase 1: ${supported_features}
- Phase 2A needed: ${yes|no} (${reason})
- Phase 2B needed: ${yes|no} (${reason})

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 SPECIAL PATTERNS DETECTED:
- ✅ Suspend functions: ${count}
- ✅ Function types: ${count}
- ✅ Nullable types: ${count}
- ✅ Collections: ${count}
- ⚠️ Complex generics: ${count}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 OVERALL COMPLEXITY ASSESSMENT:

Complexity: ${LOW|MEDIUM|HIGH|VERY HIGH}

Breakdown:
- Method complexity: ${avg_method_complexity}
- Generic complexity: ${generic_complexity}
- Type complexity: ${type_complexity}
- Special patterns: ${special_pattern_impact}

═══════════════════════════════════════════════════
```

### 9. Recommend Generation Strategy

**Based on complexity assessment:**

**Low Complexity Example:**
```
🎯 RECOMMENDED GENERATION STRATEGY: ${INTERFACE_NAME}

Complexity: LOW

✅ Phase 1 Support: FULL
- All methods have simple signatures
- No generic type parameters
- Standard types (String, Int, Boolean)
- Nullable types handled

Generation approach:
1. Use unified IR-native generation
2. Smart defaults for all types
3. Standard DSL configuration
4. Expected success: 100%

Next steps:
1. Generate fake with current plugin
2. Verify compilation
3. Write GIVEN-WHEN-THEN tests
```

**Medium Complexity (Suspend + Generics):**
```
🎯 RECOMMENDED GENERATION STRATEGY: ${INTERFACE_NAME}

Complexity: MEDIUM

✅ Phase 1 Support: PARTIAL
- Suspend functions: ✅ Fully supported
- Generic return types (Result<T>): ✅ Supported
- Method-level generics: ⚠️ Requires Phase 2A

Generation approach:
1. Generate with current plugin (Phase 1)
2. Expect type erasure for generics (T → Any)
3. Plan Phase 2A upgrade for full type safety

Workarounds:
- Use interface-level generics instead of method-level
- Accept Any casting with @Suppress annotations
- Document type safety limitations

Expected success: 85%
```

**High Complexity (Mixed Generics):**
```
🎯 RECOMMENDED GENERATION STRATEGY: ${INTERFACE_NAME}

Complexity: HIGH

⚠️ Phase 1 Support: LIMITED
- Interface-level generics: ⚠️ Type erasure
- Method-level generics: ❌ Scoping issues
- Complex constraints: ❌ Not fully supported

Recommended path:
1. Simplify interface for Phase 1:
   - Remove method-level generics
   - Use concrete types
   - Split into multiple simpler interfaces

2. OR wait for Phase 2:
   - Phase 2A: Method-level generics (2-3 weeks)
   - Phase 2B: Interface-level generics (2-3 months)

Complexity reduction:
- Original: interface Cache<K, V> { fun <R> compute(...): R }
- Simplified: interface StringCache { fun compute(...): String }

Expected success: 60% (original) vs 100% (simplified)
```

### 10. Provide Actionable Next Steps

**Based on analysis:**

**If fully supported:**
```
✅ NEXT STEPS:

1. Generate fake implementation:
   ./gradlew :module:compileKotlinJvm

2. Verify generated code:
   cat build/generated/fakt/test/kotlin/Fake${INTERFACE_NAME}Impl.kt

3. Write tests:
   @Test
   fun `GIVEN ${INTERFACE_NAME} fake WHEN ...` = runTest { ... }

4. Use in tests:
   val fake = fake${INTERFACE_NAME} {
       ${method_name} { ${behavior} }
   }
```

**If requires workarounds:**
```
⚠️ NEXT STEPS:

1. Review generic scoping analysis:
   Use generic-scoping-analyzer Skill

2. Consider simplifications:
   - Option A: Use interface-level generics
   - Option B: Use concrete types
   - Option C: Wait for Phase 2A/2B

3. If proceeding with limitations:
   - Document type safety trade-offs
   - Add @Suppress annotations where needed
   - Plan migration to Phase 2

4. Track in roadmap:
   .claude/docs/implementation/generics/complex-generics-strategy.md
```

## Supporting Files

Progressive disclosure for interface analysis:

- **`resources/structural-patterns.md`** - Common interface patterns and idioms (loaded on-demand)
- **`resources/complexity-assessment.md`** - Detailed complexity scoring logic (loaded on-demand)
- **`resources/generation-strategies.md`** - Strategy selection guide and decision tree (loaded on-demand)

## Related Skills

This Skill composes with:
- **`kotlin-api-consultant`** - Validate Kotlin API usage in interface
- **`generic-scoping-analyzer`** - Deep dive into generic challenges
- **`compilation-validator`** - Validate generated code after analysis
- **`kotlin-ir-debugger`** - Debug IR generation for complex interfaces

## Analysis Categories

### By Complexity
- **Simple**: No generics, basic types, no special patterns
- **Moderate**: Suspend functions, nullable types, collections
- **Complex**: Generics (interface or method level)
- **Very Complex**: Mixed generics, complex constraints

### By Pattern
- **Data Access**: Repository, DAO patterns
- **Services**: Business logic interfaces
- **Utilities**: Helper/tool interfaces
- **Event Handlers**: Callback/listener interfaces

## Best Practices

1. **Analyze before generating** - Understand complexity upfront
2. **Check generic patterns** - Biggest source of complexity
3. **Assess Phase support** - Know what's supported when
4. **Recommend simplifications** - When appropriate
5. **Provide clear next steps** - Actionable guidance

## Common Interface Patterns

### Pattern: Simple Service
```kotlin
@Fake
interface UserService {
    fun getUser(id: String): User
    fun saveUser(user: User): Boolean
}
```
**Complexity**: LOW (Phase 1 ✅)

### Pattern: Async Service
```kotlin
@Fake
interface AsyncDataService {
    suspend fun fetchData(): Result<Data>
    suspend fun saveData(data: Data): Result<Unit>
}
```
**Complexity**: MEDIUM (Phase 1 ✅ - suspend supported)

### Pattern: Generic Repository
```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
    fun findById(id: String): T?
}
```
**Complexity**: MEDIUM (Phase 2B needed for full type safety)

### Pattern: Complex Generics
```kotlin
@Fake
interface CacheService<K, V> {
    fun get(key: K): V?
    fun <R : V> compute(key: K, fn: (K) -> R): R
}
```
**Complexity**: HIGH (Phase 2A + 2B needed)

## Quick Analysis

**One-liner for simple checks:**
```bash
# Count methods
grep -c "fun " ${INTERFACE_FILE}

# Check for generics
grep -E "<.*>" ${INTERFACE_FILE}

# Check for suspend
grep -c "suspend fun" ${INTERFACE_FILE}
```

## Error Handling

### Interface Not Found
```
❌ Interface not found: ${INTERFACE_NAME}

Debugging:
1. Check spelling
2. Verify @Fake annotation
3. Search all Kotlin files:
   find . -name "*.kt" -exec grep -l "interface ${INTERFACE_NAME}" {} \;
```

### Ambiguous Interface Name
```
⚠️ Multiple interfaces found: ${INTERFACE_NAME}

Found:
1. com.example.service.UserService
2. com.example.data.UserService

Please specify full package name
```

## Performance Notes

- Interface file read: ~1-2 seconds
- Method/property extraction: ~2-5 seconds
- Generic analysis: ~5-10 seconds
- Total analysis: ~10-20 seconds per interface

Fast enough for interactive development!
