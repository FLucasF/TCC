# Revisão: manutenção, cenário 03

## Visão geral

A base já era um serviço Javalin em camadas. `hotel.web.HttpApi` cuida das rotas, `hotel.reservations` guarda as
regras (`ReservationService`) e o acesso a dados (`ReservationStore`, JDBC com pool), `hotel.messages` guarda os
e-mails (na tabela `messages`) e `hotel.Hotel` monta as peças. Os dados ficam no PostgreSQL, com o esquema criado pelo
Flyway (`V1__create_tables.sql`). Uma requisição de confirmação entra por `HttpApi` e chega a
`ReservationService.confirm`, que grava a reserva e manda o e-mail do hóspede. A mudança do agente é pequena e limpa.
Ele criou o pacote `hotel.notifications`, com uma interface de ouvinte (`ReservationEventListener`), uma lista de
ouvintes (`ReservationNotifications`) e dois ouvintes, a governança e o financeiro. Os dois são registrados em
`Hotel.java:28-30`, e o serviço chama a lista na confirmação e no cancelamento de uma reserva confirmada. O
comportamento está todo certo (66 de 66) e os testes novos conferem os valores. Há dois pontos fracos. O e-mail do
hóspede continua sendo uma chamada direta dentro do serviço (`ReservationService.java:73-76`), fora da estrutura de
ouvintes, então o Observer ficou pela metade. E o agente pôs a lista de ouvintes num pacote que depende de
`hotel.reservations`, o que criou um ciclo novo entre `reservations` e `notifications`.

## Arquitetura

**Números:** final com 8 pacotes, 23 dependências e 3 blocos de ciclo no tsort, contra 7 pacotes, 17 dependências e
1 ciclo na base (`documents ↔ reservations`, que já existia). O ciclo novo é `hotel.reservations ↔
hotel.notifications`, e o tsort também mostra o ciclo de três pacotes `documents → reservations → notifications`. Nenhum
pacote novo usa Javalin ou JDBC. `ReservationService` passou de 138 para 151 linhas.
**Nota:** com problemas

As camadas da base continuam inteiras. O pacote novo `hotel.notifications` não usa Javalin nem JDBC
(`measures/dependencies.md`, tabela de bibliotecas). Os ouvintes são criados e registrados num lugar só, a montagem
(`Hotel.java:27-32`), e o serviço recebe a lista pronta pelo construtor (`ReservationService.java:21-31`), sem `new`
por dentro. O defeito é o lugar onde o agente pôs as peças. O serviço importa a lista de ouvintes do pacote novo
(`ReservationService.java:5`), e esse pacote importa `Reservation` de volta: `ReservationEventListener.java:3`,
`HousekeepingNotifications.java:6` e `FinanceNotifications.java:6`. Isso fecha um ciclo novo entre dois pacotes que
não existia na base. A correção seria pequena: deixar a interface do ouvinte e a lista de ouvintes em
`hotel.reservations`, quem publica o evento, e só as implementações em `hotel.notifications`. Assim a dependência
apontaria num sentido só. O ciclo `documents ↔ reservations` veio da base e o agente não o piorou.

## Design de baixo nível

