# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel.web` (rotas e DTOs) recebe a requisição, monta um `NewReservation` e chama `hotel.reservations.ReservationService`, que usa `PriceCalculator` para o preço e `ReservationStore` (JDBC com pool) para gravar no PostgreSQL, com o esquema criado por Flyway. O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) só como constantes novas do `enum` e fez crescer os dois `switch` que a base já tinha: o do preço (`PriceCalculator.java:26-32`) e o da devolução (`ReservationService.java:104-108`). Acrescentou também um terceiro `switch` para validar o convênio (`ReservationService.java:55-66`). O Strategy que o texto pede não foi feito. Ele criou uma migração nova para `company_code` (`V2__add_company_code.sql`) e 30 testes. O roteiro passa inteiro (72 de 72), mas o defeito mais sério ficou fora dele. O pacote de fim de semana e a estadia longa contam as noites com `Period.getDays()` (`PriceCalculator.java:77` e `:93`), e esse método só devolve a parte "dias" do período, sem os meses. Pela API eu confirmei três efeitos. Uma estadia de 30 noites, de sexta 05/02/2027 a domingo 07/03/2027, é aceita como pacote por R$ 820,00 com `nights: 2`. Uma estadia longa de 33 noites (01/03 a 03/04) é recusada com "requires at least 7 nights". Uma de 40 noites (01/05 a 10/06) é gravada com `nights: 9`.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles e 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), iguais na base e no final (`measures/dependencies.md` e `measures/dependencies-base.md`). `hotel.reservations` cresceu de 508 para 617 linhas. A maior classe continua `HttpApi` (170 linhas).
**Nota:** bom

O agente não mexeu na estrutura. A borda HTTP só ganhou o campo novo: `companyCode` passa de `CreateReservationRequest.java:16` para `NewReservation` em `HttpApi.java:129`, sem nenhuma regra do plano nas rotas. A validação e o preço ficam em `hotel.reservations`, sem Javalin, e o SQL novo fica só em `ReservationStore.java:19`, `:32`, `:50` e `:133`. O ciclo `documents` ↔ `reservations` (`ReservationReceipt.java:3` importa `Reservation`, `ReservationService.java:3` importa `ReservationReceipt`) já vinha da base, e o agente não o piorou. A montagem continua num lugar só (`Hotel.java`). `PriceCalculator` mais que dobrou (de 53 para 125 linhas), mas isso pesa no design, não nas camadas.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem comportamento próprio: o `enum` `RatePlan` só tem constantes (`RatePlan.java:3-14`). O comportamento dos planos fica em 3 `switch`: o preço (`PriceCalculator.java:26-32`, 5 casos), a devolução (`ReservationService.java:104-108`, 5 casos) e a validação do convênio (`ReservationService.java:56-65`). A lista de empresas aparece duas vezes. O laço das noites foi copiado 4 vezes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto avisa que "o comercial vive inventando plano" e que cada um traz preço, regra de uso e devolução próprios. Pela referência, a resposta seria cada plano com o seu comportamento num lugar só dele. O agente fez o contrário: fez crescer os `switch` que a base já tinha. O do preço escolhe um método privado por plano (`PriceCalculator.java:26-32`), e esses métodos não são estratégias, porque o `switch` decide o comportamento e cada método é uma cópia por caso. O da devolução também cresceu (`ReservationService.java:104-108`). Além disso, as regras de uso ficaram espalhadas. A do convênio está num `switch` no serviço (`ReservationService.java:55-70`). As do pacote e da estadia longa viram `IllegalArgumentException` no meio do cálculo (`PriceCalculator.java:78-83` e `:94-96`), que o serviço traduz em `HotelException` (`ReservationService.java:50-51`). Um plano novo, como o da melhor idade, mexe em pelo menos dois arquivos e três lugares. É omissão.

**O exagero.** Não há estrutura sem necessidade: nada de interface nova, fábrica ou camada que só repassa. É contenção.

