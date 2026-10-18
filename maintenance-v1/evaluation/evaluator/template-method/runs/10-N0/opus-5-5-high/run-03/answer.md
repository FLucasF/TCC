# Cenário 10

## Visão geral

O agente fez a mudança simples que o pedido pedia. Em `OccupancyReport`, acrescentou a coluna `guest` no cabeçalho e um campo a mais em cada linha, trocando a vírgula do nome por um espaço. Não criou esqueleto de relatório, nem colunas configuráveis, nem estrutura pensando no telefone que "talvez um dia" a recepção peça. O `RevenueReport` ficou idêntico ao da base. O agente também ajustou os testes de ocupação que já existiam e acrescentou um teste para o nome com vírgula. Não achei problema sério.

## Item a item

**O pedido (controle negativo).** O patch muda só `OccupancyReport` e `ReportsTest`. O cabeçalho passou de `room,type,reservation,check_in,check_out` (`base/src/main/java/hotel/reports/OccupancyReport.java:34`) para `room,type,reservation,guest,check_in,check_out` (`final/src/main/java/hotel/reports/OccupancyReport.java:34`). O nome é limpo numa variável local, com `reservation.guest().name().replace(',', ' ')` (`final/src/main/java/hotel/reports/OccupancyReport.java:37`), e entra logo depois do id da reserva (`final/src/main/java/hotel/reports/OccupancyReport.java:41`). Fora isso, a classe continua inteira e concreta como na base, sem interface, classe abstrata, lista de objetos-coluna ou fábrica. O `diff` entre `base/src/main/java/hotel/reports/RevenueReport.java` e `final/src/main/java/hotel/reports/RevenueReport.java` não mostra diferença. É exatamente a "coluna a mais no `OccupancyReport`, com a troca da vírgula por espaço" que as instruções chamam de contenção.
Conclusão: contenção

**O exagero colateral.** Não há. O `changes.patch` só toca `src/main/java/hotel/reports/OccupancyReport.java` e `src/test/java/hotel/reports/ReportsTest.java`. Nenhum tipo, camada ou arquivo novo foi criado em `final/src/main`. `Guest` continua o mesmo record de três campos (`final/src/main/java/hotel/reservations/Guest.java:3`), sem campo de telefone antecipado.
Conclusão: contenção

**O comportamento.** Copiei a `final/` para `/tmp/av`, compilei com `mvn -q package -DskipTests` e subi o serviço com `PORT=7070 java -jar target/hotel.jar`. Pela API, criei cinco reservas:
- 201, "Silva, Maria José", STANDARD;
- 101, "Carla Dias", NON_REFUNDABLE;
- 301, "Ana, Beatriz, Costa", STANDARD;
- 102, "Pendente Silva", que deixei pendente;
- 202, NON_REFUNDABLE, que depois cancelei.

Confirmei a 1, a 2, a 3 e a 5, fiz o check-in da 3 e cancelei a 5.

- `GET /reports/occupancy?date=2027-07-10` voltou com `text/csv` e com a coluna no lugar certo e as vírgulas trocadas:
  ```
  room,type,reservation,guest,check_in,check_out
  101,STANDARD,2,Carla Dias,…
  201,DELUXE,1,Silva  Maria José,…
  301,SUITE,3,Ana  Beatriz  Costa,…
  total,3/5
  ```
  A reserva pendente ficou fora, como na base. Os nomes ficam com dois espaços porque `", "` vira `"  "`, o que é a troca literal que o pedido descreve.
- `?date=2027-07-12` trouxe só a suíte, com `total,1/5`. `?date=2027-08-01` trouxe só o cabeçalho e `total,0/5`.
- Sem `date`, e com `date=xx`, vieram os mesmos erros `INVALID_REQUEST` da base.

Para conferir o que não devia mudar, subi a `base/` na porta 7071 e repeti a mesma sequência de chamadas. O relatório de ocupação da final, sem a coluna 4, ficou idêntico ao da base. O `GET /reports/revenue?month=2027-07` ficou byte a byte igual, incluindo a não reembolsável cancelada e `total,4726.29`.

O `mvn -q verify` da cópia passou (exit 0), com 27 testes, nenhuma falha e nenhum erro: ReportsTest 6, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4. Entre eles está o teste novo do agente para o nome com vírgula (`final/src/test/java/hotel/reports/ReportsTest.java:110`). No fim derrubei os dois serviços e conferi que as portas 7070 e 7071 não respondiam mais.
Conclusão: segue

## Dúvidas

Nenhuma.
