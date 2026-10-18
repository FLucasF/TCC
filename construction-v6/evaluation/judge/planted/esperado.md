# O que a correção do PLANTED-01 tem de achar

Os itens que os defeitos plantados mudam, e os que o original (OPUS55-N0-03) já tinha e os defeitos não mudam (a correção não pode se confundir com eles). Os defeitos estão no README desta pasta. As palavras são as da linha de conclusão de cada item (`rubric/common.md`): confira a última linha `Conclusão:` de cada item da resposta.

**De onde vêm.** Os defeitos plantados (P4, A1, B2, B4, B5, B7, T2) e, para conferir que o juiz não se confunde com eles, os valores que o original (OPUS55-N0-03) já tem e os defeitos não mudam: P1 a P3, P5 a P7 e F da régua do Semgrep, e B1 e B3 calculados da foto (o B3 já era parcial no original: a notification.order_id não tem índice).

**As palavras de antes (até 17/10/2026).** A régua anterior pedia um valor de lista; a troca foi esta: `isolado` e `consulta` viraram `acerto`; o P4 `espalhado` com `condicional_no_calculo` virou `malfeito` (a estrutura dos níveis existe, mas o brinde do GOLD escapa para um `if` no cálculo); `enum_dados` e `dados` viraram `contenção`; o P6 `vazado` virou `malfeito` (o OPUS55-N0-03 tem um lado da TransLog, e a conversão dos quilos em gramas vaza para a estratégia de entrega; até 18/10 a régua do Adapter só tinha acerto e omissão, e o esperado era `omissão`); o F `container` virou `não se aplica` (o F só descreve); `atende`, `parcial` e `nao_atende` viraram `segue`, `em parte` e `não segue`; o T2 `superficiais` virou `não segue`.

## padrões

Cada item cai na correção de um pattern: P1 a P5 no Strategy, P6 no Adapter, P7 no State e F no Factory.

| execução | P1-local | P1-selecao | P2-local | P2-selecao | P3-local | P3-selecao | P4-local | P4-selecao | P5-forma | P5-proporcao | P6 | P7 | F |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| PLANTED-01 | acerto | acerto | acerto | acerto | acerto | acerto | malfeito | malfeito | contenção | contenção | malfeito | acerto | não se aplica |

## arquitetura

| execução | A1 |
|---|---|
| PLANTED-01 | não segue |

## testes

| execução | T2 |
|---|---|
| PLANTED-01 | não segue |

## banco

| execução | B1 | B2 | B3 | B4 | B5 | B7 |
|---|---|---|---|---|---|---|
| PLANTED-01 | segue | em parte | em parte | não segue | não segue | em parte |
