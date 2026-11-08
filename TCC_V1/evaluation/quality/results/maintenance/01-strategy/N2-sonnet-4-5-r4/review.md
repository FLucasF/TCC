# Revisão: manutenção, cenário 01

## Visão geral

O sistema da base continua com o mesmo desenho: `hotel.web.HttpApi` (Javalin) lê o JSON e chama `ReservationService`
(`hotel.reservations`), que valida, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore` (JDBC com
`?`, pool, esquema por migrações do Flyway). O agente trocou os dois `switch` de plano da base (o desconto em
`PriceCalculator.calculate` e o reembolso em `ReservationService.cancel`) por uma interface `RatePlanStrategy` com
uma implementação por plano, os cinco, escolhida por um mapa em `RatePlanStrategies`; acrescentou `companyCode` do
JSON até o banco (migração nova `V2__add_company_code.sql`) e nove testes. O roteiro de comportamento passou inteiro
(72 de 72), mas há um defeito real fora dele: a validação da estadia longa usa `Period.getDays()`
(`src/main/java/hotel/reservations/LongStayRatePlanStrategy.java:14`), que só conta os dias que sobram depois dos
meses, e por isso uma estadia de um mês ou mais (01/03 a 01/04/2027, 31 noites) é recusada com 400, conferido à mão
pela API. Além disso, o cálculo noite a noite com a alta temporada foi copiado em quatro estratégias.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` <-> `hotel.reservations`); na base,
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo. `hotel.reservations` passou de 13 classes/508 linhas
para 18 classes/741 linhas; nenhuma dependência nova entre pacotes.
**Nota:** bom

O agente pôs tudo o que é novo no lugar onde a regra já morava: as cinco estratégias e o registro em
`hotel.reservations` (`src/main/java/hotel/reservations/RatePlanStrategies.java:7`), sem Javalin nem JDBC nelas. A
borda HTTP só ganhou um campo e o repassa (`src/main/java/hotel/web/CreateReservationRequest.java:16`,
`src/main/java/hotel/web/HttpApi.java:129`); o SQL novo ficou em `ReservationStore`
(`src/main/java/hotel/reservations/ReservationStore.java:32`, `:44`, `:133`). O ciclo `documents`/`reservations`
veio da base (`measures/dependencies-base.md` tem o mesmo), o agente não o piorou. O único ponto a observar é que o
registro das estratégias é um mapa `static` imutável (`RatePlanStrategies.java:7`) usado direto por
`ReservationService` (`src/main/java/hotel/reservations/ReservationService.java:42`, `:84`) em vez de ser montado
no `Hotel`; para um mapa constante e sem estado num sistema deste tamanho, não é defeito.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os dois da base inclusive), 0 `switch`/`if` sobre o plano fora dela; preço,
validação e devolução de cada plano numa classe só (`RatePlanStrategy` com 3 métodos, 5 implementações); um único
ponto de seleção (`RatePlanStrategies.get`). O laço noite a noite com a alta temporada copiado em 4 classes.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

O pattern: a interface `src/main/java/hotel/reservations/RatePlanStrategy.java:7` tem `validate`, `calculatePrice` e
`calculateRefund`, e cada plano tem a sua classe com as três regras: o código da empresa e o desconto por empresa em
`CorporateRatePlanStrategy.java:10-35`, a sexta-a-domingo e o preço fechado por tipo de quarto em
`WeekendPackageRatePlanStrategy.java:12-36`, a metade na devolução em `WeekendPackageRatePlanStrategy.java:41`, as 7
noites e os 10% em `LongStayRatePlanStrategy.java`. Os dois `switch` da base sumiram: `PriceCalculator.calculate`
pede ao plano (`src/main/java/hotel/reservations/PriceCalculator.java:19-22`), `ReservationService.cancel` também
(`ReservationService.java:84-85`), e a validação é chamada sem nomear plano (`ReservationService.java:42-43`). O
`STANDARD` e o `NON_REFUNDABLE` também foram para a estrutura (`StandardRatePlanStrategy.java`,
`NonRefundableRatePlanStrategy.java`). O próximo plano (melhor idade, 60 dias de antecedência) é uma classe e uma
linha no mapa, mais a constante no `enum`. É acerto.

