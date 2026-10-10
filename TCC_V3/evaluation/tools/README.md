# Ferramentas de avaliação

Os instrumentos que **avaliam** o código dos agentes, depois que as execuções rodaram.
Os que **rodam** e **conferem** a bancada estão em `infra/scripts/` (README lá). Cada
script tem, no topo, um bloco **COMO LER** com as partes dele na ordem em que rodam.
Nenhum usa IA: os agentes do experimento são Claude, e um avaliador da mesma família é
o risco que o OBJETIVO aponta.

## A ordem, depois de um lote rodado

```
anonymize.mjs ──> pacotes cegos ──> semgrep/detect.sh ──> analysis/semgrep-<lote>.csv ─┐
              └─> mapa (código → execução, fora do git até a leitura ser commitada)    │
sample.mjs    ──> amostra + planilha em branco ──> leitura do Lucas ──> compare.mjs ──>│ conferência
metrics.sh    ──> CK e SonarQube por execução ──> aggregate-metrics.mjs ──> metricas.csv│
nota.mjs      ──> a nota de 0 a 100 (resumo)                                           │
hipoteses.mjs ──> as tabelas e o veredito de cada hipótese  <──────────────────────────┘
```

## Os scripts

| arquivo | o que faz | como faz |
|---|---|---|
| `anonymize.mjs` | gera os **pacotes cegos** que o Lucas lê e o Semgrep mede | embaralha as execuções com uma semente e dá a cada uma um código de 4 letras; copia o código para `evaluation/<padrão>/packages/<código>/` sem o que entregaria o nível (`CLAUDE.md`, `.claude/`, `meta.json`, transcrição, `target/`, o `.git` do agente) e, com `--sem-comentarios`, sem comentários e sem `.md`, com as linhas no mesmo lugar; iguala as datas dos arquivos; conta as pistas da condição no código (que não se removem: são resultado do modelo); grava o mapa código → execução. Recusa sobrescrever um mapa |
| `sem-comentarios.mjs` | tira os comentários de um arquivo **sem mudar o número de linhas** | apaga os comentários (`//` e `/* */` no `.java`, sem tocar o que está entre aspas nem nos blocos `"""`; `<!-- -->` no `.xml`; as linhas que começam com `#` ou `!` no `.properties` e no `.yml`) e deixa cada quebra de linha onde estava. É o mesmo removedor da cópia cega e da cópia do Semgrep, para o `arquivo:linha` das duas leituras bater |
| `semgrep/` | o **detector do padrão** (P1 a P5) | regras fixas do Semgrep, sem IA, sobre a cópia limpa do projeto que o build compila; ver `semgrep/README.md` |
| `sample.mjs` | sorteia a **amostra** do Lucas e, semanas depois, a **releitura** | modo `amostra`: lê o mapa, agrupa os códigos por modelo × nível e sorteia 1 por grupo com a semente; escreve só os códigos, em ordem alfabética (nada diz o grupo), e a planilha em branco. Modo `releitura`: sorteia, entre os da amostra, os que o Lucas relê, num comando separado de propósito (quem sabe desde o começo quais vai reler pode guardar as respostas) |
| `compare.mjs` | compara **duas leituras** pergunta por pergunta e aplica a regra de saída | casa as linhas pelo código do pacote; em cada uma das 4 perguntas (P4 `localizacao` e `selecao`, P5 `forma` e `proporcao`) conta as respostas iguais; célula vazia ou `indeterminado` conta como discordância; a pergunta **vale** para o lote com 90% de concordância, senão vira descritiva. Lista cada discordância com a evidência dos dois lados, para abrir o código e ver quem tem razão |
| `nota.mjs` | a **nota de 0 a 100** de cada execução, como resumo | junta a suíte (contas de 12, recusas de 9) e o Semgrep (P1 a P5): pesos A = contas 30, recusas 20, desenho 50 (10 por ponto com as duas respostas certas, 5 com uma, 0 com nenhuma; P5 sem exagero, 10), com as variantes B e C ao lado. Não compilou ou não subiu: nota 0. Um ponto `indeterminado` no Semgrep: a execução fica **sem nota**, marcada (decisão de 10/10) |
| `hipoteses.mjs` | as **tabelas e o veredito** de cada hipótese do OBJETIVO (§4) | junta por execução tudo o que foi medido (Semgrep pelo mapa, suíte, métricas, custo, uso da skill e do revisor); forma os pares (mesmo modelo, mesma réplica, níveis vizinhos); aplica a regra de cada tipo de hipótese (direcional, não-inferioridade, sem direção) com os limites calculados para o número de pares; um par com `indeterminado` sai como "sem dado" e é contado. Com `--conferencia`, as hipóteses de uma pergunta reprovada viram descritivas |
| `metrics.sh` | **métricas automáticas** de cada execução: CK e SonarQube | confere as versões travadas (o hash do `.jar` do CK, as imagens do SonarQube e do scanner); para cada execução, acha o projeto pelo `pom.xml` mais raso, roda o CK no código e o scanner do SonarQube nas classes compiladas, e espera o servidor devolver as medidas. Recusa refazer uma análise que já está no servidor |
| `aggregate-metrics.mjs` | junta as métricas de um lote num CSV | as colunas `sonar_*` vêm do que o servidor devolveu; as `ck_*`, do CSV por classe do CK, resumidas em contagens, médias e máximos; o modelo, o braço e o nível vêm do `meta.json` e do `run_id` |
| `ck/` | o CK, compilado e travado | ver "Versões travadas", abaixo |
| `sonar/` | o perfil de regras do SonarQube usado, congelado | ver "Versões travadas", abaixo |

