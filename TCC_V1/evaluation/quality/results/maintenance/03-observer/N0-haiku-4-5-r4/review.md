# Revisão: manutenção, cenário 03

## Visão geral

O agente manteve a estrutura da base (pacotes `hotel` para a montagem, `hotel.web` com o Javalin, `hotel.reservations`
com o serviço e o acesso ao banco por JDBC, `hotel.messages` com o `EmailSender` que grava na tabela `messages`,
`hotel.documents`, `hotel.reports`, `hotel.rooms`; os dados no PostgreSQL, criados por uma migração do Flyway). Uma
requisição de confirmação passa por `HttpApi` → `ReservationService.confirm`, que agora, depois do e-mail do hóspede,
chama um novo `hotel.notifications.StaffNotifier` (`src/main/java/hotel/reservations/ReservationService.java:81`); no
cancelamento, o serviço chama o mesmo notificador se a reserva estava confirmada (`ReservationService.java:95`,
`:102-104`). O que o dono pediu funciona: o roteiro passa inteiro (66 de 66) e os 29 testes passam. O mais sério é o
design: o texto avisa que vai ter cada vez mais gente querendo ser avisada (restaurante, transfer, entrando e saindo
conforme os contratos), e o agente resolveu com uma classe só que conhece todos os destinos e é chamada direto pelo
serviço; incluir o restaurante vai exigir mexer no `StaffNotifier` e, para tirar alguém, mexer no código de novo.
Também surgiu um ciclo novo entre `hotel.reservations` e `hotel.notifications`.

## Arquitetura

**Números:** 8 pacotes (7 na base), 21 dependências entre eles (17 na base), 3 ciclos reportados pelo `tsort` (1 na
base): `documents ↔ reservations` (veio da base), `reservations ↔ notifications` (novo) e o triângulo
`documents → reservations → notifications → documents` (novo, consequência dos dois).
**Nota:** com problemas