O exagero: não há estrutura sem necessidade fora do pattern. `PriceCalculator.calculate` virou um repasse de duas
linhas (`PriceCalculator.java:19-22`), mas a classe já existia, é usada pelos testes da base e continua guardando
`isHighSeason` e `cents`; não conta como camada nova.

O resto do design:

- **Duplicação:** o laço das noites e o `nightPrice` com o acréscimo da temporada estão copiados, iguais, em
  `StandardRatePlanStrategy.java:17` e `:30`, `NonRefundableRatePlanStrategy.java:17` e `:30`,
  `CorporateRatePlanStrategy.java:28` e `:42`, `LongStayRatePlanStrategy.java:24` e `:37`. Na base havia um só
  (`PriceCalculator.nightPrice`), que o agente apagou para copiar quatro vezes; quem mudar a regra da temporada tem
  de lembrar dos quatro lugares, e o plano seguinte vai copiar a quinta. Bastava um método comum (o subtotal das
  diárias) que cada estratégia chamasse.
- **A assinatura:** `calculatePrice(Room, LocalDate, LocalDate, String companyCode)`
  (`RatePlanStrategy.java:11`) põe na interface de todos os planos um parâmetro que só o convênio usa, enquanto
  `validate` recebe o `NewReservation` inteiro (`RatePlanStrategy.java:9`); o próximo plano com um dado próprio vai
  mexer na interface e nas cinco classes. Receber o `NewReservation` nos dois métodos evitaria isso.
- **Datas:** `checkIn.until(checkOut).getDays()` (`LongStayRatePlanStrategy.java:14`,
  `WeekendPackageRatePlanStrategy.java:23`) usa o componente de dias de um `Period`, não o total de noites
  (`ChronoUnit.DAYS.between`); no pacote não faz diferença (sempre 2 noites), na estadia longa é o defeito descrito
  em Comportamento.
- **Dinheiro e tipos:** `BigDecimal` com `cents` (meio para o par) em todo lugar, preços do pacote por `RoomType`
  num `Map` (`WeekendPackageRatePlanStrategy.java:12`), o plano é `enum`. `NON_REFUNDABLE_DISCOUNT` ficou em
  `PriceCalculator.java:17`, longe da única classe que o usa (`NonRefundableRatePlanStrategy.java:21`): pequeno.
- O `README.md` não foi atualizado: ainda descreve só dois planos e o corpo do `POST /reservations` sem
  `companyCode` (`README.md:32`, `:37-38`, `:70`).

## Testes

**Números:** 35 testes rodaram, 0 falhas, 0 erros, 0 pulados; 35 métodos escritos (base: 26). Cobertura de linhas
94,1% (544 de 578; base 93,8%) e de ramos 87,0% (120 de 138; base 87,1%); `hotel.reservations` com 282 de 298
linhas e 68 de 74 ramos.
**Nota:** com problemas

O agente manteve os 26 testes da base passando, só acrescentando `null` no novo campo
(`src/test/java/hotel/reservations/PriceCalculatorTest.java`, `src/test/java/hotel/reports/ReportsTest.java:33`),
sem enfraquecer nenhum, e escreveu nove testes novos em `src/test/java/hotel/reservations/ReservationServiceTest.java`
que conferem com valores os três exemplos do financeiro: Tecnosul 779,80/93,58/686,22 (`:180-185`), pacote deluxe
820,00 e devolução 410,00 (`:209-225`), estadia longa 1.749,30/174,93/1.574,37 (`:237-242`); as recusas do convênio
(sem código, vazio, inválido, `:170`), do pacote (sábado, três noites, uma noite, `:199`) e da estadia longa (6
noites, no limite, `:229`); e a devolução integral do convênio e da estadia longa (`:189-195`, `:246-252`).

O que ficou sem rede, pela pergunta da mutação:

- trocar 0.18 por 0.81 (Agrovale) ou 0.25 por 0.52 (Farmavida) em `CorporateRatePlanStrategy.java:10` não derruba
  nenhum teste: só a Tecnosul é conferida com valor (Agrovale aparece só no teste de devolução, `:189`);
- trocar 520.00 ou 1200.00 em `WeekendPackageRatePlanStrategy.java:12` não derruba nada: só o deluxe é testado;
- a estadia longa com noites de temporada não é testada, nem uma de um mês ou mais, que é justamente onde está o
  defeito do `getDays()`;
