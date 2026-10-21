# Revisão: construção, cenário 07

## Visão geral

O agente montou um serviço Javalin em sete pacotes: `hotel` (montagem e banco), `hotel.web` (rotas), `hotel.reservations` (regras, preço e acesso às reservas), `hotel.rooms`, `hotel.messages`, `hotel.documents` (o comprovante) e `hotel.reports`. Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), que lê o corpo e chama `ReservationService`, e o serviço grava pelo `ReservationStore` com JDBC e parâmetros. Os dados ficam no PostgreSQL, num esquema criado por migrações do Flyway, com dinheiro em `numeric(10,2)` e uma restrição `EXCLUDE` que impede duas reservas do mesmo quarto na mesma noite. O comportamento está todo certo: 65 de 65 conferências passaram. O defeito mais sério é de design, na parte que o texto pedia "desde o primeiro dia". As três regras de envio (bloquear endereço inválido, desviar no modo de teste, copiar para o arquivo) ficaram como três `if` dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:27-38`), e a combinação por modo está escrita ali dentro. Para as próximas regras que o dono já anunciou (madrugada, limite por dia) e para a segunda pousada, que não quer a cópia, não há lugar para encaixar nada, então vai ser preciso mexer de novo nesse método. Os testes passam, mas quase não conferem essas regras novas.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). As maiores classes são `HttpApi` (170 linhas), `ReservationStore` (143) e `ReservationService` (138).
**Nota:** com problemas

- **Borda HTTP fina.** As rotas só leem a requisição, chamam o serviço e escrevem a resposta (`src/main/java/hotel/web/HttpApi.java:61-91`). A conversão de `ErrorCode` em status HTTP fica num `switch` só (`HttpApi.java:100-107`). Só `hotel.web` e `hotel` usam o Javalin (`measures/dependencies.md`).
- **Regras sem Javalin e sem SQL.** `ReservationService` não conhece o `Context` nem monta SQL (`src/main/java/hotel/reservations/ReservationService.java:27-119`). O SQL fica isolado em `ReservationStore`, `Rooms` e `SentMessages`.
- **Montagem num lugar só,** em `src/main/java/hotel/Hotel.java:20-30`. Um detalhe: a variável `HOTEL_TEST_MODE` é lida dentro do construtor de `Hotel` (`Hotel.java:26`) e não no `main` (`Hotel.java:42-46`), que já lê `PORT` e passa o ambiente ao `Database`. Por isso o teste da API que sobe um `Hotel` (`src/test/java/hotel/web/HttpApiTest.java:227`) herda o modo do ambiente de quem roda os testes.
- **O ciclo.** `ReservationService` cria e usa `ReservationReceipt` (`ReservationService.java:3`, `:64`, `:109`, `:118`), e `ReservationReceipt` conhece `Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`, `:24-38`). É um ciclo pequeno entre dois pacotes vizinhos, mas o comprovante, que é uma visão da reserva, está no pacote errado ou é montado pela camada errada.
- **Pacote misto.** `hotel.reservations` junta regras, entidade e acesso ao banco (13 classes, 508 linhas). Os relatórios dependem direto do `ReservationStore` (`src/main/java/hotel/reports/OccupancyReport.java:4`). Num sistema deste tamanho isso é aceitável.

A nota é `com problemas` por causa do ciclo.

## Design de baixo nível

**Números:** 3 regras de envio, 0 delas numa estrutura própria. As 3 estão em `if` dentro de `EmailSender.send`: o endereço em `EmailSender.java:28`, o modo de teste em `:31` e a cópia em `:35-36`. Não há interface de envio nem embrulho, e a combinação por modo é decidida dentro do método.
**Classe:** omissão
**Exagero:** contenção
**Nota:** bom

- **O pattern.** O texto descreve regras em volta do mesmo envio, que se combinam conforme o modo, e anuncia mais regras e outra combinação por instalação. O agente escreveu um método só, com três ramos fixos (`src/main/java/hotel/messages/EmailSender.java:27-38`):
  - o bloqueio retorna antes de tudo;
  - o modo de teste é um `boolean` no construtor (`:22-25`) e decide se há desvio;
  - a cópia é uma segunda chamada a `sentMessages.add` no ramo "normal".

  Não existe uma interface de envio, nem um embrulho por regra, nem uma lista de regras montada em `Hotel`. A regra "a cópia só fora do teste" está implícita no fato de o ramo do teste retornar antes (`:31-34`). Para "a segunda pousada não quer a cópia", ou para a regra da madrugada "só no modo de teste", é preciso acrescentar `boolean` e `if` nesse método. É exatamente o caso que a referência chama de omissão.
- **Exagero:** nenhum. Não há interfaces com uma implementação só, fábricas nem camadas que só repassam. Os records (`Price`, `Guest`, `NewReservation`, `Message`, `ReservationReceipt`) têm uso real.
- **O resto do código** é limpo e usa os tipos certos:
  - `BigDecimal` com `HALF_EVEN` em cada passo (`src/main/java/hotel/reservations/PriceCalculator.java:19-31`, `:50-52`);
  - `LocalDate` e `YearMonth`;
  - `enum` para status, plano, tipo e código de erro;
  - erros como exceção de domínio, com código (`HotelException`), traduzidos num lugar só.

  Pontos menores, que não mudam a nota:
  - `Reservation.changeStatus` aceita qualquer status (`src/main/java/hotel/reservations/Reservation.java:102-104`). As transições permitidas ficam espalhadas em quatro `if` no serviço (`ReservationService.java:59`, `:78-79`, `:93`, `:104`), e não na entidade.
  - `NewReservation` é reaproveitado para reconstruir a reserva lida do banco (`src/main/java/hotel/reservations/ReservationStore.java:125-131`, `Reservation.java:24-42`). O nome passa a enganar.
  - `EmailSender` tem um construtor que fixa o modo normal (`EmailSender.java:18-20`) e só os testes o usam.
  - `catch (Exception)` ao ler o corpo (`src/main/java/hotel/web/HttpApi.java:113`) descarta a causa, mas devolve o 400 certo.

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados. São 26 métodos escritos. Cobertura: 93,6% das linhas (485 de 518) e 85,8% dos ramos (103 de 120). Em `hotel.messages`, 42 de 48 linhas e 4 de 6 ramos.
**Nota:** com problemas

- **A parte comum está bem testada, com valores:**
  - preço com temporada e meio para o par (`src/test/java/hotel/reservations/PriceCalculatorTest.java:31-75`, incluindo 37,485 → 37,48 e 29/02 de ano bissexto);
  - devoluções por plano e o que fica com a pousada (`src/test/java/hotel/reservations/ReservationServiceTest.java:109-122`);
  - situações e recusas (`ReservationServiceTest.java:53-62`, `:99-161`);
  - os dois CSV comparados por inteiro (`src/test/java/hotel/reports/ReportsTest.java:133-191`);
  - a API ponta a ponta (`src/test/java/hotel/web/HttpApiTest.java:261-313`).

  Os testes usam o banco de verdade (`src/test/java/hotel/EmptyDatabase.java`), sem mocks.
- **A parte nova do cenário quase não tem rede.** Nenhum teste usa endereço inválido nem liga o modo de teste: os únicos e-mails dos testes são `ana@example.com`, `bruno@example.com` e `carla@example.com`. Por isso os ramos de `BLOCKED` e de `[TESTE]` de `EmailSender.java:28-34` não são executados (4 de 6 ramos no pacote). A cópia para o arquivo só aparece contada (`assertEquals(2, sent.size())` em `ReservationServiceTest.java:91` e `HttpApiTest.java:273`); o destinatário, o assunto "[Cópia] " e o texto dela nunca são conferidos.
- **A pergunta da mutação:**
  - trocar a expressão do endereço válido (`EmailSender.java:13`), ou apagar o bloqueio: nenhum teste falha;
  - apagar o desvio do modo de teste, ou trocar `"[TESTE] "` ou `"Destinatário original: "`: nenhum teste falha;
  - apagar a linha da cópia (`:36`): só aí falham os dois testes que contam mensagens;
  - trocar o prefixo `"[Cópia] "` ou o endereço do arquivo: nenhum teste falha.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. São 2 migrações do Flyway. As 6 colunas de dinheiro estão em `numeric(10,2)`, nenhuma data está em texto e a única chave estrangeira tem índice. As conferências acusaram só a falta de CHECK em `reservations.status`, `reservations.rate_plan`, `rooms.type` e `messages.channel`. São 0 transações explícitas e 0 `FOR UPDATE`.
**Nota:** com problemas

- **O que está bem:**
  - o esquema nasce e evolui por migrações versionadas (`src/main/resources/db/migration/V1__create_tables.sql`, `V2__message_status.sql`);
  - a reserva liga ao quarto por chave estrangeira (`V1__create_tables.sql:21`);
  - há CHECK de datas e de hóspedes (`:27`, `:36`);
  - a regra "um quarto, uma reserva por noite" é protegida pelo próprio banco, com `EXCLUDE USING gist (room_number WITH =, daterange(check_in, check_out) WITH &&) WHERE (status <> 'CANCELLED')` (`:38-40`). O código traduz a violação em `ROOM_UNAVAILABLE` (`src/main/java/hotel/reservations/ReservationStore.java:55-58`), então a regra vale mesmo com mais de um processo. O `synchronized` do serviço é só uma camada a mais;
  - todo SQL usa `?`. As concatenações que o script listou juntam só a constante `COLUMNS` (`ReservationStore.java:18-19`, `:98`, `:111`);
  - as conexões são devolvidas por try-with-resources, com pool Hikari (`src/main/java/hotel/Database.java:23-28`).
- **Os defeitos:**
  - Os dois relatórios trazem a tabela inteira de reservas para filtrar em Java (`src/main/java/hotel/reports/OccupancyReport.java:25`, `src/main/java/hotel/reports/RevenueReport.java:25`, via `ReservationStore.java:108-121`). O relatório de ocupação ainda faz uma consulta de quarto por linha (`OccupancyReport.java:36`). A tabela cresce sem limite.
  - `reservations.status` é texto sem CHECK (`V1__create_tables.sql:33`), e a restrição `EXCLUDE` depende dele valer exatamente `'CANCELLED'`.
  - A confirmação grava a reserva e depois a mensagem em conexões separadas, sem transação (`src/main/java/hotel/reservations/ReservationService.java:62-68`). Se a gravação da mensagem falhar, a reserva fica confirmada e paga, sem e-mail registrado, e o cliente recebe 500.

## Comportamento

**Números:** 65 de 65 conferências passaram, 0 falharam:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário no modo normal: 9 de 9;
- cenário no modo de teste: 5 de 5;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, o sistema faz o que ele pediu:
- os preços (R$ 599,76, desconto de R$ 37,48), as recusas com os códigos certos, a confirmação com o e-mail e o comprovante, as devoluções e os dois relatórios;
- as reservas e as mensagens continuam lá depois de reiniciar;
- nas dez rodadas de duas reservas simultâneas do mesmo quarto, nunca saíram duas;
- as regras de e-mail funcionam nos dois modos:
  - os quatro endereços inválidos ficam `BLOCKED`, com o endereço original e sem cópia;
  - a cópia "[Cópia] " vem logo depois da original;
  - com `HOTEL_TEST_MODE=true`, sai uma mensagem só, para `testes@pousadamarealta.com.br`, com "[TESTE] " e a linha "Destinatário original: ".

O problema de design não aparece hoje no comportamento. Ele vai aparecer no custo da próxima regra.

## Dúvidas

- Na nota do banco, `com problemas` pesa os relatórios que leem a tabela inteira, a falta de CHECK em `status` e a confirmação sem transação. A peça principal, a restrição `EXCLUDE` contra reserva dupla, é exemplar. Um revisor que tratasse a carga só como observação daria `bom`.
- Na nota do design, os pontos do resto do código são pequenos e localizados. Fiquei entre `bom` e `com problemas` e escolhi `bom`, porque a classe `omissão` já registra o defeito principal.
