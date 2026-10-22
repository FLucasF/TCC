# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas, `HttpApi`), `hotel.reservations` (o
`ReservationService` com as regras e o `ReservationStore` com o JDBC), `hotel.rooms`, `hotel.messages` (o
`EmailSender` grava os e-mails na tabela `messages`), `hotel.documents` e `hotel.reports`. A classe `Hotel` monta
tudo. Os dados ficam no PostgreSQL, com uma migração do Flyway. A mudança é pequena e limpa. O agente criou um pacote
`hotel.notifications` com a interface `ReservationListener` (os eventos `confirmed` e `cancelledAfterConfirmation`)
e dois ouvintes, `HousekeepingNotices` e `FinanceNotices`. O `ReservationService` recebe a lista de ouvintes e a
percorre ao confirmar e ao cancelar uma reserva já confirmada (`final/src/main/java/hotel/reservations/ReservationService.java:77`,
`:98-100`). Os ouvintes são registrados em `Hotel` (`final/src/main/java/hotel/Hotel.java:34-36`). Tudo funciona:
as 66 conferências do roteiro passaram, e os 30 testes também. Há dois problemas. O e-mail do hóspede continua sendo
uma chamada direta dentro do serviço (`ReservationService.java:72-76`), em vez de ser um ouvinte como os outros. E o
fato de a interface morar em `notifications` criou um ciclo novo entre `reservations` e `notifications`.

## Arquitetura

**Números:** 8 pacotes (base: 7), 22 dependências entre eles (base: 17). Três ciclos acusados: `documents ↔
reservations`, que já vinha da base, mais `reservations ↔ notifications` e o ciclo de três que os dois formam juntos,
ambos novos.
**Nota:** com problemas

As camadas continuam como estavam na base. A borda HTTP não mudou. O pacote novo `hotel.notifications` não usa
Javalin nem JDBC (tabela de bibliotecas de `measures/dependencies.md`). A montagem continua num lugar só
(`Hotel.java:27-36`). O defeito é o ciclo novo: `ReservationService` importa `hotel.notifications.ReservationListener`
(`ReservationService.java:5`), e a interface importa `hotel.reservations.Reservation`
(`notifications/ReservationListener.java:3`), assim como os dois ouvintes (`FinanceNotices.java:5`,
`HousekeepingNotices.java:5`). A porta que o serviço anuncia pertence ao serviço. Com `ReservationListener` em
`hotel.reservations`, a dependência apontaria só de `notifications` para `reservations`, sem ciclo. O ciclo é pequeno
e fica entre dois pacotes vizinhos, mas é novo e vem de uma responsabilidade no lugar errado. O tamanho não é
problema: `ReservationService` passou de 138 para 151 linhas.

## Design de baixo nível

**Números:** 3 interessados na confirmação. Dois estão na estrutura (governança e financeiro, como ouvintes). O
hóspede ficou fora dela, numa chamada direta ao `EmailSender` dentro do serviço. No cancelamento, os 2 interessados
estão na estrutura.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** bom

