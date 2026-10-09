# Objetivo

> **RASCUNHO**, reescrito em 09/10/2026 para o V4 redesenhado (a avaliação automática
> na base, o Semgrep, 5 modelos × 5 réplicas). As decisões são do Lucas; o orientador
> vê o resultado analisado. Este arquivo só vale como pré-registro depois de
> commitado **antes** da primeira execução do V4; a data do commit é a prova.
>
> Revisado pelo Lucas em 09/10. Os **5 modelos** saíram do mapa em 09/10, pela regra do
> `DECISOES.md` (§2), e estão no §2. **Falta só o congelamento:** o commit, e o hash no
> README.

O [README](README.md) diz **como** o experimento roda. Este arquivo diz **para
quê**: a pergunta, o que conta como resposta, e o que já foi testado. O **porquê**
de cada escolha está no [`DECISOES.md`](DECISOES.md).

---

## 1. A pergunta

> Um harness (o conjunto de orientações que acompanha o agente: `CLAUDE.md`,
> skills e o que mais vier) influencia, **para o bem ou para o mal**, a forma como
> um agente de código aplica padrões de projeto ao construir um sistema do zero?
> Essa influência muda conforme o modelo, o padrão pedido e o que compõe o harness?

É a célula **Construir software × Design de baixo nível** da matriz combinada na
orientação de 12/09: padrões de projeto, com e sem harness, em mais de um modelo.
Manter software, arquitetura, testes e banco de dados ficam fora deste recorte.

O V4 testa **um padrão, o Strategy**, e a escada de harness de N0 a N3. Padrões e
componentes de harness novos entram depois, um de cada vez, sobre o mesmo corpo.

## 2. O que fica fixo, e o que varia

| | |
|---|---|
| **o que varia dentro de um lote** | só o **nível** do harness: N0 (braço `CONTROL`, workspace vazio), N1, N2 e N3 (braço `HARNESS`, com a pasta do nível em `experiment/harnesses/`) |
| **o harness** | **fixo dentro de um nível**. Não é ajustado ao enunciado. Cada nível é uma pasta de `experiment/harnesses/`, identificada pelo hash da árvore, gravado em `environment.harness_hash` no `meta.json` |
| **o enunciado** | um só, **igual em todos os níveis** (`experiment/prompt/prompt.md`, `8c70bb30…`). Escrito como o dono de uma loja que estuda o básico de programação por curiosidade: as regras do negócio no texto e, num anexo, o contrato da API em linguagem simples (o endereço e tabelas com os nomes dos campos, sem verbo, sem números de status e sem bloco de JSON). Sem palavra de arquitetura |
| **os modelos** | **5**: **Haiku 4.5** (`claude-haiku-4-5`), **Sonnet 4.5** (`claude-sonnet-4-5`), **Opus 4.6** (`claude-opus-4-6`), **Sonnet 5** (`claude-sonnet-5`) e **Opus 5** (`claude-opus-5`). Escolhidos por um mapa no N0 (`TESTE-MAPA-01`, 09/10), pela regra do `DECISOES.md` (§2), escrita antes de rodar: o Haiku 4.5 (o mais fraco) e o Opus 5 (o topo) entram sempre; dos outros cinco candidatos (Haiku 5.5, Sonnet 4.5, Sonnet 4.6, Sonnet 5 e Opus 4.6), o de menor nota A, o do meio e o de maior. No mapa, três ficaram fora do teto (Haiku 4.5, Sonnet 4.5 e Opus 4.6) e dois no teto (Sonnet 5 e Opus 5). O modelo é fator de bloco, chamado sempre pelo ID completo, que o `run-levels.sh` lê de `experiment/desenho-v4.json` |
| **a unidade de análise** | o **par simultâneo**: dois níveis vizinhos do mesmo modelo, que rodaram no mesmo instante, dentro do mesmo quarteto |

O V4 é **um lote de 4 níveis × 5 modelos × 5 réplicas = 100 execuções**, em **25
quartetos**: os quatro níveis de um modelo rodam juntos (`infra/scripts/run-levels.sh`),
um quarteto de cada vez, e cada rodada tem uma réplica de cada modelo, em ordem
sorteada (`experiment/ordem-v4.csv`, semente 20261009, `infra/scripts/draw-order.mjs`);
as rodadas vão em sequência. Um quarteto interrompido (por cota ou servidor) é refeito inteiro. Cada
comparação entre dois níveis vizinhos tem **25 pares** (5 modelos × 5 réplicas).

