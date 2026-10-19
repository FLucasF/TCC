# Cenário 09

## Visão geral

O agente montou um sistema simples e direto, com controlador, serviços e DTOs, sem camadas a mais. Os quatro relatórios não usam o Template Method nem nenhuma estrutura que o substitua. Cada um é um método inteiro em `ReportService`, que escreve sozinho o título, o cabeçalho, o laço das linhas e o total. A linha de título foi copiada quatro vezes (`service/ReportService.java:24`, `:44`, `:67`, `:108`), e a rota de cada relatório também foi copiada no controlador. O problema mais sério de desenho é esse: o próximo relatório que o contador pedir vai ser mais uma cópia.

No comportamento, o mais grave é que a confirmação, chamada como o site chama (sem corpo), dá 500 e nunca confirma nada. Além disso:
- a ocupação e o relatório por plano contam reservas pendentes;
- o financeiro lista linhas que não deixaram dinheiro;
- o comprovante escreve o dinheiro como `R$ 1403.64`;
- falta de campo e corpo que não é JSON dão 500;
- não há nenhum teste.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote é `com.pousadamareaalta` e se divide assim:
- `controller/ReservationController` registra todas as rotas e os tratadores de erro (`controller/ReservationController.java:27-43`).
- `service/` tem a regra:
  - `RoomService` guarda os cinco quartos fixos num `LinkedHashMap` (`service/RoomService.java:16-22`);
  - `PricingService` calcula noite, soma, desconto e total com `HALF_EVEN` (`service/PricingService.java:12-39`);
  - `ReservationService` guarda as reservas num mapa em memória e cuida das transições, do reembolso, do conflito de noites e do e-mail (`service/ReservationService.java:26-214`);
  - `ReceiptFormatter` escreve o texto do comprovante;
  - `MessageService` guarda a lista de mensagens enviadas;
  - `ReportService` gera os relatórios.
- `model/` traz as entidades, `dto/` as formas do JSON, e `exception/` uma subclasse de `ApiException` por código de erro.

Uma requisição sai da rota, passa pelo método do controlador, que converte o corpo com `bodyAsClass`, chega ao `ReservationService` e termina no mapa em memória. `Main` cria os serviços à mão, sobe o Javalin na porta de `PORT` (7070 por padrão) e registra as rotas (`Main.java:8-24`). O jar executável sai do `maven-assembly-plugin` como `target/app.jar`. O agente não escreveu testes: não existe `src/test`.

**Os relatórios.** Não há classe base, interface ou escritor genérico. Cada relatório é um método público de `ReportService`:
- `generateOccupancyReport` (`service/ReportService.java:20-38`);
- `generateRevenueReport` (`:40-61`);
- `generateTourismTaxReport` (`:63-88`);
- `generateRatePlansReport` (`:90-124`).

Cada um cria o próprio `StringBuilder`, escreve a linha `"Pousada Maré Alta,<nome>,"` com o período, escreve o cabeçalho literal, percorre as reservas montando a linha campo a campo e acrescenta o total. A seleção e a ordenação ficam em três auxiliares privados: `getOccupiedReservations`, `getMonthReservations` e `getPaidReservationsInMonth` (`:126-150`). O que se compartilha de fato são só os formatadores `formatDate`, `formatMonth` e `formatMoney` (`:152-162`). O cálculo do "dinheiro que ficou" também foi copiado: aparece uma vez no financeiro (`:49`) e outra no relatório por plano (`:103`). A taxa de R$ 4,50 é uma constante (`:13`). No controlador, os quatro tratadores (`controller/ReservationController.java:125-199`) repetem o mesmo bloco: ler o parâmetro, validar, chamar o método e definir o `Content-Type`.

**O julgamento.** As instruções do pattern dizem que é omissão "quando os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título". É exatamente o caso. Não existe um esqueleto (título → cabeçalho → linhas → total) em lugar nenhum, nem como método-modelo nem como composição. A sequência dos passos se repete nos quatro métodos. Os auxiliares de formatação não chegam a ser o "esqueleto como função utilitária" que caracterizaria o malfeito, porque nem mesmo a sequência foi fatorada. Não há estrutura do pattern convivendo com cópias: só há cópias. A classe é omissão.

**O exagero colateral.** Os serviços são classes concretas, sem interfaces de implementação única, fábricas ou builders. Os DTOs e as exceções por código cumprem um papel real. Não há exagero.

**O comportamento.** Os quatro títulos, a taxa de turismo e as linhas fixas do relatório por plano saíram certos. Mas o "pagas" e o "deixaram dinheiro" não foram respeitados na seleção:
- `getOccupiedReservations` só tira as canceladas (`service/ReportService.java:128`);
- `getMonthReservations` não filtra nada além do mês (`:137-142`), e por isso o financeiro e o relatório por plano incluem pendentes e canceladas com reembolso total.

