# O que a correção do PLANTED-01 tem de achar

Os itens que os defeitos plantados mudam, e os que o original (OPUS55-N0-03) já tinha e os defeitos não mudam (a correção não pode se confundir com eles). Os defeitos estão no README desta pasta. As palavras vêm da versão anterior da régua, que pedia um valor de lista: o `../expected/pilotos.md` diz como ler cada uma numa correção escrita.

**De onde vêm.** Os defeitos plantados (P4, A1, B2, B4, B5, B7, T2) e, para conferir que o juiz não se confunde com eles, os valores que o original (OPUS55-N0-03) já tem e os defeitos não mudam: P1 a P3, P5 a P7 e F da régua do Semgrep, e B1 e B3 calculados da foto (o B3 já era parcial no original: a notification.order_id não tem índice).

## padroes

Cada item cai na correção de um pattern: P1 a P5 no Strategy (`padroes-strategy.md`), P6 no Adapter, P7 no State e F no Factory.

| execução | P1-local | P1-selecao | P2-local | P2-selecao | P3-local | P3-selecao | P4-local | P4-selecao | P5-forma | P5-proporcao | P6 | P7 | F |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| PLANTED-01 | isolado | consulta | isolado | consulta | isolado | consulta | espalhado | condicional_no_calculo | enum_dados | dados | vazado | isolado | container |

## arquitetura

| execução | A1 |
|---|---|
| PLANTED-01 | nao_atende |

## testes

| execução | T2 |
|---|---|
| PLANTED-01 | superficiais |

## banco

| execução | B1 | B2 | B3 | B4 | B5 | B7 |
|---|---|---|---|---|---|---|
| PLANTED-01 | atende | parcial | parcial | nao_atende | nao_atende | parcial |
