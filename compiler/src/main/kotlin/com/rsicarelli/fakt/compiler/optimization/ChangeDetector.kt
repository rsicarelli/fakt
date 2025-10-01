// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.TypeInfo

/**
 * Detects changes in interface signatures for incremental compilation.
 *
 * This class encapsulates the logic for determining whether an interface
 * needs to be regenerated based on signature comparison with cached versions.
 *
 * @since 1.0.0
 */
internal class ChangeDetector {

    /**
     * Determines if a type needs regeneration based on signature comparison.
     *
     * The logic is:
     * 1. If no cached signature exists → needs regeneration (new type)
     * 2. If cached signature differs from current → needs regeneration (changed)
     * 3. If cached signature matches current → skip regeneration (unchanged)
     *
     * @param type Current type information with signature
     * @param cachedSignature Previously cached signature, or null if new
     * @return true if type needs regeneration, false if can be skipped
     */
    fun needsRegeneration(type: TypeInfo, cachedSignature: String?): Boolean {
        return when (cachedSignature) {
            null -> {
                // No cached signature - this is a new type
                true
            }
            type.signature -> {
                // Signatures match - no changes detected
                false
            }
            else -> {
                // Signatures differ - type has changed
                true
            }
        }
    }

    /**
     * Generates a unique cache key for a type.
     *
     * The key format is: "fullyQualifiedName@fileName"
     * This ensures uniqueness across different files and packages.
     *
     * @param type Type information
     * @return Unique cache key for the type
     */
    fun generateCacheKey(type: TypeInfo): String {
        return "${type.fullyQualifiedName}@${type.fileName}"
    }

    /**
     * Compares two signatures and returns true if they represent the same interface.
     *
     * @param signature1 First signature to compare
     * @param signature2 Second signature to compare
     * @return true if signatures are equivalent, false otherwise
     */
    fun signaturesMatch(signature1: String, signature2: String): Boolean {
        return signature1 == signature2
    }

    /**
     * Validates that a signature is well-formed and non-empty.
     *
     * @param signature Signature to validate
     * @return true if signature is valid, false otherwise
     */
    fun isValidSignature(signature: String): Boolean {
        return signature.isNotBlank() && signature.contains("|")
    }
}
