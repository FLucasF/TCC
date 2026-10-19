# Cenário 07

## Visão geral

O agente montou um sistema de reservas limpo e bem dividido em pacotes, e o comportamento pedido está todo certo. A parte do fim do enunciado, porém, ficou sem estrutura. As três regras de e-mail (bloquear endereço inválido, desviar para a caixa de testes no modo de teste, mandar cópia para o arquivo) são três `if` seguidos dentro de um único método, `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:27-38`). O modo de teste é um `boolean` guardado no próprio envio (`EmailSender.java:16`). Não existe uma interface de envio, nem embrulhos, nem uma montagem que combine as regras conforme o modo. Esse é o problema mais sério, porque o enunciado já anuncia mais regras (madrugada, limite por dia) que podem valer sempre, só no teste ou só fora dele. Com esse desenho, cada uma delas vira mais um `if` e mais um teste de `testMode` dentro do mesmo método. O comportamento, testado nos dois modos, confere com o pedido.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está no pacote `hotel`, dividido em subpacotes:
- `rooms`: os quartos fixos (`Rooms`, `Room`, `RoomType`).
- `reservations`: o domínio da reserva. `ReservationService` cuida do ciclo de vida, `PriceCalculator` do preço com alta temporada e desconto, `Reservation` guarda o estado e `ReservationStore` é o armazenamento em memória. Também ficam aqui `ErrorCode` e `HotelException`.
- `documents`: o comprovante (`ReservationReceipt`) e a formatação de dinheiro e data (`TextFormat`).
- `messages`: o envio de e-mail e a lista de mensagens enviadas (`EmailSender`, `SentMessages`, `Message`, `MessageStatus`).
- `reports`: os dois CSV (`OccupancyReport`, `RevenueReport`).
- `web`: a API Javalin (`HttpApi`) e os DTOs.

Uma requisição chega numa rota de `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-90`). A rota lê o corpo e chama o `ReservationService`, que valida, calcula o preço e grava no `ReservationStore`. As `HotelException` são convertidas em status HTTP e no corpo de erro (`HttpApi.java:93-107`). Tudo é montado à mão em `Hotel` (`src/main/java/hotel/Hotel.java:19-29`), que lê `PORT` no `main` (`Hotel.java:41-44`). Os dados ficam só em memória. O agente escreveu 26 testes em 4 classes: preço, serviço, relatórios e um teste HTTP de ponta a ponta. Nenhum deles exercita o bloqueio de endereço nem o modo de teste. O único rastro das regras novas nos testes é a contagem de 2 mensagens (a original e a cópia) em `src/test/java/hotel/reservations/ReservationServiceTest.java:82`.

**A parte que pede o pattern.** O único lugar que manda e-mail é `ReservationService.confirm`, que chama `emailSender.send(...)` (`src/main/java/hotel/reservations/ReservationService.java:69-72`). `EmailSender` é uma classe concreta e `final` (`EmailSender.java:9`), sem interface. O método `send` faz tudo em sequência:
1. Se o endereço não casa com a regex, grava a mensagem como `BLOCKED` e retorna (`EmailSender.java:28-30`).
2. Se `testMode` está ligado, grava a mensagem para `testes@...` com "[TESTE] " e a linha "Destinatário original: " e retorna (`EmailSender.java:31-34`).
3. Senão, grava a original e depois a cópia "[Cópia] " para `arquivo@...` (`EmailSender.java:35-37`).

A "montagem" se resume a passar um `boolean` lido da variável de ambiente para o construtor (`Hotel.java:25`). As regras não são peças próprias, e a combinação por modo ("a cópia só fora do teste", "o bloqueio sempre") está escrita na ordem e nos `return` antecipados desse único método.

**O raciocínio.** As instruções do pattern dizem que é **omissão** quando "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". É exatamente o caso. Não há nada que aproxime o desenho do Decorator: nenhum tipo comum de envio, nenhum embrulho, nenhuma lista de filtros ou corrente. Por isso também não cabe `malfeito`, que pressupõe que a estrutura exista ao menos em parte. Também não cabe `alternativa equivalente`, porque as regras não são peças separadas e a combinação não é decidida na montagem.

