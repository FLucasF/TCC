# Guia da régua: o que você responde em cada pacote

> Para o Lucas, junto com a [`regua.md`](regua.md) (versão 4, enxuta). Explica a régua
> em linguagem de leitura, e não a substitui: quando os dois divergirem, vale a régua.
> Reescrito em 09/10/2026 para a régua enxuta; a versão anterior (8 propriedades) está
> no histórico do git. Os exemplos de código "errado" são **inventados**.

---

## 1. O que você está fazendo

O experimento quer saber se o harness muda **o desenho** do código. "Desenho" aqui é
uma pergunta só:

> **Quando o negócio tem vários casos de uma mesma coisa, o código separa cada caso no
> seu canto, ou mistura todos com `if`?**

Quem responde isso nos 100 pacotes é o **Semgrep**, com regras fixas. **Você confere o
Semgrep**: lê 20 pacotes sorteados, às cegas, e responde às mesmas perguntas. Depois um
script compara. Se vocês dois concordarem em pelo menos 18 de 20 numa pergunta, a
resposta do Semgrep vale para os 100; se não, aquela pergunta fica só com a sua amostra.

Você **não julga se o código é bom**. Só responde o que vê, com a prova (`arquivo:linha`).

Em cada pacote, são **4 respostas**, em dois pontos:

| ponto | o que é | as suas perguntas |
|---|---|---|
| **P4, o clube** (`BRONZE`, `PRATA`, `OURO`) | o ponto em que separar os casos faz diferença | `localizacao`, `selecao` |
| **P5, o seguro por região** | o ponto em que separar seria **exagero**, porque só a porcentagem muda | `forma`, `proporcao` |

---

## 2. As palavras

| palavra | o que quer dizer |
|---|---|
| **caso** | uma das opções de um ponto: `OURO`, `NORTE` |
| **unidade** | uma peça de código: uma classe, uma constante de enum, uma entrada de mapa, uma função |
| **valor** | a **palavra** que vai na célula da planilha; cada pergunta tem uma lista fechada |
| **nomear o caso** | escrever qual caso é: `"OURO"`, `OURO`, `NivelOuro`, `instanceof NivelOuro`, `case OURO ->` |

**O teste para "nomear":** *se eu criar um nível novo parecido com este, esta linha
precisa mudar?*

- `if (nivel.codigo().equals("OURO")) frete = ZERO;` → **precisa** (um Diamante sem
  frete teria de entrar aqui) → **nomeia**.
- `frete = nivel.freteGratis() ? ZERO : ...;` → **não precisa** (o Diamante responde
  `true` sozinho) → **não nomeia**.

---

## 3. O roteiro de cada pacote

1. **Abra o pacote** (a cópia cega: sem comentários, sem README; as linhas estão no
   mesmo lugar do original).
2. **Busque os nomes do clube** e leia as linhas que aparecerem:
   ```bash
   grep -rnE "BRONZE|PRATA|OURO|Bronze|Prata|Ouro" <pacote>/src/main
   ```
3. **Busque também o que o clube faz**, porque um remendo pode não escrever o nome:
   ```bash
   grep -rniE "credito|fretegratis|brinde" <pacote>/src/main
   ```
4. **Limpe o ruído** (§6) e responda à `localizacao` e à `selecao`.
5. **Busque as regiões** e responda à `forma` e à `proporcao`:
   ```bash
   grep -rnE "SUDESTE|NORDESTE|NORTE|Norte|Sudeste" <pacote>/src/main
   ```
6. **Na dúvida, `indeterminado`**, e a dúvida escrita. Não é errar: conta como
   discordância, e a dúvida mostra onde a régua não está clara.
7. **Achou que sabia o nível?** Escreva na coluna `achei_que_sabia_nivel` (por exemplo,
   "N2 ou N3: a classe se chama EstrategiaClube").

---

## 4. P4, o clube

### `localizacao`: o que é de um nível aparece fora dele?

| valor | quando |
|---|---|
| `isolado` | o que é próprio de cada nível (a porcentagem do crédito, o frete grátis do Ouro, o brinde do Ouro) está **só** na unidade dele |
| `espalhado` | algum nível tem código próprio **também** em outro lugar, **inclusive** um `if` que o nomeia só para recusar ou escolher um código de erro |

