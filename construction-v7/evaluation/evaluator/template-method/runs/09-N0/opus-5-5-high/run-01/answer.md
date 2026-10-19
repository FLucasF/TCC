# Cenário 01

## Visão geral

O agente não usou Template Method nem nenhuma outra estrutura nos relatórios. Os quatro (ocupação, financeiro, taxa de turismo e planos de tarifa) são quatro métodos do `ReportService`. Cada um repete a sequência inteira: a linha de título, o cabeçalho, a seleção e a ordem das reservas, as linhas e o total. A seleção "reservas pagas que começam no mês" foi copiada três vezes. O resto do sistema não tem estrutura sobrando: são dois serviços concretos e um `Main` com as rotas. O título dos quatro relatórios e o relatório da taxa de turismo saem certos. O problema mais sério no comportamento é que o financeiro e o de planos deixam de fora a reserva não reembolsável cancelada, que o enunciado manda contar. A ocupação conta reservas pendentes. No resto do sistema também há erros: o preço das noites usa a temporada da data de entrada para a estadia toda, e o dinheiro sai como "R$ 779.80" no comprovante.

## Como foi implementado, e por que o julgamento

**O projeto.** É um único módulo Maven, o pacote `com.pousadamareaalta`, com três partes:
- `Main` (`Main.java:20-378`) monta tudo por campos estáticos (`Main.java:22-24`), sobe o Javalin na porta de `PORT` (`Main.java:27-30`) e declara todas as rotas em lambdas. Cada lambda lê o corpo com Jackson, chama o serviço e monta o JSON à mão em `HashMap`s. Os erros viram respostas num `switch` sobre a mensagem da `IllegalArgumentException` (`Main.java:339-377`).
- `model` guarda `Reservation`, `Room` e `Message`, que são POJOs com enums de status e de plano (`Reservation.java:4-10` no arquivo do modelo).
- `service` tem três classes:
  - `ReservationService` guarda os quartos, as reservas e as mensagens em `HashMap`/`ArrayList` na memória. Ela cria a reserva, valida, faz as transições de estado e gera o comprovante e o e-mail (`ReservationService.java:11-210`).
  - `PricingService` calcula a diária, a soma, o desconto e o total.
  - `ReportService` gera os CSVs.

O caminho de uma requisição: a rota em `Main` chama `ReservationService`, que usa `PricingService` e o mapa de reservas. Nos relatórios, a rota chama `ReportService`, que lê `reservationService.getReservations()`. Os testes do agente são 26, todos passando: 12 em `PricingServiceTest` e 14 em `ReservationServiceTest`. Nenhum testa relatório nem rota HTTP.

**Os relatórios.** Não há hierarquia, interface nem escritor genérico. Há quatro métodos públicos:
- `generateOccupancyReport` (`ReportService.java:20-42`);
- `generateRevenueReport` (`ReportService.java:56-85`);
- `generateTourismTaxReport` (`ReportService.java:87-120`);
- `generateRatePlansReport` (`ReportService.java:122-161`).

Cada um faz por conta própria:
- abre um `StringBuilder`;
- escreve o título com a literal "Pousada Maré Alta" copiada quatro vezes (`ReportService.java:23`, `:59`, `:90`, `:125`);
- escreve o cabeçalho (`:24`, `:60`, `:91`, `:126`);
- filtra e ordena as reservas;
- percorre a lista acumulando o total;
- fecha com a linha `total`.

O filtro "começa no mês e está pago" foi copiado em `:62-68`, `:93-99` e `:136-140`. As únicas coisas compartilhadas são utilitários: `isPaidReservation` (`:163-167`), `formatDate` e `formatMoneyForCsv`. As regras de cada caso ficam dentro do método dele: o valor da taxa é a constante `TOURISM_TAX_PER_GUEST_PER_NIGHT` (`:14`, usada em `:103-106`), e o acúmulo por plano é feito com dois `EnumMap` (`:128-146`). As quatro rotas em `Main` também são cópias quase iguais (`Main.java:211-285`).

**O julgamento.** As instruções do pattern pedem o esqueleto (título, cabeçalho, linhas, total) num lugar só e cada relatório preenchendo só o que muda. Aqui não existe esqueleto nenhum, nem para parte dos relatórios. Os quatro estão escritos inteiros, cada um com a sua linha de título. É exatamente o caso que as instruções chamam de omissão. Os utilitários compartilhados não chegam a ser um "esqueleto em função utilitária" que justificasse malfeito, porque não escrevem nenhum passo do CSV: só formatam um valor. Para pedir o relatório de cancelamentos que o contador já anunciou, seria preciso um quinto método copiado.

**O exagero colateral.** Não há interface, fábrica, builder nem camada que só repassa. O único sinal é um `PricingService` instanciado e nunca usado em `Main.java:24`, que é código morto e não estrutura.

