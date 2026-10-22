# Relatório de resultados: construção (Javalin) e manutenção

18 de outubro de 2026. Três modelos (Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5), três braços (N0, N1 e
N2), uma execução por combinação, effort `medium`. A correção (construção) e a avaliação (manutenção) foram feitas por
Claude Opus 5.5, effort `high`, às cegas. Este relatório substitui, para a construção, o
`relatorio-por-modelo-rodada-1.md`, que trata do lote em Spring Boot; a parte da manutenção é a mesma.

## 1. O que foi feito

**As duas pesquisas.**
- **Construção** (`construction-v6`, lote `batch-2`): o agente recebe uma pasta vazia e o enunciado de uma loja online
  (o serviço de pedidos: resumo da compra, registro, pagamento, envio, estoque, avisos) e constrói tudo em **Java 21 e
  Javalin 6.7.0, sem Spring**, com PostgreSQL. São 9 execuções. O enunciado esconde quatro pontos que pedem o
  Strategy (a entrega, o cupom, a forma de pagamento, o nível do clube), um que pede o Adapter (a transportadora
  TransLog), um que pede o State (a situação do pedido) e controles negativos que parecem pedir estrutura e não pedem.
- **Manutenção** (`maintenance-v1`): o agente recebe um sistema pronto e sem nenhum padrão (as reservas de uma pousada,
  em Javalin) e um pedido de mudança na voz do dono. Os cenários ímpares pedem um padrão sem dizer o nome (01 Strategy,
  03 Observer, 05 Factory Method, 07 Decorator, 09 Template Method); os pares são controles negativos. São 90
  execuções.

**Os braços.** N0: só o enunciado ou o pedido. N1: mais um `CLAUDE.md` curto com dois princípios (o próximo caso num
lugar só; nada de estrutura para o que ninguém pediu), sem nomear padrão. N2: o mesmo `CLAUDE.md` com uma linha a
mais, mandando consultar a skill `gof-patterns`, que vai junto.

**Como a construção foi medida.**
- **A correção da IA**, nas 8 dimensões: arquitetura (A1 a A6), banco (B1 a B10), testes (T1 a T4), comportamento
  (C1 a C10, em que a IA sobe o serviço com um PostgreSQL, imita a TransLog e testa pela API, inclusive com duas
  máquinas e avisos e pagamentos simultâneos) e os quatro patterns (Strategy, State, Adapter, Factory). A resposta é
  uma análise escrita com uma linha de conclusão por item.
- **O material** que a IA recebe: o relatório de arquitetura (ArchUnit), o PIT (quantos erros plantados os testes do
  próprio agente pegam) e a foto do esquema que o serviço cria.
- **A suíte de referência**, fora da correção, como conferência: 463 verificações automáticas (o catálogo, 437
  resumos, o ciclo do pedido, o reinício e a concorrência).

**As palavras.** Nos pontos que pedem um padrão: **acerto** (o padrão aplicado direito), **alternativa equivalente**
(outro desenho que resolve igual, só na manutenção), **malfeito** (a estrutura existe, mas algum caso ou regra escapa
dela), **omissão** (a variação escrita com `if`, `switch` ou cópia). Nos controles negativos: **contenção** (a
resposta simples) e **exagero** (estrutura que ninguém pediu). Nas outras dimensões: **segue**, **em parte**, **não
segue**, **não se aplica**. A palavra é o resumo; a análise escrita de cada item é a correção.

**O custo.** Construção: US$ 11,90 nas execuções e US$ 47,46 na correção. Manutenção: US$ 31,26 nas execuções dos
cenários 02 a 10 e US$ 24,72 na avaliação.

## 2. Construção em Javalin: o quadro geral

