#!/usr/bin/env python3
"""
Split SAMInterfaces.kt into individual files organized by category.
"""
import re
from pathlib import Path

# Phase to directory mapping
PHASE_MAPPING = {
    "Phase 1": "basic",
    "Phase 2": "generics",
    "Phase 4": "collections",
    "Phase 5": "stdlib_types",
    "Phase 6": "higher_order",
    "Phase 7": "variance",
    "Phase 8": "edge_cases",
}

# Copyright header
COPYRIGHT = """// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
"""

def extract_sam_interfaces(file_path):
    """Parse SAMInterfaces.kt and extract individual SAM interfaces."""
    with open(file_path, 'r') as f:
        content = f.read()

    interfaces = []
    current_phase = None
    current_category = None
    lines = content.split('\n')

    i = 0
    while i < len(lines):
        line = lines[i]

        # Detect phase header
        if line.startswith('// Phase'):
            match = re.match(r'// (Phase \d+):', line)
            if match:
                phase = match.group(1)
                current_phase = phase
                current_category = PHASE_MAPPING.get(phase)

        # Detect SAM interface start (comment before @Fake)
        if line.strip().startswith('/**') and current_category:
            # Extract comment block
            comment_lines = []
            j = i
            while j < len(lines) and not lines[j].strip().startswith('@Fake'):
                comment_lines.append(lines[j])
                j += 1

            # Check if next line is @Fake
            if j < len(lines) and lines[j].strip() == '@Fake':
                # Extract interface definition
                k = j + 1
                while k < len(lines) and not lines[k].strip().startswith('fun interface'):
                    k += 1

                if k < len(lines):
                    # Found fun interface line
                    interface_line = lines[k]
                    match = re.match(r'\s*fun interface (\w+)', interface_line)
                    if match:
                        interface_name = match.group(1)

                        # Extract full interface body
                        interface_lines = [lines[k]]
                        k += 1
                        brace_count = 1
                        while k < len(lines) and brace_count > 0:
                            interface_lines.append(lines[k])
                            brace_count += lines[k].count('{') - lines[k].count('}')
                            k += 1

                        interfaces.append({
                            'name': interface_name,
                            'category': current_category,
                            'comment': '\n'.join(comment_lines),
                            'interface': '\n'.join(interface_lines)
                        })

                        i = k - 1

        i += 1

    return interfaces

def create_interface_file(interface_data, base_path):
    """Create individual file for SAM interface."""
    category = interface_data['category']
    name = interface_data['name']

    # Build package path
    package = f"com.rsicarelli.fakt.samples.singleModule.scenarios.7_sam_interfaces.{category}"

    # Build file path
    file_path = base_path / "src/commonMain/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios/7_sam_interfaces" / category / f"{name}.kt"

    # Build file content
    content = f"""{COPYRIGHT}package {package}

import com.rsicarelli.fakt.Fake
import kotlin.Result

{interface_data['comment']}
@Fake
{interface_data['interface']}
"""

    # Write file
    file_path.parent.mkdir(parents=True, exist_ok=True)
    with open(file_path, 'w') as f:
        f.write(content)

    print(f"✓ Created {category}/{name}.kt")

def main():
    base_path = Path(__file__).parent
    sam_interfaces_file = base_path / "src/commonMain/kotlin/SAMInterfaces.kt"

    print("Parsing SAMInterfaces.kt...")
    interfaces = extract_sam_interfaces(sam_interfaces_file)

    print(f"Found {len(interfaces)} SAM interfaces")
    print(f"\nCreating individual files...")

    # Group by category for summary
    by_category = {}
    for interface in interfaces:
        cat = interface['category']
        if cat not in by_category:
            by_category[cat] = []
        by_category[cat].append(interface['name'])
        create_interface_file(interface, base_path)

    print(f"\n📊 Summary:")
    for category, names in sorted(by_category.items()):
        print(f"  {category}: {len(names)} interfaces")

    print(f"\n✅ Split complete! Created {len(interfaces)} files")

if __name__ == '__main__':
    main()
