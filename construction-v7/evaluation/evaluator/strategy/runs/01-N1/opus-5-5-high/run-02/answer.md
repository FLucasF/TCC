# Cenário 01

## Visão geral

O agente montou os planos de tarifa como um Strategy limpo. A interface `RatePlan` tem uma classe para cada um dos cinco planos, inclusive o normal e o não reembolsável. Cada classe guarda as próprias regras de preço, de quem pode usar e de devolução. Um catálogo único (`RatePlans`) escolhe o plano pelo código. O serviço só chama `plan.price(stay)` na criação e `ratePlan.refund(amountPaid)` no cancelamento, sem nomear nenhum plano. Não há `switch` nem `if` sobre o plano fora das classes de plano. O resto do sistema é enxuto, sem camadas que só repassam. Nos testes por `curl`, tudo o que o enunciado pede conferiu. Não encontrei problema sério. O único ponto discutível é pequeno: o `Stay` traz um campo `bookedOn` que nenhum plano usa, preparado para um plano futuro.

## Como foi implementado, e por que o julgamento

**O projeto.** São três pacotes:

- **`api`**:
  - `Routes` (`src/main/java/br/com/mareal/api/Routes.java:18-54`) registra as rotas do Javalin e traduz `ApiException` para `{error, message}` (`Routes.java:27-28`).
  - `RequestParser` confere o formato do corpo, das datas e do mês, e junta tudo num `NewReservation` (`RequestParser.java:19-53`). O `companyCode` só é lido se vier como texto (`RequestParser.java:49-50`).
  - `ReservationJson` define o JSON da reserva.
- **`domain`**:
  - `ReservationService` tem as regras de ciclo de vida, a lista de reservas e os quartos, todos em memória (`ReservationService.java:19-23`).
  - `Reservation` é o objeto da reserva, com os campos públicos.
  - `Season` calcula o preço por noite com a alta temporada (`Season.java:13-30`).
  - `Money` arredonda meio para o par (`Money.java:15-26`).
  - `Receipt` gera o comprovante.
  - `Reports` gera os CSVs.
  - `Outbox` guarda as mensagens enviadas.
- **`plans`**: os planos de tarifa.

O `App` lê `PORT` e monta `Outbox`, `ReservationService` e `Routes` à mão (`App.java:9-14`). O caminho de uma criação é este:

1. A rota recebe o pedido.
2. `RequestParser.reservation` confere o formato.
3. `ReservationService.create` procura o quarto (404) e confere a capacidade (422).
4. O mesmo método procura o plano no catálogo (`ReservationService.java:43-44`), monta o `Stay` e pede o preço ao plano (`ReservationService.java:45-46`).
5. Por fim confere o conflito de datas (409) e grava a reserva.

O cancelamento pede a devolução ao plano guardado na reserva (`ReservationService.java:103`). O agente escreveu 4 testes de ponta a ponta com HTTP de verdade (`PousadaTest.java:58-124`). Eles cobrem:

- a alta temporada;
- os três exemplos do financeiro;
- as recusas dos planos;
- o comprovante, os relatórios e a segunda via.

**A parte dos planos.**

- **O contrato.** `RatePlan` (`plans/RatePlan.java:9-17`) declara `code()`, `price(Stay)` e `refund(BigDecimal)`. `price` também recusa a reserva quando o plano não pode ser usado, lançando um `INVALID_REQUEST`.
- **Normal e não reembolsável.** `StandardPlan` cobra as noites sem desconto e devolve tudo (`StandardPlan.java:11-17`). `NonRefundablePlan` dá 15% e devolve zero (`NonRefundablePlan.java:8-20`).
- **Convênio.** `CorporatePlan` tem o mapa empresa → desconto (`CorporatePlan.java:9-12`). Ele recusa a reserva sem código ou com código desconhecido (`CorporatePlan.java:19-22`) e devolve tudo (`CorporatePlan.java:26-28`).
- **Pacote de fim de semana.** `WeekendPackagePlan` tem o preço fechado por tipo de quarto (`WeekendPackagePlan.java:11-14`). Ele exige entrada na sexta com exatamente 2 noites (`WeekendPackagePlan.java:22-25`) e devolve metade (`WeekendPackagePlan.java:29-31`).
- **Estadia longa.** `LongStayPlan` exige 7 noites (`LongStayPlan.java:16-18`), dá 10% sobre as noites já com a temporada (`LongStayPlan.java:19`) e devolve tudo.
- **Peças comuns.** `Pricing` é o resultado (diárias e desconto, com o total calculado). `Stay` é o contexto que o plano recebe.
- **A escolha.** O único ponto de seleção é o mapa de `RatePlans` (`RatePlans.java:10-22`). Um plano novo é uma classe nova mais uma linha na lista (`RatePlans.java:13-14`).
- **O que fica fora dos planos.** Busquei `switch`, `instanceof` e os nomes dos planos no código principal. Fora das classes de plano, o código só aparece em `ReservationJson.java:14` (`r.ratePlan.code()`), e só para mostrar o nome no JSON.

**O raciocínio.** É exatamente o desenho que as instruções chamam de acerto:

- uma interface com uma implementação para cada plano;
- os cinco planos dentro da estrutura;
- a escolha num mapa único;
- preço, validação e devolução de cada plano numa classe só dela;
- o serviço pedindo ao plano, sem perguntar qual é o caso concreto.

Não há nada do que afastaria o desenho do pattern. Não sobrou `switch` de comportamento, a validação não ficou no serviço e nenhum plano ficou de fora. Até o `companyCode` só é interpretado dentro do `CorporatePlan`: o serviço apenas repassa o campo no `Stay`.

