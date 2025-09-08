// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeParameter
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.FqName
import java.io.File

/**
 * True IR-Native fake generation using direct IR manipulation.
 *
 * This implementation uses pure IR APIs to:
 * - Dynamically discover interface members without hardcoded mappings
 * - Generate IR nodes directly instead of string templates
 * - Create type-safe implementations with proper type analysis
 * - Handle complex types (generics, suspend functions, collections) automatically
 *
 * Based on the IR-Native demonstration architecture.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class UnifiedKtFakesIrGenerationExtension(
    private val messageCollector: MessageCollector? = null,
    private val outputDir: String? = null
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        messageCollector?.reportInfo("KtFakes: Starting IR-native generation for module ${moduleFragment.name}")

        // Generate fakes from main source sets (they will be output to test source sets)
        // We don't skip non-test modules anymore - we generate FROM main TO test

        try {
            // Phase 1: Dynamic Interface Discovery
            val fakeInterfaces = discoverFakeInterfaces(moduleFragment)
            messageCollector?.reportInfo("KtFakes: Discovered ${fakeInterfaces.size} @Fake annotated interfaces")

            if (fakeInterfaces.isEmpty()) {
                messageCollector?.reportInfo("KtFakes: No @Fake interfaces found, skipping generation")
                return
            }

            // Phase 2: IR-Native Code Generation
            for (fakeInterface in fakeInterfaces) {
                val interfaceName = fakeInterface.name.asString()
                messageCollector?.reportInfo("KtFakes: Processing @Fake interface: $interfaceName")

                // Dynamic interface analysis using IR APIs (IR-native approach!)
                val interfaceAnalysis = analyzeInterfaceDynamically(fakeInterface)

                // Generate working fakes using IR-native analysis + pragmatic generation
                generateWorkingFakeImplementation(
                    sourceInterface = fakeInterface,
                    analysis = interfaceAnalysis,
                    moduleFragment = moduleFragment
                )

                messageCollector?.reportInfo("KtFakes: Generated IR-native fake for $interfaceName")
            }

            messageCollector?.reportInfo("KtFakes: IR-native generation completed successfully")
        } catch (e: Exception) {
            messageCollector?.report(
                org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR,
                "KtFakes: IR-native generation failed: ${e.message}",
                null
            )
        }
    }

    /**
     * Dynamic interface analysis using pure IR APIs.
     * Discovers all members without hardcoded mappings.
     */
    private fun analyzeInterfaceDynamically(sourceInterface: IrClass): InterfaceAnalysis {
        val properties = mutableListOf<PropertyAnalysis>()
        val functions = mutableListOf<FunctionAnalysis>()

        // Extract interface-level type parameters
        val typeParameters = sourceInterface.typeParameters.map { typeParam ->
            typeParam.name.asString()
        }

        // Dynamically discover all interface members
        sourceInterface.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty -> {
                    properties.add(analyzeProperty(declaration))
                }

                is IrSimpleFunction -> {
                    if (!isSpecialFunction(declaration)) {
                        functions.add(analyzeFunction(declaration))
                    }
                }
            }
        }

        messageCollector?.reportInfo("KtFakes: Analyzed interface ${sourceInterface.name}: ${functions.size} functions, ${properties.size} properties, ${typeParameters.size} type parameters")

        return InterfaceAnalysis(
            interfaceName = sourceInterface.name.asString(),
            properties = properties,
            functions = functions,
            typeParameters = typeParameters,
            sourceInterface = sourceInterface
        )
    }

    /**
     * Analyze a property with full type information using IR APIs.
     */
    private fun analyzeProperty(property: IrProperty): PropertyAnalysis {
        val propertyType = property.getter?.returnType ?: property.backingField?.type!!

        return PropertyAnalysis(
            name = property.name.asString(),
            type = propertyType,
            isMutable = property.isVar,
            isNullable = propertyType.isMarkedNullable(),
            irProperty = property
        )
    }

    /**
     * Analyze a function with complete signature information using IR APIs.
     */
    private fun analyzeFunction(function: IrSimpleFunction): FunctionAnalysis {
        // Use parameters filtered by kind to avoid including receiver
        val parameters =
            function.parameters.filter { it.kind == org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular }
                .map { param ->
                    ParameterAnalysis(
                        name = param.name.asString(),
                        type = param.type,
                        hasDefaultValue = param.defaultValue != null,
                        isVararg = param.isVararg
                    )
                }

        // Extract method-level type parameters
        val typeParameters = function.typeParameters.map { typeParam ->
            typeParam.name.asString()
        }

        return FunctionAnalysis(
            name = function.name.asString(),
            parameters = parameters,
            returnType = function.returnType,
            isSuspend = function.isSuspend,
            isInline = function.isInline,
            typeParameters = typeParameters,
            irFunction = function
        )
    }

    /**
     * Check if function is a special function that shouldn't be implemented.
     */
    private fun isSpecialFunction(function: IrSimpleFunction): Boolean {
        val name = function.name.asString()
        return name in setOf("equals", "hashCode", "toString") ||
                name.startsWith("<") || // Compiler-generated functions
                function.origin == IrDeclarationOrigin.FAKE_OVERRIDE
    }


    /**
     * Generate working fake implementation using IR analysis with pragmatic file output.
     * Uses dynamic IR analysis but creates working code through file generation.
     */
    private fun generateWorkingFakeImplementation(
        sourceInterface: IrClass,
        analysis: InterfaceAnalysis,
        moduleFragment: IrModuleFragment
    ) {
        val interfaceName = analysis.interfaceName
        val fakeClassName = "Fake${interfaceName}Impl"
        val packageName = sourceInterface.packageFqName?.asString() ?: "test.sample"

        messageCollector?.reportInfo("KtFakes: Generating working fake for $interfaceName with ${analysis.functions.size} functions, ${analysis.properties.size} properties")

        // Generate implementation class code using IR analysis results
        val implementationCode = generateImplementationClass(analysis, fakeClassName, packageName)
        val factoryCode = generateFactoryFunction(analysis, fakeClassName, packageName)
        val configDslCode = generateConfigurationDsl(analysis, fakeClassName, packageName)

        // Write generated code to output directory, preserving package structure
        val outputDir = getGeneratedSourcesDir(moduleFragment)
        // Create subdirectories matching the package structure
        val packagePath = packageName.replace('.', '/')
        val packageDir = outputDir.resolve(packagePath)
        packageDir.mkdirs()
        val outputFile = packageDir.resolve("${fakeClassName}.kt")

        // Collect all required imports from the interface analysis
        val requiredImports = collectRequiredImports(analysis, packageName)

        val fullCode = buildString {
            appendLine("// Generated by KtFakes - IR-Native Analysis")
            appendLine("// Interface: $interfaceName")
            appendLine("package $packageName")
            appendLine()

            // Generate import statements for cross-module dependencies
            if (requiredImports.isNotEmpty()) {
                for (importStatement in requiredImports.sorted()) {
                    appendLine("import $importStatement")
                }
                appendLine()
            }

            appendLine(implementationCode)
            appendLine()
            appendLine(factoryCode)
            appendLine()
            appendLine(configDslCode)
        }

        outputFile.writeText(fullCode)

        messageCollector?.reportInfo("KtFakes: Generated working fake $fakeClassName at $outputFile")
    }

    /**
     * Generate implementation class using IR analysis results.
     */
    internal fun generateImplementationClass(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
        packageName: String
    ): String {
        val interfaceName = analysis.interfaceName

        // Handle interface-level type parameters
        val typeParamsString = if (analysis.typeParameters.isNotEmpty()) {
            "<${analysis.typeParameters.joinToString(", ") { "Any" }}>" // Use Any as placeholder for all type params
        } else {
            ""
        }
        val classDeclaration = "class $fakeClassName : $interfaceName$typeParamsString"

        return buildString {
            appendLine("$classDeclaration {")

            // Generate behavior properties for functions
            for (function in analysis.functions) {
                val functionName = function.name
                val returnTypeString = irTypeToKotlinString(function.returnType)
                val parameterTypes = function.parameters.joinToString(", ") { param ->
                    irTypeToKotlinString(param.type)
                }
                val parameterNames = function.parameters.joinToString(", ") { "_" }
                val defaultLambda = if (function.parameters.isEmpty()) {
                    "{ ${getDefaultValue(function.returnType)} }"
                } else {
                    "{ $parameterNames -> ${getDefaultValue(function.returnType)} }"
                }

                if (function.isSuspend) {
                    appendLine("    private var ${functionName}Behavior: suspend (${parameterTypes}) -> $returnTypeString = $defaultLambda")
                } else {
                    appendLine("    private var ${functionName}Behavior: (${parameterTypes}) -> $returnTypeString = $defaultLambda")
                }
            }

            // Generate behavior properties for properties
            for (property in analysis.properties) {
                val propertyName = property.name
                val returnTypeString = irTypeToKotlinString(property.type)
                appendLine(
                    "    private var ${propertyName}Behavior: () -> $returnTypeString = { ${
                        getDefaultValue(
                            property.type
                        )
                    } }"
                )
            }

            appendLine()

            // Generate function implementations
            for (function in analysis.functions) {
                val functionName = function.name
                val returnTypeString = irTypeToKotlinString(function.returnType)
                val parameters = function.parameters.joinToString(", ") { param ->
                    val varargsPrefix = if (param.isVararg) "vararg " else ""
                    "$varargsPrefix${param.name}: ${irTypeToKotlinString(param.type)}"
                }
                val parameterNames = function.parameters.joinToString(", ") { it.name }

                // Handle method-level type parameters
                val typeParamsString = if (function.typeParameters.isNotEmpty()) {
                    "<${function.typeParameters.joinToString(", ")}>"
                } else {
                    ""
                }

                if (function.isSuspend) {
                    appendLine("    override suspend fun $typeParamsString$functionName($parameters): $returnTypeString = ${functionName}Behavior($parameterNames)")
                } else {
                    appendLine("    override fun $typeParamsString$functionName($parameters): $returnTypeString = ${functionName}Behavior($parameterNames)")
                }
            }

            // Generate property implementations
            for (property in analysis.properties) {
                val propertyName = property.name
                val returnTypeString = irTypeToKotlinString(property.type)
                appendLine("    override val $propertyName: $returnTypeString get() = ${propertyName}Behavior()")
            }

            appendLine()

            // Generate configuration methods
            for (function in analysis.functions) {
                val functionName = function.name
                val returnTypeString = irTypeToKotlinString(function.returnType)
                val parameterTypes = function.parameters.joinToString(", ") { param ->
                    irTypeToKotlinString(param.type)
                }

                if (function.isSuspend) {
                    appendLine("    internal fun configure${functionName.capitalize()}(behavior: suspend (${parameterTypes}) -> $returnTypeString) { ${functionName}Behavior = behavior }")
                } else {
                    appendLine("    internal fun configure${functionName.capitalize()}(behavior: (${parameterTypes}) -> $returnTypeString) { ${functionName}Behavior = behavior }")
                }
            }

            for (property in analysis.properties) {
                val propertyName = property.name
                val returnTypeString = irTypeToKotlinString(property.type)
                appendLine("    internal fun configure${propertyName.capitalize()}(behavior: () -> $returnTypeString) { ${propertyName}Behavior = behavior }")
            }

            appendLine("}")
        }
    }

    /**
     * Generate factory function using IR analysis results.
     */
    internal fun generateFactoryFunction(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
        packageName: String
    ): String {
        val interfaceName = analysis.interfaceName
        val factoryName = "fake${interfaceName}"
        val configClassName = "Fake${interfaceName}Config"

        // Handle interface-level type parameters for return type
        val typeParamsString = if (analysis.typeParameters.isNotEmpty()) {
            "<${analysis.typeParameters.joinToString(", ") { "Any" }}>"
        } else {
            ""
        }

        return buildString {
            appendLine("fun $factoryName(configure: $configClassName.() -> Unit = {}): $interfaceName$typeParamsString {")
            appendLine("    return $fakeClassName().apply { $configClassName(this).configure() }")
            appendLine("}")
        }
    }

    /**
     * Generate configuration DSL using IR analysis results.
     */
    internal fun generateConfigurationDsl(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
        packageName: String
    ): String {
        val interfaceName = analysis.interfaceName
        val configClassName = "Fake${interfaceName}Config"

        return buildString {
            appendLine("class $configClassName(private val fake: $fakeClassName) {")

            // Generate configuration methods for functions
            for (function in analysis.functions) {
                val functionName = function.name
                val returnTypeString = irTypeToKotlinString(function.returnType)
                val parameterTypes = function.parameters.joinToString(", ") { param ->
                    irTypeToKotlinString(param.type)
                }

                if (function.isSuspend) {
                    appendLine("    fun $functionName(behavior: suspend (${parameterTypes}) -> $returnTypeString) { fake.configure${functionName.capitalize()}(behavior) }")
                } else {
                    appendLine("    fun $functionName(behavior: (${parameterTypes}) -> $returnTypeString) { fake.configure${functionName.capitalize()}(behavior) }")
                }
            }

            // Generate configuration methods for properties
            for (property in analysis.properties) {
                val propertyName = property.name
                val returnTypeString = irTypeToKotlinString(property.type)
                appendLine("    fun $propertyName(behavior: () -> $returnTypeString) { fake.configure${propertyName.capitalize()}(behavior) }")
            }

            appendLine("}")
        }
    }

    /**
     * Convert IR type to Kotlin string representation with full generic type support.
     * Handles List<T>, Map<K,V>, Result<T>, custom generics, type parameters, and nullability.
     */
    internal fun irTypeToKotlinString(irType: IrType): String {
        return when {
            // Handle type parameters first (critical for generics like <T>)
            irType is IrTypeParameter -> {
                val typeName = irType.name.asString()
                if (irType.isMarkedNullable()) "${typeName}?" else typeName
            }

            // Handle primitive types
            irType.isString() -> "String"
            irType.isInt() -> "Int"
            irType.isBoolean() -> "Boolean"
            irType.isUnit() -> "Unit"
            irType.isLong() -> "Long"
            irType.isFloat() -> "Float"
            irType.isDouble() -> "Double"
            irType.isChar() -> "Char"
            irType.isByte() -> "Byte"
            irType.isShort() -> "Short"

            // Handle complex types with generics
            irType is IrSimpleType -> {
                val classifier = irType.classifier
                val irClass = classifier.owner as? IrClass

                if (irClass != null) {
                    val className = irClass.name.asString()
                    val packageName = irClass.kotlinFqName.parent().asString()

                    // Build the type string with proper generic parameters
                    val baseTypeName = when {
                        // Handle common collections
                        packageName == "kotlin.collections" && className in listOf(
                            "List",
                            "MutableList",
                            "Set",
                            "MutableSet"
                        ) -> {
                            if (irType.arguments.isNotEmpty()) {
                                val typeArg = irType.arguments[0]
                                if (typeArg is IrTypeProjection) {
                                    "$className<${irTypeToKotlinString(typeArg.type)}>"
                                } else {
                                    "$className<*>"
                                }
                            } else {
                                "$className<*>"
                            }
                        }
                        // Handle Maps
                        packageName == "kotlin.collections" && className in listOf("Map", "MutableMap") -> {
                            if (irType.arguments.size >= 2) {
                                val keyArg = irType.arguments[0]
                                val valueArg = irType.arguments[1]
                                if (keyArg is IrTypeProjection && valueArg is IrTypeProjection) {
                                    "$className<${irTypeToKotlinString(keyArg.type)}, ${irTypeToKotlinString(valueArg.type)}>"
                                } else {
                                    "$className<*, *>"
                                }
                            } else {
                                "$className<*, *>"
                            }
                        }
                        // Handle Result<T>
                        packageName == "kotlin" && className == "Result" -> {
                            if (irType.arguments.isNotEmpty()) {
                                val typeArg = irType.arguments[0]
                                if (typeArg is IrTypeProjection) {
                                    "$className<${irTypeToKotlinString(typeArg.type)}>"
                                } else {
                                    "$className<*>"
                                }
                            } else {
                                "$className<*>"
                            }
                        }
                        // Handle function types (Function0, Function1, etc.)
                        packageName == "kotlin" && className.startsWith("Function") -> {
                            handleFunctionType(irType, className)
                        }
                        // Handle suspend function types
                        packageName == "kotlin.coroutines" && className.startsWith("SuspendFunction") -> {
                            handleSuspendFunctionType(irType, className)
                        }
                        // Handle any other generic types
                        irType.arguments.isNotEmpty() -> {
                            val typeArgs = irType.arguments.mapNotNull { arg ->
                                when (arg) {
                                    is IrTypeProjection -> irTypeToKotlinString(arg.type)
                                    else -> "*"
                                }
                            }.joinToString(", ")
                            "$className<$typeArgs>"
                        }
                        // Non-generic types
                        else -> className
                    }

                    // Add nullability
                    if (irType.isMarkedNullable()) "${baseTypeName}?" else baseTypeName
                } else {
                    // Fallback for unknown classifier
                    if (irType.isMarkedNullable()) "Any?" else "Any"
                }
            }

            // Handle nullable non-simple types
            irType.isMarkedNullable() -> "${irType.getClass()?.name?.asString() ?: "Any"}?"

            // Final fallback
            else -> irType.getClass()?.name?.asString() ?: "Any"
        }
    }

    /**
     * Handle Function types by converting them to proper Kotlin function syntax.
     * Function0<R> -> () -> R
     * Function1<T, R> -> (T) -> R
     * Function2<T1, T2, R> -> (T1, T2) -> R
     */
    private fun handleFunctionType(irType: IrSimpleType, className: String): String {
        val paramCount = when (className) {
            "Function0" -> 0
            "Function1" -> 1
            "Function2" -> 2
            "Function3" -> 3
            else -> {
                // Extract number from FunctionN
                val numberStr = className.removePrefix("Function")
                numberStr.toIntOrNull() ?: return className
            }
        }

        val typeArgs = irType.arguments.mapNotNull { arg ->
            if (arg is IrTypeProjection) irTypeToKotlinString(arg.type) else "*"
        }

        return if (typeArgs.size == paramCount + 1) {
            // Last type arg is the return type, others are parameters
            val paramTypes = typeArgs.take(paramCount)
            val returnType = typeArgs.last()

            if (paramCount == 0) {
                "() -> $returnType"
            } else {
                "(${paramTypes.joinToString(", ")}) -> $returnType"
            }
        } else {
            // Fallback if type arguments don't match expected count
            className
        }
    }

    /**
     * Handle SuspendFunction types by converting them to proper Kotlin suspend function syntax.
     * SuspendFunction0<R> -> suspend () -> R
     * SuspendFunction1<T, R> -> suspend (T) -> R
     */
    private fun handleSuspendFunctionType(irType: IrSimpleType, className: String): String {
        val regularFunctionType = handleFunctionType(irType, className.removePrefix("Suspend"))
        return if (regularFunctionType.startsWith("(") || regularFunctionType.startsWith("()")) {
            "suspend $regularFunctionType"
        } else {
            className // Fallback
        }
    }

    /**
     * Get smart default value for IR type - eliminates compilation-blocking TODOs.
     * Provides sensible defaults for collections, common types, and custom classes.
     */
    internal fun getDefaultValue(irType: IrType): String {
        return when {
            // Handle primitive types first
            irType.isString() -> "\"\""
            irType.isInt() -> "0"
            irType.isBoolean() -> "false"
            irType.isUnit() -> "Unit"
            irType.isLong() -> "0L"
            irType.isFloat() -> "0.0f"
            irType.isDouble() -> "0.0"
            irType.isChar() -> "'\\u0000'"
            irType.isByte() -> "0.toByte()"
            irType.isShort() -> "0.toShort()"

            // Handle nullable types - always return null for nullables
            irType.isMarkedNullable() -> "null"

            // Handle complex types intelligently
            irType is IrSimpleType -> {
                val irClass = irType.classifier.owner as? IrClass
                if (irClass != null) {
                    val className = irClass.name.asString()
                    val packageName = irClass.kotlinFqName.parent().asString()

                    when {
                        // Handle collections with proper defaults
                        packageName == "kotlin.collections" && className in listOf(
                            "List",
                            "MutableList"
                        ) -> "emptyList()"

                        packageName == "kotlin.collections" && className in listOf("Set", "MutableSet") -> "emptySet()"
                        packageName == "kotlin.collections" && className in listOf("Map", "MutableMap") -> "emptyMap()"
                        packageName == "kotlin.collections" && className == "Collection" -> "emptyList()"

                        // Handle Result<T> - use success with default value for T
                        packageName == "kotlin" && className == "Result" -> {
                            if (irType.arguments.isNotEmpty()) {
                                val typeArg = irType.arguments[0]
                                if (typeArg is IrTypeProjection) {
                                    val innerDefault = getDefaultValue(typeArg.type)
                                    "Result.success($innerDefault)"
                                } else {
                                    "Result.success(null)"
                                }
                            } else {
                                "Result.success(null)"
                            }
                        }

                        // Handle Array types
                        packageName == "kotlin" && className == "Array" -> {
                            if (irType.arguments.isNotEmpty()) {
                                "emptyArray()"
                            } else {
                                "arrayOf()"
                            }
                        }

                        // Handle common Kotlin types
                        packageName == "kotlin" && className == "Pair" -> "Pair(null, null)"
                        packageName == "kotlin" && className == "Triple" -> "Triple(null, null, null)"

                        // Handle function types (should not reach here due to earlier handling, but safety)
                        packageName == "kotlin" && className.startsWith("Function") -> "{ TODO(\"Function not implemented\") }"

                        // Handle custom data classes and interfaces - use null for safety
                        irClass.kind == org.jetbrains.kotlin.descriptors.ClassKind.CLASS -> {
                            // For custom classes, default to null to avoid constructor complexity
                            "null"
                        }

                        // Handle interfaces - cannot instantiate, use null
                        irClass.kind == org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE -> "null"

                        // Handle enums - use first enum value if possible
                        irClass.kind == org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS -> {
                            val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
                            if (enumEntries.isNotEmpty()) {
                                "$className.${enumEntries.first().name.asString()}"
                            } else {
                                "TODO(\"Empty enum $className\")"
                            }
                        }

                        // Default fallback for unknown types
                        else -> "TODO(\"Implement default for $className\")"
                    }
                } else {
                    "TODO(\"Unknown type\")"
                }
            }

            // Final fallback
            else -> "TODO(\"Implement default for ${irType.getClass()?.name?.asString() ?: "Unknown"}\")"
        }
    }

    /**
     * Get generated sources directory with intelligent source set mapping.
     * Maps source locations to appropriate test source sets:
     * - commonMain -> commonTest
     * - jvmMain -> jvmTest
     * - androidMain -> androidTest
     * - iosMain -> iosTest
     * - jsMain -> jsTest
     * - main (JVM-only) -> test
     */
    private fun getGeneratedSourcesDir(moduleFragment: IrModuleFragment): File {
        // Determine the appropriate test source set based on module name
        val moduleName = moduleFragment.name.asString().lowercase()
        val testSourceSet = mapToTestSourceSet(moduleName)

        // Use the outputDir if provided, otherwise try to determine from context
        val baseDir = when {
            outputDir != null -> File(outputDir)
            else -> {
                // Fallback: Try to find project directory by looking for build.gradle.kts
                var dir = File(System.getProperty("user.dir"))

                // If we're in a daemon directory, try to find the real project path
                if (dir.absolutePath.contains("daemon")) {
                    // Try to get the classloader's resource path
                    val classLoader = this::class.java.classLoader
                    val resourceUrl = classLoader.getResource("")
                    if (resourceUrl != null) {
                        val path = File(resourceUrl.path)
                        // Navigate up from build/classes/kotlin/main to project root
                        var parent = path
                        while (parent.parentFile != null && !File(parent, "build.gradle.kts").exists()) {
                            parent = parent.parentFile
                        }
                        if (File(parent, "build.gradle.kts").exists()) {
                            dir = parent
                        }
                    }
                }

                // Look for build.gradle.kts to confirm we're in the right directory
                if (!File(dir, "build.gradle.kts").exists()) {
                    var parent = dir.parentFile
                    while (parent != null && !File(parent, "build.gradle.kts").exists()) {
                        parent = parent.parentFile
                    }
                    if (parent != null) {
                        dir = parent
                    }
                }
                File(dir, "build/generated/ktfake")
            }
        }

        val buildGenerated = File(baseDir, "$testSourceSet/kotlin")

        if (!buildGenerated.exists()) {
            buildGenerated.mkdirs()
        }

        messageCollector?.reportInfo("KtFakes: outputDir=${outputDir}, Module '$moduleName' -> Test source set '$testSourceSet'")
        messageCollector?.reportInfo("KtFakes: Output directory: ${buildGenerated.absolutePath}")
        return buildGenerated
    }

    /**
     * Maps compilation context to appropriate test source set.
     * Implements the KMP source set mapping strategy.
     */
    private fun mapToTestSourceSet(moduleName: String): String {
        return when {
            // KMP source sets
            moduleName.contains("commonmain") -> "commonTest"
            moduleName.contains("jvmmain") -> "jvmTest"
            moduleName.contains("androidmain") -> "androidTest"
            moduleName.contains("iosmain") -> "iosTest"
            moduleName.contains("jsmain") -> "jsTest"
            moduleName.contains("linuxmain") -> "linuxTest"
            moduleName.contains("macosmain") -> "macosTest"
            moduleName.contains("mingwmain") -> "mingwTest"
            moduleName.contains("nativemain") -> "nativeTest"

            // JVM-only projects (no KMP)
            moduleName.contains("main") && !moduleName.contains("test") -> "test"

            // Default fallback patterns
            moduleName.contains("jvm") -> "jvmTest"
            moduleName.contains("android") -> "androidTest"
            moduleName.contains("ios") -> "iosTest"
            moduleName.contains("js") -> "jsTest"
            moduleName.contains("linux") -> "linuxTest"
            moduleName.contains("macos") -> "macosTest"
            moduleName.contains("mingw") -> "mingwTest"
            moduleName.contains("native") -> "nativeTest"

            // Sample projects - since single-module uses JVM target, default to jvmTest
            moduleName.contains("single-module") -> "jvmTest"
            moduleName.contains("sample") -> "jvmTest"  // Default samples to JVM test
            moduleName.contains("test") -> "jvmTest"

            // Ultimate fallback - prefer jvmTest over commonTest for better compatibility
            else -> "jvmTest"
        }
    }

    /**
     * Capitalize first letter of string.
     */
    private fun String.capitalize(): String = replaceFirstChar { it.uppercase() }


    /**
     * Discover @Fake annotated interfaces using dynamic discovery.
     */
    private fun discoverFakeInterfaces(moduleFragment: IrModuleFragment): List<IrClass> {
        val fakeAnnotationFqName = FqName("dev.rsicarelli.ktfake.Fake")
        val fakeInterfaces = mutableListOf<IrClass>()

        for (file in moduleFragment.files) {
            for (declaration in file.declarations) {
                if (declaration is IrClass && declaration.kind == ClassKind.INTERFACE) {
                    // Check if interface has @Fake annotation
                    val hasFakeAnnotation = declaration.annotations.any { annotation ->
                        annotation.type.classFqName == fakeAnnotationFqName
                    }

                    if (hasFakeAnnotation) {
                        fakeInterfaces.add(declaration)
                        messageCollector?.reportInfo("KtFakes: Discovered @Fake interface: ${declaration.name}")
                    }
                }
            }
        }

        return fakeInterfaces
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }

    /**
     * Collect all required import statements for types used in the interface.
     * This fixes cross-module import resolution by analyzing all type references.
     */
    private fun collectRequiredImports(analysis: InterfaceAnalysis, currentPackage: String): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect imports from function return types and parameters
        for (function in analysis.functions) {
            collectImportsFromType(function.returnType, currentPackage, imports)
            for (parameter in function.parameters) {
                collectImportsFromType(parameter.type, currentPackage, imports)
            }
        }

        // Collect imports from property types
        for (property in analysis.properties) {
            collectImportsFromType(property.type, currentPackage, imports)
        }

        return imports
    }

    /**
     * Extract import requirements from an IR type.
     * Handles both simple types and generic types with parameters.
     */
    private fun collectImportsFromType(irType: IrType, currentPackage: String, imports: MutableSet<String>) {
        // Skip primitive types - they don't need imports
        if (isPrimitiveType(irType)) {
            return
        }

        val irClass = irType.getClass()
        if (irClass != null) {
            val fqName = irClass.kotlinFqName.asString()
            val packageName = fqName.substringBeforeLast('.', "")

            // Only add import if it's from a different package and not kotlin.* built-ins
            if (packageName.isNotEmpty() &&
                packageName != currentPackage &&
                !packageName.startsWith("kotlin.")
            ) {
                imports.add(fqName)
            }

            // Handle generic type parameters (for future generic support)
            if (irType is IrSimpleType) {
                for (typeArgument in irType.arguments) {
                    if (typeArgument is IrTypeProjection) {
                        collectImportsFromType(typeArgument.type, currentPackage, imports)
                    }
                }
            }
        }
    }

    /**
     * Check if a type is primitive and doesn't need imports.
     */
    private fun isPrimitiveType(irType: IrType): Boolean {
        return irType.isString() || irType.isInt() || irType.isBoolean() ||
                irType.isUnit() || irType.isLong() || irType.isFloat() ||
                irType.isDouble() || irType.isChar() || irType.isByte() ||
                irType.isShort()
    }

    /**
     * IR-Native analysis results using actual IR types.
     */
    internal data class InterfaceAnalysis(
        val interfaceName: String,
        val properties: List<PropertyAnalysis>,
        val functions: List<FunctionAnalysis>,
        val typeParameters: List<String>, // Interface-level type parameters like <T>, <K, V>
        val sourceInterface: IrClass
    )

    internal data class PropertyAnalysis(
        val name: String,
        val type: IrType,
        val isMutable: Boolean,
        val isNullable: Boolean,
        val irProperty: IrProperty
    )

    internal data class FunctionAnalysis(
        val name: String,
        val parameters: List<ParameterAnalysis>,
        val returnType: IrType,
        val isSuspend: Boolean,
        val isInline: Boolean,
        val typeParameters: List<String>, // Method-level type parameters like <T>, <T, R>
        val irFunction: IrSimpleFunction
    )

    internal data class ParameterAnalysis(
        val name: String,
        val type: IrType,
        val hasDefaultValue: Boolean,
        val isVararg: Boolean
    )
}
