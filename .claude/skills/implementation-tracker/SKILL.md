---
name: implementation-tracker
description: Monitors Fakt implementation progress tracking phase completion status, test pass rates, compilation success metrics, milestone validation, and identifying blockers. Use when checking status, monitoring progress, validating phases, reviewing milestones, or when user mentions "check status", "implementation progress", "phase completion", "what's done", "current state", "milestone", or phase names (Phase 1, Phase 2).
allowed-tools: Read, Grep, Glob, Bash
---

# Implementation Progress Tracker

Monitors Fakt compiler plugin implementation progress across all phases with real-time status validation.

## Core Mission

Provides comprehensive status tracking of Fakt implementation including phase completion, test metrics, compilation success rates, and milestone validation to guide development priorities.

## Instructions

### 1. Determine Status Scope

**Extract from conversation:**
- Specific phase: "check Phase 1 status"
- General overview: "what's the current status?"
- Detailed analysis: "detailed implementation report"
- Validation focus: "validate phase completion"

**Scope options:**
- General overview (default)
- Phase-specific (phase1, phase2, phase3, generics)
- Detailed analysis (all metrics)
- Validation mode (check completion criteria)

### 2. Read Current Status Documentation

**Primary architecture and implementation docs:**
```bash
Read .claude/docs/implementation/architecture/ARCHITECTURE.md
Read .claude/docs/implementation/codegen-v2/README.md
Read .claude/docs/implementation/generics/technical-reference.md
```

**Analyze current status from:**
- Test files (count passing tests)
- Generated code (count successful fakes)
- Compiler output (compilation success rate)
- Architecture documentation

### 3. Analyze Core Implementation Status

**Core Features (Foundation)**

**Read implementation docs:**
```bash
Read .claude/docs/implementation/patterns/basic-fake-generation.md
Read .claude/docs/implementation/patterns/suspend-function-handling.md
Read .claude/docs/implementation/codegen-v2/README.md
```

**Check core feature completeness:**
- [ ] Core IR generation working
- [ ] Smart default system implemented (Codegen V2)
- [ ] Function type resolution working
- [ ] Suspend function support
- [ ] GIVEN-WHEN-THEN tests in place
- [ ] Compilation success rate tracked

**Metrics to gather:**
```bash
# Count Phase 1 tests
grep -r "GIVEN.*WHEN.*THEN" compiler/src/test/kotlin/ | wc -l

# Check for TODO markers (should be 0)
grep -r "TODO" compiler/src/main/kotlin/ | wc -l

# Find generated files
find samples/*/build/generated/fakt -name "*.kt" 2>/dev/null | wc -l
```

**Core feature completion criteria:**
```
✅ Basic fake generation working
✅ Smart defaults for all types (Codegen V2 DSL)
✅ Function types with proper syntax
✅ Suspend function support
✅ Multi-module support (FakeCollectorTask)
✅ Comprehensive GIVEN-WHEN-THEN tests
```

### 4. Analyze Generic Type Support Status

**Generic Type Support**

**Read generic implementation docs:**
```bash
Read .claude/docs/implementation/generics/technical-reference.md
Read .claude/docs/implementation/generics/complex-generics-strategy.md
```

**Check generic support progress:**

**Method-Level Generics**
- [ ] Type parameter preservation
- [ ] IrTypeSubstitutor integration
- [ ] Safe type casting patterns
- [ ] Function types with generics working
- [ ] Tests for method-level generics

**Phase 2B: Class-Level Generics (Target)**
- [ ] IrTypeSubstitutor integration started
- [ ] Generic factory functions designed
- [ ] Type-safe configuration DSL planned

**Metrics:**
```bash
# Check for generic handling code
grep -r "IrTypeSubstitutor" compiler/src/main/kotlin/

# Count generic-related tests
grep -r "GIVEN.*generic" compiler/src/test/kotlin/ | wc -l
```

### 5. Analyze Test Coverage

**Overall test metrics:**
```bash
# Run tests and capture results
./gradlew test --console=plain 2>&1 | tee test-results.log

# Parse results
total_tests=$(grep -oP '\d+(?= tests? completed)' test-results.log)
passing_tests=$(grep -oP '\d+(?= passed)' test-results.log)
failing_tests=$(grep -oP '\d+(?= failed)' test-results.log)

# Calculate pass rate
pass_rate=$((passing_tests * 100 / total_tests))
```

