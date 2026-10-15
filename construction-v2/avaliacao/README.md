# avaliacao/

> [!danger] Nada desta pasta chega ao agente
> O `.dockerignore` é lista branca: só `infra/docker/` entra no contexto de
> build da imagem. Conferido em 19/09/2026.

Tudo que mede o resultado de uma execução. O agente nunca vê nenhum destes
arquivos, nem os valores esperados, nem a rubrica, nem o gabarito.

## O que tem aqui

| | |
|---|---|
| `gabarito-avaliador.md` | onde estão P1, P2 e P3 no enunciado, e o que se espera de cada um |
| ~~`rubrica-strategy.md`~~ | **removida** em 21/09/2026. O desfecho primário passou a ser o teste de extensão. Recuperável: `git show aa71c81:avaliacao/rubrica-strategy.md` |
| `rotas-descobertas.md` | o que os quatro exemplos do enunciado não cobrem, e o que foi escrito para cobrir |
| `casos/` | 60 casos da suíte escondida, nos quatro grupos da §14.3. Ver `casos/README.md` |
| `testes-extensao/` | `DRONE`, `DEZOFF`, `CARTEIRA_DIGITAL`, e o procedimento de contagem do C5 |
| `ferramentas/` | os cinco scripts abaixo |

### As ferramentas

| script | o que faz |
|---|---|
| `conferir-exemplos.sh` | sobe a aplicação de uma execução e roda um conjunto de casos contra ela |
| `comparar.mjs` | roda dentro do container e compara **campo a campo**, não só o total |
| `autoteste.mjs` | testa o `comparar.mjs` contra apps de mentira com defeito conhecido |
| ~~`gerar-casos.mjs`~~ | calculou os 60 casos em BigInt. **Removido em 22/09/2026**: já tinha rodado, e o enunciado está congelado. `git show ee81dbf:avaliacao/ferramentas/gerar-casos.mjs` |
| `anonimizar.mjs` | prepara os pacotes para a avaliação às cegas |

> [!important] Rode os autotestes antes de acreditar em qualquer número
> ```bash
> node avaliacao/ferramentas/autoteste.mjs      # o comparador
> node infra/scripts/auditoria-web.teste.mjs    # o detector de acesso externo
> ```
> Eram quatro até 21/09/2026. Os outros dois — a proposta de validade e o
> gerador de casos — saíram em 22/09 junto com os scripts que testavam.
> Entre 19 e 21/09/2026 apareceram **nove** defeitos nas ferramentas de medição,
> três deles achados por acaso e o último na própria rodada de fumaça. Ferramenta de medida sem teste próprio
> reporta número errado em silêncio, e número errado vira resultado do TCC.

---

## O fluxo da §14, em ordem

### 1 · Anonimizar

```bash
node avaliacao/ferramentas/anonimizar.mjs <run_id> [run_id ...]
```

Produz `avaliacao/pacotes/<CODIGO>/` com o código-fonte e nada mais. Ficam de
fora `CLAUDE.md`, `.claude/`, `target/`, o `.git` que o agente porventura criou,
o `meta.json`, a transcrição e o log de build. As datas de modificação são
normalizadas, porque arquivo do braço `COM` nasce depois do harness ser copiado
e isso é rastro.

A ordem é embaralhada antes de o código ser atribuído, com semente registrada no
próprio mapa — então o código não denuncia a ordem de execução, e o
embaralhamento é reproduzível.

O script também **conta as pistas que o modelo deixou**: comentário citando
`CLAUDE.md`, "harness", "orientações de projeto" ou "skill". A §14.2 manda
**registrar, não remover** — é resultado do modelo. O número vai para as ameaças
à validade.

> [!warning] O mapa é o gabarito da cegueira
> `mapa-anonimizacao.csv` está no `.gitignore`. Quem avalia é quem rodou o
> experimento, então versioná-lo antes da planilha fechar seria deixar o
> gabarito aberto na mesa. Mova-o para fora desta pasta antes de avaliar, e
> traga de volta com `git add -f` só depois do commit de `notas-extensao.csv`.

