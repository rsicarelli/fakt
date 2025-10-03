---
allowed-tools: Read, Grep, Bash(./gradlew:*), Bash(find:*), Bash(jar:*), TodoWrite, Task
argument-hint: [--type=<error_type>] [--interface=<name>] [--verbose] (optional - error analysis scope)
description: Systematic compilation error analysis and resolution for KtFakes development
model: claude-sonnet-4-20250514
---

# 🔬 Compilation Error Diagnostic & Resolution Engine

**Systematic error analysis with targeted solution recommendations**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/troubleshooting/common-issues.md` - Known error patterns and solutions
- `.claude/docs/implementation/current-status.md` - Current implementation status context
- `.claude/docs/validation/compilation-validation.md` - Compilation validation strategies
- `.claude/docs/analysis/generic-scoping-analysis.md` - Generic type error understanding
- Real compilation logs and error patterns for analysis
- Gradle build system for error reproduction and validation

**🏆 ERROR ANALYSIS BASELINE:**
- Zero TODO compilation blockers (Phase 1 achievement)
- Systematic error classification by component
- Targeted solution recommendations with verification steps
- Phase-aware error context (current Phase 1 complete, Phase 2A pending)

## Command Overview

**Usage**: `/analyze-compilation-error [--type=<error_type>] [--interface=<name>] [--verbose]`
**Example**: `/analyze-compilation-error --type=generation --interface=UserService --verbose`

Analyzes compilation errors in KtFakes development to:
- Identify root causes of compilation failures
- Provide specific solutions for common error patterns
- Guide through systematic debugging process
- Suggest preventive measures

## Command Implementation

### Error Classification
```bash
classify_error() {
    local error_log="$1"

    echo "🔍 Classifying compilation error..."

    # Plugin registration errors
    if grep -q "KtFakeCompilerPluginRegistrar" "$error_log"; then
        echo "Type: PLUGIN_REGISTRATION"
        analyze_plugin_registration_error "$error_log"

    # IR generation errors
    elif grep -q "IrGenerationExtension\|UnifiedKtFakesIrGenerationExtension" "$error_log"; then
        echo "Type: IR_GENERATION"
        analyze_ir_generation_error "$error_log"

    # FIR phase errors
    elif grep -q "FirExtension\|@Fake" "$error_log"; then
        echo "Type: FIR_DETECTION"
        analyze_fir_detection_error "$error_log"

    # Generated code compilation errors
    elif grep -q "build/generated/ktfake" "$error_log"; then
        echo "Type: GENERATED_CODE"
        analyze_generated_code_error "$error_log"

    # Dependency errors
    elif grep -q "ClassNotFoundException\|NoClassDefFoundError" "$error_log"; then
        echo "Type: DEPENDENCY"
        analyze_dependency_error "$error_log"

    # Type resolution errors
    elif grep -q "Type mismatch\|Unresolved reference" "$error_log"; then
        echo "Type: TYPE_RESOLUTION"
        analyze_type_resolution_error "$error_log"

    else
        echo "Type: UNKNOWN"
        analyze_unknown_error "$error_log"
    fi
}
```

### Plugin Registration Error Analysis
```bash
analyze_plugin_registration_error() {
    local error_log="$1"

    echo "🔧 Analyzing plugin registration error..."

    # Check for common registration issues
    if grep -q "Cannot find KtFakeCompilerPluginRegistrar" "$error_log"; then
        cat << 'EOF'
❌ Plugin Registration Error: KtFakeCompilerPluginRegistrar not found

📋 Root Cause:
- Compiler plugin JAR not built or not in classpath
- Incorrect META-INF/services configuration
- Build cache issues

📋 Solution:
1. Rebuild compiler plugin:
   ./gradlew :compiler:shadowJar

2. Verify JAR contents:
   jar tf compiler/build/libs/compiler.jar | grep KtFakeCompilerPluginRegistrar

3. Check META-INF/services:
   jar tf compiler/build/libs/compiler.jar | grep META-INF/services

4. Clean and rebuild:
   ./gradlew clean :compiler:shadowJar

📋 Verification:
- Check compiler/build/libs/compiler.jar exists
- Verify plugin configuration in build.gradle.kts
- Test with: cd test-sample && ../gradlew clean compileKotlinJvm
EOF

    elif grep -q "Multiple compiler plugins" "$error_log"; then
        cat << 'EOF'
❌ Plugin Registration Error: Multiple compiler plugins detected

📋 Root Cause:
- Duplicate plugin registration
- Conflicting plugin dependencies
- Multiple versions in classpath

📋 Solution:
1. Check for duplicate dependencies:
   ./gradlew :test-sample:dependencies | grep ktfake

2. Clean Gradle cache:
   rm -rf ~/.gradle/caches/
   ./gradlew clean

3. Verify single plugin registration:
   grep -r "KtFakeCompilerPluginRegistrar" .

📋 Verification:
- Only one ktfake dependency should be present
- Only one META-INF/services entry
EOF

    else
        echo "📋 Unknown plugin registration error - checking general solutions..."
        suggest_general_plugin_solutions
    fi
}
```

### IR Generation Error Analysis
```bash
analyze_ir_generation_error() {
    local error_log="$1"

    echo "🔧 Analyzing IR generation error..."

    # Check for specific IR issues
    if grep -q "IrGenerationExtension.*generate" "$error_log"; then
        cat << 'EOF'
❌ IR Generation Error: Extension generation failed

📋 Common Causes:
1. Interface discovery failure
2. Type resolution issues
3. Generic type handling problems
4. Method signature generation errors

📋 Debugging Steps:
1. Enable debug logging:
   ../gradlew compileKotlinJvm -i | grep -E "(KtFakes|Generated|ERROR)"

2. Check interface detection:
   grep -r "@Fake" src/ # Verify @Fake annotation present

3. Analyze interface structure:
   /analyze-interface-structure <InterfaceName>

4. Test with simple interface:
   @Fake
   interface SimpleTest {
       fun getValue(): String
   }

📋 Phase-Specific Issues:
- Method-level generics: Known limitation, requires Phase 2A
- Complex return types: Check type resolution
- Suspend functions: Should work, verify syntax
EOF

    elif grep -q "Type mismatch.*irType" "$error_log"; then
        cat << 'EOF'
❌ IR Generation Error: Type resolution failure

📋 Root Cause:
- irTypeToKotlinString() method issues
- Unsupported type patterns
- Generic type scoping problems

📋 Solution:
1. Check type mapping:
   # Review UnifiedKtFakesIrGenerationExtension.irTypeToKotlinString()

2. Test with basic types:
   interface BasicTest {
       fun getString(): String
       fun getInt(): Int
       fun getBoolean(): Boolean
   }

3. For complex types, use Phase 2A approach:
   # Method-level generics need identity function + casting

📋 Workaround:
- Replace complex generics with concrete types
- Use interface-level generics instead of method-level
- Simplify return types for testing
EOF

    else
        echo "📋 Unknown IR generation error - checking method-specific issues..."
        check_method_generation_issues "$error_log"
    fi
}
```

### Generated Code Error Analysis
```bash
analyze_generated_code_error() {
    local error_log="$1"

    echo "🔧 Analyzing generated code compilation error..."

    # Extract generated file path
    local generated_file=$(grep "build/generated/ktfake" "$error_log" | head -n 1 | grep -o "build/generated/ktfake[^:]*")

    if [ -n "$generated_file" ]; then
        echo "📋 Generated file: $generated_file"

        # Check if file exists
        if [ -f "$generated_file" ]; then
            echo "✅ Generated file exists, analyzing content..."
            analyze_generated_file_content "$generated_file" "$error_log"
        else
            echo "❌ Generated file missing - generation failed"
            suggest_generation_failure_solutions
        fi
    else
        echo "📋 Cannot locate generated file path in error log"
        suggest_general_generated_code_solutions
    fi
}

