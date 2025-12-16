# Fakt Development Commands
# Run from fakt/ directory (or from project root)

.PHONY: build test compile clean format shadowJar test-sample validate quick-test full-rebuild

# Core build commands
build:
	@echo "🏗️ Building Fakt..."
	./gradlew build

test:
	@echo "🧪 Running tests..."
	./gradlew test

compile:
	@echo "⚙️ Compiling Kotlin sources..."
	./gradlew compileKotlinJvm

clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean

format:
	@echo "✨ Formatting code with Spotless..."
	./gradlew spotlessApply
	@echo "✨ Formatting code with ktlint..."
	./gradlew ktlintFormat

# Compiler plugin specific
shadowJar:
	@echo "📦 Building compiler plugin JAR (debug only - use publish-local for actual usage)..."
	./gradlew :compiler:shadowJar

# Local publishing (use this for development!)
publish-local:
	@echo "📤 Publishing to Maven Local (no signing required locally)..."
	./gradlew publishToMavenLocal --no-daemon

# Test samples (now composite builds - auto-rebuild plugin!)
test-sample:
	@echo "🎯 Testing kmp-single-module sample (composite build)..."
	cd samples/kmp-single-module && ./gradlew build

# KMP multi-module sample
test-kmp-multi-module:
	@echo "🏢 Testing kmp-multi-module sample (composite build)..."
	cd samples/kmp-multi-module && ./gradlew :app:build

# Comprehensive validation workflow (runs all checks like CI)
validate:
	@echo "🔍 Running comprehensive validation..."
	@echo ""
	@echo "1️⃣ Formatting and linting..."
	./gradlew spotlessCheck ktlintCheck
	@echo ""
	@echo "2️⃣ Static analysis..."
	./gradlew detekt
	@echo ""
	@echo "3️⃣ License audit..."
	./gradlew checkLicense
	@echo ""
	@echo "4️⃣ Running tests..."
	./gradlew test
	@echo ""
	@echo "5️⃣ Publishing plugin locally..."
	./gradlew publishToMavenLocal --no-daemon
	@echo ""
	@echo "6️⃣ Testing samples..."
	cd samples/kmp-single-module && ../../gradlew build
	@echo ""
	@echo "✅ Full validation complete!"

# Quick development cycle (composite build auto-rebuilds plugin!)
quick-test:
	@echo "⚡ Quick test cycle (composite builds)..."
	cd samples/kmp-single-module && rm -rf build/generated
	cd samples/kmp-single-module && ./gradlew compileKotlinJvm --no-build-cache

# Full rebuild (nuclear option)
full-rebuild:
	@echo "💥 Full rebuild with clean slate..."
	./gradlew clean --no-build-cache
	./gradlew publishToMavenLocal --no-daemon
	cd samples/kmp-single-module && rm -rf build/generated
	cd samples/kmp-single-module && ./gradlew build

# Debug compiler plugin
debug:
	@echo "🐛 Debugging compiler plugin (composite build)..."
	cd samples/kmp-single-module && ./gradlew compileKotlinJvm -i | grep -E "(Fakt|Generated|ERROR)"

# Help
help:
	@echo "📚 Fakt Development Commands:"
	@echo ""
	@echo "  build           - Build entire project (plugin only, no samples)"
	@echo "  test            - Run all tests"
	@echo "  compile         - Compile Kotlin sources"
	@echo "  clean           - Clean build artifacts"
	@echo "  format          - Format code with Spotless + ktlint"
	@echo ""
	@echo "  shadowJar       - Build compiler plugin JAR (debug only)"
	@echo "  publish-local   - Publish to Maven Local (⭐ use this for development!)"
	@echo "  test-sample     - Test kmp-single-module sample (composite build)"
	@echo "  test-kmp-multi-module - Test kmp-multi-module sample (composite build)"
	@echo ""
	@echo "  validate        - ⭐ Run all validations (format, lint, tests, samples)"
	@echo "  quick-test      - Quick development cycle (auto-rebuilds plugin!)"
	@echo "  full-rebuild    - Nuclear rebuild option"
	@echo "  debug           - Debug compiler plugin output"
	@echo ""
	@echo "  help            - Show this help"
	@echo ""
	@echo "🎯 Note: Samples are now composite builds! Plugin changes auto-rebuild."
	@echo "💡 Tip: Use 'validate' before committing - runs all checks like CI!"