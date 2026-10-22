# Cenário 03

## Visão geral

O agente montou um Observer só pela metade. Existe uma interface de ouvinte (`ReservationEventListener`), um publicador com lista de inscritos (`EventPublisher`) e dois ouvintes, um da governança e um do financeiro, que são registrados no `main`. O serviço de reservas publica eventos de confirmação e de cancelamento. O ponto bom é que o serviço não conhece a governança nem o financeiro. O problema mais sério é que o e-mail do hóspede, que é o caso que já existia, continua sendo montado e gravado à mão dentro de `ReservationService.confirmReservation` (`service/ReservationService.java:76-87`), antes da publicação do evento. Esse é exatamente o "hóspede com chamada direta, e só a governança e o financeiro viraram ouvintes" que as instruções classificam como malfeito. Há um agravante: os dois ouvintes só repassam o trabalho para um `NotificationService` único, que conhece os dois destinos e monta todos os e-mails. Na leitura do código também apareceram desvios de comportamento fora do pattern, como cancelamento permitido depois do check-in, valor pago vindo do cliente e relatório de ocupação contando reservas pendentes.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote é `com.pousadamarealta`, dividido assim:
- `domain`: `Reservation`, `Room`, `RoomType`, `RatePlan`, `ReservationStatus`, `Message` e `PricingCalculator`, que é estático e calcula a alta temporada, o desconto e o arredondamento `HALF_EVEN`.
- `repository`: repositórios em memória de quartos, reservas e mensagens. Os quartos ficam num `HashMap` preenchido no construtor (`repository/RoomRepository.java:12-18`).
- `service`: `ReservationService` (regras do ciclo de vida), `ReceiptService` (texto do comprovante e do e-mail do hóspede) e `ReportService` (os CSVs).
- `events`: o mecanismo de avisos.
- `api/dto`: os DTOs de saída.

Não existe camada de controller. Todas as rotas ficam em lambdas dentro de `Application.main` (`Application.java:42-181`). Cada lambda converte o corpo, chama o serviço ou o repositório direto e traduz para HTTP as `IllegalArgumentException` cuja mensagem é o código de erro, por meio de `handleError` (`Application.java:199-217`). A montagem também acontece no `main` (`Application.java:25-37`), e a porta vem de `PORT`, com 7070 como padrão (`Application.java:39`). Os testes são unitários, `PricingCalculatorTest` e `ReservationServiceTest`, e montam o serviço com um `EventPublisher` sem nenhum ouvinte (`test/.../ReservationServiceTest.java:30`). Por isso nenhum teste confere os e-mails da governança ou do financeiro. O único teste de mensagem verifica que existe alguma mensagem depois da confirmação (`ReservationServiceTest.java:122`).

**A parte do pattern.**
- `ReservationEvent` (`events/ReservationEvent.java:6-33`) carrega um enum `Type { CONFIRMED, CANCELLED }`, a reserva e o status anterior.
- `ReservationEventListener` (`events/ReservationEventListener.java:3-5`) tem um único método, `onReservationEvent`.
- `EventPublisher` (`events/EventPublisher.java:6-22`) guarda a lista, oferece `subscribe` e `unsubscribe` e percorre a lista em `publish`.
- `ReservationService` recebe o publicador pelo construtor (`service/ReservationService.java:20-27`). Ele publica `CONFIRMED` na confirmação (`ReservationService.java:90`) e `CANCELLED` no cancelamento, só quando a reserva não estava pendente (`ReservationService.java:148-150`).
- No `main` são inscritos `GovernanceListener` e `FinanceListener` (`Application.java:35-37`). Os dois recebem o mesmo `NotificationService`. Cada um olha o tipo do evento com `if/else` e chama o método certo do notificador (`events/GovernanceListener.java:14-21`, `events/FinanceListener.java:14-21`).
- Quem monta os e-mails da governança e do financeiro, com destinatário, assunto e corpo, é o `NotificationService` (`events/NotificationService.java:20-86`). É uma classe só, com um método por destino e por evento.
- O e-mail do hóspede não passa pelo publicador. `confirmReservation` gera o texto com o `ReceiptService` e grava a `Message` no `MessageRepository` diretamente (`ReservationService.java:76-87`). Por isso o serviço também depende do `MessageRepository` (`ReservationService.java:16`).

**O raciocínio.** O que aproxima o desenho do pattern: há uma interface de ouvinte, o serviço percorre uma lista que recebe de fora, os ouvintes são registrados na montagem e existe até `unsubscribe`. Isso atende ao pedido de que o restaurante e o transfer "entram e saem". O que afasta o desenho do pattern:
1. O hóspede não é ouvinte. O serviço continua mandando o e-mail dele à mão, e as instruções citam esse caso expressamente como malfeito.
2. Os "ouvintes" são cascas finas sobre um notificador único que conhece todos os destinos (`NotificationService.java:33,47,65,80`). Para pôr o restaurante, seria preciso mexer no `NotificationService` além de criar o ouvinte. A regra de cada interessado não mora no próprio ouvinte.

