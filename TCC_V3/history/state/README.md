# O State, fora do V4

Em 07/10/2026 o State saiu do V4, que ficou só com o Strategy (60 execuções em vez
de 120). Ele tinha entrado para responder a uma pergunta de engenharia: **a
bancada aceita um segundo padrão sem ser refeita?** A resposta foi sim, com uma
ressalva:

| parte da bancada | como foi testada | resultado |
|---|---|---|
| rodar outro enunciado (`PROMPT_FILE` no `executar.sh`) | `TESTE-STATE-01` e `02`, em `runs/` | funciona (a `01` caiu no limite da assinatura; a `02` completou as 6) |
| suíte de aceitação de outro padrão | `state.mjs` nos pacotes da `TESTE-STATE-02` | 12 de 12 em Opus e Sonnet |
| anonimizar por padrão (`anonimizar.mjs --padrao`) | a opção existe, e sem ela o piloto sai igual; com o State nunca rodou, porque não se gerou pacote dele | só em parte |
| a régua lendo outro padrão | **nunca feita**: nenhum pacote do State foi lido | não provado |

## O que está aqui

| arquivo | o que é |
|---|---|
| `state.md` | o enunciado, na versão do V4 (o cliente que entende o básico), hash `4591f7425e1551fd`. **Nunca rodou.** A versão que rodou nas `TESTE-STATE` é `../prompt-v3/state.md`, hash `ebffe1724ca316b5` |
| `state.mjs` | a suíte de aceitação: os 8 exemplos (com os textos para o cliente) e 4 erros. Roda pelo `evaluation/acceptance-prototype/executor.sh`, que procura o teste na pasta montada em `/aceitacao` |
| `gabarito.md` | o gabarito da régua (nível 3): E1 ações por situação, E2 efeitos do cancelamento e da devolução, E3 texto para o cliente (controle negativo). Nunca calibrado |

A ficha do padrão State (nível 2 da régua) continua em `evaluation/regua.md`,
§3.2.

## Para trazer de volta

1. `state.md` de volta para `experiment/prompt/`, rodado com `PROMPT_FILE`.
2. `gabarito.md` em `evaluation/state/`, com os lotes no cabeçalho.
3. `state.mjs` de volta para `evaluation/acceptance-prototype/`.
4. Antes do lote: a conferência de cópia dos valores do `state.mjs` contra o
   enunciado, as fronteiras sem caso ("até", "acima de"), uma rodada `SMOKE` e a
   calibração da régua (§3.2).

As hipóteses "entre padrões" do OBJETIVO (§4.7) dependem disso.
