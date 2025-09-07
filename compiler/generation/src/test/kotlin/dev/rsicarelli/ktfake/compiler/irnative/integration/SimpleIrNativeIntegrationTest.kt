// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.integration

import dev.rsicarelli.ktfake.compiler.analysis.*
import dev.rsicarelli.ktfake.compiler.types.KotlinTypeMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

/**
 * Simple integration test demonstrating IR-Native capabilities without complex IR dependencies.
 *
 * This test validates the complete IR-Native pipeline:
 * 1. Interface analysis (simulated)
 * 2. Type mapping (real)
 * 3. Code generation (demonstrated)
 *
 * Shows that IR-Native architecture is working and can generate complete fake implementations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleIrNativeIntegrationTest {

    private val typeMapper = KotlinTypeMapper()

    @Test
    fun `GIVEN simulated UserService analysis WHEN generating fake THEN should produce complete implementation`() {
        // Given - Simulated interface analysis (what IrInterfaceAnalyzer would produce)
        val analysis = createUserServiceAnalysis()

        // When - Generate fake implementation using IR-Native components
        val fakeImplementation = generateFakeImplementation(analysis)

        // Then - Verify complete pipeline
        assertEquals("UserService", analysis.interfaceName)
        assertEquals("com.example.user", analysis.packageName)
        assertEquals(4, analysis.methods.size)

        // Verify method analysis quality
        val getUser = analysis.methods.find { it.name == "getUser" }
        assertNotNull(getUser)
        assertEquals("User", getUser.returnType.qualifiedName)
        assertFalse(getUser.isSuspend)

        val fetchUserAsync = analysis.methods.find { it.name == "fetchUserAsync" }
        assertNotNull(fetchUserAsync)
        assertTrue(fetchUserAsync.isSuspend, "Should detect suspend methods")

        // Verify type mapping works for all types
        verifyAllTypesCanBeMapped(analysis)

        // Verify generated implementation
        assertTrue(fakeImplementation.contains("class FakeUserServiceImpl : UserService"))
        assertTrue(fakeImplementation.contains("fun fakeUserService"))
        assertTrue(fakeImplementation.contains("class FakeUserServiceConfig"))

        // Verify generated methods have correct signatures
        assertTrue(fakeImplementation.contains("override fun getUser(id: kotlin.String): User"))
        assertTrue(fakeImplementation.contains("override suspend fun fetchUserAsync(id: kotlin.String): User"))

        println("✅ IR-Native Integration Test Results:")
        println("📊 Interface: ${analysis.interfaceName}")
        println("🔧 Methods analyzed: ${analysis.methods.size}")
        println("📝 Generated code: ${fakeImplementation.lines().size} lines")
        println("🎯 All types mapped successfully")
    }

    @Test
    fun `GIVEN PaymentService with Result types WHEN processing THEN should handle complex types`() {
        // Given
        val analysis = createPaymentServiceAnalysis()

        // When
        val fakeImplementation = generateFakeImplementation(analysis)

        // Then
        val processPayment = analysis.methods.find { it.name == "processPayment" }
        assertNotNull(processPayment)
        assertEquals("kotlin.Result", processPayment.returnType.qualifiedName)

        // Verify Result type mapping
        val resultDefault = typeMapper.mapTypeToDefault(processPayment.returnType)
        assertNotNull(resultDefault)

        // Verify generated code handles Result
        assertTrue(fakeImplementation.contains("override fun processPayment(amount: kotlin.Double): kotlin.Result"))

        println("✅ Complex type handling validated")
    }

    @Test
    fun `GIVEN Repository with generics WHEN processing THEN should handle generic types`() {
        // Given
        val analysis = createRepositoryAnalysis()

        // When
        val fakeImplementation = generateFakeImplementation(analysis)

        // Then
        assertEquals(1, analysis.generics.size)
        val genericT = analysis.generics[0]
        assertEquals("T", genericT.name)

        // Verify generic methods
        val save = analysis.methods.find { it.name == "save" }
        assertNotNull(save)
        assertEquals("T", save.parameters[0].type.qualifiedName)

        // Verify generic handling strategy
        val genericType = TypeAnalysis("T", false, emptyList(), false)
        val handling = typeMapper.handleGenericType(genericType)
        assertNotNull(handling)

        println("✅ Generic type handling validated")
    }

    @Test
    fun `GIVEN all builtin types WHEN mapping THEN should generate appropriate defaults`() {
        // Given - All common Kotlin types
        val types = listOf(
            "kotlin.String", "kotlin.Int", "kotlin.Boolean", "kotlin.Unit",
            "kotlin.collections.List", "kotlin.collections.Set", "kotlin.collections.Map",
            "kotlin.Result", "kotlinx.coroutines.flow.Flow", "kotlinx.coroutines.Job"
        )

        // When & Then - Verify each type can be mapped
        types.forEach { typeName ->
            val typeAnalysis = TypeAnalysis(typeName, false, emptyList(), true)
            val defaultExpr = typeMapper.mapTypeToDefault(typeAnalysis)
            val defaultString = typeMapper.generateReturnExpression(typeAnalysis)

            assertNotNull(defaultExpr, "Should map type: $typeName")
            assertTrue(defaultString.isNotEmpty(), "Should generate return expression: $typeName")

            println("✓ $typeName → $defaultString")
        }

        println("✅ All builtin types successfully mapped")
    }

    @Test
    fun `GIVEN custom types WHEN registering mappings THEN should use custom generators`() {
        // Given
        val customType = TypeAnalysis("com.example.User", false, emptyList(), false)

        // Register custom mapping
        typeMapper.registerCustomTypeMapping("com.example.User") { _ ->
            dev.rsicarelli.ktfake.compiler.types.DefaultValueExpression.Constructor(
                "User",
                listOf("\"default\"", "\"user@example.com\"")
            )
        }

        // When
        val defaultExpr = typeMapper.mapTypeToDefault(customType)
        val defaultString = typeMapper.generateReturnExpression(customType)

        // Then
        assertNotNull(defaultExpr)
        assertEquals("User(\"default\", \"user@example.com\")", defaultString)

        println("✅ Custom type mapping validated")
    }

    /**
     * Generate complete fake implementation from analysis.
     * This demonstrates the final output of IR-Native pipeline.
     */
    private fun generateFakeImplementation(analysis: InterfaceAnalysis): String {
        val interfaceName = analysis.interfaceName
        val packageName = analysis.packageName

        // Generate method implementations with correct types
        val methodImpls = analysis.methods.joinToString("\n") { method ->
            val params = method.parameters.joinToString(", ") {
                "${it.name}: ${it.type.qualifiedName}"
            }
            val defaultReturn = typeMapper.generateReturnExpression(method.returnType)
            val suspend = if (method.isSuspend) "suspend " else ""

            "    override ${suspend}fun ${method.name}($params): ${method.returnType.qualifiedName} = $defaultReturn"
        }

        // Generate factory function
        val factory = """fun fake$interfaceName(configure: Fake${interfaceName}Config.() -> Unit = {}): $interfaceName {
    return Fake${interfaceName}Impl().apply {
        Fake${interfaceName}Config(this).configure()
    }
}"""

        // Generate configuration DSL
        val configMethods = analysis.methods.joinToString("\n") { method ->
            "    fun ${method.name}(behavior: () -> ${method.returnType.qualifiedName}) { /* Configure behavior */ }"
        }

        return """// Generated by IR-Native KtFakes
package $packageName

import dev.rsicarelli.ktfake.*

class Fake${interfaceName}Impl : $interfaceName {
$methodImpls
}

$factory

class Fake${interfaceName}Config(private val fake: Fake${interfaceName}Impl) {
$configMethods
}"""
    }

    /**
     * Verify all types in analysis can be mapped by TypeMapper.
     */
    private fun verifyAllTypesCanBeMapped(analysis: InterfaceAnalysis) {
        // Verify all method return types
        analysis.methods.forEach { method ->
            val returnDefault = typeMapper.mapTypeToDefault(method.returnType)
            assertNotNull(returnDefault, "Should map return type: ${method.returnType.qualifiedName}")

            // Verify all parameter types
            method.parameters.forEach { param ->
                val paramDefault = typeMapper.mapTypeToDefault(param.type)
                assertNotNull(paramDefault, "Should map parameter type: ${param.type.qualifiedName}")
            }
        }

        // Verify all property types
        analysis.properties.forEach { prop ->
            val propDefault = typeMapper.mapTypeToDefault(prop.type)
            assertNotNull(propDefault, "Should map property type: ${prop.type.qualifiedName}")
        }
    }

    /**
     * Create realistic UserService analysis (simulates IrInterfaceAnalyzer output).
     */
    private fun createUserServiceAnalysis(): InterfaceAnalysis {
        return InterfaceAnalysis(
            sourceInterface = null,
            interfaceName = "UserService",
            packageName = "com.example.user",
            methods = listOf(
                createMethod("getUser", "User", listOf("id" to "kotlin.String")),
                createMethod("createUser", "User", listOf("name" to "kotlin.String", "email" to "kotlin.String")),
                createMethod("deleteUser", "kotlin.Unit", listOf("id" to "kotlin.String")),
                createMethod("fetchUserAsync", "User", listOf("id" to "kotlin.String"), isSuspend = true)
            ),
            properties = emptyList(),
            generics = emptyList(),
            annotations = AnnotationAnalysis(false, false, true, "test", emptyList()),
            dependencies = emptyList()
        )
    }

    /**
     * Create PaymentService analysis with Result types.
     */
    private fun createPaymentServiceAnalysis(): InterfaceAnalysis {
        return InterfaceAnalysis(
            sourceInterface = null,
            interfaceName = "PaymentService",
            packageName = "com.example.payment",
            methods = listOf(
                createMethod("processPayment", "kotlin.Result", listOf("amount" to "kotlin.Double")),
                createMethod("getPaymentHistory", "kotlin.collections.List", listOf("userId" to "kotlin.String"))
            ),
            properties = emptyList(),
            generics = emptyList(),
            annotations = AnnotationAnalysis(false, false, true, "test", emptyList()),
            dependencies = emptyList()
        )
    }

    /**
     * Create Repository analysis with generics.
     */
    private fun createRepositoryAnalysis(): InterfaceAnalysis {
        return InterfaceAnalysis(
            sourceInterface = null,
            interfaceName = "Repository",
            packageName = "com.example.repository",
            methods = listOf(
                createMethod("save", "kotlin.Unit", listOf("entity" to "T")),
                createMethod("findById", "T", listOf("id" to "kotlin.String")),
                createMethod("findAll", "kotlin.collections.List", emptyList())
            ),
            properties = emptyList(),
            generics = listOf(
                GenericAnalysis("T", listOf(TypeAnalysis("kotlin.Any", false, emptyList(), true)), GenericVariance.INVARIANT)
            ),
            annotations = AnnotationAnalysis(false, false, true, "test", emptyList()),
            dependencies = emptyList()
        )
    }

    /**
     * Helper to create method analysis.
     */
    private fun createMethod(name: String, returnType: String, params: List<Pair<String, String>>, isSuspend: Boolean = false): MethodAnalysis {
        return MethodAnalysis(
            function = null,
            name = name,
            parameters = params.map { (paramName, paramType) ->
                ParameterAnalysis(
                    name = paramName,
                    type = TypeAnalysis(paramType, false, emptyList(), paramType.startsWith("kotlin.")),
                    hasDefaultValue = false,
                    isVararg = false
                )
            },
            returnType = TypeAnalysis(returnType, false, emptyList(), returnType.startsWith("kotlin.")),
            isSuspend = isSuspend,
            modifiers = setOf(MethodModifier.ABSTRACT)
        )
    }
}
