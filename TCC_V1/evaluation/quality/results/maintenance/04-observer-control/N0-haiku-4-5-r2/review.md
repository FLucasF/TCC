# Revisão: manutenção, cenário 04

## Visão geral

A base é um serviço Javalin em sete pacotes. A borda HTTP fica em `hotel.web` (`HttpApi`), as regras em
`hotel.reservations` (`ReservationService`, `PriceCalculator`, `Reservation`), o comprovante em `hotel.documents`, os
e-mails guardados em `hotel.messages`, os relatórios em `hotel.reports`, os quartos em `hotel.rooms`, e a montagem em
`hotel.Hotel`. Uma requisição entra numa rota de `HttpApi`, vai ao `ReservationService`, e os dados vão ao PostgreSQL
pelas classes de acesso (`ReservationStore`, `SentMessages`, `Rooms`), com o esquema criado pelo Flyway
(`V1__create_tables.sql`). O pedido era uma mudança pontual: o assunto e três linhas no lugar de uma no e-mail de
confirmação. O agente fez só isso. O `diff -rq base final` mostra dois arquivos mudados: o assunto em
`src/main/java/hotel/reservations/ReservationService.java:67`, as linhas em `ReservationService.java:129-131` e a
asserção do assunto em `src/test/java/hotel/reservations/ReservationServiceTest.java:94`. Nenhuma estrutura nova. O
roteiro do comportamento passa por inteiro (58 de 58). O defeito mais sério é pequeno: nenhum teste confere as três
linhas novas do corpo do e-mail.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Fora a
contagem de linhas (`hotel.reservations` passou de 508 para 510), é idêntico a `measures/dependencies-base.md`.
**Nota:** bom

O agente não mexeu na arquitetura: mudou duas linhas de texto num método privado do serviço. A borda HTTP é fina, e as
rotas só leem a requisição, chamam o serviço e escrevem a resposta (`hotel/web/HttpApi.java:61-91`). As regras não
usam Javalin nem SQL (`hotel/reservations/ReservationService.java:1-7`). O JDBC fica nas classes de acesso
(`hotel/reservations/ReservationStore.java:34`, `hotel/messages/SentMessages.java`), e a montagem fica num lugar só
(`hotel/Hotel.java:190-199`). O ciclo vem da base: `ReservationService.java:3` importa `ReservationReceipt`, e
`hotel/documents/ReservationReceipt.java:3` importa `Reservation`. O agente não o criou nem o piorou.

## Design de baixo nível

**Números:** 1 arquivo de produção mudado e 4 linhas trocadas (1 no assunto, 1 linha do corpo trocada por 3). 0
classes, interfaces ou métodos novos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

É o controle negativo do cenário 04. A frase que tenta o agente é "talvez um dia a gente mande essa confirmação por
WhatsApp também". O agente não caiu nela. Não criou canal de mensagem abstrato, ouvinte de confirmação nem modelo de
mensagem: o `EmailSender` continua igual (`hotel/messages/EmailSender.java`). A mudança ficou onde o texto já morava,
no assunto passado a `emailSender.send` (`ReservationService.java:65-68`) e no `confirmationEmailBody`
(`ReservationService.java:125-133`). A saudação, o comprovante e o "Até breve!" ficaram intactos. É exatamente a
`contenção` que `references/controls.md` descreve.

O resto do código é o da base, e é bom. O dinheiro usa `BigDecimal`, arredondado a centavos meio para o par
(`PriceCalculator.java:50`). As datas são `LocalDate`. Situação e plano são `enum`. O estado da reserva só muda por
métodos de pacote (`Reservation.java:102-114`). Os erros têm código próprio, e as exceções de SQL são embrulhadas com a
causa (`ReservationStore.java:54-59`).

