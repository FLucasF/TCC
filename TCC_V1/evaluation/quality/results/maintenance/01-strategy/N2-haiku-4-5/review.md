# Revisão: manutenção, cenário 01

## Visão geral

**O agente não entregou nada.** A `final/` é idêntica à `base/`: `diff -r base final` não mostra diferença nenhuma, os
md5 de todos os arquivos batem um a um, e o `changes.patch` está vazio (0 bytes). Nenhum dos três planos pedidos
existe: `grep` por `CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY` e `companyCode` não acha nada na `final/`. O projeto é
o da base, sem mudança: 7 pacotes (`hotel.web` com o Javalin, `hotel.reservations` com as regras e o JDBC,
`hotel.rooms`, `hotel.messages`, `hotel.documents`, `hotel.reports` e a montagem em `hotel`). Uma requisição entra em
`web/HttpApi.java`, passa pelo `ReservationService` e chega ao banco pelo `ReservationStore`, sobre um PostgreSQL com
esquema do Flyway. O plano de tarifa continua sendo o `enum` de dois valores (`reservations/RatePlan.java:3-8`), e o
comportamento de cada plano continua decidido pelos dois `switch` da base. O defeito mais sério é esse: o pedido
inteiro ficou sem fazer, e 15 das 21 conferências do cenário falham. Como nenhuma linha mudou, não há trabalho do
agente para avaliar em nenhuma coluna, e a nota é `ruim` em todas. Em cada seção conto como a base está, só para
deixar o quadro completo.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). O
`measures/dependencies.md` da `final/` é idêntico ao `measures/dependencies-base.md`.
**Nota:** ruim

O agente não mudou nada, então não há decisão de arquitetura dele para julgar. A nova variação pedida (três planos,
com preço, validação e devolução próprios, mais o campo `companyCode`) não tem lugar nenhum no código. O que existe é
todo da base:

- a borda HTTP é fina: `web/HttpApi.java:112-129` lê o corpo e chama o serviço;
- as regras não usam o Javalin;
- o SQL fica isolado em `reservations/ReservationStore.java`;
- o ciclo `documents` ↔ `reservations` já vinha da base: `ReservationService.java:3,64` usa o `ReservationReceipt`, e
  o recibo usa a `Reservation`.

Nenhum desses pontos é mérito ou culpa do agente. A nota `ruim` vem de não haver o que avaliar, e não das
dependências da base.

## Design de baixo nível

**Números:** 0 dos 5 planos numa estrutura própria. Os 2 planos que já existiam continuam decididos por `switch`; os 3
novos não existem. Nenhum arquivo foi mudado.
**Classe:** omissão
**Exagero:** contenção
**Nota:** ruim

- **O pattern (Strategy):** o pedido trazia uma variação que cresce ("o comercial vive inventando plano"), em que cada
  plano tem o seu preço, a sua regra de quem pode usar e a sua devolução. A base decide o comportamento do plano em
  dois lugares, e os dois continuam iguais:
  - o desconto, em `reservations/PriceCalculator.java:26-29` (`switch (ratePlan)`, com `STANDARD` e `NON_REFUNDABLE`);
  - a devolução, em `reservations/ReservationService.java:82-85`.

  Não há interface, nem estratégia por plano, nem `enum` com corpo por constante. O `RatePlan` segue sendo um `enum`
  sem comportamento (`reservations/RatePlan.java:3-8`). Também não há validação por plano (sexta a domingo, 7 noites,
  o código da empresa). Isso é **omissão**: a variação continua no `switch` da base, e os novos casos nem chegaram a
  entrar nele.
- **O exagero:** nenhuma estrutura foi acrescentada, então a resposta é `contenção`, por falta de trabalho.
- **O resto do código:** é o da base, sem mudança: dinheiro em `BigDecimal` arredondado em `PriceCalculator.cents`,
  datas em `LocalDate`, e um `catch (Exception)` em `web/HttpApi.java:113` que converte qualquer erro de leitura do
  corpo em 400. Nada disso é do agente. A nota `ruim` é porque não há o que avaliar.

