# Revisão: manutenção, cenário 01

## Visão geral

A base é um serviço Javalin em pacotes por assunto: `hotel.web` (as rotas em `HttpApi`), `hotel.reservations` (o
serviço, o cálculo do preço, o plano de tarifa e o acesso ao banco em `ReservationStore`), `hotel.rooms`,
`hotel.documents`, `hotel.messages` e `hotel.reports`, montados em `hotel.Hotel`. Os dados ficam no PostgreSQL, com o
esquema numa migração Flyway (`V1__create_tables.sql`). Uma reserva entra por `HttpApi` (que monta um
`NewReservation`), passa por `ReservationService.create` (validações, `RatePlan.validate`, capacidade,
disponibilidade, `PriceCalculator.calculate`) e é gravada por `ReservationStore.add`. A mudança do agente é pequena
(cerca de 150 linhas em `src/main`) e bem colocada: o `enum RatePlan` passou a ter, em cada constante, o preço, a
validação e a devolução do plano, os dois `switch` da base sumiram, e entrou o `enum Company` com os descontos do
convênio. Os 35 testes passam, e as 72 conferências do roteiro também. Não achei nada sério. Os pontos menores são
estes: o código da empresa não é guardado na reserva, e há uma consulta repetida da empresa.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), igual na base
(`measures/dependencies-base.md`: as mesmas 17 dependências e o mesmo ciclo). `hotel.reservations` passou de 13 para 16
classes (508 → 651 linhas); a maior classe continua sendo `HttpApi` (171 linhas, 170 na base).
**Nota:** bom

