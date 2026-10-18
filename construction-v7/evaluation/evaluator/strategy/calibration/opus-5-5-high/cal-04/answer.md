# Cenário 01

## Visão geral

O agente montou um sistema enxuto e organizado, e tudo o que testei se comporta como pedido, inclusive os três planos novos e os exemplos do financeiro. O desenho dos planos, porém, não usa o Strategy. `RatePlan` é um `enum` sem comportamento nenhum. O que cada plano faz está espalhado em três pontos, todos decidindo pelo nome do plano: um `if` e um `switch` no cálculo do preço (`PriceCalculator.java`), uma sequência de `if`s de validação no serviço e um `switch` no cancelamento (os dois em `ReservationService.java`). Um plano novo, como os de melhor idade ou de 60 dias de antecedência que o comercial já anunciou, obriga a mexer nesses três lugares. Isso é o problema mais sério. Fora dos planos o desenho é contido.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em `src/main/java/hotel`, dividido em pacotes:

- `rooms`: os cinco quartos fixos no código (`rooms/Rooms.java:10-15`).
- `reservations`: o domínio. `Reservation` guarda o estado, `ReservationService` cuida das transições e das regras, `PriceCalculator` faz o preço, `ReservationStore` guarda tudo na memória e há os `enum` `RatePlan`, `ReservationStatus` e `ErrorCode`.
- `documents`: o comprovante (`ReservationReceipt`) e a formatação em pt-BR (`TextFormat`).
- `messages`: o envio de e-mail (`EmailSender`), que grava na lista de enviados (`SentMessages`).
- `reports`: os dois relatórios CSV.
- `web`: o `HttpApi` (rotas Javalin, Jackson e tradução de `HotelException` para status HTTP em `web/HttpApi.java:100-107`) e os DTOs.

Uma requisição segue este caminho: a rota (`web/HttpApi.java:64-67`) lê o corpo em `CreateReservationRequest`, confere os campos obrigatórios e monta um `NewReservation`, que inclui o `companyCode` (`web/HttpApi.java:109-130`). Daí vai para `ReservationService.create`, que valida, chama o `PriceCalculator` e grava no `ReservationStore`, um `LinkedHashMap` com ids sequenciais (`reservations/ReservationStore.java:12-18`). Tudo é montado à mão em `Hotel.java:19-28`, e o `main` lê `PORT` com 7070 como padrão (`Hotel.java:41`). O shade gera `target/hotel.jar`, com `hotel.Hotel` no manifesto (`pom.xml:66-67`). O agente escreveu 26 testes em quatro classes (preço, serviço, relatórios e HTTP), mas nenhum toca `CORPORATE`, `WEEKEND_PACKAGE` ou `LONG_STAY`: o `grep` nos testes não encontra nenhum dos três.

**Os planos de tarifa.** `RatePlan` é um `enum` só com as cinco constantes e comentários (`reservations/RatePlan.java:3-14`). As regras de cada plano ficam em três lugares:

- **Preço:** `PriceCalculator.calculate` recebe o plano e o `companyCode`. Um `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` devolve o preço fechado escolhido por um `switch` no tipo de quarto (`reservations/PriceCalculator.java:32-39`). Para os outros planos, um `switch (ratePlan)` decide o desconto de cada um (`reservations/PriceCalculator.java:40-46`). Os percentuais e o mapa das empresas são constantes da própria calculadora (`reservations/PriceCalculator.java:18-23`).
- **Validação:** três `if`s em `ReservationService.create` testam o plano pelo nome. Um cuida do convênio e consulta `PriceCalculator.COMPANY_DISCOUNTS` (`reservations/ReservationService.java:51-56`), outro cuida do pacote, de sexta a domingo (`reservations/ReservationService.java:57-62`), e o terceiro cuida da estadia longa, com o mínimo de 7 noites (`reservations/ReservationService.java:63-66`).
- **Devolução:** um `switch (reservation.ratePlan())` em `ReservationService.cancel` (`reservations/ReservationService.java:105-109`).

