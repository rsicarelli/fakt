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

| Pattern | Example | Complexity |
|---------|---------|-----------|
| No generics | `interface UserService` | LOW |
| Interface-level | `interface Repository<T>` | MEDIUM |
| Method-level | `fun <T> process(data: T): T` | MEDIUM |
| Mixed / constrained | `interface Cache<K,V> { fun <R:V> compute(...): R }` | HIGH |

Class-level generics, method-level generics and constraints are supported (`docs/get-started/features.md`); check `docs/user-guide/known-issues.md` for open cases.

### 7. Detect Special Patterns

- **Suspend functions** → behavior must also be suspend
- **Function type params** → default = empty lambda `{}`
- **Nullable returns** → default = `null`
- **Collections** → default = `emptyList()` / `emptySet()` / `emptyMap()`

### 8. Complexity Assessment

**Scoring:**
```
No generics + simple types       = LOW
Suspend + generic return types   = MEDIUM
Interface-level generics         = MEDIUM
Method-level generics            = MEDIUM
Mixed generics + constraints     = HIGH
```

### 9. Generation Strategy

Complexity says where to look first when output is wrong, not whether generation is supported:

- **LOW** — generate, verify compilation, write tests
- **MEDIUM** — also inspect the generated signatures for suspend and generic return types
- **HIGH** — check type-parameter scoping and bounds in the generated fake; if it fails, reduce to a minimal reproducer and check known issues

### 10. Report

```
INTERFACE ANALYSIS: ${INTERFACE_NAME}

Overview:
- Package: ${package}
- @Fake: present/missing
- Type params: ${list} | none
- Methods: ${count} | Properties: ${count}

Methods:
1. ${name} — ${complexity}
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
