# Comparação: evaluation/reading/leitura-lucas.csv  ×  analysis/semgrep-V5-STRATEGY.csv

44 pacotes; a pergunta vale se concordarem em pelo menos 40 (limite 0.9).

| pergunta | concordaram | decisão |
|---|---|---|
| P4_localizacao | 44 de 44 | **vale** para o lote |
| P4_selecao | 44 de 44 | **vale** para o lote |
| P5_forma | 42 de 44 | **vale** para o lote |
| P5_proporcao | 44 de 44 | **vale** para o lote |

## Discordâncias (para abrir o arquivo:linha e ver quem tem razão)

- **5R6J**, P5_forma: A = `enum-dados`, B = `mapa`
  - evidência de A: Ouro.java:9+ResumoService.java:71;ResumoService.java:48;Regiao.java:9;Regiao.java:9
  - evidência de B: p5-enum-dados=5(com/loja/checkout/regiao/Regiao.java:8) p5-mapa=1(com/loja/checkout/regiao/Regiao.java:15)
- **NK3P**, P5_forma: A = `enum-dados`, B = `mapa`
  - evidência de A: Ouro.java:9+ResumoService.java:74;ResumoService.java:43;Regiao.java:8;Regiao.java:8
  - evidência de B: p5-enum-dados=5(com/loja/checkout/dto/Regiao.java:7) p5-mapa=1(com/loja/checkout/dto/Regiao.java:14)

A regra não é corrigida para refazer esta conta: a concordância oficial é esta.