Como a estrutura existe e convive com o jeito sem ela, a classe é **malfeito**, e não omissão. Não vi exagero colateral. O `NotificationService` é um defeito do próprio desenho do pattern, não estrutura gratuita fora dele. Também não há interfaces de uma implementação só, nem fábricas, builders ou camadas que só repassam. No comportamento, os e-mails novos seguem o enunciado nos destinatários, nos assuntos e nas condições. As regras comuns do sistema têm desvios importantes, descritos no item abaixo.

## Item a item

**O pattern pedido.** Há uma interface de ouvinte (`events/ReservationEventListener.java:3-5`), um publicador com a lista de inscritos (`events/EventPublisher.java:7-21`) injetado no serviço (`service/ReservationService.java:20-27`), e a governança e o financeiro são registrados no `main` (`Application.java:36-37`). Na confirmação e no cancelamento, o serviço só publica o evento (`ReservationService.java:90`, `ReservationService.java:149`). Porém o e-mail do hóspede continua com chamada direta no serviço (`ReservationService.java:76-87`), e não como ouvinte. Além disso, os dois ouvintes delegam a um `NotificationService` que conhece os dois destinos e monta todos os e-mails (`events/NotificationService.java:20-86`). As instruções classificam como malfeito o caso em que "o hóspede tem chamada direta e só a governança e o financeiro viraram ouvintes".
Conclusão: malfeito

**O exagero colateral.** Não há. As classes fora da parte do pattern são diretas: repositórios em memória, `ReceiptService`, `ReportService`, `PricingCalculator` estático e rotas no `main`. Não existe interface com uma implementação só, fábrica, builder nem camada que só repassa. O `NotificationService` é um problema do desenho do pattern e já pesou no item acima.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

O que confere com o pedido:
- Confirmação: o hóspede recebe o e-mail com o assunto e o texto pedidos (`service/ReceiptService.java:39-50`). A governança recebe em `governanca@...`, com "Reserva confirmada - quarto N", as datas e o número de hóspedes (`events/NotificationService.java:20-38`). O financeiro recebe em `financeiro@...`, com "Pagamento recebido - reserva N" e o valor (`NotificationService.java:40-52`).
- Cancelamento de reserva confirmada: a governança recebe "Reserva cancelada - quarto N" com as datas (`NotificationService.java:54-70`). O financeiro recebe "Reembolso - reserva N" só quando o reembolso é maior que zero (`NotificationService.java:73`).
- Uma reserva pendente cancelada não publica nada (`ReservationService.java:148`).
- O preço tem alta temporada, desconto de 15% e arredondamento `HALF_EVEN` (`domain/PricingCalculator.java:12-58`).
- A disponibilidade ignora reservas canceladas (`domain/Reservation.java:132-134`).

Os desvios encontrados:
- **Cancelamento depois do check-in.** O sistema aceita cancelar reservas `CHECKED_IN` (`ReservationService.java:130-131`), com reembolso e avisos (`ReservationService.java:138`). O enunciado manda recusar com 409 `OPERATION_NOT_ALLOWED`.
- **Valor pago.** A confirmação exige um campo `amountPaid` no corpo (`Application.java:86-91`) e grava o que o cliente mandou (`ReservationService.java:74`). Pelo enunciado, o valor pago é o total da reserva e o contrato da API não prevê corpo. Uma confirmação sem esse campo é recusada com 400. Com um valor diferente do total, o reembolso e o relatório financeiro ficam errados. O e-mail do financeiro, por outro lado, usa `getTotal()` (`NotificationService.java:42`).
- **Relatório de ocupação.** Ele inclui reservas pendentes, porque `findByRoomAndDate` filtra só as canceladas (`service/ReportService.java:87`, `domain/Reservation.java:128-134`). O pedido é só das reservas pagas.
- **Relatório financeiro.** Ele lista todas as reservas que começam no mês, inclusive as pendentes e as canceladas com reembolso total, com valor 0,00 (`ReportService.java:108-117`). O pedido é só das que deixaram dinheiro com a pousada.
- **Tipo do conteúdo.** Os relatórios saem como `text/plain` (`Application.java:162,177`), e não como `text/csv; charset=utf-8`.
- **Formatação dependente do locale.** A formatação de dinheiro e do CSV usa `String.format` sem `Locale` (`NotificationService.java:89`, `ReceiptService.java:57`, `ReportService.java:117,120`). Funciona num JVM em inglês, mas inverte os separadores em `pt_BR`.
- **Diária em `double`.** A diária é convertida com `new BigDecimal(double)` (`PricingCalculator.java:28`), o que pode afetar algum empate de arredondamento.

O fluxo central (criar, confirmar, avisar, cancelar) funciona. As regras de cancelamento, de valor pago e de relatórios, porém, não seguem o pedido.
Conclusão: em parte

## Dúvidas

- Fiquei em dúvida entre malfeito e omissão por causa do `NotificationService`, um notificador único que conhece todos os destinos e que as instruções tratam como omissão. Escolhi malfeito porque a lista de ouvintes existe de verdade, é percorrida pelo serviço sem que ele saiba quem está nela e é montada no `main`. O defeito explícito que a evidência sustenta melhor é o e-mail do hóspede feito à mão no serviço (`ReservationService.java:76-87`).
- Não verifiquei em execução o que acontece numa confirmação sem corpo: o `bodyAsClass` pode lançar uma exceção que não é `IllegalArgumentException` e virar 500 em vez de 400. Isso não muda a conclusão do comportamento.
