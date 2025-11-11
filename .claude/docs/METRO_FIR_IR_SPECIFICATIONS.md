# Metro FIR + IR Architecture: Technical Specifications and Kotlin Compiler Integration

> **Executive Summary**: Metro is a compile-time dependency injection framework that leverages Kotlin's K2 compiler architecture through FIR (Frontend IR) for analysis/validation and IR (Intermediate Representation) for code generation. This approach provides significant performance advantages over KSP-based solutions by avoiding source generation and integrating directly into the compiler pipeline.

---

## 1. Architecture Overview

### 1.1 Two-Phase Compilation Pipeline

Metro implements a sophisticated two-phase compilation approach that directly integrates with the Kotlin K2 compiler:

```
┌─────────────────────────────────────────────────────────────┐
│                    Metro Compiler Plugin                    │
├─────────────────────────────────────────────────────────────┤
│  Phase 1: FIR (Frontend IR) - Analysis & Validation        │
│  ├─ Declaration Generation                                  │
│  ├─ Supertype Generation                                    │
│  ├─ Type Analysis & Validation                              │
│  ├─ Diagnostic Reporting                                    │
│  └─ Metadata Preparation                                    │
├─────────────────────────────────────────────────────────────┤
│  Phase 2: IR (Intermediate Representation) - Code Gen      │
│  ├─ Dependency Graph Construction                           │
│  ├─ Binding Resolution                                      │
│  ├─ Factory Generation                                      │
│  ├─ Implementation Synthesis                                │
│  └─ Runtime Code Emission                                   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Entry Point: MetroCompilerPluginRegistrar

**File**: `compiler/src/main/kotlin/dev/zacsweers/metro/compiler/MetroCompilerPluginRegistrar.kt`

```kotlin
public class MetroCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean = true
  
  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    // Phase 1: FIR Extensions Registration
    FirExtensionRegistrarAdapter.registerExtension(
      MetroFirExtensionRegistrar(classIds, options)
    )
    
    // Phase 2: IR Generation Extension Registration  
    IrGenerationExtension.registerExtension(
      MetroIrGenerationExtension(
        messageCollector, classIds, options, lookupTracker, expectActualTracker
      )
    )
  }
}
```

**Key Integration Points:**
- **K2 Compatibility**: Explicitly declares support for K2 compiler architecture
- **Extension Registration**: Registers both FIR and IR extensions simultaneously
- **Configuration Management**: Loads Metro-specific options and passes them to both phases
- **Instrumentation**: Integrates with Kotlin's lookup tracker and expect/actual tracker for incremental compilation

---

## 2. Phase 1: FIR (Frontend IR) Integration

### 2.1 Purpose and Scope

The FIR phase is responsible for:
- **Declaration Generation**: Creating type signatures for generated classes/functions
- **Supertype Resolution**: Establishing inheritance relationships
- **Validation**: Performing compile-time dependency graph validation
- **Error Reporting**: Generating detailed diagnostic messages
- **Metadata Preparation**: Setting up data structures for IR phase

### 2.2 FIR Extension Registrar

**File**: `compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/MetroFirExtensionRegistrar.kt`

```kotlin
public class MetroFirExtensionRegistrar(
  private val classIds: ClassIds,
  private val options: MetroOptions,
) : FirExtensionRegistrar() {
  
  override fun ExtensionRegistrarContext.configurePlugin() {
    // Built-in types and annotations support
    +MetroFirBuiltIns.getFactory(classIds, options)
    
    // Compile-time validation and error checking
    +::MetroFirCheckers
    
    // Supertype generation for factory classes
    +supertypeGenerator("Supertypes - graph factory", ::GraphFactoryFirSupertypeGenerator)
    +supertypeGenerator("Supertypes - contributed interfaces", ::ContributedInterfaceSupertypeGenerator)
    +supertypeGenerator("Supertypes - provider factories", ::ProvidesFactorySupertypeGenerator)
    
    // Declaration generation for various Metro components
    +declarationGenerator("FirGen - InjectedClass", ::InjectedClassFirGenerator)
    +declarationGenerator("FirGen - AssistedFactory", ::AssistedFactoryFirGenerator)
    +declarationGenerator("FirGen - AssistedFactoryImpl", ::AssistedFactoryImplFirGenerator)
    +declarationGenerator("FirGen - ProvidesFactory", ::ProvidesFactoryFirGenerator)
    +declarationGenerator("FirGen - BindingMirrorClass", ::BindingMirrorClassFirGenerator)
    +declarationGenerator("FirGen - ContributionsGenerator", ::ContributionsFirGenerator)
    +declarationGenerator("FirGen - ContributionHints", ContributionHintFirGenerator)
    +declarationGenerator("FirGen - DependencyGraph", ::DependencyGraphFirGenerator)
  }
}
```

### 2.3 FIR Extension Types

#### 2.3.1 Declaration Generators
**Purpose**: Create new type declarations that will be visible in subsequent compilation phases.

**Key Generators:**
- **InjectedClassFirGenerator**: Generates factory classes for `@Inject` constructors
- **AssistedFactoryFirGenerator**: Creates assisted injection factory interfaces  
- **ProvidesFactoryFirGenerator**: Generates factory classes for `@Provides` methods
- **DependencyGraphFirGenerator**: Creates dependency graph implementation classes

**Technical Characteristics:**
- Generate **type signatures only** - no implementation bodies
- Declarations must be complete enough for type resolution
- Generated types are immediately available to Kotlin metadata system
- Support for generic types, inheritance hierarchies, and annotation metadata

#### 2.3.2 Supertype Generators
**Purpose**: Establish inheritance relationships for generated classes.

**Key Generators:**
- **GraphFactoryFirSupertypeGenerator**: Sets up factory supertype relationships
- **ContributedInterfaceSupertypeGenerator**: Handles contribution-based inheritance
- **ProvidesFactorySupertypeGenerator**: Establishes provider factory supertypes

#### 2.3.3 Checkers and Validators
**Purpose**: Perform compile-time validation and error reporting.

**Key Components:**
- **MetroFirCheckers**: Core validation logic for dependency injection patterns
- **FirMetroErrors**: Structured error reporting with detailed diagnostics
- **Type Safety Validation**: Ensures binding compatibility and prevents runtime errors

### 2.4 FIR Type System Integration

#### 2.4.1 TypeKey and ContextualTypeKey

**File**: `compiler/src/main/kotlin/dev/zacsweers/metro/compiler/fir/FirTypeKey.kt`

```kotlin
// Canonical representation of a specific binding
data class FirTypeKey(
  val type: ConeKotlinType,
  val qualifier: FirAnnotation?
)

