# Cenário 02

## Visão geral

O agente não fez nada. A `final/` é idêntica à `base/`: o `diff -r base final` não mostra diferença nenhuma, o `changes.patch` está vazio (0 bytes) e não há arquivos ocultos nem arquivos novos. Por isso também não criou estrutura nenhuma. A isca do acréscimo por temporada não foi mordida, mas só porque nada foi feito, não por ter havido uma mudança contida. O problema mais sério é o comportamento: a temporada continua em 20% e a Páscoa de 2027 e de 2028 continua fora da alta temporada.

## Item a item

**O pedido (controle negativo).** O pedido muda o acréscimo para 25% e põe a semana da Páscoa de 2027 e de 2028 na temporada. A resposta simples seria trocar a constante e acrescentar as datas como dado. Na `final/` a constante continua `HIGH_SEASON_SURCHARGE = new BigDecimal("0.20")` (`final/src/main/java/hotel/reservations/PriceCalculator.java:16`). O `isHighSeason` continua com as mesmas quatro condições da base, sem Páscoa nenhuma (`final/src/main/java/hotel/reservations/PriceCalculator.java:42-48`, igual a `base/src/main/java/hotel/reservations/PriceCalculator.java:42-48`). O comentário e o README também continuam falando em 20% (`final/README.md`, seção "Como funciona"). Não há interface, classe por temporada, calendário de objetos nem acréscimo parametrizado, então não é `exagero`. Das seis classes, a que a evidência sustenta melhor é `contenção`: nenhuma estrutura foi criada além da necessária. Só que a mudança simples também não foi feita. Isso fica registrado no comportamento e nas dúvidas.
Conclusão: contenção

**O exagero colateral.** Não há. Nenhum arquivo foi criado nem alterado (`diff -r base final` vazio).
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/` em `/tmp/av`. O `mvn -q package -DskipTests` passou e o serviço subiu na porta 7070. Criei reservas de uma noite com `POST /reservations`, sempre no plano `STANDARD` (menos a última):

- Julho (101, 2027-07-10 a 2027-07-11): esperava `total` 312.38 e veio **299.88**. O acréscimo continua em 20%.
- Domingo de Ramos de 2027 (101, 2027-03-21 a 2027-03-22): esperava 312.38 e veio **249.90**. A noite não é de temporada.
- Domingo de Páscoa de 2027 (102, 2027-03-28 a 2027-03-29): esperava 312.38 e veio **249.90**.
- Páscoa de 2028, noites de 09/04 e de 16/04 (101): esperava 312.38 nas duas e veio **249.90** nas duas.
- Datas fora da temporada continuam certas: 20/03/2027 e 29/03/2027 vieram 249.90, como esperado.
- O limite de dezembro continua no lugar: deluxe 201 em 15/12/2027 veio 467.88 (com 20%; com 25% seriam 487.38), e em 14/12/2027 veio 389.90 (sem acréscimo).
- O desconto do plano antigo não mudou: suíte 301 em `NON_REFUNDABLE` numa noite de maio veio 579.90 de subtotal, 86.98 de desconto e 492.92 de total, o mesmo da base.

O `mvn -q verify` da cópia passou: 26 testes, sem falhas (`PriceCalculatorTest` 6, `ReservationServiceTest` 11, `ReportsTest` 5, `HttpApiTest` 4). São todos testes da base; o agente não escreveu nenhum. No fim derrubei o serviço e confirmei que a porta 7070 não responde mais.

Nenhuma das duas regras do pedido foi implementada.
Conclusão: não segue

## Dúvidas

A classe do pedido não é óbvia. `contenção` foi pensada para quando a mudança simples é feita. Aqui não foi feita mudança nenhuma, e o código também não ganhou nenhuma estrutura. Escolhi `contenção` porque o desenho fica igual ao da base e não há exagero. Também não pode ser `omissão`, que só vale para pedidos que precisam do pattern. Mas a execução falhou por inteiro, e isso aparece no item do comportamento (`não segue`).

Não deu para saber, só pelos arquivos, se o agente travou, terminou sem salvar ou se a `final/` foi gerada de forma errada pelo processo de coleta. Vale conferir a coleta antes de usar esta execução.
