# Régua de leitura

> **RASCUNHO, versão 4 (enxuta)**, de 09/10/2026. Substitui a versão 3 (8
> propriedades, lida inteira pelo Claude e pelo Lucas), que está no histórico do git;
> o porquê está no [`DECISOES.md`](../DECISOES.md), §6. Só vale depois de congelada:
> commit e hash no README, **antes** de qualquer leitura do V4.
>
> A régua lê **desenho**, não correção: um erro de regra de negócio não muda nenhuma
> resposta (no máximo vai para `observacao`). A correção é medida pela suíte.

## 0. Quem lê o quê

| quem | o que lê | em quantos pacotes |
|---|---|---|
| **o Semgrep** (`evaluation/tools/semgrep/`) | as perguntas desta régua, do P1 ao P5, por regras fixas | **todos** |
| **o Lucas** | as 4 perguntas do P4 e do P5, à mão, às cegas | **20**, um por modelo × nível, sorteados (`evaluation/tools/sample.mjs`) |

A leitura do Lucas **confere** o Semgrep: se os dois concordarem em menos de 18 de 20
numa pergunta, aquela pergunta vira descritiva (`evaluation/tools/compare.mjs`). Por
isso as definições abaixo são as mesmas que as regras do Semgrep implementam.

## 1. Antes de ler

- O pacote é lido **sem saber o nível nem o modelo**. A cópia que o Lucas recebe vem sem
  comentários e sem README (`anonymize.mjs --sem-comentarios`), com as linhas no mesmo
  lugar do original. O mapa só é aberto depois que a planilha estiver commitada.
- Só conta `src/main`, e só o código que alguma coisa chama.
- Comece pela busca dos nomes dos casos do gabarito (`evaluation/strategy/gabarito.md`).
  Ela traz **ruído**, que se descarta:
  - **homônimos de outro ponto:** o `freteGratis()` do clube aparece na busca do cupom
    `FRETEGRATIS`, e não conta para o cupom;
  - **pedaços de palavra:** a busca por `nivel` acha "dispo**nivel**";
  - **comentários** (a cópia cega já vem sem eles).

**Valor** é a palavra que vai na célula da planilha. Cada pergunta tem uma lista fechada
de valores; escolhe-se um.

**Nomear um caso** é escrever qual ele é: o texto `"OURO"`, a constante `OURO`, a classe
`NivelOuro`, `instanceof NivelOuro`, `case OURO ->`. O teste: *se eu criar um caso
parecido, esta linha precisa mudar?* Se precisa, ela nomeia. `nivel.freteGratis()` não
nomeia: pergunta a qualquer nível, e o novo responde sozinho.

**Unidade** é uma classe, uma constante de enum (com ou sem corpo), uma entrada de mapa
ou uma função.

**Na dúvida entre dois valores, não escolha o mais provável:** escreva `indeterminado` e
a dúvida na coluna `duvida`. Na conferência, `indeterminado` conta como discordância.

## 2. P4, o clube: duas perguntas

### 2.1 `localizacao`: o que é de um nível aparece fora dele?

| valor | quando |
|---|---|
| `isolado` | todo o código **específico** de cada nível está numa unidade só dele |
| `espalhado` | algum nível tem código específico **também** em outra unidade, **inclusive** um `if` que o nomeia numa validação ou na escolha do código de erro |

**Se o nível não tem unidade própria** (os níveis só existem como texto ou como
constantes de um enum sem corpo, comparados em métodos de todos), qualquer código
específico de um nível está, por definição, fora da casa dele: é `espalhado`. Ser texto
não decide nada por si: um mapa de texto para uma classe por nível é `isolado`.

**Não conta como código do nível fora dele:**
- o nome usado só como chave de registro ou numa **lista de válidos** (um bloco que cita
  todos os níveis só para conferir se o código existe);
- uma constante de um nível declarada fora dele;
- uma função genérica que não cita o nível, mesmo que só um nível a use (um método
  `default` de interface, uma classe-base).

Como conferir: busque os nomes dos níveis e também o que eles fazem (`credito`, `frete`,
`brinde`). Para cada linha fora da unidade do nível, pergunte: *ela faz alguma coisa só
para este nível?*

