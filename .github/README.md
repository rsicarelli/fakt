# CI/CD Strategy for Fakt

This document describes the complete CI/CD strategy, release workflows, and automation for the Fakt project.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Composite Actions](#composite-actions)
- [Workflows](#workflows)
- [Release Strategy](#release-strategy)
- [Required Secrets](#required-secrets)
- [Developer Guide](#developer-guide)

---

## Architecture Overview

Fakt uses a **modular GitHub Actions architecture** with reusable composite actions and three main workflows:

```
┌─────────────────────────────────────────────────────────────┐
│  COMPOSITE ACTIONS (.github/actions/)                       │
│  ════════════════════════════════════════════════════════   │
│  • setup-environment          → JDK + Gradle setup          │
│  • validate-ktlint            → Code formatting check       │
│  • validate-detekt            → Static analysis             │
│  • validate-spotless          → Format validation           │
│  • validate-licenses          → License audit               │
│  • run-tests                  → Test execution              │
│  • test-samples               → End-to-end sample tests     │
│  • publish-maven-central      → Artifact publication        │
│  • create-github-release      → Release creation            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  WORKFLOWS (.github/workflows/)                             │
│  ════════════════════════════════════════════════════════   │
│  1. development.yml      → PR validation (6 parallel jobs)  │
│  2. publish-release.yml  → Main release workflow            │
│  3. publish-hotfix.yml   → Emergency hotfix workflow        │
└─────────────────────────────────────────────────────────────┘
```

---

## Composite Actions

All validation and publication steps are isolated as **reusable composite actions** for:
- Clear separation of concerns
- Better UI visualization in GitHub Actions
- Easy reuse across workflows

### Setup Environment
**Location:** `.github/actions/setup-environment/action.yml`
**Purpose:** Common setup for all jobs (JDK 21 + Gradle)
**Used by:** All workflows

### Validation Actions
**Location:** `.github/actions/validate-*/action.yml`
**Available validators:**
- `validate-ktlint` - Kotlin formatting (ktlint)
- `validate-detekt` - Static analysis (detekt)
- `validate-spotless` - Code formatting (spotless)
- `validate-licenses` - Dependency license audit

### Test Execution
**Location:** `.github/actions/run-tests/action.yml`
**Purpose:** Execute all test suites from all source sets with result publishing
**Features:**
- **KMP modules (runtime)**: Runs `allTests` to test ALL targets (jvmTest, jsTest, iosTest, macosTest, linuxTest, etc.)
- **JVM modules (compiler, gradle-plugin, compiler-api)**: Runs `test` for each module
- **All modules**: Runs `check` for additional validations (apiCheck, binary compatibility, compilation validation)
- Guarantees comprehensive test coverage across all platforms

**Location:** `.github/actions/test-samples/action.yml`
**Purpose:** End-to-end validation using sample projects
**Features:**
- Tests samples via composite build (automatic shadowJar usage)
- Validates plugin functionality in real-world scenarios
- Runs `allTests` to test ALL KMP targets in samples
- Runs `check` for additional validations (apiCheck, compilation validation)
- **MANDATORY** - Releases blocked if samples fail

### Publication Actions
**Location:** `.github/actions/publish-maven-central/action.yml`
**Purpose:** Publish artifacts to Maven Central Portal
**Inputs:**
- `version` - Version to publish
- `maven-central-username` - Token ID
- `maven-central-password` - Token secret
- `signing-key` - GPG key (Base64)
- `signing-password` - GPG passphrase

**Location:** `.github/actions/create-github-release/action.yml`
**Purpose:** Create GitHub Release with auto-generated changelog
**Features:**
- Auto-generates changelog from merged PRs (`gh release create --generate-notes`)
- Auto-detects pre-release from tag (`-rc`, `-alpha`, `-beta`)

---

## Workflows

### 1. Development Workflow
**File:** `.github/workflows/development.yml`
**Trigger:** Pull requests to `main` or `master`
**Concurrency:** Cancels in-progress runs for same PR

**Jobs (run in parallel):**
1. ✅ **validate-ktlint** - Kotlin formatting check
2. ✅ **validate-detekt** - Static analysis
3. ✅ **validate-spotless** - Code formatting check
4. ✅ **validate-licenses** - Dependency license audit
5. ✅ **run-tests** - Execute all tests
6. ✅ **test-samples** - End-to-end sample project tests (REQUIRED)

**Purpose:** Ensure code quality before merge
**Branch Protection:** All checks must pass before merge is allowed

---

### 2. Publish Release Workflow
**File:** `.github/workflows/publish-release.yml`
**Trigger:** Manual (`workflow_dispatch`) with input parameter
**Input:** `bump` (choice: major, minor, patch)

**Sequential Jobs:**

**Job 1: Validation**
- Fail if not on `main`/`master` branch
- Run all validation checks (ktlint, detekt, spotless, licenses, tests)

**Job 2: Publish** (depends on validation success)
1. Create GitHub App token (for auto-commits)
2. Checkout with App token
3. Setup environment
4. **Bump version** using Kotlin script
   - Reads current version from `gradle.properties`
   - Applies bump (major/minor/patch)
   - Updates `gradle.properties`
5. **Publish to Maven Central**
   - Signs artifacts with GPG
   - Publishes to Maven Central Portal
   - Automatic release enabled
6. **Create and push git tag** (e.g., `v1.3.0`)
7. **Create GitHub Release**
   - Auto-generates changelog from PRs
   - Uses tag from previous step
8. **Bump to next SNAPSHOT**
   - Current: `1.3.0` → Next: `1.3.0-SNAPSHOT`
   - Updates `gradle.properties`
9. **Auto-commit and push** to `main`
   - Message: `chore: bump to 1.3.0-SNAPSHOT [skip ci]`

**Example Flow:**
```
gradle.properties: VERSION_NAME=1.2.0-SNAPSHOT
↓ (workflow_dispatch: bump=minor)
Validation passes ✅
↓
Bump: 1.2.0-SNAPSHOT → 1.3.0
↓
Publish to Maven Central ✅
↓
Create tag: v1.3.0
↓
Create GitHub Release ✅ (auto-changelog)
↓
Bump: 1.3.0 → 1.4.0-SNAPSHOT
↓
Auto-commit to main ✅
↓
gradle.properties: VERSION_NAME=1.4.0-SNAPSHOT
```

---

### 3. Publish Hotfix Workflow
**File:** `.github/workflows/publish-hotfix.yml`
**Trigger:** Manual (`workflow_dispatch`)
**Constraint:** Must be run from a `hotfix/*` branch (NOT `main`)

**Sequential Jobs:**

**Job 1: Validation**
- Fail if on `main`/`master` branch
- Run all validation checks

**Job 2: Publish** (depends on validation success)
1. Checkout
2. Setup environment
3. **Auto-bump patch version**
   - Script automatically applies patch bump
   - Example: `1.3.0` → `1.3.1`
4. **Publish to Maven Central**
5. **Create and push git tag** (e.g., `v1.3.1`)
6. **Create GitHub Release**
7. **Push hotfix branch** (stays at `1.3.1`)
   - **NO auto-commit to next SNAPSHOT**
   - Branch remains at hotfix version for potential `1.3.2`, `1.3.3`, etc.

**Hotfix Workflow:**
```
1. Production issue found in v1.3.0

2. Create hotfix branch:
   git checkout -b hotfix/1.3.x v1.3.0

3. Fix bug, commit to hotfix/1.3.x

4. Trigger "Publish Hotfix" workflow
   ↓
   Current: 1.3.0
   ↓ (auto patch bump)
   Release: 1.3.1
   ↓
   GitHub Release created ✅
   ↓
   Branch stays at 1.3.1 (no SNAPSHOT bump)

5. Cherry-pick to main (if needed):
   git checkout main
   git cherry-pick <commit-sha>
   git push
```

---

## Release Strategy

### Version Management
**Source of Truth:** `gradle.properties` → `VERSION_NAME`

**Format:**
- Release: `X.Y.Z` (e.g., `1.3.0`)
- Development: `X.Y.Z-SNAPSHOT` (e.g., `1.4.0-SNAPSHOT`)

**Bump Types:**
- **major:** `1.3.0` → `2.0.0` (breaking changes)
- **minor:** `1.3.0` → `1.4.0` (new features, backwards-compatible)
- **patch:** `1.3.0` → `1.3.1` (bug fixes)

### Changelog Generation
**Tool:** GitHub's native `gh release create --generate-notes`

**Features:**
- Automatically generates changelog from merged PRs
- Groups changes by labels (features, bug fixes, etc.)
- Lists all contributors
- Zero manual maintenance

**Customize:** Use PR labels to categorize changes
- `enhancement` → Features
- `bug` → Bug Fixes
- `documentation` → Documentation
- `dependencies` → Dependencies

### Release Types

**Regular Releases (from main):**
- Use "Publish Release" workflow
- Choose bump type (major/minor/patch)
- Automatic version bump
- Auto-commit next SNAPSHOT

**Hotfixes (from hotfix branch):**
- Use "Publish Hotfix" workflow
- Always patch bump
- NO auto-commit (branch stays at hotfix version)
- Manually cherry-pick to main if needed

**Pre-releases:**
- Tag with suffix: `-rc`, `-alpha`, `-beta`
- Example: `v1.3.0-rc01`
- Automatically marked as pre-release on GitHub

---

## Required Secrets

Configure in **GitHub Repository Settings → Secrets and variables → Actions**:

### Maven Central Credentials
| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `MAVEN_CENTRAL_USERNAME` | Token ID | Generate User Token at [central.sonatype.com](https://central.sonatype.com) |
| `MAVEN_CENTRAL_PASSWORD` | Token Secret | From User Token generation |

### GPG Signing
| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `GPG_SIGNING_KEY` | GPG private key (Base64) | `gpg --export-secret-keys --armor KEY_ID \| base64` |
| `GPG_SIGNING_PASSWORD` | GPG key passphrase | From GPG key generation |

**GPG Key Generation:**
```bash
# Generate key
gpg --full-generate-key
# Choose: RSA and RSA, 4096 bits, no expiration

# Export private key (Base64 for GitHub Secrets)
gpg --export-secret-keys --armor "KEY_ID" | base64

# Export public key (upload to keyserver)
gpg --export --armor "KEY_ID"
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
```

### GitHub App (for auto-commits)
| Secret Name | Description | How to Obtain |
|-------------|-------------|---------------|
| `GH_APP_ID` | GitHub App ID | Create GitHub App in repo settings |
| `GH_APP_PRIVATE_KEY` | App's private key | Generate in App settings |

**Why GitHub App?**
- Bypasses branch protection rules for auto-commits
- More secure than Personal Access Tokens
- Scoped permissions per repository

---

## Developer Guide

### Running Workflows Locally

**Important:** For Kotlin Multiplatform, use `allTests` + `check`:
- `./gradlew test` - Only runs HOST target tests (usually JVM) - ❌ INCOMPLETE
- `./gradlew allTests` - Runs tests for ALL KMP targets - ✅ COMPLETE
- `./gradlew check` - Runs validation tasks (apiCheck, etc.) - ✅ VALIDATIONS

**For complete coverage:**
1. `./gradlew :runtime:allTests` - Test ALL targets (jvmTest, jsTest, iosTest, etc.)
2. `./gradlew :compiler:test :compiler-api:test :gradle-plugin:test` - Test JVM modules
3. `./gradlew check` - Run all validations (apiCheck, binary compatibility)

**Test validation steps:**
```bash
# Ktlint
./gradlew lintKotlin

# Detekt
./gradlew detekt

# Spotless
./gradlew spotlessCheck

# License check
./gradlew checkLicense

# Run ALL tests for ALL targets (KMP)
./gradlew :runtime:allTests

# Run tests for JVM modules
./gradlew :compiler:test :compiler-api:test :gradle-plugin:test

# Run all validations (apiCheck, compilation validation)
./gradlew check

# Test samples (all targets + validations)
./gradlew -p samples/kmp-single-module allTests check
./gradlew -p samples/multi-module allTests check
```

### Version Bump Script

**Location:** `.github/scripts/bump-version.main.kts`

**Usage:**
```bash
# Bump minor version
kotlin .github/scripts/bump-version.main.kts minor

# Bump patch version
kotlin .github/scripts/bump-version.main.kts patch

# Bump major version
kotlin .github/scripts/bump-version.main.kts major

# Specify current version explicitly
kotlin .github/scripts/bump-version.main.kts patch 1.2.3
```

**Output:**
```
VERSION_CURRENT=1.2.0
VERSION_NEW=1.3.0
TAG=v1.3.0
```

### Branch Protection Rules

**Recommended settings for `main` branch:**
- ✅ Require pull request before merging
- ✅ Require approvals: 1
- ✅ Require status checks to pass:
  - `validate-ktlint`
  - `validate-detekt`
  - `validate-spotless`
  - `validate-licenses`
  - `run-tests`
  - `test-samples`
- ✅ Require conversation resolution before merging
- ✅ Require linear history

### Troubleshooting

**Publication fails with "401 Unauthorized":**
- Verify Maven Central credentials are correct
- Regenerate User Token if expired

**GPG signing fails:**
- Ensure GPG key is Base64 encoded: `gpg --export-secret-keys --armor KEY_ID | base64`
- Verify passphrase is correct

**Auto-commit fails:**
- Check GitHub App has write permissions
- Verify App ID and Private Key are correct

**License check fails:**
- Review `allowed-licenses.json` in project root
- Check dependency licenses: `./gradlew generateLicenseReport`
- Report located at: `build/reports/dependency-license/licenses.json`

---

## License Auditing

**Configuration:** `allowed-licenses.json`

**Allowed Licenses:**
- Apache 2.0
- MIT
- BSD (2-Clause, 3-Clause)
- EPL 1.0/2.0
- CC0/Public Domain

**Prohibited Licenses (copyleft):**
- GPL, LGPL, AGPL (any version)

**Check licenses:**
```bash
./gradlew checkLicense
```

**Generate report:**
```bash
./gradlew generateLicenseReport
cat build/reports/dependency-license/licenses.json
```

---

## Contributing

For contribution guidelines, see [CONTRIBUTING.md](../CONTRIBUTING.md).

For security policies, see [SECURITY.md](../SECURITY.md).
