# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes. O `hotel.web` cuida do HTTP (`HttpApi`), o `hotel.reservations` guarda as regras (`ReservationService`, `PriceCalculator`) e o acesso ao banco (`ReservationStore`), e `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports` cuidam do resto. As peças são montadas em `hotel.Hotel`. Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:64-67`), passa ao `ReservationService.create` (`src/main/java/hotel/reservations/ReservationService.java:27-48`), que calcula o preço no `PriceCalculator`, e termina no `ReservationStore`, sobre PostgreSQL com migração Flyway. O pedido trocava o acréscimo da alta temporada de 20% para 25% e acrescentava a semana da Páscoa de 2027 e de 2028. O agente mexeu só no `PriceCalculator`: a constante passou a 0,25 e o `isHighSeason` virou uma lista de condições com os dois períodos da Páscoa. Ele também atualizou os testes que dependiam do valor antigo, escreveu um teste novo para a Páscoa e ajustou o README. Não achei nada sério: os testes passam (27 de 27) e o roteiro do comportamento passou inteiro (72 de 72).

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`), tudo igual na base. A única diferença em `measures/dependencies-base.md` é o tamanho de `hotel.reservations` (508 → 521 linhas), por causa do `PriceCalculator` (53 → 66).
**Nota:** bom

A mudança não mexeu na estrutura: ela fica toda em `src/main/java/hotel/reservations/PriceCalculator.java:18` e `:48-61`, no lugar onde a regra de preço já morava, sem pacote, dependência ou classe nova. A borda HTTP continua fina (`src/main/java/hotel/web/HttpApi.java:61-91`). As regras não conhecem o Javalin, e o SQL fica no `ReservationStore`. A montagem acontece num lugar só (`src/main/java/hotel/Hotel.java:20-29`). O ciclo `documents` ↔ `reservations` (`ReservationService.java:3` usa `ReservationReceipt`, que lê `Reservation`) já existia na base, e o agente não o piorou.

## Design de baixo nível

**Números:** 1 constante alterada (`HIGH_SEASON_SURCHARGE` 0,20 → 0,25). Os 5 períodos de alta temporada (3 da base e 2 novos da Páscoa) estão numa lista privada de `Predicate<LocalDate>`, com um método privado `between`. Não há interface, classe ou acréscimo por temporada.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto tenta o agente com "um acréscimo diferente para cada temporada", e ele não caiu nisso. O acréscimo continua sendo uma constante só (`PriceCalculator.java:18`), usada no único lugar que a aplica (`PriceCalculator.java:37-39`). As datas da Páscoa entram como dado, em dois intervalos fechados (`PriceCalculator.java:52-53`), com o método privado `between` (`PriceCalculator.java:59-61`). A referência dos controles aceita isso de antemão ("as datas da Páscoa entram como dado, uma lista de períodos"). Não foi criada interface de regra de temporada, nem classe por temporada, nem nada configurável. O que o agente fez a mais foi reescrever as três condições de mês que já existiam como lambdas na mesma lista (`PriceCalculator.java:49-51`), em vez de acrescentar duas condições ao `||`. É uma reescrita local do método que o pedido mandava mudar, sem estrutura nova. Ela deixa o código um pouco mais indireto que o `||` da base, e o comentário "A new season ... is one more entry here" (`PriceCalculator.java:45-46`) antecipa um futuro, mas não constrói nada para ele.

No resto do código, que vem todo da base: o dinheiro é `BigDecimal`, arredondado com `HALF_EVEN` (`PriceCalculator.java:63-65`), as datas são `LocalDate`, e os planos e as situações são `enum`s com `switch` exaustivo (`PriceCalculator.java:28-31`, `ReservationService.java:82-85`). Os erros vão para um `HotelException` com código (`HttpApi.java:94-107`). O único `catch (Exception)` (`HttpApi.java:113`) é da base e tem justificativa no comentário. Não vejo duplicação nem método longo que um revisor pediria para mudar.

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros, 0 pulados; 27 métodos escritos (a base tinha 26). Cobertura: linhas 476 de 507 (93,9%; na base, 93,8%), ramos 105 de 120 (87,5%; na base, 87,1%). Em `hotel.reservations`, 214 de 227 linhas e 53 de 56 ramos.
**Nota:** bom

O agente atualizou com valores os testes que dependiam dos 20%. São eles: `src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36` (15/12, 312,38, total 562,28), `:44-45` (29/02), `:71-74` (suíte não reembolsável: 2899,52, 434,93 e 2464,59) e, pela API, `src/test/java/hotel/web/HttpApiTest.java:77` e `:118` (974,76). Esses testes conferem números exatos, não só o status. O teste novo `easterWeekIsHighSeasonIn2027AndIn2028` (`PriceCalculatorTest.java:77-87`) confere as duas bordas de cada ano, com o dia anterior e o seguinte. A pergunta da mutação: se a constante voltasse a 0,20 ou fosse para outro valor, cinco testes falhariam. Se um dos intervalos da Páscoa sumisse, ou se um `!isAfter` virasse `isBefore`, o teste novo falharia. O agente não apagou nem enfraqueceu nenhum teste da base, e o de julho continua (`PriceCalculatorTest.java:49-54`). Falta um teste que passe pelo `calculate` com uma estadia na Páscoa. Isso pesa pouco, porque o acréscimo e o período são conferidos separadamente.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` nas 6 colunas, nenhuma chave estrangeira está sem índice e nenhuma data é guardada como texto. Há um `EXCLUDE` contra duas reservas na mesma noite. A migração `V1__create_tables.sql` está igual à da base. As conferências só acusaram as colunas `status`, `rate_plan`, `type` e `channel` sem `CHECK`.
**Nota:** bom

