# Detector do padrão (Semgrep)

Lê o desenho de cada pacote com regras fixas, **sem IA**: o mesmo código dá sempre
a mesma resposta. Responde, para o gabarito do Strategy:

| ponto | perguntas | valores |
|---|---|---|
| P1 entrega, P2 cupom, P3 pagamento, P4 clube | `localizacao`: algum caso aparece numa condição fora da sua unidade? | `isolado`, `espalhado` |
| | `selecao`: a escolha do caso nomeia os casos? faz a conta? | `consulta`, `condicional-unica`, `condicional-no-calculo` |
| P5 região (controle negativo) | `forma` | `enum-dados`, `enum-abstrato`, `classes`, `mapa`, `switch`, `outro` |
| | `proporcao`: a resposta tem o tamanho do problema? | `dados`, `condicional`, `estrutura` |

As definições são as da régua (`evaluation/regua.md`, §2.2, §2.3, §2.1 e §2.7).

## Como roda

Da raiz do `TCC_V3`, com o Docker aberto:

```bash
evaluation/tools/semgrep/detect.sh <raiz-dos-pacotes> <saida.csv>
```

A raiz tem uma subpasta por pacote, cada uma com `src/main/java`. A saída é um CSV,
uma linha por pacote, com a evidência (`regra=quantas(primeiro arquivo:linha)`) e,
no cabeçalho, o hash das regras, do classificador e da imagem.

| arquivo | o que faz |
|---|---|
| `pontos.mjs` | os nomes dos casos de cada ponto (a única tabela a mudar para outro enunciado) |
| `gerar-regras.mjs` | gera o `regras.yml` a partir da tabela; nunca se edita o `regras.yml` à mão |
| `regras.yml` | as 32 regras (6 por ponto positivo, 8 do controle negativo) |
| `classificar.mjs` | transforma os achados nas respostas da régua |
| `detect.sh` | roda tudo; recusa se o `regras.yml` não for o que o gerador produz, ou se a saída já existir |

A imagem do Semgrep é travada pelo hash (`semgrep/semgrep:1.95.0`, `sha256:30e6afa9…`).
Trocar a imagem, as regras ou o classificador é um instrumento novo.

## Como as respostas saem

- **Condição que nomeia um caso** (`x == OURO`, `"OURO".equals(x)`, `instanceof NivelOuro`,
  `case OURO ->`): se o `if` só recusa o pedido (o corpo tem um `throw`, mesmo um nível
  abaixo), é **validação**, que conta em `localizacao` mas não em `selecao` (régua §2.3).
  Senão, é **cálculo**, e a `selecao` vira `condicional-no-calculo`.
- **Lista de válidos não conta** (régua §2.2): um bloco de comparações ligadas por `&&`
  ou `||`, sem `else`, que cita **todos** os casos do ponto.
- **Fábrica:** um `case` que só devolve um objeto (`case PIX -> new PagamentoPix()`) ou
  uma constante é `condicional-unica`. `new BigDecimal(...)` é número, não objeto.
- **Alarme de nomes desconhecidos:** se o pacote não cita **nenhum** nome de caso de um ponto
  (nem no `enum`, nem no `codigo()`, nem num nome de classe), o agente chamou os casos de
  outro jeito (`GOLD` no lugar de `OURO`), e as regras não os veriam. O ponto sai
  `indeterminado`, e não `isolado`.
- **P5:** constante de região com corpo, classe por região, ou mapa e `switch` que
  devolvem objeto são `estrutura`; `switch` que só devolve o número é `condicional`;
  `enum` só com valores ou mapa de números são `dados`.

## Validação (ensaio, 09/10)

Regras escritas olhando 5 pacotes de treino (o Haiku 4.5 do V4: `TESTE-MAPA-01` e
`TESTE-NIVEIS-01`), **congeladas**, e testadas em pacotes lidos **antes** de rodar:

| conjunto | pontos | concordou com a leitura | vale como teste? |
|---|---|---|---|
| EXT (V3), 18 pacotes | P4 | 36 de 36 | sim |
| EXT, 6 pacotes | P1 a P3 | 36 de 36 | sim |
| EXT, 18 pacotes | P5 | 16 de 36 na primeira versão; 36 de 36 corrigida | **não**: a correção olhou estes pacotes |
| Opus 5 do V4 (`TESTE-NIVEIS-01-OPUS`), 4 pacotes nunca vistos | P1 a P5 | **40 de 40** (P5: 8 de 8) | sim |
| 4 pacotes de mentira, com exagero no P5 de quatro jeitos e o clube com nomes em inglês | P5 e o alarme | 4 de 4 `estrutura`; o alarme deu `indeterminado` | sim (escritos para testar) |

A leitura de referência do ensaio foi feita pelo Claude, que também escreveu as
regras. **A validação que vale é a do Lucas**, nos 20 pacotes da amostra do V4 (o P4 e o
P5), com a regra de saída pré-registrada (abaixo de 18 de 20 numa pergunta, ela vira
descritiva). O P1 a P3 não têm conferência humana no V4 e entram como secundários.

**Limites conhecidos:**

- Uma forma de escrever que ninguém previu derruba a regra **em silêncio**: foi o que
  aconteceu no P5 (`SUDESTE(new BigDecimal("0.12"))`). O alarme cobre o caso dos nomes
  trocados; os outros, só a conferência humana.
- Nenhum pacote **real** com exagero no P5 foi visto ainda (só os de mentira).
- `if` que valida e calcula no mesmo corpo conta como validação.

## Versão congelada (09/10/2026)

| arquivo | sha256 |
|---|---|
| `pontos.mjs` | `3cd58b31105097f23c4bb944e41c539d0a61461d5eb838bb64827455e978f52d` |
| `gerar-regras.mjs` | `e46866b54bdf9a507c77c3acd8156f77e1f77d66585713f79ebbea3512bff6c4` |
| `regras.yml` | `2d255bf44d5e23434e4ce802b8a4ffb4b7718f7476764fc6783020280016c786` |
| `classificar.mjs` | `104e7ff1074364b45da48828bd1c9a36d2aa49b51a071ceef5d32b8713c70b08` |
| `detect.sh` | `7f5628ffef25b7243632c9d93e4de972684755bce5ffa714627a0c8237e6abb1` |
| imagem | `semgrep/semgrep@sha256:30e6afa99ebd8e7b4115d4904898108eb4bf77025819e9263f09cf14e6f6e549` |

Qualquer mudança depois desta data é um instrumento novo: ganha entrada no
`DECISOES.md` e é testada de novo. Em especial, **nada muda depois que os pacotes do V4
existirem**.
