# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin com sete pacotes: `hotel.web` (rotas e JSON), `hotel.reservations` (regras, preço e
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em
`hotel.Hotel`. Os dados ficam no PostgreSQL, com o esquema criado por uma migração Flyway. Uma requisição de relatório
entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:91-106`), que lê o período e chama `generate` do relatório. O
relatório pede as reservas ao `ReservationStore` e devolve o CSV. O agente criou a classe abstrata `CsvReport` com o
método-modelo `final generate` (`src/main/java/hotel/reports/CsvReport.java:25-35`). Os quatro relatórios passaram a
estendê-la: os dois da base (ocupação e financeiro) e os dois novos (taxa de turismo e planos). A escolha e a ordem
das reservas do mês foram para `ReservationStore.startingIn` (`src/main/java/hotel/reservations/ReservationStore.java:136-145`),
e a taxa ficou configurável pela variável `TOURISM_TAX_PER_GUEST_NIGHT` (`src/main/java/hotel/Hotel.java:56-57`). O
roteiro de comportamento passou inteiro (64 de 64) e os 29 testes passam. Não há defeito sério. O ponto mais fraco é um
teste que falta no relatório por plano: nenhum teste tem uma reserva NON_REFUNDABLE nem a regra da não reembolsável
cancelada.

## Arquitetura

**Números:** 7 pacotes e 17 dependências, os mesmos da base (`measures/dependencies.md` e `measures/dependencies-base.md`).
Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já vinha da base. `hotel.reports` passou de 2 para 6 classes
(de 95 para 240 linhas) e continua sem Javalin e sem JDBC.
**Nota:** bom

O agente não mexeu na forma do sistema. As rotas novas são finas, como as da base: leem o mês, chamam `generate` e
escrevem o CSV (`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios novos não conhecem o Javalin nem o SQL
(`src/main/java/hotel/reports/TourismTaxReport.java:80-92`, `src/main/java/hotel/reports/RatePlanReport.java:32-51`). A
montagem continua num lugar só, `src/main/java/hotel/Hotel.java:31-40`, agora com a taxa injetada pelo construtor. O
ciclo `documents`/`reservations` já existia na base e o agente não o piorou. Uma ressalva pequena: a escolha das
reservas do mês, que inclui a regra "deixou dinheiro com a pousada", saiu do relatório e foi para o
`ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:127-134`). Essa classe é de acesso a dados,
mas filtra em Java sobre `all()`. Mesmo assim, fica no pacote de reservas e evita repetir a regra no financeiro e no
relatório por plano. Isso não muda a nota.

## Design de baixo nível

