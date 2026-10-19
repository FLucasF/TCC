# Cenário 01

## Visão geral

O agente não usou nenhuma estrutura para os relatórios. Os quatro (ocupação, financeiro, taxa de turismo e planos de tarifa) são quatro métodos inteiros dentro de `ReservationService`, e cada um escreve à mão a sua linha de título, o cabeçalho, a seleção, a ordenação, o laço das linhas e o total. É o caso de omissão que as instruções do pattern descrevem. O resto do sistema é simples e sem estrutura sobrando. Também não há exagero: são uma classe de serviço, entidades e DTOs. Os quatro relatórios saem com o título e os valores certos num mês com dados. O problema de comportamento mais sério fica na parte comum. A confirmação exige um corpo `{"amountPaid": ...}` que o enunciado não prevê: chamada como o site chama, sem corpo, ela devolve 400. Chamada com `{}`, ela grava `amountPaid` nulo, e depois disso o relatório financeiro passa a responder 400 "Invalid month format".

## Como foi implementado, e por que o julgamento

**O projeto.** É um único pacote, `com.pousadamareal.reservas`, dividido assim:
- **`Main`** monta o Javalin, o `ObjectMapper` e todas as rotas, com a validação de entrada dentro delas (`Main.java:17-206`).
- **`domain/`** traz `Room`, `Reservation` (mutável, com `confirm`, `checkIn`, `checkOut` e `cancel`), `Message` e os enums `RatePlan`, `RoomType` e `ReservationStatus`.
- **`dto/`** traz classes de campos públicos para entrada e saída.
- **`util/PricingCalculator`** concentra as funções estáticas de preço: alta temporada, soma das noites, desconto e arredondamento meio-para-o-par (`util/PricingCalculator.java:13-67`).
- **`service/ReservationService`** guarda tudo o mais. Os quartos são uma lista constante (`service/ReservationService.java:13-19`). As reservas e as mensagens ficam num `HashMap` e numa `ArrayList` em memória (`:21-24`). Ali também moram as regras de estado, o comprovante, o e-mail e os quatro relatórios.

O caminho de uma requisição é sempre o mesmo. A rota lê o corpo ou o parâmetro e valida, chama um método público do serviço, e o serviço devolve um DTO ou uma `String` CSV. Erros viram `ApiException` com status e código (`Main.java:48-52`). Qualquer outra exceção vira 400 INVALID_REQUEST (`Main.java:54-59`). O `main` cria o serviço com `new` e sobe na porta de `PORT` ou 7070 (`Main.java:19-20`). O jar é sombreado pelo `maven-shade-plugin`, com `Main` no manifesto. Os testes do agente são 20, todos unitários: 10 do serviço (criação, confirmação, cancelamento, capacidade, e-mail) e 10 do `PricingCalculator`. Nenhum teste cobre os relatórios.

**A parte do fim (os relatórios).** Os quatro relatórios são quatro métodos públicos do serviço:
- **ocupação:** `generateOccupancyReport` (`service/ReservationService.java:300-325`);
- **financeiro:** `generateRevenueReport` (`:327-360`);
- **taxa de turismo:** `generateTourismTaxReport` (`:362-398`);
- **planos de tarifa:** `generateRatePlansReport` (`:400-450`).

Nenhum tipo representa um relatório. A escolha é feita pela rota, uma por relatório, e cada rota repete a mesma validação e o mesmo `Content-Type` (`Main.java:124-186`). Os passos repetidos são estes:
- **Título:** cada método tem a sua própria linha com "Pousada Maré Alta,<nome>," (`:302`, `:329`, `:364`, `:402`).
- **Cabeçalho:** cada um escreve o seu (`:303`, `:330`, `:365`, `:403`).
- **Seleção por mês e por estado:** copiada três vezes (`:335-341`, `:371-376`, `:415-421`).
- **Valor que fica com a pousada:** o cálculo "pago menos devolvido" está duplicado (`:346-348` e `:428-430`).
- **Linha "total":** cada um escreve a sua (`:323`, `:358`, `:396`, `:447-448`).

O valor da taxa, R$ 4,50, é uma variável local do método (`:369`). As únicas partes compartilhadas são dois formatadores privados, `formatDateForReport` e `formatAmountForReport` (`:452-458`).

**O raciocínio.** O Template Method pede o esqueleto (título → cabeçalho → linhas → total) num lugar só, com cada relatório preenchendo os passos que variam. A alternativa aceita é um escritor genérico que recebe essas partes. Aqui não existe nem uma coisa nem outra. A sequência dos passos está escrita quatro vezes, e a linha de título, que o enunciado acrescentou justamente para os quatro relatórios, foi copiada nos quatro métodos. Os dois formatadores não chegam a ser o "esqueleto" que, nas instruções, caracterizaria o malfeito, porque nem a escrita de uma linha CSV foi fatorada. Por isso a classe é **omissão**.

No exagero colateral, nada pesou. Não há interface com uma implementação só, fábrica, builder ou camada que só repassa. Se há um defeito estrutural, é o oposto: tudo fica concentrado num serviço único.

