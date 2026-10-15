# As medidas de teste, arquitetura e banco (V6)

Três medidas novas do V6, ao lado da suíte (correção) e da régua do Semgrep (padrões de projeto).
Nenhuma toca no workspace do agente: cada uma trabalha numa **cópia**, dentro da imagem travada
(`experimento-harness:v5`, a mesma do build), com os arquivos entrando e saindo por `tar` e as
dependências do Maven num volume do Docker (`v6-maven-cache`).

| arquivo | dimensão | o que mede |
|---|---|---|
| `pit.py` | **testes** | a qualidade dos testes que o agente escreveu, por mutação (PIT): pequenos erros plantados no código dele, um de cada vez; os testes que os pegam "matam" o mutante |
| `architecture.py` | **arquitetura** | três regras que não dependem de gosto, com o ArchUnit, nas classes compiladas do agente (as regras estão em `../reference/src/main/java/reference/architecture/ArchitectureRules.java`) |
| `database.py` | **banco** | o que o próprio banco garante, lido do esquema que o serviço criou (o `run_suite.py` fotografa o esquema depois da suíte, em `database-schema.json`) |
| `agent_project.py` | — | o comum: achar o projeto (o `pom.xml` mais raso) e rodar o Maven numa cópia dentro da imagem |

## As colunas

**`pit.py`.** `tests` (os métodos `@Test`/`@ParameterizedTest` do agente), `mutants`, `killed`,
`survived`, `no_coverage` (os mutantes que nenhum teste sequer executa), `mutation_score` (mortos ÷
mutantes), `test_strength` (mortos ÷ mutantes que os testes executam: a qualidade dos testes onde eles
olham), `line_coverage` e a `situation` (`ok`, `no-tests`, `tests-fail`, `no-mutants`, `not-compiled`,
`failed`). Quem não escreveu teste sai `no-tests`, com nota 0.

**`architecture.py`.**
- `packages_in_cycles`: quantos pacotes da aplicação estão presos num ciclo de dependências. É o número
  principal. O ArchUnit conta cada caminho como um ciclo (`package_cycles`), e um único par `a ↔ b` vira
  muitos ciclos quando passa por outros pacotes.
- `controller_dependents`: classes de fora da camada web que dependem de um controller.
- `incoming_web_users`: classes de fora da camada web que usam a API da web **de entrada** (as anotações e
  os tipos do Spring MVC, o `jakarta.servlet`, o `ResponseEntity`). A web **de saída** (o `RestClient`, para
  chamar a TransLog) e o `HttpStatus` não contam: um adaptador usa os dois legitimamente.

**`database.py`.** `stock_guard` (um `CHECK` que impede estoque negativo), `repeated_notice_guard` (uma
chave primária, restrição ou índice único com o id da transação, que impede o mesmo aviso duas vezes),
`foreign_keys`, `tables_without_primary_key`, `check_constraints` e `tables`. As duas travas são a resposta
do **banco** às regras "o estoque nunca fica negativo" e "o mesmo aviso pode chegar mais de uma vez": a
suíte confere o comportamento; isto confere se o banco também o garante.

## A nota principal do PIT

A **taxa de mutantes mortos** (`mutation_score`), e não a força dos testes. A força só olha os mutantes
que os testes executam: um agente com poucos testes executa poucos mutantes, e a força fica alta mesmo
sem conferir nada (o Haiku-01 tem 71% de força **sem nenhuma asserção**, com 29% de cobertura).

## Como cada medida foi validada

- **PIT, o poder de discriminação** (como as tarefas do benchmark do Akita, que têm de separar a solução
  certa da ingênua): `pit.py --without-assertions` roda os **mesmos** testes com todas as asserções
  removidas. Eles executam o mesmo código e não conferem nada, então a nota tem de despencar. Uma medida
  que não cai ali mede cobertura, e não testes. Nos 7 pilotos com testes que passam, a nota caiu em
  todos (14/10/2026):

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
- **ArchUnit:** uma classe plantada numa cópia do Opus-03, que guarda um controller e devolve um
  `ResponseEntity`, faz `controller_dependents` e `incoming_web_users` irem de 0 para 1. Os zeros dos
  pilotos são zeros de verdade.
- **Banco:** os "não" foram conferidos no esquema. Os pilotos sem trava do aviso repetido não têm tabela de
  avisos de pagamento (a idempotência fica só no código). Um serviço que não sobe sai
  `service-did-not-start`, e não "sem tabelas".

## Resultado nos 8 pilotos

Em `runs/pilots/measures/` (`pit.csv`, `pit-without-assertions.csv`, `architecture.csv`, `database.csv`).

| | testes (mortos) | pacotes em ciclo | controller / web de entrada | estoque `>= 0` no banco | aviso repetido no banco | chaves estrangeiras |
|---|---|---|---|---|---|---|
| Haiku (2) | 11% no piloto 1; os testes do piloto 2 falham | 0 | 0 / 0 | o serviço não sobe | — | — |
| Sonnet (3) | 48% a 81% | 0 a 2 | 0 / 0 | sim | não | 0, 0 e 3 |
| Opus (3) | 82% a 89% | 3 a 6 | 0 / 0 | sim | só no piloto 3 | 5 a 6 |

O Opus, melhor em quase tudo, é o que tem pacotes presos em ciclos: ele divide o código por funcionalidade
(`catalog`, `order`, `checkout`, `coupon`) e liga os pacotes por listeners de evento nos dois sentidos.

