# TCC_V6: a pesquisa nova (em construção)

O próximo experimento: o mesmo serviço de checkout em Java/Spring, agora com um enunciado
mais complexo e testável (o ciclo do pedido, o estoque, uma transportadora com API estrangeira,
persistência e concorrência). Só a **construção** é medida; não há fase de manutenção.
Os padrões observados são o Strategy, o Adapter e o Factory, com o State como secundário.

**Nada aqui está congelado.** É um rascunho de trabalho: tudo pode mudar até o congelamento,
que vai ser feito como o do V5 (um `CONGELADO-V6.sha256` e um commit antes da primeira
execução que vale).

- **As notas do desenho** (os pilotos, as armadilhas, as decisões em aberto, o que falta):
  [`NOTAS.md`](NOTAS.md).

## O que fica onde

| pasta | o que tem |
|---|---|
| `experiment/prompt/` | o enunciado (`prompt.md`) e, em `history/`, as versões anteriores |
| `evaluation/reference/` | o gabarito (a calculadora do resumo e o `Store`, o modelo do estado), os testes dele e a suíte completa que compara o serviço do agente com o gabarito ([README](evaluation/reference/README.md)) |
| `infra/scripts/` | o `pilot.sh`, que roda um piloto sem harness com a imagem do V5 |
| `runs/pilots/` | os pilotos: uma pasta por execução, com a transcrição, o build e os resultados da suíte |

Os harnesses (N1 a N3) ainda não estão aqui: o Lucas vai redesenhá-los.

## Comandos (Git Bash, a partir desta pasta)

```bash
infra/scripts/pilot.sh OPUS55-N0-04 claude-opus-5-5 medium
evaluation/reference/run-suite.sh runs/pilots/OPUS55-N0-04/workspace runs/pilots/OPUS55-N0-04/suite
```

Fora do git: o `target/` de cada build e o `classpath.txt` da suíte. O token da assinatura
é o mesmo do V5 (`../TCC_V5/.env`), que o `pilot.sh` lê sem imprimir.
