# Cenário 01

## Visão geral

O agente montou a estrutura do Strategy: uma interface `RatePlanStrategy` com uma implementação para cada um dos cinco planos, inclusive o normal e o não reembolsável, e um `EnumMap` que escolhe a implementação. A validação, o preço e a devolução de cada plano ficam na classe dele, e o cancelamento pede a devolução ao plano sem nomeá-lo. Essa parte está boa. O problema mais sério está no serviço: na criação da reserva, ele pergunta se o plano é `CORPORATE`, faz cast para `CorporateRatePlan` e chama um overload que recebe o código da empresa. O motivo é que a interface não passa o `companyCode` para o cálculo do preço. A versão da interface no plano convênio calcula o preço sem desconto, e só dá o valor certo porque o serviço desvia dela. As regras dos cinco planos funcionam como o enunciado pede. Na parte comum há falhas: o dinheiro no comprovante sai como `R$ 779.80` e `R$ 2.087.64`, corpo que não é JSON, plano desconhecido ou data mal formatada dão 500, e a segunda via de uma reserva pendente responde 400 em vez de 409.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica no pacote `com.pousadamarealta`, dividido assim:
- `Main` registra as rotas Javalin, os tratadores de exceção e monta os objetos à mão, em campos estáticos (`src/main/java/com/pousadamarealta/Main.java:23-26`). A porta vem de `PORT`, com 7070 se ela não vier (`Main.java:68-74`).
- `dto/` tem os objetos de entrada e de saída do JSON.
- `exception/` tem uma `ApiException` com o código e o status, e uma subclasse para cada erro (`exception/ApiException.java`, `exception/InvalidRequestException.java`).
- `model/` tem `Reservation`, `Room`, os enums `RoomType` (com a capacidade e a diária), `RatePlan`, `ReservationStatus` e `MessageChannel`, e `Message`.
- `service/` tem `RoomService` (os cinco quartos num `HashMap`), `MessageService` (a lista de e-mails enviados), `ReservationService` (criação, transições, disponibilidade e o texto do comprovante e do e-mail), `ReportService` (os dois CSV) e as classes dos planos.

**O caminho de uma requisição.** A rota em `Main` lê o corpo com `bodyAsClass`, confere os campos obrigatórios (`Main.java:86-91`) e chama `ReservationService.createReservation`. Esse método valida os hóspedes, a capacidade e as datas, pede ao plano que valide, confere a disponibilidade, calcula o preço e guarda a reserva num `HashMap` em memória, com ids vindos de um `AtomicLong` (`service/ReservationService.java:13-14, 30-83`). O jar é gerado pelo shade plugin, com `Main` no manifesto.

**Os testes do agente.** São 10 testes JUnit que chamam os serviços direto, sem HTTP. Seis estão em `ReservationTest`: os exemplos do enunciado, o não reembolsável e a devolução do pacote. Quatro estão em `ReportTest`, sobre os relatórios.

**Os planos.** A interface `RatePlanStrategy` declara `validate(checkIn, checkOut, nights, companyCode)`, `calculatePrice(room, checkIn, checkOut, nights)` e `calculateRefund(amountPaid)`. Ela também traz métodos default para a alta temporada, o preço da noite e o arredondamento `HALF_EVEN` (`service/RatePlanStrategy.java:12-47`). Cada plano tem a sua classe:
- `StandardRatePlan`: soma as noites e devolve tudo (`service/StandardRatePlan.java:15-32`).
- `NonRefundableRatePlan`: dá 15% de desconto e devolve zero (`service/NonRefundableRatePlan.java:26, 33-35`).
- `CorporateRatePlan`: guarda o mapa de empresas e percentuais e recusa código ausente ou fora da lista (`service/CorporateRatePlan.java:12-23`).
- `WeekendPackageRatePlan`: exige duas noites, de sexta a domingo, cobra o preço fechado por tipo de quarto e devolve 50% (`service/WeekendPackageRatePlan.java:14-44`).
- `LongStayRatePlan`: exige 7 noites ou mais e dá 10% de desconto (`service/LongStayRatePlan.java:12-34`).

