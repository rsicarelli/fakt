// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension
import java.io.File

/**
 * Dokka documentation convention.
 *
 * Configures Dokka 2.x for aggregated multi-module documentation:
 * - Root project aggregates documentation from runtime, compiler, and gradle-plugin
 * - Single unified HTML output at build/dokka/html/
 * - Proper navigation between modules
 * - Includes custom markdown documentation from docs/
 *
 * Usage: `./gradlew dokkaGenerate`
 */
fun Project.applyDokkaConvention() {
    // Root project configuration
    configure<DokkaExtension> {
        moduleName.set("Fakt")

        // Include README.md in root documentation
        dokkaSourceSets.configureEach {
            includes.from("docs/README.md")
        }
    }

    // Apply dokka plugin to subprojects that should be documented
    val documentedModules = listOf("runtime", "compiler", "gradle-plugin")

    subprojects {
        if (name in documentedModules) {
            pluginManager.apply("org.jetbrains.dokka")

            configure<DokkaExtension> {
                moduleName.set(name.replaceFirstChar { it.uppercase() })
            }
        }
    }

    // Copy docs/ directory and convert markdown to HTML
    tasks.register<Copy>("copyDocsToOutput") {
        from("docs")
        into(layout.buildDirectory.dir("dokka/html/docs"))
        dependsOn("dokkaGenerate")
    }

    tasks.register<Exec>("convertMarkdownDocs") {
        dependsOn("copyDocsToOutput")
        workingDir(projectDir)
        commandLine(
            "python3",
            "scripts/convert-markdown.py",
            "docs",
            layout.buildDirectory.dir("dokka/html/docs").get().asFile.absolutePath,
        )
        doFirst {
            println("🔄 Converting markdown documentation to HTML...")
        }
    }

    // Inject docs into navigation sidebar
    tasks.register("injectDocsIntoNavigation") {
        dependsOn("convertMarkdownDocs")
        notCompatibleWithConfigurationCache("Modifies generated HTML files")
        doLast {
            val docsDir = layout.buildDirectory.dir("dokka/html/docs").get().asFile
            val navFile = layout.buildDirectory.file("dokka/html/navigation.html").get().asFile
            val pagesFile = layout.buildDirectory.file("dokka/html/scripts/pages.json").get().asFile

            if (!navFile.exists() || !pagesFile.exists()) {
                println("⚠️  Navigation files not found, skipping injection")
                return@doLast
            }

            // Get all markdown source files to extract titles
            val sourceDocsDir = File(projectDir, "docs")
            val mdFiles = sourceDocsDir.listFiles()?.filter {
                it.extension == "md"
            } ?: emptyList()

            // Helper function to extract H1 title from markdown
            fun extractH1Title(mdFile: File): String {
                val firstLine = mdFile.readLines().firstOrNull { it.trim().startsWith("# ") }
                return firstLine?.substring(2)?.trim()
                    ?: mdFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
            }

            // Get all HTML docs (for file references)
            val htmlFiles = docsDir.listFiles()?.filter {
                it.extension == "html" && it.name != "index.html"
            }?.sortedBy { it.nameWithoutExtension } ?: emptyList()

            if (htmlFiles.isEmpty()) {
                println("⚠️  No documentation files found")
                return@doLast
            }

            // Build simple flat navigation HTML - all docs listed alphabetically by title
            val navHtml = buildString {
                appendLine("""<div class="toc--part" id="Documentation-nav-submenu" pageid="docs/README" data-nesting-level="0">""")
                appendLine("""  <div class="toc--row">""")
                appendLine("""    <button class="toc--button" aria-expanded="false" aria-label="Documentation" onclick="window.handleTocButtonClick(event, 'Documentation-nav-submenu')"></button>""")
                appendLine("""    <a href="docs/README.html" class="toc--link"><span>📚 Documentation</span></a>""")
                appendLine("""  </div>""")

                // List all docs alphabetically by H1 title under Documentation
                htmlFiles
                    .mapNotNull { htmlFile ->
                        val mdFile = mdFiles.find { it.nameWithoutExtension == htmlFile.nameWithoutExtension }
                        mdFile?.let { htmlFile to extractH1Title(it) }
                    }
                    .sortedBy { (_, title) -> title }
                    .forEachIndexed { index, (file, title) ->
                        val pageId = "docs/${file.nameWithoutExtension}"
                        appendLine("""  <div class="toc--part" id="Documentation-nav-submenu-$index" pageid="$pageId" data-nesting-level="1">""")
                        appendLine("""    <div class="toc--row">""")
                        appendLine("""      <a href="docs/${file.name}" class="toc--link"><span>$title</span></a>""")
                        appendLine("""    </div>""")
                        appendLine("""  </div>""")
                    }

                appendLine("""</div>""")
            }

            // Inject navigation at the beginning (only if not already injected)
            val navContent = navFile.readText()
            val modifiedNav = if (navContent.contains("📚 Documentation")) {
                // Already injected, remove old injection first
                val startMarker = """<div class="sideMenu">"""
                val endMarker = """<div class="toc--part" id="Compiler-nav-submenu"""" // First Dokka-generated item
                val startIndex = navContent.indexOf(startMarker) + startMarker.length
                val endIndex = navContent.indexOf(endMarker)
                if (startIndex > 0 && endIndex > startIndex) {
                    val before = navContent.substring(0, startIndex)
                    val after = navContent.substring(endIndex)
                    "$before\n$navHtml\n $after"
                } else {
                    navContent.replace(startMarker, "$startMarker\n$navHtml")
                }
            } else {
                // First time injecting
                navContent.replace(
                    """<div class="sideMenu">""",
                    """<div class="sideMenu">
$navHtml"""
                )
            }
            navFile.writeText(modifiedNav)

            // Update pages.json with H1 titles
            val pagesContent = pagesFile.readText()
            val pages = buildString {
                htmlFiles.forEach { htmlFile ->
                    val mdFile = mdFiles.find { it.nameWithoutExtension == htmlFile.nameWithoutExtension }
                    val title = mdFile?.let { extractH1Title(it) }
                        ?: htmlFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                    val searchKeys = title.split(" ").filter { it.isNotBlank() }
                    append(""",{"name":"$title","description":"Fakt Documentation: $title","location":"docs/${htmlFile.name}","searchKeys":["$title",""")
                    append(searchKeys.joinToString("\",\""))
                    append("""","Documentation"]}""")
                }
            }
            val modifiedPages = pagesContent.replace("]", "$pages]")
            pagesFile.writeText(modifiedPages)

            println("✅ Injected ${htmlFiles.size} documentation files into navigation")
        }
    }

    // Ensure docs are converted and injected after dokka generation
    tasks.named("dokkaGenerate") {
        finalizedBy("injectDocsIntoNavigation")
    }
}
