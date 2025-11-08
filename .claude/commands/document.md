---
allowed-tools: Read, Edit, Grep, Glob, TodoWrite, Task
argument-hint: [target] (optional - specific file/class to document, default: current context)
description: Professional KDoc documentation system for Kotlin compiler plugins with JetBrains standards, Three Audiences Model (User/Contributor/Maintainer), inline comments cleanup, block comments migration, ADR creation, TODO/FIXME issue linking, Dokka generation, explicitApi enforcement, @param/@return/@throws/@sample tags, public API documentation, internal utility documentation, maintainer notes for complex IR/FIR transformations, code comment refactoring, documentation quality validation, Detekt KDoc linting, and Metro-aligned documentation patterns
model: claude-sonnet-4-20250514
---

# 📚 Professional Kotlin Documentation System

**JetBrains-grade KDoc standards for compiler plugins with audience-driven documentation**

## 📚 Context Integration

**This command leverages:**
- Gemini Deep Research on Kotlin documentation best practices
- JetBrains KDoc standards and conventions
- Metro DI framework documentation patterns
- Kotlin stdlib documentation style guide
- Dokka documentation engine best practices
- `.claude/docs/development/metro-alignment.md` - Architectural patterns

**🏆 DOCUMENTATION BASELINE:**
- Three Audiences Model (User → Contributor → Maintainer)
- Documentation Responsibility Matrix
- 5 Essential KDoc Patterns
- Automated tooling enforcement (explicitApi + Detekt)
- Zero tolerance for "polluted" inline planning comments

## 📋 Core Philosophy: The Three Audiences Model

**ALL documentation must target ONE specific audience:**

### 1. 👤 User (Consumer)
- **Who:** Developer using the `@Fake` annotation to generate test fakes
- **Reads:** Generated Dokka website
- **Needs:** What the public API does and how to use it
- **Rule:** NEVER expose implementation details or architectural trade-offs

### 2. 🤝 Contributor (Collaborator)
- **Who:** Open-source developer fixing bugs or adding features
- **Reads:** Internal KDoc, inline `//` comments, TODO/FIXME
- **Needs:** Internal structure, utility function purposes, work locations
- **Rule:** Understand structure without deep architectural context

### 3. 🏗️ Maintainer (Architect)
- **Who:** Project lead or deeply embedded collaborator
- **Reads:** Formal `/* Maintainer Notes */` and ADRs
- **Needs:** Deep architectural "why" of complex IR/FIR transformations
- **Rule:** Preserve critical, high-context knowledge for complex/fragile code

---

## 📊 Documentation Responsibility Matrix

| Artifact | Syntax | Audience | Purpose | Example |
|----------|--------|----------|---------|---------|
| **Public KDoc** | `/** ... */` | User | Public Contract (what API does) | `/** Marks a class for fake generation. */` |
| **Internal KDoc** | `/** ... */` | Contributor | Internal Purpose (utility effect) | `/** Checks if IrType is marked nullable. */` |
| **Inline Comment** | `// ...` | Contributor | Immediate Rationale (why this line) | `// Must check ?, type.isNullable() unreliable` |
| **Maintainer Note** | `/* ... */` | Maintainer | Complex Implementation (deep why/how) | `/* == MAINTAINER NOTE: IR Transform == */` |
| **ADR** | `.claude/docs/adr/*.md` | Maintainer | Architectural Decision (system why) | `ADR-001: Choice of IR Backend` |
| **Issue Tracker** | GitHub Issues | Contributor | Future Work (TODO/FIXME) | `// TODO(fakt): Fix memory leak, see #123` |

---

## ✅ KDoc Technical Specification

### Core Syntax Rules

