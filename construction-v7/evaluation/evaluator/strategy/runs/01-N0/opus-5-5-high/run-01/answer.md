# Cenário 01

## Visão geral

O agente fez dos planos de tarifa um Strategy de verdade. Há uma interface `RatePlan` com três operações: código, cotação com validação e devolução. Os cinco planos ficam registrados num mapa por código, e o serviço só pede ao plano: não nomeia nenhum, nem no cálculo nem no cancelamento. O normal, o não reembolsável e a estadia longa são instâncias com parâmetros de uma mesma classe `Discounted`, cada uma com o seu percentual, mínimo de noites e regra de devolução. O convênio e o pacote têm classe própria. O restante do sistema é enxuto e sem camadas a mais. Todos os valores e todas as recusas que testei conferem com o enunciado. O único ponto discutível é pequeno. O `companyCode`, que só o convênio usa, entra na assinatura geral `quote(...)` e é guardado em toda reserva. Um plano futuro que precise de outro dado, como a data em que a reserva foi feita para o plano de 60 dias de antecedência, vai obrigar a mudar a interface.

## Como foi implementado, e por que o julgamento

**O projeto.** É um pacote só, `br.com.mare`, com poucas classes:

- `Main` sobe o Javalin, define as rotas, faz o parsing e a validação do JSON e monta as respostas. A porta vem de `PORT`, com 7070 se ela não vier (`Main.java:17-19`).
- `Hotel` guarda os quartos, as reservas (`LinkedHashMap`) e as mensagens em memória. Também cuida das transições de estado, do comprovante e dos dois CSVs (`Hotel.java:16-23`).
- `Reservation` é a entidade, com o estado mutável. `Room` e `Message` são records, `ReservationStatus` é um enum com `isPaid()`, `Money` tem o arredondamento meio para o par e a formatação R$, e `ApiException` carrega o status e o código do erro.

O caminho de uma reserva começa em `POST /reservations` (`Main.java:34`). `Main.create` lê os campos, resolve o plano com `RatePlans.find(planCode)` (`Main.java:100`) e chama `Hotel.create`. Esse método acha o quarto, pede ao plano a cotação (`Hotel.java:34`), confere a capacidade e a disponibilidade e grava a reserva. Não há interfaces de repositório nem de serviço: `Main.createApp(new Hotel())` monta tudo.

O agente escreveu um único `ApiTest` com 6 testes HTTP. Eles cobrem a temporada, os três planos novos com os exemplos do enunciado, o ciclo de vida, o e-mail, os relatórios e os erros.

**A parte do fim do enunciado.**

- **A interface.** `RatePlan` (`RatePlan.java:11-26`) declara `code()`, `quote(room, in, out, companyCode)` e `refund(amountPaid)`. A `quote` calcula o subtotal e o desconto, ou recusa com `INVALID_REQUEST`.
- **O registro.** `RatePlans` registra as cinco instâncias num `LinkedHashMap` por código (`RatePlans.java:14-28`). Esse é o único ponto de seleção.
- **`Discounted`** (`RatePlans.java:54-79`) recebe o percentual, o mínimo de noites e um `UnaryOperator` para a devolução. Com ela são feitos:
  - STANDARD: 0%, mínimo de 1 noite, devolve tudo (`:17`);
  - NON_REFUNDABLE: 15%, mínimo de 1 noite, devolve zero (`:18`);
  - LONG_STAY: 10%, mínimo de 7 noites, devolve tudo (`:21`).
- **`Corporate`** (`RatePlans.java:82-100`) tem o mapa de empresas e percentuais, a recusa sem código ou com código desconhecido e a devolução integral.
- **`WeekendPackage`** (`RatePlans.java:103-119`) tem a regra de sexta a domingo, o preço fechado por tipo de quarto e a devolução de metade.
- **A temporada** e a soma das noites são funções compartilhadas (`RatePlans.java:32-49`). Os planos que cobram diária as usam, e o pacote não.
- **Quem usa a estrutura** só pede ao plano: `Hotel.create` chama `plan.quote(...)` (`Hotel.java:34`) e `Hotel.cancel` chama `r.plan.refund(r.amountPaid)` (`Hotel.java:80`). Uma busca por `CORPORATE|WEEKEND|LONG_STAY|NON_REFUNDABLE` fora de `RatePlans.java` não acha nada.

**O raciocínio.** As instruções do pattern dão como acerto uma interface com uma implementação por plano escolhida por um mapa. É o caso aqui. A `Discounted` com parâmetros não é um `switch` disfarçado: cada constante registrada carrega o próprio comportamento (percentual, mínimo e devolução), e nenhuma parte do código pergunta qual é o plano. Os cinco planos estão na estrutura, inclusive os dois da parte comum, e o preço, a validação e a devolução de cada um ficam juntos.

