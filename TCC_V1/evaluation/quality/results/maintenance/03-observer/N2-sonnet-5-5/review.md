# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (as regras
em `ReservationService` e o acesso a dados em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que
grava as mensagens na tabela `messages`), `hotel.documents` (o comprovante), `hotel.reports`. A montagem fica em
`hotel.Hotel`. O banco é PostgreSQL com uma migração do Flyway. Uma requisição de confirmação entra por `HttpApi`, vai a
`ReservationService.confirm`, que grava a reserva e agora avisa uma lista de ouvintes (`ReservationListener`) recebida
de fora. O agente fez uma mudança pequena e limpa: criou a interface `ReservationListener`
(`src/main/java/hotel/reservations/ReservationListener.java:6`), tirou o e-mail do hóspede do serviço e o pôs num
ouvinte (`GuestConfirmationEmail`), escreveu um ouvinte para a governança e outro para o financeiro num pacote novo,
`hotel.notifications`, e registrou os três em `Hotel.java:33-36`. Todos os testes passam (29 de 29) e as 66
conferências do roteiro também. Não achei defeito sério. O que há são pontos pequenos: a regra "reserva pendente
cancelada não avisa ninguém" está repetida em cada ouvinte, e um ouvinte que falhe interrompe os outros depois de a
reserva já estar gravada, defeito que já vinha da base.

## Arquitetura

**Números:** 8 pacotes (7 na base), 21 dependências entre eles (17 na base), um ciclo, `hotel.documents` ↔
`hotel.reservations`, que já existia na base. O pacote novo `hotel.notifications` depende de 4 pacotes e só é usado
por `hotel`. A dependência `hotel.reservations -> hotel.messages` da base sumiu.
**Nota:** bom

A borda HTTP não mudou. O serviço de regras deixou de conhecer o `EmailSender`: `ReservationService.java:15` guarda só
uma `List<ReservationListener>`, e a interface do ouvinte mora no pacote das regras
(`hotel/reservations/ReservationListener.java:6`). Assim, as dependências apontam de `notifications` para
`reservations`, e não ao contrário. Os destinos concretos e os endereços de e-mail ficam no pacote novo
(`hotel/notifications/*.java`), e a ligação fica toda na montagem (`Hotel.java:28-36`). O ciclo
`documents`↔`reservations` vem da base (`ReservationService.java:3` usa `ReservationReceipt` no check-out e na segunda
via, e o comprovante usa `Reservation`), e o agente não o piorou. Nenhuma classe cresceu: `ReservationService` caiu de
138 para 129 linhas. Detalhe menor: em `Hotel.java:3-7` os imports de `notifications` ficaram no meio dos de
`messages`.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), os 3 como ouvintes da mesma interface. Os 2 eventos
(confirmação e cancelamento) passam pela lista. Nenhum e-mail sai mais de dentro do serviço.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Observer. O serviço anuncia o evento e não sabe quem é avisado: em `ReservationService.java:64`
(`listeners.forEach(l -> l.onConfirmed(...))`) e em `ReservationService.java:86` (`onCancelled`, com o status
anterior). A lista vem de fora pelo construtor e é copiada para ficar imutável (`ReservationService.java:23`). Cada
interessado é uma classe separada:

- `GuestConfirmationEmail.java:21`: o e-mail do hóspede, que já existia, passou para cá com o texto idêntico ao da base;
- `HousekeepingEmails.java:22` e `:32`: a governança;
- `FinanceEmails.java:22` e `:30`: o financeiro.

Os três são registrados na montagem (`Hotel.java:33-36`). A interface tem métodos `default` vazios
(`ReservationListener.java:9-14`), então um ouvinte novo, como o do restaurante, implementa só o evento que interessa a
ele. Os endereços entram pelo construtor, sem estar fixos dentro da classe. Nada aproxima o resultado de "malfeito":
o serviço não monta nenhum e-mail, o hóspede não tem chamada direta e o cancelamento também passa pelos ouvintes.

Exagero: não há. A interface tem três implementações, e não foi criada fábrica, registro dinâmico nem barramento de
eventos.

Resto do código, pontos pequenos que não mudam a nota:

- O filtro "só avisa se a reserva estava confirmada" aparece duas vezes, em `HousekeepingEmails.java:33` e
  `FinanceEmails.java:31`. Todo ouvinte novo de cancelamento terá de lembrar dele. É uma escolha defensável (o evento
  leva o status anterior e cada ouvinte decide), mas é uma regra repetida.