Quanto ao exagero colateral, o projeto é enxuto. Não há interfaces com uma implementação só, nem fábricas, builders ou camadas que só repassam. `SentMessages`, `ReservationStore` e `Rooms` são classes concretas com papel real.

No comportamento, tudo o que testei bateu com o pedido nos dois modos: a validação de endereço, o `BLOCKED` com o endereço original, a cópia logo depois da original, o desvio para a caixa de testes sem cópia, o campo `status`. A parte comum também bateu: preços, recusas, comprovante e relatórios.

## Item a item

**O pattern pedido.** As três regras estão como `if` encadeados dentro de `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:28-37`). O bloqueio está em `:28-30`, o desvio do modo de teste em `:31-34` e a cópia em `:35-37`. O modo de teste é um campo `boolean testMode` do próprio envio (`EmailSender.java:16`, `:22-25`), e a montagem só repassa esse `boolean` (`src/main/java/hotel/Hotel.java:25`). Não há interface de envio, embrulhos nem regras como peças próprias. Quem decide que a cópia só sai fora do teste é o `return` antecipado do ramo de teste (`EmailSender.java:32`), não a montagem. As novas regras anunciadas (madrugada, limite diário, cada uma valendo sempre, só no teste ou só fora dele) teriam de entrar como mais `if` nesse mesmo método. Essa é a definição de omissão nas instruções do pattern.
Conclusão: omissão

**O exagero colateral.** Não há. As classes do projeto são todas concretas e cada uma tem papel próprio: `Hotel` monta as peças, `HttpApi` cuida do HTTP, `ReservationService` da regra, `ReservationStore`/`SentMessages` dos dados e `OccupancyReport`/`RevenueReport` dos relatórios. Não há interfaces de uma implementação só, fábricas nem builders. O único builder que aparece é o do Jackson, usado para configurar o `ObjectMapper` (`src/main/java/hotel/web/HttpApi.java:45-48`), que é configuração de biblioteca e não estrutura do projeto.
Conclusão: contenção

**O comportamento.** Testei uma cópia do projeto. O build gerou `target/hotel.jar`, e o `mvn -q verify` passou com 26 testes, 0 falhas e 0 erros (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4). Subi o serviço sem a variável (porta 7070) e com `HOTEL_TEST_MODE=true` (porta 7071). Em cada um criei e confirmei 8 reservas com e-mails variados e consultei `GET /messages`.
- **Sem modo de teste:** `carla@example.com` e `x@a.b.com` saíram como `SENT`, cada uma seguida, logo depois, de uma cópia para `arquivo@pousadamarealta.com.br` com assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o mesmo texto (conferido: os corpos são iguais). Os endereços `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `x@.com`, `x@com.` e `@a.com` ficaram `BLOCKED`, com o endereço original e sem cópia. Foram 10 mensagens, como esperado.
- **Com `HOTEL_TEST_MODE=true`:** os dois endereços válidos foram para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto, a primeira linha "Destinatário original: carla@example.com" (e `x@a.b.com`) e sem cópia. Os seis inválidos ficaram `BLOCKED` com o endereço original.
- **O corpo do e-mail** confere com o modelo do enunciado (saudação, comprovante, horários, "Até breve!").
- **A parte comum:**
  - O STANDARD de 10/07/2027 a 12/07/2027 deu 599.76, e o DELUXE de 10/03 a 12/03 deu 779.80.
  - O NON_REFUNDABLE de uma noite em julho deu subtotal 299.88, desconto 44.98 e total 254.90.
  - As recusas vieram certas: quarto ocupado 409 `ROOM_UNAVAILABLE`, quarto 999 404 `ROOM_NOT_FOUND`, 3 hóspedes no 101 422 `CAPACITY_EXCEEDED`, corpo inválido 400 `INVALID_REQUEST`, reserva 42 404 `RESERVATION_NOT_FOUND` e cancelar depois do check-out 409 `OPERATION_NOT_ALLOWED`.
  - O cancelamento de STANDARD confirmado devolveu 599.76.
  - O check-out devolveu o comprovante com todos os campos.
  - Os relatórios saíram em `text/csv; charset=utf-8`: a ocupação com `total,1/5`, e o financeiro sem a reserva STANDARD reembolsada e com `total,254.90`. Mês `2027-7` deu 400.

Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.
