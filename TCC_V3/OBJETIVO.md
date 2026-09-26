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
| **o que varia dentro de um lote** | só a condição: `CONTROL` (workspace vazio) ou `HARNESS` (com uma versão de `experimento/harnesses/`) |
| **o harness** | **fixo dentro de uma versão**, igual em todos os padrões testados com ela. Não é ajustado a um enunciado. Cada versão é uma pasta de `experimento/harnesses/`, identificada pelo hash da árvore, gravado em `environment.harness_hash` no `meta.json` |
| **o enunciado** | um por padrão testado, **igual nos dois braços**. Escrito como um cliente pedindo o sistema, sem palavra de arquitetura |
| **os modelos** | Opus, Sonnet e Haiku; o modelo é fator de bloco |
| **a unidade de análise** | o **par simultâneo**: a `CONTROL` e a `HARNESS` que rodaram no mesmo instante, no mesmo modelo |

O desenho de cada lote é 3 modelos × 2 condições × 3 réplicas = 18 execuções, 9
pares. Detalhes, hashes e cuidados estão no README.

## 3. O que conta como influência

Um harness pode melhorar um eixo e piorar outro, por isso a influência é lida em
três:

| eixo | a pergunta | instrumento | estado |
|---|---|---|---|
| **desenho** | aplicou o padrão onde o enunciado pede, e deixou de aplicar onde seria exagero? | régua de leitura, cega e dupla | a construir |
| **correção** | o código calcula o que o enunciado pede? | suíte de aceitação oculta, via HTTP | a construir |
| **custo e processo** | quanto gastou, e como trabalhou, para chegar lá? | `meta.json`: `tokens.input_total`, `tokens.output`, `timing.duration_api_ms`, `outcome.turns`, `outcome.tool_calls_by_name` | **pronto** |

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

Cada hipótese tem um **eixo**, uma **direção** (declarada ou "sem direção") e um
**quando**:

- **agora**: harness v1 (só o `CLAUDE.md`) com o Strategy;
- **2º padrão**: exige pelo menos dois padrões testados;
- **2ª versão**: exige pelo menos duas versões do harness.

As **primárias** (★) são as que respondem a pergunta; as demais são secundárias
e exploratórias. A distinção importa: com muitas hipóteses e 9 pares por lote,
alguma vai "dar certo" por acaso, e só as primárias sustentam conclusão.

### 4.1 Como uma hipótese é lida (proposta)

Com 3 réplicas por célula, cada modelo tem só 3 pares: o menor p de um teste de
sinal é 0,125, mesmo com os 3 na mesma direção. Somando os 9 pares, o teste de
sinal chega abaixo de 0,05 só com 8 ou 9 na mesma direção, e junta modelos que se
comportam diferente. Por isso a leitura é **por pares, descritiva, e decidida
aqui antes dos dados**; o teste de sinal sobre os 9 pares pode ser reportado ao
lado, como informação, não como critério.

Em cada par, a `HARNESS` fica **melhor**, **igual** ou **pior** que a `CONTROL` no
que a hipótese mede. Com isso:

| tipo de hipótese | apoiada quando | contrariada quando |
|---|---|---|
| **direcional** | há mais pares melhores que piores em pelo menos 2 dos 3 modelos, e em nenhum modelo há mais piores que melhores | há mais pares piores que melhores em pelo menos 2 dos 3 modelos |
| **não-inferioridade** ("não piora") | no máximo 1 dos 9 pares é pior | 2 ou mais pares são piores |
| **sem direção** ("altera") | pelo menos 7 dos 9 pares vão na mesma direção, qualquer que seja | nenhuma direção chega a 7 |

Fora desses casos, a hipótese é **inconclusiva**, e isso é resultado, não falha.
A tabela de pares é sempre publicada inteira, qualquer que seja a leitura.

### 4.2 Desenho: influência positiva

Cada uma liga uma regra do harness v1 a algo observável no código.

