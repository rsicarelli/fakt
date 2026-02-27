// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.renderer

import com.rsicarelli.fakt.codegen.model.CodeAnnotation
import com.rsicarelli.fakt.codegen.model.CodeBlock
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeComment
import com.rsicarelli.fakt.codegen.model.CodeDataClass
import com.rsicarelli.fakt.codegen.model.CodeDeclaration
import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.model.CodeFunction
import com.rsicarelli.fakt.codegen.model.CodeMember
import com.rsicarelli.fakt.codegen.model.CodeParameter
import com.rsicarelli.fakt.codegen.model.CodeProperty
import com.rsicarelli.fakt.codegen.model.CodeRegionEnd
import com.rsicarelli.fakt.codegen.model.CodeRegionStart
import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.model.CodeTypeParameter
import com.rsicarelli.fakt.codegen.model.ConstructorProperty

/**
 * Renders [CodeFile] to string using pass-the-builder pattern.
 *
 * This is the main entry point for converting a code model to Kotlin source.
 *
 * Example:
 * ```kotlin
 * val file = codeFile("com.example") { ... }
 * val builder = CodeBuilder()
 * file.renderTo(builder)
 * val kotlinCode = builder.build()
 * ```
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeFile.renderTo(builder: CodeBuilder) {
    // File header
    header?.let { builder.appendLine("// $it") }

    // File-level annotations (before package declaration)
    if (fileAnnotations.isNotEmpty()) {
        fileAnnotations.forEach { annotation -> annotation.renderAsFileAnnotation(builder) }
        builder.appendLine()
    }

    // Package
    val escapedPackage = packageName.escapePackageName()
    builder.appendLine("package $escapedPackage")
    builder.appendLine()

    // Imports (sorted)
    if (imports.isNotEmpty()) {
        imports.sorted().forEach { import -> builder.appendLine("import $import") }
        builder.appendLine()
    }

    // Declarations
    declarations.forEachIndexed { index, decl ->
        decl.renderTo(builder)
        if (index < declarations.lastIndex) {
            builder.appendLine()
        }
    }
}

/**
 * Renders [CodeDeclaration] to [CodeBuilder].
 *
 * Dispatches to appropriate renderer based on declaration type.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeDeclaration.renderTo(builder: CodeBuilder) {
    when (this) {
        is CodeClass -> renderTo(builder)
        is CodeDataClass -> renderTo(builder)
        is CodeFunction -> renderTo(builder)
        is CodeProperty -> renderTo(builder)
    }
}

/**
 * Renders [CodeMember] to [CodeBuilder].
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeMember.renderTo(builder: CodeBuilder) {
    when (this) {
        is CodeComment -> renderTo(builder)
        is CodeRegionStart -> renderTo(builder)
        is CodeRegionEnd -> renderTo(builder)
        is CodeFunction -> renderTo(builder)
        is CodeProperty -> renderTo(builder)
    }
}

/**
 * Renders [CodeComment] to [CodeBuilder].
 *
 * Generates a single-line comment.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeComment.renderTo(builder: CodeBuilder) {
    builder.appendLine("// $text")
}

/**
 * Renders [CodeRegionStart] to [CodeBuilder].
 *
 * Generates an IDE-collapsible region start marker.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeRegionStart.renderTo(builder: CodeBuilder) {
    builder.appendLine("//region $name")
}

/**
 * Renders [CodeRegionEnd] to [CodeBuilder].
 *
 * Generates an IDE-collapsible region end marker.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeRegionEnd.renderTo(builder: CodeBuilder) {
    builder.appendLine("//endregion")
}

/**
 * Renders [CodeClass] to [CodeBuilder].
 *
 * Generates complete class structure with members.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeClass.renderTo(builder: CodeBuilder) {
    // Render annotations first (before class declaration)
    annotations.forEach { annotation -> annotation.renderTo(builder) }

    val header = buildClassHeader()

    // Render class
    builder.block(header) {
        members.forEachIndexed { index, member ->
            member.renderTo(this)
            if (index < members.lastIndex && !areCompactSiblings(member, members[index + 1])) {
                appendLine()
            }
        }
    }
}

/** Builds the full class header string (modifiers, name, type params, constructor, supertypes). */
private fun CodeClass.buildClassHeader(): String {
    val modifiersStr = modifiers.joinToString(" ") { it.name.lowercase() }
    val modifierPrefix = if (modifiersStr.isNotEmpty()) "$modifiersStr " else ""

    val typeParamsStr =
        if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString { it.render() }}>"
        } else {
            ""
        }

    val constructorStr = renderConstructorProperties()

    val superTypesStr =
        if (superTypes.isNotEmpty()) {
            " : ${superTypes.joinToString { it.render() }}"
        } else {
            ""
        }

    val whereClauseStr = whereClause?.let { " where $it" } ?: ""

    return "${modifierPrefix}class $name$typeParamsStr$constructorStr$superTypesStr$whereClauseStr"
}