```kotlin
/**
 * Summary paragraph (first paragraph, ends at blank line).
 * Used for tooltips and API list summaries.
 *
 * Detailed description starts here after blank line.
 * Can use **bold**, *italic*, `inline code`, and lists.
 *
 * @param T the type of element (NOT just "@param T")
 * @param name the member to add (NOT just "the name parameter")
 * @return the new size of the group (NOT just "returns Int")
 * @throws IOException if the file cannot be read (NOT just "throws IOException")
 * @see [RelatedClass] for related functionality
 * @sample com.rsicarelli.fakt.samples.UserServiceFakeSample
 * @since 1.0.0
 */
```

### ✅ DO:
- Link code elements with `[ElementName]` or `[kotlin.reflect.KClass.properties]`
- Use Markdown for formatting (`**bold**`, `*italic*`, `` `code` ``)
- Document generic type parameters with `@param <T>`
- Use `@sample` to embed verifiable, compilable examples
- Write descriptive tag content (explain semantics, NOT syntax)
- Document `@receiver` for extension functions
- Include `@since` for versioned APIs

### ❌ DON'T:
- Use Javadoc-style `{@link}` or `{@code}` (NOT supported in Kotlin)
- Write obvious documentation (`@return Int` without explaining what it represents)
- Skip `@receiver` for extension functions
- Omit `@throws` for functions that can throw exceptions
- State the obvious ("this function increments the counter")
- Mix audience concerns (User + Contributor + Maintainer)

---

## 🎨 The 5 Essential KDoc Patterns

### Pattern 1: Public API (User-Facing)

**Rule:** ALL public/protected declarations MUST have KDoc.

**✅ Example:**
```kotlin
/**
 * Marks an interface or abstract class for test fake generation.
 *
 * When applied, the **Fakt** compiler plugin will generate a concrete
 * implementation of this class, named `Fake${OriginalName}Impl`. This generated
 * class will provide default, non-null implementations for all abstract
 * properties and functions.
 *
 * Example of generated code:
 * ```kotlin
 * // Given:
 * @Fake
 * interface UserService {
 *     fun getUser(id: String): User
 * }
 *
 * // Fakt generates:
 * class FakeUserServiceImpl : UserService {
 *     override fun getUser(id: String): User = TODO("Stub")
 * }
 * ```
 *
 * @sample com.rsicarelli.fakt.samples.UserServiceFakeSample
 * @see FakeConfig for advanced configuration options
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
annotation class Fake
```

**Analysis:** Tells User WHAT it does, WHAT gets generated, includes inline example + verifiable sample.

---

### Pattern 2: Internal Utility (Contributor-Facing)

**Rule:** Document purpose and non-obvious constraints. Skip if self-evident.

**❌ DON'T (Over-documented):**
```kotlin
/**
 * This function checks if an IrType is nullable. It does this by
 * casting the type to an IrSimpleType and then checking if the
 * hasQuestionMark property is true. It is internal.
 */
internal fun IrType.isNullable(): Boolean { ... }
```

**✅ DO (Concise, valuable):**
```kotlin
/**
 * Checks if this IR type is syntactically marked as nullable (e.g., `String?`).
 *
 * Note: This checks for the explicit `?` mark. It does not resolve type
 * parameters or flexible platform types.
 */
internal fun IrType.isNullable(): Boolean {
    // Cast needed because IrType interface doesn't expose hasQuestionMark
    val simpleType = this as? IrSimpleType ?: return false
    return simpleType.hasQuestionMark
}
```

**Analysis:** Clarifies scope/limitations for Contributors. Inline comment explains non-obvious cast.

---

### Pattern 3: Complex Transformation (Maintainer-Facing)

**Rule:** Use layered documentation: KDoc for Contributors + formal `/* Maintainer Note */` inside function.

