# Relatório por modelo: a primeira rodada da construção e da manutenção

17 e 18 de outubro de 2026. Três modelos (Claude Haiku 4.5, Claude Sonnet 4.5 e Claude Sonnet 5.5), três braços
(N0, N1 e N2), uma execução por combinação, effort `medium`. A correção e a avaliação foram feitas por Claude Opus 5.5
(effort `high`), às cegas, com as réguas de cada pesquisa.

## Como ler

**As duas pesquisas.**
- **Construção** (`construction-v6`): o agente recebe uma pasta vazia e o enunciado de uma loja online (o serviço de
  pedidos, em Java com Spring Boot e PostgreSQL) e constrói tudo. São 9 execuções (3 modelos x 3 braços).
- **Manutenção** (`maintenance-v1`): o agente recebe um sistema pronto, sem nenhum padrão (as reservas de uma
  pousada, em Javalin), e um pedido de mudança na voz do dono. Os cenários ímpares pedem um padrão sem dizer o nome
  (01 Strategy, 03 Observer, 05 Factory Method, 07 Decorator, 09 Template Method); os pares são controles negativos,
  que parecem pedir o padrão e pedem só uma mudança simples. São 90 execuções (10 cenários x 3 braços x 3 modelos).

**Os braços.** N0: só o enunciado ou o pedido. N1: mais um `CLAUDE.md` curto com dois princípios (o próximo caso
num lugar só; nada de estrutura para o que ninguém pediu). N2: o mesmo `CLAUDE.md` com uma linha a mais, mandando
consultar a skill `gof-patterns`, que vai junto.

**As palavras.** Nos pontos que pedem um padrão: **acerto** (o padrão aplicado direito), **alternativa
equivalente** (outro desenho que resolve igual, só na manutenção), **malfeito** (a estrutura existe, mas algum caso
ou alguma regra escapa dela), **omissão** (a variação escrita com `if`, `switch` ou cópia). Nos controles negativos:
**contenção** (a resposta simples, como devia) e **exagero** (estrutura que ninguém pediu). Nas outras dimensões:
**segue**, **em parte**, **não segue**. A palavra é o resumo; a análise escrita de cada item é a correção.

**As fontes.**
- Construção: `construction-v6/runs/batch-1/` (as execuções), `analysis/batch-1/judge/opus-5-5-high/` (a correção,
  com a régua nova de 18/10 no Adapter e no comportamento), `analysis/batch-1/pit.csv` (o PIT) e
  `analysis/batch-1/suite/` (a suíte de referência, rodada à parte como conferência: 463 verificações).
- Manutenção: `maintenance-v1/runs/` e `evaluation/evaluator/<pattern>/runs/<cenário>-<braço>/opus-5-5-high/`. O
  cenário 01 foi avaliado antes (16 e 17/10), no formato antigo do avaliador, que dava só a classe, sem testar o
  comportamento pela API; os cenários 02 a 10, no formato atual.

**O cuidado principal.** Uma execução por combinação. Tudo o que está aqui é indício, não conclusão: uma diferença
entre dois braços pode ser acaso. Servem para ver se as hipóteses parecem se sustentar e onde vale gastar réplicas.

## Visão geral

### Construção

| modelo | braço | custo | tempo | Strategy (12 pontos) | State (P7) | Adapter (P6) | suíte (463) | comportamento (C1 a C10) | testes | PIT |
|---|---|---|---|---|---|---|---|---|---|---|
| Haiku 4.5 | N0 | US$ 0,55 | 10 min | 9 omissão (3 n/a) | omissão | malfeito | 189 | 3 segue, 4 em parte, 3 não segue | 10 | 9% |
| Haiku 4.5 | N1 | US$ 1,03 | 11 min | 8 omissão, 3 malfeito, 1 acerto | omissão | malfeito | não sobe | não sobe | 6 | 13% |
| Haiku 4.5 | N2 | US$ 0,89 | 12 min | 12 omissão | omissão | **acerto** | não sobe | não sobe | 1 | 7% |
| Sonnet 4.5 | N0 | US$ 2,94 | 20 min | 12 omissão | omissão | malfeito | 286 | 6 segue, 3 em parte, 1 não segue | 14 | 47% |
| Sonnet 4.5 | N1 | US$ 1,77 | 15 min | **10 acerto**, 2 malfeito | omissão | malfeito | 194 | 4 segue, 5 em parte, 1 não segue | 6 | 41% |
| Sonnet 4.5 | N2 | US$ 3,17 | 14 min | **10 acerto**, 2 malfeito | malfeito | malfeito | 436 | 4 segue, 5 em parte, 1 não segue | 1 | 0% |
| Sonnet 5.5 | N0 | US$ 1,25 | 10 min | 12 acerto | malfeito | malfeito | **463** | 10 segue | 20 | 78% |
| Sonnet 5.5 | N1 | US$ 1,09 | 10 min | 12 acerto | **acerto** | malfeito | **463** | 10 segue | 14 | 95% |
| Sonnet 5.5 | N2 | US$ 1,11 | 10 min | 12 acerto | malfeito | malfeito | **463** | 10 segue | 9 | 94% |

