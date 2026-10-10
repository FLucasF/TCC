# Detector do padrão (Semgrep)

Lê o desenho de cada pacote com regras fixas, **sem IA**: o mesmo código dá sempre
a mesma resposta. Responde, para o gabarito do Strategy:

| ponto | perguntas | valores |
|---|---|---|
| P1 entrega, P2 cupom, P3 pagamento, P4 clube | `localizacao`: algum caso aparece fora da sua unidade? | `isolado`, `espalhado` |
| | `selecao`: a escolha do caso nomeia os casos? faz a conta? | `consulta`, `condicional-unica`, `condicional-no-calculo` |
| P5 região (controle negativo) | `forma` | `enum-dados`, `enum-abstrato`, `classes`, `mapa`, `switch`, `outro` |
| | `proporcao`: a resposta tem o tamanho do problema? | `dados`, `condicional`, `estrutura` |

As definições são as da régua (`evaluation/regua.md`, versão 4: §1, §2 e §3).

## Como roda

Da raiz do `TCC_V3`, com o Docker aberto:

```bash
evaluation/tools/semgrep/detect.sh <raiz-dos-pacotes> <saida.csv>
```

A raiz tem uma subpasta por pacote, cada uma com `src/main/java`. No V4, a raiz são as
cópias cegas (`evaluation/strategy/packages`), e o CSV sai por código. A saída tem uma
linha por pacote, com as respostas, a **evidência** (`regra=quantas(primeiro arquivo:linha)`,
só do que contou) e os **avisos** (trechos que o Semgrep não conseguiu ler), e, no
cabeçalho, o hash das regras, do classificador, do removedor de comentários e da imagem.

| arquivo | o que faz |
|---|---|
| `pontos.mjs` | os nomes dos casos de cada ponto, com os sinônimos em inglês (a única tabela a mudar para outro enunciado) |
| `gerar-regras.mjs` | gera o `regras.yml` a partir da tabela; nunca se edita o `regras.yml` à mão |
| `regras.yml` | as 51 regras (10 por ponto positivo, 11 do controle negativo) |
| `copia-limpa.mjs` | a cópia sem comentários que o Semgrep lê, com as linhas no mesmo lugar (o mesmo removedor da cópia cega, `../sem-comentarios.mjs`) |
| `classificar.mjs` | transforma as linhas marcadas nas respostas da régua |
| `detect.sh` | roda tudo; recusa se o `regras.yml` não for o que o gerador produz, ou se a saída já existir |
| `corpus/` | os quatro corpora de validação e o `validar.sh`, que refaz a validação inteira |

A imagem do Semgrep é travada pelo hash (`semgrep/semgrep:1.95.0`, `sha256:30e6afa9…`).
Trocar a imagem, as regras ou o classificador é um instrumento novo.

**Não edite nada desta pasta enquanto o `detect.sh` roda.** O Bash lê o script aos
poucos, enquanto executa, e uma edição no meio derruba a rodada (aconteceu em 09/10).

## Como as respostas saem

- **Nomear um caso:** uma comparação com o caso (`x == OURO`, `OURO == x`,
  `"OURO".equals(x)`, `Objects.equals(x, "OURO")`, `x.compareTo(PRATA)`, uma constante de
  outro nome com o mesmo valor), `instanceof NivelOuro`, um rótulo `case OURO ->` ou
  `case Ouro o ->` (Java 21), ou um `EnumSet.of(OURO)` que não cita todos os casos. O
  nome em inglês (`GOLD`, `CREDIT_CARD`, `NORTH`) é o mesmo caso.
- **O que não conta** (régua §2.1): a **recusa** (o `if` lança um erro ou devolve um erro)
  conta em `localizacao` mas não em `selecao`; a **lista de válidos** (um bloco que cita
  todos os casos só para conferir, em `if` ou em `case`) não conta; o **registro** (a
  classe `Ouro` dizendo que atende `"OURO"`) não conta; o `instanceof` dentro do
  `equals()` não conta.
- **Fábrica:** um `case` ou `if` que só devolve ou guarda um objeto (`new PagamentoPix()`,
  `NivelClube.OURO`, um campo `ouro` já criado, `Ouro::new`) é `condicional-unica`. Um
  nome de número (`creditoOuro`, `PERCENTUAL_OURO`) não é objeto: é conta.
- **Alarme:** se o pacote não cita **nenhum** nome de um ponto (nem do enunciado, nem em
  inglês), o agente chamou os casos de outro jeito, e o ponto sai `indeterminado`, e não
  um `isolado` falso.
