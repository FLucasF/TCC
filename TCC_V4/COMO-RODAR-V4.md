# Como rodar o V4, passo a passo

> **CONGELADO em 09/10/2026**, junto com o `OBJETIVO`, antes da primeira execução. Vai
> para o `TCC_V4` com o mesmo nome. Todos os comandos são do Git Bash,
> a partir da raiz do `TCC_V4`. O porquê de cada passo está no `OBJETIVO.md` e no
> `DECISOES.md`; aqui fica só **o que fazer, em que ordem, e o que conferir**.

## As sementes (registradas aqui, antes de rodar)

| sorteio | semente | onde entra |
|---|---|---|
| a ordem dos quartetos do confirmatório | 20261009 | já sorteada: `experiment/ordem-v4.csv` |
| a ordem dos quartetos do exploratório | 20261010 | já sorteada: `experiment/ordem-v4-exploratorio.csv` |
| os códigos cegos do confirmatório | **20261011** | passo 3 |
| os 20 pacotes que o Lucas lê | **20261012** | passo 3 |
| os códigos do exploratório | **20261013** | passo 5 |
| a releitura (4 ou 5 dos 20) | escolhida **no dia**, e registrada no commit | passo 4 |

## 0. Antes de cada sessão de rodadas

- [ ] O Docker aberto.
- [ ] O SonarQube **parado** (ele disputa memória com os 4 containers):
  `docker stop tcc-sonarqube`.
- [ ] Nenhuma variável `PROMPT_FILE`, `IMAGE`, `EFFORT` ou `DESENHO` no terminal
  (o `run-levels.sh` recusa as três primeiras no lote, mas é melhor nem tê-las).
- [ ] A janela do Max com **pelo menos 35% livre** antes de **cada** quarteto (o maior
  gasta ~18%).

## 1. O confirmatório: 25 quartetos

Um quarteto de cada vez, **na ordem** de `experiment/ordem-v4.csv`: a coluna `comando`
traz a linha pronta. Exemplo (o primeiro):

