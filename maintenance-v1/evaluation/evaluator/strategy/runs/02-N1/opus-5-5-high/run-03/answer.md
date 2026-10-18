```markdown
# Cenário 02

## Visão geral

Este é o pedido da alta temporada que mudou, o controle negativo. O agente fez a mudança simples e não acrescentou estrutura. Mexeu num arquivo só, `PriceCalculator.java`. Trocou a constante `HIGH_SEASON_SURCHARGE` de 0.20 para 0.25 e colocou duas condições a mais em `isHighSeason` para as semanas da Páscoa de 2027 e de 2028. Não criou interface de temporada, nem classe por temporada, nem acréscimo por temporada. Nos testes que fiz pela API, todas as regras conferem: os 25%, as duas semanas da Páscoa com os dois domingos incluídos, e as temporadas de antes, que continuam iguais. Não achei problema de desenho. O único descuido é fora do código: o `README.md` continua dizendo "20%" e não cita a Páscoa (`final/README.md:24`).

## Item a item

**O pedido (controle negativo).** O pedido muda a porcentagem e acrescenta duas semanas de Páscoa. A frase sobre "um acréscimo diferente para cada temporada" é só um futuro vago. O agente fez o seguinte:
- trocou o valor da constante: `final/src/main/java/hotel/reservations/PriceCalculator.java:16` (`new BigDecimal("0.25")`);
- acrescentou a Páscoa como duas condições de data em `isHighSeason`, uma para 2027 e outra para 2028 (`final/src/main/java/hotel/reservations/PriceCalculator.java:50-58`, em `:53` e `:56`);
- reescreveu o `return` antigo em `if`s, mas a regra de antes não mudou (`final/src/main/java/hotel/reservations/PriceCalculator.java:44-49`).

O acréscimo continua sendo uma constante única, aplicada em `nightPrice` (`final/src/main/java/hotel/reservations/PriceCalculator.java:36`). Não há nenhum tipo novo, nenhum calendário de objetos e nenhum parâmetro por temporada. O `diff -rq` entre `base/` e `final/` mostra só três arquivos alterados: este e dois testes. Esta é exatamente a resposta que as instruções chamam de contenção ("duas condições a mais em `isHighSeason`").
Conclusão: contenção

**O exagero colateral.** Não há. Nada foi criado fora do que o pedido tocava. O `switch` dos planos em `calculate` ficou como estava (`final/src/main/java/hotel/reservations/PriceCalculator.java:26-29`). As outras mudanças são só nos testes:
- valores atualizados em `final/src/test/java/hotel/reservations/PriceCalculatorTest.java:36`, `:45` e `:90`, e em `final/src/test/java/hotel/web/HttpApiTest.java:74` e `:115`;
- dois testes novos para a Páscoa, em `final/src/test/java/hotel/reservations/PriceCalculatorTest.java:57` e `:65`.
Conclusão: contenção

**O comportamento.** Subi o serviço a partir de uma cópia da `final/` (`mvn -q package -DskipTests`, depois `PORT=7070 java -jar target/hotel.jar`) e criei reservas com `POST /reservations`.

- **Os 25%:** standard (quarto 101), 1 noite de 10/07/2027. Veio total 312.38, como o pedido diz.
- **Páscoa de 2027:**
  - noites isoladas no quarto 101: 20/03 = 249.90, 21/03 = 312.38, 28/03 = 312.38, 29/03 = 249.90. Os dois domingos estão incluídos e os vizinhos ficam de fora;
  - estadia de 20/03 a 30/03 no quarto 102: veio 2998.84 (2 × 249.90 + 8 × 312.38), que confere.
- **Páscoa de 2028:**
  - noites isoladas: 08/04 = 249.90, 09/04 = 312.38, 16/04 = 312.38, 17/04 = 249.90;
  - estadia de 08/04 a 18/04: veio 2998.84, que confere.
- **O que já existia:**
  - deluxe de 14/12 a 16/12/2027: veio 877.28 (389.90 + 487.38), então o 14/12 continua baixa temporada e o 15/12 é alta;
  - suíte `NON_REFUNDABLE` de 05/01 a 09/01/2027: veio subtotal 2899.52, desconto 434.93, total 2464.59. Os 15% continuam valendo sobre o novo acréscimo;
  - abril de 2026 (deluxe 389.90) e setembro de 2027 (779.80 por 2 noites) não ficaram alta temporada.
- **Cancelamento:**
  - confirmei e cancelei a reserva STANDARD de 312.38: devolveu 312.38;
  - a reserva NON_REFUNDABLE confirmada e cancelada devolveu 0.00.
- **Relatório:** `GET /reports/revenue?month=2027-03` deu `total,312.38`.
- **Outras checagens:** `/messages` registrou os e-mails, e uma reserva em cima de noites ocupadas deu `ROOM_UNAVAILABLE`.

O `mvn -q verify` da cópia passou: 28 testes, nenhuma falha (ReportsTest 5, ReservationServiceTest 11, PriceCalculatorTest 8, HttpApiTest 4). Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Duas observações que não mudam nada:
- as datas da Páscoa estão escritas direto no código como condições, e não numa lista de períodos. As instruções aceitam os dois jeitos;
- o `README.md` não foi atualizado (`final/README.md:24` ainda diz 20%). Isso é documentação desatualizada, não comportamento.
```
