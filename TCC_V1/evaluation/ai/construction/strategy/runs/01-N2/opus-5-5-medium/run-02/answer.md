# Cenário 01

## Visão geral

O agente aplicou o Strategy aos cinco planos de tarifa, incluindo o normal e o não reembolsável. Há uma interface `RatePlan` com três operações: validar, cotar e devolver. Cada plano tem a sua classe, e um registro (`RatePlans`) é o único lugar que sabe quais planos existem. A reserva e o hotel pedem tudo ao plano sem nunca dizer o nome dele. Não há `switch` nem `if` sobre o plano fora das classes de plano. As regras conferem com o enunciado, mas a conferência foi só por leitura do código. Não encontrei problema sério. O ponto mais discutível é pequeno: o plano normal é modelado como um "plano com desconto de 0%".

## Como foi implementado, e por que o julgamento

**O projeto.** Há dois pacotes:

- **`pousada`**:
  - `App` monta o Javalin, lê o JSON, valida os campos e transforma a resposta em JSON ou CSV.
  - `Hotel` guarda os quartos e as reservas e faz as operações.
  - `Reservation` cuida dos estados e do dinheiro pago e devolvido.
  - `Stay` é a estadia: o quarto, as datas, o código da empresa, a contagem das noites e a soma das noites com a temporada.
  - `Season` calcula a alta temporada, `Money` arredonda (HALF_EVEN) e formata os valores.
  - `Quote` guarda noites, diárias, desconto e total.
  - `Receipt` monta o comprovante, `Reports` monta os CSV e `Outbox` guarda os e-mails.
  - `ErrorCode` e `PousadaException` fazem os erros.
- **`pousada.plan`**: a interface `RatePlan`, a base abstrata `DiscountedPlan`, as cinco classes de plano e o registro `RatePlans`.

**O caminho de uma reserva.** `POST /reservations` chega em `App.createReservation` (`src/main/java/pousada/App.java:63-89`), que confere os campos e busca o plano com `RatePlans.find` (`App.java:86`). Depois chama `Hotel.create` (`src/main/java/pousada/Hotel.java:28-48`), que verifica, nesta ordem, se o quarto existe e a capacidade. Em seguida pede ao plano que valide a estadia (`Hotel.java:38`), confere se o quarto está livre e guarda a cotação feita pelo plano (`Hotel.java:45`).

**Dados, subida e testes.**
- Os dados ficam em listas na memória, dentro do `Hotel` e do `Outbox`.
- `App.main` sobe o servidor na porta da variável `PORT`, ou na 7070 se ela não vier (`App.java:137-140`).
- O `AppTest` tem cinco testes HTTP: a temporada no standard, o convênio com as duas recusas, o pacote com a devolução de 410,00, a estadia longa com a recusa por 6 noites, e um teste que cobre e-mail, relatórios, conflito de quarto e erros 400.

**A parte do pattern.**
- **A interface.** `RatePlan` (`src/main/java/pousada/plan/RatePlan.java:9-18`) declara `code()`, `validate(Stay)`, `quote(Stay)` e `refund(BigDecimal)`.
- **A base comum.** `DiscountedPlan` (`plan/DiscountedPlan.java:10-22`) é um template para os planos que cobram as noites e tiram uma porcentagem. Cada subclasse só diz a fração do desconto (`discountFraction`).
- **Os planos de antes:**
  - `StandardPlan`: desconto "0" e devolve tudo (`plan/StandardPlan.java:10-12`).
  - `NonRefundablePlan`: desconto "0.15" e devolve zero (`plan/NonRefundablePlan.java:11-13`).
- **Os planos novos:**
  - `CorporatePlan`: tem o mapa de empresas e descontos (`plan/CorporatePlan.java:10-13`), a validação do código (`:18-22`), o desconto pela empresa (`:24`) e a devolução total (`:26`).
  - `LongStayPlan`: exige pelo menos 7 noites (`plan/LongStayPlan.java:14-18`), dá 10% (`:20`) e devolve tudo (`:22`).
  - `WeekendPackagePlan`: implementa a interface direto. Tem o preço fechado por tipo de quarto (`plan/WeekendPackagePlan.java:14-17`), exige entrada na sexta e 2 noites (`:22-26`), cota sem temporada e sem desconto (`:29-31`) e devolve metade (`:33`).
- **A escolha do plano.** É um mapa preenchido num único ponto (`plan/RatePlans.java:11-17`).
- **Quem usa o plano.** A reserva guarda o plano e, no cancelamento, chama `plan.refund(amountPaid)` (`src/main/java/pousada/Reservation.java:66`). A resposta da API usa `plan.code()` (`App.java:33`). Uma busca por `ratePlan`, `RatePlan` e pelos nomes dos planos fora de `pousada.plan` só encontra declarações de tipo e essa busca do plano no registro.

