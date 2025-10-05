# Generic Support - Quick Start Guide

> **Para começar AGORA**: Siga este guia passo a passo
> **Tempo estimado**: 2-3 semanas
> **Pré-requisitos**: Conhecimento de Kotlin IR APIs

## 📋 Checklist de Início

- [ ] Ler este Quick Start (você está aqui!)
- [ ] Ler [ROADMAP.md](./ROADMAP.md) - visão geral completa
- [ ] Ler [Technical Reference](./technical-reference.md) - APIs do Kotlin
- [ ] Consultar [Test Matrix](./test-matrix.md) - entender validação

**Próximo passo**: Começar Phase 1, Task 1.1

---

## 🎯 Objetivo Geral

**Transformar**:
```kotlin
// HOJE (não funciona):
@Fake
interface Repository<T> {
    fun save(item: T): T
}
// ❌ SKIPPED: "Generic interfaces not supported"
```

**Em**:
```kotlin
// AMANHÃ (funcionando):
@Fake
interface Repository<T> {
    fun save(item: T): T
}

// Gera automaticamente:
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
    override fun save(item: T): T = saveBehavior(item)
}

inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T>

// Uso type-safe:
val userRepo = fakeRepository<User> {
    save { user -> user }
}
val user: User = userRepo.save(User("123", "Test")) // ✅ TYPE SAFE!
```

---

## 🚀 Primeira Semana - Phase 1

### Day 1-2: Criar GenericIrSubstitutor

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/GenericIrSubstitutor.kt`

**Começar com**:
```kotlin
@OptIn(UnsafeDuringIrConstructionAPI::class)
class GenericIrSubstitutor(private val pluginContext: IrPluginContext) {

    fun createSubstitutionMap(
        originalInterface: IrClass,
        superType: IrSimpleType
    ): Map<IrTypeParameterSymbol, IrTypeArgument> {
        // TODO: Implement
        return emptyMap()
    }
}
```

**Referência**: [Phase 1, Task 1.1](./phase1-core-infrastructure.md#task-11-create-genericirsubstitutorkt-day-1-2)

**Teste primeiro** (TDD):
```kotlin
@Test
fun `GIVEN interface with single type parameter WHEN creating map THEN should work`() {
    // Given
    val mockInterface = createMockInterface("Repository", listOf("T"))

    // When
    val map = substitutor.createSubstitutionMap(mockInterface, mockSuperType)

    // Then
    assertEquals(1, map.size)
}
```

---

### Day 2-3: Enhance TypeResolver

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/types/TypeResolver.kt`

**Mudar linha 118-124**:
```kotlin
// ANTES:
irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
    val typeParam = irType.classifier.owner as IrTypeParameter
    if (preserveTypeParameters) {
        typeParam.name.asString()
    } else {
        "Any" // ❌ Type erasure
    }
}

// DEPOIS:
irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
    val typeParam = irType.classifier.owner as IrTypeParameter
    typeParam.name.asString() // ✅ Always preserve
}
```

