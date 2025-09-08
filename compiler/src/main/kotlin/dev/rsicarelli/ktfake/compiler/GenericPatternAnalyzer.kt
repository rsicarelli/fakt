// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import kotlin.reflect.KClass

/**
 * Core pattern analyzer for KtFakes compile-time type safety.
 * 
 * Analyzes interfaces to determine the optimal code generation strategy:
 * - Class-level generics → Generate truly generic fake classes
 * - Method-level generics → Generate specialized handlers  
 * - Mixed generics → Generate hybrid approach
 * - No generics → Use existing simple generation
 */
@OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)
@Suppress("DEPRECATION")
class GenericPatternAnalyzer {

    /**
     * Analyze an interface to determine its generic pattern and optimal generation strategy.
     */
    fun analyzeInterface(irClass: IrClass): GenericPattern {
        val classTypeParams = irClass.typeParameters
        val methodTypeParams = extractMethodTypeParameters(irClass)
        val usageAnalysis = analyzeUsagePatterns(irClass)
        
        return when {
            // Simple case: no generics at all
            classTypeParams.isEmpty() && methodTypeParams.isEmpty() -> {
                GenericPattern.NoGenerics
            }
            
            // Class-level generics only - perfect for generic class generation
            classTypeParams.isNotEmpty() && methodTypeParams.isEmpty() -> {
                GenericPattern.ClassLevelGenerics(
                    typeParameters = classTypeParams,
                    constraints = extractTypeConstraints(classTypeParams)
                )
            }
            
            // Method-level generics only - use specialized handlers
            classTypeParams.isEmpty() && methodTypeParams.isNotEmpty() -> {
                GenericPattern.MethodLevelGenerics(
                    genericMethods = methodTypeParams,
                    detectedTypes = usageAnalysis.concreteTypes,
                    transformationPatterns = usageAnalysis.transformations
                )
            }
            
            // Mixed: both class and method level generics
            else -> {
                GenericPattern.MixedGenerics(
                    classTypeParameters = classTypeParams,
                    classConstraints = extractTypeConstraints(classTypeParams),
                    genericMethods = methodTypeParams,
                    detectedTypes = usageAnalysis.concreteTypes,
                    transformationPatterns = usageAnalysis.transformations
                )
            }
        }
    }