**O raciocínio do julgamento.**
- **O que leva ao acerto.** É o caso que as instruções descrevem: uma interface, uma implementação por plano e um ponto de seleção por mapa. Os dois planos que já existiam também estão na estrutura. Cada plano tem preço, validação e devolução num lugar só dele, e quem calcula e cancela não pergunta qual é o plano. Um plano novo, como o da melhor idade, é uma classe nova e uma linha no registro.
- **O que poderia afastar dele, e por que não afasta.** A base `DiscountedPlan` concentra o cálculo, mas não decide nada pelo plano: só pede a fração à subclasse por polimorfismo. A busca do código da empresa fica no `App`, mas só repassa o valor ao `Stay`; quem interpreta o código é o `CorporatePlan`.
- **Exagero colateral.** `DiscountedPlan` é compartilhada por quatro planos. `Stay` é um objeto de parâmetros que os planos usam de verdade. `RatePlans` é o ponto de seleção que o pattern pede. Não há interface com uma implementação só, fábrica, builder ou camada que só repassa.
- **Comportamento.** As contas, as recusas e as devoluções conferem com o pedido, pela leitura do código (detalhes no item abaixo).

## Item a item

**O pattern pedido.** O agente criou a interface `RatePlan`, com validação, cotação e devolução (`plan/RatePlan.java:9-18`). Os cinco planos têm classe própria:

- `plan/StandardPlan.java:7`
- `plan/NonRefundablePlan.java:8`
- `plan/CorporatePlan.java:9`
- `plan/WeekendPackagePlan.java:13`
- `plan/LongStayPlan.java:8`

O plano é escolhido por um mapa num único ponto (`plan/RatePlans.java:9-27`). O hotel pede a validação e a cotação ao plano (`Hotel.java:38`, `Hotel.java:45`), e a reserva pede a devolução (`Reservation.java:66`). Não há `switch` nem `if` de comportamento sobre o plano em nenhum lugar, e os dois planos que já existiam estão na mesma estrutura dos novos. É o Strategy que o texto precisava.
Conclusão: acerto

**O exagero colateral.** Não há. `DiscountedPlan` (`plan/DiscountedPlan.java:10`) é reaproveitada por quatro planos. `Stay` (`src/main/java/pousada/Stay.java:8`) carrega o que os planos usam. `RatePlans` é o ponto de seleção do próprio pattern. O resto (`Hotel`, `Reports`, `Receipt`, `Outbox`) tem papel próprio e não só repassa chamadas.
Conclusão: contenção

**O comportamento.** A conferência foi por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.

- **Convênio Tecnosul**, deluxe de 10/03 a 12/03/2027: 779,80 × 0,12 = 93,576, que arredonda para 93,58; o total dá 686,22 (`CorporatePlan.java:24`, `DiscountedPlan.java:18-21`, `Quote.java:6-8`). Confere.
- **Descontos das empresas**: Agrovale 0,18 e Farmavida 0,25, sobre as diárias (`CorporatePlan.java:10-13`). Confere.
- **Recusa do convênio**: sem código ou com código fora da lista, dá 400 com INVALID_REQUEST (`CorporatePlan.java:19-20`, `PousadaException.java` com `invalid`, `ErrorCode.java`). Nos outros planos o `companyCode` é ignorado. Confere.
- **Pacote**:
  - Só é aceito com entrada na sexta e 2 noites, ou seja, saída no domingo (`WeekendPackagePlan.java:23`). O dia 12/03/2027 é sexta.
  - Cobra 520,00, 820,00 ou 1.200,00 conforme o quarto, sem temporada e com desconto zero (`WeekendPackagePlan.java:14-17`, `:30`).
  - Cancelado depois de pago, devolve 410,00 de 820,00 (`:33`).
  - Confere.
- **Estadia longa**, standard de 01/03 a 08/03/2027: 7 × 249,90 = 1.749,30, desconto de 174,93 e total de 1.574,37. A temporada continua valendo, porque o cálculo usa `Stay.nightsTotal` (`Stay.java:14-20`). Com menos de 7 noites, dá 400 (`LongStayPlan.java:15-16`). Confere.
- **Convênio e estadia longa devolvem tudo** (`CorporatePlan.java:26`, `LongStayPlan.java:22`). Confere.
- **Normal e não reembolsável**:
  - O normal devolve tudo e o não reembolsável dá 15% de desconto e devolve zero.
  - A temporada vai de 15/12 até fevereiro, mais julho, com 20% a mais na noite (`Season.java:10-18`). O exemplo de 599,76 confere.
  - Uma reserva pendente cancelada devolve 0, porque `amountPaid` é zero.
  - Com desconto zero, o comprovante omite a linha de desconto (`Receipt.java:166`).
  - Confere.
- **O resto do sistema**: as transições de estado (`Reservation.java:48-68`), os relatórios (`Reports.java`), o e-mail (`Hotel.java:63-68`) e os códigos de erro seguem o enunciado pela leitura.
Conclusão: segue

## Dúvidas

- O plano normal herda de `DiscountedPlan` com desconto "0" (`StandardPlan.java:10`). Funciona, e o comprovante esconde o desconto zerado, mas é uma modelagem um pouco forçada. Não muda a classe.
- O código da empresa é comparado exatamente como chega (`CorporatePlan.java:19`), então "tecnosul" em minúsculas é recusado. Isso cabe no texto, que define o código em maiúsculas.
- A validação do plano acontece antes de conferir se o quarto está livre (`Hotel.java:38-43`). Assim, um pedido que fere as duas regras recebe 400, e não 409. O enunciado não define essa ordem.
