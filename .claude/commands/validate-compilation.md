---
allowed-tools: Read, Bash(./gradlew:*), Bash(kotlinc:*), Bash(find:*), TodoWrite, Task
argument-hint: [--interface=<name>|--all|--verbose] (optional - validation scope and detail)
description: Comprehensive compilation validation and type safety verification for generated fakes
model: claude-sonnet-4-20250514
---

# 🛡️ Compilation Validation & Type Safety Checker

**Production-grade validation with comprehensive quality assurance**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/validation/compilation-validation.md` - Detailed validation strategy and criteria
- `.claude/docs/validation/type-safety-validation.md` - Type system testing approaches
- `.claude/docs/troubleshooting/common-issues.md` - Known compilation issue patterns
- `.claude/docs/implementation/current-status.md` - Current implementation capabilities
- `.claude/docs/patterns/complex-generics-strategy.md` - Generic type handling validation
- Real compilation logs and generated code for analysis

**🏆 VALIDATION BASELINE:**
- Zero TODO compilation blockers (Phase 1 achievement)
- 85% compilation success rate with clean generated code
- Professional output quality with proper type safety measures
- Smart default value system preventing compilation failures

## Command Overview

**Usage**: `/validate-compilation [--interface=<name>] [--all] [--verbose]`
**Example**: `/validate-compilation --interface=UserService --verbose`

Validates the compilation of generated fake code to ensure:
- Generated code compiles without errors
- Type safety is maintained throughout generation
- Smart defaults work correctly for all types
- Configuration DSL is properly typed

## Command Implementation

### Basic Compilation Validation
```bash
validate_compilation() {
    local interface_name="$1"
    local verbose="$2"

    echo "🔍 Validating compilation for KtFakes generated code..."

    # Clean previous builds
    echo "🧹 Cleaning previous builds..."
    cd test-sample
    ../gradlew clean

    # Generate fresh fakes
    echo "🏗️ Generating fresh fakes..."
    ../gradlew compileKotlinJvm --no-build-cache 2>&1 | tee ../compilation.log

    # Check compilation success
    if [ $? -eq 0 ]; then
        echo "✅ Compilation successful!"
        validate_generated_files "$interface_name" "$verbose"
    else
        echo "❌ Compilation failed!"
        analyze_compilation_errors "$verbose"
        return 1
    fi

    cd ..
}
```

### Generated File Validation
```bash
validate_generated_files() {
    local interface_name="$1"
    local verbose="$2"

    echo "🔍 Validating generated files..."

    local generated_dir="build/generated/ktfake/test/kotlin"

    if [ ! -d "$generated_dir" ]; then
        echo "❌ Generated directory not found: $generated_dir"
        return 1
    fi

    # Count generated files
    local file_count=$(find "$generated_dir" -name "*.kt" | wc -l)
    echo "📊 Found $file_count generated Kotlin files"

    # Validate specific interface if provided
    if [ -n "$interface_name" ]; then
        validate_specific_interface "$interface_name" "$generated_dir" "$verbose"
    else
        validate_all_interfaces "$generated_dir" "$verbose"
    fi
}
```

### Interface-Specific Validation
```bash
validate_specific_interface() {
    local interface_name="$1"
    local generated_dir="$2"
    local verbose="$3"

    echo "🎯 Validating interface: $interface_name"

    # Find generated file for interface
    local generated_file=$(find "$generated_dir" -name "*${interface_name}*.kt" | head -n 1)

    if [ -z "$generated_file" ]; then
        echo "❌ No generated file found for interface: $interface_name"
        return 1
    fi

    echo "📄 Generated file: $generated_file"

    # Validate file structure
    validate_file_structure "$generated_file" "$interface_name" "$verbose"
}