```kotlin
/**
 * Generates the concrete fake implementation of the annotated class,
 * synthesizing all necessary properties and functions.
 *
 * This is the core IR transformation for Fakt's code generation pipeline.
 */
internal fun IrClassBuilder.generateFakeImplementation() {
    /*
     * == MAINTAINER NOTE: IR Fake Implementation Generation ==
     *
     * AUDIENCE: Future maintainers of Fakt (e.g., you).
     *
     * CONTEXT: This function is the core of the `IrGenerationExtension`. It is
     *          called after the `@Fake` annotation is resolved and we have a
     *          symbol for the user's abstract class.
     *
     * GOAL: To synthesize a new `IrClass` that subclasses the user's
     *       annotated class and provides concrete implementations for all
     *       abstract members.
     *
     * HOW IT WORKS (THE "WHY"):
     * The process is a multi-step IR transformation:
     * 1. Symbol Creation: We first create a new `IrClassSymbol` for our
     *    fake class. This is critical for the linker to find our new class.
     * 2. Member Iteration: We walk the original class's `declarations`
     *    and filter for abstract `IrFunction` and `IrProperty` nodes.
     * 3. Body Generation: For each abstract member, we must generate a
     *    synthetic body (e.g., `return 0` for Int, `TODO(...)` for
     *    complex types). This is the most fragile part.
     *
     * FRAGILE ASSUMPTIONS (READ BEFORE EDITING):
     * - This code *heavily* assumes the K2 IR backend. It may break
     *   with future Kotlin versions and require rewrite.
     * - This transformation *must* run at the correct phase, otherwise
     *   we will fail to generate correct code.
     *
     * SEE ALSO:
     * - ADR-001: Choice of IR vs. FIR
     * - Issue #42: Tracking K2 compatibility
     */

    // Create the symbol for the new 'Fake...Impl' class
    val fakeClassSymbol = ...

    // ... complex IR manipulation code ...
}
```