### 2.2 `selecao`: o caminho do texto até a conta nomeia níveis?

| valor | quando |
|---|---|
| `consulta` | o nível é achado **sem nomear** nenhum: um `Map`, beans injetados por código, `valueOf` de um enum que carrega o comportamento ou os dados, um laço genérico. **Um `if` que só recusa o pedido logo depois (valida, mas não calcula) não muda isto.** |
| `condicional-unica` | um `switch`/`if` que nomeia os níveis **num lugar só** e **devolve um objeto** para outro calcular (uma fábrica) |
| `condicional-no-calculo` | um `switch`/`if` que nomeia os níveis e **faz a conta** ali. Vale também quando o nível foi achado por consulta, mas a conta de algum nível é feita por um `if` que o nomeia (o `if` passou por cima da estrutura) |

Um `enum` que é só rótulo, com a conta num `switch` que nomeia os níveis, é
`condicional-no-calculo`. Um `switch (this)` ou `if (this == OURO)` dentro de um método
do enum que faz a conta também.

## 3. P5, o seguro por região (controle negativo): duas perguntas

O enunciado diz que só a porcentagem muda. Separar cada região numa peça é o
**exagero** que este ponto mede.

### 3.1 `forma`: que formato a variação tem?

| valor | o que se vê |
|---|---|
| `enum-dados` | um `enum` em que as constantes só carregam valores, e um método só para todas |
| `enum-abstrato` | um `enum` em que alguma constante tem corpo próprio (`NORTE { ... }`) |
| `classes` | uma classe por região, com um tipo comum |
| `mapa` | um `Map` da região para um valor ou para um objeto |
| `switch` | um `switch` ou cadeia de `if` sobre a região |
| `outro` | nenhum dos acima; descrever em `observacao` |

### 3.2 `proporcao`: a resposta tem o tamanho do problema?

| valor | quando |
|---|---|
| `dados` | a variação como dado: um `enum` que só carrega números, um mapa de números, uma tabela |
| `condicional` | um `switch`/`if` que só **devolve o número** de cada região |
| `estrutura` | uma classe por região, uma constante de enum **com corpo**, ou um mapa ou `switch` que devolve um **objeto** por região |

## 4. A ficha (aplicada depois, por conta)

| ponto | acerto | erro | exagero |
|---|---|---|---|
| positivo (P1 a P4) | `isolado` **e** `consulta` ou `condicional-unica` | `espalhado`, ou `condicional-no-calculo` | — |
| controle negativo (P5) | `dados` ou `condicional` | — | `estrutura` |

A **nota** de 0 a 100 (`evaluation/tools/nota.mjs`) dá 10 por ponto positivo com as
duas respostas certas, 5 com uma, 0 com nenhuma, e 10 ao P5 sem exagero; ela é resumo, e
as hipóteses são lidas nas respostas separadas.

## 5. A planilha

Gerada em branco pelo `sample.mjs`, uma linha por pacote:

```
leitor,blind_code,P4_localizacao,P4_selecao,P5_forma,P5_proporcao,evidencia,duvida,achei_que_sabia_nivel,observacao
```

- `evidencia`: o `arquivo:linha` de cada resposta, na ordem das colunas, separados por `;`.
  O que vai em cada uma:

  | valor | a evidência |
  |---|---|
  | `isolado` | a casa de um nível (a classe ou a constante dele) **e** as linhas de fora que usam o nível sem nomear |
  | `espalhado` | a linha de fora que faz algo só para um nível |
  | `consulta` | a linha onde o nível é achado sem nomear (o `Map.get`, o `valueOf`, o catálogo) |
  | `condicional-unica`, `condicional-no-calculo` | a linha do `switch`/`if` que nomeia o nível |
  | `forma` e `proporcao` | a linha onde a variação por região está escrita |
- `achei_que_sabia_nivel`: vazio, ou o nível e o motivo (por exemplo, "N2 ou N3: a
  classe se chama EstrategiaClube"). Se o leitor acertar o nível muitas vezes, o
  cegamento vazou, e isso vai para as limitações.

A releitura (`sample.mjs releitura`) usa a mesma planilha, numa linha por pacote relido.
