# Régua de leitura

> **RASCUNHO, versão 3**, depois das rodadas 1 e 2 de calibração
> ([relatório](calibracao-relatorio.md)). Só vale depois de congelada: commit e hash
> no README, **antes** de qualquer leitura do experimento.
>
> A régua lê **desenho**, não correção: um erro de regra de negócio (um limite no
> valor errado, uma conta que não fecha) não muda nenhuma propriedade; no máximo vai
> para `observacao`. Correção é medida pela suíte de aceitação.

Diz como um pacote de código é lido para responder às hipóteses de desenho do
[`OBJETIVO.md`](../OBJETIVO.md). Tem três níveis:

| nível | onde está | vale para |
|---|---|---|
| **1. Propriedades**: o que se observa no código | aqui, §2 | todo padrão, todo enunciado |
| **2. Ficha do padrão**: o que é acerto e o que é exagero | aqui, §3 | todo enunciado daquele padrão |
| **3. Gabarito do enunciado**: os pontos e os casos de cada um | `evaluation/<padrao>/gabarito.md`, junto dos pacotes | só aquele enunciado |

Quem lê **não julga** se "é Strategy". Registra o que vê, propriedade por
propriedade, com evidência. Quem transforma isso em acerto é a ficha, aplicada
depois, por conta.

---

## 1. Antes de ler

- O pacote é lido **sem saber o braço**. O mapa de anonimização não é aberto
  até a planilha estar commitada.
- Leia o gabarito antes do primeiro pacote. Ele lista, para cada ponto, os
  identificadores dos casos (`OURO`, `MOTOBOY`...), que são o ponto de partida
  da busca. O identificador conta onde aparecer: constante de enum, literal de
  texto (`"OURO"` num método `codigo()`), ou nome de classe que designa o caso
  (`NivelOuro`, `PagamentoPix`, e portanto também `instanceof PagamentoPix`).
- **Só o código de produção conta** (`src/main`), e só o que é **alcançável**: um
  método que nada chama não entra na leitura (confira os chamadores antes de ler
  um método). Um ramo dentro de um método alcançável conta, mesmo que na prática
  nunca seja executado. Os testes e a documentação (README) não entram, a não ser
  em `pista_condicao`.
- **Na dúvida entre dois valores, não escolha o mais provável:** deixe
  `indeterminado` e escreva a dúvida na coluna `duvida`, citando a seção da régua.

## 2. Nível 1: propriedades

Cada ponto de variação do gabarito, em cada pacote, recebe os valores abaixo.
**Todo valor tem evidência `arquivo:linha`**; sem evidência, o valor é
`indeterminado`. O valor `n/a` dispensa evidência.

Uma **unidade** é uma classe, uma constante de enum (com ou sem corpo), uma
entrada de mapa ou uma função. Duas funções no mesmo arquivo são duas unidades.

### 2.1 `forma`: descritiva, não pontua

| valor | o que se vê |
|---|---|
| `classes` | uma classe por caso, com um tipo comum (interface ou classe abstrata), qualquer que seja o jeito de escolher a classe (mapa, injeção, `switch`) |
| `enum-abstrato` | um `enum` em que ao menos um comportamento está no corpo de cada constante, mesmo que outros venham de dados no construtor |
| `enum-dados` | um `enum` em que as constantes só carregam valores, e o método é um só para todas (dentro do enum ou fora dele, num serviço genérico), mesmo que esse método tenha um `if (this == X)` (isso vai para `selecao`) |
| `mapa` | um `Map` do caso para um valor ou para uma função |
| `switch` | um `switch` ou uma cadeia de `if` sobre o caso, com o comportamento dentro dos ramos ou em funções chamadas pelos ramos, uma por caso |
| `outro` | nenhum dos acima; descrever em `observacao` |

### 2.2 `localizacao`: onde mora o comportamento de cada caso

| valor | regra |
|---|---|
| `isolado` | todo o código **específico** de cada caso está numa **unidade** só dele, e nenhuma outra unidade tem código que só vale para aquele caso |
| `espalhado` | algum caso tem código específico em mais de uma unidade. Exemplo: `NivelOuro` calcula o crédito, e `CalculadoraFrete` tem `if (nivel == OURO)` |

Como verificar: busque o identificador de cada caso em `src/main`. Se um caso
aparece **com comportamento** em mais de uma unidade, é `espalhado`. Não contam
como comportamento:
- aparecer só como nome (numa lista de validação, num `valueOf`, como chave de
  registro), ou numa comparação cujo resultado não é usado;