**O exploratório.** Depois dos 25 quartetos acima, que são o estudo **confirmatório**,
roda, **obrigatoriamente** (decisão do Lucas, 09/10: o trabalho só fecha com os dois
lotes), um segundo lote, `V4-EXPLOR`, com os outros modelos disponíveis no Claude:
**Haiku 5.5, Sonnet 4.6, Opus 4.7, Opus 4.8 e Sonnet 5.5** (decisão de 09/10, depois
do mapa). Ficam fora o Fable e o Opus 5.5, que exige um Claude Code mais novo que o da
bancada (2.1.280 ou mais; a imagem tem o 2.1.269). O desenho é o mesmo: 4 níveis × 5
modelos × 5 réplicas = 100 execuções, a mesma imagem, o mesmo enunciado, as mesmas
medidas automáticas (a suíte, o Semgrep, o SonarQube e o CK, e a nota), em ordem
sorteada (`experiment/ordem-v4-exploratorio.csv`, semente 20261010), com o desenho em
`experiment/desenho-v4-exploratorio.json`. **Nenhuma hipótese é testada nele:** as
regras do §4 valem só para o confirmatório, e o exploratório entra no texto como
tabelas e gráficos marcados como exploratórios (por exemplo, as gerações Opus 4.6 →
4.7 → 4.8 → 5 e Sonnet 4.5 → 4.6 → 5 → 5.5). Não tem leitura humana: o Semgrep dele
não é conferido, e a conferência dos 20 pacotes (§4.10) só vale para o confirmatório.
*Por quê:* mostra o panorama dos modelos sem mudar o que foi pré-registrado. Os 5
do confirmatório saíram de uma regra fixada antes do mapa, e trocar o conjunto depois
de ver o mapa seria um desvio. No N0 do mapa (uma execução cada), o Haiku 5.5 (nota A
70) e o Opus 4.7 (60) ficaram fora do teto, e o Sonnet 4.6, o Opus 4.8 e o Sonnet 5.5
no teto (100).

**O TCC_V3 é a bancada de testes.** Os lotes daqui (o `EXT`, os `TESTE-*`) serviram
para validar a bancada, o enunciado e os instrumentos, e não entram na análise.

## 3. O que conta como influência

Um harness pode melhorar um eixo e piorar outro, por isso a influência é lida em
quatro, todos medidos **sem IA** em todas as execuções:

| eixo | a pergunta | instrumento | em quantas |
|---|---|---|---|
| **desenho** | aplicou o padrão onde o enunciado pede, e deixou de aplicar onde seria exagero? | o **Semgrep**, com regras próprias sobre os casos do gabarito (`evaluation/tools/semgrep/`), **conferido** pela leitura humana às cegas (§4.10) | 100 (o Semgrep); 20 (a conferência) |
| **correção** | o código calcula e recusa o que o enunciado pede? | a suíte de aceitação, via HTTP: 12 casos de conta e 9 de recusa (`infra/scripts/acceptance.sh`) | 100 |
| **qualidade** | o código ficou mais simples ou mais complicado? | SonarQube e CK (`evaluation/tools/metrics.sh`) | 100 |
| **custo e processo** | quanto gastou, e como trabalhou, para chegar lá? | `meta.json`: `tokens.input_total`, `tokens.output`, `timing.duration_api_ms`, `outcome.turns`, `outcome.tool_calls_by_name` | 100 |

**Nenhum modelo de IA avalia nada.** O Claude não lê os pacotes: os agentes do
experimento são Claude, e um avaliador da mesma família é o risco que o documento do
orientador de 12/09 aponta. A leitura humana existe para **conferir** o Semgrep.

**Influência negativa** conta tanto quanto a positiva. Em desenho, ela aparece como
exagero (aplicar o padrão onde o enunciado não pede) ou código maior sem ganho. Em
correção, como estrutura comprada com erro. Em custo, como gasto a mais sem ganho em
outro eixo.

Por isso o enunciado tem os dois tipos de ponto (o gabarito está em
`evaluation/strategy/gabarito.md`):

- **pontos positivos** (P1 entrega, P2 cupom, P3 pagamento, P4 clube): o enunciado
  descreve uma variação que o padrão resolve. O **P4** é o foco: o `OURO` mexe no
  frete, que é calculado em outro lugar, e é onde separar os casos mais faz diferença;
- **controle negativo** (P5, o seguro por região): só a porcentagem muda, e aplicar o
  padrão ali é o erro.

---

## 4. As hipóteses

### Como ler esta seção

As hipóteses estão agrupadas por **eixo**, e cada uma tem um nome no formato
*Eixo: o que ela afirma*.

| eixo | a pergunta |
|---|---|
| **Desenho** | o padrão foi aplicado onde o enunciado pede? |
| **Exagero** | o padrão foi aplicado onde o enunciado **não** pede? |
| **Correção** | o código calcula e recusa o que o enunciado pede? |
| **Qualidade** | o código ficou mais simples? |
| **Custo** | quanto o agente gastou, e como trabalhou? |
| **Modelo** | o efeito do harness muda de um modelo para outro? |
| **Skills**, **Processo** | o que cada degrau de cima (N2, N3) acrescenta |

**As principais** são as que respondem à pergunta do §1. Em todas as tabelas deste
arquivo, **★ quer dizer "principal"**. No texto, cada uma é chamada pela forma curta:

| forma curta | nome | em uma frase | instrumento |
|---|---|---|---|
| **a hipótese do desenho** | Desenho: isola cada caso no P4 | com o harness, o agente separa melhor os níveis do clube | Semgrep, conferido |
| **a hipótese do exagero** | Exagero: aplica onde não pede | o harness muda o quanto o agente exagera no P5 | Semgrep, conferido |
| **a hipótese da correção** | Correção: suíte inteira | com o harness, o código não fica mais errado | a suíte |
| **a hipótese da qualidade** | Qualidade: menos complexidade | com o harness, a complexidade cognitiva cai | SonarQube |
| **a hipótese do modelo** | Modelo: efeito maior no mais fraco | o efeito do harness no desenho é maior no modelo mais fraco | Semgrep, conferido |

As demais são **secundárias**: são medidas e publicadas, mas não sustentam conclusão
sozinhas. O **custo** é todo secundário, por orientação de 12/09: depende de cache e de
outras variáveis fora do experimento. A distinção importa: com muitas hipóteses,
alguma vai "dar certo" por acaso.

Se a conferência (§4.10) reprovar uma pergunta do Semgrep, as hipóteses que dependem
dela passam a **descritivas**, lidas só na amostra conferida.

Cada hipótese tem também uma **direção** (declarada, ou "sem direção" quando o
harness pode puxar para os dois lados).

**Palavras usadas nas regras:**

- **nível**: N0 a N3, a escada de harness (§5). O N0 é o braço `CONTROL`; o N1, o N2 e
  o N3, o braço `HARNESS`, como fica gravado no `meta.json`.
- **quarteto**: os quatro níveis de um mesmo modelo, rodados no mesmo instante. O V4
  tem 25: 5 modelos × 5 réplicas.
- **par**: dois níveis vizinhos do mesmo quarteto (N1 × N0, N2 × N1, N3 × N2). Cada
  comparação tem 25 pares.
- **melhor, igual, pior**: em cada par, como o nível de cima ficou em relação ao de
  baixo, no que a hipótese mede.
- **teto**: um modelo cujo nível de baixo já acerta tudo o que a hipótese mede, nas 5
  réplicas. Ali o nível de cima não tem como melhorar: só empatar ou piorar.

**Qual comparação cada hipótese faz.** As principais e as do §4.2 ao §4.6 comparam
**N1 com N0**: o `CLAUDE.md` contra nenhum harness, o contraste mais limpo. As do §4.8
comparam os degraus de cima (N2 × N1 e N3 × N2). O **teste de Page** (abaixo) olha a
escada inteira, de N0 a N3.

### 4.1 Como uma hipótese é lida

Com 5 réplicas, cada modelo tem 5 pares por comparação: mesmo com os 5 na mesma
direção, o menor p de um teste de sinal é 0,0625. Somando os 25 pares, o teste de
sinal junta modelos que se comportam diferente. Por isso a leitura é **por pares,
descritiva, e decidida aqui antes dos dados**; o teste de sinal sobre os 25 pares
pode ser reportado ao lado, como informação, não como critério.

| tipo de hipótese | apoiada quando | contrariada quando |
|---|---|---|
| **direcional** | na maioria dos modelos **fora do teto** há mais pares melhores que piores, e em nenhum modelo (no teto ou não) há mais piores que melhores | na maioria dos modelos fora do teto há mais pares piores que melhores |
| **não-inferioridade** ("não piora") | o **saldo** (pares piores − pares melhores) é no máximo 3 | o saldo é 4 ou mais |
| **sem direção** ("altera"), medida contínua (tokens, tempo) | pelo menos **18 dos 25** pares vão na mesma direção, qualquer que seja | o lado maior tem no máximo 15 pares (eles se dividem); com 16 ou 17, inconclusiva |
| **sem direção** ("altera"), medida sim/não (exagerou ou não) | pelo menos 5 pares não empatados, e o lado maior no mínimo da tabela abaixo | no máximo 2 pares não empatados, ou eles se dividem (o lado menor tem pelo menos um terço deles); o resto é inconclusivo |

Fora desses casos, a hipótese é **inconclusiva**, e isso é resultado, não falha. A
tabela de pares é sempre publicada inteira, qualquer que seja a leitura. "Igual"
nunca conta como direção.

**Por que 18 de 25.** Se o harness não mudasse nada, cada par seria cara ou coroa. A
chance de pelo menos X dos 25 caírem do mesmo lado só por sorte, somando os dois lados:

| pares do mesmo lado | chance por sorte |
|---|---|
| 19 ou mais | 1,5% |
| **18 ou mais** | **4,3%** |
| 17 ou mais | 10,8% |
| 16 ou mais | 23% |

A convenção é aceitar um resultado quando a chance de ele ser só sorte fica abaixo de
5%; 18 é o menor número que passa (com 15 pares, era 12, a 3,5%). Com 16 ou 17 do
mesmo lado há indício sem prova, e a hipótese é inconclusiva; só com os pares
divididos (15 a 10 ou mais equilibrado) ela é contrariada.

