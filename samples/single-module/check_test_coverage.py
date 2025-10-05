#!/usr/bin/env python3
"""
Check test coverage - ensure every interface has a corresponding test.
"""
import os
from pathlib import Path
import re

# Define paths
PROJECT_ROOT = Path(__file__).parent
SRC_DIR = PROJECT_ROOT / "src" / "commonMain" / "kotlin" / "com" / "rsicarelli" / "fakt" / "samples" / "singleModule" / "scenarios"
TEST_DIR = PROJECT_ROOT / "src" / "commonTest" / "kotlin" / "com" / "rsicarelli" / "fakt" / "samples" / "singleModule" / "scenarios"

# Interfaces to exclude (use @CompanyTestDouble, not @Fake)
EXCLUDED_INTERFACES = {"CompanyService", "EnterpriseRepository"}

def get_all_interfaces():
    """Get all interface files from source directory."""
    interfaces = []
    for kt_file in SRC_DIR.rglob("*.kt"):
        interface_name = kt_file.stem
        if interface_name not in EXCLUDED_INTERFACES:
            interfaces.append(interface_name)
    return sorted(interfaces)

def get_tested_interfaces():
    """Get all interfaces referenced in test files."""
    tested = set()

    for test_file in TEST_DIR.rglob("*Test.kt"):
        content = test_file.read_text()

        # Find all fake factory calls: fakeXxx {
        fake_calls = re.findall(r'fake([A-Z][a-zA-Z]*)\s*[{<(]', content)
        tested.update(fake_calls)

        # Also check imports: import ...fakeXxx
        fake_imports = re.findall(r'import.*\.fake([A-Z][a-zA-Z]*)', content)
        tested.update(fake_imports)

    return tested

def main():
    all_interfaces = get_all_interfaces()
    tested_interfaces = get_tested_interfaces()

    # Find missing tests
    missing_tests = []
    for interface in all_interfaces:
        if interface not in tested_interfaces:
            missing_tests.append(interface)

    # Report
    print(f"📊 Test Coverage Analysis")
    print(f"=" * 60)
    print(f"Total interfaces: {len(all_interfaces)}")
    print(f"Tested interfaces: {len(tested_interfaces)}")
    print(f"Missing tests: {len(missing_tests)}")
    print()

    if missing_tests:
        print("❌ Interfaces WITHOUT tests:")
        print("-" * 60)
        for interface in sorted(missing_tests):
            # Find the file path
            for kt_file in SRC_DIR.rglob(f"{interface}.kt"):
                rel_path = kt_file.relative_to(SRC_DIR)
                print(f"  • {interface:<40} ({rel_path.parent})")
        print()
        print(f"⚠️  Need to create {len(missing_tests)} test files!")
        return 1
    else:
        print("✅ All interfaces have test coverage!")
        print()
        print("Test file breakdown:")
        for test_file in sorted(TEST_DIR.rglob("*Test.kt")):
            rel_path = test_file.relative_to(TEST_DIR)
            print(f"  • {rel_path}")
        return 0

if __name__ == "__main__":
    exit(main())