- uma constante de um caso declarada fora do corpo dele (`PESO_MAXIMO_MOTOBOY`):
  é dado, não comportamento;
- uma função auxiliar **genérica e parametrizada**, que não cita o caso, mesmo
  que só um caso a use (`unidadesGratis(aCada)`); vale igual para uma classe-base
  abstrata (`FretePorPeso`) e para um método `default` de interface;
- um `if` sobre o caso numa validação ou na escolha do código de erro **conta**:
  é comportamento do caso fora da unidade dele (`espalhado`).

### 2.3 `selecao`: como o caso é achado

Avalia o mecanismo que leva do dado de entrada **ao comportamento** do caso: como
o programa decide **qual conta fazer**.

| valor | regra |
|---|---|
| `consulta` | o comportamento é achado sem nomear caso nenhum: `Map.get`, injeção de beans por chave, `Enum.valueOf` **quando a constante achada carrega o comportamento ou os dados de que uma conta genérica precisa**, ou **busca genérica** (laço ou `stream` sobre os valores comparando o nome; um `Map` montado a partir de `values()`) |
| `condicional-unica` | um `switch` ou cadeia de `if` que **nomeia os casos**, **num lugar só**, e cujos ramos **devolvem um objeto** (uma estratégia) para outro chamar: uma fábrica |
| `condicional-no-calculo` | um `if`/`switch` que **nomeia os casos** e cujos ramos **fazem a conta**, diretamente ou chamando uma função por caso que devolve o resultado |

Três situações que confundiam:

- **Enum que é só rótulo.** Se o `enum` não carrega comportamento e a conta vem
  de um `switch` que nomeia os casos, o mecanismo é esse `switch`
  (`condicional-no-calculo`), e não o `valueOf`.
- **`if` que passa por cima da estrutura.** Se o caso é achado por consulta, mas
  a conta de algum caso é feita por um `if` que o nomeia, no lugar do que a
  estrutura faria, vale `condicional-no-calculo`. A escolha de qual conta fazer
  saiu da estrutura.
- **`if` só de validação ou de código de erro.** Um `if` sobre o caso que não faz
  a conta dele (checar disponibilidade, escolher o código de erro, e também um
  método `isMotoboy()` no enum usado só para isso) **não** entra em `selecao`: vai
  para `localizacao` e, se o caso for exigente, para `assinatura`.

Um `switch (this)` ou `if (this == X)` dentro de um método de `enum` que **faz a
conta** é `condicional-no-calculo`. Um teste de nulo (`if (x == null) throw`, ou
"sem cupom") não é condição sobre o caso.

### 2.4 `assinatura`: nos casos marcados como exigentes no gabarito

| valor | regra |
|---|---|
| `comporta` | tudo o que o caso exigente faz passa pelo contrato comum. O contrato pode fazer isso com um método que **calcula** (`frete(freteDaModalidade)`), que **responde** (um booleano ou valor opcional, como `freteGratis()` ou `pesoMaximo`) ou que **valida** (`validarDisponibilidade()` que lança erro), desde que o código de fora só use o resultado **sem nomear caso nenhum** |
| `remendo` | alguma coisa que um caso exigente faz é tratada **fora** do contrato **nomeando o caso**. Exemplo: `if (modalidade == MOTOBOY && peso > 5)` no serviço. Conta mesmo que repita uma verificação que o contrato já faz |
| `n/a` | o ponto não tem caso exigente no gabarito |

Quando a `forma` é `switch` e não existe contrato, o lugar do caso é o `switch`
(ou o método) que calcula os casos; um `if` sobre o caso fora dele é `remendo`.

Num ponto com mais de um caso exigente, `comporta` só se **todos** cabem; basta
um de fora para `remendo`, e a evidência diz qual.

### 2.5 `parte_comum`: o que o gabarito lista como comum ao ponto

O gabarito lista, por ponto, **só regras de negócio** comuns aos casos (uma
fórmula, uma conta). O arredondamento **não** está nessa lista.

| valor | regra |
|---|---|
| `unica` | cada regra comum listada está escrita **uma vez**. Chamar uma função comum em vários lugares **não** é repetição |
| `repetida` | alguma regra comum listada tem a **lógica escrita** (os operadores, a fórmula) em mais de um lugar, copiada ou reescrita de outro jeito em cada caso. Basta **uma** das regras listadas para o valor ser `repetida` |

Uma regra que o gabarito marca como **própria de um caso** (por exemplo, o ajuste do
`CARTAO` derivado da tabela Price) não conta como repetição. Se o gabarito não
lista regra comum para o ponto, o valor é `n/a`.

