# Revisão: manutenção, cenário 03

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (rotas, `HttpApi`), `hotel.reservations` (regras em
`ReservationService`, acesso ao banco em `ReservationStore`), `hotel.rooms`, `hotel.messages` (e-mails guardados na
tabela `messages`), `hotel.documents` (comprovante, formatação) e `hotel.reports`, montados em `hotel.Hotel`; os dados
ficam no PostgreSQL, com o esquema criado pelo Flyway. Uma confirmação entra por `HttpApi`, passa por
`ReservationService.confirm`, que grava a reserva e agora percorre uma lista de `ReservationListener`
(`ReservationService.java:65`). O agente criou o pacote `hotel.notifications` com uma interface de ouvinte e três
implementações (hóspede, governança, financeiro), tirou o e-mail do hóspede de dentro do serviço e passou a anunciar
também o cancelamento (`ReservationService.java:87`). Tudo o que o roteiro confere passa (66 de 66) e os 30 testes
passam. O ponto mais sério é arquitetural e pequeno: a interface do ouvinte ficou no pacote novo e importa
`Reservation`, criando um ciclo novo entre `hotel.reservations` e `hotel.notifications`.

## Arquitetura

**Números:** 8 pacotes (a base tinha 7), 22 dependências entre eles (a base, 17); 3 ciclos pelo `tsort` na final
(`documents`↔`reservations`, `reservations`↔`notifications` e o triângulo dos três) contra 1 na base
(`documents`↔`reservations`). Maior classe continua `HttpApi` (170 linhas); `ReservationService` caiu de 138 para 130.
**Nota:** com problemas

O que está bom: a borda HTTP não mudou; o pacote novo não usa Javalin nem JDBC (tabela de bibliotecas de
`measures/dependencies.md`); o serviço recebe a lista de ouvintes de fora (`ReservationService.java:20`) e não sabe
quem são; a montagem continua em `Hotel.java:26`.

O defeito: a interface `ReservationListener` foi posta em `hotel.notifications` (`notifications/ReservationListener.java:3`
importa `hotel.reservations.Reservation`), e o serviço depende dela (`reservations/ReservationService.java:4`). Daí o
ciclo novo `reservations` ↔ `notifications`, que não existia na base. Bastaria a interface ficar em
`hotel.reservations`, junto de quem anuncia, para as dependências apontarem num sentido só
(`notifications → reservations`). O ciclo `documents` ↔ `reservations` veio da base
(`ReservationService.java:3`, `:102-118`, já usavam `ReservationReceipt`) e o agente não o piorou. Um detalhe: a lista de
quem é avisado e os endereços ficaram em `notifications/ReservationListeners.java:12-16`, e não na própria classe de
montagem; `Hotel.java:26` só chama `ReservationListeners.standard(...)`. Funciona como um segundo lugar de montagem,
aceitável neste tamanho.

## Design de baixo nível

**Números:** 3 interessados (hóspede, governança, financeiro), os 3 na estrutura e 0 fora dela; 2 eventos
(confirmação e cancelamento), os 2 anunciados pela lista; nenhum e-mail montado dentro do serviço; o caso que já existia
(o e-mail do hóspede) também virou ouvinte.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern: é o Observer como a referência descreve. Há uma interface de ouvinte com um método por evento e
implementações vazias por padrão (`notifications/ReservationListener.java:10-15`), para cada ouvinte tratar só o
que lhe interessa. Há uma implementação por interessado: `GuestNotice.java:20-30`, `HousekeepingNotice.java:20-39` e
`FinanceNotice.java:20-36`. O serviço só percorre a lista que recebeu (`ReservationService.java:65` e `:87`), e o
`confirmationEmailBody` e o `HOTEL_NAME` saíram do serviço. O e-mail do hóspede, que já existia, virou ouvinte com o
texto idêntico ao da base (`GuestNotice.java:22-29`), e não ficou chamada direta. Para o restaurante ou o transfer
entrarem, basta uma classe nova e uma linha em `ReservationListeners.java:13-16`, sem tocar no serviço. O que afasta
um pouco: a lista é fixa no código (`List.of`), mas o texto não pede trocar ouvintes em tempo de execução. Além disso,
o registro fica numa fábrica estática do pacote e não no `Hotel`, um desvio pequeno do "registrado na montagem".

Exagero: nenhum. A interface tem três implementações e é o próprio pattern; `ReservationListeners` é só a lista. Não
há fábrica, builder nem camada que só repasse.

