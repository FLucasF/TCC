# Revisão: manutenção, cenário 01

## Visão geral

O agente partiu de uma base que já tinha os pacotes `hotel.web` (rotas Javalin e DTOs), `hotel.reservations` (serviço, cálculo de preço, `ReservationStore` com JDBC), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`, com a montagem em `hotel`. Uma requisição `POST /reservations` passa por `HttpApi` (lê o corpo e monta um `NewReservation`, `src/main/java/hotel/web/HttpApi.java:128-130`), vai para `ReservationService.create`, que pede o preço a `PriceCalculator.calculate`, e é gravada por `ReservationStore` no PostgreSQL, com o esquema criado pelo Flyway. A mudança é pequena e bem localizada. O `enum RatePlan` passou a ser a estratégia: cada constante tem o seu próprio corpo, com validação, preço e devolução (`src/main/java/hotel/reservations/RatePlan.java:15-113`). Os dois `switch` da base saíram (`PriceCalculator.java:23-27`, `ReservationService.java:83`). O `companyCode` entrou por uma migração nova (`V2__add_company_code.sql:2`). As 72 conferências do roteiro passam, e os 32 testes também. O defeito mais sério é pequeno: os testes não conferem as taxas da Agrovale e da Farmavida, nem o preço do pacote no quarto standard.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, os mesmos da base. Há 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), que já existia na base (`measures/dependencies-base.md`). O JDBC só aparece em `hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`, e o Javalin só em `hotel` e `hotel.web`.
**Nota:** bom

A mudança não mexeu na forma do sistema:
- **A borda HTTP:** continua fina. Só repassa o campo novo (`src/main/java/hotel/web/HttpApi.java:128-130`; `src/main/java/hotel/web/CreateReservationRequest.java:16`).
- **As regras novas:** ficam em `hotel.reservations`, sem Javalin e sem SQL (`src/main/java/hotel/reservations/RatePlan.java`, `src/main/java/hotel/reservations/StayTerms.java:8`).
- **O SQL:** fica em `ReservationStore`, que só ganhou uma coluna (`src/main/java/hotel/reservations/ReservationStore.java:19,44,132`).

O ciclo `documents` ↔ `reservations` vem da base: `ReservationService.java:3` importa `ReservationReceipt`, e `ReservationReceipt.java:3` importa `Reservation`. O agente não o piorou. A maior classe continua sendo `HttpApi` (171 linhas); `RatePlan` (113) é a única que cresceu de verdade, e cresceu com regra que é dela.

## Design de baixo nível

**Números:** os 5 planos (os 2 da base e os 3 novos) estão na estrutura, e nenhum fica fora dela. Sobram 0 `switch` ou `if` de comportamento sobre o plano no cálculo e no cancelamento. Fora do plano, sobra 1 regra pequena: `NewReservation.java:16` descarta o código da empresa quando o plano não é `CORPORATE`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É o `enum` como estratégia, que a referência aceita como acerto:
- **A base:** `RatePlan` declara o comportamento padrão de cada plano: validar não faz nada (`RatePlan.java:90`), o preço são as noites menos o desconto (`RatePlan.java:94-98`), o desconto padrão é zero (`RatePlan.java:101`) e a devolução padrão é tudo (`RatePlan.java:106`).
- **Os planos:** cada constante sobrescreve o que é seu:
  - `STANDARD` usa os padrões (`RatePlan.java:15`);
  - `NON_REFUNDABLE` dá 15% e não devolve nada (`RatePlan.java:18-28`);
  - `CORPORATE` valida o código e dá o desconto da empresa (`RatePlan.java:31-47`);
  - `WEEKEND_PACKAGE` valida sexta com 2 noites, cobra o preço fechado por tipo de quarto e devolve metade (`RatePlan.java:50-72`);
  - `LONG_STAY` valida 7 noites e dá 10% (`RatePlan.java:75-87`).
- **Quem usa a estrutura:** pede ao plano, sem nomear os planos. O cálculo chama `ratePlan.validate(stay)` e depois `ratePlan.price(stay, this)` (`PriceCalculator.java:25-26`). O cancelamento chama `reservation.ratePlan().refund(...)` (`ReservationService.java:83`).
- **Os casos da base:** os dois `switch` da base (o desconto no `PriceCalculator` e a devolução no `cancel`) foram removidos, e os dois planos antigos foram para dentro da estrutura.
- **O próximo plano:** pode ser uma constante nova, sem mexer em outro lugar. A exceção é se ele precisar de um dado novo da requisição, como o `companyCode` precisou.

**O exagero.** O agente só criou o `StayTerms` (`StayTerms.java:8`), que junta os dados que o plano precisa saber. É um objeto de parâmetro pequeno e usado. Não há interface, fábrica nem camada que só repassa.

**O resto do código** está limpo, com pontos menores:
- as taxas e os preços ficam em `Map<…, String>` e são convertidos para `BigDecimal` a cada chamada (`RatePlan.java:33`, `RatePlan.java:52`, `RatePlan.java:110`), quando poderiam ser constantes `BigDecimal`;
- `RatePlan` e `PriceCalculator` se chamam nos dois sentidos (`PriceCalculator.java:26` e `RatePlan.java:95`), um acoplamento leve dentro do mesmo pacote;
- a sobrecarga `calculate(..., ratePlan)` passa `null` como código (`PriceCalculator.java:18-19`);
- o código da empresa é `String` solta em três records (`NewReservation.java:12`, `StayTerms.java:8`, `Reservation.java:15`), o que é aceitável neste tamanho;
- o dinheiro continua em `BigDecimal`, com o arredondamento da base (`percentOff`, `RatePlan.java:110-112`).

## Testes

**Números:** 32 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 32 métodos escritos. A base tinha 26, e os 26 continuam passando. Cobertura: linhas 514/545 (94,3%) e ramos 109/124 (87,9%); na base, 93,8% e 87,1%. Em `hotel.reservations`: 251/264 linhas e 57/60 ramos.
**Nota:** com problemas

Os testes novos conferem com valores os três exemplos do financeiro:
- **Tecnosul:** 779,80, 93,58 e 686,22 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:88-94`).
- **Pacote deluxe:** 820,00, desconto 0,00, e também a suíte em alta temporada, 1.200,00 (`PriceCalculatorTest.java:105-114`).
- **Estadia longa:** 1.749,30, 174,93 e 1.574,37, com a fronteira das 7 noites (`PriceCalculatorTest.java:127-135`).

