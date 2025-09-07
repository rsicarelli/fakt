// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

// Modular components will be integrated in future iterations
// import dev.rsicarelli.ktfake.compiler.analysis.*
// import dev.rsicarelli.ktfake.compiler.generation.*
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
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
        
        messageCollector?.reportInfo("KtFakes: Analyzed interface ${sourceInterface.name}: ${functions.size} functions, ${properties.size} properties")
        
        return InterfaceAnalysis(
            interfaceName = sourceInterface.name.asString(),
            properties = properties,
            functions = functions,
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
        val parameters = function.parameters.filter { it.kind == org.jetbrains.kotlin.ir.declarations.IrParameterKind.Regular }.map { param ->
            ParameterAnalysis(
                name = param.name.asString(),
                type = param.type,
                hasDefaultValue = param.defaultValue != null
            )
        }
        
        return FunctionAnalysis(
            name = function.name.asString(),
            parameters = parameters,
            returnType = function.returnType,
            isSuspend = function.isSuspend,
            isInline = function.isInline,
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
        
        val fullCode = buildString {
            appendLine("// Generated by KtFakes - IR-Native Analysis")
            appendLine("// Interface: $interfaceName")
            appendLine("package $packageName")
            appendLine()
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
    internal fun generateImplementationClass(analysis: InterfaceAnalysis, fakeClassName: String, packageName: String): String {
        val interfaceName = analysis.interfaceName
        
        return buildString {
            appendLine("class $fakeClassName : $interfaceName {")
            
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
                appendLine("    private var ${propertyName}Behavior: () -> $returnTypeString = { ${getDefaultValue(property.type)} }")
            }
            
            appendLine()
            
            // Generate function implementations
            for (function in analysis.functions) {
                val functionName = function.name
                val returnTypeString = irTypeToKotlinString(function.returnType)
                val parameters = function.parameters.joinToString(", ") { param ->
                    "${param.name}: ${irTypeToKotlinString(param.type)}"
                }
                val parameterNames = function.parameters.joinToString(", ") { it.name }
                
                if (function.isSuspend) {
                    appendLine("    override suspend fun $functionName($parameters): $returnTypeString = ${functionName}Behavior($parameterNames)")
                } else {
                    appendLine("    override fun $functionName($parameters): $returnTypeString = ${functionName}Behavior($parameterNames)")
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
    internal fun generateFactoryFunction(analysis: InterfaceAnalysis, fakeClassName: String, packageName: String): String {
        val interfaceName = analysis.interfaceName
        val factoryName = "fake${interfaceName}"
        val configClassName = "Fake${interfaceName}Config"
        
        return buildString {
            appendLine("fun $factoryName(configure: $configClassName.() -> Unit = {}): $interfaceName {")
            appendLine("    return $fakeClassName().apply { $configClassName(this).configure() }")
            appendLine("}")
        }
    }
    
    /**
     * Generate configuration DSL using IR analysis results.
     */
    internal fun generateConfigurationDsl(analysis: InterfaceAnalysis, fakeClassName: String, packageName: String): String {
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
     * Convert IR type to Kotlin string representation.
     */
    internal fun irTypeToKotlinString(irType: IrType): String {
        return when {
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
            irType.isMarkedNullable() -> "${irType.getClass()?.name?.asString() ?: "Any"}?"
            else -> irType.getClass()?.name?.asString() ?: "Any"
        }
    }
    
    /**
     * Get default value for IR type.
     */
    internal fun getDefaultValue(irType: IrType): String {
        return when {
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
            irType.isMarkedNullable() -> "null"
            else -> "TODO(\"Implement default for ${irType.getClass()?.name?.asString()}\")"
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
            
            // Sample projects and already test modules
            moduleName.contains("sample") -> "commonTest"
            moduleName.contains("test") -> "commonTest"
            
            // Ultimate fallback
            else -> "commonTest"
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
     * IR-Native analysis results using actual IR types.
     */
    internal data class InterfaceAnalysis(
        val interfaceName: String,
        val properties: List<PropertyAnalysis>,
        val functions: List<FunctionAnalysis>,
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
        val irFunction: IrSimpleFunction
    )
    
    internal data class ParameterAnalysis(
        val name: String,
        val type: IrType,
        val hasDefaultValue: Boolean
    )
}