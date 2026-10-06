# Os harnesses

Cada pasta aqui é **um nível de harness**: o que o agente recebe no braço
`HARNESS`. A pasta inteira é copiada para a raiz do workspace, e o hash dela vai
para `environment.harness_hash` no `meta.json`.

## A escada: N0 a N4

Os níveis vêm da proposta do orientador (12/09/2026). Cada nível **acumula** o
anterior, e o tipo diz como o harness age: uma instrução o modelo lê e pode
ignorar; um sensor devolve retorno objetivo; um processo muda a ordem do
trabalho.

| nível | o que acrescenta | tipo | pasta | estado |
|---|---|---|---|---|
| **N0** | nada: só o enunciado | — | nenhuma: é o braço `CONTROL` | rodado, lotes `EXT` |
| **N1** | orientação: o `CLAUDE.md` com 4 regras sobre variação | instrução | `N1/` | rodado, lotes `EXT-01` a `03`, hash `560577922737dbb9` |
| **N2** | conhecimento: o N1 + a skill `gof-patterns` | instrução | `N2/` | pronto, hash `27987df0bd1febe2` |
| **N3** | processo: o N2 + desenho antes do código e um revisor independente | processo | `N3/` | pronto, hash `5f4c492bf3d12f68` |
| **N4** | verificação automática: build e testes rodando sozinhos | sensor | — | **descartado** (06/10), ver abaixo |

**O que cada pasta tem.**

- `N1/`: o `CLAUDE.md`.
- `N2/`: o mesmo `CLAUDE.md`, byte a byte, e a skill `gof-patterns`. Manter o
  `CLAUDE.md` igual é o que permite atribuir à skill a diferença entre os dois.
- `N3/`: o `CLAUDE.md` do N1, intacto no começo, com uma seção **Processo**
  acrescentada no fim (registrar um desenho curto em `.claude/desenho.md` antes
  do código, implementar, e pedir revisão antes de terminar); a mesma skill do
  N2, byte a byte; e o subagente `.claude/agents/revisor.md`, que só lê (`Read`,
  `Grep`, `Glob`), usa o mesmo modelo do agente (`model: inherit`) e devolve no
  máximo dez problemas. O desenho fica em `.claude/` de propósito: o anonimizador
  remove essa pasta dos pacotes, e um arquivo de desenho solto entregaria o braço
  na leitura cega. O revisor não usa o vocabulário da régua nem dos enunciados.

**Por que a verificação automática foi descartada (06/10/2026).** O sensor
previsto era um hook que roda `mvn verify` quando o agente diz que terminou, e
devolve o erro se falhar. Mas o agente **já faz isso sozinho**: nas 62 execuções
feitas até aqui (`BATCH`, `EXT`, `SMOKE`, `TESTE-*`), todas rodaram o Maven por
conta própria, todas testaram depois da última edição e todas terminaram com o
build passando. No Haiku, o modelo mais fraco, foram 20 de 20, rodando o Maven
de 4 a 14 vezes por execução. Um sensor de build não teria o que corrigir. Um
sensor que visse o que o agente não vê teria de usar a suíte de aceitação ou as
métricas de desenho, e os dois são instrumentos da avaliação: o nível seria
corrigido pelo gabarito. Em tarefas deste tamanho, com estes modelos, a
verificação de build já é o comportamento padrão do Claude Code; em tarefas
grandes ou com modelos mais fracos, o sensor pode voltar a fazer sentido.

**Por que a numeração difere da proposta do orientador.** Na proposta, o N3 era
a verificação automática e o N4 era o processo. Com a verificação descartada, a
escada teria um buraco no meio (N0, N1, N2 e N4). Para ficar organizada, os dois
trocaram de lugar: os níveis que existem ficam contíguos, de N0 a N3, cada um
acumulando o anterior, e o descartado vai para o fim, como N4. A troca é só de
número; o conteúdo de cada nível é o da proposta. Correspondência: N3 da
proposta (verificação) = N4 aqui, descartado; N4 da proposta (processo) = N3 aqui.

**Nomes até 06/10/2026.** `N1/` era `only-claude/`, e `N2/` era
`claude-and-skills/`. O conteúdo não mudou com a troca, e o hash do `N1/` é o
mesmo gravado nas execuções do EXT. Havia também `only-skills/` (skills sem o
`CLAUDE.md`), que nunca rodou e saiu por não ser um nível: a escada acumula.
Os registros datados, como `analysis/testes-2026-09-30.md`, usam os nomes antigos.

## Rodar com um nível

```bash
HARNESS=N2 infra/scripts/rodada.sh STATE-01 1
```

Sem `HARNESS`, vale `N1`. O `CONTROL` ignora a variável.

## Adicionar uma skill