// TypeKey with usage context (Provider, Lazy, etc.)
data class FirContextualTypeKey(
  val typeKey: FirTypeKey,
  val wrapperType: ContextualWrapperType,
  val isDeferrable: Boolean
)
```

**Purpose:**
- **TypeKey**: Represents the canonical form of a dependency binding (type + optional qualifier)
- **ContextualTypeKey**: Adds context about how the dependency is used (wrapped in Provider, Lazy, etc.)
- **Type Resolution**: Enables precise dependency matching and cycle detection
- **Metadata Generation**: Prepares type information for IR phase consumption

#### 2.4.2 Dependency Graph Analysis in FIR

The FIR phase performs preliminary dependency graph analysis to:
- Validate that all dependencies can be satisfied
- Detect circular dependencies early
- Generate detailed error messages with source location information
- Prepare metadata for efficient IR processing

---

## 3. Phase 2: IR (Intermediate Representation) Integration

### 3.1 Purpose and Scope

The IR phase is responsible for:
- **Implementation Generation**: Creating actual method bodies and executable code
- **Dependency Graph Implementation**: Building the complete runtime dependency resolution system
- **Factory Code Generation**: Generating efficient factory classes and provider methods  
- **Optimization**: Applying performance optimizations and code reduction
- **Platform Lowering**: Generating platform-specific code for multiplatform targets

### 3.2 IR Generation Extension

**File**: `compiler/src/main/kotlin/dev/zacsweers/metro/compiler/ir/MetroIrGenerationExtension.kt`

```kotlin
public class MetroIrGenerationExtension(
  private val messageCollector: MessageCollector,
  private val classIds: ClassIds,
  private val options: MetroOptions,
  private val lookupTracker: LookupTracker?,
  private val expectActualTracker: ExpectActualTracker,
) : IrGenerationExtension {
  
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val symbols = Symbols(moduleFragment, pluginContext, classIds, options)
    val context = IrMetroContext(pluginContext, messageCollector, symbols, options, lookupTracker, expectActualTracker)
    
    context(context) { generateInner(moduleFragment) }
  }
  
  context(context: IrMetroContext)
  private fun generateInner(moduleFragment: IrModuleFragment) {
    // Phase 1: Transform contribution interfaces and collect data
    tracer.traceNested("Transform contributions") {
      moduleFragment.transform(ContributionTransformer(context), contributionData)
    }
    
    // Phase 2: Transform dependency graphs and generate implementations
    tracer.traceNested("Core transformers") { nestedTracer ->
      val dependencyGraphTransformer = DependencyGraphTransformer(
        context, contributionData, nestedTracer, HintGenerator(context, moduleFragment)
      )
      moduleFragment.transform(dependencyGraphTransformer, null)
    }
  }
}
```

### 3.3 IR Transformation Pipeline

#### 3.3.1 Two-Stage IR Processing

**Stage 1: Contribution Processing**
- Processes `$$MetroContribution` interfaces
- Collects dependency binding information
- Builds contribution metadata for dependency resolution
- Enables multi-module dependency aggregation

**Stage 2: Dependency Graph Transformation**
- Builds complete dependency graphs
- Generates factory implementations
- Creates binding resolution logic
- Optimizes generated code for runtime performance

#### 3.3.2 Core IR Transformers

**File**: `compiler/src/main/kotlin/dev/zacsweers/metro/compiler/ir/transformers/DependencyGraphTransformer.kt`

```kotlin
internal class DependencyGraphTransformer(
  context: IrMetroContext,
  private val contributionData: IrContributionData,
  private val parentTracer: Tracer,
  hintGenerator: HintGenerator,
) : IrElementTransformerVoid(), IrMetroContext by context {
  
  private val membersInjectorTransformer = MembersInjectorTransformer(context)
  private val injectConstructorTransformer = InjectConstructorTransformer(context, membersInjectorTransformer)
  private val assistedFactoryTransformer = AssistedFactoryTransformer(context, injectConstructorTransformer)
  private val bindingContainerTransformer = BindingContainerTransformer(context)
  private val contributionHintIrTransformer = ContributionHintIrTransformer(context, hintGenerator)
  
  // Implements the actual dependency graph transformation logic
}
```

**Key Transformer Components:**
- **MembersInjectorTransformer**: Handles field and method injection
- **InjectConstructorTransformer**: Processes constructor injection
- **AssistedFactoryTransformer**: Implements assisted injection patterns
- **BindingContainerTransformer**: Processes binding containers and modules
- **ContributionHintIrTransformer**: Handles contribution hints for incremental compilation

### 3.4 IR Code Generation Patterns

#### 3.4.1 Factory Class Generation

Metro generates Dagger-style factory classes for dependency injection:

```kotlin
// Generated by Metro IR phase
@Generated("metro")
internal class UserService_Factory @Inject constructor(
  private val networkApi: Provider<NetworkApi>,
  private val database: Provider<Database>
) : Factory<UserService> {
  
  override fun get(): UserService {
    return UserService(
      networkApi.get(),
      database.get()
    )
  }
  
  companion object {
    @JvmStatic
    fun create(networkApi: Provider<NetworkApi>, database: Provider<Database>): UserService_Factory {
      return UserService_Factory(networkApi, database)
    }
  }
}
```

#### 3.4.2 Dependency Graph Implementation

**Generated Dependency Graph:**
```kotlin
// Generated by Metro IR phase  
@Generated("metro")
internal class ExampleGraph_Impl : ExampleGraph {
  
