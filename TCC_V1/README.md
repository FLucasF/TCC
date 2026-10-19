# TCC_V1: a construção e a manutenção com patterns

As duas pesquisas do TCC num lugar só, prontas para rodar do zero (montado em 19/10/2026). Elas perguntam a mesma
coisa de dois lados: quando o texto que o agente recebe precisa de um pattern (sem dizer o nome), o agente aplica, e
aplica certo? E o harness muda isso?

| pasta | o que tem |
|---|---|
| [`construction/`](construction/) | a **construção**: o agente recebe uma pasta vazia e o enunciado do sistema inteiro da pousada, com a parte do cenário no fim, e constrói do zero |
| [`maintenance/`](maintenance/) | a **manutenção**: o agente recebe o sistema pronto da pousada, sem nenhum pattern (a base), e um pedido de mudança |
| [`evaluation/`](evaluation/) | só o `common.md`: o que olhar num projeto e o formato de uma correção em texto, sem classes |
| [`skills/`](skills/) | a skill `design-patterns`, escrita para o TCC, e o README dela (origem, cuidados e o viés declarado) |

As duas usam o mesmo domínio (a Pousada Maré Alta), a mesma pilha (Java 21 e Javalin 6.7.0, sem Spring), os mesmos 5
cenários (Strategy no 01, Observer no 03, Factory Method no 05, Decorator no 07, Template Method no 09; os controles
negativos ficaram de fora) e a mesma imagem do experimento (`experimento-harness:v5`). O nome de cada cenário traz o
pattern esperado; o agente nunca vê esse nome.

**Os braços,** nas duas pesquisas:

- **N0**: só o texto (o enunciado ou o pedido);
- **N1**: o `CLAUDE.md` com os princípios (o próximo caso num lugar só; nada de estrutura para o que ninguém pediu);
- **N2**: o mesmo `CLAUDE.md` com uma linha a mais, "use a skill `design-patterns`", e a skill na pasta.

**De onde veio.** Os enunciados, a base, os harnesses e os scripts vêm da `construction-v7` e da `maintenance-v1`, que
ficam no repositório como registro, com as execuções de antes (que usaram a skill pública `gof-patterns` no N2). Aqui
não há execução nenhuma: as pastas `runs/` nascem quando se roda.

## Comandos (a partir desta pasta)

```bash
python construction/infra/scripts/run_one_scenario.py 01 N0 claude-sonnet-5-5 medium
```

```bash
python maintenance/infra/scripts/run_one_scenario.py 01 N0 claude-sonnet-5-5 medium
```

Os scripts de execução só rodam com as pastas do experimento commitadas (o `meta.json` guarda o hash delas). O token
da assinatura é o mesmo das outras pesquisas (`../construction-v5/.env`), que o Docker lê pelo `--env-file`; os
scripts só conferem se ele existe, sem imprimir o valor. As verificações: `python -m mypy` e `python -m ruff check`.
