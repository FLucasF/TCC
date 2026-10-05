# Experimento: harness × sem harness

Mede se um arquivo `CLAUDE.md` com orientação de processo altera o desenho que o
Claude Code produz, construindo uma API de checkout em Java/Spring do zero.

**18 execuções**: 3 modelos × 2 condições × 3 réplicas. A única diferença entre os
braços é um arquivo de 14 linhas copiado para a raiz do workspace.

| condição | o workspace começa com |
|---|---|
| `CONTROL` | nada |
| `HARNESS` | uma versão de `experiment/harnesses/`; padrão `only-claude/`, só o `CLAUDE.md` |

**Um experimento só**, com o enunciado de cinco pontos de variação (P1 a P5): as
execuções `EXT-01` a `EXT-03`. O lote `BATCH-01` a `03`, com o enunciado de três
pontos, é o **piloto**: foi ele que mostrou o efeito de teto e motivou P4 e P5.
Tudo o que é só do piloto está em `history/pilot/`; as execuções dele continuam
em `runs/`, e o prefixo diz de qual são.

| | experimento (`EXT`) | piloto (`BATCH`) |
|---|---|---|
| enunciado | `experiment/prompt/prompt.md` | `history/pilot/prompt.md` |
| gabarito | `evaluation/strategy/gabarito.md` | — |
| leitura cega | `evaluation/strategy/leitura-claude-cego-ext.csv` | `history/pilot/leitura-claude-cego.csv` |
| mapa de anonimização | `evaluation/strategy/mapa-anonimizacao.csv` | `history/pilot/mapa-anonimizacao.csv` (semente 24) |
| pacotes cegos (fora do git) | `evaluation/strategy/packages/` | `history/pilot/packages/` |

Cada padrão testado tem a sua pasta em `evaluation/`, com o gabarito junto dos
pacotes que ele lê. A régua, comum a todos, é `evaluation/regua.md`.

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

O `executar.sh` lê o enunciado de `experiment/prompt/prompt.md`, a não ser que
`PROMPT_FILE` aponte outro. **O caminho precisa ser absoluto**: ele vira a origem
de uma montagem do Docker, que recusa caminho relativo, e a execução falharia
depois de já ter criado a pasta da run. Da raiz do `TCC_V3`, use `$PWD/`:

```bash
# o segundo padrão (State)
PROMPT_FILE=$PWD/experiment/prompt/state.md infra/scripts/rodada.sh STATE-01 1
# repetir o piloto
PROMPT_FILE=$PWD/history/pilot/prompt.md infra/scripts/rodada.sh ...
```

O braço `HARNESS` recebe `experiment/harnesses/only-claude/`, a não ser que
`HARNESS` nomeie outra versão:
`HARNESS=claude-and-skills infra/scripts/rodada.sh ...`. Como montar uma versão
com skills está em [`experiment/harnesses/README.md`](experiment/harnesses/README.md).

Depois:

```bash
node infra/scripts/agregar.mjs --prefix EXT --out analysis/resultados-ext.csv
node evaluation/tools/anonimizar.mjs EXT-01-OPUS-CONTROL ... --seed N --padrao strategy
```

O `--out` é necessário: sem ele, o `agregar.mjs` grava em `analysis/resultados.csv`,
que é o CSV do **piloto**, e o sobrescreveria. O CSV do EXT traz os custos (as hipóteses de
custo), então só é gerado na fase 2 do plano, com o `OBJETIVO.md` congelado.

Com `--padrao`, pacotes e mapa vão para `evaluation/<padrao>/`, ao lado do
gabarito, e o script **recusa** se já houver um mapa lá. Sem `--padrao`, grava em
`evaluation/`, como sempre gravou; é assim que o piloto se regenera, e o resultado
é idêntico a `history/pilot/`.

A semente de cada lote fica registrada na última coluna do mapa dele.

---

## Os cinco scripts

