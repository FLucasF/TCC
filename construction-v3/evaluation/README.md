# `evaluation/`: como o código dos agentes é avaliado

Tudo o que mede o que um agente produziu: as regras de leitura, o gabarito do
enunciado, a suíte que testa o serviço, as ferramentas automáticas e onde as leituras
ficam. Nenhum instrumento daqui usa IA para avaliar (`OBJETIVO.md`, §3).

## Os quatro eixos e onde cada um mora

| eixo | o instrumento | onde |
|---|---|---|
| **desenho** (o padrão foi aplicado onde o enunciado pede, e não onde seria exagero?) | o Semgrep, conferido pela leitura humana às cegas | `tools/semgrep/`, `regua.md`, `GUIA-DA-REGUA.md`, `strategy/` |
| **correção** (o serviço calcula e recusa o que o enunciado pede?) | a suíte de aceitação, por HTTP | `acceptance-prototype/` (rodada por `infra/scripts/acceptance.sh`) |
| **qualidade** (o código ficou mais simples ou mais complicado?) | CK e SonarQube | `tools/metrics.sh`, saída em `metrics/` |
| **custo e processo** | o `meta.json` de cada execução | `runs/` e `infra/scripts/aggregate.mjs` |

## O que tem aqui

| item | o que é | vai para o `TCC_V5`? |
|---|---|---|
| `regua.md` | **a régua** (versão 5): as 4 perguntas do desenho (P4 `localizacao` e `selecao`, P5 `forma` e `proporcao`), com a lista fechada de valores e a definição de cada um. O Semgrep implementa as mesmas definições, e é por isso que a leitura do Lucas consegue conferi-lo | sim (congelada) |
| `GUIA-DA-REGUA.md` | a régua em linguagem de leitura: o roteiro de cada pacote (achar o projeto, as buscas, os três testes), exemplos inventados e as armadilhas da busca. Quando diverge da régua, vale a régua | sim (congelado) |
| `strategy/` | o padrão do V5. `gabarito.md`: os pontos (P1 a P5) e os casos de cada um, lidos por script (o `verify.mjs` confere o hash do enunciado e o lote no cabeçalho). Depois do passo 3: `packages/` (as **cópias cegas**, um código de 4 letras por execução, fora do git) e `mapa-anonimizacao.csv` (código → execução, fora do git e **fechado para o Lucas** até a leitura dele ser commitada) | o `gabarito.md` (congelado); o resto nasce no V5 |
| `reading/` | nasce no passo 3: `amostra.csv` (os 44 códigos sorteados), `leitura-lucas.csv` (a planilha), depois `releitura.csv` e `releitura-lucas.csv` | nasce no V5 |
| `acceptance-prototype/` | **a suíte de aceitação**: os 21 casos (12 contas, 9 recusas), a calculadora de referência, o executor que sobe o serviço no container e os mutantes que provam que a suíte reprova código errado. README próprio | sim (congelada) |
| `tools/` | as ferramentas: anonimização, Semgrep, sorteio, comparação, nota, hipóteses, métricas, e os testes de cada uma. README próprio, com o que cada script faz e como | sim (congelada) |
| `metrics/` | a saída do `tools/metrics.sh`: `metrics/<lote>/<run_id>/` (o `ck/` e o `sonar/` de cada execução, e o `status`, fora do git) e `metrics/<lote>/metricas.csv` (uma linha por execução, no git) | nasce no V5 |
| `calibration-pilot/`, `calibration-strategy/`, `calibration-v4/` e `calibracao-relatorio.md` | **a história da régua**: as rodadas de calibração das versões 1 a 3 (pacotes do piloto e do EXT, lidos às cegas, em partes) e a calibração da versão 4 pelo Lucas em 2 pacotes do V3 (8 de 8 com o Semgrep). O relatório resume as três rodadas da versão 3 | não (só a bancada) |
| `strategy/leitura-claude-cego-ext.csv` | a leitura às cegas do EXT pelo Claude na régua versão 3, de antes da regra de que nenhum modelo de IA avalia | não (só a bancada) |

## O caminho de um pacote, do agente à hipótese

1. `runs/<id>/workspace/` é o que o agente deixou.
2. `tools/anonymize.mjs` copia para `strategy/packages/<CÓDIGO>/`, sem comentários e sem
   nada que diga o nível.
3. `tools/semgrep/detect.sh` responde à régua em todos os pacotes → `analysis/semgrep-<lote>.csv`.
4. `tools/sample.mjs` sorteia 1 pacote por modelo × nível (44) → `reading/`.
5. O Lucas lê os 44 pela régua e pelo guia → `reading/leitura-lucas.csv`.
6. `tools/compare.mjs` compara a leitura com o Semgrep: cada pergunta vale para os 220
   com 40 de 44, ou vira descritiva.
7. `tools/nota.mjs` e `tools/hipoteses.mjs` juntam o desenho, a suíte, as métricas e o
   custo, e dão a nota e o veredito de cada hipótese.
