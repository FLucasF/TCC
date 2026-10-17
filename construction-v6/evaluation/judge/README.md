# A correção por IA (V6)

A correção da construção é feita por uma IA que **lê as réguas em markdown e interpreta o código**, numa sessão com
o Lucas, uma dimensão de uma execução por vez. Não há script nem lista fechada de respostas: a IA passa pelos itens
da régua e escreve um feedback, como o de um revisor de código, com evidência em `arquivo:linha`. As correções
depois são lidas e juntadas por uma IA. O que sustenta a correção e o que falta antes de valer está no
[protocolo](PROTOCOLO.md).

| arquivo | o que é |
|---|---|
| [`rubric/common.md`](rubric/common.md) | **comece por aqui**: o que ler na pasta da execução, o que não abrir, as regras e o formato da resposta |
| [`rubric/patterns/`](rubric/patterns/common.md) | os padrões, **um prompt por pattern**, como na manutenção: a parte comum (`common.md`: as definições e o E, a estrutura sem necessidade) e `strategy.md` (P1 a P4 e o controle negativo P5), `adapter.md` (P6 e o seu controle negativo), `state.md` (P7, o remendo do cancelamento e o controle negativo do aviso) e `factory.md` (F, descritivo, e o controle negativo) |
| [`rubric/architecture.md`](rubric/architecture.md) | a arquitetura: A1 a A6, lidos com o relatório de arquitetura |
| [`rubric/tests.md`](rubric/tests.md) | os testes: T1 a T4, lidos com os números do PIT |
| [`rubric/database.md`](rubric/database.md) | o banco: B1 a B10, cada um com a fonte, lidos com a foto do esquema |
| [`planted/`](planted/README.md) | um serviço com defeitos plantados e o que a correção dele tem de achar |
| [`expected/pilotos.md`](expected/pilotos.md) | as respostas que as ferramentas já deram nos 8 pilotos, para conferir uma correção |
| `batches/` | o registro da validação de 17/10 (abaixo): as respostas e as transcrições das correções |

## Como corrigir uma execução

Numa conversa nova, para cada dimensão (e, nos padrões, para cada pattern: são quatro conversas):

1. Peça à IA para ler o `rubric/common.md` e a régua da dimensão.
2. Diga qual é a pasta da execução (`runs/<lote>/<execução>/`).
3. A IA lê o enunciado, o projeto e o material da dimensão, e escreve `correcao/<dimensão>.md` na pasta da
   execução.
4. Revise a correção. Para conferir a IA de tempos em tempos, peça a correção do `planted/runs/PLANTED-01` e
   compare com o `planted/esperado.md`.

O material precisa existir antes: o relatório de arquitetura (`evaluation/measures/report_architecture.py`), a linha
do PIT (`evaluation/measures/measure_test_quality.py`) e a foto do banco (a suíte, `run_suite_against_service.py`).

## A validação (17/10/2026)

As réguas foram testadas com o Claude Opus 5.5 (effort `high`), uma dimensão por vez, às cegas (a pasta com um
código no lugar do nome, o projeto sem comentários). Isso foi feito por um script, que depois saiu: a correção não
usa script. Naquela versão, a régua pedia um valor de lista em cada item; as perguntas, as definições e os critérios
são os de agora, e mudou só a forma da resposta. As respostas e as transcrições estão em `batches/`, e a chave dos
códigos em cada `key.json`.

| prova | resultado |
|---|---|
| os erros plantados | **21 de 21**: os 8 itens que os defeitos mudam e os 13 que não mudam (`batches/planted-1/`) |
| contra as ferramentas, nos 8 pilotos | **115 de 116**: padrões 99 de 100 (a régua do Semgrep), banco 16 de 16 (as fotos) (`batches/pilots-1/`) |
| o formato escrito, no serviço plantado | **o banco: 6 de 6** conclusões iguais ao esperado; as outras dimensões faltam (`batches/written-1/`) |
| contra ela mesma (corrigir de novo e comparar) | **não feita** |
| contra o Lucas (a leitura às cegas) | **não feita** |

**A única diferença** (o P6 do OPUS55-N0-02: a IA diz `vazado`, a régua do Semgrep dizia `isolado`) vem da régua: ela
põe os **gramas** entre as unidades da TransLog, e esse agente converte kg em gramas fora do adaptador; a régua do
Semgrep só procurava as horas. **Decidido em 17/10: os gramas entram** (é a calibração de 14/10, "converter a unidade
da transportadora fora do adaptador é vazamento", aplicada a todas as unidades dela), e o esperado do Opus-02 passou a
`vazado`: com isso, as 116 respostas conferem.

**O que a IA achou sem que ninguém pedisse**, conferido no código: o serviço do Sonnet-02 recebe o `PedidoRequest`
da camada web (A1), o Opus-03 tem uma interface com uma implementação só (`PurchaseHistory`, E), e o Sonnet-01 e o
Sonnet-02 não têm nenhuma chave estrangeira (B2).

**O custo**: cerca de US$ 0,50 por dimensão de uma execução (US$ 2,63 no serviço plantado, US$ 15,29 nos 8 pilotos),
e 32 correções usaram uns 25 pontos da janela de 5 horas da assinatura.