A confirmação lê `amountPaid` do corpo (`controller/ReservationController.java:87-89`, `dto/ConfirmationRequest.java:4`), quando o enunciado manda usar o total da reserva. Sem corpo, dá 500. O comprovante usa `"R$ %.2f"` (`service/ReceiptFormatter.java:8`) em vez de `R$ 1.234,56`. Por isso, mesmo com o resto da parte comum funcionando, o comportamento segue só em parte.

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios estão escritos inteiros, cada um com a sua linha de título: `service/ReportService.java:24` (occupancy), `:44` (revenue), `:67` (tourism-tax) e `:108` (rate-plans). Cada um também tem o seu cabeçalho (`:25`, `:45`, `:68`, `:109`), o seu laço de linhas e o seu total (`:36`, `:59`, `:86`, `:122`). Não há classe abstrata, método-modelo, interface de partes ou escritor genérico que os relatórios atravessem. O que é comum (`formatDate`, `formatMoney`, `:152-162`) é formatação de valor, não o esqueleto. As rotas também foram copiadas (`controller/ReservationController.java:125-199`). Um relatório de cancelamentos ou de hóspedes por estado seria um quinto método copiado. É o caso descrito como omissão.
Conclusão: omissão

**O exagero colateral.** Não há. Os serviços são concretos e instanciados diretamente em `Main.java:11-15`, sem interfaces de implementação única, fábricas, builders ou camadas que só repassam. Os DTOs (`dto/ReservationDTO.java`, `dto/ReceiptDTO.java`) e o `ReceiptFormatter`, que é reutilizado no e-mail, têm uso real.
Conclusão: contenção

**O comportamento.** Testei numa cópia (`mvn -q package -DskipTests`, `PORT=7070 java -jar target/app.jar`), com seis reservas em julho de 2027:
1. 101 STANDARD de 10 a 12, confirmada;
2. 201 NON_REFUNDABLE de 05 a 08, 3 hóspedes, com check-out;
3. 202 NON_REFUNDABLE de 10 a 11, confirmada e cancelada;
4. 301 pendente;
5. 102 STANDARD confirmada e cancelada com reembolso;
6. 201 STANDARD de 10 a 12, com check-in.

- **Títulos.** Os quatro saíram certos: `Pousada Maré Alta,occupancy,2027-07-10`, `...,revenue,2027-07`, `...,tourism-tax,2027-07` e `...,rate-plans,2027-07`. O `Content-Type` é `text/csv; charset=utf-8`.
- **Taxa de turismo.** Certa. Saíram as reservas 2, 1 e 6, nessa ordem, com 40.50, 18.00 e 18.00 e `total,,,,,76.50`. A pendente e as canceladas ficaram de fora. Agosto vazio dá só o título, o cabeçalho e `total,,,,,0.00`.
- **Por plano.** Saiu `STANDARD,4,1535.52`, `NON_REFUNDABLE,2,1590.79` e `total,6,3126.31`. O esperado era `STANDARD,2,...` e `total,4,...`, porque a pendente 4 e a cancelada com reembolso 5 não deixaram dinheiro e o financeiro não deveria contá-las. Os valores batem e a NON_REFUNDABLE cancelada (397.70) está incluída. Em agosto, as duas linhas aparecem zeradas, como pedido.
- **Financeiro.** O total de 3126.31 está certo, mas a lista traz `4,...,PENDING,0.00` e `5,...,CANCELLED,0.00`, que deveriam ficar de fora (`service/ReportService.java:137-142`).
- **Ocupação de 10/07.** Lista a 301 pendente e fecha com `total,3/5`. O esperado era `total,2/5` (`:128`).
- **Preço.** A reserva 1 deu `subtotal 599.76`, como no exemplo do enunciado. A reserva 2 deu desconto de 210.55 e total de 1193.09, o que confere.
- **Confirmação como o site chama.** `POST /reservations/1/confirmation` sem corpo, ou com `{}`, devolve 500 INTERNAL_ERROR e a reserva continua pendente. Só funciona se o corpo trouxer `amountPaid`, e aí o valor pago passa a ser o que vier no corpo.
- **Comprovante e e-mail.** O assunto e o texto estão certos, mas o dinheiro sai como `R$ 1403.64` em vez de `R$ 1.403,64` (`service/ReceiptFormatter.java:8`).
- **Recusas.** Quarto já ocupado dá 409 ROOM_UNAVAILABLE, capacidade excedida dá 422, segunda via de pendente dá 409 e reserva inexistente dá 404, como pedido. Mas `guests: 0` dá 422 CAPACITY_EXCEEDED em vez de 400 (`service/ReservationService.java:34`), falta de campo e corpo que não é JSON dão 500 em vez de 400, e mês inválido dá 400.
- **JSON.** Os campos de dinheiro saem como `double`, então o desconto aparece como `0.0` e não com 2 casas (`dto/ReservationDTO.java:16-21`).
- **`mvn verify`.** BUILD SUCCESS com "No tests to run": o agente não escreveu nenhum teste.

O serviço foi derrubado no fim.
Conclusão: em parte

## Dúvidas

O enunciado não diz se as linhas de total do financeiro e da taxa devem completar as colunas vazias (`total,,,,3126.31`) ou ser só `total,<valor>`. O texto da taxa ("a última linha é 'total' e o valor") aponta para a forma curta, mas não contei isso como erro. Também não testei o `String.format` sem `Locale` (`service/ReportService.java:161`) com outra localidade da JVM: num ambiente pt_BR ele escreveria `599,76` no CSV. Nenhum dos dois pontos muda as conclusões.
