#!/usr/bin/env python3
"""
Move regular interfaces and their tests to organized package structure.
Updates package declarations automatically.
"""
from pathlib import Path
import shutil
import re

# File mapping: source -> (category, has_test)
FILE_MAPPING = {
    # 1_basic (P0)
    "AnalyticsService.kt": ("1_basic", True),  # Contains both AnalyticsService and AnalyticsServiceExtended
    "AuthenticationService.kt": ("1_basic", True),
    "ProductService.kt": ("1_basic", True),
    "UserRepository.kt": ("1_basic", True),
    "CompanyService.kt": ("1_basic", False),
    "ComplexApiService.kt": ("1_basic", True),

    # 2_properties_and_methods (P0)
    "PropertyAndMethodInterface.kt": ("2_properties_and_methods", True),
    "AsyncDataService.kt": ("2_properties_and_methods", True),
    "EventProcessor.kt": ("2_properties_and_methods", True),
    "CollectionService.kt": ("2_properties_and_methods", True),
    "ResultService.kt": ("2_properties_and_methods", True),

    # 3_generics_basic (P0)
    "SimpleRepository.kt": ("3_generics_basic", True),
    "DataCache.kt": ("3_generics_basic", True),
    "GenericEventProcessor.kt": ("3_generics_basic", True),
    "WorkflowManager.kt": ("3_generics_basic", True),
    "EnterpriseRepository.kt": ("3_generics_basic", True),

    # 4_generics_multiple (P1)
    "KeyValueStore.kt": ("4_generics_multiple", True),
    "CacheService.kt": ("4_generics_multiple", True),
    "TripleStore.kt": ("4_generics_multiple", True),

    # 5_generics_constraints (P1)
    "SortedRepository.kt": ("5_generics_constraints", True),

    # 6_generics_method_level (P2)
    "DataProcessor.kt": ("6_generics_method_level", True),
    "MixedProcessor.kt": ("6_generics_method_level", True),
    "GenericRepository.kt": ("6_generics_method_level", True),

    # models
    "Product.kt": ("models", False),
}

# Test mapping (special cases)
TEST_MAPPING = {
    "AnalyticsServiceExtendedTest.kt": ("1_basic", "AnalyticsServiceTest.kt"),  # Combined file
}

COPYRIGHT = """// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0"""

def update_package_declaration(content, new_package):
    """Update package declaration in file content."""
    # Replace existing package declaration
    pattern = r'^package\s+[\w.]+\s*$'
    replacement = f'package {new_package}'
    updated = re.sub(pattern, replacement, content, count=1, flags=re.MULTILINE)

    # If no package declaration exists, add it after copyright
    if updated == content:
        lines = content.split('\n')
        insert_pos = 0

        # Find position after copyright/license
        for i, line in enumerate(lines):
            if line.startswith('//') or line.strip() == '':
                insert_pos = i + 1
            else:
                break

        lines.insert(insert_pos, f'package {new_package}\n')
        updated = '\n'.join(lines)

    return updated

def move_file(src_path, dest_dir, category, is_test=False):
    """Move file and update package declaration."""
    # Read file
    with open(src_path, 'r') as f:
        content = f.read()

    # Determine new package
    base_pkg = "com.rsicarelli.fakt.samples.singleModule"
    if category == "models":
        new_package = f"{base_pkg}.{category}"
    else:
        new_package = f"{base_pkg}.scenarios.{category}"

    # Update package
    updated_content = update_package_declaration(content, new_package)

    # Write to new location
    dest_file = dest_dir / src_path.name
    dest_file.parent.mkdir(parents=True, exist_ok=True)

    with open(dest_file, 'w') as f:
        f.write(updated_content)

    file_type = "test" if is_test else "interface"
    print(f"✓ Moved {file_type}: {src_path.name} → {category}/")

def main():
    base_path = Path(__file__).parent
    src_main = base_path / "src/commonMain/kotlin"
    src_test = base_path / "src/commonTest/kotlin"

    new_main_base = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"
    new_test_base = base_path / "src/commonTest/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"
    new_models = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/models"

    print("Moving interfaces and tests...\n")

    moved_count = 0

    # Move interface files
    for filename, (category, has_test) in FILE_MAPPING.items():
        src_file = src_main / filename

        if not src_file.exists():
            print(f"⚠ Warning: {filename} not found, skipping")
            continue

        # Determine destination
        if category == "models":
            dest_dir = new_models
        else:
            dest_dir = new_main_base / category

        # Move interface file
        move_file(src_file, dest_dir, category, is_test=False)
        moved_count += 1

        # Move corresponding test file
        if has_test:
            test_filename = filename.replace(".kt", "Test.kt")
            test_file = src_test / test_filename

            if test_file.exists():
                test_dest_dir = new_test_base / category
                move_file(test_file, test_dest_dir, category, is_test=True)
                moved_count += 1
            else:
                print(f"  ⚠ Warning: Test not found: {test_filename}")

    # Move special case tests
    for test_file, (category, _) in TEST_MAPPING.items():
        src_file = src_test / test_file
        if src_file.exists():
            test_dest_dir = new_test_base / category
            move_file(src_file, test_dest_dir, category, is_test=True)
            moved_count += 1

    print(f"\n✅ Moved {moved_count} files successfully!")

    # Summary by category
    print(f"\n📊 Files by category:")
    for category in sorted(set(cat for cat, _ in FILE_MAPPING.values())):
        interface_count = sum(1 for _, (cat, _) in FILE_MAPPING.items() if cat == category)
        test_count = sum(1 for _, (cat, has_test) in FILE_MAPPING.items() if cat == category and has_test)
        print(f"  {category}: {interface_count} interfaces, {test_count} tests")

if __name__ == '__main__':
    main()