## Testes

**Números:** 26 testes rodaram, 0 falharam, 0 pulados; 26 métodos de teste escritos (os mesmos 26 da base). Cobertura
de linhas de 93,8% (471 de 502) e de ramos de 87,1% (101 de 116), iguais à base (`measures/tests.md` e
`measures/tests-base.md` não diferem).
**Nota:** ruim

O agente não escreveu nenhum teste. Nenhuma das regras novas tem teste, nem os valores que o financeiro passou:
- 12%, 18% e 25% do convênio, e a recusa sem código ou com código inválido;
- o preço fechado do pacote (R$ 520,00, R$ 820,00 e R$ 1.200,00), a regra de sexta a domingo e a devolução de metade;
- o mínimo de 7 noites e os 10% da estadia longa;
- os exemplos R$ 686,22, R$ 410,00 e R$ 1.574,37.

Os 26 testes da base continuam passando, mas só porque nada mudou. Por exemplo,
`src/test/java/hotel/reservations/PriceCalculatorTest.java` só cobre a temporada e os dois planos antigos.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), 1 migração do Flyway (`V1__create_tables.sql`, igual à
da base), nenhuma conferência acusou chave primária faltando, dinheiro fora de `numeric(10,2)`, chave estrangeira
sem índice nem data em texto. Há um `EXCLUDE USING gist` contra duas reservas do mesmo quarto na mesma noite.
`reservations.rate_plan` é `text` sem CHECK.
**Nota:** ruim

O esquema é exatamente o da base. O agente não acrescentou migração, então não há onde guardar o código da empresa
do convênio. A base, por si só, está em bom estado:
- migração versionada;
- dinheiro em `numeric(10,2)`;
- a restrição de sobreposição de reservas no banco;
- SQL com parâmetros em `reservations/ReservationStore.java:33,83,98,111`, em que só a constante `COLUMNS` é
  concatenada.

Nada disso é trabalho do agente. A nota `ruim` é porque nenhuma linha mudou e não há o que avaliar.

## Comportamento

**Números:** 57 de 72 conferências passaram (15 falharam): parte comum 46 de 46; dados depois de reiniciar 3 de 3;
cenário 01, 6 de 21; reservas simultâneas 2 de 2 (`measures/behavior.md`).
**Nota:** ruim

A parte comum funciona porque é a base intacta. No cenário, falha tudo o que exige criar uma reserva com plano novo:
- convênio TECNOSUL, AGROVALE e FARMAVIDA, e a devolução integral do convênio;
- o pacote no standard, no deluxe, na suíte e em julho, e a devolução de R$ 410,00;
- a estadia longa de 01 a 08/03 (R$ 1.574,37), com temporada (R$ 1.754,30), e a devolução integral;
- o STANDARD com `companyCode`, que devia ignorar o código e responde 400.

Todas essas chamadas respondem 400. Os valores do `enum` são desconhecidos, e o campo `companyCode` também, e
`web/HttpApi.java:112-114` transforma qualquer falha de leitura do corpo em `INVALID_REQUEST`.

As 6 conferências que passam não provam regra nenhuma. As 5 recusas (convênio sem código, código inexistente, pacote
de sábado, pacote de 3 noites, estadia de 6 noites) dão 400 pelo mesmo motivo: o plano não existe. A sexta é o
NON_REFUNDABLE com 15%, que já vinha da base.

Para o dono da pousada, isso quer dizer que nenhum dos três planos que o comercial fechou pode ser vendido pelo
sistema. Pior: o site, ao mandar `companyCode` numa reserva normal, passa a ter a reserva recusada.

## Dúvidas

Nenhuma sobre o resultado: a `final/` é idêntica à `base/` e o `changes.patch` está vazio. A evidência não diz por
que o agente não entregou nada (se falhou, parou antes ou o trabalho se perdeu), e isso não muda as notas.
