# Revisão: manutenção, cenário 09

## Visão geral

O agente partiu de uma base organizada em pacotes por assunto: `hotel.web` para a borda HTTP com Javalin,
`hotel.reservations` para as regras e o acesso ao banco (`ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents` e `hotel.reports`, com a montagem em `hotel.Hotel`. Uma requisição de relatório entra por
`HttpApi`, que lê o mês e chama `XxxReport.generate`. O relatório carrega todas as reservas com `ReservationStore.all()`,
filtra e ordena em Java e devolve o CSV. Os dados ficam no PostgreSQL, num esquema criado pelo Flyway. A mudança se
resume a acrescentar a linha de título em `OccupancyReport` e `RevenueReport`, criar `TourismTaxReport` e
`RatePlansReport` como cópias inteiras do mesmo roteiro e ligar as duas rotas novas. Não há nenhum esqueleto comum,
embora o dono tenha pedido de forma explícita: "quando o jeito dos relatórios mudar, quero que mude em todos de uma
vez". O ponto mais grave é um erro de cálculo que o roteiro não pegou. A taxa de turismo conta as noites com
`LocalDate.compareTo` (`src/main/java/hotel/reports/TourismTaxReport.java:40`), e esse método devolve a diferença de
meses quando os meses são diferentes. Por isso toda estadia que atravessa a virada do mês sai com o número errado de
noites e com a taxa errada. Conferi pela API: uma reserva de 30/07 a 02/08 com 2 hóspedes sai como "1 noite, 9.00",
quando deveriam ser 3 noites e R$ 27,00.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` comparado com
`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. Só
mudou o tamanho: `hotel.reports` passou de 2 classes e 95 linhas para 4 classes e 216 linhas, e `HttpApi` de 170 para
186 linhas.
**Nota:** bom

As peças novas ficaram no lugar certo. As rotas novas são finas: leem o mês, chamam o relatório e escrevem o CSV
(`src/main/java/hotel/web/HttpApi.java:99-106`). Os relatórios não conhecem o Javalin nem o JDBC e leem os dados pelo
`ReservationStore` (`src/main/java/hotel/reports/TourismTaxReport.java:26`,
`src/main/java/hotel/reports/RatePlansReport.java:32`). A montagem continua num lugar só
(`src/main/java/hotel/Hotel.java:28-35`). O agente não criou dependência nova nem ciclo novo. O único ciclo vem da
base.

## Design de baixo nível

**Números:** dos 4 relatórios, nenhum passa por uma estrutura comum. A linha de título está escrita à mão em 4 lugares
(`OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:36`, `RatePlansReport.java:43`), e o
roteiro inteiro (selecionar, ordenar, título, cabeçalho, linhas, total) está repetido nas 4 classes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O pedido descreve um roteiro fixo, comum a quatro relatórios, que vai crescer (relatórios de
cancelamentos e de hóspedes por estado) e que vai mudar em todos de uma vez (a linha com a data no fim). Isso pede um
Template Method, ou um escritor genérico que receba as partes que variam. O agente não fez nem um nem outro. Cada
relatório continua escrito inteiro, e os dois novos são cópias do `RevenueReport`: compare
`TourismTaxReport.java:24-38` com `RevenueReport.java:23-37`. Quando a linha com a data no fim for pedida, alguém vai
ter de alterar 4 classes, e depois 6. Pelo critério da referência, quatro relatórios escritos inteiros, cada um com a
sua linha de título, são uma **omissão**.

**O exagero.** Não há estrutura sem necessidade: nem interface, nem fábrica, nem camada nova.

**O resto do código novo.** Os defeitos estão concentrados nas duas classes novas:

- **A regra do financeiro foi copiada.** O pedido diz que o relatório de planos conta "as mesmas reservas que o
  relatório financeiro conta". Em vez de reaproveitar essa seleção, o agente copiou o filtro
  (`RatePlansReport.java:33-35` repete `RevenueReport.java:26-28`). Se a regra do financeiro mudar, os dois
  relatórios passam a divergir sem aviso.
- **As noites foram recalculadas, e do jeito errado.** A reserva já guarda as noites em `Price.nights()`
  (`src/main/java/hotel/reservations/Price.java:5`, usado em `ReservationReceipt.java:34`). O relatório recalcula com
  `checkOut().compareTo(checkIn())` (`TourismTaxReport.java:40`), que não conta dias. É daí que vem o erro descrito na
  coluna do comportamento.
- **Os planos estão fixos no código.** Os mapas são `HashMap` preenchidos à mão com os dois planos
  (`RatePlansReport.java:24-30`), enquanto a saída percorre `RatePlan.values()` (`RatePlansReport.java:49`). Um
  terceiro plano faria `countByPlan.get(plan) + 1` dar `NullPointerException` (`RatePlansReport.java:37`). Um
  `EnumMap` preenchido a partir de `values()` evitaria isso.
- **Detalhes menores.** `java.math.RoundingMode` aparece com o nome completo no meio do código
  (`TourismTaxReport.java:44`). O valor da taxa, que segundo o dono muda todo ano, é uma constante na classe
  (`TourismTaxReport.java:17`), aceitável mas sem nenhum ponto de troca. O README não ganhou as duas rotas novas.

## Testes

**Números:** 31 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 31 métodos escritos, contra 26 na base. A
cobertura de linhas foi de 94,6% (548 de 579), contra 93,8% na base, e a de ramos de 88,8% (119 de 134), contra
87,1%. `hotel.reports` ficou com 115 de 115 linhas e 40 de 40 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O que está bem:

- Os testes da base continuam todos lá, e a única mudança neles foi a linha de título nos valores esperados
  (`src/test/java/hotel/reports/ReportsTest.java:61,74,87,104,116`). Nenhum foi enfraquecido.
- Os testes novos comparam o CSV inteiro, com valores: a taxa de abril (`ReportsTest.java:123-137`), os planos em
  abril com `NON_REFUNDABLE,0,0.00` (`ReportsTest.java:156-168`) e um mês vazio (`ReportsTest.java:171-181`).
- Pela API, o teste de `HttpApiTest.java:124-140` confere `total,18.00` com 2 hóspedes e 2 noites, e
  `STANDARD,1,935.76`.

Na pergunta da mutação:

- Tirar a multiplicação por hóspedes faz o teste de `HttpApiTest.java:134` falhar.
- Tirar o filtro de dinheiro retido faz o teste de `ReportsTest.java:156` falhar.
- Contar as noites de um jeito errado só para estadias que atravessam o mês **não faz nenhum teste falhar**: todas as
  reservas testadas ficam dentro de um mês só. É justamente o erro que existe.
- O plano `NON_REFUNDABLE` nunca aparece com valor diferente de zero. Também falta um teste de que a
  `NON_REFUNDABLE` cancelada conta no relatório de planos: a reserva de maio que a base usa para isso
  (`ReportsTest.java:96`) não é usada pelo relatório de planos. Trocar a ordem dos planos ou somar o plano errado
  passaria despercebido.

O teste `tourismTaxWithMultipleGuests` (`ReportsTest.java:140-153`) usa 1 hóspede, como o próprio comentário da linha
146 admite: o nome não corresponde ao que o teste confere.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)`, sem chave estrangeira sem índice
e sem data em texto. A restrição `EXCLUDE` contra duas reservas na mesma noite está no lugar. A única migração,
`V1__create_tables.sql`, está igual à da base, e o agente não acrescentou nenhuma (`measures/database.md`). As
conferências acusaram só as colunas de situação e de tipo sem `CHECK`, que já vinham assim da base.
**Nota:** bom

A mudança não precisava mexer no esquema, e o agente não mexeu: a migração da base ficou intacta, e o serviço subiu
contra um banco vazio. As consultas da base usam parâmetros, e os `+` listados pelo script só juntam constantes
(`src/main/java/hotel/reservations/ReservationStore.java:33,83,98,111`). Os dois relatórios novos repetem o padrão da
base de trazer a tabela inteira com `ReservationStore.all()` (`ReservationStore.java:111`) e filtrar o mês em Java
(`TourismTaxReport.java:26`, `RatePlansReport.java:32`). Isso não derruba a coluna, mas agora são quatro varreduras
completas da tabela, que cresce sem limite.

## Comportamento

**Números:** o roteiro passou em 64 de 64 conferências (`measures/behavior.md`): 46 de 46 na parte comum, 3 de 3
depois de reiniciar, 13 de 13 no cenário e 2 de 2 nas reservas simultâneas. Fora do roteiro, conferi pela API um caso
que o pedido cobre ("R$ 4,50 por hóspede por noite"), e ele **falhou**.
**Nota:** com problemas

O roteiro confirma o que o pedido descreve: o título nos quatro relatórios, com o nome e o período, a ocupação e o
financeiro iguais aos de antes depois do título, a taxa de turismo com as colunas e o total, os dois planos na ordem
pedida, mesmo sem reserva, e o 400 para um mês fora do formato.

Depois, subi o serviço contra um banco vazio e criei duas reservas que atravessam a virada do mês:

- Uma reserva de 2027-07-30 a 2027-08-02, com 2 hóspedes, confirmada. O próprio JSON da reserva diz `"nights":3`, mas
  `GET /reports/tourism-tax?month=2027-07` devolveu `1,201,2027-07-30,2027-08-02,2,1,9.00` e `total,9.00`. O certo
  seria `3,27.00`.
- Uma reserva de 2027-12-30 a 2028-01-02, também com 2 hóspedes: deu `1,9.00`, quando deveria dar 3 noites e R$ 27,00.

A causa está em `TourismTaxReport.java:40`: `LocalDate.compareTo` devolve a diferença de anos, ou de meses, antes da
de dias. Para o dono da pousada, isso quer dizer que **toda estadia que começa num mês e termina no seguinte sai no
relatório da prefeitura com menos taxa do que a devida**. Numa pousada de litoral, as estadias que cruzam o fim do mês
são comuns. O relatório parece certo (tem as colunas, o total, o formato), e por isso o erro só apareceria numa
fiscalização. As estadias dentro de um mês só saem certas.

O relatório de planos deu certo no mesmo teste: `NON_REFUNDABLE,1,764.69` em dezembro.

## Dúvidas

- **A nota do comportamento.** Dei `com problemas` porque o roteiro passou inteiro e só uma regra do cenário falha.
  Mas o erro produz um valor errado num relatório fiscal, em silêncio e num caso frequente. Pelo critério geral de
  "risco de dado errado", também caberia `ruim`.
- **O valor da taxa como constante** (`TourismTaxReport.java:17`). O dono avisa que o valor muda todo ano, mas não
  pede que ele seja configurável. Contei isso como um ponto menor, não como defeito.
