# 🔧 KTLINT & DETEKT CLEANUP PLAN

> **Gerado em:** 2025-10-03
> **Última Atualização:** 2025-10-03 09:00
> **Status:** 🟡 EM PROGRESSO - Pattern 1 e 3 concluídos
> **Objetivo:** Resolver issues de linting e complexidade de código

## 📊 Status Atual

**Total de Issues:**
- **Ktlint:** 4 issues (2 compiler + 2 sample)
- **Detekt:** 67 weighted issues
  - Complexity: 23 issues
    - CyclomaticComplexMethod: 8
    - LongMethod: 3
    - NestedBlockDepth: 6
    - TooManyFunctions: 4
    - Outros: 2
  - Outros: 44 issues

---

## 🎯 Estratégia de Correção por Pattern

### Pattern 1: **Ktlint - Formatação Simples** (4 issues)

**Prioridade:** 🟢 BAIXA - Quick wins
**Esforço:** 5 minutos
**Impacto:** Build verde ✅

#### 📋 Issues Identificados

| Arquivo | Linha | Problema | Fix |
|---------|-------|----------|-----|
| `ServiceLoaderValidationTest.kt` | 162 | Max line length (141 chars) | Quebrar linha |
| `ServiceLoaderValidationTest.kt` | 190 | Max line length (141 chars) | Quebrar linha |
| `BasicValidationTest.kt` | 5 | Wildcard import | Import específico |
| `RealWorldValidationTest.kt` | 5 | Wildcard import | Import específico |

#### ✅ Ações

**Fase 1.1: ServiceLoaderValidationTest.kt**
```kotlin
// ❌ Antes (141 chars):
"FaktCommandLineProcessor should be in $expectedPackage package. Found: $className. If you moved this class, update META-INF/services..."

// ✅ Depois:
"FaktCommandLineProcessor should be in $expectedPackage package. " +
    "Found: $className. If you moved this class, update META-INF/services..."
```

**Fase 1.2: BasicValidationTest.kt e RealWorldValidationTest.kt**
```kotlin
// ❌ Antes:
import kotlin.test.*

// ✅ Depois:
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
```

**Validação:**
```bash
./gradlew :compiler:ktlintCheck
./gradlew :samples:single-module:ktlintCheck
```

---

### Pattern 2: **Detekt - Métodos Borderline** (2 issues)

**Prioridade:** 🟢 BAIXA - Aceitável
**Esforço:** 0 minutos (skip)
**Decisão:** Documentar e manter

#### 📋 Issues no Limite (Aceitáveis)

| Arquivo | Método | Métrica | Valor | Limite | Status |
|---------|--------|---------|-------|--------|--------|
| `TypeResolver.kt` | `handleFunctionType` | Complexity | 15 | 15 | ✅ NO LIMITE |
| `KtFakeCompilerPluginRegistrar.kt` | `registerExtensions` | Lines | 62 | 60 | ✅ NO LIMITE |

#### ✅ Ação

**Documentar como "Conhecido e Aceitável":**
- Métodos no limite são permitidos quando bem estruturados
- registerExtensions() é entry point do plugin - naturalmente mais longo
- handleFunctionType() lida com Function0-22 - complexidade justificada

**Status:** ✅ SKIP - Manter como está

---

### Pattern 3: **Detekt - Complexidade Moderada** (4 issues)

**Prioridade:** 🟡 MÉDIA - Refactoring recomendado
**Esforço:** 2-3 horas
**Benefício:** Código mais testável e manutenível

#### 📋 Issues Identificados

| Arquivo | Método | Problema | Acima do Limite |
|---------|--------|----------|-----------------|
| `UnifiedFaktIrGenerationExtension.kt` | `generate()` | 73 linhas + nested | +21% |
| `TypeResolver.kt` | `getDefaultValue()` | Complexity 19 | +26% |
| `TypeResolver.kt` | `handleGenericType()` | Complexity 19 | +26% |
| `TypeResolver.kt` | `irTypeToKotlinString()` | Complexity 25 | +66% |

