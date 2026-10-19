# Cenário 01

## Visão geral

O agente construiu os planos de tarifa como um Strategy limpo. Existe uma interface `RatePlan` com três operações: validar, cotar e devolver. Cada um dos cinco planos tem uma classe própria, o normal e o não reembolsável inclusive. Os planos ficam num catálogo que é um mapa por código. Nem o serviço nem a reserva perguntam qual é o plano: chamam `validate`, `quote` e `refund`. O resto do sistema é enxuto, sem camadas que só repassam. Tudo o que testei bate com o enunciado: os três exemplos do financeiro, as recusas, a devolução de cada plano, o comprovante, o e-mail e os relatórios. Não achei problema sério. O único detalhe é que a soma das noites com a temporada é calculada no serviço para todos os planos, e o pacote simplesmente a ignora (ver "Dúvidas").

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo está num único pacote, `marealta`:
- `App` monta o Javalin, registra as rotas e traduz `ApiException` para `{error, message}`.
- `ReservationRequest` lê e valida o JSON do POST e resolve o plano pelo código.
- `ReservationService` guarda os quartos e as reservas numa `ArrayList` em memória, cria a reserva (quarto, validação do plano, capacidade, conflito de noites, preço), confirma (e manda o e-mail pelo `Outbox`), cancela, faz check-in e check-out e tira a segunda via.
- `Reservation` guarda a máquina de estados e o valor pago e devolvido.
- `Room` e `RoomType` trazem a diária e o preço da noite com a temporada (`Season`).
- `Money` faz o arredondamento HALF_EVEN, as porcentagens e o formato em R$.
- `Receipt` gera o comprovante em JSON e em texto, e `Reports` gera os dois CSVs.
- `ReservationView` e `RoomView` são o formato de saída.

O caminho de uma criação é este:
1. A rota `POST /reservations` (`src/main/java/marealta/App.java:27`) chama `ReservationRequest.parse`, que pede o plano a `RatePlans.find` (`src/main/java/marealta/ReservationRequest.java:41`).
2. `ReservationService.create` (`src/main/java/marealta/ReservationService.java:30`) monta um `Stay` e chama `plan.validate(stay)` (`:35`).
3. Depois confere a capacidade e o conflito de noites, soma as noites com a temporada (`:47-50`) e pede ao plano o `Quote` (`:53`).

O sistema sobe por `App.main`, que lê a variável `PORT` com 7070 como padrão (`src/main/java/marealta/App.java:82-85`). O jar sombreado tem `marealta.App` no manifesto. Os testes do agente são 9 testes JUnit no nível do serviço, sem HTTP (`src/test/java/marealta/ReservationServiceTest.java`). Cobrem a temporada, o convênio, o pacote, a estadia longa, o não reembolsável, conflito e capacidade, e o e-mail.

**Os planos de tarifa.**
- **A interface.** `RatePlan` (`src/main/java/marealta/RatePlan.java:10-23`) declara `code()`, `validate(Stay)` (vazio por padrão), `quote(Stay, nightsTotal)` (por padrão sem desconto) e `refund(amountPaid)`.
- **As cinco implementações:**
  - `StandardPlan` devolve tudo (`src/main/java/marealta/StandardPlan.java:7`).
  - `NonRefundablePlan` dá 15% e não devolve nada (`src/main/java/marealta/NonRefundablePlan.java:9-13`).
  - `CorporatePlan` guarda o mapa das empresas, recusa código ausente ou desconhecido, aplica o desconto da empresa e devolve tudo (`src/main/java/marealta/CorporatePlan.java:7-26`).
  - `WeekendPackagePlan` valida a entrada na sexta com saída dois dias depois, usa o preço fechado por tipo de quarto e devolve metade (`src/main/java/marealta/WeekendPackagePlan.java:8-29`).
  - `LongStayPlan` exige 7 noites, dá 10% e devolve tudo (`src/main/java/marealta/LongStayPlan.java:6-22`).
- **A escolha do plano.** É feita num único ponto: um `LinkedHashMap` por código montado em `RatePlans.defaults()` (`src/main/java/marealta/RatePlans.java:16-27`).
- **O cancelamento.** Só chama `plan.refund(amountPaid)` (`src/main/java/marealta/Reservation.java:58`). Como uma reserva pendente tem `amountPaid` zero, a devolução sai zero sem nenhum `if`.
- **A procura por nomes de plano.** Um grep pelos nomes dos planos fora das classes de plano só encontra o registro no catálogo.

**O julgamento.** É exatamente o que as instruções chamam de acerto:
- há uma implementação por plano para os cinco, os dois da parte comum inclusive;
- preço, validação e devolução de cada plano moram no próprio plano;
- a seleção acontece num ponto só, por mapa;
- não há `switch` nem `if` de comportamento sobre o plano no serviço, na reserva, nos relatórios ou no comprovante.

Um plano novo, como o da melhor idade, é uma classe nova e uma linha em `RatePlans.defaults()`. O que poderia afastar do pattern é a soma das noites com a temporada ficar fora do plano (`src/main/java/marealta/ReservationService.java:47-50`). Mas ela é a mesma para todos os planos e não decide nada pelo plano. O pacote a ignora no próprio `quote` (`src/main/java/marealta/WeekendPackagePlan.java:26`), então não há decisão por plano fora da estrutura.