  private val userService_Factory = UserService_Factory.create(
    networkApi_Factory,
    database_Factory
  )
  
  override fun userService(): UserService = userService_Factory.get()
  
  override fun inject(target: MainActivity) {
    target.userService = userService()
    target.analyticsService = analyticsService()
  }
}
```

#### 3.4.3 Performance Optimizations

**Direct Field Access:**
- Generated code uses direct field references instead of method calls
- Eliminates reflection and runtime service location overhead
- Enables aggressive JVM optimizations (inlining, escape analysis)

**Factory Reuse:**
- Same factory classes are reused across modules
- Reduces code duplication and binary size
- Improves incremental compilation performance

**Lazy Initialization:**
- Supports `Lazy<T>` and `Provider<T>` wrappers
- Implements double-checked locking for singletons
- Optimizes memory usage for large dependency graphs

---

## 4. Architecture Comparison: FIR + IR vs KSP

### 4.1 Performance Characteristics

| Aspect | Metro (FIR + IR) | KSP-Based Solutions |
|--------|-----------------|---------------------|
| **ABI Change Builds** | 5.3s baseline | 10.3s - 40.5s (+94% to +663%) |
| **Non-ABI Builds** | 2.6s baseline | 3.3s - 7.1s (+26% to +171%) |
| **Graph Processing** | 6.9s baseline | 8.7s - 28.9s (+25% to +318%) |
| **Incremental Compilation** | Native support via K2 IC APIs | Limited KSP incremental support |
| **Memory Usage** | Lower (no source generation) | Higher (generated source overhead) |

### 4.2 Technical Advantages

#### 4.2.1 Compilation Efficiency

**FIR + IR Benefits:**
- **No Source Generation**: Avoids creating intermediate Kotlin files that need compilation
- **Direct IR Generation**: Generates bytecode-ready IR directly into target platforms  
- **Single Compilation Pass**: No need for multiple kotlinc invocations
- **Incremental Compilation**: Direct integration with K2 incremental compilation APIs

**KSP Limitations:**
- **Multiple Compilation Rounds**: KSP → kotlinc → platform compilation
- **Source Generation Overhead**: Generated files must be parsed and compiled  
- **Limited Incremental Support**: KSP's incremental compilation has known limitations
- **Build Cache Complexity**: Generated sources complicate build caching strategies

#### 4.2.2 Code Generation Capabilities

**FIR + IR Advanced Features:**
```kotlin
// Metro can generate private members directly in existing classes
class UserRepository @Inject constructor() {
  // Generated by Metro - not possible with source generation
  @Generated("metro")
  private fun injectMembers(injector: MembersInjector<UserRepository>) {
    injector.injectAnalyticsService(this)
  }
}
```

**Source Generation Limitations:**
- Cannot modify existing classes
- Cannot access private members
- Cannot preserve complex default value expressions
- Limited cross-module coordination

#### 4.2.3 Error Reporting and Diagnostics

**Metro's FIR Integration:**
```
e: ExampleGraph.kt:8:3 [Metro/MissingBinding] Cannot find an @Inject constructor or @Provides-annotated function/property for: kotlin.Int

    kotlin.Int is requested at
        [test.ExampleGraph] test.ExampleGraph.int