#### ✅ Ações

**Fase 3.1: UnifiedFaktIrGenerationExtension.kt - generate()**

*Problema:* Método muito longo com validação de generics inline

*Solução:*
```kotlin
// Extrair validação de generics para método separado
private fun validateGenericInterface(fakeInterface: IrClass): GenericValidationResult {
    val genericParams = fakeInterface.typeParameters
    return when {
        genericParams.isEmpty() -> GenericValidationResult.Valid
        // ... lógica de validação
    }
}
```

*Benefícios:*
- generate() reduzido para ~50 linhas
- Validação testável separadamente
- NestedBlockDepth resolvido

---

**Fase 3.2: TypeResolver.kt - getDefaultValue()**

*Problema:* Grande when com 19 branches de complexidade

*Solução:*
```kotlin
// Extrair defaults primitivos para função dedicada
private fun getPrimitiveDefault(irType: IrType): String? = when {
    irType.isString() -> "\"\""
    irType.isInt() -> "0"
    irType.isBoolean() -> "false"
    // ... outros primitivos
    else -> null
}

fun getDefaultValue(irType: IrType): String =
    getPrimitiveDefault(irType) ?: handleClassDefault(irType)
```

*Benefícios:*
- Complexity de 19 → ~10
- Lógica primitiva separada de lógica complexa
- Mais fácil adicionar novos tipos

---

**Fase 3.3: TypeResolver.kt - handleGenericType()**

*Problema:* Conversão de type arguments inline aumenta complexidade

*Solução:*
```kotlin
// Extrair conversão de type arguments
private fun typeArgumentsToString(
    arguments: List<IrTypeArgument>,
    preserveTypeParameters: Boolean
): String {
    if (arguments.isEmpty()) return ""

    val args = arguments.mapNotNull { arg ->
        when (arg) {
            is IrType -> irTypeToKotlinString(arg, preserveTypeParameters)
            else -> null
        }
    }
    return if (args.isNotEmpty()) "<${args.joinToString(", ")}>" else ""
}
```

*Benefícios:*
- Complexity de 19 → ~12
- Reutilizável em outros contextos
- Testes unitários dedicados

---

**Fase 3.4: TypeResolver.kt - irTypeToKotlinString()**

*Problema:* Método faz muitas coisas - function types, generics, arrays, nullable

*Solução:*
```kotlin
// Extrair conversão de function types
private fun functionTypeToString(
    irType: IrType,
    preserveTypeParameters: Boolean
): String? {
    // Lógica específica de Function0-22
}

// Extrair conversão de generic types
private fun genericTypeToString(
    irType: IrSimpleType,
    preserveTypeParameters: Boolean
): String {
    // Lógica específica de generics
}

fun irTypeToKotlinString(irType: IrType, preserveTypeParameters: Boolean): String =
    when {
        isFunctionType(irType) -> functionTypeToString(irType, preserveTypeParameters)
        irType is IrSimpleType -> genericTypeToString(irType, preserveTypeParameters)
        // ... outros casos
    }
```

*Benefícios:*
- Complexity de 25 → ~8
- Cada tipo tem lógica dedicada
- Muito mais testável

---

**Validação Pattern 3:**
```bash
# Após cada fase, rodar:
./gradlew :compiler:test -x detekt -x ktlintCheck

# Validação final:
./gradlew :compiler:detekt | grep "TypeResolver\|UnifiedFaktIrGenerationExtension"
```

**Tempo estimado:** 2-3 horas

---

### Pattern 4: **Detekt - Complexidade Alta** (3 issues) ⚠️

**Prioridade:** 🔴 ALTA - Precisa refactoring
**Esforço:** 4-6 horas
**Benefício:** Código profissional e manutenível

#### 📋 Issues Críticos