/** Renders constructor properties — multi-line when more than one, single-line otherwise. */
private fun CodeClass.renderConstructorProperties(): String =
    when {
        constructorProperties.isEmpty() -> ""
        constructorProperties.size > 1 -> {
            val props = constructorProperties.joinToString(",\n    ") { it.render() }
            "(\n    $props,\n)"
        }
        else -> "(${constructorProperties.joinToString(", ") { it.render() }})"
    }

/** Returns true if two adjacent members are both compact and of the same kind. */
private fun areCompactSiblings(current: CodeMember, next: CodeMember): Boolean =
    (current is CodeProperty && current.compact && next is CodeProperty && next.compact) ||
        (current is CodeFunction && current.compact && next is CodeFunction && next.compact)

/**
 * Renders [ConstructorProperty] to string.
 *
 * @return Constructor property as Kotlin source string
 */
public fun ConstructorProperty.render(): String {
    val modifiersStr = modifiers.joinToString(" ") { it.name.lowercase() }
    val modifierPrefix = if (modifiersStr.isNotEmpty()) "$modifiersStr " else ""
    val defaultStr = defaultValue?.let { " = $it" } ?: ""
    val keyword = if (isMutable) "var" else "val"
    return "${modifierPrefix}$keyword $name: $type$defaultStr"
}

