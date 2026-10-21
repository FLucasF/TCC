# Revisão: construção, cenário 09

## Visão geral

O projeto tem 7 pacotes por assunto (`hotel`, `web`, `reservations`, `rooms`, `messages`, `documents`, `reports`).
Uma requisição entra em `web/HttpApi.java`, que lê o corpo ou o parâmetro e chama `ReservationService` ou uma das
quatro classes de relatório. Os dados ficam no PostgreSQL. O acesso é por JDBC puro, isolado em `ReservationStore`,
`Rooms` e `SentMessages`, com pool Hikari e esquema criado por migração do Flyway
(`src/main/resources/db/migration/V1__create_tables.sql`). Uma restrição `EXCLUDE` impede a reserva dupla no próprio
banco. Tudo funciona: 64 de 64 conferências do comportamento passaram e 28 de 28 testes também. O problema mais sério
está no que o cenário pede: os quatro relatórios em CSV estão escritos inteiros, cada um com a sua cópia da linha de
título, do cabeçalho, do laço e do total. Não há esqueleto comum. A mudança anunciada pelo dono ("todos os relatórios
vão ter que terminar com uma linha com a data [...] quero que mude em todos de uma vez") vai exigir mexer em quatro
lugares.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), segundo `measures/dependencies.md`. A maior classe é `HttpApi` (186 linhas). O Javalin só aparece em `hotel` e `hotel.web`; o JDBC, em `reservations`, `rooms` e `messages`.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota lê a entrada, chama o serviço ou o relatório e escreve a resposta
  (`web/HttpApi.java:69-107`). A tradução de erro para status fica num só `switch` (`web/HttpApi.java:116-123`).
  Nenhuma regra de preço ou de situação da reserva mora nos handlers.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` não usa `Context` nem monta SQL
  (`reservations/ReservationService.java:27-119`). O SQL fica em classes de acesso a dados por assunto:
  - `reservations/ReservationStore.java`;
  - `rooms/Rooms.java`;
  - `messages/SentMessages.java`.

  O pacote `reservations` usa JDBC só por causa do `ReservationStore`. É empacotamento por assunto, aceitável neste
  tamanho.
- **A montagem fica num lugar só:** `Hotel.java:22-31` cria e liga todas as peças, e `main` lê o ambiente
  (`Hotel.java:44-48`).
- **O defeito é o ciclo entre `documents` e `reservations`.**
  - `ReservationService` importa `ReservationReceipt` (`reservations/ReservationService.java:3`).
  - `ReservationReceipt` importa `Reservation` (`documents/ReservationReceipt.java:3, 24`).
  - Além disso, o texto do e-mail de confirmação é montado dentro do serviço de reservas
    (`reservations/ReservationService.java:125-131`). Apresentação mora na regra, e é ela que puxa o comprovante para
    dentro de `reservations`.

  É um ciclo pequeno, entre dois pacotes vizinhos, mas é novo e mostra uma responsabilidade no lugar errado.

## Design de baixo nível

**Números:** 4 relatórios, 0 dentro de um esqueleto comum e 4 escritos inteiros. A linha de título "Pousada Maré Alta,…" aparece copiada 4 vezes, e o nome da pousada aparece mais uma vez em `ReservationService.HOTEL_NAME`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Template Method).** O texto descreve quatro relatórios com os mesmos passos e avisa que virão
mais, com uma mudança de formato que deve valer "em todos de uma vez". O projeto não tem classe abstrata, nem escritor
genérico, nem qualquer peça comum. Cada relatório repete a sequência inteira:
- escolher as reservas: `store.all()` e o filtro;
- ordenar;
- a linha de título;
- o cabeçalho;
- o laço das linhas;
- a linha do total.

Onde isso está:
- `reports/OccupancyReport.java:23-46`, título na linha 34;
- `reports/RevenueReport.java:23-49`, título na linha 35;
- `reports/TourismTaxReport.java:22-50`, título na linha 33;
- `reports/RatePlanReport.java:20-48`, título na linha 31.

A referência descreve exatamente esse caso como omissão: "os quatro relatórios ficam escritos inteiros, cada um com a
sua linha de título". Nada aproxima da resposta certa além de as quatro classes terem o mesmo método `generate`, e nem
isso vem de uma interface ou base comum.

**O exagero.** Não há estrutura sem necessidade: nenhuma interface com uma implementação só, nenhuma fábrica, nenhuma
camada que só repassa. As classes de relatório são `final` e concretas. O resultado é contenção.

**O resto do código** é, em geral, limpo:
- `BigDecimal` com `HALF_EVEN` a cada passo (`reservations/PriceCalculator.java:23, 28, 36, 50-52`);
- `LocalDate` e `YearMonth`;
- `enum` para situação, plano e tipo de quarto;
- `Reservation` com estado privado, que só muda por métodos de pacote (`reservations/Reservation.java:102-114`);
- nenhum `static` mutável.

Os defeitos, localizados:
- **A regra "o dinheiro que ficou com a pousada no mês" está copiada.** O filtro está escrito igual em
  `reports/RevenueReport.java:24-31` e `reports/RatePlanReport.java:21-28`. O texto diz que o relatório por plano conta
  "as mesmas reservas que o relatório financeiro conta", e uma mudança nessa regra precisa ser lembrada em dois
  lugares.
- **O nome da pousada aparece em cinco lugares:** `reservations/ReservationService.java:12` e os quatro títulos acima.
- **Um `catch (Exception)` largo** em `web/HttpApi.java:129`. Ele transforma qualquer falha ao ler o corpo em
  `INVALID_REQUEST`, e o comentário reconhece o atalho.
- **A situação da reserva muda por dois caminhos diferentes.** Alguns métodos têm nome (`confirmPayment`, `cancel`) e
  outros usam um `changeStatus` genérico (`reservations/Reservation.java:102-104`). As transições permitidas ficam
  espalhadas em `if`s no serviço (`reservations/ReservationService.java:59, 78-79, 93, 104`). É pouco, mas não está
  num lugar só.

## Testes

**Números:** 28 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 28 métodos de teste no código. A cobertura é de 93,5% das linhas (532 de 569) e 89,1% dos ramos (123 de 138). O pacote `hotel.reports` tem 105 de 105 linhas e 44 de 44 ramos cobertos (`measures/tests.md`). Os testes misturam unidade (`PriceCalculatorTest`), serviço contra o banco real (`ReservationServiceTest`, `ReportsTest`) e API por HTTP (`HttpApiTest`), sem mocks.
**Nota:** com problemas

**O que está bem testado, com valores exatos:**
- **O preço:**
  - a temporada que começa em 15/12 e termina em fevereiro, inclusive o ano bissexto;
  - julho;
  - o desconto com "meio para o par", 37,485 → 37,48;
  - o desconto sobre a alta temporada.

  Está em `reservations/PriceCalculatorTest.java:19-75`.
- **O ciclo da reserva, as recusas, a devolução por plano e o e-mail:**
  `reservations/ReservationServiceTest.java:128-198`.
- **Os relatórios:** são comparados como texto inteiro, título incluído (`reports/ReportsTest.java:56-146`).

**As regras novas que ficam sem rede:**
- **O hóspede na taxa de turismo não é conferido.** Todas as reservas do `ReportsTest` têm 1 hóspede
  (`reports/ReportsTest.java:30-33`). Se alguém apagasse o fator `guests()` em `reports/TourismTaxReport.java:37-38`,
  o teste continuaria dando 9,00 e passaria.
- **O relatório por plano só é testado com `NON_REFUNDABLE,0,0.00`** (`reports/ReportsTest.java:128-134`).
  - A contagem de uma reserva não reembolsável não é conferida.
  - Também não é conferido que uma não reembolsável cancelada continue contando, como o texto pede. Trocar o filtro
    de `RatePlanReport` por `isPaid()` não quebraria nenhum teste.
- **A ordem da taxa de turismo** ("a mesma do financeiro") não é posta à prova: nos dados de teste, a ordem por id e a
  ordem por data coincidem.
- **O teste pela API não chama** `/reports/tourism-tax` nem `/reports/rate-plans` (`web/HttpApiTest.java:346-359`).

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária, criadas por 1 migração do Flyway. Não há dinheiro fora de `numeric(10,2)`, data guardada como texto nem chave estrangeira sem índice. Há 5 restrições de regra, entre elas o `EXCLUDE USING gist` contra a reserva dupla. As colunas `status`, `rate_plan`, `type` e `channel` são `text` sem `CHECK`. Não há nenhuma transação explícita (`measures/database.md`).
**Nota:** com problemas

**O que está bem:**
- **O esquema** é criado pelo Flyway ao subir (`Database.java:28`), numa migração versionada
  (`db/migration/V1__create_tables.sql`). Isso deixa o próximo pedido evoluir o banco.
- **Os tipos estão certos:**
  - dinheiro em `numeric(10, 2)` (`V1__create_tables.sql:30-35`);
  - datas em `date`;
  - a reserva ligada ao quarto por chave estrangeira (`V1__create_tables.sql:21`);
  - `CHECK` nos hóspedes e nas datas (`V1__create_tables.sql:27, 36`).
- **A reserva dupla é barrada pelo próprio banco**, com a restrição de exclusão que ignora as canceladas
  (`V1__create_tables.sql:38-40`). O código traduz a violação para `ROOM_UNAVAILABLE`
  (`reservations/ReservationStore.java:54-58`). Isso vale entre processos, e não só no `synchronized` do serviço.
- **O SQL usa sempre `?`.** Os quatro trechos montados com `+` só concatenam constantes, como `COLUMNS` em
  `reservations/ReservationStore.java:18-19`.
- **As conexões são devolvidas** com try-with-resources e vêm de um pool.

**Os defeitos:**
- **Falta transação onde duas escritas têm de andar juntas.** A confirmação grava a reserva e depois grava o e-mail,
  em duas conexões separadas e sem transação:
  - a reserva em `reservations/ReservationService.java:62-63` e `reservations/ReservationStore.java:64-76`;
  - o e-mail em `reservations/ReservationService.java:65-68` e `messages/SentMessages.java:20-35`.

  Se o segundo `INSERT` falhar, a reserva fica `CONFIRMED`, o hóspede não tem e-mail registrado e o cliente recebe
  500. Repetir a confirmação já dá 409.
- **As transições são ler, conferir em Java e gravar.** O `UPDATE` não tem `WHERE status = ...`
  (`reservations/ReservationStore.java:67`), e o que impede duas confirmações simultâneas é só o `synchronized` de um
  processo (`reservations/ReservationService.java:57`).
- **Os quatro relatórios trazem a tabela de reservas inteira** (`reservations/ReservationStore.java:108-121`) e
  filtram por mês ou noite em Java:
  - `reports/OccupancyReport.java:25`;
  - `reports/RevenueReport.java:25`;
  - `reports/TourismTaxReport.java:24`;
  - `reports/RatePlanReport.java:22`.

  A tabela cresce sem limite.
- **As colunas de situação e plano são `text` sem `CHECK`** (`V1__create_tables.sql:28, 33`).

## Comportamento

**Números:** 64 de 64 conferências passaram (`measures/behavior.md`):
- a parte comum do enunciado: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 09 (os relatórios): 13 de 13;
- as duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que o texto descreve funciona:
- **O preço:** a alta temporada e o arredondamento meio para o par (599,76 e 37,48/212,42).
- **As recusas,** com os códigos e status pedidos.
- **A reserva do começo ao fim:** a confirmação com o e-mail e o comprovante, o check-in, o check-out e a segunda via.
- **As devoluções** em cada plano.
- **Os quatro relatórios:**
  - todos começam com "Pousada Maré Alta,<nome>,<período>";
  - a taxa de turismo cobra R$ 4,50 por hóspede por noite;
  - o relatório por plano mostra os dois planos mesmo sem reserva e conta a não reembolsável cancelada;
  - um mês vazio dá total 0,00.
- **Os dados continuam depois de reiniciar.**
- **Nas 10 rodadas de reservas simultâneas,** só uma de cada par foi criada e a outra recebeu 409.

Nenhuma falha a explicar.

## Dúvidas

Nenhuma sobre as notas. Uma observação: o fato de o comportamento estar todo certo não muda a classe `omissão`. A
parte do cenário que o texto pede ("quando o jeito dos relatórios mudar, quero que mude em todos de uma vez") é sobre
a estrutura, e ela não existe.