| Arquivo | Método | Problema | Acima do Limite |
|---------|--------|----------|-----------------|
| `ImplementationGenerator.kt` | `generateImplementation()` | **164 linhas** 🚨 | +273% |
| `ImplementationGenerator.kt` | `generateKotlinStdlibDefault()` | Complexity 33 🚨 | +120% |
| `TypeResolver.kt` | `handleClassDefault()` | Complexity 24 | +60% |

#### ✅ Ações

**Fase 4.1: ImplementationGenerator.kt - generateImplementation() 🚨**

*Problema:* Método gigante de 164 linhas fazendo tudo - behavior properties, method overrides, config methods

*Solução - Refactoring completo:*

```kotlin
// 1️⃣ Extrair geração de behavior properties
private fun generateBehaviorProperties(analysis: InterfaceAnalysis): String =
    buildString {
        analysis.methods.forEach { method ->
            appendLine("    private var ${method.name}Behavior: ${method.behaviorType} = ${method.defaultBehavior}")
        }
        analysis.properties.forEach { property ->
            appendLine("    private var ${property.name}Behavior: ${property.behaviorType} = ${property.defaultBehavior}")
        }
    }

// 2️⃣ Extrair geração de method overrides
private fun generateMethodOverrides(analysis: InterfaceAnalysis): String =
    buildString {
        analysis.methods.forEach { method ->
            appendLine("""
                override ${method.signature} {
                    return ${method.name}Behavior(${method.params})
                }
            """.trimIndent())
        }
    }

// 3️⃣ Extrair geração de config methods
private fun generateConfigMethods(analysis: InterfaceAnalysis): String =
    buildString {
        analysis.methods.forEach { method ->
            appendLine("    internal fun configure${method.name.capitalize()}(behavior: ${method.behaviorType}) {")
            appendLine("        ${method.name}Behavior = behavior")
            appendLine("    }")
        }
    }

// 4️⃣ Método principal agora é simples orquestrador
fun generateImplementation(analysis: InterfaceAnalysis, fakeClassName: String): String =
    buildString {
        appendLine("class $fakeClassName : ${analysis.interfaceName} {")
        append(generateBehaviorProperties(analysis))
        appendLine()
        append(generateMethodOverrides(analysis))
        appendLine()
        append(generateConfigMethods(analysis))
        appendLine("}")
    }
```

*Benefícios:*
- 164 linhas → ~40 linhas no método principal
- Cada fase é testável independentemente
- Complexity drasticamente reduzida
- Single Responsibility Principle

---

**Fase 4.2: ImplementationGenerator.kt - generateKotlinStdlibDefault() 🚨**

*Problema:* When giant com 33 branches de complexidade para todos os tipos Kotlin

*Solução - Separar por categoria:*

```kotlin
// 1️⃣ Primitivos
private fun getPrimitiveDefaults(typeString: String): String? = when (typeString) {
    "String" -> "\"\""
    "Int" -> "0"
    "Long" -> "0L"
    "Boolean" -> "false"
    "Double" -> "0.0"
    "Float" -> "0.0f"
    "Byte" -> "0"
    "Short" -> "0"
    "Char" -> "'\\u0000'"
    else -> null
}

// 2️⃣ Collections
private fun getCollectionDefaults(typeString: String): String? = when {
    typeString.startsWith("List") -> "emptyList()"
    typeString.startsWith("Set") -> "emptySet()"
    typeString.startsWith("Map") -> "emptyMap()"
    typeString.startsWith("MutableList") -> "mutableListOf()"
    typeString.startsWith("MutableSet") -> "mutableSetOf()"
    typeString.startsWith("MutableMap") -> "mutableMapOf()"
    typeString.startsWith("Array") -> "emptyArray()"
    else -> null
}

// 3️⃣ Kotlin stdlib special types
private fun getKotlinStdlibDefaults(typeString: String): String? = when {
    typeString.startsWith("Result") -> "Result.success(Unit)"
    typeString.startsWith("Sequence") -> "emptySequence()"
    typeString.startsWith("Flow") -> "emptyFlow()"
    typeString.startsWith("Pair") -> "Pair(null, null)"
    typeString == "Unit" -> "Unit"
    else -> null
}

// 4️⃣ Método principal orquestra
fun generateKotlinStdlibDefault(typeString: String): String =
    getPrimitiveDefaults(typeString)
        ?: getCollectionDefaults(typeString)
        ?: getKotlinStdlibDefaults(typeString)
        ?: handleDomainType(typeString)
```

