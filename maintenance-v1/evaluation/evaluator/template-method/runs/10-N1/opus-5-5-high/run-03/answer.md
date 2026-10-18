# Cenário 10

## Visão geral

O pedido é o controle negativo do nome do hóspede no relatório de ocupação, e o agente fez a mudança simples. Pôs uma coluna a mais no cabeçalho e um `append` a mais na linha do `OccupancyReport`, trocando a vírgula do nome por um espaço. Não criou esqueleto de relatórios, sistema de colunas nem nenhuma outra estrutura, e não mexeu no `RevenueReport`. O `diff -rq` entre `base/` e `final/` mostra só dois arquivos alterados: `OccupancyReport.java` e `ReportsTest.java`. Não achei nenhum problema sério. O comportamento testado pela API confere com o pedido, e o relatório financeiro saiu igual ao da base.

## Item a item

**O pedido (controle negativo).** O cabeçalho passou a ter `guest` logo depois de `reservation` (`final/src/main/java/hotel/reports/OccupancyReport.java:34`). Na linha, entrou um `.append(reservation.guest().name().replace(',', ' '))` entre o id da reserva e a entrada (`final/src/main/java/hotel/reports/OccupancyReport.java:40`). O resto do método continua como na base: a seleção das reservas, a ordenação e o total (`final/src/main/java/hotel/reports/OccupancyReport.java:24-31,44`). A frase "talvez um dia a recepção peça o telefone" não foi usada como desculpa para criar colunas configuráveis, uma interface ou uma classe base. É exatamente a "coluna a mais no `OccupancyReport`, com a troca da vírgula por espaço" que as instruções descrevem como contenção.
Conclusão: contenção

**O exagero colateral.** Não há. Nenhum arquivo de produção além do `OccupancyReport.java` foi alterado, e nenhum arquivo foi criado. Nos testes, o agente ajustou os dois casos que já existiam e acrescentou o caso da vírgula no nome (`final/src/test/java/hotel/reports/ReportsTest.java:53-55,65-66,71-85`).
Conclusão: contenção

**O comportamento.** Montei uma cópia da `final/` e outra da `base/`, compilei as duas com `mvn -q package -DskipTests` e subi a da `final/` na porta 7070 e a da `base/` na 7071. Nas duas, criei pela API as mesmas cinco reservas:
- 101 em nome de "Silva, Ana", confirmada;
- 201 em nome de "Carla Dias", não reembolsável, confirmada e com check-in feito;
- 301 em nome de "José, da, Costa", ainda pendente;
- 202 em nome de "Ãngela Müller", confirmada;
- 102, não reembolsável, confirmada e depois cancelada.

O que veio:
- **`GET /reports/occupancy?date=2027-07-10`:** veio `200`, `text/csv`, com o cabeçalho `room,type,reservation,guest,check_in,check_out`, as linhas `101,STANDARD,1,Silva  Ana,2027-07-10,2027-07-12` e `201,DELUXE,2,Carla Dias,2027-07-09,2027-07-11`, e `total,2/5`. A vírgula virou espaço, como o pedido diz. A reserva pendente e a cancelada ficaram de fora, como na base.
- **`date=2027-07-09`:** o nome com acento saiu intacto (`Ãngela Müller`).
- **`date=2027-08-01`:** veio só o cabeçalho e `total,0/5`.
- **Comparação com a base:** um `diff` das saídas mostrou que só mudaram o cabeçalho e a coluna nova. Todo o resto do relatório de ocupação ficou igual.
- **O que o pedido não mudou:** o `GET /reports/revenue?month=2027-07` saiu idêntico ao da base, com o mesmo cabeçalho, as quatro linhas e `total,3095.61`. O `GET /reports/occupancy?date=xx` continua devolvendo o erro `INVALID_REQUEST` com a mesma mensagem.
- **Testes do agente:** o `mvn verify` da cópia passou, com 27 testes, nenhuma falha e `BUILD SUCCESS`. Os testes de relatório (`ReportsTest`) são 6.

Derrubei os dois serviços no fim.
Conclusão: segue

## Dúvidas

Nenhuma. Um detalhe: "Silva, Ana" vira "Silva  Ana", com dois espaços, porque a vírgula é trocada por um espaço e o espaço que já vinha depois dela fica. É a leitura literal do pedido ("troca por um espaço"), e as instruções descrevem a mesma troca simples.
