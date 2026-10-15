# Ferramentas de avaliação

| arquivo | o que faz |
|---|---|
| `anonymize.mjs` | gera os pacotes cegos para a leitura (ver o README da raiz); com `--sem-comentarios` (o V4), a cópia sai sem comentários e sem `.md`, com as linhas no mesmo lugar |
| `semgrep/` | o detector do padrão (P1 a P5), sem IA; ver `semgrep/README.md` |
| `sample.mjs` | sorteia a amostra do Lucas (1 pacote por modelo × nível) e, depois, a releitura; gera as planilhas em branco |
| `compare.mjs` | compara duas leituras pergunta por pergunta (o Lucas × o Semgrep, ou a leitura × a releitura) e aplica a regra de saída |
| `nota.mjs` | a nota de 0 a 100 de cada execução (pesos A, com B e C ao lado; a trava de quem não compila ou não sobe) |
| `metrics.sh` | métricas automáticas de cada execução de um lote: CK e SonarQube |
| `aggregate-metrics.mjs` | junta as métricas de um lote num CSV, uma linha por execução |
| `ck/` | o CK, compilado e travado (abaixo) |
| `sonar/` | o perfil de regras do SonarQube usado, congelado |

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