*Benefícios:*
- Complexity de 33 → ~5 por função
- Fácil adicionar novos tipos por categoria
- Testável por categoria
- Lógica clara e organizada

---

**Fase 4.3: TypeResolver.kt - handleClassDefault()**

*Problema:* Complexity 24 com lógica de Result/Collection inline

*Solução:*

```kotlin
// Extrair defaults específicos
private fun getResultDefault(irType: IrType): String {
    val typeArg = extractFirstTypeArgument(irType)
    return "Result.success($typeArg)"
}

private fun getCollectionDefault(irClass: IrClass, irType: IrType): String {
    val className = irClass.name.asString()
    return when {
        className.startsWith("List") -> "emptyList()"
        className.startsWith("Set") -> "emptySet()"
        className.startsWith("Map") -> "emptyMap()"
        else -> "null"
    }
}

fun handleClassDefault(irType: IrType): String {
    val irClass = irType.getClass() ?: return "null"
    val className = irClass.name.asString()

    return when {
        className == "Result" -> getResultDefault(irType)
        isCollectionType(className) -> getCollectionDefault(irClass, irType)
        // ... outros casos
        else -> handleDomainType(irType)
    }
}
```

*Benefícios:*
- Complexity de 24 → ~12
- Lógica de Result separada
- Lógica de Collection separada
- Extensível para novos tipos

---

**Validação Pattern 4:**
```bash
# Após cada fase:
./gradlew :compiler:test --tests "*ImplementationGenerator*"
./gradlew :compiler:test --tests "*TypeResolver*"

# Validação final:
./gradlew :compiler:detekt | grep -E "ImplementationGenerator|TypeResolver"
```

**Tempo estimado:** 4-6 horas

---

### Pattern 5: **Detekt - Complexidade Extrema** (1 issue) 🚨🔥

**Prioridade:** 🔴 CRÍTICA - Refactoring obrigatório
**Esforço:** 3-4 horas
**Benefício:** Arquitetura limpa e extensível

#### 📋 Issue Crítico

| Arquivo | Método | Problema | Status |
|---------|--------|----------|--------|
| `SourceSetMapper.kt` | `mapToTestSourceSet()` | **Complexity 53** 🔥 | +353% |
| `SourceSetMapper.kt` | - | TooManyFunctions (13) | +18% |

#### ⚠️ Análise do Problema

**Atual - When Statement Gigante:**
```kotlin
private fun mapToTestSourceSet(moduleName: String): String {
    val normalizedName = moduleName.lowercase()

    return when {
        // Tier 1: Common (2 cases)
        normalizedName.contains("commonmain") -> "commonTest"

        // Tier 2: Platform categories (4 cases)
        normalizedName.contains("nativemain") -> "nativeTest"

        // Tier 3: Specific platforms (4 cases)
        normalizedName.contains("jvmmain") -> "jvmTest"

        // Tier 4: Apple platforms (4 cases)
        normalizedName.contains("iosmain") -> "iosTest"

        // Tier 5: Platform variants (20 cases!) 🚨
        normalizedName.contains("iosarm64main") -> "iosArm64Test"
        normalizedName.contains("iosx64main") -> "iosX64Test"
        // ... 18 more cases

        // Tier 6: Android (1 case)
        normalizedName.contains("androidmain") -> resolveAndroidTestTarget(normalizedName)

        // Tier 7: Legacy (1 case)
        normalizedName.contains("main") -> "test"

        // Default fallbacks (10 cases)
        normalizedName.contains("jvm") -> "jvmTest"
        // ... 9 more cases

        // Ultimate fallback
        else -> intelligentFallback(normalizedName)
    }
}
```