| | hipótese | regra do harness v1 | direção | quando |
|---|---|---|---|---|
| **D1** ★ | nos pontos positivos, a `HARNESS` tem mais pontos em que o comportamento de cada caso mora numa unidade só dele e a escolha entre os casos não é uma cadeia de condições | 2 | declarada: mais | agora |
| **D2** | nos pontos em que um caso exige mais que os outros (como o OURO no P4), a `HARNESS` tem mais vezes uma assinatura que comporta o caso mais exigente, sem remendo fora da estrutura | 3 | declarada: mais | agora |
| **D3** | a `HARNESS` repete menos, dentro de cada caso, o que é comum a todos (a mesma fórmula ou o mesmo arredondamento copiados em cada variante) | 1 | declarada: menos | agora |
| **D4** | para acrescentar um caso novo num ponto positivo, a `HARNESS` exige editar menos lugares do código existente | 1 e 2 | declarada: menos | agora |
| **D5** | as 3 réplicas de um mesmo modelo são mais parecidas entre si na `HARNESS` que na `CONTROL` (o harness torna o desenho mais consistente) | todas | declarada: mais parecidas | agora |

### 4.3 Desenho: influência negativa

| | hipótese | direção | quando |
|---|---|---|---|
| **N1** ★ | nos controles negativos, o harness **altera** a taxa de exagero (aplicar o padrão onde o enunciado não pede) | sem direção | agora |
| **N2** | fora dos pontos de variação, o harness altera a quantidade de estrutura especulativa: interface com uma implementação só, fábrica para um caso só, extensão para variação que o enunciado não descreve | sem direção | agora |
| **N3** | a `HARNESS` produz mais arquivos para o mesmo enunciado | declarada: mais | agora |
| **N4** | quando o enunciado pede um padrão, a `HARNESS` aplica mais vezes um padrão **diferente** do pedido | declarada: mais | 2º padrão |

A N1 é sem direção de propósito, porque o harness puxa para os dois lados: a
regra 4 ("não crie estrutura para variação que você imagina") prevê **menos**
exagero, e a ênfase das regras 1 a 3 em isolar o que varia pode induzir **mais**.
O resultado diz qual das duas forças ganha.

### 4.4 Correção

| | hipótese | direção | quando |
|---|---|---|---|
| **C1** ★ | a `HARNESS` não passa em menos casos da suíte oculta que a `CONTROL` | não-inferioridade | agora |
| **C2** | a `HARNESS` não tem mais builds quebrados (`outcome.build_ok`) | não-inferioridade | agora |
| **C3** | nos casos de borda do enunciado (empate de arredondamento, precedência entre erros, colisão entre regras como OURO e FRETEGRATIS), a `HARNESS` não erra mais | não-inferioridade | agora |
| **C4** | nos pontos em que a `HARNESS` aplicou o padrão, os casos daquele ponto passam tanto quanto na `CONTROL` (estrutura não custa correção) | não-inferioridade | agora |

### 4.5 Custo e processo

| | hipótese | direção | quando |
|---|---|---|---|
| **K1** ★ | o harness altera o consumo de entrada (`tokens.input_total`) | sem direção | agora |
| **K2** | o harness altera o tempo de API (`timing.duration_api_ms`) | sem direção | agora |
| **K3** | a direção do efeito no custo muda conforme o modelo | declarada: muda | agora |
| **K4** | o harness altera o processo: turnos, e a proporção entre ler, escrever e executar (`outcome.tool_calls_by_name`) | sem direção | agora |

O custo é sem direção porque o piloto mostrou o sinal trocando entre modelos: o
harness fez gastar mais em um e menos em outro.

### 4.6 Modelo

| | hipótese | direção | quando |
|---|---|---|---|
| **M1** ★ | o efeito positivo em desenho (D1) é maior no modelo que menos acerta sem harness | declarada: maior | agora |
| **M2** | nos modelos que acertam tudo sem harness (teto), o harness não piora o desenho | não-inferioridade | agora |
| **M3** | a influência negativa (N1, N2) aparece em modelos diferentes dos que têm a influência positiva | sem direção | agora |

O piloto já sugere a M1 e a M2: no enunciado de três pontos, Opus e Sonnet
acertaram todos os pontos **nos dois braços**. Onde o modelo já acerta sozinho,
não há espaço para efeito positivo, e a pergunta passa a ser se o harness
atrapalha.

### 4.7 Entre padrões

Comparação **observacional**: cada padrão é um lote próprio, sem par entre lotes.

