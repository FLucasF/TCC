# Cenário 01

## Visão geral

O agente montou um sistema de reservas organizado em camadas simples (controllers, services, domínio, DTOs, utilitários), e a parte comum funciona quase toda como o enunciado pede. Na parte do fim, as regras de envio, ele chegou a escrever uma interface `MessageRule` e três classes de regra (`EmailValidationRule`, `ArchiveCopyRule`, `TestModeRule`), mas nenhuma delas é usada. O envio de verdade passa por `MessageProcessor.process`, que resolve as três regras com `if`/`else` dentro de um único método, e o modo de teste chega a ele como um `boolean`. É exatamente o desenho que o pattern deveria evitar. As próprias classes mortas também não são embrulhos: cada uma recebe `isTestMode` e decide sozinha se vale ou não. No comportamento, os casos principais conferem nos dois modos. Os problemas são dois: a validação aceita endereços como `ana@.com` e `ana@gmail.`, e o dinheiro no comprovante sai com espaço não separável depois do "R$".

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote raiz `com.pousadamarealta` tem os pacotes abaixo:

- `controller`: um controller por recurso. `ReservationController` lê o corpo, valida os campos e as datas e converte as exceções do service em respostas `{error, message}`.
- `service`:
  - `RoomService` guarda os cinco quartos num `HashMap`.
  - `ReservationService` guarda as reservas num `HashMap<Long, Reservation>` com um `AtomicLong`. Ele faz a disponibilidade, monta o comprovante e o e-mail e chama `MessageService.send` na confirmação (`service/ReservationService.java:79-94`, `:176-188`).
  - `ReportService` monta os CSVs.
  - `MessageService` guarda a lista de mensagens.
- `domain`: `Reservation` tem as transições e o reembolso (`domain/Reservation.java:109-159`). `RoomType` é um enum com capacidade e diária.
- `util`: `PriceCalculator` (HALF_EVEN a cada passo), `MoneyFormatter`, `DateFormatter` e `CpfFormatter`.
- `messaging`: a parte do cenário.

Uma requisição segue este caminho: rota em `App.java:40-50`, depois o controller, o `ReservationService` e os mapas em memória. A montagem fica toda no `App.main`: ele lê `PORT` e `HOTEL_TEST_MODE` (`App.java:19-20`, `:71-74`) e cria os services e controllers à mão. Os testes do agente são 26, todos de unidade: `ReservationServiceTest` (12), `PriceCalculatorTest` (7) e `MessageProcessorTest` (7). Nenhum teste toca nas classes de regra.

**A parte do fim.** Na montagem, o `App` só passa um `boolean` adiante (`App.java:23`), e `MessageService` cria `new MessageProcessor(testMode)` (`service/MessageService.java:17-18`). Dentro de `MessageProcessor.process` está todo o comportamento:

- o primeiro `if` testa o endereço e devolve a mensagem `BLOCKED` (`messaging/MessageProcessor.java:16-24`);
- o segundo `if (testMode)` desvia para `testes@…` com "[TESTE] " e "Destinatário original: " (`:29-38`);
- o `else` monta a mensagem normal e acrescenta a cópia para `arquivo@…` (`:39-57`);
- a validação do endereço é um método privado do próprio processor (`:66-83`).

Nenhuma regra é uma peça própria, e a combinação por modo é um `if` dentro do envio. As classes `MessageRule` (`messaging/MessageRule.java:5-7`), `EmailValidationRule`, `ArchiveCopyRule` e `TestModeRule` não são instanciadas em lugar nenhum. O único uso de qualquer coisa de `messaging` fora dela é o `MessageProcessor`. Além de mortas, elas não seriam um Decorator:

- não envolvem um envio interno, só devolvem listas;
- a interface recebe `boolean isTestMode` (`MessageRule.java:6`);
- cada regra decide o modo por dentro: `ArchiveCopyRule.java:13` (`if (isTestMode) return empty`) e `TestModeRule.java:13` (`if (!isTestMode) return empty`).

A validação de endereço aparece duplicada, igual, em `EmailValidationRule.java:23-40` e `MessageProcessor.java:66-83`.

**O julgamento.** As instruções chamam de omissão o caso em que "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". É o que o código que roda faz (`MessageProcessor.java:16`, `:29`, `:39`). Uma regra nova, como a de madrugada ou o limite por dia, exigiria mexer nesse método e em mais um `if` de modo. O que poderia puxar para `malfeito` é a existência das classes de regra. Mas elas não estão no caminho de nenhuma mensagem, então não há estrutura convivendo com o jeito manual: há só o jeito manual, mais código morto. E, mesmo que estivessem ligadas, a combinação por modo continuaria escrita dentro delas. Fiquei com `omissão`.

No exagero colateral, fora da parte de mensagens não há interface com implementação única, fábrica, builder nem camada que só repassa. O `ReportService` faz trabalho real (monta o CSV), e os DTOs de resposta são uma separação comum.