**Problema:** 53 branches em um único método! 🔥

#### ✅ Solução - Strategy Pattern

**Arquitetura proposta:**

```kotlin
// 1️⃣ Interface Strategy
interface SourceSetMappingStrategy {
    fun canHandle(moduleName: String): Boolean
    fun mapToTestSourceSet(moduleName: String): String
    fun getFallbacks(): List<String>
}

// 2️⃣ Common Strategy
class CommonSourceSetStrategy : SourceSetMappingStrategy {
    override fun canHandle(moduleName: String): Boolean =
        moduleName.contains("commonmain", ignoreCase = true)

    override fun mapToTestSourceSet(moduleName: String): String = "commonTest"

    override fun getFallbacks(): List<String> = emptyList()
}

// 3️⃣ Apple Platform Strategy
class ApplePlatformStrategy : SourceSetMappingStrategy {
    private val appleTargets = mapOf(
        "iosmain" to "iosTest",
        "macosmain" to "macosTest",
        "tvosmain" to "tvosTest",
        "watchosmain" to "watchosTest",
        // Platform variants
        "iosarm64main" to "iosArm64Test",
        "iosx64main" to "iosX64Test",
        "iossimulatorarm64main" to "iosSimulatorArm64Test",
        // ... outros
    )

    override fun canHandle(moduleName: String): Boolean =
        appleTargets.keys.any { moduleName.contains(it, ignoreCase = true) }

    override fun mapToTestSourceSet(moduleName: String): String {
        val entry = appleTargets.entries.find { (key, _) ->
            moduleName.contains(key, ignoreCase = true)
        }
        return entry?.value ?: "iosTest"
    }

    override fun getFallbacks(): List<String> =
        listOf("appleTest", "nativeTest", "commonTest")
}

// 4️⃣ Android Platform Strategy
class AndroidPlatformStrategy : SourceSetMappingStrategy {
    override fun canHandle(moduleName: String): Boolean =
        moduleName.contains("android", ignoreCase = true)

    override fun mapToTestSourceSet(moduleName: String): String {
        // Instrumented vs Unit tests
        return when {
            moduleName.contains("androidnative") -> resolveAndroidNativeTest(moduleName)
            else -> "androidUnitTest"
        }
    }

    override fun getFallbacks(): List<String> = listOf("commonTest")
}

// 5️⃣ JVM Platform Strategy
class JvmPlatformStrategy : SourceSetMappingStrategy {
    override fun canHandle(moduleName: String): Boolean =
        moduleName.contains("jvm", ignoreCase = true)

    override fun mapToTestSourceSet(moduleName: String): String = "jvmTest"

    override fun getFallbacks(): List<String> = listOf("commonTest")
}

// 6️⃣ Native Platform Strategy
class NativePlatformStrategy : SourceSetMappingStrategy {
    private val nativeTargets = mapOf(
        "linuxmain" to "linuxTest",
        "mingwmain" to "mingwTest",
        "linuxarm64main" to "linuxArm64Test",
        "linuxx64main" to "linuxX64Test",
        "mingwx64main" to "mingwX64Test",
    )

    override fun canHandle(moduleName: String): Boolean =
        nativeTargets.keys.any { moduleName.contains(it, ignoreCase = true) }

    override fun mapToTestSourceSet(moduleName: String): String {
        val entry = nativeTargets.entries.find { (key, _) ->
            moduleName.contains(key, ignoreCase = true)
        }
        return entry?.value ?: "nativeTest"
    }

    override fun getFallbacks(): List<String> = listOf("commonTest")
}

// 7️⃣ Refactored SourceSetMapper
internal class SourceSetMapper(
    private val outputDir: String?,
    private val messageCollector: MessageCollector?,
) {
    private val strategies = listOf(
        CommonSourceSetStrategy(),
        ApplePlatformStrategy(),
        AndroidPlatformStrategy(),
        JvmPlatformStrategy(),
        NativePlatformStrategy(),
    )

    private fun mapToTestSourceSet(moduleName: String): String {
        val normalizedName = moduleName.lowercase()

        // Find matching strategy
        val strategy = strategies.find { it.canHandle(normalizedName) }

        return strategy?.mapToTestSourceSet(normalizedName)
            ?: intelligentFallback(normalizedName)
    }

    // ... resto dos métodos mantidos
}
```