Similar bindings:
  - @Named("qualified") Int (Different qualifier). Type: Provided. Source: ExampleGraph.kt:11:3
  - Number (Supertype). Type: Provided. Source: ExampleGraph.kt:10:3  
  - Set<Int> (Multibinding). Type: Multibinding.
```

**Advantages:**
- **IDE Integration**: Errors appear directly in K2 IDE plugin
- **Source Location Accuracy**: Precise error location mapping
- **Rich Diagnostics**: Contextual information and suggested fixes
- **Real-time Validation**: Errors appear as you type (in K2 IDE)

---

## 5. Implementation Specifications for Similar Systems

### 5.1 Compiler Plugin Architecture

#### 5.1.1 Plugin Registration Pattern

```kotlin
// Template for FIR + IR compiler plugin
class YourCompilerPluginRegistrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean = true
  
  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    // Load configuration
    val options = YourOptions.load(configuration)
    if (!options.enabled) return
    
    val classIds = ClassIds.fromOptions(options)
    val messageCollector = configuration.messageCollector
    
    // Phase 1: FIR Registration
    FirExtensionRegistrarAdapter.registerExtension(
      YourFirExtensionRegistrar(classIds, options)
    )
    
    // Phase 2: IR Registration
    IrGenerationExtension.registerExtension(
      YourIrGenerationExtension(messageCollector, classIds, options)
    )
  }
}
```

#### 5.1.2 FIR Extension Structure

```kotlin
class YourFirExtensionRegistrar(
  private val classIds: ClassIds,
  private val options: YourOptions
) : FirExtensionRegistrar() {
  
  override fun ExtensionRegistrarContext.configurePlugin() {
    // Built-in support
    +YourFirBuiltIns.getFactory(classIds, options)
    
    // Validation and error reporting
    +::YourFirCheckers
    
    // Declaration generation
    +declarationGenerator("YourFeature", ::YourDeclarationGenerator)
    
    // Supertype generation  
    +supertypeGenerator("YourSupertypes", ::YourSupertypeGenerator)
    
    // Status transformation (optional)
    +statusTransformer("YourTransformations", ::YourStatusTransformer)
  }
}
```

#### 5.1.3 IR Extension Structure

```kotlin
class YourIrGenerationExtension(
  private val messageCollector: MessageCollector,
  private val classIds: ClassIds,
  private val options: YourOptions
) : IrGenerationExtension {
  
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val context = YourIrContext(pluginContext, messageCollector, options)
    
    // Multi-stage processing
    val contributionData = YourContributionData(context)
    
    // Stage 1: Collect metadata
    moduleFragment.transform(YourMetadataCollector(context), contributionData)
    
    // Stage 2: Generate implementations
    moduleFragment.transform(YourMainTransformer(context, contributionData), null)
  }
}
```

### 5.2 Type System Integration

#### 5.2.1 Type Key Pattern

```kotlin
// Canonical type representation
data class YourTypeKey(
  val type: ConeKotlinType,  // FIR phase
  // val type: IrType,       // IR phase
  val qualifier: YourQualifier?,
  val scope: YourScope?
) {
  // Implement equality and hashing for efficient lookup
  override fun equals(other: Any?): Boolean = TODO()
  override fun hashCode(): Int = TODO()
}