**A medida sim/não.** Quando a medida é exagerou ou não, a maioria dos pares tende a
empatar (nenhum dos dois exagerou). Contam então só os pares em que os níveis diferem,
com a mesma conta da moeda, e o limite de **6,3%** já usado com 15 pares (o rigoroso
seria 5%, mas o exagero é raro, e a hipótese principal ficaria inconclusiva quase
sempre, por construção):

| pares não empatados | o lado maior precisa ter pelo menos | chance por sorte |
|---|---|---|
| 5 | 5 (5 a 0) | 6,3% |
| 6, 7, 8 | todos | 3,1%, 1,6%, 0,8% |
| 9 | 8 (8 a 1) | 3,9% |
| 10 | 9 | 2,1% |
| 11 | 10 | 1,2% |
| 12 | 10 | 3,9% |
| 13 | 11 | 2,2% |
| 14 | 11 | 5,7% |
| 15 | 12 | 3,5% |

Acima de 15, o lado maior precisa de uma chance por sorte de no máximo 6,3% no teste
de sinal de dois lados, calculada da mesma forma.

**Por que o saldo até 3 na não-inferioridade.** Mesmo sem efeito nenhum do harness, o
modelo não acerta sempre igual, e alguns pares saem diferentes por acaso, para os dois
lados. O saldo desconta os melhores. Com 15 pares, até 2 não se separava do acaso;
com 25, a mesma proporção (cerca de 13% dos pares) dá 3. De 4 em diante, o harness
atrapalha mais do que ajuda.

**Por que o teto sai da contagem.** Um modelo no teto não tem como mostrar melhora;
contá-lo tornaria uma hipótese direcional impossível de apoiar sempre que vários
modelos estivessem no teto, mesmo com efeito claro nos outros. Por isso, nas hipóteses
direcionais, os modelos no teto (na medida da própria hipótese) saem da contagem de
"melhor" e são lidos por *Modelo: no teto, não piora*. Se todos estiverem no teto, a
hipótese é registrada como "sem espaço para efeito". A regra do teto não se aplica a
medidas contínuas (complexidade, tokens).

**A hipótese do modelo compara modelos, não pares.** É apoiada quando o modelo com
menos acertos no N0 da hipótese do desenho (média das 5 réplicas) tem o maior saldo de
pares nela (melhores − piores), sem empate; contrariada quando outro modelo tem saldo
maior; com empate no maior saldo, inconclusiva.

**Tamanho do efeito.** Em cada comparação, e em cada modelo, o saldo de pares dividido
pelo número de pares vai de −1 a +1, e é a versão pareada do *Cliff's delta*. É
publicado ao lado de cada hipótese, com a leitura usual (abaixo de 0,15, desprezível;
perto de 0,33, médio; acima de 0,47, grande), sem mudar o veredito.

**Tendência, como informação.** Para cada medida, a pergunta "a qualidade sobe de N0 a
N3?" é respondida pelo **teste de Page**, a versão pareada do Jonckheere-Terpstra
sugerido pelo orientador: cada quarteto é um bloco, e a ordem testada é
N0 < N1 < N2 < N3. Ele é publicado com o p, por modelo e no conjunto, mas **não decide
nenhuma hipótese**.

**Exemplo, com números inventados.** Na hipótese do desenho (N1 × N0), o Haiku 4.5 tem
4 pares melhores e 1 igual; o Opus 4.6, 3 melhores e 2 iguais; os outros três modelos
estão no teto, com 5 pares iguais cada. Os dois modelos fora do teto têm mais melhores
que piores, e nenhum modelo piorou. **A hipótese do desenho é apoiada.** A do modelo
também: o Haiku 4.5, o mais fraco no N0, tem o maior saldo (+4).

### 4.2 Desenho

Nesta tabela e nas seguintes, **★ marca as hipóteses principais**; as sem ★ são
secundárias. Cada uma liga uma regra do `CLAUDE.md` (o N1) a algo observável no código.
Um ponto tem **acerto** quando o Semgrep responde `isolado` e, na seleção, `consulta` ou
`condicional-unica` (a ficha, `evaluation/regua.md` §4).

| hipótese | o que afirma | instrumento | direção |
|---|---|---|---|
| **Desenho: isola cada caso no P4** ★ | a `HARNESS` tem mais pacotes com acerto no P4 (o clube) | Semgrep, conferido | declarada: mais |
| **Desenho: isola cada caso no P1 a P3** | o mesmo, na entrega, no cupom e no pagamento | Semgrep, **sem conferência humana** | declarada: mais |
| **Desenho: não repete o comum** | a `HARNESS` repete menos o que é comum aos casos | `sonar_duplicated_lines_density` | declarada: menos |
| **Desenho: réplicas mais parecidas** | as 5 réplicas de um modelo dão respostas mais parecidas entre si no N1 que no N0 (as do Semgrep do P1 ao P5) | Semgrep | declarada: mais parecidas |
| ~~Desenho: comporta o caso exigente~~ | fora do V4: a pergunta saiu da régua (09/10); é quase o "espalhado" do P4 | — | — |
| ~~Desenho: caso novo com pouca edição~~ | fora do V4: a manutenção ficou para trabalho futuro (09/10) | — | — |

