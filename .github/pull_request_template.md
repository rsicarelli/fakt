## Description
<!-- Clear description of what this PR changes -->

## Related Issue
Fixes #(issue)

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation
- [ ] Refactor/Performance/Build

## Pre-Submission Checklist
- [ ] Code formatted: `./gradlew spotlessApply` (CI runs `spotlessCheck`)
- [ ] Static analysis: `./gradlew detekt`
- [ ] Public API check passing: `./gradlew apiCheck` (run `./gradlew apiDump` if the API change is intended)
- [ ] Tests added/updated and passing: `./gradlew test`
- [ ] Generated code compiles (verified with sample project: `make publish-local && make test-sample`)
- [ ] Updated documentation if needed
- [ ] No breaking changes OR breaking changes documented

**Shortcut (if using Makefile):** `make format && make validate`

## Additional Context
<!-- Screenshots, notes for reviewers, etc. -->
