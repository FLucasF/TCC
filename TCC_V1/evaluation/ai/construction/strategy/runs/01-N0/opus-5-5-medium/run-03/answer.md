# Cenário 01

## Visão geral

O agente montou um sistema em camadas simples: um controller, alguns serviços e repositórios em memória. A maior parte das regras pedidas está certa, e as contas dos exemplos do enunciado conferem. O Strategy, porém, não foi aplicado. `RatePlan` é um `enum` sem comportamento nenhum (`model/RatePlan.java:3-9`). Tudo o que muda de um plano para outro ficou no `PricingService`, decidido por plano em quatro pontos: um `if` no cálculo do preço, um `switch` no desconto, um `switch` na devolução e um `switch` na validação (`service/PricingService.java:33`, `:62`, `:106`, `:118`). Para criar o próximo plano, como o da melhor idade, seria preciso mexer em todos esses lugares. É o caso típico de omissão. Pela leitura, há também dois desvios de comportamento fora dos planos: a segunda via sai para reserva não paga, e o dinheiro no comprovante usa um espaço não separável.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `com.pousadamarealta` se divide assim:
- `model`: `Room`, `RoomType` (capacidade e diária no `enum`), `Reservation`, `ReservationStatus`, `RatePlan`, `Company` e `Message`.
- `repository`: `RoomRepository` com os cinco quartos fixos, `ReservationRepository` com um `LinkedHashMap` e um `AtomicLong` para a numeração, e `MessageRepository` com uma lista.
- `service`: `PricingService` (preço, desconto, devolução e validação dos planos), `ReservationService` (o ciclo de vida da reserva), `DocumentService` (o texto do comprovante), `EmailService` (grava a mensagem) e `ReportService` (os CSV).
- `dto`: records de entrada e de saída.
- `controller/ApiController`: as rotas e o mapeamento das exceções para os códigos de erro (`controller/ApiController.java:36-84`).

O caminho de uma reserva é este:
1. `ApiController.createReservation` lê o corpo, confere os campos obrigatórios, converte as datas e o `ratePlan` (`controller/ApiController.java:93-122`).
2. Chama `ReservationService.createReservation`, que valida datas, hóspedes, quarto e plano (`service/ReservationService.java:118-140`), depois capacidade e conflito de datas (`:32-38`).
3. Pede o preço ao `PricingService.calculatePrice` (`:41`) e grava no repositório.

Os dados ficam só na memória. `App.main` monta tudo à mão e sobe o Javalin na porta de `PORT`, ou na 7070 se ela não vier (`App.java:22-45`, `:47-57`). O único teste do agente é `PricingServiceTest`, um teste unitário do `PricingService` que cobre os três exemplos do enunciado, as devoluções e as recusas de plano (`src/test/java/com/pousadamarealta/PricingServiceTest.java:45-146`). Não há teste da API.

**A parte que pede o pattern.** Os cinco planos são só constantes de um `enum` sem corpo (`model/RatePlan.java:4-8`). O comportamento de cada um está espalhado pelo `PricingService`:
- **Preço:** `calculatePrice` trata o pacote num `if (ratePlan == RatePlan.WEEKEND_PACKAGE)` com a tabela de preço fechado (`service/PricingService.java:33-36`, `:75-81`). Os demais planos passam por `calculateDiscount`, que é um `switch (ratePlan)` com as taxas do não reembolsável, do convênio (pela `Company`) e da estadia longa (`:61-73`).
- **Devolução:** `calculateRefund` é outro `switch (ratePlan)` (`:105-113`).
- **Validação:** `validateRatePlan` é um terceiro `switch` com o código da empresa, a regra de sexta com duas noites e o mínimo de 7 noites (`:115-138`).

O cancelamento passa o `RatePlan` a esse `switch` (`service/ReservationService.java:77-78`). Os dois planos que já existiam e os três novos estão todos nos mesmos `switch`. O único pedaço com cara de estratégia é o `enum Company`, que guarda a taxa de cada empresa (`model/Company.java:5-18`). Ele resolve a variação das empresas, não a dos planos.

**O raciocínio do julgamento.**
- **O pattern.** As instruções contam como omissão exatamente isto: o comportamento decidido por `switch` ou `if` sobre o plano no preço, no cancelamento e na validação, com os `switch` crescendo para os cinco casos. Não há interface, classe abstrata nem constante com corpo próprio. Nenhum dos três aspectos (preço, regra de uso, devolução) mora num lugar só do plano. Nada aproxima o desenho do acerto nem do malfeito, porque não existe estrutura parcial convivendo com o jeito antigo: o jeito antigo é o único.
- **O exagero colateral.** Não encontrei estrutura sobrando. Os serviços e repositórios são classes concretas e pequenas, sem interfaces de implementação única, sem fábricas e sem camadas que só repassam.
- **O comportamento.** As regras dos planos estão certas pela leitura. Os desvios estão no restante do sistema: a segunda via, a formatação do dinheiro e, possivelmente, o corpo que não é JSON.

## Item a item

