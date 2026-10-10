# Experimento: harness × sem harness

Mede se um arquivo `CLAUDE.md` com orientação de processo altera o desenho que o
Claude Code produz, construindo uma API de checkout em Java/Spring do zero.

O **porquê** de cada escolha de projeto, e o que foi descartado, está em
[`DECISOES.md`](DECISOES.md).

**18 execuções**: 3 modelos × 2 condições × 3 réplicas. A única diferença entre os
braços é um arquivo de 14 linhas copiado para a raiz do workspace.

| condição | o workspace começa com |
|---|---|
| `CONTROL` | nada |
| `HARNESS` | um nível de `experiment/harnesses/`; padrão `N1/` (N1: só o `CLAUDE.md`). A escada N0 a N4 está no [README dos harnesses](experiment/harnesses/README.md) |

**Um experimento só**, com o enunciado de cinco pontos de variação (P1 a P5): as
execuções `EXT-01` a `EXT-03`. O TCC_V3 é a **bancada de testes**: o `EXT` valida a
bancada e os instrumentos, e o lote que vale vai rodar numa versão futura (V4), com
5 réplicas por célula. O lote `BATCH-01` a `03`, com o enunciado de três
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
infra/scripts/run-one.sh SMOKE-00-OPUS-CONTROL claude-opus-5 CONTROL
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

O `run-one.sh` lê o enunciado de `experiment/prompt/prompt.md`, a não ser que
`PROMPT_FILE` aponte outro. **O caminho precisa ser absoluto**: ele vira a origem
de uma montagem do Docker, que recusa caminho relativo, e a execução falharia
depois de já ter criado a pasta da run. Da raiz do `TCC_V3`, use `$PWD/`:

```bash
# o State, que saiu do V4 e está em history/state/ (ver o README de lá)
PROMPT_FILE=$PWD/history/state/state.md infra/scripts/rodada.sh STATE-01 1
# repetir o piloto
PROMPT_FILE=$PWD/history/pilot/prompt.md infra/scripts/rodada.sh ...
```

O braço `HARNESS` recebe `experiment/harnesses/N1/`, a não ser que
`HARNESS` nomeie outro nível:
`HARNESS=N2 infra/scripts/rodada.sh ...`. Como montar um nível
com skills está em [`experiment/harnesses/README.md`](experiment/harnesses/README.md).

Depois:

```bash
node infra/scripts/aggregate.mjs --prefix EXT --out analysis/resultados-ext.csv
node evaluation/tools/anonymize.mjs EXT-01-OPUS-CONTROL ... --seed N --padrao strategy
```

O `--out` é necessário: sem ele, o `aggregate.mjs` grava em `analysis/resultados.csv`,
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
| `infra/scripts/run-one.sh` | uma execução: preflight, container, `claude -p`, e o build num segundo container **sem o token** |
| `infra/scripts/rodada.sh` | as seis de uma rodada, em paralelo |
| `infra/scripts/extract-meta.mjs` | transcrição → `meta.json`, 103 campos |
| `infra/scripts/aggregate.mjs` | os `meta.json` → CSV, 37 colunas |
| `evaluation/tools/anonymize.mjs` | pacotes cegos: tira o `CLAUDE.md`, normaliza datas, embaralha |

As **métricas automáticas** (CK e SonarQube, secundárias) têm scripts próprios,
`evaluation/tools/metrics.sh` e `evaluation/tools/aggregate-metrics.mjs`, com as
versões travadas e o passo a passo no [README das ferramentas](evaluation/tools/README.md).

**Nenhum deles olha o código para julgar.** Nenhum aplica a régua, dá nota ou
propõe descarte: a régua é aplicada por quem lê. O campo `valid` do `meta.json`
nasce `null` e é preenchido por humano.

O porquê de cada decisão está nas mensagens de commit, datadas.

---

## Hashes, para conferir

O enunciado e o harness são identificados por conteúdo byte a byte. Confira
**depois de clonar ou copiar**, antes de rodar:

```bash
sha256sum experiment/prompt/prompt.md      # 8c70bb30493dbfbb...
sha256sum history/pilot/prompt.md        # 53db3424b3972795...
docker image inspect --format '{{.Id}}' experimento-harness:v3
```

| | |
|---|---|
| `experiment/prompt/prompt.md` | `8c70bb30493dbfbbde8b2d4be857669993335b0e4d6114c8c0b777ee00c69984` |
| `history/state/state.md` (o State do V4, que nunca rodou: saiu em 07/10) | `4591f7425e1551fd721a88e9053ac81b3f94bea180e45dd569e2dd3bdc35eb4e` |
| `history/prompt-v3/prompt.md` (rodou no `EXT`, nos `TESTE-STRATEGY` e nos `TESTE-P4`) | `b7cdb594cb49efee4c0081e947a157b6bd2ebaecc015ec0e5b5f93d973f01e35` |
| `history/prompt-v3/state.md` (rodou nos `TESTE-STATE`) | `ebffe1724ca316b55ea218ef53e3ba4c1928a2ee0b5137be4f36af753ef98580` |
| `history/pilot/prompt.md` | `53db3424b397279573658bfc048a369a33e0a2c8b71530252105e4f841bfd124` |
| `experiment/harnesses/N1/` (árvore; era `only-claude/`) | `560577922737dbb9252fe3dbd0e26d06e45a7abe6b455c002eae61f8ea24b882` |
| `experiment/harnesses/N2/` (árvore; com a skill `gof-patterns`) | `27987df0bd1febe2aff3731e15b9de2b5b7d35b6dd85cb46e3098518915389ac` |
| `experiment/harnesses/N3/` (árvore; o N2 + processo com revisor) | `5f4c492bf3d12f6811b648cc4ac15cac0b06513b47e069e6bc5d077acf4df3df` |
| `infra/docker/Dockerfile` | `f9dd2d29f2038775d3a522e716e98d6044bf29eeead33f5812fea41bb578abdf` |
| imagem `experimento-harness:v3` | `sha256:54de317c40864b3ea2932396e6d492c63347e9ebf35a816616d572f699e2abd6` |
| Claude Code, na imagem | `2.1.269` |

| script | hash (congelado em 09/10; a lista completa é o `CONGELADO-V4.sha256`) | se tiver defeito |
|---|---|---|
| `run-one.sh` (era `executar.sh`) | `f033395313c82fb1` (era `39d619c6a0469e2f`; mudaram só comentários, conferido no `git diff`) | perde **a execução** |
| `run-levels.sh` | `328b17c5b0431744` | perde **o quarteto** dos níveis N0 a N3 |
| `extract-meta.mjs` (era `extrair-meta.mjs`) | `a4c873d3f84f562d` (era `b6dfe6ad4eb05acc`; só comentários) | nada — a transcrição sobrevive |
| `aggregate.mjs` (era `agregar.mjs`) | `936cc9c005ef19e9` (era `19338b460497b34f`; só comentários) | nada — o `meta.json` sobrevive |
| `acceptance.sh` | `11674d3f51ecd922` | perde **a medida de correção** |
| `metrics.sh` (era `metricas.sh`) | `8c6ea09252a9a561` (era `135ecbe45c22afe4`; só comentários) | perde **as métricas automáticas** (CK, SonarQube) |
| `aggregate-metrics.mjs` (era `agregar-metricas.mjs`) | `a5ae0773cf9a9ff7` (era `74479258f31dce1f`; ganhou a coluna `nivel`) | nada — as saídas por execução sobrevivem |
| `anonymize.mjs` (era `anonimizar.mjs`) | `7ffddc046ad4e5c2` (era `4836f9213ad687a6`; o removedor de comentários foi para `sem-comentarios.mjs`, saída idêntica) | perde **a cegueira** |
| `rodada.sh` (fora do V4) | `ff82b1c8a50357da` | perde **o pareamento** |

### Os scripts do V4, na ordem em que entram

