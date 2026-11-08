# Revisão: manutenção, cenário 01

## Visão geral

A base já era um sistema em pacotes por assunto: `hotel.web` (Javalin, `HttpApi`), `hotel.reservations` (regras em
`ReservationService`, preço em `PriceCalculator`, JDBC em `ReservationStore`), `hotel.rooms`, `hotel.messages`,
`hotel.documents`, `hotel.reports`, com a montagem em `hotel`. Uma criação de reserva entra por `HttpApi`, que transforma
o JSON em `NewReservation` (`src/main/java/hotel/web/HttpApi.java:128-130`). Depois passa por
`ReservationService.create`, que valida, pede o preço e grava no PostgreSQL por `ReservationStore`, com o esquema criado
pelo Flyway. A mudança do agente é pequena e está no lugar certo. O `enum RatePlan` virou a estratégia: cada uma das
cinco constantes tem o seu `validate`, `price` e `refund` (`src/main/java/hotel/reservations/RatePlan.java:18-110`).
Os dois `switch` da base saíram de `PriceCalculator` e de `ReservationService.cancel`. O `companyCode` vai do JSON até
uma coluna nova, criada por uma migração nova (`V2__company_code.sql`). O roteiro de comportamento passou inteiro
(72/72) e os 32 testes passam. Não há defeito sério. O ponto mais fraco são os testes: duas das três taxas de convênio
não são conferidas, e nada testa o `companyCode` pela API.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual na base
(`measures/dependencies-base.md`). O agente não criou nenhuma dependência nova entre pacotes. `hotel.reservations` passou
de 508 para 663 linhas, e `RatePlan` (135 linhas) é agora a 4ª maior classe.
**Nota:** bom

- A borda HTTP continua fina. A única mudança em `hotel.web` é passar o campo novo:
  `src/main/java/hotel/web/HttpApi.java:128-130`, `CreateReservationRequest.java:16` e `ReservationResponse.java:19,39`.
  Nenhuma regra de plano foi parar no handler.
- As regras novas ficam em `hotel.reservations`, sem Javalin e sem JDBC. `RatePlan.java` importa só `hotel.rooms`, que
  `reservations` já usava, e `java.time`/`java.math`. A recusa sai como `HotelException(INVALID_REQUEST)`
  (`RatePlan.java:132-134`), e o mapeamento para 400, que já existia, cuida dela.