analyze_generated_file_content() {
    local file="$1"
    local error_log="$2"

    echo "🔍 Analyzing generated file content..."

    # Check for syntax errors
    if grep -q "Expecting" "$error_log"; then
        cat << 'EOF'
❌ Generated Code Error: Syntax error in generated code

📋 Common Issues:
1. Missing imports
2. Incorrect type syntax
3. Invalid method signatures
4. Malformed lambda expressions

📋 Solution:
1. Check generated file syntax:
   kotlinc -cp ... <generated_file> # Compile separately

2. Review generation logic:
   # Check string concatenation in generators
   # Verify type string generation
   # Validate method signature construction

3. Test with minimal interface:
   @Fake
   interface MinimalTest {
       fun test(): String
   }
EOF

    elif grep -q "Type mismatch" "$error_log"; then
        echo "📋 Type mismatch in generated code detected"
        analyze_type_mismatch_in_generated_code "$file" "$error_log"

    elif grep -q "Unresolved reference" "$error_log"; then
        echo "📋 Unresolved reference in generated code detected"
        analyze_unresolved_reference_in_generated_code "$file" "$error_log"
    fi
}
```

### Type Resolution Error Analysis
```bash
analyze_type_resolution_error() {
    local error_log="$1"

    echo "🔧 Analyzing type resolution error..."

    if grep -q "Generic.*not accessible" "$error_log"; then
        cat << 'EOF'
❌ Type Resolution Error: Generic type scoping issue

📋 Root Cause:
This is the core architectural challenge identified in our generic scoping analysis.
Method-level generic type parameters are not accessible at the class level.

📋 Current Status:
- Interface-level generics: ✅ Supported
- Method-level generics: ⚠️ Phase 2A required

📋 Example Issue:
interface Repository<T> {
    fun <R> process(data: T): R  // <R> not accessible at class level
}

📋 Current Workaround:
1. Use interface-level generics:
   interface Repository<T, R> {
       fun process(data: T): R
   }

2. Use concrete types:
   interface Repository {
       fun process(data: String): String
   }

📋 Phase 2A Solution (In Development):
- Identity function approach: private var behavior: (Any?) -> Any? = { it }
- Dynamic casting: return behavior(data) as R
- Suppressed unchecked cast warnings

📋 References:
- Generic Scoping Analysis: .claude/docs/analysis/generic-scoping-analysis.md
- Current Status: .claude/docs/implementation/current-status.md
EOF

    elif grep -q "Result.*not found" "$error_log"; then
        cat << 'EOF'
❌ Type Resolution Error: Result type handling

📋 Root Cause:
- kotlin.Result import issues
- Default value generation for Result types

📋 Solution:
1. Check imports in generated code:
   # Should include: import kotlin.Result

2. Verify default value generation:
   # Result<T> should default to: Result.success("")

3. Test with simpler return type:
   interface TestService {
       fun getValue(): String  // Instead of Result<String>
   }
EOF

    else
        echo "📋 Other type resolution error detected"
        suggest_general_type_resolution_solutions
    fi
}
```

### Solution Recommendations
```bash
suggest_solution() {
    local error_type="$1"
    local interface_name="$2"

    echo "💡 Recommended solution for $error_type:"

    case "$error_type" in
        "PLUGIN_REGISTRATION")
            echo "1. Rebuild compiler plugin: ./gradlew :compiler:shadowJar"
            echo "2. Verify JAR contents and META-INF/services"
            echo "3. Clean and retry: ./gradlew clean"
            ;;

        "IR_GENERATION")
            echo "1. Analyze interface structure: /analyze-interface-structure $interface_name"
            echo "2. Test with simplified interface"
            echo "3. Check for method-level generics (Phase 2A required)"
            ;;

        "GENERATED_CODE")
            echo "1. Check generated file syntax"
            echo "2. Verify type string generation"
            echo "3. Test compilation of generated code separately"
            ;;

        "TYPE_RESOLUTION")
            echo "1. Review generic scoping analysis"
            echo "2. Use interface-level generics"
            echo "3. Consider concrete types for testing"
            ;;

        "DEPENDENCY")
            echo "1. Check dependency configuration"
            echo "2. Refresh Gradle dependencies"
            echo "3. Verify ktfake version compatibility"
            ;;

        *)
            echo "1. Check common issues guide"
            echo "2. Enable verbose logging"
            echo "3. Create minimal reproduction case"
            ;;
    esac
}
```

### Systematic Debugging Workflow
```bash
debug_systematic() {
    local interface_name="$1"

    echo "🔍 Systematic debugging workflow for $interface_name:"

    # Step 1: Environment validation
    echo "📋 Step 1: Environment validation"
    /setup-development-environment --validate

    # Step 2: Interface analysis
    echo "📋 Step 2: Interface analysis"
    /analyze-interface-structure "$interface_name"

    # Step 3: Plugin build verification
    echo "📋 Step 3: Plugin build verification"
    ./gradlew :compiler:shadowJar
    jar tf compiler/build/libs/compiler.jar | grep KtFakeCompilerPluginRegistrar

    # Step 4: Generation attempt with logging
    echo "📋 Step 4: Generation attempt with detailed logging"
    cd test-sample
    ../gradlew clean compileKotlinJvm -i 2>&1 | tee ../compilation.log
    cd ..

    # Step 5: Error analysis
    echo "📋 Step 5: Error analysis"
    if [ -f "compilation.log" ]; then
        classify_error "compilation.log"
    fi

    # Step 6: Generated code verification
    echo "📋 Step 6: Generated code verification"
    if [ -d "test-sample/build/generated/ktfake/test/kotlin" ]; then
        echo "✅ Generated code found:"
        ls -la test-sample/build/generated/ktfake/test/kotlin/
    else
        echo "❌ No generated code found"
    fi
}
```

### Command Usage Examples

#### Basic Error Analysis
```bash
# Analyze current compilation error
/analyze-compilation-error

