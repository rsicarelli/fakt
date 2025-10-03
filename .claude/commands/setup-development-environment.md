---
allowed-tools: Read, Bash(./gradlew:*), Bash(java:*), Bash(find:*), Bash(mkdir:*), Write, TodoWrite, Task
argument-hint: [--full|--quick|--validate] (optional - setup scope, default: quick)
description: Complete KtFakes development environment setup with IDE integration and validation
model: claude-sonnet-4-20250514
---

# ⚙️ Development Environment Orchestrator

**One-command setup for productive KtFakes development**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/setup/development-setup.md` - Comprehensive setup procedures
- `.claude/docs/troubleshooting/common-issues.md` - Environment issue resolution
- `.claude/docs/implementation/current-status.md` - Current build system status
- `.claude/docs/validation/testing-guidelines.md` - Testing environment standards
- `.claude/docs/architecture/unified-ir-native.md` - Technical foundation requirements
- Real project structure and gradle build configuration

**🏆 SETUP BASELINE:**
- Gradle 8.10.2+ with shadow JAR build system
- JDK 21+ compatibility with Kotlin 2.0+
- Multi-module project structure support
- IDE integration for IntelliJ IDEA and VSCode

## Command Overview

**Usage**: `/setup-development-environment [--full|--quick|--validate]`
**Default**: Quick setup with validation

Sets up the complete KtFakes development environment including:
- Gradle configuration validation
- JDK compatibility verification
- Kotlin compiler plugin dependencies
- Test environment setup
- IDE integration preparation

## Command Implementation

### Environment Detection
```bash
# 1. Check system prerequisites
check_system_requirements() {
    echo "🔍 Checking system requirements..."

    # JDK version check
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo "Java version: $java_version"

    # Gradle version check
    gradle_version=$(./gradlew --version | grep "Gradle" | cut -d' ' -f2)
    echo "Gradle version: $gradle_version"

    # Kotlin version check
    kotlin_version=$(./gradlew dependencies | grep "org.jetbrains.kotlin:" | head -n 1 | grep -o "[0-9]\+\.[0-9]\+\.[0-9]\+")
    echo "Kotlin version: $kotlin_version"
}
```

### Quick Setup (Default)
```bash
setup_quick() {
    echo "⚡ Quick KtFakes environment setup..."

    # 1. Gradle wrapper setup
    echo "📦 Setting up Gradle wrapper..."
    chmod +x gradlew
    ./gradlew wrapper --gradle-version=8.10.2

    # 2. Clean previous builds
    echo "🧹 Cleaning previous builds..."
    ./gradlew clean

    # 3. Download dependencies
    echo "📥 Downloading dependencies..."
    ./gradlew :compiler:dependencies

    # 4. Verify basic compilation
    echo "🔨 Verifying compilation..."
    ./gradlew :compiler:compileKotlin

    # 5. Setup test sample
    echo "🧪 Setting up test sample..."
    cd test-sample
    ../gradlew compileKotlinJvm
    cd ..

    echo "✅ Quick setup complete!"
}
```

### Full Setup
```bash
setup_full() {
    echo "🚀 Full KtFakes development environment setup..."

    # 1. System validation
    check_system_requirements

    # 2. Gradle configuration
    echo "⚙️ Configuring Gradle..."
    ./gradlew wrapper --gradle-version=8.10.2
    ./gradlew --refresh-dependencies

    # 3. Clean build
    echo "🧹 Clean build..."
    ./gradlew clean

    # 4. Compiler plugin build
    echo "🔨 Building compiler plugin..."
    ./gradlew :compiler:shadowJar

    # 5. Test environment setup
    echo "🧪 Setting up test environment..."
    setup_test_environment

    # 6. IDE configuration
    echo "💻 Setting up IDE integration..."
    setup_ide_integration

    # 7. Validation tests
    echo "✅ Running validation tests..."
    validate_environment

    echo "🎉 Full setup complete!"
}
```

### Test Environment Setup
```bash
setup_test_environment() {
    echo "🧪 Setting up test environment..."

    # 1. Compiler tests
    echo "  📋 Setting up compiler tests..."
    ./gradlew :compiler:test --info

    # 2. Test sample setup
    echo "  📋 Setting up test sample..."
    cd test-sample

    # Clean generated code
    rm -rf build/generated/ktfake/

    # Generate fresh fakes
    ../gradlew clean compileKotlinJvm --no-build-cache

    # Verify generation
    if [ -d "build/generated/ktfake/test/kotlin" ]; then
        echo "  ✅ Fake generation working"
        ls -la build/generated/ktfake/test/kotlin/
    else
        echo "  ❌ Fake generation failed"
        exit 1
    fi

    # Run tests
    ../gradlew jvmTest

    cd ..
}
```

### IDE Integration Setup
```bash
setup_ide_integration() {
    echo "💻 Setting up IDE integration..."

    # 1. IntelliJ IDEA configuration
    echo "  📋 IntelliJ IDEA setup..."

    # Generate IDE files
    ./gradlew idea

    # Setup Kotlin compiler settings
    cat > .idea/kotlinc.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="Kotlin2JvmCompilerArguments">
    <option name="jvmTarget" value="21" />
  </component>
  <component name="KotlinCommonCompilerArguments">
    <option name="apiVersion" value="2.0" />
    <option name="languageVersion" value="2.0" />
  </component>
</project>
EOF

    # 2. VSCode configuration (if .vscode exists)
    if [ -d ".vscode" ]; then
        echo "  📋 VSCode setup..."
        setup_vscode_integration
    fi

    echo "  ✅ IDE integration configured"
}
```

### Environment Validation
```bash
validate_environment() {
    echo "✅ Validating development environment..."

    local validation_passed=true

    # 1. JDK validation
    echo "  📋 Validating JDK..."
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -ge "21" ]; then
        echo "  ✅ JDK $java_version compatible"
    else
        echo "  ❌ JDK $java_version not supported (requires JDK 21+)"
        validation_passed=false
    fi

    # 2. Gradle validation
    echo "  📋 Validating Gradle..."
    if ./gradlew --version > /dev/null 2>&1; then
        echo "  ✅ Gradle working"
    else
        echo "  ❌ Gradle not working"
        validation_passed=false
    fi

    # 3. Kotlin compilation validation
    echo "  📋 Validating Kotlin compilation..."
    if ./gradlew :compiler:compileKotlin > /dev/null 2>&1; then
        echo "  ✅ Kotlin compilation working"
    else
        echo "  ❌ Kotlin compilation failed"
        validation_passed=false
    fi

    # 4. Plugin build validation
    echo "  📋 Validating plugin build..."
    if ./gradlew :compiler:shadowJar > /dev/null 2>&1; then
        echo "  ✅ Plugin build working"
    else
        echo "  ❌ Plugin build failed"
        validation_passed=false
    fi

    # 5. Test environment validation
    echo "  📋 Validating test environment..."
    cd test-sample
    if ../gradlew compileKotlinJvm > /dev/null 2>&1; then
        echo "  ✅ Test environment working"
    else
        echo "  ❌ Test environment failed"
        validation_passed=false
    fi
    cd ..

    # 6. Generated code validation
    echo "  📋 Validating generated code..."
    if [ -f "test-sample/build/generated/ktfake/test/kotlin/TestServiceFakes.kt" ]; then
        echo "  ✅ Code generation working"
    else
        echo "  ❌ Code generation not working"
        validation_passed=false
    fi

    # Final result
    if [ "$validation_passed" = true ]; then
        echo "🎉 Environment validation passed!"
        show_quick_start_guide
    else
        echo "❌ Environment validation failed!"
        show_troubleshooting_guide
        exit 1
    fi
}
```

### Quick Start Guide
```bash
show_quick_start_guide() {
    cat << 'EOF'
🚀 KtFakes Development Environment Ready!

📋 Quick Commands:
  ./gradlew :compiler:shadowJar     # Build compiler plugin
  cd test-sample && ../gradlew build # Test fake generation
  ./gradlew test                     # Run all tests

📋 Development Workflow:
  1. Edit compiler code in compiler/src/main/kotlin/
  2. Rebuild: ./gradlew :compiler:shadowJar
  3. Test: cd test-sample && ../gradlew clean compileKotlinJvm
  4. Verify: Check generated code in test-sample/build/generated/

📋 Testing:
  ./gradlew :compiler:test          # Unit tests
  cd test-sample && ../gradlew jvmTest # Integration tests

📋 Documentation:
  📋 Testing Guidelines: .claude/docs/validation/testing-guidelines.md
  📋 Architecture: .claude/docs/architecture/unified-ir-native.md
  📋 Troubleshooting: .claude/docs/troubleshooting/common-issues.md

📋 Commands Available:
  /debug-ir-generation <interface>
  /analyze-interface-structure <interface>
  /validate-metro-alignment
  /run-bdd-tests <pattern>

Happy coding! 🎉
EOF
}
```

### Troubleshooting Guide
```bash
show_troubleshooting_guide() {
    cat << 'EOF'
❌ Environment Setup Issues Detected

📋 Common Solutions:

1. JDK Issues:
   - Install JDK 21+: https://adoptium.net/
   - Set JAVA_HOME: export JAVA_HOME=/path/to/jdk21
   - Verify: java -version

2. Gradle Issues:
   - Update wrapper: ./gradlew wrapper --gradle-version=8.10.2
   - Refresh dependencies: ./gradlew --refresh-dependencies
   - Clean: ./gradlew clean

3. Compilation Issues:
   - Check Kotlin version in gradle/libs.versions.toml
   - Verify compiler plugin dependencies
   - Clear Gradle cache: rm -rf ~/.gradle/caches/

4. Test Environment Issues:
   - Clean generated code: rm -rf */build/generated/
   - Rebuild plugin: ./gradlew :compiler:shadowJar
   - Test generation: cd test-sample && ../gradlew clean compileKotlinJvm

📋 Get Help:
   - Troubleshooting Guide: .claude/docs/troubleshooting/common-issues.md
   - Run diagnostics: /debug-ir-generation
   - Check status: /check-implementation-status

Try running setup again after fixing issues.
EOF
}
```

### Command Usage Examples

#### Quick Setup (Default)
```bash
# Basic environment setup
/setup-development-environment

