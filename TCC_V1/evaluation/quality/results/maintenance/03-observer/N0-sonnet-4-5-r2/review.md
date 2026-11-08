# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin com pacotes por assunto. `hotel.web.HttpApi` recebe a requisição e chama
`hotel.reservations.ReservationService`, que guarda os dados no PostgreSQL por meio de `ReservationStore`, `Rooms` e
`SentMessages` (o esquema vem da migração Flyway `V1__create_tables.sql`). A montagem fica em `hotel.Hotel`. O agente
criou o pacote `hotel.notifications` com sete classes: dois eventos (`ReservationConfirmed` e `ReservationCancelled`,
debaixo de uma interface selada `ReservationEvent`), a interface `ReservationEventListener`, um
`ReservationEventPublisher` com a lista de ouvintes e dois ouvintes (`HousekeepingNotifier` e `FinanceNotifier`),
registrados em `Hotel.java:29-31`. O serviço publica os eventos em `ReservationService.java:78` e `:99`. O
comportamento está todo certo (66 de 66 conferências) e os 30 testes passam. Os problemas estão na estrutura. O
e-mail do hóspede continua sendo uma chamada direta dentro do serviço (`ReservationService.java:73-77`): só a
governança e o financeiro viraram ouvintes, o que a referência do pattern classifica como malfeito. Os eventos ficaram
num pacote que o serviço importa e que importa o serviço de volta, o que cria um ciclo novo entre `reservations` e
`notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 22 dependências entre pacotes (a base tinha 17). São 3 ciclos: o
`documents ↔ reservations`, que já vinha da base, e dois novos, `reservations ↔ notifications` e
`documents → reservations → notifications → documents`. Só `hotel` e `hotel.web` usam o Javalin; o JDBC fica em
`messages`, `reservations` e `rooms`. O pacote `hotel.notifications` não usa nenhuma biblioteca.
**Nota:** com problemas

As camadas continuam como na base. O agente não mexeu no `HttpApi`. As regras novas não conhecem nem o Javalin nem o
JDBC: os ouvintes só formatam texto e chamam `EmailSender.send` (`notifications/FinanceNotifier.java:31-45`,
`notifications/HousekeepingNotifier.java:30-48`). A montagem fica num lugar só, `Hotel.java:27-34`, que cria um único
`EmailSender`, registra os ouvintes e entrega o publicador ao serviço. Os testes repetem essa mesma montagem, como se
vê abaixo.

O defeito é o ciclo novo. Os eventos e a interface do ouvinte, que fazem parte do contrato do serviço, ficaram em
`hotel.notifications`. Por isso o serviço importa esse pacote (`reservations/ReservationService.java:5-7`), e o
pacote importa o serviço de volta (`notifications/ReservationConfirmed.java:3`,
`notifications/ReservationCancelled.java:3-4`, `notifications/FinanceNotifier.java:5`). Se os eventos e a interface
ficassem em `hotel.reservations`, quem publica, a dependência teria um sentido só
(`notifications → reservations`). Nenhuma classe cresceu demais: `ReservationService` passou de 138 para 150 linhas.

## Design de baixo nível

**Números:** na confirmação há 3 interessados. Dois estão na estrutura (governança e financeiro) e um continua fora
dela (o hóspede, por chamada direta). No cancelamento, os 2 interessados estão na estrutura. A estrutura tem 1
interface de ouvinte, 2 ouvintes, 1 publicador e 2 eventos.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

Aproxima do Observer: o serviço não sabe quem é a governança nem o financeiro. Ele só publica
(`ReservationService.java:78` e `:99`) num publicador que vem de fora pelo construtor (`ReservationService.java:28`),
e o publicador percorre a lista de ouvintes (`ReservationEventPublisher.java:15-19`). Cada interessado é uma classe
separada, e os ouvintes são registrados na montagem (`Hotel.java:29-31`). A confirmação e o cancelamento usam os dois
a mesma estrutura. Para pôr o restaurante ou o transfer, basta escrever um ouvinte e acrescentar uma linha em
`Hotel`, sem tocar no serviço.

Afasta do Observer: o hóspede, o interessado que já existia, não virou ouvinte. `ReservationService.confirm` ainda
monta o comprovante e manda o e-mail ele mesmo (`ReservationService.java:73-77`, com o texto em `:137-143`) e por
isso continua recebendo o `EmailSender` (`ReservationService.java:20` e `:27`). A referência descreve esse caso
literalmente como malfeito: "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes". Na
prática, o sistema tem duas formas de avisar alguém, e quem quiser mudar o e-mail do hóspede (ou tirá-lo, ou mandar
por outro canal) ainda precisa mexer no serviço.

Exagero: não há. A interface selada com os dois eventos, a interface do ouvinte, o publicador e os dois ouvintes
fazem parte do pattern. O segundo construtor de `ReservationCancelled` (`ReservationCancelled.java:9-11`), que
converte a situação anterior em `boolean`, é pequeno e não chega a ser estrutura sem necessidade.

O resto do código, ou seja, a nota:
- **A regra repetida.** A regra "a reserva pendente cancelada não avisa ninguém" está copiada em cada ouvinte
  (`HousekeepingNotifier.java:23` e `FinanceNotifier.java:24`, o `if (cancelled.wasConfirmed())`). O serviço publica
  `ReservationCancelled` até para uma reserva pendente (`ReservationService.java:99`). Assim, cada ouvinte que vier
  (o restaurante, o transfer) precisa lembrar de repetir esse filtro, senão avisa por engano. Um evento que só saísse
  quando a reserva já estava confirmada, ou dois eventos distintos, poria a regra num lugar só.
- **A montagem copiada.** A montagem dos ouvintes foi copiada para três classes de teste
  (`ReportsTest.java:35-40`, `ReservationServiceTest.java:33-38`, `NotificationTest.java:36-41`), além de `Hotel`.
  Cada ouvinte novo terá de ser acrescentado em quatro lugares, ou os testes vão divergir da produção.
- **Sem como sair.** O texto diz que o restaurante e o transfer "entram e saem", mas o publicador só tem `register`
  (`ReservationEventPublisher.java:11`). Como o registro acontece na montagem, isso é aceitável; só fica registrado
  aqui.
- **O que está certo.** Os tipos estão corretos: dinheiro em `BigDecimal` formatado por `TextFormat.money`, datas em
  `LocalDate` formatadas por `TextFormat.date`. O `switch` sobre a interface selada é exaustivo
  (`FinanceNotifier.java:21-28`). O teste de reembolso cuida do `null` (`FinanceNotifier.java:47-50`).

## Testes

**Números:** 30 testes rodaram, nenhum falhou e nenhum foi pulado, de 30 métodos escritos (a base tinha 26; o agente
acrescentou 4 em `NotificationTest`). Cobertura de linhas de 94,6% (538 de 569; a base tinha 93,8%) e de ramos de
88,1% (118 de 134; a base tinha 87,1%). O pacote `hotel.notifications` tem 59 de 59 linhas e 17 de 18 ramos cobertos.
**Nota:** com problemas

O agente testou a parte nova contra o banco de verdade, nos quatro casos que o texto pede: a confirmação, o
cancelamento com reembolso, o cancelamento NON_REFUNDABLE sem aviso ao financeiro (conferido pela contagem
`messagesBeforeCancel + 1`, `NotificationTest.java:118`) e o cancelamento de reserva pendente sem nenhum aviso
(`NotificationTest.java:135`). Os assuntos, as datas e o número de hóspedes da governança são conferidos com valores
(`NotificationTest.java:66-69` e `:96-98`). Os testes da base continuam passando. As mudanças neles só acompanham as
mensagens novas: a contagem passou de 1 para 3 (`ReservationServiceTest.java:103` e `:120`, `HttpApiTest.java:81`), e
o e-mail do hóspede agora é localizado pelo destinatário em vez da posição na lista. O conteúdo do e-mail do hóspede
continua conferido (`ReservationServiceTest.java:108-111`), e nenhuma conferência ficou mais fraca.

A falha está no dinheiro. Os dois e-mails do financeiro só conferem que o corpo contém `"R$ "`
(`NotificationTest.java:77` e `:106`). Feita a pergunta da mutação: se `notifyPayment` escrevesse
`reservation.price().subtotal()` em vez de `amountPaid()` (`FinanceNotifier.java:35`), ou se `notifyRefund` escrevesse
o valor pago em vez de `refund()` (`FinanceNotifier.java:43`), nenhum teste falharia. O valor, que é exatamente o que o
financeiro precisa ver, fica sem rede. A troca de `> 0` por `>= 0` em `hasRefund` (`FinanceNotifier.java:49`) seria
pega pelo teste da NON_REFUNDABLE. Nenhum teste confere que um ouvinte registrado depois também recebe os eventos,
mas o publicador é simples o bastante para isso pesar pouco.

## Banco

**Números:** 3 tabelas (`messages`, `reservations`, `rooms`), todas com chave primária. O dinheiro está em
`numeric(10,2)` e as datas em `date`; a chave estrangeira tem índice. O `EXCLUDE USING gist` impede que duas reservas
ativas do mesmo quarto se sobreponham. 1 migração Flyway, igual à da base. As conferências só acusaram as colunas de
situação e de tipo em `text` sem CHECK (`reservations.status`, `rate_plan`, `rooms.type`, `messages.channel`), o que
já vinha da base. O SQL montado com `+` só junta constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`).
**Nota:** bom