**Analysis:** Three layers:
1. Concise KDoc for Contributors (what it does)
2. Detailed maintainer note (why it's complex, fragile assumptions)
3. Short inline comments (what specific lines do)

---

### Pattern 4: Architectural Decision (Maintainer-Facing)

**Rule:** Externalize "why we chose this approach" to ADRs. Delete long code comments.

**File:** `.claude/docs/adr/001-choice-of-ir-backend.md`

```markdown
# ADR-001: Choice of IR Backend for Transformation

**Date:** 2025-01-05
**Status:** Accepted

## Context

We must choose a compiler API to perform code generation for Fakt. The primary
options are the K1 IR backend or the new K2/FIR frontend.

## Decision

We will use the Kotlin IR backend (via `IrGenerationExtension`) for all code
transformations in Fakt version 1.x.

## Consequences

**Pros:**
- IR provides a unified backend for all platforms (JVM, JS, Native), which is
  a core goal for a multiplatform library
- Proven pattern from Metro DI framework

**Cons:**
- The plugin will require updates for K2 compiler changes
- IR API is experimental and historically poorly documented, increasing
  maintenance burden

**Mitigation:**
- Track K2 compatibility in Issue #42
- Maintain comprehensive maintainer notes for complex IR transformations
```

**In code, replace 20-line comment with:**
```kotlin
// See ADR-001 for rationale on using IR backend
```

---

### Pattern 5: Internal-but-Public API (Advanced)

**Rule:** Use `@RequiresOptIn` for unstable internal APIs that might be useful to plugin authors.

```kotlin
/**
 * Marks APIs that are internal to Fakt's implementation.
 * These APIs are not stable and may change or be removed
 * without notice in any release. Use at your own risk.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an internal Fakt API and is not guaranteed to be stable."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalFaktApi

/**
 * A utility function for dumping the state of an IR tree.
 * This is public for use in test modules but is not stable.
 *
 * @receiver the IR element to dump
 * @return a formatted string representation of the IR tree
 */
@InternalFaktApi
fun IrElement.dumpTree(): String { ... }
```

**Usage (forces explicit opt-in):**
```kotlin
@OptIn(InternalFaktApi::class)
fun myTestHelper() {
    myIrElement.dumpTree()  // Now allowed, but developer was warned
}
```

---

## 🔄 Migration Strategy: Cleaning Existing Code

### Step 1: Audit (Categorize All Comments)

Search for `//` and `/*` project-wide. Categorize each comment:

1. **PLANNING** → Keywords: `TODO`, `FIXME`, `HACK`, `NOTE`, "we should...", "this could be better..."
2. **DESIGN** → Keywords: "why we chose...", "this is better than...", "the architecture is..."
3. **IMPLEMENTATION** → Keywords: "how this works:", "this is a hack because...", "this IR node...", "pass 1..."
4. **DOCUMENTATION** → Anything describing the purpose of the next declaration

### Step 2: Refactor (Move or Delete)

| Category | Action | Result |
|----------|--------|--------|
| **PLANNING** | Convert to GitHub Issue | `// TODO(fakt): Fix memory leak, see #123` |
| **DESIGN** | Move to ADR | `// See ADR-001 for rationale on using IR` |
| **IMPLEMENTATION** (trivial) | Delete if obvious | Remove `// increment i` |
| **IMPLEMENTATION** (complex) | Formalize as Maintainer Note | Use Pattern 3 template |
| **DOCUMENTATION** | Promote to KDoc | Convert `// Checks for nulls` to `/** ... */` |

---

## ✅ Documentation Quality Checklist

### For Public API (User-Facing)

- [ ] Is the member public/protected intentionally?
- [ ] Does it have explicit visibility and return type?
- [ ] Does it have a KDoc summary?
- [ ] Are all `@param`, `@property`, `@return`, `@throws` present and descriptive?
- [ ] Is there a `@sample` for non-trivial APIs?
- [ ] Is `@see` used for related concepts?
- [ ] Is `@since` present for versioned releases?

### For Internal API (Contributor-Facing)

- [ ] Is KDoc written for Contributors (describes effect/purpose, not implementation)?
- [ ] If code is self-evident, is it correctly free of redundant KDoc?
- [ ] If logic is complex, is it documented with `//` (immediate why) or `/* Maintainer Note */` (deep why)?
- [ ] Is `@InternalFaktApi` used for unstable internal APIs?
- [ ] Is `@PublishedApi` used correctly for inline functions?

### For Process & Planning

- [ ] Is every `// TODO` or `// FIXME` linked to a GitHub Issue?
- [ ] Are high-level design decisions documented in ADRs?
- [ ] Are there no commented-out code blocks? (Use version control for history)

---

## 🛠️ Required Tooling Configuration

### 1. Dokka (Documentation Generator)

**Root `build.gradle.kts`:**
```kotlin
plugins {
    id("org.jetbrains.dokka") version "2.0.0" apply false
}

tasks.register("dokkaHtmlMultiModule", org.jetbrains.dokka.gradle.DokkaMultiModuleTask::class.java) {
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}
```

**Subproject `build.gradle.kts`:**
```kotlin
plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleVersion.set(version.toString())
}
```

### 2. Explicit API Mode (API Surface Guard)

**In `kotlin { }` block:**
```kotlin
kotlin {
    explicitApi()  // Forces explicit visibility + return types
}
```

### 3. Detekt (KDoc Linter)

**`detekt.yml`:**
```yaml
comments:
  active: true
  UndocumentedPublicClass:
    active: true
    searchInNestedClass: true
    searchInInnerClass: true
  UndocumentedPublicFunction:
    active: true
  UndocumentedPublicProperty:
    active: true
```

**Result:** Build fails if public API is missing KDoc.

---

## 🎯 Usage Examples

### Document Specific File

```bash
/document compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.kt
```

**What happens:**
1. Audit all comments in file
2. Categorize (PLANNING/DESIGN/IMPLEMENTATION/DOCUMENTATION)
3. Apply appropriate pattern (1-5)
4. Validate with checklist
5. Report changes made

### Document Entire Module

```bash
/document compiler
```

**What happens:**
1. Scan all Kotlin files in compiler module
2. Generate audit report
3. Apply patterns systematically
4. Create ADRs for design decisions
5. Create GitHub issues for TODOs
6. Validate with tooling

### Audit Only (No Changes)

```bash
/document --audit-only
```

**Output:**
```
📊 DOCUMENTATION AUDIT REPORT

📁 Files Scanned: 47
💬 Comments Found: 312

📋 CATEGORIZATION:
- PLANNING: 23 (7%)     → Need GitHub Issues
- DESIGN: 8 (3%)        → Need ADRs
- IMPLEMENTATION: 89 (29%)  → Need refinement/deletion
- DOCUMENTATION: 192 (61%)  → Need KDoc promotion

🚨 CRITICAL ISSUES:
- 15 public APIs missing KDoc
- 8 long design comments (>10 lines) need ADRs
- 23 TODO/FIXME without issue links

✅ WELL-DOCUMENTED:
- @Fake annotation (Pattern 1) ✅
- FakeConfig class (Pattern 1) ✅
- IrType.isNullable() (Pattern 2) ✅

🎯 PRIORITY FIXES:
1. Document 15 public APIs (explicitApi mode will enforce)
2. Create ADRs for 8 design decisions
3. Link 23 TODOs to GitHub Issues
```

---

## 🎓 Key Principles Summary

1. **One Comment, One Audience** - Never mix User/Contributor/Maintainer concerns
2. **Code is Not a Notebook** - Planning notes belong in Issues, design rationale in ADRs
3. **KDoc is Public Contract** - Describes WHAT and WHY, not HOW (implementation details)
4. **Maintainer Notes are Sacred** - Complex IR/FIR logic MUST preserve deep "why" knowledge
5. **Automation Enforces Standards** - explicitApi() + Detekt = guaranteed documentation coverage
6. **@sample is King** - Verifiable, compilable examples are better than lengthy descriptions
7. **Self-Evident Code Needs No KDoc** - Don't document the obvious

---

## 🚀 Quick Reference: When to Use What

```
User asks: "What does @Fake do?"
└─> Pattern 1: Public API KDoc with @sample

Contributor asks: "What does isNullable() check?"
└─> Pattern 2: Internal KDoc with constraints

Maintainer asks: "Why is this IR transform so complex?"
└─> Pattern 3: Maintainer Note inside function

Architect asks: "Why did we choose IR over FIR?"
└─> Pattern 4: ADR in .claude/docs/adr/

Plugin author asks: "Can I use dumpTree()?"
└─> Pattern 5: @InternalFaktApi with @RequiresOptIn
```

---

## 📚 Before/After Examples from Fakt Codebase

### Before (Polluted):
```kotlin
// This annotation marks a class for fake generation.
// We use IR because it works across all platforms.
// TODO: Add support for generic types
// NOTE: This might break in K2, see Metro for reference
@Target(AnnotationTarget.CLASS)
annotation class Fake
```

### After (Professional):
```kotlin
/**
 * Marks an interface or abstract class for test fake generation.
 *
 * When applied, the **Fakt** compiler plugin will generate a concrete
 * implementation of this class, named `Fake${OriginalName}Impl`.
 *
 * @sample com.rsicarelli.fakt.samples.BasicFakeUsage
 * @see FakeConfig for advanced configuration options
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS)
annotation class Fake

// TODO(fakt): Add support for generic types, see #45
// See ADR-001 for rationale on IR backend choice
```

---

## Related Commands

- `/validate-metro-alignment` - Ensure documentation follows Metro patterns
- `/consult-kotlin-api` - Validate KDoc for Kotlin compiler APIs
- `/run-bdd-tests` - Test that generated code is properly documented

## Technical References

- Kotlin KDoc Specification: https://kotlinlang.org/docs/kotlin-doc.html
- Dokka Documentation: https://kotlinlang.org/docs/dokka-introduction.html
- Metro Documentation Patterns: `/metro/compiler/src/main/kotlin/`
- JetBrains Coding Conventions: https://kotlinlang.org/docs/coding-conventions.html
- Fakt Documentation Standards: This command
