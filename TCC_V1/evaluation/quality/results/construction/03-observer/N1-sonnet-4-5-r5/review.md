# Revisão: construção, cenário 03

## Visão geral

O projeto separa a borda HTTP (`api.controller`, `api.dto`, `api.error`), as regras (`domain.model`, `domain.service`, `domain.event`, `domain.repository` com interfaces), os ouvintes dos avisos (`application.handler`) e a infraestrutura (`infrastructure.database` com JDBC puro, `infrastructure.messaging`, `infrastructure.notification` com o despachante de eventos). Tudo é montado em `Main.java:32-99`. Uma requisição passa pela rota do Javalin (`Main.java:74-84`), pelo controller, pelo `ReservationService` e pelos repositórios JDBC, e os dados ficam em três tabelas que `DatabaseInitializer` cria no PostgreSQL. A parte do cenário é um Observer bem feito: o serviço só publica `ReservationConfirmedEvent` e `ReservationCancelledEvent`, e cada interessado é um ouvinte registrado no `main`. O problema mais sério é o banco. A coluna `guest_cpf` é `VARCHAR(11)` (`DatabaseInitializer.java:34`), então criar uma reserva com o CPF escrito como o próprio texto mostra ("111.222.333-44") dá 500. Por isso o roteiro de comportamento passou só 18 de 66 conferências. Outros problemas: nada impede duas reservas do mesmo quarto ao mesmo tempo (reproduzi 10 em 10 com CPF só de dígitos), os valores em reais dos e-mails saem com espaço não separável ("R$ 599,76"), e os testes cobrem só o cálculo de preço (5,9% das linhas).

## Arquitetura

**Números:** 12 pacotes, 32 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). O Javalin aparece só em `com.marealta`, `api.controller` e `api.error`, e o JDBC só em `com.marealta` (o `DataSource` do `main`) e `infrastructure.database`. A maior classe é `JdbcReservationRepository` (271 linhas).
**Nota:** com problemas

O que está bom: as regras não conhecem o Javalin nem o SQL (`ReservationService.java`, `PricingService.java`, `ReportService.java` não importam nenhum dos dois). O SQL fica isolado nos `Jdbc*` de `infrastructure.database`. A montagem acontece num lugar só (`Main.java:34-63`, `Main.java:92-99`) e as dependências entram pelo construtor. Os defeitos são localizados:

- **O domínio depende da infraestrutura.** `ReservationService.java:11` importa `infrastructure.notification.EventDispatcher` (a dependência `domain.service -> infrastructure.notification` do jdeps). Os ouvintes de `application.handler` implementam `infrastructure.notification.EventHandler` (por exemplo, `FinanceNotificationHandler.java:7`). O contrato do evento devia morar no domínio ou na aplicação.
- **Regra na borda HTTP.** O controller vai direto ao repositório (`ReservationController.java:52`, `:84`) e decide ali que a segunda via só existe para reserva paga (`ReservationController.java:87-91`). O controller também guarda uma tabela própria de tipo por número de quarto (`ReservationController.java:124-131`), em vez de perguntar ao `RoomRepository`.
- **O DTO de resposta depende da infraestrutura.** `api.dto -> infrastructure.messaging` (`MessageResponse.java:3`), porque a entidade `Message` mora em `infrastructure.messaging`, e não no domínio.

## Design de baixo nível

**Números:** 5 interessados (3 na confirmação, 2 no cancelamento), todos ouvintes separados e registrados no `main` (`Main.java:93-98`); nenhum e-mail montado ou mandado fora da estrutura. Há 3 interfaces de dados com uma implementação só (`RoomRepository`, `ReservationRepository`, `MessageStore`) e 1 camada que só repassa (`EmailService`).
**Classe:** acerto
**Exagero:** exagero
**Nota:** com problemas

**O pattern.** `ReservationService` recebe o `EventDispatcher` de fora (`ReservationService.java:26`) e só publica eventos: `eventDispatcher.dispatch(new ReservationConfirmedEvent(updated))` (`ReservationService.java:90`) e `dispatch(new ReservationCancelledEvent(updated, previousStatus))` (`ReservationService.java:138`). O serviço não conhece nenhum destinatário. O despachante guarda listas de ouvintes por tipo de evento e percorre a lista (`EventDispatcher.java:13-25`). Cada interessado é uma classe com `EventHandler<T>`, inclusive o hóspede (`GuestNotificationHandler.java:14`), a governança (`HousekeepingNotificationHandler.java:13`, `HousekeepingCancellationHandler.java:12`) e o financeiro (`FinanceNotificationHandler.java:12`, `FinanceCancellationHandler.java:13`). O registro fica na montagem (`Main.java:92-99`). As condições de cada caso ficam no próprio ouvinte: "só se estava confirmada" (`HousekeepingCancellationHandler.java:24`, `FinanceCancellationHandler.java:26`) e "só se há algo a devolver" (`FinanceCancellationHandler.java:31`). Para pôr o restaurante ou o transfer basta uma classe nova e uma linha no `main`. É o acerto da referência. O defeito de formatação do dinheiro (abaixo) está nos ouvintes e não muda a classe.