O pedido não exigia mudar o esquema, e o agente não mexeu nele: `V1__create_tables.sql` está igual. Os avisos novos
são gravados na tabela `messages` pelo mesmo `EmailSender`/`SentMessages` da base. O roteiro confirma que as
mensagens continuam lá depois de reiniciar o serviço. Um detalhe que vem da base e que o agente ampliou sem piorar a
natureza do problema: a confirmação grava a reserva (`ReservationService.java:72`) e depois as mensagens, agora três
em vez de uma, sem transação (o script não acha nenhum `setAutoCommit(false)`). Se um desses `INSERT` falhar, a
reserva fica confirmada sem todos os avisos. A base já tinha esse defeito com uma mensagem só, por isso ele não pesa
na nota do agente.

## Comportamento

**Números:** 66 de 66 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 15 de 15 no cenário
03 e 2 de 2 nas reservas simultâneas.
**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API. Na confirmação, o hóspede continua recebendo o mesmo
e-mail. A governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes. O financeiro
recebe "Pagamento recebido - reserva 1", com R$ 779,80. No cancelamento de uma reserva confirmada, a governança
recebe "Reserva cancelada - quarto 201", com as datas, e o financeiro recebe "Reembolso - reserva 1", com R$ 779,80.
Na NON_REFUNDABLE, só a governança é avisada. Uma reserva pendente cancelada não manda nada. O que já funcionava
(preços, recusas, check-in e check-out, relatórios, dados depois de reiniciar, reservas simultâneas) continua
funcionando.

## Dúvidas

- A nota de design de baixo nível ficou em `com problemas` por dois motivos: o filtro `wasConfirmed` repetido em cada
  ouvinte e a montagem dos ouvintes copiada nas três classes de teste. São defeitos localizados. Um revisor menos
  exigente poderia aceitá-los e dar `bom`.
- Se o hóspede deve ser ouvinte não depende de interpretação: a referência do cenário diz isso de forma explícita.
  Por isso a classe ficou em `malfeito`, embora o comportamento esteja todo certo.
