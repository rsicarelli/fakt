# Fakt Development Commands
# Run from ktfake/ directory (cd ktfake && make <command>)

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
	@echo "✨ Formatting code..."
	./gradlew spotlessApply

# Compiler plugin specific
shadowJar:
	@echo "📦 Building compiler plugin JAR..."
	./gradlew :compiler:shadowJar

# Test samples (now composite builds - auto-rebuild plugin!)
test-sample:
	@echo "🎯 Testing kmp-single-module sample (composite build)..."
	cd samples/kmp-single-module && ./gradlew build

# KMP multi-module sample
test-kmp-multi-module:
	@echo "🏢 Testing kmp-multi-module sample (composite build)..."
	cd samples/kmp-multi-module && ./gradlew :app:build

# Comprehensive validation workflow
validate: shadowJar test-sample test
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
	./gradlew :compiler:shadowJar
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
	@echo "  format          - Format code with Spotless"
	@echo ""
	@echo "  shadowJar       - Build compiler plugin JAR"
	@echo "  test-sample     - Test kmp-single-module sample (composite build)"
	@echo "  test-kmp-multi-module - Test kmp-multi-module sample (composite build)"
	@echo ""
	@echo "  validate        - Full validation workflow"
	@echo "  quick-test      - Quick development cycle (auto-rebuilds plugin!)"
	@echo "  full-rebuild    - Nuclear rebuild option"
	@echo "  debug           - Debug compiler plugin output"
	@echo ""
	@echo "  help            - Show this help"
	@echo ""
	@echo "🎯 Note: Samples are now composite builds! Plugin changes auto-rebuild."