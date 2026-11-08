# Revisão: manutenção, cenário 03

## Visão geral

O sistema (herdado da base) tem sete pacotes: `hotel` (montagem em `Hotel` e o `Database`), `hotel.web` (as rotas
Javalin em `HttpApi`), `hotel.reservations` (as regras em `ReservationService`, o preço, e o acesso a dados em
`ReservationStore`), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava na tabela `messages`),
`hotel.documents` (comprovante e formatação) e `hotel.reports`. Uma requisição entra pelo `HttpApi`, chama o
`ReservationService`, que usa o `ReservationStore`/`Rooms` (JDBC com parâmetros) sobre o PostgreSQL criado por uma
migração Flyway. A mudança do agente ficou toda em `hotel.reservations`: uma interface `ReservationListener` com dois
eventos (`ReservationConfirmedEvent`, `ReservationCancelledEvent`) e cinco ouvintes (hóspede, governança e financeiro
na confirmação; governança e financeiro no cancelamento). O serviço percorre a lista nos dois momentos, e o e-mail do
hóspede também virou ouvinte. O comportamento está todo certo (66 de 66). O que há de mais sério é que a lista de
ouvintes não vem da montagem: o próprio `ReservationService` cria os cinco ouvintes por nome
(`src/main/java/hotel/reservations/ReservationService.java:35-42`), e o `Hotel` continua usando o construtor antigo
(`src/main/java/hotel/Hotel.java:25`). Assim, para pôr ou tirar o restaurante ou o transfer, alguém ainda tem de
editar o serviço.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são idênticos aos da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo).
`hotel.reservations` passou de 13 classes e 508 linhas para 21 classes e 687 linhas, e `ReservationService` de 138
para 155 linhas.
**Nota:** com problemas

As camadas da base continuam inteiras: o agente não mexeu em `hotel.web` nem no acesso a dados, os ouvintes não
conhecem Javalin nem JDBC e não surgiu nenhuma dependência nova entre pacotes. O ciclo `documents` ↔ `reservations`
veio da base, onde `ReservationService` já usava `ReservationReceipt`. Os ouvintes novos o reforçam um pouco, porque
importam `hotel.documents.TextFormat`/`ReservationReceipt`
(`src/main/java/hotel/reservations/GovernanceConfirmationListener.java:3`,
`src/main/java/hotel/reservations/GuestConfirmationListener.java:3`), mas o ciclo não piorou de forma medível.

O defeito que o agente introduziu está na montagem. Os ouvintes são peças do sistema e deveriam ser ligadas em
`Hotel`, que monta todo o resto. Em vez disso, o serviço cria as dependências por dentro, em
`defaultListeners(emailSender)` (`src/main/java/hotel/reservations/ReservationService.java:35-42`), chamado pelo
construtor de 4 parâmetros (`:30-33`), que é o único que `Hotel` usa (`src/main/java/hotel/Hotel.java:25`). Os
ouvintes são classes package-private (`final class ...`, por exemplo
`src/main/java/hotel/reservations/FinanceConfirmationListener.java:6`), então `Hotel`, que fica no pacote `hotel`,
nem conseguiria criá-los. É uma mistura localizada, mas justamente no ponto que o dono avisou que vai mudar com os
contratos.

## Design de baixo nível

**Números:** 5 interessados (hóspede, governança e financeiro na confirmação; governança e financeiro no cancelamento),
os 5 como ouvintes e 0 fora da estrutura. Os 2 eventos passam pela lista
(`ReservationService.java:83-85` e `:109-111`). A lista é escolhida dentro do serviço, com os 5 ouvintes criados por
nome em `ReservationService.java:37-41`, e o construtor que recebe a lista de fora (`:18-28`) não é usado por ninguém,
nem pelo `Hotel` nem pelos testes.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Observer: há uma interface de ouvinte (`src/main/java/hotel/reservations/ReservationListener.java:3-7`)
com uma implementação por interessado. O serviço só anuncia o evento e percorre a lista (`ReservationService.java:81-85`
e `:106-112`), e não monta mais nenhum e-mail. O e-mail do hóspede, que a base escrevia dentro do serviço
(`confirmationEmailBody`), foi para `GuestConfirmationListener.java:16-29` com o mesmo texto. O cancelamento também
passa pelos ouvintes, e a regra "só avisa se havia algo a devolver" fica no ouvinte do financeiro
(`FinanceCancellationListener.java:22`).

O que afasta: a referência pede que o serviço "não conheça quem é avisado" e que os ouvintes sejam "registrados na
montagem do sistema". Aqui o serviço conhece e cria cada ouvinte concreto (`ReservationService.java:35-42`), e não há
como registrá-los de fora, porque o construtor com a lista não é usado e as classes não são públicas. A estrutura
existe, mas a decisão de quem é avisado continua dentro do serviço. Por isso a classe é malfeito, e não acerto.

Exagero: não há. As classes de evento e a interface são parte do pattern, e não há fábrica, builder nem camada que só
repassa.

O resto do design tem defeitos localizados, todos da mudança:
- O construtor novo recebe um `EmailSender emailSender` que nunca usa (`ReservationService.java:22`; o corpo em
  `:24-27` o ignora).
- A interface obriga cada ouvinte a implementar os dois eventos, e cada um dos cinco ouvintes deixa um método vazio
  (`FinanceCancellationListener.java:16`, `FinanceConfirmationListener.java:24`,
  `GovernanceCancellationListener.java:15`, `GovernanceConfirmationListener.java:26`,
  `GuestConfirmationListener.java:31`). Métodos `default` vazios na interface, ou uma interface por evento, evitariam
  essa repetição.