No exagero colateral, o resto do sistema é direto:

- não há repositório com interface;
- não há fábrica nem builder;
- o `Outbox` é uma classe concreta;
- o serviço não é repassado por uma camada intermediária.

O `Pricing`, o `Stay` e o `RatePlans` fazem parte da própria estrutura pedida. No comportamento, todos os valores, recusas, devoluções e relatórios que testei conferiram.

## Item a item

**O enunciado (pede o pattern).** O agente fez uma interface `RatePlan` (`plans/RatePlan.java:9-17`) com cinco implementações:

- `StandardPlan.java:6`
- `NonRefundablePlan.java:7`
- `CorporatePlan.java:8`
- `WeekendPackagePlan.java:10`
- `LongStayPlan.java:7`

Cada uma tem o seu preço, a sua recusa e a sua devolução. A escolha é feita só pelo mapa de `RatePlans.java:10-22`. A criação chama `plan.price(stay)` (`ReservationService.java:46`) e o cancelamento chama `r.ratePlan.refund(r.amountPaid)` (`ReservationService.java:103`), e nenhum dos dois nomeia plano. Não há `switch`, nem `if`, nem `instanceof` sobre o plano em nenhum outro lugar. É o Strategy pedido, com os dois planos da parte comum incluídos.
Conclusão: acerto

**O exagero colateral.** Não há. Os quartos ficam numa lista no serviço (`ReservationService.java:19-22`), sem repositório e sem interface. `Outbox` é uma classe concreta (`Outbox.java:7-17`). `Season`, `Money`, `Reports` e `Receipt` são utilitários estáticos e records simples. A montagem é feita à mão em `App.java:11-13`. O único detalhe especulativo é o campo `bookedOn` do `Stay` (`Stay.java:8`, preenchido em `ReservationService.java:45`), junto com o `Clock` injetado. Ele foi feito para um plano futuro de antecedência e nenhum plano o usa. Mas é um campo, não uma estrutura, e fica dentro da parte dos planos.
Conclusão: contenção

**O comportamento.** Fiz o build numa cópia em `/tmp/copia`, subi o serviço na porta 7070 e chamei a API com `curl`.

Preços na criação:

| Caso | Diárias | Desconto | Total |
|---|---|---|---|
| STANDARD 101, 10/07 a 12/07/2027 | 599,76 | 0,00 | 599,76 |
| CORPORATE TECNOSUL, deluxe, 10/03 a 12/03 | 779,80 | 93,58 | 686,22 |
| CORPORATE AGROVALE, standard, 2 noites | 499,80 | 89,96 | 409,84 |
| CORPORATE FARMAVIDA, suíte, 2 noites | 1159,80 | 289,95 | 869,85 |
| WEEKEND_PACKAGE deluxe, sexta 12/03 a domingo 14/03 | 820,00 | 0,00 | 820,00 |
| WEEKEND_PACKAGE standard, julho (alta temporada) | 520,00 | 0,00 | 520,00 |
| WEEKEND_PACKAGE suíte, dezembro (alta temporada) | 1200,00 | 0,00 | 1200,00 |
| LONG_STAY standard, 01/03 a 08/03 | 1749,30 | 174,93 | 1574,37 |
| LONG_STAY deluxe, 7 noites em julho | 3275,16 | 327,52 | 2947,64 |
| NON_REFUNDABLE deluxe | 779,80 | 116,97 | 662,83 |

No pacote, a temporada foi ignorada, como pedido. Na estadia longa de julho, a temporada valeu nas noites. Um `companyCode` mandado num STANDARD foi ignorado.

Recusas, todas 400 `INVALID_REQUEST` com mensagem:

- convênio sem código, com código `ACME` ou com `tecnosul` em minúsculas;
- pacote de sábado a segunda, de sexta a sábado ou de sexta a segunda;
- estadia longa de 6 noites;
- plano desconhecido.

Devoluções depois de pagar:

| Plano | Pago | Devolvido |
|---|---|---|
| Convênio | 686,22 | 686,22 |
| Pacote deluxe | 820,00 | 410,00 |
| Pacote suíte | 1200,00 | 600,00 |
| Estadia longa | 1574,37 | 1574,37 |
| Não reembolsável | 662,83 | 0,00 |
| Normal | 599,76 | 599,76 |

Um pacote cancelado ainda pendente devolveu 0,00.

Parte comum:

- O e-mail tem o assunto certo e traz o comprovante com a linha "Desconto" antes do total.
- O check-in seguido de check-out devolveu o comprovante.
- Os erros vieram como esperado: 409 ao cancelar de novo, 409 na segunda via de uma cancelada, 404 `RESERVATION_NOT_FOUND`, 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED`, 404 `ROOM_NOT_FOUND`, 400 para corpo que não é JSON, para campo faltando e para mês `2027-7`.
- O financeiro de 2027-03 mostrou só o pacote cancelado com 410.00. O de 2027-04 mostrou a reserva com check-out (409.84) mais a não reembolsável cancelada (662.83), com total 1072.67, em `text/csv; charset=utf-8`.
- A ocupação deu `total,1/5`.

O `mvn verify` da cópia passou com BUILD SUCCESS: 4 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. O único ponto discutível é o `bookedOn` não usado (`Stay.java:8`), um pouco de preparo antecipado. Não pesei isso como exagero porque não é uma estrutura (interface, fábrica ou camada) e fica dentro da parte que o fim do enunciado pede.