O que aproxima do Observer: uma interface de ouvinte (`ReservationListener.java:9-18`) com uma implementação por
interessado (`HousekeepingNotices`, `FinanceNotices`). O serviço recebe a lista de fora (`ReservationService.java:27`,
`:32`, com `List.copyOf`) e a percorre sem saber quem está nela (`:77`, `:99`). O cancelamento também usa ouvintes, e
a regra "só avisa o financeiro se houver reembolso" mora no ouvinte (`FinanceNotices.java:28-30`), não no serviço. Para
pôr o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel.java:34-36`.

O que afasta: o e-mail do hóspede, o interessado que já existia na base, continua montado e enviado pelo próprio
serviço (`ReservationService.java:72-76`, com o texto em `confirmationEmailBody`, `:138-144`). A referência do pattern
chama isso de malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". Por isso o
serviço ainda depende de `EmailSender` e de `ReservationReceipt` (`:3-4`, `:19`). Os avisos ficam com duas formas
convivendo: uma pela lista e outra à mão.

Exagero: nenhum. A interface tem duas implementações e é o ponto de extensão que o texto pede. Os métodos `default`
vazios (`ReservationListener.java:12-17`) deixam cada ouvinte escolher os eventos que quer, sem cerimônia. Não há
fábrica, builder nem camada que só repassa.

Resto do código: os ouvintes são curtos e usam o `TextFormat` da base para dinheiro e datas (`FinanceNotices.java:23`,
`HousekeepingNotices.java:23-25`), sem reformatar à mão. O `wasConfirmed` é calculado antes de `cancel` mudar o
estado (`ReservationService.java:91`), o que está certo. A base já usava `BigDecimal`, `LocalDate` e `enum` nos lugares
certos. Há dois pontos menores. Uma exceção num ouvinte interrompe os seguintes e devolve erro com a reserva já
gravada (`ReservationService.java:71` e `:77`). E o comentário de `EmailSender` ainda diz "Sends e-mails to guests"
(`messages/EmailSender.java:3`), embora a classe agora mande e-mails também para a equipe.

## Testes

**Números:** 30 testes rodaram, 0 falharam (base: 26, todos mantidos). 30 métodos escritos, 4 deles novos, em
`ReservationNoticesTest`. Cobertura de linhas 93,9% (507/540; base 93,8%). Cobertura de ramos 87,7% (107/122; base
87,1%). `hotel.notifications`: 29 de 31 linhas e 2 de 2 ramos.
**Nota:** bom

Os quatro testes novos rodam contra o banco, com o serviço de verdade, e conferem a parte nova com valores. Na
confirmação, conferem 3 mensagens, o assunto "Reserva confirmada - quarto 201", as duas datas, o assunto do financeiro
e "R$ 779,80" (`notifications/ReservationNoticesTest.java:61-69`). No cancelamento com reembolso, conferem os dois
avisos com assunto, datas e valor (`:72-89`). No NON_REFUNDABLE, só a governança recebe aviso (`:91-102`). Na reserva
pendente cancelada, ninguém recebe (`:104-111`). O teste da API passou a esperar 3 mensagens depois da confirmação
(`web/HttpApiTest.java:81`). Isso é a regra nova, não um teste enfraquecido, e ele passa pela montagem real de `Hotel`
(`HttpApiTest.java:35`), o que também confere o registro dos ouvintes.

A pergunta da mutação: apagar o `if` do reembolso zero (`FinanceNotices.java:28-30`) quebra o teste da linha 100.
Trocar `wasConfirmed` por `true` quebra o teste da linha 110. Apagar a chamada a um dos ouvintes quebra os testes das
linhas 61 e 85. A única asserção fraca é a de quantos hóspedes vêm: `body().contains("2")` (`ReservationNoticesTest.java:66`)
passa só por causa das datas ("2027"). Se alguém apagasse a linha "Hóspedes" do e-mail, nenhum teste falharia.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`). 1 migração do Flyway, igual à da base. As conferências
acusaram só as colunas de situação e de tipo em `text` sem CHECK (`reservations.status`, `reservations.rate_plan`,
`rooms.type`, `messages.channel`), um ponto que vem da base. Dinheiro em `numeric(10,2)`, chave estrangeira com índice,
datas em `date`, e a sobreposição de reservas impedida por `EXCLUDE USING gist`.
**Nota:** bom

O pedido não precisava de mudança no esquema, e o agente não mexeu nele: a migração `V1__create_tables.sql` está
igual à da base, e os avisos usam a tabela `messages` que já existia, pelo `EmailSender`. O SQL montado com `+` em
`ReservationStore.java:33`, `:83`, `:98` e `:111` só concatena constantes (`COLUMNS`) e vem da base. Um ponto menor:
confirmar grava a reserva e depois cada e-mail em comandos separados, sem transação (`ReservationService.java:71-77`).
A base já fazia assim com o e-mail do hóspede, e agora são três gravações em vez de uma. Se uma falhar, a reserva fica
confirmada e falta algum aviso.

## Comportamento

**Números:** 66 de 66 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 03:
15 de 15. Reservas simultâneas: 2 de 2.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu acontece pela API. Quando uma reserva é confirmada, o hóspede recebe o
mesmo e-mail de antes, a governança recebe "Reserva confirmada - quarto 201" com as datas e o número de hóspedes, e o
financeiro recebe "Pagamento recebido - reserva 1" com R$ 779,80. Quando uma reserva confirmada é cancelada, a
governança recebe as datas liberadas e o financeiro recebe o reembolso. Se não houver reembolso, o financeiro não é
avisado. Uma reserva pendente cancelada não avisa ninguém. O que já funcionava continua funcionando.

## Dúvidas

A classe ficou entre `malfeito` e `acerto`. A estrutura está certa para os casos novos e para o cancelamento, mas o
hóspede, o caso que já existia, não virou ouvinte (`ReservationService.java:72-76`). A referência cita esse caso
literalmente como malfeito, e por isso a nota ficou nessa classe.
