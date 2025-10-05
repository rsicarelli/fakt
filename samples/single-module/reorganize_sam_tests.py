#!/usr/bin/env python3
"""
Reorganize SAM tests: Transform from 8 consolidated test files to 63 individual test files.
Each SAM interface will have its own dedicated test file.
"""
import os
import re
from pathlib import Path
from typing import Dict, List, Tuple

# Project paths
PROJECT_ROOT = Path(__file__).parent
TEST_DIR = PROJECT_ROOT / "src" / "commonTest" / "kotlin" / "com" / "rsicarelli" / "fakt" / "samples" / "singleModule" / "scenarios" / "7_sam_interfaces"

# Copyright header
COPYRIGHT_HEADER = """// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0"""

# Map nested test class names to (interface_name, category, test_methods)
# Format: "NestedClassName" -> ("InterfaceName", "category")
NESTED_CLASS_TO_INTERFACE = {
    # SAMBasicTest.kt - individual tests (not nested)
    "SAMBasicTest": [
        ("IntValidator", "basic"),
        ("NullableHandler", "basic"),
        ("VoidAction", "basic"),
        ("AsyncValidator", "basic"),
        ("BiFunction", "basic"),
        ("StringFormatter", "basic"),
    ],

    # SAMGenericClassTest.kt - individual tests (not nested, but multiple interfaces tested)
    "SAMGenericClassTest": [
        ("Transformer", "generics"),
        ("Converter", "generics"),
        ("ComparableProcessor", "generics"),
        ("MultiConstraintHandler", "generics"),
        ("NullableTransformer", "generics"),
        ("ListMapper", "generics"),
        ("ResultHandler", "generics"),
        ("AsyncTransformer", "generics"),
    ],

    # SAMCollectionsTest.kt - nested classes
    "ListMapperTests": ("ListMapper", "generics"),  # Note: ListMapper is in generics, tested in collections
    "SetTransformerTests": ("SetTransformer", "collections"),
    "MapProcessorTests": ("MapProcessor", "collections"),
    "ArrayHandlerTests": ("ArrayHandler", "collections"),

    # SAMStdlibTypesTest.kt - nested classes
    "ResultHandlerTests": ("ResultHandler", "generics"),  # ResultHandler is in generics
    "PairMapperTests": ("PairMapper", "stdlib_types"),
    "TripleProcessorTests": ("TripleProcessor", "stdlib_types"),
    "SequenceMapperTests": ("SequenceMapper", "stdlib_types"),
    "ResultFunctionHandlerTests": ("ResultFunctionHandler", "stdlib_types"),

    # SAMHigherOrderTest.kt - nested classes
    "FunctionExecutorTests": ("FunctionExecutor", "higher_order"),
    "PredicateFilterTests": ("PredicateFilter", "higher_order"),
    "TransformChainTests": ("TransformChain", "higher_order"),
    "AsyncProducerTests": ("AsyncProducer", "variance"),  # AsyncProducer is in variance
    "CallbackHandlerTests": ("CallbackHandler", "higher_order"),

    # SAMVarianceTest.kt - nested classes
    "CovariantProducerTests": ("CovariantProducer", "variance"),
    "ContravariantConsumerTests": ("ContravariantConsumer", "variance"),
    "InvariantTransformerTests": ("InvariantTransformer", "variance"),
    "CovariantListProducerTests": ("CovariantListProducer", "variance"),
    "ContravariantListConsumerTests": ("ContravariantListConsumer", "variance"),
    "BivariantMapperTests": ("BivariantMapper", "variance"),

    # SAMEdgeCasesTest.kt - nested classes
    "VarargsProcessorTests": ("VarargsProcessor", "edge_cases"),
    "IntArrayProcessorTests": ("IntArrayProcessor", "edge_cases"),
    "StarProjectionHandlerTests": ("StarProjectionHandler", "edge_cases"),
    "RecursiveGenericTests": ("RecursiveGeneric", "edge_cases"),
    "NestedGenericMapperTests": ("NestedGenericMapper", "edge_cases"),
    "ComplexBoundHandlerTests": ("ComplexBoundHandler", "edge_cases"),
    "NullabilityEdgeCaseTests": ("NullableTransformer", "generics"),  # NullableTransformer in generics

    # SAMRemainingTest.kt - nested classes (26 interfaces)
    "ActionWrapperTests": ("ActionWrapper", "higher_order"),
    "ArrayProcessorTests": ("ArrayProcessor", "edge_cases"),
    "CollectionFilterTests": ("CollectionFilter", "collections"),
    "ConsumerTests": ("Consumer", "variance"),
    "ProducerTests": ("Producer", "variance"),
    "ErrorHandlerTests": ("ErrorHandler", "stdlib_types"),
    "FunctionComposerTests": ("FunctionComposer", "higher_order"),
    "PredicateCombinerTests": ("PredicateCombiner", "higher_order"),
    "VariantTransformerTests": ("VariantTransformer", "variance"),
    "ListConsumerTests": ("ListConsumer", "variance"),
    "ListProcessorTests": ("ListProcessor", "collections"),
    "MapTransformerTests": ("MapTransformer", "collections"),
    "MapWithFunctionTests": ("MapWithFunction", "collections"),
    "IterableProcessorTests": ("IterableProcessor", "collections"),
    "LazyProviderTests": ("LazyProvider", "stdlib_types"),
    "MutableListHandlerTests": ("MutableListHandler", "collections"),
    "NestedCollectionHandlerTests": ("NestedCollectionHandler", "collections"),
    "PairProcessorTests": ("PairProcessor", "stdlib_types"),
    "RecursiveComparatorTests": ("RecursiveComparator", "edge_cases"),
    "SequenceFilterTests": ("SequenceFilter", "stdlib_types"),
    "SetFilterTests": ("SetFilter", "collections"),
    "ResultFunctionMapperTests": ("ResultFunctionMapper", "higher_order"),
    "ResultProcessorTests": ("ResultProcessor", "stdlib_types"),
    "ResultProducerTests": ("ResultProducer", "variance"),
    "SuspendExecutorTests": ("SuspendExecutor", "higher_order"),
    "TripleAggregatorTests": ("TripleAggregator", "stdlib_types"),
}

