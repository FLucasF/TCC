# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas), `hotel.reservations` (serviço, regras e o
acesso a dados `ReservationStore`), `hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports`. A montagem
fica em `hotel.Hotel`. Uma requisição entra pelo `HttpApi`, chama o `ReservationService`, que grava pelo
`ReservationStore` (JDBC com pool, esquema por Flyway). Os e-mails são linhas na tabela `messages`. Para atender o
pedido, o agente criou o pacote `hotel.notifications` com uma interface de ouvinte, `ReservationEventListener`, e dois
ouvintes, `HousekeepingNotifier` e `FinanceNotifier`. Eles são registrados em `Hotel.java:182-185` e o
`ReservationService` percorre a lista em `confirm` e em `cancel`. O comportamento pedido funciona: o roteiro deu 66 de
66. Há três defeitos. O e-mail do hóspede continuou como chamada direta dentro do serviço
(`ReservationService.java:72-76`), então o Observer ficou pela metade. A interface foi posta num pacote que forma um
ciclo novo com `hotel.reservations`. E cada ouvinte precisa repetir a regra "reserva pendente cancelada não avisa
ninguém", que chega a eles como um parâmetro `boolean`.

## Arquitetura

**Números:** 8 pacotes (eram 7 na base), 22 dependências entre eles (eram 17), 3 ciclos: `documents↔reservations`,
`reservations↔notifications` e `documents→reservations→notifications`. A base tinha 1, o `documents↔reservations`.
**Nota:** com problemas

O que já era bom na base continua igual. A borda HTTP não foi tocada. As regras não usam o Javalin. O JDBC fica em
`ReservationStore`, `Rooms` e `SentMessages`. A montagem continua num lugar só: os ouvintes são criados e passados ao
serviço em `Hotel.java:181-187`, e o serviço recebe a lista de fora (`ReservationService.java:27,32`), sem `new` por
dentro. Os ouvintes também não usam JDBC: só usam `EmailSender` e `TextFormat`.

O problema é o ciclo novo `hotel.reservations ↔ hotel.notifications`, que vem de onde o agente pôs a interface.
`ReservationEventListener` fica em `hotel.notifications` e depende de `hotel.reservations.Reservation`
(`notifications/ReservationEventListener.java:3`). Ao mesmo tempo, o serviço importa a interface
(`reservations/ReservationService.java:5`). A interface é o contrato que o serviço anuncia, então devia morar em
`hotel.reservations`, e só os ouvintes ficariam em `notifications`, dependendo de `reservations` num sentido só. É um
ciclo entre dois pacotes vizinhos e fácil de desfazer, mas é novo. O ciclo `documents↔reservations` veio da base. A
maior classe continua sendo `HttpApi` (170 linhas), da base. O `ReservationService` passou de 138 para 153 linhas.

## Design de baixo nível