| modelo | braço | custo | tempo | Strategy (12 pontos) | State | Adapter | suíte (463) | comportamento (C1 a C10) | testes | PIT |
|---|---|---|---|---|---|---|---|---|---|---|
| Haiku 4.5 | N0 | US$ 0,79 | 9 min | 12 omissão | omissão | omissão | não sobe* | 2 segue, 4 em parte, 1 não segue, 3 n/a | 3 | 0,3% |
| Haiku 4.5 | N1 | US$ 0,87 | 9 min | 12 omissão | omissão | malfeito | 267 | 4 segue, 4 em parte, 2 não segue | 1 | 0,6% |
| Haiku 4.5 | N2 | US$ 0,96 | 10 min | **5 acerto, 5 malfeito**, 2 omissão | omissão | malfeito | não sobe | não sobe | 1 | 12% |
| Sonnet 4.5 | N0 | US$ 1,27 | 9 min | 12 omissão | omissão | omissão | 221 | 4 segue, 6 em parte | 4 | 1,6% |
| Sonnet 4.5 | N1 | US$ 1,40 | 9 min | 4 acerto, 8 omissão | omissão | **acerto** | 265 | 5 segue, 5 em parte | 2 | 24% |
| Sonnet 4.5 | N2 | US$ 2,40 | 13 min | **11 acerto**, 1 malfeito | omissão | malfeito | 2 | 3 segue, 2 em parte, 5 não segue | 2 | 1,5% |
| Sonnet 5.5 | N0 | US$ 1,03 | 7 min | 12 acerto | omissão | malfeito | **463** | **10 segue** | 10 | 68% |
| Sonnet 5.5 | N1 | US$ 1,06 | 6 min | 12 acerto | malfeito | malfeito | 462 | **10 segue** | 11 | 58% |
| Sonnet 5.5 | N2 | US$ 1,11 | 7 min | 12 acerto | malfeito | malfeito | **463** | **10 segue** | 16 | 50% |

\* Na primeira subida, com o banco vazio, o serviço quebra; na segunda, sobe (detalhe na seção do Haiku).

O Strategy tem 12 pontos: os quatro casos que pedem o padrão, cada um em duas perguntas (onde mora o código de cada
caso e como o caso é escolhido), e os quatro remendos (regras cruzadas que tentam a um `if` fora do lugar). Os
controles negativos deram contenção em quase tudo. As exceções são duas estruturas sem necessidade (o item E), ambas
no N2: o Haiku criou uma interface `ProductProvider` de uma implementação só (registrada na correção do Strategy), e
o Sonnet 4.5 quatro interfaces de repositório que nada substitui (registradas duas vezes, nas correções do State e do
Adapter, porque cada pattern registra o que acha perto dele).

**A arquitetura.** O A1 (a regra de negócio não conhece a web) deu `não segue` em 8 das 9 execuções: quase todos fazem
o serviço receber o corpo da requisição (o `JsonNode`, os DTOs da API) ou carregar o código HTTP nas recusas. A exceção
é o Sonnet 4.5 no N2. Nos ciclos entre pacotes (A2), cinco não têm ciclo (`segue`), três têm ciclos que fecham só por tipo
de dado (`em parte`), e o Sonnet 5.5 no N1 tem um laço que fecha por comportamento (`order` e `checkout` chamando um ao outro): `não segue`. O
resumo e o pedido usam o mesmo cálculo em todos.

**O banco.** Todos os nove criam o esquema com um script próprio (`CREATE TABLE IF NOT EXISTS` ao subir); nenhum usa
Flyway ou Liquibase. Os do Sonnet 5.5 se protegem das duas máquinas criando o esquema ao mesmo tempo com uma trava do
PostgreSQL (`pg_advisory_lock`).

## 3. Construção, modelo a modelo

### Claude Haiku 4.5

**Em uma frase:** em Javalin, nenhum dos três serviços dele funciona por inteiro; o N2 é o braço em que ele aplica
padrões pela primeira vez, mas o serviço desse braço não sobe.