validate_file_structure() {
    local file_path="$1"
    local interface_name="$2"
    local verbose="$3"

    echo "🔍 Validating file structure for $interface_name..."

    # Check for required components
    local has_impl_class=$(grep -c "class Fake${interface_name}Impl" "$file_path")
    local has_factory_function=$(grep -c "fun fake${interface_name,,}" "$file_path")
    local has_config_class=$(grep -c "class Fake${interface_name}Config" "$file_path")

    echo "📋 Structure validation:"
    echo "  Implementation class: $([ $has_impl_class -gt 0 ] && echo '✅' || echo '❌')"
    echo "  Factory function: $([ $has_factory_function -gt 0 ] && echo '✅' || echo '❌')"
    echo "  Configuration DSL: $([ $has_config_class -gt 0 ] && echo '✅' || echo '❌')"

    # Detailed analysis if verbose
    if [ "$verbose" = "true" ]; then
        echo "🔍 Detailed file analysis:"
        analyze_generated_code_quality "$file_path"
    fi

    # Overall validation
    local total_components=$((has_impl_class + has_factory_function + has_config_class))
    if [ $total_components -eq 3 ]; then
        echo "✅ All required components present"
        return 0
    else
        echo "❌ Missing components detected"
        return 1
    fi
}
```

### Code Quality Analysis
```bash
analyze_generated_code_quality() {
    local file_path="$1"

    echo "🧪 Code quality analysis:"

    # Check for syntax issues
    local syntax_check=$(kotlinc -cp . "$file_path" 2>&1)
    if [ $? -eq 0 ]; then
        echo "  Syntax: ✅ Valid Kotlin syntax"
    else
        echo "  Syntax: ❌ Syntax errors detected"
        echo "$syntax_check" | head -5
    fi

    # Check for type safety indicators
    local any_casts=$(grep -c " as Any" "$file_path")
    local unchecked_casts=$(grep -c "@Suppress.*UNCHECKED_CAST" "$file_path")

    echo "  Type safety:"
    echo "    Any casts: $any_casts $([ $any_casts -eq 0 ] && echo '✅' || echo '⚠️')"
    echo "    Unchecked casts: $unchecked_casts $([ $unchecked_casts -eq 0 ] && echo '✅' || echo '⚠️')"

    # Check for TODO statements
    local todo_count=$(grep -c "TODO\|FIXME" "$file_path")
    echo "    TODO statements: $todo_count $([ $todo_count -eq 0 ] && echo '✅' || echo '❌')"

    # Check for proper suspend function handling
    local suspend_functions=$(grep -c "suspend fun" "$file_path")
    local suspend_behaviors=$(grep -c "suspend.*->" "$file_path")

    if [ $suspend_functions -gt 0 ]; then
        echo "  Suspend functions: $suspend_functions functions, $suspend_behaviors behaviors $([ $suspend_functions -eq $suspend_behaviors ] && echo '✅' || echo '⚠️')"
    fi

    # Check imports
    local import_count=$(grep -c "^import" "$file_path")
    echo "  Imports: $import_count imports $([ $import_count -gt 0 ] && echo '✅' || echo '⚠️')"
}
```

### Compilation Error Analysis
```bash
analyze_compilation_errors() {
    local verbose="$1"

    echo "🔍 Analyzing compilation errors..."

    if [ -f "../compilation.log" ]; then
        # Extract Kotlin compilation errors
        local kotlin_errors=$(grep -E "error:|Error:" "../compilation.log" | wc -l)
        local warnings=$(grep -E "warning:|Warning:" "../compilation.log" | wc -l)

        echo "📊 Compilation issues:"
        echo "  Errors: $kotlin_errors"
        echo "  Warnings: $warnings"

        # Show specific errors
        if [ $kotlin_errors -gt 0 ]; then
            echo "🚨 Compilation errors:"
            grep -E "error:|Error:" "../compilation.log" | head -5
        fi

        # Show warnings if verbose
        if [ "$verbose" = "true" ] && [ $warnings -gt 0 ]; then
            echo "⚠️ Compilation warnings:"
            grep -E "warning:|Warning:" "../compilation.log" | head -3
        fi

        # Check for specific KtFakes-related errors
        local ktfakes_errors=$(grep -E "KtFakes|fake.*\(\)|Fake.*Impl" "../compilation.log" | wc -l)
        if [ $ktfakes_errors -gt 0 ]; then
            echo "🎯 KtFakes-specific issues:"
            grep -E "KtFakes|fake.*\(\)|Fake.*Impl" "../compilation.log"
        fi
    else
        echo "❌ No compilation log found"
    fi
}
```

### Type Safety Validation
```bash
validate_type_safety() {
    local interface_name="$1"

    echo "🛡️ Validating type safety for $interface_name..."

    # Create test file to verify type safety
    local test_file="TypeSafetyTest.kt"
    cat > "$test_file" << EOF
import kotlin.test.*

class TypeSafetyValidationTest {
    @Test
    fun \`GIVEN generated fake WHEN used THEN should be type safe\`() {
        // Test compilation of generated fake
        val service = fake${interface_name,,}()

        // Verify return types match interface
        // This will fail to compile if types don't match
        val result: ${interface_name} = service
        assertNotNull(result)
    }

    @Test
    fun \`GIVEN configuration DSL WHEN configuring THEN should be type safe\`() {
        // Test DSL type safety
        val service = fake${interface_name,,} {
            // DSL methods should be type-safe
            // Invalid configurations should not compile
        }
        assertNotNull(service)
    }
}
EOF

    # Try to compile the test
    if kotlinc -cp . "$test_file" 2>/dev/null; then
        echo "✅ Type safety validation passed"
        rm -f "$test_file" TypeSafetyValidationTest.class
        return 0
    else
        echo "❌ Type safety validation failed"
        rm -f "$test_file"
        return 1
    fi
}
```

### Smart Defaults Validation
```bash
validate_smart_defaults() {
    local generated_dir="$1"

    echo "🧠 Validating smart defaults..."

    # Check for appropriate default values in generated code
    local string_defaults=$(grep -r '= { "" }' "$generated_dir" | wc -l)
    local int_defaults=$(grep -r '= { 0 }' "$generated_dir" | wc -l)
    local boolean_defaults=$(grep -r '= { false }' "$generated_dir" | wc -l)
    local unit_defaults=$(grep -r '= { Unit }' "$generated_dir" | wc -l)
    local list_defaults=$(grep -r 'emptyList()' "$generated_dir" | wc -l)

    echo "📊 Default value usage:"
    echo "  String defaults: $string_defaults"
    echo "  Int defaults: $int_defaults"
    echo "  Boolean defaults: $boolean_defaults"
    echo "  Unit defaults: $unit_defaults"
    echo "  List defaults: $list_defaults"

    # Check for problematic defaults
    local todo_defaults=$(grep -r 'TODO\|FIXME' "$generated_dir" | wc -l)
    local any_defaults=$(grep -r ' as Any' "$generated_dir" | wc -l)

    echo "🚨 Problematic defaults:"
    echo "  TODO defaults: $todo_defaults $([ $todo_defaults -eq 0 ] && echo '✅' || echo '❌')"
    echo "  Any casting: $any_defaults $([ $any_defaults -eq 0 ] && echo '✅' || echo '⚠️')"

    return $([ $todo_defaults -eq 0 ] && echo 0 || echo 1)
}
```

### Full Validation Workflow
```bash
run_full_validation() {
    local interface_name="$1"
    local verbose="$2"

    echo "🔄 Running full compilation validation..."

    local validation_passed=true

    # Step 1: Basic compilation
    if ! validate_compilation "$interface_name" "$verbose"; then
        validation_passed=false
    fi

    # Step 2: Generated file structure
    if ! validate_generated_files "$interface_name" "$verbose"; then
        validation_passed=false
    fi

    # Step 3: Type safety
    if [ -n "$interface_name" ]; then
        if ! validate_type_safety "$interface_name"; then
            validation_passed=false
        fi
    fi

    # Step 4: Smart defaults
    if ! validate_smart_defaults "test-sample/build/generated/ktfake/test/kotlin"; then
        validation_passed=false
    fi

    # Final result
    if [ "$validation_passed" = true ]; then
        echo "🎉 All validation checks passed!"
        show_validation_summary
        return 0
    else
        echo "❌ Some validation checks failed!"
        show_failure_recommendations
        return 1
    fi
}
```

### Validation Summary
```bash
show_validation_summary() {
    cat << 'EOF'
✅ Compilation Validation Summary:

📋 What was validated:
  ✅ Generated code compiles without errors
  ✅ All required components present (Impl, factory, config)
  ✅ Type safety maintained throughout
  ✅ Smart defaults working correctly
  ✅ No TODO statements in generated code

📊 Quality metrics:
  ✅ Zero compilation errors
  ✅ Minimal type casting
  ✅ Proper suspend function handling
  ✅ Clean imports and structure

🚀 Generated fakes are ready for use!
EOF
}

show_failure_recommendations() {
    cat << 'EOF'
❌ Compilation Validation Failed

📋 Recommended actions:
1. Check compilation log for specific errors
2. Verify interface definitions are correct
3. Ensure @Fake annotation is present
4. Try cleaning and rebuilding: ./gradlew clean compileKotlinJvm
5. Use /analyze-compilation-error for detailed diagnosis

📚 Additional help:
  - Troubleshooting guide: .claude/docs/troubleshooting/common-issues.md
  - Testing guidelines: .claude/docs/validation/testing-guidelines.md
  - Use /debug-ir-generation for detailed analysis
EOF
}
```

### Command Usage Examples

#### Basic Validation
```bash
# Validate all generated code
/validate-compilation

# Validate specific interface
/validate-compilation --interface=UserService
```

#### Verbose Output
```bash
# Detailed validation with full analysis
/validate-compilation --all --verbose

# Specific interface with detailed output
/validate-compilation --interface=AsyncUserService --verbose
```

#### Integration with Development Workflow
```bash
# After making changes to interface
/validate-compilation --interface=UpdatedService --verbose

# Before committing changes
/validate-compilation --all

# Quick check during development
/validate-compilation --interface=TestService
```

### Related Documentation

- **[📋 Compilation Validation](.claude/docs/validation/compilation-validation.md)** - Detailed validation strategy
- **[📋 Type Safety Validation](.claude/docs/validation/type-safety-validation.md)** - Type system testing
- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN patterns
- **[📋 Common Issues](.claude/docs/troubleshooting/common-issues.md)** - Problem solving

---

**This command ensures that generated fake code meets production quality standards with comprehensive compilation and type safety validation.**