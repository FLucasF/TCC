# Cenário 01

## Visão geral

O agente entregou um sistema completo e enxuto, que funciona. A variação dos relatórios, porém, ficou sem nenhuma estrutura comum. Os quatro relatórios (`OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport`) são classes soltas e cada uma escreve sozinha a sequência inteira: escolhe as reservas, ordena, escreve o título, o cabeçalho, as linhas e o total. A linha de título `"Pousada Maré Alta,<nome>,"` aparece copiada nos quatro. Não há classe-base, escritor genérico nem interface. O comportamento está correto em tudo o que testei, e não há estrutura demais no resto do sistema. O problema mais sério é a ausência do pattern que o enunciado pedia: o contador vai pedir mais relatórios, e cada um vai exigir uma quinta cópia do esqueleto.

## Como foi implementado, e por que o julgamento

**O projeto.** São seis pacotes em `src/main/java/hotel`:

- `rooms`: o record `Room`, o enum `RoomType` e `Rooms`, uma lista fixa com os cinco quartos (`rooms/Rooms.java:10-15`).
- `reservations`: `Reservation`, que guarda o estado e tem `isPaid()` e `amountKept()` (`reservations/Reservation.java:80-89`); `PriceCalculator`, com a alta temporada, o desconto e o arredondamento HALF_EVEN (`reservations/PriceCalculator.java:19-52`); `ReservationService`, com as transições, o cancelamento e o e-mail de confirmação (`reservations/ReservationService.java:27-132`); `ReservationStore`, um `LinkedHashMap` em memória (`reservations/ReservationStore.java:12-30`); e os enums e a exceção `HotelException` com `ErrorCode`.
- `documents`: o record `ReservationReceipt`, que é ao mesmo tempo o JSON e o texto do comprovante, e `TextFormat` para o R$ e as datas em português.
- `messages`: `SentMessages`, a lista de mensagens enviadas, e `EmailSender`, que grava nela com o canal `"EMAIL"`.
- `reports`: os quatro relatórios.
- `web`: `HttpApi`, com as rotas, a leitura do corpo e dos parâmetros e o mapeamento de `ErrorCode` para status HTTP (`web/HttpApi.java:116-123`), e os records de entrada e saída.

`Hotel` monta tudo à mão no construtor (`Hotel.java:21-31`) e o `main` lê a variável `PORT` (`Hotel.java:44`). O shade plugin gera `target/hotel.jar` com `hotel.Hotel` no manifesto.

Uma requisição segue este caminho: a rota do Javalin chama `ReservationService` ou o relatório certo, que lê a `ReservationStore` e devolve um record, ou o texto CSV no caso dos relatórios. Os erros sobem como `HotelException` e um handler único os converte em `{error, message}` (`web/HttpApi.java:109-114`).

Os testes do agente são 28 em quatro classes. `ReportsTest` (7) compara o CSV inteiro de cada relatório, inclusive o título (`src/test/java/hotel/reports/ReportsTest.java:53,79,108,121`). Os outros são `ReservationServiceTest` (11), `PriceCalculatorTest` (6) e `HttpApiTest` (4), que sobe o servidor.

**A parte do fim do enunciado.** Cada relatório é uma classe `final` com um `generate(...)` próprio, e nenhuma delas tem tipo em comum com as outras:

- `reports/OccupancyReport.java:23-46`: filtra `isPaid()` e a noite (24-30), ordena pelo quarto (31), escreve o título (34), o cabeçalho (35), as linhas (36-43) e `total,n/5` (44).
- `reports/RevenueReport.java:23-49`: filtra `amountKept() > 0` e o mês (24-31), ordena por entrada e depois por id (32), escreve o título (35), o cabeçalho (36), as linhas somando o total (37-46) e o total (47).
- `reports/TourismTaxReport.java:22-50`: filtra `isPaid()` e o mês (23-29), repete a mesma ordenação do financeiro (30), escreve o título (33), o cabeçalho (34) e as linhas com a taxa (35-47); a constante `TAX_PER_GUEST_PER_NIGHT` está em `:14`.
- `reports/RatePlanReport.java:20-48`: repete o filtro do financeiro linha por linha (21-28, cópia de `RevenueReport.java:24-31`), escreve o título (31), o cabeçalho (32), uma linha por `RatePlan.values()` (34-45) e o total (46).

A escolha do relatório não passa por nenhuma estrutura. São quatro campos e quatro parâmetros de construtor em `HttpApi` (`web/HttpApi.java:32-35, 37-44`) e quatro rotas quase iguais, cada uma chamando o seu `generate` e repetindo o `contentType` (`web/HttpApi.java:91-106`). O nome da pousada existe como constante em `ReservationService.java:12`, mas os relatórios usam o literal copiado quatro vezes.

**O raciocínio.** O Template Method pede o esqueleto (título, cabeçalho, linhas, total) num lugar só e cada relatório preenchendo os passos que mudam. A alternativa aceita é um escritor genérico que recebe essas partes. Aqui não existe nenhuma das duas coisas: não há classe abstrata, nem método-modelo, nem um escritor que receba colunas, seleção, linha e total. A sequência dos passos está escrita por inteiro em cada um dos quatro `generate`, e o título está copiado em cada um. É exatamente o caso que as instruções chamam de omissão. Não chega a ser malfeito, porque esse caso pressupõe um esqueleto parcial ou ao menos uma função utilitária compartilhada, e aqui não há nem isso.