**Números:** os 4 relatórios estão na estrutura (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e
`RatePlanReport` estendem `CsvReport`). Nenhum fica fora dela. O título é escrito em um lugar só
(`src/main/java/hotel/reports/CsvReport.java:28`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Template Method, e o agente o aplicou como a referência descreve. `CsvReport.generate` é `final`
e é o esqueleto: título, cabeçalho, linhas e total (`src/main/java/hotel/reports/CsvReport.java:25-35`). Os passos que
mudam, `name()`, `header()` e `body(P)`, são abstratos (`CsvReport.java:19-23`). Os dois relatórios da base foram
refeitos como subclasses e perderam o `StringBuilder` próprio (`src/main/java/hotel/reports/OccupancyReport.java:13-51`,
`src/main/java/hotel/reports/RevenueReport.java:14-42`). Os dois novos seguem o mesmo molde. Quando vier a linha final
com a data, a mudança é uma linha em `CsvReport.generate`, e um relatório novo (cancelamentos, hóspedes por estado) é
só mais uma subclasse. Para não repetir o filtro, a escolha e a ordem das reservas foram para
`ReservationStore.startingIn` com um `Predicate` (`src/main/java/hotel/reservations/ReservationStore.java:136-145`).

Não há exagero. O tipo genérico `P` existe porque a ocupação recebe `LocalDate` e os outros recebem `YearMonth`, e o
record `Body` é o mínimo para devolver linhas e total. O segundo construtor de `Hotel` e a variável de ambiente para a
taxa (`src/main/java/hotel/Hotel.java:27-31,56-57`) respondem a "a prefeitura muda o valor da taxa todo ano".

O resto do código está limpo, com `BigDecimal` e arredondamento em centavos para a taxa
(`src/main/java/hotel/reports/TourismTaxReport.java:85-86`). Há só detalhes pequenos:

- O texto `"total,"` é montado à mão nas quatro subclasses (`OccupancyReport.java:50`, `RevenueReport.java:41`,
  `TourismTaxReport.java:44`, `RatePlanReport.java:50`), quando o rótulo poderia estar no esqueleto.
- O zero com duas casas é feito de dois jeitos: `BigDecimal.ZERO.setScale(2)` em `RevenueReport.java:35` e
  `PriceCalculator.cents(...)` nos novos. Para isso, `PriceCalculator.cents` virou público
  (`src/main/java/hotel/reservations/PriceCalculator.java:50`).
- As quatro rotas de relatório repetem o mesmo trecho (`HttpApi.java:91-106`), um padrão que já vinha da base.
- A taxa lida do ambiente não é validada: um valor negativo seria aceito (`Hotel.java:56-57`).

Nada disso pediria mudança antes de aceitar.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 pulados; há 29 métodos escritos. A base tinha 26, todos mantidos. Cobertura
de linhas: 94,2% (533 de 566), contra 93,8% na base. Cobertura de ramos: 88,5% (115 de 130), contra 87,1% na base.
`hotel.reports` tem 88 de 88 linhas e 28 de 28 ramos cobertos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente atualizou os cinco testes de relatório da base para conferir o CSV inteiro com a linha de título
(`src/test/java/hotel/reports/ReportsTest.java:57-119`) e não enfraqueceu nenhum. Também acrescentou três testes com
valores:

- a taxa de 2 hóspedes por 3 noites dá 27.00, o exemplo do pedido (`ReportsTest.java:123-137`);
- a taxa deixa de fora a reserva não paga e a cancelada, cobra a CHECKED_IN e segue a ordem do financeiro
  (`ReportsTest.java:140-150`);
- o relatório por plano mostra a linha NON_REFUNDABLE com 0 (`ReportsTest.java:153-163`).

Pela API, o teste confere só o fim dos dois relatórios novos (`src/test/java/hotel/web/HttpApiTest.java:119-120`).

Uma regra nova ficou sem rede. O teste do relatório por plano usa o mês de abril, que não tem nenhuma reserva
NON_REFUNDABLE (`ReportsTest.java:153-163`), e a reserva não reembolsável cancelada só é criada no teste do financeiro
(`ReportsTest.java:96`). Com isso, nenhum teste verifica:

- uma linha NON_REFUNDABLE com valor;
- a regra "contando as mesmas reservas que o financeiro conta".

Pergunta da mutação: se `RatePlanReport.java:33` trocasse `keptMoneyStartingIn` por `paidStartingIn`, a não
reembolsável cancelada sumiria do relatório e todos os testes continuariam passando. O mesmo vale para trocar `==` por
`!=` só no caso NON_REFUNDABLE. Também falta um teste da taxa com uma reserva CHECKED_OUT e outro com um valor de taxa
diferente de 4,50. Nenhum desses casos tem defeito no código, mas um revisor exigente pediria pelo menos o teste do
relatório por plano com uma não reembolsável cancelada.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` e não há chave estrangeira sem
índice nem data guardada como texto. Há uma restrição `EXCLUDE` contra duas reservas do mesmo quarto na mesma noite. A
única migração, `V1__create_tables.sql`, está igual à da base. As colunas `status`, `rate_plan`, `type` e `channel` são
`text` sem CHECK, o que vem da base (`measures/database.md`).
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mexeu nele nem em nenhuma migração. O código novo de dados só lê:
`startingIn` usa `all()`, que tem SQL constante e parametrizado com try-with-resources
(`src/main/java/hotel/reservations/ReservationStore.java:111-124,136-145`). Ele traz a tabela inteira para filtrar o mês
em Java. Isso já era assim nos relatórios da base (o `RevenueReport` da base percorria `store.all()`): o agente não
piorou nem resolveu, e só vale uma nota de carga se a tabela crescer muito. Os candidatos a SQL montado com `+` da
medição são todos constantes (`ReservationStore.java:36,86,101,114`).

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 09 | 13 de 13 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada:

- Os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>", com os nomes combinados.
- A ocupação e o financeiro continuam iguais depois do título.
- O relatório da taxa de turismo cobra R$ 4,50 por hóspede por noite só das reservas pagas, na ordem do financeiro,
  e com mês vazio mostra total 0.00.
- O relatório por plano traz STANDARD e depois NON_REFUNDABLE, mesmo sem reservas, e soma o que ficou com a pousada.
- Um mês fora do formato dá 400.
- O que já funcionava continua funcionando, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

## Dúvidas

O pedido diz que "a prefeitura muda o valor da taxa todo ano", sem dizer como. O agente leu a taxa de uma variável
de ambiente, `TOURISM_TAX_PER_GUEST_NIGHT` (`src/main/java/hotel/Hotel.java:56-57`), e não de uma tabela com o valor
por ano. Do jeito que está, um relatório de um mês do ano anterior, tirado depois da mudança, sai com a taxa nova. O
texto não fecha esse ponto, e a evidência não decide se isso importa para a prefeitura.
