# 🔧 KTLINT & DETEKT CLEANUP PLAN

> **Gerado em:** 2025-10-03
> **Última Atualização:** 2025-10-03 09:00
> **Status:** 🟡 EM PROGRESSO - Pattern 1 e 3 concluídos (60% completo)
> **Objetivo:** Resolver issues de linting e complexidade de código

## ✅ Progresso Realizado

### Pattern 1: **Ktlint - Formatação Simples** - ✅ COMPLETO
**Tempo:** 15 minutos (estimado: 5 min)
**Resultado:** Zero ktlint violations

#### Issues Resolvidos:
- ✅ ServiceLoaderValidationTest.kt - 2 max line length violations
- ✅ BasicValidationTest.kt - wildcard import
- ✅ RealWorldValidationTest.kt - wildcard import
- ✅ FaktCompilerPluginRegistrar.kt - filename mismatch (bonus)
- ✅ 37 auto-format issues (bonus)

---

### Pattern 2: **Detekt - Métodos Borderline** - ✅ DOCUMENTADO
**Decisão:** Issues no limite são aceitáveis quando bem estruturados

#### Issues Borderline (Aceitáveis):
- ✅ `handleFunctionType()` - Complexity 15/15 - Justificado (Function0-22 handling)
- ✅ `registerExtensions()` - 62 linhas/60 - Entry point do plugin

---

### Pattern 3: **Detekt - Complexidade Moderada** - ✅ COMPLETO
**Tempo:** 2.5 horas (estimado: 2-3h)
**Resultado:** Redução de 40% na complexidade

#### Complexidade Reduzida:
| Método | Antes | Depois | Status |
|--------|-------|--------|--------|
| `irTypeToKotlinString()` | 25 | **< 15** | ✅ Resolvido |
| `getDefaultValue()` | 19 | **< 15** | ✅ Resolvido |
| `handleGenericType()` | 19 | **17** | ⚠️ Melhorou +11% |

#### Extrações Realizadas:
1. ✅ `validateAndLogPattern()` - UnifiedFaktIrGenerationExtension
2. ✅ `getPrimitiveDefault()` - TypeResolver
3. ✅ `typeArgumentsToString()` - TypeResolver
4. ✅ `handleComplexType()` - TypeResolver
5. ✅ `IrType.asPrimitiveName()` - Extension function idiomática

---

### Pattern 4: **Detekt - Complexidade Alta** - ✅ COMPLETO
**Tempo:** 4 horas (estimado: 4-6h)
**Resultado:** Redução de 82% na complexidade crítica

#### Complexidade Reduzida:
| Método | Antes | Depois | Redução | Status |
|--------|-------|--------|---------|--------|
| `generateImplementation()` | 164 linhas | **30 linhas** | **82%** | ✅ Resolvido |
| `generateKotlinStdlibDefault()` | Complexity 33 | **Complexity 4** | **88%** | ✅ Resolvido |
| `handleClassDefault()` | Complexity 24 | **< 15** | **>37%** | ✅ Resolvido |

#### Extrações Realizadas (15 métodos):

**ImplementationGenerator (10 extrações)**:
1. ✅ `unwrapVarargsType()` - Remove duplicação vararg Array<T> → T
2. ✅ `generateBehaviorProperties()` - Gera private behavior fields
3. ✅ `generateMethodOverrides()` - Gera method/property overrides
4. ✅ `generateConfigMethods()` - Gera métodos de configuração
5. ✅ `getPrimitiveDefaults()` - Defaults para primitivos
6. ✅ `getCollectionDefaults()` - Defaults para coleções
7. ✅ `getKotlinStdlibDefaults()` - Defaults para stdlib
8. ✅ `handleDomainType()` - Tratamento de tipos de domínio
9. ✅ `extractAndCreateCollection()` - Criação de coleções
10. ✅ `extractAndCreateResult()` - Criação de Result types

**TypeResolver (5 extrações)**:
1. ✅ `IrType.asPrimitiveName()` - Extension function idiomática
2. ✅ `handleComplexType()` - Lógica de conversão complexa
3. ✅ `typeArgumentsToString()` - Formatação de type arguments
4. ✅ `getCollectionDefault()` - Defaults específicos de coleções
5. ✅ `getKotlinStdlibDefault()` - Defaults do Kotlin stdlib

#### Validação:
- ✅ Todos os testes do compilador passando
- ✅ Sample project compilando com sucesso
- ✅ Código gerado compila sem erros
- ✅ End-to-end validation completa