| | hipótese | direção | quando |
|---|---|---|---|
| **X1** | o efeito positivo é maior nos padrões em que a `CONTROL` acerta menos | declarada: maior | 2º padrão |
| **X2** | o harness v1, que fala de variação, tem efeito positivo em padrões de variação (State, Template Method, Chain) e efeito nulo ou negativo em padrões fora dessa família | declarada | 2º padrão |
| **X3** | a taxa de exagero (N1) é maior nos padrões mais parecidos com os que o harness descreve | declarada: maior | 2º padrão |

### 4.8 Entre versões do harness

Também **observacional**: cada versão roda em lotes próprios, e a comparação é
entre lotes.

| | hipótese | direção | quando |
|---|---|---|---|
| **S1** | acrescentar skills aumenta o efeito positivo em desenho, além do `CLAUDE.md` sozinho | declarada: aumenta | 2ª versão |
| **S2** | acrescentar skills aumenta o custo | declarada: aumenta | 2ª versão |
| **S3** | o efeito de uma skill depende de o modelo carregá-la: execuções que não a invocam se comportam como a `CONTROL` | declarada | 2ª versão |
| **S4** | mais componentes no harness alteram a taxa de exagero (N1) | sem direção | 2ª versão |

---

## 5. O que já foi testado

### Os padrões

| padrão | enunciado | pontos positivos | controle negativo | lote | avaliação | estado |
|---|---|---|---|---|---|---|
| **Strategy** | `experimento/prompt/prompt.md` | P1 entrega, P2 cupom, P3 pagamento, P4 clube | P5 imposto | `EXT-01` a `03` | `avaliacao/strategy/` | rodado; leitura feita antes da régua, a refazer |

A coluna **avaliação** é a pasta do padrão: o gabarito, os pacotes cegos, o mapa
e as leituras ficam juntos lá, e o cabeçalho do gabarito repete o hash do
enunciado e o prefixo do lote.

### Os harnesses

| versão | componentes | hash da árvore | lotes |
|---|---|---|---|
| **`only-claude`** | `CLAUDE.md` com 4 regras sobre variação | `560577922737dbb9` | `EXT-01` a `03` |
| `claude-and-skills` | o mesmo `CLAUDE.md` + skills | muda ao entrar a primeira skill | pronta, sem skill |
| `only-skills` | só skills | muda ao entrar a primeira skill | pronta, sem skill |

Nas hipóteses, "harness v1" é o `only-claude`, e "2ª versão" é qualquer outra
destas depois de rodar.

### Como algo novo entra

Um **padrão novo** entra como uma linha na primeira tabela, **antes** de rodar:

1. o enunciado em `experimento/prompt/<padrao>.md`, rodado com `PROMPT_FILE`;
2. o lote com o nome do padrão no prefixo (por exemplo `STATE-01`);
3. pelo menos um ponto positivo e um controle negativo;
4. uma rodada `SMOKE-` antes do lote, para ver se o enunciado não bate no teto
   (os dois braços acertam tudo) nem no chão (nenhum acerta);
5. a pasta `avaliacao/<padrao>/` com o `gabarito.md`, e os pacotes gerados com
   `anonimizar.mjs --padrao <padrao>`.

Uma **versão nova do harness** é uma pasta nova em `experimento/harnesses/`
(como montar: [`experimento/harnesses/README.md`](experimento/harnesses/README.md)),
rodada com `HARNESS=<nome>`. Ela entra como uma linha na segunda tabela, antes de
rodar, com os componentes e o hash, e roda **pelo menos o padrão Strategy**, para
que a comparação com o `only-claude` tenha um ponto em comum.

O **piloto** (`BATCH-01` a `03`, em `historico/piloto/`) não entra na análise:
foi ele que mostrou o teto e motivou P4 e P5.

## 6. O que este trabalho não afirma

- **Não generaliza para qualquer enunciado.** O resultado vale para enunciados
  como os daqui: um serviço pequeno, construído do zero, em Java/Spring.
- **Não compara padrões nem versões de harness estatisticamente.** Dentro de um
  lote há pares; entre lotes, não. Dizer que o harness ajuda mais num padrão, ou
  que a v2 é melhor que a v1, é observação.
- **Não mede o harness fora do Claude Code.** Os modelos rodam no Claude Code como
  ele vem, que já traz as suas próprias orientações. O efeito medido é o do
  harness **somado** a essa base.
