# KtFakes Code Generation Strategies Analysis

> **Created**: January 2025  
> **Status**: Strategic Planning Phase  
> **Problem**: Current hardcoded string-template approach is brittle, not scalable, and breaks with simple changes

## 🚨 **Current Problem Analysis**

### **Critical Issues with Current Approach:**
1. **Hardcoded Method Signatures**: Adding `val memes: String` broke the entire generation
2. **String Template Brittleness**: No type safety, easy to break with syntax errors
3. **Manual Maintenance**: Every new method/property requires manual code updates
4. **Poor DevEx**: Difficult to debug, extend, or understand generated code
5. **No Dynamic Discovery**: Can't automatically detect interface changes
6. **Scaling Nightmare**: Each new interface shape requires manual mapping

### **Current Architecture Problems:**
```kotlin
// CURRENT: Hardcoded and brittle
when (name) {
    "getValue" -> "getValue(): String"
    "setValue" -> "setValue(value: String): Unit"
    "track" -> "track(event: String): Unit"
    // 😱 What about properties? Complex generics? Suspend functions?
}
```

## 🎯 **Design Requirements for New Strategy**

### **Must Have:**
- ✅ **Type Safe**: Compile-time validation of generated code
- ✅ **Dynamic Discovery**: Automatically detect interface shapes
- ✅ **Scalable**: Handle any interface without manual mapping
- ✅ **Maintainable**: Easy to understand and modify
- ✅ **Extensible**: Simple to add new fake behaviors
- ✅ **Robust**: Handle edge cases (generics, suspend, properties, etc.)

### **Nice to Have:**
- 🎨 **Great DevEx**: Clear debugging and error messages
- ⚡ **Performance**: Fast compilation times
- 🧪 **Testable**: Easy to unit test generation logic
- 📚 **Self-Documenting**: Generated code is readable

---

## 🏗️ **STRATEGY 1: IR-Native Dynamic Reflection**

### **Concept:**
Use Kotlin IR API to dynamically inspect interface structures and generate IR nodes directly (no string templates).

### **Implementation Approach:**
```kotlin
class IRNativeFakeGenerator(private val pluginContext: IrPluginContext) {
    
    fun generateFakeClass(sourceInterface: IrClass): IrClass {
        val fakeClass = pluginContext.irFactory.createClass(...)
        
        // Dynamically discover and implement all members
        sourceInterface.declarations.forEach { member ->
            when (member) {
                is IrSimpleFunction -> addMethodImplementation(fakeClass, member)
                is IrProperty -> addPropertyImplementation(fakeClass, member)
                is IrConstructor -> addConstructorImplementation(fakeClass, member)
            }
        }
        
        return fakeClass
    }
    
    private fun addMethodImplementation(fakeClass: IrClass, method: IrSimpleFunction) {
        val implementation = createMethodImplementation(
            name = method.name,
            returnType = method.returnType,
            parameters = method.valueParameters,
            isSuspend = method.isSuspend
        )
        fakeClass.addChild(implementation)
    }
    
    private fun addPropertyImplementation(fakeClass: IrClass, property: IrProperty) {
        val backingField = createBackingField(property.type)
        val getter = createGetter(property, backingField)
        val setter = if (property.isVar) createSetter(property, backingField) else null
        
        fakeClass.addChild(backingField)
        fakeClass.addChild(getter)
        setter?.let { fakeClass.addChild(it) }
    }
}
```

### **Pros:**
- ✅ **Fully Type Safe**: Native IR generation with compile-time validation
- ✅ **Zero String Templates**: No syntax errors or formatting issues
- ✅ **Complete Dynamic Discovery**: Handles any interface shape automatically
- ✅ **Native Kotlin Integration**: Works with IDE, debuggers, tooling
- ✅ **Performance**: Direct IR generation is fastest approach
- ✅ **Future Proof**: Uses official Kotlin compiler APIs

### **Cons:**
- ❌ **Steep Learning Curve**: IR API is complex and poorly documented
- ❌ **High Development Cost**: Requires deep Kotlin compiler knowledge
- ❌ **API Instability**: Kotlin IR APIs change between versions
- ❌ **Debugging Complexity**: Harder to debug IR node creation
- ❌ **Limited Examples**: Few real-world IR generation examples

