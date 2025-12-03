# Deployment & Release Management

> **Automated CI/CD pipeline for seamless Fakt releases to Maven Central**

Fakt uses a sophisticated, automated deployment pipeline that ensures reliable releases while maintaining semantic versioning and comprehensive documentation.

## 🏗️ Architecture Overview

Our deployment system follows a **single source of truth** approach:

```
gradle.properties:VERSION_NAME → Maven Central + GitHub Releases
```

All version information flows from `gradle.properties:VERSION_NAME`, ensuring consistency across the entire ecosystem.

## 🤖 Continuous Integration & Deployment

### **Continuous Deploy (Automated)**

**Trigger**: Every merge to `main` branch
**Target**: Maven Central (SNAPSHOT repository)
**Workflow**: `.github/workflows/continuous-deploy.yml`

```mermaid
graph LR
    A[PR Merged] → B[Detect SNAPSHOT] → C[Publish to Maven Central] → D[✅ Available]
```

**Process**:
1. **Validation**: Ensures `VERSION_NAME` contains `-SNAPSHOT`
2. **Publishing**: Automatic upload to Maven Central staging
3. **Availability**: SNAPSHOT immediately available for consumption

**Example**:
```bash
# gradle.properties
VERSION_NAME=1.0.0-SNAPSHOT

# After merge → Available as:
# com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT
# com.rsicarelli.fakt:compiler:1.0.0-SNAPSHOT
```

---

### **Manual Releases (Semantic Versioning)**

**Trigger**: Manual workflow dispatch
**Target**: Maven Central (release repository) + GitHub Releases
**Workflow**: `.github/workflows/publish-release.yml`

```mermaid
graph TD
    A[Manual Trigger] → B[Choose Version Bump] → C[Update Version] → D[Sync Documentation]
    D → E[Publish to Maven Central] → F[Create Git Tag] → G[GitHub Release]
    G → H[Auto-bump to next SNAPSHOT]
```

**Supported Bump Types**:
- **`major`**: Breaking changes (1.0.0 → 2.0.0)
- **`minor`**: New features (1.0.0 → 1.1.0)
- **`patch`**: Bug fixes (1.0.0 → 1.0.1)

**Process**:
1. **Version Bump**: Semantic versioning via `.github/scripts/bump-version.main.kts`
2. **Documentation Sync**: Auto-update all docs/samples with new version
3. **Maven Central**: Publish signed artifacts
4. **GitHub Release**: Auto-generated changelog with commit history
5. **Next Development**: Auto-bump to next SNAPSHOT (e.g., 1.1.0 → 1.2.0-SNAPSHOT)

---

### **Hotfix Releases (Emergency Patches)**

**Trigger**: Manual workflow from hotfix branch
**Target**: Maven Central + GitHub Releases
**Workflow**: `.github/workflows/publish-hotfix.yml`

```mermaid
graph LR
    A[Hotfix Branch] → B[Patch Bump] → C[Publish] → D[Tag & Release]
```

**Branch Strategy**:
```bash
# Create hotfix branch from release tag
git checkout v1.0.0
git checkout -b hotfix/1.0.x

# Make fixes, then trigger hotfix workflow
# Automatically bumps: 1.0.0 → 1.0.1
```

## 🔄 Version Management

### **Single Source of Truth**

All versioning flows from `gradle.properties`:

```properties
# gradle.properties - The authoritative version source
VERSION_NAME=1.0.0-SNAPSHOT
```

### **Automatic Synchronization**

The `.github/scripts/sync-docs-version.main.kts` script ensures consistency across:

- **Version Catalogs**: `gradle/libs.versions.toml`
- **Documentation**: All `.md` files
- **Samples**: Build scripts in `samples/`
- **README**: Main project documentation

**Patterns Updated**:
```kotlin
// Plugin declarations
id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"

// Version catalog entries
fakt = "1.0.0-SNAPSHOT"

// Maven coordinates
com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT

// Documentation examples
- **Fakt**: 1.0.0-SNAPSHOT+
```

## 🚀 Release Process

### **Standard Release (Minor/Major)**

1. **Prepare Release**
   - Ensure `main` branch is stable
   - Verify all tests pass
   - Review pending changes for scope (breaking vs feature vs patch)

2. **Trigger Release**
   ```bash
   # Navigate to Actions → Publish Release → Run workflow
   # Select bump type: major | minor | patch
   ```

3. **Automatic Process**
   - Version bump (e.g., `1.0.0-SNAPSHOT` → `1.1.0`)
   - Documentation synchronization
   - Maven Central publication
   - Git tag creation (`v1.1.0`)
   - GitHub Release with auto-generated changelog
   - Next SNAPSHOT bump (`1.2.0-SNAPSHOT`)