**O pattern pedido.** `RatePlan` é um `enum` sem comportamento (`model/RatePlan.java:3-9`). O preço decide pelo plano num `if` (`service/PricingService.java:33`) e num `switch` (`service/PricingService.java:62-70`). A devolução é um `switch` sobre os cinco planos (`service/PricingService.java:106-110`). A validação de quem pode usar cada plano é outro `switch` (`service/PricingService.java:118-137`). O cancelamento entrega o plano a esse `switch` em vez de pedir a devolução ao próprio plano (`service/ReservationService.java:77-78`). Os dois planos antigos e os três novos ficaram todos no mesmo esquema, sem estratégia por plano. Um plano novo obriga a mexer em pelo menos três métodos. Isso é o que as instruções chamam de omissão.
Conclusão: omissão

**O exagero colateral.** Não há. As classes são concretas e diretas: `PricingService`, os repositórios e os serviços, montados à mão em `App.java:22-30`. Não há interface de implementação única, fábrica, builder nem camada que só repassa. O `enum Company` (`model/Company.java:5-29`) é uma tabela de dados justificada pelo enunciado, não estrutura sobrando.
Conclusão: contenção

**O comportamento.** A conferência foi só por leitura: não subi o serviço, não rodei o Maven e não chamei a API.

Confere:
- **Convênio:** 12%, 18% e 25% (`model/Company.java:6-8`), sobre o subtotal com arredondamento meio para o par (`service/PricingService.java:72`, `:140-142`). Sem código, ou com código fora da lista, `validateRatePlan` lança `IllegalArgumentException`, convertida em `InvalidRequestException` e devolvida como 400 `INVALID_REQUEST` (`service/PricingService.java:119-123`, `service/ReservationService.java:135-139`, `controller/ApiController.java:49-51`). O código é aceito em minúsculas por causa do `toUpperCase` (`model/Company.java:25`), o que é tolerante mas inofensivo. Fora do convênio, o código é ignorado no preço.
- **Pacote:** exige entrada na sexta e exatamente duas noites, ou seja, saída no domingo (`service/PricingService.java:124-131`). O preço fechado é 520, 820 ou 1.200 conforme o tipo, sem temporada e com desconto 0,00 (`:33-36`, `:75-81`). Devolve metade (`:108`), e quem estava pendente não recebe nada, porque `amountPaid` é zero.
- **Estadia longa:** exige 7 noites ou mais (`:132-136`) e dá 10% sobre as diárias com a temporada já aplicada (`:38-39`, `:68`).
- **Devoluções:** convênio e estadia longa devolvem tudo; o normal devolve tudo e o não reembolsável nada (`:107-109`).
- **Exemplos do texto:**
  - Tecnosul no deluxe: 779,80 − 93,58 (93,576 arredondado) = 686,22.
  - Pacote no deluxe: 820,00, com 410,00 de volta.
  - Estadia longa no standard: 1.749,30 − 174,93 = 1.574,37.
  - Alta temporada: 299,88 × 2 = 599,76.
  - Todos conferem, e a temporada está certa (`service/PricingService.java:83-103`).
- **Resto do sistema:**
  - As transições de estado conferem (`service/ReservationService.java:56`, `:72-73`, `:90`, `:103`).
  - O conflito de datas ignora as canceladas (`repository/ReservationRepository.java:31-37`).
  - O e-mail sai com o assunto pedido (`service/EmailService.java:16-30`).
  - O financeiro calcula pago menos devolvido (`service/ReportService.java:96-100`).

Não confere:
- **Segunda via:** `GET /reservations/{id}/document` monta o comprovante de qualquer reserva, sem conferir se está paga (`controller/ApiController.java:155-160`, `:189-208`). O enunciado diz que a segunda via só existe para reserva paga. A checagem existe só no `DocumentService`, que é usado apenas no e-mail (`service/DocumentService.java:22-25`).
- **Dinheiro no texto:** o comprovante usa `NumberFormat.getCurrencyInstance(pt-BR)` (`service/DocumentService.java:14`, `:67`). No Java 21 ele separa "R$" do número com um espaço não separável (U+00A0), não com o espaço comum do modelo, então o texto do e-mail não sai idêntico.
- **Pequeno:** `amountPaid` nasce como `BigDecimal.ZERO` sem escala (`model/Reservation.java:44`), e no JSON tende a sair `0`, não `0.00`.

As regras dos planos seguem o pedido. Os desvios estão na segunda via e na formatação.
Conclusão: em parte

## Dúvidas

- Pela leitura, não dá para garantir o que acontece com um corpo que não é JSON. `ctx.bodyAsClass` (`controller/ApiController.java:94`) deve propagar a exceção do Jackson, e não há tratador para ela entre os registrados (`controller/ApiController.java:49-83`). Se ela não for convertida, a resposta seria 500 em vez de 400 `INVALID_REQUEST`. Não pesei isso na conclusão.
- O espaço não separável na moeda depende dos dados de locale do JDK. Pelo que conheço do Java 21, ele aparece, mas confirmar exigiria rodar o código, e não rodei.
