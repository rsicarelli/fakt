# 🚀 GitHub Pages com MkDocs - Passo a Passo Completo

> **Template baseado na configuração do Fakt**
>
> Use este guia para configurar GitHub Pages em qualquer projeto novo

## 📋 **Resumo Rápido**

**O que você vai ter no final:**
- 🌐 Site de documentação profissional
- 🔄 Deploy automático a cada push
- 📱 Tema Material Design responsivo
- 🔍 Busca integrada
- 📚 Versionamento de docs (opcional)

**Tempo necessário:** ~30 minutos

---

## 🗂️ **ETAPA 1: Criar Estrutura de Arquivos**

### 1.1 Criar `mkdocs.yml` (raiz do projeto)

```yaml
# mkdocs.yml
site_name: Seu Projeto
repo_name: usuario/projeto
repo_url: https://github.com/usuario/projeto
site_url: https://usuario.github.io/projeto/
site_description: "Descrição do seu projeto"
site_author: Seu Nome
remote_branch: gh-pages

copyright: 'Copyright &copy; 2025 Seu Nome'

theme:
  name: 'material'
  palette:
    - media: '(prefers-color-scheme: light)'
      scheme: default
      primary: 'deep purple'
      accent: 'purple'
      toggle:
        icon: material/brightness-7
        name: Switch to dark mode
    - media: '(prefers-color-scheme: dark)'
      scheme: slate
      primary: 'deep purple'
      accent: 'purple'
      toggle:
        icon: material/brightness-4
        name: Switch to light mode
  font:
    text: 'Inter'
    code: 'Fira Code'
  features:
    - content.code.copy
    - content.code.select
    - navigation.tabs
    - navigation.sections
    - navigation.expand
    - navigation.top
    - search.suggest
    - search.highlight
    - content.tabs.link

plugins:
  - search
  - mike
  - callouts

markdown_extensions:
  - smarty
  - codehilite:
      guess_lang: false
  - footnotes
  - meta
  - toc:
      permalink: true
  - pymdownx.betterem:
      smart_enable: all
  - pymdownx.caret
  - pymdownx.inlinehilite
  - pymdownx.magiclink
  - pymdownx.smartsymbols
  - pymdownx.superfences
  - pymdownx.emoji
  - pymdownx.details
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.highlight:
      anchor_linenums: true
  - tables
  - admonition
  - attr_list
  - md_in_html
  - nl2br

extra:
  version:
    provider: mike

nav:
  - 'Home': index.md
  - 'Getting Started': getting-started.md
  - 'FAQ': faq.md
```

### 1.2 Criar diretório e páginas iniciais

```bash
# Criar estrutura de docs
mkdir docs

# Página inicial
cat > docs/index.md << 'EOF'
# Bem-vindo ao Projeto

Esta é a documentação oficial do projeto.

## Início Rápido

Comece aqui com os passos básicos para usar o projeto.

## Recursos

- ✅ Feature 1: Descrição da feature
- ✅ Feature 2: Outra funcionalidade
- ✅ Feature 3: Mais uma feature

## Links Úteis

- [Getting Started](getting-started.md) - Guia de início
- [FAQ](faq.md) - Perguntas frequentes
- [GitHub](https://github.com/usuario/projeto) - Código fonte
EOF

# Página de início rápido
cat > docs/getting-started.md << 'EOF'
# Getting Started

Guia para começar a usar o projeto.

## Instalação

```bash
# Comandos de instalação aqui
npm install seu-projeto
# ou
pip install seu-projeto
```

## Configuração Básica

Passos básicos de configuração:

1. Configure o ambiente
2. Execute o setup inicial
3. Teste a instalação

## Próximos Passos

- Leia a [documentação completa](index.md)
- Veja os [exemplos práticos](examples.md)
- Consulte o [FAQ](faq.md)
EOF

# Página FAQ
cat > docs/faq.md << 'EOF'
# FAQ - Perguntas Frequentes

## Como instalar?

Siga o guia em [Getting Started](getting-started.md).

## Como contribuir?

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Abra um Pull Request

## Onde reportar bugs?

Abra uma issue no [GitHub](https://github.com/usuario/projeto/issues).
EOF
```