#### 🎯 Benefícios da Solução

**Antes:**
- ❌ 1 método com complexity 53
- ❌ 46 branches no when
- ❌ 13 funções na classe
- ❌ Impossível testar estratégias individuais
- ❌ Difícil adicionar novos targets

**Depois:**
- ✅ 5 strategies com complexity ~5 cada
- ✅ Cada strategy é testável independentemente
- ✅ Fácil adicionar novos targets (criar nova strategy)
- ✅ Classe principal com apenas 7-8 funções
- ✅ Código limpo e SOLID

**Complexity Reduction:**
- `mapToTestSourceSet()`: 53 → 3 ✅
- `SourceSetMapper`: 13 funções → 8 funções ✅

#### 📋 Implementação Passo a Passo

**Fase 5.1: Criar interface e Common Strategy (30 min)**
```bash
# Criar SourceSetMappingStrategy.kt
# Criar CommonSourceSetStrategy.kt
# Testes unitários para Common
```

**Fase 5.2: Implementar Apple Strategy (45 min)**
```bash
# Criar ApplePlatformStrategy.kt
# Migrar 20 variants do Apple
# Testes unitários abrangentes
```

**Fase 5.3: Implementar Android, JVM, Native Strategies (1h)**
```bash
# Criar AndroidPlatformStrategy.kt
# Criar JvmPlatformStrategy.kt
# Criar NativePlatformStrategy.kt
# Testes unitários para cada
```

**Fase 5.4: Refatorar SourceSetMapper (1h)**
```bash
# Integrar strategies
# Remover when gigante
# Manter fallback logic
# Testes de integração
```

**Fase 5.5: Validação final (30 min)**
```bash
# Rodar todos os testes
./gradlew :compiler:test --tests "*SourceSetMapper*"

# Validar detekt
./gradlew :compiler:detekt | grep "SourceSetMapper"

# Validar com sample project
./gradlew :samples:single-module:build
```

---

**Validação Pattern 5:**
```bash
# Build completo
./gradlew :compiler:build -x ktlintCheck

# Verificar metrics
./gradlew :compiler:detekt | grep -A 10 "SourceSetMapper"
```

**Tempo estimado:** 3-4 horas

---

### Pattern 6: **Detekt - TooManyFunctions** (4 issues)

**Prioridade:** 🟡 MÉDIA - Será resolvido automaticamente
**Esforço:** 0 horas (side effect de outros patterns)
**Status:** Monitorar

#### 📋 Issues

| Arquivo | Funções | Limite | Status |
|---------|---------|--------|--------|
| `ImplementationGenerator.kt` | 11 | 11 | ✅ NO LIMITE |
| `SourceSetMapper.kt` | 13 | 11 | 🔄 Pattern 5 resolve |
| `GenericPatternAnalyzer.kt` | 13 | 11 | 🤔 Avaliar |
| (outro arquivo) | ? | 11 | 🔍 Identificar |

#### ✅ Ações

**ImplementationGenerator:**
- ✅ NO LIMITE - aceitável para generator class
- Pattern 4 pode adicionar funções auxiliares (OK)

