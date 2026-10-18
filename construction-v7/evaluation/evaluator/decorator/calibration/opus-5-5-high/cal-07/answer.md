# Cenário 01

## Visão geral

O agente entregou um sistema de reservas enxuto e correto, sem estrutura sobrando. A parte do fim do enunciado, porém, ficou sem o pattern. As três regras de envio (bloquear endereço inválido, desviar para a caixa de testes, fazer a cópia para o arquivo) estão escritas como três `if` seguidos num único método, `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:27-38`). Esse envio recebe um `boolean testMode` no construtor e decide sozinho a combinação por modo. Não existe uma interface de envio, nem um embrulho por regra, nem uma montagem que empilhe as regras conforme o modo. Para pôr no sistema as regras que o enunciado já anuncia ("não mandar de madrugada", "limite por dia", cada uma podendo valer sempre, só no teste ou só fora dele), seria preciso abrir esse método e cruzar mais `if` com o modo. O comportamento pedido está todo certo nos dois modos, e o `mvn verify` passa.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `hotel`, dividido assim:
- `rooms`: os cinco quartos fixos no código (`Rooms`, `Room`, `RoomType`);
- `reservations`: as regras da reserva:
  - `ReservationService` cuida do ciclo de vida;
  - `PriceCalculator` calcula o preço, com alta temporada e desconto;
  - `ReservationStore` guarda as reservas na memória, num `LinkedHashMap`;
  - `Reservation`, `Price`, `Guest`, `NewReservation`, os enums e `HotelException`/`ErrorCode` completam o pacote;
- `documents`: `ReservationReceipt` (o comprovante e o texto dele) e `TextFormat` (formato brasileiro de dinheiro e datas);
- `reports`: `OccupancyReport` e `RevenueReport`, que montam os CSVs;
- `messages`: `EmailSender`, `SentMessages` (a lista em memória), `Message` e `MessageStatus`;
- `web`: `HttpApi` (rotas, leitura do corpo, mapa de erro para status HTTP) e os records de entrada e saída.