**Números:** há 3 interessados no total. Dois estão na estrutura de ouvintes, a governança
(`HousekeepingNotifications.java:10`) e o financeiro (`FinanceNotifications.java:11`). O terceiro, o hóspede, ficou
fora dela e continua sendo uma chamada direta do serviço (`ReservationService.java:73-76`, texto em
`ReservationService.java:138-144`). O cancelamento passa todo pelos ouvintes (`ReservationService.java:98-100`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

**O que aproxima do Observer.** A interface `ReservationEventListener` tem dois eventos,
`onReservationConfirmed` e `onReservationCancelled` (`ReservationEventListener.java:7-14`). Cada interessado tem a
sua implementação, e cada uma monta o próprio e-mail. A lista de ouvintes (`ReservationNotifications.java:11-27`)
só percorre os ouvintes registrados. O serviço anuncia os dois eventos e não sabe quem os recebe
(`ReservationService.java:77` e `:99`). O registro fica na montagem (`Hotel.java:29-30`). Para pôr o restaurante ou o
transfer, basta uma classe nova e uma linha em `Hotel`. A regra "só avisa o financeiro quando há algo a devolver"
fica no próprio ouvinte (`FinanceNotifications.java:33`), e não no serviço.

**O que afasta.** O hóspede também é um interessado na confirmação, mas continua com chamada direta:
`ReservationService.java:73-76` chama o `EmailSender` com o texto montado em `confirmationEmailBody`
(`ReservationService.java:138-144`). O serviço ainda depende do `EmailSender` (`ReservationService.java:18`). A
referência do pattern descreve esse caso como malfeito: "o hóspede tem chamada direta e só a governança e o
financeiro viraram ouvintes". Os avisos da confirmação ficam divididos entre dois mecanismos.

**Exagero:** não há. A interface tem duas implementações de verdade, e a lista de ouvintes é o próprio sujeito do
pattern. Não aparece nenhuma fábrica, builder ou camada que só repassa.

**O resto do código.** O código novo é curto e claro. O dinheiro e as datas usam os formatadores que já existiam
(`TextFormat.money` e `TextFormat.date`, `FinanceNotifications.java:3` e `HousekeepingNotifications.java:3`), sem
duplicar a formatação. A situação anterior ao cancelamento é guardada antes de mudar
(`ReservationService.java:91`), e isso está certo. Os dois ouvintes repetem um pouco do jeito de montar o corpo do
e-mail, o que é aceitável. Há uma limitação pequena: `ReservationNotifications` só tem `addListener`
(`ReservationNotifications.java:13`). Como o registro fica na montagem, isso basta para os contratos que "entram e
saem". O resto do código veio da base e o agente não o piorou.

## Testes

**Números:** final com 30 testes rodados, 0 falhas, 0 erros, 0 pulados e 30 métodos escritos. A base tinha 26, todos
passando. A cobertura de linhas foi de 93,8% para 94,4% (527 de 558) e a de ramos de 87,1% para 88,1% (111 de 126).
O pacote `hotel.notifications` tem 46 de 46 linhas e 6 de 6 ramos cobertos.
**Nota:** bom

O agente escreveu 4 testes novos em `ReservationServiceTest`, contra o banco de verdade:

- a confirmação (`ReservationServiceTest.java:176-193`) confere o destinatário, o assunto, as datas e o valor
  "R$ 779,80";
- o cancelamento de uma reserva confirmada (`:196-213`) confere a governança, com as datas, e o financeiro, com
  "R$ 1.169,70";
- o cancelamento de uma reserva pendente (`:216-222`) confere que nenhuma mensagem sai;
- o cancelamento NON_REFUNDABLE (`:225-235`) confere que saem 4 mensagens, sem o financeiro.

Nenhum teste da base foi apagado. Os que mudaram só ajustaram a contagem de mensagens de 1 para 3
(`ReservationServiceTest.java:103`, `:117` e `HttpApiTest.java:81`), e continuam conferindo o e-mail do hóspede em
`sent.get(0)`.

Fiz a pergunta da mutação em três regras da parte nova:

- trocar `> 0` por `>= 0` em `FinanceNotifications.java:33` faz o teste da `:232` falhar (seriam 5 mensagens);
- tirar o `if (wasConfirmed)` faz o teste da `:221` falhar;
- trocar o valor do reembolso faz o teste da `:212` falhar.

A asserção fraca é a do número de hóspedes: `housekeeping.body().contains("2")` (`ReservationServiceTest.java:188`)
passa só por causa de "2027" ou "201". Se alguém apagar a linha "Hóspedes" do corpo
(`HousekeepingNotifications.java:27`), nenhum teste falha. É um ponto só, numa suíte que confere com valores todo o
resto da parte nova.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`. A chave estrangeira tem índice. Uma restrição `EXCLUDE` com gist impede duas
reservas do mesmo quarto na mesma noite. Há 1 migração Flyway, `V1__create_tables.sql`, igual à da base. As
conferências só acusaram as colunas `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK, e isso já vinha da
base.
**Nota:** bom

O agente não mexeu no banco, e o pedido não precisava. Os e-mails novos são guardados pelo mesmo `EmailSender` e
`SentMessages` da base, com SQL parametrizado. Nenhuma migração foi editada. Os candidatos a SQL montado com `+`
(`ReservationStore.java:33, 83, 98, 111`) só concatenam a constante `COLUMNS` e são da base.

Há um ponto que já existia na base e que a mudança só estende. Na confirmação, a reserva é gravada
(`ReservationService.java:70`) antes dos e-mails, cada um num insert próprio, sem transação. Se um insert de mensagem
falhar no meio, a reserva fica confirmada com parte dos avisos guardados. O e-mail do hóspede da base já tinha esse
risco.

## Comportamento

**Números:** 66 de 66 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 03: 15 de 15;
- duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API. Na confirmação, o hóspede continua recebendo o mesmo e-mail. A governança
recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e os 2 hóspedes. O financeiro recebe "Pagamento
recebido - reserva 1", com R$ 779,80. No cancelamento de uma reserva confirmada, a governança recebe as datas
liberadas e o financeiro recebe o reembolso de R$ 779,80. Numa NON_REFUNDABLE cancelada, o financeiro não é avisado,
e uma reserva pendente cancelada não manda nada. O que já funcionava continua funcionando, inclusive depois de
reiniciar e com dois pedidos simultâneos.

## Dúvidas

A nota dos testes ficou entre `bom` e `com problemas`. A asserção do número de hóspedes
(`ReservationServiceTest.java:188`) é fraca e deixa essa parte do e-mail da governança sem rede. Dei `bom` porque é
um ponto isolado e todas as outras regras novas são conferidas com valores.
