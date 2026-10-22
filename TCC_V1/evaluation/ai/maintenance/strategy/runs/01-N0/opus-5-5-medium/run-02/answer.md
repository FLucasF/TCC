# Cenário 01

## Visão geral

O agente pôs os três planos novos no mesmo formato que já existia: a constante no `enum RatePlan`, sem comportamento, e uma decisão por `switch` em cada lugar onde o plano muda alguma coisa. O `switch` do preço no `PriceCalculator` e o do reembolso no `ReservationService.cancel` passaram a ter cinco casos, e surgiu um terceiro `switch`, também no serviço, para as regras de quem pode usar cada plano. As regras pedidas foram implementadas direito e os exemplos do financeiro conferem. O problema mais sério é o desenho: não há estratégia nenhuma. Cada plano novo (o da melhor idade, o dos 60 dias) vai exigir mudanças em três `switch`, em duas classes.

## Como foi implementado, e por que o julgamento

**O projeto.** O sistema monta tudo em `hotel.Hotel` (`final/src/main/java/hotel/Hotel.java:19-28`). Ele cria `Rooms`, um `ReservationStore` em memória, o `PriceCalculator`, o `EmailSender` e os relatórios, passa tudo ao `ReservationService` e entrega esse serviço ao `HttpApi` (Javalin). Os pacotes são estes:

- `web`: rotas, DTOs `CreateReservationRequest`/`ReservationResponse` e a tradução de `ErrorCode` para HTTP, por exemplo `INVALID_REQUEST` → 400 em `final/src/main/java/hotel/web/HttpApi.java:100-106`.
- `reservations`: o serviço, o cálculo de preço, `Reservation`, `Price`, `RatePlan` e o store.
- `rooms`: os cinco quartos, com tipo e diária, em `final/src/main/java/hotel/rooms/Rooms.java:11-15`.
- `messages`, `documents` e `reports`.

Uma criação de reserva segue este caminho:

1. O `POST /reservations` lê o corpo em `HttpApi.readNewReservation` (`final/src/main/java/hotel/web/HttpApi.java:109-130`). O `companyCode` é opcional e só é repassado.
2. `ReservationService.create` (`final/src/main/java/hotel/reservations/ReservationService.java:27-53`) valida as datas, os hóspedes, o quarto e a capacidade.
3. Em seguida, chama `validateRatePlan` (linha 42), confere a disponibilidade e pede o preço a `PriceCalculator.calculate` (linha 50).
4. A reserva é guardada no `ReservationStore`, em memória.

Os testes do agente cobrem:

- os três descontos do convênio, os três preços do pacote e a estadia longa no `PriceCalculatorTest`;
- as recusas e as devoluções no `ReservationServiceTest`;
- três cenários HTTP (400 e 201 com os totais) no `HttpApiTest`.

**A parte que pede o pattern.** O `RatePlan` continua um `enum` puro. Só ganhou três constantes, com comentários (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). Cada regra de plano mora num lugar diferente:

- **Preço:** `PriceCalculator.calculate` faz `switch (ratePlan)` sobre os cinco planos (`final/src/main/java/hotel/reservations/PriceCalculator.java:22-29`) e chama cinco métodos privados. Quatro deles repetem o mesmo laço de diárias (linhas 32-77). Os descontos por empresa ficam num `switch` de strings (linhas 81-87). Os preços do pacote ficam num `switch` sobre o tipo de quarto (linhas 90-96).
- **Validação:** `ReservationService.validateRatePlan` faz `switch (request.ratePlan())` com casos para `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY` (`final/src/main/java/hotel/reservations/ReservationService.java:56-77`). Ali dentro está a lista de códigos de empresa (linha 79-81), que repete a lista do `PriceCalculator`, e a regra de sexta a domingo (linhas 83-88).
- **Devolução:** o `switch` de `cancel` simplesmente cresceu para cinco casos (`final/src/main/java/hotel/reservations/ReservationService.java:121-127`).

