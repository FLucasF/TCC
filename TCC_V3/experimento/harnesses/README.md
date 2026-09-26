# Os harnesses

Cada pasta aqui é **uma versão de harness**: o que o agente recebe no braço
`HARNESS`. A pasta inteira é copiada para a raiz do workspace, e o hash dela vai
para `environment.harness_hash` no `meta.json`.

| versão | o agente recebe | estado |
|---|---|---|
| `only-claude/` | o `CLAUDE.md` com as 4 regras sobre variação | em uso: lotes `EXT-01` a `03`, hash `560577922737dbb9` |
| `claude-and-skills/` | o mesmo `CLAUDE.md` + skills | pronta para receber skills |
| `only-skills/` | só skills, sem `CLAUDE.md` | pronta para receber skills |

O `CLAUDE.md` de `claude-and-skills/` é cópia byte a byte do de `only-claude/`.
Mantê-lo igual é o que permite atribuir às skills a diferença entre as duas.

## Rodar com uma versão

```bash
HARNESS=claude-and-skills infra/scripts/rodada.sh STATE-01 1
```

Sem `HARNESS`, vale `only-claude`. O `CONTROL` ignora a variável.

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

## De onde veio cada skill

| versão | skill | origem | versão ou commit | data |
|---|---|---|---|---|
| | | | | |