- **N0.** Tudo numa classe de cálculo, com os casos comparados por texto (`coupon.equals("WELCOME10")`) e a conversa
  com a TransLog num método privado dela. O serviço quebra na primeira subida (um `commit()` com o `autoCommit` ligado
  ao gravar o catálogo); na segunda sobe, porque o catálogo já entrou. Mas não registra pedido nenhum: todo `POST
  /orders` dá erro 500, porque o JDBC não sabe gravar um `java.time.Instant`.
- **N1.** Ainda sem padrão nenhum (os casos viraram enums que são só rótulo, e o cálculo é `switch`), mas o cliente da
  TransLog ficou numa classe própria, recebendo quilos e devolvendo reais e dias (só um detalhe vaza: `malfeito`). O
  serviço sobe e guarda tudo, mas toda resposta que leva uma data dá erro 500: falta ao Jackson o módulo de datas. A
  correção foi além da resposta e conferiu no banco que as operações aconteceram (o estoque baixou, o crédito entrou).
  A suíte: 267 de 463.
- **N2.** **Abriu a skill e aplicou o Strategy** no cupom, no pagamento e no clube (interface, uma classe por caso,
  uma fábrica que escolhe), com o clube bem resolvido; a entrega ficou de fora. Criou também um `TransLogAdapter`, mas
  ele só repassa a resposta crua, e a tradução das unidades ficou no cálculo (`malfeito`). O serviço não sobe: o SQL do
  esquema tem um erro de sintaxe (um `REFERENCES orders(id)` solto). E criou uma interface de uma implementação só
  (exagero no item E).
- **Testes:** 3, 1 e 1, quase sem cobertura (o melhor PIT é 12%). Dois dos três projetos escolheram JUnit 4.

### Claude Sonnet 4.5

**Em uma frase:** o harness muda o desenho dele (de omissão a Strategy nos quatro pontos), mas o serviço sofre com o
que o Javalin não dá pronto, e o N2 quebra inteiro por uma dependência esquecida.

- **N0.** Omissão: um serviço por assunto (`ShippingService`, `CouponService`...), cada um com um `switch` nos casos, e
  a TransLog num método privado do serviço de entrega. A suíte: 221. Um valor inválido no JSON (nível, região, cupom)
  vira `500 INTERNAL_ERROR` em vez de `422` com o código da tabela; o pagamento aceita campo vazio. A correção registrou
  bem a concorrência: a disputa pela última peça é resolvida pelo banco (`UPDATE ... WHERE stock >= ?`).
- **N1.** Só os cupons viraram Strategy; a entrega, o pagamento e o clube ficaram em `switch` no cálculo. Em compensação,
  é **o único `acerto` de Adapter do lote**: o cliente da TransLog recebe quilos e devolve reais e dias, e nada dela
  vaza. A suíte: 265. O WELCOME10 é aceito depois de um pedido pago, e cancelar um pedido já cancelado é aceito.
