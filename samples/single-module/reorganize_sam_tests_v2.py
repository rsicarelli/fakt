#!/usr/bin/env python3
"""
Reorganize SAM tests: Transform from 8 consolidated test files to 63 individual test files.
Version 2: Improved test extraction with manual mapping.
"""
import re
from pathlib import Path
from typing import Dict, List, Set

# Project paths
PROJECT_ROOT = Path(__file__).parent
TEST_DIR = PROJECT_ROOT / "src" / "commonTest" / "kotlin" / "com" / "rsicarelli" / "fakt" / "samples" / "singleModule" / "scenarios" / "7_sam_interfaces"

# Copyright header
COPYRIGHT_HEADER = """// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0"""

def extract_all_test_methods(content: str) -> List[Dict]:
    """Extract all test methods with metadata."""
    tests = []

    # Pattern to match @Test functions
    pattern = r'@Test\s+fun\s+`([^`]+)`\(\)([^{]*)\{((?:[^{}]|\{(?:[^{}]|\{[^{}]*\})*\})*)\}'

    for match in re.finditer(pattern, content, re.MULTILINE | re.DOTALL):
        test_name = match.group(1)
        signature = match.group(2).strip()
        body = match.group(3)

        # Full test method
        full_method = f"    @Test\n    fun `{test_name}`(){signature} {{\n{body}}}"

        tests.append({
            "name": test_name,
            "signature": signature,
            "body": body,
            "full": full_method,
        })

    return tests

def identify_interface(test_body: str) -> str:
    """Identify which SAM interface a test belongs to by analyzing fake factory calls."""
    # Look for fake{Interface} pattern
    fake_pattern = r'fake([A-Z][a-zA-Z]+)\s*[<{(]'
    matches = re.findall(fake_pattern, test_body)

    if matches:
        return matches[0]

    return None

def detect_needed_imports(test_methods: List[str]) -> Set[str]:
    """Detect which imports are needed based on test content."""
    imports = {
        "kotlin.test.Test",
        "kotlin.test.assertEquals",
    }

    content = "\n".join(test_methods)

    # Check for common assertions
    if "assertTrue" in content:
        imports.add("kotlin.test.assertTrue")
    if "assertFalse" in content:
        imports.add("kotlin.test.assertFalse")
    if "assertNull" in content:
        imports.add("kotlin.test.assertNull")
    if "assertContentEquals" in content:
        imports.add("kotlin.test.assertContentEquals")
    if "assertIs<" in content:
        imports.add("kotlin.test.assertIs")
    if "runTest" in content or "= runTest {" in content:
        imports.add("kotlinx.coroutines.test.runTest")

    return imports

def get_category_for_interface(interface_name: str) -> str:
    """Map interface name to category based on where it should be located."""
    # Manual mapping based on source file locations
    category_map = {
        # basic/
        "IntValidator": "basic",
        "StringFormatter": "basic",
        "VoidAction": "basic",
        "AsyncValidator": "basic",
        "BiFunction": "basic",
        "NullableHandler": "basic",

        # generics/
        "Transformer": "generics",
        "Converter": "generics",
        "ComparableProcessor": "generics",
        "MultiConstraintHandler": "generics",
        "NullableTransformer": "generics",
        "ListMapper": "generics",
        "ResultHandler": "generics",
        "AsyncTransformer": "generics",

        # collections/
        "ArrayHandler": "collections",
        "CollectionFilter": "collections",
        "IterableProcessor": "collections",
        "ListProcessor": "collections",
        "MapProcessor": "collections",
        "MapTransformer": "collections",
        "MapWithFunction": "collections",
        "MutableListHandler": "collections",
        "NestedCollectionHandler": "collections",
        "SetFilter": "collections",
        "SetTransformer": "collections",

        # stdlib_types/
        "ErrorHandler": "stdlib_types",
        "LazyProvider": "stdlib_types",
        "PairMapper": "stdlib_types",
        "PairProcessor": "stdlib_types",
        "ResultFunctionHandler": "stdlib_types",
        "ResultProcessor": "stdlib_types",
        "SequenceFilter": "stdlib_types",
        "SequenceMapper": "stdlib_types",
        "TripleAggregator": "stdlib_types",
        "TripleProcessor": "stdlib_types",

        # higher_order/
        "ActionWrapper": "higher_order",
        "CallbackHandler": "higher_order",
        "FunctionComposer": "higher_order",
        "FunctionExecutor": "higher_order",
        "PredicateCombiner": "higher_order",
        "PredicateFilter": "higher_order",
        "ResultFunctionMapper": "higher_order",
        "SuspendExecutor": "higher_order",
        "TransformChain": "higher_order",

        # variance/
        "AsyncProducer": "variance",
        "BivariantMapper": "variance",
        "Consumer": "variance",
        "ContravariantConsumer": "variance",
        "ContravariantListConsumer": "variance",
        "CovariantListProducer": "variance",
        "CovariantProducer": "variance",
        "InvariantTransformer": "variance",
        "ListConsumer": "variance",
        "Producer": "variance",
        "ResultProducer": "variance",
        "VariantTransformer": "variance",

        # edge_cases/
        "ArrayProcessor": "edge_cases",
        "ComplexBoundHandler": "edge_cases",
        "IntArrayProcessor": "edge_cases",
        "NestedGenericMapper": "edge_cases",
        "RecursiveComparator": "edge_cases",
        "RecursiveGeneric": "edge_cases",
        "StarProjectionHandler": "edge_cases",
        "VarargsProcessor": "edge_cases",
    }

    return category_map.get(interface_name, "unknown")

