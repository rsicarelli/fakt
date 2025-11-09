// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.optimization

import com.rsicarelli.fakt.compiler.ir.transform.IrClassGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrGenerationMetadata
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import java.io.File
import java.security.MessageDigest

/**
 * Builds an MD5 signature (hash) for interface source file.
 *
 * Signature is computed from the source file content, detecting any changes:
 * - Properties added/removed/modified
 * - Methods added/removed/modified
 * - Type parameters changed
 * - Comments changed
 * - Formatting changed
 *
 * Used for file-based caching to detect when regeneration is needed.
 *
 * @return MD5 hash of source file content, or fallback signature if file unavailable
 */
fun IrGenerationMetadata.buildSignature(): String {
    val filePath = sourceInterface.getSourceFilePath()
    return if (filePath != null) {
        val sourceFile = File(filePath)
        if (sourceFile.exists()) {
            sourceFile.readBytes().md5()
        } else {
            // Fallback if file doesn't exist
            "interface $packageName.$interfaceName|props:${properties.size}|funs:${functions.size}"
        }
    } else {
        // Fallback if can't determine file path
        "interface $packageName.$interfaceName|props:${properties.size}|funs:${functions.size}"
    }
}

/**
 * Builds an MD5 signature (hash) for class source file.
 *
 * Signature is computed from the source file content, detecting any changes:
 * - Abstract/open properties added/removed/modified
 * - Abstract/open methods added/removed/modified
 * - Type parameters changed
 * - Comments changed
 * - Formatting changed
 *
 * Used for file-based caching to detect when regeneration is needed.
 *
 * @return MD5 hash of source file content, or fallback signature if file unavailable
 */
fun IrClassGenerationMetadata.buildSignature(): String {
    val filePath = sourceClass.getSourceFilePath()
    return if (filePath != null) {
        val sourceFile = File(filePath)
        if (sourceFile.exists()) {
            sourceFile.readBytes().md5()
        } else {
            // Fallback if file doesn't exist
            val propCount = abstractProperties.size + openProperties.size
            val funCount = abstractMethods.size + openMethods.size
            "class $packageName.$className|props:$propCount|funs:$funCount"
        }
    } else {
        // Fallback if can't determine file path
        val propCount = abstractProperties.size + openProperties.size
        val funCount = abstractMethods.size + openMethods.size
        "class $packageName.$className|props:$propCount|funs:$funCount"
    }
}

/**
 * Get source file path from IrClass by navigating parent hierarchy to IrFile.
 *
 * @return Absolute source file path or null if unavailable
 */
private fun IrClass.getSourceFilePath(): String? {
    var current = parent
    while (current != null) {
        when (current) {
            is IrFile -> return current.fileEntry.name
            is IrDeclaration -> current = current.parent
            else -> break
        }
    }
    return null
}

/**
 * Compute MD5 hash of byte array.
 *
 * @return Hexadecimal MD5 hash string (32 characters)
 */
private fun ByteArray.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}