**Test categories:**
```bash
# GIVEN-WHEN-THEN compliance
grep -r "fun \`GIVEN" compiler/src/test/kotlin/ | wc -l

# Check for forbidden "should" pattern
should_count=$(grep -r "fun \`should" compiler/src/test/kotlin/ | wc -l)
# Should be 0!
```

**Output:**
```
📊 TEST METRICS:
- Total tests: {count}
- Passing: {count} ({percentage}%)
- Failing: {count}
- GIVEN-WHEN-THEN compliant: {count}
- "should" violations: {count} {✅ or ❌}
```

### 6. Analyze Compilation Success Rate

**Compilation metrics:**
```bash
# Compile samples and track success across all platforms
./gradlew :samples:jvm-single-module:build 2>&1 | tee compile-jvm.log
./gradlew :samples:android-single-module:build 2>&1 | tee compile-android.log

cd samples/kmp-single-module
../../gradlew compileKotlinJvm 2>&1 | tee ../../compile-kmp.log

# Check exit code
if [ $? -eq 0 ]; then
    echo "✅ Compilation successful"
else
    echo "❌ Compilation failed"
fi

# Count compilation errors
error_count=$(grep -c "error:" ../../compile.log)
warning_count=$(grep -c "warning:" ../../compile.log)
```

**Calculate success rate:**
```bash
# Count interfaces processed
interface_count=$(find src/commonMain/kotlin -name "*.kt" -exec grep -l "@Fake" {} \; | wc -l)

# Count successfully generated fakes
generated_count=$(find build/generated/fakt -name "Fake*Impl.kt" 2>/dev/null | wc -l)

# Success rate
success_rate=$((generated_count * 100 / interface_count))
```

**Output:**
```
🔨 COMPILATION METRICS:
- Interfaces with @Fake: {count}
- Successfully generated: {count}
- Compilation errors: {count}
- Warnings: {count}
- Success rate: {percentage}%
```

### 7. Check Known Issues & Blockers

**Identify blockers:**

**From troubleshooting docs:**
```bash
# Check common issues
Read .claude/docs/troubleshooting/common-issues.md
```

**From implementation docs:**
```bash
# Check for unresolved issues and limitations
grep -r "❌\|⚠️\|FIXME\|TODO\|Limitation" .claude/docs/implementation/
```

**Categorize blockers:**
- 🔴 **Critical**: Prevents progress
- 🟡 **Medium**: Workarounds exist
- 🟢 **Low**: Nice-to-have, future improvement

### 8. Validate Milestone Completion

**Phase 1 Milestones:**
```
✅ Milestone 1.1: Basic IR generation
✅ Milestone 1.2: Smart defaults system
✅ Milestone 1.3: Function type resolution
✅ Milestone 1.4: Zero TODO blockers
✅ Milestone 1.5: 85% compilation success
```

**Phase 2 Milestones:**
```
✅ Milestone 2A.1: Method-level generics (identity pattern)
🚧 Milestone 2A.2: Class-level generics (IrTypeSubstitutor)
⏳ Milestone 2B.1: Generic factory functions
⏳ Milestone 2B.2: Type-safe configuration DSL
```

### 9. Generate Comprehensive Status Report

**Full status report:**

