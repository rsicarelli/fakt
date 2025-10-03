# Plano de Limpeza Completa: fakt/fakt → Fakt/fakt

## 🎯 Objetivo
Remover TODAS as referências a `fakt`, `fakt`, `KtFake`, `Fakt` e substituir por `fakt`/`Fakt` conforme apropriado.

## 📋 Nomenclatura Oficial

### ✅ Correto
- **Nome do projeto**: Fakt
- **Pacote base**: `com.rsicarelli.fakt`
- **Diretórios gerados**: `build/generated/fakt/`
- **Configuration keys**: `fakt.enabled`, `fakt.debug`, `fakt.outputDir`
- **Arquivos de cache**: `fakt-report.json`, `fakt-signatures.cache`
- **Comentários técnicos**: "KtFake" ou "Fakt" (preferir Fakt)

### ❌ Errado (remover)
- fakt
- fakt
- Fakt
- ktFakes

## 📊 Arquivos a Corrigir

### **Configuração**
- `.gitignore` (se existir referências)
- `gradle.properties` (se existir)
- `settings.gradle.kts` (se existir)

### **Documentação** (19 arquivos .md encontrados)
```
./COMPILER_CLEANUP_PLAN.md
./PUBLISHED_PLUGIN_DEBUGGING.md
./docs/API_SPECIFICATIONS.md
./docs/ARCHITECTURE.md
./docs/CODE_GENERATION_STRATEGIES.md
./docs/COMPILE_TIME_EXAMPLES.md
./docs/COMPILE_TIME_GENERIC_SOLUTIONS.md
./docs/CURRENT_STATUS.md
./docs/FINAL_COMPILE_TIME_SOLUTION.md
./docs/GENERIC_IMPLEMENTATION_PROGRESS.md
./docs/GENERIC_TYPE_SCOPING_ANALYSIS.md
./docs/IMPLEMENTATION_DECISION.md
./docs/IMPLEMENTATION_ROADMAP.md
./docs/IR_NATIVE_DEMO.md
./docs/IR_NATIVE_DEMONSTRATION.md
./docs/KOTLIN_COMPILER_IR_API_GUIDE.md
./docs/README.md
./docs/TESTING_STATUS_REPORT.md
./docs/TEST_COVERAGE_ANALYSIS.md
./gradle-plugin-analysis.md
./samples/README.md
```

## 🔄 Estratégia de Substituição

### **Regras:**
1. **Em código Kotlin**: Manter apenas `Fakt` (nome oficial da classe/projeto)
2. **Em paths/diretórios**: Usar `fakt` (minúsculo)
3. **Em comentários**: Preferir `Fakt` ou `KtFake` (legado, pode manter se contexto histórico)
4. **Em documentação**: Usar `Fakt` consistentemente

### **Find & Replace Patterns:**
```bash
# Pattern 1: fakt → fakt
sed -i '' 's/fakt/fakt/g' <file>

# Pattern 2: fakt → fakt
sed -i '' 's/fakt/fakt/g' <file>

# Pattern 3: Fakt → Fakt
sed -i '' 's/Fakt/Fakt/g' <file>

# Pattern 4: KtFake → Fakt (cuidado com comentários históricos)
# Fazer manualmente se necessário
```

## ✅ Execução

### **Fase 1: Arquivos de Configuração**
- [ ] .gitignore
- [ ] gradle files (se houver)
- [ ] properties files

### **Fase 2: Documentação**
- [ ] Atualizar todos os .md files
- [ ] Verificar exemplos de código na documentação
- [ ] Atualizar READMEs

### **Fase 3: Validação Final**
- [ ] Grep para verificar que não sobrou nenhuma referência
- [ ] Testar compilação
- [ ] Testar testes
- [ ] Verificar geração no diretório correto

## 🎯 Critério de Sucesso

Executar: `grep -r "fakt\|fakt\|Fakt" . --include="*.md" --include="*.kt" --include="*.kts" --include="*.gitignore" | grep -v build | grep -v .git`

**Resultado esperado**: Nenhuma ocorrência (ou apenas comentários históricos intencionais)