| etapa | script | situação |
|---|---|---|
| rodar | `experiment/ordem-v4.csv`: a ordem dos 25 quartetos, com o comando de cada um (sorteada por `infra/scripts/draw-order.mjs`, semente 20261009); depois, `experiment/ordem-v4-exploratorio.csv`, o lote exploratório (semente 20261010) | sorteadas em 09/10 |
| rodar | `infra/scripts/run-levels.sh`: um quarteto (os níveis N0 a N3 de um modelo, juntos), com o modelo pelo apelido do desenho | congelado em 09/10 |
| rodar | `infra/scripts/run-one.sh`: uma execução (o `run-levels.sh` chama) | congelado em 09/10 |
| rodar | `infra/scripts/extract-meta.mjs`: a transcrição vira `meta.json` (o `run-one.sh` chama) | congelado |
| medir custo | `infra/scripts/aggregate.mjs`: os `meta.json` do lote num CSV | congelado |
| medir correção | `infra/scripts/acceptance.sh`: a suíte em todas as execuções | congelado em 09/10 |
| medir desenho | `evaluation/tools/semgrep/detect.sh`: o padrão do P1 ao P5, sem IA (versão 2; `corpus/validar.sh` refaz a validação) | congelado em 09/10 |
| medir qualidade | `evaluation/tools/metrics.sh` e `aggregate-metrics.mjs`: CK e SonarQube | congelados |
| ler às cegas | `evaluation/tools/anonymize.mjs --sem-comentarios`: os pacotes cegos, sem comentários | congelado em 09/10 |
| ler às cegas | `evaluation/tools/sample.mjs`: sorteia os 20 pacotes do Lucas e, depois, a releitura | congelado em 09/10 |
| conferir | `evaluation/tools/compare.mjs`: a leitura do Lucas × o Semgrep, e a regra de saída | congelado em 09/10 |
| resumir | `evaluation/tools/nota.mjs`: a nota de 0 a 100 de cada execução | congelado em 09/10 |
| decidir | `evaluation/tools/hipoteses.mjs`: os pares, o teto e o veredito de cada hipótese pelas regras do §4.1 do `OBJETIVO`; a prova é o `hipoteses-teste.mjs` (17 de 17) | congelado em 09/10 |
| conferir | `infra/scripts/verify.mjs`: as fontes do lote batem entre si (o desenho em `experiment/desenho-v4.json`); a prova de que acusa é o `verify-teste.mjs` | congelado em 09/10 |

O `blind-read.sh` (a leitura do Claude isolada num container) não é mais preciso: desde 09/10 o Claude não lê os pacotes.

Validam os instrumentos, sem medir o V4: `validate-mutants.mjs` e
`check-prompt.mjs`, em `evaluation/acceptance-prototype/`. Ficam fora do V4:
o `rodada.sh` (o par antigo, que o quarteto substitui) e o `analisar-rodada.mjs`
(resumo de rodada da bancada, coberto pelo `aggregate.mjs` e pelo `verify.mjs`).

Em 07/10/2026 os scripts do V4 foram para o inglês: `executar.sh` → `run-one.sh`,
`extrair-meta.mjs` → `extract-meta.mjs`, `agregar.mjs` → `aggregate.mjs`,
`metricas.sh` → `metrics.sh`, `agregar-metricas.mjs` → `aggregate-metrics.mjs`,
`anonimizar.mjs` → `anonymize.mjs` e, na suíte, `validar-mutantes.mjs` →
`validate-mutants.mjs`, `mutantes.mjs` → `mutants.mjs`, `conferir-enunciado.mjs` →
`check-prompt.mjs` (e o `rodada-niveis.sh` → `run-levels.sh`, antes). Os scripts
citam uns aos outros e a si mesmos nos comentários de uso, então os bytes mudaram
**só nos nomes**: desfeita a troca de nomes, cada um é byte a byte igual à versão
anterior (conferido pelo hash). Hashes anteriores: `executar.sh` `6bd76c6e96de9d84`,
`rodada.sh` `e6c65d2e4d84ede4`, `anonimizar.mjs` `ec6bfe8ecc37abb0`,
`extrair-meta.mjs` `8c1d1dd228ae5ddf`, `agregar.mjs` `a78b48b2e2ec5a44`,
`metricas.sh` `f8e34e5a1777992b`, `agregar-metricas.mjs` `850afb08da82743a`. Os
lotes já rodados (o `EXT` e os de teste) rodaram com os nomes antigos; os registros
datados e o histórico continuam citando esses nomes.

