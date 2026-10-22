# Revisão: manutenção, cenário 01

## Visão geral

O agente manteve a forma da base: `hotel.web.HttpApi` lê o JSON e monta um `NewReservation`, o
`hotel.reservations.ReservationService` aplica as regras e chama o `PriceCalculator`, e o `ReservationStore` grava no
PostgreSQL por JDBC, com o esquema criado pelo Flyway. A mudança é pequena e funciona. Os três planos novos entram no
`enum` (`src/main/java/hotel/reservations/RatePlan.java:7-13`). O preço e as regras de quem pode usar cada plano ficaram
num `switch` maior em `PriceCalculator.calculate` (`src/main/java/hotel/reservations/PriceCalculator.java:44-85`), e a
devolução, no `switch` que já existia em `ReservationService.cancel` (`src/main/java/hotel/reservations/ReservationService.java:83-87`).
O `companyCode` passa da API até uma coluna nova, criada por uma migração nova (`V2__add_company_code.sql`). Todos os
testes passam (45) e todas as 72 conferências de comportamento também. O mais sério é o design: o texto avisa que
o comercial vive inventando planos, e mesmo assim cada plano novo continua espalhado em dois `switch`. O `switch` do
preço ainda trocou a forma exaustiva por uma com `default`, que esconde do compilador um plano esquecido.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base. Há um ciclo (`hotel.documents` ↔
`hotel.reservations`), que já existia na base. O que mudou: `hotel.reservations` passou de 508 para 583 linhas e
`hotel.web` de 236 para 238. A maior classe continua sendo `HttpApi`, com 171 linhas.
**Nota:** bom

O agente não mexeu na estrutura, e as mudanças ficaram cada uma na camada certa:
- **HTTP:** a borda só ganhou o campo `companyCode` (`src/main/java/hotel/web/CreateReservationRequest.java:16`) e o repassa
  (`src/main/java/hotel/web/HttpApi.java:128-130`), sem regra nova.
- **Regras:** as regras dos planos ficaram em `hotel.reservations`, sem Javalin nem SQL (`PriceCalculator.java:44-85`,
  `ReservationService.java:83-87`).
- **Dados:** o SQL novo ficou no `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:19,32,44,133`).
- **Montagem:** continua num lugar só (`src/main/java/hotel/Hotel.java:21-27`).

O ciclo `documents` ↔ `reservations` vem da base: `ReservationService` usa `ReservationReceipt`, e
`ReservationReceipt.java:3` importa `Reservation`. O agente não o piorou.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem um lugar só seu. O comportamento de cada plano fica em 2 `switch` sobre o
`RatePlan`: o do preço e da validação (`PriceCalculator.java:44-85`, 5 casos mais um `default`) e o da devolução
(`ReservationService.java:83-87`, 5 casos). Não há nenhuma classe nova, nem interface.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O texto pede o Strategy sem dizer o nome: cada plano tem o seu cálculo, a sua regra de quem pode usar e a
sua devolução, e "o comercial vive inventando plano". O agente fez exatamente o que a referência chama de omissão:
os dois `switch` da base cresceram para os cinco casos.
- O preço, a validação do código da empresa (`PriceCalculator.java:54-57`), a de sexta a domingo (`:63-68`) e a das
  7 noites (`:75-78`) estão todos em `case` dentro de `calculate`.
- A devolução está em outro `switch`, em outro arquivo (`ReservationService.java:84-86`).

O plano de 60 dias ou o da melhor idade vai exigir mexer em `RatePlan`, `PriceCalculator` e `ReservationService` de
novo. O `enum` continua sem comportamento (`RatePlan.java:3-14`). Também não há exagero: nenhuma interface, fábrica ou
camada sem uso.

**O resto do código.** Os tipos estão certos (`BigDecimal` com `cents` meio-para-o-par, `LocalDate`, `DayOfWeek`, mapas
imutáveis com `Map.of` em `PriceCalculator.java:23-31`). Mas há defeitos localizados que um revisor pediria para
corrigir:
- **O `default` do preço.** O `switch` de expressão exaustivo da base virou um `switch` de comandos com `break` e um
  `default` que cobra o preço cheio (`PriceCalculator.java:82-84`). Um plano novo esquecido ali passa a compilar e é
  cobrado sem desconto e sem validação, em silêncio. O `switch` do `cancel` continuou exaustivo, então os dois
  lugares nem se protegem do mesmo jeito.
- **O método `calculate`.** Passou de 15 para 55 linhas (`PriceCalculator.java:33-88`) e mistura o cálculo com a
  validação de quem pode usar cada plano (lança `HotelException` em três pontos). No pacote, ele ainda calcula as
  diárias noite a noite (`:36-39`) e depois joga o resultado fora, trocando-o pelo preço fechado (`:70`).
- **A metade do pacote.** É um número mágico solto no serviço (`new BigDecimal("0.50")`, `ReservationService.java:86`),
  enquanto os outros percentuais são constantes nomeadas no calculador.
- **Um campo sem uso.** O `companyCode` é gravado em `Reservation` (`src/main/java/hotel/reservations/Reservation.java:15,39`),
  mas não tem getter e ninguém o lê: é estado sem uso.
