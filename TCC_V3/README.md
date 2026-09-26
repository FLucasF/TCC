# Experimento: harness × sem harness

Mede se um arquivo `CLAUDE.md` com orientação de processo altera o desenho que o
Claude Code produz, construindo uma API de checkout em Java/Spring do zero.

**18 execuções**: 3 modelos × 2 condições × 3 réplicas. A única diferença entre os
braços é um arquivo de 14 linhas copiado para a raiz do workspace.

| condição | o workspace começa com |
|---|---|
| `CONTROL` | nada |
| `HARNESS` | uma versão de `experimento/harnesses/`; padrão `only-claude/`, só o `CLAUDE.md` |

**Um experimento só**, com o enunciado de cinco pontos de variação (P1 a P5): as
execuções `EXT-01` a `EXT-03`. O lote `BATCH-01` a `03`, com o enunciado de três
pontos, é o **piloto**: foi ele que mostrou o efeito de teto e motivou P4 e P5.
Tudo o que é só do piloto está em `historico/piloto/`; as execuções dele continuam
em `runs/`, e o prefixo diz de qual são.

| | experimento (`EXT`) | piloto (`BATCH`) |
|---|---|---|
| enunciado | `experimento/prompt/prompt.md` | `historico/piloto/prompt.md` |
| leitura cega | `avaliacao/leitura-claude-cego-ext.csv` | `historico/piloto/leitura-claude-cego.csv` |
| mapa de anonimização | `avaliacao/mapa-anonimizacao.csv` | `historico/piloto/mapa-anonimizacao.csv` (semente 24) |
| pacotes cegos (fora do git) | `avaliacao/pacotes/` | `historico/piloto/pacotes/` |

---

## Rodar

Pré-requisitos: Docker Desktop, Git Bash, Node 18+, ~1,5 GB livre.

```bash
claude setup-token              # e o resultado num .env na raiz:
                                #   CLAUDE_CODE_OAUTH_TOKEN=<token>
docker build -f infra/docker/Dockerfile -t experimento-harness:v3 .
```

Uma execução:

```bash
infra/scripts/executar.sh SMOKE-00-OPUS-CONTROL claude-opus-5 CONTROL
```

Uma rodada — as **seis simultâneas**, que é o que torna a análise pareada possível:

```bash
infra/scripts/rodada.sh SMOKE-01
```

O lote são três rodadas, e a réplica é o segundo argumento:

```bash
infra/scripts/rodada.sh EXT-01 1
infra/scripts/rodada.sh EXT-02 2
infra/scripts/rodada.sh EXT-03 3
```

O `executar.sh` lê o enunciado de `experimento/prompt/prompt.md`, a não ser que
`PROMPT_FILE` aponte outro. Para repetir o piloto:
`PROMPT_FILE=historico/piloto/prompt.md infra/scripts/rodada.sh ...`.

O braço `HARNESS` recebe `experimento/harnesses/only-claude/`, a não ser que
`HARNESS` nomeie outra versão:
`HARNESS=claude-and-skills infra/scripts/rodada.sh ...`. Como montar uma versão
com skills está em [`experimento/harnesses/README.md`](experimento/harnesses/README.md).

Depois:

```bash
node infra/scripts/agregar.mjs --prefix EXT            # -> analise/resultados.csv
node avaliacao/ferramentas/anonimizar.mjs EXT-01-OPUS-CONTROL ... --seed N
```

A semente de cada lote fica registrada na última coluna do mapa dele. O
`anonimizar.mjs` sempre escreve em `avaliacao/`: para regenerar o piloto, guarde
antes o mapa que estiver lá.

---

## Os cinco scripts

| | |
|---|---|
| `infra/scripts/executar.sh` | uma execução: preflight, container, `claude -p`, e o build num segundo container **sem o token** |
| `infra/scripts/rodada.sh` | as seis de uma rodada, em paralelo |
| `infra/scripts/extrair-meta.mjs` | transcrição → `meta.json`, 103 campos |
| `infra/scripts/agregar.mjs` | os `meta.json` → CSV, 37 colunas |
| `avaliacao/ferramentas/anonimizar.mjs` | pacotes cegos: tira o `CLAUDE.md`, normaliza datas, embaralha |

