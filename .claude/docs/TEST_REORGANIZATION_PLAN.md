# Plano de Reorganização dos Testes

## 🎯 Objetivo
Reorganizar testes para espelhar a estrutura de pacotes de produção após o Sprint 1 de reorganização.

## 📊 Estrutura Atual vs Desejada

### **Produção (Atual - Após Sprint 1)**
```
compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
├── FaktCompilerPluginRegistrar.kt           # Raiz
├── config/
│   ├── FaktCommandLineProcessor.kt
│   └── FaktOptions.kt
├── fir/
│   └── FaktFirExtensionRegistrar.kt
├── ir/
│   ├── UnifiedFaktIrGenerationExtension.kt
│   └── analysis/
│       └── GenericPatternAnalyzer.kt
├── codegen/
│   ├── CodeGenerator.kt
│   ├── ImplementationGenerator.kt
│   ├── FactoryGenerator.kt
│   └── ConfigurationDslGenerator.kt
├── output/
│   └── SourceSetMapper.kt
├── optimization/
│   ├── CompilerOptimizations.kt (interface)
│   └── IncrementalCompiler.kt
└── types/
    ├── TypeInfo.kt
    ├── TypeResolver.kt
    └── ImportResolver.kt
```

### **Testes (Atual - Desorganizado)**
```
compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/
├── CompilerOptimizationsTest.kt              ❌ Deveria estar em optimization/
├── FaktCommandLineProcessorSimpleTest.kt     ❌ Deveria estar em config/
├── FaktCompilerPluginRegistrarSimpleTest.kt  ✅ Correto (raiz)
├── GenericPatternAnalyzerTest.kt             ❌ Deveria estar em ir/analysis/
├── ServiceLoaderValidationTest.kt            ✅ Correto (raiz - testa META-INF)
├── fir/
│   └── FakeAnnotationDetectorSimpleTest.kt   ✅ Correto
└── generation/                               ❌ Nome incorreto, deveria ser codegen/
    ├── CodeGenerationModulesContractTest.kt
    └── ExtractedModulesIntegrationTest.kt
```

### **Testes (Desejado - Espelhando Produção)**
```
compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/
├── FaktCompilerPluginRegistrarSimpleTest.kt  # Raiz (testa classe raiz)
├── ServiceLoaderValidationTest.kt            # Raiz (testa META-INF services)
├── config/
│   └── FaktCommandLineProcessorSimpleTest.kt
├── fir/
│   └── FakeAnnotationDetectorSimpleTest.kt   # Já correto ✅
├── ir/
│   └── analysis/
│       └── GenericPatternAnalyzerTest.kt
├── codegen/
│   ├── CodeGenerationModulesContractTest.kt
│   └── ExtractedModulesIntegrationTest.kt
└── optimization/
    └── CompilerOptimizationsTest.kt
```

## 🔄 Ações Necessárias

### 1. Criar novos diretórios
```bash
mkdir -p compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/config
mkdir -p compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/ir/analysis
mkdir -p compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/codegen
mkdir -p compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/optimization
```

### 2. Mover arquivos (git mv para preservar histórico)
```bash
# Mover para config/
git mv compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/FaktCommandLineProcessorSimpleTest.kt \
       compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/config/

# Mover para ir/analysis/
git mv compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/GenericPatternAnalyzerTest.kt \
       compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/ir/analysis/

# Mover para optimization/
git mv compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/CompilerOptimizationsTest.kt \
       compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/optimization/

# Renomear generation/ para codegen/
git mv compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/generation \
       compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/codegen
```

### 3. Manter na raiz
- ✅ `FaktCompilerPluginRegistrarSimpleTest.kt` - testa classe raiz
- ✅ `ServiceLoaderValidationTest.kt` - testa META-INF services

## ✅ Benefícios

1. **Navegação intuitiva**: Encontrar teste do `config/FaktOptions` em `config/FaktOptionsTest`
2. **Coesão clara**: Testes agrupados por módulo funcional
3. **Manutenibilidade**: Mudanças em um pacote facilitam encontrar testes relacionados
4. **Convenção padrão**: Espelha estrutura de produção (best practice)
5. **IDE friendly**: Navegação por pacotes funciona melhor

## 📋 Validação

Após reorganização, verificar:
- [ ] Todos os testes compilam sem erros
- [ ] Imports atualizados corretamente
- [ ] `./gradlew :compiler:test` passa 100%
- [ ] Estrutura espelha produção perfeitamente

## 🎯 Próximos Passos

1. Aprovação do plano
2. Execução das movimentações (preservando git history)
3. Validação de compilação
4. Commit com mensagem: "test: reorganize tests to mirror production package structure"