A escolha é feita por um `EnumMap<RatePlan, RatePlanStrategy>` preenchido no construtor do serviço (`service/ReservationService.java:17, 23-27`). A validação passa pela interface (`ReservationService.java:50-51`), e o cancelamento também: `strategies.get(...).calculateRefund(...)` (`ReservationService.java:116-117`).

**O raciocínio.** Até aqui o desenho é o que as instruções do pattern chamam de acerto: uma implementação por plano, os dois planos da parte comum incluídos, um mapa como ponto único de seleção e a validação e a devolução dentro de cada plano. O que o afasta do acerto é o cálculo do preço na criação:

```java
if (ratePlan == RatePlan.CORPORATE) {
    pricing = ((CorporateRatePlan) strategy).calculatePrice(room, checkIn, checkOut, nights, companyCode);
} else {
    pricing = strategy.calculatePrice(room, checkIn, checkOut, nights);
}
```
(`service/ReservationService.java:57-62`)

Quem usa a estrutura pergunta qual é o plano concreto e faz downcast. As instruções do pattern dão exatamente esse exemplo para `malfeito`. A causa é a interface: o contrato de preço não recebe o contexto da reserva (o código da empresa). Por isso o `CorporateRatePlan` tem duas versões de `calculatePrice`. A da interface devolve as diárias sem desconto (`service/CorporateRatePlan.java:26-38`), o que está errado para o convênio, e a certa é um overload fora do contrato (`CorporateRatePlan.java:40-56`). Na prática, um sexto plano que precise de algum dado da reserva vai exigir outro `if` com cast no serviço. Isso não é um `switch` de comportamento espalhado (é um ponto só, e o cancelamento e a validação estão limpos), então não chega a `omissão`. Mas é a estrutura convivendo com uma decisão por plano no lugar central do cálculo. Por isso dei `malfeito`.

À parte da classe: o laço que soma as noites está copiado em quatro classes, e duas vezes no convênio (`StandardRatePlan.java:16-25`, `NonRefundableRatePlan.java:16-25`, `CorporateRatePlan.java:27-36` e `41-50`, `LongStayRatePlan.java:20-29`). É duplicação, não muda a classe.

**O exagero colateral.** Fora dos planos não há estrutura sem necessidade. Os serviços são classes concretas sem interfaces, não há fábrica nem builder, e os DTOs e as exceções por código de erro têm uso real.

**O comportamento.** Todas as regras dos planos conferem. Na parte comum há falhas reais: o formato do dinheiro no texto do comprovante e do e-mail, os erros 500 em entradas inválidas e o código de erro e a regra da segunda via. Por isso dei `em parte`.

## Item a item

**O enunciado (pede o pattern).** Há uma interface `RatePlanStrategy` (`service/RatePlanStrategy.java:10-16`) com cinco implementações, escolhidas por `EnumMap` (`service/ReservationService.java:17, 23-27`). A validação e a devolução passam pela interface (`ReservationService.java:50-51, 116-117`). Mas o cálculo do preço na criação pergunta se o plano é `CORPORATE` e faz cast para chamar um overload com o `companyCode` (`ReservationService.java:58-59`). A versão de `calculatePrice` do contrato, no convênio, calcula sem o desconto (`service/CorporateRatePlan.java:26-38`), e a certa fica fora da interface (`CorporateRatePlan.java:40-56`). A estrutura existe e cobre os cinco planos, mas quem a usa ainda pergunta qual é o caso concreto. Não é o Strategy completo que o enunciado precisava.
Conclusão: malfeito