No comportamento, pesaram três coisas:
1. A confirmação depende de um `amountPaid` vindo do cliente (`Main.java:83-89`, `domain/Reservation.java:46-49`), quando o enunciado diz que o valor pago é o total da reserva.
2. Os valores zero saem como `0`, e não `0.00`, tanto no CSV quanto no JSON (`service/ReservationService.java:456-458`, `util/PricingCalculator.java:51`).
3. A seleção do financeiro conta qualquer reserva não reembolsável cancelada, mesmo a que foi cancelada ainda pendente e não deixou dinheiro nenhum (`service/ReservationService.java:341`). O relatório por plano herda o erro (`:421`).

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios são métodos independentes, cada um escrito do começo ao fim. Cada um tem a sua linha de título (`service/ReservationService.java:302`, `:329`, `:364`, `:402`), o seu cabeçalho, a sua seleção (`:305-312`, `:335-343`, `:371-378`, `:415-422`), o seu laço e a sua linha "total" (`:323`, `:358`, `:396`, `:447`). Não há classe abstrata, método-modelo ou passos abstratos. Também não há um escritor genérico que receba as partes que variam. As rotas também se repetem, uma por relatório (`Main.java:124-186`). O único código comum são dois formatadores de uma linha (`:452-458`), que não fixam a sequência dos passos. Um relatório novo, de cancelamentos ou de hóspedes por estado, seria mais uma cópia inteira. É exatamente a omissão das instruções: "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título".
Conclusão: omissão

**O exagero colateral.** Não há. As classes são diretas: entidades, enums, DTOs de campos públicos, um serviço e um calculador estático. Não aparece interface com implementação única, fábrica, builder nem camada de repasse.
Conclusão: contenção

**O comportamento.** Trabalhei numa cópia em `/tmp`. Rodei `mvn -q package -DskipTests`, subi o jar `reservas-1.0.0.jar` na porta 7070 e o derrubei no fim. Também rodei o `mvn -q verify`: passou, com 20 testes do próprio agente e nenhuma falha.

O que confere:
- **Preço:** um STANDARD de 10/07 a 12/07/2027 no quarto 101 deu `subtotal` 599.76 e `total` 599.76. Um NON_REFUNDABLE no 201, de 05/07 a 08/07, deu 1403.64 − 210.55 = 1193.09.
- **Recusas:** 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND, 404 RESERVATION_NOT_FOUND, 400 INVALID_REQUEST (saída igual à entrada, 0 hóspedes, corpo que não é JSON, mês 2027-13, data 2027-7-1) e 409 OPERATION_NOT_ALLOWED (check-in de pendente) vieram como esperado.
- **E-mail:** saiu com o texto do enunciado e o CPF formatado.
- **Títulos:** os quatro relatórios começam com "Pousada Maré Alta,occupancy,2027-07-10", "…,revenue,2027-07", "…,tourism-tax,2027-07" e "…,rate-plans,2027-07".
- **Ocupação:** em 10/07 deu `total,3/5`, em ordem de quarto. Em 12/07 a reserva que sai nesse dia não conta.
- **Financeiro:** veio em ordem de entrada e depois de número. Inclui a não reembolsável cancelada depois de paga (1193.09) e exclui a STANDARD cancelada com devolução. Total 3253.42.
- **Taxa de turismo:** colunas certas. 2 hóspedes × 2 noites = 18.00, 3 × 1 = 13.50, 2 × 3 = 27.00, total 58.50. Ficaram de fora a cancelada e a pendente.
- **Planos de tarifa:** `STANDARD,2,1295.64`, `NON_REFUNDABLE,2,1957.78` e `total,4,3253.42`. Em agosto, sem reservas, as duas linhas aparecem.

O que não confere:
1. **Confirmação sem corpo:** `POST /reservations/1/confirmation` sem corpo, do jeito que o enunciado descreve a chamada do site, deu `400 {"error":"INVALID_REQUEST","message":"Request body is required"}` (`Main.java:83-85`). Só com `{"amountPaid": ...}` dá para confirmar, e o valor aceito é qualquer um, não o total.
2. **Confirmação com `{}`:** confirma com `"amountPaid":null`. Depois disso, `GET /reports/revenue?month=2027-07` passa a responder `400 Invalid month format`, porque a exceção interna é engolida em `Main.java:151-152`.
3. **Valores zero:** em mês vazio, os relatórios mostram `STANDARD,0,0` e `total,0,0` (planos) e `total,0` (taxa), e não `0.00`. No JSON, `discount` sai `0` e `refund` sai `0`.
4. **Não reembolsável cancelada ainda pendente:** entra no financeiro como `7,202,…,CANCELLED,0`, e o relatório por plano passa a contar 3 reservas NON_REFUNDABLE, embora ela não tenha deixado dinheiro.
5. **Segunda via de reserva cancelada:** `GET /reservations/5/document` devolve 200. A regra só recusa a pendente (`service/ReservationService.java:174`).

Conclusão: em parte

## Dúvidas

Tenho uma só, e ela não muda a classe: o peso da falha da confirmação no comportamento. O enunciado não traz corpo para `POST /reservations/{id}/confirmation` e diz que o valor pago é o total. Por isso li a exigência de `amountPaid` como desvio da parte comum, que impede o site de confirmar do jeito combinado. Fiquei com "em parte", e não "não segue", porque as regras próprias dos quatro relatórios saem certas com dados reais quando a confirmação é feita mandando o total no corpo. Os desvios do fim do enunciado são menores: o formato dos zeros e a reserva não reembolsável cancelada ainda pendente, contada como se tivesse deixado dinheiro.