No comportamento, os dois modos conferem para endereço válido e para os inválidos do enunciado. A falha é a regra de validação, que só exige um ponto em qualquer lugar do domínio (`MessageProcessor.java:81-82`). Há também um desvio de formato na parte comum (`util/MoneyFormatter.java:9-12`).

## Item a item

**O enunciado (pede o pattern).** O envio passa por `MessageService.send`, depois `MessageProcessor.process` (`service/MessageService.java:22`). Lá, as três regras são `if`s no mesmo método: o endereço em `messaging/MessageProcessor.java:16`, o modo de teste em `:29` e a cópia no `else` em `:39-57`. O modo chega como `boolean` desde `App.java:20-23`. Existem `MessageRule` e três implementações (`messaging/EmailValidationRule.java:8`, `messaging/ArchiveCopyRule.java:8`, `messaging/TestModeRule.java:8`), mas nada as instancia. Além disso, elas não embrulham um envio e decidem o modo por dentro (`ArchiveCopyRule.java:13`, `TestModeRule.java:13`). Não há interface de envio, nem embrulhos que chamem o próximo, nem combinação decidida na montagem. É o caso de omissão descrito nas instruções.
Conclusão: omissão

**O exagero colateral.** Não há. Fora da parte de mensagens, as camadas fazem trabalho próprio: os controllers traduzem HTTP e erros (`controller/ReservationController.java:25-70`), os services têm as regras (`service/ReservationService.java:27-72`) e o `ReportService` monta os CSVs (`service/ReportService.java:20-69`). Não há interfaces de implementação única, fábricas nem builders. A interface `MessageRule` sem uso fica dentro da parte do cenário, por isso não conta aqui.
Conclusão: contenção

**O comportamento.** Subi o jar duas vezes, na 7070 (normal) e na 7071 (`HOTEL_TEST_MODE=true`). Criei e confirmei reservas com os endereços `carla@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `ana@.com` e `ana@gmail.`.

- Modo normal:
  - `carla@gmail.com` virou uma mensagem `SENT` seguida da cópia `SENT` para `arquivo@pousadamarealta.com.br`, com "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto. Confere.
  - `ana@gmail`, `ana.gmail.com` e `a@b@c.com` saíram `BLOCKED`, com o endereço original e sem cópia. Confere.
  - `ana@.com` e `ana@gmail.` saíram `SENT` com cópia. Esperava `BLOCKED`, porque o enunciado pede "um nome com pelo menos um ponto no meio" e o código só testa `domain.contains(".")` (`MessageProcessor.java:81-82`).
- Modo de teste:
  - Os endereços válidos foram para `testes@pousadamarealta.com.br`, com "[TESTE] " e a primeira linha "Destinatário original: carla@gmail.com", sem cópia. Confere.
  - Os três inválidos ficaram `BLOCKED` com o endereço original. Confere.
  - `ana@.com` e `ana@gmail.` também foram desviados como válidos, com o mesmo erro.
- Todas as mensagens têm `status`.
- Parte comum:
  - O STANDARD de 10/07 a 12/07/2027 deu 599.76. Confere.
  - O NON_REFUNDABLE de uma noite no quarto 102 deu desconto 37.48 e total 212.42. Confere.
  - O NON_REFUNDABLE confirmado e cancelado ficou com `refund` 0. O STANDARD confirmado e cancelado devolveu 389.90. O segundo cancelamento deu 409 `OPERATION_NOT_ALLOWED`.
  - Sobreposição deu 409 `ROOM_UNAVAILABLE`. Quarto inexistente deu 404. Capacidade excedida deu 422. Saída igual à entrada, corpo que não é JSON e campo faltando deram 400. Reserva inexistente deu 404.
  - O check-out devolveu o comprovante, e a segunda via de uma reserva paga respondeu 200.
  - A ocupação de 2027-08-10 veio `text/csv; charset=utf-8` com `total,1/5`. O financeiro de 2027-08 contou a não reembolsável cancelada (212.42) e deu `total,792.32`. O mês `2027-13` deu 400.
  - O texto do e-mail segue o modelo, mas entre "R$" e o valor vem o byte `C2 A0` (espaço não separável), porque `NumberFormat.getCurrencyInstance` é usado em `util/MoneyFormatter.java:9-12`. Não é o "R$ 779,80" com espaço comum.
- `mvn verify` na cópia: 26 testes, 0 falhas, `BUILD SUCCESS`.
- Os dois serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

A existência de `MessageRule` e das três classes de regra poderia sugerir `malfeito` ("a estrutura existe, mas convive com o jeito sem ela"). Fiquei com `omissão` por dois motivos. Primeiro, essas classes não estão em nenhum caminho de execução: só `MessageProcessor` é instanciado (`service/MessageService.java:18`). Segundo, nem como estrutura elas seriam Decorator ou corrente, porque decidem o modo por dentro (`ArchiveCopyRule.java:13`, `TestModeRule.java:13`).

Sobre o espaço não separável no dinheiro: ele visualmente parece certo, e só um teste que compare o texto byte a byte o rejeitaria. Anotei como desvio porque o enunciado mostra o formato com espaço comum.
