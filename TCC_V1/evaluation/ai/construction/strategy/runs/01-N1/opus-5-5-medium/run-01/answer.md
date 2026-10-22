# Cenário 01

## Visão geral

O agente aplicou o Strategy nos cinco planos de tarifa. Há uma interface `RatePlan` e uma classe para cada plano, e isso vale também para o `STANDARD` e o `NON_REFUNDABLE`, que já existiam. Cada classe guarda a sua validação, o seu preço, o seu desconto e a sua devolução. Um mapa escolhe a classe, e o serviço nunca nomeia um plano. As regras dos planos e os exemplos do enunciado conferem na leitura. O desenho tem dois pontos fracos que não mudam a classe. O primeiro: o mesmo laço de diárias com temporada está copiado em quatro classes. O segundo: o `CorporateRatePlan` guarda estado, porque o código da empresa é gravado no `validate` e lido depois no desconto. Por isso a fábrica cria uma instância nova a cada uso, por reflexão. O problema mais sério de comportamento fica fora dos planos: a segunda via do comprovante sai também para reservas que não foram pagas.

## Como foi implementado, e por que o julgamento

**O projeto.** O código fica no pacote `com.pousada`, dividido em:

- `controller`: as rotas do Javalin;
- `service`: `ReservationService`, `RoomService`, `MessageService` e `ReportService`;
- `model`: as entidades e os enums;
- `dto`: os formatos de entrada e saída;
- `exception`: uma exceção para cada código de erro;
- `rateplan`: os planos de tarifa;
- `util`: `MoneyUtil`, que arredonda com HALF_EVEN, e `DateUtil`, que diz se é temporada, sexta ou domingo.

`Application` monta tudo à mão. Cria os serviços e os controllers, configura o Jackson com `JavaTimeModule`, liga cada exceção à sua resposta de erro e sobe na porta da variável `PORT`, ou na 7070 se ela não vier (`src/main/java/com/pousada/Application.java:22-80`). Os dados ficam só em memória: um `HashMap` de reservas (`service/ReservationService.java:17`), os quartos fixos em `RoomService` e uma lista de mensagens.

Uma criação de reserva segue este caminho:

1. `ReservationController.createReservation` confere os campos obrigatórios, lê as datas e converte o `ratePlan` com `RatePlanType.valueOf` (`controller/ReservationController.java:36-62`, `:129-135`).
2. `ReservationService.createReservation` (`service/ReservationService.java:25-60`) busca o quarto e confere hóspedes, capacidade e ordem das datas.
3. O serviço pede o plano à fábrica e chama `ratePlan.validate(...)`.
4. Depois confere se o quarto está livre e calcula subtotal, desconto e total.

O agente escreveu 6 testes, que chamam o serviço direto, sem passar pela API (`src/test/java/com/pousada/ReservationTest.java`). Eles cobrem a alta temporada, o convênio Tecnosul, o pacote, a estadia longa, o e-mail de confirmação e o não reembolsável.

**A parte que pede o pattern.** A interface `RatePlan` (`rateplan/RatePlan.java:9-20`) tem cinco métodos: `getType`, `validate`, `calculateSubtotal`, `calculateDiscount` e `calculateRefund`. Cada plano tem a sua implementação:

- **Normal** (`StandardRatePlan`): desconto zero e devolve tudo (`rateplan/StandardRatePlan.java:42-49`).
- **Não reembolsável** (`NonRefundableRatePlan`): 15% de desconto e devolução zero (`rateplan/NonRefundableRatePlan.java:42-49`).
- **Convênio** (`CorporateRatePlan`): um mapa com as empresas e os descontos, a recusa de código ausente ou desconhecido e a devolução total (`rateplan/CorporateRatePlan.java:15-19`, `:29-34`, `:56-64`).
- **Pacote de fim de semana** (`WeekendPackageRatePlan`): duas noites, de sexta a domingo, com preço fechado por tipo de quarto, sem desconto, e devolve metade (`rateplan/WeekendPackageRatePlan.java:17-21`, `:29-58`).
- **Estadia longa** (`LongStayRatePlan`): 7 noites ou mais, 10% de desconto e devolução total (`rateplan/LongStayRatePlan.java:21-56`).

A escolha passa por um ponto só, um mapa em `RatePlanFactory` (`rateplan/RatePlanFactory.java:10-31`). Quem usa os planos só chama métodos da interface: a criação em `service/ReservationService.java:42-51` e o cancelamento em `service/ReservationService.java:93-94`. Não há `switch` nem `if` sobre o plano em nenhum lugar fora do pacote `rateplan`; a busca no código só achou o `valueOf` do controller e a passagem do `companyCode`.

**O raciocínio.**

- **O pattern:** é exatamente o que as instruções do pattern chamam de acerto. Há uma interface, uma implementação para cada um dos cinco planos (os dois antigos inclusive) e um mapa que escolhe. Preço, validação e devolução de cada plano ficam todos na classe do plano. Nada no cálculo nem no cancelamento pergunta qual é o plano concreto.
- **O que afasta do ideal sem mudar a classe:**
  - O laço de diárias com temporada está copiado em `StandardRatePlan:23-39`, `NonRefundableRatePlan:23-39`, `CorporateRatePlan:37-53` e `LongStayRatePlan:30-46`. É duplicação, e não uma decisão por plano.
  - O estado no `CorporateRatePlan` (`:21`, `:33`, `:57`) amarra o desconto à chamada anterior do `validate`. Por causa disso a fábrica instancia por reflexão (`RatePlanFactory.java:28`).
