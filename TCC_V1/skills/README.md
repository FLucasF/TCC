# As skills do TCC_V1

| skill | de onde vem | onde é usada |
|---|---|---|
| [`design-patterns/`](design-patterns/) | escrita para o TCC (Claude Opus 5.5 com o Lucas, 19/10/2026) | o N2, nas duas pesquisas (copiada em `<pesquisa>/experiment/harnesses/N2/.claude/skills/`) |

A skill pública que o N2 usava antes, a `gof-patterns` ([grndlvl/software-patterns](https://github.com/grndlvl/software-patterns),
commit `85e94a3`, MIT), fica nas pastas antigas (`construction-v7`, `maintenance-v1`), com as execuções feitas com ela.

**Este README fica fora da pasta da skill de propósito:** tudo o que está em `design-patterns/` é copiado para o
workspace do agente, e o que se diz aqui (as tarefas, a régua, o viés) não pode chegar a ele.

## A `design-patterns`

**Por que existe.** A `gof-patterns` é um catálogo de 896 KB (os 23 patterns, cada um com 30 a 45 KB de pseudocódigo):
o `SKILL.md` que o agente recebe é só um índice, a árvore de decisão é de frases curtas e não separa os patterns
vizinhos (o Sonnet 4.5 escolheu o Strategy para os relatórios do cenário 09 duas vezes, no N2 e no N2u), e o "quando
não usar" é curto. No GitHub não há uma skill de patterns bem avaliada por si: as estrelas são das coleções, e as
coleções grandes não têm uma; a mais próxima (`decebals/claude-code-java`) também é um catálogo.

**Como foi escrita,** seguindo as boas práticas de skills do Claude:

- a **descrição** diz o que a skill faz e quando usar, porque é ela que decide se a skill é aberta;
- o **`SKILL.md`** é curto (cerca de 100 linhas) e é um processo de decisão: há uma variação que merece estrutura?
  o que varia? qual dos vizinhos? como aplicar por inteiro? quando não usar;
- as **referências** (uma por pattern, de 48 a 64 linhas) são lidas sob demanda, cada uma com o quando usar, o quando
  não usar, um esboço em Java, como saber que ficou certo, os erros comuns e os vizinhos;
- o texto explica o porquê das regras em vez de dar ordens soltas;
- e diz, logo no começo, que a skill é material de referência para a tarefa em andamento, que ela não responde
  perguntas e que o resultado de usá-la é código (a armadilha do Sonnet 4.5 no N2: veja
  `docs/relatorio-construction-v7.md`, "O erro da skill aberta com uma pergunta").

**Os cuidados com o viés,** porque ela foi escrita por quem conhece as tarefas e a régua:

- cobre 13 patterns, não só os 5 do estudo: os vizinhos com que eles se confundem (State, Chain of Responsibility,
  Abstract Factory, Builder, Adapter, Facade, Composite, Command) entram com a mesma estrutura;
- nenhum exemplo é do domínio das tarefas (nada de pousada, reserva, e-mail, relatório, documento ou pagamento): são
  rotas de mapa, máquina de venda, jogos de tabuleiro, estação meteorológica, streams de dados, eventos de interface,
  inimigos de um jogo, temas de interface, requisições HTTP, sensores, conversão de vídeo, formas geométricas e um
  editor de texto; a revisão procurou e trocou as palavras perto das tarefas que tinham escapado ("pricing", "price");
- os erros comuns vêm da literatura dos patterns, em termos gerais, e não das frases da régua.

**O viés que sobra, declarado.** Os 5 patterns do estudo têm referências um pouco mais longas (62 a 64 linhas, contra
48 a 59 dos outros), por pedido do Lucas ("deixando claro o uso dos patterns utilizados"). E vários erros comuns
descritos coincidem com o que a régua chama de `malfeito` (só os casos novos na estrutura, quem usa a estrutura
perguntando o caso concreto, a combinação decidida dentro das peças), porque são os erros clássicos de cada pattern.
Por isso, o N2 com esta skill mede o efeito de uma skill boa e alinhada com a régua, e não de uma skill qualquer; a
comparação com a skill pública fica nas execuções antigas (`construction-v7` e `maintenance-v1`).
