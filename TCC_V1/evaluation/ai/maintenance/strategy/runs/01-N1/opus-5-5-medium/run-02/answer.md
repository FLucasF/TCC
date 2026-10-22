# Cenário 01

## Visão geral

O agente transformou o `enum RatePlan` numa estratégia. Cada constante tem o seu próprio corpo e sobrescreve só o que é dela entre quatro ganchos: `validate`, `discountRate`, `price` e `refund`. Os dois `switch` de comportamento da base sumiram. O cálculo do preço e o cancelamento agora pedem ao plano e não citam nenhum plano pelo nome. Os cinco planos ficaram na estrutura, inclusive o `STANDARD` e o `NON_REFUNDABLE`, que já existiam, e as regras de quem pode usar cada plano (empresa, sexta a domingo, 7 noites) também moram dentro do plano. Não há estrutura sobrando. Pela leitura, o comportamento confere com o pedido e com os exemplos do financeiro. Não encontrei problema sério. A única observação pequena é a ordem das checagens: a validação do plano roda antes da busca do quarto.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. `hotel.Hotel` monta as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:19-28`):

- `Rooms` guarda os quartos;
- `ReservationStore` guarda as reservas em memória;
- `PriceCalculator` calcula o preço;
- `EmailSender` e `SentMessages` cuidam dos e-mails;
- os relatórios ficam em `hotel.reports`;
- `HttpApi`, em `hotel.web`, registra as rotas.

O caminho de uma requisição é este:

1. `POST /reservations` chega em `HttpApi.readNewReservation`, que lê o `CreateReservationRequest` (agora com `companyCode`), confere os campos obrigatórios e monta um `NewReservation` (`final/src/main/java/hotel/web/HttpApi.java:109-131`).
2. `ReservationService.create` valida as datas e o número de hóspedes, chama `request.ratePlan().validate(request)` (`final/src/main/java/hotel/reservations/ReservationService.java:34`), busca o quarto, confere a capacidade e a disponibilidade e chama `priceCalculator.calculate(room, request)` (`:50`).
3. O `HotelException` vira o status HTTP no tratador de erros (`HttpApi.java:94-107`), e `INVALID_REQUEST` vira 400.

Os testes novos ficam em dois arquivos:

- `PriceCalculatorTest`: convênio das três empresas, pacote dentro e fora da temporada nos três tipos de quarto, estadia longa com e sem temporada;
- `ReservationServiceTest`: recusas do convênio, do pacote e da estadia longa, e a devolução de cada um.

**A parte do pattern.** O comportamento padrão fica em `RatePlan` (`final/src/main/java/hotel/reservations/RatePlan.java:92-110`):

- `validate` não recusa nada;
- `discountRate` é zero;
- `price` aplica `discountRate` sobre o subtotal da temporada;
- `refund` devolve o que foi pago.

Cada plano tem o seu corpo e sobrescreve só o que é dele:

| Plano | Validação | Preço | Devolução |
|---|---|---|---|
| `STANDARD` (`:18`) | padrão | padrão | padrão |
| `NON_REFUNDABLE` (`:20-30`) | padrão | `discountRate` de 15% | zero |
| `CORPORATE` (`:32-49`) | código no mapa das empresas (`:39-43`) | taxa do mapa (`:46-48`) | padrão |
| `WEEKEND_PACKAGE` (`:51-76`) | sexta e saída dois dias depois (`:58-64`) | sobrescreve `price` com o preço fechado por `RoomType`, sem temporada (`:67-70`) | metade (`:73-75`) |
| `LONG_STAY` (`:78-90`) | 7 noites (`:80-84`) | 10% (`:87-89`) | padrão |

`PriceCalculator.calculate` soma as noites com a temporada e entrega o resultado ao plano (`final/src/main/java/hotel/reservations/PriceCalculator.java:18-27`). O cancelamento chama `reservation.ratePlan().refund(...)` (`ReservationService.java:87`). Um `grep` por `switch` no `final/src/main` só encontra o `switch` de `ErrorCode` no `HttpApi`, que não é sobre plano.

**O julgamento.** As instruções aceitam como acerto "o `enum` como estratégia: cada constante com o seu próprio corpo". É exatamente esse o desenho:

- não há `switch (this)` nem `if (ratePlan == …)`;
- os cinco planos estão dentro da estrutura;
- preço, validação e devolução de cada plano moram juntos na constante;
- o cálculo e o serviço não sabem quais planos existem.

Um plano novo (melhor idade, 60 dias de antecedência) seria uma constante nova, sem mexer em `PriceCalculator` nem em `ReservationService`. O método-gancho `discountRate` dentro do `price` padrão é um template pequeno que deixa cada plano mais curto. Não é um `switch` disfarçado.

**Exagero colateral.** Não há interface sobrando, fábrica ou camada de repasse. O construtor extra de `NewReservation` (`final/src/main/java/hotel/reservations/NewReservation.java:15-18`) só mantém compatíveis as chamadas antigas.

**Comportamento.** As contas e as regras conferem por leitura (detalhes no item abaixo).

## Item a item

**O pattern pedido.** O agente usou o `enum` `RatePlan` como estratégia (`final/src/main/java/hotel/reservations/RatePlan.java:16-111`). Os comportamentos padrão estão em `:92-110`, e cada uma das cinco constantes sobrescreve os seus (`:18`, `:20-30`, `:32-49`, `:51-76`, `:78-90`). Os `switch` da base (`base/src/main/java/hotel/reservations/PriceCalculator.java:26` e `base/src/main/java/hotel/reservations/ReservationService.java:86`) foram trocados por chamadas ao plano:

- `final/src/main/java/hotel/reservations/PriceCalculator.java:26`;
- `final/src/main/java/hotel/reservations/ReservationService.java:87`;
- a validação em `final/src/main/java/hotel/reservations/ReservationService.java:34`.

Os dois planos antigos e os três novos estão na mesma estrutura, e nenhuma regra de plano ficou no serviço ou no cálculo. É a forma de "enum como estratégia" que as instruções contam como acerto.
Conclusão: acerto

**O exagero colateral.** Não há. Não há interfaces com uma implementação só, fábricas ou builders. A sobrecarga de construtor em `final/src/main/java/hotel/reservations/NewReservation.java:15-18` é só conveniência para as chamadas existentes.
Conclusão: contenção

**O comportamento.** Conferi por leitura, sem subir o serviço nem rodar o Maven.

- **Convênio:**
  - as taxas de 12%, 18% e 25% estão em `RatePlan.java:33-36`;
  - sem código ou com código desconhecido, sai `INVALID_REQUEST` (`RatePlan.java:40-41`), que vira 400 (`HttpApi.java:102`);
  - o exemplo deluxe de 10 a 12/03/2027 dá 779,80 × 0,12 = 93,576, que arredonda para 93,58, e o total fica em 686,22. Confere;
  - a devolução é integral (padrão em `:108-110`).
- **Pacote:**
  - só aceita entrada na sexta e saída exatamente dois dias depois (`:59-60`);
  - o preço fechado é 520,00, 820,00 e 1.200,00, com subtotal igual ao total e desconto zero, sem temporada (`:52-55`, `:67-70`);
  - a devolução é metade: 820,00 vira 410,00 (`:74`).
- **Estadia longa:**
  - menos de 7 noites é recusado (`:81`);
  - o desconto é 10% sobre o subtotal com temporada: no exemplo, 1.749,30, 174,93 e 1.574,37. Confere;
  - a devolução é integral.
- **Planos antigos:**
  - `STANDARD` continua sem desconto e devolve tudo;
  - `NON_REFUNDABLE` dá 15% (com o mesmo arredondamento `cents`, meio para o par) e devolve 0,00. É igual à base.
- **`companyCode`:** é lido do corpo (`HttpApi.java:130`) e ignorado nos outros planos.
- **Reserva pendente:** quando cancelada, devolve zero em qualquer plano, porque `amountPaid` é 0,00 (`Reservation.java:30`). É o mesmo que acontecia na base.
- **Ressalva:** a validação do plano roda antes da busca do quarto. Um pedido com quarto inexistente e plano inválido recebe 400 em vez de 404. Isso não contraria nada do pedido.

Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. A ressalva sobre a ordem entre a validação do plano e o 404 do quarto (`ReservationService.java:34-37`) é só uma observação. O código da empresa é comparado exatamente como foi enviado, então "tecnosul" em minúsculas seria recusado. Isso é coerente com o texto, que diz que o código é o nome em maiúsculas.