    /**
     * Extract method-level type parameters from all functions in the interface.
     */
    private fun extractMethodTypeParameters(irClass: IrClass): List<GenericMethod> {
        return irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.typeParameters.isNotEmpty() }
            .map { function ->
                GenericMethod(
                    name = function.name.asString(),
                    typeParameters = function.typeParameters,
                    constraints = extractTypeConstraints(function.typeParameters),
                    parameters = emptyList(), // TODO: Implement parameter extraction without deprecated APIs
                    returnType = function.returnType,
                    isSuspend = function.isSuspend
                )
            }
    }

    /**
     * Extract type constraints (where clauses) from type parameters.
     */
    private fun extractTypeConstraints(typeParameters: List<IrTypeParameter>): List<TypeConstraint> {
        return typeParameters.flatMap { typeParam ->
            typeParam.superTypes.map { superType ->
                TypeConstraint(
                    typeParameter = typeParam.name.asString(),
                    constraint = irTypeToString(superType),
                    constraintType = superType
                )
            }
        }
    }

    /**
     * Analyze usage patterns to detect which concrete types are used with generic methods.
     * This performs intelligent analysis to determine optimal code generation strategies.
     */
    private fun analyzeUsagePatterns(irClass: IrClass): UsageAnalysis {
        val detectedTypes = mutableSetOf<String>()
        val transformations = mutableListOf<TransformationPattern>()
        
        // Step 1: Analyze method signatures for type hints
        irClass.declarations.filterIsInstance<IrSimpleFunction>().forEach { function ->
            // TODO: Extract parameter type hints without deprecated APIs
            
            val returnTypeHints = extractTypeHints(function.returnType)
            detectedTypes.addAll(returnTypeHints)
        }
        
        // Step 2: Detect transformation patterns from method signatures
        irClass.declarations.filterIsInstance<IrSimpleFunction>().forEach { function ->
            if (function.typeParameters.isNotEmpty()) {
                val patterns = detectTransformationPatterns(function)
                transformations.addAll(patterns)
            }
        }
        
        // Step 3: Add common types based on interface context
        val contextTypes = detectContextualTypes(irClass)
        detectedTypes.addAll(contextTypes)
        
        // Step 4: Merge with baseline common types
        detectedTypes.addAll(detectCommonTypes())
        
        return UsageAnalysis(
            concreteTypes = detectedTypes,
            transformations = transformations
        )
    }
    
    /**
     * Extract type hints from IrType to detect concrete usage patterns.
     */
    private fun extractTypeHints(irType: IrType): Set<String> {
        val hints = mutableSetOf<String>()
        
        when {
            irType is IrSimpleType -> {
                // Extract class name if it's a concrete type
                val classifier = irType.classifier
                if (classifier !is IrTypeParameterSymbol) {
                    val typeName = irTypeToString(irType)
                    if (!typeName.contains("kotlin.") || 
                        typeName in setOf("kotlin.String", "kotlin.Int", "kotlin.Long", "kotlin.Boolean")) {
                        hints.add(typeName)
                    }
                }
                
                // Recursively analyze type arguments
                irType.arguments.forEach { typeArgument ->
                    if (typeArgument is IrType) {
                        hints.addAll(extractTypeHints(typeArgument))
                    }
                }
            }
        }
        
        return hints
    }
    
    /**
     * Detect transformation patterns from method signatures.
     */
    private fun detectTransformationPatterns(function: IrSimpleFunction): List<TransformationPattern> {
        val patterns = mutableListOf<TransformationPattern>()
        
        // Look for methods with pattern like: <T, R> transform(T) -> R
        if (function.typeParameters.size >= 2) {
            val typeParams = function.typeParameters.map { it.name.asString() }
            // TODO: Extract input types without deprecated parameter APIs
            val inputTypes = emptyList<String>()
            val outputType = extractTypeParameterUsage(function.returnType, typeParams)
            
            // Create transformation patterns for common naming patterns
            inputTypes.forEach { inputType ->
                outputType?.let { output ->
                    if (inputType != output) {
                        patterns.add(TransformationPattern(inputType, output))
                    }
                }
            }
        }
        
        return patterns
    }
    
    /**
     * Extract which type parameter is being used in an IrType.
     */
    private fun extractTypeParameterUsage(irType: IrType, typeParams: List<String>): String? {
        return when {
            irType is IrSimpleType && irType.classifier is IrTypeParameterSymbol -> {
                val typeParam = irType.classifier as IrTypeParameterSymbol
                val paramName = typeParam.owner.name.asString()
                if (paramName in typeParams) paramName else null
            }
            else -> null
        }
    }
    
    /**
     * Detect contextual types based on interface naming and structure.
     */
    private fun detectContextualTypes(irClass: IrClass): Set<String> {
        val contextTypes = mutableSetOf<String>()
        val className = irClass.name.asString()
        
        // Analyze interface name for domain hints
        when {
            className.contains("User", ignoreCase = true) -> {
                contextTypes.addAll(setOf("User", "UserDto", "UserProfile"))
            }
            className.contains("Order", ignoreCase = true) -> {
                contextTypes.addAll(setOf("Order", "OrderItem", "OrderSummary"))
            }
            className.contains("Product", ignoreCase = true) -> {
                contextTypes.addAll(setOf("Product", "ProductDto", "ProductInfo"))
            }
            className.contains("Repository", ignoreCase = true) -> {
                contextTypes.addAll(setOf("Entity", "Long", "String"))
            }
            className.contains("Service", ignoreCase = true) -> {
                contextTypes.addAll(setOf("Request", "Response", "Result"))
            }
            className.contains("Cache", ignoreCase = true) -> {
                contextTypes.addAll(setOf("String", "Int", "CacheKey", "CacheValue"))
            }
        }
        
        return contextTypes
    }

    /**
     * Detect common concrete types that are likely to be used with generic methods.
     * This is a starting point - will be enhanced with real usage analysis.
     */
    internal fun detectCommonTypes(): Set<String> {
        // Start with commonly used types in Kotlin projects
        return setOf(
            "kotlin.String",
            "kotlin.Int", 
            "kotlin.Long",
            "kotlin.Boolean",
            "kotlin.collections.List",
            "kotlin.collections.Map",
            "kotlin.collections.Set",
            // Add more based on project analysis
            "User", // Common domain type
            "Order", // Common domain type  
            "Product", // Common domain type
            "Entity" // Common base type
        )
    }

    /**
     * Detect common transformation patterns (T -> R mappings) based on naming conventions.
     */
    internal fun detectCommonTransformations(): List<TransformationPattern> {
        val baseTypes = setOf("User", "Order", "Product", "Entity", "Item", "Record")
        val patterns = mutableListOf<TransformationPattern>()
        
        // Common transformation patterns in enterprise applications
        baseTypes.forEach { baseType ->
            patterns.addAll(listOf(
                // Entity to DTO patterns
                TransformationPattern(baseType, "${baseType}Dto"),
                TransformationPattern(baseType, "${baseType}Response"),
                TransformationPattern(baseType, "${baseType}Summary"),
                TransformationPattern(baseType, "${baseType}View"),
                TransformationPattern(baseType, "${baseType}Info"),
                
                // Request to entity patterns
                TransformationPattern("${baseType}Request", baseType),
                TransformationPattern("Create${baseType}Request", baseType),
                TransformationPattern("Update${baseType}Request", baseType),
                
                // Collection transformations
                TransformationPattern("List<$baseType>", "List<${baseType}Dto>"),
                TransformationPattern("Set<$baseType>", "Set<${baseType}Summary>"),
                
                // Async patterns
                TransformationPattern(baseType, "Result<$baseType>"),
                TransformationPattern(baseType, "CompletableFuture<$baseType>"),
                TransformationPattern(baseType, "Flow<$baseType>")
            ))
        }
        
        // Generic patterns
        patterns.addAll(listOf(
            TransformationPattern("T", "Result<T>"),
            TransformationPattern("T", "Optional<T>"),
            TransformationPattern("T", "List<T>"),
            TransformationPattern("List<T>", "T"),
            TransformationPattern("T", "CompletableFuture<T>")
        ))
        
        return patterns
    }

    /**
     * Convert IrType to string representation for analysis.
     */
    private fun irTypeToString(irType: IrType): String {
        return when {
            irType is IrSimpleType && irType.classifier is IrTypeParameterSymbol -> {
                val typeParam = irType.classifier as IrTypeParameterSymbol
                val paramName = typeParam.owner.name.asString()
                if (irType.isMarkedNullable()) "${paramName}?" else paramName
            }
            irType is IrSimpleType -> {
                // Build full qualified name with type arguments
                val classifier = irType.classifier
                val baseName = classifier.toString()
                    .substringAfterLast('/')
                    .substringAfterLast('.')
                
                val typeArguments = if (irType.arguments.isNotEmpty()) {
                    val args = irType.arguments.mapNotNull { arg ->
                        when (arg) {
                            is IrType -> irTypeToString(arg)
                            else -> null
                        }
                    }
                    if (args.isNotEmpty()) "<${args.joinToString(", ")}>" else ""
                } else ""
                
                val fullType = baseName + typeArguments
                if (irType.isMarkedNullable()) "${fullType}?" else fullType
            }
            else -> {
                // Fallback to toString for other types
                val typeString = irType.toString()
                // Clean up common IR type representations
                typeString.substringAfterLast('/')
                    .substringAfterLast('.')
                    .replace("IrClass", "")
            }
        }
    }
    
    /**
     * Validate the analyzed pattern for consistency and completeness.
     */
    fun validatePattern(pattern: GenericPattern, irClass: IrClass): List<String> {
        val warnings = mutableListOf<String>()
        
        when (pattern) {
            is GenericPattern.ClassLevelGenerics -> {
                if (pattern.typeParameters.isEmpty()) {
                    warnings.add("ClassLevelGenerics pattern has no type parameters")
                }
                
                // Validate constraints are properly extracted
                pattern.constraints.forEach { constraint ->
                    if (constraint.constraint.isBlank()) {
                        warnings.add("Empty constraint found for type parameter ${constraint.typeParameter}")
                    }
                }
            }
            
            is GenericPattern.MethodLevelGenerics -> {
                if (pattern.genericMethods.isEmpty()) {
                    warnings.add("MethodLevelGenerics pattern has no generic methods")
                }
                
                if (pattern.detectedTypes.isEmpty()) {
                    warnings.add("No concrete types detected for method-level generics")
                }
            }
            
            is GenericPattern.MixedGenerics -> {
                if (pattern.classTypeParameters.isEmpty() && pattern.genericMethods.isEmpty()) {
                    warnings.add("MixedGenerics pattern has neither class nor method generics")
                }
            }
            
            GenericPattern.NoGenerics -> {
                // Verify there really are no generics
                val hasClassGenerics = irClass.typeParameters.isNotEmpty()
                val hasMethodGenerics = irClass.declarations
                    .filterIsInstance<IrSimpleFunction>()
                    .any { it.typeParameters.isNotEmpty() }
                
                if (hasClassGenerics || hasMethodGenerics) {
                    warnings.add("Interface has generics but classified as NoGenerics")
                }
            }
        }
        
        return warnings
    }
    
    /**
     * Get a summary of the analysis results for debugging.
     */
    fun getAnalysisSummary(pattern: GenericPattern): String {
        return when (pattern) {
            GenericPattern.NoGenerics -> 
                "No generic parameters detected - using simple generation"
                
            is GenericPattern.ClassLevelGenerics ->
                "Class-level generics: ${pattern.typeParameters.size} type parameters, " +
                "${pattern.constraints.size} constraints"
                
            is GenericPattern.MethodLevelGenerics ->
                "Method-level generics: ${pattern.genericMethods.size} generic methods, " +
                "${pattern.detectedTypes.size} detected types, " +
                "${pattern.transformationPatterns.size} transformation patterns"
                
            is GenericPattern.MixedGenerics ->
                "Mixed generics: ${pattern.classTypeParameters.size} class type parameters, " +
                "${pattern.genericMethods.size} generic methods, " +
                "${pattern.detectedTypes.size} detected types"
        }
    }
}

