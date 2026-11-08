# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras em
`ReservationService`, SQL em `ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender` grava na tabela
`messages`), `hotel.documents` e `hotel.reports`. A classe `hotel.Hotel` monta tudo. Uma requisição entra pela
`HttpApi`, passa pelo `ReservationService` e chega ao PostgreSQL pelas classes de acesso a dados, com o esquema criado
pelo Flyway. O agente acrescentou o pacote `hotel.notifications`: uma interface `ReservationEventListener` e dois
ouvintes, `HousekeepingNotifier` e `FinanceNotifier`. O `ReservationService` recebe a lista de ouvintes na montagem
(`Hotel.java:29-33`) e a percorre na confirmação e no cancelamento de uma reserva confirmada. O comportamento está
todo certo (66 de 66 conferências) e os testes passam. Há três problemas. O e-mail do hóspede continua sendo mandado
direto pelo serviço (`ReservationService.java:74-77`) em vez de virar mais um ouvinte, e é isso que deixa o pattern
malfeito. A interface ficou num pacote que depende de `reservations`, o que cria um ciclo novo entre pacotes. E os
ouvintes repetem por conta própria condições que o serviço já decidiu.

## Arquitetura

**Números:** 8 pacotes (7 na base) e 23 dependências entre eles (17 na base). A base tinha um ciclo,
`documents ↔ reservations`. A final tem três: o da base, um novo `reservations ↔ notifications` e o ciclo de três
`documents → reservations → notifications → documents` (`measures/dependencies.md`). Nenhum pacote novo usa Javalin
nem JDBC. A maior classe continua sendo a `HttpApi`, com 170 linhas; o `ReservationService` passou de 138 para 157.
**Nota:** com problemas

As camadas da base continuam como estavam: o agente não mexeu na `HttpApi`, as regras continuam sem Javalin e sem
SQL, e a montagem continua num lugar só, porque os ouvintes são criados e registrados em `Hotel.java:28-33`. Isso está
certo. O defeito é o lugar da interface. `ReservationEventListener` mora em `hotel.notifications`
(`notifications/ReservationEventListener.java:1`), e o serviço a importa
(`reservations/ReservationService.java:5`). Ao mesmo tempo, os ouvintes desse mesmo pacote importam `Reservation` e
`ReservationStatus` de `hotel.reservations` (`notifications/FinanceNotifier.java:5-6`,
`notifications/HousekeepingNotifier.java:5-6`). O resultado é um ciclo novo entre dois pacotes vizinhos. Com a
interface em `hotel.reservations`, que é quem anuncia o evento, a dependência iria num sentido só: os ouvintes
dependeriam das reservas, e as reservas não saberiam que eles existem. O ciclo `documents ↔ reservations` já vinha da
base e não é do agente.

## Design de baixo nível

**Números:** há 3 interessados na confirmação (o hóspede, a governança e o financeiro): 2 estão na estrutura de
ouvintes e 1, o hóspede, ficou fora, numa chamada direta. Os 2 interessados no cancelamento estão na estrutura. A
interface de ouvinte tem 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Observer: há uma interface de ouvinte com um evento por acontecimento
(`notifications/ReservationEventListener.java:7-13`) e uma implementação por interessado
(`HousekeepingNotifier`, `FinanceNotifier`). O serviço recebe a lista de fora (`ReservationService.java:27,32`) e a
percorre sem saber quem está nela (`ReservationService.java:78-80` e `103-105`). O registro é feito na montagem
(`Hotel.java:29-31`). Para pôr o restaurante ou o transfer basta escrever outro ouvinte e acrescentá-lo nessa lista.
O cancelamento também passa pela estrutura.

O que afasta: o hóspede, que era o único interessado da base, continua com chamada direta. O serviço ainda monta o
e-mail dele (`ReservationService.java:73-77`, com o texto em `confirmationEmailBody`, `ReservationService.java:144-150`)
e, por isso, ainda depende do `EmailSender` (`ReservationService.java:19,26`). A referência do pattern dá esse caso
como malfeito, com todas as letras: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".

Exagero: nenhum. A interface tem duas implementações e foi feita para receber mais, e não há fábrica, builder nem
camada que só repassa.

O resto do código: o que o agente escreveu é curto e legível, usa o `TextFormat` da base para dinheiro e datas
(`HousekeepingNotifier.java:26-27`, `FinanceNotifier.java:26`) e guarda os endereços em constantes. O defeito está nos
ouvintes, que voltam a perguntar o que o serviço já decidiu. O serviço só chama `onReservationCancelled` quando a
reserva estava confirmada (`ReservationService.java:94,101`), mas `HousekeepingNotifier.java:33` confere de novo
`status() == CANCELLED` e, como sinal de "estava confirmada", usa `amountPaid().signum() > 0`. Fica a mesma regra
escrita de duas maneiras: se um dia houver reserva confirmada com valor zero (uma cortesia, por exemplo), a governança
deixa de ser avisada sem que ninguém perceba. `FinanceNotifier.java:32-33` também repete o teste de status e o de
`refund() != null`, que nunca falham nesse ponto. Esses desvios são o que deixa 4 dos 10 ramos do pacote sem cobertura
(`measures/tests.md`). O defeito é localizado e não espalhado.