**Números:** 3 interessados na confirmação: 2 na estrutura (governança e financeiro) e 1 fora dela (o hóspede, por
chamada direta). 2 interessados no cancelamento, os 2 na estrutura. 1 interface com 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do pattern.** Existe uma interface de ouvinte (`ReservationEventListener.java:6-11`) com uma
implementação por interessado (`HousekeepingNotifier`, `FinanceNotifier`). O serviço percorre uma lista recebida de
fora, tanto na confirmação (`ReservationService.java:77-79`) quanto no cancelamento (`ReservationService.java:100-102`).
A lista é montada em `Hotel.java:182-185`. Os textos da governança e do financeiro ficam inteiros nos ouvintes, e não
no serviço. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel`.

**O que afasta (e dá o malfeito).** O hóspede, que é o interessado que já existia, não virou ouvinte. O serviço ainda
monta o comprovante e manda o e-mail ele mesmo (`ReservationService.java:72-76`), com o texto em
`confirmationEmailBody` (`ReservationService.java:140-146`). Por isso continua dependendo de `EmailSender`
(`ReservationService.java:19,26`). A referência do cenário descreve exatamente esse caso como malfeito: "o hóspede
tem chamada direta e só a governança e o financeiro viraram ouvintes".

**Exagero.** Nenhum. A interface tem duas implementações e é o ponto de extensão que o pedido anuncia. Não há fábrica,
builder nem camada que só repassa.

**O resto do código.** Fora a parte do pattern, o que pesa é o contrato do cancelamento.
`onReservationCancelled(Reservation, boolean wasPreviouslyConfirmed)` (`ReservationEventListener.java:10`) passa a
cada ouvinte a decisão de ignorar o cancelamento de uma reserva pendente. A mesma guarda aparece em
`HousekeepingNotifier.java:31` e em `FinanceNotifier.java:30`, e o próximo ouvinte (o restaurante, o transfer)
precisa lembrar de repeti-la. Se esquecer, avisa sobre uma reserva que nunca foi paga. O pedido diz que uma reserva
pendente cancelada não avisa ninguém, então é uma regra do serviço: ele não devia anunciar esse caso, ou devia
anunciar um evento próprio, como "confirmada e depois cancelada". Também faltam dois cuidados:
- os ouvintes são chamados em sequência, sem nada que isole uma falha: se um lançar exceção, os seguintes não rodam e
  a requisição dá erro com a reserva já gravada (`ReservationService.java:71,77-79` e `99-102`);
- o comentário de `EmailSender` (`messages/EmailSender.java:3`) ainda diz que ele "manda e-mails para hóspedes",
  o que deixou de ser verdade.

O resto está bem feito: o dinheiro e as datas reaproveitam `TextFormat` (`FinanceNotifier.java:24,36`,
`HousekeepingNotifier.java:23-24`), em vez de formatar de novo. O reembolso é comparado com `compareTo`
(`FinanceNotifier.java:30`), e não com `equals`, o que estaria errado com `BigDecimal`. O que veio da base continua
igual (`BigDecimal`, `LocalDate`, `enum`, estado encapsulado em `Reservation`).

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de 94,4% das linhas (518 de 549) e 88,3% dos ramos (113 de 128). A base tinha 93,8% e 87,1%. O pacote
`hotel.notifications` ficou com 37 de 37 linhas e 6 de 6 ramos.
**Nota:** com problemas

O agente testou o que mudou, pelo serviço e contra o banco de verdade, em `NotificationsTest`, com 4 casos:
- a confirmação, com 3 mensagens, os destinatários e os assuntos exatos (`NotificationsTest.java:57-72`);
- o cancelamento de uma reserva confirmada, com 5 mensagens, os assuntos e as datas (`:84-95`);
- o cancelamento de uma reserva pendente, que não manda nada (`:105`);
- o cancelamento de uma `NON_REFUNDABLE`, que não avisa o financeiro (`:116-119`).

Nenhum teste da base foi apagado nem afrouxado. O `HttpApiTest` passou a esperar 3 mensagens em vez de 1
(`HttpApiTest.java:81`), o que acompanha a mudança. Os outros testes só ganharam a lista vazia de ouvintes no
construtor.

O problema está nas asserções do conteúdo, que são fracas em pontos que o pedido fixa:
- o valor pago e o valor do reembolso não são conferidos, só a presença de "R$ " (`NotificationsTest.java:73,95`);
- o número de hóspedes é conferido com `contains("2")` (`:68`), que passa por causa do "2027" das datas.

Pela pergunta da mutação, estes defeitos passariam:
- trocar `amountPaid()` por `price().subtotal()` em `FinanceNotifier.java:24`;
- trocar `refund()` por `amountKept()` em `FinanceNotifier.java:36` (o e-mail diria R$ 0,00);
- apagar a linha "Hóspedes" de `HousekeepingNotifier.java:25`;
- apagar `!wasPreviouslyConfirmed ||` em `FinanceNotifier.java:30`. Nesse caso nenhum teste falha, porque uma
  pendente tem reembolso zero e a outra guarda cobre.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)`,
datas em `date`, a chave estrangeira com índice. Há um `EXCLUDE USING gist` contra duas reservas no mesmo quarto e
nas mesmas noites. A única migração, `V1__create_tables.sql`, está igual à da base. As conferências só acusaram as
colunas de situação e de tipo em `text` sem `CHECK`, o que vem da base.
**Nota:** bom

O agente não mexeu no esquema nem no SQL, e o pedido não exigia: os avisos são linhas novas na tabela `messages`, pelo
mesmo `SentMessages.add` com parâmetros (`messages/SentMessages.java:20-35`). Os candidatos de SQL montado com `+`
em `ReservationStore.java:33,83,98,111` só juntam constantes, e vêm da base. Duas coisas também vieram da base e o
agente não piorou:
- não há transação entre gravar a reserva (`ReservationService.java:71,99`) e gravar as mensagens;
- as colunas `status` e `rate_plan` não têm `CHECK`.

O agente só acrescentou inserções que seguem o mesmo caminho.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 03: 15 de 15;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Na confirmação, o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201",
com 10/03/2027, 12/03/2027 e os 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80. No
cancelamento de uma reserva confirmada, a governança e o financeiro recebem os avisos com as datas e o valor
(R$ 779,80). A `NON_REFUNDABLE` cancelada avisa só a governança, e a pendente cancelada não avisa ninguém. Para o dono
da pousada, tudo o que ele pediu funciona pela API, e o que já existia continua igual.

## Dúvidas

Entre `malfeito` e `acerto` não há muita dúvida: a referência descreve como malfeito exatamente o caso do hóspede com
chamada direta. Fica em aberto se o parâmetro `wasPreviouslyConfirmed` deveria pesar na classe do pattern ou só na
nota do design. Contei só na nota.