/**
 * Sealed class representing different generic patterns found in interfaces.
 */
sealed class GenericPattern {
    /**
     * Interface has no generic parameters at all.
     * Use existing simple generation approach.
     */
    object NoGenerics : GenericPattern()

    /**
     * Interface has generic parameters at class level only.
     * Generate truly generic fake class with full type safety.
     */
    data class ClassLevelGenerics(
        val typeParameters: List<IrTypeParameter>,
        val constraints: List<TypeConstraint>
    ) : GenericPattern()

    /**
     * Interface has generic parameters at method level only.
     * Generate specialized handlers for detected concrete types.
     */
    data class MethodLevelGenerics(
        val genericMethods: List<GenericMethod>,
        val detectedTypes: Set<String>,
        val transformationPatterns: List<TransformationPattern>
    ) : GenericPattern()

    /**
     * Interface has both class-level and method-level generics.
     * Generate hybrid approach combining both strategies.
     */
    data class MixedGenerics(
        val classTypeParameters: List<IrTypeParameter>,
        val classConstraints: List<TypeConstraint>,
        val genericMethods: List<GenericMethod>,
        val detectedTypes: Set<String>,
        val transformationPatterns: List<TransformationPattern>
    ) : GenericPattern()
}

/**
 * Represents a generic method with its type parameters and constraints.
 */
data class GenericMethod(
    val name: String,
    val typeParameters: List<IrTypeParameter>,
    val constraints: List<TypeConstraint>,
    val parameters: List<MethodParameter>,
    val returnType: IrType,
    val isSuspend: Boolean
)

/**
 * Represents a method parameter.
 */
data class MethodParameter(
    val name: String,
    val type: IrType,
    val isVararg: Boolean
)

/**
 * Represents a type constraint (where clause).
 */
data class TypeConstraint(
    val typeParameter: String,
    val constraint: String,
    val constraintType: IrType
)

/**
 * Result of usage pattern analysis.
 */
data class UsageAnalysis(
    val concreteTypes: Set<String>,
    val transformations: List<TransformationPattern>
)

/**
 * Represents a common transformation pattern (T -> R).
 */
data class TransformationPattern(
    val inputType: String,
    val outputType: String
)