| | |
|---|---|
| `infra/scripts/executar.sh` | uma execução: preflight, container, `claude -p`, e o build num segundo container **sem o token** |
| `infra/scripts/rodada.sh` | as seis de uma rodada, em paralelo |
| `infra/scripts/extrair-meta.mjs` | transcrição → `meta.json`, 103 campos |
| `infra/scripts/agregar.mjs` | os `meta.json` → CSV, 37 colunas |
| `evaluation/tools/anonimizar.mjs` | pacotes cegos: tira o `CLAUDE.md`, normaliza datas, embaralha |

**Nenhum deles olha o código para julgar.** Nenhum aplica a régua, dá nota ou
propõe descarte: a régua é aplicada por quem lê. O campo `valid` do `meta.json`
nasce `null` e é preenchido por humano.

O porquê de cada decisão está nas mensagens de commit, datadas.

---

## Hashes, para conferir

O enunciado e o harness são identificados por conteúdo byte a byte. Confira
**depois de clonar ou copiar**, antes de rodar:

```bash
sha256sum experiment/prompt/prompt.md      # b7cdb594cb49efee...
sha256sum history/pilot/prompt.md        # 53db3424b3972795...
docker image inspect --format '{{.Id}}' experimento-harness:v3
```

| | |
|---|---|
| `experiment/prompt/prompt.md` | `b7cdb594cb49efee4c0081e947a157b6bd2ebaecc015ec0e5b5f93d973f01e35` |
| `history/pilot/prompt.md` | `53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124` |
| `experiment/prompt/state.md` (segundo padrão, a rodar) | `ebffe1724ca316b55ea218ef53e3ba4c1928a2ee0b5137be4f36af753ef98580` |
| `experiment/harnesses/only-claude/` (árvore) | `560577922737dbb9252fe3dbd0e26d06e45a7abe6b455c002eae61f8ea24b882` |
| `infra/docker/Dockerfile` | `f9dd2d29f2038775d3a522e716e98d6044bf29eeead33f5812fea41bb578abdf` |
| imagem `experimento-harness:v3` | `sha256:54de317c40864b3ea2932396e6d492c63347e9ebf35a816616d572f699e2abd6` |
| Claude Code, na imagem | `2.1.269` |

| script | hash | se tiver defeito |
|---|---|---|
| `executar.sh` | `e571cc45da3c98db` | perde **a execução** |
| `rodada.sh` | `e6c65d2e4d84ede4` | perde **o pareamento** |
| `anonimizar.mjs` | `ec6bfe8ecc37abb0` | perde **a cegueira** |
| `extrair-meta.mjs` | `8c1d1dd228ae5ddf` | nada — a transcrição sobrevive |
| `agregar.mjs` | `a78b48b2e2ec5a44` | nada — o `meta.json` sobrevive |

Em 05/10/2026 as pastas foram renomeadas para inglês (`analise` → `analysis`,
`avaliacao` → `evaluation`, `experimento` → `experiment`, `historico` → `history`,
e as de dentro). Três scripts citam pastas e mudaram **só nos caminhos**:
`executar.sh` era `0efc44e6d2cceec8`, `anonimizar.mjs` era `bcf480270b4984d2`,
`agregar.mjs` era `4e8001f7a3012b8d`. Conferido que fazem o mesmo: o `executar.sh`
calcula na pasta nova o mesmo hash de harness (`560577922737dbb9`) e de enunciado
(`b7cdb594cb49efee`); o `anonimizar.mjs` refaz o piloto com a semente 24 com os
mesmos bytes (pacotes e mapa); o `agregar.mjs` refaz o CSV do piloto idêntico. O
caminho **dentro** do container (`/experimento/prompt.md`) não mudou, para o lote
STATE ver o mesmo ambiente que o EXT viu. Ficaram com o nome em português, de
propósito: `infra/docker/aquecimento/`, porque o `Dockerfile` a copia para a imagem
(renomear exigiria reconstruir a imagem), e as pastas internas dos harnesses de
`history/bench-test/`, cujos caminhos entram no hash gravado naqueles `meta.json`.

O `executar.sh` era `be71fb1c98bdc14b` até ganhar a variável `HARNESS`. Os lotes
`SMOKE`, `BATCH`, `TESTE-P4` e `EXT` rodaram com essa versão; sem `HARNESS`, a nova
faz o mesmo, com o mesmo hash de harness.

