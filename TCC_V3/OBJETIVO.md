# Objetivo

> **RASCUNHO**: as hipóteses (§4) e as regras de leitura (§4.1) são proposta, e a
> decisão é do Lucas com o orientador. Este arquivo só vale como registro depois
> de commitado **antes** da leitura que ele orienta; a data do commit é a prova.

O [README](README.md) diz **como** o experimento roda. Este arquivo diz **para
quê**: a pergunta, o que conta como resposta, e o que já foi testado.

---

## 1. A pergunta

> Um harness (o conjunto de orientações que acompanha o agente: `CLAUDE.md`,
> skills e o que mais vier) influencia, **para o bem ou para o mal**, a forma como
> um agente de código aplica padrões de projeto ao construir um sistema do zero?
> Essa influência muda conforme o modelo, o padrão pedido e o que compõe o harness?

É a célula **Construir software × Design de baixo nível** da matriz combinada na
orientação de 12/09: padrões de projeto, com e sem harness, em mais de um modelo.
Manter software, arquitetura, testes e banco de dados ficam fora deste recorte.

O trabalho começa com o harness mais simples, **só um `CLAUDE.md`**, e com um
padrão só, **Strategy**. Esse primeiro par serve para organizar o corpo do
projeto: bancada, régua, leitura e análise. Padrões e componentes de harness
novos entram depois, um de cada vez, sobre o mesmo corpo.

## 2. O que fica fixo, e o que varia

| | |
|---|---|
| **o que varia dentro de um lote** | só a condição: `CONTROL` (workspace vazio) ou `HARNESS` (com uma versão de `experiment/harnesses/`) |
| **o harness** | **fixo dentro de uma versão**, igual em todos os padrões testados com ela. Não é ajustado a um enunciado. Cada versão é uma pasta de `experiment/harnesses/`, identificada pelo hash da árvore, gravado em `environment.harness_hash` no `meta.json` |
| **o enunciado** | um por padrão testado, **igual nos dois braços**. Escrito como um cliente pedindo o sistema, sem palavra de arquitetura |
| **os modelos** | Opus, Sonnet e Haiku; o modelo é fator de bloco |
| **a unidade de análise** | o **par simultâneo**: a `CONTROL` e a `HARNESS` que rodaram no mesmo instante, no mesmo modelo |

O desenho de cada lote é 3 modelos × 2 condições × 3 réplicas = 18 execuções, 9
pares. Detalhes, hashes e cuidados estão no README.

**O TCC_V3 é a bancada de testes.** Os lotes daqui, inclusive o `EXT`, servem para
validar a bancada, os enunciados e os instrumentos de medida. O experimento que
vale vai rodar numa versão futura (V4), com 5 réplicas por célula; as regras de
leitura do §4.1, calculadas aqui para 3 réplicas, serão refeitas para 5 antes de
rodar.

## 3. O que conta como influência

Um harness pode melhorar um eixo e piorar outro, por isso a influência é lida em
três:

| eixo | a pergunta | instrumento | estado |
|---|---|---|---|
| **desenho** | aplicou o padrão onde o enunciado pede, e deixou de aplicar onde seria exagero? | régua de leitura, cega e dupla; métricas automáticas (CK, SonarQube) como complemento | régua a construir; métricas **prontas** |
| **correção** | o código calcula o que o enunciado pede? | suíte de aceitação oculta, via HTTP | a construir |
| **custo e processo** | quanto gastou, e como trabalhou, para chegar lá? | `meta.json`: `tokens.input_total`, `tokens.output`, `timing.duration_api_ms`, `outcome.turns`, `outcome.tool_calls_by_name` | **pronto** |

**As métricas automáticas são secundárias.** Medem tamanho, complexidade
cognitiva, duplicação, acoplamento e coesão do código de produção
([`evaluation/tools/README.md`](evaluation/tools/README.md)). Complementam a régua,
não a substituem, e nenhuma decide hipótese. Quatro delas estão ligadas a
hipóteses no §4.9, como conferência independente da régua; as outras são
descritivas, publicadas por par, como o custo.