- Os ouvintes são chamados em sequência, sem isolar as falhas (`ReservationService.java:64,86`). Se um falhar, os
  seguintes não rodam e a requisição responde com erro, mas a reserva já foi gravada (`:62`, `:84`). A base já fazia
  isso com o e-mail do hóspede; o agente só manteve o mesmo jeito.
- Os tipos estão certos: dinheiro em `BigDecimal` formatado por `TextFormat.money`, datas por `TextFormat.date`
  (reaproveitando o formato do comprovante, como o texto pede) e o status como `enum` (`ReservationStatus`).

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados, 29 métodos escritos (a base tinha 26). Cobertura de
linhas de 94,1% (513/545; a base tinha 93,8%) e de ramos de 87,7% (107/122; a base tinha 87,1%). O pacote
`hotel.notifications` tem 40/40 linhas e 6/6 ramos cobertos.
**Nota:** bom

O agente escreveu 3 testes novos em `ReservationServiceTest`, contra o banco de verdade:

- `confirmingTellsHousekeepingAndFinance` (`ReservationServiceTest.java:106`): confere o destinatário e o assunto com o
  número do quarto e da reserva, as duas datas e "Hóspedes: 2";
- `cancellingAConfirmedReservationTellsHousekeepingAndFinance` (`:122`): confere os assuntos de cancelamento e de
  reembolso e o valor "R$ 779,80";
- `cancellingWithoutRefundOrBeforeConfirmationSkipsFinanceOrEveryone` (`:136`): a pendente cancelada não manda nada
  (`:139`), e a NON_REFUNDABLE cancelada avisa só a governança (`:145`).

Os testes da base que contavam mensagens foram ajustados de 1 para 3 (`:97`, `:155`, `HttpApiTest`), o que é a mudança
pedida e não um enfraquecimento. Nenhum teste foi apagado. Em `ReportsTest` o serviço passou a receber `List.of()`, sem
efeito nos relatórios.

A pergunta da mutação:

- Trocar `<= 0` por `< 0` em `FinanceEmails.java:31` seria pego pelo `:145`.
- Apagar o filtro de status em `HousekeepingEmails.java:33` seria pego pelo `:139`.
- Ficam sem rede três pontos:
  - o valor do e-mail de pagamento: `:118` só confere `"R$ "`, e trocar `amountPaid()` por outro valor passaria;
  - a data de saída no aviso de cancelamento: só a de entrada é conferida, em `:130`;
  - o destinatário dos e-mails de cancelamento: o `to` não é conferido.

Remover o filtro de status do `FinanceEmails` também passaria, mas ali o comportamento não muda, porque a pendente tem
0 a devolver. As lacunas são pequenas: o roteiro de comportamento confere esses valores, e elas não tiram a nota.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 colunas). Nenhuma chave estrangeira
sem índice e nenhuma data em texto. Há um `EXCLUDE USING gist` contra duas reservas na mesma noite. A conferência só
acusou as colunas `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK, que vêm da base. A migração
`V1__create_tables.sql` está igual à da base, e o agente não acrescentou nenhuma.
**Nota:** bom

A mudança não precisava mexer no esquema, e o agente não mexeu: os e-mails novos vão para a mesma tabela `messages`
pelo `EmailSender` que já existia (`hotel/messages/EmailSender.java:12-13`). O SQL com `+` que o script lista
(`ReservationStore.java:33,83,98,111`) só concatena constantes, e vem da base. Tudo que é herdado da base:

- as colunas de situação sem CHECK;
- nenhuma transação explícita, de modo que a gravação da reserva e as das mensagens não andam juntas;
- a carga da tabela inteira nos relatórios.

Com a mudança, a confirmação passa a fazer quatro gravações seguidas sem transação em vez de duas. É o mesmo padrão da
base, e por isso não conta como defeito do agente.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46/46, depois de reiniciar 3/3, cenário 03 15/15, reservas
simultâneas 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API. O hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva
confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva
1" com R$ 779,80. No cancelamento de uma reserva confirmada, a governança recebe as datas liberadas e o financeiro
recebe o reembolso de R$ 779,80. Se o plano é NON_REFUNDABLE, só a governança é avisada, e uma reserva pendente
cancelada não avisa ninguém. A parte comum, os dados depois de reiniciar e as reservas simultâneas continuam como na
base.

## Dúvidas

- A regra "uma reserva pendente cancelada não avisa ninguém" ficou em cada ouvinte, e não no serviço. Pesei isso como
  ponto menor e dei `bom` no design. Um revisor mais rígido poderia dar `com problemas` por essa repetição, somada à
  falta de isolamento entre os ouvintes (este último vem da base).
