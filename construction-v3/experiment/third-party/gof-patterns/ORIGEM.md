# gof-patterns: origem

A skill do nível N2 (`experiment/harnesses/N2/.claude/skills/gof-patterns/`) é
uma cópia **intacta** de material de terceiros. Esta pasta fica **fora** da pasta
do harness de propósito: tudo o que está em `N2/` chega ao agente.

| | |
|---|---|
| autor | grndlvl |
| repositório | [grndlvl/software-patterns](https://github.com/grndlvl/software-patterns) |
| caminho | `.claude/skills/gof-patterns/` |
| commit | `85e94a3bc19e9063a51b12289bb027a8bfbb13e8` (05/02/2026) |
| licença | MIT, Copyright (c) 2025 grndlvl (texto em `LICENSE`, nesta pasta) |
| conteúdo | 25 arquivos `.md`, 896.108 bytes: o `SKILL.md`, o `pattern-selection.md` e uma página por padrão do GoF (23) |
| copiado em | 06/10/2026 |

**Conferência.** Cada um dos 25 arquivos foi comparado com o blob do git
naquele commit (`git hash-object` contra o sha da API do GitHub): os 25
conferem. Nenhum foi editado.

**Por que do repositório original, e não do agregador.** A skill também aparece
em `majiayu000/claude-skill-registry`, mas lá só há o `SKILL.md` e um
`metadata.json`: as 24 páginas para onde o `SKILL.md` aponta não foram
copiadas, e a skill seria uma tabela de links quebrados.

**Por que uma skill pública, e não uma escrita para o TCC.** Uma skill escrita
por quem conhece as tarefas tende a ser moldada para elas (o primeiro rascunho,
escrito com o Claude, repetiu uma frase do enunciado do Strategy). Esta foi
escrita sem conhecer as tarefas, como um usuário real a encontraria.

**Limitação conhecida.** Os exemplos completos são os canônicos desses padrões e
caem perto do domínio das tarefas: o de State é um sistema de pedido (`pay`,
`ship`, `deliver`, `cancel`, `refund`), e o de Strategy é pagamento
(`PaymentStrategy`). Declarado no §6 do `OBJETIVO.md`. Como as páginas de cada
padrão só são lidas sob demanda, a transcrição de cada execução mostra se o
agente abriu a página do State ou do Strategy.