**Influência negativa** conta tanto quanto a positiva. Em desenho, ela aparece
como exagero (aplicar o padrão onde o enunciado não pede), estrutura especulativa
ou código maior sem ganho. Em correção, como estrutura comprada com erro de
cálculo. Em custo, como gasto a mais sem ganho em outro eixo.

Por isso todo enunciado tem os dois tipos de ponto:

- **ponto positivo**: o enunciado descreve uma variação que o padrão resolve;
- **controle negativo**: o enunciado descreve algo que *parece* pedir o padrão e
  não pede. Aplicar o padrão ali é o erro.

---

## 4. As hipóteses

### Como ler esta seção

As hipóteses estão agrupadas por **eixo**, e cada uma tem um nome no formato
*Eixo: o que ela afirma*. O exagero é a influência negativa no desenho, do §3;
ganha eixo próprio porque é medido à parte.

| eixo | a pergunta |
|---|---|
| **Desenho** | o padrão foi aplicado onde o enunciado pede? |
| **Exagero** | o padrão foi aplicado onde o enunciado **não** pede? |
| **Correção** | o código calcula o que o enunciado pede? |
| **Custo** | quanto o agente gastou, e como trabalhou? |
| **Modelo** | o efeito do harness muda de um modelo para outro? |
| **Entre padrões**, **Skills**, **Processo** | só depois do 2º padrão testado, ou dos níveis N2 e N3 do harness rodados |

**As quatro principais** são as que respondem à pergunta do §1. Em todas as
tabelas deste arquivo, **★ quer dizer "principal"**. No texto, cada uma é chamada
pela forma curta:

| forma curta | nome | em uma frase |
|---|---|---|
| **a hipótese do desenho** | Desenho: isola cada caso | com o harness, o agente aplica melhor o padrão onde o enunciado pede |
| **a hipótese do exagero** | Exagero: aplica onde não pede | o harness muda o quanto o agente exagera |
| **a hipótese da correção** | Correção: suíte inteira | com o harness, o código não fica mais errado |
| **a hipótese do modelo** | Modelo: efeito maior no mais fraco | o efeito do harness no desenho é maior no modelo mais fraco |

As demais são **secundárias**: são medidas e publicadas, mas não sustentam
conclusão sozinhas. O **custo** (tokens e tempo) é todo secundário, por orientação
de 12/09: depende de cache e de outras variáveis fora do experimento, e serve como
dado complementar, não como conclusão. *Custo: tokens de entrada* foi principal
até 06/10. A distinção importa: com muitas hipóteses e 9 pares por lote,
alguma vai "dar certo" por acaso.

Cada hipótese tem também uma **direção** (declarada, ou "sem direção" quando o
harness pode puxar para os dois lados) e um **quando**:

- **agora**: harness v1 (só o `CLAUDE.md`) com o Strategy;
- **2º padrão**: exige pelo menos dois padrões testados;
- **N2**, **N3**: exige aquele nível do harness rodado (a escada está no §5).

Até 05/10 as hipóteses tinham códigos (D1, N1, C1...), que ainda aparecem no
histórico do git. A correspondência está no §7.

**Palavras usadas nas regras:**

- **braço**: `CONTROL` (workspace vazio) ou `HARNESS` (com o harness).
- **par**: uma `CONTROL` e uma `HARNESS` do mesmo modelo, rodadas no mesmo
  instante. Cada lote tem 9 pares: 3 modelos × 3 réplicas.
- **melhor, igual, pior**: em cada par, como a `HARNESS` ficou em relação à
  `CONTROL`, no que a hipótese mede.
- **teto**: um modelo cuja `CONTROL` já acerta tudo o que a hipótese mede, nas 3
  réplicas. Ali o harness não tem como melhorar: só empatar ou piorar. No piloto,
  Opus e Sonnet estavam no teto de desenho.

### 4.1 Como uma hipótese é lida (proposta)

Com 3 réplicas por célula, cada modelo tem só 3 pares: o menor p de um teste de
sinal é 0,125, mesmo com os 3 na mesma direção. Somando os 9 pares, o teste de
sinal chega abaixo de 0,05 só com 8 ou 9 na mesma direção, e junta modelos que se
comportam diferente. Por isso a leitura é **por pares, descritiva, e decidida
aqui antes dos dados**; o teste de sinal sobre os 9 pares pode ser reportado ao
lado, como informação, não como critério.