### 4.3 Exagero

| hipótese | o que afirma | instrumento | direção |
|---|---|---|---|
| **Exagero: aplica onde não pede** ★ | no P5, o harness **altera** a taxa de exagero (`proporcao = estrutura`) | Semgrep, conferido | sem direção |
| **Exagero: mais arquivos** | a `HARNESS` produz mais tipos para o mesmo enunciado | `sonar_classes` | declarada: mais |
| **Exagero: estrutura especulativa** | o harness altera o acoplamento e a coesão médios, que interface e fábrica sem necessidade mudam | `ck_cbo_media`, `ck_lcom_media` | sem direção |
| **Exagero: padrão diferente do pedido** | quando o enunciado pede um padrão, a `HARNESS` aplica mais vezes outro | — | só com um 2º padrão |

A hipótese do exagero é sem direção de propósito, porque o harness puxa para os dois
lados: a regra 4 ("não crie estrutura para variação que você imagina") prevê **menos**
exagero, e a ênfase das regras 1 a 3 em isolar o que varia pode induzir **mais**.

### 4.4 Correção

A medida é a dos **pontos da suíte**: cada caso de conta vale 1 ou 0; cada caso de
recusa vale 1 com o código e o status de recusa certos, 0,5 com o código certo e status
de sucesso, e 0 no resto (máximo 21). Quem não compila ou não sobe tem 0.

| hipótese | o que afirma | direção |
|---|---|---|
| **Correção: suíte inteira** ★ | a `HARNESS` não faz menos pontos na suíte que a `CONTROL` | não-inferioridade |
| **Correção: contas** | o mesmo, só nos 12 casos de conta | não-inferioridade |
| **Correção: recusas** | o mesmo, só nos 9 casos de recusa | não-inferioridade |
| **Correção: não quebra o build** | a `HARNESS` não tem mais builds quebrados nem serviços que não sobem | não-inferioridade |
| **Correção: casos de borda** | nos casos de fronteira, de precedência entre erros e de colisão (OURO com FRETEGRATIS), a `HARNESS` não erra mais | não-inferioridade |

### 4.5 Custo

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Custo: tokens de entrada** | o harness altera o consumo de entrada (`tokens.input_total`) | sem direção | agora |
| **Custo: tempo de API** | o harness altera o tempo de API (`timing.duration_api_ms`) | sem direção | agora |
| **Custo: sinal muda por modelo** | a direção do efeito no custo muda conforme o modelo | declarada: muda | agora |
| **Custo: processo de trabalho** | o harness altera o processo: turnos, e a proporção entre ler, escrever e executar (`outcome.tool_calls_by_name`) | sem direção | agora |

O custo é sem direção porque o piloto mostrou o sinal trocando entre modelos: o
harness fez gastar mais em um e menos em outro.

**Cache.** O cache de prompt fica nos servidores da Anthropic: requisições que
começam com o mesmo texto (o prompt de sistema do Claude Code, as ferramentas, o
enunciado) reaproveitam o processamento, e o isolamento das execuções não tem como
impedir isso. Ele **não é desligado**, porque um usuário real usa cache. Os tokens
são registrados separados no `meta.json` (`tokens.input`, `tokens.cache_read`,
`tokens.cache_write`), e `tokens.input_total` é a soma dos três. O cache não muda o
código gerado, só o tempo e os tokens; por isso o custo é secundário e não sustenta
conclusão.

### 4.6 Modelo

| hipótese | o que afirma | direção |
|---|---|---|
| **Modelo: efeito maior no mais fraco** ★ | o efeito na hipótese do desenho é maior no modelo que menos acerta o P4 sem harness | declarada: maior |
| **Modelo: no teto, não piora** | nos modelos que acertam tudo sem harness (teto), o harness não piora o desenho nem a correção | não-inferioridade |
| **Modelo: quem melhora não é quem exagera** | a influência negativa (o exagero) aparece em modelos diferentes dos que têm a influência positiva | sem direção |

O ensaio já sugere a hipótese do modelo e *Modelo: no teto, não piora*: no EXT, o
Opus e o Sonnet acertaram o P4 e a suíte nos dois braços, e só o Haiku variou. No
teste do V4 (`TESTE-NIVEIS-01`), os quatro níveis do Opus fizeram 21 de 21. Onde o
modelo já acerta sozinho, não há espaço para efeito positivo, e a pergunta passa a ser
se o harness atrapalha. Por isso a escada de 5 modelos tem modelos no teto e fora dele.

### 4.7 Entre padrões