As recusas também estão testadas:
- **Convênio:** código nulo, vazio, minúsculo e inexistente (`PriceCalculatorTest.java:97-102`).
- **Pacote:** quinta, sexta a segunda e sexta a sábado (`PriceCalculatorTest.java:117-124`).

As devoluções do pacote (410,00) e do convênio (686,22) estão testadas contra o banco, assim como o `companyCode` guardado (`src/test/java/hotel/reservations/ReservationServiceTest.java:164-176`). Nenhum teste da base foi apagado ou enfraquecido.

Ficam sem rede, pela pergunta da mutação:
- **As taxas da Agrovale e da Farmavida:** trocar `"0.18"` ou `"0.25"` em `RatePlan.java:33` não derruba nenhum teste.
- **O pacote no standard:** trocar `"520.00"` em `RatePlan.java:52` também não.
- **A temporada na estadia longa:** nenhum teste a confere.
- **A devolução integral da estadia longa:** também não é conferida.
- **O `companyCode` ignorado nos outros planos:** a regra de `NewReservation.java:16` não tem teste.
- **A API:** nenhum teste de `HttpApiTest` passa o campo `companyCode` nem confere o 400 pela API.

## Banco

**Números:** 3 tabelas e 2 migrações do Flyway. A `V1__create_tables.sql` está igual à da base, e a `V2__add_company_code.sql` é nova. As conferências não acusaram nada: nenhuma tabela sem chave primária, todo dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto. As colunas de situação e de tipo estão sem CHECK, como na base. Sobre as reservas, há EXCLUDE por quarto e noites.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a que já estava aplicada (`src/main/resources/db/migration/V2__add_company_code.sql:2`). Um banco que já roda a base sobe sem problema. A coluna `company_code` aceita nulo, o que é coerente com os outros planos. O INSERT e a leitura continuam parametrizados (`src/main/java/hotel/reservations/ReservationStore.java:31-50`, `ReservationStore.java:132`). Não há SQL montado com dado de fora: os `+` só juntam constantes. A falta de CHECK em `rate_plan` (`V1__create_tables.sql:28`) vem da base. Não há uma restrição que amarre `company_code` ao plano `CORPORATE`, mas isso é miúdo.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46/46;
- os dados depois de reiniciar: 3/3;
- o cenário 01: 21/21;
- as reservas simultâneas: 2/2.

**Nota:** bom

O dono da pousada tem o que pediu, pela API:
- **Convênio:** as três empresas com os descontos certos (Tecnosul 686,22, Agrovale 639,44, Farmavida 584,85). Sem código, ou com um código desconhecido, a resposta é 400 `INVALID_REQUEST`, e o cancelamento devolve tudo.
- **Pacote:** 520,00, 820,00 e 1.200,00 também em julho, recusado fora de sexta a domingo, com devolução de metade (410,00).
- **Estadia longa:** 1.574,37, com a temporada valendo (1.754,30), recusada com 6 noites e devolvendo tudo.
- **O resto:** o `companyCode` é ignorado no STANDARD, e o NON_REFUNDABLE continua com 15%. Toda a parte comum segue como antes.

Uma mudança fora do roteiro: a validação do plano passou para antes da conferência de capacidade e de disponibilidade (`src/main/java/hotel/reservations/ReservationService.java:37-47`; na base, o preço vinha depois). Um pedido que viola ao mesmo tempo a regra do plano e a capacidade recebe agora 400 em vez de 422. O texto não decide essa ordem, e os planos da base não são afetados, porque a validação deles é vazia.

## Dúvidas

- **Uma regra de plano fora do plano:** a regra "o código da empresa só vale para o convênio" ficou em `NewReservation.java:15-19` (`ratePlan != RatePlan.CORPORATE`). A referência diz que isso, sozinho, não muda a classe. Mas é um `if` sobre um plano específico, e um plano futuro que também use um código teria de mexer ali.
- **O código minúsculo:** o código da empresa é comparado exatamente; "tecnosul" é recusado (`RatePlan.java:37`, testado em `PriceCalculatorTest.java:98`). O texto diz que o código é o nome em maiúsculas, e a escolha é defensável, mas o site pode mandar em minúsculas.
- **A nota dos testes:** fica entre `com problemas` e `bom`. Os exemplos do texto estão todos conferidos; o que falta são duas das três taxas do convênio e um dos três preços do pacote.
