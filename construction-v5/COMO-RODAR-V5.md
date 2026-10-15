# Como rodar o V5, passo a passo

> **CONGELADO em 10/10/2026**, junto com o `OBJETIVO`, antes da primeira execução. Vai
> para o `TCC_V5` com o mesmo nome. Todos os comandos são do Git Bash, a partir da raiz do
> `TCC_V5`. O porquê de cada passo está no `OBJETIVO.md` e no `DECISOES.md`; aqui fica
> só **o que fazer, em que ordem, e o que conferir**. Substitui o `COMO-RODAR-V4.md`: um
> lote só (`V5-STRATEGY`), 11 modelos, 55 quartetos, 220 execuções, imagem `v5`.

## As sementes (registradas aqui, antes de rodar)

| sorteio | semente | onde entra |
|---|---|---|
| a ordem dos 55 quartetos | **20261020** | já sorteada: `experiment/ordem-v5.csv` |
| os códigos cegos | **20261021** | passo 3 |
| os 44 pacotes que o Lucas lê | **20261022** | passo 3 |
| a releitura (11 dos 44) | escolhida **no dia**, e registrada no commit | passo 4 |

## 0. Antes de cada sessão de rodadas

- [ ] O Docker aberto, com a imagem `experimento-harness:v5` (o `verify.mjs` confere o ID).
- [ ] O SonarQube **parado** (ele disputa memória com os 4 containers):
  `docker stop tcc-sonarqube`.
- [ ] Nenhuma variável `PROMPT_FILE`, `IMAGE`, `EFFORT` ou `DESENHO` no terminal
  (o `run-levels.sh` recusa as duas primeiras no lote, mas é melhor nem tê-las).
- [ ] **A janela de 5 h** com folga para o modelo do próximo quarteto: começar só se a
  janela livre for pelo menos **1,5 × o maior gasto medido daquele modelo + 5 pontos**
  (emenda de operação de 10/10, `DECISOES.md`). O maior gasto medido de cada modelo, em
  % da janela, atualizado a cada quarteto:

  | modelo | maior gasto medido | precisa livre |
  |---|---|---|
  | Opus 4.6 | 23% | 40% |
  | Opus 4.7 | ~16% (estimado) | 29% |
  | Opus 4.8 | 13% | 25% |
  | Opus 5 | 13% | 25% |
  | Opus 5.5 | 6% (no ensaio `TESTE-V5-01`, 10/10; com o trabalho da conversa junto) | 14% |
  | Sonnet 4.5 | 10% | 20% |
  | Sonnet 4.6 | 15% | 28% |
  | Sonnet 5 | 7% | 16% |
  | Sonnet 5.5 | 3% | 10% |
  | Haiku 4.5 | 7% | 16% |
  | Haiku 5.5 | ~6% (estimado) | 14% |

  Os números são da conta como ela estava no V4; se o plano mudar, eles são medidos de
  novo no primeiro quarteto de cada modelo.

## 1. Os 55 quartetos

Um quarteto de cada vez, **na ordem** de `experiment/ordem-v5.csv`: a coluna `comando`
traz a linha pronta. O primeiro:

```bash
infra/scripts/run-levels.sh V5-STRATEGY-01 1 SONNET46     # o primeiro da ordem
```

**Depois de cada quarteto**, olhe o resumo que ele imprime no fim:

- os 4 com `codigo=0` e `meta.json: completed`: **ok**, siga para o próximo;
- algum com `error`/`interrupted`, ou a mensagem *"You've hit your session limit"*: o
  quarteto foi **cortado**. Ele é refeito **inteiro**:
  1. mova as 4 pastas para fora de `runs/`: `mkdir -p runs-descartadas && mv runs/<prefixo>-<APELIDO>-N* runs-descartadas/`;
  2. registre no `DECISOES.md` (data, quarteto, motivo);
  3. espere a janela e rode o mesmo comando de novo.
- algum com `completed` mas sem compilar (`build_ok=false`): **não** se refaz. É dado
  do modelo (a nota dele será 0).

**Depois de cada rodada** (a cada 11 quartetos, uma réplica de cada modelo):

1. Procure um `.git` que um agente tenha criado dentro do workspace. Se houver, o git
   guarda a pasta só como ponteiro, sem o código (aconteceu no V4):
   ```bash
   find runs -maxdepth 3 -name .git
   ```
   Para cada um: `mv runs/<id>/workspace/.git runs/<id>/git-do-agente`.
2. Confira que nenhum `.env` entra, e registre no git:
   ```bash
   git add runs/V5-STRATEGY-0N-* runs/logs/V5-STRATEGY-0N-* && git commit -m "dados(v5): rodada N"
   git ls-files -s runs | awk '$1=="160000"'     # tem de sair vazio (nenhum ponteiro)
   ```
3. `node infra/scripts/verify.mjs experiment/desenho-v5.json` acusa só as réplicas que
   ainda faltam; qualquer outro erro, **pare**.

## 2. Depois dos 55: correção, custo, qualidade e coerência

```bash
node infra/scripts/aggregate.mjs --prefix V5-STRATEGY --out analysis/resultados.csv   # custo e processo
infra/scripts/acceptance.sh V5-STRATEGY                                               # correção: a suíte nas 220
docker start tcc-sonarqube                                                             # espere ~1 min ficar UP
evaluation/tools/metrics.sh V5-STRATEGY                                                # qualidade: CK e SonarQube
docker stop tcc-sonarqube
node infra/scripts/verify.mjs experiment/desenho-v5.json                               # tem de sair "tudo coerente"
```

