# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em camadas: `hotel.web.HttpApi` recebe a requisição e chama `hotel.reservations.ReservationService`, que aplica as regras e grava por `ReservationStore` (JDBC, PostgreSQL com migração Flyway `V1`). Os e-mails vão para a tabela `messages` por `hotel.messages.EmailSender`. A montagem fica em `hotel.Hotel`. O agente mexeu em pouca coisa. Criou a interface `ReservationListener` (`src/main/java/hotel/reservations/ReservationListener.java:10-26`), com os eventos `Confirmed` e `Cancelled`, e um pacote novo, `hotel.notifications`, com `GovernanceNotifier`, `FinanceNotifier` e `StaffNotifications.all`. O serviço percorre a lista de ouvintes na confirmação e no cancelamento (`ReservationService.java:84-85`, `:106-107`, `:142-158`). Pela API, o sistema faz tudo o que foi pedido: 66 de 66 conferências passaram. A estrutura, porém, ficou pela metade, por dois motivos. O e-mail do hóspede continua sendo uma chamada direta dentro do serviço (`ReservationService.java:80-83`). E a lista de ouvintes não é montada em `Hotel`: o próprio serviço a cria pelo construtor de 4 argumentos (`ReservationService.java:24-27`). Isso abriu um ciclo novo entre `hotel.reservations` e `hotel.notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 22 dependências entre pacotes (a base tinha 17), 3 ciclos pelo `tsort` (a base tinha 1). O ciclo `documents ↔ reservations` já vinha da base. São novos `reservations ↔ notifications` e o ciclo de três, `documents → reservations → notifications`.
**Nota:** com problemas

- As camadas da base continuam inteiras. O pacote novo `hotel.notifications` não usa Javalin nem JDBC (tabela de bibliotecas em `measures/dependencies.md`). Os ouvintes dependem só de `EmailSender`, `TextFormat` e dos eventos.
- O ciclo novo é do agente. `ReservationService` importa `hotel.notifications.StaffNotifications` (`src/main/java/hotel/reservations/ReservationService.java:5`) e o chama no construtor (`:26`). Já `hotel.notifications` depende de `hotel.reservations.ReservationListener` (`src/main/java/hotel/notifications/GovernanceNotifier.java:7`, `FinanceNotifier.java:6`). As regras passaram a conhecer quem é avisado, e o lugar dessa decisão seria a montagem.
- A montagem ficou partida. `Hotel` continua chamando o construtor antigo (`src/main/java/hotel/Hotel.java:24-25`, sem mudança em relação à base), e é o serviço quem cria os seus ouvintes por dentro. O construtor que recebe a lista de fora existe (`ReservationService.java:29-40`), mas só os testes o usam. Para tirar o restaurante ou o transfer, alguém teria de editar `StaffNotifications.all` (`src/main/java/hotel/notifications/StaffNotifications.java:13-15`), e não a classe que monta o sistema.
- Tamanho: `ReservationService` passou de 138 para 177 linhas e virou a maior classe, mas continua razoável.

É um ciclo novo entre dois pacotes vizinhos, numa mistura localizada. Não chega a desfazer as camadas.

## Design de baixo nível

**Números:** 5 avisos ao todo: o hóspede na confirmação, a governança na confirmação e no cancelamento, o financeiro na confirmação e no cancelamento. Os 4 da equipe estão nos ouvintes. O do hóspede, que já existia na base, ficou fora da estrutura, numa chamada direta. A lista de ouvintes é criada dentro do serviço, e não registrada na montagem.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Observer:
- há uma interface de ouvinte com métodos `default`, de modo que cada ouvinte sobrescreve só o evento que lhe interessa (`ReservationListener.java:13-18`), e uma implementação por interessado (`GovernanceNotifier`, `FinanceNotifier`);
- o serviço só anuncia o evento e não monta nenhum e-mail da equipe (`ReservationService.java:84-85`, `:106-107`);
- o cancelamento também tem ouvintes, e cada ouvinte decide por si o que fazer (`FinanceNotifier.java:31`, `GovernanceNotifier.java:34`).

O que afasta, e que a referência põe em "malfeito" com todas as letras:
- o hóspede continua com a chamada direta `emailSender.send(...)` e o texto montado em `confirmationEmailBody` (`ReservationService.java:79-83`, `:164-170`), e só a governança e o financeiro viraram ouvintes;
- os ouvintes não são registrados na montagem: o construtor de 4 argumentos chama `StaffNotifications.all(emailSender)` (`ReservationService.java:24-27`), e `Hotel.java:24-25` usa justamente esse construtor. Assim, o serviço depende de quem é avisado, por um caminho indireto.

**O exagero.** Não há estrutura sem necessidade. Os dois `record` de evento, a interface e a lista estática são o mínimo que o pattern pede. `StaffNotifications` só existe porque a montagem foi parar no lugar errado, e esse defeito já conta na classe.

