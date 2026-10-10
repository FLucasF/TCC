# Comparação: evaluation/reading-ensaio/leitura-simulada-claude.csv  ×  analysis/semgrep-V4-STRATEGY.csv

20 pacotes; a pergunta vale se concordarem em pelo menos 18 (limite 0.9).

| pergunta | concordaram | decisão |
|---|---|---|
| P4_localizacao | 19 de 20 | **vale** para o lote |
| P4_selecao | 19 de 20 | **vale** para o lote |
| P5_forma | 19 de 20 | **vale** para o lote |
| P5_proporcao | 19 de 20 | **vale** para o lote |

## Discordâncias (para abrir o arquivo:linha e ver quem tem razão)

- **C4RK**, P4_localizacao: A = `isolado`, B = `indeterminado`
  - evidência de A: checkout/clube/Ouro.java:9 e checkout/ResumoService.java:48;checkout/ResumoService.java:101;checkout/Regiao.java:7;checkout/Regiao.java:7
- **C4RK**, P4_selecao: A = `consulta`, B = `indeterminado`
  - evidência de A: checkout/clube/Ouro.java:9 e checkout/ResumoService.java:48;checkout/ResumoService.java:101;checkout/Regiao.java:7;checkout/Regiao.java:7
- **C4RK**, P5_forma: A = `enum-dados`, B = `outro`
  - evidência de A: checkout/clube/Ouro.java:9 e checkout/ResumoService.java:48;checkout/ResumoService.java:101;checkout/Regiao.java:7;checkout/Regiao.java:7
- **C4RK**, P5_proporcao: A = `dados`, B = `indeterminado`
  - evidência de A: checkout/clube/Ouro.java:9 e checkout/ResumoService.java:48;checkout/ResumoService.java:101;checkout/Regiao.java:7;checkout/Regiao.java:7

A regra não é corrigida para refazer esta conta: a concordância oficial é esta.
