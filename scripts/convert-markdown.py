#!/usr/bin/env python3
"""Convert markdown files to Dokka-compliant HTML with proper theme support."""

import sys
import subprocess
from pathlib import Path

# Auto-install markdown if needed
try:
    import markdown
    from markdown.extensions.codehilite import CodeHiliteExtension
    from markdown.extensions.fenced_code import FencedCodeExtension
    from markdown.extensions.tables import TableExtension
    from markdown.extensions.toc import TocExtension
except ImportError:
    print("📦 Installing required packages...")
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "markdown", "pygments"])
    import markdown
    from markdown.extensions.codehilite import CodeHiliteExtension
    from markdown.extensions.fenced_code import FencedCodeExtension
    from markdown.extensions.tables import TableExtension
    from markdown.extensions.toc import TocExtension

# Dokka-compliant HTML template with theme support
DOKKA_TEMPLATE = """<!doctype html>
<html class="no-js" lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <title>{title}</title>
    <link href="../images/logo-icon.svg" rel="icon" type="image/svg">
    <script>var pathToRoot = "../";</script>
    <script>document.documentElement.classList.replace("no-js", "js");</script>
    <script>const storage = localStorage.getItem("dokka-dark-mode")
      if (storage == null) {{
        const osDarkSchemePreferred = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
        if (osDarkSchemePreferred === true) {{
          document.getElementsByTagName("html")[0].classList.add("theme-dark")
        }}
      }} else {{
        const savedDarkMode = JSON.parse(storage)
        if (savedDarkMode === true) {{
          document.getElementsByTagName("html")[0].classList.add("theme-dark")
        }}
      }}
    </script>
    <script type="text/javascript" src="../scripts/sourceset_dependencies.js" async></script>
    <link href="../styles/style.css" rel="Stylesheet">
    <link href="../styles/main.css" rel="Stylesheet">
    <link href="../styles/prism.css" rel="Stylesheet">
    <link href="../styles/logo-styles.css" rel="Stylesheet">
    <link href="../ui-kit/ui-kit.min.css" rel="Stylesheet">
    <link href="../styles/docs-custom.css" rel="Stylesheet">
    <script type="text/javascript" src="../scripts/safe-local-storage_blocking.js"></script>
    <script type="text/javascript" src="../scripts/navigation-loader.js" async></script>
    <script type="text/javascript" src="../scripts/platform-content-handler.js" async></script>
    <script type="text/javascript" src="../scripts/main.js" defer></script>
    <script type="text/javascript" src="../scripts/prism.js" async></script>
    <script type="text/javascript" src="../ui-kit/ui-kit.min.js" defer></script>
</head>
<body>
<div class="root">
    <header class="navigation theme-dark" id="navigation-wrapper" role="banner">
        <a class="library-name--link" href="../index.html" tabindex="1">
            Fakt
        </a>
        <button class="navigation-controls--btn navigation-controls--btn_toc ui-kit_mobile-only" id="toc-toggle" type="button">Toggle table of contents</button>
        <div class="navigation-controls--break ui-kit_mobile-only"></div>
        <div class="library-version" id="library-version"></div>
        <div class="navigation-controls">
            <button class="navigation-controls--btn navigation-controls--btn_theme" id="theme-toggle-button" type="button">Switch theme</button>
            <div class="navigation-controls--btn navigation-controls--btn_search" id="searchBar" role="button">Search in API</div>
        </div>
    </header>
    <div id="container">
        <nav id="leftColumn" class="sidebar" data-item-type="SECTION" data-item-config='{{"defaultSize": 280, "minSize": 200, "maxSize": 400}}'>
            <a class="toc--skip-link" href="#main">Skip to content</a>
            <div class="dropdown theme-dark_mobile" data-role="dropdown" id="toc-dropdown">
                <ul role="listbox" id="toc-listbox" class="dropdown--list dropdown--list_toc-list" data-role="dropdown-listbox" aria-label="Table of contents">
                    <div class="dropdown--header">
                        <span>Fakt</span>
                        <button class="button" data-role="dropdown-toggle" aria-label="Close table of contents">
                            <i class="ui-kit-icon ui-kit-icon_cross"></i>
                        </button>
                    </div>
                    <div class="sidebar--inner" id="sideMenu"></div>
                </ul>
                <div class="dropdown--overlay"></div>
            </div>
        </nav>
        <div id="resizer" class="resizer" data-item-type="BAR"></div>
        <div id="main" data-item-type="SECTION" role="main">
            <div class="main-content" id="content" pageIds="{page_id}">
                {breadcrumbs}
                <div class="cover">
                    {content}
                </div>
            </div>
            <div class="footer">
                <div class="footer--container">
                    <a href="#content" id="go-to-top-link" class="footer--button footer--button_go-to-top"></a>
                    <div class="footer--content">
                        <div>
                            <span>Generated by </span>
                            <a class="footer--link footer--link_external" href="https://github.com/Kotlin/dokka">Dokka</a>
                            <div>© 2025 Copyright</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
"""