**O resto do código.** Os defeitos estão concentrados no que o agente escreveu:
- **O laço das noites copiado.** O mesmo laço que soma as diárias está copiado quatro vezes (`PriceCalculator.java:38-41`, `:48-51`, `:59-62` e `:98-100`). Na estadia longa e no pacote, as noites são contadas de outro jeito, com `checkIn.until(checkOut).getDays()` (`:77` e `:93`). Essa diferença é a origem do erro descrito na visão geral: `Period.getDays()` ignora os meses, e as cópias, com contagens diferentes, deixaram de concordar.
- **A lista de empresas em dois lugares.** Ela aparece em `ReservationService.java:69` e de novo em `PriceCalculator.java:63-66`, com um `default -> null` seguido de teste de `null` (`:67-71`). Uma empresa nova precisa ser lembrada nos dois.
- **Um `catch` largo demais.** O `catch (IllegalArgumentException)` envolve também `store.add` (`ReservationService.java:47-52`). Um `IllegalArgumentException` vindo do JDBC viraria um 400 "dado inválido" e esconderia a causa.
- **Valores soltos no código.** A devolução de 50% é um `new BigDecimal("0.5")` no meio do `switch` (`ReservationService.java:107`), e os preços do pacote ficam no código do cálculo (`PriceCalculator.java:85-87`), enquanto os outros percentuais viraram constantes (`:18-23`).

Os tipos estão certos (`BigDecimal` com HALF_EVEN, `LocalDate`, `enum`), e não há estado mutável novo. Por isso a nota é `com problemas`, e não `ruim`.

## Testes

**Números:** 56 testes rodaram, 0 falharam e 0 foram pulados; há 56 métodos escritos (a base tinha 26). A cobertura é de 94,5% das linhas (536 de 567) e 87,9% dos ramos (138 de 157); na base era 93,8% e 87,1%. Em `hotel.reservations`, os ramos cobertos são 86 de 93. Fonte: `measures/tests.md` e `measures/tests-base.md`.
**Nota:** bom

Os 26 testes da base continuam lá e passando. O agente só acrescentou o `companyCode` às chamadas (por exemplo, `ReportsTest.java:33`) e não enfraqueceu nenhuma asserção. A parte nova é conferida com valores, em três níveis:
- **No cálculo:** Tecnosul 93,58 / 686,22 (`PriceCalculatorTest.java:80-90`), Agrovale (`:93-102`), Farmavida (`:105-114`), os três preços do pacote (`:123-155`) e a estadia longa com e sem temporada (`:169-195`). As recusas também estão lá: dia da semana errado, 3 noites e 6 noites (`:157-200`).
- **No serviço, contra o banco:** a devolução de 410,00 do pacote (`ReservationServiceTest.java:214-221`) e a devolução integral no convênio e na estadia longa (`:224-241`).
- **Pela API:** 400 `INVALID_REQUEST` para convênio sem código e com código inválido (`HttpApiTest.java:137-158`).

A pergunta da mutação: trocar 0.12 por 0.21, 0.5 por outro valor, ou `< 7` por `<= 7` quebra algum teste. O que fica sem rede:
- **As estadias que passam de um mês:** nenhum teste cobre uma estadia que atravessa o mês, e por isso o erro do `Period.getDays()` passou.
- **O pacote na alta temporada:** só é testado em março, então somar a temporada ao preço fechado não seria pego.
- **O código ignorado nos outros planos:** a regra "nos outros planos o `companyCode` pode ser ignorado" não tem teste.

Há também repetições: `exampleCorporateTecnosulDeluxe` (`PriceCalculatorTest.java:203`), `exampleWeekendPackageDeluxe` (`:214`) e `exampleLongStayStandard` (`:225`) repetem `corporateTecnosulDiscount` (`:80`), `weekendPackageDeluxeRoom` (`:135`) e `longStayDiscount10Percent` (`:169`). Os testes da API comparam dinheiro com `asDouble()` e tolerância de 0,01 (`HttpApiTest.java:133`). São defeitos pequenos; o conjunto confere as regras pedidas.

## Banco