Comparação **observacional**: cada padrão é um lote próprio, sem par entre lotes.
**Nenhuma destas roda no V4**, que tem um padrão só; ficam escritas para quando
um segundo padrão entrar (o State está pronto para isso em `history/state/`).

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Entre padrões: mais efeito onde a CONTROL erra mais** | o efeito positivo é maior nos padrões em que a `CONTROL` acerta menos | declarada: maior | 2º padrão |
| **Entre padrões: só na família de variação** | o harness v1, que fala de variação, tem efeito positivo em padrões de variação (State, Template Method, Chain) e efeito nulo ou negativo em padrões fora dessa família | declarada | 2º padrão |
| **Entre padrões: exagera nos parecidos** | a taxa de exagero (a da hipótese do exagero) é maior nos padrões mais parecidos com os que o harness descreve | declarada: maior | 2º padrão |

### 4.8 Entre níveis do harness

No V4, os níveis rodam no mesmo quarteto, então estas comparações também são
**por pares** (N2 × N1, N3 × N2), lidas com as regras do §4.1. Cada nível acumula
o anterior (§5), e por isso cada um é comparado com o de logo abaixo.

**Skills (o N2 contra o N1)**

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Skills: mais efeito no desenho** | acrescentar skills aumenta o efeito positivo em desenho, além do `CLAUDE.md` sozinho | declarada: aumenta | N2 |
| **Skills: mais custo** | acrescentar skills aumenta o custo | declarada: aumenta | N2 |
| **Skills: só valem se carregadas** | o efeito de uma skill depende de o modelo carregá-la: execuções que não a invocam se comportam como o N1 | declarada | N2 |
| **Skills: mudam o exagero** | mais componentes no harness alteram a taxa de exagero (a da hipótese do exagero) | sem direção | N2 |

**Processo (o N3 contra o N2)**

O N3 acrescenta um desenho curto antes do código e um revisor independente, que
confere o código contra a tarefa e aponta, entre outras coisas, complexidade
desnecessária.

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Processo: corrige mais** | o N3 passa em mais casos da suíte oculta que o N2: o revisor confere o código contra a tarefa | declarada: mais | N3 |
| **Processo: mais efeito no desenho** | o efeito positivo em desenho do N3 é maior que o do N2 | declarada: maior | N3 |
| **Processo: menos exagero** | o N3 exagera menos que o N2: o revisor aponta complexidade desnecessária | declarada: menos | N3 |
| **Processo: mais custo** | o N3 gasta mais tokens e tempo que o N2: o desenho e a revisão são turnos a mais | declarada: mais | N3 |
| **Processo: só vale se usado** | execuções do N3 que não chamam o revisor se comportam como o N2 | declarada | N3 |

O uso do revisor se confere na transcrição: o subagente aparece em
`outcome.subagent_stats` do `meta.json`. O custo segue secundário, como todo o eixo
do custo.

### 4.9 Qualidade e métricas automáticas

As métricas são as colunas do `metricas.csv` (ver
[`evaluation/tools/README.md`](evaluation/tools/README.md)).

| hipótese | métrica | direção na `HARNESS` | por quê |
|---|---|---|---|
| **Qualidade: menos complexidade** ★ | `sonar_cognitive_complexity` | menor | escolher o caso por uma cadeia de `if`/`switch` é o que a complexidade cognitiva pune |
| **Qualidade: menos code smells** | `sonar_code_smells` | menor | o que o SonarQube aponta como problema de manutenção |

**De onde veio a complexidade cognitiva.** No ensaio do EXT (V3), ela foi a métrica
que mais mudou com o harness (caiu em 8 dos 9 pares), e por isso é proposta como
principal (decisão do Lucas, 09/10). É uma escolha feita olhando dados anteriores, e por isso declarada aqui: o
V4 são dados novos, e, se o ensaio foi sorte, o V4 mostra. No teste do Haiku do V4 (uma
réplica, `TESTE-NIVEIS-01`), ela **subiu** com o harness.

**Como se lê.** Cada métrica é lida por pares, com a regra do §4.1 do tipo da
hipótese (direcional ou sem direção, medida contínua), sem a regra do teto. Valores
iguais no par contam como igual.

As demais colunas do `metricas.csv` (linhas, métodos, complexidade ciclomática, WMC,
RFC, DIT) são **descritivas**: publicadas, sem hipótese.

### 4.10 A conferência do Semgrep, a releitura e a nota

**A conferência.** O Semgrep mede o desenho dos 100 pacotes. Para saber se ele mede
certo, o Lucas lê **20 pacotes**, um por modelo × nível, sorteados com semente
registrada (`evaluation/tools/sample.mjs`), **às cegas**: a cópia vem sem comentários
e sem README, com os códigos anonimizados, e ele não vê o resultado do Semgrep. Ele
responde às 4 perguntas da régua no P4 e no P5 (`evaluation/regua.md`). As duas
leituras são commitadas antes da comparação, e um script compara
(`evaluation/tools/compare.mjs`).

