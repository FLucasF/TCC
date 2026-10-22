# Revisão: manutenção, cenário 01

## Visão geral

O sistema tem 7 pacotes: `hotel` (montagem em `Hotel.java`, pool e Flyway em `Database.java`), `hotel.web` (Javalin), `hotel.reservations` (regras e acesso a dados), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição entra em `HttpApi`, que lê o corpo (`HttpApi.java:109-135`) e chama `ReservationService`. O serviço pede o preço ao `PriceCalculator` e grava com `ReservationStore` (JDBC com parâmetros) no PostgreSQL, cujo esquema vem de migrações Flyway. O agente trocou os dois `switch` de plano da base (o desconto em `PriceCalculator.calculate` e a devolução em `ReservationService.cancel`) por uma interface `RatePlanStrategy` com cinco implementações, uma por plano, escolhidas por um mapa (`RatePlanStrategies.java:10-17`). Também acrescentou o `companyCode` da HTTP ao banco, com uma migração nova (`V2__add_company_code.sql`). As 72 conferências do comportamento passam, os 35 testes passam e nenhuma migração da base foi editada. Não há defeito sério. O ponto fraco é o design dentro das estratégias: o laço que soma as diárias foi copiado em quatro classes, e o `PriceCalculator` e as estratégias dependem um do outro.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, as mesmas da base (`measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.reservations` passou de 13 para 18 classes (de 508 para 768 linhas). Javalin só em `hotel` e `hotel.web`.
**Nota:** bom

O agente não mexeu no desenho dos pacotes: as dependências e o ciclo são idênticos aos da base. O ciclo vem de `ReservationService.java:3` (importa `hotel.documents.ReservationReceipt`) e de `ReservationReceipt.java:3` (importa `hotel.reservations.Reservation`), e é da base. A borda HTTP continua fina. A mudança na web é só ler o campo novo (`CreateReservationRequest.java:16`) e repassá-lo (`HttpApi.java:128-135`). A regra dos planos fica toda em `hotel.reservations`, sem Javalin e sem SQL nas estratégias, e o SQL novo ficou isolado em `ReservationStore.java:31-44`. A montagem geral continua em `Hotel.java:21-27`. A exceção pequena é que o `PriceCalculator` monta as estratégias por dentro (`PriceCalculator.java:20-22`, `new RatePlanStrategies(this)`). Como isso fica no mesmo pacote e não cria dependência nova, conta no design e não aqui.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan`, `LongStayRatePlan`) e 0 fora dela. Sobrou 0 `switch`/`if` de comportamento sobre o plano: os dois da base foram removidos. Há 1 ponto de seleção (mapa). O mesmo laço de diárias aparece 4 vezes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A interface `RatePlanStrategy` (`RatePlanStrategy.java:11-21`) reúne as três coisas que variam por plano: a validação (`validate`), o preço (`calculatePrice`) e a devolução (`calculateRefund`). Cada plano, inclusive os dois que já existiam, tem a sua classe:
- a validação do convênio (código obrigatório e na lista) está em `CorporateRatePlan.java:22-29`;
- a da sexta a domingo está em `WeekendPackageRatePlan.java:20-32`;
- a das 7 noites está em `LongStayRatePlan.java:19-25`;
- as devoluções ficam em cada classe (por exemplo, a metade em `WeekendPackageRatePlan.java:42-44` e o zero em `NonRefundableRatePlan.java:33-35`).

O cálculo pede ao plano sem nomeá-lo (`PriceCalculator.java:24-30`), e o cancelamento também (`ReservationService.java:83` → `PriceCalculator.java:32-34`). A escolha é um mapa num lugar só (`RatePlanStrategies.java:10-17`), que a referência aceita. Nenhum `switch` de comportamento sobre o plano ficou no código. Um plano novo é uma classe e uma linha no mapa.

**O exagero.** `RatePlanStrategies` é uma classe que só embrulha um `Map` com um `get`. É o ponto de seleção do pattern e não uma camada fora dele, por isso não conta como exagero. Não há fábricas, builders nem interfaces de uma implementação só.

**O resto do design, os defeitos:**
- **Duplicação.** O laço que soma as diárias foi copiado igual em quatro estratégias: `StandardRatePlan.java:23-25`, `NonRefundableRatePlan.java:25-27`, `CorporateRatePlan.java:34-36` e `LongStayRatePlan.java:30-32`. O cálculo do desconto percentual e o `new Price(...)` também se repetem em três delas. Antes era um trecho só no `PriceCalculator`. Uma mudança na regra das diárias vai ter de ser feita em quatro lugares.
- **Dependência circular entre objetos.** O `PriceCalculator` cria as estratégias passando `this` no próprio construtor (`PriceCalculator.java:20-22`), e as estratégias chamam de volta `priceCalculator.nightPrice` (por exemplo, `StandardRatePlan.java:24`). O `PriceCalculator` faz ao mesmo tempo o papel de contexto e de utilitário das diárias. O objeto vaza ainda incompleto, e nada disso pode ser trocado de fora.
- **Assinatura larga.** Todo método da interface recebe `room, checkIn, checkOut, nights, companyCode` (`RatePlanStrategy.java:14,17`). Só o convênio usa o `companyCode`, e a maioria ignora `checkOut` e `room` no `validate`. O próximo plano com um dado próprio vai obrigar a mudar as cinco classes. Um objeto com o pedido resolveria isso.
- **Detalhes:**
  - a devolução do pacote é uma linha confusa, com nome qualificado inline e um `cents` redundante sobre um valor que já tem escala 2 (`WeekendPackageRatePlan.java:43`);
  - `PACKAGE_PRICES.get(room.type())` devolve `null` para um tipo de quarto novo, sem aviso (`WeekendPackageRatePlan.java:36`);
  - o código da empresa é um `String` solto no domínio (`NewReservation.java:12`).

O resto do código veio da base: dinheiro em `BigDecimal` com meio para o par (`PriceCalculator.java:53-55`) e datas em `LocalDate`.

## Testes

**Números:** 35 testes rodaram, 0 falharam e 0 foram pulados; 35 métodos escritos. Na base eram 26: o agente acrescentou 9 e manteve os 26. Cobertura de linhas: 94,4% (543/575; na base, 93,8%). Cobertura de ramos: 87,1% (115/132; na base, 87,1%). Em `hotel.reservations`: 276/290 linhas e 63/68 ramos.
**Nota:** com problemas

**O que os testes novos conferem com valores:**
- os três exemplos do texto, pelo cálculo: R$ 686,22 em `PriceCalculatorTest.java:80-88`, R$ 820,00 em `PriceCalculatorTest.java:90-97` e R$ 1.574,37 em `PriceCalculatorTest.java:99-108`;
- os mesmos exemplos pelo serviço, contra o banco: `ReservationServiceTest.java:163-207`;
- as recusas (convênio sem código e com código inválido, pacote fora de sexta a domingo, 6 noites): `ReservationServiceTest.java:164-207`;
- a devolução de R$ 410,00 do pacote: `ReservationServiceTest.java:210-221`;
- a devolução integral do convênio e da estadia longa: `ReservationServiceTest.java:224-239`;
- um teste pela API: `HttpApiTest.java:124-155`.

Nenhum teste da base foi apagado nem enfraquecido; o patch só acrescentou o `null` do `companyCode` nas chamadas.

**Pela pergunta da mutação, ficam sem rede:**
- os descontos da Agrovale (0.18) e da Farmavida (0.25): nenhum teste usa esses códigos;
- os preços do pacote no standard (520,00) e na suíte (1.200,00): só o deluxe é testado;
- o pacote em alta temporada: nada falharia se o preço fechado passasse a somar a temporada;
- a estadia longa com noites de temporada.

Além disso, o teste pela API confere só o status 400 e não o código `INVALID_REQUEST` que o pessoal do site pediu (`HttpApiTest.java:155`), e usa `asDouble` para dinheiro (`HttpApiTest.java:143-149`). As regras principais estão cobertas, mas metade das tabelas de valores novas não tem teste.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro: 6 colunas em `numeric(10,2)`, todas ok. 0 chaves estrangeiras sem índice, 0 datas em texto. 4 colunas de situação ou tipo sem CHECK, todas vindas da base. O EXCLUDE contra duas reservas na mesma noite foi mantido. Flyway com 2 migrações: `V1` igual à da base e `V2__add_company_code.sql` nova. SQL sempre com `?`.
**Nota:** bom

O agente evoluiu o esquema do jeito certo: uma migração nova, sem editar a que já existia (`V2__add_company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code text`, anulável, o que não quebra as reservas antigas). O SQL novo continua parametrizado (`ReservationStore.java:31-44`) e a leitura usa a coluna nova (`ReservationStore.java:131-133`). O `rate_plan` sem CHECK (`V1__create_tables.sql`) vem da base; com Flyway, um CHECK sobre a lista de planos seria só mais uma migração, e o agente não a fez, mas a falta não é dele. Ponto pequeno: o código da empresa é gravado em qualquer plano, inclusive no STANDARD, quando vem no pedido (`ReservationStore.java:44`). O texto diz que nos outros planos ele "pode ignorar"; o preço ignora o código, mas o dado fica guardado sem uso.

## Comportamento

**Números:** 72 de 72 conferências passaram, nenhuma falhou:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

O sistema faz pela API o que o dono pediu:
- **Convênio:** Tecnosul, Agrovale e Farmavida com 12%, 18% e 25% (R$ 686,22, R$ 639,44 e R$ 584,85). Sem código ou com código desconhecido, responde 400 `INVALID_REQUEST`. O cancelamento devolve tudo.
- **Pacote de fim de semana:** R$ 520,00, R$ 820,00 e R$ 1.200,00, e em julho continua R$ 820,00. É recusado de sábado a segunda e de sexta a segunda. O cancelamento devolve R$ 410,00.
- **Estadia longa:** R$ 1.574,37, com a temporada valendo nas noites de temporada (R$ 1.754,30). Com 6 noites é recusada. O cancelamento devolve tudo.
- **Planos antigos:** o `companyCode` num STANDARD é ignorado no preço, e o NON_REFUNDABLE e toda a parte comum continuam como antes.

Uma observação fora do roteiro, que não muda a nota: a validação do plano roda depois da checagem de quarto ocupado (`ReservationService.java:42-47`). Um convênio com código errado num quarto ocupado responde 409 `ROOM_UNAVAILABLE` em vez de 400. O texto não fala dessa ordem.

## Dúvidas

- O exagero: `RatePlanStrategies` (`RatePlanStrategies.java:6-22`) é uma classe que só repassa um `Map.get`. Contei-a como o ponto de seleção do pattern, e por isso como contenção. Lida como camada sem necessidade, viraria exagero leve.
- Gravar o `companyCode` também nos planos que não são convênio (`ReservationStore.java:44`) é uma regra de plano pequena fora do plano. A referência manda só registrá-la aqui, sem mudar a classe.