### 2.6 `custo_caso_novo`: quantos arquivos mudam além do registro do caso

Um **número**: quantos arquivos **existentes** de `src/main` precisariam ser
editados para o **caso hipotético** do gabarito funcionar **certo**, inclusive
devolvendo o código de erro que o gabarito indica para ele.

A regra olha a **edição**, não o arquivo:

- **Não conta** a edição que só **registra** o caso novo onde os casos são
  listados: a constante nova no `enum`, a entrada no mapa ou registro, o `case`
  novo num `switch` de **fábrica** (que devolve objeto). Não conta **qualquer que
  seja o arquivo** em que essa lista mora, inclusive dentro de um serviço. Sem
  `enum` nem registro, a lista de códigos válidos faz o papel de registro.
  Registrar o caso é o custo mínimo de qualquer desenho.
- **Conta** o arquivo que precise de **qualquer outra** edição: um serviço, uma
  validação, uma lista de códigos válidos separada do registro, um `switch` que
  **calcula** (inclusive quando a `forma` é `switch`: ali o `case` novo carrega a
  conta, e não só o registro), uma regra de negócio que o caso novo exige (o
  `VALE` "só à vista" numa validação de parcelas).
- Dois pontos no mesmo arquivo contam **1**.

Na evidência, liste os arquivos. Quando o custo é 0, cite o mecanismo que
dispensa a edição (o registro automático, o `enum` que já é a lista). Arquivo
novo não conta; teste e README não contam.

### 2.7 `proporcao`: só nos pontos marcados como controle negativo

| valor | regra |
|---|---|
| `dados` | a variação como dado: tabela, `Map` de valores, `enum` que só carrega números |
| `condicional` | um `switch` ou `if` que só devolve o valor de cada caso |
| `estrutura` | uma classe, uma estratégia ou uma constante de enum **com corpo** por caso |
| `n/a` | o ponto não é controle negativo |

Nos controles negativos, `localizacao`, `selecao`, `assinatura`, `parte_comum` e
`custo_caso_novo` são `n/a`: só `forma` e `proporcao` são lidas.

### 2.8 Por pacote, fora dos pontos

| coluna | o que registra |
|---|---|
| `arquivos_main` | **não é preenchida por quem lê**: é contada por script (`.java` sob `src/main`) |
| `especulativa` | `nenhuma`, ou a lista (com `arquivo:linha`) de **estrutura** para variação que o enunciado **não** descreve: interface com uma implementação só, fábrica para um caso só, ponto de extensão para algo que o enunciado diz que não varia (moeda). **Não conta:** parâmetro sem uso num contrato (vai para `observacao`), infraestrutura genérica usada por vários pontos (um `Catalogo<T>`), objeto nulo (`SemCupom`) |
| `pista_condicao` | `nenhuma`, ou texto em **qualquer arquivo do pacote** (código, README, `pom.xml`) que cite o harness, o `CLAUDE.md` ou "orientações de projeto". Texto que só descreve como estender (Javadoc, README: "basta escrever uma classe", "onde mexer quando o negócio muda") **não** é pista |
| `duvida` | a dúvida, quando alguma coluna ficou `indeterminado` |

## 3. Nível 2: fichas

### 3.1 Strategy

Aplicada **depois** da leitura, sobre a planilha.

| tipo de ponto (do gabarito) | acerto | erro | exagero |
|---|---|---|---|
| **positivo** | `localizacao = isolado` e `selecao` ∈ {`consulta`, `condicional-unica`} | `espalhado`, ou `condicional-no-calculo` | — |
| **positivo com caso exigente** | o acerto acima **e** `assinatura = comporta` | o erro acima, ou `assinatura = remendo` | — |
| **controle negativo** | `proporcao` ∈ {`dados`, `condicional`} | — | `proporcao = estrutura` |

`parte_comum` e `custo_caso_novo` não entram no acerto: medem *Desenho: não repete o comum* e *Desenho: caso novo com pouca edição* à parte.

| hipótese | de onde sai |
|---|---|
| Desenho: isola cada caso | acerto nos pontos positivos |
| Desenho: comporta o caso exigente | `assinatura` nos casos exigentes, em todos os pontos que os têm |
| Desenho: não repete o comum | `parte_comum` |
| Desenho: caso novo com pouca edição | `custo_caso_novo` |
| Desenho: réplicas mais parecidas | as 3 réplicas de um modelo e braço, comparadas na `forma` e no acerto de cada ponto |
| Exagero: aplica onde não pede | exagero nos controles negativos |
| Exagero: estrutura especulativa | `especulativa`, por pacote |
| Exagero: mais arquivos | `arquivos_main`, por pacote |

