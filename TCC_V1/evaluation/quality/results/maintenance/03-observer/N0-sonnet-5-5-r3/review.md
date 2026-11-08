# Revisão: manutenção, cenário 03

## Visão geral

A base já era um sistema em camadas: `hotel.web.HttpApi` (Javalin) recebe a requisição e chama
`hotel.reservations.ReservationService`, que aplica as regras e grava por `ReservationStore` (JDBC, PostgreSQL, esquema
por Flyway). Os e-mails "saem" pelo `EmailSender`, que os grava na tabela `messages`. O agente criou o pacote
`hotel.notifications` com uma interface `ReservationListener` (dois eventos: `confirmed` e
`confirmedReservationCancelled`) e um ouvinte por interessado (`GuestNotifications`, `HousekeepingNotifications`,
`FinanceNotifications`). Tirou o e-mail do hóspede de dentro do serviço e passou a fazer o serviço percorrer uma lista de
ouvintes que recebe no construtor, montada em `Hotel` (`final/src/main/java/hotel/Hotel.java:25-28`). O roteiro do
comportamento passou inteiro (66 de 66), e o `mvn verify` passa (30 testes). O mais sério é pequeno: o agente deixou o
construtor antigo do serviço, que recebe o `EmailSender` e chama `ReservationListeners.standard`
(`src/main/java/hotel/reservations/ReservationService.java:21-24`). Com isso o serviço continua sabendo quem é avisado,
e esse construtor, junto com a interface colocada em `notifications`, abre um ciclo novo entre `hotel.reservations` e
`hotel.notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 22 dependências entre eles (a base tinha 17), com 3 ciclos pelo `tsort`. A base
tinha 1 ciclo, `documents ↔ reservations`. Os ciclos novos são `reservations ↔ notifications` e o ciclo de três
`documents → reservations → notifications`.
**Nota:** com problemas

- As camadas da base continuam onde estavam. O pacote novo `hotel.notifications` não usa Javalin nem JDBC
  (`measures/dependencies.md`, tabela das bibliotecas), e quem liga as peças continua sendo `Hotel`
  (`src/main/java/hotel/Hotel.java:25-28`).
- O ciclo novo vem de duas escolhas:
  - A interface do ouvinte ficou no pacote dos ouvintes (`src/main/java/hotel/notifications/ReservationListener.java:10`).
    O serviço depende dela (`src/main/java/hotel/reservations/ReservationService.java:5,19`), e ela e os ouvintes
    dependem de `hotel.reservations.Reservation` (`ReservationListener.java:4`, `FinanceNotifications.java:5`).
  - O serviço importa a lista concreta `ReservationListeners` (`ReservationService.java:6,23`) para manter o construtor
    antigo. Só os testes usam esse construtor (`src/test/java/hotel/reservations/ReservationServiceTest.java:24-26`,
    `src/test/java/hotel/reports/ReportsTest.java:27-28`).
- A solução seria pôr a interface em `hotel.reservations` e apagar o construtor antigo: as dependências passariam a
  apontar só de `notifications` para `reservations`.
- É um ciclo novo entre dois pacotes vizinhos, que a skill põe em "com problemas".
- O ciclo `documents ↔ reservations` veio da base.
- `ReservationService` cresceu de 138 para 158 linhas. Continua no tamanho das outras classes grandes.

## Design de baixo nível

**Números:** 3 interessados, todos na estrutura: o hóspede, que já existia, e a governança e o financeiro, que são
novos. São 2 eventos, os dois anunciados pela lista de ouvintes. Nenhum e-mail é montado no serviço, e nenhum
interessado é chamado direto.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido (Observer):

- **O que aproxima:**
  - O serviço anuncia a confirmação e o cancelamento de uma reserva confirmada por `notifyListeners`
    (`ReservationService.java:73`, `:94-96`) e não sabe que e-mails saem.
  - Cada interessado é uma classe separada, com o seu endereço e o seu texto:
    - o hóspede, `GuestNotifications.java:19-27`, com o texto da base mantido;
    - a governança, `HousekeepingNotifications.java:20-38`;
    - o financeiro, `FinanceNotifications.java:20-38`. A regra "sem nada a devolver, não avisar o financeiro" ficou no
      próprio ouvinte (`FinanceNotifications.java:30-32`), e não no serviço.
  - O caso que já existia, o hóspede, também virou ouvinte.
  - A interface tem métodos `default` vazios (`ReservationListener.java:13-21`), e por isso o restaurante ou o transfer
    implementam só o evento que lhes interessa.
  - A lista chega de fora pelo construtor (`ReservationService.java:26-33`) e é montada na inicialização
    (`Hotel.java:28`, com `ReservationListeners.java:12-17`). Pôr ou tirar um interessado é mexer nessa lista, não no
    serviço.
  - Se um ouvinte falha, os outros continuam sendo avisados (`ReservationService.java:135-151`).
- **O que afasta:** o construtor antigo (`ReservationService.java:21-24`) faz o próprio serviço montar a lista padrão de
  interessados e ainda importa `EmailSender` (`:4`). Não chega a ser "malfeito", porque o serviço não monta nenhum
  e-mail e a produção usa a lista que vem de fora. Mas é um resto do jeito antigo que deveria sair.

O exagero: nenhuma estrutura sem necessidade. A interface tem três implementações de verdade. `ReservationListeners`
é só uma função que monta a lista (`ReservationListeners.java:12-17`). Ela poderia morar em `Hotel`, mas não é uma camada
que só repassa.

O resto do design:

- O agente reaproveitou `TextFormat` para dinheiro e datas (`HousekeepingNotifications.java:25-26`,
  `FinanceNotifications.java:25`) em vez de formatar de novo.
- O dinheiro continua em `BigDecimal` e as datas em `LocalDate`.
- Os defeitos que puxam a nota para "com problemas":
  - o construtor duplicado que mantém o acoplamento;
  - o ciclo citado acima.
- Pequenos:
  - `notifyListeners` relança a falha depois de `store.update` (`ReservationService.java:71-73`, `:148-150`). A reserva
    fica confirmada e o cliente recebe erro. A base fazia o mesmo com o e-mail único, então isso não foi piorado.
  - O comentário de `EmailSender` ainda diz "to guests" (`src/main/java/hotel/messages/EmailSender.java:3`). Isso veio
    da base.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 30 métodos escritos. A base tinha 26, e os 4 novos
estão em `ReservationServiceTest`.

| | final | base |
|---|---|---|
| cobertura de linhas | 93,4% (521/558) | 93,8% |
| cobertura de ramos | 85,4% (111/130) | 87,1% |
| `hotel.notifications`, linhas | 41/42 | (pacote novo) |
| `hotel.notifications`, ramos | 3/4 | (pacote novo) |

**Nota:** bom

- **A parte nova está testada com valores:**
  - confirmação, com a governança, o assunto com o quarto 201, as duas datas e "Hóspedes: 2"
    (`src/test/java/hotel/reservations/ReservationServiceTest.java:105-121`), e o financeiro com o assunto e "R$ 779,80"
    (`:117-120`);
  - cancelamento de uma reserva confirmada, com os dois avisos e o valor (`:124-137`);
  - NON_REFUNDABLE cancelada, em que só a governança é avisada (`:140-149`);
  - pendente cancelada, em que ninguém é avisado (`:152-158`).
- **Os testes da base:** só mudou a contagem de mensagens, de 1 para 3 (`:91`, `:165`, e
  `src/test/java/hotel/web/HttpApiTest.java:81`). As outras asserções sobre o e-mail do hóspede ficaram (`:92-96`).
  Nenhum teste foi apagado ou enfraquecido.
- **A pergunta da mutação:**
  - Apagar o `if (wasConfirmed)` (`ReservationService.java:94`) faz `:152` falhar.
  - Apagar a guarda do reembolso (`FinanceNotifications.java:30`) faz `:140` falhar.
  - Trocar `checkOut` por `checkIn` no e-mail de confirmação da governança faz `:115` falhar.
- **O que fica sem rede:**
  - A data de saída no aviso de cancelamento não é conferida (`:133` só confere 10/03/2027).
  - O caminho em que um ouvinte falha e os outros continuam (`ReservationService.java:138-150`) não tem teste. Por isso
    os ramos de `reservations` não cobertos passaram de 3 para 6.
  - Os testes usam o construtor antigo (`ReservationServiceTest.java:24-26`). Nenhum testa o serviço com uma lista de
    ouvintes própria.
- **Detalhes:**
  - `:132-135` depende da ordem em que saem os e-mails, que o dono disse não importar.
  - Há duas linhas em branco seguidas (`:98-99`) e falta uma linha em branco antes de `:159`.

Nada disso deixa sem teste uma regra que o pedido descreve.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`). Há 1 migração Flyway, `V1__create_tables.sql`, igual à
da base. As conferências acusaram só colunas de situação e tipo em `text` sem CHECK (`messages.channel`,
`reservations.rate_plan`, `reservations.status`, `rooms.type`). Dinheiro em `numeric(10,2)`, chaves primárias, chave
estrangeira com índice e `EXCLUDE` contra reserva duplicada estão todos no lugar.
**Nota:** bom

