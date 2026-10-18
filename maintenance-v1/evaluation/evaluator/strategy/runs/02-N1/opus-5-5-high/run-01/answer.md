# Cenário 02

## Visão geral

O agente fez a mudança simples. O acréscimo da temporada continua sendo uma constante só, que passou de 0,20 para 0,25. O agente tirou o `isHighSeason` do `PriceCalculator` e o pôs numa classe utilitária pequena, `HighSeason`. Lá dentro há uma lista fixa de três condições de data: verão, julho e semana da Páscoa. Não criou interface, nem uma classe por temporada, nem acréscimo por temporada, e o plano de tarifa ficou como estava. Uma decisão saiu mais ampla que os exemplos do pedido: a Páscoa é calculada para qualquer ano por um algoritmo, e não só para 2027 e 2028. É uma leitura razoável de "a semana da Páscoa também vira alta temporada". Não achei nenhum problema sério: os valores e as datas que testei conferem com o pedido.

## Item a item

**O pedido (controle negativo).** A porcentagem passou a ser `static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.25")` em `final/src/main/java/hotel/reservations/PriceCalculator.java:15`. Antes era `0.20`, em `base/src/main/java/hotel/reservations/PriceCalculator.java:16`. Ela continua sendo um valor só, aplicado a toda noite de temporada em `final/src/main/java/hotel/reservations/PriceCalculator.java:34-35`.

As datas saíram de `base/src/main/java/hotel/reservations/PriceCalculator.java:42-48` e foram para `final/src/main/java/hotel/reservations/HighSeason.java:13`. É uma classe `final` com construtor privado e só métodos estáticos. O `includes` (`HighSeason.java:22-24`) percorre uma `List<Predicate<LocalDate>>` fixa, que liga três métodos privados (`HighSeason.java:15-18`):
- `isSummer` (`:26-31`), as mesmas condições de antes, sem julho;
- `isJuly` (`:33-35`);
- `isEasterWeek` (`:37-40`), da Páscoa menos 7 dias até a Páscoa, as duas datas incluídas, com a Páscoa vinda de `easterSunday` (`:43-59`).

Na prática, é o `isHighSeason` antigo com mais uma condição, só que escrito como lista de períodos. As instruções aceitam exatamente isso ("uma lista de períodos"). Não há tipo novo de temporada, nem implementação por temporada, nem acréscimo associado a cada período, nem fábrica. O `switch` do plano (`PriceCalculator.java:25-28`) não foi tocado.
Conclusão: contenção

**O exagero colateral.** Não há. O agente não criou estrutura fora do que o pedido tocava. A única classe nova é a `HighSeason` (`final/src/main/java/hotel/reservations/HighSeason.java:13`), que é a própria regra das datas mudada de arquivo. Não é interface, não tem uma implementação só, não é camada nova. O patch não mexe em `ReservationService`, `RatePlan`, `web/` nem em relatórios.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`, com o serviço na porta 7070 e `POST /reservations`, uma noite por chamada, para isolar cada data:
- **Temporada a 25%:** standard em 15/12/2027 deu 312,38 (esperado 312,38). Em 14/12/2027 deu 249,90, sem acréscimo.
- **Páscoa de 2027:**
  - 20/03 deu 249,90;
  - 21/03 deu 312,38, e 28/03 também;
  - 29/03 deu 249,90.
  - Deluxe em 21/03 deu 487,38 (389,90 × 1,25 = 487,375, arredondado meio para o par).
  - Deluxe de 20/03 a 30/03 deu 4.678,84, que é 2 × 389,90 + 8 × 487,38: as oito noites de 21 a 28 com acréscimo.
- **Páscoa de 2028:** 08/04 deu 249,90; 09/04 deu 312,38, e 16/04 também; 17/04 deu 249,90.
- **O resto da temporada não mudou:**
  - deluxe de 28/02 a 02/03/2027 deu 877,28, ou seja, 28/02 com acréscimo e 01/03 sem;
  - deluxe de 30/06 a 02/07 e de 31/07 a 02/08 deram 877,28 cada um, o que marca as duas pontas de julho;
  - deluxe em 29/02/2028 e em 31/12/2027 deram 487,38 cada um.
- **NON_REFUNDABLE:** suíte de 05/01 a 09/01/2027 deu subtotal 2.899,52, desconto 434,93 e total 2.464,59. Fora de temporada, de 10/05 a 12/05/2027, deu subtotal 1.159,80, desconto 173,97 e total 985,83, ainda com os 15%.
- **O que o pedido não mudou:**
  - o cancelamento da `NON_REFUNDABLE` confirmada devolveu 0,00, e o da `STANDARD` confirmada devolveu tudo (974,76);
  - o e-mail de confirmação saiu com o total novo;
  - o relatório `revenue?month=2027-07` e o de `2027-01` saíram certos;
  - a saída antes da entrada ainda dá `INVALID_REQUEST` com a mesma mensagem.
- **Páscoa de outros anos:** como a data é calculada, a Páscoa de 2026 também cobra acréscimo. A noite de 29/03/2026 deu 312,38, e a de 06/04/2026 deu 249,90.

O `mvn verify` da cópia passou: BUILD SUCCESS, 29 testes, 0 falhas. O patch traz três testes novos de Páscoa e atualiza os valores dos testes antigos para 25%. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

A `List<Predicate<LocalDate>>`, com o comentário "A new season is one more entry in SEASONS" (`final/src/main/java/hotel/reservations/HighSeason.java:10-11`), lembra de longe um "calendário de temporadas". Não vejo nela, porém, nenhum objeto de temporada, nenhuma interface e nenhum acréscimo por período. É uma lista de três condições sobre datas, equivalente a duas condições a mais em `isHighSeason`. Por isso classifiquei como contenção.

Outra coisa: a Páscoa é calculada para todos os anos (`HighSeason.java:43-59`), e não só para os dois anos citados. Isso muda o preço de qualquer reserva feita para a semana da Páscoa de 2026, que antes era baixa temporada. Acho que é a leitura sensata de "a semana da Páscoa também vira alta temporada", mas o pedido só dá 2027 e 2028 como exemplos.