O agente não mexeu na forma do sistema. A regra nova mora inteira em `hotel.reservations`, que não usa Javalin nem
Jackson (tabela de bibliotecas em `measures/dependencies.md`). A borda HTTP ganhou só o campo `companyCode`
(`src/main/java/hotel/web/CreateReservationRequest.java:16`), repassado sem lógica em
`src/main/java/hotel/web/HttpApi.java:128-130`. O serviço chama o plano sem conhecer os planos
(`src/main/java/hotel/reservations/ReservationService.java:36` e `:82`). `RatePlan` passou a depender de
`hotel.rooms` (`Room`, `RoomType`, `src/main/java/hotel/reservations/RatePlan.java:3-4`), uma dependência que o pacote
já tinha. O ciclo `documents` ↔ `reservations` (`ReservationService.java:3` importa `ReservationReceipt`, que importa
`Reservation`) veio da base, e o agente não o piorou. Nenhuma classe nova ficou grande demais: `RatePlan` tem 114
linhas, divididas em cinco constantes curtas.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (as duas constantes da base e as três novas), cada uma com o seu preço, a sua
validação e a sua devolução. Fora dela, 0 casos: nenhum `switch` nem `if` sobre o plano em `PriceCalculator` ou
`ReservationService` (grep de `ratePlan` em `src/main`: só chamadas a `validate`, `price` e `refund`, mais a
persistência por `name()`/`valueOf`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

O pattern aparece como o `enum` usado como estratégia, uma forma que a referência aceita como acerto. `RatePlan`
declara três métodos com comportamento padrão: `validate` não recusa nada, `price` devolve a soma das noites e
`refund` devolve tudo (`src/main/java/hotel/reservations/RatePlan.java:96-108`). Cada constante sobrescreve o que é
dela:
- `NON_REFUNDABLE`: 15% e devolução zero (`RatePlan.java:19-29`);
- `CORPORATE`: valida e aplica o desconto da empresa (`RatePlan.java:32-48`);
- `WEEKEND_PACKAGE`: sexta a domingo, preço fechado por tipo de quarto, metade de volta (`RatePlan.java:51-76`);
- `LONG_STAY`: mínimo de 7 noites, 10% (`RatePlan.java:79-94`);
- `STANDARD` usa os padrões (`RatePlan.java:15`).

Os dois `switch` de comportamento da base saíram: o do desconto em `PriceCalculator.calculate`, que agora monta o preço
das noites e pede ao plano o preço final (`src/main/java/hotel/reservations/PriceCalculator.java:27-28`), e o da
devolução em `ReservationService.cancel` (`ReservationService.java:82`). A validação de cada plano também ficou no
plano, chamada de um ponto só (`ReservationService.java:36`). Um plano novo é uma constante nova, sem mexer em outro
arquivo.

Não há exagero. `Company` (`src/main/java/hotel/reservations/Company.java:7-31`) é um `enum` de domínio com o desconto
de cada empresa e uma busca pelo código. Não há interface sem uso, fábrica nem camada que só repassa. O auxiliar
`withDiscount` (`RatePlan.java:110-113`) evita repetir o cálculo do desconto em três planos.

O resto do código está limpo: dinheiro em `BigDecimal` com `cents` (meio para o par) a cada passo
(`RatePlan.java:74`, `:111`), datas em `LocalDate`, `DayOfWeek` para a sexta (`RatePlan.java:59`), nomes claros. Há
detalhes que não chegam a pedir mudança:
- `CORPORATE` busca a empresa duas vezes, em `validate` e em `price` (`RatePlan.java:35` e `:40`);
- `WEEKEND_PACKAGE.price` usa `prices.get(room.type())` sem tratar um tipo de quarto ausente do mapa
  (`RatePlan.java:68`); hoje os três tipos estão lá;
- as taxas `0.15` e `0.10` são literais dentro dos métodos (`RatePlan.java:22`, `:92`), sem constante com nome;
- o construtor auxiliar de `NewReservation`, com `companyCode` nulo (`src/main/java/hotel/reservations/NewReservation.java:15-18`),
  serve só para não mexer nos chamadores antigos; o `null` é tratado em `Company.fromCode` (`Company.java:23-30`).

## Testes

**Números:** 35 testes rodaram, 0 falhas, 0 erros, 0 pulados; 35 métodos escritos (a base tinha 26, todos mantidos).
Cobertura de linhas de 94,3% (514/545; base 93,8%) e de ramos de 87,7% (107/122; base 87,1%). `hotel.reservations`
tem 251/264 linhas e 55/58 ramos.
**Nota:** bom

O agente escreveu 9 testes novos que conferem as regras novas com os valores do texto:
- convênio Tecnosul: 779,80 de diárias, 93,58 de desconto, 686,22 de total, e devolução de 686,22
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:170-178`);
- Agrovale (140,36) e Farmavida (194,95) (`:181-186`);
- recusa sem código, com código desconhecido e com o código em minúsculas (`:189-193`);
- pacote deluxe a 820,00, com devolução de 410,00 (`:196-205`);
- pacote standard e suíte em julho a 520,00 e 1.200,00 (`:208-213`);
- recusa do pacote começando na quinta, com 3 noites e com 1 noite (`:216-223`);
- estadia longa: 1.749,30 / 174,93 / 1.574,37 e devolução total (`:226-234`);
- estadia longa recusada com 6 noites (`:237-240`);
- pela API, convênio sem código (400 `INVALID_REQUEST`) e com `AGROVALE` (201)
  (`src/test/java/hotel/web/HttpApiTest.java:92-102`).

Os testes da base só mudaram para usar a nova assinatura de `calculate`, por um auxiliar
(`src/test/java/hotel/reservations/PriceCalculatorTest.java:19-22`), sem perder asserções.

Na pergunta da mutação: trocar 0.12, 0.18 ou 0.25, o 0.5 da devolução do pacote, o mínimo de 7 noites (6 recusa, 7
aceita) ou o dia da semana faz algum teste falhar. Ficam sem rede só dois pontos menores. Nenhum teste da estadia longa
passa por noites de alta temporada, mas a temporada vem do cálculo comum, que já é testado. O pacote não é testado pela
API.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária; dinheiro em `numeric(10,2)`;
chave estrangeira com índice; nenhuma data em texto. As conferências acusaram só as colunas de tipo e situação sem
`CHECK` (`reservations.rate_plan`, `reservations.status`, `rooms.type`, `messages.channel`), que já eram assim na
base. A migração `V1__create_tables.sql` está igual à da base, e nenhuma foi acrescentada.
**Nota:** bom

A mudança não precisou mexer no esquema. O plano é gravado pelo nome em `rate_plan text`
(`src/main/java/hotel/reservations/ReservationStore.java:43`), sem `CHECK` que limite os valores (desde a base,
`src/main/resources/db/migration/V1__create_tables.sql:28`), então os três planos novos gravam e leem sem migração.
A migração da base não foi editada. A proteção contra a reserva dupla continua na restrição `EXCLUDE`
(`V1__create_tables.sql:39-41`), e o SQL continua com parâmetros (os `+` listados pelo script só juntam constantes,
`ReservationStore.java:31-33`). O único ponto é uma escolha: o `companyCode` não é guardado na reserva (o `INSERT` de
`ReservationStore.java:31-33` não tem a coluna). O desconto fica gravado, mas não a empresa que o justificou. O texto
não pediu esse registro (veja "Dúvidas").

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.
**Nota:** bom

Os valores que o financeiro passou batem pela API:
- convênio Tecnosul: R$ 779,80 / 93,58 / 686,22;
- pacote deluxe: R$ 820,00, com devolução de R$ 410,00;
- estadia longa: R$ 1.749,30 / 174,93 / 1.574,37.

Também passaram:
- os descontos da Agrovale e da Farmavida;
- as recusas com 400 `INVALID_REQUEST` (convênio sem código ou com código desconhecido, pacote de sábado a segunda ou
  de sexta a segunda, estadia longa de 6 noites);
- o pacote em julho com o mesmo preço e a estadia longa com noites de temporada (R$ 1.754,30);
- o `companyCode` ignorado no STANDARD;
- o não reembolsável com os 15% da base.

A parte comum e a reserva simultânea seguem como antes. Para o dono da pousada: os três planos funcionam como ele
descreveu, e nada do que já funcionava quebrou.

## Dúvidas

O código da empresa do convênio não fica guardado na reserva (`ReservationStore.java:31-33`), e nem o comprovante nem
o relatório mostram de qual empresa veio o desconto. O texto não pede isso, mas o financeiro pode querer saber. Não
mudei nenhuma nota por isso.