Se o `verify` acusar algo, **pare** e resolva antes de seguir. Commit:
`runs/*/acceptance.txt` e `evaluation/metrics/V5-STRATEGY/metricas.csv`.

## 3. As cópias cegas, o Semgrep e o sorteio

```bash
node evaluation/tools/anonymize.mjs $(ls runs | grep '^V5-STRATEGY-') --seed 20261021 --padrao strategy --sem-comentarios
evaluation/tools/semgrep/detect.sh evaluation/strategy/packages analysis/semgrep-V5-STRATEGY.csv
node infra/scripts/verify.mjs experiment/desenho-v5.json --mapa evaluation/strategy/mapa-anonimizacao.csv --semgrep analysis/semgrep-V5-STRATEGY.csv
node evaluation/tools/sample.mjs amostra evaluation/strategy/mapa-anonimizacao.csv evaluation/reading --seed 20261022
```

- O `verify` com `--semgrep` **recusa** o lote se o Semgrep não leu código nenhum de
  algum pacote (aviso `sem-codigo`): é defeito do instrumento, não do modelo, e se
  resolve antes do sorteio (o ensaio do V4 mostrou por quê).
- O mapa (`evaluation/strategy/mapa-anonimizacao.csv`) fica fora do git e **o Lucas não
  abre**. Os scripts leem; ninguém olha.
- **Commit agora**, antes de qualquer leitura: `analysis/semgrep-V5-STRATEGY.csv`,
  `evaluation/reading/amostra.csv` e a planilha em branco.

## 4. A leitura do Lucas e a conferência

1. O Lucas lê os 44 códigos de `evaluation/reading/amostra.csv`, em
   `evaluation/strategy/packages/<CODIGO>/`, com a régua e o guia (o passo 1 do guia acha
   o projeto pelo `pom.xml`), e preenche `evaluation/reading/leitura-lucas.csv`. Sem olhar
   o CSV do Semgrep, e sem abrir o ensaio do V4 (`TCC_V4/analysis/ensaio/`).
2. **Commit da planilha preenchida.**
3. A conferência e a coerência:

```bash
node evaluation/tools/compare.mjs evaluation/reading/leitura-lucas.csv analysis/semgrep-V5-STRATEGY.csv > analysis/conferencia-V5-STRATEGY.md
node infra/scripts/verify.mjs experiment/desenho-v5.json --mapa evaluation/strategy/mapa-anonimizacao.csv --amostra evaluation/reading --semgrep analysis/semgrep-V5-STRATEGY.csv
```

4. **Commit da conferência.** A regra de saída (90%: **40 de 44**) vale como saiu:
   nenhuma regra do Semgrep é corrigida para refazer esta conta.
5. Só então o mapa entra no git: `git add -f evaluation/strategy/mapa-anonimizacao.csv`.
6. A nota (uma execução com ponto `indeterminado` no Semgrep sai **sem nota**, marcada):

```bash
node evaluation/tools/nota.mjs analysis/acceptance-V5-STRATEGY.csv analysis/semgrep-V5-STRATEGY.csv evaluation/strategy/mapa-anonimizacao.csv > analysis/notas-V5-STRATEGY.csv
```

7. **As tabelas e o veredito de cada hipótese**, pelas regras do §4.1 (o script está
   congelado desde antes das rodadas; nada se decide aqui):

```bash
node evaluation/tools/hipoteses.mjs experiment/desenho-v5.json --semgrep analysis/semgrep-V5-STRATEGY.csv --mapa evaluation/strategy/mapa-anonimizacao.csv --aceitacao analysis/acceptance-V5-STRATEGY.csv --metricas evaluation/metrics/V5-STRATEGY/metricas.csv --resultados analysis/resultados.csv --conferencia analysis/conferencia-V5-STRATEGY.md --json analysis/hipoteses-V5-STRATEGY.json > analysis/hipoteses-V5-STRATEGY.md
```

   Se ele listar "execuções com medida faltando", pare e descubra por quê antes de ler
   qualquer veredito.
8. **A releitura**, uma ou duas semanas depois, com uma semente escolhida no dia:
   `node evaluation/tools/sample.mjs releitura evaluation/reading --seed <N>` (11 dos 44);
   o Lucas lê sem abrir a planilha antiga, e o `compare.mjs` mede se ele responde igual.

## Se algo der errado

| o que | o que fazer |
|---|---|
| quarteto cortado pela cota ou pela API | refaz inteiro (passo 1); as 4 pastas vão para `runs-descartadas/` |
| `verify` acusa erro | parar; o erro diz a checagem e a execução; nada segue com dado incoerente |
| o Semgrep não leu código de um pacote (`sem-codigo`) | o `verify` recusa; achar o formato de pasta que escapou (`copia-limpa-teste.mjs` tem os conhecidos), corrigir como emenda datada, refazer o Semgrep **antes** do sorteio |
| o Semgrep avisa um trecho não lido | o `verify` lista o pacote; o Lucas confere à mão se ele estiver na amostra |
| o SonarQube falha no meio do `metrics.sh` | o script recusa refazer por cima de uma análise que já está no servidor (de propósito). Anote quais execuções falharam e decida caso a caso, registrando no `DECISOES.md` |
| uma decisão nova no meio | não muda nada do que está congelado; vira emenda datada no `DECISOES.md` |