O `anonimizar.mjs` era `6f4e96ec116312e4` até ganhar `--padrao`. Os pacotes e
mapas do `BATCH` e do `EXT` foram gerados com essa versão; sem `--padrao`, a nova
gera os mesmos bytes (conferido regenerando o piloto com a semente 24).

> [!danger] `core.autocrlf` desta máquina é `true`
> O `.gitattributes` trata `experiment/**` e `history/**` como binário por isso. Sem ele o git
> converteria fim de linha no commit e o hash mudaria **sem que uma palavra
> mudasse**.

---

## Coisas que quebram se você mexer sem saber

**Não restrinja ferramentas.** Sem `--disallowedTools`, `--allowedTools` nem
`--tools`. É decisão, não esquecimento: a pergunta é sobre o Claude Code como ele
vem.

**Não reaproveite `run_id`.** O script recusa se a pasta existir. Deu errado, cria
outra com id novo.

**Não rode lote com a assinatura em uso em outro lugar.** As execuções usam o mesmo
token de assinatura de uma sessão interativa do Claude Code. Se a cota acabar no
meio, as seis recebem `You've hit your session limit` (HTTP 429) e terminam como
`error`: falha de infraestrutura, que não vale e se refaz com id novo. Aconteceu na
`TESTE-STATE-01`.

**Não rode as seis em sequência.** A simultaneidade é o que iguala horário e carga
de servidor entre os braços, e a análise compara pares.

**Alias e snapshot datado são o mesmo modelo.** Pede-se `claude-haiku-4-5` e volta
`claude-haiku-4-5-20251001`. Comparação estrita descartaria execuções boas.
`startsWith` não serve: `claude-opus-5-1` começa com `claude-opus-5`.

**A pasta da versão de harness é copiada inteira.** Um `.bak` ou uma nota
esquecidos em `experiment/harnesses/<versao>/` entram no workspace do agente e
contaminam o braço. Anotação sobre as versões fica em
`experiment/harnesses/README.md`, fora delas.

**O Docker Desktop pode cair ao abrir, depois de ser fechado sem desligar direito.**
O erro fala em `remove ...\AppData\Local\Docker\run\sailor-ingest.sock: Não é
possível o acesso ao arquivo pelo sistema` (ou em `docker-secrets-engine\engine.sock`).
Sobraram arquivos de conexão que o Windows não deixa apagar. **Não clique em "Reset to
factory defaults"**: isso apaga a imagem `experimento-harness:v3`, e o ID dela está
nesta página. O que resolve: fechar o Docker Desktop, `wsl --shutdown`, e
**renomear** as pastas `%LOCALAPPDATA%\Docker\run` e
`%LOCALAPPDATA%\docker-secrets-engine` (por exemplo, com o sufixo `.parado`). Ao
abrir, o Docker cria as duas de novo. Reiniciar o Windows também libera os arquivos.

**Não edite `meta.json` à mão.** Ele é derivado da transcrição. Se um número
parecer errado, o conserto é no `extrair-meta.mjs` e rodar de novo — o dado bruto
está no `.jsonl`.

---

## O que falta, e não é engenharia

A **régua do desfecho** existe em rascunho: [`evaluation/regua.md`](evaluation/regua.md),
versão 3, com o gabarito do Strategy em `evaluation/strategy/gabarito.md`. Ela foi
calibrada em três rodadas sobre pacotes fora da análise, por dois leitores
automáticos, até nenhuma célula ficar `indeterminado`
([relatório](evaluation/calibracao-relatorio.md)). **Ainda não está congelada:**
falta a leitura humana de calibração e o hash dela neste README. Nenhum script lê
por ela, por decisão.

Consequência: das hipóteses do [`OBJETIVO.md`](OBJETIVO.md), só as de **custo**
(tokens e tempo) são respondidas pelo CSV. As de desenho esperam a régua
congelada, e as de correção esperam a suíte de aceitação.

As execuções com prefixo `SMOKE-` e `TESTE-` são de validação e estão **fora** da
análise; as pastas `evaluation/calibracao-*` guardam a calibração da régua feita
sobre elas e sobre o piloto.