// Contextual usage information
data class YourContextualTypeKey(
  val typeKey: YourTypeKey,
  val wrapperType: YourWrapperType,
  val isOptional: Boolean,
  val isDeferrable: Boolean
)
```

#### 5.2.2 Cross-Phase Data Sharing

```kotlin
// FIR phase preparation
class YourFirDataCollector {
  fun collectTypeMetadata(declaration: FirDeclaration): YourTypeMetadata {
    return YourTypeMetadata(
      typeKey = YourTypeKey.from(declaration.returnType),
      sourceLocation = declaration.source,
      annotations = declaration.annotations
    )
  }
}

// IR phase consumption  
class YourIrTransformer {
  fun processTypeMetadata(metadata: YourTypeMetadata): IrExpression {
    // Use FIR-collected metadata to generate IR
    return generateImplementation(metadata.typeKey, metadata.annotations)
  }
}
```

### 5.3 Performance Optimization Patterns

#### 5.3.1 Incremental Compilation Support

```kotlin
// Leverage Kotlin's incremental compilation APIs
class YourIrGenerationExtension(
  private val lookupTracker: LookupTracker?,
  private val expectActualTracker: ExpectActualTracker
) : IrGenerationExtension {
  
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    // Track type lookups for incremental compilation
    lookupTracker?.record(filePath, position, scopeFqName, name)
    
    // Track expect/actual relationships for multiplatform
    expectActualTracker.reportExpectActual(expectedClassId, actualClassId, filePath)
  }
}
```

#### 5.3.2 Caching and Memoization

```kotlin
// Cache expensive computations across transformations
class YourTransformerCache {
  private val typeAnalysisCache = mutableMapOf<YourTypeKey, YourAnalysisResult>()
  private val generatedDeclarations = mutableMapOf<ClassId, IrClass>()
  
