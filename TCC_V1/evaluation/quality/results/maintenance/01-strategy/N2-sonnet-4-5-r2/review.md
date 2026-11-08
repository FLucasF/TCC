# Revisão: manutenção, cenário 01

## Visão geral

A base já era em camadas e o agente a manteve: `hotel.web` (Javalin, `HttpApi`) lê o corpo e chama
`ReservationService` (`hotel.reservations`). O serviço valida, pergunta a `ReservationStore` (JDBC, consultas com
`?`) se o quarto está livre, calcula o preço e grava no PostgreSQL. O esquema vem de migrações Flyway. Para os três
planos novos, o agente pôs no `enum RatePlan` uma estratégia por constante (`RatePlan.java:4-8`) atrás da interface
`RatePlanStrategy`, que tem três métodos: validar, calcular o preço e calcular a devolução. Os cinco planos ficaram
em classes próprias, os dois antigos inclusive, e os dois `switch` da base saíram: o do preço, que ficava em
`PriceCalculator`, e o da devolução, em `ReservationService.cancel`. O código da empresa chega por um campo novo,
`companyCode`, e vai para uma coluna nova, criada por uma migração nova (`V2__add_company_code.sql`). O `mvn verify`
passa com 34 testes, e as 72 conferências do roteiro passaram. Não há defeito sério. Os pontos fracos são
pequenos: o mesmo cálculo copiado em quatro estratégias, o `companyCode` presente na assinatura de todos os planos,
testes novos que só conferem os exemplos do texto (sem Agrovale, sem Farmavida, sem o pacote standard e suíte, sem
alta temporada nos planos novos) e o README, que não foi atualizado.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e
`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O
pacote `hotel.reservations` passou de 13 para 17 classes (de 508 para 728 linhas). Nenhuma dependência nova.
**Nota:** bom

O agente não mexeu na forma do sistema. Na borda HTTP, a única mudança é passar `body.companyCode()` adiante
(`hotel/web/HttpApi.java:129`); o handler continua só lendo, chamando o serviço e respondendo. As regras novas ficam
nas estratégias, dentro de `hotel.reservations`, sem Javalin nem JDBC (`CorporateRatePlan.java:1-44`,
`WeekendPackageRatePlan.java:1-45`). O serviço só pede ao plano (`ReservationService.java:43` e `:84`). O SQL
continua isolado em `ReservationStore`, que ganhou a coluna nova (`ReservationStore.java:44`). A montagem continua
num lugar só: as estratégias são criadas dentro das próprias constantes do enum (`RatePlan.java:4-8`), o que basta
para objetos sem estado. O ciclo `documents` ↔ `reservations` vem da base (`ReservationReceipt` lê `Reservation`,
e `ReservationService` cria `ReservationReceipt`), e o agente não o piorou.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 que já existiam e os 3 novos). Nenhum `switch` ou `if` sobre o plano
ficou fora dela: o `grep` por `RatePlan.`/`ratePlan()` em `src/main` só acha seleção (`.strategy()`), persistência
(`name()`/`valueOf`) e resposta. As três decisões de cada plano (preço, validação e devolução) estão na mesma classe.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É o Strategy que a referência descreve: uma interface (`RatePlanStrategy.java:11`) e uma implementação
por plano, escolhida por um campo do `enum` (`RatePlan.java:4-8`, `:16`). Os dois planos antigos também entraram:
`StandardRatePlan.java:16-26` e `NonRefundableRatePlan.java:18-28`. Os dois `switch` da base foram trocados por
chamadas ao plano: `PriceCalculator.java:18-20` e `ReservationService.java:84`. As validações de cada plano ficaram
no próprio plano, e não em `if` no serviço: o código da empresa em `CorporateRatePlan.java:18-29`, sexta-feira e duas
noites em `WeekendPackageRatePlan.java:20-31`, as 7 noites em `LongStayRatePlan.java:14-20`. O serviço só chama
`validate` (`ReservationService.java:43`). Um plano novo (melhor idade, antecedência de 60 dias) seria uma classe nova
mais uma constante no enum. Nada mais precisaria mudar.

**O exagero.** Não há. Cada classe nova é uma estratégia que a variação justifica. `PriceCalculator.calculate`
(`PriceCalculator.java:18-20`) ficou só repassando a chamada para o plano, mas é o ponto de entrada que já existia,
mantido para os testes da base. Não é estrutura nova.

**O resto do design**, que pesa na nota:
- **Duplicação.** Quatro estratégias repetem o mesmo bloco: subtotal, noites, desconto = taxa × subtotal,
  `new Price(...)`. Está em `StandardRatePlan.java:16-21`, `NonRefundableRatePlan.java:18-23`,
  `CorporateRatePlan.java:32-38` e `LongStayRatePlan.java:23-28`. Só muda a taxa. Um método comum de "desconto
  percentual" tiraria a cópia; do jeito que está, uma mudança na regra de arredondamento precisa ser feita em quatro
  lugares.
- **Interface larga.** `validate` e `calculatePrice` recebem `companyCode` em todos os planos
  (`RatePlanStrategy.java:17` e `:20`), mas só o convênio usa. O próximo plano com dado próprio (a data da reserva,
  para o de 60 dias, ou a idade, para a melhor idade) vai mudar a assinatura dos cinco. Um objeto de contexto da
  reserva evitaria isso.
- **Acoplamento nos dois sentidos.** `PriceCalculator` chama o plano passando a si mesmo (`PriceCalculator.java:19`),
  e o plano chama de volta `calculateSubtotal` e `countNights`.
- **Noites contadas duas vezes.** As noites são contadas com um laço (`PriceCalculator.java:30-36`), e não com
  `ChronoUnit.DAYS.between`, e a contagem acontece duas vezes por reserva: uma no serviço
  (`ReservationService.java:42`) e outra dentro do plano.
- **O que está certo.** O dinheiro é `BigDecimal`, arredondado com o mesmo `cents` (meio para o par) da base, e a
  metade do pacote também passa por ele (`WeekendPackageRatePlan.java:43`). Os erros usam o `HotelException` com
  `INVALID_REQUEST` que já existia. Nenhum estado mutável foi acrescentado.

## Testes

**Números:** 34 testes rodaram, 0 falhas, 0 erros, 0 pulados, e há 34 métodos de teste no código (a base tinha 26,
todos passando). Cobertura de linhas: 529 de 560, 94,5% (base 93,8%). Cobertura de ramos: 111 de 126, 88,1% (base
87,1%). Em `hotel.reservations`: 267 de 280 linhas e 59 de 62 ramos (`measures/tests.md`).
**Nota:** com problemas

Os 26 testes da base continuam lá, com o mesmo nome e as mesmas asserções. O agente só acrescentou `null` como
`companyCode` nas chamadas, por exemplo em `PriceCalculatorTest.java:21-30` e `ReportsTest.java:30-33`. Os 8 testes
novos conferem valores:
- os três exemplos do texto: `PriceCalculatorTest.java:79-87` (686,22), `:90-98` (820,00) e `:101-109` (1.574,37);
- as recusas do convênio sem código, com código vazio e com código desconhecido (`ReservationServiceTest.java:168-178`);
- o pacote começando no sábado e o pacote de três noites (`ReservationServiceTest.java:181-189`);
- a estadia longa com 6 e com 7 noites, que segura a fronteira `<` 7 (`ReservationServiceTest.java:192-198`);
- as três devoluções, 686,22, 410,00 e 1.574,37 (`ReservationServiceTest.java:201-213`);
- uma chamada pela API com o `companyCode`, que confere o 201 e o 400 (`HttpApiTest.java:124-138`).

A pergunta da mutação deixa regras novas sem rede:
- **Agrovale e Farmavida.** Os testes só usam TECNOSUL (`PriceCalculatorTest.java:81`,
  `ReservationServiceTest.java:176`). Trocar 0.18 por 0.81 ou 0.25 por 0.52 em `CorporateRatePlan.java:11-15` não
  derruba nenhum teste.
- **O preço fechado do standard e da suíte.** Só o deluxe é conferido (`PriceCalculatorTest.java:90-98`, apesar do
  nome "PerRoomType"). Errar R$ 520,00 ou R$ 1.200,00 passa.
- **A alta temporada.** Nenhum teste põe os planos novos na temporada. O teste
  `longStayGives10PercentOffEvenInHighSeason` (`PriceCalculatorTest.java:101`) usa 01 a 08/03, que é baixa
  temporada, então o nome promete o que o teste não confere. Se o pacote passasse a somar o acréscimo da temporada,
  ou se a estadia longa deixasse de somar, nenhum teste falharia.

O roteiro do comportamento confere esses três pontos, e eles funcionam; o que falta é a rede dos testes do próprio
projeto.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está em `numeric(10,2)` (6 de 6 colunas), as datas em
`date`, e a chave estrangeira `room_number` tem índice (o do `EXCLUDE`). A única coisa que a conferência acusou foi
`rate_plan`, `status`, `type` e `channel` sem `CHECK`, e isso vem da base. Há 2 migrações Flyway: a `V1` igual à da
base e a `V2` nova (`measures/database.md`).
**Nota:** bom

A mudança no esquema foi feita como manda a regra do projeto: uma migração nova, que só acrescenta uma coluna que
aceita nulo (`src/main/resources/db/migration/V2__add_company_code.sql:2`), sem tocar na `V1`. Assim, um banco que
já roda o sistema sobe sem problema. O código novo de acesso ao banco continua com parâmetros
(`ReservationStore.java:31-33` e `:44`) e lê a coluna de volta ao montar a reserva (`ReservationStore.java:133`).
A falta de `CHECK` em `rate_plan` vem da base e continua sem efeito, porque o enum aceita os cinco valores sem
mudar o esquema. A proteção contra reserva dupla é o `EXCLUDE` da base, que o agente manteve. Um ponto pequeno, que
não muda a nota: o `companyCode` é gravado do jeito que chega mesmo quando o plano não é o convênio
(`ReservationStore.java:44`). O texto diz para ignorar o campo nesses planos, e ele não muda o preço (o roteiro
confere isso), mas fica um texto sem validação numa reserva STANDARD.

## Comportamento

**Números:** 72 de 72 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2 (10 rodadas, sempre uma criada e a outra com 409).
**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- **Convênio.** Tecnosul, Agrovale e Farmavida dão 686,22, 639,44 e 584,85. Sem código, ou com um código que não
  existe, a resposta é 400 `INVALID_REQUEST`. A devolução é integral.
- **Pacote.** Sai R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho. A devolução é de R$ 410,00. A entrada no
  sábado e a estadia de sexta a segunda são recusadas com 400.
- **Estadia longa.** Dá 1.574,37 e continua somando a temporada nas noites de alta (1.754,30 de 28/06 a 05/07).
  Com 6 noites, é recusada. A devolução é integral.
- **O que já existia.** O `companyCode` é ignorado nos outros planos, e o normal e o não reembolsável continuam como
  antes.

Nada falhou.

## Dúvidas

- O código da empresa só é aceito em maiúsculas e sem espaços (`CorporateRatePlan.java:24`): "tecnosul" é
  recusado. O texto diz que "o código é o nome dela em maiúsculas", então a recusa é defensável, mas o site pode
  mandar em minúsculas.
- O `companyCode` é gravado também nos planos que não são o convênio (`ReservationStore.java:44`), e não aparece
  na resposta da API (`hotel/web/ReservationResponse.java:9-25`). É uma regra de plano pequena fora do plano, e
  sozinha não muda a classe.
- O README não foi atualizado (`final/README.md` é igual ao da base): continua descrevendo só STANDARD e
  NON_REFUNDABLE e não fala do `companyCode`. Isso não entra em nenhuma das colunas, mas quem mantiver o sistema
  depois vai ler uma descrição desatualizada.
