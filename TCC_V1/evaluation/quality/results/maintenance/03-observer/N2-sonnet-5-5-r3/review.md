# Revisão: manutenção, cenário 03

## Visão geral

O projeto é o da base, em oito pacotes: `hotel.web` (o Javalin, `HttpApi`) recebe a requisição e chama
`hotel.reservations.ReservationService`, que aplica as regras e grava pelo `ReservationStore` (JDBC, PostgreSQL com
Flyway); as mensagens ficam na tabela `messages` pelo `SentMessages`, e a montagem é a classe `hotel.Hotel`. A mudança
do agente foi pequena e bem localizada (`changes.patch`): criou a interface `ReservationListener`
(`src/main/java/hotel/reservations/ReservationListener.java:9` e `:13`), um pacote novo `hotel.notifications` com um
ouvinte por interessado (hóspede, governança, financeiro), tirou o e-mail do hóspede de dentro do serviço e passou o
serviço a percorrer a lista de ouvintes que recebe na montagem (`src/main/java/hotel/reservations/ReservationService.java:64`
e `:86`, `src/main/java/hotel/Hotel.java:33-36`). Os 30 testes passam e as 66 conferências do roteiro passam. Não há
defeito sério; o que há são detalhes: o filtro "só se estava confirmada" repetido em cada ouvinte e o README que não
fala dos avisos novos.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 21 dependências entre eles (a base tinha 17), 1 ciclo
(`hotel.documents` ↔ `hotel.reservations`), o mesmo que a base já tinha.
**Nota:** bom

O pacote novo `hotel.notifications` depende de `reservations`, `rooms`, `messages` e `documents` e só é usado pela
montagem (`hotel`). Com a mudança, `hotel.reservations` deixou de depender de `hotel.messages` (estava em
`measures/dependencies-base.md` e não está mais em `measures/dependencies.md`): agora o serviço só conhece a interface
`ReservationListener`, que mora no próprio pacote dele, e a dependência aponta de `notifications` para `reservations`.
Os ouvintes são criados e ligados num lugar só, `src/main/java/hotel/Hotel.java:28-36`. O ciclo
`documents` ↔ `reservations` vem da base: `ReservationService` ainda usa `ReservationReceipt` no check-out e na segunda
via (`src/main/java/hotel/reservations/ReservationService.java:3`, `:108`, `:117`), e `ReservationReceipt` lê
`Reservation`. O agente não o piorou. A borda HTTP (`HttpApi`, 170 linhas) não mudou, e o serviço ficou menor (138 → 129
linhas).

## Design de baixo nível

**Números:** 3 de 3 interessados são ouvintes (hóspede, governança, financeiro), incluindo o que já existia na base (o
hóspede); 2 eventos (confirmação e cancelamento) passam pela lista; nenhum e-mail montado no serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: `ReservationService` anuncia a confirmação e o cancelamento a uma lista de `ReservationListener` recebida no
construtor (`src/main/java/hotel/reservations/ReservationService.java:19-23`, `:64`, `:86`) e não sabe quem é avisado.
Cada interessado é uma classe separada: `GuestConfirmationEmail` (`src/main/java/hotel/notifications/GuestConfirmationEmail.java:21`),
com o mesmo assunto e o mesmo corpo que a base montava em `confirmationEmailBody`; `HousekeepingEmail`
(`src/main/java/hotel/notifications/HousekeepingEmail.java:23`, `:33`); e `FinanceEmail`
(`src/main/java/hotel/notifications/FinanceEmail.java:23`, `:31`). A lista é montada em `Hotel`
(`src/main/java/hotel/Hotel.java:33-36`), então um restaurante ou um transfer entra como mais uma classe e mais uma linha
na montagem, sem mexer no serviço. Os métodos `default` vazios da interface deixam cada ouvinte implementar só o evento
que lhe interessa (o hóspede só a confirmação). Os endereços são passados no construtor, e não fixados nas classes.

Exagero: nenhum. A interface tem três implementações, e nada foi criado além do que o pattern pede (nenhuma fábrica,
nenhum barramento de eventos, nenhuma camada que só repassa).

