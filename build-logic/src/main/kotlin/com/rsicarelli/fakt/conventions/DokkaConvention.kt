// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.DokkaExtension

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

            // Get all HTML docs (excluding index.html)
            val docFiles = docsDir.listFiles()?.filter {
                it.extension == "html" && it.name != "index.html"
            }?.sortedBy { it.nameWithoutExtension } ?: emptyList()

            if (docFiles.isEmpty()) {
                println("⚠️  No documentation files found")
                return@doLast
            }

            // Map categories to their index files
            val categoryIndexFiles = mapOf(
                "Getting Started" to "getting-started",
                "Architecture" to "architecture",
                "Specifications" to "specifications",
                "Testing" to "testing",
                "Implementation" to "implementation"
            )

            // Group docs by category
            val categories = mapOf(
                "Getting Started" to listOf("README"),
                "Architecture" to listOf("ARCHITECTURE", "METRO_FIR_IR_SPECIFICATIONS", "IR_NATIVE_DEMO", "IR_NATIVE_DEMONSTRATION"),
                "Specifications" to listOf("API_SPECIFICATIONS", "COMPILE_TIME_GENERIC_SOLUTIONS", "CODE_GENERATION_STRATEGIES"),
                "Testing" to listOf("TESTING_GUIDELINES", "TESTING_STATUS_REPORT", "TEST_COVERAGE_ANALYSIS"),
                "Implementation" to listOf(
                    "CURRENT_STATUS",
                    "IMPLEMENTATION_ROADMAP",
                    "IMPLEMENTATION_DECISION",
                    "GENERIC_IMPLEMENTATION_PROGRESS",
                    "GENERIC_TYPE_SCOPING_ANALYSIS",
                    "KOTLIN_COMPILER_IR_API_GUIDE",
                    "FINAL_COMPILE_TIME_SOLUTION",
                    "COMPILE_TIME_EXAMPLES"
                )
            )

            // Build navigation HTML
            val navHtml = buildString {
                appendLine("""<div class="toc--part" id="Documentation-nav-submenu" pageid="docs/getting-started" data-nesting-level="0">""")
                appendLine("""  <div class="toc--row">""")
                appendLine("""    <button class="toc--button" aria-expanded="false" aria-label="Documentation" onclick="window.handleTocButtonClick(event, 'Documentation-nav-submenu')"></button>""")
                appendLine("""    <a href="docs/getting-started.html" class="toc--link"><span>📚 Documentation</span></a>""")
                appendLine("""  </div>""")

                var categoryIndex = 0
                categories.forEach { (category, files) ->
                    val categoryFiles = docFiles.filter { it.nameWithoutExtension in files }
                    if (categoryFiles.isNotEmpty()) {
                        // Use category index file as the category link
                        val categoryIndexFile = categoryIndexFiles[category] ?: "getting-started"
                        val categoryLink = "$categoryIndexFile.html"
                        val categoryPageId = "docs/$categoryIndexFile"
                        appendLine("""  <div class="toc--part" id="Documentation-nav-submenu-$categoryIndex" pageid="$categoryPageId" data-nesting-level="1">""")
                        appendLine("""    <div class="toc--row">""")
                        appendLine("""      <button class="toc--button" aria-expanded="false" aria-label="$category" onclick="window.handleTocButtonClick(event, 'Documentation-nav-submenu-$categoryIndex')"></button>""")
                        appendLine("""      <a href="docs/$categoryLink" class="toc--link"><span>$category</span></a>""")
                        appendLine("""    </div>""")

                        categoryFiles.forEachIndexed { fileIndex, file ->
                            val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                            val pageId = "docs/${file.nameWithoutExtension}"
                            appendLine("""    <div class="toc--part" id="Documentation-nav-submenu-$categoryIndex-$fileIndex" pageid="$pageId" data-nesting-level="2">""")
                            appendLine("""      <div class="toc--row">""")
                            appendLine("""        <a href="docs/${file.name}" class="toc--link"><span>$title</span></a>""")
                            appendLine("""      </div>""")
                            appendLine("""    </div>""")
                        }

                        appendLine("""  </div>""")
                        categoryIndex++
                    }
                }

                appendLine("""</div>""")
            }

            // Inject navigation at the beginning
            val navContent = navFile.readText()
            val modifiedNav = navContent.replace(
                """<div class="sideMenu">""",
                """<div class="sideMenu">
$navHtml"""
            )
            navFile.writeText(modifiedNav)

            // Update pages.json
            val pagesContent = pagesFile.readText()
            val pages = buildString {
                docFiles.forEach { file ->
                    val title = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                    val searchKeys = title.split(" ").filter { it.isNotBlank() }
                    append(""",{"name":"$title","description":"Fakt Documentation: $title","location":"docs/${file.name}","searchKeys":["$title",""")
                    append(searchKeys.joinToString("\",\""))
                    append("""","Documentation"]}""")
                }
            }
            val modifiedPages = pagesContent.replace("]", "$pages]")
            pagesFile.writeText(modifiedPages)

            println("✅ Injected ${docFiles.size} documentation files into navigation")
        }
    }

    // Ensure docs are converted and injected after dokka generation
    tasks.named("dokkaGenerate") {
        finalizedBy("injectDocsIntoNavigation")
    }
}