---

## ⚙️ **ETAPA 2: GitHub Actions Workflow**

### 2.1 Criar arquivo de dependências

```bash
# Criar diretório se não existir
mkdir -p .github/workflows

# Arquivo de dependências Python
cat > .github/workflows/mkdocs-requirements.txt << 'EOF'
click==8.2.2
future==1.0.0
Jinja2==3.1.6
livereload==2.7.1
lunr==0.8.0
Markdown==3.9
MarkupSafe==3.0.2
mike==2.1.3
mkdocs==1.6.1
mkdocs-get-deps==0.2.0
mkdocs-material==9.6.16
mkdocs-material-extensions==1.3.1
mkdocs-callouts==1.16.0
Pygments==2.19.2
pymdown-extensions==10.16.1
python-dateutil==2.9.0.post0
PyYAML==6.0.2
repackage==0.7.3
six==1.17.0
termcolor==3.1.0
tornado==6.5.2

# Imaging dependencies for mkdocs-material social plugin
cairosvg==2.8.2
pillow==11.3.0
EOF
```

### 2.2 Criar workflow do GitHub Actions

```yaml
# .github/workflows/docs.yml
name: Deploy Documentation

on:
  push:
    branches: ["main"]
  workflow_dispatch:

permissions:
  contents: write
  pages: write
  id-token: write

concurrency:
  group: 'pages'
  cancel-in-progress: false

jobs:
  docs-site:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.13'

      - name: Install MkDocs dependencies
        run: |
          pip install --requirement .github/workflows/mkdocs-requirements.txt

      - name: Configure Git for Mike
        run: |
          git config --local user.name "github-actions[bot]"
          git config --local user.email "github-actions[bot]@users.noreply.github.com"

      - name: Fetch gh-pages branch
        run: |
          git fetch origin gh-pages:gh-pages || echo "gh-pages branch doesn't exist yet"

      - name: Deploy docs with Mike
        run: |
          # Deploy para branch main com alias 'latest'
          mike deploy --update-aliases --push main latest
          # Define 'latest' como versão default
          mike set-default --push latest
```

---

## 🔧 **ETAPA 3: Configurar GitHub**

### 3.1 Habilitar GitHub Pages

1. **Acesse as configurações:**
   ```
   GitHub.com → Seu Repositório → Settings → Pages
   ```

2. **Configure Source:**
   - **Source**: Deploy from a branch
   - **Branch**: `gh-pages`
   - **Folder**: `/ (root)`

3. **Salve as configurações**

### 3.2 Configurar Permissões

1. **Settings → Actions → General:**
   - ✅ **Workflow permissions**: Read and write permissions
   - ✅ **Allow GitHub Actions to create and approve pull requests**

### 3.3 Fazer primeiro commit

```bash
# Adicionar todos os arquivos
git add .

# Commit inicial
git commit -m "docs: setup MkDocs with GitHub Pages

- Add mkdocs.yml configuration
- Add initial documentation pages
- Add GitHub Actions workflow for auto-deploy
- Configure Material theme with dark/light mode"

# Push para trigger o workflow
git push origin main
```

---

## ✅ **ETAPA 4: Validar e Testar**

### 4.1 Verificar Workflow

1. **GitHub → Actions → "Deploy Documentation"**
   - ✅ Workflow deve executar automaticamente
   - ✅ Verificar se completa sem erros

2. **GitHub → Branches**
   - ✅ Branch `gh-pages` deve ter sido criado

### 4.2 Acessar Site

1. **URL do site:**
   ```
   https://usuario.github.io/projeto/
   ```

2. **Verificar funcionalidades:**
   - ✅ Páginas carregando
   - ✅ Navegação funcionando
   - ✅ Busca ativa
   - ✅ Dark/Light mode

### 4.3 Teste Local (Opcional)