## Os testes

Cada instrumento que decide alguma coisa tem um teste que roda sozinho, sem Docker e
sem dado real, e sai com 1 se algum caso falhar:

```bash
node evaluation/tools/nota-teste.mjs                    # 12 de 12: os pesos, o meio ponto, a trava, o indeterminado
node evaluation/tools/hipoteses-teste.mjs               # 17 de 17: um lote de mentira com o veredito planejado
node evaluation/tools/semgrep/copia-limpa-teste.mjs     # 9 de 9: os formatos de pasta que um agente pode deixar
evaluation/tools/semgrep/corpus/validar.sh              # os 4 corpora do Semgrep (precisa do Docker)
```

| teste | o que prova | como prova |
|---|---|---|
| `nota-teste.mjs` | que a nota faz a conta dos pesos e trata a trava e o `indeterminado` como a régua manda | monta CSVs da suíte e do Semgrep com 12 execuções de conta feita à mão (tudo certo = 100; P4 errado = 90; meio ponto = 95; exagero no P5 = 90; metade das contas = 85; `indeterminado` = sem nota; não compilou = 0) e compara com o que o `nota.mjs` devolve |
| `hipoteses-teste.mjs` | que o `hipoteses.mjs` aplica as regras do §4.1 como estão escritas | monta um lote de mentira com os resultados planejados para cada hipótese dar um veredito conhecido, e muda uma coisa por caso: cada caso tem de dar o veredito que a regra manda |

## Métricas automáticas

Medem o **código de produção** (`src/main/java`) de cada execução, com duas
ferramentas que não se sobrepõem:

| ferramenta | mede |
|---|---|
| **SonarQube** | linhas de código, classes, métodos, complexidade ciclomática e **cognitiva**, **duplicação**, code smells e dívida técnica |
| **CK** (Aniche) | por classe: acoplamento (**CBO**), coesão (**LCOM**), complexidade (**WMC**), herança (DIT), RFC |

São **secundárias**: complementam a régua de leitura, não a substituem. Quatro
delas conferem hipóteses que a régua mede (§4.9 do `OBJETIVO.md`): complexidade
cognitiva, duplicação, número de classes, e acoplamento e coesão médios. Nenhuma
muda o veredito de uma hipótese. Não
julgam nada; só extraem números. Por isso rodam direto em `runs/`, sem pacote
anonimizado: uma ferramenta não sabe qual é o braço, e a cegueira existe para
proteger o julgamento humano.

### Versões travadas

Mudar qualquer uma é um instrumento novo. O `metrics.sh` confere o `.jar` e a
imagem antes de rodar, e se recusa se não baterem.