**O exagero colateral.** Não há. `RoomService`, `MessageService`, `ReservationService` e `ReportService` são classes concretas montadas à mão em `Main.java:23-26`, sem interface de uma implementação só, sem fábrica, builder ou camada que só repassa. As subclasses de `ApiException` (`exception/`) servem para carregar o código e o status de cada erro.
Conclusão: contenção

**O comportamento.** Testei na cópia, com o jar `target/reservas-1.0.0.jar`.

Conferem:
- **Exemplos do enunciado:** o STANDARD de 10/07 a 12/07/2027 dá 599,76. O convênio Tecnosul no 201, de 10/03 a 12/03/2027, dá 779,80 de diárias, 93,58 de desconto e 686,22 de total. O pacote no deluxe, de 12/03 a 14/03/2027, sai 820,00 e, pago e cancelado, devolve 410,00. A estadia longa no 102, de 01/03 a 08/03/2027, dá 1749,30, 174,93 e 1574,37.
- **Convênio:** Agrovale na suíte dá 208,76 de desconto (18%) e Farmavida dá 289,95 (25%). Sem código, com `ACME` ou com `tecnosul` em minúsculas, a resposta é 400 `INVALID_REQUEST`. Pago e cancelado, devolve tudo (951,04).
- **Pacote:** de sábado a segunda, de sexta a sábado e de sexta a um domingo uma semana depois, a resposta é 400. Em julho, num standard, sai 520,00, sem temporada. Na suíte, 1200,00.
- **Estadia longa:** com 6 noites, 400. Em julho no deluxe, 3275,16, 327,52 e 2947,64 (com a temporada valendo). Paga e cancelada, devolve tudo.
- **Outros planos:** o STANDARD com `companyCode` ignora o código. O NON_REFUNDABLE dá 116,97 de desconto e, pago e cancelado, devolve 0. Pendente cancelado devolve 0.
- **Transições:** cancelar de novo e fazer check-in de reserva pendente dão 409.
- **Erros comuns:** 404 de quarto e de reserva inexistentes, 409 `ROOM_UNAVAILABLE`, 422 `CAPACITY_EXCEEDED` e 400 para zero hóspedes e para saída igual à entrada.
- **Relatórios:** saem em `text/csv; charset=utf-8`, e o financeiro conta o pacote cancelado (410,00) e o não reembolsável cancelado (662,83).
- **Testes do agente:** o `mvn verify` passa, com 10 testes, 0 falhas e 0 erros.

Não conferem:
- **Dinheiro no comprovante:** sai como `R$ 779.80` e `R$ 2.087.64`, em vez de `R$ 779,80` e `R$ 2.087,64` (`service/ReservationService.java:207-209`). Isso afeta o e-mail de confirmação.
- **Erros 500:** corpo que não é JSON, `ratePlan` desconhecido, data `2027-9-1` e id `abc` dão 500 `INTERNAL_ERROR` em vez de 400 (`Main.java:44-53` só trata `BadRequestResponse`).
- **Segunda via:** para reserva pendente, dá 400 `INVALID_REQUEST`, quando a operação não permitida é 409 `OPERATION_NOT_ALLOWED`. Para reserva cancelada depois de paga, sai com 200, porque a regra olha para `amountPaid > 0` e não para a situação da reserva (`Main.java:159-161`).
- **Casas decimais:** `discount` e `amountPaid` zerados saem no JSON como `0`, e não com 2 casas.
Conclusão: em parte

## Dúvidas

Pesei entre `malfeito` e `acerto` com ressalva. O `if` com cast é um ponto só, e todo o resto passa pela interface. Fiquei com `malfeito` porque o caso bate com o exemplo das instruções do pattern ("quem usa a estrutura ainda pergunta qual é o caso concreto") e porque a interface ficou incompleta: o `calculatePrice` do contrato dá um preço errado para o convênio (`service/CorporateRatePlan.java:26-38`).

Uma regra pequena: o `companyCode` é guardado em todas as reservas, qualquer que seja o plano (`service/ReservationService.java:74`). Não pesou na classe.