**O exagero colateral.**
- `RatePlans` tem um construtor que recebe uma lista, mas a interface que ele carrega tem cinco implementações reais, e ele pertence à parte do fim.
- Fora dessa parte não há interface com uma implementação só, nem fábrica, builder ou camada de repasse. `Outbox` é uma classe concreta, e `ReservationView` e `RoomView` são só formatos de saída.

**O comportamento.** Todos os valores, recusas e devoluções que testei batem com o enunciado, sem divergência.

## Item a item

**O enunciado (pede o pattern).** O agente fez uma interface `RatePlan` (`src/main/java/marealta/RatePlan.java:10`) com uma classe por plano para os cinco:
- `src/main/java/marealta/StandardPlan.java:5`
- `src/main/java/marealta/NonRefundablePlan.java:5`
- `src/main/java/marealta/CorporatePlan.java:6`
- `src/main/java/marealta/WeekendPackagePlan.java:7`
- `src/main/java/marealta/LongStayPlan.java:5`

Cada classe concentra a validação, o preço e a devolução do seu plano. A escolha é por mapa (`src/main/java/marealta/RatePlans.java:10-27`). A criação chama `validate` e `quote` sem nomear o plano (`src/main/java/marealta/ReservationService.java:35`, `:53`), e o cancelamento chama `refund` (`src/main/java/marealta/Reservation.java:58`). Não existe `switch` ou `if` sobre o plano em lugar nenhum fora das classes de plano. É o Strategy pedido, aplicado a todos os casos.
Conclusão: acerto

**O exagero colateral.** Não há. As únicas abstrações são a de `RatePlan`, que tem cinco implementações, e o catálogo `RatePlans`, ambos da parte do fim. O resto são classes concretas e diretas: `ReservationService`, `Reports`, `Outbox` e `Receipt` (`src/main/java/marealta/App.java:20-23`).
Conclusão: contenção

**O comportamento.** Rodei numa cópia: `mvn -q package -DskipTests` gerou `target/reservas-1.0.0.jar` com `Main-Class: marealta.App`. Subi o serviço na 7070 e chamei a API com `curl`.

- **Convênio:**
  - Tecnosul, deluxe de 10/03 a 12/03/2027: subtotal 779.80, desconto 93.58, total 686.22, como esperado.
  - Agrovale: desconto 140.36 (18%); Farmavida, na suíte: 289.95 (25%). Ambos corretos.
  - Sem código, com `ACME` ou com `tecnosul` em minúsculas: 400 `INVALID_REQUEST`.
- **Pacote:**
  - Deluxe de sexta 12/03 a domingo 14/03: 820.00, sem desconto. Depois de pago e cancelado, volta 410.00.
  - Standard em julho, alta temporada: 520.00. A suíte custa 1200.00, e cancelada devolve 600.00.
  - Entrada no sábado, de sexta a segunda e de sexta a sábado: 400 `INVALID_REQUEST`.
  - O `companyCode` enviado num plano que não é o convênio é ignorado.
- **Estadia longa:**
  - Standard de 01/03 a 08/03: 1749.30, 174.93 e 1574.37. Cancelada depois de paga, devolve tudo.
  - De 28/06 a 05/07, atravessando julho: 1949.22 e 194.92, ou seja, a temporada vale.
  - Com 6 noites: 400 `INVALID_REQUEST`.
- **Parte comum:**
  - Standard de 10/07 a 12/07: 599.76.
  - Não reembolsável na suíte em julho: desconto 208.76, total 1183.00; cancelado depois de pago devolve 0.00. O normal cancelado devolve tudo, e uma reserva pendente cancelada devolve 0.00.
  - Plano desconhecido dá 400; capacidade excedida, 422 `CAPACITY_EXCEEDED`; noites em conflito, 409 `ROOM_UNAVAILABLE`; cancelar de novo, 409 `OPERATION_NOT_ALLOWED`; segunda via de reserva pendente, 409; reserva inexistente, 404.
- **Comprovante e e-mail:** o comprovante no check-out e na segunda via vem com os campos pedidos. O e-mail tem o assunto e o texto do enunciado, com a linha "Desconto".
- **Relatórios:** os CSVs vêm em `text/csv; charset=utf-8`. A ocupação mostra `total,1/5`, contando quem entra na noite e não quem sai. O financeiro inclui o não reembolsável cancelado (1183.00) e a metade que ficou com a pousada no pacote cancelado (410.00).

O `mvn -q verify` da cópia passou: 9 testes, 0 falhas, 0 erros. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma que mude as conclusões. A soma das noites com a temporada é feita no serviço, antes de chamar o plano (`src/main/java/marealta/ReservationService.java:47-50`), e o pacote a recebe e ignora (`src/main/java/marealta/WeekendPackagePlan.java:26`). Não é uma decisão por plano fora da estrutura. Mas um plano futuro que precise do preço noite a noite, e não só da soma, faria mexer na assinatura de `quote`.
