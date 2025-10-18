# Security Policy

## Supported Versions

We actively support the following versions of Fakt with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

**DO NOT** open public issues for security vulnerabilities.

### Private Disclosure Process

If you discover a security vulnerability in Fakt, please report it privately:

**📧 Email:** security@rsicarelli.dev

**Include in your report:**
1. **Description** - Detailed description of the vulnerability
2. **Impact** - What can an attacker do? What is compromised?
3. **Reproduction** - Step-by-step instructions to reproduce
4. **Environment** - Versions (Fakt, Kotlin, Gradle, OS)
5. **Suggested Fix** - (Optional) Ideas for fixing the issue

---

## Response Timeline

You can expect the following timeline:

| Stage | Timeline |
|-------|----------|
| **Initial Response** | Within 48 hours |
| **Assessment** | Within 7 days |
| **Fix Development** | Depends on severity |
| **Coordinated Disclosure** | Typically 90 days after fix |

---

## What Happens Next?

### 1. Acknowledgment
We'll acknowledge receipt of your report within 48 hours.

### 2. Assessment
We'll investigate and assess the severity of the vulnerability:
- **Critical:** Remote code execution, arbitrary file access
- **High:** Elevation of privileges, information disclosure
- **Medium:** Denial of service, limited information disclosure
- **Low:** Minor issues with limited impact

### 3. Fix Development
We'll develop and test a fix. Timeline depends on:
- Severity of the issue
- Complexity of the fix
- Testing requirements

### 4. Coordinated Disclosure
**Standard disclosure timeline: 90 days**

We'll work with you to:
- Agree on disclosure date
- Prepare advisory
- Credit you (if desired) in release notes and security advisory

### 5. Public Release
Once the fix is released:
- Security advisory published on GitHub
- CVE assigned (if applicable)
- Release notes include security fix details
- Credit given to reporter (unless anonymity requested)

---

## Security Best Practices for Users

### Dependency Management

**Keep Fakt updated:**
```gradle
plugins {
    id("com.rsicarelli.fakt") version "1.x.x" // Use latest version
}
```

**Monitor for updates:**
- Watch the [GitHub repository](https://github.com/rsicarelli/fakt)
- Subscribe to [GitHub Releases](https://github.com/rsicarelli/fakt/releases)
- Enable [Dependabot alerts](https://github.com/rsicarelli/fakt/security/dependabot)

### Build Environment Security

**Use trusted plugin repositories:**
```kotlin
pluginManagement {
    repositories {
        mavenCentral()      // ✅ Trusted
        gradlePluginPortal() // ✅ Trusted
        // ❌ Avoid untrusted repos
    }
}
```

**Verify artifact signatures:**
All Fakt releases are signed with GPG. Verify signatures:
```bash
# Download .asc signature file from Maven Central
# Verify signature
gpg --verify fakt-compiler-1.0.0.jar.asc fakt-compiler-1.0.0.jar
```

**Lock dependency versions:**
```kotlin
// Use exact versions (not dynamic)
id("com.rsicarelli.fakt") version "1.2.3" // ✅ Good
id("com.rsicarelli.fakt") version "1.+"   // ❌ Avoid
```

### Build Isolation

**Run builds in isolated environments:**
- Use containerized CI/CD (Docker)
- Avoid running untrusted code in production environment
- Separate build and production credentials

---

## Known Vulnerabilities

### Current Status
✅ No known security vulnerabilities

### Past Vulnerabilities
*(This section will list historical vulnerabilities and their fixes)*

**Format:**
```
CVE-YYYY-XXXXX | Severity: High | Fixed in: v1.x.x
Description: [Brief description]
Fix: [Brief fix description]
```

---

## Security Audit History

| Date | Type | Findings | Status |
|------|------|----------|--------|
| 2025-01 | Internal Review | Dependency license audit | ✅ Passed |
| 2025-01 | Static Analysis | Detekt + Spotless | ✅ Passed |
| TBD | External Audit | Pending | 🔄 Planned |

---

## Scope

### In Scope

The following are within the scope of our security policy:

✅ **Fakt Compiler Plugin:**
- Code generation vulnerabilities
- Compiler crashes or DoS
- Information disclosure

✅ **Gradle Plugin:**
- Configuration injection
- Path traversal
- Credential leakage

✅ **Dependencies:**
- Known vulnerabilities in transitive dependencies
- License compatibility issues

### Out of Scope

The following are **NOT** considered security vulnerabilities:

❌ **User errors:**
- Misconfiguration by users
- Insecure code written by users

❌ **Test/Development code:**
- Issues in sample projects
- Issues in test fixtures

❌ **Third-party issues:**
- Vulnerabilities in Kotlin compiler
- Vulnerabilities in Gradle

❌ **Denial of Service via:**
- Extremely large input files (expected build failure)
- Resource exhaustion by design

---

## Security Features

### Dependency License Auditing

Fakt automatically audits all dependencies for license compatibility:

```bash
./gradlew checkLicense
```

**Allowed licenses:**
- Apache 2.0, MIT, BSD
- EPL 1.0/2.0
- CC0/Public Domain

**Blocked licenses:**
- GPL, LGPL, AGPL (copyleft)

### Code Generation Safety

**Sandboxed code generation:**
- Generated code operates within test scope only
- No production code modification
- No access to system resources

**Type safety:**
- Generated code is fully type-safe
- Compile-time validation
- No reflection-based bypasses

---

## Bug Bounty

**Status:** Not currently offered

We appreciate responsible disclosure but do not currently offer a bug bounty program. This may change as the project grows.

---

## Security Updates

### Notification Channels

Stay informed about security updates:

1. **GitHub Security Advisories:** [Subscribe](https://github.com/rsicarelli/fakt/security/advisories)
2. **GitHub Watch:** Enable "Releases only" notifications
3. **RSS Feed:** [Releases RSS](https://github.com/rsicarelli/fakt/releases.atom)

### Update Policy

**Security patches:**
- Released as soon as possible
- Prioritized over feature releases
- May skip minor version if critical

**Example:**
- Current: v1.2.0
- Security fix: v1.2.1 (patch)
- Critical fix: v1.3.0 (minor) if patch insufficient

---

## Contact

**Security concerns:** security@rsicarelli.dev
**General questions:** [GitHub Discussions](https://github.com/rsicarelli/fakt/discussions)
**Bug reports:** [GitHub Issues](https://github.com/rsicarelli/fakt/issues)

---

## Acknowledgments

We thank the following security researchers for responsible disclosure:

*(This section will credit security researchers who report vulnerabilities)*

---

Thank you for helping keep Fakt secure! 🔒