| tipo de hipótese | apoiada quando | contrariada quando |
|---|---|---|
| **direcional** | na maioria dos modelos **fora do teto** há mais pares melhores que piores, e em nenhum modelo (no teto ou não) há mais piores que melhores | na maioria dos modelos fora do teto há mais pares piores que melhores |
| **não-inferioridade** ("não piora") | no máximo 1 dos 9 pares é pior | 2 ou mais pares são piores |
| **sem direção** ("altera"), medida contínua (tokens, tempo) | pelo menos 7 dos 9 pares vão na mesma direção, qualquer que seja | nenhuma direção chega a 7 |
| **sem direção** ("altera"), medida sim/não (exagerou ou não) | todos os pares não empatados vão na mesma direção, e são pelo menos 3 | menos de 3 pares não empatados, ou eles se dividem |

Fora desses casos, a hipótese é **inconclusiva**, e isso é resultado, não falha.
A tabela de pares é sempre publicada inteira, qualquer que seja a leitura.
"Igual" nunca conta como direção.

**Por que o teto sai da contagem.** Um modelo no teto não tem como mostrar
melhora; contá-lo tornaria uma hipótese direcional impossível de apoiar sempre que
dois modelos estivessem no teto, mesmo com efeito claro no terceiro. Por isso, nas
hipóteses direcionais, os modelos no teto (na medida da própria hipótese) saem da
contagem de "melhor" e são lidos por *Modelo: no teto, não piora*. Se os três
estiverem no teto, a hipótese é registrada como "sem espaço para efeito", e a
resposta vem de *Modelo: no teto, não piora*.

**Por que a medida sim/não tem regra própria.** Quando a medida é exagerou ou
não, a maioria dos pares tende a empatar (nenhum dos dois exagerou), e a regra dos
7 em 9 nunca seria alcançada. Contam então só os pares em que os braços diferem.

**A hipótese do modelo compara modelos, não pares.** É apoiada quando o modelo
com menos acertos na `CONTROL` da hipótese do desenho (média das 3 réplicas) tem
o maior saldo de pares nela (melhores − piores), sem empate; contrariada quando
outro modelo tem saldo maior; com empate no maior saldo, inconclusiva.

**Exemplo, com números inventados.** Na hipótese do desenho, o Haiku tem 2 pares
melhores e 1 igual; Opus e Sonnet estão no teto, com 3 pares iguais cada. O Haiku
é o único modelo fora do teto, e nele há mais melhores que piores; nenhum modelo
piorou. **A hipótese do desenho é apoiada.** E a do modelo também: o Haiku, o mais
fraco na `CONTROL`, tem o maior saldo (+2, contra 0 dos outros).

### 4.2 Desenho

Nesta tabela e nas seguintes, **★ marca as hipóteses principais**; as sem ★
são secundárias. O eixo do custo não tem principal.

Cada uma liga uma regra do harness v1 a algo observável no código.

| hipótese | o que afirma | regra do harness v1 | direção | quando |
|---|---|---|---|---|
| **Desenho: isola cada caso** ★ | nos pontos positivos, a `HARNESS` tem mais pontos em que o comportamento de cada caso mora numa unidade só dele e a escolha entre os casos não é uma cadeia de condições | 2 | declarada: mais | agora |
| **Desenho: comporta o caso exigente** | nos pontos em que um caso exige mais que os outros (como o OURO no P4), a `HARNESS` tem mais vezes uma assinatura que comporta o caso mais exigente, sem remendo fora da estrutura | 3 | declarada: mais | agora |
| **Desenho: não repete o comum** | a `HARNESS` repete menos, dentro de cada caso, o que é comum a todos (a mesma fórmula ou o mesmo arredondamento copiados em cada variante) | 1 | declarada: menos | agora |
| **Desenho: caso novo com pouca edição** | para acrescentar um caso novo num ponto positivo, a `HARNESS` exige editar menos lugares do código existente | 1 e 2 | declarada: menos | agora |
| **Desenho: réplicas mais parecidas** | as 3 réplicas de um mesmo modelo são mais parecidas entre si na `HARNESS` que na `CONTROL` (o harness torna o desenho mais consistente) | todas | declarada: mais parecidas | agora |

