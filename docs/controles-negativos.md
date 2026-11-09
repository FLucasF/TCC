# Os controles negativos da manutenção

Gerado por `TCC_V1/evaluation/quality/controls_report.py`. Em cada controle, a resposta certa é a mudança pequena (`contenção`); `exagero` é criar uma estrutura que ninguém pediu.

Execuções revisadas: 90.

## Contenção por nível

|  | N0 | N1 | N2 |
|---|---|---|---|
| contenção nos controles | 30 de 30 | 30 de 30 | 28 de 30 |
| *pattern* certo nos cenários que pedem | 19 de 75 | 33 de 75 | 58 de 75 |
| comportamento `bom` nos controles | 29 de 30 | 30 de 30 | 28 de 30 |
| `mvn verify` passa nos controles | 30 de 30 | 30 de 30 | 30 de 30 |

## Contenção por cenário e nível

| cenário | N0 | N1 | N2 |
|---|---|---|---|
| 02 temporada (Strategy) | 6 de 6 | 6 de 6 | 4 de 6 |
| 04 e-mail (Observer) | 6 de 6 | 6 de 6 | 6 de 6 |
| 06 crianças (Factory Method) | 6 de 6 | 6 de 6 | 6 de 6 |
| 08 assinatura (Decorator) | 6 de 6 | 6 de 6 | 6 de 6 |
| 10 coluna (Template Method) | 6 de 6 | 6 de 6 | 6 de 6 |

## Contenção por modelo e nível

| modelo | N0 | N1 | N2 |
|---|---|---|---|
| Haiku 4.5 | 10 de 10 | 10 de 10 | 10 de 10 |
| Sonnet 4.5 | 10 de 10 | 10 de 10 | 10 de 10 |
| Sonnet 5.5 | 10 de 10 | 10 de 10 | 8 de 10 |

## As execuções com exagero

| cenário | nível | modelo | classe |
|---|---|---|---|
| 02 temporada (Strategy) | N2 | Sonnet 5.5 | exagero |
| 02 temporada (Strategy) | N2 | Sonnet 5.5 | exagero |