  fun analyzeType(typeKey: YourTypeKey): YourAnalysisResult {
    return typeAnalysisCache.computeIfAbsent(typeKey) {
      performExpensiveAnalysis(it)
    }
  }
}
```

### 5.4 Error Reporting Best Practices

#### 5.4.1 Structured Error System

```kotlin
// Define structured error types
sealed class YourError(
  val message: String,
  val severity: CompilerMessageSeverity = CompilerMessageSeverity.ERROR
) {
  class MissingDependency(val typeKey: YourTypeKey) : YourError(
    "Cannot find binding for ${typeKey.type}"
  )
  
  class CircularDependency(val cycle: List<YourTypeKey>) : YourError(
    "Circular dependency detected: ${cycle.joinToString(" -> ")}"
  )
}

// Error reporting with context
fun IrMetroContext.reportError(error: YourError, element: IrElement) {
  messageCollector.report(
    error.severity,
    error.message,
    element.fileOrNull?.let { CompilerMessageSourceLocation.create(it.path, -1, -1, null) }
  )
}
```

#### 5.4.2 Diagnostic Context

```kotlin
// Provide rich diagnostic context
class YourDiagnosticContext {
  fun generateMissingBindingError(
    typeKey: YourTypeKey,
    requestLocation: IrElement,
    similarBindings: List<YourTypeKey>
  ): String {
    return buildString {
      appendLine("Cannot find binding for: ${typeKey.type}")
      appendLine()
      appendLine("${typeKey.type} is requested at")
      appendLine("    ${requestLocation.render()}")
      
      if (similarBindings.isNotEmpty()) {
        appendLine()
        appendLine("Similar bindings:")
        similarBindings.forEach { binding ->
          appendLine("  - ${binding.render()} (${binding.differenceFrom(typeKey)})")
        }
      }
    }
  }
}
```

---

## 6. Migration Path from KSP to FIR + IR

### 6.1 Assessment Phase

#### 6.1.1 Current KSP Analysis
```kotlin
// Analyze existing KSP processor
class KspToFirIrMigrationAnalyzer {
  fun analyzeKspProcessor(processor: SymbolProcessor): MigrationPlan {
    return MigrationPlan(
      generatedTypes = analyzeGeneratedTypes(processor),
      dependencies = analyzeDependencies(processor), 
      validations = analyzeValidations(processor),
      complexity = estimateComplexity(processor)
    )
  }
}
```

#### 6.1.2 Compatibility Assessment
- **Code Generation Patterns**: Identify what code is currently generated
- **Validation Logic**: Map KSP validation to FIR checkers
- **Multi-Round Processing**: Identify dependencies that require multi-round processing
- **Performance Requirements**: Establish performance improvement targets

### 6.2 Implementation Strategy

#### 6.2.1 Phase 1: FIR Foundation
1. **Create FIR extension registrar** with basic structure
2. **Implement core checkers** for validation logic
3. **Add declaration generators** for type signatures
4. **Establish error reporting** system

#### 6.2.2 Phase 2: IR Implementation  
1. **Create IR generation extension** with transformation pipeline
2. **Implement core transformers** for code generation
3. **Add optimization passes** for performance
4. **Integrate incremental compilation** support

#### 6.2.3 Phase 3: Migration and Testing
1. **Parallel implementation** alongside existing KSP processor
2. **Comprehensive testing** with existing codebase
3. **Performance benchmarking** against KSP baseline  
4. **Gradual rollout** with feature flags

### 6.3 Common Migration Challenges

#### 6.3.1 Multi-Round Processing
**KSP Approach:**
```kotlin
// KSP supports multiple rounds naturally
class MyKspProcessor : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val toProcess = resolver.getSymbolsWithAnnotation("my.Annotation")
    // Generate new sources
    // Return symbols that need reprocessing
    return deferredSymbols
  }
}
```

**FIR + IR Solution:**  
```kotlin
// FIR + IR requires careful coordination
class MyFirGenerator : FirDeclarationGenerationExtension {
  override fun generateTopLevelClassifiersAndNestedClassifiers(
    declaration: FirClassLikeDeclaration
  ): List<FirDeclaration> {
    // All dependencies must be resolved in single pass
    // Use dependency graph analysis to order generation
    return generateDependentDeclarations(declaration)
  }
}
```

#### 6.3.2 Cross-Module Dependencies
**Challenge**: KSP processes each module independently
**Solution**: Use FIR contribution system and IR metadata exchange

```kotlin
// FIR contribution pattern for cross-module coordination
class CrossModuleContributionGenerator : FirDeclarationGenerationExtension {
  override fun generateTopLevelClassifiersAndNestedClassifiers(
    declaration: FirClassLikeDeclaration  
  ): List<FirDeclaration> {
    return if (declaration.hasAnnotation("ContributesTo")) {
      listOf(generateContributionMetadata(declaration))
    } else {
      emptyList()
    }
  }
}
```

---

## 7. Lessons Learned and Best Practices

### 7.1 FIR Development Guidelines

#### 7.1.1 Defensive Programming
- **Null Safety**: FIR may have incomplete information during IDE usage
- **Validation**: Always validate assumptions about declaration completeness
- **Error Handling**: Graceful degradation when information is unavailable

```kotlin
// Defensive FIR code example
fun analyzeFirDeclaration(declaration: FirCallableDeclaration): AnalysisResult? {
  val returnType = declaration.returnTypeRef.coneType
  if (returnType is ConeErrorType) {
    // Type resolution failed - common in IDE scenarios
    return null  // Graceful degradation
  }
  
  return AnalysisResult(returnType)
}
```

#### 7.1.2 Performance Considerations
- **Lazy Evaluation**: Use lazy computation for expensive operations
- **Caching**: Cache results but be aware of memory implications
- **Minimal Processing**: Only process what's absolutely necessary in FIR

### 7.2 IR Development Guidelines  

#### 7.2.1 Aggressive Optimization
- **Assert Expectations**: IR phase should have complete information
- **Fail Fast**: Report clear errors for invalid states
- **Performance Focus**: Optimize for runtime efficiency

```kotlin
// Aggressive IR code example  
fun processIrDeclaration(declaration: IrClass): IrExpression {
  requireNotNull(declaration.primaryConstructor) {
    "Expected primary constructor for ${declaration.name}"
  }
  
  // Aggressive optimization - inline when beneficial
  return generateOptimizedImplementation(declaration)
}
```

#### 7.2.2 Code Generation Patterns
- **Factory Classes**: Follow established patterns (Dagger-style)
- **Direct Field Access**: Avoid method call overhead where possible  
- **Platform Optimization**: Generate platform-specific optimizations

### 7.3 Testing Strategies

#### 7.3.1 Multi-Level Testing
```
├── Unit Tests (Compiler Components)
│   ├── FIR Generator Tests
│   ├── IR Transformer Tests
│   └── Type Analysis Tests
├── Integration Tests (Full Compilation)
│   ├── Box Tests (Execute Generated Code)
│   ├── Diagnostic Tests (Error Reporting)  
│   └── Dump Tests (FIR/IR Inspection)
└── Performance Tests (Build Time)
    ├── ABI Change Scenarios
    ├── Non-ABI Change Scenarios
    └── Multi-Module Builds