def extract_test_methods(file_content: str, class_name: str) -> List[str]:
    """Extract all test methods from a specific nested class or main class."""
    methods = []

    if class_name in ["SAMBasicTest", "SAMGenericClassTest"]:
        # Non-nested structure - extract all @Test methods
        pattern = r'(@Test\s+fun\s+`[^`]+`\(\)[^{]*\{(?:[^{}]|\{[^{}]*\})*\})'
        matches = re.findall(pattern, file_content, re.MULTILINE | re.DOTALL)
        return matches

    # Nested class structure - find the nested class first
    nested_pattern = rf'(inner\s+class\s+{class_name}\s*\{{[^{{}}]*(?:\{{[^{{}}]*(?:\{{[^{{}}]*\}})*[^{{}}]*\}})*[^{{}}]*\}})'
    nested_match = re.search(nested_pattern, file_content, re.MULTILINE | re.DOTALL)

    if not nested_match:
        return []

    nested_content = nested_match.group(1)

    # Extract test methods from nested class
    method_pattern = r'(@Test\s+fun\s+`[^`]+`\(\)[^{]*\{(?:[^{}]|\{[^{}]*\})*\})'
    matches = re.findall(method_pattern, nested_content, re.MULTILINE | re.DOTALL)
    return matches

def group_tests_by_interface(file_path: Path, class_mappings: Dict) -> Dict[Tuple[str, str], List[str]]:
    """Group all test methods by (interface_name, category)."""
    content = file_path.read_text()
    grouped_tests = {}

    for class_name, mapping in class_mappings.items():
        if isinstance(mapping, list):
            # SAMBasicTest / SAMGenericClassTest - split tests among multiple interfaces
            all_methods = extract_test_methods(content, class_name)

            # Try to match tests to interfaces by analyzing imports and fake calls
            for interface_name, category in mapping:
                interface_tests = []
                factory_name = f"fake{interface_name}"

                for method in all_methods:
                    # Check if this test mentions the interface's factory
                    if factory_name in method or interface_name in method:
                        interface_tests.append(method)

                if interface_tests:
                    key = (interface_name, category)
                    grouped_tests.setdefault(key, []).extend(interface_tests)
        else:
            # Nested class structure
            interface_name, category = mapping
            methods = extract_test_methods(content, class_name)

            if methods:
                key = (interface_name, category)
                grouped_tests.setdefault(key, []).extend(methods)

    return grouped_tests

def detect_needed_imports(test_methods: List[str]) -> set:
    """Detect which imports are needed based on test content."""
    imports = {
        "kotlin.test.Test",
        "kotlin.test.assertEquals",
    }

    content = "\n".join(test_methods)

    # Check for common imports
    if "assertTrue" in content:
        imports.add("kotlin.test.assertTrue")
    if "assertFalse" in content:
        imports.add("kotlin.test.assertFalse")
    if "assertNull" in content:
        imports.add("kotlin.test.assertNull")
    if "assertContentEquals" in content:
        imports.add("kotlin.test.assertContentEquals")
    if "assertIs" in content:
        imports.add("kotlin.test.assertIs")
    if "runTest" in content:
        imports.add("kotlinx.coroutines.test.runTest")

    return imports

