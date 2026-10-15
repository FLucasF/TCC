# `history/`: o que já foi usado e saiu, guardado para conferência

O que serviu a uma etapa anterior da bancada e não é usado pelo V5, mas que alguma
execução em `runs/`, algum registro do `DECISOES.md` ou algum hash do README ainda
aponta. Nada daqui é apagado, porque apagar quebraria essas referências: um hash
gravado num `meta.json` tem de continuar achando o arquivo que o gerou.

## O que tem aqui

| pasta | o que é | quem aponta para ela |
|---|---|---|
| `pilot/` | **o piloto** (lote `BATCH`): o enunciado de três pontos (`prompt.md`), as cópias cegas dos pacotes (`packages/`), o mapa da anonimização e a leitura às cegas pelo Claude (`leitura-claude-cego.csv`), de antes da regra de que nenhum modelo de IA avalia. Foi o piloto que mostrou o efeito de teto e motivou o P4 e o P5 | as execuções `BATCH-*` em `runs/`; o README da raiz |
| `prompt-v3/` | os enunciados que rodaram no V3: `prompt.md` (o do `EXT`, dos `TESTE-STRATEGY` e dos `TESTE-P4`) e `state.md` (o dos `TESTE-STATE`) | o `prompt_hash` dessas execuções; o README da raiz |
| `state/` | **o padrão State**, que saiu do V4 em 07/10 (o V4 e o V5 ficam só com o Strategy): o enunciado na versão do V4, que nunca rodou, o gabarito e a suíte de aceitação (`state.mjs`). Está pronto para quando um segundo padrão entrar. README próprio | o `DECISOES.md` (07/10); o `OBJETIVO` (§6) |
| `bench-test/` | **a validação da bancada**: o enunciado barato que só pergunta o que o agente recebeu (`prompt.md`, `b796f244…`) e dois harnesses de teste (`teste-claude-and-skills`, `teste-only-skills`), com a skill `verificacao-harness` que manda responder `SKILL-CARREGADA-OK`. Foi com eles que se provou que cada nível chega ao agente, em 26/09 (Claude Code 2.1.269) e em 10/10 na imagem `v5` (2.1.288) | as execuções `TESTE-BANCADA-*`, `TESTE-IDS-*` e `TESTE-ENSAIO-*`; `experiment/harnesses/README.md` |
| `prompt-harder-draft/` | um rascunho de enunciado mais difícil (09/10), com três regras que cruzam pontos, para tentar tirar os modelos fortes do teto. Testado nos três mais fortes (`TESTE-DIFICIL-01`), não tirou, e o enunciado original ficou. README próprio | o `DECISOES.md` (09/10) |

## Vai para o `TCC_V5`?

**Não.** O `TCC_V5` só recebe o que está no `CONGELADO-V5.sha256`. Esta pasta fica na
bancada (`TCC_V3`).