### **Example Generated Code:**
```kotlin
// Generated directly as IR nodes, no strings involved
class FakeUserServiceImpl : UserService {
    private var _name: String = ""
    private var _age: Int = 0
    
    override val name: String get() = _name
    override var age: Int 
        get() = _age
        set(value) { _age = value }
    
    override suspend fun fetchUser(id: Long): User = User.empty()
    override fun updateProfile(profile: Profile): Unit {}
}
```

---

## 🏗️ **STRATEGY 2: KotlinPoet + Reflection-Based Discovery**

### **Concept:**
Use KotlinPoet for type-safe code generation combined with runtime reflection to discover interface structures.

### **Implementation Approach:**
```kotlin
class KotlinPoetFakeGenerator {
    
    fun generateFakeClass(interfaceClass: IrClass): FileSpec {
        val interfaceInfo = analyzeInterface(interfaceClass)
        
        return FileSpec.builder(interfaceInfo.packageName, "${interfaceInfo.name}Fakes")
            .addType(buildFakeImplementation(interfaceInfo))
            .addFunction(buildFactoryFunction(interfaceInfo))
            .addType(buildConfigurationClass(interfaceInfo))
            .build()
    }
    
    private fun analyzeInterface(interfaceClass: IrClass): InterfaceInfo {
        return InterfaceInfo(
            name = interfaceClass.name.asString(),
            packageName = interfaceClass.packageFqName?.asString() ?: "",
            methods = interfaceClass.functions.map(::analyzeMethod),
            properties = interfaceClass.properties.map(::analyzeProperty)
        )
    }
    
    private fun buildFakeImplementation(info: InterfaceInfo): TypeSpec {
        return TypeSpec.classBuilder("Fake${info.name}Impl")
            .addSuperinterface(ClassName(info.packageName, info.name))
            .apply {
                info.properties.forEach { addProperty(buildPropertyImplementation(it)) }
                info.methods.forEach { addFunction(buildMethodImplementation(it)) }
            }
            .build()
    }
    
    private fun buildPropertyImplementation(prop: PropertyInfo): PropertySpec {
        return when (prop.mutability) {
            PropertyMutability.VAL -> PropertySpec.builder(prop.name, prop.type)
                .initializer(prop.type.defaultValue())
                .build()
            PropertyMutability.VAR -> PropertySpec.builder(prop.name, prop.type)
                .mutable(true)
                .initializer(prop.type.defaultValue())
                .build()
        }
    }
}

data class InterfaceInfo(
    val name: String,
    val packageName: String,
    val methods: List<MethodInfo>,
    val properties: List<PropertyInfo>
)

data class PropertyInfo(
    val name: String,
    val type: TypeName,
    val mutability: PropertyMutability
)

data class MethodInfo(
    val name: String,
    val returnType: TypeName,
    val parameters: List<ParameterInfo>,
    val isSuspend: Boolean
)
```

### **Pros:**
- ✅ **Type Safe Generation**: KotlinPoet ensures valid Kotlin code
- ✅ **Excellent DevEx**: Clear, readable code generation logic
- ✅ **Rich Ecosystem**: Well-documented with many examples
- ✅ **Testable**: Easy to unit test generated code structures
- ✅ **Maintainable**: Clean separation of concerns
- ✅ **IDE Support**: Great tooling support for KotlinPoet

### **Cons:**
- ❌ **External Dependency**: Requires KotlinPoet library
- ❌ **String Output**: Still generates string files (though type-safely)
- ❌ **Runtime Overhead**: Reflection analysis can be slower
- ❌ **Complex Type Handling**: Generics and complex types need careful handling
- ❌ **File Writing Required**: Need to manage file I/O and paths

### **Example Generated Code:**
```kotlin
// Generated via KotlinPoet - clean, readable, type-safe
class FakeUserServiceImpl : UserService {
    override var name: String = ""
    override var age: Int = 0
    
    override suspend fun fetchUser(id: Long): User = User(id = 0L, name = "")
    
    override fun updateProfile(profile: Profile): Unit = Unit
}
```

---

## 🏗️ **STRATEGY 3: Template Engine + Schema-Driven Generation**

### **Concept:**
Use a mature template engine (like Velocity or FreeMarker) with a schema-based approach for interface analysis.

