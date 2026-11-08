# Revisão: manutenção, cenário 09

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras e o
`ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. A montagem fica
em `hotel.Hotel`. Uma requisição de relatório entra em `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:91-106`),
que lê o parâmetro e chama o relatório. O relatório pega `store.all()` e filtra em Java. Os dados ficam no PostgreSQL,
num esquema criado pelo Flyway. O agente criou uma classe abstrata `Report` com o método-modelo `final`
(`final/src/main/java/hotel/reports/Report.java:12-19`) e pôs os quatro relatórios como subclasses dela: os dois que
já existiam e os dois novos. Também criou as duas rotas novas e 4 testes. As 64 conferências do roteiro passam. O
problema mais sério é outro: para passar os dados aos passos do modelo, cada relatório agora guarda o resultado em
campos da instância (`RevenueReport.java:18-19`, `TourismTaxReport.java:18-19`, `RatePlansReport.java:15-17`,
`OccupancyReport.java:17`). Só existe uma instância de cada relatório para o servidor inteiro (`Hotel.java:29-35`).
Fiz um teste com pedidos ao mesmo tempo para meses diferentes, e **301 de 1200 respostas saíram misturadas**: o título
de julho com linhas de agosto, um total de `49090.80` quando julho dava `26776.80`, e uma taxa de turismo de agosto
com 90 linhas em vez de 30. A base usava variáveis locais e não tinha esse defeito.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. O único ciclo é
`hotel.documents ↔ hotel.reservations`, que já existia na base (`measures/dependencies.md` e
`measures/dependencies-base.md`). O pacote `hotel.reports` passou de 2 para 6 classes e de 95 para 330 linhas. Ele
continua sem Javalin e sem JDBC.
**Nota:** bom

As peças novas entraram nas camadas certas. Os relatórios ficam em `hotel.reports` e dependem só de
`hotel.reservations`: `TourismTaxReport.java:3-4` e `RatePlansReport.java:3-5` importam `Reservation`,
`ReservationStore` e `RatePlan`. As rotas novas são finas: leem o mês, chamam o relatório e escrevem o CSV
(`HttpApi.java:99-106`). Elas reaproveitam o `readMonth` que já existia (`HttpApi.java:166`). A montagem continua num
lugar só, `Hotel.java:28-35`, onde os relatórios novos são criados e passados ao `HttpApi`. O agente não criou
dependência nova entre pacotes nem ciclo novo. Um ponto pequeno: o `README.md` não foi atualizado e só lista as duas
rotas antigas (`README.md:56-57`).

## Design de baixo nível

**Números:** os 4 relatórios (`occupancy`, `revenue`, `tourism-tax` e `rate-plans`) estão na estrutura, e nenhum
ficou fora dela. O esqueleto está em `Report.generate` (final), com 4 passos abstratos. Nenhum relatório escreve à mão
o título, o cabeçalho ou o total. Num teste de concorrência que eu mesmo rodei, 301 de 1200 respostas saíram erradas.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** O que aproxima do Template Method:

- `Report` é uma classe abstrata com o método-modelo `public final String generate(String period)`, que monta o
  título com o nome da pousada, o cabeçalho, as linhas e o total (`Report.java:12-19`).
- Os passos `reportName`, `header`, `rows` e `totalLine` são abstratos (`Report.java:22-31`).
- Os dois relatórios da base viraram subclasses e deixaram de escrever o cabeçalho e o total por conta própria
  (`OccupancyReport.java:13,36-63`, `RevenueReport.java:15,44-72`). Os dois novos também são subclasses
  (`TourismTaxReport.java:13`, `RatePlansReport.java:12`).
- A linha com a data que o dono anunciou para o ano que vem entra num lugar só, `Report.java:17`.

O que afasta: a escolha e a ordenação das reservas não são passos do modelo. Cada subclasse faz isso no seu próprio
`generate(YearMonth|LocalDate)` antes de chamar `super.generate(...)` (por exemplo, `TourismTaxReport.java:25-41`).
Isso não basta para o malfeito, porque a sequência título, cabeçalho, linhas e total não se repete em nenhum relatório.

**Exagero.** Não há. Não existe interface com uma implementação só, fábrica ou camada que só repassa: são uma classe
abstrata e quatro subclasses.

**O resto do código: a nota.**

- **Estado mutável compartilhado entre requisições (sério).** Os passos do modelo não recebem os dados como parâmetro.
  Por isso cada `generate` grava o resultado em campos (`this.withRevenue = ...`, `this.revenue = ...`) em
  `RevenueReport.java:26,36`, `TourismTaxReport.java:26,36`, `RatePlansReport.java:24,39-40` e
  `OccupancyReport.java:25`. Depois, `rows()` e `totalLine()` leem esses campos. Como cada relatório é um objeto único,
  usado por todas as threads do Jetty (`Hotel.java:33-35`), dois pedidos simultâneos escrevem nos mesmos campos.
  Medi isso subindo o `final/target/hotel.jar` com 30 reservas em julho e 30 em agosto e disparando 300 rodadas de 4
  pedidos simultâneos (revenue e tourism-tax, julho e agosto): 301 de 1200 respostas vieram erradas. Exemplos:
  - um `revenue` de 2027-07 com linha de agosto e `total,49090.80`, quando o certo é `26776.80`;
  - totais como `102644.40` e `total,0.00`;
  - um `tourism-tax` de 2027-08 com 90 linhas e `total,810.00`.

  Na base, os dois relatórios usavam variáveis locais e não tinham esse defeito (`base/.../RevenueReport.java`, no
  `changes.patch`: `List<Reservation> withRevenue = new ArrayList<>();`). Para o dono, isso é dado errado no relatório
  do contador e no da prefeitura.
- **API vazada.** O `public final generate(String period)` herdado de `Report` pode ser chamado direto em qualquer
  relatório, sem a preparação. Nesse caso `rows()` encontra os campos ainda `null` e a chamada termina em
  `NullPointerException` (por exemplo, `RevenueReport.java:56`).
- **Duplicação.**
  - O filtro "dinheiro que ficou e começa no mês" foi copiado de `RevenueReport.java:28-30` para
    `RatePlansReport.java:30-32`, quando o texto diz que os dois contam as mesmas reservas.
  - A ordenação foi copiada de `RevenueReport.java:34` para `TourismTaxReport.java:34`.
  - Em `TourismTaxReport`, as noites são calculadas duas vezes (`:58` e `:77`) e a taxa de cada reserva também
    (`:38` e `:59`).
- **Cópia por plano.** `RatePlansReport.rows()` escreve à mão uma linha para `STANDARD` e outra para `NON_REFUNDABLE`
  (`RatePlansReport.java:62-67`), em vez de percorrer o `EnumMap` que já está na ordem do enum. Se surgir um plano
  novo, ele entra no total (`:41-44`) mas não aparece nas linhas.
- **O que está certo.**
  - Dinheiro em `BigDecimal`, com a taxa como constante (`TourismTaxReport.java:15`).
  - `setScale(2)` sem modo de arredondamento (`:80`), o que funciona porque 4,50 × inteiro sempre tem 2 casas.
  - Nomes claros.

A nota é `ruim` pelo primeiro item: um risco real de relatório errado, presente nos quatro relatórios e trazido pelo
próprio refactor.

## Testes

**Números:**

- `final`: 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados. São 30 métodos escritos.
- Cobertura do `final`: 94,8% das linhas (566/597) e 89,3% dos ramos (125/140). Em `hotel.reports`, 133/133 linhas e
  46/46 ramos.
- `base`: 26 testes e 93,8% das linhas.
- O agente acrescentou 4 testes, manteve os 26 da base passando e não apagou nem enfraqueceu nenhum. Nos 5 testes de
  relatório da base, só acrescentou a linha de título esperada.

**Nota:** com problemas

O que está bem:

- Os testes de unidade comparam o CSV inteiro com `assertEquals`:
  - taxa de 9,00 para 1 hóspede em 2 noites, com a reserva não paga e a cancelada fora da lista
    (`ReportsTest.java:122-134`);
  - 3 hóspedes em 3 noites dão 40,50 (`ReportsTest.java:136-152`);
  - um plano sem reserva aparece com `0,0.00` (`ReportsTest.java:154-165`);
  - a separação por plano (`ReportsTest.java:167-181`);
  - o título nos dois relatórios antigos (`ReportsTest.java:61,74,87,104,116`).
- Trocar 4.50, inverter a ordem dos planos ou mudar o nome de um relatório faz algum teste falhar.

O que ficou sem rede:

- A regra que o texto destaca no relatório por plano, "contando as mesmas reservas que o relatório financeiro conta",
  não tem teste com uma `NON_REFUNDABLE` cancelada. A reserva cancelada do `@BeforeEach` é `STANDARD` com devolução
  (`ReportsTest.java:46,52-53`). Se alguém trocasse `reservation.amountKept().signum() > 0` por
  `reservation.isPaid()` em `RatePlansReport.java:30-32`, todos os testes continuariam passando, mas o relatório
  perderia a não reembolsável cancelada.
- Nenhum teste inclui uma reserva `CHECKED_OUT` na taxa de turismo.
- No teste pela API (`HttpApiTest.java:123-134`), as asserções novas são `contains` e `startsWith`, mais fracas, mas
  isso é aceitável porque o CSV completo já é conferido nos testes de unidade.
- Nenhum teste chama um relatório duas vezes ou em paralelo, então o defeito do estado compartilhado passa sem ser
  visto.

## Banco

**Números:** 3 tabelas (`messages`, `reservations` e `rooms`), todas com chave primária. A única migração,
`V1__create_tables.sql`, está igual à da base, e o agente não acrescentou migração. O dinheiro está em
`numeric(10,2)`. Nenhuma chave estrangeira está sem índice e nenhuma data está em texto. Há um `EXCLUDE USING gist`
contra duas reservas na mesma noite. Pela conferência, as colunas de situação e plano são `text` sem `CHECK`
(`measures/database.md`).
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: as migrações e o `ReservationStore` são os da base. O SQL montado
com `+` que o script aponta (`ReservationStore.java:33,83,98,111`) concatena só constantes (`COLUMNS`), e os valores
vão por `?`. O que se pode apontar já vinha da base:

- as colunas `status`, `rate_plan`, `type` e `channel` sem `CHECK`;
- os relatórios que leem a tabela inteira com `store.all()` (`ReservationStore.java:111`) e filtram em Java.

O agente repetiu esse segundo padrão nos dois relatórios novos (`TourismTaxReport.java:27`,
`RatePlansReport.java:29`). Isso vale uma nota sobre a carga quando a tabela crescer, mas não muda a coluna.

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 09 | 13 de 13 |
| duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Pela API, o sistema faz o que o dono pediu:

- os quatro relatórios começam com `Pousada Maré Alta,<nome>,<período>`;
- a ocupação e o financeiro continuam iguais depois do título;
- a taxa de turismo lista as reservas pagas do mês, na ordem do financeiro, e soma R$ 4,50 por hóspede por noite;
- um mês vazio dá `total,0.00`;
- o relatório por plano mostra `STANDARD` e `NON_REFUNDABLE`, mesmo com zero, e conta a não reembolsável cancelada;
- um mês fora do formato responde 400.

Nada que o roteiro chama falhou. O defeito de concorrência dos relatórios, que eu mesmo medi, não aparece no roteiro
e foi contado no design de baixo nível. Mesmo assim, o dono precisa saber: se duas pessoas (ou o site e o contador)
tirarem relatórios ao mesmo tempo, um deles pode sair com linhas e total de outro mês.

## Dúvidas

- **A classe do pattern.** O esqueleto é um Template Method correto e cobre os quatro relatórios, por isso dei
  `acerto`. Mas o jeito de passar os dados aos passos, por campos da instância, é justamente o que causa o defeito de
  concorrência. Um revisor poderia ver isso como "malfeito" na execução do pattern. Pelos critérios da referência,
  fiquei com `acerto` e pus o peso na nota.
- **A nota do design.** Para ser `ruim`, contei o risco de dado errado medido no `final/target/hotel.jar` com pedidos
  simultâneos. O roteiro oficial não exercita esse caso, e numa pousada pequena relatórios simultâneos devem ser raros.
  Ainda assim, quando acontece, o relatório sai com dado errado, sem aviso.
- **A taxa de turismo.** A taxa ficou como constante no código (`TourismTaxReport.java:15`), então mudar o valor a
  cada ano exige uma nova versão do sistema. O texto não pediu que fosse configurável, por isso não contei como defeito.