```

#### 7.3.2 Metro's Testing Infrastructure

**Box Tests** - Full compilation and execution:
```kotlin
// compiler-tests/src/test/data/box/basic/inject.kt
@Inject
class UserService @Inject constructor(private val api: NetworkApi)

@DependencyGraph
interface TestGraph {
  fun userService(): UserService
}

fun box(): String {
  val graph = TestGraph::class.create()
  val service = graph.userService()
  return if (service != null) "OK" else "FAIL"
}
```

**Diagnostic Tests** - Error reporting validation:
```kotlin
// compiler-tests/src/test/data/diagnostics/missingBinding.kt
@DependencyGraph
interface TestGraph {
  <!Metro/MissingBinding!>fun userService(): UserService<!>
}
```

---

## 8. Future Opportunities and Considerations

### 8.1 Advanced FIR + IR Possibilities

#### 8.1.1 Advanced Code Generation
- **Cross-Platform Optimization**: Generate platform-specific code paths
- **Compile-Time Computation**: Move runtime logic to compile-time where possible
- **Dead Code Elimination**: Remove unused dependency graph branches

#### 8.1.2 IDE Integration Enhancements  
- **Real-Time Validation**: Live dependency graph validation as you type
- **Code Navigation**: Navigate to generated factory implementations
- **Debugging Support**: Enhanced debugging experience for generated code

### 8.2 Ecosystem Integration

#### 8.2.1 Build Tool Enhancement
- **Gradle Integration**: Deeper integration with Gradle's incremental compilation
- **Build Cache Optimization**: Optimal build cache keys for generated code
- **Parallel Processing**: Leverage multi-core builds more effectively

#### 8.2.2 Tooling Ecosystem
- **Static Analysis**: Custom lint rules for dependency injection patterns
- **Metrics and Reporting**: Detailed build performance analytics  
- **Documentation Generation**: Auto-generate dependency graph documentation

---

## 9. Conclusion

Metro's FIR + IR architecture demonstrates the significant advantages of deep Kotlin compiler integration over traditional source generation approaches like KSP. The two-phase design provides:

### 9.1 Technical Benefits
- **5-40x faster build times** for ABI changes compared to KSP solutions
- **Zero source generation overhead** with direct IR emission
- **Advanced code generation capabilities** not possible with source generation
- **Superior error reporting** with IDE integration

### 9.2 Architectural Advantages
- **Native incremental compilation** support through K2 APIs
- **Multiplatform optimization** with platform-specific code generation
- **Memory efficiency** without intermediate source files
- **Future-proof design** aligned with Kotlin compiler evolution

### 9.3 Implementation Viability

For your fake generator project, adopting Metro's FIR + IR approach could provide:

1. **Superior Performance**: Dramatic build time improvements, especially for large codebases
2. **Advanced Features**: Capabilities like private member injection and complex default value handling
3. **Better Developer Experience**: Real-time validation and superior error reporting
4. **Scalability**: Better handling of large dependency graphs and multi-module projects

The architectural patterns and implementation strategies documented here provide a comprehensive foundation for building similar compiler plugin-based code generation systems that leverage the full power of the Kotlin K2 compiler architecture.

---

## 10. References and Further Reading

### 10.1 Metro Project Resources
- **Main Repository**: https://github.com/ZacSweers/metro
- **Documentation**: https://zacsweers.github.io/metro
- **Benchmark Results**: `/metro/benchmark/README.md`
- **Compiler Tests**: `/metro/compiler-tests/README.md`

### 10.2 Kotlin Compiler Documentation  
- **K2 Compiler Architecture**: Kotlin official documentation
- **FIR Extension APIs**: JetBrains compiler plugin documentation
- **IR Generation**: Kotlin compiler backend documentation
- **Incremental Compilation**: Kotlin build performance guides

### 10.3 Related Projects
- **Dagger**: Dependency injection baseline and runtime patterns
- **Anvil**: Code generation aggregation patterns
- **Kotlin-Inject**: Kotlin-native dependency injection approaches
- **KSP**: Kotlin Symbol Processing for comparison

---

*This specification document provides a comprehensive foundation for understanding and implementing FIR + IR based code generation systems in Kotlin, specifically tailored for projects similar to your fake generator requirements.*