/**
 * Renders [CodeDataClass] to [CodeBuilder].
 *
 * Generates data class with constructor properties.
 *
 * Examples:
 * - Single property: `public data class UserCall(val id: String)`
 * - Multi-property: ```kotlin public data class UserCall( val id: String, val name: String, ) ```
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeDataClass.renderTo(builder: CodeBuilder) {
    // Render annotations first
    annotations.forEach { annotation -> annotation.renderTo(builder) }

    // Build modifiers
    val modifiersStr = modifiers.joinToString(" ") { it.name.lowercase() }
    val modifierPrefix = if (modifiersStr.isNotEmpty()) "$modifiersStr " else ""

    // Render data class
    if (properties.size == 1) {
        // Single property: inline format
        val prop = properties.first()
        builder.appendLine("${modifierPrefix}data class $name(val ${prop.name}: ${prop.type})")
    } else {
        // Multi-property: multi-line format
        builder.appendLine("${modifierPrefix}data class $name(")
        builder.indent {
            properties.forEach { prop -> appendLine("val ${prop.name}: ${prop.type},") }
        }
        builder.appendLine(")")
    }
}

/**
 * Renders [CodeAnnotation] to [CodeBuilder].
 *
 * Generates annotation with arguments. Examples:
 * - `@OptIn(ExperimentalApi::class)`
 * - `@Deprecated("old", level = DeprecationLevel.WARNING)`
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeAnnotation.renderTo(builder: CodeBuilder) {
    val argsStr =
        if (arguments.isEmpty()) {
            ""
        } else {
            "(${arguments.joinToString(", ")})"
        }
    builder.appendLine("@$name$argsStr")
}

/**
 * Renders [CodeAnnotation] as a file-level annotation.
 *
 * Generates file-level annotation with @file: prefix. Examples:
 * - `@file:OptIn(ExperimentalObjCRefinement::class)`
 * - `@file:Suppress("UNCHECKED_CAST")`
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeAnnotation.renderAsFileAnnotation(builder: CodeBuilder) {
    val argsStr =
        if (arguments.isEmpty()) {
            ""
        } else {
            "(${arguments.joinToString(", ")})"
        }
    builder.appendLine("@file:$name$argsStr")
}

/**
 * Renders [CodeFunction] to [CodeBuilder].
 *
 * Generates complete function signature and body, with optional KDoc.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeFunction.renderTo(builder: CodeBuilder) {
    // Render KDoc if present
    kdoc?.let { renderKDoc(builder, it) }
    annotations.forEach { it.renderTo(builder) }
    val signature = buildFunctionSignature()
    when (body) {
        is CodeBlock.Expression -> renderExpressionBody(builder, signature)
        is CodeBlock.Statements -> renderStatementsBody(builder, signature)
        CodeBlock.Empty -> builder.appendLine(signature.full)
    }
}

/**
 * Renders KDoc content to the builder.
 *
 * KDoc is rendered as a multi-line comment with proper formatting:
 * ```
 * /**
 *  * First line
 *  * Second line
 *  */
 * ```
 */
private fun renderKDoc(builder: CodeBuilder, kdoc: String) {
    val lines = kdoc.lines()
    builder.appendLine("/**")
    lines.forEach { line ->
        if (line.isBlank()) {
            builder.appendLine(" *")
        } else {
            builder.appendLine(" * $line")
        }
    }
    builder.appendLine(" */")
}

private fun CodeFunction.buildFunctionSignature(): FunctionSignature {
    val modifiersStr = buildModifiersString()
    val typeParamsStr =
        typeParameters
            .takeIf { it.isNotEmpty() }
            ?.let { "<${it.joinToString { p -> p.render() }}> " } ?: ""
    val receiverStr = receiverType?.let { "${it.render()}." } ?: ""
    val paramsStr = parameters.joinToString { it.render() }
    val returnTypeStr = ": ${returnType.render()}"
    val whereClauseStr = whereClause?.let { " where $it" } ?: ""
    return FunctionSignature(
        prefix = "${modifiersStr}fun $typeParamsStr$receiverStr$name($paramsStr)",
        returnType = returnTypeStr,
        whereClause = whereClauseStr,
    )
}

private fun CodeFunction.buildModifiersString(): String =
    buildString(capacity = 50) {
        if (modifiers.isNotEmpty()) {
            append(modifiers.joinToString(" ") { it.name.lowercase() })
            append(" ")
        }
        if (isSuspend) append("suspend ")
        if (isInline) append("inline ")
    }

private class FunctionSignature(
    private val prefix: String,
    private val returnType: String,
    private val whereClause: String,
) {
    val full: String
        get() = "$prefix$returnType$whereClause"

    fun withOptionalReturn(skipUnit: Boolean): String =
        when {
            skipUnit && returnType == ": Unit" -> "$prefix$whereClause"
            else -> full
        }
}

private fun CodeFunction.renderExpressionBody(builder: CodeBuilder, sig: FunctionSignature) {
    val skipUnit = returnType.render() == "Unit"
    val expr = (body as CodeBlock.Expression).expr.render()
    builder.appendLine("${sig.withOptionalReturn(skipUnit)} = $expr")
}

private fun CodeFunction.renderStatementsBody(builder: CodeBuilder, sig: FunctionSignature) {
    builder.block(sig.full) { (body as CodeBlock.Statements).statements.forEach { appendLine(it) } }
}

/**
 * Renders [CodeProperty] to [CodeBuilder].
 *
 * Generates property declaration with optional getter/setter.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeProperty.renderTo(builder: CodeBuilder) {
    // Render property annotations first
    annotations.forEach { annotation -> annotation.renderTo(builder) }

    val modifiersStr = modifiers.joinToString(" ") { it.name.lowercase() }
    val modifierPrefix = if (modifiersStr.isNotEmpty()) "$modifiersStr " else ""
    val varOrVal = if (isMutable) "var" else "val"
    val typeStr = type.render()

    when {
        getter != null || setter != null -> {
            builder.appendLine("$modifierPrefix$varOrVal $name: $typeStr")
            builder.indent {
                getter?.let {
                    when (it) {
                        is CodeBlock.Expression -> appendLine("get() = ${it.expr.render()}")

                        is CodeBlock.Statements -> {
                            block("get()") { it.statements.forEach { stmt -> appendLine(stmt) } }
                        }

                        CodeBlock.Empty -> {}
                    }
                }
                setter?.let {
                    when (it) {
                        is CodeBlock.Expression -> appendLine("set(value) = ${it.expr.render()}")

                        is CodeBlock.Statements -> {
                            block("set(value)") {
                                it.statements.forEach { stmt -> appendLine(stmt) }
                            }
                        }

                        CodeBlock.Empty -> {}
                    }
                }
            }
        }

        initializer != null -> {
            builder.appendLine(
                "$modifierPrefix$varOrVal $name: $typeStr = ${initializer!!.render()}"
            )
        }

        else -> {
            builder.appendLine("$modifierPrefix$varOrVal $name: $typeStr")
        }
    }
}

/**
 * Renders [CodeType] to string.
 *
 * Converts type representation to Kotlin type syntax.
 *
 * @return Type as Kotlin source string
 */
public fun CodeType.render(): String =
    when (this) {
        is CodeType.Simple -> name
        is CodeType.Generic -> "$name<${arguments.joinToString { it.render() }}>"
        is CodeType.Nullable -> "${inner.render()}?"
        is CodeType.Lambda -> {
            val params = parameters.joinToString { it.render() }
            val suspend = if (isSuspend) "suspend " else ""
            "$suspend($params) -> ${returnType.render()}"
        }
    }

/**
 * Renders [CodeTypeParameter] to string.
 *
 * @return Type parameter as Kotlin source string
 */
public fun CodeTypeParameter.render(): String =
    buildString(capacity = 40) {
        if (variance != CodeTypeParameter.Variance.INVARIANT) {
            append("${variance.name.lowercase()} ")
        }
        if (isReified) append("reified ")
        append(name)
        if (constraints.isNotEmpty()) {
            append(" : ${constraints.first()}")
        }
    }

/**
 * Renders [CodeParameter] to string.
 *
 * @return Parameter as Kotlin source string
 */
public fun CodeParameter.render(): String =
    buildString(capacity = 50) {
        if (isVararg) append("vararg ")
        append("$name: ${type.render()}")
        defaultValue?.let { append(" = ${it.render()}") }
    }

/**
 * Renders [CodeExpression] to string.
 *
 * @return Expression as Kotlin source string
 */
public fun CodeExpression.render(): String =
    when (this) {
        is CodeExpression.StringLiteral -> "\"$value\""
        is CodeExpression.NumberLiteral -> value
        is CodeExpression.Lambda -> {
            val params = if (parameters.isEmpty()) "" else "${parameters.joinToString()} -> "
            "{ $params$body }"
        }

        is CodeExpression.FunctionCall -> {
            val args = arguments.joinToString { it.render() }
            "$name($args)"
        }

        is CodeExpression.Raw -> code
    }

/**
 * Escapes package name segments (keywords, digits).
 *
 * Kotlin keywords and segments starting with digits must be escaped with backticks.
 *
 * @return Escaped package name
 */
private fun String.escapePackageName(): String {
    if (isEmpty()) return this
    return split('.').joinToString(".") { segment ->
        when {
            segment.firstOrNull()?.isDigit() == true -> "`$segment`"
            segment in KOTLIN_KEYWORDS -> "`$segment`"
            else -> segment
        }
    }
}

/** Set of Kotlin keywords that must be escaped in package names. */
private val KOTLIN_KEYWORDS =
    setOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while",
    )

/**
 * Renders only the file header (header comment, file annotations, package, imports).
 *
 * Used when rendering a CodeFile's declarations separately from its header, e.g., to reorder
 * sections in the output file.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeFile.renderHeaderOnly(builder: CodeBuilder) {
    // File header
    header?.let { builder.appendLine("// $it") }

    // File-level annotations (before package declaration)
    if (fileAnnotations.isNotEmpty()) {
        fileAnnotations.forEach { annotation -> annotation.renderAsFileAnnotation(builder) }
        builder.appendLine()
    }

    // Package
    val escapedPackage = packageName.escapePackageName()
    builder.appendLine("package $escapedPackage")
    builder.appendLine()

    // Imports (sorted)
    if (imports.isNotEmpty()) {
        imports.sorted().forEach { import -> builder.appendLine("import $import") }
        builder.appendLine()
    }
}

/**
 * Renders only the declarations (classes, functions, properties) without header.
 *
 * Used when the header has already been rendered via [renderHeaderOnly], e.g., to reorder sections
 * in the output file.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeFile.renderDeclarationsOnly(builder: CodeBuilder) {
    declarations.forEachIndexed { index, decl ->
        decl.renderTo(builder)
        if (index < declarations.lastIndex) {
            builder.appendLine()
        }
    }
}

/**
 * Convenience extension to render [CodeFile] directly to a String.
 *
 * @return The rendered code as a String
 */
public fun CodeFile.renderToString(): String {
    val builder = CodeBuilder()
    renderTo(builder)
    return builder.build()
}
