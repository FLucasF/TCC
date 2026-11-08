# Revisão: manutenção, cenário 03

## Visão geral

A base tem sete pacotes: `hotel` (montagem e banco), `hotel.web` (rotas Javalin), `hotel.reservations` (regras e
acesso a dados das reservas), `hotel.rooms`, `hotel.messages` (o `EmailSender`, que grava cada e-mail na tabela
`messages`), `hotel.documents` (comprovante e formatação) e `hotel.reports`. Uma requisição entra em `HttpApi`, chama
`ReservationService`, que usa `ReservationStore`/`Rooms` (JDBC, PostgreSQL com Flyway). A mudança do agente é pequena
e localizada. Ele criou a interface `ReservationListener` (`onConfirmed`/`onCancelled`) e três ouvintes
(`GuestNotifier`, `HousekeepingNotifier`, `FinanceNotifier`). O serviço agora recebe a lista de ouvintes de fora e a
percorre na confirmação e no cancelamento de reserva confirmada. A lista é montada em `Hotel`. É um Observer correto,
e o e-mail do hóspede foi movido sem mudar uma vírgula. As 66 conferências do roteiro passaram e os 27 testes
também. Não há defeito sério. As ressalvas são miúdas: uma asserção fraca no teste do número de hóspedes, o
parâmetro `room` que dois ouvintes ignoram, e um ouvinte que lança exceção ainda impede os seguintes de rodarem.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre pacotes, iguais na base e na final (`measures/dependencies.md`,
`measures/dependencies-base.md`). Um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base.
`hotel.reservations` passou de 13 para 17 classes (508 → 634 linhas). Nenhuma dependência nova entre pacotes.
**Nota:** bom

O agente não mexeu nas camadas. A borda HTTP (`hotel.web`) continua igual, e as regras continuam sem Javalin.
Os ouvintes dependem só de `hotel.messages` e `hotel.documents`, que `hotel.reservations` já usava na base. Por isso
o grafo não mudou. A montagem continua num lugar só: os ouvintes são criados e registrados em
`src/main/java/hotel/Hotel.java:29-34`. O serviço deixou de conhecer o `EmailSender`
(`src/main/java/hotel/reservations/ReservationService.java:17,25`). O ciclo `documents` ↔ `reservations` vem da base
(`ReservationReceipt` lê `Reservation`, o serviço devolve `ReservationReceipt` em `ReservationService.java:110-127`),
e o agente não o piorou. Uma ressalva pequena: os ouvintes de e-mail ficaram no pacote `hotel.reservations`, junto
das regras e do `ReservationStore`. Um pacote próprio de avisos deixaria mais claro o que é regra e o que é reação a
ela, mas num sistema deste tamanho isso não é defeito.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro) e 2 eventos (confirmação, cancelamento de reserva
confirmada). Os 3 interessados estão na estrutura, o hóspede também, e nenhum fica fora dela. O serviço não monta
nenhum e-mail e não conhece nenhum destino.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern pedido é o Observer, e é o que o agente fez. Há uma interface de ouvinte
(`src/main/java/hotel/reservations/ReservationListener.java:9,12`) com uma implementação por interessado:
`GuestNotifier.java:17-23`, `HousekeepingNotifier.java:19-37` e `FinanceNotifier.java:20-37`. O serviço recebe a
lista pronta (`ReservationService.java:19-26`) e a percorre sem perguntar quem é cada ouvinte, tanto na confirmação
(`ReservationService.java:65-68`) quanto no cancelamento (`ReservationService.java:90-95`). A regra "pendente
cancelada não avisa ninguém" fica no serviço (`ReservationService.java:83,90`), que é onde está o estado. A regra
"só avisa o financeiro se há o que devolver" fica no ouvinte dele (`FinanceNotifier.java:30`), que é quem decide.
O e-mail do hóspede, que na base estava escrito à mão em `ReservationService.confirmationEmailBody`, virou ouvinte
com o mesmo texto (`GuestNotifier.java:30-36`). Ligar o restaurante ou o transfer passa a ser uma classe nova e uma
linha em `Hotel.java:30-33`, sem tocar no serviço.

Não há exagero. A interface tem três implementações de verdade, e não há fábrica, builder, barramento de eventos
nem camada que só repassa.

Sobre o resto do código, há pontos pequenos que um revisor anotaria, mas que não justificam pedir mudança antes de
aceitar:
- A interface obriga todo ouvinte a implementar os dois eventos, e por isso o `GuestNotifier` tem um `onCancelled`
  vazio (`GuestNotifier.java:26-28`). Métodos `default` vazios na interface evitariam isso quando o restaurante e o
  transfer, que só querem a confirmação, entrarem.
- O parâmetro `room` é ignorado pela governança e pelo financeiro, que usam `reservation.roomNumber()`
  (`HousekeepingNotifier.java:20,31`).
- O `GuestNotifier` lê a constante `ReservationService.HOTEL_NAME` (`GuestNotifier.java:21`), o que deixa o ouvinte
  dependente do serviço.