4. **Verification**
   ```bash
   # Check Maven Central availability
   # https://central.sonatype.com/artifact/com.rsicarelli.fakt/runtime

   # Verify GitHub Release
   # https://github.com/rsicarelli/fakt/releases
   ```

---

### **Hotfix Release**

1. **Create Hotfix Branch**
   ```bash
   git checkout v1.0.0
   git checkout -b hotfix/1.0.x
   git push -u origin hotfix/1.0.x
   ```

2. **Apply Fixes**
   ```bash
   # Make necessary changes
   git commit -m "fix: critical issue in runtime"
   git push origin hotfix/1.0.x
   ```

3. **Trigger Hotfix Workflow**
   ```bash
   # From hotfix branch → Actions → Publish Hotfix → Run workflow
   # Automatically bumps patch version: 1.0.0 → 1.0.1
   ```

4. **Merge Back** (Manual)
   ```bash
   # After successful hotfix, merge back to main
   git checkout main
   git merge hotfix/1.0.x
   ```

## 🔐 Security & Signing

### **Maven Central Requirements**

All releases are automatically signed using GPG keys stored as GitHub Secrets:

```yaml
# Required Secrets
MAVEN_CENTRAL_USERNAME    # Sonatype Central Portal token
MAVEN_CENTRAL_PASSWORD    # Sonatype Central Portal password
GPG_SIGNING_KEY          # GPG private key (Base64 encoded)
GPG_SIGNING_PASSWORD     # GPG key passphrase
```

### **Artifact Verification**

Published artifacts include:
- **JAR files**: Core runtime and compiler
- **Sources JAR**: For IDE debugging
- **Javadoc JAR**: API documentation
- **POM files**: Dependency metadata
- **GPG signatures**: `.asc` files for verification

## 📊 Monitoring & Verification

### **Release Health Checks**

After each release, verify:

1. **Maven Central Availability**
   ```bash
   # Check new version appears
   curl -s "https://search.maven.org/solrsearch/select?q=g:com.rsicarelli.fakt"
   ```

2. **GitHub Release Creation**
   - Release notes generated
   - Assets attached correctly
   - Tag points to correct commit

3. **Documentation Sync**
   - Version catalog updated
   - Sample projects reference new version
   - Documentation examples current

### **SNAPSHOT Monitoring**

SNAPSHOT releases are published continuously:
```bash
# Latest SNAPSHOT always available
com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT
```

## 🛠️ Development Workflow

### **For Contributors**

```bash
# Normal development - SNAPSHOTs published automatically
git checkout -b feature/awesome-feature
# ... make changes ...
git commit -m "feat: add awesome feature"
git push origin feature/awesome-feature
# Create PR → Merge → SNAPSHOT automatically published
```

### **For Maintainers**

```bash
# Release planning
1. Review accumulated changes since last release
2. Determine semantic version impact (major/minor/patch)
3. Trigger appropriate release workflow
4. Monitor release pipeline completion
5. Verify artifacts in Maven Central
6. Announce release in appropriate channels
```

## ⚡ Quick Reference

| Task | Command | Result |
|------|---------|--------|
| **Development** | Merge PR to main | Auto-publishes SNAPSHOT |
| **Feature Release** | Workflow dispatch → `minor` | 1.0.0 → 1.1.0 |
| **Breaking Changes** | Workflow dispatch → `major` | 1.0.0 → 2.0.0 |
| **Bug Fix Release** | Workflow dispatch → `patch` | 1.0.0 → 1.0.1 |
| **Emergency Fix** | Hotfix branch workflow | Immediate patch release |
| **Version Check** | `gradle.properties` | Single source of truth |

---

## 🆘 Troubleshooting

### **Common Issues**

**Release workflow fails with signing errors**:
```bash
# Verify GPG secrets are correctly configured
# Check GPG_SIGNING_KEY is properly Base64 encoded
# Ensure GPG_SIGNING_PASSWORD matches the key
```

**SNAPSHOT not appearing in Maven Central**:
```bash
# SNAPSHOTs may take 5-10 minutes to propagate
# Check workflow logs for publication errors
# Verify VERSION_NAME contains "-SNAPSHOT" suffix
```

**Documentation out of sync**:
```bash
# Run manual synchronization
kotlin .github/scripts/sync-docs-version.main.kts
git add . && git commit -m "docs: sync version references"
```

### **Emergency Procedures**

**Corrupt release published**:
1. Do not delete from Maven Central (impossible)
2. Immediately publish hotfix with incremented version
3. Update documentation to skip problematic version

**Failed release workflow**:
1. Check workflow logs for specific error
2. Fix underlying issue
3. Re-run workflow (safe to retry)
4. Manual cleanup if needed (tags, releases)

---

*For additional support, consult the [Troubleshooting Guide](../troubleshooting.md) or open an issue.*