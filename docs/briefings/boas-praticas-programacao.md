> **Deprecated em 2026-09-07.** Foi a fonte de onde as regras do `CLAUDE.md` e as
> skills foram curadas. O resultado dessa curadoria vive nos próprios arquivos do
> harness; este guia não é mais consultado para decidir nada. Vale como registro
> para o TCC.

# Boas Práticas de Programação — Guia Geral

> Documento agnóstico de linguagem, framework e domínio. Os exemplos usam pseudocódigo
> ou linguagens diferentes de propósito: a ideia é o princípio, não a sintaxe.

**Como usar este guia:** não é uma lista de regras para obedecer cegamente. Toda boa
prática existe para resolver um problema concreto; quando o problema não existe no seu
contexto, a prática vira burocracia. Por isso cada seção traz *por que* a regra existe e
*quando quebrá-la*.

---

## Índice

1. [Princípios fundamentais](#1-princípios-fundamentais)
2. [SOLID](#2-solid)
3. [Legibilidade: nomes, funções e comentários](#3-legibilidade-nomes-funções-e-comentários)
4. [Tratamento de erros](#4-tratamento-de-erros)
5. [Arquitetura e fronteiras](#5-arquitetura-e-fronteiras)
6. [Testes](#6-testes)
7. [Git, branches e commits](#7-git-branches-e-commits)
8. [Code review e Pull Requests](#8-code-review-e-pull-requests)
9. [Versionamento e changelog](#9-versionamento-e-changelog)
10. [APIs: design, evolução e documentação de mudanças](#10-apis-design-evolução-e-documentação-de-mudanças)
11. [Documentação de projeto](#11-documentação-de-projeto)
12. [Logs, métricas e observabilidade](#12-logs-métricas-e-observabilidade)
13. [Segurança](#13-segurança)
14. [Performance](#14-performance)
15. [Configuração, ambientes e segredos](#15-configuração-ambientes-e-segredos)
16. [CI/CD e automação](#16-cicd-e-automação)
17. [Dados e migrações](#17-dados-e-migrações)
18. [Dívida técnica e refatoração](#18-dívida-técnica-e-refatoração)
19. [Anti-padrões comuns](#19-anti-padrões-comuns)
20. [Checklists rápidos](#20-checklists-rápidos)
21. [Leituras recomendadas](#21-leituras-recomendadas)

---

## 1. Princípios fundamentais

### 1.1 Código é lido muito mais do que é escrito

A métrica que importa não é "quão rápido eu escrevo", é "quanto tempo alguém — inclusive
você daqui a seis meses — leva para entender e mudar isso com segurança". Otimize para o
leitor.

### 1.2 KISS — Keep It Simple

Prefira a solução mais simples que resolve o problema atual. Complexidade é um custo
permanente pago por todo mundo que toca o código depois.

Sinais de complexidade desnecessária:
- Camadas de abstração com uma única implementação e nenhuma perspectiva de segunda.
- Configuração para coisas que nunca variam.
- Generalização feita "para o caso de".

### 1.3 YAGNI — You Aren't Gonna Need It

Não construa hoje o que só será necessário num futuro hipotético. Requisitos futuros
quase sempre chegam diferentes do que você imaginou, e o código especulativo já custou
manutenção no intervalo.

> **Quando quebrar:** decisões difíceis de reverter (formato de dados persistido, contrato
> público de API, esquema de eventos) merecem antecipação. O critério é o **custo de
> reverter**, não o de construir.

### 1.4 DRY — Don't Repeat Yourself (com cuidado)

DRY é sobre não duplicar **conhecimento**, não sobre não duplicar **caracteres**. Dois
trechos parecidos que mudam por razões diferentes devem permanecer separados.

```
# Duplicação real (mesmo conhecimento, mesma razão de mudar)
if user.age >= 18: ...        # em 4 arquivos
→ extrair: is_adult(user)

# Duplicação acidental (código parecido, razões diferentes)
validar_cpf_do_cliente()  e  validar_cpf_do_fornecedor()
→ regras podem divergir amanhã; unificar cedo cria acoplamento falso
```

Regra prática: **espere a terceira ocorrência** antes de abstrair. Duplicação é mais
barata que a abstração errada.

### 1.5 Separação de responsabilidades

Cada módulo deve ter uma razão para existir e uma razão para mudar. Misturar regra de
negócio, acesso a dados e formatação de saída no mesmo lugar é o que torna código difícil
de testar.

### 1.6 Alta coesão, baixo acoplamento

- **Coesão:** o que muda junto fica junto.
- **Acoplamento:** o quanto um módulo precisa saber sobre os outros.

O objetivo é poder mudar um módulo sem abrir os demais. Sempre que uma alteração pequena
exige tocar em cinco arquivos distantes, você tem acoplamento demais.

### 1.7 Princípio da menor surpresa

Funções fazem o que o nome promete e nada além. Um `getUser()` que grava no banco, um
`toString()` que dispara requisição HTTP, um setter que valida e lança exceção — tudo isso
quebra a expectativa de quem lê.

### 1.8 Fail fast

Detecte o erro o mais perto possível da origem. Validar entrada na fronteira do sistema e
falhar imediatamente é melhor que propagar um valor inválido por dez camadas e explodir
num lugar sem contexto.

### 1.9 Composição sobre herança

Herança acopla o filho à implementação do pai e é frágil a mudanças. Composição (injetar
colaboradores) dá o mesmo reuso com menos rigidez.

```
# Frágil
class RelatorioPDF extends RelatorioBase { ... }

# Flexível
class Relatorio:
    def __init__(self, formatador): self.formatador = formatador
```

Use herança quando existir uma relação **é-um** genuína e estável, e prefira herdar de
interfaces/tipos abstratos a herdar de classes concretas.

### 1.10 Imutabilidade por padrão

Dados imutáveis eliminam uma classe inteira de bugs (alias inesperado, mutação
concorrente, estado alterado por baixo). Torne mutável só o que precisa ser.

### 1.11 Lei de Deméter ("fale só com seus amigos")

```
# Ruim: conhece a estrutura interna de três objetos
pedido.getCliente().getEndereco().getCidade().getNome()

# Melhor
pedido.cidadeDeEntrega()
```

Cada `.` encadeado é uma suposição sobre a estrutura alheia que vai quebrar quando ela mudar.

### 1.12 Explícito é melhor que implícito

Dependências, efeitos colaterais e estados devem ser visíveis na assinatura. Estado global,
singletons escondidos e "mágica" de framework economizam três linhas hoje e custam uma
tarde de depuração depois.

### 1.13 Boy Scout Rule

Deixe o código um pouco melhor do que encontrou — renomear uma variável, extrair uma
função, apagar código morto. Melhoria incremental é o que evita a "grande refatoração"
que nunca acontece.

---

## 2. SOLID

Cinco princípios de design orientado a objetos (aplicáveis, com adaptação, a código
procedural e funcional também).

### S — Single Responsibility Principle (Responsabilidade Única)

> Uma classe/módulo deve ter **uma única razão para mudar**.

"Razão para mudar" quase sempre significa "um grupo de interessados". Se o time de
negócio, o time de infraestrutura e o time de relatórios pedem alterações no mesmo
arquivo, ele tem responsabilidades demais.

```
# Viola: calcula, persiste e formata
class Fatura:
    def calcular_total(self): ...
    def salvar_no_banco(self): ...
    def gerar_pdf(self): ...

# Respeita
class Fatura:            def calcular_total(self): ...
class FaturaRepository:  def salvar(self, fatura): ...
class FaturaPDFRenderer: def renderizar(self, fatura): ...
```

**Sintoma de violação:** o nome da classe precisa de um "e" para ser descrito
("cuida do usuário **e** manda e-mail").

### O — Open/Closed Principle (Aberto/Fechado)

> Aberto para extensão, fechado para modificação.

Adicionar um comportamento novo não deveria exigir editar código existente e testado.

```
# Viola: cada novo meio de pagamento edita o mesmo if
def calcular_taxa(pagamento):
    if pagamento.tipo == "cartao":  return ...
    elif pagamento.tipo == "pix":   return ...
    elif pagamento.tipo == "boleto": return ...   # e assim por diante

# Respeita: polimorfismo / estratégia
class MeioDePagamento(ABC):
    def taxa(self, valor) -> Decimal: ...

class Pix(MeioDePagamento):     def taxa(self, valor): return 0
class Cartao(MeioDePagamento):  def taxa(self, valor): return valor * 0.03
```

> **Quando quebrar:** não crie a hierarquia antes de existir variação real. OCP prematuro é
> YAGNI disfarçado. Aplique quando o eixo de variação já se mostrou.

### L — Liskov Substitution Principle (Substituição de Liskov)

> Um subtipo deve poder substituir o tipo base sem quebrar o programa.

O contrato do subtipo não pode ser mais restritivo na entrada nem mais fraco na saída.

```
# Clássico: Quadrado herdando de Retângulo
r = Retangulo()      # ou Quadrado()
r.setLargura(5); r.setAltura(4)
assert r.area() == 20     # falha se r for Quadrado
```

**Sintomas de violação:**
- Métodos sobrescritos que lançam `NotImplementedException`.
- Código cliente com `if isinstance(x, SubtipoY)`.
- Subclasse que "ignora" um parâmetro do pai.

### I — Interface Segregation Principle (Segregação de Interfaces)

> Nenhum cliente deve ser forçado a depender de métodos que não usa.

```
# Viola: quem só imprime é obrigado a implementar fax e scanner
interface Multifuncional { imprimir(); digitalizar(); enviarFax(); }

# Respeita
interface Impressora  { imprimir(); }
interface Scanner     { digitalizar(); }
```

Interfaces pequenas e focadas facilitam testes (menos mocks) e reduzem recompilação/
redeploy desnecessários.

### D — Dependency Inversion Principle (Inversão de Dependência)

> Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender
> de abstrações. Detalhes dependem de abstrações, não o contrário.

```
# Viola: regra de negócio conhece PostgreSQL
class ServicoDePedidos:
    def __init__(self): self.db = PostgresClient(...)

# Respeita: a abstração pertence ao domínio, a implementação ao adaptador
class ServicoDePedidos:
    def __init__(self, repositorio: PedidoRepository): ...
```

Isso é o que permite trocar banco, provedor de e-mail ou gateway de pagamento sem tocar na
regra de negócio — e testar a regra sem subir infraestrutura.

### Resumo prático do SOLID

| Princípio | Pergunta que você deve se fazer |
|---|---|
| SRP | Quantos motivos diferentes fariam eu editar este arquivo? |
| OCP | Para adicionar o próximo caso, eu edito ou eu estendo? |
| LSP | Posso trocar a implementação sem que o cliente perceba? |
| ISP | Este cliente é obrigado a conhecer coisas que não usa? |
| DIP | Minha regra de negócio conhece nomes de tecnologia? |

---

## 3. Legibilidade: nomes, funções e comentários

### 3.1 Nomes

- **Revele a intenção:** `dias_desde_ultimo_login` em vez de `d`.
- **Sem abreviações crípticas:** `qtd` tudo bem, `qtdItPd` não.
- **Sem notação húngara nem prefixos de tipo:** o tipo já está no tipo.
- **Sem ruído:** `UserData`, `UserInfo`, `UserObject` não dizem nada diferente de `User`.
- **Booleanos afirmativos:** `esta_ativo` é melhor que `nao_esta_inativo`.
- **Consistência de vocabulário:** escolha um termo por conceito (`buscar` vs `obter` vs
  `carregar`) e use sempre o mesmo em todo o projeto.
- **Tamanho proporcional ao escopo:** `i` num loop de três linhas é ótimo; variável de
  módulo precisa de nome completo.
- **Use a linguagem do domínio:** se o negócio chama de "apólice", o código chama de
  "apolice", não de "contrato".

### 3.2 Funções

- **Pequenas.** Se você precisa rolar a tela, provavelmente há duas funções ali.
- **Um nível de abstração por função.** Não misture `validar_cpf()` com manipulação de
  string byte a byte no mesmo corpo.
- **Poucos parâmetros.** Três é um bom limite; acima disso, agrupe num objeto/struct.
- **Evite parâmetros booleanos** que mudam o comportamento — `salvar(true)` não diz nada.
  Prefira duas funções ou um enum nomeado.
- **Sem efeitos colaterais escondidos.** Uma função ou responde algo (query) ou muda algo
  (command); misturar os dois complica o raciocínio (princípio CQS).
- **Evite retornar `null`/`nil`** quando puder retornar lista vazia, `Optional`/`Maybe` ou
  lançar erro explícito.

### 3.3 Comentários

Bons comentários explicam **por quê**, nunca **o quê**.

```
# Ruim: repete o código
i = i + 1   # incrementa i

# Bom: explica a razão não óbvia
# A API do parceiro rejeita lotes > 500; acima disso ela responde 200 com
# corpo vazio em vez de erro. Ticket #4821.
LOTE_MAXIMO = 500
```

Regras:
- Se o comentário existe para compensar um nome ruim, corrija o nome.
- Comentário desatualizado é pior que nenhum — ele mente com autoridade.
- Não deixe código comentado; o histórico do Git já guarda isso.
- `TODO`/`FIXME` devem ter dono e/ou link para issue, senão viram lixo permanente.

### 3.4 Formatação

Não discuta estilo em code review: delegue a ferramenta. Um formatador automático
(Prettier, Black, gofmt, rustfmt, spotless, clang-format...) rodando em pre-commit e no CI
encerra o assunto e economiza horas de debate improdutivo.

---

## 4. Tratamento de erros

### 4.1 Princípios

- **Nunca engula erro silenciosamente.** `catch (Exception e) {}` é uma bomba-relógio.
- **Trate no nível que tem contexto para decidir.** Camadas baixas propagam; a fronteira
  decide o que fazer (retry, fallback, erro ao usuário).
- **Preserve a causa raiz.** Ao re-lançar, encadeie a exceção original (`raise ... from e`,
  `throw new X(msg, cause)`, `fmt.Errorf("...: %w", err)`).
- **Distinga erro esperado de erro excepcional.** "CPF inválido" é fluxo normal e vale um
  tipo de retorno (`Result`, `Either`, valor de erro); "disco cheio" é excepcional.
- **Mensagens úteis:** diga o que falhou, com qual valor e o que fazer.
  `"Falha ao processar pedido 8891: valor negativo (-5,00)"` em vez de `"erro"`.
- **Nunca vaze detalhes internos para o usuário final** (stack trace, SQL, caminho de
  arquivo). Logue completo, exponha resumido com um ID de correlação.

### 4.2 Tipos de erro

```
Erros de entrada     → 4xx / validação → mensagem clara para o cliente
Erros de negócio     → 4xx / 409 / 422 → código de erro estável e documentado
Erros de infra       → 5xx             → retry com backoff, alerta, circuit breaker
Erros de programação → bug             → falhar alto, corrigir, adicionar teste
```

### 4.3 Resiliência

- **Timeout em toda chamada externa.** Sem exceção. Padrão sem timeout é infinito.
- **Retry apenas para falhas transitórias**, com *exponential backoff + jitter*, e apenas
  em operações idempotentes.
- **Circuit breaker** para não martelar um serviço já caído.
- **Degradação graciosa:** melhor mostrar o carrinho sem recomendações que derrubar a
  página inteira.
- **Idempotência:** operações que podem ser repetidas (por retry, por reentrega de
  mensagem) precisam de chave de idempotência.

---

## 5. Arquitetura e fronteiras

### 5.1 Camadas e dependências apontando para dentro

```
   [ Entrada: HTTP, CLI, fila, cron ]
                  ↓
   [ Aplicação: casos de uso, orquestração ]
                  ↓
   [ Domínio: entidades e regras de negócio ]   ← não conhece ninguém
                  ↑
   [ Infra: banco, HTTP client, fila, storage ]
```

Regra de ouro: **o domínio não importa nada de fora**. Nada de anotação de ORM, cliente
HTTP ou classe de framework dentro da regra de negócio. É isso que permite testar o núcleo
em milissegundos e trocar tecnologia sem reescrever o produto.

### 5.2 Ports & Adapters (arquitetura hexagonal)

- **Port:** interface definida pelo domínio (`PedidoRepository`, `NotificadorDeCliente`).
- **Adapter:** implementação concreta (`PostgresPedidoRepository`, `SendgridNotificador`).

Nos testes, um adapter em memória substitui o real. Em produção, o container de injeção de
dependência escolhe qual usar.

### 5.3 Modularização

- Organize por **funcionalidade/domínio**, não por tipo técnico.
  `pedidos/{modelo, servico, repositorio}` funciona melhor que
  `modelos/, servicos/, repositorios/` quando o projeto cresce.
- Deixe explícito o que é público do módulo e o que é interno.
- Evite dependências circulares entre módulos — são sinal de que a fronteira está no lugar
  errado.

### 5.4 Estado

- Prefira funções puras onde der: mesma entrada, mesma saída, sem efeito colateral. São
  triviais de testar e de paralelizar.
- Concentre o estado mutável em poucos lugares bem identificados.
- Estado global compartilhado é a origem de boa parte dos bugs difíceis.

### 5.5 Quando escolher a arquitetura mais simples

Nem todo sistema precisa de hexagonal, CQRS, event sourcing ou microsserviços. Comece
monolítico e modular; extraia serviços quando houver razão real (escala independente,
times independentes, ciclo de deploy independente) — não por moda. Sistemas distribuídos
trocam complexidade de código por complexidade operacional, e essa conta chega.
---

## 6. Testes

### 6.1 Por que testar

Teste não é sobre "provar que funciona": é sobre **poder mudar o código com segurança**.
Um sistema sem testes é um sistema onde ninguém refatora, e código que ninguém refatora
apodrece.

### 6.2 Pirâmide de testes

```
        /\        E2E / interface        poucos, lentos, frágeis, alto valor de confiança
       /  \       Integração             médios: banco real, fila real, API real
      /____\      Unitários              muitos, rápidos, isolados
```

O formato importa: se a sua "pirâmide" está invertida (muitos E2E, poucos unitários), a
suíte vai ser lenta, instável e ninguém vai confiar nela.

### 6.3 Anatomia de um bom teste — AAA

```
def test_pedido_com_cupom_aplica_desconto():
    # Arrange
    pedido = Pedido(itens=[Item(preco=100)])
    cupom  = Cupom(percentual=10)

    # Act
    total = pedido.total_com(cupom)

    # Assert
    assert total == 90
```

### 6.4 Princípios FIRST

- **F**ast — suíte unitária inteira em segundos, não minutos.
- **I**ndependent — nenhum teste depende da ordem ou do resultado de outro.
- **R**epeatable — mesmo resultado em qualquer máquina, hoje e amanhã.
- **S**elf-validating — passa ou falha; não exige alguém lendo log.
- **T**imely — escrito junto com o código, não "depois quando der".

### 6.5 Regras práticas

- **Um comportamento por teste.** Nome do teste descreve o comportamento:
  `deve_rejeitar_saque_acima_do_saldo`.
- **Teste comportamento público, não implementação.** Teste acoplado a detalhe interno
  quebra em toda refatoração e vira um freio, não uma rede.
- **Nada de aleatoriedade, data atual ou rede em teste unitário.** Injete relógio e
  geradores. `now()` dentro do código é uma dependência escondida.
- **Teste os limites:** vazio, um, muitos, negativo, nulo, máximo, caractere especial,
  fuso horário, concorrência.
- **Todo bug corrigido ganha um teste** que falhava antes da correção. É a única garantia
  de que ele não volta.
- **Testes também são código:** merecem nomes bons, sem duplicação absurda — mas privilegie
  clareza sobre DRY aqui; um teste deve ser legível sem caçar helpers.

### 6.6 Test doubles

| Tipo | O que faz |
|---|---|
| Dummy | Só preenche parâmetro, nunca é usado |
| Stub | Devolve resposta pré-programada |
| Spy | Stub que registra como foi chamado |
| Mock | Verifica interações esperadas |
| Fake | Implementação real simplificada (repositório em memória) |

Prefira **fakes** a mocks quando possível: mocks demais deixam o teste acoplado à
implementação e dão falsa sensação de cobertura.

### 6.7 Cobertura

Cobertura é um **indicador**, não uma meta. 100% de cobertura com asserções fracas não
protege nada; 70% bem escolhidos protegem muito. Use cobertura para achar áreas críticas
sem teste, não para bater métrica.

### 6.8 Outros tipos úteis

- **Testes de contrato** (consumer-driven, ex. Pact): garantem que provedor e consumidor de
  uma API continuam compatíveis sem precisar subir tudo junto.
- **Property-based testing** (Hypothesis, QuickCheck, fast-check): gera centenas de entradas
  e verifica invariantes; excelente para parsing, serialização e cálculos.
- **Testes de regressão visual / snapshot:** úteis para UI, perigosos quando atualizados no
  automático sem ninguém olhar.
- **Smoke tests pós-deploy:** verificação mínima de que o que subiu está vivo.

---

## 7. Git, branches e commits

### 7.1 Commits atômicos

Um commit = uma mudança lógica completa e coerente. Não misture refatoração, correção de
bug e formatação no mesmo commit — isso torna revisão, `bisect` e revert quase impossíveis.

Se o commit precisa de "e" na descrição, provavelmente são dois commits.

### 7.2 Mensagem de commit

Formato clássico:

```
<tipo>(<escopo>): <resumo no imperativo, minúsculo, sem ponto final>
                            ← linha em branco
Corpo: explica O QUÊ e sobretudo POR QUÊ. O "como" está no diff.
Descreve o contexto, a alternativa descartada e o efeito colateral conhecido.
                            ← linha em branco
Refs: #123
BREAKING CHANGE: descrição do que quebrou e como migrar
```

Regras:
- Assunto no **imperativo** ("adiciona", "corrige", "remove") — completa a frase "Se
  aplicado, este commit ___".
- Assunto com até ~50–72 caracteres; corpo quebrado em ~72.
- Explique o porquê. O diff mostra o que mudou; só a mensagem mostra a intenção.

### 7.3 Conventional Commits

Convenção amplamente adotada, e a base para gerar changelog e versão automaticamente:

```
feat:     nova funcionalidade                → bump MINOR
fix:      correção de bug                    → bump PATCH
docs:     só documentação
style:    formatação, sem mudança de comportamento
refactor: mudança interna sem alterar comportamento externo
perf:     melhoria de performance
test:     adição/ajuste de testes
build:    build system, dependências
ci:       pipeline
chore:    tarefas auxiliares
revert:   reversão de commit anterior

feat!: ...  ou  BREAKING CHANGE: no rodapé   → bump MAJOR
```

Exemplos:

```
feat(auth): adiciona login via OAuth com Google

fix(pagamentos): trata timeout do gateway como falha retentável

O gateway responde 504 em picos e o cliente estava marcando o pedido
como recusado, gerando estorno indevido. Agora o timeout entra na fila
de retry com backoff. Refs: #2291

refactor(pedidos)!: remove PedidoLegacyService

BREAKING CHANGE: quem usava PedidoLegacyService deve migrar para
PedidoService.criar(); a assinatura agora exige o campo `canal`.
```

### 7.4 Estratégia de branches

Não existe uma certa; existe a adequada ao seu ciclo de entrega.

- **Trunk-based development:** branches curtíssimas (horas a 1–2 dias), integração contínua
  na `main`, feature flags para o que não está pronto. É o que melhor sustenta entrega
  frequente.
- **GitHub Flow:** `main` sempre deployável + branches de feature + PR. Simples, funciona
  para a maioria dos times.
- **Git Flow:** `develop`, `release/*`, `hotfix/*`. Faz sentido para software versionado
  com releases planejadas; costuma ser peso morto para SaaS de deploy contínuo.

Independente da escolha:
- Branches de vida curta. Branch longa = merge doloroso garantido.
- Nomeie de forma previsível: `feat/checkout-pix`, `fix/2291-timeout-gateway`.
- `main` sempre verde: se o CI quebrou, consertar é prioridade sobre qualquer feature.

### 7.5 Higiene do repositório

- `.gitignore` desde o primeiro commit; nunca versione build, `node_modules`, `.env`,
  credenciais, dumps.
- **Segredo commitado é segredo vazado**: rotacione a chave, não basta remover o arquivo.
- Não reescreva histórico já publicado em branch compartilhada (`push --force` na `main`).
  Em branch pessoal, `rebase -i` para limpar antes do PR é ótimo.
- `merge` vs `rebase`: rebase para atualizar sua branch com a base; merge (ou squash) para
  integrar na principal. Escolha uma política e documente-a.
- Use **tags anotadas** para releases (`v1.4.0`) e assine commits/tags quando o contexto
  exigir rastreabilidade.

---

## 8. Code review e Pull Requests

### 8.1 Para quem abre o PR

- **Pequeno.** Abaixo de ~400 linhas alteradas a revisão é útil; acima disso vira carimbo.
  Se a mudança é grande, divida em PRs encadeados (refatoração primeiro, comportamento
  depois).
- **Descrição responde:** o que muda, por quê, como testar, o que ficou de fora, riscos.
  Link para issue/ticket.
- **Auto-revise antes de pedir revisão.** Leia o próprio diff; metade dos comentários você
  mesmo faria.
- **CI verde antes de chamar alguém.** Não gaste o tempo do revisor com lint quebrado.
- **Inclua evidência** quando for visual ou comportamental: print, gif, saída de log,
  passos de teste.

### 8.2 Para quem revisa

- **Revise em até ~24h.** PR parado bloqueia pessoa e envelhece.
- **Separe severidade:**
  - `bloqueante:` bug, risco de segurança, quebra de contrato.
  - `sugestão:` melhoria que vale discutir.
  - `nit:` preferência pessoal, não bloqueia.
- **Comente o código, não a pessoa.** "Essa função pode receber `null` aqui" em vez de
  "você esqueceu".
- **Pergunte antes de afirmar.** "Existe motivo para não usar transação aqui?" costuma
  revelar contexto que você não tinha.
- **Elogie o que está bom.** Review não é só caça a defeito.
- **Aprovar significa "eu me responsabilizo junto".**

### 8.3 O que olhar (em ordem de importância)

1. **Correção** — resolve o problema? há caso de borda ignorado?
2. **Segurança** — entrada validada? injeção? dado sensível em log? autorização checada?
3. **Design** — está no lugar certo? cria acoplamento indevido?
4. **Testes** — cobrem o comportamento novo e o caso que falhava?
5. **Legibilidade e nomes.**
6. **Performance** — só onde importa (loop sobre I/O, consulta N+1, alocação em hot path).
7. **Estilo** — deveria estar automatizado; se está sendo discutido, configure o linter.

### 8.4 Resolvendo conflitos de opinião

Discussão que passa de dois turnos vai para chamada de 10 minutos. Se persistir, decide-se
por critério explícito (custo de manutenção, reversibilidade, padrão já existente no
projeto) e registra-se a decisão — de preferência num ADR (seção 11.3).

---

## 9. Versionamento e changelog

### 9.1 Versionamento semântico (SemVer)

```
MAJOR.MINOR.PATCH        ex.: 2.7.3

MAJOR → mudança incompatível (quebra quem usa)
MINOR → funcionalidade nova, compatível com versões anteriores
PATCH → correção de bug, compatível
```

Pré-lançamento e build: `1.0.0-rc.1`, `1.0.0+20260901`.

Regras que as pessoas esquecem:
- `0.x.y` significa "instável, tudo pode quebrar".
- Depois de publicar, a versão é **imutável**: corrigiu? publique nova versão.
- Mudar comportamento observável sem mudar assinatura **também** é breaking change.
- Deprecar não é remover: deprecar é MINOR, remover é MAJOR.

### 9.2 CHANGELOG

Mantenha um `CHANGELOG.md` legível por humanos (padrão *Keep a Changelog*):

```markdown
## [2.8.0] - 2026-09-01

### Added
- Endpoint `GET /v2/pedidos/{id}/rastreio`.

### Changed
- `POST /v2/pedidos` agora aceita `canal` opcional (default: `web`).

### Deprecated
- Campo `status_legacy` na resposta de pedido. Use `status`. Remoção prevista para 3.0.0.

### Removed
- Suporte a TLS 1.1.

### Fixed
- Timeout do gateway não marcava mais o pedido como recusado (#2291).

### Security
- Atualização de dependência com CVE-2026-XXXX.
```

Boas práticas:
- Escreva para **quem consome**, não para quem desenvolve. "Corrige bug no service" não
  ajuda ninguém; "pedidos pagos por Pix não apareciam no relatório diário" ajuda.
- Uma entrada por mudança perceptível; mudanças internas invisíveis não precisam entrar.
- Sempre destaque **breaking changes** com instruções de migração.
- Gere automaticamente a partir dos Conventional Commits (semantic-release, changesets,
  git-cliff) — mas revise o texto antes de publicar.
---

## 10. APIs: design, evolução e documentação de mudanças

Vale para API HTTP, gRPC, GraphQL, biblioteca pública, contrato de mensageria e até
interface interna entre módulos: **API é promessa**. O custo de quebrar é pago por
terceiros.

### 10.1 Design

- **Consistência acima de elegância.** Um padrão medíocre aplicado em todo lugar é melhor
  que três padrões ótimos misturados (nomes de campo, formato de data, paginação, erro).
- **Substantivos para recursos, verbos no método:** `GET /pedidos/{id}`, não
  `GET /buscarPedido?id=`.
- **Use o protocolo:** códigos de status corretos (200/201/204/400/401/403/404/409/422/429/
  5xx), verbos com a semântica certa, cabeçalhos padrão.
- **Datas em ISO-8601 com fuso** (`2026-09-01T14:03:00Z`). Sempre.
- **Dinheiro nunca em ponto flutuante:** inteiro em centavos ou decimal com escala + moeda.
- **Identificadores opacos:** o cliente não deve inferir nada do formato do ID.
- **Paginação desde o dia 1** (cursor é mais robusto que offset), com limites máximos.
- **Idempotência** em operações de escrita sensíveis (`Idempotency-Key`), para que retry não
  duplique cobrança.
- **Erros padronizados e legíveis por máquina** — o formato *Problem Details*
  (RFC 9457/7807) é um bom padrão:

```json
{
  "type": "https://api.exemplo.com/erros/saldo-insuficiente",
  "title": "Saldo insuficiente",
  "status": 422,
  "detail": "Saldo disponível de R$ 12,00 é menor que o valor solicitado de R$ 50,00.",
  "instance": "/contas/889/saques/44",
  "codigo": "SALDO_INSUFICIENTE",
  "trace_id": "01JD8Z7..." 
}
```

O `codigo` deve ser **estável**; a mensagem pode mudar, o código não. Documente todos.

- **Rate limiting** com cabeçalhos informativos e `429 + Retry-After`.
- **Nada de dado sensível na URL** (ele vai para log, histórico, referer).

### 10.2 Compatibilidade: o que quebra e o que não quebra

**Mudanças compatíveis (aditivas, seguras):**
- Adicionar endpoint novo.
- Adicionar campo **opcional** na requisição (com default).
- Adicionar campo novo na resposta — desde que os clientes ignorem campos desconhecidos
  (documente isso como regra do seu contrato!).
- Adicionar novo valor de enum **se** os clientes forem tolerantes (senão, é breaking).

**Mudanças incompatíveis (breaking):**
- Remover ou renomear campo/endpoint/parâmetro.
- Tornar obrigatório um campo antes opcional.
- Mudar tipo, formato, unidade ou semântica de um campo (`valor` de reais para centavos é
  breaking silencioso — o pior tipo).
- Mudar código de status, código de erro ou regra de validação.
- Mudar ordenação padrão, tamanho de página padrão ou comportamento default.
- Reduzir garantias (deixar de ser idempotente, virar assíncrono).

> **Regra prática:** se o cliente precisa mudar o código dele para continuar funcionando,
> é breaking change — mesmo que o "contrato técnico" não tenha mudado.

### 10.3 Estratégias de versionamento

| Estratégia | Exemplo | Comentário |
|---|---|---|
| Path | `/v1/pedidos` | Mais simples e visível; o mais comum |
| Header | `Accept: application/vnd.api.v2+json` | Mais "puro", menos legível/testável |
| Query | `?version=2` | Fácil de esquecer, cache complica |
| Sem versão + aditivo | GraphQL, gRPC | Evolução contínua com deprecação de campos |

Recomendações:
- Versione **só quando precisar quebrar**; prefira evoluir de forma aditiva.
- Mantenha no máximo 2 versões vivas — cada versão extra é manutenção dobrada.
- Nunca faça a v2 ser um fork eterno da v1: tenha data de desligamento da antiga.

### 10.4 Documentar mudanças na API (o processo)

Esta é a parte que quase todo time faz mal. Um processo mínimo e suficiente:

**1. Especificação como fonte da verdade**
Mantenha um contrato versionado no repositório: OpenAPI/Swagger (REST), `.proto` (gRPC),
schema (GraphQL), AsyncAPI (eventos/filas). A documentação é **gerada** dele; documentação
escrita à mão desatualiza em semanas.

**2. Diff automático de contrato no CI**
Ferramentas como `oasdiff`, `buf breaking`, `graphql-inspector`, `openapi-diff` detectam
breaking change no pull request. O PR que quebra o contrato falha o build ou exige label
explícita `breaking-change` com aprovação. Isso transforma "documentar mudança" de disciplina
em automação.

**3. Entrada no CHANGELOG da API**
Um changelog **da API**, separado do changelog interno do serviço, escrito na linguagem do
consumidor:

```markdown
## 2026-09-01 — v2.8

**Novo**
- `GET /v2/pedidos/{id}/rastreio` devolve os eventos de entrega.

**Alterado (compatível)**
- `POST /v2/pedidos` aceita `canal` (opcional, default `web`).

**Depreciado**
- `GET /v2/pedidos?status_legacy=` — use `status=`. Desligamento: 2027-03-01.
  Guia de migração: /docs/migracao/status-legacy

**Breaking (somente v3)**
- `valor` passa de reais decimais para centavos inteiros.
  Antes: `{"valor": 12.34}` → Depois: `{"valor_centavos": 1234}`
```

**4. Ciclo de deprecação explícito**

```
Anunciar → Marcar → Avisar em runtime → Medir uso → Desligar
```

- **Anunciar** com antecedência compatível com o público (interno: semanas; parceiro
  externo: 6–12 meses).
- **Marcar** na spec (`deprecated: true` no OpenAPI, `@deprecated` no GraphQL/proto) com o
  substituto indicado.
- **Avisar em runtime** com os cabeçalhos padronizados:
  `Deprecation: true`, `Sunset: Sat, 01 Mar 2027 00:00:00 GMT`,
  `Link: <https://docs.exemplo.com/migracao/status-legacy>; rel="deprecation"`.
- **Medir uso** por cliente antes de desligar; avise nominalmente quem ainda chama.
- **Desligar** com desligamento progressivo (*brownout*: derrubar o endpoint por 1h em
  janelas anunciadas) antes do corte definitivo.

**5. Guia de migração, não só changelog**
Para toda breaking change, escreva um documento curto com: o que mudou, por quê, exemplo
antes/depois, prazo, e o que fazer se você não puder migrar a tempo.

**6. Comunicação ativa**
Changelog no portal, e-mail/webhook para consumidores registrados, aviso no canal do time
para APIs internas. Documentação que ninguém foi avisado que existe não conta como aviso.

**7. Contract tests**
Testes de contrato no CI do provedor rodando contra as expectativas dos consumidores fecham
o ciclo: você descobre a quebra antes do cliente.

### 10.5 Checklist de mudança de API

- [ ] A mudança é aditiva? Se não, justifiquei por que precisa quebrar?
- [ ] Spec (OpenAPI/proto/schema) atualizada no mesmo PR do código?
- [ ] Diff de contrato rodou no CI e o resultado foi avaliado?
- [ ] CHANGELOG da API atualizado em linguagem de consumidor?
- [ ] Novos códigos de erro documentados?
- [ ] Se depreciei: substituto indicado, data de sunset, cabeçalhos, guia de migração?
- [ ] Consumidores identificados e avisados?
- [ ] Testes de contrato passando?
- [ ] Exemplos da documentação atualizados e funcionando?
- [ ] Feature flag / rollout gradual para mudança de comportamento?

---

## 11. Documentação de projeto

### 11.1 README

O README é a porta de entrada. Deve responder, em ordem:

1. **O que é isso** (uma frase).
2. **Para que serve / que problema resolve.**
3. **Como rodar localmente** — do zero, com comandos copiáveis que realmente funcionam.
4. **Como rodar os testes.**
5. **Como configurar** (variáveis de ambiente, com exemplo em `.env.example`).
6. **Como fazer deploy.**
7. **Arquitetura em 5 linhas + diagrama** (opcional, mas transformador).
8. **Onde pedir ajuda / quem é dono.**

Teste do README: alguém novo consegue subir o projeto sem chamar ninguém? Se não, ele está
errado.

### 11.2 Docs-as-code

Documentação vive **no repositório**, versionada junto com o código, revisada no mesmo PR.
Wiki externa desatualiza porque não faz parte do fluxo de mudança.

### 11.3 ADR — Architecture Decision Records

Registre decisões arquiteturais relevantes em arquivos curtos (`docs/adr/0007-fila-sqs.md`):

```markdown
# ADR 0007 — Usar SQS em vez de Kafka para o pipeline de notificações

- Status: aceito
- Data: 2026-09-01

## Contexto
Volume estimado de 50k mensagens/dia, sem necessidade de replay histórico
nem de múltiplos consumidores independentes. Time sem experiência operacional
com Kafka.

## Decisão
Usar SQS com DLQ.

## Consequências
+ Custo e operação muito menores; entrega gerenciada.
− Sem replay longo; se precisarmos disso, migração será necessária.
− Ordenação apenas com FIFO queue (limite de throughput).

## Alternativas consideradas
Kafka (rejeitado: custo operacional), Redis Streams (rejeitado: durabilidade).
```

Meia página, escrita uma vez, evita a pergunta "por que diabos isso é assim?" repetida por
anos. Registre a decisão **inclusive quando ela for rejeitada depois** — supersede o ADR
antigo em vez de apagá-lo.

### 11.4 Documentação de código

- Documente a **interface pública**: o que faz, parâmetros, retorno, erros lançados,
  efeitos colaterais, thread-safety, complexidade quando relevante.
- Exemplos de uso valem mais que descrição prosaica.
- Doc que só repete a assinatura é ruído; delete.

### 11.5 Runbooks

Para todo serviço em produção: o que fazer quando o alerta X dispara, como reiniciar, como
reverter, quais dashboards olhar, quem escalar. Escrito antes do incidente, não durante.

---

## 12. Logs, métricas e observabilidade

### 12.1 Os três pilares

- **Logs** — eventos discretos, com contexto. "O que aconteceu neste request?"
- **Métricas** — números agregados ao longo do tempo. "Como o sistema está agora?"
- **Traces** — o caminho de uma requisição atravessando serviços. "Onde foi o tempo?"

### 12.2 Logs

- **Estruturados (JSON)**, não frases livres. Máquina precisa filtrar.
- **Com identificador de correlação** (`trace_id`/`request_id`) propagado por todo o fluxo,
  inclusive entre serviços e filas. Sem isso, investigar incidente é adivinhação.
- **Níveis com significado combinado:**
  - `ERROR` — precisa de ação humana; alguém deveria ser acordado se for grave.
  - `WARN` — anomalia tolerada, vale investigar em volume.
  - `INFO` — marcos de negócio ("pedido 889 criado").
  - `DEBUG` — detalhe para desenvolvimento.
- **Nunca logue segredo ou dado pessoal:** senha, token, cartão, CPF completo, conteúdo de
  mensagem. Mascare (`****1234`) e trate LGPD/GDPR como requisito, não como detalhe.
- **Logue no início e no fim das operações relevantes**, com duração e resultado.
- **Volume tem custo.** Log de tudo em `INFO` num loop quente é dinheiro queimado e ruído.

```json
{"ts":"2026-09-01T14:03:00Z","level":"ERROR","msg":"falha ao cobrar pedido",
 "pedido_id":"889","gateway":"stripe","erro":"timeout após 5s",
 "trace_id":"01JD8Z7","tentativa":3}
```

### 12.3 Métricas

- **RED** para serviços: **R**ate (requisições/s), **E**rrors (taxa de erro),
  **D**uration (latência).
- **USE** para recursos: **U**tilization, **S**aturation, **E**rrors.
- Meça **percentis** (p50, p95, p99), não média. A média esconde exatamente o cliente que
  está sofrendo.
- Meça também métricas de **negócio** (pedidos/minuto, taxa de conversão): elas detectam
  incidentes que a infraestrutura não vê.

### 12.4 Alertas

- Alerte por **sintoma percebido pelo usuário** (erro, latência, fila crescendo), não por
  causa interna (CPU a 80% pode ser perfeitamente normal).
- Todo alerta precisa ser **acionável** e ter runbook. Alerta que não gera ação treina o
  time a ignorar alertas.
- Combata a fadiga de alerta com agressividade: alerta ruidoso é pior que nenhum.

### 12.5 SLO

Defina objetivos explícitos ("99,9% das requisições abaixo de 300ms no mês") e use o *error
budget* para decidir entre entregar features e investir em confiabilidade. Isso troca
discussão de opinião por dado.

---

## 13. Segurança

### 13.1 Regras não negociáveis

- **Nunca confie na entrada.** Valide tipo, formato, tamanho e faixa, em **allowlist**
  (o que é permitido) e não em blocklist.
- **Valide no servidor.** Validação de front-end é usabilidade, não segurança.
- **Parametrize consultas.** Concatenar string em SQL/NoSQL/LDAP/comando de shell é a
  origem de toda injeção.
- **Escape na saída** conforme o contexto (HTML, atributo, JS, URL) para evitar XSS.
- **Nunca implemente criptografia própria.** Use bibliotecas consagradas e algoritmos
  atuais.
- **Senhas com hash lento e salgado** (Argon2id, bcrypt, scrypt) — jamais MD5/SHA sozinho,
  jamais texto puro, jamais criptografia reversível.
- **TLS em tudo**, inclusive tráfego interno quando viável.

### 13.2 Autenticação e autorização

- Separe os conceitos: **autenticação** = quem você é; **autorização** = o que pode fazer.
- **Cheque autorização em toda operação**, no servidor, considerando o objeto acessado
  (evita IDOR: `GET /pedidos/889` de outro cliente).
- **Menor privilégio:** cada serviço, usuário e token com o mínimo necessário e escopo
  limitado.
- **Tokens curtos + refresh**, revogáveis, com rotação.
- Considere MFA para operações e contas sensíveis.

### 13.3 Segredos

- Nunca no código, no repositório, no Dockerfile, no ticket ou no chat.
- Use gerenciador de segredos (Vault, AWS/GCP Secrets Manager, variáveis injetadas).
- **Rotação periódica** e rotação imediata em caso de suspeita.
- Escaneie o repositório em busca de segredos no CI (gitleaks, truffleHog).

### 13.4 Dependências

- Fixe versões (lockfile) e faça builds reprodutíveis.
- Escaneie vulnerabilidades continuamente (Dependabot/Renovate + SCA).
- Atualize com frequência e em pequenas doses; atualização acumulada de dois anos é um
  projeto, não uma tarefa.
- Avalie antes de adotar: manutenção ativa? licença compatível? quantas dependências
  transitivas arrasta?
- Gere **SBOM** quando o contexto exigir rastreabilidade de supply chain.

### 13.5 Privacidade e dados pessoais

- Colete o mínimo necessário e defina retenção (dado guardado para sempre é passivo).
- Criptografe em trânsito e em repouso.
- Pseudonimize/anonimize em ambientes de teste — nunca copie base de produção crua para
  homologação.
- Registre acesso a dado sensível (trilha de auditoria).

### 13.6 Pense como atacante

Faça *threat modeling* leve em features sensíveis: quem quer abusar disso, como, e o que
acontece se conseguir. Meia hora de conversa evita boa parte dos incidentes. Referência
prática: OWASP Top 10 e OWASP ASVS.

---

## 14. Performance

### 14.1 A ordem correta

```
1. Faça funcionar
2. Faça certo (claro, testado)
3. Faça rápido — se medir mostrar que precisa
```

"Otimização prematura é a raiz de todo mal" não é desculpa para escolher um algoritmo
quadrático quando o linear é igualmente simples. É um alerta contra micro-otimizações sem
evidência.

### 14.2 Medir antes de otimizar

- **Profile, não palpite.** A intuição sobre gargalo erra a maior parte das vezes.
- Otimize o que domina o tempo. 90% do ganho está em <10% do código.
- Estabeleça baseline e meça de novo depois: otimização sem número é fé.

### 14.3 Onde os problemas realmente moram

- **I/O, não CPU:** rede e disco são ordens de grandeza mais lentos.
- **Consultas N+1** (o clássico dos ORMs): 1 query vira 1.001.
- **Falta de índice** e consultas sem limite.
- **Serialização/desserialização** de payloads gigantes.
- **Chamadas sequenciais** que poderiam ser paralelas.
- **Trabalho síncrono que deveria ser assíncrono** (e-mail, PDF, relatório → fila).

### 14.4 Cache

Cache resolve latência e cria problema de consistência. Antes de adicionar, defina:
- **O que** cachear (dados caros e pouco voláteis).
- **Por quanto tempo** (TTL) e **como invalidar**.
- O que acontece no **cache miss em massa** (*stampede*) — use lock ou jitter no TTL.
- Se dado desatualizado por N segundos é aceitável para o negócio.

### 14.5 Complexidade

Conheça a complexidade do que você escreve (O(n), O(n log n), O(n²)) e, principalmente,
**qual é o n real em produção**. O(n²) com n=10 é irrelevante; com n=100.000 é um incidente.

---

## 15. Configuração, ambientes e segredos

- **Configuração fora do código** (princípio do 12-Factor App): mesma imagem/artefato roda
  em todos os ambientes, mudando só a configuração.
- **Variáveis de ambiente** para o que muda por ambiente; arquivo versionado para o que é
  igual em todos.
- **Valide a configuração na inicialização** e falhe imediatamente se faltar algo. Melhor
  não subir que subir quebrado e descobrir no primeiro request.
- **Sem valores padrão perigosos:** default nunca deve ser "aponta para produção",
  "autenticação desligada" ou "modo debug ligado".
- **Paridade entre ambientes:** quanto mais dev/homologação diferem de produção, mais bugs
  só aparecem em produção.
- **Feature flags** para separar deploy de release — permitem entregar código desligado,
  ligar para 1% dos usuários e reverter sem novo deploy. Contrapartida: flags têm que ser
  removidas depois; flag esquecida vira dívida e caminho de código não testado.

---

## 16. CI/CD e automação

### 16.1 O que o pipeline deve fazer, em ordem

```
lint + format check → build → testes unitários → testes de integração
→ análise estática/segurança (SAST, dependências, segredos)
→ diff de contrato de API → artefato versionado → deploy staging
→ smoke tests → deploy produção (gradual) → verificação pós-deploy
```

### 16.2 Princípios

- **Automatize tudo o que é repetido.** Passo manual em release é onde o erro entra.
- **Rápido:** feedback do pipeline principal em minutos. Pipeline de 40 minutos faz o time
  parar de rodar antes de abrir PR.
- **Determinístico:** teste que falha aleatoriamente (*flaky*) deve ser corrigido ou
  quarentenado. Falha intermitente tolerada destrói a confiança na suíte inteira.
- **O mesmo artefato promovido entre ambientes.** Não recompile para produção.
- **Deploys pequenos e frequentes.** Menor lote = menor risco = rollback trivial.
- **Rollback sempre possível e testado.** Saber reverter vale mais que evitar erro.
- Estratégias: *blue-green*, *canary*, *rolling* — escolha conforme o custo de falha.
- **Infraestrutura como código** (Terraform, Pulumi, manifests versionados): ambiente
  reproduzível e revisável.

---

## 17. Dados e migrações

- **Migrações versionadas e no repositório**, aplicadas por ferramenta (Flyway, Liquibase,
  Alembic, migrations do framework) — nunca por SQL rodado à mão em produção.
- **Sempre para frente e reversíveis quando possível.** Tenha o plano de reversão escrito
  antes de aplicar.
- **Migração e deploy são independentes** — use o padrão *expand/contract*:

```
1. EXPAND   → adiciona a coluna nova (nullable), sem remover a antiga
2. MIGRATE  → código passa a escrever nas duas e ler da nova (backfill dos dados)
3. CONTRACT → depois que tudo está migrado e estável, remove a coluna antiga
```

Isso permite deploy sem downtime e rollback em qualquer etapa.

- **Backfill em lotes**, com pausa e monitoramento — `UPDATE` em 50 milhões de linhas numa
  transação derruba o banco.
- **Nunca renomeie/apague coluna no mesmo deploy** que muda o código: alguma instância antiga
  ainda está rodando.
- **Backup testado.** Backup nunca restaurado não é backup; agende restauração de teste.
- **Cuidado com dados em produção:** todo script que altera dado precisa de revisão, dry-run
  e contagem esperada de linhas afetadas.

---

## 18. Dívida técnica e refatoração

### 18.1 Dívida técnica é uma decisão, não um acidente

Existe dívida **deliberada** ("vamos entregar simplificado para validar a hipótese, com
prazo para refazer") e dívida **acidental** (ninguém sabia melhor). A primeira é
ferramenta legítima; a segunda é aprendizado. O que não pode é dívida invisível.

Torne-a explícita: registre em issue com contexto, impacto e custo estimado. Dívida que não
está escrita não entra em priorização e só cobra juros.

### 18.2 Refatoração

- **Refatorar = mudar estrutura sem mudar comportamento.** Se o comportamento mudou, não é
  refatoração, é reescrita — e precisa de outro tipo de cuidado.
- **Só refatore com rede de testes.** Sem teste, você está apenas reescrevendo com fé.
- **Em passos pequenos, com commits separados** dos commits de comportamento. Isso torna o
  PR revisável e o `revert` cirúrgico.
- **Refatore junto com a feature** que passa por aquele código ("regra do escoteiro"), em
  vez de esperar o sprint mítico de refatoração que nunca é priorizado.

### 18.3 Reescrever do zero

Quase sempre é a decisão errada. O sistema antigo, feio como é, contém anos de correções de
casos de borda que ninguém documentou. Prefira estrangulamento gradual (*strangler fig*):
o novo sistema intercepta e assume funcionalidades uma a uma até o antigo poder ser
desligado.

---

## 19. Anti-padrões comuns

| Anti-padrão | Por que dói |
|---|---|
| **God object / God service** | Classe que sabe tudo; toda mudança passa por ela |
| **Código copiado e adaptado** | O bug corrigido em um lugar sobrevive nos outros quatro |
| **Números e strings mágicos** | `if status == 3` — ninguém sabe o que é 3 daqui a um ano |
| **Aninhamento profundo** | 5 níveis de `if`; resolva com *early return* e guard clauses |
| **Engolir exceção** | Erro some, sintoma aparece três camadas depois, sem rastro |
| **Otimização prematura** | Complexidade paga sem evidência de ganho |
| **Abstração prematura** | Framework interno para um caso de uso; mais caro que a duplicação |
| **Flags booleanas de comportamento** | `processar(true, false, true)` é ilegível |
| **Estado global mutável** | Bugs não reproduzíveis, testes que só passam em certa ordem |
| **Comentário como muleta** | Comentar código ruim em vez de melhorar o código |
| **Branch de vida longa** | Merge hell garantido, integração adiada até o pior momento |
| **PR gigante** | Ninguém revisa 3.000 linhas de verdade; vira aprovação simbólica |
| **Testes acoplados à implementação** | Quebram em toda refatoração; viram um freio |
| **Ambiente de dev diferente de prod** | "Na minha máquina funciona" |
| **Log sem correlação** | Investigar incidente vira arqueologia |
| **Segredo no repositório** | Vazamento permanente no histórico |
| **Versionar sem changelog** | Consumidor descobre a quebra em produção |
| **Deprecar sem prazo nem aviso** | Quebra a confiança de quem consome sua API |

---

## 20. Checklists rápidos

### Antes de commitar

- [ ] O commit é atômico e tem uma única mudança lógica?
- [ ] A mensagem explica o **porquê** e segue a convenção do projeto?
- [ ] Testes rodando e passando localmente?
- [ ] Sem `console.log`/`print` de depuração, código comentado ou `TODO` órfão?
- [ ] Sem segredo, credencial ou dado real de cliente?
- [ ] Lint e formatador rodaram?

### Antes de abrir o PR

- [ ] Diff pequeno e coeso (idealmente < 400 linhas)?
- [ ] Descrição com contexto, motivo, como testar e riscos?
- [ ] Auto-revisão feita?
- [ ] CI verde?
- [ ] Documentação/spec/changelog atualizados no mesmo PR?
- [ ] Migração de banco compatível com a versão anterior do código?

### Antes de fazer release

- [ ] Versão seguindo SemVer?
- [ ] CHANGELOG escrito para o consumidor, com breaking changes destacadas?
- [ ] Guia de migração publicado, se houver quebra?
- [ ] Testes de contrato e smoke tests passando?
- [ ] Plano de rollback definido e testado?
- [ ] Consumidores avisados, quando aplicável?
- [ ] Monitoramento e alertas cobrindo o que mudou?

### Ao criar um projeto/serviço novo

- [ ] README que permite subir o projeto do zero?
- [ ] `.gitignore`, `.env.example`, formatador e linter configurados?
- [ ] Pipeline de CI mínimo desde o primeiro commit?
- [ ] Estratégia de testes definida?
- [ ] Log estruturado com correlação e healthcheck?
- [ ] Métricas e alertas básicos (RED)?
- [ ] Gestão de segredos definida?
- [ ] Dono e runbook registrados?
- [ ] Primeiro ADR escrito com as decisões iniciais?

---

## 21. Leituras recomendadas

**Livros**
- *Clean Code* — Robert C. Martin (com senso crítico; nem toda regra envelheceu bem)
- *The Pragmatic Programmer* — Hunt & Thomas
- *Refactoring* — Martin Fowler
- *Domain-Driven Design* / *Implementing DDD* — Evans / Vernon
- *Designing Data-Intensive Applications* — Martin Kleppmann
- *Working Effectively with Legacy Code* — Michael Feathers
- *Accelerate* — Forsgren, Humble & Kim (o que realmente correlaciona com performance de time)
- *Site Reliability Engineering* — Google

**Referências online**
- Conventional Commits — conventionalcommits.org
- Semantic Versioning — semver.org
- Keep a Changelog — keepachangelog.com
- The Twelve-Factor App — 12factor.net
- OWASP Top 10 e OWASP Cheat Sheet Series — owasp.org
- Google Engineering Practices (code review) — google.github.io/eng-practices
- RFC 9457 — Problem Details for HTTP APIs
- Refactoring Guru (padrões e refatorações) — refactoring.guru

---

> **Fechamento.** Nenhuma dessas práticas vale mais que o julgamento de quem conhece o
> contexto. O objetivo final não é seguir regras: é entregar software que funciona, que
> pode ser mudado com segurança e que a próxima pessoa consegue entender. Quando uma regra
> deste documento atrapalhar esses três objetivos, a regra está errada para o seu caso —
> mas escreva por que você a quebrou.
