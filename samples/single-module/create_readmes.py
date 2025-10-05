#!/usr/bin/env python3
"""
Create README.md files for each scenario category.
"""
from pathlib import Path

READMES = {
    "1_basic": {
        "title": "1. Basic Interfaces",
        "priority": "P0 - Must Work",
        "description": "Fundamental interface fake generation without generics",
        "what_tested": [
            "Simple interfaces with methods only",
            "Properties (val/var)",
            "Unit return types",
            "Interface inheritance (extends)",
            "Multiple method interfaces",
        ],
        "test_coverage": [
            "AnalyticsService.kt → AnalyticsServiceTest.kt",
            "AnalyticsServiceExtended.kt → AnalyticsServiceExtendedTest.kt",
            "AuthenticationService.kt → AuthenticationServiceTest.kt",
            "ProductService.kt → ProductServiceTest.kt",
            "UserRepository.kt → UserRepositoryTest.kt",
            "ComplexApiService.kt → ComplexApiServiceTest.kt",
        ],
        "related_docs": [
            "Testing Guidelines: `.claude/docs/validation/testing-guidelines.md`",
            "Current Status: `.claude/docs/implementation/current-status.md`",
        ],
    },
    "2_properties_and_methods": {
        "title": "2. Properties & Methods",
        "priority": "P0 - Must Work",
        "description": "Interfaces combining properties and methods",
        "what_tested": [
            "val properties with getters",
            "var properties with getters/setters",
            "Methods with various return types",
            "Suspend functions",
            "Collections in signatures",
        ],
        "test_coverage": [
            "PropertyAndMethodInterface.kt → PropertyAndMethodInterfaceTest.kt",
            "AsyncDataService.kt → AsyncDataServiceTest.kt",
            "EventProcessor.kt → EventProcessorTest.kt",
            "CollectionService.kt → CollectionServiceTest.kt",
            "ResultService.kt → ResultServiceTest.kt",
        ],
        "related_docs": [
            "Architecture: `.claude/docs/architecture/unified-ir-native.md`",
        ],
    },
    "3_generics_basic": {
        "title": "3. Basic Generics",
        "priority": "P0 - Must Work",
        "description": "Single type parameter generic interfaces",
        "what_tested": [
            "Single type parameter: `interface Repository<T>`",
            "Generic methods with class-level types",
            "Type-safe fake generation",
            "Generic properties",
        ],
        "test_coverage": [
            "SimpleRepository.kt → SimpleRepositoryTest.kt",
            "DataCache.kt → DataCacheTest.kt",
            "GenericEventProcessor.kt → GenericEventProcessorTest.kt",
            "WorkflowManager.kt → WorkflowManagerTest.kt",
            "EnterpriseRepository.kt → EnterpriseRepositoryTest.kt",
        ],
        "related_docs": [
            "Generics Roadmap: `.claude/docs/implementation/generics/ROADMAP.md`",
            "Phase 1: Core Infrastructure",
        ],
    },
    "4_generics_multiple": {
        "title": "4. Multiple Type Parameters",
        "priority": "P1 - Should Work",
        "description": "Interfaces with multiple generic type parameters",
        "what_tested": [
            "Two type parameters: `interface KeyValueStore<K, V>`",
            "Three type parameters: `interface TripleStore<K1, K2, V>`",
            "Generic collections",
            "Type-safe operations across parameters",
        ],
        "test_coverage": [
            "KeyValueStore.kt → KeyValueStoreTest.kt",
            "CacheService.kt → CacheServiceTest.kt",
            "TripleStore.kt → TripleStoreTest.kt",
        ],
        "related_docs": [
            "Generics Roadmap: `.claude/docs/implementation/generics/ROADMAP.md`",
            "Phase 2: Code Generation",
        ],
    },
    "5_generics_constraints": {
        "title": "5. Generic Constraints",
        "priority": "P1 - Should Work",
        "description": "Generics with type constraints",
        "what_tested": [
            "Upper bounds: `<T : Comparable<T>>`",
            "Multiple constraints: `where T : A, T : B`",
            "Constraint propagation to generated code",
        ],
        "test_coverage": [
            "SortedRepository.kt → SortedRepositoryTest.kt",
        ],
        "related_docs": [
            "Technical Reference: `.claude/docs/implementation/generics/technical-reference.md`",
        ],
    },
    "6_generics_method_level": {
        "title": "6. Method-Level Generics",
        "priority": "P2 - Nice to Have",
        "description": "Interfaces with method-level generic type parameters",
        "what_tested": [
            "Method-level generics: `fun <T> process(item: T): T`",
            "Mixed class and method generics",
            "Type erasure handling",
            "Identity function pattern for generic methods",
        ],
        "test_coverage": [
            "DataProcessor.kt → DataProcessorTest.kt",
            "MixedProcessor.kt → MixedProcessorTest.kt",
            "GenericRepository.kt → GenericRepositoryTest.kt",
        ],
        "related_docs": [
            "Generics Roadmap: `.claude/docs/implementation/generics/ROADMAP.md`",
            "Known Limitations section",
        ],
    },
    "7_sam_interfaces": {
        "title": "7. SAM Interfaces (fun interface)",
        "priority": "P0-P3 (varies by category)",
        "description": "Single Abstract Method (SAM) interfaces with various complexity levels",
        "what_tested": [
            "**basic/** [P0]: Simple SAM with primitives, nullable types, Unit returns",
            "**generics/** [P0]: SAM with class-level generic type parameters",
            "**collections/** [P1]: SAM with List, Map, Set, Array types",
            "**stdlib_types/** [P1]: SAM with Result, Sequence, Pair, Triple, Lazy",
            "**higher_order/** [P2]: SAM with function parameters and returns",
            "**variance/** [P2]: SAM with variance annotations (in/out)",
            "**edge_cases/** [P3]: Varargs, star projections, recursive generics",
        ],
        "test_coverage": [
            "SAMBasicTest.kt - 14 basic SAM tests",
            "SAMGenericClassTest.kt - Generic SAM tests",
            "SAMCollectionsTest.kt - Collection SAM tests",
            "SAMStdlibTypesTest.kt - Kotlin stdlib type tests",
            "SAMHigherOrderTest.kt - Higher-order function tests",
            "SAMVarianceTest.kt - Variance annotation tests",
            "SAMEdgeCasesTest.kt - Edge case scenarios",
            "SAMRemainingTest.kt - Additional coverage (26 tests)",
        ],
        "related_docs": [
            "SAM Interfaces organized in subcategories:",
            "  - basic/ (6 interfaces)",
            "  - generics/ (8 interfaces)",
            "  - collections/ (11 interfaces)",
            "  - stdlib_types/ (10 interfaces)",
            "  - higher_order/ (9 interfaces)",
            "  - variance/ (12 interfaces)",
            "  - edge_cases/ (8 interfaces)",
        ],
    },
}