**O raciocínio.** As instruções dizem que é omissão quando o comportamento dos planos é decidido por `switch` no cálculo do preço, no cancelamento ou numa validação. Em especial, quando "os `switch` que já existiam crescendo para os cinco casos". É exatamente o que aconteceu, nos três lugares. Não há interface, classe abstrata nem `enum` com corpo por constante. Nenhum plano tem as suas regras num lugar só dele, e nada aproxima o desenho do Strategy. Os métodos privados `calculateX` do `PriceCalculator` também não são estratégias: são ramos de um `switch` de comportamento, dentro de uma classe que conhece todos os planos.

**Exagero colateral:** nenhuma estrutura nova foi criada.

**Comportamento:** conferido por leitura, como pedido, e as regras batem com o texto.

## Item a item

**O pattern pedido.** O `RatePlan` segue sem comportamento (`final/src/main/java/hotel/reservations/RatePlan.java:3-14`). O plano decide o preço por `switch` em `final/src/main/java/hotel/reservations/PriceCalculator.java:23-29`, a devolução por `switch` em `final/src/main/java/hotel/reservations/ReservationService.java:121-127` e a validação por um terceiro `switch` em `final/src/main/java/hotel/reservations/ReservationService.java:57-76`. Os dois `switch` da base cresceram para cinco casos e um terceiro apareceu. Um plano novo continua exigindo mudanças em três lugares de duas classes. Isso é o oposto do Strategy que o texto pedia, e as instruções classificam esse desenho como omissão.
Conclusão: omissão

**O exagero colateral.** Não há. O agente não criou interface, fábrica, builder nem camada nova; só acrescentou campos (`companyCode` em `NewReservation`, `Reservation` e `CreateReservationRequest`) e métodos privados. Há duplicação: o laço das diárias aparece quatro vezes em `final/src/main/java/hotel/reservations/PriceCalculator.java:32-77`, e a lista de empresas aparece duas vezes (`PriceCalculator.java:81-87` e `ReservationService.java:79-81`). Mas isso é repetição, não estrutura demais.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, sem subir o serviço, sem rodar o Maven e sem chamar a API.

- **Convênio:** 12%, 18% e 25% (`PriceCalculator.java:83-85`). O desconto é sobre as diárias com a temporada e arredondado com `HALF_EVEN`, como na base. Sem código, ou com código fora da lista, o serviço lança `INVALID_REQUEST` (`ReservationService.java:58-61`), que vira 400 (`HttpApi.java:102`).
- **Exemplo da Tecnosul:** o deluxe custa 389,90 × 2 = 779,80, o desconto é 93,58 e o total é R$ 686,22. Confere.
- **Pacote:** só aceita entrada na sexta, saída no domingo e 2 noites (`ReservationService.java:83-88`); fora disso, 400. O preço é fechado em 520/820/1200 por tipo de quarto, sem temporada e sem desconto (`PriceCalculator.java:64-67`, `90-96`). A devolução é a metade (`ReservationService.java:125`). Para o deluxe de 12 a 14/03/2027: R$ 820,00, e R$ 410,00 de volta. Confere.
- **Estadia longa:** abaixo de 7 noites, 400 (`ReservationService.java:68-73`). O desconto de 10% é sobre o `nightPrice`, que inclui a temporada (`PriceCalculator.java:69-77`, `98-104`). Para o standard de 01 a 08/03/2027: 1.749,30 − 174,93 = R$ 1.574,37. Confere.
- **Devolução total:** o convênio e a estadia longa devolvem tudo (`ReservationService.java:124`, `126`).
- **Planos da base:** o normal (sem desconto, devolução total) e o não reembolsável (15%, devolução zero) mantêm a mesma lógica da base (`PriceCalculator.java:32-51`, `ReservationService.java:122-123`).
- **`companyCode`:** é opcional no HTTP e ignorado nos outros planos. Só é guardado na reserva.
- **Ressalvas pequenas:** o `getCorporateDiscount` lançaria NPE com código nulo e devolve `null` com código desconhecido (`PriceCalculator.java:82-86`). Isso só seria alcançável chamando o calculador direto, porque a validação do serviço vem antes. O `import java.time.DayOfWeek` em `PriceCalculator` ficou sem uso.

Conclusão: segue

## Dúvidas

Nenhuma.