O resto: o evento de cancelamento leva um `boolean wasConfirmed` (`ReservationListener.java:14`), e a regra "reserva
pendente cancelada não avisa ninguém" fica repetida em cada ouvinte (`HousekeepingNotice.java:31`,
`FinanceNotice.java:29`). Ali, o `!wasConfirmed` do financeiro é redundante, porque uma pendente tem devolução zero. É
um ponto pequeno, e deixa espaço para um ouvinte futuro que queira saber de pendentes canceladas. Dinheiro continua em
`BigDecimal`, formatado pelo `TextFormat` que já existia (`FinanceNotice.java:24`), e datas em `LocalDate`
(`HousekeepingNotice.java:24-25`). Há só detalhes de forma: uma linha longa em `Hotel.java:26` e espaçamento irregular
no teste (`ReservationServiceTest.java:100-101` e `:168-169`). Os ouvintes rodam depois do `store.update`, dentro do
`synchronized`. Se um falhar, a reserva já está confirmada e a chamada responde erro, mas a base já fazia o mesmo com o
e-mail do hóspede.

## Testes

**Números:** final: 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos; linhas 513/545 (94,1%), ramos
109/124 (87,9%); `hotel.notifications` 42/43 linhas e 6/6 ramos. Base: 26 testes, linhas 93,8%, ramos 87,1%.
**Nota:** bom

O agente manteve os 26 testes da base passando e não apagou nenhum. Três asserções mudaram de 1 para 3 mensagens
(`ReservationServiceTest.java:93`, `:175`, `HttpApiTest.java:81`), o que acompanha a mudança pedida e não a enfraquece.
O teste do e-mail do hóspede continua conferindo assunto e valores (`ReservationServiceTest.java:94-98`). Os 4 testes
novos cobrem todas as regras novas contra o banco real:
- confirmação com assunto, datas e número de hóspedes da governança, além do assunto e do valor do financeiro
  (`:111-125`);
- cancelamento de confirmada, com 2 avisos e os assuntos e datas certos (`:127-147`);
- NON_REFUNDABLE sem aviso ao financeiro (`:149-159`);
- pendente cancelada sem aviso a ninguém (`:161-168`).

A pergunta da mutação: apagar o aviso ao financeiro na confirmação, trocar `signum() == 0` por `!= 0`
(`FinanceNotice.java:29`) ou tirar o `!wasConfirmed` da governança (`HousekeepingNotice.java:31`) derruba algum teste.
As fraquezas são pequenas:
- os valores em dinheiro são conferidos contra o que o próprio código calcula (`TextFormat.money(cancelled.refund())`,
  `:146`; `reservation.price().total()`, `:124`), não contra um literal como "R$ 779,80";
- as datas são conferidas com `contains`, e uma troca entre entrada e saída passaria (`:119-120`);
- `confirmingSendsTheReceiptByEmail` supõe que o hóspede é a primeira mensagem (`:94-95`), o que depende da ordem de
  `ReservationListeners.java:13`, embora o texto diga que a ordem não importa.

Nenhum teste novo pela API, mas o `HttpApiTest` já cobria o caminho.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
datas em `date`; FK `reservations.room_number` com índice; `EXCLUDE` contra reserva dupla; 1 migração Flyway (`V1`),
igual à da base; as conferências só acusam `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK.
**Nota:** bom

O agente não tocou no banco: `V1__create_tables.sql` é igual ao da base (o script confirma) e não há migração nova,
porque o pedido não exige. As colunas de situação sem CHECK (`V1__create_tables.sql:28`, `:33`) vêm da base. O código
novo grava os avisos pelo `SentMessages.add` que já existia, com SQL parametrizado e try-with-resources
(`messages/SentMessages.java:24-38`). Uma observação: a confirmação agora faz um `UPDATE` e três `INSERT` em
`messages`, cada um na sua conexão e sem transação (`ReservationService.java:63-65`). Se uma gravação falhar no meio,
ficam avisos parciais. A base já tinha o mesmo padrão com um `INSERT`; o agente só multiplicou o número de gravações,
sem criar o problema.

## Comportamento

**Números:** 66 de 66 conferências passaram: parte comum 46/46, dados depois de reiniciar 3/3, cenário 03 15/15,
reservas simultâneas 2/2.
**Nota:** bom

Para o dono da pousada, ao confirmar uma reserva:
- o hóspede continua recebendo o mesmo e-mail;
- a governança recebe "Reserva confirmada - quarto 201", com 10/03/2027, 12/03/2027 e 2 hóspedes;
- o financeiro recebe "Pagamento recebido - reserva 1", com R$ 779,80.

Ao cancelar uma confirmada, a governança recebe as datas liberadas e o financeiro recebe "Reembolso - reserva 1" com
R$ 779,80. Numa NON_REFUNDABLE, só a governança é avisada. Uma pendente cancelada não manda nada. Tudo o que já
funcionava (preços, recusas, check-in e check-out, relatórios, dados depois de reiniciar, nenhuma reserva dupla)
continua igual.

## Dúvidas

O registro dos ouvintes ficou em `ReservationListeners.standard` (`notifications/ReservationListeners.java:12-16`) e
não diretamente em `Hotel`. Tratei isso como acerto, porque o serviço não conhece os ouvintes e a lista é montada fora
dele, chamada pela montagem (`Hotel.java:26`). Um leitor mais estrito da referência poderia ver aí um desvio do
"registrado na montagem do sistema", mas não chega a ser um notificador único, porque cada destino continua sendo um
ouvinte separado.