**O resto do código** (o que o agente escreveu; o resto veio da base sem mudança):
- `notifyListeners` relança a primeira falha (`ReservationService.java:155-157`) depois de a reserva já estar gravada (`:78`, `:105`) e de o hóspede já ter recebido o e-mail (`:80`). Se um ouvinte falhar, a API responde com erro para uma confirmação que já foi feita. Quem repetir o pedido recebe 409, e os ouvintes seguintes da mesma rodada são chamados, mas a falha sobe sem tratamento como `IllegalStateException`. `HttpApi.java:94` só mapeia `HotelException`.
- `roomOf(reservation)` é consultado duas vezes seguidas na confirmação (`ReservationService.java:79` e `:85`): duas idas ao banco atrás do mesmo quarto.
- Os eventos levam a `Reservation`, que é mutável, e o `Cancelled` carrega um `refund` que a reserva já tem. Um ouvinte pode, em tese, alterar a reserva que recebe.
- A condição `!event.wasConfirmed()` em `FinanceNotifier.java:31` não decide nada sozinha. Uma reserva pendente nunca foi paga, então o `refund` já é zero (`ReservationService.java:99-101`). Não é um erro, mas é uma regra repetida.
- Os tipos estão certos: `BigDecimal` para o valor e `TextFormat.money/date` reaproveitados do comprovante (`FinanceNotifier.java:3`, `GovernanceNotifier.java:3`).

São defeitos localizados. O código é legível.

## Testes

**Números:** 31 testes rodaram, nenhum falhou, nenhum foi pulado; há 31 métodos de teste (a base tinha 26, também todos passando). Cobertura de linhas: 93,2% (base: 93,8%). Cobertura de ramos: 86,2% (base: 87,1%). Em `hotel.notifications`, linhas 34 de 34 e ramos 6 de 6. Em `hotel.reservations`, ramos 54 de 60 (base: 49 de 52): os 3 ramos novos sem cobertura são os da coleta de falhas em `notifyListeners`.
**Nota:** com problemas

- O agente testou a parte nova contra o banco de verdade, com valores:
  - o assunto e as datas da governança na confirmação (`src/test/java/hotel/reservations/ReservationServiceTest.java:115-131`);
  - o assunto e o valor do financeiro (`:126-130`);
  - o cancelamento de reserva confirmada, com as datas e o reembolso (`:133-148`);
  - a reserva sem devolução, em que o financeiro não é avisado (`:151-159`);
  - a reserva pendente cancelada, que não avisa ninguém (`:162-168`);
  - um ouvinte qualquer plugado de fora (`:171-187`).
- Os testes da base não perderam força. As contagens passaram de 1 para 3 (`ReservationServiceTest.java:92`, `:106`, `HttpApiTest.java:81`), como a mudança pedia, e o e-mail do hóspede continua sendo conferido no assunto e no corpo (`ReservationServiceTest.java:93-98`).
- Asserções fracas:
  - o número de hóspedes é conferido com `body().contains("2")` (`ReservationServiceTest.java:125`), e o "2" já aparece nas datas "2027". Se alguém apagasse a linha `"Hóspedes: "` (`GovernanceNotifier.java:29`), o teste continuaria passando;
  - os valores em dinheiro são comparados com `TextFormat.money(...)` (`:130`, `:147`), o mesmo código que produz o texto, e não com um literal como "R$ 779,80".
- Sem rede:
  - a regra de que uma falha num ouvinte não impede os outros (`ReservationService.java:142-158`) não tem teste;
  - a condição `wasConfirmed` do financeiro (`FinanceNotifier.java:31`) pode ser apagada sem que nenhum teste falhe. Na prática, ela é redundante.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro em `numeric(10,2)` nas 6 colunas. Nenhuma chave estrangeira sem índice, nenhuma data guardada como texto. Há a restrição `EXCLUDE USING gist` contra reservas sobrepostas. As 4 colunas de situação e tipo (`status`, `rate_plan`, `type`, `channel`) não têm CHECK. A migração `V1__create_tables.sql` está igual à da base, e o agente não acrescentou nenhuma.
**Nota:** bom

- A mudança não exigia nada do esquema, e o agente não mexeu nele. Os avisos à equipe vão para a mesma tabela `messages`, por `SentMessages.add`, que já usava parâmetros e try-with-resources (`src/main/java/hotel/messages/SentMessages.java:20-35`).
- O SQL montado com `+` (`ReservationStore.java:33`, `:83`, `:98`, `:111`) só junta constantes, e veio da base.
- Também veio da base, sem piora: a falta de CHECK nas colunas de situação e a ausência de transação entre `store.update` e os inserts de mensagem. O agente acrescentou mais inserts fora de transação na confirmação e no cancelamento, mas seguiu o mesmo modelo da base para o e-mail do hóspede.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 03: 15 de 15. Reservas simultâneas: 2 de 2.
**Nota:** bom

Nada falhou. Para o dono da pousada:
- na confirmação, o hóspede continua recebendo o mesmo e-mail;
- a governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e o número de hóspedes;
- o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80;
- no cancelamento de reserva confirmada, chegam os avisos de "Reserva cancelada" e de "Reembolso", e este só quando há valor a devolver;
- a reserva pendente cancelada não avisa ninguém;
- tudo continua depois de reiniciar, e nunca há duas reservas do mesmo quarto nas mesmas noites.

O defeito de falha de ouvinte descrito no design não aparece em nenhuma chamada que o texto descreve, porque gravar um e-mail não costuma falhar.

## Dúvidas

- Classe: o agente plugou direito os interessados novos, mas deixou o hóspede como chamada direta e a lista de ouvintes criada dentro do serviço. A referência põe as duas coisas em "malfeito", e por isso dei `malfeito`. Um leitor que considere só os interessados novos poderia ver aqui algo perto de `acerto`.
- Testes: dei `com problemas` pela asserção vazia do número de hóspedes e pela falta de teste do isolamento de falhas. Um leitor mais tolerante, que pese a boa cobertura da parte nova com valores, poderia dar `bom`.