### 4.3 Exagero

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Exagero: aplica onde não pede** ★ | nos controles negativos, o harness **altera** a taxa de exagero (aplicar o padrão onde o enunciado não pede) | sem direção | agora |
| **Exagero: estrutura especulativa** | fora dos pontos de variação, o harness altera a quantidade de estrutura especulativa: interface com uma implementação só, fábrica para um caso só, extensão para variação que o enunciado não descreve | sem direção | agora |
| **Exagero: mais arquivos** | a `HARNESS` produz mais arquivos para o mesmo enunciado | declarada: mais | agora |
| **Exagero: padrão diferente do pedido** | quando o enunciado pede um padrão, a `HARNESS` aplica mais vezes um padrão **diferente** do pedido | declarada: mais | 2º padrão |

A hipótese do exagero é sem direção de propósito, porque o harness puxa para os
dois lados: a regra 4 ("não crie estrutura para variação que você imagina") prevê
**menos** exagero, e a ênfase das regras 1 a 3 em isolar o que varia pode induzir
**mais**. O resultado diz qual das duas forças ganha.

### 4.4 Correção

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Correção: suíte inteira** ★ | a `HARNESS` não passa em menos casos da suíte oculta que a `CONTROL` | não-inferioridade | agora |
| **Correção: não quebra o build** | a `HARNESS` não tem mais builds quebrados (`outcome.build_ok`) | não-inferioridade | agora |
| **Correção: casos de borda** | nos casos de borda do enunciado (empate de arredondamento, precedência entre erros, colisão entre regras como OURO e FRETEGRATIS), a `HARNESS` não erra mais | não-inferioridade | agora |
| **Correção: onde aplicou o padrão** | nos pontos em que a `HARNESS` aplicou o padrão, os casos daquele ponto passam tanto quanto na `CONTROL` (estrutura não custa correção) | não-inferioridade | agora |

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

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Modelo: efeito maior no mais fraco** ★ | o efeito na hipótese do desenho é maior no modelo que menos acerta sem harness | declarada: maior | agora |
| **Modelo: no teto, não piora** | nos modelos que acertam tudo sem harness (teto), o harness não piora o desenho | não-inferioridade | agora |
| **Modelo: quem melhora não é quem exagera** | a influência negativa (o exagero e a estrutura especulativa) aparece em modelos diferentes dos que têm a influência positiva | sem direção | agora |

O piloto já sugere a hipótese do modelo e *Modelo: no teto, não piora*: no
enunciado de três pontos, Opus e Sonnet acertaram todos os pontos **nos dois
braços**. Onde o modelo já acerta sozinho, não há espaço para efeito positivo, e a
pergunta passa a ser se o harness atrapalha.

### 4.7 Entre padrões

Comparação **observacional**: cada padrão é um lote próprio, sem par entre lotes.

| hipótese | o que afirma | direção | quando |
|---|---|---|---|
| **Entre padrões: mais efeito onde a CONTROL erra mais** | o efeito positivo é maior nos padrões em que a `CONTROL` acerta menos | declarada: maior | 2º padrão |
| **Entre padrões: só na família de variação** | o harness v1, que fala de variação, tem efeito positivo em padrões de variação (State, Template Method, Chain) e efeito nulo ou negativo em padrões fora dessa família | declarada | 2º padrão |
| **Entre padrões: exagera nos parecidos** | a taxa de exagero (a da hipótese do exagero) é maior nos padrões mais parecidos com os que o harness descreve | declarada: maior | 2º padrão |

### 4.8 Entre níveis do harness

Também **observacional**: cada nível roda em lotes próprios, e a comparação é
entre lotes. Cada nível acumula o anterior (§5), então cada um é comparado com o
nível logo abaixo: o N2 com o N1, o N3 com o N2.

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

### 4.9 Métricas automáticas ligadas às hipóteses

