# Revisão: manutenção, cenário 01

## Visão geral

A base já era um projeto em camadas. `hotel.web.HttpApi` recebe a requisição e monta um `NewReservation`, que vai para `hotel.reservations.ReservationService`. O serviço usa o `PriceCalculator`, e a `ReservationStore` grava tudo em PostgreSQL, com o esquema criado por migrações do Flyway. O agente pôs o comportamento dos cinco planos no próprio `enum RatePlan`, uma constante com corpo por plano, com três métodos sobrescritíveis: `check`, `price` e `refund`. O valor de entrada é um `record Stay`. Os dois `switch` de comportamento da base sumiram (`PriceCalculator.calculate` e `ReservationService.cancel`). Ele também criou o `enum Company` com os descontos do convênio, uma migração nova (`V2__add_company_code.sql`) e o campo `companyCode` no corpo do POST. As 72 conferências do roteiro passam, e os 36 testes também. Não achei nada sério. O que vi é pequeno: o `companyCode` é gravado em qualquer plano, mesmo quando deveria ser ignorado, e ficaram duas sobrecargas que só servem para os testes antigos.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). A base tinha os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reservations` passou de 13 para 18 classes e de 508 para 657 linhas.
**Nota:** bom

- **Borda HTTP:** continua fina. A única mudança em `HttpApi` é repassar o `companyCode` (`src/main/java/hotel/web/HttpApi.java:128-130`). O campo novo do JSON fica no DTO da borda (`src/main/java/hotel/web/CreateReservationRequest.java:16`).
- **Regras:** ficaram em `hotel.reservations`, sem Javalin nem JDBC. `RatePlan`, `Company` e `Stay` não importam nada de infraestrutura.
- **SQL:** continua só na `ReservationStore`. As mudanças lá são a coluna nova na inserção e na leitura (`src/main/java/hotel/reservations/ReservationStore.java:44`, `:133`).
- **Montagem:** não mudou.
- **O ciclo:** `hotel.documents` ↔ `hotel.reservations` veio da base, e o agente não o piorou.
- **Tamanho:** nenhuma classe cresceu demais. `RatePlan` tem 101 linhas, e `ReservationService` diminuiu de 138 para 134.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 da base e os 3 novos). Nenhum `switch` ou `if` sobre o plano ficou fora do `enum`. Preço, validação e devolução de cada plano ficam na sua constante.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern.** É o `enum` como estratégia, que a referência aceita como acerto. A constante com corpo sobrescreve três métodos: `check` (`src/main/java/hotel/reservations/RatePlan.java:84`), `price` (`:88`) e `refund` (`:93`).

- `STANDARD` usa o comportamento padrão: preço cheio, sem restrição, devolve tudo.
- `NON_REFUNDABLE` sobrescreve `price` e `refund` (`RatePlan.java:17-27`).
- `CORPORATE` sobrescreve `check` e `price` (`:29-42`).
- `WEEKEND_PACKAGE` sobrescreve `check`, `price` e `refund` (`:44-67`).
- `LONG_STAY` sobrescreve `check` e `price` (`:69-81`).

Quem usa a estrutura não nomeia nenhum plano:

- O cálculo chama `ratePlan.check(stay)` e `ratePlan.price(stay)` (`src/main/java/hotel/reservations/PriceCalculator.java:30-32`).
- O cancelamento chama `reservation.ratePlan().refund(...)` (`src/main/java/hotel/reservations/ReservationService.java:82`).

As regras de quem pode usar cada plano (sexta a domingo, 7 noites, código da empresa) também ficaram dentro do plano, não em `if` no serviço. O único `switch` novo escolhe o preço do pacote pelo tipo de quarto (`RatePlan.java:55-59`), não pelo plano, e mora dentro da constante dele.

**Exagero.** Não há. `Stay` (`src/main/java/hotel/reservations/Stay.java:11`) é o objeto de parâmetros que as estratégias precisam. `Company` (`src/main/java/hotel/reservations/Company.java:7`) é um `enum` de dados. Não há fábrica, interface solta nem camada que só repassa.

**O resto do código.** O dinheiro continua em `BigDecimal`, com o `cents` de sempre (`RatePlan.java:97-99`), e as datas em `LocalDate`. Os defeitos que achei são pequenos e localizados:

- **Sobrecargas só para os testes antigos.** `PriceCalculator.calculate` de 4 argumentos (`PriceCalculator.java:18-20`) e o construtor de 6 argumentos de `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:15-18`) ficaram só para não mexer nesses testes. Nenhum código de produção usa a primeira.
- **Código da empresa resolvido duas vezes.** O `CORPORATE` procura o código em `check` e de novo em `price` (`RatePlan.java:32`, `:40`), e o código anda como `String` solta no `Stay`.
- **Validação escondida no cálculo.** `calculate` também recusa a reserva (`PriceCalculator.java:31`). O método que se chama "calcular" lança erro de regra.
- **Código ignorado, mas gravado.** O `companyCode` é gravado em qualquer plano (`ReservationStore.java:44`). Em um `STANDARD` com `"companyCode": "XYZ"`, o "XYZ" vai para o banco. O texto mandava ignorar o campo nos outros planos, e esta é uma regra de plano que ficou fora do plano, pequena (ver Dúvidas).

Nada disso é espalhado nem torna o código difícil de mudar.

## Testes

**Números:** 36 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 36 métodos escritos (a base tinha 26, todos mantidos). Cobertura de linhas: 94,3% (517 de 548; a base tinha 93,8%). Cobertura de ramos: 88,2% (112 de 127; a base tinha 87,1%). Em `hotel.reservations`: 254 de 267 linhas e 60 de 63 ramos (`measures/tests.md`).
**Nota:** bom

O agente escreveu 10 testes e não apagou nem enfraqueceu nenhum da base: o patch só acrescenta. Os testes conferem a parte nova com valores:

- **Os exemplos do financeiro:**
  - Tecnosul: 779,80, 93,58 e 686,22 (`src/test/java/hotel/reservations/PriceCalculatorTest.java:79-86`).
  - Pacote deluxe: 820,00, e devolução de 410,00 (`PriceCalculatorTest.java:99-113`, `src/test/java/hotel/reservations/ReservationServiceTest.java:163-176`).
  - Estadia longa: 1.749,30, 174,93 e 1.574,37 (`PriceCalculatorTest.java:126-136`).
- **Estadia longa com temporada:** 1.899,24 (`PriceCalculatorTest.java:133-135`).
- **Recusas:**
  - Convênio sem código, com código vazio, em minúsculas e desconhecido (`PriceCalculatorTest.java:89-96`).
  - Pacote de quinta a sábado, de sexta a segunda e de sexta a sábado (`PriceCalculatorTest.java:116-123`).
  - Estadia longa com 6 noites (`PriceCalculatorTest.java:139-142`).
  - O código `INVALID_REQUEST` no serviço (`ReservationServiceTest.java:188-195`) e pela API (`src/test/java/hotel/web/HttpApiTest.java:124-139`).
- **Devolução dos três planos novos:** `ReservationServiceTest.java:163-176`.

A pergunta da mutação:

- Trocar 0.12 por 0.21 quebra `corporateRateTakesTheCompanyDiscount`.
- Trocar `< 7` por `<= 7` quebra o teste de 7 noites, que precisa ser aceito.
- Trocar 0.50 da devolução do pacote quebra o teste de 410,00.
- Agrovale é conferido pelo total, 639,44 (`ReservationServiceTest.java:184`).
- Farmavida só é conferido pela constante, `assertEquals(0.25, Company.FARMAVIDA.discount())` (`PriceCalculatorTest.java:94`), e não por uma reserva. É uma rede fraca, mas uma troca do número ainda seria pega.

Há só detalhes de forma: a indentação torta em `ReservationServiceTest.java:184` e a falta de linha em branco antes dos `@Test` novos (`ReservationServiceTest.java:162`).

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, e não há chave estrangeira sem índice nem data em texto. O `EXCLUDE` contra duas reservas na mesma noite continua lá. Migrações: a `V1__create_tables.sql` está igual à da base, e a `V2__add_company_code.sql` é nova (`measures/database.md`). Os avisos são `rate_plan`, `status`, `type` e `channel` em `text` sem `CHECK`, e a listagem de todas as reservas (`ReservationStore.java:112`). Tudo isso veio da base.
**Nota:** bom

O agente fez a evolução do jeito certo. Não editou a migração da base e criou uma nova com uma coluna que aceita nulo (`src/main/resources/db/migration/V2__add_company_code.sql:2`). Por isso um banco já em uso sobe sem problema, e as reservas antigas ficam com `company_code` nulo. O SQL novo usa parâmetros (`ReservationStore.java:31-44`). Os fragmentos concatenados que o script aponta são só a constante `COLUMNS` e literais.

Ressalva pequena: o `companyCode` é gravado mesmo nos planos que deveriam ignorá-lo (`ReservationStore.java:44`), e a coluna não tem `CHECK` ligando o código ao plano. Isso não corrompe nada que o sistema use, porque o código só é lido para montar o `NewReservation` (`ReservationStore.java:133`), mas deixa no banco um dado sem sentido.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou (`measures/behavior.md`):

- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- as duas reservas ao mesmo tempo: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API:

- **Os valores do financeiro:** R$ 686,22 no convênio da Tecnosul, R$ 820,00 no pacote, com devolução de R$ 410,00, e R$ 1.574,37 na estadia longa.
- **Os outros descontos do convênio:** Agrovale dá R$ 639,44 e Farmavida dá R$ 584,85.
- **O preço fechado do pacote:** R$ 520,00, R$ 820,00 e R$ 1.200,00, o mesmo em julho, na alta temporada.
- **A temporada na estadia longa:** continua valendo nas noites de temporada.
- **As recusas:** convênio sem código ou com código errado, pacote fora de sexta a domingo e estadia longa com 6 noites recebem 400 com o código `INVALID_REQUEST`.
- **O `companyCode` nos outros planos:** é ignorado no preço.
- **O que já existia:** o normal e o não reembolsável continuam iguais, e os dados sobrevivem a um reinício. A dupla reserva simultânea continua sendo recusada com 409.

Uma mudança de comportamento fora do roteiro: agora o preço, com a validação do plano, é calculado antes de conferir se o quarto está livre (`ReservationService.java:41-46`). Um pedido com plano inválido para um quarto ocupado recebe 400, e não mais 409. O texto não fecha essa ordem, e a escolha é razoável.

## Dúvidas

- O agente grava o `companyCode` em qualquer plano (`ReservationStore.java:44`), e o pedido dizia que nos outros planos ele "pode ignorar". O preço o ignora, e o dado gravado não tem efeito. É a "regra de plano isolada fora do plano" de que a referência fala, e não muda a classe. Pesei como ressalva pequena no banco e no design, sem baixar a nota; um revisor mais rigoroso poderia pedir que só o convênio guardasse o código.
- O convênio só aceita o código exatamente em maiúsculas: `tecnosul` é recusado (`Company.java:25`, conferido em `PriceCalculatorTest.java:90`). O texto diz que o código é o nome em maiúsculas, então é uma leitura possível, mas o site pode mandar em outra caixa.