Há um detalhe do agente, pequeno demais para mudar a nota: o assunto repete o nome da pousada como texto
(`ReservationService.java:67`), quando a constante `HOTEL_NAME` (`ReservationService.java:12`) já existe e continua
sendo usada no corpo (`ReservationService.java:127`). A base montava o assunto com a constante. Se o nome mudar, o
assunto fica para trás. O `switch` sobre o plano aparece em dois lugares (`PriceCalculator.java:26` e
`ReservationService.java:82`), mas isso vem da base.

## Testes

**Números:** 26 testes rodaram, 0 falhas, 0 erros, 0 pulados; 26 métodos escritos (os mesmos da base). Cobertura de
linhas 471 de 502 (93,8%) e de ramos 101 de 116 (87,1%), igual à base (`measures/tests.md` e
`measures/tests-base.md`).
**Nota:** com problemas

O agente manteve os 26 testes passando e não apagou nem enfraqueceu nenhum. Atualizou a asserção do assunto para o
texto novo (`src/test/java/hotel/reservations/ReservationServiceTest.java:94`), e assim o assunto tem rede. Mas a maior
parte do pedido, as três linhas novas no lugar da antiga, ficou sem teste. O teste da confirmação só confere
`Desconto` e `Total` no corpo (`ReservationServiceTest.java:95-96`), e nenhum teste em `src/test` procura "Café",
"recepção", "14h" ou "Até breve". A pergunta da mutação:

- apagar a linha do café (`ReservationService.java:130`): nenhum teste falha;
- errar o telefone da recepção (`ReservationService.java:131`): nenhum teste falha;
- voltar a linha antiga do horário: nenhum teste falha.

A base já não testava a linha antiga, mas cabia ao agente testar o que ele mesmo mudou. Os testes rodam contra o
banco de verdade (4 arquivos) e pela API (1), sem mocks.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. 1 migração Flyway,
`V1__create_tables.sql`, igual à da base. Dinheiro em `numeric(10,2)` nas 6 colunas. Nenhuma chave estrangeira sem
índice. Nenhuma data como texto. As colunas de situação, plano, tipo e canal são `text` sem CHECK.
**Nota:** bom

O agente não mexeu no banco: nenhuma migração editada nem acrescentada, e o pedido não exigia nada disso. O esquema da
base é sólido. A reserva duplicada é impedida pela restrição `EXCLUDE USING gist`
(`src/main/resources/db/migration/V1__create_tables.sql:38-39`), traduzida em `ROOM_UNAVAILABLE`
(`ReservationStore.java:21`, `:55`). O SQL usa parâmetros (`?`), e as concatenações que o script aponta só juntam
constantes (`ReservationStore.java:98`, `:111`). As conexões vêm de um pool Hikari, com try-with-resources.

Três pontos fracos vieram da base, e o agente não os piorou:
- a confirmação grava a reserva e a mensagem em duas conexões, sem transação (`ReservationService.java:63-68`);
- os relatórios leem a tabela inteira de reservas para filtrar em Java (`hotel/reports/OccupancyReport.java:25`,
  `hotel/reports/RevenueReport.java:25`);
- falta CHECK nas colunas de situação.

## Comportamento

**Números:** 58 de 58 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 04 7 de 7,
reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Para o dono da pousada, o hóspede recebe o e-mail com o assunto novo, "Sua reserva na Pousada Maré Alta está
confirmada!". O corpo traz as três linhas novas, em ordem, no lugar da antiga, e a linha antiga sumiu. A saudação
"Olá, Carla Dias!", o comprovante com o total e o "Até breve!" no fim continuam lá. Todo o resto continua funcionando
como antes: preços, recusas, cancelamentos, relatórios, os dados depois de reiniciar e as duas reservas simultâneas
(só uma é criada em cada uma das 10 rodadas).

## Dúvidas

Nenhuma sobre as notas. A única hesitação foi o nome da pousada repetido como texto no assunto
(`ReservationService.java:67`), em vez da constante `HOTEL_NAME`. Julguei que é detalhe e não pesa na nota do design.