O que afasta um pouco do ideal é o `companyCode` na assinatura geral (`RatePlan.java:18`) e guardado em `Reservation` para todos os planos (`Reservation.java:14`), sem que ninguém o leia depois. É uma regra de entrada de um plano só que vazou para o contrato. Mas não há decisão por plano fora da estrutura, então a classe fica em acerto.

No exagero colateral não há nada: `Money` é um utilitário de verdade, e não há fábricas, builders, camadas que só repassam nem interfaces com uma implementação só fora dos planos. O comportamento conferiu em todas as chamadas que fiz.

## Item a item

**O enunciado (pede o pattern).** A interface `RatePlan` (`RatePlan.java:11-26`) tem cinco instâncias registradas por código (`RatePlans.java:16-22`), selecionadas num ponto só (`RatePlans.java:28`, usado em `Main.java:100`). O preço, a validação e a devolução de cada plano moram no próprio plano:

- `Discounted.quote/refund`: `RatePlans.java:69-78`;
- `Corporate`: `RatePlans.java:90-99`;
- `WeekendPackage`: `RatePlans.java:111-118`.

O serviço só delega: `Hotel.java:34` para o preço e `Hotel.java:80` para a devolução. Não há `switch` nem `if` sobre o plano em lugar nenhum, e o normal e o não reembolsável estão dentro da estrutura. Isso é o Strategy que o enunciado pede.
Conclusão: acerto

**O exagero colateral.** Não há. São um serviço concreto (`Hotel`), as rotas em `Main`, um utilitário de dinheiro (`Money.java`) e records simples. A única interface é a dos planos, que tem cinco instâncias.
Conclusão: contenção

**O comportamento.** O build com `mvn -q package -DskipTests` deu certo, e o serviço subiu na 7070 a partir de `pousada-mare-alta-1.0.0.jar`.

| Chamada | Esperado | Veio |
|---|---|---|
| Convênio TECNOSUL, deluxe, 10 a 12/03/2027 | 779,80, 93,58, 686,22 | confere |
| Convênio AGROVALE, mesmo deluxe | 140,36 de desconto | confere |
| Convênio FARMAVIDA, suíte | 289,95 de desconto | confere |
| Convênio sem código, com `ACME` e com `tecnosul` minúsculo | 400 `INVALID_REQUEST` | 400 `INVALID_REQUEST` |
| Pacote: deluxe 820,00, standard em julho 520,00 (sem temporada), suíte 1.200,00 | esses preços | confere |
| Pacote começando no sábado, com 3 noites e com 1 noite | 400 | 400 |
| Pacote pago e depois cancelado | devolução de 410,00 | 410,00 |
| Estadia longa, standard, 01 a 08/03/2027 | 1.749,30, 174,93, 1.574,37 | confere |
| Estadia longa com 6 noites | 400 | 400 |
| Estadia longa com 7 noites em julho | 2.099,16, 209,92, 1.889,24 (temporada vale) | confere |
| Cancelamento depois de pago: convênio, estadia longa e standard | devolvem tudo | confere |
| Cancelamento depois de pago: não reembolsável (509,80) | devolve 0,00 | 0,00 |
| Cancelamento de reservas pendentes | 0,00 | 0,00 |
| STANDARD com `companyCode` | código ignorado, 935,76 | confere |

Na parte comum também conferiu:

- 3 pessoas num standard dá 422 `CAPACITY_EXCEEDED`, e a sobreposição de noites dá 409 `ROOM_UNAVAILABLE`.
- O e-mail sai com o texto exato do enunciado e com a linha de desconto.
- A segunda via do pacote está correta.
- O financeiro de 03/2027 conta o pacote cancelado por 410,00, e o de 07/2027 conta o não reembolsável cancelado por 509,80. Os dois vêm em `text/csv; charset=utf-8`.
- O relatório de ocupação dá `total,1/5`.
- Um mês inválido dá 400.

No `mvn verify` da cópia passaram os 6 testes do agente, sem falhas. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

O `companyCode`, que é um dado de um plano só, faz parte da assinatura geral `RatePlan.quote` (`RatePlan.java:18`). Ele também é guardado em toda reserva, qualquer que seja o plano (`Reservation.java:14`, `Hotel.java:44-45`), e nada o lê depois. O próximo plano que precisar de outro dado de entrada vai obrigar a mudar a interface e todas as implementações. É uma regra de plano pequena fora do plano, que não é de preço nem de devolução, e por isso não mudou a classe.

Há também uma questão de ordem: a validação do plano roda antes da de capacidade e da de disponibilidade (`Hotel.java:34-43`). Assim, um pedido que quebra as duas regras recebe 400 e não 422 ou 409. O enunciado não define essa precedência.