O pedido não mexia no banco, e o agente não mexeu: a única migração está igual à da base, então quem já roda o sistema sobe sem problema. O esquema tem os tipos certos (`src/main/resources/db/migration/V1__create_tables.sql:30-35`) e a exclusão que impede duas reservas do mesmo quarto na mesma noite (`V1__create_tables.sql:39-40`), traduzida em 409 no código (`src/main/java/hotel/reservations/ReservationStore.java:54-58`). O SQL usa parâmetros, e as concatenações listadas pelo script juntam só constantes (`ReservationStore.java:33`, `:98`, `:111`). Os pontos que um revisor apontaria vêm todos da base: as colunas de situação sem `CHECK` (`V1__create_tables.sql:33`) e os relatórios, que carregam a tabela inteira de reservas para filtrar em Java (`src/main/java/hotel/reports/RevenueReport.java:25`, `src/main/java/hotel/reports/OccupancyReport.java:25`). O preço fica gravado na reserva quando ela é criada, e por isso as reservas que já existiam mantêm o valor antigo, o que é o comportamento sensato.

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. A parte comum passou 46 de 46, os dados depois de reiniciar 3 de 3, o acréscimo novo 5 de 5, a semana da Páscoa 10 de 10, os outros períodos 6 de 6 e as duas reservas ao mesmo tempo 2 de 2.
**Nota:** bom

Para o dono da pousada: a noite de temporada do standard sai R$ 312,38, como ele pediu (o deluxe sai 487,38 e a suíte 724,88). O desconto do não reembolsável é calculado sobre o valor novo (46,86). As noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 são cobradas como alta temporada, e as noites vizinhas (20/03, 29/03, 08/04, 17/04) não. Uma estadia de 20 a 22/03/2027 soma uma noite comum e uma de temporada (562,28). Dezembro a partir do dia 15, janeiro, fevereiro e julho continuam iguais. Todo o resto do sistema (recusas, confirmação com e-mail, check-in e check-out, cancelamentos, relatórios, dados depois de reiniciar, reservas simultâneas) continua funcionando.

## Dúvidas

Fiquei em dúvida se reescrever as três condições de mês da base como lambdas numa `List<Predicate<LocalDate>>` (`PriceCalculator.java:48-57`) é só uma "lista de períodos", que conta como contenção, ou o começo de uma estrutura de "regras de temporada". Dei contenção porque a lista é privada, não há tipo, interface ou acréscimo por temporada, e tudo continua dentro do método que o pedido mandava mudar. Um revisor mais rígido poderia preferir as duas condições a mais no `||` da base.