def generate_test_file(interface_name: str, category: str, test_methods: List[str]) -> str:
    """Generate a complete test file for a single SAM interface."""
    factory_name = f"fake{interface_name}"
    package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}"

    # Detect needed imports
    imports = detect_needed_imports(test_methods)

    # Build file content
    lines = [
        COPYRIGHT_HEADER,
        f"package {package}",
        "",
        f"import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}.{interface_name}",
        f"import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{category}.{factory_name}",
    ]

    # Add detected imports
    for imp in sorted(imports):
        lines.append(f"import {imp}")

    lines.extend([
        "",
        "/**",
        f" * Tests for {interface_name} SAM interface.",
        " */",
        f"class {interface_name}Test {{",
        ""
    ])

    # Add test methods
    for method in test_methods:
        lines.append(method)
        lines.append("")

    lines.append("}")

    return "\n".join(lines)

def main():
    print("🔧 SAM Test Reorganization (v2)")
    print("=" * 60)

    # Process all consolidated test files
    consolidated_files = [
        "SAMBasicTest.kt",
        "SAMGenericClassTest.kt",
        "SAMCollectionsTest.kt",
        "SAMStdlibTypesTest.kt",
        "SAMHigherOrderTest.kt",
        "SAMVarianceTest.kt",
        "SAMEdgeCasesTest.kt",
        "SAMRemainingTest.kt",
    ]

    # Collect all tests grouped by interface
    tests_by_interface: Dict[str, List[str]] = {}

    for filename in consolidated_files:
        file_path = TEST_DIR / filename
        if not file_path.exists():
            print(f"⚠️  Skipping {filename} (not found)")
            continue

        print(f"📖 Reading {filename}")
        content = file_path.read_text()

        # Extract all test methods
        test_methods = extract_all_test_methods(content)

        for test in test_methods:
            # Identify which interface this test belongs to
            interface_name = identify_interface(test["body"])

            if interface_name:
                tests_by_interface.setdefault(interface_name, []).append(test["full"])
            else:
                print(f"  ⚠️  Could not identify interface for test: {test['name'][:60]}...")

    print(f"\n✅ Extracted tests for {len(tests_by_interface)} interfaces")
    print()

    # Generate individual test files
    created_count = 0
    for interface_name, test_methods in sorted(tests_by_interface.items()):
        category = get_category_for_interface(interface_name)

        if category == "unknown":
            print(f"⚠️  Unknown category for {interface_name}, skipping")
            continue

        # Create category directory
        category_dir = TEST_DIR / category
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
    print(f"  Total tests: {sum(len(tests) for tests in tests_by_interface.values())}")
    print()
    print("✅ SAM test reorganization complete!")
    print()
    print("Next steps:")
    print("  1. Review generated test files in each category folder")
    print("  2. Delete consolidated test files")
    print("  3. Run: ./gradlew :samples:single-module:allTests")

if __name__ == "__main__":
    main()