**O exagero.** `RoomRepository`, `ReservationRepository` e `MessageStore` têm uma implementação JDBC cada uma, e nada as substitui: nenhum teste usa outra implementação. `EmailService.java:10-12` só repassa para `messageStore.save("EMAIL", ...)`. A interface marcadora `DomainEvent.java:3` não tem nenhum método. É pouco, mas é estrutura sem uso.

**O resto do código:**
- **Duplicação.** A tabela de tipo por número de quarto está copiada em `ReservationController.java:124-131` e em `GuestNotificationHandler.java:66-73`, embora `RoomType` e o banco já guardem essa informação. Os formatadores de data e de dinheiro se repetem em 5 ouvintes (`GuestNotificationHandler.java:15-17`, `FinanceNotificationHandler.java:13-14`, `FinanceCancellationHandler.java:14-15`, `HousekeepingNotificationHandler.java:16`, `HousekeepingCancellationHandler.java:13`).
- **Estado e segurança entre threads.** `NumberFormat` em `static final`, compartilhado entre as threads do Jetty (`GuestNotificationHandler.java:17`, `FinanceNotificationHandler.java:14`, `FinanceCancellationHandler.java:15`), e `NumberFormat` não é seguro entre threads. Ele também é a causa do "R$" seguido de espaço não separável (U+00A0) nos e-mails, em vez do "R$ 1.234,56" do texto. `Reservation` tem `setStatus`, `setAmountPaid` e `setRefund` públicos (`Reservation.java:103-121`). As transições de estado ficam espalhadas pelo serviço, e o modelo só responde `canX()`.
- **Erros.** `ErrorHandler.java:31-33` transforma qualquer exceção em 500 sem registrar a causa em lugar nenhum: o log do serviço (`measures/behavior-service.log`) não diz por que as criações falharam. Existem duas `InvalidRequestException` iguais (`ErrorHandler.java:37`, `ReservationService.java:155`). O id que não é número cai no formato padrão do Javalin (`{"id":[{"message":"TYPE_CONVERSION_FAILED"...}]}`), e não no `{error, message}`. `CapacityExceededException` guarda campos que nunca são lidos (`ReservationService.java:162-169`).
- **Tipos.** Dinheiro em `BigDecimal` com `HALF_EVEN` em cada passo (`PricingService.java:18-19`, `:34`, `:39-40`, `:46`), datas em `LocalDate`, situações e planos em `enum`: certo. Mas `BigDecimal.ZERO` sem escala (`PricingService.java:42`, `ReservationService.java:71`, `:130`) sai no JSON como `0`, e não `0.00`. O construtor de `Reservation` tem 16 argumentos e é repetido inteiro em `JdbcReservationRepository.java:50-67` só para trocar o id.

## Testes

**Números:** 11 testes rodaram, 0 falharam, 0 pulados; 11 métodos escritos, num arquivo só (`PricingServiceTest`). Cobertura: 47 de 794 linhas (5,9%) e 16 de 188 ramos (8,5%). Zero linhas cobertas em `api.*`, `application.handler`, `infrastructure.*` e `com.marealta` (`measures/tests.md`).
**Nota:** ruim

Os testes conferem com valores só o preço: a diária normal e a de alta temporada (`PricingServiceTest.java:25-64`), a virada de 14 para 15 de dezembro (`:46-50`, `:75-80`), o desconto do NON_REFUNDABLE (`:83-87`) e o total (`:97-101`). Mesmo aí o arredondamento "meio para o par" fica sem rede: 779,80 × 0,15 = 116,97 é exato (`:84-86`), então trocar `HALF_EVEN` por `HALF_UP` em `PricingService.java:40` não quebra nenhum teste. O caso que distingue (249,90 × 0,15 = 37,485 → 37,48) não está testado. A parte nova do cenário não tem nenhum teste. Apagar o ouvinte do financeiro no `main` (`Main.java:95`), tirar a condição "só se estava confirmada" (`HousekeepingCancellationHandler.java:24`) ou a de "nada a devolver" (`FinanceCancellationHandler.java:31`) não quebra nada. Também ficam sem teste: as transições de estado, a devolução no cancelamento, as recusas e os códigos de erro, os relatórios, o banco e a API. Um teste pela API com o CPF formatado teria pegado o 500 que derruba o sistema. A coluna está quase ausente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas, todas ok) e datas em `date`. As conferências acusaram: a chave estrangeira `reservations(room_number) -> rooms` sem índice; nenhum CHECK em `status`, `rate_plan`, `type` e `channel`; nenhuma restrição UNIQUE/EXCLUDE; 0 transações explícitas; 0 `FOR UPDATE`. O esquema é criado por `CREATE TABLE` no código, sem migração (`measures/database.md`).
**Nota:** ruim