### **Implementation Approach:**
```kotlin
class TemplateDrivenGenerator {
    private val templateEngine = VelocityEngine()
    
    fun generateFakes(interfaceSchema: InterfaceSchema): GeneratedFiles {
        val context = VelocityContext().apply {
            put("interface", interfaceSchema)
            put("utils", CodeGenUtils())
            put("typeMapper", KotlinTypeMapper())
        }
        
        return GeneratedFiles(
            implementation = processTemplate("fake-impl.vm", context),
            factory = processTemplate("factory-function.vm", context),
            configuration = processTemplate("config-dsl.vm", context)
        )
    }
    
    fun analyzeInterface(irClass: IrClass): InterfaceSchema {
        return InterfaceSchema(
            name = irClass.name.asString(),
            packageName = irClass.packageFqName?.asString() ?: "",
            members = irClass.declarations.map { member ->
                when (member) {
                    is IrSimpleFunction -> FunctionMember(
                        name = member.name.asString(),
                        returnType = mapIrTypeToSchema(member.returnType),
                        parameters = member.valueParameters.map { mapParameterToSchema(it) },
                        modifiers = extractModifiers(member)
                    )
                    is IrProperty -> PropertyMember(
                        name = member.name.asString(),
                        type = mapIrTypeToSchema(member.backingField?.type ?: member.getter?.returnType!!),
                        isMutable = member.isVar,
                        modifiers = extractModifiers(member)
                    )
                    else -> UnknownMember(member.javaClass.simpleName)
                }
            }
        )
    }
}

// Template files (fake-impl.vm):
```
class Fake${interface.name}Impl : ${interface.fullyQualifiedName} {
    #foreach($property in $interface.properties)
    override var ${property.name}: ${property.type.kotlinName} = ${utils.defaultValue($property.type)}
    #end
    
    #foreach($method in $interface.methods)
    override #if($method.isSuspend)suspend #end fun ${method.name}(
        #foreach($param in $method.parameters)
        ${param.name}: ${param.type.kotlinName}#if($foreach.hasNext), #end
        #end
    ): ${method.returnType.kotlinName} = ${utils.defaultValue($method.returnType)}
    #end
}
```
```

### **Pros:**
- ✅ **Mature Technology**: Battle-tested template engines
- ✅ **Clear Separation**: Logic vs presentation clearly separated
- ✅ **Designer Friendly**: Non-programmers can modify templates
- ✅ **Flexible Output**: Easy to generate multiple file formats
- ✅ **Rich Features**: Conditionals, loops, macros, includes
- ✅ **Testable**: Easy to test templates independently

### **Cons:**
- ❌ **External Dependencies**: Requires template engine library
- ❌ **Learning Curve**: Need to learn template syntax
- ❌ **Debug Complexity**: Harder to debug template issues
- ❌ **Performance**: Template processing adds overhead
- ❌ **Type Safety**: Templates are not type-checked
- ❌ **IDE Support**: Limited IDE support for template files

---

## 🏗️ **STRATEGY 4: AST-Based Code Generation with Kotlin Symbol Processing**

### **Concept:**
Use Kotlin Symbol Processing (KSP) approach within the compiler plugin to build Abstract Syntax Trees for generation.

### **Implementation Approach:**
```kotlin
class ASTBasedGenerator {
    
    fun generateFakeImplementation(interfaceSymbol: InterfaceSymbol): KotlinFile {
        val astBuilder = KotlinASTBuilder()
        
        return astBuilder.buildFile {
            packageDeclaration(interfaceSymbol.packageName)
            
            imports {
                import(interfaceSymbol.fullyQualifiedName)
                import("com.rsicarelli.fakt.*")
            }
            
            classDeclaration("Fake${interfaceSymbol.simpleName}Impl") {
                superTypes(interfaceSymbol.qualifiedName)
                
                // Generate properties with backing fields and accessors
                interfaceSymbol.properties.forEach { property ->
                    backingField("_${property.name}", property.type.defaultValue())
                    
                    propertyOverride(property.name, property.type) {
                        if (property.isMutable) {
                            getter { return_("_${property.name}") }
                            setter("value") { assign("_${property.name}", "value") }
                        } else {
                            getter { return_("_${property.name}") }
                        }
                    }
                }
                
                // Generate method implementations
                interfaceSymbol.functions.forEach { function ->
                    functionOverride(function.name, function.returnType) {
                        parameters(function.parameters)
                        
                        if (function.isSuspend) modifier("suspend")
                        
                        body {
                            return_(function.returnType.defaultValue())
                        }
                    }
                }
            }
            
            factoryFunction("fake${interfaceSymbol.simpleName}") {
                returnType(interfaceSymbol.qualifiedName)
                parameter("configure", "Fake${interfaceSymbol.simpleName}Config.() -> Unit = {}")
                
                body {
                    return_("Fake${interfaceSymbol.simpleName}Impl().apply { Fake${interfaceSymbol.simpleName}Config(this).configure() }")
                }
            }
        }
    }
}

// DSL for AST building
class KotlinASTBuilder {
    fun buildFile(block: FileBuilder.() -> Unit): KotlinFile {
        return FileBuilder().apply(block).build()
    }
}

class FileBuilder {
    fun classDeclaration(name: String, block: ClassBuilder.() -> Unit) { ... }
    fun functionDeclaration(name: String, block: FunctionBuilder.() -> Unit) { ... }
    fun propertyDeclaration(name: String, type: String, block: PropertyBuilder.() -> Unit) { ... }
}
```