# Same as
/setup-development-environment --quick
```

#### Full Setup
```bash
# Complete environment setup with validation
/setup-development-environment --full
```

#### Validation Only
```bash
# Validate existing environment
/setup-development-environment --validate
```

#### Troubleshooting Workflow
```bash
# If setup fails, try step by step:
/setup-development-environment --validate  # Check current state
/setup-development-environment --quick     # Fix basic issues
/setup-development-environment --full      # Complete setup
```

### Integration with Other Commands

#### Post-Setup Validation
```bash
# After setup, validate with other commands
/check-implementation-status
/debug-ir-generation TestService
/run-bdd-tests should_generate_
```

#### Development Workflow Integration
```bash
# Setup → Develop → Test → Debug cycle
/setup-development-environment
# ... make changes ...
/debug-ir-generation UserService
/run-bdd-tests GIVEN_interface_WHEN_generating
```

### Related Documentation

- **[📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)** - GIVEN-WHEN-THEN standards
- **[📋 Common Issues](.claude/docs/troubleshooting/common-issues.md)** - Problem solving guide
- **[📋 Architecture Overview](.claude/docs/architecture/unified-ir-native.md)** - Technical foundation

---

**This command ensures a properly configured development environment for productive KtFakes development with comprehensive validation and troubleshooting support.**