### 2 · Avaliação automática

```bash
# a pasta inteira: uma subida da aplicacao, os seis grupos
CASOS=avaliacao/casos avaliacao/ferramentas/conferir-exemplos.sh <run_id> [run_id ...]
```

Reporta `% aprovados` por grupo e grava uma linha por run × grupo em
`analise/funcional.csv`, que é de onde a análise tira a tabela 15.1c. O build pós-execução (`mvn verify`) já rodou no
`executar.sh` e está em `runs/<id>/build.txt`.

### 3 · ~~Rubrica~~ — removida em 21/09/2026

O desfecho primário passou a ser o teste de extensão, do passo 4. Não há mais
nota subjetiva, nem segundo avaliador, nem kappa de Cohen.

Se a rubrica voltar: `git show aa71c81:avaliacao/rubrica-strategy.md`

### 4 · Teste de extensão

Procedimento em [`testes-extensao/README.md`](testes-extensao/README.md). A
contagem é mecânica, por `git diff --numstat`. Vai para `notas-extensao.csv`.

### 4a · Agregar e analisar

```bash
node infra/scripts/agregar.mjs --prefixo LOTE   # meta.json -> analise/resultados.csv
```

As tabelas da §15 são montadas **à mão** a partir desse CSV. Havia um
`analisar.mjs` que as gerava; saiu em 22/09/2026, por decisão de quem avalia.
Recuperável em `git show ee81dbf:infra/scripts/analisar.mjs`.

O `conferir-exemplos.sh` já vai alimentando `analise/funcional.csv` a cada
execução conferida, uma linha por run × grupo.

A análise **não reporta p-valor**, de propósito: com n=3 por braço o menor p
bicaudal alcançável num Mann-Whitney é 0,10, então p<0,05 é impossível por
construção e o número só induziria a ler um "não significativo" que vem do
desenho. No lugar dele, a medida de efeito é de pares: em quantos pares SEM/COM
o braço COM supera o SEM.

### 5 · A avaliação, sem segundo avaliador

Revisto em 21/09/2026, quando a rubrica saiu, e em 22/09, quando saíram os
scripts que davam palpite. O desenho anterior tinha **dois avaliadores
independentes** e **kappa de Cohen**, porque nota subjetiva precisa de prova de
confiabilidade. Com contagem de arquivos não há nota para concordar ou discordar.

1. Autor aplica as três extensões em cada pacote anonimizado, pelo procedimento
   da §14.5 → `notas-extensao.csv` → **commit**, que congela a planilha
2. Cada extensão só conta se **passar nos casos** de `testes-extensao/`
3. A contagem sai de `git status --porcelain` e `git diff --numstat`, não de
   leitura de código
4. O professor confere o **procedimento** por amostra: se a menor alteração foi
   mesmo a menor. Divergência aí é achado sobre o procedimento, não sobre nota

> [!note] Calibração antes de começar
> §14.6: aplicar as extensões em 1 ou 2 pacotes das execuções `MED-`, que estão
> fora da análise, para medir quanto tempo leva antes de dimensionar o `n`.
> Nunca do lote.

---

## As planilhas

Geradas pelo `anonimizar.mjs`, já com uma linha por pacote × ponto:

| arquivo | cabeçalho |
|---|---|
| `notas-extensao.csv` | `codigo_cego,ponto,extensao,passou_nos_casos,arquivos_criados,arquivos_alterados,linhas_alteradas,forma,observacoes` |

Até 21/09/2026 eram **quatro** planilhas: `notas-autor.csv`,
`notas-professor.csv` e `consenso.csv` tinham os seis critérios da rubrica, e
saíram junto com ela. A coluna `C5_confirmado` também era referência a critério
da rubrica; no lugar entrou `forma`, o desfecho secundário descritivo da §14.4,
anotado à mão ao abrir o pacote.

O script **não sobrescreve** planilha que já tenha nota preenchida.