Quatro métricas automáticas (as colunas do `metricas.csv`, ver
[`evaluation/tools/README.md`](evaluation/tools/README.md)) servem de **conferência**
de uma hipótese que a régua já mede. Pré-registradas em 06/10/2026, antes de
qualquer dado do lote que vale (V4).

| métrica | confere a hipótese | direção esperada na `HARNESS` | por quê |
|---|---|---|---|
| `sonar_cognitive_complexity` | a hipótese do desenho | menor | escolher o caso por uma cadeia de `if`/`switch` é o que a complexidade cognitiva pune |
| `sonar_duplicated_lines_density` | Desenho: não repete o comum | menor | a parte comum copiada em cada caso aparece como linha duplicada |
| `sonar_classes` | Exagero: mais arquivos | maior | conta os tipos com nome, o mesmo critério de `ck_tipos` |
| `ck_cbo_media` e `ck_lcom_media` | Exagero: estrutura especulativa | sem direção | interface e fábrica sem necessidade mudam o acoplamento e a coesão médios |

**Como se lê.** Cada métrica é lida por pares, com a regra do §4.1 do tipo da
hipótese que ela confere (direcional ou sem direção, medida contínua), sem a
regra do teto, que não se aplica a uma medida contínua. Valores iguais no par
contam como igual.

**O que ela não faz.** A métrica **não muda o veredito** da hipótese, que continua
sendo o da régua (ou da suíte). Ela é publicada ao lado, como **concorda** (a
mesma leitura), **não concorda** (a leitura oposta) ou **inconclusiva**. Um par em
que a métrica e a régua apontam lados opostos é listado para ser relido, e a
divergência é registrada e discutida, nunca resolvida trocando um número pelo
outro.

As demais colunas do `metricas.csv` (linhas, métodos, complexidade ciclomática,
code smells, `ck_anonymous`, WMC, RFC, DIT) são **descritivas**: publicadas, sem
hipótese.

---

## 5. O que já foi testado

### Os padrões

| padrão | enunciado | pontos positivos | controle negativo | lote | avaliação | estado |
|---|---|---|---|---|---|---|
| **Strategy** | `experiment/prompt/prompt.md` | P1 entrega, P2 cupom, P3 pagamento, P4 clube | P5 imposto | `EXT-01` a `03` | `evaluation/strategy/` | rodado como lote de teste (3 réplicas); leitura feita antes da régua, a refazer |
| **State** | `experiment/prompt/state.md` | E1 ações por situação, E2 efeitos do cancelamento e da devolução | E3 texto para o cliente | `STATE-01` a `03` (a rodar) | `evaluation/state/` | enunciado e gabarito escritos; falta `SMOKE`, calibração e lote |

A coluna **avaliação** é a pasta do padrão: o gabarito, os pacotes cegos, o mapa
e as leituras ficam juntos lá, e o cabeçalho do gabarito repete o hash do
enunciado e o prefixo do lote.

### Os harnesses

Os níveis seguem a escada N0 a N4 do orientador, explicada no
[README dos harnesses](experiment/harnesses/README.md): cada nível acumula o anterior.
Com a verificação automática descartada, ela e o processo trocaram de número em
relação à proposta, para os níveis que existem ficarem contíguos (N0 a N3) e o
descartado ir para o fim (N4); os motivos estão no mesmo README.

| nível | componentes | pasta | hash da árvore | lotes |
|---|---|---|---|---|
| **N0** | nada (o braço `CONTROL`) | — | — | `EXT-01` a `03` |
| **N1** | `CLAUDE.md` com 4 regras sobre variação | `N1/` | `560577922737dbb9` | `EXT-01` a `03` |
| N2 | o mesmo `CLAUDE.md` + a skill `gof-patterns` (pública, intacta) | `N2/` | `27987df0bd1febe2` | pronta, não rodou |
| N3 | o N2 + processo: um desenho curto antes do código e um revisor independente | `N3/` | `5f4c492bf3d12f68` | pronto, não rodou |
| N4 | verificação automática (build e testes que rodam sozinhos) | — | — | **descartado** (06/10): o agente já se verifica sozinho em 62 de 62 execuções; ver o README dos harnesses |

