# Revisão: manutenção, cenário 01

## Visão geral

O sistema (da base) tem sete pacotes: `hotel` monta as peças (`Hotel.java:21-27`) e o pool, `hotel.web` é a borda
Javalin (`HttpApi`), `hotel.reservations` guarda as regras (`ReservationService`, `PriceCalculator`) e o acesso a dados
(`ReservationStore`, JDBC com `?`), e o resto (`rooms`, `messages`, `documents`, `reports`) cuida do que o nome diz.
Uma requisição `POST /reservations` passa por `HttpApi.readNewReservation` (`HttpApi.java:109-131`), vai para
`ReservationService.create` (`ReservationService.java:27-50`) e termina em `ReservationStore.add`, no PostgreSQL com
migrações do Flyway. O agente trocou os dois `switch` da base por uma interface `RatePlanStrategy` com uma classe para
cada um dos cinco planos, guardou o `companyCode` numa migração nova (`V2__add_company_code.sql`) e escreveu 8 testes
novos. Pela API, tudo o que o texto pede funciona (72 de 72). O ponto mais fraco é o design: o convênio não cabe na
interface, por isso `PriceCalculator.calculate` ainda pergunta `if (ratePlan == RatePlan.CORPORATE)` e faz um cast
(`PriceCalculator.java:33-34`). Além disso, `CorporateRatePlanStrategy.calculatePrice` da interface lança
`UnsupportedOperationException` (`CorporateRatePlanStrategy.java:35`).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Os números
são os mesmos da base (`measures/dependencies-base.md`: 7 pacotes, 17 dependências, o mesmo ciclo); só
`hotel.reservations` cresceu, de 13 classes e 508 linhas para 17 classes e 762 linhas.
**Nota:** bom

O agente não mexeu nas camadas. A borda HTTP continua fina: a única mudança em `HttpApi` é passar adiante o
`companyCode` (`HttpApi.java:128-130`), e a recusa por regra de plano é um `HotelException(INVALID_REQUEST)` lançado
nas estratégias (por exemplo, `CorporateRatePlanStrategy.java:26-29`), que a borda já transformava em 400
(`HttpApi.java:101-102`). As estratégias não conhecem Javalin nem JDBC; o SQL novo ficou em `ReservationStore`
(`ReservationStore.java:31-33`, `:44`, `:133`). A montagem principal continua em `Hotel.java:21-27`. O ciclo
`documents` ↔ `reservations` já vinha da base: `ReservationService` cria o `ReservationReceipt`
(`ReservationService.java:3`, `:66`). Há um ponto pequeno de montagem. O mapa de estratégias é criado dentro do
construtor de `PriceCalculator` (`PriceCalculator.java:22-29`), e o serviço usa esse mesmo calculador para validar e
para devolver (`ReservationService.java:42`, `:84`). Isso pesa no design, não nas camadas.

## Design de baixo nível

**Números:** 5 de 5 planos têm classe própria, com validação, preço e devolução (`Standard`, `NonRefundable`,
`Corporate`, `WeekendPackage`, `LongStay`). Os dois `switch` de comportamento da base sumiram. Sobrou 1 `if` sobre o
plano, com cast, no ponto que usa a estrutura (`PriceCalculator.java:33-34`), e 1 método da interface que um dos
planos não implementa (`CorporateRatePlanStrategy.java:35`).
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O que aproxima do Strategy.** Existe a interface `RatePlanStrategy` (`RatePlanStrategy.java:11-21`), com
`validate`, `calculatePrice` e `refundAmount`, e uma implementação para cada plano, inclusive os dois que já existiam.
A escolha é feita num mapa (`PriceCalculator.java:23-28`). O cancelamento pede a devolução ao plano, sem nomeá-lo
(`ReservationService.java:84-85`). A validação de cada plano está dentro dele: sexta a domingo em
`WeekendPackageRatePlanStrategy.java:23-31`, 7 noites em `LongStayRatePlanStrategy.java`, código da empresa em
`CorporateRatePlanStrategy.java:23-31`. A parte comum das diárias e da temporada foi para um só método,
`calculateByNightlyRate` (`PriceCalculator.java:44-53`), e não foi copiada.

**O que afasta.** A assinatura `calculatePrice(Room, LocalDate, LocalDate)` não deixa o plano ver o `companyCode`. Para
contornar isso, o agente:

- fez o método da interface lançar `UnsupportedOperationException` no convênio (`CorporateRatePlanStrategy.java:35`),
  o que viola o contrato da interface;
- criou uma sobrecarga com o código (`:38-41`);
- pôs no ponto de uso `if (ratePlan == RatePlan.CORPORATE) return ((CorporateRatePlanStrategy) strategy)...`
  (`PriceCalculator.java:33-34`).

É exatamente "quem usa a estrutura ainda perguntando qual é o caso". Os planos que o dono já anunciou (melhor idade,
antecedência de 60 dias) também precisam de dados da reserva que a assinatura não passa: a data de nascimento e a data
em que a reserva foi feita. Cada um vai somar mais um `if` com cast nesse lugar. Bastaria passar `NewReservation` para
o cálculo, como já se faz em `validate`. Por isso a classe é `malfeito`, e não `acerto`.

**Exagero.** Não há estrutura sem necessidade fora do pattern. A interface tem cinco implementações; não há fábrica,
builder nem camada que só repassa. Fica `contenção`.

**O resto do design.**