- **P5:** constante de região com corpo, classe por região, classe anônima ou função por
  região, e mapa ou `switch` que devolve uma classe por região são `estrutura`; um
  `switch`/`if` que só devolve o número é `condicional`; `enum` só com valores, mapa de
  números, lista de objetos **de uma classe só** e taxas na configuração são `dados`. Um
  `switch` que só traduz o texto para a constante (`case "NORTE" -> NORTE`) não conta.
- **Erro do Semgrep num arquivo** não para o lote: o trecho que ele não leu (ex.: um
  recurso novo do Java) vai para a coluna `avisos`, e o `verify.mjs` lista esses pacotes.

## Validação

### Versão 1 (09/10, manhã)

Regras escritas olhando 5 pacotes de treino, testadas no EXT (P4 36 de 36; P1 a P3 36
de 36; P5 16 de 36 na primeira versão, 36 de 36 corrigida, olhando os pacotes), nos 4
Opus 5 do V4 (40 de 40) e em 4 exageros de mentira (4 de 4). A leitura de referência
foi feita pelo Claude, que escreveu as regras.

### Versão 2 (09/10, revisão rigorosa)

Cada corpus é um gerador de pacotes pequenos, cada um com **uma** forma de escrever, e
a resposta da régua escrita **antes** de rodar as regras (`corpus/`).

| conjunto | o que é | versão 1 | versão 2 |
|---|---|---|---|
| corpus 1, 40 pacotes | desenvolvimento: as formas que guiaram a versão 2 | **41 de 80** | 80 de 80 |
| corpus 2, 22 pacotes | **controle**: formas novas, escritas depois do corpus 1 | — | 42 de 44 na primeira passada (as 2 falhas eram as previstas), 44 de 44 corrigida |
| corpus 3, 5 pacotes | desenvolvimento: a revisão do código e dois casos da regressão | — | 10 de 10 |
| corpus 4, 6 pacotes | desenvolvimento: nomes em inglês e `compareTo` | — | 12 de 12 |
| **64 pacotes reais** (EXT, piloto, testes do V4) | **regressão**: versão 1 × versão 2 | — | 637 de 640 respostas iguais |

**A regressão nos pacotes reais** é a medida de quanto a versão 1 errava no código de
verdade: em **3 pacotes de 64**, e as três mudanças foram conferidas no código:

- `EXT-01-HAIKU-HARNESS`, P1 `selecao`: `condicional-no-calculo` → `consulta`. O
  `if (MOTOBOY && peso > 5) return Optional.of("MODALIDADE_INDISPONIVEL")` só recusa.
- `BATCH-02-HAIKU-HARNESS`, P3 `localizacao`: `isolado` → `espalhado`. Um
  `instanceof PagamentoPix` fora da casa do Pix, que a versão 1 nunca via.
- `TESTE-DIFICIL-01-OPUS5-N0`, P5 `proporcao`: `indeterminado` → `dados`. Uma lista de
  objetos de uma classe só (`new RegiaoFixa("NORTE", ...)`).

Uma primeira passada da regressão pegou **dois defeitos introduzidos pela própria
versão 2** (um `case SUDESTE, SUL ->` lido como função por região, e um `instanceof`
dentro de uma recusa contado como conta). Os dois foram corrigidos e viraram casos do
corpus 3.

**Defeitos da versão 1 achados:** o `instanceof` com nome composto (`NivelOuro`) e a
recusa sem `throw` nunca funcionaram (o `metavariable-regex` do Semgrep casa só a partir
do começo do texto); o mapa do P5 deixava passar um objeto por região (o `\s*` fora da
verificação devolvia o espaço); um arquivo com um recurso do Java 21 parava o lote
inteiro; texto de comentário era lido como código.

**Limites que ficam:**

- Uma forma de escrever que ninguém previu ainda pode passar. O alarme cobre os nomes
  trocados; o resto, a conferência humana (abaixo de 18 de 20 numa pergunta, ela deixa de
  usar o Semgrep).
- A comparação pela posição (`nivel.ordinal() >= 2`) não é vista: ela não diz de qual
  ponto é o caso.
- Código que nada chama conta como se fosse chamado.
- O corpus de controle tem 22 pacotes; os outros três foram escritos junto com as
  correções. **A validação que vale é a do Lucas**, nos 20 pacotes da amostra do V4.

## Versão congelada

A versão 1 foi congelada em 09/10 de manhã (`regras.yml` `2d255bf4…`, `classificar.mjs`
`104e7ff1…`) e substituída pela versão 2 no mesmo dia, antes de qualquer pacote do V4
existir (`DECISOES.md`, §6). Os hashes da versão 2 entram aqui no congelamento do V4.
Qualquer mudança depois disso é um instrumento novo: ganha entrada no `DECISOES.md` e é
testada de novo. **Nada muda depois que os pacotes do V4 existirem.**