**A regra de saída.** Uma pergunta do Semgrep **vale para os 100 pacotes** se a
concordância for de pelo menos **18 de 20**. Abaixo disso, ela vira **descritiva**: as
hipóteses que dependem dela são lidas só nos 20 pacotes conferidos, e a discordância
entra nas limitações. Célula vazia ou `indeterminado`, de qualquer lado, conta como
discordância. Nas discordâncias, a regra do Semgrep **não** é corrigida para refazer a
conta oficial; uma regra corrigida pode aparecer como análise extra, marcada como tal.
O P1 a P3 não têm conferência humana e entram só como secundários.

**A releitura.** Há um leitor só. No lugar da concordância entre dois leitores, ele
relê **4 ou 5** dos 20 pacotes uma ou duas semanas depois, sorteados nesse momento, sem
ver as respostas antigas, e a mesma comparação mede se ele responde igual. A coluna
`achei_que_sabia_nivel` da planilha mede se o cegamento vazou (por exemplo, por nomes
de classe).

**A nota.** Cada execução recebe uma nota de 0 a 100, **como resumo**: as hipóteses são
lidas nas medidas separadas. Pesos A: contas 30, recusas 20, padrão 50 (P1 a P4, 10
por ponto: 10 com a localização e a seleção certas, 5 com uma, 0 com nenhuma; o P5 sem
exagero, 10). Quem não compila ou não sobe tem nota 0. As variantes B (40, 20, 40) e C
(35, 25, 40) são publicadas ao lado, para mostrar se a ordem depende dos pesos
(`evaluation/tools/nota.mjs`). A qualidade do SonarQube fica fora da nota, porque não
tem um "100" natural.

---

## 5. O que já foi testado

### O padrão

| padrão | enunciado | pontos positivos | controle negativo | lote | avaliação | estado |
|---|---|---|---|---|---|---|
| **Strategy** | `experiment/prompt/prompt.md` (`8c70bb30…`) | P1 entrega, P2 cupom, P3 pagamento, P4 clube | P5 seguro por região | `V4-STRATEGY-01` a `05` | `evaluation/strategy/` | pronto para o V4; ensaiado no `EXT` (V3) e nos `TESTE-MAPA-01` e `TESTE-NIVEIS-01` (V4) |
| **State** | `history/state/state.md` | E1 ações por situação, E2 efeitos do cancelamento e da devolução | E3 texto para o cliente | nenhum | `history/state/` | **fora do V4** (07/10). Serviu para provar que a bancada aceita um segundo padrão |

### Os instrumentos

| instrumento | estado | onde |
|---|---|---|
| a suíte (21 casos) | a referência confirmada por 4 implementações independentes do Opus (21 de 21 cada); 17 mutantes reprovados | `evaluation/acceptance-prototype/`, `infra/scripts/acceptance.sh` |
| o Semgrep | congelado em 09/10; P1 a P4 validados no EXT e nos 4 Opus, o P5 nos 4 Opus e em 4 exageros de mentira | `evaluation/tools/semgrep/` |
| SonarQube e CK | travados por hash; rodados no EXT e nos testes do V4 | `evaluation/tools/` |
| a régua (versão 4) e o guia | rascunho de 09/10; a calibração do Lucas em 1 ou 2 pacotes falta | `evaluation/regua.md`, `evaluation/GUIA-DA-REGUA.md` |

### Os harnesses

Os níveis seguem a escada N0 a N4 do orientador, explicada no
[README dos harnesses](experiment/harnesses/README.md): cada nível acumula o anterior.
Com a verificação automática descartada, ela e o processo trocaram de número em
relação à proposta, para os níveis que existem ficarem contíguos (N0 a N3) e o
descartado ir para o fim (N4).

| nível | componentes | pasta | hash da árvore |
|---|---|---|---|
| **N0** | nada (o braço `CONTROL`) | — | — |
| **N1** | `CLAUDE.md` com 4 regras sobre variação | `N1/` | `560577922737dbb9` |
| **N2** | o mesmo `CLAUDE.md` + a skill `gof-patterns` (pública, intacta) | `N2/` | `27987df0bd1febe2` |
| **N3** | o N2 + processo: um desenho curto antes do código e um revisor independente | `N3/` | `5f4c492bf3d12f68` |
| N4 | verificação automática | — | **descartado** (06/10): o agente já se verifica sozinho |

Os quatro níveis rodaram juntos no teste do V4 (`TESTE-NIVEIS-01`), com o Haiku 4.5 e
com o Opus 5. Nas hipóteses, "harness v1" é o N1; as de *Skills* dependem do N2, e as
de *Processo*, do N3.

## 6. O que este trabalho não afirma

- **Não generaliza para qualquer enunciado.** O resultado vale para enunciados como o
  daqui: um serviço pequeno, construído do zero, em Java/Spring.
- **Não compara padrões nem versões de harness estatisticamente.** Dentro de um lote
  há pares; entre lotes, não.
- **O Semgrep pode errar em silêncio.** Uma forma de escrever que as regras não
  previram passa sem aviso (no ensaio, isso aconteceu no P5 e foi corrigido). O alarme
  de nomes desconhecidos e a conferência humana reduzem o risco, mas não o eliminam, e
  o P1 a P3 não têm conferência.