As peças travadas das métricas (o `.jar` do CK, as imagens do SonarQube e do
scanner, o perfil de regras) estão, com hash, no [README das ferramentas](evaluation/tools/README.md).

Em 05/10/2026 as pastas foram renomeadas para inglês (`analise` → `analysis`,
`avaliacao` → `evaluation`, `experimento` → `experiment`, `historico` → `history`,
e as de dentro). Três scripts citam pastas e mudaram **só nos caminhos**:
`run-one.sh` era `0efc44e6d2cceec8`, `anonymize.mjs` era `bcf480270b4984d2`,
`aggregate.mjs` era `4e8001f7a3012b8d`. Conferido que fazem o mesmo: o `run-one.sh`
calcula na pasta nova o mesmo hash de harness (`560577922737dbb9`) e de enunciado
(`b7cdb594cb49efee`); o `anonymize.mjs` refaz o piloto com a semente 24 com os
mesmos bytes (pacotes e mapa); o `aggregate.mjs` refaz o CSV do piloto idêntico. O
caminho **dentro** do container (`/experimento/prompt.md`) não mudou, para o lote
STATE ver o mesmo ambiente que o EXT viu. Ficaram com o nome em português, de
propósito: `infra/docker/aquecimento/`, porque o `Dockerfile` a copia para a imagem
(renomear exigiria reconstruir a imagem), e as pastas internas dos harnesses de
`history/bench-test/`, cujos caminhos entram no hash gravado naqueles `meta.json`.

Em 06/10/2026 os harnesses viraram os níveis da escada do orientador (`only-claude/`
→ `N1/`, `claude-and-skills/` → `N2/`; `only-skills/`
saiu sem nunca ter rodado). O `run-one.sh` mudou em duas linhas: o harness padrão
(`only-claude` → `N1`) e a validação do nome, que passou a aceitar maiúsculas
(continua recusando espaço, barra e `..`). Era `e571cc45da3c98db`. O hash da árvore
do `N1/` é o mesmo do `only-claude/` (`560577922737dbb9`), conferido na pasta nova.
No mesmo dia, o N2 recebeu a skill `gof-patterns`, o N3 virou o nível de processo
(desenho antes do código e um revisor) e a verificação automática foi descartada,
indo para o N4; a escada e os motivos estão no README dos harnesses.

Também em 06/10/2026, os enunciados do V4: nos dois, o cliente "entende o
básico" e montou a parte técnica pesquisando, no lugar do "desenvolvedor do site"
e do "time técnico"; no do Strategy, as três inconsistências do §6 do OBJETIVO
foram corrigidas (exemplos 1 a 4, resposta do anexo, limite do boleto). Em
07/10, o imposto por região do Strategy virou seguro por região, com a mesma
forma: imposto somado no checkout não existe no Brasil. As versões que rodaram na
bancada foram para `history/prompt-v3/`, com os mesmos hashes; o que mudou e por
quê está no [README do enunciado](experiment/prompt/README.md). A suíte do
Strategy acompanhou: 21 casos, sem observações, 17 de 17 mutantes. No mesmo dia,
o State saiu do V4, que ficou com um padrão só (60 execuções): ele serviu para
provar que a bancada aceita um segundo padrão, e o material dele está em
`history/state/`.

O `run-one.sh` era `be71fb1c98bdc14b` até ganhar a variável `HARNESS`. Os lotes
`SMOKE`, `BATCH`, `TESTE-P4` e `EXT` rodaram com essa versão; sem `HARNESS`, a nova
faz o mesmo, com o mesmo hash de harness.

