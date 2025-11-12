// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.renderer

import com.rsicarelli.fakt.codegen.model.CodeBlock
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeDeclaration
import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.model.CodeFunction
import com.rsicarelli.fakt.codegen.model.CodeMember
import com.rsicarelli.fakt.codegen.model.CodeParameter
import com.rsicarelli.fakt.codegen.model.CodeProperty
import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.model.CodeTypeParameter

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
    header?.let {
        builder.appendLine("// $it")
    }

    // Package
    val escapedPackage = packageName.escapePackageName()
    builder.appendLine("package $escapedPackage")
    builder.appendLine()

    // Imports (sorted)
    if (imports.isNotEmpty()) {
        imports.sorted().forEach { import ->
            builder.appendLine("import $import")
        }
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
        is CodeFunction -> renderTo(builder)
        is CodeProperty -> renderTo(builder)
    }
}

/**
 * Renders [CodeClass] to [CodeBuilder].
 *
 * Generates complete class structure with members.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeClass.renderTo(builder: CodeBuilder) {
    // Build class header
    val modifiersStr = modifiers.joinToString(" ") { it.name.lowercase() }
    val modifierPrefix = if (modifiersStr.isNotEmpty()) "$modifiersStr " else ""

    val typeParamsStr = if (typeParameters.isNotEmpty()) {
        "<${typeParameters.joinToString { it.render() }}>"
    } else ""

    val superTypesStr = if (superTypes.isNotEmpty()) {
        " : ${superTypes.joinToString { it.render() }}"
    } else ""

    val whereClauseStr = whereClause?.let { " where $it" } ?: ""

    // Render class
    builder.block("${modifierPrefix}class $name$typeParamsStr$superTypesStr$whereClauseStr") {
        members.forEachIndexed { index, member ->
            member.renderTo(this)
            if (index < members.lastIndex) {
                appendLine()
            }
        }
    }
}

/**
 * Renders [CodeFunction] to [CodeBuilder].
 *
 * Generates complete function signature and body.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeFunction.renderTo(builder: CodeBuilder) {
    val modifiersStr = buildString(capacity = 50) {
        if (modifiers.isNotEmpty()) {
            append(modifiers.joinToString(" ") { it.name.lowercase() })
            append(" ")
        }
        if (isSuspend) append("suspend ")
        if (isInline) append("inline ")
    }

    val typeParamsStr = if (typeParameters.isNotEmpty()) {
        "<${typeParameters.joinToString { it.render() }}> "
    } else ""

    val receiverStr = receiverType?.let { "${it.render()}." } ?: ""

    val paramsStr = parameters.joinToString { it.render() }
    val returnTypeStr = ": ${returnType.render()}"

    when (body) {
        is CodeBlock.Expression -> {
            builder.appendLine(
                "${modifiersStr}fun $typeParamsStr$receiverStr$name($paramsStr)$returnTypeStr = ${(body as CodeBlock.Expression).expr.render()}"
            )
        }

        is CodeBlock.Statements -> {
            builder.block("${modifiersStr}fun $typeParamsStr$receiverStr$name($paramsStr)$returnTypeStr") {
                body.statements.forEach { stmt ->
                    appendLine(stmt)
                }
            }
        }

        CodeBlock.Empty -> {
            builder.appendLine("${modifiersStr}fun $typeParamsStr$receiverStr$name($paramsStr)$returnTypeStr")
        }
    }
}

/**
 * Renders [CodeProperty] to [CodeBuilder].
 *
 * Generates property declaration with optional getter/setter.
 *
 * @param builder The [CodeBuilder] to write to
 */
public fun CodeProperty.renderTo(builder: CodeBuilder) {
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
                        is CodeBlock.Expression ->
                            appendLine("get() = ${it.expr.render()}")

                        is CodeBlock.Statements -> {
                            block("get()") {
                                it.statements.forEach { stmt -> appendLine(stmt) }
                            }
                        }

                        CodeBlock.Empty -> {}
                    }
                }
                setter?.let {
                    when (it) {
                        is CodeBlock.Expression ->
                            appendLine("set(value) = ${it.expr.render()}")

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
public fun CodeType.render(): String = when (this) {
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
public fun CodeTypeParameter.render(): String = buildString(capacity = 40) {
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
public fun CodeParameter.render(): String = buildString(capacity = 50) {
    if (isVararg) append("vararg ")
    append("$name: ${type.render()}")
    defaultValue?.let { append(" = ${it.render()}") }
}

/**
 * Renders [CodeExpression] to string.
 *
 * @return Expression as Kotlin source string
 */
public fun CodeExpression.render(): String = when (this) {
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

/**
 * Set of Kotlin keywords that must be escaped in package names.
 */
private val KOTLIN_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for",
    "fun", "if", "in", "interface", "is", "null", "object", "package",
    "return", "super", "this", "throw", "true", "try", "typealias",
    "typeof", "val", "var", "when", "while"
)