- **Há um leitor humano só.** A releitura mede a estabilidade dele, não a concordância
  entre pessoas, que é o que o documento do orientador de 12/09 pedia.
- **Os instrumentos foram desenhados com a ajuda de um modelo de IA** (o Claude, da
  mesma família dos agentes). Nenhum deles usa IA para avaliar, e a conferência humana
  é o que valida o Semgrep.
- **O ensaio influenciou escolhas**: a complexidade cognitiva como principal e a
  escada de 5 modelos vieram do que o EXT e os testes mostraram.
- **O N3 gasta muito mais** (no teste, cerca de 3 vezes os tokens do N0). Se ele
  melhorar, pode ser o processo ou só mais computação; o custo sai ao lado de cada
  resultado.
- **O teto:** os modelos fortes acertam quase tudo sem harness. Neles, o estudo só diz
  se o harness atrapalha.
- **A skill do N2 (e do N3, que a acumula) traz exemplos próximos do domínio.** É a
  `gof-patterns`, pública e intacta, escrita sem conhecer as tarefas
  ([`experiment/third-party/gof-patterns/ORIGEM.md`](experiment/third-party/gof-patterns/ORIGEM.md)).
  O exemplo de Strategy dela é pagamento, um dos pontos do enunciado: um efeito do N2
  pode vir do conhecimento ou do molde. A transcrição registra se o agente abriu a
  página do padrão.
- **Não mede o harness fora do Claude Code.** Os modelos rodam no Claude Code como ele
  vem, que já traz as suas próprias orientações. O efeito medido é o do harness
  **somado** a essa base.
- **A manutenção ficou de fora** (09/10): o custo de acrescentar um caso novo não é
  medido.
- **O P3 (pagamento) não tem frase de crescimento** no enunciado, ao contrário do P1,
  do P2 e do P4: um `switch` sobre as 3 formas conta como erro no Semgrep sem que o
  enunciado dê motivo para separá-las. Por isso, e por não ter conferência humana, o
  P3 é só secundário.
- **O exploratório não confirma nada.** Os 5 modelos dele são descritos, não testados,
  e o Semgrep dele não tem conferência humana. O Opus 5.5 ficou fora porque exige um
  Claude Code mais novo que o da bancada.
- **O mapa é uma execução por modelo.** Ele só põe os modelos em degraus, e o lugar de
  cada um pode variar de uma execução para outra (o Haiku 4.5 fez 20 e depois 17 de 21
  no N0).

## 7. Códigos usados até 05/10

Até 05/10/2026 as hipóteses eram citadas por códigos: a letra do eixo e um número.
Eles foram trocados por nomes, que se leem sem legenda. Os códigos continuam nos
commits anteriores e nos registros datados (`evaluation/calibracao-relatorio.md`,
`analysis/testes-2026-09-30.md`); esta tabela serve só para lê-los. Atenção: os
códigos N1 a N4 daqui são das hipóteses de **Exagero**, não os níveis de harness
N1 a N4 do §5, que têm o mesmo nome por coincidência. Como no §4,
★ quer dizer "principal".

| código | nome |
|---|---|
| D1 ★ | Desenho: isola cada caso (a hipótese do desenho) |
| D2 | Desenho: comporta o caso exigente |
| D3 | Desenho: não repete o comum |
| D4 | Desenho: caso novo com pouca edição |
| D5 | Desenho: réplicas mais parecidas |
| N1 ★ | Exagero: aplica onde não pede (a hipótese do exagero) |
| N2 | Exagero: estrutura especulativa |
| N3 | Exagero: mais arquivos |
| N4 | Exagero: padrão diferente do pedido |
| C1 ★ | Correção: suíte inteira (a hipótese da correção) |
| C2 | Correção: não quebra o build |
| C3 | Correção: casos de borda |
| C4 | Correção: onde aplicou o padrão |
| K1 | Custo: tokens de entrada (principal até 06/10) |
| K2 | Custo: tempo de API |
| K3 | Custo: sinal muda por modelo |
| K4 | Custo: processo de trabalho |
| M1 ★ | Modelo: efeito maior no mais fraco (a hipótese do modelo) |
| M2 | Modelo: no teto, não piora |
| M3 | Modelo: quem melhora não é quem exagera |
| X1 | Entre padrões: mais efeito onde a CONTROL erra mais |
| X2 | Entre padrões: só na família de variação |
| X3 | Entre padrões: exagera nos parecidos |
| S1 | Skills: mais efeito no desenho |
| S2 | Skills: mais custo |
| S3 | Skills: só valem se carregadas |
| S4 | Skills: mudam o exagero |

Na suíte de aceitação, os mutantes se chamavam M1 a M16 até 05/10, o que colidia
com os códigos de Modelo; passaram a ser "mutante 1" a "mutante 16" (`MUT1` a
`MUT16` no código).