**SourceSetMapper:**
- 🔄 Pattern 5 resolve automaticamente
- Strategy pattern distribui responsabilidades

**GenericPatternAnalyzer:**
- 🤔 Avaliar se é crítico
- Analyzer classes tendem a ter mais funções
- Possivelmente aceitável dado o domínio (generic analysis)

**Decisão:** Monitorar após Patterns 3, 4, 5. Se necessário, extrair analyzer helpers.

---

## 📋 Plano de Execução

### 🟢 Sprint 1: Quick Wins (30 minutos)

**Objetivo:** Zero ktlint errors

#### Tarefas:
- [ ] **1.1** - Quebrar linhas em ServiceLoaderValidationTest.kt (162, 190)
- [ ] **1.2** - Substituir wildcard imports em BasicValidationTest.kt
- [ ] **1.3** - Substituir wildcard imports em RealWorldValidationTest.kt
- [ ] **1.4** - Rodar ktlintCheck e validar zero errors
- [ ] **1.5** - Documentar Pattern 2 como "aceitável"

**Validação:**
```bash
./gradlew ktlintCheck
```

**Critério de sucesso:** ✅ Zero ktlint errors

---

### 🟡 Sprint 2: Refactoring Médio (4 horas)

**Objetivo:** Resolver complexidade moderada (Pattern 3)

#### Tarefas:
- [ ] **3.1** - UnifiedFaktIrGenerationExtension: extrair validateGenericInterface() (45 min)
- [ ] **3.2** - TypeResolver: extrair getPrimitiveDefault() (30 min)
- [ ] **3.3** - TypeResolver: extrair typeArgumentsToString() (45 min)
- [ ] **3.4** - TypeResolver: extrair functionTypeToString() e genericTypeToString() (1h)
- [ ] **3.5** - Testes unitários para métodos extraídos (1h)
- [ ] **3.6** - Validação detekt Pattern 3 (30 min)

**Validação:**
```bash
./gradlew :compiler:test
./gradlew :compiler:detekt | grep -E "UnifiedFakt|TypeResolver"
```

**Critério de sucesso:** Complexity reduzida em 30-40%

---

### 🔴 Sprint 3: Refactoring Alto (8 horas)

**Objetivo:** Resolver issues críticos (Patterns 4 e 5)

#### Pattern 4: ImplementationGenerator & TypeResolver (4h)

**Tarefas:**
- [ ] **4.1.1** - ImplementationGenerator: extrair generateBehaviorProperties() (45 min)
- [ ] **4.1.2** - ImplementationGenerator: extrair generateMethodOverrides() (45 min)
- [ ] **4.1.3** - ImplementationGenerator: extrair generateConfigMethods() (45 min)
- [ ] **4.1.4** - Testes para generateImplementation() refatorado (45 min)
- [ ] **4.2.1** - ImplementationGenerator: separar generateKotlinStdlibDefault() por categorias (1h)
- [ ] **4.2.2** - Testes por categoria (primitives, collections, stdlib) (30 min)
- [ ] **4.3** - TypeResolver: simplificar handleClassDefault() (30 min)

#### Pattern 5: SourceSetMapper Strategy Pattern (4h)

**Tarefas:**
- [ ] **5.1** - Criar interface SourceSetMappingStrategy (30 min)
- [ ] **5.2** - Implementar CommonSourceSetStrategy (30 min)
- [ ] **5.3** - Implementar ApplePlatformStrategy (45 min)
- [ ] **5.4** - Implementar AndroidPlatformStrategy (30 min)
- [ ] **5.5** - Implementar JvmPlatformStrategy (15 min)
- [ ] **5.6** - Implementar NativePlatformStrategy (30 min)
- [ ] **5.7** - Refatorar SourceSetMapper principal (1h)
- [ ] **5.8** - Testes unitários para cada strategy (1h)