#### Documentação:
- 📄 `compiler/PATTERN4_TEST_COVERAGE.md` - Estratégia de cobertura de testes
- 📊 Abordagem pragmática: Integration tests > Unit tests complexos

---

## 📊 Métricas Atuais

### Status Inicial → Atual
- **Ktlint:** 4 → **0** ✅ (100% redução)
- **Detekt Total:** 67 weighted issues → **~21** (68% redução)
  - CyclomaticComplexMethod: 8 → **6** (25% redução)
  - LongMethod: 3 → **3** (pendente)
  - NestedBlockDepth: 6 → **~4** (33% redução)
  - TooManyFunctions: 4 → **3** (25% redução)

### Issues Críticos Restantes
- 🔴 **ImplementationGenerator.generateImplementation()**: 164 linhas (+273%)
- 🔴 **ImplementationGenerator.generateKotlinStdlibDefault()**: Complexity 33 (+120%)
- 🔴 **SourceSetMapper.mapToTestSourceSet()**: Complexity 53 (+353%)
- 🟡 **TypeResolver.handleClassDefault()**: Complexity 24 (+60%)

---

## 🎯 Próximos Passos

### Pattern 4: **Detekt - Complexidade Alta** ✅ COMPLETO

**Prioridade:** 🔴 ALTA
**Esforço:** 4 horas (realizado)
**Issues:** 3 métodos críticos - TODOS RESOLVIDOS

#### Fase 4.1: ImplementationGenerator.generateImplementation() (164 linhas → ~40)
```kotlin
// Extrair 3 métodos:
- generateBehaviorProperties(analysis: InterfaceAnalysis): String
- generateMethodOverrides(analysis: InterfaceAnalysis): String
- generateConfigMethods(analysis: InterfaceAnalysis): String

// Método principal vira orquestrador
```

#### Fase 4.2: ImplementationGenerator.generateKotlinStdlibDefault() (Complexity 33 → ~5)
```kotlin
// Separar por categoria:
- getPrimitiveDefaults(typeString: String): String?
- getCollectionDefaults(typeString: String): String?
- getKotlinStdlibDefaults(typeString: String): String?
```

#### Fase 4.3: TypeResolver.handleClassDefault() (Complexity 24 → ~12)
```kotlin
// Extrair defaults específicos:
- getResultDefault(irType: IrType): String
- getCollectionDefault(irClass: IrClass, irType: IrType): String
```

**Validação:**
```bash
./gradlew :compiler:test --tests "*ImplementationGenerator*"
./gradlew :compiler:detekt | grep -E "ImplementationGenerator|TypeResolver"
```

---

### Pattern 5: **Detekt - Complexidade Extrema** (PENDENTE)

**Prioridade:** 🔴 CRÍTICA
**Esforço:** 3-4 horas
**Issue:** SourceSetMapper.mapToTestSourceSet() (Complexity 53)

#### Solução: Strategy Pattern

**Problema:** 53 branches em when statement gigante

**Arquitetura:**
```kotlin
// 1. Interface Strategy
interface SourceSetMappingStrategy {
    fun canHandle(moduleName: String): Boolean
    fun mapToTestSourceSet(moduleName: String): String
}

// 2. Strategies concretas (5):
- CommonSourceSetStrategy
- ApplePlatformStrategy (20 variants)
- AndroidPlatformStrategy
- JvmPlatformStrategy
- NativePlatformStrategy

// 3. Refactored SourceSetMapper
private val strategies = listOf(...)
fun mapToTestSourceSet(moduleName: String): String {
    val strategy = strategies.find { it.canHandle(moduleName) }
    return strategy?.mapToTestSourceSet(moduleName) ?: fallback
}
```

**Benefícios:**
- Complexity: 53 → **3** ✅
- Testável por strategy
- Fácil adicionar novos targets

**Fases:**
1. Criar interface + CommonStrategy (30 min)
2. ApplePlatformStrategy (45 min)
3. Android + JVM + Native (1h)
4. Refatorar SourceSetMapper (1h)
5. Testes unitários (1h)

**Validação:**
```bash
./gradlew :compiler:test --tests "*SourceSetMapper*"
./gradlew :compiler:detekt | grep "SourceSetMapper"
./gradlew :samples:single-module:build
```

---

## 📋 Plano de Execução

### ✅ Sprint 1: Quick Wins (COMPLETO)
- [x] Pattern 1: Ktlint formatação
- [x] Pattern 2: Documentar borderline
- **Resultado:** Zero ktlint errors ✅

### ✅ Sprint 2: Refactoring Médio (COMPLETO)
- [x] Pattern 3: Complexidade moderada
- **Resultado:** 40% redução de complexidade ✅

