# Revisão: manutenção, cenário 01

## Visão geral

O agente manteve a estrutura da base (pacotes `hotel.web` para o HTTP com Javalin, `hotel.reservations` para as regras
e o acesso ao banco em `ReservationStore`, `hotel.rooms`, `hotel.reports`, `hotel.documents`, `hotel.messages`; a
montagem em `hotel.Hotel`) e trocou os dois `switch` de plano da base (desconto em `PriceCalculator.calculate`,
devolução em `ReservationService.cancel`) por uma interface `RatePlanStrategy` com uma classe por plano (os cinco),
escolhidas por um mapa em `RatePlanStrategies`. Uma requisição `POST /reservations` passa por
`HttpApi.readNewReservation` (que agora lê o `companyCode`), por `ReservationService.create` (que pede a validação ao
plano) e por `PriceCalculator.calculate` (que pede o desconto ao plano); os dados ficam no PostgreSQL, com uma migração
nova `V2__add_company_code.sql`. Os testes passam (37, 11 novos), e 71 das 72 conferências do roteiro passam. O
defeito mais sério é o pacote de fim de semana: como a interface só deixa o plano dar um desconto sobre a soma das
diárias, o preço fechado virou um "desconto" que pode ser negativo. No exemplo do texto, a reserva sai com diárias
R$ 779,80, desconto R$ -40,20 e total R$ 820,00, quando o texto pede diárias R$ 820,00 e nenhum desconto
(`final/src/main/java/hotel/reservations/WeekendPackagePlan.java:33-37`). O README não foi atualizado: ainda descreve só
dois planos (`final/README.md:32-38`).

## Arquitetura

**Números:** 7 pacotes e 17 dependências, os mesmos da base (`measures/dependencies.md` e `measures/dependencies-base.md`); um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. `hotel.reservations` passou de 13 para 18 classes (508 → 720 linhas), e nenhuma classe cresceu de forma relevante (`HttpApi` 170 → 171, `ReservationStore` 143 → 145).
**Nota:** bom

A mudança respeita as camadas da base. A borda HTTP só passou a ler um campo a mais e repassá-lo
(`src/main/java/hotel/web/HttpApi.java:128-131`, `src/main/java/hotel/web/CreateReservationRequest.java:16`), sem regra
de plano. As regras novas ficam em classes do pacote `hotel.reservations` que não conhecem nem Javalin nem JDBC
(`CorporatePlan`, `WeekendPackagePlan`, `LongStayPlan`, `StandardPlan`, `NonRefundablePlan`). O SQL continua só em
`ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:31-49`, `:132-133`). O serviço pede a
validação ao plano antes de checar a disponibilidade (`src/main/java/hotel/reservations/ReservationService.java:42-43`)
e a devolução no cancelamento (`:84-85`). O registro de estratégias é um mapa estático imutável
(`src/main/java/hotel/reservations/RatePlanStrategies.java:7-16`). Não é `static` mutável, e num sistema deste tamanho
é aceitável como único ponto de seleção. O agente não criou nenhuma dependência ou ciclo novo. O ciclo
documents↔reservations vem da base.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (os 2 que já existiam e os 3 novos), 0 fora dela. Não sobrou nenhum `switch`/`if` de comportamento sobre o plano em `src/main` (o único `switch` restante é o de `ErrorCode` → status HTTP, `HttpApi.java:101`). A interface tem 3 operações (validar, desconto, devolução).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** É um Strategy. A interface `RatePlanStrategy` (`src/main/java/hotel/reservations/RatePlanStrategy.java:14-24`)
tem uma implementação por plano, e cada plano junta num lugar só dele a validação, o preço e a devolução:
- o convênio, com o mapa das empresas e a recusa sem código válido (`CorporatePlan.java:16-29`);
- o pacote, sexta a domingo em 2 noites e preço por tipo de quarto (`WeekendPackagePlan.java:13-38`);
- a estadia longa, com o mínimo de 7 noites e 10% (`LongStayPlan.java:10-27`).