- nada passa pela API: `HttpApiTest` não mudou, então o `companyCode` no JSON e a resposta 400 das regras do plano
  não têm teste pela borda.

Os testes são bons para os casos que o dono deu, mas deixam metade das tabelas novas sem conferência e deixaram
escapar um erro de regra.

## Banco

**Números:** 3 tabelas, todas com chave primária; 2 migrações do Flyway (`V1__create_tables.sql` igual à base,
`V2__add_company_code.sql` nova); dinheiro todo em `numeric(10,2)`; nenhuma chave estrangeira sem índice; nenhuma
data em texto; `rate_plan` e `status` em `text` sem CHECK (como na base); EXCLUDE de sobreposição de noites mantido;
SQL com `+` só com constantes.
**Nota:** bom

A mudança no esquema foi feita do jeito certo: uma migração nova, sem tocar na da base
(`src/main/resources/db/migration/V2__add_company_code.sql:1`, `ALTER TABLE reservations ADD COLUMN company_code
text`), anulável, o que serve às reservas que já existem. O código grava e lê a coluna com parâmetro
(`src/main/java/hotel/reservations/ReservationStore.java:44`, `:133`). A falta de CHECK em `rate_plan` veio da base
(o agente acrescentou três valores ao `enum`, e um CHECK teria de ser mudado a cada plano, o que justifica não ter).
Um ponto menor: o código da empresa é gravado em qualquer plano (`ReservationStore.java:44`), e não só no convênio;
o pedido dizia que nos outros planos ele "pode ignorar", e o preço o ignora (o roteiro confere: desconto 0,00), mas
o texto fica guardado sem uso e não aparece na resposta da API.

## Comportamento

**Números:** 72 de 72 conferências passaram: parte comum 46 de 46, dados depois de reiniciar 3 de 3, cenário 01 21
de 21, reservas simultâneas 2 de 2. Fora do roteiro, chamando a API à mão: `LONG_STAY` de 01/03 a 01/04/2027 (31
noites) e de 01/03 a 03/04/2027 (33 noites) responderam 400 `INVALID_REQUEST` "long stay requires at least 7 nights".
**Nota:** com problemas

Tudo o que o roteiro pede funciona: os três exemplos do financeiro (686,22; 820,00 e 410,00; 1.574,37), as três
empresas com os seus descontos, as recusas com 400 `INVALID_REQUEST`, o pacote nos três quartos e em julho sem
temporada, a estadia longa com temporada, o `companyCode` ignorado nos outros planos, e o que já existia continuou
igual, inclusive depois de reiniciar e com duas reservas ao mesmo tempo.

Mas a regra "7 noites ou mais" está errada para estadias longas de verdade: a conta usa `Period.getDays()`
(`src/main/java/hotel/reservations/LongStayRatePlanStrategy.java:14`), que para 01/03 a 01/04 dá 0 (o período é "1
mês e 0 dias") e para 01/03 a 03/04 dá 2. Para o dono da pousada, isso quer dizer que o hóspede que quer ficar um
mês, o cliente que o plano de estadia longa mais quer atrair, recebe "a reserva não pode ser feita", enquanto o de 8
noites consegue; qualquer estadia de um mês ou mais só passa se os dias que sobram além dos meses inteiros forem 7 ou
mais. O pacote de fim de semana usa a mesma conta (`WeekendPackageRatePlanStrategy.java:23`), mas ali o período é
sempre de 2 dias e o erro não aparece.

## Dúvidas

- O defeito da estadia longa de um mês ou mais não está no roteiro; foi conferido à mão pela API (31 e 33 noites
  recusadas com 400). Pesou só em Comportamento; em Design aparece como a causa (o uso de `Period.getDays()`), sem
  mudar a nota, que já vinha da duplicação.
- O código da empresa é aceito só em maiúsculas exatas (`tecnosul` é recusado com 400,
  `CorporateRatePlanStrategy.java:19`). O texto diz que o código "é o nome dela em maiúsculas", então é uma leitura
  razoável, e não contei como defeito.
- Gravar o `companyCode` também nos planos que não são convênio (`ReservationStore.java:44`) é uma regra de plano
  pequena fora do plano; pela referência do Strategy, não muda a classe.