# Category mapping (sync with DokkaConvention.kt)
CATEGORY_MAPPING = {
    "Getting Started": ["README"],
    "Architecture": ["ARCHITECTURE", "METRO_FIR_IR_SPECIFICATIONS", "IR_NATIVE_DEMO", "IR_NATIVE_DEMONSTRATION"],
    "Specifications": ["API_SPECIFICATIONS", "COMPILE_TIME_GENERIC_SOLUTIONS", "CODE_GENERATION_STRATEGIES"],
    "Testing": ["TESTING_GUIDELINES", "TESTING_STATUS_REPORT", "TEST_COVERAGE_ANALYSIS"],
    "Implementation": [
        "CURRENT_STATUS", "IMPLEMENTATION_ROADMAP", "IMPLEMENTATION_DECISION",
        "GENERIC_IMPLEMENTATION_PROGRESS", "GENERIC_TYPE_SCOPING_ANALYSIS",
        "KOTLIN_COMPILER_IR_API_GUIDE", "FINAL_COMPILE_TIME_SOLUTION", "COMPILE_TIME_EXAMPLES"
    ]
}

CATEGORY_INDEX_FILES = {
    "Getting Started": "getting-started",
    "Architecture": "architecture",
    "Specifications": "specifications",
    "Testing": "testing",
    "Implementation": "implementation"
}


def get_category_for_file(filename: str) -> tuple[str | None, str | None]:
    """
    Get category name and index file for a given filename.
    Returns (category_name, category_index_file) or (None, None) if not found.
    """
    # Check if this is a category index file
    for category_name, index_file in CATEGORY_INDEX_FILES.items():
        if filename == index_file:
            return category_name, index_file

    # Check if this is a regular doc file
    for category_name, files in CATEGORY_MAPPING.items():
        if filename in files:
            index_file = CATEGORY_INDEX_FILES[category_name]
            return category_name, index_file

    return None, None


def generate_breadcrumbs(filename: str, title: str) -> str:
    """Generate breadcrumbs HTML based on file category."""
    category_name, category_index_file = get_category_for_file(filename)

    base = '<div class="breadcrumbs"><a href="../index.html">Fakt</a><span class="delimiter">/</span>'

    if category_name is None:
        # Fallback for uncategorized files
        return f'{base}<span class="current">Documentation</span></div>'

    # Check if this IS the category index file
    if filename == category_index_file:
        # Breadcrumbs: Fakt > Documentation > Category
        return f'{base}<a href="getting-started.html">Documentation</a><span class="delimiter">/</span><span class="current">{category_name}</span></div>'

    # Regular doc file
    # Breadcrumbs: Fakt > Documentation > Category > File
    return f'{base}<a href="getting-started.html">Documentation</a><span class="delimiter">/</span><a href="{category_index_file}.html">{category_name}</a><span class="delimiter">/</span><span class="current">{title}</span></div>'


def extract_h1_title(md_content: str, fallback_filename: str) -> str:
    """Extract H1 title from markdown content."""
    lines = md_content.split('\n')
    for line in lines:
        line = line.strip()
        if line.startswith('# '):
            return line[2:].strip()
    # Fallback to filename if no H1 found
    return fallback_filename.replace('_', ' ').replace('-', ' ').title()


def convert_markdown_to_html(md_file: Path, output_dir: Path):
    """Convert a markdown file to Dokka-compliant HTML."""
    with open(md_file, 'r', encoding='utf-8') as f:
        md_content = f.read()

    # Convert markdown to HTML
    html_content = markdown.markdown(
        md_content,
        extensions=[
            'fenced_code',
            'codehilite',
            'tables',
            'toc',
            'nl2br',
        ],
        extension_configs={
            'codehilite': {
                'css_class': 'highlight',
                'linenums': False,
            }
        }
    )

    # Extract title from H1 in markdown content
    title = extract_h1_title(md_content, md_file.stem)

    # Generate page ID for navigation matching
    page_id = f"docs/{md_file.stem}"

    # Generate breadcrumbs - simplified (no categories)
    breadcrumbs = f'<div class="breadcrumbs"><a href="../index.html">Fakt</a><span class="delimiter">/</span><a href="README.html">Documentation</a><span class="delimiter">/</span><span class="current">{title}</span></div>'

    # Generate final HTML using Dokka template
    final_html = DOKKA_TEMPLATE.format(
        title=title,
        page_id=page_id,
        breadcrumbs=breadcrumbs,
        content=html_content
    )

    # Write output file
    output_file = output_dir / f"{md_file.stem}.html"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(final_html)

    print(f"✅ Converted: {md_file.name} → {output_file.name}")


def main():
    if len(sys.argv) != 3:
        print("Usage: python convert-markdown.py <input_dir> <output_dir>")
        sys.exit(1)

    input_dir = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])

    if not input_dir.exists():
        print(f"❌ Input directory not found: {input_dir}")
        sys.exit(1)

    output_dir.mkdir(parents=True, exist_ok=True)

    # Find all markdown files (excluding index.html if exists)
    md_files = sorted([
        f for f in input_dir.glob("*.md")
        if f.name != "index.html"
    ])

    if not md_files:
        print(f"⚠️  No markdown files found in {input_dir}")
        return

    print(f"Converting {len(md_files)} markdown files...")

    for md_file in md_files:
        try:
            convert_markdown_to_html(md_file, output_dir)
        except Exception as e:
            print(f"❌ Error converting {md_file.name}: {e}")

    print(f"\n✅ Converted {len(md_files)} files to HTML")


if __name__ == "__main__":
    main()