**O comportamento.** O que pesou foi o seguinte:
- o filtro `isPaidReservation` exclui CANCELLED, e por isso o financeiro e o de planos perdem a reserva não reembolsável cancelada;
- a ocupação filtra só "não cancelada" (`ReportService.java:46`) e conta as pendentes;
- `calculateNightlyPrice` é chamado uma vez, com a data de entrada (`ReservationService.java:66`);
- `formatMoney` depende do locale da máquina (`ReservationService.java:203-205`);
- o cancelamento deixa `refund` como null quando não há devolução (`ReservationService.java:159-161`);
- reserva inexistente nas rotas POST vira 400 em texto puro (`Main.java:96`, `:116`, `:136`, `:156`).

## Item a item

**O enunciado (pede o pattern).** Os quatro relatórios são métodos independentes que repetem o título, o cabeçalho, a seleção, o laço e o total: `ReportService.java:20-42`, `:56-85`, `:87-120` e `:122-161`. A linha de título está copiada em `:23`, `:59`, `:90` e `:125`, e a seleção das reservas pagas do mês em `:62-68`, `:93-99` e `:136-140`. Não há classe abstrata, método-modelo nem escritor genérico que receba as partes que variam. Só se compartilham formatadores e o predicado `isPaidReservation` (`:163-175`). É o caso descrito como omissão: os quatro relatórios escritos inteiros, cada um com a sua linha de título.
Conclusão: omissão

**O exagero colateral.** Não há. A montagem é direta, com dois serviços concretos sem interface e as rotas no `Main` (`Main.java:22-24`). O `PricingService` instanciado em `Main.java:24` e nunca usado é código morto, não estrutura.
Conclusão: contenção

**O comportamento.** Criei 7 reservas em julho e março de 2027: confirmações, um cancelamento não reembolsável confirmado (nº 2, R$ 1.193,09), um cancelamento normal, um check-out, um check-in e uma reserva pendente (nº 5).

O que conferiu:
- O título "Pousada Maré Alta,<nome>,<período>" sai certo nos quatro relatórios, com `text/csv; charset=utf-8`.
- A taxa de turismo de 2027-07 deu `4,…,4,3,54.00`, `1,…,2,2,18.00`, `7,…,2,2,18.00` e `total,,,,,90.00`, com as colunas e a ordem pedidas.
- O de planos em agosto, sem reservas, traz as duas linhas zeradas na ordem pedida.
- O exemplo do enunciado (STANDARD de 10/07 a 12/07/2027) deu 599.76.
- 409 ROOM_UNAVAILABLE, 404 ROOM_NOT_FOUND, 422 CAPACITY_EXCEEDED e 400 INVALID_REQUEST (corpo que não é JSON, datas invertidas, mês inválido) saem certos.
- A segunda via de uma reserva pendente dá 409.

O que não conferiu:
- **Financeiro e planos.** O financeiro deu `total,,,,3482.80` sem a reserva nº 2, quando o esperado era 4675.89. O de planos deu `NON_REFUNDABLE,1,795.40` e `total,3,3482.80`, quando o esperado era `NON_REFUNDABLE,2,1988.49` e `total,4,4675.89`. A causa é `isPaidReservation` (`ReportService.java:163-167`), que exclui a reserva cancelada.
- **Ocupação.** A de 2027-07-10 lista a reserva pendente nº 5 e dá `total,2/5`, quando o esperado era `total,1/5` (`ReportService.java:46`).
- **Comprovante e e-mail.** Saem com "Diárias: R$ 779.80", quando o pedido é "R$ 779,80" (`ReservationService.java:204`).
- **Temporada.** De 13/12 a 17/12/2027 deu 999.60, quando o esperado era 1099.56. De 30/06 a 02/07 deu 499.80, quando o esperado era 549.78. A temporada vem só da data de entrada (`ReservationService.java:66`).
- **Devolução.** Ao cancelar a não reembolsável, o `refund` fica null em vez de 0.00.
- **Reserva inexistente.** `POST /reservations/999/confirmation` e `/check-in` devolvem 400 com o texto `RESERVATION_NOT_FOUND` em vez de 404 em JSON.
- **Segunda via.** A de uma reserva cancelada sai com 200.
- **JSON.** O dinheiro sai como `0.0` e `779.8`, não com 2 casas.

`mvn -q verify` na cópia: 26 testes, 0 falhas, build com sucesso.
Conclusão: em parte

## Dúvidas

Não houve dúvida na classificação. Um detalhe ficou em aberto: as linhas `total` do financeiro (`total,,,,X`, 5 campos para 6 colunas) e da taxa (`total,,,,,X`, 6 campos para 7 colunas) não alinham o valor com a coluna dele. O enunciado diz só "'total' e o valor", então não contei isso como erro.