- O SQL continua isolado em `ReservationStore` (`ReservationStore.java:31-33,44,132`).
- O único ciclo (`documents` ↔ `reservations`, de `ReservationService.java:3`) já existia na base e não é do agente.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 da base e os 3 novos), cada um com preço, validação e devolução próprios.
Não sobrou `switch` nem `if` de comportamento sobre o plano no cálculo ou no cancelamento. Sobra 1 regra de plano fora
da estrutura, pequena: zerar o `companyCode` quando o plano não é convênio (`NewReservation.java:15-19`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** O agente usou o `enum` como estratégia, que a referência aceita: cada constante tem o próprio corpo, e
`price` e `refund` são abstratos (`RatePlan.java:117-121`). `validate` tem um padrão vazio (`RatePlan.java:113-114`),
sobrescrito pelos planos que têm restrição (`:51-55`, `:75-79`, `:95-99`). Os dois planos que já existiam também foram
para a estrutura (`STANDARD` em `:20-30`, `NON_REFUNDABLE` em `:32-42`). Os três pontos que usam o plano pedem a ele,
sem nomear nenhum:
- a validação: `ReservationService.java:42`;
- o preço: `PriceCalculator.java:22-24`;
- a devolução: `ReservationService.java:84`, onde antes havia um `switch`.

Os dados de cada plano ficam dentro da constante dele: as empresas em `RatePlan.java:45-48`, os preços do pacote em
`:69-72`. Um plano novo (melhor idade, 60 dias de antecedência) é uma constante nova num arquivo só. A cópia do cálculo
"noites menos um percentual" foi evitada com o auxiliar `discounted` (`RatePlan.java:124-130`).

**O exagero.** Nenhuma estrutura sem necessidade: não há interface, fábrica nem builder. O agente manteve o
`calculate(...)` de 4 argumentos (`PriceCalculator.java:18-20`) e o construtor de 6 argumentos de `NewReservation`
(`NewReservation.java:21-24`) para compatibilidade, o que custa pouco.

**O resto do código (observações que não derrubam a nota):**
- A assinatura das estratégias carrega um dado de um só plano. `validate` e `price` recebem `companyCode` em todos os
  planos (`RatePlan.java:113,117-118`), mas só o convênio o usa. O próximo plano que o texto antecipa (60 dias de
  antecedência) precisa da data da reserva e obrigaria a mudar a assinatura das cinco constantes. Passar a própria
  `NewReservation` teria resolvido isso.
- `price` depende de `validate` ter rodado antes. `CORPORATE.price` com código nulo chamaria `Map.of(...).get(null)` e
  lançaria `NullPointerException` (`RatePlan.java:59`). `WEEKEND_PACKAGE` grava 2 noites fixas (`:84`), sejam quais
  forem as datas. Isso não aparece hoje porque o serviço valida antes (`ReservationService.java:42`), mas
  `PriceCalculator.calculate` é público.
- Há um vaivém entre as duas classes: `PriceCalculator.calculate` repassa para `ratePlan.price(this, …)`
  (`PriceCalculator.java:23`), que chama de volta `calculator.sumOfNights` (`RatePlan.java:127`).
- Os tipos estão certos: `BigDecimal` com `cents` (meio para o par) no desconto e na metade da devolução
  (`RatePlan.java:89,128`), `LocalDate`/`DayOfWeek` nas datas, `enum` para planos e tipos de quarto.

## Testes

**Números:** 32 testes rodaram, 0 falhas, 0 erros, 0 pulados; 32 métodos escritos (a base tinha 26, todos mantidos sem
mudança; o agente acrescentou 6). Cobertura de linhas de 94,3% (514/545) e de ramos de 87,9% (109/124); na base, 93,8%
e 87,1%. Em `hotel.reservations`: 250/263 linhas e 57/60 ramos.
**Nota:** com problemas

O que está bem: os testes novos conferem valores, e os três exemplos do financeiro estão lá.
- `PriceCalculatorTest.java:80-88`: Tecnosul, 779,80 / 93,58 / 686,22.
- `:90-102`: o pacote deluxe a R$ 820,00 sem desconto e com 2 noites, e a suíte em julho a R$ 1.200,00 (o preço
  fechado vale na temporada).
- `:104-111`: a estadia longa, 1.749,30 / 174,93 / 1.574,37.
- `ReservationServiceTest.java:171-182`: o convênio sem código e com código desconhecido dá `INVALID_REQUEST`, o código
  fica gravado e a devolução é integral.
- `:184-194`: o pacote começando na quinta e o de sexta a segunda são recusados, o `companyCode` é descartado e a
  devolução é de R$ 410,00.
- `:196-204`: 6 noites é recusado e 7 é aceito, o que pega a troca de `<` por `<=` em `RatePlan.java:96`.
- Os testes rodam contra o PostgreSQL de verdade.

O que fica sem rede (a pergunta da mutação):
- As taxas da Agrovale (18%) e da Farmavida (25%) não têm teste. Trocar `0.18` por `0.81` em `RatePlan.java:47`, ou
  `0.25` por `0.52` em `:48`, deixaria todos os testes verdes.
- O preço do pacote no quarto standard (R$ 520,00, `RatePlan.java:70`) também não tem teste.
- `HttpApiTest` não foi mexido, e nada testa o campo `companyCode` pela API. Se alguém apagasse `body.companyCode()`
  em `HttpApi.java:130`, todo convênio vindo do site seria recusado, e a suíte continuaria passando. A resposta
  400/`INVALID_REQUEST` das regras de plano também não é testada pela API.
- A estadia longa com noites de temporada não é testada, embora use o mesmo `sumOfNights` já coberto pelos testes da
  base.

São lacunas pequenas e baratas de fechar (um teste parametrizado das três empresas e um teste de API do convênio), mas
deixam regras pedidas sem conferência.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` (6 de 6 colunas), nenhuma chave
estrangeira sem índice, nenhuma data em texto. A `EXCLUDE USING gist` contra duas reservas na mesma noite continua lá.
Há 2 migrações: `V1__create_tables.sql` igual à base, `V2__company_code.sql` nova. As colunas `rate_plan`, `status`,
`type` e `channel` estão sem `CHECK`, como já estavam na base.
**Nota:** bom

- O agente evoluiu o esquema do jeito certo: acrescentou uma migração (`src/main/resources/db/migration/V2__company_code.sql:2`,
  `ALTER TABLE reservations ADD COLUMN company_code text`) e não editou a V1. Um banco que já roda o sistema sobe sem
  problema.
- A coluna nova é anulável, o que está certo, porque só o convênio a usa. O valor entra por parâmetro
  (`ReservationStore.java:44`) e é lido em `:132`. Os SQL montados com `+` só concatenam a constante `COLUMNS`
  (`ReservationStore.java:99,112`).
- O preço é gravado inteiro (subtotal, desconto, total), então mudar a taxa de uma empresa no futuro não altera as
  reservas antigas.
- Observação menor: o `rate_plan` sem `CHECK` veio da base. O banco também não liga `company_code` ao plano
  `CORPORATE`, regra que o código garante (`NewReservation.java:16-18`).

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 01: 21/21;
- reservas simultâneas: 2/2.
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- o convênio dá 12%, 18% e 25% (686,22, 639,44 e 584,85) e recusa com 400/`INVALID_REQUEST` a reserva sem código ou
  com código desconhecido;
- o pacote custa R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho, recusa sábado a segunda e sexta a segunda, e
  devolve R$ 410,00 no cancelamento;
- a estadia longa fecha 1.574,37, vale a temporada (1.754,30), recusa 6 noites e devolve tudo;
- o `companyCode` em outro plano é ignorado;
- o `STANDARD` e o `NON_REFUNDABLE` continuam como antes.

Nada da parte comum quebrou, os dados sobrevivem ao reinício e duas reservas simultâneas do mesmo quarto nunca passam
juntas.

## Dúvidas

- Uma regra de plano ficou fora do plano: `NewReservation.java:16-18` descarta o `companyCode` quando o plano não é
  `CORPORATE`. A referência do pattern diz que isso, sozinho, não muda a classe, e mantive `acerto`.
- O código da empresa é comparado exatamente, então `tecnosul` em minúsculas é recusado (`RatePlan.java:52`). O texto
  diz que o código "é o nome dela em maiúsculas", e li isso como escolha razoável, não como defeito.
- Fiquei em dúvida entre `bom` e `com problemas` nos testes. Os testes novos são bons e conferem os três exemplos, mas
  a mutação das taxas da Agrovale e da Farmavida e a falta de teste do campo novo pela API pesaram para
  `com problemas`.