```
═══════════════════════════════════════════════════
📊 FAKT IMPLEMENTATION STATUS REPORT
═══════════════════════════════════════════════════

📅 Status as of: {date}
🏗️ Current Phase: Phase {X}
🎯 Overall Progress: {percentage}%

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ PHASE 1: CORE INFRASTRUCTURE (MAP Foundation)
Status: {Complete ✅ | In Progress 🚧 | Not Started ⏳}
Progress: {percentage}%

Key Achievements:
✅ Smart default system (Zero TODO blockers)
✅ Function type resolution (Perfect syntax)
✅ GIVEN-WHEN-THEN test standard (53+ tests)
✅ 85% compilation success rate
✅ Two-phase FIR→IR architecture

Remaining:
{List if any}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🚧 PHASE 2: GENERIC TYPE SUPPORT
Status: {In Progress 🚧}
Progress: {percentage}%

Phase 2A (Method-Level Generics):
✅ Identity function pattern
✅ Safe casting implementation
✅ Function types with generics
Status: Complete ✅

Phase 2B (Class-Level Generics):
🚧 IrTypeSubstitutor integration
⏳ Generic factory functions
⏳ Type-safe configuration DSL
Status: In Progress 🚧

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 METRICS DASHBOARD:

Tests:
- Total: {count} tests
- Passing: {count} ({percentage}%)
- GIVEN-WHEN-THEN compliant: {count}
- "should" violations: {count} {✅/❌}

Compilation:
- Interfaces processed: {count}
- Successfully generated: {count}
- Success rate: {percentage}%
- Errors: {count}

Code Quality:
- TODO markers: {count} {✅/❌}
- Type safety: {status}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🚨 KNOWN ISSUES & BLOCKERS:

{List blockers by priority}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 MILESTONES:

Completed:
✅ {List completed milestones}

In Progress:
🚧 {List in-progress milestones}

Upcoming:
⏳ {List upcoming milestones}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 NEXT PRIORITIES:

1. {Priority 1}
2. {Priority 2}
3. {Priority 3}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 REFERENCES:
- Architecture: .claude/docs/implementation/architecture/ARCHITECTURE.md
- Codegen V2: .claude/docs/implementation/codegen-v2/README.md
- Generics: .claude/docs/implementation/generics/technical-reference.md
- Multi-Module: .claude/docs/implementation/multi-module/collector-task-implementation.md
- Troubleshooting: .claude/docs/troubleshooting/common-issues.md

═══════════════════════════════════════════════════
```

### 10. Suggest Next Actions

**Based on status:**

**If core features incomplete:**
```
🎯 FOCUS: Complete Core Foundation
- Address remaining blockers
- Reach 90%+ compilation success
- Ensure all core tests pass
```

**If Phase 1 complete, Phase 2 in progress:**
```
🎯 FOCUS: Phase 2 Generic Type Support
- Continue IrTypeSubstitutor integration
- Implement generic factory functions
- Design type-safe configuration DSL
- Target: 95% compilation success
```

**If blockers identified:**
```
🚨 ACTION REQUIRED: Address Blockers
1. {Blocker 1} - Priority: {level}
2. {Blocker 2} - Priority: {level}

Recommended Skills:
- For compilation issues: compilation-validator
- For error analysis: compilation-error-analyzer
```

## Supporting Files

Progressive disclosure for status tracking:

- **`resources/status-dashboard-template.md`** - Report template (loaded on-demand)
- **`resources/phase-completion-criteria.md`** - Checklist per phase (loaded on-demand)
- **`resources/metrics-tracking.md`** - Metrics definitions (loaded on-demand)

## Related Skills

This Skill composes with:
- **`compilation-validator`** - Validate compilation metrics
- **`bdd-test-runner`** - Get test metrics
- **`compiler-architecture-validator`** - Validate architectural patterns

## Status Categories

| Status | Symbol | Meaning |
|--------|--------|---------|
| Complete | ✅ | Fully implemented and validated |
| In Progress | 🚧 | Actively being worked on |
| Not Started | ⏳ | Planned but not yet begun |
| Blocked | 🔴 | Cannot proceed (blocker exists) |
| On Hold | ⏸️ | Paused temporarily |

## Best Practices

1. **Check status regularly** - Weekly for active projects
2. **Update docs after milestones** - Keep current-status.md fresh
3. **Track metrics objectively** - Real numbers, not estimates
4. **Identify blockers early** - Don't let them compound
5. **Celebrate achievements** - Recognize completed phases

## Quick Status Checks

**One-liner status:**
```
Overall: {percentage}% | Phase 1: ✅ | Phase 2: 🚧 {percentage}% | Tests: {passing}/{total}
```

**Traffic light status:**
- 🟢 Green: On track, no blockers
- 🟡 Yellow: Minor issues, workarounds available
- 🔴 Red: Blocked, needs attention

## Current Known Status (Phase 1)

As of Phase 1 completion:
- ✅ 85% compilation success
- ✅ 53+ GIVEN-WHEN-THEN tests
- ✅ Zero TODO blockers
- ✅ Smart defaults working
- ✅ Function type generation perfect

**Target for Phase 2:**
- 🎯 95% compilation success
- 🎯 Generic type support complete
- 🎯 80+ GIVEN-WHEN-THEN tests
- 🎯 Type-safe configuration DSL