README_TEMPLATE = """# {title} [{priority}]

## What's Tested

{what_tested}

## Test Coverage

{test_coverage}

## Related Documentation

{related_docs}

---

*Generated for Fakt compiler plugin sample project*
*Package: `com.rsicarelli.fakt.samples.singleModule.scenarios.{folder}`*
"""

def create_readme(category, data, base_path):
    """Create README.md for a category."""
    # Build what's tested section
    what_tested = "\n".join(f"- {item}" for item in data["what_tested"])

    # Build test coverage section
    test_coverage = "\n".join(f"- ✅ {item}" for item in data["test_coverage"])

    # Build related docs section
    related_docs = "\n".join(f"- {item}" for item in data["related_docs"])

    # Format README
    content = README_TEMPLATE.format(
        title=data["title"],
        priority=data["priority"],
        what_tested=what_tested,
        test_coverage=test_coverage,
        related_docs=related_docs,
        folder=category,
    )

    # Write README
    readme_path = base_path / category / "README.md"
    readme_path.parent.mkdir(parents=True, exist_ok=True)

    with open(readme_path, 'w') as f:
        f.write(content)

    print(f"✓ Created README: {category}/README.md")

def main():
    base_path = Path(__file__).parent / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"

    print("Creating README.md files...\n")

    for category, data in READMES.items():
        create_readme(category, data, base_path)

    print(f"\n✅ Created {len(READMES)} README files!")

if __name__ == '__main__':
    main()