- O agente não mexeu no banco. A mudança não pedia esquema novo, porque os avisos vão para a mesma tabela `messages`
  pelo `EmailSender` (`src/main/java/hotel/messages/SentMessages.java:35-50`, com SQL parametrizado).
- O que as conferências acusaram veio da base:
  - as colunas de situação sem CHECK;
  - nenhuma transação: a atualização da reserva e as gravações das mensagens não andam juntas (`ReservationService.java:71-73`).
- Com três mensagens por confirmação em vez de uma, uma falha no meio deixa parte dos avisos gravados. É o mesmo
  desenho da base, aplicado a mais gravações. Não pesa como defeito novo.

## Comportamento

**Números:** 66 de 66 conferências passaram:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 03 | 15 de 15 |
| as duas reservas ao mesmo tempo | 2 de 2 |

**Nota:** bom

Para o dono da pousada, isto quer dizer:

- O hóspede continua recebendo o mesmo e-mail.
- Na confirmação:
  - a governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes;
  - o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- No cancelamento de uma reserva confirmada:
  - a governança recebe "Reserva cancelada - quarto 201", com as datas;
  - o financeiro recebe "Reembolso - reserva 1", com R$ 779,80, e só quando há valor a devolver;
  - a NON_REFUNDABLE avisa só a governança.
- A reserva pendente cancelada não avisa ninguém.
- Tudo o que já funcionava continua funcionando, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

## Dúvidas

O construtor antigo de `ReservationService` (`ReservationService.java:21-24`) monta, de dentro do serviço, a lista
padrão de interessados. Eu o tratei como resto a limpar (nota do design e da arquitetura) e não como "malfeito",
porque a produção recebe a lista de fora (`Hotel.java:28`) e o serviço não monta nenhum e-mail. Um revisor mais rígido
poderia ler esse construtor como o serviço ainda conhecendo quem é avisado.