No exagero colateral, o resto do sistema é direto: não há interfaces com uma implementação só, nem fábricas, builders ou repositórios abstratos. A camada mais fina é `EmailSender`, mas ela tem papel próprio (fixa o canal `EMAIL`); veja as Dúvidas.

No comportamento, todas as regras da parte do fim e as da parte comum que testei conferem.

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios estão escritos inteiros e independentes. Cada um tem a sua seleção, a sua ordenação e a sua linha de título copiada (`reports/OccupancyReport.java:34`, `reports/RevenueReport.java:35`, `reports/TourismTaxReport.java:33`, `reports/RatePlanReport.java:31`), além do seu cabeçalho, das suas linhas e do seu total. Não há classe-base, método-modelo nem escritor genérico. A seleção do financeiro está duplicada em `reports/RatePlanReport.java:21-28` e a ordenação em `reports/TourismTaxReport.java:30`. Também não há tipo comum para a API: são quatro dependências e quatro rotas copiadas (`web/HttpApi.java:91-106`). Um quinto relatório (cancelamentos, hóspedes por estado) exigiria mais uma cópia de tudo. Isso não é o Template Method nem a composição equivalente.
Conclusão: omissão

**O exagero colateral.** Não há. Os quartos são uma lista fixa (`rooms/Rooms.java:10-15`). O preço é uma classe concreta só (`reservations/PriceCalculator.java:14`). O armazenamento é um mapa sem interface (`reservations/ReservationStore.java:10-30`). A montagem é feita à mão em `Hotel.java:21-31`. Os records de entrada e saída da web (`web/CreateReservationRequest.java`, `web/ReservationResponse.java`) têm função real: validar os campos obrigatórios e dar forma ao JSON. `EmailSender` (`messages/EmailSender.java:12-14`) só repassa para `SentMessages`, mas acrescenta o canal e nomeia o envio de e-mail; não pesei como exagero.
Conclusão: contenção

**O comportamento.** Testei numa cópia (`/tmp/copia`). Rodei `mvn -q package -DskipTests` e subi `target/hotel.jar` na porta 7070. Criei reservas de julho de 2027: um STANDARD confirmado, um NON_REFUNDABLE confirmado e depois cancelado, um com check-in e check-out, um pendente, um STANDARD confirmado e cancelado, um com check-in, e um de agosto.

- Preço: 101 de 10/07 a 12/07 deu `subtotal 599.76`. O NON_REFUNDABLE no 201 por 3 noites deu `1403.64 − 210.55 = 1193.09`.
- Título: os quatro relatórios começam com `Pousada Maré Alta,occupancy,2027-07-10`, `…,revenue,2027-07`, `…,tourism-tax,2027-07` e `…,rate-plans,2027-07`, todos com `Content-Type: text/csv; charset=utf-8`.
- Taxa de turismo de julho: cabeçalho `reservation,room,check_in,check_out,guests,nights,tax` e as linhas `3,301,…,3,1,13.50`, `1,101,…,2,2,18.00` e `7,201,…,3,3,40.50` (a com check-in), seguidas de `total,72.00`. Ficaram de fora a pendente, a não reembolsável cancelada e a STANDARD cancelada. A ordem é a do financeiro. Um mês vazio dá `total,0.00`.
- Planos de julho: `STANDARD,2,1295.64`, `NON_REFUNDABLE,1,1193.09` (a cancelada conta) e `total,3,2488.73`, que bate com o `total,2488.73` do financeiro. Em agosto aparece `NON_REFUNDABLE,0,0.00` mesmo sem reserva.
- Financeiro e ocupação: o financeiro tem a ordem por entrada e depois por id, e a não reembolsável cancelada aparece com 1193.09. A ocupação de 12/07 não conta quem sai nesse dia e mostra `total,1/5`.
- Recusas: `month=2027-7` e `month=x` dão 400 INVALID_REQUEST, e a falta de `month` também. Pedi 3 hóspedes no 101 e veio 422 CAPACITY_EXCEEDED. Uma reserva em noite já ocupada deu 409 ROOM_UNAVAILABLE. A segunda via e o check-in de reserva pendente dão 409 OPERATION_NOT_ALLOWED. Reserva 99 deu 404, quarto 999 deu 404, e corpo que não é JSON, 0 hóspedes e saída igual à entrada deram 400.
- E-mail e comprovante: `GET /messages` traz o canal EMAIL, o assunto certo e o texto com o comprovante, inclusive a linha `Desconto: R$ 210,55` e `R$ 1.403,64`. O check-out devolve o comprovante com `title "Comprovante de reserva"`.

O `mvn verify` da cópia deu BUILD SUCCESS, com 28 testes, 0 falhas e 0 erros. No fim derrubei o serviço e confirmei que a porta 7070 não responde mais.
Conclusão: segue

## Dúvidas

Só uma, no exagero colateral: `EmailSender` (`messages/EmailSender.java:4-15`) é uma camada que repassa para `SentMessages.add`. Considerei-a justificada porque fixa o canal `EMAIL` e separa o "enviar" do "registrar", que o enunciado distingue. Um leitor mais rígido poderia chamá-la de camada que só repassa. Na classificação do pattern não houve dúvida.
