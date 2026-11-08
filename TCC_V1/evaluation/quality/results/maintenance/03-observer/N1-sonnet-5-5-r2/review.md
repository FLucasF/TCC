# Revisão: manutenção, cenário 03

## Visão geral

O sistema da base tem a montagem em `hotel.Hotel`, a borda HTTP em `hotel.web` (Javalin), as regras em
`hotel.reservations` (`ReservationService`, `PriceCalculator`) e o acesso ao banco em classes próprias
(`ReservationStore`, `Rooms`, `SentMessages`), com o esquema criado por uma migração Flyway. Uma requisição de
confirmação passa por `HttpApi` → `ReservationService.confirm` → `ReservationStore.update`. Depois disso o serviço avisa
uma lista de `ReservationListener` que recebe de fora. O agente criou essa interface no pacote `reservations`
(`src/main/java/hotel/reservations/ReservationListener.java:9`) e três ouvintes num pacote novo, `hotel.notifications`:
o hóspede, a governança e o financeiro. Os três são registrados em `src/main/java/hotel/Hotel.java:31-34`. O Observer
ficou bem aplicado e todas as conferências do comportamento passam (66 de 66). O ponto mais sério é que nenhum teste
novo confere os avisos: os 26 testes são os mesmos da base, e a regra do cancelamento nunca é executada pelos testes.

## Arquitetura

**Números:** 8 pacotes (7 na base, mais `hotel.notifications`), 21 dependências entre pacotes (17 na base), um ciclo
(`hotel.documents` ↔ `hotel.reservations`), que já existia na base.
**Nota:** bom

A mudança melhorou as dependências. Na base, `hotel.reservations` dependia de `hotel.messages`, porque o serviço chamava
o `EmailSender` direto. No final essa dependência sumiu: `reservations` passou de 3 para 2 dependências em
`measures/dependencies.md`. Agora quem conhece o e-mail é `hotel.notifications`, que depende de `reservations`
(através da interface `ReservationListener`), de `messages`, de `documents` e de `rooms`. Nada depende de
`notifications` além da montagem (`src/main/java/hotel/Hotel.java:5-7`).

A interface do ouvinte ficou no pacote das regras (`src/main/java/hotel/reservations/ReservationListener.java:9`).
Assim o serviço não conhece quem é avisado, e a dependência aponta de `notifications` para `reservations`, que é a
direção certa. A montagem continua num lugar só (`src/main/java/hotel/Hotel.java:29-36`). O novo pacote não usa
Javalin nem JDBC.

O ciclo `documents` ↔ `reservations` vem da base: `ReservationService` usa `ReservationReceipt`
(`src/main/java/hotel/reservations/ReservationService.java:3`, `:110`, `:119`), e o agente não o piorou.
`ReservationService` encolheu de 138 para 131 linhas.

## Design de baixo nível

**Números:** 3 interessados (o hóspede, a governança, o financeiro) e 2 eventos (a confirmação e o cancelamento de uma
reserva confirmada). Os 3 interessados estão na estrutura, inclusive o hóspede, que é o caso que já existia, e nenhum
ficou fora dela. O serviço não monta nenhum e-mail.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** A estrutura segue a referência do Observer em todos os pontos:

- uma interface de ouvinte com um método por evento e implementação vazia por padrão
  (`src/main/java/hotel/reservations/ReservationListener.java:12`, `:16`);
- uma implementação por interessado: `GuestNotifier.java:21`, `HousekeepingNotifier.java:21` e `:31`,
  `FinanceNotifier.java:21` e `:29`;
- o serviço recebe a lista de fora (`ReservationService.java:19`, com uma cópia imutável em `:23`) e só percorre essa
  lista, na confirmação (`ReservationService.java:64`) e no cancelamento (`ReservationService.java:85-88`);
- os ouvintes são registrados na montagem (`Hotel.java:31-34`).

O e-mail do hóspede também virou ouvinte, com o mesmo texto da base (`GuestNotifier.java:21-31`). O
`confirmationEmailBody` e o `HOTEL_NAME` saíram do serviço. A regra "só avisar o financeiro quando há o que devolver"
mora no ouvinte do financeiro (`FinanceNotifier.java:30`), e não no serviço. A regra "a pendente cancelada não avisa
ninguém" ficou no serviço (`ReservationService.java:78`, `:85`), onde está a informação da situação anterior. Isso é
razoável: o evento se chama, com precisão, `confirmedReservationCancelled`. Para avisar o restaurante ou o transfer,
basta criar uma classe e acrescentá-la à lista em `Hotel`, sem tocar no serviço.

**O exagero.** Não há estrutura sem necessidade: nenhuma fábrica, nenhum registro dinâmico, nenhuma camada que só
repassa. A interface tem três implementações.

**O resto do código.** A mudança é pequena e limpa, e o dinheiro e as datas continuam com `BigDecimal` e `LocalDate`,
formatados pelo `TextFormat` que já existia (`FinanceNotifier.java:25`, `HousekeepingNotifier.java:25-27`). Há pontos
menores:

- Os ouvintes rodam depois de `store.update` (`ReservationService.java:62-64`, `:84-88`) e sem proteção. Se um deles
  lançar uma exceção (uma falha ao gravar a mensagem), a reserva já ficou confirmada, a resposta é um erro, e os
  ouvintes seguintes não são avisados. A base já tinha a mesma ordem com um único e-mail; com uma lista que vai
  crescer, o efeito aumenta.
- O comentário de `EmailSender` ainda diz "Sends e-mails to guests" (`src/main/java/hotel/messages/EmailSender.java:3`),
  e agora ele também manda para a equipe.
- O agente deixou um arquivo vazio, sem uso, em `src/test/java/hotel/notifications_placeholder`.

Nada disso exige reescrever o código antes de aceitar.

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados; 26 métodos de teste escritos (os mesmos 26 da base, nenhum
novo). Cobertura de linhas de 92,1% (499 de 542; 93,8% na base) e de ramos de 86,1% (105 de 122; 87,1% na base). No
pacote novo, `hotel.notifications`: 25 de 36 linhas e **0 de 2 ramos**.
**Nota:** com problemas

O agente manteve os testes da base passando, sem apagar nenhum, e os adaptou à nova assinatura do construtor:
`ReservationServiceTest.java:27` passa só o `GuestNotifier`, e `ReportsTest.java:27` passa `List.of()`. O teste do
e-mail do hóspede continua conferindo o destinatário, o assunto e os valores (`ReservationServiceTest.java:91-97`).

A parte nova, porém, não tem teste próprio. A única mudança de asserção é a contagem de mensagens, de 1 para 3, em
`src/test/java/hotel/web/HttpApiTest.java:80`, que confere quantos e-mails saíram, mas não para quem nem com que
conteúdo. Nenhum teste chama `HousekeepingNotifier` nem `FinanceNotifier` com um cancelamento, e por isso os dois
ramos do pacote não são cobertos. O arquivo `notifications_placeholder` sugere que esses testes foram planejados e não
escritos. A pergunta da mutação:

- Se alguém apagar a condição `signum() == 0` (`FinanceNotifier.java:30`), o financeiro passa a receber "Reembolso"
  de R$ 0,00 e nenhum teste falha.
- Se alguém apagar o `if (wasConfirmed)` (`ReservationService.java:85`), a pendente cancelada passa a avisar todo
  mundo e nenhum teste falha: no `ReservationServiceTest` só há o ouvinte do hóspede, que não reage a cancelamento.
- Se alguém trocar o endereço da governança, o assunto "Pagamento recebido - reserva N", as datas ou o valor no corpo,
  nenhum teste falha, porque só a contagem de 3 está conferida.

## Banco

**Números:** 3 tabelas, todas com chave primária; dinheiro em `numeric(10,2)` (6 colunas); a chave estrangeira tem
índice; nenhuma data em texto; restrição `EXCLUDE` contra duas reservas do mesmo quarto na mesma noite. As
conferências só acusaram as colunas `status`, `rate_plan`, `type` e `channel` em `text` sem `CHECK`. A migração
`V1__create_tables.sql` ficou igual à da base, e o agente não acrescentou nenhuma.
**Nota:** bom

A mudança não precisava mexer no esquema, e não mexeu: os avisos são gravados na tabela `messages` que já existia,
através do mesmo `EmailSender`/`SentMessages` (`src/main/java/hotel/messages/EmailSender.java:12-14`). O SQL usa
parâmetros (os `+` de `ReservationStore.java:33`, `:83`, `:98` e `:111` só juntam constantes). As colunas sem `CHECK`
e a ausência de transação entre a atualização da reserva e a gravação das mensagens vêm da base. Agora uma
confirmação grava até três mensagens em escritas separadas (`ReservationService.java:62-64`), sem transação, mas isso
não perde dado da reserva.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 03:
15 de 15. Duas reservas ao mesmo tempo: 2 de 2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:

- **Confirmação:** o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201",
  com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.
- **Cancelamento de reserva confirmada:** a governança recebe "Reserva cancelada - quarto 201", com as datas. O
  financeiro recebe "Reembolso - reserva 1", com R$ 779,80.
- **Casos de não avisar:** na NON_REFUNDABLE cancelada, só a governança é avisada. A pendente cancelada não manda
  nada.

O que já funcionava continua funcionando, os dados sobrevivem a um reinício, e as reservas simultâneas são barradas.

## Dúvidas

Nos testes, entre `com problemas` e `ruim`. A suíte existe, passa e confere com valores as regras da base. Mas a parte
nova não tem nenhum teste próprio, só uma contagem em `HttpApiTest.java:80`. Fiquei em `com problemas` porque o
critério de `ruim` fala em não haver testes ou em testes que não conferem nada, e aqui as regras antigas continuam bem
conferidas.