- **O código é texto solto.** O código da empresa é um `String` que circula por quatro tipos, e a lista das empresas
  é um mapa dentro do calculador. Isso é aceitável neste tamanho.

## Testes

**Números:** 45 testes rodaram, com 0 falhas e 0 pulados (a base tinha 26, todos mantidos). São 45 métodos escritos,
19 deles novos. A cobertura é de 93,9% das linhas (506 de 539) e 87,2% dos ramos (116 de 133); na base, era de 93,8%
e 87,1%. Em `hotel.reservations`, a cobertura é de 243 de 258 linhas e 64 de 69 ramos.
**Nota:** bom

O agente testou a parte nova com os valores do texto, em três níveis.
- **Unidade, no calculador.** Tecnosul com 779,80, 93,58 e 686,22
  (`src/test/java/hotel/reservations/PriceCalculatorTest.java:79-89`). Agrovale e Farmavida (`:91-105`). Recusa sem
  código e com código inválido (`:107-118`). O pacote a 820,00 e nos três tipos de quarto (`:120-146`). Recusa de
  quarta a sexta, de sexta a segunda e de sexta a sábado (`:148-167`). A estadia longa com 1.749,30, 174,93 e
  1.574,37 (`:169-178`), com temporada (`:180-193`) e a recusa com 6 noites (`:195-200`).
- **Serviço, contra o banco.** Convênio e estadia longa devolvem tudo; o pacote devolve 410,00 e retém 410,00
  (`src/test/java/hotel/reservations/ReservationServiceTest.java:169-198`).
- **Pela API.** As respostas 400 com `INVALID_REQUEST` e os totais (`src/test/java/hotel/web/HttpApiTest.java:124-204`).

**A pergunta da mutação.**
- Trocar 0,12 por 0,21, `nights < 7` por `nights <= 7` ou a metade do pacote por outro valor faz algum teste falhar.
- Os testes de Agrovale, Farmavida e da estadia longa na temporada calculam o esperado com a mesma fórmula do código
  (`PriceCalculatorTest.java:96`, `:189-192`), em vez de um valor escrito. Isso pega uma troca de percentual, mas não
  um erro de fórmula.
- Ficam sem rede: o pacote na alta temporada (todos os testes usam março, então nenhum teste prova que o preço
  fechado ignora a temporada) e o `companyCode` ignorado nos outros planos.

São lacunas pequenas. Nenhum teste da base foi apagado nem enfraquecido: só ganharam o argumento novo, `null`.

## Banco

**Números:** 3 tabelas, todas com chave primária. Dinheiro em `numeric(10,2)` em todas as colunas. Nenhuma chave
estrangeira sem índice e nenhuma data em texto. Há um `EXCLUDE` que impede duas reservas do mesmo quarto na mesma
noite. A migração V1 está igual à da base e a V2 é nova. A única coisa que as conferências acusaram foram as colunas
`rate_plan`, `status`, `type` e `channel` em `text` sem `CHECK`, e isso já vinha da base.
**Nota:** bom

O agente fez a mudança do jeito certo: uma migração nova, sem editar a V1
(`src/main/resources/db/migration/V2__add_company_code.sql:2`). A coluna é anulável, o que serve para os planos que
não usam o código. O SQL continua com parâmetros (`ReservationStore.java:31-50`) e lê a coluna nova na volta
(`:133`). Os candidatos a SQL montado com `+` que o script listou só concatenam a constante `COLUMNS`.

Há uma ressalva pequena: o código é gravado como veio também nos planos que não são de convênio
(`ReservationStore.java:44`). O texto manda ignorá-lo nesses planos, e assim uma reserva `STANDARD` pode ficar com um
`company_code` qualquer no banco. Não afeta preço nem devolução.

## Comportamento

**Números:** 72 de 72 conferências passaram:

| parte | passaram |
|---|---|
| a parte comum | 46 de 46 |
| os dados depois de reiniciar | 3 de 3 |
| o cenário 01 | 21 de 21 |
| as reservas simultâneas | 2 de 2 |

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API, com os valores que o financeiro passou:
- Tecnosul: 779,80 de diárias, 93,58 de desconto e 686,22 de total.
- Agrovale e Farmavida, com os seus percentuais.
- O convênio sem código ou com código errado é recusado com 400 `INVALID_REQUEST`.
- O pacote sai a 520,00, 820,00 e 1.200,00, inclusive em julho, e recusa outras datas. Pago e cancelado, devolve
  410,00.
- A estadia longa dá 1.574,37, aplica a temporada e recusa 6 noites.
- Convênio e estadia longa devolvem tudo.
- O `companyCode` é ignorado no plano normal.

O normal e o não reembolsável continuam como antes, os dados sobrevivem a um reinício e o banco impede a reserva
dupla.

## Dúvidas

- O README não foi atualizado: continua descrevendo só `STANDARD` e `NON_REFUNDABLE` (`README.md:32-38`). Não
  entrou em nenhuma nota, porque o texto não pedia isso.
- O código da empresa precisa estar exatamente em maiúsculas (`PriceCalculator.java:54`): "tecnosul" é recusado. O
  texto diz que o código "é o nome dela em maiúsculas", então tratei isso como uma leitura razoável, não como defeito.