**O raciocínio.** As instruções do Strategy pedem que cada plano tenha preço, validação e devolução num lugar só dele, e que o cálculo e o cancelamento perguntem ao plano sem nomeá-lo. Aqui acontece o contrário: nenhum plano tem lugar próprio, e os três pontos (preço, validação e devolução) decidem por `switch` ou `if` sobre o plano. As instruções chamam isso de omissão. Não há nem uma estrutura parcial que permitisse falar em malfeito: não existe interface, classe abstrata nem `enum` com corpo por constante. O `switch` sobre `RoomType` dentro do pacote é um detalhe do preço do pacote, e não muda nada.

No exagero colateral, as classes fora dos planos são todas concretas e usadas. `EmailSender` (`messages/EmailSender.java:4-15`) é uma classe fina sobre `SentMessages`, mas não é interface nem fábrica: dá nome ao canal de e-mail que o enunciado descreve. Não há interface com implementação única, builder nem camada que só repassa.

No comportamento, tudo o que testei confere com o enunciado.

## Item a item

**O enunciado (pede o pattern).** Os cinco planos são constantes nuas de um `enum` (`reservations/RatePlan.java:3-14`). O preço decide pelo plano com um `if` e um `switch` (`reservations/PriceCalculator.java:32-46`). A validação de convênio, pacote e estadia longa são `if`s sobre o plano no serviço (`reservations/ReservationService.java:51-66`). A devolução é outro `switch` sobre o plano (`reservations/ReservationService.java:105-109`). Não existe estratégia por plano. Um plano novo pede mudanças no `enum`, no calculador e em dois métodos do serviço, que é exatamente o que o pattern deveria evitar.
Conclusão: omissão

**O exagero colateral.** Não há. A composição em `Hotel.java:19-28` é direta, as classes são concretas e cada uma tem trabalho próprio. `EmailSender` (`messages/EmailSender.java:12-14`) é pequeno, mas não é abstração especulativa.
Conclusão: contenção

**O comportamento.** O build saiu limpo e o serviço subiu na 7070. Chamei a API com `curl`:

| O que testei | O que veio |
|---|---|
| Convênio Tecnosul, deluxe de 10/03 a 12/03/2027 | 779,80 / 93,58 / **686,22** |
| Convênio Agrovale | 18%: 140,36 sobre 779,80 |
| Convênio Farmavida | 25%: 289,95 sobre 1.159,80 |
| Convênio sem código, com `ACME` ou com `tecnosul` minúsculo | 400 `INVALID_REQUEST` |
| Pacote no deluxe, de 12/03 a 14/03/2027 | **820,00** sem desconto |
| Pacote no standard em janeiro (temporada) | 520,00, sem acréscimo |
| Pacote na suíte | 1.200,00 |
| Pacote começando no sábado, ou com 3 noites | 400 |
| Estadia longa, standard de 01/03 a 08/03/2027 | 1.749,30 / 174,93 / **1.574,37** |
| Estadia longa com 6 noites | 400 |
| Estadia longa em julho | a temporada vale: 2.099,16, menos 209,92, dá 1.889,24 |
| `companyCode` num pedido `STANDARD` | ignorado |
| Não reembolsável | 15%: 116,97 sobre 779,80 |
| Standard de 10/07 a 12/07/2027 | 599,76 |

No cancelamento depois de pago, o convênio devolveu tudo (686,22), o pacote devolveu **410,00**, a estadia longa devolveu tudo (1.574,37) e o não reembolsável devolveu 0,00. O pacote cancelado ainda pendente devolveu 0,00.

O e-mail de confirmação saiu com o assunto e o texto pedidos, com a linha "Desconto". A segunda via de uma reserva pendente dá 409. O ciclo de check-in e check-out e o comprovante funcionam.

Nos relatórios, o financeiro de 2027-03 traz o pacote cancelado com 410,00. O de 2027-04 traz o não reembolsável cancelado com 662,83. A ocupação conta a noite de entrada e não a de saída, e vem em `text/csv; charset=utf-8`.

Os erros da parte comum conferem: 400 para JSON inválido, 409 para quarto ocupado, 422 para capacidade, 404 para quarto e para reserva inexistentes.

O `mvn verify` da cópia passou: 26 testes, 0 falhas, 0 erros. Nenhum desses testes cobre os três planos novos. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.