O Strategy tem 12 pontos: os quatro casos que pedem o padrão (entrega, cupom, forma de pagamento, nível do clube),
cada um em duas perguntas (onde mora o código de cada caso e como o caso é escolhido), e os quatro remendos (regras
cruzadas que tentam a um `if` fora do lugar). Os controles negativos (a região como dado, os objetos sem regra, as
entregas sem transportadora, a situação do aviso e o item E, estrutura sem necessidade) deram contenção em quase
todas as execuções e não estão na tabela; as exceções são o item E do Factory no Haiku N1 (`exagero`) e a forma da
região no Sonnet 4.5 N0 (`não se aplica`). A suíte é a soma das verificações que
passaram (catálogo, resumo, ciclo, reinício e concorrência).

### Manutenção

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

O comportamento conta os cenários 02 a 10 (9 por braço): o cenário 01 foi avaliado no formato antigo, sem ele.
Nenhuma execução criou estrutura fora do que o pedido tocava (o "exagero colateral" deu contenção nas 81 execuções
dos cenários 02 a 10).

O cenário a cenário, para os que pedem o padrão:

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

## Claude Haiku 4.5

**Em uma frase:** barato e rápido, mas não chega a construir um serviço que funcione e quase não aplica padrões
sozinho; a skill, quando ele a usa, é o que mais o ajuda.

### Na construção

- **O serviço só sobe no N0, e mesmo ali com muitos erros.** No N1 e no N2 o serviço compila, passa no `mvn verify`
  e não sobe, por erros de conhecimento da biblioteca: no N1, a coluna `installments` mapeada duas vezes na entidade
  `Order` (o mesmo erro do piloto de 17/10); no N2, o dialeto `PostgreSQL12Dialect`, que o Hibernate 7 não tem mais.
  O `mvn verify` passa porque os testes dele não sobem o Spring.
- **No N0, a suíte passou em 189 de 463.** A revisão à mão achou regras erradas no cálculo: o FREESHIPPING é sempre
  recusado; o GOLD deixa de pagar frete também no motoboy; o WELCOME10 não confere se é a primeira compra; o desconto
  do Pix vale mesmo com cupom; não há a recusa do boleto acima de R$ 1.000; a TransLog fora do ar vira "entrega
  indisponível". No ciclo do pedido: o prazo de 48 horas conta horas inteiras (48h e 1 segundo ainda passa), o
  cliente só passa a existir no pagamento, e o estoque não tem trava (de 2 jaquetas, vendeu 6). E o banco está em
  `ddl-auto: create-drop`: cada reinício apaga tudo.
- **O desenho é a omissão clássica nos três braços.** Um serviço de cálculo com um `if` por entrega, cupom, pagamento
  e nível. No N1 aparece um começo de padrão (os cupons num enum com o cálculo em cada constante), mas o nome
  FREESHIPPING volta a aparecer no cálculo do frete, o que dá desconto em dobro (por isso o `malfeito` no cupom).
- **O Adapter é o único ponto em que ele vai bem**: no N2, o cliente da TransLog guarda toda a língua dela (gramas,
  centavos, horas, STD e EXP) num lugar só. Mas devolve `null` tanto para "CEP não atendido" quanto para "fora do
  ar", e o serviço não consegue dar o erro certo.
- **Os testes caem com o harness:** 10, 6 e 1. O PIT fica entre 7% e 13%.
- **No N2 ele não abriu a skill**, apesar da linha do `CLAUDE.md`.

### Na manutenção

- **Sem a skill, não aplica padrão nenhum:** omissão nos cinco cenários que pedem padrão, no N0 e no N1. O típico
  é o cenário 05 no N0: em vez de uma estrutura que escolhe qual documento criar, dois `if/else` dentro da única
  classe de documento, e os pontos de uso continuam fazendo `new` dela. No 09, os relatórios novos são cópias
  inteiras de um antigo.
- **No N2 aparecem os únicos acertos dele: Strategy (01) e Factory Method (05).** Ele abriu a skill em só 2 das 10
  execuções do N2 (os cenários 01 e 03). O acerto do 01 veio com a skill aberta; o do 05, sem ela, só com o
  `CLAUDE.md` e a linha a mais.
- **Nunca exagera:** contenção nos cinco controles, nos três braços.
- **O comportamento fica "em parte" em 8 das 27 execuções** (os cenários 05 e 07 nos três braços, o 09 no N0 e o 08
  no N1): a mudança funciona no caminho principal e erra uma regra do pedido (no 09, a taxa de turismo cobrada onde
  não devia).