O `anonymize.mjs` era `6f4e96ec116312e4` até ganhar `--padrao`. Os pacotes e
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
parecer errado, o conserto é no `extract-meta.mjs` e rodar de novo — o dado bruto
está no `.jsonl`.

---

## O V4 congelado (09/10/2026)

O pré-registro do V4 foi **congelado em 09/10/2026, antes da primeira execução**: o
`OBJETIVO`, o passo a passo (`COMO-RODAR-V4.md`), a régua, o guia, o gabarito, o
enunciado, os três harnesses, os dois desenhos e as duas ordens, a suíte, o Semgrep, as
métricas e todos os scripts, inclusive o da análise (`hipoteses.mjs`). O commit de
congelamento é a prova; a lista completa, com o sha256 de cada um dos 113 arquivos, é o
[`CONGELADO-V4.sha256`](CONGELADO-V4.sha256). Para conferir uma cópia (no `TCC_V4`, ou
depois de clonar):

```bash
sha256sum -c CONGELADO-V4.sha256 --quiet     # sem saída: tudo idêntico
```

| o que | sha256 (16 primeiros) |
|---|---|
| `OBJETIVO.md` | `fee6d667c534d6ff` |
| `COMO-RODAR-V4.md` | `b20a13d688842706` |
| `evaluation/regua.md` (versão 4) | `92056c336412805d` |
| `evaluation/GUIA-DA-REGUA.md` | `a4e31883b69a1ae3` |
| `evaluation/strategy/gabarito.md` | `514ab32688ebe7ba` |
| `experiment/prompt/prompt.md` | `8c70bb30493dbfbb` |
| `experiment/desenho-v4.json` / `desenho-v4-exploratorio.json` | `69ddd55f64e57b47` / `39ce6195d9b29cb2` |
| `experiment/ordem-v4.csv` / `ordem-v4-exploratorio.csv` | `a4459f47d0bc5dd9` / `3cb53de49a4633d2` |
| a suíte (`strategy.mjs`) e a calculadora (`ref-strategy.mjs`) | `42ee8a2e4a61a00e` / `76cfc53aa8f4760a` |
| o Semgrep (`regras.yml` e `classificar.mjs`) | `4cf151b183fd67a1` / `bc9db272266da555` |
| a análise (`hipoteses.mjs`) | `e661a09577ddf81f` |
| os harnesses N1, N2, N3 (árvore, como o `run-one.sh` calcula) | `560577922737dbb9`, `27987df0bd1febe2`, `5f4c492bf3d12f68` |
| a imagem `experimento-harness:v3` (Claude Code 2.1.269) | `sha256:54de317c40864b3e…` |

**Daqui em diante nada disso muda.** Uma mudança vira emenda datada no fim do
`OBJETIVO`, com o motivo, e as conclusões dizem o que foi lido antes e depois dela.

## O que falta, e não é engenharia

Rodar, pelo [`COMO-RODAR-V4.md`](COMO-RODAR-V4.md), na pasta `TCC_V4`. O porquê de cada
escolha está no [`DECISOES.md`](DECISOES.md); as hipóteses e as regras de leitura, no
[`OBJETIVO.md`](OBJETIVO.md); o histórico da preparação, no
[`MINIPLANO-V4.md`](MINIPLANO-V4.md).

A **régua** está em [`evaluation/regua.md`](evaluation/regua.md), versão 4 (enxuta: 4
perguntas, no P4 e no P5), com o gabarito em `evaluation/strategy/gabarito.md` e o guia
em `evaluation/GUIA-DA-REGUA.md`; calibrada pelo Lucas em 2 pacotes em 09/10
(`evaluation/calibration-v4/`) e congelada com o `OBJETIVO`. A versão 3 (8 propriedades,
calibrada por dois leitores automáticos, [relatório](evaluation/calibracao-relatorio.md))
está no histórico do git.

As execuções com prefixo `SMOKE-` e `TESTE-` são de validação e estão **fora** da
análise; as pastas `evaluation/calibration-*` guardam a calibração da régua antiga,
menos a `calibration-v4/`, que é a da régua enxuta.
