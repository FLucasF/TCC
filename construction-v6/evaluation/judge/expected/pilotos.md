# As respostas que as ferramentas já deram nos 8 pilotos

Para conferir uma correção dos pilotos item a item. Uma célula vazia é um item que nenhuma ferramenta responde: ali só vale a leitura. `indeterminado` conta como discordância.

**De onde vêm.** padroes: a régua do Semgrep removida em 15/10 (validada em 2.200 de 2.200 respostas contra o V5), rodada do histórico do git (commit 28ddaa29) nos 8 pilotos em 17/10/2026; o P6 só nos pilotos cujo enunciado tinha o vocabulário estrangeiro da TransLog. banco: B1 e B3 calculados das fotos do esquema (os pilotos sem foto, cujo serviço não subiu, ficam de fora). No OPUS55-N0-03, B2, B4, B5 e B7 lidos da foto (as ligações têm chave estrangeira, o dinheiro é numeric, os instantes têm fuso, e há o CHECK do estoque, a chave com o id da transação e o CHECK da quantidade): é o par de controle do serviço plantado.

## padroes

| execução | P1-local | P1-selecao | P2-local | P2-selecao | P3-local | P3-selecao | P4-local | P4-selecao | P5-forma | P5-proporcao | P6 | P7 | F |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HAIKU45-N0-01 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional |  | espalhado | nenhum |
| HAIKU45-N0-02 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional | vazado | espalhado | nenhum |
| OPUS55-N0-01 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados |  | isolado | container |
| OPUS55-N0-02 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | isolado | isolado | container |
| OPUS55-N0-03 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | isolado | container |
| SONNET55-N0-01 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados |  | espalhado | container |
| SONNET55-N0-02 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados |  | espalhado | container |
| SONNET55-N0-03 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | espalhado | container |

## banco

| execução | B1 | B2 | B3 | B4 | B5 | B7 |
|---|---|---|---|---|---|---|
| OPUS55-N0-01 | atende |  | parcial |  |  |  |
| OPUS55-N0-02 | atende |  | parcial |  |  |  |
| OPUS55-N0-03 | atende | atende | parcial | atende | atende | atende |
| SONNET55-N0-01 | atende |  | nao_se_aplica |  |  |  |
| SONNET55-N0-02 | atende |  | nao_se_aplica |  |  |  |
| SONNET55-N0-03 | atende |  | atende |  |  |  |

**Em aberto:** o P6 do OPUS55-N0-02 está `isolado` porque a régua do Semgrep só via as horas. A régua atual põe os gramas entre as unidades da TransLog, e esse agente converte kg em gramas fora do adaptador: pela régua atual, `vazado`. O Lucas confirma qual vale.