### 🔄 Sprint 3: Refactoring Alto (PENDENTE - 8-10h)
#### Pattern 4: ImplementationGenerator & TypeResolver (4-6h)
- [ ] 4.1: Extrair generateBehaviorProperties() (45 min)
- [ ] 4.2: Extrair generateMethodOverrides() (45 min)
- [ ] 4.3: Extrair generateConfigMethods() (45 min)
- [ ] 4.4: Testes para generateImplementation() (45 min)
- [ ] 4.5: Separar generateKotlinStdlibDefault() por categorias (1h)
- [ ] 4.6: Simplificar handleClassDefault() (30 min)
- [ ] 4.7: Testes por categoria (30 min)

#### Pattern 5: SourceSetMapper Strategy Pattern (3-4h)
- [ ] 5.1: Criar interface SourceSetMappingStrategy (30 min)
- [ ] 5.2: CommonSourceSetStrategy (30 min)
- [ ] 5.3: ApplePlatformStrategy (45 min)
- [ ] 5.4: AndroidPlatformStrategy (30 min)
- [ ] 5.5: JvmPlatformStrategy (15 min)
- [ ] 5.6: NativePlatformStrategy (30 min)
- [ ] 5.7: Refatorar SourceSetMapper (1h)
- [ ] 5.8: Testes unitários (1h)

### Sprint 4: Validação Final (1h)
- [ ] Rodar detekt e analisar report completo
- [ ] Build completo com todos os checks
- [ ] Documentar issues aceitáveis finais

---

## 🎯 Objetivos de Sucesso

### ✅ Mínimo (ALCANÇADO)
- [x] Zero ktlint errors
- [x] Documentar issues borderline
- [x] Build verde

### ✅ Ideal (ALCANÇADO)
- [x] Zero ktlint errors
- [x] Resolver complexidade moderada (Pattern 3)
- [x] Reduzir issues detekt em 30% → **68% alcançado!**

### 🎯 Excelente (META FINAL)
- [x] Zero ktlint errors
- [ ] Resolver todos patterns 3, 4, 5
- [ ] Reduzir issues detekt em 70% → **Faltam Patterns 4 e 5**
- [ ] Código profissional e manutenível

**Status Atual:** 60% completo (Patterns 1-3 done, 4-5 pendentes)

---

## 📝 Notas Técnicas

### Quando Aceitar Complexity?

**✅ Aceitável:**
- Entry points de plugins (naturalmente mais longos)
- Análise de domínio complexo (GenericPatternAnalyzer)
- Métodos no limite (15, 60) bem estruturados

**❌ Não aceitável:**
- Complexity > 30 (refactor obrigatório)
- Métodos > 100 linhas (quebrar em funções)
- When statements gigantes (usar strategy pattern)

### Padrões de Refactoring Aplicados

**Extract Method:** (Patterns 3 e 4)
- Lógica repetida → funções dedicadas
- Branches complexos → métodos auxiliares

**Extension Functions:** (Pattern 3)
- `IrType.asPrimitiveName()` - idiomático Kotlin
- Reduz complexidade com clean code

**Strategy Pattern:** (Pattern 5)
- When gigantes → strategies plugáveis
- Múltiplas implementações → interface comum

**Category Methods:** (Pattern 4.2)
- Tipos agrupáveis → métodos por categoria
- Primitives, Collections, Stdlib separados

---

## 📊 Resumo Executivo

### Completado (80%)
- ✅ **Pattern 1:** Ktlint - 100% limpo
- ✅ **Pattern 2:** Borderline - Documentado
- ✅ **Pattern 3:** Complexidade moderada - 40% redução
- ✅ **Pattern 4:** Complexidade alta - **CONCLUÍDO** (4h)
  - ✅ generateImplementation() 164 linhas → 30 linhas (**82% redução**)
  - ✅ generateKotlinStdlibDefault() complexity 33 → 4 (**88% redução**)
  - ✅ handleClassDefault() complexity 24 → < 15 (**resolvido**)
  - ✅ 15 extrações realizadas
  - ✅ Todos os testes passando
  - ✅ Sample project compilando com sucesso

### Pendente (20%)
- 🔴 **Pattern 5:** Complexidade extrema (3-4h)
  - mapToTestSourceSet() complexity 53 → 3
  - Strategy pattern para source sets

### Impacto Final Esperado
- **Detekt:** 67 → **~10 issues** (85% redução)
- **Complexity crítica:** Todos métodos < 20
- **Código:** Production-ready e extensível

**Próximo passo:** Iniciar Pattern 4 (ImplementationGenerator refactoring)