| peça | versão | identidade |
|---|---|---|
| CK | compilado do commit `8a1ef916d597d33ad619d748b616bb7dbc3f0c36` de [mauricioaniche/ck](https://github.com/mauricioaniche/ck) (29/04/2026, com suporte a Java 21) | `ck/ck-0.7.1-8a1ef916-jar-with-dependencies.jar`, sha256 `f3c0e5bc159189ebd213badf693ab792a31cbea749eb9f97b9185938c54a7baf` |
| SonarQube | Community `26.9.0.129388-community` | imagem `sha256:c0f1160bccfa435db4168c2d7df69af3c3ea7bfeb87395613014048fb33a68c1` |
| scanner | `sonarsource/sonar-scanner-cli:12.2.0.4256_8.1.0` | `sha256:a3f4215076706c95a17a68c19322ee916e40a3acd081a8c1a1e839e0194afa57` |
| regras | perfil "Sonar way" para Java, o padrão daquela versão, 599 regras | `sonar/perfil-java-sonar-way.xml`, sha256 `4556f31722521b03f9d9ad5358de2a8888ef30db1637a18540e1aabbd4ef9555` |

**Por que o CK é compilado, e não baixado.** A última versão publicada no Maven
Central (0.7.0, de 2022) **ignora todo `record`**: num pacote de teste, viu 31
de 40 tipos, e os 9 que faltaram eram todos `record`. O suporte a Java 21 entrou
no código em abril de 2026, sem versão publicada. O `.jar` foi compilado daquele
commit (`mvn -DskipTests package`, JDK 21.0.11, Maven 3.9.16) e entra no
repositório, porque compilar de novo pode dar bytes diferentes. Compilado, viu
40 de 40.

### Subir o SonarQube (uma vez)

```bash
docker run -d --name tcc-sonarqube -p 127.0.0.1:9000:9000 \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -v tcc-sonar-data:/opt/sonarqube/data -v tcc-sonar-extensions:/opt/sonarqube/extensions \
  -v tcc-sonar-logs:/opt/sonarqube/logs \
  sonarqube@sha256:c0f1160bccfa435db4168c2d7df69af3c3ea7bfeb87395613014048fb33a68c1
```

Só a máquina local enxerga a porta 9000. O `SONAR_ES_BOOTSTRAP_CHECKS_DISABLE`
dispensa o ajuste de `vm.max_map_count` no sistema, aceitável para uso local.
Os volumes guardam a configuração quando o Docker reinicia; depois, basta
`docker start tcc-sonarqube`.

Na primeira vez, **à mão** (o Claude não cria credenciais): entrar em
`http://localhost:9000` com `admin`/`admin`, trocar a senha, gerar um token do
tipo *Global Analysis Token* (*My Account → Security*) e salvá-lo em `.env.sonar`,
na raiz, como `SONAR_TOKEN=...`. O `.env.sonar` está no `.gitignore`, e os scripts
nunca imprimem o token.

### Rodar

```bash
evaluation/tools/metrics.sh TESTE-STRATEGY-01
```

Para cada execução do lote: acha o projeto pelo `pom.xml` mais raso (como o
build da bancada), roda o CK em `src/main/java` e o scanner com as classes de
`target/classes`, espera o SonarQube processar e grava tudo em
`evaluation/metrics/<prefixo>/<run_id>/`. No fim, o `aggregate-metrics.mjs` gera
`evaluation/metrics/<prefixo>/metricas.csv`. A pasta `evaluation/metrics/` é
derivada, e fica fora do git.

**Nunca refaz uma análise.** Recusa se a pasta de saída já existe, ou se alguma
execução já foi analisada no SonarQube (cada execução é um projeto, com o
`run_id` como chave). Conferido em 06/10.

### O CSV

Uma linha por execução: `run_id`, `model`, `condition`, `replicate`, `status`
(`ok`, ou o motivo da falha), as medidas do SonarQube (`sonar_*`) e os agregados
do CK (`ck_*`).

- **`ck_tipos` conta só tipos com nome** (classe, interface, enum, record), o
  mesmo critério de `sonar_classes`. Os dois concordaram nas 6 execuções de
  `TESTE-STRATEGY-01`: duas ferramentas independentes, o mesmo número.
- **`ck_anonymous` conta à parte o corpo de cada constante de enum** com
  comportamento próprio, que o CK vê como classe anônima. Mede um desenho (o
  enum com corpo por constante); misturado às médias, faria esse desenho parecer
  ter mais classes que o equivalente com classes.
- **As médias e máximos do CK usam só os tipos com nome; a soma de WMC usa
  tudo**, porque a lógica dentro das constantes também é código.

### Validação

Em 06/10/2026, sobre `TESTE-STRATEGY-01` (fora da análise): as 6 execuções com
`status = ok`, `ck_tipos` igual a `sonar_classes` em todas, e as três travas
conferidas (pasta já existente, execução já analisada, prefixo inválido).
