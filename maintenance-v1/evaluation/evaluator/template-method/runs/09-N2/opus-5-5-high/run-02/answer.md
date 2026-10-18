# Cenário 09

## Visão geral

O agente não mudou nada. A `final/` é idêntica à `base/`: o `diff -r base final` não mostra nenhuma diferença e o `changes.patch` está vazio (0 bytes). Os dois relatórios antigos continuam escritos inteiros e sem a linha de título (`src/main/java/hotel/reports/OccupancyReport.java:23-45`, `src/main/java/hotel/reports/RevenueReport.java:23-48`). Também não há relatório de taxa de turismo nem de planos de tarifa, e o `HttpApi` só registra `/reports/occupancy` e `/reports/revenue` (`src/main/java/hotel/web/HttpApi.java:83-90`). Não sobra nada de desenho para elogiar, e o problema mais sério é que o pedido ficou inteiro sem atender. O `mvn verify` passa, mas só com os 26 testes que já existiam na base.

## Item a item

**O pedido (pede o pattern).** O pedido traz dois relatórios novos com os mesmos passos dos dois antigos, uma linha de título que vale para todos e o aviso de que virão mais. O que ele precisava era um esqueleto comum (título, cabeçalho, linhas, total) com os quatro relatórios preenchendo só os passos que mudam. Na `final/` não há classe abstrata, escritor genérico nem relatório novo:
- `OccupancyReport.generate` (`src/main/java/hotel/reports/OccupancyReport.java:23-45`) e `RevenueReport.generate` (`src/main/java/hotel/reports/RevenueReport.java:23-48`) seguem fazendo à mão a sequência inteira, como na base.
- O pacote `hotel/reports` tem só esses dois arquivos.
- As rotas novas não existem (`src/main/java/hotel/web/HttpApi.java:83-90`).

O esqueleto não existe, e a variação continua resolvida com cada relatório escrito inteiro, como antes. Dentre as classes previstas, a evidência sustenta melhor a omissão (ver "Dúvidas").
Conclusão: omissão

**O exagero colateral.** Não há. Nenhum arquivo foi criado ou alterado (`diff -r base final` vazio).
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q package -DskipTests` compilou e o `mvn verify` passou com 26 testes, 0 falhas. São os mesmos testes da base: `ReportsTest` 5, `ReservationServiceTest` 11, `PriceCalculatorTest` 6 e `HttpApiTest` 4. O agente não escreveu nenhum teste. Depois subi o serviço com `PORT=7070 java -jar target/hotel.jar` e criei duas reservas:
- a reserva 1: quarto 101, "Carla Dias", de 10 a 13/07/2027, 2 hóspedes, STANDARD, confirmada;
- a reserva 2: quarto 201, "Bruno, Lima", de 9 a 11/07/2027, 1 hóspede, NON_REFUNDABLE, confirmada e depois cancelada.

Com elas, chamei os quatro relatórios:
- `GET /reports/occupancy?date=2027-07-10`: esperava a primeira linha `Pousada Maré Alta,occupancy,2027-07-10`. Veio direto o cabeçalho `room,type,reservation,check_in,check_out`, depois `101,STANDARD,1,2027-07-10,2027-07-13` e `total,1/5`, sem título. Fora o título, o conteúdo está igual ao de hoje.
- `GET /reports/revenue?month=2027-07`: esperava `Pousada Maré Alta,revenue,2027-07` na primeira linha. Veio sem título: o cabeçalho, as reservas 2 (CANCELLED, 795.40) e 1 (CONFIRMED, 899.64) e `total,1695.04`. A reserva não reembolsável cancelada continua contando, como antes.
- `GET /reports/tourism-tax?month=2027-07`: esperava o título, as linhas da reserva 2 (1×2 noites = 9.00) e da reserva 1 (2×3 noites = 27.00), e `total,36.00`. Veio **404** `Endpoint GET /reports/tourism-tax not found`.
- `GET /reports/rate-plans?month=2027-07`: esperava o título, `STANDARD,1,899.64`, `NON_REFUNDABLE,1,795.40` e `total,2,1695.04`. Veio **404** `Endpoint GET /reports/rate-plans not found`.

Nenhuma das regras novas está implementada. O serviço foi derrubado no fim.
Conclusão: não segue

## Dúvidas

Este caso não se encaixa bem em nenhuma das seis classes. A `omissão` descreve "a mudança foi feita sem o pattern", e aqui a mudança nem foi feita: a execução não produziu nenhuma alteração. Pode ter sido uma falha da execução, e não uma decisão de desenho do agente. Escolhi `omissão` porque, pela evidência, o pattern não existe e os relatórios continuam escritos inteiros, um a um. O leitor deve saber, porém, que essa omissão não vem de uma solução com `if`s ou cópias, e sim da ausência total de mudança.