1. Copie a **pasta** da skill para `<versao>/.claude/skills/<nome-da-skill>/`. Ela
   precisa ter um `SKILL.md`. Copie os arquivos; não instale como plugin, que vem
   pela rede e pode mudar entre execuções.
2. Registre na tabela abaixo de onde ela veio.
3. Confira se a skill chama algum programa externo. Se chamar, ele precisa existir
   na imagem, e isso muda o `Dockerfile`.
4. Commite **antes** de rodar. O hash da versão muda, e é ele que identifica, no
   `meta.json`, o que cada execução recebeu.

O `.gitkeep` das pastas `.claude/skills/` só existe para o git guardar a pasta
vazia. O `executar.sh` não o copia e não o conta no hash, e recusa rodar uma
versão com `.claude/skills/` vazia.

## Cuidados

- **Nada de nota ou README dentro de uma versão.** Tudo o que estiver na pasta
  chega ao agente e vira parte do harness. Anotação fica **neste** arquivo.
- **Versão nova é pasta nova.** Mudar uma versão que já rodou apaga a
  correspondência entre o nome e o que as execuções receberam; o hash no
  `meta.json` continua certo, mas o nome passa a mentir.
- **O anonimizador já tira `CLAUDE.md` e `.claude/` dos pacotes**, então nem o
  `CLAUDE.md` nem as skills entregam o braço na leitura cega. Arquivo que a skill
  **fizer o agente escrever** (um `ADR.md`, por exemplo) fica no pacote, e conta
  como pista da condição.
- **Como conferir que a skill carregou:** `isolation_init.skills` no `meta.json`
  lista as skills que o Claude Code achou ao iniciar. A skill da versão tem de
  aparecer nas execuções `HARNESS` e não nas `CONTROL`. Se o agente a **usou**
  aparece em `outcome.tool_calls_by_name`, na ferramenta `Skill`.

## Validação da bancada

Em 26/09/2026, quatro execuções `TESTE-BANCADA-*` (Haiku, esforço baixo, um
enunciado que só pergunta o que o agente recebeu) conferiram, dentro do container:

| execução | chegou ao workspace | skill no `isolation_init.skills` | resposta |
|---|---|---|---|
| `CONTROL` | nada | não | sem `CLAUDE.md`, sem skill |
| `ONLY-CLAUDE` | `CLAUDE.md` | não | com `CLAUDE.md`, sem skill; hash `560577922737dbb9` |
| `CLAUDE-SKILLS` | `CLAUDE.md` + skill | sim, e usada | `SKILL-CARREGADA-OK` |
| `ONLY-SKILLS` | só a skill | sim, e usada | `SKILL-CARREGADA-OK` |

As versões de teste e o enunciado estão em `history/bench-test/`, com os
mesmos hashes gravados nos `meta.json`. Os nomes dessas execuções são os de antes da
escada: `ONLY-CLAUDE` corresponde ao N1, `CLAUDE-SKILLS` ao N2, e `ONLY-SKILLS` à
variante que saiu.

**Repetido em 06/10/2026**, depois das renomeações das pastas e dos níveis e das
mudanças no `executar.sh`: `TESTE-BANCADA-02-CONTROL` e `TESTE-BANCADA-02-N1`
(Haiku, esforço baixo, o mesmo enunciado, hash `b796f244`). O CONTROL recebeu o
workspace vazio e respondeu que não há `CLAUDE.md`; o N1, recebido **sem passar
`HARNESS`** (o padrão), chegou só com o `CLAUDE.md`, com o mesmo hash de 26/09
(`560577922737dbb9`), e o agente citou a primeira linha dele. A mesma imagem e a
mesma versão do Claude Code. O build das duas sai com o código 66 (*SEM POM*), o
esperado, porque o enunciado não pede código. Na mesma data, `HARNESS=N2`, `N3` e
`N4` foram recusados antes de gastar cota, sem criar pasta. (Naquela hora o N2
ainda não tinha skill, e o N3 e o N4 estavam vazios, com a numeração anterior à
troca descrita acima.) Uma pasta `.claude/skills/` vazia é
recusada antes de subir o container.

## De onde veio cada skill

| versão | skill | origem | versão ou commit | data |
|---|---|---|---|---|
| N2 e N3 | `gof-patterns` (25 arquivos, 896 KB), **intacta**, a mesma cópia nos dois | [grndlvl/software-patterns](https://github.com/grndlvl/software-patterns), `.claude/skills/gof-patterns/`; MIT, © 2025 grndlvl | `85e94a3bc19e9063a51b12289bb027a8bfbb13e8` | 06/10/2026 |

A licença e o registro de origem ficam em `experiment/third-party/gof-patterns/`,
fora da pasta do harness. Os exemplos completos da skill são os canônicos (State
com pedido, Strategy com pagamento) e caem perto do domínio das tarefas: está no
§6 do `OBJETIVO.md`.