- **É o mais barato por execução:** US$ 7,36 nas 27 execuções dos cenários 02 a 10, uns 4 minutos cada.

## Claude Sonnet 4.5

**Em uma frase:** o modelo em que o harness mais muda o desenho, e também o que mostra que um desenho melhor não é,
sozinho, um serviço melhor.

### Na construção

- **O harness muda o desenho dele mais que o de qualquer outro.** No N0, omissão em tudo: um calculador de 453
  linhas com um `switch` por entrega, cupom, pagamento, região e nível. No N1, o Strategy de verdade nos quatro
  pontos: uma interface para cada um, uma classe por caso e um mapa montado pelo Spring que escolhe a classe; uma
  entrega nova é uma classe nova. No N2, o mesmo Strategy e mais um State de verdade: cada situação do pedido é uma
  classe que decide o que pode e o que não pode (o único State do lote feito com classes).
- **Mas o serviço não melhora junto.** A suíte: 286 no N0, 194 no N1, 436 no N2. Os erros são de borda, não de
  regra de negócio: um valor inválido no JSON (nível, região, cupom, entrega) devolve `400 Bad Request` em vez de
  `422` com o código da tabela (N0 e N1); a TransLog fora do ar vira "entrega indisponível", porque a classe dela
  engole a exceção (N1); o pagamento e o envio com campo vazio são aceitos.
- **A concorrência é o ponto fraco nos três braços**, e a régua nova do C7 e do C8 pegou. No N0, o mesmo aviso de
  pagamento chegando cinco vezes juntas baixa o estoque cinco vezes e dá o crédito cinco vezes; a correção achou mais
  dois defeitos que a suíte não pega: num pedido de vários itens que perde a disputa, a baixa parcial do estoque fica
  gravada, e pedidos com os itens em ordem diferente travam o PostgreSQL (deadlock) e ficam presos aguardando
  pagamento. No N1, quem perde a última peça recebe erro 500 e fica aguardando pagamento. No N2, de 2 jaquetas vendeu
  6 (a trava existe, mas a conferência do estoque é feita antes dela), e o `createdAt` guardado sem fuso faz um
  pagamento com exatamente 48 horas ser recusado.
- **Os testes desabam com o harness:** 14, 6 e 1. No N2 o único teste é um `contextLoads` vazio, e o PIT dá 0%.
- **Abriu a skill no N2.** Foi o mais caro do lote (US$ 2,94 e US$ 3,17 no N0 e no N2) e o mais lento (até 20 min).

### Na manutenção

- **Sem a skill, nenhum acerto:** no N0 e no N1, omissão ou malfeito nos cinco cenários. O Observer (03) sai malfeito
  nos três braços; no Decorator (07), o N0 põe as três regras novas dentro do próprio envio de e-mail, uma depois da
  outra.
- **No N2, acerta o Strategy (01) e o Factory Method (05).** Abriu a skill em 8 das 10 execuções do N2.
- **Mas no N2 travou duas vezes:** nos cenários 02 e 09, abriu a skill, explicou o padrão e não mudou o código (0
  arquivos alterados). O pedido não foi feito, e o comportamento saiu "não segue". No 02, um controle negativo, a
  classe saiu "contenção" porque não há estrutura nenhuma, mas é contenção por não ter feito nada.
- **Nunca exagera.** O comportamento fica "em parte" em 7 das 27 execuções (os cenários 05 e 07 nos três braços e o
  09 no N0, quase os mesmos do Haiku).
- **É o mais caro por execução:** US$ 18,22 nas 27 execuções dos cenários 02 a 10, uns 5 minutos cada.

## Claude Sonnet 5.5

**Em uma frase:** constrói um serviço que passa em tudo nos três braços e aplica o Strategy sozinho; o harness
mexe no que sobra (o State e os padrões menos óbvios da manutenção), e o N1 é o braço em que ele vai melhor.

### Na construção

- **Passa em todas as 463 verificações da suíte nos três braços**, e a correção do comportamento deu "segue" nos 10
  itens nos três, inclusive nos testes de simultaneidade da régua nova. É o único modelo cujo serviço aguenta as duas
  máquinas, o aviso repetido ao mesmo tempo e o reinício.
- **O Strategy está certo já no N0**, nos 12 pontos: uma interface por ponto, uma classe por caso, um `Money` com o
  arredondamento num lugar só.
- **O State é o que muda entre os braços.** No N1, um enum em que cada situação diz o que permite (o próprio
  comentário avisa que "uma situação nova entra só aqui"): acerto. No N0 e no N2, uma tabela de transições com
  `switch`, e o cancelamento ainda pergunta no serviço "estava pago?": malfeito.
