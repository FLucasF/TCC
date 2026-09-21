---
tags: [tcc, experimento, avaliacao, rubrica]
confidencial: true
criado: 2026-09-20
---

# Rubrica de Strategy

> [!danger] Nunca entra no container
> Vale o mesmo que para o `gabarito-avaliador.md`. O `.dockerignore` é lista
> branca e já barra `avaliacao/` do contexto de build.

Instrumento do **desfecho primário**. A escala em si está na §14.4 do
`docs/plano.md` e as regras de aceitação na §14.4a; aqui ela vira formulário,
com os critérios instanciados por ponto e com **âncoras de código real**.

## Por que âncoras

Sem elas, o C1 nível 2 diz *"abstração clara cobrindo os comportamentos que
variam"*. Isso é prosa: você lê de um jeito, o professor lê de outro, e o kappa
de Cohen da §14.6 sai baixo — que é justamente o número que sustenta a
afirmação de que a avaliação foi confiável. Com âncora, avaliar deixa de ser
julgar e passa a ser comparar contra um caso conhecido.

> [!important] De onde vêm as âncoras, e por que isso é legítimo
> Todas saíram das execuções `FUMACA-` e `MED-`, que são teste de
> infraestrutura e calibração e estão **declaradamente fora da análise**. Usar o
> que elas produziram para ancorar a escala é legítimo; usar o lote seria
> escolher a régua depois do resultado.
>
> Esta rubrica foi fechada em 20/09/2026, **antes** de qualquer pacote do lote
> ser pontuado, e entra no pré-registro.

---

## 1. Como aplicar

1. **Às cegas.** O avaliador recebe o pacote anonimizado, sem `CLAUDE.md`, sem
   `.claude/`, sem `meta.json` e sem o id da run.
2. **Três vezes por pacote**, uma para cada ponto: P1, P2, P3.
3. **Rubrica antes do teste de extensão.** Fazer a extensão primeiro influencia
   a nota de C5 (§14.5 do plano).
4. Uma linha por **pacote × ponto** no formulário da seção 6.
5. Divergência entre avaliadores vai para `consenso.csv` com justificativa.

---

## 2. O que varia em cada ponto

O C1 fala em "os comportamentos que variam". Aqui isso vira explícito. Sem esta
tabela, o nível 1 — *"cobre só parte dos comportamentos"* — não tem como ser
aplicado.

| ponto | variantes | comportamentos que variam |
|---|---|---|
| **P1 · Entrega** | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY` | **custo** do frete · **prazo** em dias · **disponibilidade** (motoboy até 5 kg) |
| **P2 · Cupons** | `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` | **elegibilidade** (MENOS50 a partir de R$ 300) · **cálculo do desconto**, e sobre o quê ele incide: produtos, frete ou itens |
| **P3 · Pagamento** | `PIX`, `CARTAO`, `BOLETO` | **ajuste do total** · **parcelas permitidas** · **cálculo da parcela** · **disponibilidade** (boleto até R$ 1.000) |

Um pacote que abstraia só o custo do frete, deixando prazo e disponibilidade
decididos por condicional, cobre **um de três** comportamentos em P1: C1 = 1.

---

## 3. As seis formas observadas

Varredura das 24 execuções arquivadas. Serve de catálogo: quase todo pacote cai
numa destas, e o rótulo vai no campo de observação.

| forma | o que é | onde aparece |
|---|---|---|
| **classes** | uma classe por variante, atrás de uma interface | 10 execuções |
| **enum+corpo** | `enum` em que cada constante sobrescreve métodos | 6 execuções |
| **mapa-dados** | `Map<String, Config>` de dados, cálculo genérico | 5 ocorrências |
| **regra parametrizada** | tipo de regra (`PERCENTUAL`, `FIXO`…) + `switch` no tipo | P2 de `MED-01-HAIKU-SEM` |
| **enum-simples** | `enum` que só nomeia as variantes, sem comportamento | 3 ocorrências |
| **switch / ifs** | condicional por identidade com a lógica dentro | o resto |

> [!note] "enum-simples" não é abstração do que varia
> `enum FormaPagamento { PIX, CARTAO, BOLETO; }` abstrai a **identidade** da
> variante, não o **comportamento** dela. Vale como tipo, não como Strategy.

---

## 4. Os critérios, com âncoras

Cada critério recebe **0 (ausente)**, **1 (parcial)** ou **2 (correto)**.

Os quatro critérios com âncora — C1, C2, C3 e C5 — são os que movem a
classificação derivada e são onde a divergência entre avaliadores acontece. C4 e
C6 ficam em prosa.

### C1 · Abstração da variação

> Existe uma abstração para o comportamento que varia, e ela cobre os
> comportamentos listados na seção 2?

**0 — não há abstração para o comportamento que varia**

`MED-07-VAZIO-HAIKU-SEM`, P1. Tudo dentro de `CheckoutService`, com o resultado
montado inline:

```java
return switch (modalidade) {
    case "ECONOMICA" -> { ... }
    case "EXPRESSA" -> { ... }
    case "RETIRADA_LOJA" -> new ShippingInfo(BigDecimal.ZERO, 1);
    case "MOTOBOY" -> new ShippingInfo(new BigDecimal("18.00"), 0);
};
```

**1 — a abstração existe, mas cobre só parte dos comportamentos, ou só os dados**

`MED-01-HAIKU-SEM`, P2. A classe `Cupom` guarda tipo, valor e mínimo — dados —
e o cálculo volta para o serviço:

```java
CUPONS.put("MENOS50", new Cupom("MENOS50", TipoCupom.FIXO,
                                new BigDecimal("50.00"), new BigDecimal("300.00")));
