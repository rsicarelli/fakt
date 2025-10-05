// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.propertiesAndMethods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive test for CollectionService fake generation.
 *
 * Validates:
 * - Collection type transformations (List -> Set, Set -> Map)
 * - Method-level generics with collections
 * - Nested collection handling
 * - Map transformations with generics
 */
class CollectionServiceTest {
    @Test
    fun `GIVEN CollectionService WHEN processing strings THEN should transform List to Set`() {
        // Given
        val fake =
            fakeCollectionService {
                processStrings { items ->
                    items.map { it.uppercase() }.toSet()
                }
            }

        // When
        val result = fake.processStrings(listOf("a", "b", "a", "c"))

        // Then
        assertEquals(3, result.size) // Set removes duplicates
        assertTrue(result.contains("A"))
        assertTrue(result.contains("B"))
        assertTrue(result.contains("C"))
    }

    @Test
    fun `GIVEN CollectionService WHEN processing numbers THEN should transform Set to Map`() {
        // Given
        val fake =
            fakeCollectionService {
                processNumbers { items ->
                    items.associateWith { "Number: $it" }
                }
            }

        // When
        val result = fake.processNumbers(setOf(1, 2, 3))

        // Then
        assertEquals(3, result.size)
        assertEquals("Number: 1", result[1])
        assertEquals("Number: 2", result[2])
        assertEquals("Number: 3", result[3])
    }

    @Test
    fun `GIVEN CollectionService WHEN transforming map THEN should use method-level generics`() {
        // Given
        val fake =
            fakeCollectionService {
                transformMap<String, Int> { map, transformer ->
                    map.mapValues { (k, v) -> transformer(k, v) }
                }
            }

        // When
        val inputMap = mapOf("a" to 1, "b" to 2, "c" to 3)
        val result =
            fake.transformMap(inputMap) { key, value ->
                "$key=$value"
            }

        // Then
        assertEquals(3, result.size)
        assertEquals("a=1", result["a"])
        assertEquals("b=2", result["b"])
        assertEquals("c=3", result["c"])
    }

    @Test
    fun `GIVEN CollectionService WHEN handling nested collections THEN should preserve structure`() {
        // Given
        val fake =
            fakeCollectionService {
                nestedCollections { data ->
                    data.entries.map { (key, listOfSets) ->
                        mapOf(key to listOfSets.flatten().first())
                    }
                }
            }

        // When
        val input =
            mapOf(
                "group1" to listOf(setOf(1, 2), setOf(3, 4)),
                "group2" to listOf(setOf(5, 6)),
            )
        val result = fake.nestedCollections(input)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.containsKey("group1") })
        assertTrue(result.any { it.containsKey("group2") })
    }

    @Test
    fun `GIVEN CollectionService WHEN using defaults THEN should have empty collections`() {
        // Given
        val fake = fakeCollectionService()

        // When
        val stringResult = fake.processStrings(listOf("a", "b"))
        val numberResult = fake.processNumbers(setOf(1, 2))
        val mapResult = fake.transformMap(mapOf("a" to 1)) { k, v -> "$k$v" }
        val nestedResult = fake.nestedCollections(mapOf("a" to listOf(setOf(1))))

        // Then
        assertTrue(stringResult.isEmpty()) // Default: empty set
        assertTrue(numberResult.isEmpty()) // Default: empty map
        assertTrue(mapResult.isEmpty()) // Default: empty map
        assertTrue(nestedResult.isEmpty()) // Default: empty list
    }
}
