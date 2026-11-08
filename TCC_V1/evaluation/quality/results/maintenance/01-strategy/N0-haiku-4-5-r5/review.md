# Revisão: manutenção, cenário 01

## Visão geral

O sistema da base continua com a mesma forma: `hotel.web` (Javalin: `HttpApi`, os records de requisição e resposta) chama `hotel.reservations` (o `ReservationService` com as regras, o `PriceCalculator` com o preço e o `ReservationStore` com o JDBC). O esquema fica no PostgreSQL e é criado por migrações Flyway. O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` `RatePlan` e fez crescer, plano a plano, os `switch` que a base já tinha. No preço, `PriceCalculator.calculate` virou um `switch` com um método por plano (`PriceCalculator.java:29-35`). No cancelamento, a devolução fica num `switch` com cinco casos (`ReservationService.java:135-141`). Ele também criou um terceiro `switch`, o de validação, dentro do serviço (`ReservationService.java:55-62`). O campo `companyCode` vai da API até uma coluna nova (`V2__add_company_code.sql`). Tudo funciona: os 72 testes do roteiro passam, os 40 testes do projeto passam e o banco evoluiu por uma migração nova. O ponto mais sério é de design: o texto avisa que "o comercial vive inventando plano", mas o comportamento de cada plano ficou espalhado em três `switch` e duas classes. A falta do Strategy (omissão) obriga o próximo plano a mexer em três lugares. Há também duplicação dentro do código novo.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais à base (`measures/dependencies-base.md`). Há um ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O pacote `hotel.reservations` passou de 508 para 663 linhas. A maior classe é agora `ReservationService` (194 linhas, eram 138), e `PriceCalculator` foi de 53 para 132 linhas.
**Nota:** bom

As camadas da base ficaram como estavam. A borda HTTP só ganhou o repasse do campo novo (`web/HttpApi.java:129`, `web/CreateReservationRequest.java:16`, `web/ReservationResponse.java:19,39`), sem regra de plano. As regras novas ficaram em `hotel.reservations`, sem Javalin nem SQL: a validação está em `reservations/ReservationService.java:55-101` e o preço em `reservations/PriceCalculator.java:28-102`. O SQL continua só no `ReservationStore`, que agora grava e lê `company_code` (`reservations/ReservationStore.java:44`, `:132`). A montagem não mudou. O agente não criou nenhuma dependência nova nem nenhum ciclo novo; o ciclo `documents` ↔ `reservations` vem da base. O único sinal de alerta é o tamanho: `ReservationService` cresceu 56 linhas, quase todas de validação por plano, que pertence ao plano e não ao serviço. Isso é um problema de design (próxima seção), não de camada.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem o próprio comportamento num lugar só dele. O comportamento dos cinco é decidido por três `switch` sobre `RatePlan`: o preço em `PriceCalculator.java:29-35`, a validação em `ReservationService.java:56-61` e a devolução em `ReservationService.java:135-141`. Fora dos `switch` há ainda um quarto ponto: a lista das empresas aparece duas vezes, em `ReservationService.java:71-73` e em `PriceCalculator.java:71-81`.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Strategy):** o pedido descreve cinco planos, cada um com preço, regra de uso e devolução próprios, e avisa que outros virão ("melhor idade", "60 dias de antecedência"). O agente não criou nenhuma estrutura por plano: `RatePlan` continua um `enum` sem comportamento (`RatePlan.java:3-14`). Cada plano novo entrou como mais um `case` nos dois `switch` que a base já tinha (`PriceCalculator.java:29-35`, `ReservationService.java:135-141`) e num terceiro `switch`, novo, de validação (`ReservationService.java:55-62`), com as regras de sexta a domingo, de 7 noites e do código da empresa em métodos privados do serviço (`ReservationService.java:64-93`). A referência do pattern chama de omissão exatamente isto: "os `switch` que já existiam crescendo para os cinco casos". O próximo plano vai exigir mexer em três `switch`, em duas classes, e talvez nas listas de empresas. Nada aproxima isto do Strategy: os métodos `calculateX` separados (`PriceCalculator.java:38-102`) continuam escolhidos por um `switch` de comportamento e não carregam a validação nem a devolução.

**O exagero:** nenhum. Não há interface, fábrica nem camada sem uso; o agente fez o mínimo de estrutura (contenção).

**O resto do código:**
- **A mesma conta copiada.** O laço que soma as diárias está copiado quatro vezes (`PriceCalculator.java:39-44`, `:49-54`, `:60-65`, `:94-99`); a base tinha um só. O laço que conta as noites também está duplicado, em `PriceCalculator.java:112-118` e em `ReservationService.java:95-101`, quando bastaria `ChronoUnit.DAYS.between` ou um método comum. Com as quatro cópias, uma mudança na temporada ou no arredondamento precisa ser feita em quatro lugares.
- **A mesma regra em dois lugares.** As empresas do convênio estão listadas em `ReservationService.java:72` (para validar) e de novo em `PriceCalculator.java:75-80` (para o desconto). Se uma empresa for acrescentada só no cálculo, ela será recusada. Se for acrescentada só na validação, o cálculo cai no `default -> BigDecimal.ZERO` (`PriceCalculator.java:79`, e `:72-73` para `null`) e a reserva sai sem desconto, sem nenhum erro. Esse padrão de devolver zero em silêncio esconde o erro em vez de falhar.
- **Uma regra sem constante.** A metade devolvida pelo pacote é um número solto, `new BigDecimal("0.50")` (`ReservationService.java:139`), enquanto todas as outras taxas são constantes nomeadas (`PriceCalculator.java:18-26`).
- **Imports sem uso.** `RoomType` e `DayOfWeek` são importados e não usados (`PriceCalculator.java:4`, `:7`).
- **O que está certo.** O dinheiro continua em `BigDecimal`, arredondado por `cents` (meio para o par), e as datas em `LocalDate`.

São defeitos localizados, mas aparecem em quase todo o código novo. Por isso a nota é `com problemas`.

## Testes

**Números:** 40 testes rodaram, com 0 falhas, 0 erros e 0 pulados, em 40 métodos escritos; a base tinha 26. A cobertura foi de 94,2% das linhas (555 de 589) e 88,3% dos ramos (144 de 163), contra 93,8% e 87,1% na base. Em `hotel.reservations` são 292 de 308 linhas e 92 de 99 ramos.
**Nota:** bom

O agente manteve os 26 testes da base. Mudou só a chamada, que ganhou o argumento `null` (`ReportsTest.java:33`, `ReservationServiceTest.java:36`, `PriceCalculatorTest.java:24` e outras), e não enfraqueceu nenhuma asserção. Ele escreveu 14 testes novos, todos com valores:
- **Preço:** as três empresas (`PriceCalculatorTest.java:80`, `:91`, `:102`), com o exemplo do financeiro de R$ 686,22; o pacote nos três quartos (`:113`, `:125`, `:134`); a estadia longa com o exemplo de R$ 1.574,37 (`:143`) e com noites de alta temporada (`:155`).
- **Recusas e devoluções:** convênio com código inválido e sem código (`ReservationServiceTest.java:164`); pacote fora de sexta a domingo e com 1 noite (`:198`); estadia longa com 6 noites (`:237`); devolução integral no convênio e na estadia longa (`:185`, `:253`); metade no pacote, R$ 410,00 (`:224`).

**A pergunta da mutação:**
- Trocar 0.12 por 0.21 quebra `PriceCalculatorTest.java:80`.
- Trocar `nights < 7` por `nights <= 7` quebra `ReservationServiceTest.java:237`, porque o caso de 7 noites tem de ser aceito.
- Trocar `"0.50"` por outro valor quebra `ReservationServiceTest.java:224`.

**O que ficou sem rede:**
- Nenhum teste chama a API com o campo novo `companyCode` nem confere o 400 pela HTTP: `HttpApiTest` não mudou.
- O pacote só é testado em março. Se alguém aplicasse a alta temporada ao pacote, nenhum teste do projeto falharia; o roteiro de comportamento confere isso.
- Nenhum teste confere que o `companyCode` é ignorado nos outros planos.

São lacunas pequenas perto do que está conferido.

## Banco

**Números:** 3 tabelas, todas com chave primária. Há 2 migrações Flyway: `V1__create_tables.sql` está igual à base e `V2__add_company_code.sql` é nova. O dinheiro está todo em `numeric(10,2)`, as datas em `date`, e não há chave estrangeira sem índice. As colunas `rate_plan`, `status`, `type` e `channel` são `text` sem CHECK; isso vem da base. Não há transação explícita.
**Nota:** bom

O agente fez a mudança de esquema do jeito certo: uma migração nova, sem tocar na que já existia (`src/main/resources/db/migration/V2__add_company_code.sql:1`). Com isso, um banco que já roda o sistema sobe sem erro, e as reservas antigas ficam com `company_code` nulo. O SQL novo continua com parâmetros (`reservations/ReservationStore.java:32-33`, `:44`) e a leitura usa `row.getString("company_code")` (`:132`). A dupla reserva continua barrada pelo `EXCLUDE` da base, e o roteiro confirmou isso nas reservas simultâneas.

Dois pontos pequenos, que não mudam a nota:
- O código da empresa é gravado em qualquer plano (`ReservationStore.java:44`, sem olhar o plano). Uma reserva `STANDARD` que mandou um `companyCode` guarda e devolve esse texto, embora o pedido diga para ignorá-lo nos outros planos. O preço sai certo.
- Não há CHECK ligando `company_code` ao plano. Num esquema que já não tinha CHECK nos planos, não é de cobrar.

## Comportamento

**Números:** 72 de 72 conferências passaram. Na parte comum, 46 de 46. Depois de reiniciar, 3 de 3. No cenário 01, 21 de 21. Nas reservas simultâneas, 2 de 2.
**Nota:** bom

Os três exemplos do financeiro saem pela API como o texto dá:
- Tecnosul no deluxe: R$ 779,80 de diárias, R$ 93,58 de desconto e R$ 686,22 de total.
- Pacote deluxe: R$ 820,00, com devolução de R$ 410,00.
- Estadia longa: R$ 1.749,30 de diárias, R$ 174,93 de desconto e R$ 1.574,37 de total.

As regras de uso também passaram:
- Agrovale (18%) e Farmavida (25%) dão o desconto certo.
- O convênio sem código, ou com código fora da lista, é recusado com 400 `INVALID_REQUEST`.
- O pacote vale R$ 520,00, R$ 820,00 e R$ 1.200,00, inclusive em julho, e é recusado fora de sexta a domingo.
- A estadia longa conta a temporada e recusa 6 noites.
- O convênio e a estadia longa devolvem tudo.

O `STANDARD` com `companyCode` ignora o código no preço, e o `NON_REFUNDABLE` continua com 15%. Para o dono da pousada: os planos novos funcionam como ele pediu, e nada do que já funcionava quebrou.

## Dúvidas

- Guardar o `companyCode` também nos planos que não são o convênio (`ReservationStore.java:44`; ele também aparece em `ReservationResponse.java:39`) é uma leitura possível de "pode ignorar": o preço ignora o código, mas o registro não. Não pesou em nenhuma nota.
- Na nota de Testes, fiquei entre `bom` e `com problemas`, porque falta um teste pela API do campo novo. Dei `bom` porque todas as regras novas estão conferidas com valores no nível do serviço e do cálculo.