Nas hipóteses, "harness v1" é o N1; as de *Skills* dependem do N2, e as de
*Processo*, do N3. Até 06/10 as pastas se chamavam `only-claude/` (N1) e
`claude-and-skills/` (N2); havia também `only-skills/`, que nunca rodou e saiu.

### Como algo novo entra

Um **padrão novo** entra como uma linha na primeira tabela, **antes** de rodar:

1. o enunciado em `experiment/prompt/<padrao>.md`, rodado com `PROMPT_FILE`;
2. o lote com o nome do padrão no prefixo (por exemplo `STATE-01`);
3. pelo menos um ponto positivo e um controle negativo;
4. uma rodada `SMOKE-` antes do lote, para ver se o enunciado não bate no teto
   (os dois braços acertam tudo) nem no chão (nenhum acerta);
5. a pasta `evaluation/<padrao>/` com o `gabarito.md`, e os pacotes gerados com
   `anonimizar.mjs --padrao <padrao>`.

Uma **versão nova do harness** é uma pasta nova em `experiment/harnesses/`
(como montar: [`experiment/harnesses/README.md`](experiment/harnesses/README.md)),
rodada com `HARNESS=<nome>`. Ela entra como uma linha na segunda tabela, antes de
rodar, com os componentes e o hash, e roda **pelo menos o padrão Strategy**, para
que a comparação com o N1 tenha um ponto em comum.

O **piloto** (`BATCH-01` a `03`, em `history/pilot/`) não entra na análise:
foi ele que mostrou o teto e motivou P4 e P5.

## 6. O que este trabalho não afirma

- **Não generaliza para qualquer enunciado.** O resultado vale para enunciados
  como os daqui: um serviço pequeno, construído do zero, em Java/Spring.
- **Não compara padrões nem versões de harness estatisticamente.** Dentro de um
  lote há pares; entre lotes, não. Dizer que o harness ajuda mais num padrão, ou
  que a v2 é melhor que a v1, é observação.
- **O enunciado do Strategy tem duas inconsistências, achadas depois do lote EXT.**
  Os exemplos 1 a 4 vieram do piloto: não trazem clube nem região, e os totais não
  têm imposto, embora o próprio enunciado mande recusar pedido sem clube ou região.
  E o exemplo de resposta do anexo mistura números do piloto com um imposto
  calculado sem desconto. O enunciado já rodou e não muda; a correção é medida só
  com o exemplo 5 e casos com clube e região, e o comportamento diante dos
  exemplos 1 a 4 é registrado como observação
  ([`analysis/testes-2026-09-30.md`](analysis/testes-2026-09-30.md)).
- **E uma terceira, achada ao validar a suíte de aceitação (03/10).** O passo 5
  define o total do pedido **com** imposto; a regra do boleto, entre parênteses,
  **sem**. As duas leituras só divergem nos pedidos que ficam abaixo de R$ 1.000
  sem imposto e acima com ele. A suíte não conta esse caso: registra como
  observação qual leitura cada execução seguiu, do mesmo jeito que os exemplos
  1 a 4 ([`evaluation/acceptance-prototype/README.md`](evaluation/acceptance-prototype/README.md)).
  Decidido antes de a suíte rodar sobre o lote EXT.
- **A skill do N2 (e do N3, que a acumula) traz exemplos próximos do domínio das tarefas.** É a `gof-patterns`,
  pública e intacta, escrita sem conhecer as tarefas
  ([`experiment/third-party/gof-patterns/ORIGEM.md`](experiment/third-party/gof-patterns/ORIGEM.md)).
  Os exemplos completos dela são os canônicos: o de State é um pedido (pagar,
  enviar, entregar, cancelar, devolver), e o de Strategy é pagamento. Um efeito do
  N2 pode vir do conhecimento ou do molde. A transcrição registra se o agente abriu
  a página do padrão, e isso separa as execuções que leram o exemplo das que não
  leram.
- **Não mede o harness fora do Claude Code.** Os modelos rodam no Claude Code como
  ele vem, que já traz as suas próprias orientações. O efeito medido é o do
  harness **somado** a essa base.

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
