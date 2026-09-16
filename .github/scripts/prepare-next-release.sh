#!/bin/bash
# prepare-next-release.sh - Unified post-release preparation
# Usage: prepare-next-release.sh <published_version> <bump_type> <release_type>

set -e

PUBLISHED_VERSION=$1
BUMP_TYPE=$2
RELEASE_TYPE=$3

if [ -z "$PUBLISHED_VERSION" ] || [ -z "$BUMP_TYPE" ] || [ -z "$RELEASE_TYPE" ]; then
  echo "❌ Usage: $0 <published_version> <bump_type> <release_type>"
  echo "   Example: $0 1.0.0-alpha01 minor alpha"
  exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 Preparing for next release after $PUBLISHED_VERSION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Step 1: Sync documentation to published version
echo ""
echo "📚 Step 1/5: Syncing docs to published version..."
kotlin .github/scripts/sync-docs-version.main.kts "$PUBLISHED_VERSION"
echo "  ✓ Documentation synced to $PUBLISHED_VERSION"

# Step 2: Clean kotlin-js-store
echo ""
echo "🧹 Step 2/5: Cleaning vendor/kotlin-js-store..."
if [ -d "vendor/kotlin-js-store" ]; then
  rm -rf vendor/kotlin-js-store
  echo "  ✓ Removed vendor/kotlin-js-store/"
else
  echo "  ℹ No vendor/kotlin-js-store directory found (skipping)"
fi

# Step 3: Regenerate lock files
echo ""
echo "🔄 Step 3/5: Regenerating JS/WASM lock files..."
./gradlew kotlinUpgradePackageLock --no-daemon --quiet || true
./gradlew kotlinWasmUpgradePackageLock --no-daemon --quiet || true
echo "  ✓ Lock files regenerated"

# Step 4: Bump to next development version (NO SNAPSHOT)
echo ""
echo "⬆️  Step 4/5: Bumping to next development version..."
kotlin .github/scripts/bump-version.main.kts "$BUMP_TYPE" "$RELEASE_TYPE"
NEXT_VERSION=$(grep "version=" gradle.properties | cut -d'=' -f2)
echo "  ✓ Bumped to $NEXT_VERSION"

# Step 5: Stage all changes for commit
echo ""
echo "📝 Step 5/5: Staging changes..."
git add \
  gradle.properties \
  gradle/libs.versions.toml \
  docs/ \
  samples/ \
  README.md \
  gradle-plugin/README.md \
  gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/FaktGradleSubplugin.kt \
  vendor/kotlin-js-store 2>/dev/null || true

# Check if there are changes to commit
if git diff --cached --quiet; then
  echo "  ⚠ No changes to commit"
  exit 0
fi

# Create unified commit
echo ""
echo "💾 Creating unified commit..."
git commit -m "chore: prepare for next version $NEXT_VERSION"
echo "  ✓ Committed: chore: prepare for next version $NEXT_VERSION"

# Output for GitHub Actions
if [ -n "$GITHUB_OUTPUT" ]; then
  echo "next_version=$NEXT_VERSION" >> "$GITHUB_OUTPUT"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Done! Ready to push."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
