# Os harnesses

Cada pasta aqui é **um nível de harness**: o que o agente recebe no braço
`HARNESS`. A pasta inteira é copiada para a raiz do workspace, e o hash dela vai
para `environment.harness_hash` no `meta.json`.

## A escada: N0 a N4

Os níveis seguem a proposta do orientador (12/09/2026). Cada nível **acumula** o
anterior, e o tipo diz como o harness age: uma instrução o modelo lê e pode
ignorar; um sensor devolve retorno objetivo.

| nível | o que acrescenta | tipo | pasta | estado |
|---|---|---|---|---|
| **N0** | nada: só o enunciado | — | nenhuma: é o braço `CONTROL` | rodado, lotes `EXT` |
| **N1** | orientação: o `CLAUDE.md` com 4 regras sobre variação | instrução | `N1/` | rodado, lotes `EXT-01` a `03`, hash `560577922737dbb9` |
| **N2** | conhecimento: o N1 + skills | instrução | `N2/` | pronta, sem skill |
| **N3** | verificação automática: o N2 + build, testes ou regras que rodam sozinhos | sensor | `N3/` | vazia, a montar |
| **N4** | processo: especificação, desenho e tarefas, com revisor independente | processo | `N4/` | vazia, a montar |

O `CLAUDE.md` de `N2/` é cópia byte a byte do de `N1/`: manter igual é o que
permite atribuir às skills a diferença entre os dois. O `N3/`, quando montado,
leva o mesmo `CLAUDE.md` e as mesmas skills do `N2/`, mais a verificação (por
exemplo, um `.claude/settings.json` com um hook que roda `mvn verify` quando o
agente diz que terminou).

Uma pasta sem `CLAUDE.md` nem skill é recusada pelo `executar.sh`, e uma
`.claude/skills/` vazia também. Por isso nem o `N2/` (sem skill) nem o `N3/`
e o `N4/` (vazios) rodam por engano.

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
variante que saiu. Uma pasta `.claude/skills/` vazia é
recusada antes de subir o container.

## De onde veio cada skill

| versão | skill | origem | versão ou commit | data |
|---|---|---|---|---|
| | | | | |