...
// no CheckoutService:
switch (cupom.getTipo()) { case PERCENTUAL: ... }
```

Mesma execução, P3, outra maneira de chegar em 1:

```java
public enum FormaPagamento { PIX, CARTAO, BOLETO; }   // nomeia, não abstrai
```

**2 — abstração clara cobrindo os comportamentos que variam**

`MED-07-VAZIO-OPUS-SEM`, P2. Uma interface que cobre elegibilidade e cálculo:

```java
public interface Cupom {
    String codigo();
    default boolean aplicavel(ContextoCupom contexto) { return true; }
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
```

`MED-05-HAIKU-COM`, P1, pela forma `enum+corpo` — cobre custo, prazo e limite:

```java
MOTOBOY(18.00, 0.00, 0) {
    @Override public Double calcularFrete(Double pesoKg) { return 18.00; }
    @Override public boolean temLimitacao() { return true; }
    @Override public Double getLimitePeso() { return 5.0; }
};
```

### C2 · Uma implementação por variação

> Cada variante tem o próprio lugar, ou a lógica de todas mora junta?

**0 — lógica de todas as variações num lugar só**

`MED-07-VAZIO-HAIKU-SEM`: sete arquivos no total, e `CheckoutService` com os
três pontos dentro.

**1 — algumas variações isoladas, outras não**

**Sem âncora nas 24 execuções.** Nenhum pacote isolou parte das variantes de um
mesmo ponto e deixou as outras soltas — a decisão aparece sempre como tudo ou
nada dentro do ponto. Aplicar pela definição, e registrar o caso em observações
se aparecer no lote, para virar âncora numa versão futura.

**2 — cada variação em sua própria implementação**

`MED-07-VAZIO-OPUS-SEM`, P2: `CupomBemvindo10`, `CupomMenos50`,
`CupomFreteGratis`, `CupomLeve3Pague2`, cada uma num arquivo.

> Pela §14.4a, `enum` com corpo por constante **também** é 2 aqui: a JLS compila
> cada constante com corpo numa subclasse anônima, então cada variante tem o
> próprio lugar de fato.

### C3 · Contexto sem condicional por variação

> O serviço delega, ou ainda decide por variante?

**Critério mecânico:** se o comportamento de alguma variante exige condicional
sobre a **identidade** dela fora da seleção, o teto é 1.

**0 — condicional por variante decidindo o comportamento, no serviço**

`MED-01-HAIKU-SEM`, P3: `switch (formaPagamento)` duas vezes dentro do
`CheckoutService`, nas linhas 210 e 269 — a mesma decisão repetida em pontos
diferentes.

**1 — condicional reduzida, mas ainda presente fora da seleção**

`FUMACA-01`, P2. O `enum Cupom` tem corpo por constante, e mesmo assim uma
variante vazou de volta:

```java
// ResumoCheckoutService.java:35
if (cupom == Cupom.LEVE3PAGUE2) { ... }
```

`MED-05-HAIKU-SEM`, P1, pelo outro caminho: `Map<String, EntregaConfig>` de
dados, com os casos que não cabem na tabela voltando como identidade:

```java
if ("RETIRADA_LOJA".equals(modalidadeEntrega)) { return arredondar(BigDecimal.ZERO); }
if ("FRETEGRATIS".equals(modalidadeEntrega))   { return arredondar(BigDecimal.ZERO); }
```

**2 — o serviço apenas delega**

`MED-05-HAIKU-COM`, P1: zero condicional por modalidade no serviço, que fica com
166 linhas contra 332 do braço `SEM` da mesma rodada.

### C4 · Seleção da variação

Sem âncora, por escolha: raramente é onde os avaliadores divergem.

- **0** — seleção espalhada ou duplicada em mais de um lugar
- **1** — seleção centralizada, mas com condicional manual
- **2** — seleção sem condicional: `Map` injetado, registro por anotação, ou
  `enum` com comportamento

Exemplo de 2, para calibrar: `MED-07-VAZIO-OPUS-SEM` injeta `List<Cupom>` no
construtor de `Cupons` e monta o `Map` por `codigo()` — cupom novo se registra
sozinho, sem tocar na seleção.

### C5 · Aberto para extensão

> Quantos arquivos **existentes** precisam mudar para acrescentar uma variante?

É o critério que o teste de extensão da §14.5 confirma com número. A regra é de
contagem, não de opinião.

A contagem sai mecânica do procedimento em `testes-extensao/README.md`.

**0 — a variante nova exige alterar várias classes existentes**

Forma `switch`: acrescentar `DRONE` obriga a mexer no cálculo do frete, na
validação da modalidade, no prazo e na disponibilidade — lugares diferentes.

**1 — exige alterar 1 classe existente, além de criar a nova**

Forma `enum+corpo`, `MED-05-HAIKU-COM`. A constante nova entra **dentro** do
`enum`, que é arquivo existente. É o que a §14.4a decidiu, e é a diferença que
separa esta forma da seguinte.

**2 — exige apenas criar uma implementação, e no máximo um registro declarativo**

`MED-07-VAZIO-OPUS-SEM`: a classe nova com `@Component` se registra sozinha,
zero arquivos existentes alterados. O comentário na interface diz isso em voz
alta: *"Cupom novo = classe nova anotada com @Component implementando esta
interface."*

> [!warning] A extensão escolhida decide parte da resposta
> `DEZOFF` é R$ 10,00 fixo nos produtos, sem condição — a **mesma família** de
> regra do `MENOS50`. Num desenho de regra parametrizada, ele entra como uma
> linha de dado e pontua C5 = 2 sem nenhuma classe nova. O mesmo vale para
> `DRONE` num `Map` de tarifas.
>
> Isso não é defeito do pacote nem da rubrica: é o desenho parametrizado sendo
> genuinamente bom para variante da mesma família. Mas significa que as três
> extensões de hoje **não** distinguem "parametrizado" de "uma classe por
> variante", e distinguem as duas de `enum+corpo` e de `switch`.
>
> **Decidido em 20/09/2026: aceitar o empate e declarar.** O motivo é que ele
> está certo pela própria definição de C5 — um desenho parametrizado que absorve
> a variante como dado faz **menos** que "criar uma implementação", não mais. A
> segunda extensão por ponto, de família nova, ficou como trabalho futuro:
> dobraria o trabalho manual do avaliador, de 54 aplicações para 108. Ver
> `avaliacao/testes-extensao/README.md`.

### C6 · Coesão das implementações

Sem âncora, por escolha.

- **0** — implementações acessam HTTP, DTO de request, ou montam resposta de erro da API
- **1** — pequenos vazamentos de responsabilidade
- **2** — contêm apenas a regra de negócio da variação

---

## 5. Classificação derivada

Derivada por regra, **não opinativa**:

| classe | regra |
|---|---|
| ✅ **Strategy correto** | C1, C2, C3 = 2 **e** total ≥ 10 |
| 🟡 **Strategy parcial** | C1 ≥ 1 **e** C2 ≥ 1, sem atingir "correto" |
| ❌ **Sem Strategy** | C1 = 0 **ou** C2 = 0 |

Total por ponto: 0 a 12. Por pacote: 0 a 36. Métrica derivada por execução:
**quantos pontos (0–3) estão com Strategy correto**.

> [!note] Consequência da regra de C5 para `enum+corpo`
> Um pacote em `enum+corpo` bem feito chega a C1=2, C2=2, C3=2, C4=2, C5=1,
> C6=2 → **total 11**, e classifica como **Strategy correto**. A diferença para
> a forma por classes (12) aparece na pontuação e no teste de extensão, não na
> classificação. É intencional: os dois **são** Strategy, e um é mais aberto que
> o outro.

---

## 6. Formulário

Uma linha por pacote × ponto.

```csv
codigo_cego,ponto,C1,C2,C3,C4,C5,C6,total,classe,forma,outro_padrao,excesso_engenharia,condicional,implementacao_p2,observacoes
```

| coluna | valores |
|---|---|
| `ponto` | `P1`, `P2`, `P3` |
| `C1`–`C6` | `0`, `1`, `2` |
| `total` | soma de C1 a C6 |
| `classe` | `correto`, `parcial`, `sem` |
| `forma` | `classes`, `enum+corpo`, `mapa-dados`, `regra-parametrizada`, `enum-simples`, `switch`, `ifs`, `outra` |
| `outro_padrao` | nome do padrão usado no lugar, se houver |
| `excesso_engenharia` | `sim`/`nao` — padrões desnecessários empilhados |
| `condicional` | só em P3: `concentrada` (1 lugar) ou `espalhada` |
| `implementacao_p2` | só em P2: `por-cupom` ou `regra-generica` |
| `observacoes` | inclusive comentários do código citando regra ou orientação de projeto |

---

## 7. O que esta rubrica ainda não tem

> [!warning] Testada em 21/09/2026, e não decidiu em três pontos
> A régua foi aplicada aos pacotes `FUMACA-03-OPUS-COM` e `FUMACA-03-HAIKU-SEM`,
> os dois fora da análise. A maior parte dos julgamentos decidiu sozinha — o
> C5=1 do `enum`, o `if` sobre parâmetro que não conta como condicional por
> variação, e os dois pontos do segundo pacote que são cadeia de `if` por código.
>
> Em **três** foi preciso escolher uma leitura para conseguir continuar. As três
> estão na tabela abaixo, **sem decisão**, por escolha de 21/09/2026: primeiro
> gerar dado, decidir depois.
>
> **Elas precisam ser fechadas antes de o primeiro pacote do lote ser aberto.**
> Depois disso, escolher a leitura vira escolher olhando o resultado.

| lacuna | o que fazer |
|---|---|
| **C1 — comportamento como dado, não como método** | O nível 1 diz "cobre só parte dos comportamentos, **ou só os dados**", e não diz o que fazer quando *parte* dos comportamentos é método e outra é dado por variante que a determina sozinha — o `prazoDias` no construtor de um `enum`. **Em aberto** |
| **C2 — tabela de dados com cálculo genérico** | Uma linha de tabela é "uma implementação por variação"? A âncora da Forma 2 fixa C1=1 e C3≤1 e **é silenciosa sobre C2** — e como a classificação derivada usa `C2=0`, a resposta **inverte o resultado** entre "parcial" e "sem Strategy". **Em aberto** |
| **C6 — quando não existe implementação** | Os três níveis falam de "implementações". Se está tudo num serviço único, não há o que pontuar, e o C6 entra no total 0–12. **Em aberto** |
| Âncora para **C2 = 1** | Nenhuma execução produziu isolamento parcial dentro de um ponto. Registrar se aparecer no lote |
| C4 e C6 sem âncora | Por escolha. Se o kappa vier baixo nesses dois, ancorar antes do consenso |
| Extensão que separe parametrizado de por-classe | Decidido: aceitar o empate, ver C5. Trabalho futuro |
| Calibração dos dois avaliadores | §14.6: pontuar juntos 1 ou 2 pacotes das execuções de medição, nunca do lote |
