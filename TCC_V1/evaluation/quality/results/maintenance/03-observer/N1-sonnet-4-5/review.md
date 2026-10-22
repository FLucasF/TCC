# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras e acesso
a dados das reservas: `ReservationService`, `ReservationStore`, `PriceCalculator`), `hotel.rooms`, `hotel.documents`
(comprovante e formatação), `hotel.messages` (o `EmailSender`, que grava os e-mails na tabela `messages`),
`hotel.reports` e `hotel` (a montagem, `Hotel`). Uma requisição entra em `HttpApi`, chama `ReservationService`, que
usa `ReservationStore` (JDBC) e grava no PostgreSQL, com o esquema criado por Flyway. O agente fez uma mudança pequena
e limpa: criou a interface `ReservationListener` com `onConfirmation`/`onCancellation`, dois ouvintes
(`HousekeepingNotifier`, `FinanceNotifier`), passou a lista ao `ReservationService` pelo construtor e a montou em
`Hotel`. Tudo o que o dono pediu funciona (66 de 66 conferências) e os 29 testes passam. O mais sério é de design: o
e-mail do hóspede continuou como chamada direta dentro de `ReservationService.confirm`
(`src/main/java/hotel/reservations/ReservationService.java:73-76`), fora da lista de ouvintes, e o evento de
cancelamento não diz se a reserva estava confirmada, o que obriga cada ouvinte a deduzir isso de outro jeito.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`); na base, os
mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.reservations` passou de 13 classes/508 linhas para
16/620.
**Nota:** bom

O agente não mudou o grafo de pacotes. O ciclo vem da base (`src/main/java/hotel/documents/ReservationReceipt.java:3`
importa `Reservation`; `src/main/java/hotel/reservations/ReservationService.java:3` importa `ReservationReceipt`); os
dois ouvintes novos acrescentam mais duas referências no mesmo sentido (`FinanceNotifier.java:3` e
`HousekeepingNotifier.java:3` importam `hotel.documents.TextFormat`), sem ciclo novo. A montagem continua num lugar
só: os ouvintes são criados e ligados em `src/main/java/hotel/Hotel.java:27-33`, e o serviço os recebe pelo
construtor (`ReservationService.java:21-32`), sem `new` por dentro nem `static`. A borda HTTP não foi tocada e não
ganhou regra; os ouvintes não conhecem Javalin nem SQL. Uma ressalva pequena: os textos dos e-mails da governança e do
financeiro ficaram no pacote de domínio `hotel.reservations`; um pacote próprio de avisos deixaria esse pacote, já o
maior, mais enxuto quando entrarem o restaurante e o transfer. Não chega a pedir mudança antes de aceitar.

## Design de baixo nível

**Números:** 3 interessados na confirmação (hóspede, governança, financeiro): 2 na estrutura de ouvintes, 1 fora (o
hóspede, chamado direto no serviço). 2 interessados no cancelamento: 2 na estrutura. 1 interface, 2 implementações.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

O que aproxima do Observer: há uma interface de ouvinte (`src/main/java/hotel/reservations/ReservationListener.java:6-13`)
com uma implementação por interessado (`HousekeepingNotifier`, `FinanceNotifier`), o serviço percorre a lista nos dois
eventos (`ReservationService.java:77-79` e `:100-102`) sem saber quem está nela, e a lista é registrada na montagem
(`Hotel.java:33`). Acrescentar o restaurante ou o transfer é uma classe nova e uma linha em `Hotel`. O que afasta: o
e-mail do hóspede, o interessado que já existia na base, continua montado e enviado à mão dentro do serviço
(`ReservationService.java:72-76`, com o texto em `confirmationEmailBody`, `:140-146`); o serviço ainda conhece um
destinatário e ainda depende de `EmailSender` e de `ReservationReceipt` só por causa dele. Pela referência do
cenário, "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes" é malfeito.

Exagero: nenhum. A interface tem duas implementações reais (e terá mais), e não há fábrica, builder nem camada que
só repassa.

O resto do código novo tem defeitos localizados:

- O evento de cancelamento não leva a situação anterior da reserva, e cada ouvinte deduz "estava confirmada" de um
  jeito: a governança pelo `amountPaid > 0` (`HousekeepingNotifier.java:34`), o financeiro pela devolução diferente de
  zero (`FinanceNotifier.java:30`). A regra do dono ("a reserva que nem chegou a ser confirmada pode ser cancelada sem
  avisar ninguém") fica implícita e repetida em cada ouvinte; o próximo (o restaurante, o transfer) vai ter de
  descobrir esse truque, e o financeiro só acerta o caso da pendente porque a devolução de uma pendente é zero. O
  serviço sabe a situação anterior (`ReservationService.java:89-90`) e poderia publicar isso no evento.
- Verificações mortas: `HousekeepingNotifier.java:31` confere se a reserva está `CANCELLED` dentro de
  `onCancellation`, que só é chamado depois de `reservation.cancel` (`ReservationService.java:97`); o `refund() == null`
  de `FinanceNotifier.java:30` nunca é verdadeiro ali pelo mesmo motivo.
- Os endereços da governança e do financeiro são constantes nas classes (`HousekeepingNotifier.java:10`,
  `FinanceNotifier.java:11`); aceitável para o tamanho, mas o dono diz que quem é avisado muda com o tempo.
- O `ReservationService` passou a receber `List<ReservationListener>` sem cópia defensiva (`ReservationService.java:31`);
  em `Hotel` vem de `List.of`, então não é risco hoje.

Nenhum desses quebra o sistema; os tipos (`BigDecimal`, `LocalDate`, `TextFormat` da base para R$ e datas) estão
certos e reaproveitados.

## Testes

**Números:** 29 testes rodaram, 0 falhas, 0 erros, 0 pulados; 29 métodos escritos (a base tinha 26). Cobertura de
linhas 94,2% (521/553; base 93,8%), de ramos 86,9% (113/130; base 87,1%). `hotel.reservations`: 257/271 linhas,
61/66 ramos.
**Nota:** bom

O agente acrescentou três testes em `src/test/java/hotel/reservations/ReservationServiceTest.java` e reforçou dois:

- confirmação (`:88-112`): além do hóspede, confere destinatário, assunto, as duas datas e "Hóspedes: 1" da
  governança, e destinatário, assunto e "R$ 212,42" do financeiro;
- cancelamento de confirmada (`:179-197`): assunto e datas da governança, assunto e "R$ 779,80" do reembolso;
- pendente cancelada não avisa ninguém (`:200-206`, `assertEquals(0, ...)`);
- `NON_REFUNDABLE` cancelada avisa só a governança (`:209-220`);
- pela API, `src/test/java/hotel/web/HttpApiTest.java:81-84` confere os três destinatários da confirmação.

A pergunta da mutação: apagar o teste do `amountPaid > 0` em `HousekeepingNotifier.java:34` faz falhar `:205`; apagar
o teste de devolução zero em `FinanceNotifier.java:30` faz falhar `:217`; trocar o valor pago pelo total sem desconto
ou pela devolução faz falhar `:111`/`:196`. As regras novas têm rede. Os testes da base continuam passando; os que
mudaram (`:95`, `:120`, `HttpApiTest.java:81`) só tiveram a contagem de e-mails ajustada de 1 para 3, sem perder
asserção. Ressalva pequena: os testes dependem da ordem dos e-mails (`sent.get(1)`, `sent.get(2)`), que o dono disse
não importar; mudar a ordem dos ouvintes em `Hotel` quebra testes sem quebrar regra.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária; dinheiro em `numeric(10,2)`;
nenhuma chave estrangeira sem índice; nenhuma data em texto; `EXCLUDE` contra duas reservas na mesma noite. Acusado:
as colunas de situação/tipo (`reservations.status`, `reservations.rate_plan`, `rooms.type`, `messages.channel`) sem
CHECK. Migração `V1__create_tables.sql` igual à da base; nenhuma migração nova.
**Nota:** bom

O pedido não exigia mudança no esquema, e o agente não mexeu nele nem no código de acesso a dados: a migração da base
está intacta (`src/main/resources/db/migration/V1__create_tables.sql`), e os avisos novos usam o mesmo `EmailSender`
→ `SentMessages` da base, com SQL parametrizado. A falta de CHECK nas colunas de situação veio da base. A confirmação
grava a reserva e depois os e-mails sem transação (`ReservationService.java:70-79`), como a base já fazia com o
e-mail do hóspede; o agente estendeu esse padrão a mais dois inserts (e acrescentou o mesmo no cancelamento, `:98-102`),
mas não criou o problema.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 03 15/15,
reservas simultâneas 2/2.
**Nota:** bom

Tudo o que o dono pediu acontece pela API: na confirmação, o hóspede recebe o mesmo e-mail de antes, a governança
recebe "Reserva confirmada - quarto 201" com 10/03/2027, 12/03/2027 e 2 hóspedes, e o financeiro "Pagamento recebido -
reserva 1" com R$ 779,80; no cancelamento de uma confirmada, a governança recebe "Reserva cancelada - quarto 201" com
as datas e o financeiro "Reembolso - reserva 1" com R$ 779,80; a `NON_REFUNDABLE` cancelada avisa só a governança; a
pendente cancelada não manda nada. Nada do que já funcionava quebrou.

## Dúvidas

A classe ficou entre `malfeito` e `acerto`: a estrutura de ouvintes está bem feita para os interessados novos, mas o
hóspede, que já existia, continua fora dela. A referência do cenário nomeia esse caso como malfeito, e foi o que pesou.
O README não foi atualizado com os avisos novos; não pesou em nenhuma coluna.