Uma requisição segue este caminho: a rota em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`) lê e valida o corpo e chama o `ReservationService`. O serviço aplica a regra, guarda no `ReservationStore` e, na confirmação, chama `EmailSender.send` (`src/main/java/hotel/reservations/ReservationService.java:62-74`). A resposta volta como `ReservationResponse` ou `ReservationReceipt`, e as `HotelException` viram `{error, message}` no tratador de exceções (`src/main/java/hotel/web/HttpApi.java:93-107`).

A montagem fica no construtor de `Hotel` (`src/main/java/hotel/Hotel.java:19-29`): ele cria as peças à mão, lê `HOTEL_TEST_MODE` e passa o resultado como booleano para o `EmailSender` (`src/main/java/hotel/Hotel.java:25`). O `main` lê `PORT` (`src/main/java/hotel/Hotel.java:41-44`). O jar sobe sozinho graças ao shade com `mainClass` (`pom.xml:66-68`).

O agente escreveu quatro classes de teste, com 26 testes:
- `PriceCalculatorTest`: preço;
- `ReservationServiceTest`: ciclo de vida e e-mail de confirmação;
- `ReportsTest`: relatórios;
- `HttpApiTest`: a API ponta a ponta.

**A parte do fim.** Toda ela mora em `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:9-39`). Os dois endereços fixos e a expressão regular do endereço válido ficam nas constantes (`:11-13`). O modo chega como `boolean` (`:16`, `:22-25`). O método `send` faz tudo em sequência:
1. Se o endereço não casa com a expressão, grava a mensagem `BLOCKED` com o endereço original e retorna (`:28-30`).
2. Senão, se está em modo de teste, grava uma única mensagem para a caixa de testes, com "[TESTE] " no assunto e a linha "Destinatário original" no texto, e retorna (`:31-34`).
3. Senão, grava a original e logo depois a cópia "[Cópia] " para o arquivo (`:35-37`).

Os casos não são representados como peças. Cada um é um ramo de `if`, e quem escolhe entre eles é o próprio envio, perguntando o endereço e o modo. A combinação por modo ("a cópia só fora do teste", "o bloqueio sempre") não foi decidida na montagem. Ela aparece só na ordem dos `return` dentro do método. O `SentMessages.add` (`src/main/java/hotel/messages/SentMessages.java:11-15`) é só o armazenamento.

**O raciocínio.** As instruções do pattern definem exatamente esta situação como omissão: "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio". Não há uma interface de envio com o envio real e embrulhos de mesma interface. Também não há uma corrente ou lista de filtros com uma peça por regra, que valeria como alternativa equivalente. E a montagem em `Hotel` não escolhe regra nenhuma: só repassa um booleano. Também não é malfeito, porque para isso a estrutura teria de existir pelo menos em parte, e aqui não existe nenhum embrulho.

Fora dessa parte, o desenho é contido. Não há interface com uma implementação só, fábrica, builder nem camada de repasse. `ReservationStore`, `PriceCalculator` e os relatórios são classes concretas com trabalho real. O único construtor que vale notar é o de compatibilidade `EmailSender(SentMessages)` (`:18-20`), que é conveniência e não estrutura. No comportamento, testei os dois modos pela API e tudo confere com o pedido. Os testes do agente, porém, não cobrem o bloqueio nem o modo de teste: a única verificação do envio é o `assertEquals(2, sent.size())` (`src/test/java/hotel/reservations/ReservationServiceTest.java:81`), que confirma a cópia só de forma indireta.

## Item a item

**O enunciado (pede o pattern).** As três regras são três `if` no mesmo método `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:28`, `:31`, `:35-36`). O modo de teste é um `boolean` guardado no próprio envio (`src/main/java/hotel/messages/EmailSender.java:16`) e passado pela montagem sem empilhar nada (`src/main/java/hotel/Hotel.java:25`). Não há uma interface de envio, nem um embrulho por regra, nem uma corrente ou lista de filtros. A combinação por modo está escrita na ordem dos `return` dentro do envio. Uma regra nova ("madrugada", "limite por dia", valendo só num dos modos) exigiria editar esse método e cruzar mais condições com `testMode`. É justamente o caso que o enunciado anuncia e que o Decorator resolveria.
Conclusão: omissão

**O exagero colateral.** Não há. As classes do resto do sistema são concretas e cada uma tem trabalho próprio: `ReservationService`, `PriceCalculator`, `ReservationStore`, `OccupancyReport` e `RevenueReport` (`src/main/java/hotel/reports/OccupancyReport.java:13`). A montagem é feita à mão no construtor de `Hotel` (`src/main/java/hotel/Hotel.java:19-29`), sem interfaces de uma implementação só, fábricas, builders ou camadas que só repassam.
Conclusão: contenção

**O comportamento.** Montei a cópia do projeto em `/tmp/copia` com `mvn -q package -DskipTests` e subi o `target/hotel.jar` duas vezes: na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma, criei e confirmei cinco reservas com `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `x@.com`.

- **Modo normal (7070):**
  - a mensagem para `carla@example.com` saiu `SENT` e foi seguida da cópia para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", o mesmo texto e o canal `EMAIL`;
  - os quatro endereços inválidos apareceram como `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste (7071):**
  - houve uma única mensagem para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - Pousada Maré Alta", a primeira linha "Destinatário original: carla@example.com" e sem cópia;
  - os inválidos saíram `BLOCKED` com o endereço original.
- **O texto do e-mail** bate com o enunciado, com o comprovante no meio ("2 diárias", "Diárias: R$ 779,80", "Total: R$ 779,80").
- **A parte comum:**
  - o STANDARD de 10/07 a 12/07/2027 deu `subtotal 599.76`, como no exemplo do enunciado;
  - o NON_REFUNDABLE de uma noite em julho deu `299.88 / 44.98 / 254.90`;
  - o check-out devolveu o comprovante com todos os campos, e a segunda via de uma reserva confirmada também funcionou;
  - o cancelamento da reserva não reembolsável deu `refund 0.00`;
  - as recusas vieram certas: `ROOM_UNAVAILABLE` 409, `CAPACITY_EXCEEDED` 422, corpo que não é JSON 400 `INVALID_REQUEST` e mês `2027-7` 400;
  - a ocupação de 10/07/2027 veio em `text/csv; charset=utf-8` com `total,1/5`;
  - o financeiro de 07/2027 incluiu a reserva não reembolsável cancelada, com `total,854.66`.
- **Os testes do agente:** o `mvn -q verify` passou (exit 0), com 26 testes, 0 falhas e 0 erros (11 + 5 + 6 + 4). Nenhum deles cobre o bloqueio nem o modo de teste.
- **Encerramento:** derrubei os dois serviços no fim e conferi que as portas 7070 e 7071 não respondem mais.

Conclusão: segue

## Dúvidas

Nenhuma.