```bash
infra/scripts/run-levels.sh V4-STRATEGY-01 1 SONNET45
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

**Depois de cada rodada** (a cada 5 quartetos), registre no git o que já rodou:

```bash
git add runs/V4-STRATEGY-0N-* runs/logs/V4-STRATEGY-0N-* && git commit -m "dados(v4): rodada N do confirmatorio"
```

## 2. Depois dos 25: correção, custo, qualidade e coerência

```bash
node infra/scripts/aggregate.mjs --prefix V4- --out analysis/resultados.csv   # custo (os dois lotes juntos)
infra/scripts/acceptance.sh V4-STRATEGY                                       # correção: a suíte nos 100
docker start tcc-sonarqube                                                     # espere ~1 min ficar UP
evaluation/tools/metrics.sh V4-STRATEGY                                        # qualidade: CK e SonarQube
node infra/scripts/verify.mjs experiment/desenho-v4.json                       # tem de sair "tudo coerente"
```

Se o `verify` acusar algo, **pare** e resolva antes de seguir. Commit:
`runs/*/acceptance.txt` e `evaluation/metrics/V4-STRATEGY/metricas.csv`.

## 3. As cópias cegas, o Semgrep e o sorteio

```bash
node evaluation/tools/anonymize.mjs $(ls runs | grep '^V4-STRATEGY-') --seed 20261011 --padrao strategy --sem-comentarios
evaluation/tools/semgrep/detect.sh evaluation/strategy/packages analysis/semgrep-V4-STRATEGY.csv
node evaluation/tools/sample.mjs amostra evaluation/strategy/mapa-anonimizacao.csv evaluation/reading --seed 20261012
```

- O mapa (`evaluation/strategy/mapa-anonimizacao.csv`) fica fora do git e **o Lucas não
  abre**. Os scripts leem; ninguém olha.
- **Commit agora**, antes de qualquer leitura: `analysis/semgrep-V4-STRATEGY.csv`,
  `evaluation/reading/amostra.csv` e a planilha em branco.

## 4. A leitura do Lucas e a conferência

1. O Lucas lê os 20 códigos de `evaluation/reading/amostra.csv`, em
   `evaluation/strategy/packages/<CODIGO>/`, com a régua e o guia, e preenche
   `evaluation/reading/leitura-lucas.csv`. Sem olhar o CSV do Semgrep.
2. **Commit da planilha preenchida.**
3. A conferência e a coerência:

```bash
node evaluation/tools/compare.mjs evaluation/reading/leitura-lucas.csv analysis/semgrep-V4-STRATEGY.csv > analysis/conferencia-V4-STRATEGY.md
node infra/scripts/verify.mjs experiment/desenho-v4.json --mapa evaluation/strategy/mapa-anonimizacao.csv --amostra evaluation/reading --semgrep analysis/semgrep-V4-STRATEGY.csv
```

4. **Commit da conferência.** A regra de saída (18 de 20) vale como saiu: nenhuma regra
   do Semgrep é corrigida para refazer esta conta.
5. Só então o mapa entra no git: `git add -f evaluation/strategy/mapa-anonimizacao.csv`.
6. A nota e as tabelas:

```bash
node evaluation/tools/nota.mjs analysis/acceptance-V4-STRATEGY.csv analysis/semgrep-V4-STRATEGY.csv evaluation/strategy/mapa-anonimizacao.csv > analysis/notas-V4-STRATEGY.csv
```

7. **As tabelas e o veredito de cada hipótese**, pelas regras do §4.1 (o script está
   congelado desde antes das rodadas; nada se decide aqui):

```bash
node evaluation/tools/hipoteses.mjs experiment/desenho-v4.json --semgrep analysis/semgrep-V4-STRATEGY.csv --mapa evaluation/strategy/mapa-anonimizacao.csv --aceitacao analysis/acceptance-V4-STRATEGY.csv --metricas evaluation/metrics/V4-STRATEGY/metricas.csv --resultados analysis/resultados.csv --conferencia analysis/conferencia-V4-STRATEGY.md --json analysis/hipoteses-V4-STRATEGY.json > analysis/hipoteses-V4-STRATEGY.md
```

   Se ele listar "execuções com medida faltando", pare e descubra por quê antes de ler
   qualquer veredito.
8. **A releitura**, uma ou duas semanas depois, com uma semente escolhida no dia:
   `node evaluation/tools/sample.mjs releitura evaluation/reading --seed <N>`; o Lucas lê
   sem abrir a planilha antiga, e o `compare.mjs` mede se ele responde igual.

## 5. O exploratório: os outros 25 quartetos

Pode rodar enquanto o Lucas lê (passo 4). Mesmas regras do passo 1, na ordem de
`experiment/ordem-v4-exploratorio.csv` (o comando já traz o `DESENHO=`). Depois:

```bash
node infra/scripts/aggregate.mjs --prefix V4- --out analysis/resultados.csv
infra/scripts/acceptance.sh V4-EXPLOR
evaluation/tools/metrics.sh V4-EXPLOR
node infra/scripts/verify.mjs experiment/desenho-v4-exploratorio.json
node evaluation/tools/anonymize.mjs $(ls runs | grep '^V4-EXPLOR-') --seed 20261013 --padrao strategy-explor --sem-comentarios
evaluation/tools/semgrep/detect.sh evaluation/strategy-explor/packages analysis/semgrep-V4-EXPLOR.csv
node evaluation/tools/nota.mjs analysis/acceptance-V4-EXPLOR.csv analysis/semgrep-V4-EXPLOR.csv evaluation/strategy-explor/mapa-anonimizacao.csv > analysis/notas-V4-EXPLOR.csv
node evaluation/tools/hipoteses.mjs experiment/desenho-v4-exploratorio.json --exploratorio --semgrep analysis/semgrep-V4-EXPLOR.csv --mapa evaluation/strategy-explor/mapa-anonimizacao.csv --aceitacao analysis/acceptance-V4-EXPLOR.csv --metricas evaluation/metrics/V4-EXPLOR/metricas.csv --resultados analysis/resultados.csv > analysis/hipoteses-V4-EXPLOR.md
```

Sem leitura humana: o mapa do exploratório pode entrar no git logo
(`git add -f evaluation/strategy-explor/mapa-anonimizacao.csv`).

## Se algo der errado

| o que | o que fazer |
|---|---|
| quarteto cortado pela cota ou pela API | refaz inteiro (passo 1); as 4 pastas vão para `runs-descartadas/` |
| `verify` acusa erro | parar; o erro diz a checagem e a execução; nada segue com dado incoerente |
| o Semgrep avisa um trecho não lido | o `verify` lista o pacote; o Lucas confere à mão se ele estiver na amostra |
| o SonarQube falha no meio do `metrics.sh` | o script recusa refazer por cima de uma análise que já está no servidor (de propósito). Anote quais execuções falharam (o `status` de cada pasta) e decida caso a caso, registrando no `DECISOES.md` |
| uma decisão nova no meio | não muda nada do que está congelado; vira emenda datada no `DECISOES.md` |