- **O exagero:** a fábrica é o próprio ponto de seleção do pattern, então não conta como exagero. Fora da parte dos planos não há interfaces de uma implementação só, builders nem camadas que só repassam.
- **O comportamento:** todas as regras dos planos conferem na leitura. No resto do sistema, a segunda via e talvez o tratamento de corpo inválido estão errados.

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlan` (`rateplan/RatePlan.java:9-20`) e cinco implementações, uma por plano, com os dois que já existiam inclusive (`rateplan/StandardRatePlan.java:11`, `rateplan/NonRefundableRatePlan.java:11`, `rateplan/CorporateRatePlan.java:13`, `rateplan/WeekendPackageRatePlan.java:15`, `rateplan/LongStayRatePlan.java:13`). Cada uma tem a sua validação, o seu preço, o seu desconto e a sua devolução. A escolha acontece num único mapa (`rateplan/RatePlanFactory.java:10-25`). O serviço só chama a interface, na criação (`service/ReservationService.java:42-51`) e no cancelamento (`service/ReservationService.java:93-94`), sem nomear nenhum plano. Não há `switch` nem `if` de comportamento por plano no projeto. Um plano novo, como o da melhor idade, seria uma classe nova e uma linha a mais no `register`. Esse é o Strategy que o texto pedia. Há duas ressalvas que não mudam a classe: o laço de diárias está duplicado em quatro classes, e o `CorporateRatePlan` guarda estado, o que obriga a fábrica a criar uma instância nova por reflexão (`rateplan/RatePlanFactory.java:28`).
Conclusão: acerto

**O exagero colateral.** Não há. `RatePlanFactory` é o ponto de seleção do próprio pattern. Fora dele, o projeto se resume a controllers, serviços, DTOs, exceções e dois utilitários estáticos (`util/MoneyUtil.java`, `util/DateUtil.java`). Não há interfaces de uma implementação só nem camadas que só repassam.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código, sem subir o serviço nem chamar a API.

O que confere:

- **Convênio:** 12%, 18% e 25% (`rateplan/CorporateRatePlan.java:15-19`). Código ausente ou fora da lista lança `InvalidRequestException` (`:30-31`), que vira 400 `INVALID_REQUEST` (`Application.java:48-50`).
- **Exemplo do convênio:** 779,80 × 0,12 = 93,576, que arredonda para R$ 93,58, e o total dá R$ 686,22. Confere.
- **Pacote:** exige 2 noites, entrada na sexta e saída no domingo (`rateplan/WeekendPackageRatePlan.java:30-42`). O preço é R$ 520,00, R$ 820,00 ou R$ 1.200,00, sem temporada e sem desconto (`:46-53`), e a devolução é metade (`:57`).
- **Exemplo do pacote:** 12/03/2027 é sexta; dá R$ 820,00 e devolve R$ 410,00.
- **Estadia longa:** recusa menos de 7 noites (`rateplan/LongStayRatePlan.java:24-26`), tem temporada no laço (`:37-39`) e 10% de desconto. O exemplo dá R$ 1.749,30, R$ 174,93 e R$ 1.574,37. Confere.
- **Normal e não reembolsável:** o normal em julho dá R$ 299,88 por noite e R$ 599,76 no total. O não reembolsável dá 15% de desconto e devolução zero.
- **Cancelamento de reserva pendente:** o `amountPaid` é zero, então a devolução é zero (`model/Reservation.java:42`, `service/ReservationService.java:94`).

Os problemas, fora dos planos:

- A rota `GET /reservations/{id}/document` monta o comprovante sem conferir se a reserva foi paga (`controller/ReservationController.java:103-113`). Uma reserva pendente ou cancelada recebe 200 com o comprovante, quando deveria receber 409 `OPERATION_NOT_ALLOWED`. A conferência de pagamento existe só no texto do e-mail (`service/ReservationService.java:157-159`).
- O corpo é lido com `ctx.bodyAsClass` (`controller/ReservationController.java:37`), e só a `BadRequestResponse` está ligada a uma resposta de erro (`Application.java:72-74`). Um corpo que não é JSON provavelmente cai num 500, e não no 400 pedido.
- `amountPaid` começa em `BigDecimal.ZERO` (`model/Reservation.java:42`), então sai `0`, e não `0.00`. O mesmo acontece com o `total` do financeiro quando o mês não tem reservas (`service/ReportService.java:70`, `:97`).

Conclusão: em parte

## Dúvidas

- Não tenho certeza de que um corpo que não é JSON vira 500. Depende de como o Javalin 6.7.0 trata o erro do Jackson dentro do `bodyAsClass`, e não executei nada para confirmar. Os outros problemas de comportamento (a segunda via de reserva não paga e os valores sem as 2 casas) não dependem disso.
- O estado guardado no `CorporateRatePlan` (`rateplan/CorporateRatePlan.java:21`, `:33`) e a instância nova por reflexão (`rateplan/RatePlanFactory.java:28`) são uma estratégia com estado e acoplada à ordem das chamadas. Funciona como está, então julguei como ressalva e não como motivo para outra classe.
- O código da empresa não fica gravado na reserva. O enunciado não pede que ele apareça na reserva, então não contei como falha.
