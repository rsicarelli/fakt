#!/usr/bin/env python3
"""
Fix package names to remove leading digits (invalid in Kotlin).
Folder: 1_basic → Package: scenarios.basic
"""
from pathlib import Path
import re

# Mapping of folder names to valid package names
FOLDER_TO_PACKAGE = {
    "1_basic": "basic",
    "2_properties_and_methods": "properties_and_methods",
    "3_generics_basic": "generics_basic",
    "4_generics_multiple": "generics_multiple",
    "5_generics_constraints": "generics_constraints",
    "6_generics_method_level": "generics_method_level",
    "7_sam_interfaces": "sam_interfaces",
}

def fix_package_declaration(file_path, folder_name):
    """Fix package declaration in a Kotlin file."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Get valid package name
    package_segment = FOLDER_TO_PACKAGE.get(folder_name, folder_name)

    # Build correct package based on file location
    if "7_sam_interfaces" in str(file_path) or "sam_interfaces" in str(file_path):
        # SAM interface file - has subcategory
        parts = file_path.parts
        sam_idx = parts.index("7_sam_interfaces")
        if sam_idx + 1 < len(parts) and parts[sam_idx + 1] != file_path.name:
            # Has subcategory
            subcategory = parts[sam_idx + 1]
            new_package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.{subcategory}"
        else:
            new_package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces"
    elif "models" in str(file_path):
        new_package = "com.rsicarelli.fakt.samples.singleModule.models"
    else:
        # Regular scenario folder
        new_package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.{package_segment}"

    # Replace package declaration
    updated = re.sub(
        r'^package\s+[\w.]+\s*$',
        f'package {new_package}',
        content,
        count=1,
        flags=re.MULTILINE
    )

    # Write back
    with open(file_path, 'w') as f:
        f.write(updated)

    return new_package

def main():
    base_path = Path(__file__).parent

    # Fix main sources
    scenarios_main = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"
    # Fix test sources
    scenarios_test = base_path / "src/commonTest/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"

    fixed_count = 0

    for scenarios_dir in [scenarios_main, scenarios_test]:
        if not scenarios_dir.exists():
            continue

        for kt_file in scenarios_dir.rglob("*.kt"):
            # Determine folder name
            relative = kt_file.relative_to(scenarios_dir)
            folder_name = relative.parts[0] if relative.parts else None

            if folder_name:
                new_pkg = fix_package_declaration(kt_file, folder_name)
                print(f"✓ Fixed: {kt_file.name} → {new_pkg}")
                fixed_count += 1

    # Fix models
    models_dir = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/models"
    if models_dir.exists():
        for kt_file in models_dir.rglob("*.kt"):
            new_pkg = fix_package_declaration(kt_file, "models")
            print(f"✓ Fixed: {kt_file.name} → {new_pkg}")
            fixed_count += 1

    print(f"\n✅ Fixed {fixed_count} files!")

if __name__ == '__main__':
    main()
