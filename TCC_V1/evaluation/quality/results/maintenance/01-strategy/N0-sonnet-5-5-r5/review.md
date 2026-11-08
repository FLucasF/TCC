# Revisão: manutenção, cenário 01

## Visão geral

O sistema da base já era separado em pacotes: `hotel.web` (Javalin: `HttpApi`, os records de pedido e resposta),
`hotel.reservations` (as regras: `ReservationService`, `PriceCalculator`, `RatePlan`, e o acesso a dados em
`ReservationStore`), `hotel.rooms`, `hotel.documents`, `hotel.messages`, `hotel.reports`, e `hotel` (a montagem). Uma
criação de reserva entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:110-130`), vira um `NewReservation`, passa
por `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27-49`), que pede o preço
ao `PriceCalculator` e grava via `ReservationStore` (JDBC com `?`) no PostgreSQL, com o esquema criado pelo Flyway. O
agente pôs os três planos novos como constantes do `enum RatePlan`, cada uma com o seu corpo (validação, preço e
devolução), e tirou os dois `switch` que a base tinha, inclusive os dos planos antigos. O `companyCode` vai do JSON
até uma coluna nova, criada numa migração nova (`V2__company_code.sql`). O roteiro de comportamento passou inteiro
(72 de 72) e os 30 testes passam. Não há nada sério. O que há são detalhes de design: a validação do plano roda duas
vezes, e o `companyCode` entrou na assinatura de todos os planos. Os testes novos ficaram só no nível de unidade.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). Na base são
os mesmos 7 pacotes, as mesmas 17 dependências e o mesmo ciclo (`measures/dependencies-base.md`). `hotel.reservations`
foi de 13 para 15 classes e de 508 para 638 linhas. A maior classe continua sendo `HttpApi` (171 linhas).
**Nota:** bom

O agente não mudou a forma do sistema. A borda HTTP continua fina: só lê o campo novo e o repassa
(`src/main/java/hotel/web/HttpApi.java:128-130`, `src/main/java/hotel/web/CreateReservationRequest.java:16`,
`src/main/java/hotel/web/ReservationResponse.java:19,39`). As regras novas ficaram em `hotel.reservations`, sem
Javalin e sem SQL (`src/main/java/hotel/reservations/RatePlan.java`). A única dependência nova de `RatePlan` é
`hotel.rooms.RoomType` (`RatePlan.java:4`), para o preço fechado do pacote, e o pacote já dependia de `hotel.rooms`. O
SQL novo ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,31-33,44,133`). O
ciclo `documents` ↔ `reservations` veio da base: `ReservationService.java:3` importa `ReservationReceipt`, que importa
`Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O agente não o criou nem o piorou.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (as 4 constantes com corpo próprio e o `STANDARD` com o comportamento padrão
do enum). Sobraram 0 `switch` ou `if` de comportamento sobre o plano no cálculo e no cancelamento (a base tinha 2
`switch`). Há 1 regra de plano fora do enum, que não é de preço nem de devolução (`NewReservation.java:16`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O `enum RatePlan` funciona como estratégia, e cada constante tem o seu corpo. O `NON_REFUNDABLE`
sobrescreve o preço e a devolução (`src/main/java/hotel/reservations/RatePlan.java:20-30`). O `CORPORATE` traz a
tabela de empresas, a validação e o preço (`RatePlan.java:33-50`). O `WEEKEND_PACKAGE` tem a validação de sexta a
domingo, o preço fechado por tipo de quarto e a devolução de metade (`RatePlan.java:53-76`). O `LONG_STAY` tem o
mínimo de 7 noites e os 10% (`RatePlan.java:79-93`). O padrão fica em `RatePlan.java:95-107`: o `STANDARD` valida
tudo, não dá desconto e devolve tudo. Os dois `switch` da base saíram. O cálculo agora só pede ao plano
(`src/main/java/hotel/reservations/PriceCalculator.java:24,31`) e o cancelamento também
(`src/main/java/hotel/reservations/ReservationService.java:83`), sem nomear nenhum plano. A validação também mora no
plano e o serviço só a chama (`ReservationService.java:34`). Um plano novo é uma constante nova. A referência
considera isso acerto. A única regra de plano fora do enum é `NewReservation.java:16`, que descarta o `companyCode`
quando o plano não é `CORPORATE`. A referência diz que esse caso não muda a classe; ele está nas dúvidas.

**O exagero.** Não há estrutura sem necessidade: nada de interface, fábrica ou registro a mais. Os auxiliares
`discounted` e `invalid` (`RatePlan.java:109-116`) evitam repetição de verdade.

**O resto do design.** Os pontos que um revisor exigente pediria para mudar:
- A validação do plano roda duas vezes em cada criação, em `ReservationService.java:34` e de novo em
  `PriceCalculator.java:24`. A regra não está duplicada, mas a chamada repetida torna confuso quem é o dono da
  validação.
- O `companyCode`, que só o convênio usa, entrou na assinatura de `validate` e de `price` de todos os planos
  (`RatePlan.java:96,100`). Também entrou nas duas sobrecargas do cálculo (`PriceCalculator.java:18-24`). Os
  parâmetros `room` e `companyCode` ficam sem uso na maioria das constantes. O texto já anuncia planos para a melhor
  idade e para a antecedência de 60 dias, e cada um vai precisar de outro dado (a idade, a data do pedido). Do jeito
  que está, cada plano novo muda a assinatura dos cinco. Passar um objeto de contexto (por exemplo, o próprio
  `NewReservation`) evitaria isso.
- A constante nomeada `NON_REFUNDABLE_DISCOUNT` da base virou um `new BigDecimal("0.15")` dentro do método
  (`RatePlan.java:23`). O mesmo acontece com `0.10` e `0.5` (`RatePlan.java:91,74`).
- Há uma linha de 136 colunas em `ReservationService.java:47`.

O resto está bem: `BigDecimal` com o arredondamento da base (`PriceCalculator.cents`), `Map.of` imutável nas tabelas,
o erro com o código pedido (`RatePlan.java:114-115`), e o construtor antigo de `NewReservation` mantido para não
quebrar quem já o chamava (`NewReservation.java:9-12`). São defeitos pequenos e localizados.

## Testes

**Números:** 30 testes rodaram, 0 falhas, 0 erros, 0 pulados; 30 métodos escritos (a base tinha 26 e todos
continuam). Cobertura de linhas de 94,3% (510 de 541; na base, 93,8%) e de ramos de 87,1% (108 de 124; na base,
87,1%). Em `hotel.reservations`, 246 de 259 linhas e 56 de 60 ramos.
**Nota:** com problemas

O agente acrescentou 4 testes, todos em `src/test/java/hotel/reservations/PriceCalculatorTest.java`, e conferem com
os valores do texto:
- o convênio da Tecnosul com 779,80, 93,58 e 686,22 (`PriceCalculatorTest.java:80-87`);
- a recusa sem código, com código vazio e com código desconhecido (`:89-95`);
- o pacote deluxe a 820,00 com desconto 0,00, a devolução de 410,00 e as recusas de quinta a sábado e de três noites
  (`:97-108`);
- a estadia longa com 1749,30, 174,93 e 1574,37, e a recusa de 6 noites, que confere a fronteira das 7 noites
  (`:110-119`).

Nenhum teste da base foi apagado ou enfraquecido.

O que ficou sem rede:
- A pergunta da mutação: trocar o 0.18 da Agrovale ou o 0.25 da Farmavida (`RatePlan.java:36-37`) não derruba nenhum
  teste. O mesmo vale para o preço do pacote no standard e na suíte (`RatePlan.java:55,57`).
- Também não há teste para a estadia longa com noites de temporada, nem para o pacote na alta temporada.
- A devolução integral do convênio e da estadia longa (o padrão de `RatePlan.java:105-107`) só é conferida
  indiretamente.
- Nada do que é novo passa pelo serviço, pelo banco ou pela API. Nenhum teste grava e relê o `company_code`
  (`ReservationStore.java:44,133`), confere o 400 `INVALID_REQUEST` pela API, cancela uma reserva dos planos novos
  pelo `ReservationService`, nem confere que o `companyCode` é ignorado fora do convênio (`NewReservation.java:16`).

O comportamento passou no roteiro externo, mas os testes do projeto não protegem essas partes.

## Banco

**Números:** 3 tabelas, todas com chave primária. Todo o dinheiro está em `numeric(10,2)`, nenhuma chave estrangeira
está sem índice e nenhuma data está em texto. Há 2 migrações do Flyway: `V1` igual à da base e `V2__company_code.sql`
nova. As colunas `rate_plan`, `status`, `type` e `channel` são `text` sem CHECK, e isso veio da base. A restrição
`EXCLUDE` contra a dupla reserva continua lá.
**Nota:** bom

O agente fez a mudança do jeito certo. Criou uma migração nova, sem editar a que já existia
(`src/main/resources/db/migration/V2__company_code.sql:2`, `ALTER TABLE reservations ADD COLUMN company_code text`),
então quem já roda o sistema sobe sem perder dados. A coluna aceita nulo, o que faz sentido porque só o convênio a
usa. O SQL novo usa parâmetros (`src/main/java/hotel/reservations/ReservationStore.java:31-33,44`) e a leitura
devolve o código (`ReservationStore.java:133`). Os trechos com `+` que o script listou só concatenam constantes
(`COLUMNS`). `rate_plan` como `text` sem CHECK vem de `V1__create_tables.sql:28`, da base. O agente poderia ter
aproveitado para exigir `company_code` quando o plano é `CORPORATE`, mas o texto não pede isso e a regra está no
código.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o cenário 01: 21 de 21;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Tudo o que o dono pediu funciona pela API:
- o convênio dá 12%, 18% e 25% (686,22 para a Tecnosul, como o financeiro conferiu) e recusa com 400
  `INVALID_REQUEST` quando falta o código ou o código não existe;
- o pacote custa 520,00, 820,00 ou 1.200,00 o ano inteiro, inclusive em julho, devolve 410,00 no deluxe cancelado e
  só aceita de sexta ao domingo seguinte;
- a estadia longa dá 1.574,37 no exemplo, vale a temporada nas noites de alta e recusa 6 noites;
- o convênio e a estadia longa devolvem tudo;
- o `companyCode` num plano normal é ignorado.

O normal e o não reembolsável continuam como antes, os dados sobrevivem ao reinício e o quarto não é reservado duas
vezes.

## Dúvidas

- A nota de design ficou entre `bom` e `com problemas`. Escolhi `com problemas` por dois pontos que um revisor
  exigente pediria para ajustar: a validação chamada duas vezes (`ReservationService.java:34` e
  `PriceCalculator.java:24`) e o `companyCode` na assinatura de todos os planos (`RatePlan.java:96,100`), que o
  próximo plano anunciado vai obrigar a mudar. Nenhum dos dois causa erro hoje.
- A regra de descartar o `companyCode` fora do convênio está em `NewReservation.java:16`, fora do `RatePlan`. É uma
  regra pequena, que não é de preço nem de devolução, e pela referência não muda a classe `acerto`. Ainda assim, ela
  nomeia um plano fora da estrutura.
- O texto não diz se o código da empresa deve aceitar minúsculas ("tecnosul"). O agente aceita só maiúsculas
  (`RatePlan.java:41`). É uma leitura razoável de "o código é o nome dela em maiúsculas", e o roteiro não confere esse
  caso.