**Referência**: [Phase 1, Task 1.2](./phase1-core-infrastructure.md#task-12-enhance-typeresolverkt-day-2-3)

---

### Day 3: Remover Generic Filter

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`

**Deletar linhas 189-193**:
```kotlin
// ❌ DELETE THIS:
interfaceAnalyzer.checkGenericSupport(fakeInterface) != null -> {
    val genericError = interfaceAnalyzer.checkGenericSupport(fakeInterface)
    messageCollector?.reportInfo("Fakt: Skipping generic interface: $genericError")
    null
}
```

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/analysis/InterfaceAnalyzer.kt`

**Mudar**:
```kotlin
// ANTES:
fun checkGenericSupport(irClass: IrClass): String? =
    checkInterfaceLevelGenerics(irClass)
        ?: checkMethodLevelGenerics(irClass)

// DEPOIS:
fun checkGenericSupport(irClass: IrClass): String? = null // ✅ Allow all
```

**Referência**: [Phase 1, Task 1.3](./phase1-core-infrastructure.md#task-13-remove-generic-filter-day-3)

---

### Day 4-5: Integration Test

**Arquivo**: `compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt`

**Teste mínimo**:
```kotlin
@Test
fun `GIVEN simple Repository WHEN compiling THEN should not skip`() {
    val compilation = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("Test.kt", """
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Repository<T> {
                fun save(item: T): T
            }
        """))
        compilerPlugins = listOf(FaktCompilerPluginRegistrar())
    }

    val result = compilation.compile()
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
}
```

**Critério de sucesso Phase 1**: Este teste PASSA ✅

**Referência**: [Phase 1, Task 1.4](./phase1-core-infrastructure.md#task-14-integration-test-day-4-5)

---

## 🚀 Segunda Semana - Phase 2

### Day 1-2: Update ImplementationGenerator

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ImplementationGenerator.kt`

**Mudar linhas 40-49**:
```kotlin
// ANTES:
val interfaceWithGenerics = if (analysis.typeParameters.isNotEmpty()) {
    val genericParams = analysis.typeParameters.joinToString(", ") { "Any" }
    "${analysis.interfaceName}<$genericParams>"
} else {
    analysis.interfaceName
}
appendLine("class $fakeClassName : $interfaceWithGenerics {")

// DEPOIS:
val typeParams = if (analysis.typeParameters.isNotEmpty()) {
    "<${analysis.typeParameters.joinToString(", ")}>"
} else ""
val interfaceName = "${analysis.interfaceName}$typeParams"
appendLine("class $fakeClassName$typeParams : $interfaceName {")
```

**Output esperado**:
```kotlin
// ANTES: class FakeRepositoryImpl : Repository<Any>
// DEPOIS: class FakeRepositoryImpl<T> : Repository<T>
```

**Referência**: [Phase 2, Task 2.1](./phase2-code-generation.md#task-21-update-implementationgeneratorkt-day-1-2)

---

### Day 2-3: Update FactoryGenerator

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/FactoryGenerator.kt`

**Reescrever completamente** - ver [Phase 2, Task 2.2](./phase2-code-generation.md#task-22-update-factorygeneratorkt-day-2-3)

**Output esperado**:
```kotlin
inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T> {
    return FakeRepositoryImpl<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

---

### Day 3-4: Update ConfigurationDslGenerator

**Arquivo**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ConfigurationDslGenerator.kt`

**Adicionar type parameters** - ver [Phase 2, Task 2.3](./phase2-code-generation.md#task-23-update-configurationdslgeneratorkt-day-3-4)

**Output esperado**:
```kotlin
class FakeRepositoryConfig<T>(
    private val fake: FakeRepositoryImpl<T>
) {
    fun save(behavior: (T) -> T) {
        fake.configureSave(behavior)
    }
}
```

---

### Day 4-5: Integration Test Phase 2

**Teste**: Generated code compiles AND is type-safe

```kotlin
@Test
fun `GIVEN generic fake WHEN using with concrete type THEN type safe`() {
    // Stage 1: Generate
    val genResult = compile(repositoryInterface)
    assertEquals(OK, genResult.exitCode)

    // Stage 2: Use with type safety
    val useResult = compile("""
        val repo = fakeRepository<User> {
            save { user -> user }
        }
        val user: User = repo.save(User("test")) // TYPE CHECK!
    """)
    assertEquals(OK, useResult.exitCode)
}
```

**Critério de sucesso Phase 2**: Type safety funciona! ✅

---

## 🚀 Terceira Semana - Phase 3

### Day 1-3: Implement Full Test Matrix

**Arquivo**: `compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt`

**Implementar**:
- [ ] P0: T0.1 - Single type parameter (Repository<T>)
- [ ] P0: T0.2 - Multiple type parameters (Cache<K, V>)
- [ ] P1: T1.1 - Method-level generics
- [ ] P1: T1.2 - Mixed generics
- [ ] P2: T2.1 - Type constraints
- [ ] P2: T2.2 - Variance

**Target**: 95% dos testes P0-P2 passando

**Referência**: [Test Matrix](./test-matrix.md), [Phase 3, Task 3.1](./phase3-testing-integration.md#task-31-implement-full-test-matrix-day-1-3)

---

### Day 3-4: Edge Case Handling

**Criar**: `GenericEdgeCaseHandler.kt`

Lidar com:
- Star projections: `List<*>`
- Recursive generics: `Node<T : Node<T>>`
- Nested generics: `Map<K, List<V>>`

**Estratégia**: Fallback gracioso para casos complexos

**Referência**: [Phase 3, Task 3.2](./phase3-testing-integration.md#task-32-edge-case-handling-day-3-4)

---

### Day 4-5: Performance Benchmarking

**Teste**: Compilation time overhead

**Target**: <10% overhead vs non-generic baseline

```kotlin
Baseline (10 non-generic): 2.5s
Generic (10 generic): 2.7s
Overhead: 8% ✅ PASS
```

**Referência**: [Phase 3, Task 3.3](./phase3-testing-integration.md#task-33-performance-benchmarking-day-4-5)

---

### Day 5-7: Documentation & Examples

**Atualizar**:
- [ ] `samples/single-module/src/.../TestService.kt` - exemplos funcionando
- [ ] `CLAUDE.md` - mover generics para "Funcionando"
- [ ] `README.md` - adicionar generic support
- [ ] Current status docs

**Referência**: [Phase 3, Tasks 3.4-3.5](./phase3-testing-integration.md#task-34-documentation--examples-day-5-6)

---

## 🚀 Quarta Semana - Phase 4: SAM Interfaces (BONUS!) ⭐

**GREAT NEWS**: SAM interfaces já funcionam! ✨

**Status**: 80% Complete - Code generation working, 2 bugs to fix

**Discovery**:
A infraestrutura das Phases 1-3 automaticamente suporta SAM (`fun interface`) porque:
1. SAM interfaces são `ClassKind.INTERFACE` no Kotlin IR (como qualquer interface)
2. GenericIrSubstitutor trata type parameters corretamente
3. Generators preservam a assinatura do método único

**Exemplo de Generated Code** (Já Funcionando!):
```kotlin
// Source:
@Fake
fun interface Transformer<T> {
    fun transform(input: T): T
}

// Generated (PRODUCTION-READY!):
class FakeTransformerImpl<T> : Transformer<T> {
    private var transformBehavior: (T) -> T = { it }
    override fun transform(input: T): T = transformBehavior(input)
}

inline fun <reified T> fakeTransformer(
    configure: FakeTransformerConfig<T>.() -> Unit = {}
): Transformer<T> = FakeTransformerImpl<T>().apply {
    FakeTransformerConfig<T>(this).configure()
}
```

**Usage (Type-Safe!):**
```kotlin
val stringTransformer = fakeTransformer<String> {
    transform { input -> input.uppercase() }
}

val result: String = stringTransformer.transform("hello")  // ✅ TYPE SAFE!
assertEquals("HELLO", result)
```

### O que falta (Apenas 20%):

**Day 1: Fix Varargs Bug** ⚠️
```kotlin
// Bug: VarargsProcessor gera código inválido
@Fake fun interface VarargsProcessor {
    fun process(vararg items: String): List<String>
}

// Generated (WRONG):
private var processBehavior: (vararg String) -> List<String>  // ❌

// Should be:
private var processBehavior: (Array<out String>) -> List<String>  // ✅
```

**Fix**: Convert varargs to `Array<out T>` in ImplementationGenerator

**Day 2: Fix Star Projections** ⚠️
```kotlin
// Bug: StarProjectionHandler perde assinatura
@Fake fun interface StarProjectionHandler {
    fun handle(items: List<*>): Int
}

// Generated (WRONG):
override fun handle(items: List<Any?>): Int  // ❌ overrides nothing

// Should be:
override fun handle(items: List<*>): Int  // ✅
```

**Fix**: Preserve `*` syntax in TypeResolver

**Day 3: Run All 77 SAM Tests**
```bash
./gradlew :samples:single-module:jvmTest --tests "*SAM*"
```

**Target**: 73+/77 testes passando (95%+)

**Test Coverage** (77 tests):
| File | Tests | Coverage |
|------|-------|----------|
| SAMBasicTest | 8 | P0: Primitives, nullables |
| SAMGenericClassTest | 10 | P0: Generics |
| SAMCollectionsTest | 10 | P1: Collections |
| SAMStdlibTypesTest | 12 | P1: Stdlib |
| SAMHigherOrderTest | 10 | P2: Higher-order |
| SAMVarianceTest | 13 | P2: Variance |
| SAMEdgeCasesTest | 14 | P3: Edge cases |

**Referência**: [Phase 4: SAM Interfaces](./phase4-sam-interfaces.md)

**Why This Matters**:
Esta fase demonstra o **ROI de arquitetura sólida**! Investimos 3 semanas nas Phases 1-3
construindo infraestrutura robusta. Resultado: SAM support veio 80% "de graça" porque a
base estava correta. **Qualidade multiplica valor.**

---

## ✅ Validation Checklist Final

Antes de considerar DONE:

- [ ] `fakeRepository<User> {}` compila sem erros
- [ ] Type safety funciona: `val user: User = repo.save(user)`
- [ ] Cache<K, V> funciona (múltiplos parâmetros)
- [ ] Tests P0 passando 100%
- [ ] Tests P1-P2 passando >90%
- [ ] Performance <10% overhead
- [ ] `publishToMavenLocal` + test em projeto real
- [ ] Documentação completa

---

## 🚨 Se Travar

### Problema: "IrTypeSubstitutor não funciona"
**Solução**: Consulte [Technical Reference](./technical-reference.md) - seção "Common Pitfalls"

### Problema: "Tests não passam"
**Solução**: Use kotlin-compile-testing multi-stage validation - [Test Matrix](./test-matrix.md)

### Problema: "Generated code não compila"
**Solução**: Verifique preservação de type parameters em TypeResolver

### Problema: "Dúvida sobre Metro patterns"
**Solução**: `/validate-metro-alignment` ou consulte Metro source

---

## 📚 Ordem de Leitura Recomendada

1. ✅ Este Quick Start (você está aqui!)
2. [ROADMAP.md](./ROADMAP.md) - 10 min
3. [Technical Reference](./technical-reference.md) - 30 min
4. [Phase 1](./phase1-core-infrastructure.md) - começar codando
5. [Test Matrix](./test-matrix.md) - quando escrever tests

**Total reading time**: ~1 hora antes de começar

---

## 🎯 Mentalidade Correta

### ✅ FAZER:
- TDD: write tests first
- Metro patterns: check alignment
- Type safety: validate at use-site
- GIVEN-WHEN-THEN: the absolute standard
- Incremental: one phase at a time

### ❌ NÃO FAZER:
- Skip tests (temptation!)
- Ignore Metro (critical!)
- Type erasure shortcuts (avoid!)
- Batch all phases (risky!)
- Skip documentation (future pain!)

---

## 🚀 Let's Go!

**Primeiro comando**:

```bash
# Start todo tracking
# Item 1: Review all documentation

# Then create the file:
touch compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/GenericIrSubstitutor.kt
```

**Boa sorte! 🎉**

Se tiver dúvidas:
- Consulte documentação em `.claude/docs/implementation/generics/`
- Use `/consult-kotlin-api IrTypeSubstitutor`
- Use `/validate-metro-alignment`
- Check test-matrix para exemplos

---

**Remember**: We build MAPs, not MVPs. Quality over speed! 🏆
