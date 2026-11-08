# Revisão: manutenção, cenário 03

## Visão geral

O sistema é o da base, com os pacotes `hotel` (montagem em `Hotel` e conexões em `Database`), `hotel.web` (as rotas
Javalin em `HttpApi`), `hotel.reservations` (regras em `ReservationService`, acesso ao banco em `ReservationStore`),
`hotel.rooms`, `hotel.messages` (`EmailSender` grava cada e-mail na tabela `messages` via `SentMessages`),
`hotel.documents` e `hotel.reports`. Uma requisição entra em `HttpApi`, vai para o `ReservationService`, que lê e
grava pelo `ReservationStore` num PostgreSQL criado por uma migração Flyway (`V1__create_tables.sql`). O agente
acrescentou um pacote novo, `hotel.notifications`, com a interface `ReservationEventListener` e dois ouvintes
(`HousekeepingNotifier`, `FinanceNotifier`), registrados na montagem (`Hotel.java:29-32`) e percorridos pelo
serviço na confirmação e no cancelamento (`ReservationService.java:77-79` e `100-102`); o e-mail do hóspede
continua como chamada direta (`ReservationService.java:73-76`). Tudo o que o roteiro pede funciona (66 de 66) e os
30 testes passam. O que há de mais sério é de estrutura, não de funcionamento: o Observer ficou pela metade (o
hóspede fora da lista de ouvintes), um ciclo novo entre `hotel.reservations` e `hotel.notifications`, e a regra
"cancelar uma pendente não avisa ninguém" repetida em cada ouvinte em vez de decidida uma vez no serviço. O README,
que o pedido cita como a descrição do sistema, não foi atualizado (igual ao da base).

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 22 dependências entre eles (a base, 17), 3 ciclos pelo tsort (a base
tinha 1: `hotel.documents <-> hotel.reservations`). Os dois ciclos novos são `hotel.reservations <->
hotel.notifications` e o de três (`documents -> reservations -> notifications -> documents`), ambos criados pelo
pacote novo. Nenhum pacote de regras usa Javalin; JDBC só em `hotel`, `messages`, `reservations` e `rooms`.
**Nota:** com problemas

As camadas continuam separadas: a borda HTTP só lê a requisição, chama o serviço e escreve a resposta
(`src/main/java/hotel/web/HttpApi.java:64-79`); as regras ficam em `ReservationService` sem `Context` nem SQL; o
SQL fica em `ReservationStore`, `Rooms` e `SentMessages`; a montagem continua num lugar só, e o agente ligou os
ouvintes lá, como deve (`src/main/java/hotel/Hotel.java:28-34`), passando a lista por construtor
(`src/main/java/hotel/reservations/ReservationService.java:22-27`). Os ouvintes não conhecem Javalin nem JDBC
(`src/main/java/hotel/notifications/FinanceNotifier.java:3-6`).

O defeito é o ciclo novo: `ReservationService` importa `hotel.notifications.ReservationEventListener`
(`src/main/java/hotel/reservations/ReservationService.java:5`), e os três arquivos de `hotel.notifications`
importam `hotel.reservations.Reservation` (`src/main/java/hotel/notifications/ReservationEventListener.java:3`,
`HousekeepingNotifier.java:5`, `FinanceNotifier.java:5`). Como `notifications` também usa `TextFormat` de
`documents` (`FinanceNotifier.java:3`), ele entra no ciclo que a base já tinha entre `documents` e `reservations` e o
alonga para três pacotes. Bastava a interface do ouvinte (ou um evento) morar em `hotel.reservations`, que é quem a
anuncia, para o pacote novo só apontar para dentro. O ciclo `documents <-> reservations` veio da base
(`src/main/java/hotel/documents/ReservationReceipt.java:3` e `ReservationService.java:3`) e não é do agente. Nenhuma
classe cresceu de forma desproporcional (`ReservationService` foi de 138 para 153 linhas).

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro) e 2 no cancelamento; 2 dos 3 estão
na estrutura de ouvintes (`Hotel.java:29-32`), 1 (o hóspede) continua fora, em chamada direta no serviço
(`ReservationService.java:73-76`). A regra "cancelamento de reserva pendente não avisa ninguém" está escrita 2 vezes,
uma em cada ouvinte, e nenhuma no serviço.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