# Analyze specific interface error
/analyze-compilation-error --interface=UserService
```

#### Type-Specific Analysis
```bash
# Focus on IR generation errors
/analyze-compilation-error --type=ir_generation

# Focus on generated code errors
/analyze-compilation-error --type=generated_code

# Focus on plugin registration
/analyze-compilation-error --type=plugin_registration
```

#### Verbose Debugging
```bash
# Full debugging with all details
/analyze-compilation-error --interface=UserService --verbose

# Systematic debugging workflow
/analyze-compilation-error --interface=UserService --workflow
```

### Integration with Other Commands

#### Pre-Analysis
```bash
# Setup → Analyze → Debug workflow
/setup-development-environment --validate
/analyze-interface-structure UserService
/analyze-compilation-error --interface=UserService
```

#### Post-Analysis Testing
```bash
# After fixing issues
/analyze-compilation-error --interface=UserService
/debug-ir-generation UserService
/run-bdd-tests GIVEN_UserService_WHEN_generating
```

### Related Documentation

- **[📋 Common Issues Guide](.claude/docs/troubleshooting/common-issues.md)** - General problem solving
- **[📋 Generic Scoping Analysis](.claude/docs/analysis/generic-scoping-analysis.md)** - Core challenge details
- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standards

---

**This command provides systematic compilation error analysis and resolution guidance for effective KtFakes development troubleshooting.**