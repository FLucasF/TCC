# A medida dos testes (V6)

A qualidade dos testes que o agente escreveu, por **mutação** (PIT): pequenos erros são plantados no
código dele, um de cada vez, e os testes dele rodam contra cada um. Um erro que os testes pegam é um
mutante "morto"; um que passa, "sobreviveu".

É uma das duas medidas por execução do V6, ao lado da suíte (a correção, em `../reference/`). O resto
da correção (o desenho, a arquitetura, o banco) é feito pela **IA junto com o Lucas**, lendo o código
(decisão de 15/10/2026, nas notas).

A medida nunca toca no workspace do agente: trabalha numa **cópia**, dentro da imagem travada
(`experimento-harness:v5`, a mesma do build), com os arquivos entrando e saindo por `tar` e as
dependências do Maven num volume do Docker (`v6-maven-cache`).

| arquivo | o que faz |
|---|---|
| `measure_test_quality.py` | roda o PIT nos testes do agente e escreve um CSV por lote |
| `maven_on_agent_copy.py` | acha o projeto (o `pom.xml` mais raso) e roda o Maven numa cópia dentro da imagem |

```bash
python evaluation/measures/measure_test_quality.py runs/pilots analysis/pilots/pit.csv
```

## As colunas

`tests` (os métodos `@Test`/`@ParameterizedTest` do agente), `mutants`, `killed`, `survived`,
`no_coverage` (os mutantes que nenhum teste sequer executa), `mutation_score` (mortos ÷ mutantes),
`test_strength` (mortos ÷ mutantes que os testes executam), `line_coverage` e a `situation` (`ok`,
`no-tests`, `tests-fail`, `no-mutants`, `not-compiled`, `failed`). Quem não escreveu teste sai
`no-tests`, com nota 0.

**A nota principal é a taxa de mutantes mortos** (`mutation_score`), e não a força dos testes. A força
só olha os mutantes que os testes executam: um agente com poucos testes executa poucos mutantes, e a
força fica alta mesmo sem conferir nada (o Haiku-01 tem 71% de força **sem nenhuma asserção**, com 29%
de cobertura).

## Como foi validada

O **poder de discriminação**, como nas tarefas do benchmark do Akita, que têm de separar a solução certa
da ingênua: `measure_test_quality.py --without-assertions` roda os **mesmos** testes com todas as
asserções removidas. Eles executam o mesmo código e não conferem nada, então a nota tem de despencar;
uma medida que não cai ali mede cobertura, e não testes. Nos 7 pilotos com testes que passam, a nota
caiu em todos (14/10/2026):

| piloto | com asserções | sem asserções |
|---|---|---|
| Haiku-01 | 0,112 | 0,095 |
| Sonnet-01 | 0,479 | 0,250 |
| Sonnet-02 | 0,812 | 0,496 |
| Sonnet-03 | 0,782 | 0,105 |
| Opus-01 | 0,863 | 0,398 |
| Opus-02 | 0,823 | 0,381 |
| Opus-03 | 0,886 | 0,539 |

O que sobra sem asserções são os mutantes que fazem o código lançar exceção e derrubar o teste. Sem as
asserções, um `assertThrows` removido deixa o teste falhar antes de qualquer mutante; nesse modo o PIT
pula esses testes (`skipFailingTests`, que só funciona na configuração do plugin: um `-D` é ignorado em
silêncio).

## Resultado nos 8 pilotos

Em `analysis/pilots/` (`pit.csv` e `pit-without-assertions.csv`): o Haiku mata 11% dos mutantes no
piloto 1 (os testes do piloto 2 falham); o Sonnet, de 48% a 81%; o Opus, de 82% a 89%.