```bash
# Instalar dependências localmente
pip install mkdocs mkdocs-material mike

# Servir localmente
mkdocs serve
# → http://127.0.0.1:8000

# Build manual
mkdocs build

# Deploy manual (se precisar)
mkdocs gh-deploy --force
```

---

## 🎨 **ETAPA 5: Personalizar (Opcional)**

### 5.1 Customizar Tema

No `mkdocs.yml`, altere:

```yaml
theme:
  palette:
    - scheme: default
      primary: 'blue'        # Sua cor principal
      accent: 'cyan'         # Cor de destaque
```

### 5.2 Adicionar Logo

```yaml
theme:
  logo: assets/logo.png       # Adicione logo
  favicon: assets/favicon.ico # Favicon customizado
```

### 5.3 Plugins Extras

```yaml
plugins:
  - social                    # Cards para redes sociais
  - git-revision-date        # Data de modificação
  - minify:                  # Minificação
      minify_html: true
```

---

## 🛠️ **Troubleshooting**

### Workflow falhando?

```bash
# Verificar logs no GitHub Actions
# Common issues:

1. Permissões: Settings → Actions → Workflow permissions
2. Branch protection: Settings → Branches → main
3. Dependências: Verificar mkdocs-requirements.txt
```

### Site não carregando?

```bash
# Verificar:
1. GitHub Pages habilitado (Settings → Pages)
2. Branch gh-pages existe
3. URL correta: https://usuario.github.io/projeto/
```

### Erro 404?

```bash
# site_url no mkdocs.yml deve ser exato:
site_url: https://usuario.github.io/projeto/  # Com / no final!
```

---

## 📝 **Checklist Final**

### ✅ Arquivos Criados
- [ ] `mkdocs.yml` (configuração principal)
- [ ] `docs/index.md` (página inicial)
- [ ] `docs/getting-started.md` (guia)
- [ ] `docs/faq.md` (FAQ)
- [ ] `.github/workflows/docs.yml` (workflow)
- [ ] `.github/workflows/mkdocs-requirements.txt` (deps)

### ✅ GitHub Configurado
- [ ] Repository → Settings → Pages habilitado
- [ ] Source: Deploy from branch `gh-pages`
- [ ] Workflow permissions: Read and write
- [ ] Actions habilitadas

### ✅ Funcionando
- [ ] Workflow executou com sucesso
- [ ] Branch `gh-pages` criado
- [ ] Site acessível em `https://usuario.github.io/projeto/`
- [ ] Navegação e busca funcionando

---

## 💡 **Dicas Extras**

### Versionamento Automático

Se você tem versioning no projeto (ex: package.json, gradle.properties):

```yaml
# No workflow, adicione:
- name: Extract version
  id: version
  run: |
    VERSION=$(grep "version" package.json | cut -d'"' -f4)
    echo "VERSION=$VERSION" >> $GITHUB_OUTPUT

- name: Deploy versioned docs
  run: |
    mike deploy --push ${{ steps.version.outputs.VERSION }} latest
```

### URLs Customizadas

Para usar domínio próprio:

```yaml
# mkdocs.yml
site_url: https://docs.meusite.com/
```

```bash
# Criar arquivo CNAME no branch gh-pages
echo "docs.meusite.com" > CNAME
```

### Analytics

```yaml
# mkdocs.yml
extra:
  analytics:
    provider: google
    property: G-XXXXXXXXXX
```

---

## 🎯 **Resultado Final**

Ao seguir este guia, você terá:

- 🌐 **Site profissional** com tema Material Design
- 🔄 **Deploy automático** a cada push para main
- 📱 **Responsivo** e otimizado para mobile
- 🔍 **Busca integrada** e rápida
- 🌙 **Dark/Light mode** automático
- 📚 **Versionamento** de documentação (com Mike)
- ⚡ **Performance otimizada** com minificação

**Igual ao site do Fakt: https://rsicarelli.github.io/fakt/**

---

*Este guia foi baseado na configuração real e funcional do projeto Fakt.*