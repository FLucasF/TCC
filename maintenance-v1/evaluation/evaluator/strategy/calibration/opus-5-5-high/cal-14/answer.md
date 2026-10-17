# Cenário 02

## Visão geral

O agente fez a mudança simples, como o controle negativo pedia. Trocou a constante do acréscimo de 0,20 para 0,25. Pôs as duas semanas da Páscoa como dado: uma lista privada de períodos de datas, que entra como mais uma condição em `isHighSeason`. Não criou interface de temporada, classe por temporada nem acréscimo por temporada, e não mexeu em mais nada fora do `PriceCalculator` e de dois testes. O comportamento confere com o pedido em tudo o que testei. O único problema é pequeno e não é de código: o README continua dizendo que a temporada custa "20% a mais" e não cita a Páscoa (`final/README.md:23-24`).

## Item a item

**O pedido (controle negativo).** Na base, o acréscimo era a constante `HIGH_SEASON_SURCHARGE = 0.20` (`base/src/main/java/hotel/reservations/PriceCalculator.java:16`), e as datas da temporada ficavam em `isHighSeason` (`base/src/main/java/hotel/reservations/PriceCalculator.java:41-47`). Na `final/`, o agente mudou o seguinte:

- A constante agora vale `0.25` (`final/src/main/java/hotel/reservations/PriceCalculator.java:17`), e continua sendo um único acréscimo para todas as temporadas, como o pedido mandava.
- A Páscoa entrou como dado numa lista `EASTER_WEEKS` com dois intervalos fechados, 21/03 a 28/03/2027 e 09/04 a 16/04/2028 (`final/src/main/java/hotel/reservations/PriceCalculator.java:21-23`). Ela usa um `record DateRange` privado com um método `contains` (`final/src/main/java/hotel/reservations/PriceCalculator.java:25-29`).
- `isHighSeason` ganhou uma condição a mais, `EASTER_WEEKS.stream().anyMatch(...)` (`final/src/main/java/hotel/reservations/PriceCalculator.java:60`). As regras antigas de dezembro, janeiro, fevereiro e julho ficaram como estavam (`final/src/main/java/hotel/reservations/PriceCalculator.java:56-59`).
- `nightPrice` não mudou (`final/src/main/java/hotel/reservations/PriceCalculator.java:45-51`).

O `DateRange` é um registro de dados privado da classe, sem polimorfismo e sem papel de "temporada". É exatamente a "lista de períodos" que as instruções aceitam como contenção. A isca "acréscimo diferente para cada temporada" foi ignorada, como devia.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só toca `PriceCalculator.java` e dois testes (`PriceCalculatorTest.java` e `HttpApiTest.java`). O `diff -r base final` confirma que nenhum outro arquivo mudou. O `switch` dos planos de tarifa ficou intocado (`final/src/main/java/hotel/reservations/PriceCalculator.java:38-41`).
Conclusão: contenção

**O comportamento.** Compilei uma cópia da `final/`, subi o serviço na porta 7070 e criei reservas com `POST /reservations`:

- **Acréscimo de 25%:**
  - Standard, de 15 a 16/12/2027: `total` 312.38, como o pedido diz.
  - Deluxe, de 29/02 a 01/03/2028: 487.38.
  - Deluxe, de 31/07 a 01/08/2027: 487.38.
  - Suíte `NON_REFUNDABLE`, de 01 a 03/07/2027: subtotal 1449.76, desconto 217.46 e total 1232.30. Confere com 724,88 × 2 e 15% arredondado meio para o par.
- **Páscoa de 2027:**
  - Noite de 20/03: 249.90, fora da temporada.
  - Noites de 21/03 e de 28/03: 312.38, as duas pontas incluídas.
  - Noite de 29/03: 249.90.
- **Páscoa de 2028:** de 08 a 18/04/2028, com 10 noites, o total veio 2998.84. São 2 × 249,90 + 8 × 312,38, ou seja, as noites de 09 a 16/04 contaram como temporada, e as de 08 e 17/04, não.
- **O que não mudou:**
  - Noite de 14/12: 249.90.
  - Noite de 01/03/2028: 389.90.
  - Noites de 30/06 e de 01/08: 389.90.
  - Páscoa de 2026, que não foi pedida: 389.90.
  - Cancelamento `STANDARD` pago devolve tudo (`refund` 312.38), e cancelamento `NON_REFUNDABLE` devolve 0.00.
  - Plano `VIP` é recusado com 400 `INVALID_REQUEST`.
  - O relatório de receita de 2027-03 e o de ocupação de 21/03/2027 saem em CSV com os valores novos.
  - O comprovante (`/document`) traz 312.38.

O `mvn -q verify` da cópia passou com 27 testes e nenhuma falha. São 11 em `ReservationServiceTest`, 5 em `ReportsTest`, 7 em `PriceCalculatorTest` (um novo, `easterWeekIsHighSeason`) e 4 em `HttpApiTest`, com os valores atualizados para 25%. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Fica só a anotação de que o README não foi atualizado: ainda diz "a noite custa 20% a mais" e não cita a Páscoa (`final/README.md:23-24`). Isso é documentação, e não altera nem o desenho nem o comportamento testado.
