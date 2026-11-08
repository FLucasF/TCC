# Revisão: manutenção, cenário 01

## Visão geral

O projeto é o da base, com a mesma forma: `hotel.web` (Javalin) lê a requisição e chama `hotel.reservations`, onde `ReservationService` aplica as regras, `PriceCalculator` calcula o preço e `ReservationStore` fala com o PostgreSQL por JDBC; `Hotel` monta tudo, e o esquema vem das migrações do Flyway (`V1` da base, intocada, e uma `V2` nova com a coluna `company_code`). O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` e fez cada um funcionar como o texto pede: o roteiro do comportamento passou inteiro (72 de 72), os 40 testes passam e o banco evoluiu por migração nova. O que há de mais sério é o design: o texto avisa que o comercial vive inventando plano, e o agente resolveu a variação fazendo os `switch` da base crescerem para os cinco casos em três lugares (`ReservationService.create`, `ReservationService.cancel`, `PriceCalculator.calculate`), com a lista de empresas copiada em dois deles; o próximo plano vai mexer em quatro arquivos. Os testes novos conferem os exemplos do texto com valores, mas deixam sem rede os descontos da Agrovale e da Farmavida, os preços do pacote no standard e na suíte e a devolução integral do convênio e da estadia longa.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), exatamente como na base (`measures/dependencies.md` e `measures/dependencies-base.md` diferem só em linhas de código: `hotel.reservations` 508 → 578, `hotel.web` 236 → 238).
**Nota:** bom

O agente não mexeu na estrutura: nenhum pacote, dependência ou ciclo novo; o único ciclo (`ReservationService` usa `ReservationReceipt`, que usa `Reservation`) já existia na base e veio dela. A borda continua fina: `HttpApi.java:128-130` só passa o `companyCode` novo do JSON para o `NewReservation`, e a regra do plano (código obrigatório no convênio, sexta a domingo, 7 noites) ficou no serviço (`ReservationService.java:47-68`), sem Javalin e sem SQL. O acesso ao banco continuou isolado em `ReservationStore` (`ReservationStore.java:19`, `:32-33`, `:44`, `:133`, só a coluna nova), e a montagem não mudou (`Hotel.java:294-303`). A única classe que cresceu de verdade foi `ReservationService` (138 → 165 linhas), por causa do bloco de `switch` em `create`; isso pesa no design, não aqui.

## Design de baixo nível

**Números:** 0 dos 5 planos numa estrutura própria; 3 `switch` de comportamento sobre o plano (`ReservationService.java:47-68`, `ReservationService.java:108-112`, `PriceCalculator.java:33-39`) mais 1 sobre a empresa (`PriceCalculator.java:52-57`); a lista das três empresas escrita em 2 lugares (`ReservationService.java:73` e `PriceCalculator.java:53-55`). Um plano novo toca 4 arquivos (`RatePlan`, `ReservationService.create`, `ReservationService.cancel`, `PriceCalculator.calculate`).
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

O pattern pedido é o Strategy: cada plano com o seu preço, a sua validação e a sua devolução num lugar só dele, e o cálculo e o cancelamento pedindo ao plano. O que aproxima: nada de estrutura; o `RatePlan` continua um `enum` sem comportamento (`RatePlan.java:3-14`). O que afasta: os dois `switch` que a base tinha para dois planos cresceram para cinco casos, e um terceiro apareceu. Em `ReservationService.java:47-68` o `switch (request.ratePlan())` decide a validação de cada plano e qual método de cálculo chamar (o código da empresa em `:49-51`, sexta a domingo em `:55-57`, 7 noites em `:61-64`); em `PriceCalculator.java:33-39` outro `switch` decide o desconto por plano; em `ReservationService.java:108-112` um terceiro decide a devolução. O `WEEKEND_PACKAGE` ainda ganhou um método à parte, `calculateWeekendPackage` (`PriceCalculator.java:41-48`), escolhido pelo `switch` do serviço. É exatamente o caso que a referência chama de omissão: o próximo plano (melhor idade, 60 dias de antecedência) continua mexendo em vários lugares.

Exagero: não há. Nenhuma interface com uma implementação só, nenhuma fábrica ou camada que só repassa; o agente foi contido, até demais.

O design do resto do código: a base era limpa (dinheiro em `BigDecimal` com `HALF_EVEN`, datas em `LocalDate`, `enum` para plano e situação, exceções com código), e os tipos novos seguem o mesmo padrão. Os defeitos são localizados na parte nova:

- a lista das empresas está duplicada: `ReservationService.java:72-74` (`isValidCompanyCode`) e `PriceCalculator.java:52-57` (`getCorporateDiscount`); uma empresa nova tem de entrar nos dois, e o `default` do segundo lança `IllegalArgumentException` que nunca chega porque o primeiro já recusou;
- `calculateWeekendPackage` (`PriceCalculator.java:41`) recebe `checkIn` e `checkOut` e não usa nenhum dos dois (as noites são o literal `2` em `:47`); os preços do pacote são literais dentro do `switch` (`:43-45`), enquanto todos os outros valores do cálculo são constantes nomeadas (`:19-23`);
- o `case WEEKEND_PACKAGE -> cents(BigDecimal.ZERO)` em `PriceCalculator.java:37` é um caminho morto que, se alguém chamar `calculate` com esse plano, devolve o preço por diária em vez do pacote: dois jeitos de calcular o mesmo plano na mesma classe;
- nomes totalmente qualificados no meio do código em vez de `import` (`java.time.temporal.ChronoUnit` em `ReservationService.java:61`; `java.math.BigDecimal` e `java.math.RoundingMode` em `:111`), e a linha `:111` com a divisão da devolução passa de 130 colunas;
- `isWeekendPackageValid` (`PriceCalculator.java:81-86`) confere as duas noites com `checkOut.minusDays(1).equals(checkIn.plusDays(1))`, que funciona mas obriga o leitor a fazer a conta;
- `create` passou de 19 para 44 linhas (`ReservationService.java:27-70`) e agora valida, escolhe o cálculo e grava.

## Testes

**Números:** 40 testes rodaram, 0 falharam, 0 pulados; 40 métodos de teste no código (a base tinha 26: 14 novos, nenhum apagado ou enfraquecido). Cobertura de linhas 93,3% (505 de 541; base 93,8%) e de ramos 83,4% (126 de 151; base 87,1%); em `hotel.reservations`, 74 de 87 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

Os testes da base continuam passando, só com o parâmetro novo (`null`) nas chamadas. Os 14 novos conferem com valores os exemplos do texto: Tecnosul em deluxe, R$ 779,80 / 93,58 / 686,22 (`PriceCalculatorTest.java:78-88` e `ReservationServiceTest.java:175-184`); o pacote deluxe a R$ 820,00 (`PriceCalculatorTest.java:91-100`, `ReservationServiceTest.java:192-200`) e a devolução de R$ 410,00 depois de pago (`ReservationServiceTest.java:218-228`); a estadia longa de 7 noites, R$ 1.749,30 / 174,93 / 1.574,37 (`PriceCalculatorTest.java:103-112`, `ReservationServiceTest.java:209-215`). As recusas estão testadas: convênio sem código e com código inválido (`ReservationServiceTest.java:164-173`, e pela API com 400 e `INVALID_REQUEST` em `HttpApiTest.java:124-131`), pacote fora de sexta a domingo (`:186-190`), estadia longa com 6 noites, que é o limite certo (`:203-207`). Pela API, o pacote e a estadia longa conferem o total (`HttpApiTest.java:145-171`); o convênio pela API só confere o `ratePlan` (`:133-143`), sem o valor.

O que ficou sem rede, pela pergunta da mutação:

- os descontos da Agrovale (18%) e da Farmavida (25%) não aparecem em nenhum teste: trocar `0.18` por `0.81` em `PriceCalculator.java:21` ou `0.25` por `0.52` em `:22` não derruba teste algum;
- os preços do pacote no standard (R$ 520,00) e na suíte (R$ 1.200,00) não são conferidos: trocar os literais em `PriceCalculator.java:43` e `:45` passa;
- a devolução integral do convênio e da estadia longa não é testada: mover `CORPORATE` ou `LONG_STAY` para o `case NON_REFUNDABLE` em `ReservationService.java:109-110` passa;
- a recusa do pacote é testada só com quarta a sexta (duas noites em dias errados, `ReservationServiceTest.java:188`); sexta a segunda (três noites) e sábado a segunda não têm teste, então tirar a conferência das duas noites em `PriceCalculator.java:82-84` passa;
- a estadia longa com noites de temporada (o texto diz que ela continua valendo) e o `companyCode` ignorado nos outros planos não têm teste.

É isso que explica a cobertura de ramos cair de 87,1% para 83,4%: os ramos novos sem teste são os das empresas, dos tipos de quarto do pacote e das devoluções.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 2 migrações do Flyway: `V1__create_tables.sql` igual à base e `V2__add_company_code.sql` nova. As conferências não acusaram nada: nenhuma tabela sem chave primária, todo dinheiro em `numeric(10,2)`, nenhuma chave estrangeira sem índice, nenhuma data em texto (`measures/database.md`).
**Nota:** bom

A mudança do esquema foi feita do jeito que o README e a boa prática pedem: uma migração nova com um único `ALTER TABLE reservations ADD COLUMN company_code text` (`V2__add_company_code.sql:1`), sem tocar na `V1`; quem já roda o sistema sobe sem perder dados. A coluna é opcional (`NULL`), o que combina com um campo que só o convênio usa. O código que fala com o banco continuou com parâmetros `?`: as concatenações que o script lista (`ReservationStore.java:19`, `:32-33`, `:99`, `:112`) são só a lista constante de colunas; o `companyCode` entra por `setString(9, …)` (`:44`) e sai por `getString("company_code")` (`:133`). A proteção contra duas reservas na mesma noite continua a `EXCLUDE … daterange(check_in, check_out) WITH &&` da base, confirmada pelas 10 rodadas simultâneas do roteiro. As colunas `rate_plan` e `status` em `text` sem `CHECK` vêm da base e não foram pioradas: o `CORPORATE`, `WEEKEND_PACKAGE` e `LONG_STAY` entram e saem por `RatePlan.valueOf` (`:132`).

## Comportamento

**Números:** 72 de 72 conferências passaram: a parte comum 46 de 46, os dados depois de reiniciar 3 de 3, o cenário 01 21 de 21, as duas reservas ao mesmo tempo 2 de 2 (`measures/behavior.md`).
**Nota:** bom

Nada falhou. Para o dono da pousada: os três planos funcionam como o financeiro conferiu (Tecnosul R$ 686,22, Agrovale R$ 639,44, Farmavida R$ 584,85; pacote deluxe R$ 820,00 e R$ 410,00 de volta; estadia longa R$ 1.574,37), as recusas respondem 400 com `INVALID_REQUEST` (convênio sem código ou com código errado, pacote de sábado a segunda ou de três noites, estadia longa de 6 noites), o pacote em julho continua R$ 820,00, a estadia longa com noites de temporada cobra a temporada (R$ 1.754,30), o convênio e a estadia longa devolvem tudo, o `companyCode` num plano normal é ignorado, e o normal e o não reembolsável continuam como antes. Reiniciar o serviço não perde nada, e dois pedidos ao mesmo tempo para o mesmo quarto deixam só um passar.

## Dúvidas

- O `companyCode` é gravado em `reservations.company_code` para qualquer plano (`ReservationStore.java:44`), não só para o convênio. O texto diz que nos outros planos "se vier, pode ignorar"; o serviço ignora no preço, mas o banco guarda o que o site mandou. Não muda nota alguma, mas é uma regra pequena fora do plano que a referência manda registrar aqui.
- O código da empresa é comparado como veio (`ReservationService.java:73`): `tecnosul` em minúsculas é recusado. O texto diz que o código é o nome em maiúsculas, então a escolha é defensável; fica registrada.
