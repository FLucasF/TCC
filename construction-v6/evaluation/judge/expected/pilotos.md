# As respostas que as ferramentas já deram nos 8 pilotos

Para conferir uma correção dos pilotos item a item. Uma célula vazia é um item que nenhuma ferramenta responde: ali só vale a leitura.

**Como ler as palavras.** Elas vêm da versão anterior da régua, que pedia um valor de lista em cada item. Numa correção escrita, confira se o texto chega à mesma conclusão: `isolado`/`espalhado`, se o código de cada caso mora na casa dele ou se algum caso escapa; `consulta`, `condicional_unica` e `condicional_no_calculo`, as três formas de escolher o caso (a régua dos padrões as descreve); `enum_dados`/`dados`, a variação por região como dado; `vazado`, algum termo ou unidade da TransLog fora do lado dela; `container`/`nenhum`, como os objetos são criados; no banco, `atende`, `parcial` e `nao_atende`, se a prática é seguida em tudo, em parte ou em nada.

**De onde vêm.** padroes: a régua do Semgrep removida em 15/10 (validada em 2.200 de 2.200 respostas contra o V5), rodada do histórico do git (commit 28ddaa29) nos 8 pilotos em 17/10/2026; o P6 só nos pilotos cujo enunciado tinha o vocabulário estrangeiro da TransLog. banco: B1 e B3 calculados das fotos do esquema (os pilotos sem foto, cujo serviço não subiu, ficam de fora). No OPUS55-N0-03, B2, B4, B5 e B7 lidos da foto (as ligações têm chave estrangeira, o dinheiro é numeric, os instantes têm fuso, e há o CHECK do estoque, a chave com o id da transação e o CHECK da quantidade): é o par de controle do serviço plantado.

## padroes

Cada item cai na correção de um pattern: P1 a P5 no Strategy (`padroes-strategy.md`), P6 no Adapter, P7 no State e F no Factory.

| execução | P1-local | P1-selecao | P2-local | P2-selecao | P3-local | P3-selecao | P4-local | P4-selecao | P5-forma | P5-proporcao | P6 | P7 | F |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HAIKU45-N0-01 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional |  | espalhado | nenhum |
| HAIKU45-N0-02 | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | espalhado | condicional_no_calculo | switch | condicional | vazado | espalhado | nenhum |
| OPUS55-N0-01 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados |  | isolado | container |
| OPUS55-N0-02 | isolado | consulta | isolado | consulta | isolado | consulta | isolado | consulta | enum_dados | dados | vazado | isolado | container |
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

**O P6 do OPUS55-N0-02 é `vazado`, e não o `isolado` da régua do Semgrep** (17/10/2026): a régua do Semgrep só via as horas; a régua atual põe os gramas entre as unidades da TransLog, e esse agente converte kg em gramas fora do adaptador (`EntregaPorTransportadora.java:26`). É a decisão de calibração de 14/10 ("converter a unidade da transportadora fora do adaptador é vazamento"), aplicada a todas as unidades dela.