A borda HTTP e o acesso ao banco não mudaram e continuam separados como na base (o Javalin só em `hotel.web` e em
`hotel`, o JDBC em `hotel.reservations`, `hotel.messages` e `hotel.rooms`, segundo `measures/dependencies.md`). O
pacote novo `hotel.notifications` não usa Javalin nem JDBC. Mas a dependência ficou no sentido errado nos dois
lados: o serviço importa o notificador (`src/main/java/hotel/reservations/ReservationService.java:5`) e o notificador
importa `Reservation` (`src/main/java/hotel/notifications/StaffNotifier.java:5`), o que cria o ciclo novo
`reservations ↔ notifications`. Se o serviço só anunciasse um evento por uma interface do próprio pacote, a seta seria
de `notifications` para `reservations` e não haveria ciclo. Além disso, a montagem saiu do lugar: a classe `Hotel`,
onde a base liga as peças, não foi tocada (`src/main/java/hotel/Hotel.java:24-25`), e quem cria o `StaffNotifier` é o
próprio serviço, num construtor que faz `new` por dentro (`ReservationService.java:21-24`). O ciclo
`documents ↔ reservations` já existia na base e não é do agente.

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro) e 2 no cancelamento; 0 deles atrás de uma
interface de ouvinte. O hóspede continua sendo chamado direto no serviço (`ReservationService.java:77-80`), a
governança e o financeiro ficam fixos em uma classe só (`StaffNotifier.java:18-28`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pedido descreve um Observer: o serviço anuncia "reserva confirmada" e "reserva cancelada", e cada interessado é um
ouvinte separado, registrado na montagem, para que o restaurante e o transfer entrem e saiam sem mexer no serviço. O
agente fez o que a referência chama de omissão: um "notificador" único que conhece todos os destinos e manda os
e-mails ele mesmo. `StaffNotifier.notifyReservationConfirmed` chama, em sequência, a governança e o financeiro
(`StaffNotifier.java:18-21`), e `notifyReservationCancelled` faz o mesmo, com a regra do reembolso dentro dele
(`StaffNotifier.java:23-28`); os endereços estão fixos no código (`StaffNotifier.java:37`, `:45`, `:54`, `:62`). Não há
interface nem lista de ouvintes: o serviço recebe um `StaffNotifier` concreto (`ReservationService.java:19`, `:31`), e o
e-mail do hóspede continua escrito e enviado à mão no serviço (`ReservationService.java:76-80`, `:142-148`). Incluir o
restaurante é editar o `StaffNotifier`, e tirar o transfer é editar de novo; nada disso é configurável na montagem.
O que aproxima um pouco: os textos dos avisos saíram do serviço e ficaram numa classe própria, e a decisão de avisar
só reservas que estavam confirmadas ficou explícita (`ReservationService.java:95`, `:102`).

Exagero: não há estrutura sem necessidade; o agente fez o mínimo (uma classe concreta, sem interfaces nem fábricas).

O resto do código que o agente escreveu tem defeitos localizados: dois imports sem uso (`StaffNotifier.java:6-7`,
`BigDecimal` e `LocalDate`); os endereços da governança e do financeiro repetidos em dois lugares cada
(`StaffNotifier.java:37` e `:54`; `:45` e `:62`) e o bloco de datas duplicado (`StaffNotifier.java:34-35` e `:52-53`);
o construtor de quatro argumentos cria a dependência por dentro (`ReservationService.java:23`), enquanto o de cinco
(`ReservationService.java:26-37`), o que permitiria a injeção, não é chamado por ninguém, nem pelos testes. O
dinheiro e as datas usam os tipos e a formatação da base (`TextFormat.money`/`date`, `src/main/java/hotel/documents/TextFormat.java:17-23`).

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (26 na base, +3). Cobertura de
linhas 94,3% (515 de 546; 93,8% na base) e de ramos 87,7% (107 de 122; 87,1% na base); `hotel.notifications` com 37 de
37 linhas e 2 de 2 ramos.
**Nota:** bom

A parte nova está conferida com valores. Na confirmação, o teste da base foi estendido para conferir o destinatário,
o assunto, as datas, o número de hóspedes e o valor pago do financeiro
(`src/test/java/hotel/reservations/ReservationServiceTest.java:85-110`). No cancelamento, há um teste para a reserva
STANDARD confirmada, com as datas da governança e o reembolso "R$ 249,90" (`ReservationServiceTest.java:177-196`), um
para a NON_REFUNDABLE, em que o financeiro não é avisado (`ReservationServiceTest.java:199-210`, só 4 mensagens), e
um para a pendente, que não avisa ninguém (`ReservationServiceTest.java:213-219`). A pergunta da mutação: apagar o
`if (wasConfirmed)` (`ReservationService.java:102`) derruba o teste da pendente; trocar `signum() > 0` por `>= 0`
(`StaffNotifier.java:25`) derruba o da NON_REFUNDABLE; trocar as datas de entrada e saída derruba os dois testes que
conferem as datas. O que fica com a rede mais frouxa: todos os testes usam 1 hóspede, então o "Hóspedes: 1"
(`ReservationServiceTest.java:104`) não distingue o número de hóspedes de um valor fixo; o teste pela API só confere os
destinatários (`src/test/java/hotel/web/HttpApiTest.java:81-84`). Os testes dependem da ordem dos e-mails
(`sent.get(1)`, `sent.get(2)`), que o texto diz não importar, o que os deixa frágeis a uma mudança de ordem, mas não
errados. Os testes da base continuam lá; os que mudaram (`assertEquals(1, ...)` para `3`,
`ReservationServiceTest.java:92` e `:118`; `HttpApiTest.java:81`) acompanham a regra nova, sem enfraquecer nada.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto; a migração `V1__create_tables.sql` igual à da base;
colunas de situação/tipo sem CHECK (`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`); nenhuma
transação explícita.
**Nota:** bom

O agente não tocou no banco: os avisos novos usam o mesmo `EmailSender`, que grava na tabela `messages` como a base já
fazia (`src/main/java/hotel/messages/EmailSender.java:12-14`), e nenhuma migração foi editada ou acrescentada
(`measures/database.md`). O esquema da base é sólido: a dupla reserva é barrada por um `EXCLUDE USING gist` sobre o
quarto e as datas (`src/main/resources/db/migration/V1__create_tables.sql`), e o SQL é parametrizado (os `+` de
`ReservationStore.java:33`, `:83`, `:98`, `:111` só juntam constantes). A falta de CHECK nas colunas de situação e a
ausência de transação entre a atualização da reserva e a gravação das mensagens vêm da base; o agente apenas passou de
uma para três gravações de mensagem depois de `store.update` (`ReservationService.java:75-81`), sem transação, como a
base fazia com o e-mail do hóspede.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46 de 46; dados depois de reiniciar 3 de 3; cenário 03 15 de
15; duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Tudo o que o dono pediu acontece pela API: na confirmação, o hóspede continua recebendo o mesmo e-mail, a governança
recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento
recebido - reserva 1" com R$ 779,80; no cancelamento de uma reserva confirmada, a governança e o financeiro
("Reembolso - reserva 1", R$ 779,80) são avisados, o financeiro fica de fora quando a reserva NON_REFUNDABLE não tem
nada a devolver, e a pendente cancelada não avisa ninguém. A parte que já funcionava continua igual, os dados ficam
depois de reiniciar e as reservas simultâneas não duplicam. Para o dono, hoje está certo; o problema aparece no
próximo pedido (o restaurante, o transfer), que vai exigir mexer no código, como a coluna de design explica.

## Dúvidas

O construtor de cinco argumentos de `ReservationService` (`ReservationService.java:26-37`) não é usado por ninguém;
contei como defeito do design do resto do código (nota), e não como exagero, porque não é uma estrutura nova, só um
ponto de injeção que ficou sem uso. De resto, nenhuma.