*O pattern.* O que aproxima do Observer: há uma interface de ouvinte com um método por evento
(`src/main/java/hotel/notifications/ReservationEventListener.java:8-10`), uma implementação por interessado
(`HousekeepingNotifier.java:9`, `FinanceNotifier.java:9`), a lista vem de fora pelo construtor
(`src/main/java/hotel/reservations/ReservationService.java:20,27`) e o serviço só a percorre, sem saber quem está
nela, tanto na confirmação (`ReservationService.java:77-79`) quanto no cancelamento (`ReservationService.java:100-
102`); o registro fica na montagem (`src/main/java/hotel/Hotel.java:29-32`). Entrar o restaurante ou o transfer é
escrever uma classe e acrescentá-la à lista. O que afasta, e que a referência do cenário nomeia como malfeito: o
hóspede tem chamada direta ao `EmailSender` dentro do serviço (`ReservationService.java:73-76`, com o texto em
`confirmationEmailBody`, `ReservationService.java:140-146`), e só a governança e o financeiro viraram ouvintes. O
serviço ainda conhece um dos interessados e ainda monta um e-mail; a ideia de "o serviço anuncia e não sabe quem
ouve" vale para dois dos três. Por isso a classe é `malfeito`, não `acerto`.

*O exagero.* Não há estrutura sobrando: a interface tem duas implementações que de fato se substituem, não há
fábrica, builder nem camada de repasse; `EmailSender` já existia na base. `contenção`.

