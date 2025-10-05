#!/usr/bin/env python3
"""
Move SAM test files and update imports to use new package structure.
"""
from pathlib import Path
import re

# SAM test files to move
SAM_TESTS = [
    "SAMBasicTest.kt",
    "SAMCollectionsTest.kt",
    "SAMEdgeCasesTest.kt",
    "SAMGenericClassTest.kt",
    "SAMHigherOrderTest.kt",
    "SAMRemainingTest.kt",
    "SAMStdlibTypesTest.kt",
    "SAMVarianceTest.kt",
]

# Category to SAM interface mapping (for import generation)
# We'll need to detect which SAM interfaces are used and add appropriate imports
SAM_CATEGORIES = {
    "basic": ["IntValidator", "NullableHandler", "VoidAction", "AsyncValidator", "BiFunction", "StringFormatter"],
    "generics": ["Transformer", "Converter", "ComparableProcessor", "MultiConstraintHandler",
                 "NullableTransformer", "ListMapper", "ResultHandler", "AsyncTransformer"],
    "collections": ["ListProcessor", "MapTransformer", "SetFilter", "NestedCollectionHandler",
                    "MutableListHandler", "CollectionFilter", "MapWithFunction", "IterableProcessor",
                    "SetTransformer", "MapProcessor", "ArrayHandler"],
    "stdlib_types": ["ResultProcessor", "SequenceMapper", "PairProcessor", "TripleAggregator",
                     "ErrorHandler", "LazyProvider", "ResultFunctionHandler", "SequenceFilter",
                     "PairMapper", "TripleProcessor"],
    "higher_order": ["FunctionExecutor", "SuspendExecutor", "FunctionComposer", "PredicateCombiner",
                     "ActionWrapper", "ResultFunctionMapper", "PredicateFilter", "TransformChain",
                     "CallbackHandler"],
    "variance": ["Producer", "Consumer", "VariantTransformer", "ResultProducer", "ListConsumer",
                 "AsyncProducer", "CovariantProducer", "ContravariantConsumer", "InvariantTransformer",
                 "CovariantListProducer", "ContravariantListConsumer", "BivariantMapper"],
    "edge_cases": ["VarargsProcessor", "StarProjectionHandler", "RecursiveComparator", "ArrayProcessor",
                   "IntArrayProcessor", "RecursiveGeneric", "NestedGenericMapper", "ComplexBoundHandler"],
}

def update_sam_test_imports(content):
    """Update imports from test.sample to new SAM package structure."""
    # Remove old test.sample import (if it exists)
    content = re.sub(r'^import test\.sample\.\*\s*$', '', content, flags=re.MULTILINE)

    # Find which SAM interfaces are referenced in the file
    used_interfaces = set()
    for category, interfaces in SAM_CATEGORIES.items():
        for interface in interfaces:
            # Check if interface is used (look for fake{Interface} pattern)
            if f"fake{interface}" in content or f"Fake{interface}" in content or interface in content:
                used_interfaces.add((category, interface))

    # Generate import statements
    imports_by_category = {}
    for category, interface in used_interfaces:
        if category not in imports_by_category:
            imports_by_category[category] = []
        imports_by_category[category].append(interface)

    # Build import block
    import_lines = []
    for category in sorted(imports_by_category.keys()):
        interfaces = sorted(imports_by_category[category])
        pkg = f"com.rsicarelli.fakt.samples.singleModule.scenarios.7_sam_interfaces.{category}"
        for interface in interfaces:
            import_lines.append(f"import {pkg}.{interface}")
            # Also import the fake function (convention: fake{Interface})
            import_lines.append(f"import {pkg}.fake{interface}")

    # Find position to insert imports (after package declaration)
    lines = content.split('\n')
    insert_pos = 0

    for i, line in enumerate(lines):
        if line.startswith('package '):
            # Insert after package and blank line
            insert_pos = i + 2
            break

    # Insert import block
    if import_lines and insert_pos > 0:
        # Ensure there's a blank line after package
        if insert_pos < len(lines) and lines[insert_pos - 1].strip() != '':
            lines.insert(insert_pos, '')
            insert_pos += 1

        # Insert imports
        for import_line in reversed(import_lines):
            lines.insert(insert_pos, import_line)

        content = '\n'.join(lines)

    return content

def move_sam_test(src_path, dest_dir):
    """Move SAM test file and update package + imports."""
    # Read file
    with open(src_path, 'r') as f:
        content = f.read()

    # Update package
    new_package = "com.rsicarelli.fakt.samples.singleModule.scenarios.7_sam_interfaces"
    content = re.sub(r'^package\s+[\w.]+\s*$', f'package {new_package}', content, count=1, flags=re.MULTILINE)

    # Update imports
    content = update_sam_test_imports(content)

    # Write to new location
    dest_file = dest_dir / src_path.name
    dest_file.parent.mkdir(parents=True, exist_ok=True)

    with open(dest_file, 'w') as f:
        f.write(content)

    print(f"✓ Moved SAM test: {src_path.name}")

def main():
    base_path = Path(__file__).parent
    src_test = base_path / "src/commonTest/kotlin"
    dest_test = base_path / "src/commonTest/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios/7_sam_interfaces"

    print("Moving SAM test files...\n")

    moved_count = 0
    for test_file in SAM_TESTS:
        src_file = src_test / test_file

        if not src_file.exists():
            print(f"⚠ Warning: {test_file} not found, skipping")
            continue

        move_sam_test(src_file, dest_test)
        moved_count += 1

    print(f"\n✅ Moved {moved_count} SAM test files successfully!")

if __name__ == '__main__':
    main()