**Não conta:** o nome só como chave de registro; uma **lista de válidos** (um bloco que
cita todos os níveis só para conferir se o código existe); uma constante de um nível
declarada fora dele; uma função genérica que não cita o nível.

**Exemplo inventado de `espalhado`:**
```java
// no serviço, e não na classe do Ouro
if (nivel instanceof NivelOuro) return BigDecimal.ZERO;
```

### `selecao`: o caminho do texto até a conta nomeia níveis?

Chega o texto `"OURO"` do site. Como o código chega no comportamento do Ouro?

| valor | o que você vê |
|---|---|
| `consulta` | busca num `Map`, beans do Spring, `valueOf` de um enum que carrega o comportamento ou os dados, um laço genérico. **Um `if` que só recusa logo depois não muda isto** |
| `condicional-unica` | um `switch`/`if` **num lugar só** que **devolve um objeto**: `case "OURO" -> new NivelOuro();` |
| `condicional-no-calculo` | um `switch`/`if` que nomeia o nível e **faz a conta**: `case "OURO" -> subtotal * 0.05;`. Vale também quando o nível foi achado por consulta, mas a conta de algum nível é feita por um `if` que o nomeia |

**Exemplo inventado de `condicional-no-calculo` escondido:**
```java
enum NivelClube { BRONZE(0), PRATA(0.02), OURO(0.05); ... }   // achado por valueOf...
// ...mas no serviço:
if (nivel != NivelClube.OURO) return false;                   // o brinde é feito por um if
return subtotal.compareTo(QUINHENTOS) > 0;
```

---

## 5. P5, o seguro por região

### `forma`: que formato a variação tem?

`enum-dados` (constantes só com números) · `enum-abstrato` (alguma constante com corpo
`{ ... }`) · `classes` (uma classe por região) · `mapa` · `switch` · `outro`

### `proporcao`: a resposta tem o tamanho do problema?

| valor | exemplo inventado |
|---|---|
| `dados` | `enum Regiao { NORTE(0.025), SUL(0.01); ... }` ou `Map.of("NORTE", 0.025, ...)` |
| `condicional` | `switch (regiao) { case NORTE -> 0.025; case SUL -> 0.01; ... }` |
| `estrutura` (exagero) | `class SeguroNorte implements Seguro { ... }`, `NORTE { BigDecimal calcular(...) {...} }`, `Map.of("NORTE", new TaxaNorte())` |

**Cuidado:** `NORTE(new BigDecimal("0.025"))` é **dado** (o parêntese de dentro é um
número). Só é estrutura quando a região tem **corpo** `{ ... }` ou vira um **objeto**
próprio.

---

## 6. As armadilhas da busca

| armadilha | exemplo | o que fazer |
|---|---|---|
| homônimo de outro ponto | o `freteGratis()` do clube na busca do cupom `FRETEGRATIS` | a linha é de outro ponto; não conta |
| pedaço de palavra | `nivel` acha "dispo**nivel**" | olhe a palavra inteira |
| `if` de validação | `if (!pagamento.disponivel(...)) throw` logo depois da busca | não muda a `selecao`; se nomear o nível, entra na `localizacao` |
| lista de válidos | `!c.equals("BRONZE") && !c.equals("PRATA") && !c.equals("OURO")` | não conta |
| campos da entrada e da saída | `nivelClube` no request, `brinde` na resposta | é onde o dado entra e sai, não regra |

---

## 7. Cola de bolso

| pergunta | em uma linha | valores |
|---|---|---|
| P4 `localizacao` | o que é de um nível aparece fora dele? | `isolado`, `espalhado` |
| P4 `selecao` | o caminho do texto até a conta nomeia níveis? e faz a conta? | `consulta`, `condicional-unica`, `condicional-no-calculo` |
| P5 `forma` | que formato a variação por região tem? | `enum-dados`, `enum-abstrato`, `classes`, `mapa`, `switch`, `outro` |
| P5 `proporcao` | a resposta tem o tamanho do problema? | `dados`, `condicional`, `estrutura` |

Em qualquer uma: **não sabe → `indeterminado` + a dúvida escrita.**