Os dois planos antigos também viraram classes (`StandardPlan.java`, `NonRefundablePlan.java`). A escolha é um único
ponto, um mapa (`RatePlanStrategies.java:7-16`). O cálculo (`PriceCalculator.java:25-26`) e o cancelamento
(`ReservationService.java:84-85`) pedem ao plano sem nomear nenhum. O próximo plano ("melhor idade", "60 dias de
antecedência") é uma classe nova e uma linha no mapa.

**O que afasta.** A forma da interface. `calculateDiscount` (`RatePlanStrategy.java:17`) só deixa o plano dizer um
desconto sobre um subtotal que o `PriceCalculator` já somou noite a noite. O plano não pode definir o preço. Por isso o
pacote de preço fechado foi encaixado como "desconto = subtotal − preço do pacote" (`WeekendPackagePlan.java:35-37`).
Fora da temporada, esse desconto é negativo (−40,20 no exemplo do texto). Na temporada, é positivo (+115,76 num
deluxe em julho), e o texto pede o contrário: diárias iguais ao preço do pacote e sem desconto. O defeito aparece no
comportamento e no comprovante (ver "Comportamento"). É um defeito de modelagem do método da estratégia, não de
estrutura: nenhum plano ficou fora dela, e nada decide pelo plano. Por isso a classe é acerto, e o defeito pesa na nota.

**Exagero.** Não há: nenhuma fábrica, builder ou camada de repasse. O mapa de estratégias é o ponto de seleção que a
referência aceita.

**O resto do código (a nota).**
- A assinatura `calculateDiscount(subtotal, request, room, checkIn, checkOut)` repete dados: `checkIn` e `checkOut` já
  estão em `request`, e só um plano usa `room` (`RatePlanStrategy.java:17`). O mesmo vale para
  `PriceCalculator.calculate(room, checkIn, checkOut, request)`, que recebe as datas e também o pedido que as contém
  (`PriceCalculator.java:17`). As chamadas nos testes ficaram redundantes (`calculator.calculate(room, request.checkIn(),
  request.checkOut(), request)`, `PriceCalculatorTest.java:22-23`).
- `RatePlanStrategies.strategyFor` usa `Map.get` (`RatePlanStrategies.java:15`). Um plano novo no `enum` sem a linha
  no mapa dá `NullPointerException` em tempo de execução, e não um erro de compilação. Um `EnumMap` com verificação,
  ou a estratégia como campo do `enum`, fecharia isso.
- O `companyCode` é gravado em qualquer plano (`ReservationStore.java:44`), embora o texto diga que fora do convênio
  ele pode ser ignorado. O preço o ignora (conferido no roteiro), mas o dado fica no banco.
- O README ficou desatualizado: só fala de `STANDARD` e `NON_REFUNDABLE`, e o corpo de exemplo não tem `companyCode`
  (`README.md:32-38`, `:70`).

Os tipos estão certos: `BigDecimal` com `cents` (meio para o par) e `LocalDate`/`DayOfWeek`.

## Testes

**Números:** 37 testes rodaram, 0 falhas, 0 erros, 0 pulados; 37 métodos escritos (a base tinha 26, todos mantidos e passando). Linhas: 516 de 549 (94,0%), contra 93,8% na base. Ramos: 108 de 124 (87,1%), igual à base. `hotel.reservations`: 253/268 linhas, 56/60 ramos (`measures/tests.md`, `measures/tests-base.md`).
**Nota:** com problemas

O agente testou cada plano novo com valores e manteve os 26 testes da base passando. Alguns ajustes só trocaram a
assinatura, sem enfraquecer nenhuma asserção (`PriceCalculatorTest.java:20-85`). O que está conferido:
- os três exemplos do financeiro: convênio TECNOSUL 779,80/93,58/686,22 (`PriceCalculatorTest.java:89-99`, e pela API
  em `HttpApiTest.java:124-136`) e estadia longa 1.749,30/174,93/1.574,37 (`PriceCalculatorTest.java:116-125`);
- as devoluções: 686,22, 410,00 e 1.574,37 (`ReservationServiceTest.java:176`, `:202`, `:222`);
- as recusas: convênio sem código ou com código inválido, pelo serviço e pela API com 400 `INVALID_REQUEST`
  (`ReservationServiceTest.java:164`, `HttpApiTest.java:139-157`); pacote com entrada errada ou saída errada
  (`ReservationServiceTest.java:187-199`); estadia longa com 6 noites (`:213`).

O que ficaria sem rede, pela pergunta da mutação:
- **AGROVALE e FARMAVIDA.** Só a TECNOSUL é testada. Trocar 0.18 por 0.81, ou 0.25 por 0.52
  (`CorporatePlan.java:18-19`), não derruba nenhum teste.
- **Os preços de 520,00 e 1.200,00 do pacote.** Só o deluxe é testado. Trocar o standard ou a suíte no mapa
  (`WeekendPackagePlan.java:13-16`) passa.
- **O pacote na alta temporada.** Não há teste.
- **A regra de exatamente 2 noites.** Apagar o `nights != 2` (`WeekendPackagePlan.java:26-28`) não derruba nenhum
  teste. Os casos de `weekendPackageRequiresFridayToSunday` caem antes, no dia da semana, e uma sexta → domingo da
  semana seguinte não é testada.
- **A estadia longa com noites de temporada.** Não há teste.
- **O `companyCode` ignorado nos outros planos.** Não há teste.
- **O preço fechado do pacote.** O teste consagra o defeito: `weekendPackageUsesFixedPrice` espera diárias 779,80
  (`PriceCalculatorTest.java:111`), quando o texto diz que as diárias são o preço do pacote, e não confere o desconto.

Os níveis estão bem misturados: unidade do cálculo, serviço contra o banco e API, como na base.

## Banco

**Números:** 3 tabelas, todas com chave primária; 2 migrações Flyway, `V1__create_tables.sql` igual à da base e `V2__add_company_code.sql` nova; dinheiro em `numeric(10,2)` em todas as 6 colunas; nenhuma chave estrangeira sem índice; nenhuma data em texto; `EXCLUDE` contra reserva dupla mantido; `rate_plan` e `status` em `text` sem `CHECK` (vem da base) (`measures/database.md`).
**Nota:** bom

O agente fez o certo para evoluir o esquema: uma migração nova, sem tocar na V1, que acrescenta uma coluna anulável
(`src/main/resources/db/migration/V2__add_company_code.sql:2`). Um banco que já roda o sistema sobe sem problema. O SQL
continua com parâmetros. Os `+` que o script listou só juntam constantes (`ReservationStore.java:33`, `:84`, `:99`,
`:112`). O `INSERT` e o mapeamento de leitura foram estendidos com cuidado (`ReservationStore.java:31-49`, `:132-133`).
O serviço sobe contra um banco vazio, e os dados continuam depois de reiniciar (3 de 3 no roteiro).

Há duas ressalvas pequenas, que não mudam a nota:
- `company_code` é gravado mesmo nos planos em que não vale (`ReservationStore.java:44`).
- O pacote fora da temporada grava um `discount` negativo (−40,20). Nenhum `CHECK (discount >= 0)` o recusaria, mas a
  base também não tinha esse `CHECK`. O defeito é do cálculo (ver "Design" e "Comportamento").

A falta de `CHECK` nas colunas de plano e situação vem da base.

## Comportamento

**Números:** 71 de 72 conferências passaram. Parte comum: 46 de 46. Dados depois de reiniciar: 3 de 3. Cenário 01: 20 de 21. Duas reservas ao mesmo tempo: 2 de 2 (`measures/behavior.md`).
**Nota:** com problemas

Tudo o que já funcionava continua funcionando: os preços, a temporada, o arredondamento, as recusas, o e-mail, o
check-in e o check-out, as devoluções do normal e do não reembolsável e os dois relatórios. No cenário, passam:
- os três convênios: TECNOSUL 686,22, AGROVALE 639,44 e FARMAVIDA 584,85;
- a recusa sem código ou com código inválido (400 `INVALID_REQUEST`) e a devolução total do convênio;
- o pacote: total 520,00, 820,00 e 1.200,00 (inclusive em julho), devolução da metade (410,00), recusa de sábado a
  segunda e de sexta a segunda;
- a estadia longa: 1.574,37, com temporada 1.754,30, recusa com 6 noites e devolução total;
- o `companyCode` ignorado no plano normal.

A falha: o pacote deluxe de 12 a 14/03/2027 deveria ter diárias 820,00, desconto 0,00 e total 820,00. A reserva veio
com diárias 779,80, desconto −40,20 e total 820,00. O texto diz que "as diárias e o total da reserva são o próprio
preço do pacote, sem desconto". Ele fecha esse ponto, e não há uma leitura razoável que leve a um desconto negativo.
Para o dono da pousada, o valor cobrado está certo (R$ 820,00 entra e R$ 410,00 volta no cancelamento), mas o que a
reserva mostra está errado:
- a resposta da API e o banco têm um "desconto" de −R$ 40,20;
- o comprovante enviado por e-mail mostra "Diárias: R$ 779,80" e "Total: R$ 820,00", sem linha de desconto
  (`final/src/main/java/hotel/documents/ReservationReceipt.java:48-52`, que só mostra desconto positivo). O hóspede
  paga mais do que as diárias que o comprovante lista.
- na alta temporada, o pacote aparece com um "desconto" positivo (115,76 num deluxe em julho) que o texto diz não
  existir.

## Dúvidas

- Dei `acerto` ao pattern, embora o pacote de preço fechado tenha sido encaixado como um desconto
  (`WeekendPackagePlan.java:33-37`). O preço do pacote mora na classe do plano, e nenhum `switch` decide por ele. Mas o
  subtotal continua sendo calculado noite a noite pelo `PriceCalculator`, para todos os planos. Se se entender que o
  plano deveria controlar o preço inteiro, isso aproximaria de `malfeito`. Fiquei com `acerto` porque a referência pede
  "o cálculo pedindo ao plano, sem nomear os planos", e é o que acontece.
- O valor do desconto na alta temporada (115,76) foi calculado pela leitura do código, não por uma conferência do
  roteiro (o roteiro só confere o total em julho, que está certo).