- **N2.** **Strategy nos quatro pontos** (interfaces, uma classe por caso, mapas montados no `main`, as regras cruzadas
  dentro do caso certo), um Observer no pagamento confirmado, e o único A1 `segue` do lote. Abriu a skill. Mas
  **esqueceu a dependência do Jackson**: o Javalin não tem como gerar JSON, e toda resposta dá erro 500 ("you don't
  have an object mapper configured"). A suíte: 2 de 463. Os 2 testes dele (só da classe `Money`) não pegaram isso.
- **Testes:** 4, 2 e 2, com cobertura entre 1% e 31%.

### Claude Sonnet 5.5

**Em uma frase:** constrói um serviço que passa em tudo nos três braços, já com o Strategy no N0; o harness mexe no
State e no número de testes.

- **Os três braços** passam em 463 (N0 e N2) e 462 (N1) de 463 na suíte, e a correção deu `segue` nos 10 itens do
  comportamento nos três, inclusive nos avisos e pagamentos simultâneos nas duas máquinas. O único desvio da suíte: no
  N1, depois do reinício, os itens de um pedido voltam em outra ordem.
- **Strategy** nos quatro pontos já no N0: interfaces para a entrega, o cupom e o pagamento, uma classe por caso, um
  mapa por código; o clube é um enum só com dados (a taxa, a isenção do frete, o piso do brinde), o que a régua aceita.
- **State:** omissão no N0 (o serviço compara a situação); malfeito no N1 (um enum com a tabela de transições) e no N2
  (uma tabela de operações separada da situação). Nenhum fez a situação decidir sozinha.
- **Adapter:** `malfeito` nos três. O cliente da TransLog é bom (uma porta, o preço já em reais, o `ZIP_NOT_SERVED`
  tratado), mas a porta foi escrita na língua dela (recebe gramas e o código do serviço).
- **Testes de verdade:** sobem o Javalin numa porta livre, com um armazenamento em memória e uma TransLog falsa, e
  conferem os exemplos do financeiro campo a campo. Foram 10, 11 e 16 testes, com o PIT entre 50% e 68%.
- **Eficiência:** 6 a 7 minutos e 13 a 18 turnos por execução (o Haiku e o Sonnet 4.5 usaram de 58 a 97).

## 4. Construção: Javalin contra Spring

O lote anterior (`batch-1`) fez a mesma coisa em Spring Boot. A comparação mostra o que muda quando o framework deixa
de dar pronto o JSON, o banco e a injeção:

| execução | Strategy em Javalin (em Spring) | suíte em Javalin (em Spring) |
|---|---|---|
| Haiku N0 | 12 omissão (9 omissão) | não sobe na 1ª (189) |
| Haiku N1 | 12 omissão (8 omissão, 3 malfeito, 1 acerto) | 267 (não subia) |
| Haiku N2 | 5 acerto, 5 malfeito, 2 omissão (12 omissão) | não sobe (não subia) |
| Sonnet 4.5 N0 | 12 omissão (12 omissão) | 221 (286) |
| Sonnet 4.5 N1 | 4 acerto, 8 omissão (10 acerto, 2 malfeito) | 265 (194) |
| Sonnet 4.5 N2 | 11 acerto, 1 malfeito (10 acerto, 2 malfeito) | 2 (436) |
| Sonnet 5.5 N0, N1, N2 | 12 acerto (12 acerto) | 463, 462, 463 (463 nos três) |

- **O Sonnet 5.5 não sente a troca.** Os mesmos acertos e a suíte perfeita nos dois frameworks.
- **Para o Haiku e o Sonnet 4.5, o Javalin traz erros novos de montagem**: o módulo de datas e a dependência do
  Jackson esquecidos, o JDBC sem saber gravar um `Instant`, o `commit` com `autoCommit`. São coisas que o Spring
  resolvia sozinho.
- **O efeito do harness no desenho aparece nos dois frameworks**, mas não do mesmo jeito: em Spring, o Sonnet 4.5 já
  aplicava o Strategy no N1; em Javalin, só no N2. O Haiku só aplicou no Javalin, no N2.
- **Os testes:** em Spring, eles diminuíam do N0 ao N2 nos três modelos. Em Javalin, isso se repete no Haiku (3, 1, 1)
  e no Sonnet 4.5 (4, 2, 2), mas não no Sonnet 5.5 (10, 11, 16).

## 5. Manutenção

Os mesmos números do relatório da primeira rodada.

| modelo | braço | pedem o padrão (01, 03, 05, 07, 09) | controles (02, 04, 06, 08, 10) | comportamento (02 a 10) |
|---|---|---|---|---|
| Haiku 4.5 | N0 | 5 omissão | 5 contenção | 6 segue, 3 em parte |
| Haiku 4.5 | N1 | 5 omissão | 5 contenção | 6 segue, 3 em parte |
| Haiku 4.5 | N2 | **2 acerto**, 1 malfeito, 2 omissão | 5 contenção | 7 segue, 2 em parte |
| Sonnet 4.5 | N0 | 1 malfeito, 4 omissão | 5 contenção | 6 segue, 3 em parte |
| Sonnet 4.5 | N1 | 2 malfeito, 3 omissão | 5 contenção | 7 segue, 2 em parte |
| Sonnet 4.5 | N2 | **2 acerto**, 2 malfeito, 1 omissão | 5 contenção | 5 segue, 2 em parte, **2 não segue** |
| Sonnet 5.5 | N0 | 1 acerto, 2 malfeito, 2 omissão | 5 contenção | 9 segue |
| Sonnet 5.5 | N1 | **3 acerto, 1 alternativa**, 1 malfeito | 5 contenção | 9 segue |
| Sonnet 5.5 | N2 | 2 acerto, 1 alternativa, 2 malfeito | 4 contenção, **1 exagero** | 9 segue |

| cenário | modelo | N0 | N1 | N2 |
|---|---|---|---|---|
| 01 Strategy | Haiku 4.5 | omissão | omissão | acerto |
| | Sonnet 4.5 | omissão | omissão | acerto |
| | Sonnet 5.5 | acerto | acerto | acerto |
| 03 Observer | Haiku 4.5 | omissão | omissão | malfeito |
| | Sonnet 4.5 | malfeito | malfeito | malfeito |
| | Sonnet 5.5 | malfeito | acerto | malfeito |
| 05 Factory Method | Haiku 4.5 | omissão | omissão | acerto |
| | Sonnet 4.5 | omissão | omissão | acerto |
| | Sonnet 5.5 | omissão | acerto | acerto |
| 07 Decorator | Haiku 4.5 | omissão | omissão | omissão |
| | Sonnet 4.5 | omissão | malfeito | malfeito |
| | Sonnet 5.5 | malfeito | alternativa | alternativa |
| 09 Template Method | Haiku 4.5 | omissão | omissão | omissão |
| | Sonnet 4.5 | omissão | omissão | omissão |
| | Sonnet 5.5 | omissão | malfeito | malfeito |

- **Haiku 4.5.** Sem a skill, nenhum padrão (no 05, dois `if/else` dentro da única classe de documento; no 09, os
  relatórios novos copiados de um antigo). No N2, acertos no Strategy (01, com a skill aberta) e no Factory Method (05,
  sem ela): abriu a skill em 2 das 10 execuções do N2. Comportamento "em parte" em 8 das 27 (05 e 07 nos três braços,
  09 no N0, 08 no N1), quase sempre uma regra do pedido errada no caminho principal certo. O mais barato por execução
  fora o Sonnet 5.5: US$ 7,36 nas 27, uns 4 minutos cada.
- **Sonnet 4.5.** Sem a skill, nenhum acerto. No N2, acertos no 01 e no 05, mas em dois cenários (02 e 09) abriu a
  skill, explicou o padrão e **não mudou o código** (o comportamento saiu `não segue`). Abriu a skill em 8 das 10
  execuções do N2. O mais caro: US$ 18,22 nas 27, uns 5 minutos cada.
- **Sonnet 5.5.** O único com acerto no N0 (o 01) e o que mais acerta com o harness (no N1, 3 acertos e 1 alternativa
  equivalente). A alternativa do Decorator (07) é uma lista de regras que o envio aplica em sequência, que as
  instruções aceitam. O Template Method (09) ninguém acerta: ele criou a classe abstrata, mas o "passo" dela é o
  relatório inteiro. O único exagero da manutenção é dele (N2, cenário 02: as datas da Páscoa viraram objetos atrás de
  uma interface). Comportamento `segue` nas 27, abriu a skill nas 10 do N2, e é o mais barato e rápido: US$ 5,68 nas
  27, menos de 2 minutos cada.

## 6. O que aparece nas duas pesquisas (indícios)

1. **O harness muda o desenho de quem não acerta sozinho.** No Sonnet 4.5 (construção, as duas rodadas) e no Haiku
   (manutenção e construção em Javalin), os acertos aparecem com o harness, e quase sempre no N2. O modelo que já
   sabe (o Sonnet 5.5 no Strategy) não muda.
2. **O N2 é o braço que mais move o Haiku e o Sonnet 4.5; o N1 é o melhor braço do Sonnet 5.5 na manutenção.**
3. **Desenho melhor não é serviço melhor.** O exemplo mais forte é o Sonnet 4.5 no N2 em Javalin: o melhor desenho dele
   e o pior serviço (2 de 463).
4. **Quase ninguém exagera.** Na construção, duas estruturas sem necessidade, ambas no N2 e no item E; na manutenção,
   um exagero em 45 controles, também no N2. O excesso de estrutura, quando aparece, está no braço da skill.
5. **A skill pode travar o trabalho.** As duas execuções da manutenção em que o Sonnet 4.5 não mudou nada são do N2.
6. **O Sonnet 5.5 é o melhor e o mais barato nas duas pesquisas;** o Sonnet 4.5, o mais caro.
7. **Os testes diminuem com o harness no Haiku e no Sonnet 4.5** (nas duas rodadas da construção); no Sonnet 5.5, só na
   rodada em Spring.

## 7. A régua e as ferramentas nesta rodada

- **A régua nova** (18/10): o Adapter ganhou o nível `malfeito` (o lado da TransLog existe, mas algo vaza), e o C7 e o C8
  passaram a testar avisos e pagamentos simultâneos e o que acontece com quem perde a disputa. Validada no serviço
  plantado (deu `malfeito` no P6, como o esperado).
- **A régua ficou neutra ao framework** e vale para o Javalin e para o Spring do serviço plantado.
- **As ferramentas consertadas para o Javalin**: o relatório de arquitetura lista as classes que usam o Javalin; a
  escolha do jar pega o que tem `Main-Class`; a porta vai pela variável `PORT`; o PIT deixou de falhar nos projetos em
  JUnit 4.
- **A correção e a suíte concordaram** onde as duas olham, e a correção foi mais fundo em dois casos: o serviço do Haiku
  N0, que só sobe na segunda tentativa, e o do Haiku N1, em que as operações acontecem no banco mesmo com a resposta
  dando erro 500.

## 8. Os limites

- **Uma execução por combinação.** Cada célula das tabelas é um caso só. Os indícios da seção 6 são para escolher onde
  repetir, não para concluir. A variação entre as duas rodadas da construção (o Sonnet 4.5 no N1 aplicou o Strategy
  em Spring e não em Javalin) mostra o quanto uma execução só pode enganar.
- **O cenário 01 da manutenção foi avaliado no formato antigo** (só a classe, sem o comportamento pela API).
- **Não há um serviço plantado em Javalin.** A régua foi validada no plantado em Spring.
- **A correção é feita por um Claude**, corrigindo código de Claude. As provas que faltam: corrigir as mesmas
  execuções de novo, para ver se a IA chega às mesmas conclusões, e o Lucas corrigir três execuções às cegas.
- **O enunciado da construção tem lacunas** (o GOLD com FREESHIPPING quando o frete já é zero, o fuso do servidor,
  entre outras, no `NOTAS.md` do `construction-v6`). Nenhuma explica as falhas grandes.

## 9. Onde estão os dados

- Construção: `construction-v6/runs/batch-2/` (as execuções), `analysis/batch-2/judge/opus-5-5-high/` (a correção),
  `analysis/batch-2/pit.csv`, `analysis/batch-2/suite/`; a rodada em Spring em `runs/batch-1/` e `analysis/batch-1/`.
- Manutenção: `maintenance-v1/runs/` e `evaluation/evaluator/<pattern>/runs/<cenário>-<braço>/opus-5-5-high/`.
- As decisões e os achados, dia a dia: o `NOTAS.md` do `construction-v6`.