### 3.2 State

> **O State saiu do V4 em 07/10** (o material está em `history/state/`). Esta
> ficha fica para quando ele, ou outro padrão de variação, voltar.

**Os mesmos critérios de acerto, erro e exagero da ficha do Strategy (§3.1)**, e
o mesmo mapa de hipóteses. O que muda é só o que o gabarito do State
(`history/state/gabarito.md`) chama de caso: as **situações** do pedido, que
mudam durante a vida do objeto.

Duas observações para quem aplica a ficha:

- Nas ações por situação (E1), uma **tabela de transições** é `consulta`, e conta
  como acerto: as transições, sozinhas, são dado. O que distingue um desenho do
  outro é o ponto seguinte, dos efeitos (E2), que uma tabela não resolve sozinha.
- O erro típico do State é o `switch (situacao)` repetido dentro de cada ação:
  a mesma escolha em vários lugares, `condicional-no-calculo`.

**Ainda não calibrada**: nenhum pacote do State existe. Antes da leitura do lote
`STATE`, a ficha passa por uma calibração sobre uma rodada `SMOKE` dele.

## 4. As planilhas

Ficam em `evaluation/<padrao>/`, junto dos pacotes e do gabarito usados. Uma
leitura dividida entre várias sessões grava as partes em `parts/<leitor>-<n>.csv`,
depois juntadas em `leitura-<leitor>.csv`.

**`leitura-<leitor>.csv`**: uma linha por pacote × ponto.

```
leitor,blind_code,ponto,forma,localizacao,selecao,assinatura,parte_comum,custo_caso_novo,proporcao,evidencia,duvida,observacao
```

Na `evidencia`, cada valor com o seu `arquivo:linha`, separados por `;`, na ordem
das colunas.

**`leitura-<leitor>-pacote.csv`**: uma linha por pacote.

```
leitor,blind_code,especulativa,pista_condicao,duvida,observacao
```

## 5. Decisões

Tomadas por Claude em 26/09/2026, **por delegação do Lucas**, depois da rodada 1
de calibração. Cada uma com o motivo:

1. **Fábrica com `switch` é acerto** (`condicional-unica`). O que o harness quer
   evitar é a condição espalhada pelas contas; a fábrica só escolhe, num lugar só.
2. **No controle negativo, `switch` que devolve o valor é acerto.** O ponto mede
   exagero; só estrutura por caso é o erro.
3. **A assinatura conta em todos os casos exigentes**, não só no P4. Na calibração
   ela discriminou pacotes em P1 a P3 também.
4. **O custo não conta o arquivo que é a estrutura do ponto.** Contando, `enum`
   dava sempre 1 e `classes` sempre 0: media a forma, não o acoplamento.
5. **Parte comum:** chamar uma função comum não é repetição, e o arredondamento
   sai do gabarito. Na rodada 1, 48% das células ficaram `indeterminado` por isso.
6. **Pergunta no contrato, lida sem nomear caso, é `comporta`.** A assinatura
   atende o caso exigente; o remendo típico, nomear o caso fora, não acontece.
7. **Parâmetro sem uso num contrato não é `especulativa`.** A regra 4 do harness
   fala de estrutura; um parâmetro a mais vai para `observacao`.

Depois da rodada 2, também por delegação, em 27/09/2026:

8. **A parte comum do P3 sai do gabarito** (`n/a`). A relação `totalFinal = total
   + ajuste` deu `repetida` em quase todo pacote, porque a resposta exige os dois
   campos; sete dos doze leitores apontaram, cada um por conta própria, que ela
   media o formato do contrato. Ficam P1 e P4, cujas fórmulas discriminam.
9. **Um `if` que faz a conta de um caso no lugar da estrutura é
   `condicional-no-calculo`**, mesmo que o caso tenha sido achado por consulta.
   Três leitores propuseram isso e um o contrário; ficou a leitura mais próxima da
   regra 2 do harness, porque a escolha de qual conta fazer saiu da estrutura.
   `if` só de validação ou de código de erro continua fora de `selecao`.
10. **O custo olha a edição, não o arquivo.** Registrar o caso onde os casos são
    listados nunca conta, qualquer que seja o arquivo; qualquer outra edição conta.
    Resolve a fábrica que mora dentro do serviço e o desenho sem `enum` nem
    registro, que na rodada 2 davam 0 ou 1 conforme o leitor.
11. **Um `switch` que calcula conta no custo, mesmo quando a `forma` é `switch`.**
    Ali o `case` novo carrega a conta, e não só o registro.