**Validação:**
```bash
./gradlew :compiler:test
./gradlew :compiler:detekt
./gradlew :samples:single-module:build
```

**Critério de sucesso:** Complexity reduzida em 60-70%

---

### ✅ Sprint 4: Validação Final (1 hora)

**Objetivo:** Confirmar zero issues críticos

#### Tarefas:
- [ ] **V.1** - Rodar detekt e analisar report completo (15 min)
- [ ] **V.2** - Rodar ktlint em todos os módulos (10 min)
- [ ] **V.3** - Build completo com todos os checks (20 min)
- [ ] **V.4** - Documentar issues aceitáveis (borderline) (15 min)

**Validação final:**
```bash
./gradlew clean build
./gradlew detekt
./gradlew ktlintCheck
```

**Critério de sucesso:**
- ✅ Zero ktlint errors
- ✅ Zero detekt issues críticos (complexity > 40)
- ✅ Build verde em todos os módulos

---

## 🎯 Objetivos de Sucesso

### ✅ Mínimo (Sprint 1)
- [x] Zero ktlint errors
- [x] Documentar issues borderline
- [x] Build verde

**Tempo:** 30 minutos
**Impacto:** Build limpo para desenvolvimento

### ✅ Ideal (Sprint 1-2)
- [x] Zero ktlint errors
- [x] Resolver complexidade moderada (Pattern 3)
- [x] Reduzir issues detekt em 30%

**Tempo:** 4.5 horas
**Impacto:** Código mais manutenível e testável

### ✅ Excelente (Sprint 1-3)
- [x] Zero ktlint errors
- [x] Resolver todos patterns 3, 4, 5
- [x] Reduzir issues detekt em 70%
- [x] Código profissional e manutenível

**Tempo:** 12.5 horas
**Impacto:** Arquitetura limpa, extensível e production-ready

---

## 📊 Métricas de Progresso

### Status Inicial
- **Ktlint:** 4 issues
- **Detekt:** 67 weighted issues
  - CyclomaticComplexMethod: 8
  - LongMethod: 3
  - NestedBlockDepth: 6
  - TooManyFunctions: 4

### Status Alvo (Após Sprint 3)
- **Ktlint:** 0 issues ✅
- **Detekt:** ~20 weighted issues (70% redução)
  - CyclomaticComplexMethod: 2-3 (todos < 18)
  - LongMethod: 0-1
  - NestedBlockDepth: 2-3
  - TooManyFunctions: 1-2

### Issues Aceitáveis (Documentados)
- TypeResolver.handleFunctionType (complexity 15) - Justificado
- KtFakeCompilerPluginRegistrar.registerExtensions (62 linhas) - Entry point
- GenericPatternAnalyzer (13 funções) - Analyzer complexo

---

## 🔍 Notas Técnicas

### Quando Aceitar Complexity?

**✅ Aceitável:**
- Entry points de plugins (naturalmente mais longos)
- Análise de domínio complexo (GenericPatternAnalyzer)
- Métodos no limite (15, 60) bem estruturados

**❌ Não aceitável:**
- Complexity > 30 (refactor obrigatório)
- Métodos > 100 linhas (quebrar em funções)
- When statements gigantes (usar strategy pattern)

### Padrões de Refactoring

**Extract Method:**
- Usado em Patterns 3 e 4
- Ideal para: lógica repetida, branches complexos

**Strategy Pattern:**
- Usado em Pattern 5
- Ideal para: when gigantes, múltiplas implementações

**Category Methods:**
- Usado em Pattern 4.2
- Ideal para: tipos de dados agrupáveis (primitives, collections)

---

## 📝 Conclusão

Este plano organiza sistematicamente os problemas de linting por patterns de solução, priorizando quick wins e refactorings de alto impacto. Seguir os sprints sequencialmente garante progresso mensurável e build sempre verde.

**Próximos passos:** Começar com Sprint 1 (Quick Wins) para ter build limpo imediatamente.
