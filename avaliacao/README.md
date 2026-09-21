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
| `rubrica-strategy.md` | o instrumento do **desfecho primário**, com âncoras de código real |
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
| `gerar-casos.mjs` | calcula os valores esperados em BigInt, e se recusa a escrever se não reproduzir os exemplos do enunciado |
| `anonimizar.mjs` | prepara os pacotes para a avaliação às cegas |

> [!important] Rode os autotestes antes de acreditar em qualquer número
> ```bash
> node avaliacao/ferramentas/autoteste.mjs      # o comparador
> node infra/scripts/auditoria-web.teste.mjs    # o detector de acesso externo
> node avaliacao/ferramentas/gerar-casos.mjs    # o gerador, que se confere
> ```
> Em 19 e 20/09/2026 apareceram **oito** defeitos nas ferramentas de medição, e
> três deles foram achados por acaso. Ferramenta de medida sem teste próprio
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
> `mapa-anonimizacao.csv` está no `.gitignore`. O autor é um dos dois
> avaliadores, então versioná-lo antes de as notas fecharem seria deixar o
> gabarito aberto na mesa. Mova-o para fora desta pasta antes de avaliar, e
> traga de volta com `git add -f` só depois do commit de `notas-autor.csv`.

### 2 · Avaliação automática

```bash
# a pasta inteira: uma subida da aplicacao, os seis grupos
CASOS=avaliacao/casos avaliacao/ferramentas/conferir-exemplos.sh <run_id> [run_id ...]
```

Reporta `% aprovados` por grupo e grava uma linha por run × grupo em
`analise/funcional.csv`, que é de onde a análise tira a tabela 15.1c. O build pós-execução (`mvn verify`) já rodou no
`executar.sh` e está em `runs/<id>/build.txt`.

### 3 · Rubrica, às cegas

Uma linha por pacote × ponto em `notas-autor.csv`, seguindo
[`rubrica-strategy.md`](rubrica-strategy.md). **Antes** do teste de extensão:
fazer a extensão primeiro influencia a nota de C5.

### 4 · Teste de extensão

Procedimento em [`testes-extensao/README.md`](testes-extensao/README.md). A
contagem é mecânica, por `git diff --numstat`. Vai para `notas-extensao.csv`.

### 4a · Agregar e analisar

```bash
node infra/scripts/agregar.mjs --prefixo LOTE   # meta.json -> analise/resultados.csv
node infra/scripts/analisar.mjs --prefixo LOTE  # as tabelas da §15, em Markdown
```

O `conferir-exemplos.sh` já vai alimentando `analise/funcional.csv` a cada
execução conferida, uma linha por run × grupo.

A análise **não reporta p-valor**, de propósito: com n=3 por braço o menor p
bicaudal alcançável num Mann-Whitney é 0,10, então p<0,05 é impossível por
construção e o número só induziria a ler um "não significativo" que vem do
desenho. No lugar dele, a medida de efeito é de pares: em quantos pares SEM/COM
o braço COM supera o SEM.

### 5 · Dois avaliadores

1. Autor avalia tudo → `notas-autor.csv` → **commit**, que congela antes de ver
   as notas do professor
2. Professor avalia de forma independente → `notas-professor.csv`
3. Concordância: % exata por critério, e **kappa de Cohen** na classificação
4. Divergências discutidas → `consenso.csv`, com justificativa
5. A análise usa `consenso.csv`; a concordância é reportada no TCC

> [!note] Calibração antes de começar
> §14.6: pontuar juntos 1 ou 2 pacotes das execuções `MED-`, que estão fora da
> análise. Nunca do lote.

---

## As planilhas

Geradas pelo `anonimizar.mjs`, já com uma linha por pacote × ponto:

| arquivo | cabeçalho |
|---|---|
| `notas-autor.csv`, `notas-professor.csv` | `codigo_cego,ponto,C1..C6,total,classe,forma,outro_padrao,excesso_engenharia,condicional,implementacao_p2,observacoes` |
| `consenso.csv` | o mesmo, mais `justificativa` |
| `notas-extensao.csv` | `codigo_cego,ponto,extensao,passou_nos_casos,arquivos_criados,arquivos_alterados,linhas_alteradas,C5_confirmado,observacoes` |

O script **não sobrescreve** planilha que já tenha nota preenchida.
