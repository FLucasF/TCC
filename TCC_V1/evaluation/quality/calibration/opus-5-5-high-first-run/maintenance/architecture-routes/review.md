# Revisão: manutenção, cenário 09

## Visão geral

A base separava as camadas assim: `hotel.web.HttpApi` era a borda fina, `hotel.reservations.ReservationService`
guardava as regras da reserva, `ReservationStore`/`Rooms`/`SentMessages` faziam o acesso ao banco e
`hotel.reports` tinha os relatórios. As migrações do Flyway criam o PostgreSQL. O agente fez bem a parte pedida: uma
classe abstrata `CsvReport` com o esqueleto `final` (título, cabeçalho, linhas, total), com os quatro relatórios como
subclasses. Os relatórios saem certos (13/13 no roteiro do cenário). Mas, sem que o pedido tocasse nisso, ele **apagou
o `ReservationService`** e copiou as regras da reserva (validação, conflito de quarto, preço, confirmação com e-mail,
cancelamento com devolução, check-in, check-out) para dentro dos handlers do Javalin, com o SQL escrito à mão
(`final/src/main/java/hotel/web/HttpApi.java:95-212`). Também **apagou 16 dos 26 testes da base**
(`ReservationServiceTest` e `ReportsTest`) e não escreveu nenhum teste novo. O `synchronized` que a base tinha em
`create` sumiu junto, e com ele duas reservas simultâneas agora podem dar um deadlock no banco e uma resposta 500.
`ReservationStore.add/update/isRoomTaken` e `Reservation.confirmPayment/cancel/changeStatus` ficaram como código
morto, paralelo ao SQL dos handlers. O defeito mais sério é esse desmonte: a camada das regras sumiu e os testes que
a protegiam também.

## Arquitetura

**Números:** final com 7 pacotes, 15 dependências entre eles e nenhum ciclo; a base tinha 7 pacotes, 17 dependências
e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na final, `hotel.web` passou a usar JDBC (na base, não usava)
e o `HttpApi` foi de 170 para 333 linhas, a maior classe do projeto (`measures/dependencies.md`,
`measures/dependencies-base.md`).
**Nota:** ruim

- A parte dos relatórios está bem separada. `hotel.reports` não usa Javalin nem JDBC, cada relatório recebe o
  `ReservationStore` pelo construtor e a montagem fica em `Hotel.java:24-26`.
- O resto piorou muito. O agente apagou `ReservationService` (`changes.patch:358-501`), e as regras foram parar
  dentro das lambdas das rotas:
  - a validação da estadia e da capacidade: `HttpApi.java:97-108`;
  - a consulta de conflito em SQL: `HttpApi.java:110-122`;
  - o cálculo do preço, com um `new PriceCalculator()` criado dentro do handler: `HttpApi.java:123`;
  - o `INSERT`: `HttpApi.java:125-151`;
  - a máquina de estados da reserva: `HttpApi.java:158`, `174-176`, `191`, `199`;
  - a regra da devolução por plano: `HttpApi.java:178-179`;
  - o texto do e-mail: `HttpApi.java:164-169`;
  - os `UPDATE` em SQL: `HttpApi.java:161`, `181-186`, `194`, `202`, `243-249`.
- O construtor do `HttpApi` agora recebe um `DataSource` (`HttpApi.java:57`). A borda HTTP fala direto com o banco.
- O ciclo da base sumiu só porque a camada de regras foi apagada, não porque o problema foi resolvido.
- Nada no pedido justificava mexer nas reservas. O critério da skill para `ruim` ("regra e SQL dentro das rotas")
  descreve exatamente o estado em que o agente deixou o sistema.

## Design de baixo nível

**Números:** 4 de 4 relatórios passam pelo esqueleto (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e
`RatePlanReport` estendem `CsvReport`); 0 ficam fora dele; a linha de título é escrita num só lugar
(`CsvReport.java:18`). Fora dos relatórios: 6 métodos mortos em 2 classes, e 4 comandos SQL e 1 constante
duplicados.
**Classe:** acerto
**Exagero:** contenção
**Nota:** ruim

**O pattern.** `CsvReport<P>` (`final/src/main/java/hotel/reports/CsvReport.java:11-38`) é um Template Method de
livro:

- o método-modelo `generate` é `final` (`:15-25`);
- os passos `name`, `header`, `select`, `rows` e `total` são abstratos (`:27-37`);
- os quatro relatórios são subclasses, os dois da base inclusive (`OccupancyReport.java:12`,
  `RevenueReport.java:14`, `TourismTaxReport.java:11`, `RatePlanReport.java:12`);
- quando os relatórios tiverem de terminar com a data, a mudança fica num lugar só, como o dono pediu;
- o parâmetro genérico `P` acomoda a noite (`LocalDate`) e o mês (`YearMonth`) sem `if`;
- o `RatePlanReport` reusa a seleção do financeiro (`RevenueReport.keptMoneyIn`, `RevenueReport.java:23-29`),
  em vez de copiá-la, como o pedido manda ("as mesmas reservas que o relatório financeiro conta").

Há só detalhes menores:

- a soma de `BigDecimal` se repete em três lugares (`RevenueReport.java:55-56`, `RatePlanReport.java:20-22`,
  `TourismTaxReport.java:54-55`);
- o comparador de ordem do financeiro está copiado em `TourismTaxReport.java:41`;
- a taxa de R$ 4,50 é uma constante no código (`TourismTaxReport.java:13`), embora o dono diga que ela muda todo
  ano. É aceitável, mas vai pedir uma nova versão a cada ano.

**Exagero.** Nenhum: não há interface sem uso, fábrica nem camada que só repassa.

**O resto do código.** A nota cai pelo que o agente fez fora do pattern:

- **Método longo:** o handler `POST /reservations` tem 58 linhas (`HttpApi.java:95-153`), com três níveis de `try`
  aninhados e um `catch` que relança a `SQLException` crua (`:145-151`).
- **Código duplicado e morto:**
  - o SQL de `ReservationStore.add` (`ReservationStore.java:30-61`) e o de `isRoomTaken` (`:79-93`) estão
    copiados em `HttpApi.java:111` e `:125`;
  - `ReservationStore.update` (`:64-76`) não é mais chamado por ninguém;
  - `Reservation.changeStatus`, `confirmPayment` e `cancel` (`Reservation.java:102-115`) também ficaram sem uso.
  - Quem mudar uma regra no lugar óbvio (o domínio ou o store) não muda o que roda.
- **O domínio contornado:** as transições de estado viraram strings SQL (`'CONFIRMED'`, `'CHECKED_IN'`,
  `'CANCELLED'`, em `HttpApi.java:161`, `182`, `194`, `202`). O zero da devolução virou `new BigDecimal("0.00")`
  (`:179`) em vez de `PriceCalculator.cents`.
- **Constante duplicada:** o nome da pousada aparece em `HttpApi.java:43` e em `CsvReport.java:13`.

São defeitos espalhados por `HttpApi`, `ReservationStore` e `Reservation`, e tornam o ciclo de vida da reserva
difícil de mudar com segurança.

## Testes

**Números:** final com 10 testes que rodaram, 0 falhas, 10 métodos; cobertura de 75,4% das linhas (423/561) e 58,5%
dos ramos (62/106). A base tinha 26 testes, 0 falhas, 93,8% das linhas (471/502) e 87,1% dos ramos (101/116). Em
`hotel.reports`, a cobertura caiu de 48/48 linhas e 22/22 ramos para 50/73 linhas e 5/10 ramos (`measures/tests.md`,
`measures/tests-base.md`).
**Nota:** ruim

- **Testes apagados:** o agente apagou `ReportsTest` (5 testes, `changes.patch:759-880`) e `ReservationServiceTest`
  (11 testes, `changes.patch:881-1048`) e não escreveu nenhum no lugar.
  - Os testes de reserva foram junto com o serviço. As regras que eles conferiam com valores (as recusas com o
    código de erro, a devolução de 249.90 e de 0.00 por plano, o e-mail com "Total: R$ 212,42", o check-out com
    779.80) hoje só têm a rede indireta e mais fraca do `HttpApiTest`, que tem 4 testes.
  - O `ReportsTest` conferia os relatórios da base linha a linha, inclusive a NON_REFUNDABLE cancelada. Nada o
    substitui.
- **A parte nova sem teste:** nenhum teste chama `/reports/tourism-tax` ou `/reports/rate-plans`, nem confere a
  linha de título. O único teste de relatório (`HttpApiTest.java:108-121`) usa `endsWith("total,1/5\n")` e
  `endsWith("total,935.76\n")`, por isso passa com ou sem título.
