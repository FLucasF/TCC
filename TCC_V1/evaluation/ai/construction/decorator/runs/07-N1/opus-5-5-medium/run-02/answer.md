# Cenário 07

## Visão geral

O agente entregou o sistema inteiro: quartos, reservas, ciclo de status, relatórios CSV e a lista de mensagens com o campo `status`. Mas as três regras de envio ficaram todas num único método, `MessageService.sendEmail`, como três `if` em sequência: o endereço inválido, o modo de teste e a cópia no `else` (`src/main/java/com/pousadamarealta/service/MessageService.java:25-44`). O próprio construtor do serviço lê `HOTEL_TEST_MODE` (`MessageService.java:21`). Não existe interface de envio, nem embrulho, nem montagem que escolha a combinação por modo. Isso é exatamente a omissão que as instruções descrevem. As regras de mensagem, isoladamente, estão quase todas corretas. O problema mais sério de comportamento está em outro lugar: a confirmação exige no corpo um `amountPaid` que o enunciado não prevê. Sem ele a chamada responde 400 e o e-mail nem chega a ser gerado. Além disso, o dinheiro sai com ponto decimal no comprovante do e-mail.

## Como foi implementado, e por que o julgamento

**O projeto.** O projeto tem um pacote `domain` com classes simples (`Reservation`, `Room`, `Receipt`, `Message`, `MessageStatus`, `MessageChannel`, `RatePlan`, `ReservationStatus`). O pacote `service` tem `RoomService` (os cinco quartos fixos em memória, `RoomService.java:18-24`), `ReservationService` (criação, transições, comprovante e texto do e-mail), `ReportService` (os dois CSV) e `MessageService` (a lista de mensagens e o envio). No pacote `util` ficam `PriceCalculator`, `MoneyUtil` (HALF_EVEN, `MoneyUtil.java:6`), `DateUtil` e `EmailValidator`. Na camada `api`, `ApiRoutes` registra as rotas, faz o parse com Gson, valida, traduz exceções em códigos de erro e serializa à mão para `Map`. `Application.main` lê `PORT`, cria os serviços com `new`, configura o Javalin com Gson e registra as rotas (`Application.java:16-50`). Tudo fica em `ArrayList` na memória.

O caminho de uma confirmação é este: `ApiRoutes.confirmReservation` (`ApiRoutes.java:150`) chama `ReservationService.confirmReservation` (`ReservationService.java:72`), que monta o comprovante e o texto e chama `messageService.sendEmail(...)` (`ReservationService.java:92`). Os testes do agente são unitários: `MessageServiceTest`, `RoomServiceTest` e os de `util`. Não há teste de rota nem do modo de teste.

**A parte que pede o pattern.** Existe uma só classe, `MessageService`. O método `sendEmail` faz tudo:
- bloqueia, se `!EmailValidator.isValid(to)`, gravando `BLOCKED` com o endereço original (`MessageService.java:26-29`);
- se `testMode`, troca o destinatário, o assunto e o corpo (`MessageService.java:31-37`);
- senão, grava o envio e a cópia para o arquivo (`MessageService.java:38-43`).

O modo é um campo `boolean` lido do ambiente dentro do próprio serviço (`MessageService.java:21`). A combinação "cópia só fora do teste" é a própria estrutura `if/else`. `Application.java:34` só faz `new MessageService()`, sem decidir nada.

**O raciocínio.** As instruções pedem que cada regra seja um embrulho, ou uma peça própria numa corrente ou lista, e que a montagem empilhe as peças conforme o modo. Aqui não há peça nenhuma: as três regras são `if` dentro do envio, e a combinação por modo está escrita dentro do envio. Para acrescentar "não mandar de madrugada" valendo só no teste, ou para tirar a cópia na segunda pousada, seria preciso editar esse método e criar mais flags. É a definição de **omissão**. O `EmailValidator` separado é só uma função utilitária de validação, não uma peça do envio.

Exagero colateral não há: as classes são diretas e não aparecem interfaces, fábricas ou camadas que só repassam. No comportamento pesaram três coisas: a confirmação que depende de um campo não pedido, a formatação monetária errada no texto do e-mail e uma validação de endereço um pouco frouxa.

## Item a item

**O pattern pedido.** O endereço, o modo de teste e a cópia são três ramos de `if` dentro de `MessageService.sendEmail` (`src/main/java/com/pousadamarealta/service/MessageService.java:26`, `:31`, `:38-43`). O modo é lido no construtor do próprio serviço (`MessageService.java:21`), e a montagem não compõe nada (`src/main/java/com/pousadamarealta/Application.java:34`). Não há interface de envio, embrulho, corrente nem lista de regras. Por isso as regras novas e as diferenças entre instalações não podem ser combinadas sem mexer no envio. É o caso previsto como omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Os serviços são classes concretas, únicas e usadas diretamente (`Application.java:32-35`). Não aparecem interfaces com uma implementação só, fábricas, builders ou camadas que só repassam.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço, conforme a orientação desta correção.

O que confere:
- A mensagem para endereço inválido sai como `BLOCKED`, com o endereço original e sem cópia, também no modo de teste, porque o bloqueio vem antes do teste (`MessageService.java:26-29`).
- Fora do teste, a mensagem original vai com `SENT` e logo depois vem a cópia para `arquivo@pousadamarealta.com.br` com "[Cópia] " (`MessageService.java:39-42`).
- No teste, a mensagem vai para `testes@pousadamarealta.com.br` com "[TESTE] " e a primeira linha "Destinatário original: …", sem cópia (`MessageService.java:32-37`).
- O `GET /messages` traz `status` (`ApiRoutes.java:263`).
- O resto que conferi também segue: quartos, preços com HALF_EVEN, transições, reembolso e CSVs.

O que não confere:
- A validação aceita "ana@.com" e "ana@gmail.", porque só verifica `domain.contains(".")` (`src/main/java/com/pousadamarealta/util/EmailValidator.java:15`). O ponto deveria estar "no meio".
- A confirmação exige `amountPaid` no corpo e devolve 400 sem ele (`ApiRoutes.java:153-158`). Além disso, usa o valor enviado em vez do total da reserva (`ApiRoutes.java:160`, `ReservationService.java:85`). Um `POST /reservations/{id}/confirmation` sem corpo, como o enunciado descreve, não confirma e não gera e-mail nenhum.
- O dinheiro no comprovante do e-mail sai como "R$ 779.80" e não "R$ 779,80". O código formata com `%,.2f` e troca toda vírgula por ponto (`ReservationService.java:286`), o que dá também "1.234.56" para valores acima de mil.
- Detalhe menor: valores como zero saem no JSON como `0.0`, porque passam por `double` (`ApiRoutes.java:349-351`).
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe. No comportamento, fica uma dúvida: o efeito do `amountPaid` obrigatório depende de como o cliente chama a confirmação. Se o site mandar `amountPaid` igual ao total, o fluxo funciona. Mas o enunciado não prevê esse campo e diz que o valor pago é o total da reserva, por isso tratei isso como desvio.