O resto do código: o serviço anuncia o cancelamento também quando a reserva estava pendente, e cada ouvinte filtra
isso por conta própria (`src/main/java/hotel/notifications/HousekeepingEmail.java:34`,
`src/main/java/hotel/notifications/FinanceEmail.java:32`). Funciona e deixa o evento disponível para quem quiser saber
de qualquer cancelamento, mas cada novo ouvinte vai precisar lembrar da mesma condição. Os tipos estão certos
(`BigDecimal` com `TextFormat.money`, `LocalDate` com `TextFormat.date`, o `enum ReservationStatus`), e a lista é
copiada como imutável (`ReservationService.java:23`). A troca do plano por `switch` no cancelamento
(`ReservationService.java:79-82`) já vinha da base. Detalhes pequenos: uma linha de mais de 150 colunas em
`src/test/java/hotel/reports/ReportsTest.java:30`, e o README não foi atualizado (sem diferença em relação à base)
para dizer que a governança e o financeiro passam a ser avisados.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26); cobertura de
linhas 94,1% (513 de 545; a base tinha 93,8%) e de ramos 87,7% (107 de 122; a base tinha 87,1%); `hotel.notifications`
com 40 de 40 linhas e 6 de 6 ramos.
**Nota:** bom

O agente escreveu `src/test/java/hotel/notifications/StaffNotificationsTest.java`, com 4 testes contra o banco
real que conferem a parte nova com valores: assunto e corpo exatos da governança e do financeiro na confirmação
(`:66-70`, incluindo "Valor pago: R$ 779,80"), assunto e corpo no cancelamento de uma reserva confirmada (`:81-86`), o
financeiro sem aviso quando não há o que devolver (`financeIsNotToldWhenThereIsNothingToRefund`) e nenhum e-mail
quando se cancela uma reserva pendente (`cancellingAPendingReservationTellsNobody`). A pergunta da mutação: tirar o
`signum() == 0` de `FinanceEmail.java:32` faz um teste falhar; tirar o filtro de `HousekeepingEmail.java:34` também
(no teste da reserva pendente); trocar o assunto ou o formato do valor também. Tirar só o `previousStatus` de
`FinanceEmail.java:32` não seria percebido, mas nesse caso o comportamento não muda, porque uma reserva pendente não tem
nada a devolver. Os testes da base continuam todos lá, com as mesmas asserções. Mudou só a montagem do serviço
(`ReservationServiceTest.java:27`, `ReportsTest.java:30`) e a contagem de mensagens na API, de 1 para 3
(`src/test/java/hotel/web/HttpApiTest.java:81`), que é o comportamento novo pedido. O e-mail do hóspede continua
conferido pelo teste da base (`ReservationServiceTest.java:95-97`). O que ficou sem conferir é pouco: no cancelamento
NON_REFUNDABLE, o aviso da governança é só contado (`assertEquals(2, ...)`), sem ver o conteúdo.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto; a migração `V1__create_tables.sql` igual à da base, sem
migração nova; as conferências só acusaram as colunas de situação e de tipo sem CHECK (`status`, `rate_plan`,
`type`, `channel`), que já vinham da base.
**Nota:** bom

A mudança não precisava de esquema novo (os avisos vão para a tabela `messages` que já existia), e o agente não mexeu
nas migrações nem no SQL. A proteção contra duas reservas na mesma noite é o `EXCLUDE USING gist` da base, e o SQL é
todo parametrizado (os `+` de `ReservationStore.java:33`, `:83`, `:98`, `:111` só juntam constantes). Um ponto que
cresceu com a mudança, embora o modelo seja da base: a confirmação grava a reserva
(`ReservationService.java:62`) e depois três mensagens em conexões separadas, sem transação
(`src/main/java/hotel/messages/SentMessages.java:39-53`). Se uma das gravações de mensagem falhar, a reserva fica
confirmada e os avisos seguintes não saem. A base já fazia assim com um e-mail só.

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário
03 15 de 15, as reservas simultâneas 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada, isto quer dizer o seguinte. O hóspede continua recebendo o mesmo e-mail de
confirmação. A governança recebe "Reserva confirmada - quarto 201", com a entrada, a saída e o número de hóspedes. O
financeiro recebe "Pagamento recebido - reserva N", com o valor no formato do comprovante (R$ 779,80). No cancelamento
de uma reserva confirmada, a governança recebe as datas liberadas, e o financeiro recebe o reembolso só quando há o que
devolver. Uma reserva pendente cancelada não avisa ninguém. Tudo o que já funcionava continua igual, inclusive depois
de reiniciar e com dois pedidos simultâneos para o mesmo quarto.

## Dúvidas

A nota do design ficou em `bom` apesar do filtro "só se estava confirmada" repetido em dois ouvintes
(`HousekeepingEmail.java:34`, `FinanceEmail.java:32`). Um revisor poderia preferir que o serviço anunciasse um evento
próprio, "uma reserva confirmada foi cancelada", e não pediria necessariamente essa mudança antes de aceitar. Por isso
a nota não foi para `com problemas`.