## Testes

**Números:** final: 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos. Cobertura de 94,4% das
linhas (521/552) e 85,8% dos ramos (115/134). `hotel.notifications`: 37 de 37 linhas, 6 de 10 ramos. Base: 26 testes,
93,8% das linhas e 87,1% dos ramos.
**Nota:** bom

O agente manteve os 26 testes da base passando. Mudou apenas a contagem de mensagens de 1 para 3 onde a confirmação
passou a mandar três e-mails (`ReservationServiceTest.java:101,123`, `HttpApiTest.java:81`), sem apagar nenhuma
conferência antiga: as do e-mail do hóspede continuam lá (`ReservationServiceTest.java:102-106`). A parte nova foi
testada com valores:

- na confirmação, o destinatário, o assunto, as duas datas, o número de hóspedes e o valor pago, para a governança e
  para o financeiro (`ReservationServiceTest.java:107-114`);
- no cancelamento de uma reserva STANDARD confirmada, o assunto, as datas e o reembolso de R$ 249,90
  (`ReservationServiceTest.java:143-158`);
- na reserva pendente cancelada, nenhuma mensagem (`ReservationServiceTest.java:162-168`);
- na NON_REFUNDABLE cancelada, só a governança é avisada (`ReservationServiceTest.java:171-182`, que conta as
  mensagens).

A pergunta da mutação: tirar a condição do reembolso no `FinanceNotifier`, trocar `> 0` por `>= 0` ou apagar o `if
(wasConfirmed)` do serviço derruba algum teste, porque os testes contam as mensagens. Os ramos sem rede são só os
desvios redundantes dos ouvintes (os 4 ramos não cobertos). Há duas fraquezas pequenas: as asserções dependem da
ordem dos e-mails (`sent.get(1)`, `sent.get(2)`), que o dono disse não importar, e nenhum teste exercita um ouvinte
isolado. Nenhuma das duas deixa uma regra pedida sem conferência.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)`,
nenhuma chave estrangeira sem índice, nenhuma data em texto. Existe um `EXCLUDE USING gist` que impede duas reservas
do mesmo quarto em noites que se sobrepõem. A conferência acusou só as colunas de situação e de tipo sem `CHECK`
(`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`). A única migração,
`V1__create_tables.sql`, está igual à da base (`measures/database.md`).
**Nota:** bom

A mudança não precisava de banco, e o agente não mexeu nele: os novos e-mails vão para a tabela `messages` pelo mesmo
`EmailSender` da base (`messages/EmailSender.java:12-14`). Não há migração nova nem editada, nem SQL novo. A falta de
`CHECK` nas colunas de situação já vinha da base. Também vem da base o fato de a atualização da reserva e a gravação
das mensagens não ficarem numa transação (`ReservationService.java:71-80`, nenhum `setAutoCommit(false)`); o agente
só acrescentou mais gravações depois do `store.update`. O SQL montado com `+` em `ReservationStore.java:33,83,98,111`
junta apenas constantes, como `COLUMNS`, e também vem da base.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 03:
15 de 15. Reservas simultâneas: 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Tudo o que o dono pediu acontece pela API. Na confirmação, o hóspede continua recebendo o e-mail de sempre. A
governança recebe "Reserva confirmada - quarto 201" com a entrada, a saída e o número de hóspedes, e o financeiro
recebe "Pagamento recebido - reserva 1" com R$ 779,80. No cancelamento de uma reserva confirmada, a governança recebe
"Reserva cancelada - quarto 201" com as datas, e o financeiro recebe "Reembolso - reserva 1" com o valor. Na
NON_REFUNDABLE, só a governança é avisada, e a reserva pendente cancelada não avisa ninguém. A parte comum continua
igual à da base, os dados ficam depois de reiniciar e não se cria reserva dupla em pedidos simultâneos.

## Dúvidas

- O `amountPaid().signum() > 0` de `HousekeepingNotifier.java:33` dá certo hoje, porque toda reserva confirmada tem
  total positivo (o banco exige `nightly_rate > 0`). Por isso, contei-o como defeito de design e não de comportamento.
- A nota de design ficou entre `bom` e `com problemas`, já que os defeitos do código novo são pequenos e localizados.
  Fiquei com `com problemas` porque a condição duplicada no ouvinte é uma regra escrita em dois lugares, e um revisor
  exigente pediria para tirá-la.
