# Cenário 02

## Visão geral

Este pedido é um controle negativo, e o agente fez a mudança simples. Trocou a constante do acréscimo para 25% e pôs a Páscoa como mais uma condição em `isHighSeason`. Para isso escreveu dois métodos estáticos privados do pacote, `isHolyWeek` e `calculateEaster`, no próprio `PriceCalculator`. Não criou interface, classe por temporada, calendário de objetos nem acréscimo por temporada. O acréscimo continua sendo um só para todas as temporadas, como o pedido diz. Não há um problema de desenho sério. A única ressalva é de alcance: o agente calcula a Páscoa com um algoritmo em vez de guardar as duas datas pedidas. Com isso, toda Páscoa de qualquer ano vira temporada, inclusive a de 2026, que o pedido não citou. Nos testes, todos os valores do pedido conferiram, as datas antigas da temporada não mudaram, e o `mvn verify` passou.

## Item a item

**O pedido (controle negativo).** O agente mudou a constante em `final/src/main/java/hotel/reservations/PriceCalculator.java:16` (`HIGH_SEASON_SURCHARGE = new BigDecimal("0.25")`; na base era `0.20`). A semana da Páscoa entrou como mais uma condição na expressão que já existia, `|| isHolyWeek(date)`, em `final/src/main/java/hotel/reservations/PriceCalculator.java:51`. O método `isHolyWeek` (`PriceCalculator.java:55-59`) pega a data da Páscoa e confere se o dia está entre o domingo de Ramos (a Páscoa menos 7 dias) e a Páscoa. A data vem de `calculateEaster` (`PriceCalculator.java:62-78`), que é o algoritmo gregoriano anônimo, só aritmética. É uma função auxiliar e não uma estrutura: não tem tipo novo, polimorfismo, mapa de temporadas nem parâmetro de acréscimo por temporada. `nightPrice` continua aplicando o mesmo acréscimo a qualquer noite de temporada (`PriceCalculator.java:33-39`). Isso equivale às "duas condições a mais em `isHighSeason`", que as instruções aceitam como contenção. A diferença é que as datas são calculadas, não escritas como dado.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só toca `PriceCalculator.java` e dois arquivos de teste (`PriceCalculatorTest.java`, `HttpApiTest.java`). O `switch` dos planos em `final/src/main/java/hotel/reservations/PriceCalculator.java:26-29` e o resto do sistema ficaram como estavam. Nenhuma interface, fábrica ou camada nova foi criada.
Conclusão: contenção

**O comportamento.** Montei a cópia da `final/` e subi o serviço na porta 7070. Criei reservas de uma noite com `POST /reservations`:

- **Noite de temporada:** um standard em 15/12/2027 saiu `total: 312.38`, como o pedido diz. Em 14/12/2027 saiu `249.90`.
- **Páscoa de 2027:** as noites de 21/03, 25/03 e 28/03 saíram `312.38`. As de 20/03 e 29/03 saíram `249.90`.
- **Páscoa de 2028:** as noites de 09/04 e 16/04 saíram `312.38`. As de 08/04 e 17/04 saíram `249.90`.
- **Semana inteira:** de 21/03 a 29/03/2027 são 8 noites, e o total foi `2499.04` (8 × 312,38).
- **Datas antigas da temporada:** um deluxe de 10/07 a 12/07/2027 saiu `974.76` (2 × 487,38). De 31/07 a 02/08 saiu `877.28` (487,38 + 389,90). De 28/02 a 02/03/2027 também saiu `877.28`. A suíte `NON_REFUNDABLE` em janeiro deu subtotal `2899.52`, desconto `434.93` e total `2464.59`, o que confere com 25% e os 15% do plano.

Também conferi o que o pedido não mudou:

- **Baixa temporada:** um standard de 2 noites em agosto saiu `499.80`.
- **Reembolsos:** o standard confirmado e cancelado devolveu `499.80`. O `NON_REFUNDABLE` devolveu `0.00`.
- **Erros:** um quarto já ocupado deu `ROOM_UNAVAILABLE`, e hóspedes demais deram `CAPACITY_EXCEEDED`.
- **Relatório e e-mail:** o relatório financeiro de 2027-07 trouxe `total,974.76`, e o e-mail de confirmação mostrou "Diárias: R$ 974,76".

O `mvn -q verify` da cópia passou com 29 testes e nenhuma falha: 9 em `PriceCalculatorTest`, 11 em `ReservationServiceTest`, 4 em `HttpApiTest` e 5 em `ReportsTest`. No fim, derrubei o serviço.

Achei uma diferença fora do texto do pedido. A noite de 29/03/2026, que cai na semana da Páscoa de 2026, também saiu com acréscimo: a suíte custou `724.88` em vez de `579.90`. O pedido só fala de 2027 e 2028, mas diz que "a semana da Páscoa também vira alta temporada", o que permite ler a regra como geral. Por isso não contei como erro.
Conclusão: segue

## Dúvidas

- **A Páscoa calculada.** O pedido dá só as datas de 2027 e 2028. O agente generalizou para qualquer ano com `calculateEaster` (`final/src/main/java/hotel/reservations/PriceCalculator.java:62-78`). Não considero isso exagero: é uma função de cálculo, não uma estrutura nem um acréscimo parametrizado. Mas aumenta o alcance da mudança. Reservas de 29/03 a 05/04/2026 passam a ter acréscimo, coisa que o dono não pediu de forma explícita. Guardar as duas semanas como dado teria seguido o pedido mais à letra.
- **O README.** O agente não atualizou o README da `final/`, que ainda diz "a noite custa 20% a mais" e não cita a Páscoa. É só documentação desatualizada e não muda nenhuma das classes.