def generate_test_file(interface_name: str, category: str, test_methods: List[str]) -> str:
    """Generate a complete test file for a single SAM interface."""
    factory_name = f"fake{interface_name}"
    package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}"

    # Detect needed imports
    imports = detect_needed_imports(test_methods)

    # Build imports section
    import_lines = [
        f"package {package}",
        "",
        f"import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}.{interface_name}",
        f"import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}.{factory_name}",
    ]

    for imp in sorted(imports):
        import_lines.append(f"import {imp}")

    # Build test class
    test_class = f"""
/**
 * Tests for {interface_name} SAM interface.
 */
class {interface_name}Test {{
"""

    # Add test methods
    for method in test_methods:
        # Indent test method (4 spaces)
        indented = "\n".join("    " + line for line in method.split("\n"))
        test_class += f"\n{indented}\n"

    test_class += "}\n"

    # Combine all parts
    file_content = COPYRIGHT_HEADER + "\n"
    file_content += "\n".join(import_lines)
    file_content += "\n"
    file_content += test_class

    return file_content

def main():
    print("🔧 SAM Test Reorganization")
    print("=" * 60)

    # Track all tests grouped by interface
    all_grouped_tests: Dict[Tuple[str, str], List[str]] = {}

    # Process each consolidated test file
    consolidated_files = {
        "SAMBasicTest.kt": ["SAMBasicTest"],
        "SAMGenericClassTest.kt": ["SAMGenericClassTest"],
        "SAMCollectionsTest.kt": ["ListMapperTests", "SetTransformerTests", "MapProcessorTests", "ArrayHandlerTests"],
        "SAMStdlibTypesTest.kt": ["ResultHandlerTests", "PairMapperTests", "TripleProcessorTests", "SequenceMapperTests", "ResultFunctionHandlerTests"],
        "SAMHigherOrderTest.kt": ["FunctionExecutorTests", "PredicateFilterTests", "TransformChainTests", "AsyncProducerTests", "CallbackHandlerTests"],
        "SAMVarianceTest.kt": ["CovariantProducerTests", "ContravariantConsumerTests", "InvariantTransformerTests", "CovariantListProducerTests", "ContravariantListConsumerTests", "BivariantMapperTests"],
        "SAMEdgeCasesTest.kt": ["VarargsProcessorTests", "IntArrayProcessorTests", "StarProjectionHandlerTests", "RecursiveGenericTests", "NestedGenericMapperTests", "ComplexBoundHandlerTests", "NullabilityEdgeCaseTests"],
        "SAMRemainingTest.kt": [
            "ActionWrapperTests", "ArrayProcessorTests", "CollectionFilterTests", "ConsumerTests", "ProducerTests",
            "ErrorHandlerTests", "FunctionComposerTests", "PredicateCombinerTests", "VariantTransformerTests",
            "ListConsumerTests", "ListProcessorTests", "MapTransformerTests", "MapWithFunctionTests",
            "IterableProcessorTests", "LazyProviderTests", "MutableListHandlerTests", "NestedCollectionHandlerTests",
            "PairProcessorTests", "RecursiveComparatorTests", "SequenceFilterTests", "SetFilterTests",
            "ResultFunctionMapperTests", "ResultProcessorTests", "ResultProducerTests", "SuspendExecutorTests",
            "TripleAggregatorTests"
        ],
    }

    for filename, class_names in consolidated_files.items():
        file_path = TEST_DIR / filename
        if not file_path.exists():
            print(f"⚠️  Skipping {filename} (not found)")
            continue

        print(f"📖 Reading {filename}")
        class_mappings = {name: NESTED_CLASS_TO_INTERFACE[name] for name in class_names if name in NESTED_CLASS_TO_INTERFACE}
        grouped = group_tests_by_interface(file_path, class_mappings)

        for key, tests in grouped.items():
            all_grouped_tests.setdefault(key, []).extend(tests)

    print(f"\n✅ Extracted tests for {len(all_grouped_tests)} interfaces")
    print()

    # Generate individual test files
    created_count = 0
    for (interface_name, category), test_methods in all_grouped_tests.items():
        # Determine output directory
        category_dir = TEST_DIR / category.replace("_", "_")  # Use actual folder name
        category_dir.mkdir(parents=True, exist_ok=True)

        # Generate test file
        test_file_content = generate_test_file(interface_name, category, test_methods)
        test_file_path = category_dir / f"{interface_name}Test.kt"

        test_file_path.write_text(test_file_content)
        print(f"✓ Created {category}/{interface_name}Test.kt ({len(test_methods)} tests)")
        created_count += 1

    print()
    print(f"📊 Summary")
    print(f"  Created: {created_count} test files")
    print(f"  Total tests: {sum(len(tests) for tests in all_grouped_tests.values())}")
    print()
    print("✅ SAM test reorganization complete!")
    print()
    print("Next steps:")
    print("  1. Review generated test files")
    print("  2. Delete consolidated test files (SAMBasicTest.kt, etc.)")
    print("  3. Run: ./gradlew :samples:single-module:allTests")

if __name__ == "__main__":
    main()