- **O CPF não cabe.** `guest_cpf VARCHAR(11)` (`DatabaseInitializer.java:34`) recusa o CPF com pontos e traço, o formato que o próprio texto usa ("111.222.333-44"). O `INSERT` falha e a API devolve 500. Confirmei chamando a API: o mesmo corpo com "11122233344" cria a reserva. Esse é o motivo da quase totalidade das falhas do comportamento.
- **Dupla reserva.** A disponibilidade é conferida com um `SELECT COUNT(*)` (`JdbcReservationRepository.java:147-183`) e depois vem um `INSERT` separado (`ReservationService.java:47`, `:75`). Não há transação, trava nem restrição (`EXCLUDE` sobre `daterange`). Com CPF só de dígitos, mandei 10 pares de pedidos simultâneos para o quarto 202: as 10 rodadas criaram as duas reservas (201/201), e o banco ficou com 10 noites reservadas duas vezes. É dado errado. Com quarto em dobro, o relatório de ocupação também conta errado, porque usa `reservations.size()` e não os quartos distintos (`ReportService.java:47`).
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` (`DatabaseInitializer.java:19-59`) não sabe acrescentar uma coluna numa tabela que já existe.
- **Duas fontes de verdade para os quartos.** `rooms` guarda `capacity` e `nightly_rate` (`DatabaseInitializer.java:23-24`, `:70-75`), mas `JdbcRoomRepository.java:26` e `:45` leem só `number` e `type`, e a capacidade e a diária vêm do `enum` `RoomType`. Mudar a diária no banco não muda nada.
- **Conexões.** Não há pool: `PGSimpleDataSource` (`Main.java:112`) abre uma conexão nova a cada chamada. O script contou "pool: 1 arquivo", mas `PGSimpleDataSource` não faz pool. As conexões são devolvidas com try-with-resources.
- **Atomicidade.** A confirmação grava a reserva e só depois grava as mensagens, em conexões separadas, sem transação (`ReservationService.java:88-90`). Se um ouvinte falhar, a reserva fica confirmada sem os avisos e a API responde 500.
- **O que está certo.** Todo SQL usa parâmetros `?` (nenhum candidato de SQL montado com `+`), e os dados continuam lá depois de reiniciar.

## Comportamento

**Números:** 18 de 66 conferências passaram (48 falharam). Por parte: a parte comum, 14 de 46; os dados depois de reiniciar, 1 de 3; o cenário 03, 2 de 15; duas reservas ao mesmo tempo, 1 de 2 (`measures/behavior.md`).
**Nota:** ruim

Quase todas as falhas vêm de uma causa só: `POST /reservations` com o CPF "111.222.333-44" responde 500 INTERNAL_ERROR (o defeito do `VARCHAR(11)`, contado no banco). Sem reserva criada, falham a confirmação, o check-in, o check-out, a segunda via, os cancelamentos, os relatórios com dados e todo o cenário. Na prática, o site não consegue criar reserva com o CPF digitado como as pessoas costumam digitar. As conferências que passaram são as recusas que não chegam ao `INSERT` (capacidade, quarto inexistente, datas, corpo inválido), os cabeçalhos dos relatórios e as duas do cenário que só pedem silêncio. As duas do cenário passam sozinhas, porque nada é mandado mesmo.

Para saber o que há por trás, chamei a API com CPF só de dígitos. A lógica está certa: 599,76 no STANDARD de julho, desconto de 37,48 e total de 212,42 no NON_REFUNDABLE de uma noite, devolução de 299,88/0/0 conforme o plano e a situação, os cinco e-mails do cenário com os destinatários, os assuntos, as datas e o número de hóspedes certos, nada para o financeiro quando não há devolução, e os relatórios certos. Mesmo assim, três defeitos continuam:

- os valores em reais nos e-mails saem com espaço não separável ("R$ 599,76"). Quem procurar "Total: R$ 599,76" no texto do e-mail não acha, e o e-mail do hóspede não fica igual ao modelo do texto;
- os valores zero saem no JSON como `0`, e não `0.00` (`amountPaid`, `discount`, `refund`), contra "número com 2 casas";
- duas reservas simultâneas do mesmo quarto são criadas as duas (10 em 10 rodadas). O roteiro só não viu porque todas deram 500.

A falha "o 201 comporta 3" vem de outra escolha: o número do quarto sai como texto ("201") e o roteiro procura por número. O texto não fecha esse tipo, então essa falha não pesa.

## Dúvidas

- O texto não diz em que formato o site manda o CPF. O agente supôs 11 dígitos, como mostra `GuestNotificationHandler.java:83-88`, que formata só quando o tamanho é 11. Mesmo assim, o texto mostra o CPF formatado, e a resposta a um CPF formatado é um 500 sem mensagem, e não uma recusa 400 nem o dado guardado. Por isso contei como defeito do agente.
- O exagero é leve (três interfaces de repositório e um `EmailService` que só repassa). Outro revisor poderia aceitar as interfaces como inversão de dependência do domínio e dar `contenção`.
