# As medidas por execução (V6)

Duas ferramentas da correção, que rodam numa cópia do projeto do agente:
- **a qualidade dos testes**, por **mutação** (PIT): pequenos erros são plantados no código dele, um de cada
  vez, e os testes dele rodam contra cada um. Um erro que os testes pegam é um mutante "morto"; um que passa,
  "sobreviveu";
- **o relatório de arquitetura** (ArchUnit): os pacotes, as dependências entre eles e os ciclos, para a
  leitura da IA. Não é nota: o que um ciclo significa é a IA quem decide.

Elas ficam ao lado da suíte (a correção, em `../reference/`). A correção julga quatro dimensões (a
arquitetura, os padrões, os testes e o banco) e é feita pela **IA junto com o Lucas**, lendo o código; estas
ferramentas são material dessa leitura (decisões de 15/10 e 17/10/2026, nas notas).

As duas nunca tocam no workspace do agente: trabalham numa **cópia**, dentro da imagem travada
(`experimento-harness:v5`, a mesma do build), com os arquivos entrando e saindo por `tar` e as
dependências do Maven num volume do Docker (`v6-maven-cache`).

| arquivo | o que faz |
|---|---|
| `measure_test_quality.py` | roda o PIT nos testes do agente e escreve um CSV por lote |
| `report_architecture.py` | compila a cópia e escreve, em cada execução, o relatório de arquitetura (`<run>/architecture/`) |
| `maven_on_agent_copy.py` | acha o projeto (o `pom.xml` mais raso) e roda o Maven numa cópia dentro da imagem |

```bash
python evaluation/measures/measure_test_quality.py runs/pilots analysis/pilots/pit.csv
```

```bash
python evaluation/measures/report_architecture.py runs/pilots
```

## As colunas do PIT

`tests` (os métodos `@Test`/`@ParameterizedTest` do agente), `mutants`, `killed`, `survived`,
`no_coverage` (os mutantes que nenhum teste sequer executa), `mutation_score` (mortos ÷ mutantes),
`test_strength` (mortos ÷ mutantes que os testes executam), `line_coverage` e a `situation` (`ok`,
`no-tests`, `tests-fail`, `no-mutants`, `not-compiled`, `failed`). Quem não escreveu teste sai
`no-tests`, com nota 0.

**A nota principal é a taxa de mutantes mortos** (`mutation_score`), e não a força dos testes. A força
só olha os mutantes que os testes executam: um agente com poucos testes executa poucos mutantes, e a
força fica alta mesmo sem conferir nada (o Haiku-01 tem 71% de força **sem nenhuma asserção**, com 29%
de cobertura).

## Como o PIT foi validado

O **poder de discriminação**, como nas tarefas do benchmark do Akita, que têm de separar a solução certa
da ingênua: `measure_test_quality.py --without-assertions` roda os **mesmos** testes com todas as
asserções removidas. Eles executam o mesmo código e não conferem nada, então a nota tem de despencar;
uma medida que não cai ali mede cobertura, e não testes. Nos 7 pilotos com testes que passam, a nota
caiu em todos (14/10/2026, refeito em 17/10):

| piloto | com asserções | sem asserções |
|---|---|---|
| Haiku-01 | 0,112 | 0,095 |
| Sonnet-01 | 0,479 | 0,250 |
| Sonnet-02 | 0,812 | 0,492 |
| Sonnet-03 | 0,782 | 0,105 |
| Opus-01 | 0,863 | 0,394 |
| Opus-02 | 0,823 | 0,372 |
| Opus-03 | 0,886 | 0,539 |

**Refeito em 17/10/2026**: a retirada das asserções deixava escapar uma quando o comentário logo antes dela tinha
um `;` (o ponto e vírgula do comentário parecia o fim do comando anterior). Agora os comentários saem antes da busca
(`without_comments`), e nos testes dos 8 pilotos não sobra nenhuma asserção. Três notas sem asserções caíram um
pouco (Sonnet-02 de 0,496 para 0,492, Opus-01 de 0,398 para 0,394, Opus-02 de 0,381 para 0,372); a conclusão não
muda.

O que sobra sem asserções são os mutantes que fazem o código lançar exceção e derrubar o teste. Sem as
asserções, um `assertThrows` removido deixa o teste falhar antes de qualquer mutante; nesse modo o PIT
pula esses testes (`skipFailingTests`, que só funciona na configuração do plugin: um `-D` é ignorado em
silêncio).

## O PIT nos 8 pilotos

Em `analysis/pilots/` (`pit.csv` e `pit-without-assertions.csv`): o Haiku mata 11% dos mutantes no
piloto 1 (os testes do piloto 2 falham); o Sonnet, de 48% a 81%; o Opus, de 82% a 89%.

## O relatório de arquitetura

Em cada execução, `architecture/architecture.json` (tudo) e `architecture-report.txt` (o mesmo, para ler).
Uma execução que já tem a pasta é pulada: um relatório nunca é escrito por cima de outro. As regras estão em
`../reference/src/main/java/reference/architecture/ArchitectureReport.java`:
- **os pacotes**: os que ficam logo abaixo do pacote raiz (`com.loja.orders.(*)..`), com as classes de cada
  um. As classes do próprio pacote raiz (a `Application`) ficam fora de todos;
- **as dependências entre pacotes**, cada uma com as dependências de classe que a fazem
  (`catalog.InventoryService -> order.OrderItem`);
- **os ciclos**: cada grupo de pacotes que dependem uns dos outros em círculo (um componente fortemente
  conexo), **uma vez só**, com as dependências de dentro do grupo. O ArchUnit, pelo `beFreeOfCycles`, conta
  cada caminho como um ciclo, e um único par `a ↔ b` vira muitos quando passa por outros pacotes;
- **fora da camada web**, as classes que dependem de um controller e as que usam a API da web **de entrada**
  (as anotações e os tipos do Spring MVC, o `jakarta.servlet`, o `ResponseEntity`). A web **de saída** (o
  `RestClient`, para chamar a TransLog) e o `HttpStatus` não contam: um adaptador usa os dois
  legitimamente.

**Como foi validado (17/10/2026).** Nos 8 pilotos, os pacotes em ciclo são os mesmos da medida antiga, que
contava pelo ArchUnit (`architecture.py`, no histórico do git até o commit `3426ad23`): Opus-01 4, Opus-02 3,
Opus-03 6, Sonnet-02 2, os outros 0. Uma classe plantada numa cópia do Opus-03, que guarda um controller e
devolve um `ResponseEntity`, aparece nas duas regras de camada; nos pilotos, as duas dão 0, e os zeros são de
verdade.
