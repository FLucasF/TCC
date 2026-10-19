# Cenário 01

## Visão geral

As três regras de envio (bloquear endereço inválido, mandar cópia para o arquivo, desviar para a caixa de testes) foram escritas como `if`/`else` dentro de um único método, `MessageService.sendMessage`. Não há interface de envio, nem embrulhos, nem montagem por modo. O próprio serviço lê `HOTEL_TEST_MODE` e decide tudo ali dentro. Isso é a omissão que as instruções do pattern descrevem. As regras em si funcionam: o bloqueio, a cópia e o desvio de teste saíram como o enunciado pede. O resto do sistema é simples e sem estrutura sobrando. O problema mais sério de comportamento está na parte comum. A confirmação exige do site um corpo com `amountPaid` e recusa com 400 a chamada sem corpo. Além disso, grava o valor que o cliente mandou, e não o total da reserva. O dinheiro do comprovante também sai como `R$ 599.76`, quando deveria ser `R$ 599,76`.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` tem quatro subpacotes:
- `api/` guarda os DTOs de entrada e saída (`CreateReservationRequest`, `ConfirmReservationRequest`, `ReservationResponse`, `ReceiptResponse`, `MessageResponse`, `ErrorResponse`).
- `controller/` tem um controlador Javalin por recurso (`RoomController`, `ReservationController`, `MessageController`, `ReportController`).
- `domain/` tem as entidades e os enums (`Reservation`, `Room`, `Message`, `MessageStatus`, `RatePlan`…).
- `service/` traz as regras: `ReservationService` (estados, disponibilidade, cancelamento), `PriceCalculator` (estático, com as noites, a alta temporada e o desconto), `ReceiptGenerator` (estático, o texto do comprovante), `ReportService` (os CSVs), `RoomService` (os cinco quartos fixos), `MessageService` (a lista de mensagens) e `EmailValidator` (estático).

O caminho de uma requisição é sempre o mesmo. A rota registrada em `HotelApp.java` chama um método do controlador, que converte o JSON, chama o serviço e monta a resposta. Os erros sobem como `IllegalArgumentException` com o código na mensagem e viram status HTTP em `ReservationController.handleException` (`controller/ReservationController.java:236-260`). Os dados ficam em `HashMap`/`ArrayList` na memória de cada serviço.

A montagem é feita à mão no `main` (`HotelApp.java:9-21`), e a porta vem de `PORT`, com 7070 como padrão (`HotelApp.java:23-31`). O agente escreveu 46 testes JUnit. São testes de unidade de `EmailValidator`, `MessageService`, `PriceCalculator` e `ReservationService`, mais um `IntegrationTest` que, apesar do nome, exercita os serviços sem HTTP. Nenhum teste cobre o modo de teste.

**A parte do fim (as regras de envio).** Uma única classe cuida de tudo, `service/MessageService.java`:
- o construtor lê a variável de ambiente e guarda `testMode` (`service/MessageService.java:13`);
- `sendMessage` começa pelo `if (!EmailValidator.isValid(to))`, que grava a mensagem `BLOCKED` com o endereço original e retorna (`service/MessageService.java:17-21`);
- depois vem o `if (testMode)`, que monta a mensagem para `testes@…` com `[TESTE] ` e a linha "Destinatário original" (`service/MessageService.java:23-35`);
- o `else` grava a mensagem real e, logo em seguida, a cópia para `arquivo@…` com `[Cópia] ` (`service/MessageService.java:36-50`).

A validação de endereço fica em `service/EmailValidator.java:4-49`, com o único @ (`:18`), algo antes (`:25`), pelo menos um ponto depois (`:40`) e o ponto que não pode estar na ponta (`:44`). No `main`, o serviço é criado sem nenhuma configuração (`HotelApp.java:11`). O único lugar que chama o envio é a confirmação (`controller/ReservationController.java:109-113`).

**O raciocínio.** O pattern pede uma interface de envio, um envio real e um embrulho por regra, empilhados na montagem conforme o modo. Aqui não há nada disso: nem uma interface, nem uma peça por regra, nem uma corrente ou lista de filtros. As três regras são três ramos de `if` no mesmo método. A combinação por modo ("a cópia só fora do teste") está escrita como o `else` do `if (testMode)` (`service/MessageService.java:23,36`), e quem decide o modo é o próprio serviço lendo a variável (`:13`), não o `main`. Para acrescentar "não mandar de madrugada" ou "limite por dia" com as variantes sempre, só no teste e só fora dele, seria preciso abrir esse método e cruzar mais ramos. É exatamente o caso de "os três `if` ficam dentro do envio" que as instruções chamam de omissão. Separar a validação de endereço em `EmailValidator` não muda isso: é um auxiliar estático, não uma peça da estrutura de envio.

No exagero colateral não pesou nada. Os utilitários são estáticos, os serviços são concretos e não há interfaces de uma implementação só, fábricas nem camadas de repasse.

No comportamento pesou o contrário. A parte do fim confere, mas a confirmação da parte comum não segue o contrato descrito, e o comprovante formata o dinheiro errado.

## Item a item

**O enunciado (pede o pattern).** As três regras são `if`s no mesmo método. O bloqueio está em `service/MessageService.java:17-21`, o desvio de teste em `service/MessageService.java:23-35` e a cópia, no `else`, em `service/MessageService.java:36-50`. O modo é lido dentro do próprio serviço (`service/MessageService.java:13`), e `HotelApp.java:11` só faz `new MessageService()`, sem empilhar nada. Não há interface de envio, embrulho, corrente nem lista de regras. A combinação por modo está escrita no fluxo de controle do envio, o que é o oposto do Decorator, em que cada regra embrulha o envio e a montagem decide quais entram.
Conclusão: omissão

**O exagero colateral.** Não há. Os serviços são classes concretas criadas diretamente no `main` (`HotelApp.java:9-21`), e os cálculos ficam em utilitários estáticos (`service/PriceCalculator.java`, `service/ReceiptGenerator.java`). Não existe interface de implementação única, fábrica, builder nem camada que só repassa. O `MessageStatus` com um campo `code` igual ao nome (`domain/MessageStatus.java:3-15`) é redundante, mas trivial.
Conclusão: contenção

**O comportamento.** Subi o jar `target/hotel-system-1.0.0.jar` duas vezes: na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Criei três reservas, com `carla@gmail.com`, `ana@gmail` e `ana.gmail.com`, e confirmei as três.

Parte do fim:
- **Modo normal:** a mensagem para `carla@gmail.com` saiu como `SENT` (id 1). Logo depois veio a cópia para `arquivo@pousadamarealta.com.br`, com o assunto `[Cópia] Reserva confirmada - Pousada Maré Alta` e o mesmo texto, também `SENT` (id 2). `ana@gmail` e `ana.gmail.com` ficaram `BLOCKED`, com o endereço original e sem cópia. Tudo como esperado.
- **Modo de teste:** a mensagem foi para `testes@pousadamarealta.com.br`, com `[TESTE] ` no assunto e o texto começando por "Destinatário original: carla@gmail.com", sem cópia. Os dois endereços inválidos ficaram `BLOCKED` com o endereço original. Tudo como esperado.
- **Detalhe:** depois da linha "Destinatário original" entra uma linha em branco antes do texto (`service/MessageService.java:25`), embora o enunciado peça só "uma primeira linha a mais". Pesa pouco.
- **Campo `status`:** presente em toda mensagem.

Parte comum:
- **Preço:** STANDARD de 10/07/2027 a 12/07/2027 deu `subtotal 599.76` e `total 599.76`, como o enunciado. NON_REFUNDABLE de 2 noites DELUXE em março deu `779.80 − 116.97 = 662.83`, correto.
- **Recusas:** reserva sobreposta deu 409 `ROOM_UNAVAILABLE`, 3 hóspedes no 101 deu 422 `CAPACITY_EXCEEDED`, corpo que não é JSON deu 400 `INVALID_REQUEST` e reserva 99 deu 404 `RESERVATION_NOT_FOUND`. Tudo certo.
- **Check-out e segunda via:** devolvem o comprovante com os campos pedidos.
- **Relatórios:** o de ocupação de 2027-07-10 trouxe `101,STANDARD,1,…` e `total,1/5`, em `text/csv;charset=utf-8`. O financeiro de 2027-03 contou o NON_REFUNDABLE cancelado e deixou de fora o STANDARD devolvido. Certo.
- **Problema 1, a confirmação:** `POST /reservations/1/confirmation` sem corpo respondeu 400 `INVALID_REQUEST` ("No content to map…"), e com `{}` respondeu 400 "Missing amountPaid field" (`controller/ReservationController.java:88-94`). O enunciado diz que o site só faz o POST e que o pago é o total da reserva. Aqui o valor vem do cliente e é gravado como veio (`controller/ReservationController.java:96-97`): com `{"amountPaid":1}`, a reserva ficou com `amountPaid: 1` e o reembolso saiu 1. Pela mesma causa, confirmar uma reserva que não existe sem corpo dá 400 em vez de 404.
- **Problema 2, o dinheiro no comprovante:** sai `R$ 599.76`, `R$ 779.80` e `Desconto: R$ 116.97` (`service/ReceiptGenerator.java:12,32,38`, `DecimalFormat("0.00")`), em vez de `R$ 599,76`, e sem o separador de milhar que o enunciado mostra (`R$ 1.234,56`).

Testes do agente: `mvn -q verify` na cópia passou, com 46 testes, 0 falhas e 0 erros (EmailValidatorTest 12, IntegrationTest 5, MessageServiceTest 5, PriceCalculatorTest 10, ReservationServiceTest 14). Nenhum cobre o modo de teste. Os serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe: não existe nenhuma estrutura de regras de envio, então não há como confundir com `malfeito`.

No comportamento, o "em parte" vem da parte comum (a confirmação que exige `amountPaid` e o formato do dinheiro no comprovante). As regras da parte do fim conferem, com a ressalva menor da linha em branco depois de "Destinatário original".