### **Pros:**
- ✅ **Structured Approach**: Clear AST representation
- ✅ **Type Safety**: Compile-time validation of structure
- ✅ **Composable**: Easy to build complex structures
- ✅ **Testable**: Each AST component can be unit tested
- ✅ **Extensible**: Easy to add new AST node types
- ✅ **IDE Integration**: Potential for great IDE support

### **Cons:**
- ❌ **High Complexity**: Building full AST system is complex
- ❌ **Custom Infrastructure**: Need to build and maintain AST builders
- ❌ **Performance Overhead**: AST building and traversal cost
- ❌ **Learning Curve**: Team needs to learn custom AST API
- ❌ **Maintenance Burden**: Custom infrastructure needs maintenance

---

## 📊 **COMPARATIVE ANALYSIS**

### **Complexity vs Capability Matrix:**
```
High Capability │ 
                │ [IR-Native] ●
                │           
                │ [AST-Based] ●     [KotlinPoet] ●
                │                              
Low Capability  │           [Template] ●
                └────────────────────────────────
                Low Complexity    High Complexity
```

### **Scoring Matrix (1-5 scale, 5 = best):**

| Criterion        | IR-Native | KotlinPoet | Template | AST-Based |
|-----------------|-----------|------------|----------|-----------|
| **Type Safety**     | 5         | 5          | 2        | 4         |
| **Maintainability** | 2         | 4          | 3        | 3         |
| **Scalability**     | 5         | 4          | 3        | 4         |
| **DevEx**           | 2         | 5          | 3        | 3         |
| **Performance**     | 5         | 3          | 2        | 3         |
| **Learning Curve**  | 1         | 4          | 4        | 2         |
| **Future Proof**    | 4         | 4          | 4        | 3         |
| **Testability**     | 3         | 5          | 4        | 4         |
| **Total Score**     | **27**    | **34**     | **25**   | **26**    |

---

## 🎯 **RECOMMENDATIONS**

### **🥇 RECOMMENDED: Strategy 2 - KotlinPoet + Reflection**
**Why**: Best balance of type safety, developer experience, and maintainability.

### **🥈 ALTERNATIVE: Strategy 1 - IR-Native** 
**Why**: Highest performance and capability, but requires significant Kotlin compiler expertise.

### **🥉 BACKUP: Strategy 3 - Template Engine**
**Why**: Solid fallback option with good flexibility and proven technology.

---

## 🚀 **IMPLEMENTATION ROADMAP**

### **Phase 1: Proof of Concept (1-2 weeks)**
- Implement basic KotlinPoet integration
- Create interface analysis system
- Generate simple property and method implementations
- Validate approach with existing test cases

### **Phase 2: Full Implementation (2-3 weeks)**
- Complete type mapping system
- Handle complex types (generics, nullability, etc.)
- Implement configuration DSL generation
- Add comprehensive error handling

### **Phase 3: Polish & Optimization (1 week)**
- Performance optimization
- Edge case handling
- Documentation and examples
- Migration from current system

### **Success Criteria:**
- ✅ Adding new interface members requires zero code changes
- ✅ Generated code passes all existing tests
- ✅ System handles complex types automatically
- ✅ Clear error messages for edge cases
- ✅ Performance equal or better than current approach

---

## 📚 **NEXT STEPS**

1. **Review and Approve Strategy**: Choose preferred approach
2. **Create Detailed Design**: Expand chosen strategy with detailed specs  
3. **Build Prototype**: Implement minimal viable version
4. **Migration Plan**: Strategy for transitioning from current system
5. **Testing Strategy**: Ensure no regression in existing functionality

**The goal**: Transform from brittle string templates to a robust, scalable, type-safe code generation engine that handles any interface shape automatically.