- **Acoplamento circular.** As estratégias recebem o `PriceCalculator` no construtor (`PriceCalculator.java:24-28`),
  e o mesmo `PriceCalculator` guarda o mapa delas. Ele acumula o papel de registro dos planos e o de calculador de
  diárias, e o serviço vai buscar nele a validação e a devolução (`strategyFor`, `PriceCalculator.java:39-41`;
  `ReservationService.java:42`, `:84`), que não são preço.
- **Validação redundante.** O pacote confere "sexta", depois "domingo" e depois "+2 dias"
  (`WeekendPackageRatePlanStrategy.java:23-31`). A terceira condição já implica a segunda.
- **Noites fixas.** O preço do pacote grava as noites como o literal `2` (`WeekendPackageRatePlanStrategy.java:37`).
- **Código guardado à toa.** O `companyCode` é gravado em qualquer plano (`ReservationStore.java:44`;
  `Reservation.java:39`), embora o texto diga que fora do convênio ele deve ser ignorado. O preço o ignora, mas o dado
  fica guardado sem uso.

Os tipos estão certos: `BigDecimal` com `HALF_EVEN` (`PriceCalculator.java:18`, `:72-74`; a metade em
`WeekendPackageRatePlanStrategy.java:42`) e `enum` para o plano e o tipo de quarto.

## Testes

**Números:** 34 testes rodaram, 0 falharam, 0 pulados; 34 métodos escritos (na base eram 26, todos mantidos e
passando). Cobertura de linhas de 93,8% (532/567) e de ramos de 86,7% (111/128). Na base eram 93,8% e 87,1%.
`hotel.reservations` tem 269/286 linhas e 59/64 ramos.
**Nota:** com problemas

O agente testou a parte nova com valores, nos dois níveis (calculador e serviço contra o banco):

- os três exemplos do financeiro: R$ 686,22 em `PriceCalculatorTest.java:78-87`, R$ 820,00 em `:90-99` e R$ 1.574,37
  em `:102-110`;
- as recusas do convênio sem código e com código inválido (`ReservationServiceTest.java:159-175`);
- o pacote fora de sexta a domingo, nos três jeitos (`:179-201`);
- a estadia longa com 6 noites recusada e com 7 aceita (`:216-228`);
- a devolução da metade, R$ 410,00 (`:204-213`), e a devolução total da estadia longa (`:231-240`).

Os testes da base só ganharam o argumento `null` novo e não ficaram mais fracos.

Pela pergunta da mutação, ficam sem rede:

- os descontos da Agrovale (0,18) e da Farmavida (0,25): só a Tecnosul é testada;
- os preços do pacote no standard (520,00) e na suíte (1.200,00): só o deluxe é testado;
- o pacote em alta temporada (que precisa continuar com o preço fechado);
- a estadia longa com noites de temporada;
- a devolução total do convênio (`CorporateRatePlanStrategy.java:44-45` poderia devolver zero sem que nada falhasse);
- o campo `companyCode` e o 400 da regra de plano pela API: `HttpApiTest` não mudou.

O roteiro do comportamento confere esses pontos, mas os testes do projeto não.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, as datas em `date`, e não há
chave estrangeira sem índice. Uma `EXCLUDE` impede duas reservas do mesmo quarto na mesma noite. 2 migrações: a `V1`
está igual à da base e a `V2` é nova. As colunas de situação e de plano não têm `CHECK`.
**Nota:** bom

A mudança no esquema veio na migração nova `V2__add_company_code.sql:2` (`ALTER TABLE reservations ADD COLUMN
company_code text`), sem editar a `V1`: um banco que já roda o sistema sobe sem problema. A coluna aceita nulo, o que
está certo para um campo usado só no convênio. O SQL novo usa parâmetros (`ReservationStore.java:31-33`, `:44`), e a
leitura volta o campo (`:133`). As concatenações que o script lista juntam só constantes (`COLUMNS`). A falta de
`CHECK` em `rate_plan` e `status` já vinha da base, e os três valores novos do `enum` entram nela sem mudança. Fica a
observação, já citada no design, de que o código é gravado também nos planos que deviam ignorá-lo
(`ReservationStore.java:44`), sem dano aos dados.

## Comportamento

**Números:** 72 de 72 conferências passaram:

- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que foi pedido funciona pela API:

- **Convênio:** a Tecnosul dá R$ 779,80 / 93,58 / 686,22; a Agrovale e a Farmavida dão os descontos certos; a reserva
  sem código ou com código desconhecido é recusada com 400 `INVALID_REQUEST`; o cancelamento devolve tudo.
- **Pacote:** R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho; recusado fora de sexta a domingo; devolve
  R$ 410,00.
- **Estadia longa:** R$ 1.574,37, com a temporada valendo; recusada com 6 noites; devolve tudo.
- **Planos que já existiam:** o normal ignora o `companyCode`, e o não reembolsável continua com 15%.

A parte que já funcionava continua igual, os dados sobrevivem ao reinício, e duas reservas simultâneas nunca criam as
duas.

## Dúvidas

- O defeito de design que decide o `malfeito` é um só, mas está no próprio ponto de seleção do Strategy
  (`PriceCalculator.java:33-34`, com o cast e o `UnsupportedOperationException` em
  `CorporateRatePlanStrategy.java:35`). Todos os cinco planos têm classe, e a validação e a devolução passam pela
  estrutura. Alguém pode ler isso como um `acerto` com uma mancha. Fiquei com `malfeito` porque o critério nomeia
  justamente o "quem usa a estrutura ainda perguntando qual é o caso".
- Gravar o `companyCode` em todos os planos (`ReservationStore.java:44`) é uma regra pequena de plano fora do plano.
  Pela referência, isso não muda a classe sozinho.
