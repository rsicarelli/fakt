#!/usr/bin/env python3
"""
Add model imports to files that reference them.
"""
from pathlib import Path
import re

# Files that need imports
IMPORTS_NEEDED = {
    "AuthenticationService.kt": ["User"],
    "ProductService.kt": ["Product"],
    "UserRepository.kt": ["User"],
    "CompanyService.kt": ["CompanyTestDouble"],
    "EnterpriseRepository.kt": ["CompanyTestDouble"],
}

def add_imports(file_path, models):
    """Add import statements for models."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Find package declaration
    lines = content.split('\n')
    import_insert_pos = None

    for i, line in enumerate(lines):
        if line.startswith('package '):
            # Insert imports after package and any existing imports
            import_insert_pos = i + 1
            # Skip past existing imports
            while import_insert_pos < len(lines) and (
                lines[import_insert_pos].startswith('import ') or
                lines[import_insert_pos].strip() == ''
            ):
                import_insert_pos += 1
            break

    if import_insert_pos is None:
        print(f"⚠ Warning: No package declaration in {file_path.name}")
        return False

    # Build import statements
    imports = [f"import com.rsicarelli.fakt.samples.singleModule.models.{model}" for model in models]

    # Insert imports
    for import_line in reversed(imports):
        # Check if import already exists
        if import_line not in content:
            lines.insert(import_insert_pos, import_line)

    # Write back
    updated_content = '\n'.join(lines)
    with open(file_path, 'w') as f:
        f.write(updated_content)

    return True

def main():
    base_path = Path(__file__).parent
    scenarios_main = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios"

    fixed_count = 0

    for filename, models in IMPORTS_NEEDED.items():
        # Find the file
        for kt_file in scenarios_main.rglob(filename):
            if add_imports(kt_file, models):
                print(f"✓ Added imports to {filename}: {', '.join(models)}")
                fixed_count += 1

    print(f"\n✅ Updated {fixed_count} files!")

if __name__ == '__main__':
    main()