- Os ouvintes rodam em sequência, depois do `store.update` e sem isolar falhas (`ReservationService.java:64-68`). Se
  um ouvinte lançar exceção, a reserva já foi gravada como confirmada, os ouvintes seguintes não rodam e o cliente
  recebe erro. Na base era igual, com um e-mail só. Agora são três, e a tendência é crescer.

O dinheiro continua em `BigDecimal`, e a comparação do reembolso usa `compareTo` (`FinanceNotifier.java:30`), não
`equals`.

## Testes

**Números:** 27 testes rodaram, 0 falharam, 0 pulados (27 métodos escritos; a base tinha 26, também todos
passando). Cobertura de linhas 94,4% (522/553; base 93,8%), de ramos 88,1% (111/126; base 87,1%). Em
`hotel.reservations`, 258/271 linhas e 59/62 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes da base. Ele só ajustou as contagens de mensagens de 1 para 3, sem apagar nada.
Também acrescentou asserções da parte nova:
- **Confirmação:** destino, assunto e valor pago do financeiro, e destino, assunto e datas da governança
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:100-109`). O e-mail do hóspede continua conferido
  com valores (`:95-99`).
- **Cancelamento:** um teste novo (`ReservationServiceTest.java:138-169`). Ele confere que a pendente cancelada não
  manda nada (`:146-147`), os dois avisos da STANDARD com assunto, datas e o reembolso de R$ 249,90 (`:149-160`) e
  que a NON_REFUNDABLE só avisa a governança (`:162-168`).
- **Pela API:** os três destinos (`src/test/java/hotel/web/HttpApiTest.java:81-84`).

A pergunta da mutação dá resultado bom na maior parte. Trocar `> 0` por `>= 0` em `FinanceNotifier.java:30` faria
`:165` falhar. Apagar o `if (wasConfirmed)` (`ReservationService.java:90`) faria `:147` falhar. Tirar um ouvinte da
lista faria as contagens falharem.

Há um ponto sem rede, e é uma regra pedida: o número de hóspedes no e-mail da governança. A asserção
`sent.get(1).body().contains("1")` (`ReservationServiceTest.java:105`) passa com qualquer corpo que tenha o quarto
101 ou as datas, então apagar a linha `"Hóspedes: " + reservation.guests()` (`HousekeepingNotifier.java:25`) não
derrubaria teste nenhum. Além disso, os testes leem as mensagens por posição (`sent.get(1)`, `sent.get(2)`), e o
texto diz que a ordem não importa. Mudar a ordem de registro em `Hotel` quebraria os testes sem mudar o
comportamento.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. Dinheiro todo em
`numeric(10,2)`. Nenhuma chave estrangeira sem índice e nenhuma data como texto. EXCLUDE com gist contra duas
reservas do mesmo quarto na mesma noite. 1 migração Flyway, `V1__create_tables.sql`, igual à da base
(`measures/database.md`). As conferências acusaram só as colunas de situação e de tipo (`status`, `rate_plan`,
`channel`, `type`) em `text` sem CHECK. Há 0 transações explícitas.
**Nota:** bom

O agente não tocou no banco. A mudança não pedia coluna nova, e os e-mails novos usam a tabela `messages` que já
existia, pelo mesmo `EmailSender`. A migração da base continua intacta. O SQL de `ReservationStore` usa parâmetros
(`?`); as concatenações listadas pelo script juntam só a constante `COLUMNS`
(`src/main/java/hotel/reservations/ReservationStore.java:98,111`). As colunas de texto sem CHECK e a falta de
transação vêm da base. O que a mudança agravou um pouco foi a falta de transação entre `store.update` e os e-mails
(`ReservationService.java:64-68`): agora são até quatro escritas independentes por confirmação em vez de duas, e uma
falha no meio deixa a reserva confirmada com só parte dos avisos gravados. Isso não é defeito novo de esquema.

## Comportamento

**Números:** 66 de 66 conferências passaram (`measures/behavior.md`). A parte comum: 46/46. Os dados depois de
reiniciar: 3/3. O cenário 03: 15/15. As reservas simultâneas: 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- Na confirmação, o hóspede continua recebendo o mesmo e-mail. A governança recebe "Reserva confirmada - quarto 201"
  com entrada 10/03/2027, saída 12/03/2027 e 2 hóspedes. O financeiro recebe "Pagamento recebido - reserva 1" com
  R$ 779,80.
- No cancelamento de reserva confirmada, a governança recebe o aviso com as datas livres. O financeiro recebe
  "Reembolso - reserva 1" com R$ 779,80 só quando há o que devolver: a NON_REFUNDABLE não o avisa.
- A reserva pendente cancelada não manda nada.
- O que já funcionava continua funcionando: preços, recusas, comprovante, relatórios, dados depois de reiniciar e as
  duas reservas simultâneas.

Para o dono da pousada, a governança e o financeiro passam a ser avisados do jeito que ele descreveu, sem perder nada
do que havia.

## Dúvidas

Na nota dos testes, fiquei entre `bom` e `com problemas`. Escolhi `com problemas` porque o número de hóspedes no
e-mail da governança é uma regra pedida, e a asserção dele (`ReservationServiceTest.java:105`) não confere nada. Fora
isso, os testes cobrem a parte nova com valores.