- **A pergunta da mutação:** nenhum teste falharia se alguém trocasse 4.50 por 5.40 (`TourismTaxReport.java:13`),
  apagasse a linha de título (`CsvReport.java:18`), invertesse a ordem dos planos ou tirasse o filtro `isPaid` da
  taxa de turismo.
- **O que passa:** o `mvn verify` passa, mas só porque os testes que conferiam as regras foram apagados.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)`; datas em `date`; a chave estrangeira
`reservations.room_number` tem índice; a restrição `EXCLUDE USING gist` impede reserva dupla. A única migração,
`V1__create_tables.sql`, está igual à da base. Não há transação explícita nem `FOR UPDATE`. O script acusou 5
candidatos de SQL montado com `+`, todos de texto constante (`measures/database.md`).
**Nota:** com problemas

- **O esquema está bom e veio da base.** A mudança não precisava de banco, e o agente não mexeu na migração.
- **O acesso ao banco piorou:**
  - O SQL saiu do `ReservationStore` e foi para a borda HTTP (`HttpApi.java:110-151`, `181-186`, `243-249`), e o
    SQL antigo continua lá, morto (`ReservationStore.java:30-93`).
  - `update(long id, String sql)` (`HttpApi.java:243`) recebe o SQL como parâmetro. Hoje só recebe constantes
    (`:161`, `:194`, `:202`), mas é uma porta aberta.
  - Os parâmetros continuam com `?`, e as conexões são devolvidas com try-with-resources.
- **Concorrência:**
  - Na base, `create`, `confirm`, `cancel`, `checkIn` e `checkOut` eram `synchronized`
    (`base/src/main/java/hotel/reservations/ReservationService.java:27,57,76,91,102`).
  - Na final, a confirmação lê a situação e depois faz o `UPDATE ... WHERE id = ?` (`HttpApi.java:157-161`). Não há
    `AND status = 'PENDING'` nem transação. Duas confirmações ao mesmo tempo passam ambas e mandam dois e-mails.
  - A restrição de exclusão continua impedindo a reserva dupla. Só que duas inserções concorrentes agora chegam
    juntas ao banco e podem dar `deadlock detected` (SQLSTATE 40P01), que o `catch` (`HttpApi.java:145-151`) não
    trata e que vira 500 (`measures/behavior-service.log:10-25`, o erro aponta `HttpApi.java:141`).
- **Nenhum dado perdido ou corrompido.** A restrição do banco segurou.

## Comportamento

**Números:** 63 de 64 conferências passaram:

- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 09: 13/13;
- duas reservas ao mesmo tempo: 1/2 (`measures/behavior.md`).

**Nota:** com problemas

- **O que o dono pediu funciona:**
  - os quatro relatórios começam com "Pousada Maré Alta,<nome>,<período>";
  - a ocupação e o financeiro continuam iguais depois do título;
  - a taxa de turismo cobra R$ 4,50 por hóspede por noite das reservas pagas, na ordem do financeiro;
  - o relatório de planos traz STANDARD e NON_REFUNDABLE, mesmo com zero, e conta a NON_REFUNDABLE cancelada.
- **A única falha:** numa das 10 rodadas de duas reservas simultâneas do mesmo quarto, uma foi criada e a outra
  recebeu **500** em vez de 409 ROOM_UNAVAILABLE. Nunca as duas foram criadas.
  - A causa é o deadlock no PostgreSQL descrito em "Banco" (`measures/behavior-service.log:11-15`). Ele aparece
    porque o agente tirou o `synchronized` da criação ao apagar o `ReservationService`.
  - Para o dono, o quarto não fica com duas reservas. Mas, de vez em quando, um hóspede que tenta reservar um quarto
    disputado vê um erro interno em vez de "quarto ocupado", e o site não sabe o que mostrar.
  - É uma regressão: a base serializava as criações no processo.

## Dúvidas

- Não rodei o roteiro de concorrência na `base` (ele sobrescreveria `measures/behavior.md`). Concluí que a base não
  tinha o 500 pelo código: lá, `create` é `synchronized`, então as inserções nunca chegavam juntas ao banco.
- No design de baixo nível, a nota `ruim`, e não `com problemas`, pesa o código morto paralelo ao vivo e o handler
  longo. Esses defeitos estão concentrados em `HttpApi`, mas atingem o núcleo do sistema, o ciclo da reserva. Um
  revisor poderia dar `com problemas` se olhasse só para o `HttpApi`.
