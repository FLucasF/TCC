# O ensaio geral da análise (10/10/2026)

> **Não é resultado do TCC.** O V4 foi descartado antes da análise e refeito como V5,
> com os 11 modelos num lote só (ver `TCC_V3/DECISOES.md`). Esta pasta guarda o
> **ensaio** que rodou a análise inteira do V4 de ponta a ponta para conferir se o fluxo
> funciona, se valida o que deve validar e se a correção não alucina. Os números daqui
> **não entram** em nenhuma conclusão. **Lucas: não abra os resultados antes da sua
> leitura do V5** (os modelos e o enunciado são os mesmos, e ver como eles se saíram
> no V4 poderia guiar a sua leitura).

## O que rodou

Os passos 2 a 4 do `COMO-RODAR-V4.md`, nos 100 pacotes do confirmatório (`V4-STRATEGY`),
com os scripts congelados do V4 e as sementes registradas (anonimização 20261011,
amostra 20261012):

| passo | resultado |
|---|---|
| suíte de aceitação | as 100 execuções medidas (`runs/<id>/acceptance.txt`) |
| custo e coerência (`aggregate.mjs`, `verify.mjs`) | "tudo coerente"; os 113 arquivos congelados conferem |
| cópias cegas, Semgrep, sorteio | 100 pacotes, `analysis/semgrep-V4-STRATEGY.csv`, 20 sorteados |
| **a leitura dos 20, simulada** | feita pelo **Claude**, às cegas, pela régua e pelo guia, sem ver o Semgrep antes, no lugar da leitura do Lucas (`evaluation/reading-ensaio/leitura-simulada-claude.csv`); as 95 citações `arquivo:linha` conferidas por script contra o código (e a conferência pegou 3 erros plantados) |
| conferência (`compare.mjs`) | 19 de 20 nas 4 perguntas (`evaluation/reading-ensaio/conferencia-simulada.md`) |
| métricas (CK e SonarQube) | 100 de 100, sem célula vazia |
| nota e hipóteses | rodaram sem falta de dado (`notas-V4-STRATEGY.csv`, `hipoteses-V4-STRATEGY.*`) |

A leitura simulada **não é** a do Lucas e não vale como conferência humana: o OBJETIVO
diz que nenhum modelo de IA avalia nada, porque os agentes do experimento são Claude.
Ela serviu só para exercitar o fluxo com uma planilha preenchida do jeito que a do
Lucas seria.

## O que o ensaio achou

**Um defeito no Semgrep.** A única discordância (o pacote `C4RK`) era do Semgrep: quando
o agente cria o projeto numa subpasta (`checkout/src/main`), o `copia-limpa.mjs` só
aceitava `<pacote>/src/main` e não lia nenhum arquivo, e o Semgrep respondia
"indeterminado" em tudo. Foram 4 dos 100 pacotes, todos do mesmo modelo e em níveis com
harness: um erro **não aleatório**. O build, a suíte e as métricas achavam o projeto
(pelo `pom.xml` mais raso); só o Semgrep não. As hipóteses tratavam o "indeterminado"
como sem dado, mas a nota o contava como zero. O defeito mudava **3 dos 32 vereditos**.

Corrigido no `TCC_V3` para o V5 (o V4 congelado não muda):

- `semgrep-V4-STRATEGY-corrigido.csv`: a mesma análise com o Semgrep corrigido. Só as 4
  linhas dos pacotes com subpasta mudaram; as outras 96, idênticas byte a byte.
- Com ele, a conferência simulada sobe para 20 de 20, e os 4 pacotes batem com a leitura
  nas 16 respostas. `hipoteses-V4-STRATEGY-corrigido.*` é a análise refeita.
- Os 4 corpora do Semgrep continuam 80/80, 44/44, 10/10 e 12/12.

**Outros ajustes para o V5:** o `verify.mjs` passa a recusar um pacote em que o Semgrep
não leu código; a nota deixa a execução **sem nota** (e não zero) quando o Semgrep responde
"indeterminado"; o `detect.sh` funciona com `MSYS_NO_PATHCONV=1` no terminal; a régua e o
guia mandam achar o projeto pelo `pom.xml` antes de buscar.

## Os arquivos

| arquivo | o que é |
|---|---|
| `../semgrep-V4-STRATEGY.csv` | o Semgrep congelado do V4, com o defeito |
| `notas-V4-STRATEGY.csv`, `hipoteses-V4-STRATEGY.*` | a nota e as hipóteses com o Semgrep do V4 |
| `semgrep-V4-STRATEGY-corrigido.csv`, `hipoteses-V4-STRATEGY-corrigido.*` | o mesmo com o Semgrep corrigido |
| `../../evaluation/reading-ensaio/` | a amostra, a leitura simulada e a conferência |
| `../../evaluation/strategy/mapa-anonimizacao.csv` | o mapa código → execução do ensaio (sem ele, nada aqui se refaz) |
| `../../evaluation/metrics/V4-STRATEGY/metricas.csv` | CK e SonarQube das 100 execuções |

As 12 execuções `V4-EXPLOR-01-*` (3 quartetos da rodada 1 do exploratório) também
entram no git como registro: o exploratório parou quando o V4 foi substituído pelo V5.