- Os endereços da governança e do financeiro aparecem como literais repetidos, um em cada par de classes
  (`GovernanceConfirmationListener.java:22` e `GovernanceCancellationListener.java:25`;
  `FinanceConfirmationListener.java:20` e `FinanceCancellationListener.java:26`), sem uma constante ou configuração.
- Há um import sem uso, `java.math.BigDecimal` em `FinanceCancellationListener.java:4`.

Os tipos estão certos: `BigDecimal` e `TextFormat.money` para dinheiro, `LocalDate` para datas. O resto do código
veio da base sem mudança.

## Testes

**Números:** 28 testes rodaram, 0 falhas, 0 erros, 0 pulados; 28 métodos escritos. A base tinha 26, e o agente
acrescentou 2 sem apagar nenhum. Cobertura de linhas: 537 de 568 (94,5%), contra 93,8% na base. Cobertura de ramos:
111 de 126 (88,1%), contra 87,1% na base. Em `hotel.reservations`: 275 de 288 linhas e 59 de 62 ramos.
**Nota:** com problemas

O que está testado: as duas confirmações agora conferem que saem 3 e-mails e que o 2º e o 3º vão para a governança e
para o financeiro (`src/test/java/hotel/reservations/ReservationServiceTest.java:91-98`,
`src/test/java/hotel/web/HttpApiTest.java:81-84`). O cancelamento de uma reserva confirmada STANDARD avisa os dois, e
o de uma NON_REFUNDABLE avisa só a governança (`ReservationServiceTest.java:127-146`). Isso pega a mutação
`signum() > 0` → `>= 0`. O cancelamento de uma reserva pendente não avisa ninguém (`ReservationServiceTest.java:149-156`),
o que pega a remoção do `if (wasConfirmed)`. Os testes antigos foram ajustados de 1 para 3 mensagens, sem perder as
asserções do e-mail do hóspede (`:93-96`).

O que ficou sem rede: nenhum teste confere o assunto nem o corpo dos e-mails novos, só o destinatário. Se alguém
trocasse "Reserva confirmada - quarto" por outro texto, pusesse o `id` no lugar do número do quarto, trocasse
`amountPaid()` por `total()`, invertesse entrada e saída, apagasse a linha "Hóspedes" ou escrevesse o reembolso com o
valor errado (`GovernanceConfirmationListener.java:17-21`, `FinanceConfirmationListener.java:17-19`,
`FinanceCancellationListener.java:23-25`), todos os testes continuariam passando. Os testes também dependem da ordem
das mensagens (`after.get(after.size() - 2)`), que o pedido diz não importar. A parte nova está testada na estrutura,
mas não nos valores que o dono pediu.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em
`numeric(10,2)`, não há chave estrangeira sem índice nem data guardada como texto, e a dupla reserva é barrada por um
`EXCLUDE USING gist`. As conferências acusaram só as colunas de situação e de tipo sem `CHECK` (`status`,
`rate_plan`, `type`, `channel`). A única migração da base, `V1__create_tables.sql`, está igual.
**Nota:** bom

O agente não mexeu no banco: nenhuma migração nova ou editada, nenhum SQL novo. Os e-mails novos usam o
`EmailSender`/`SentMessages` da base, com `INSERT` parametrizado (`src/main/java/hotel/messages/SentMessages.java:22-23`).
As colunas de texto sem `CHECK` (`src/main/resources/db/migration/V1__create_tables.sql:4,28,33,46`) vieram da base.
Um ponto menor: a confirmação agora grava a reserva e depois três mensagens, sem transação
(`ReservationService.java:80-85`). Se uma inserção falhasse no meio, a reserva ficaria confirmada com parte dos
avisos. A base já fazia a mesma coisa com uma mensagem, e o pedido não pede atomicidade, então isso não muda a nota.

## Comportamento

**Números:** 66 de 66 conferências passaram, 0 falharam. A parte comum: 46 de 46. Os dados depois de reiniciar: 3 de
3. O cenário 03: 15 de 15. As reservas simultâneas: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API.
- Na confirmação, o hóspede continua recebendo o e-mail de sempre. A governança recebe "Reserva confirmada - quarto
  201", com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro recebe "Pagamento recebido - reserva 1", com
  R$ 779,80.
- No cancelamento de uma reserva confirmada, a governança recebe "Reserva cancelada - quarto 201" com as datas, e o
  financeiro recebe "Reembolso - reserva 1" com R$ 779,80.
- Uma reserva NON_REFUNDABLE cancelada avisa só a governança, e uma pendente cancelada não avisa ninguém.

O que já funcionava continua funcionando, inclusive depois de reiniciar e com dois pedidos simultâneos. Para o dono,
o sistema faz o que ele pediu hoje. O custo aparece no próximo pedido: incluir o restaurante ou o transfer vai exigir
mexer no `ReservationService` (ver Arquitetura e Design).

## Dúvidas

A classe do pattern ficou entre acerto e malfeito. A estrutura é a do Observer: uma interface, um ouvinte por
interessado, o hóspede incluído, os dois eventos passando pela lista e um construtor que aceita a lista de fora
(`ReservationService.java:18-28`). Por isso daria para ler como acerto com a montagem no lugar errado. Escolhi
malfeito porque a lista que roda de fato é criada dentro do serviço (`ReservationService.java:35-42`, via
`Hotel.java:25`), e a referência pede explicitamente que o serviço não conheça quem é avisado e que o registro fique
na montagem. A mesma escolha pesou na nota da Arquitetura (montagem) e na do Design (o parâmetro sem uso). Tentei não
contar o mesmo defeito duas vezes, mas a fronteira entre as duas colunas aqui não é nítida.