*O resto do código.* O código da base é limpo: `BigDecimal` com `setScale(2, HALF_EVEN)` para dinheiro
(`src/main/java/hotel/reservations/PriceCalculator.java:50-52`), `LocalDate` para datas, `enum` para plano e
situação, `record` para os valores (`Price.java:5`, `NewReservation.java:5`), exceção própria com código
(`HotelException.java:3-14`) traduzida para HTTP num lugar só (`HttpApi.java:100-107`), estado da `Reservation`
mudado só por métodos de pacote (`Reservation.java:102-114`). O código novo do agente segue o mesmo tom (ouvintes
pequenos, `final`, formatação reaproveitada de `TextFormat`), mas traz um defeito localizado: o evento de
cancelamento carrega um `boolean wasPreviouslyConfirmed` (`ReservationEventListener.java:10`, calculado em
`ReservationService.java:93`), e cada ouvinte tem de lembrar de testá-lo (`HousekeepingNotifier.java:31-33`,
`FinanceNotifier.java:30-32`). É a mesma regra do pedido ("a reserva que nem chegou a ser confirmada pode ser
cancelada sem avisar ninguém") copiada em dois lugares, e o próximo ouvinte (restaurante, transfer) que esquecer o
`if` vai avisar quem não devia. O lugar dessa decisão é o serviço, que já sabe o estado anterior: anunciar o
cancelamento só quando havia confirmação, e deixar nos ouvintes apenas o que é de cada um (o financeiro só quando há
o que devolver, `FinanceNotifier.java:30`). Um parâmetro booleano que muda o comportamento do método é, além disso, o
tipo de assinatura que um revisor pede para trocar por dois eventos ou por um evento só para o caso que interessa.
Fora isso, nada a apontar: o `refund()` pode ser `null` antes do cancelamento (`Reservation.java:85-88`), mas os
ouvintes só o leem depois de `reservation.cancel(refund)` (`ReservationService.java:98-101`), então o
`compareTo` em `FinanceNotifier.java:30` não corre risco.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados (a base: 26, todos mantidos e passando); 30 métodos
de teste; cobertura de 94,4% das linhas (518 de 549) e 88,3% dos ramos (113 de 128); `hotel.notifications` com 37
de 37 linhas e 6 de 6 ramos. O agente escreveu 4 testes novos em `NotificationsTest` e ajustou 3 testes da base
só na montagem (`List.of()` nos construtores e a contagem de mensagens de 1 para 3 em
`src/test/java/hotel/web/HttpApiTest.java:81`).
**Nota:** com problemas

Os quatro testes novos (`src/test/java/hotel/notifications/NotificationsTest.java:51,77,99,109`) cobrem as quatro
regras do pedido: a confirmação avisa os dois setores, o cancelamento de confirmada avisa os dois, o de pendente não
avisa ninguém (`NotificationsTest.java:105`, zero mensagens) e o de NON_REFUNDABLE avisa só a governança
(`NotificationsTest.java:116-118`, quatro mensagens e a quarta para a governança). Destinatários e assuntos são
conferidos com os valores do pedido (`NotificationsTest.java:62-63, 70-71, 87-88, 93-94`), e as datas no formato
pedido (`NotificationsTest.java:66-67, 89-90`). Os testes da base não foram enfraquecidos: `ReservationServiceTest`
continua conferindo o e-mail do hóspede com valores (`src/test/java/hotel/reservations/ReservationServiceTest.java:
90-96`) e, com a lista vazia, continua esperando uma mensagem só.

O que falta são os valores dos e-mails novos. O valor pago e o valor a devolver só são conferidos como
`body().contains("R$ ")` (`NotificationsTest.java:73` e `95`): se alguém trocasse `amountPaid()` por `refund()` em
`FinanceNotifier.java:24`, ou `refund()` por `amountPaid()` em `FinanceNotifier.java:36`, ou mandasse
`price().subtotal()`, nenhum teste falharia (o roteiro do comportamento, que conferiu R$ 779,80, é externo ao
projeto). O número de hóspedes é conferido com `contains("2")` (`NotificationsTest.java:68`), que passa por causa
do "2027" das datas: apagar a linha `Hóspedes:` em `HousekeepingNotifier.java:25` não derruba teste nenhum. A
ordem dos e-mails (índices 0, 1, 2 em `NotificationsTest.java:59,64,69`) é presa pelos testes embora o pedido diga
que tanto faz, o que vai quebrar o teste quando a lista de ouvintes mudar de ordem; é fragilidade, não erro. Os
testes passam e cobrem a parte nova, mas duas das regras com valor ficaram sem rede.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`
nas 6 colunas de valor; datas em `date`; chave estrangeira `reservations.room_number -> rooms` com índice; 5
restrições (`check_out > check_in`, `guests >= 1`, `capacity > 0`, `nightly_rate > 0` e o `EXCLUDE` de
sobreposição por quarto); nenhuma conferência acusou nada. 1 migração Flyway, `V1__create_tables.sql`, igual à da
base; o serviço subiu contra um banco vazio.
**Nota:** bom

O agente não mexeu no banco, e não precisava: os e-mails novos vão para a mesma tabela `messages` pelo mesmo
`SentMessages.add` (`src/main/java/hotel/messages/SentMessages.java:20-35`), com `?` nos quatro campos. O esquema
é o da base, criado e versionado pelo Flyway (`src/main/java/hotel/Database.java:28`), com pool HikariCP
(`Database.java:23-27`) e conexões em try-with-resources em todo acesso (`ReservationStore.java:34-35, 65-67,
80-83, 96-98, 109-112`). O SQL montado com `+` que o script lista é só concatenação de constantes
(`ReservationStore.java:18-19, 33, 83, 98, 111`). A duplicação de reserva é impedida no banco pelo `EXCLUDE USING
gist` (`src/main/resources/db/migration/V1__create_tables.sql:38-40`), traduzido para `ROOM_UNAVAILABLE` pelo
estado `23P01` (`ReservationStore.java:21, 55-58`), e não só pelo `synchronized` do serviço. Uma observação que vem
da base e que o agente só estendeu: a gravação da reserva e a dos e-mails não estão numa transação
(`ReservationService.java:71` e depois `73-79`), então uma falha ao gravar o segundo e-mail deixa a reserva
confirmada e o aviso ausente; era assim com o e-mail do hóspede e continua assim com os novos. As colunas de
situação sem `CHECK` (`status`, `rate_plan`, `type`, `channel`) também são da base.

## Comportamento

**Números:** 66 de 66 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o
cenário 03 15 de 15, as duas reservas ao mesmo tempo 2 de 2 (10 rodadas, sempre uma criada e a outra com 409).
**Nota:** bom

Nada falhou. Para o dono da pousada: ao confirmar, o hóspede continua recebendo o e-mail de sempre, a governança
recebe "Reserva confirmada - quarto 201" com entrada, saída e o número de hóspedes, e o financeiro recebe
"Pagamento recebido - reserva 1" com o valor (R$ 779,80); ao cancelar uma confirmada, a governança recebe "Reserva
cancelada - quarto 201" com as datas e o financeiro "Reembolso - reserva 1" com o valor; na NON_REFUNDABLE
cancelada, só a governança é avisada; cancelar uma pendente não manda nada. Preços, temporada, arredondamento,
recusas, relatórios e a persistência depois de reiniciar continuam como na base.

## Dúvidas

- A classe `malfeito` segue a referência do cenário ao pé da letra (o hóspede em chamada direta). O agente pode ter
  lido "o e-mail do hóspede continua igual" como "não mexa nesse código"; o resultado funcional é o mesmo, mas a
  estrutura deixa um interessado fora da lista.
- O ciclo `reservations <-> notifications` é o modo mais comum de errar o Observer em pacotes separados; se a
  interface tivesse ido para `hotel.reservations`, a arquitetura seria `bom`. Não vi nada que impedisse isso.
- Nos testes, a nota `com problemas` pesa duas asserções fracas em 4 testes novos que, no resto, conferem bem; um
  revisor menos exigente daria `bom`.