**Nenhum deles olha o código para julgar.** Não há régua, nota, nem proposta de
descarte. O campo `valid` do `meta.json` nasce `null` e é preenchido por humano.

O porquê de cada decisão está nas mensagens de commit, datadas.

---

## Hashes, para conferir

O enunciado e o harness são identificados por conteúdo byte a byte. Confira
**depois de clonar ou copiar**, antes de rodar:

```bash
sha256sum experimento/prompt/prompt.md      # b7cdb594cb49efee...
sha256sum historico/piloto/prompt.md        # 53db3424b3972795...
docker image inspect --format '{{.Id}}' experimento-harness:v3
```

| | |
|---|---|
| `experimento/prompt/prompt.md` | `b7cdb594cb49efee4c0081e947a157b6bd2ebaecc015ec0e5b5f93d973f01e35` |
| `historico/piloto/prompt.md` | `53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124` |
| `experimento/harnesses/only-claude/` (árvore) | `560577922737dbb9252fe3dbd0e26d06e45a7abe6b455c002eae61f8ea24b882` |
| `infra/docker/Dockerfile` | `f9dd2d29f2038775d3a522e716e98d6044bf29eeead33f5812fea41bb578abdf` |
| imagem `experimento-harness:v3` | `sha256:54de317c40864b3ea2932396e6d492c63347e9ebf35a816616d572f699e2abd6` |
| Claude Code, na imagem | `2.1.269` |

| script | hash | se tiver defeito |
|---|---|---|
| `executar.sh` | `0efc44e6d2cceec8` | perde **a execução** |
| `rodada.sh` | `e6c65d2e4d84ede4` | perde **o pareamento** |
| `anonimizar.mjs` | `6f4e96ec116312e4` | perde **a cegueira** |
| `extrair-meta.mjs` | `8c1d1dd228ae5ddf` | nada — a transcrição sobrevive |
| `agregar.mjs` | `4e8001f7a3012b8d` | nada — o `meta.json` sobrevive |

O `executar.sh` era `be71fb1c98bdc14b` até ganhar a variável `HARNESS`. Os lotes
`SMOKE`, `BATCH`, `TESTE-P4` e `EXT` rodaram com essa versão; sem `HARNESS`, a nova
faz o mesmo, com o mesmo hash de harness.

> [!danger] `core.autocrlf` desta máquina é `true`
> O `.gitattributes` trata `experimento/**` e `historico/**` como binário por isso. Sem ele o git
> converteria fim de linha no commit e o hash mudaria **sem que uma palavra
> mudasse**.

---

## Coisas que quebram se você mexer sem saber

**Não restrinja ferramentas.** Sem `--disallowedTools`, `--allowedTools` nem
`--tools`. É decisão, não esquecimento: a pergunta é sobre o Claude Code como ele
vem.

**Não reaproveite `run_id`.** O script recusa se a pasta existir. Deu errado, cria
outra com id novo.

**Não rode as seis em sequência.** A simultaneidade é o que iguala horário e carga
de servidor entre os braços, e a análise compara pares.

**Alias e snapshot datado são o mesmo modelo.** Pede-se `claude-haiku-4-5` e volta
`claude-haiku-4-5-20251001`. Comparação estrita descartaria execuções boas.
`startsWith` não serve: `claude-opus-5-1` começa com `claude-opus-5`.

**A pasta da versão de harness é copiada inteira.** Um `.bak` ou uma nota
esquecidos em `experimento/harnesses/<versao>/` entram no workspace do agente e
contaminam o braço. Anotação sobre as versões fica em
`experimento/harnesses/README.md`, fora delas.

**Não edite `meta.json` à mão.** Ele é derivado da transcrição. Se um número
parecer errado, o conserto é no `extrair-meta.mjs` e rodar de novo — o dado bruto
está no `.jsonl`.

---

## O que falta, e não é engenharia

A **régua do desfecho**: como um pacote é lido para dizer se usou Strategy. Nenhum
script faz isso, por decisão. As execuções produzem os pacotes; a leitura é
humana e ainda não está definida.

Consequência: das cinco hipóteses, só a do **custo** (tokens e tempo) é respondida
pelo CSV. As outras quatro esperam essa régua.

As execuções com prefixo `SMOKE-` são de validação e estão **fora** da análise.