- **O Adapter fica em "malfeito" nos três:** há um cliente da TransLog atrás de uma interface, com o resultado tipado
  (atendeu, não atende o CEP, fora do ar), mas a conversão dos quilos em gramas (e, no N0 e no N1, das horas em dias)
  ficou na estratégia de entrega, fora dele.
- **Os testes são de integração de verdade** (H2 e uma TransLog falsa num servidor HTTP local), com o PIT entre 78% e
  95%. Mas também caem com o harness: 20, 14 e 9.
- **Abriu a skill no N2.** Custou entre US$ 1,09 e US$ 1,25 e uns 10 minutos, com 16 a 19 turnos (o Haiku e o
  Sonnet 4.5 usaram de 57 a 105).

### Na manutenção

- **É o único que acerta alguma coisa no N0** (o Strategy, cenário 01) e o que mais acerta com o harness: no N1, 3
  acertos (01, 03 e 05) e 1 alternativa equivalente (07); no N2, 2 acertos (01 e 05) e 1 alternativa (07).
- **A alternativa equivalente no Decorator (07)** é um bom exemplo de como a avaliação lê o desenho: em vez de
  embrulhos, uma lista de regras, cada uma uma peça própria, que o envio aplica em sequência sem saber qual é qual. As
  instruções aceitam esse desenho de antemão.
- **O Template Method (09) é o padrão que ninguém acerta.** No N1 e no N2 ele criou a classe abstrata, mas o "passo"
  dela é o relatório inteiro: cada relatório ainda escreve à mão a seleção, o cabeçalho, as linhas e o total.
- **O único exagero da manutenção é dele, no N2, cenário 02:** em vez de pôr as datas da Páscoa como dado, uma
  interface `Season` e uma classe com uma lista de temporadas como objetos, o que a frase-isca do pedido convida a
  fazer.
- **O comportamento deu "segue" nas 27 execuções dos cenários 02 a 10.** Abriu a skill nas 10 do N2. É o mais rápido e o mais barato da
  manutenção: US$ 5,68 nas 27 execuções, menos de 2 minutos cada.

## O que aparece entre os modelos e os braços (indícios)

1. **O harness muda o desenho de quem não acerta sozinho.** O efeito maior é no Sonnet 4.5 da construção (de omissão
   em tudo para Strategy nos quatro pontos). Na manutenção, o N2 é o único braço em que o Haiku e o Sonnet 4.5
   acertam algum padrão, e o N1 é o melhor braço do Sonnet 5.5. O modelo que já sabe (o Sonnet 5.5 no Strategy)
   não muda.
2. **No Haiku, a linha do `CLAUDE.md` pesa mais que a skill.** Ele abriu a skill em 2 das 10 execuções do N2 da
   manutenção e em nenhuma da construção; dos dois acertos dele no N2, um veio sem a skill aberta.
3. **Desenho melhor não é serviço melhor.** O Sonnet 4.5 no N1 da construção tem um desenho bem melhor que o do N0 e
   passa em menos verificações da suíte (194 contra 286).
4. **Os testes diminuem do N0 ao N2 nos três modelos da construção** (Haiku 10, 6, 1; Sonnet 4.5 14, 6, 1; Sonnet 5.5
   20, 14, 9). É o indício mais inesperado: a atenção ao desenho parece tirar espaço dos testes. Na manutenção não
   se mediu.
5. **Quase ninguém exagera.** O medo de que o harness levasse ao excesso de estrutura quase não aparece: um exagero
   em 45 controles da manutenção (Sonnet 5.5, N2) e um na construção (o item E do Factory no Haiku N1).
6. **A skill pode travar o trabalho.** As duas execuções em que o Sonnet 4.5 não mudou nada são do N2, logo depois de
   ler a skill.
7. **Custo e qualidade não andam juntos.** O Sonnet 5.5 é o melhor e o mais barato da manutenção; o Sonnet 4.5 é o
   mais caro nas duas pesquisas.

## Os limites desta rodada

- **Uma execução por combinação.** Cada célula das tabelas é um caso só. Os indícios acima são para escolher onde
  repetir, não para concluir.
- **O cenário 01 da manutenção foi avaliado no formato antigo** (só a classe, sem o comportamento pela API).
- **A correção é feita por um Claude**, corrigindo código de Claude. Está declarado no protocolo; a concordância com
  a revisão à mão e com a suíte (registrada no `NOTAS.md` do `construction-v6`) foi alta no desenho, e a régua nova do
  C7 e do C8 fechou a diferença que havia no comportamento.
- **As provas da correção ainda não foram feitas:** corrigir as mesmas execuções de novo, para ver se a IA chega às
  mesmas conclusões, e o Lucas corrigir três execuções às cegas.
- **O enunciado da construção tem lacunas** (o GOLD com FREESHIPPING quando o frete já é zero, o fuso do servidor,
  entre outras, no `NOTAS.md`). Nenhuma explica as falhas grandes.
