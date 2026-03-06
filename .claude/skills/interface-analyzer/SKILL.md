---
name: interface-analyzer
description: Deep structural analysis of @Fake annotated interfaces — method signatures, properties, generics, suspend functions, complexity scoring, and generation strategy. Use when analyzing interfaces for fake generation, assessing complexity, understanding what code will be generated for an interface, or debugging unexpected generated output. Make sure to use this skill whenever you need to understand an interface's structure before modifying generation logic — it reveals edge cases like nested generics and suspend overloads that affect code generation.
allowed-tools: Read, Grep, Glob, Bash
---

# Interface Structure Deep Analyzer

Structural analysis of @Fake interfaces with complexity assessment and generation strategy recommendations.

## Instructions

### 1. Identify Target Interface

**Extract from conversation:**
- "analyze UserService", "check AsyncDataService structure"
- If unclear, ask: "Which interface would you like me to analyze?"

### 2. Locate Interface

```bash
# Find interface file
find . -path "*/src/*/kotlin/*" -name "*.kt" -exec grep -l "interface ${INTERFACE_NAME}" {} \;

# Verify @Fake annotation
grep -B 5 "interface ${INTERFACE_NAME}" ${INTERFACE_FILE} | grep "@Fake"
```

**If not found:**
```
Interface '${INTERFACE_NAME}' not found
1. Check spelling (case-sensitive)
2. Verify @Fake annotation present
3. Try: find . -name "*.kt" -exec grep -l "interface.*Service" {} \;
```

### 3. Extract & Analyze

**Read the interface file and parse:**
- Package declaration and imports
- Generic type parameters (if any)
- Supertypes (if any)
- All property declarations
- All method declarations

### 4. Analyze Methods

**For each method, document:**

```
METHOD: ${name}
Signature: ${full_signature}
Modifiers: suspend? | operator? | infix?
Method-level generics: <T, R>? | none
Parameters: (name: Type, ...)
Return type: ReturnType
Complexity: LOW | MEDIUM | HIGH
```

**Complexity indicators:**
- LOW: Simple types (String, Int, Boolean), no generics
- MEDIUM: Complex types (User, Result<T>), suspend functions
- HIGH: Method-level generics, function types, complex constraints

### 5. Analyze Properties

**For each property:**
```
PROPERTY: ${name}
Declaration: val/var ${name}: Type
Nullable: yes/no
Default strategy: "" | 0 | false | null | emptyList() | emptyMap()
```

### 6. Analyze Generic Types

**Classify patterns:**

| Pattern | Example | Complexity | Support |
|---------|---------|-----------|---------|
| No generics | `interface UserService` | LOW | Phase 1 ✅ |
| Interface-level | `interface Repository<T>` | MEDIUM | Type erasure (T→Any) |
| Method-level | `fun <T> process(data: T): T` | MEDIUM | Scoping challenge |
| Mixed | `interface Cache<K,V> { fun <R:V> compute(...): R }` | HIGH | Phase 2A+2B |

### 7. Detect Special Patterns

- **Suspend functions** → Phase 1 ✅, behavior must also be suspend
- **Function type params** → Phase 1 ✅, default = empty lambda `{}`
- **Nullable returns** → Phase 1 ✅, default = `null`
- **Collections** → Phase 1 ✅, default = `emptyList()` / `emptySet()` / `emptyMap()`

### 8. Complexity Assessment

**Scoring:**
```
No generics + simple types       = LOW
Suspend + generic return types   = MEDIUM
Interface-level generics         = MEDIUM (Phase 2B)
Method-level generics            = MEDIUM (Phase 2A)
Mixed generics + constraints     = HIGH (Phase 2A+2B)
```

### 9. Generation Strategy

**Based on complexity:**

**LOW** — Full Phase 1 support, 100% expected success
- Generate with current plugin, verify compilation, write tests

**MEDIUM** — Partial support, may need workarounds
- Suspend: fully supported
- Generic return types (Result<T>): supported
- Interface-level generics: type erasure (T→Any)

**HIGH** — Limited support, consider simplifications
- Split into simpler interfaces
- Use concrete types instead of generics
- Or wait for Phase 2A/2B

### 10. Report

```
INTERFACE ANALYSIS: ${INTERFACE_NAME}

Overview:
- Package: ${package}
- @Fake: present/missing
- Type params: ${list} | none
- Methods: ${count} | Properties: ${count}

Methods:
1. ${name} — ${complexity} — ${support_status}
...

Properties:
1. ${name}: ${type} — default: ${default}
...

Generics: ${NONE|INTERFACE|METHOD|MIXED}
Special Patterns: suspend(${n}), function types(${n}), nullable(${n}), collections(${n})

COMPLEXITY: ${LOW|MEDIUM|HIGH}
STRATEGY: ${recommendation}

Next steps:
- ${actionable items}
```

## Supporting Files

- **`resources/structural-patterns.md`** — Common interface patterns
- **`resources/complexity-assessment.md`** — Detailed scoring logic
- **`resources/generation-strategies.md`** — Strategy selection guide

## Related Skills

- **`kotlin-api-consultant`** — Validate Kotlin API usage
- **`compilation`** — Validate generated code after analysis
