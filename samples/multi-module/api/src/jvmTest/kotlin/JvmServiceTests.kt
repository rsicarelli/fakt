// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package api.jvm

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
class JvmServiceTests {

    @Test
    fun `FileSystemService fake can be created`() {
        val fakeFileSystemService = fakeFileSystemService()
        assertNotNull(fakeFileSystemService)
        assertTrue(fakeFileSystemService is FileSystemService)
        
        // Basic method calls should not throw
        fakeFileSystemService.readFile("test.txt")
        fakeFileSystemService.writeFile("test.txt", "content")
        fakeFileSystemService.createDirectory("testdir")
    }

    @Test
    fun `DatabaseService fake can be created`() {
        val fakeDatabaseService = fakeDatabaseService()
        assertNotNull(fakeDatabaseService)
        assertTrue(fakeDatabaseService is DatabaseService)
        
        // Properties should be accessible
        fakeDatabaseService.isConnected
    }

    @Test
    fun `JvmSystemService fake has properties accessible`() {
        val fakeSystemService = fakeJvmSystemService()
        assertNotNull(fakeSystemService)
        assertTrue(fakeSystemService is JvmSystemService)
        
        // Properties should be accessible (will return default values from fake)
        fakeSystemService.javaVersion
        fakeSystemService.osName
        fakeSystemService.availableProcessors
        fakeSystemService.maxMemory
    }
}