**Números:** 3 tabelas, todas com chave primária, e todo o dinheiro em `numeric(10,2)`. Nenhuma chave estrangeira está sem índice e nenhuma data está em texto. São 2 migrações Flyway: a V1 igual à da base e a V2 nova. A exclusão `EXCLUDE USING gist` contra reserva dupla foi mantida. Colunas `rate_plan`, `status`, `type` e `channel` sem CHECK (já assim na base). Fonte: `measures/database.md`.
**Nota:** bom

O agente fez o certo para evoluir o esquema: não editou a V1 e criou uma migração nova, `V2__add_company_code.sql:1` (`ALTER TABLE reservations ADD COLUMN company_code text`), que vale para um banco que já tem dados. O SQL novo usa parâmetro (`ReservationStore.java:33` e `:50`) e fica no mesmo try-with-resources da base. Dois pontos pequenos:
- **O código gravado em todo plano.** O código da empresa é gravado em qualquer plano (`ReservationStore.java:50`): uma reserva STANDARD enviada com `"companyCode":"TECNOSUL"` fica no banco e na resposta com `companyCode: "TECNOSUL"`, como confirmei pela API. O texto diz que, nos outros planos, o código "pode ignorar". Não há CHECK ligando `company_code` a `rate_plan`.
- **O CHECK que faltava na base.** A falta de CHECK em `rate_plan` vem da base, mas teria sido um bom momento para pô-lo.

A coluna `nights` errada (9 numa estadia de 40 noites) é gravada certinho pelo banco; o defeito é do cálculo e pesa no comportamento.

## Comportamento

**Números:** 72 de 72 conferências passaram: 46 de 46 na parte comum, 3 de 3 depois de reiniciar, 21 de 21 no cenário 01 e 2 de 2 nas reservas simultâneas (`measures/behavior.md`).
**Nota:** com problemas

Pelo roteiro, tudo funciona: os três exemplos do financeiro (686,22, 820,00 com devolução de 410,00, e 1.574,37), as três empresas, as recusas com 400 `INVALID_REQUEST`, o pacote em julho a 820,00, a estadia longa com temporada, as devoluções, o normal e o não reembolsável como antes. Fora do roteiro, chamei a API e confirmei que duas regras do texto falham quando a estadia passa de um mês, porque as noites são contadas com `Period.getDays()` (`PriceCalculator.java:77` e `:93`):
- **O pacote fora das datas:** o texto diz "exatamente duas noites", sexta e "o domingo logo depois". Mesmo assim, uma reserva do deluxe de sexta 05/02/2027 a domingo 07/03/2027 (30 noites, com alta temporada em fevereiro) foi criada como `WEEKEND_PACKAGE` por R$ 820,00, com `nights: 2`. Para o dono da pousada, isso é o quarto preso um mês inteiro pelo preço de um fim de semana.
- **A estadia longa:** a de 01/03 a 03/04/2027 (33 noites) foi recusada com "Long stay requires at least 7 nights". A de 01/05 a 10/06/2027 (40 noites) foi aceita, mas com `nights: 9` na resposta e no banco. O valor (R$ 8.996,40) está certo, porque vem do laço.

O sistema não quebra e a parte comum está íntegra, mas parte das regras do cenário falha em datas comuns. Por isso a nota é `com problemas`. Também pela API, o `companyCode` mandado num plano STANDARD não altera o preço (o roteiro confere), mas é gravado e devolvido; isso já está contado no banco.

## Dúvidas

- Na nota do comportamento, hesitei entre `com problemas` e `ruim`. O erro do pacote deixa vender 30 noites por R$ 820,00, um risco de dinheiro perdido. Mas ele só aparece quando entrada e saída estão em meses diferentes e o dia da saída fica a 2 dias do da entrada, e não aparece em nenhum dos casos que o texto dá. Fiquei com `com problemas`.
- Gravar o `companyCode` nos planos que não são convênio é uma leitura possível de "pode ignorar" (ignorar no preço). Contei como defeito pequeno, sem mudar notas.
- Nos testes, `bom` pesou a cobertura com valores de todas as regras novas. Um revisor mais severo poderia dar `com problemas`, porque nenhum teste pegou o erro das estadias que passam de um mês.
