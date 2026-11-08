# Revisão: manutenção, cenário 01

## Visão geral

A base já estava em camadas: `hotel.web` (Javalin; `HttpApi` lê o JSON e monta um `NewReservation`) chama `hotel.reservations` (`ReservationService` com as regras, `PriceCalculator` com o preço, `ReservationStore` com o SQL por JDBC). O `hotel.Hotel` monta tudo, e os dados ficam no PostgreSQL, com o esquema criado por migrações do Flyway. O agente acrescentou os três planos (`CORPORATE`, `WEEKEND_PACKAGE`, `LONG_STAY`) ao `enum` `RatePlan`, o campo `companyCode` do pedido até o banco (migração nova `V2__add_company_code_column.sql`) e 20 testes. Pela API, tudo o que o texto pede funciona (72 de 72 conferências). O problema mais sério é de design: o texto avisa que vão chegar outros planos, mas a variação ficou em três `switch` sobre o plano, um para o preço (`PriceCalculator.java:41`), um para a validação (`ReservationService.java:54`) e um para a devolução (`ReservationService.java:119`). Além disso, o laço das diárias foi copiado quatro vezes. Assim, cada plano novo vai exigir mudanças em três lugares.

## Arquitetura

**Números:** 7 pacotes e 17 dependências entre eles, iguais aos da base (`measures/dependencies.md` e `measures/dependencies-base.md`). Há 1 ciclo, `hotel.documents` ↔ `hotel.reservations`, que já existia na base. O Javalin só aparece em `hotel` e `hotel.web`; o JDBC, em `hotel`, `hotel.messages`, `hotel.reservations` e `hotel.rooms`. `hotel.reservations` cresceu de 508 para 664 linhas.
**Nota:** bom

O agente não mudou a forma do sistema. O handler continua fino: ele só converte o `companyCode` em `Optional` (`final/src/main/java/hotel/web/HttpApi.java:128-129`) e o passa adiante. As regras novas ficaram no serviço e no cálculo, sem Javalin nem SQL (`ReservationService.java:53-78`, `PriceCalculator.java:35-136`), e o SQL novo ficou só no `ReservationStore` (`ReservationStore.java:19`, `:32-33`, `:50`, `:126-134`). O agente não criou dependência nova entre pacotes. O ciclo `documents` ↔ `reservations` vem da base. As classes que mais cresceram (`ReservationService` foi de 138 para 174 linhas, `PriceCalculator` de 53 para 141) ainda têm um tamanho razoável. O crescimento delas é um problema de design, tratado na próxima seção, e não de camadas.

## Design de baixo nível

**Números:** dos 5 planos, nenhum tem o próprio comportamento num lugar só dele. Há 3 `switch` de comportamento sobre `RatePlan`: o preço (`PriceCalculator.java:41-47`, 5 casos), a validação (`ReservationService.java:54-77`, 3 casos e um `default`) e a devolução (`ReservationService.java:119-124`, 5 casos). O laço das diárias aparece copiado 4 vezes.
**Classe:** omissão
**Exagero:** contenção
**Nota:** com problemas

**O pattern pedido (Strategy).** O texto diz que "o comercial vive inventando plano" e que cada plano traz o seu preço, a sua regra de quem pode usar e a sua devolução. Mesmo assim, o `RatePlan` continua um `enum` sem comportamento (`RatePlan.java:3-14`). Os dois `switch` que a base tinha foram ampliados de dois para cinco casos: o do preço virou `PriceCalculator.java:41-47`, que chama um método privado por plano, e o da devolução virou `ReservationService.java:119-124`. Um terceiro `switch` nasceu para a validação (`ReservationService.java:54-77`). Por isso, a próxima regra (melhor idade, 60 dias de antecedência) vai mexer no `enum`, no `PriceCalculator` e em dois `switch` do `ReservationService`. As regras de cada plano também ficaram espalhadas. Os dados do convênio estão em `PriceCalculator.java:25-28`, mas a recusa do convênio está em `ReservationService.java:55-60`. O mínimo de 7 noites está em `PriceCalculator.java:23`, mas a conferência dele está em `ReservationService.java:68-75`. O desconto de 50% do pacote é um literal dentro do serviço (`ReservationService.java:123`). Pela referência, isso é omissão: são `switch` de comportamento, e não um ponto único que só escolhe a implementação. Os métodos `calculateX` separados não mudam a classe, porque quem chama continua perguntando qual é o plano.

**O exagero.** O agente não criou interface, fábrica nem camada sem necessidade. Classifico como contenção.

**O resto do código:**
- **Duplicação:** o mesmo laço de diárias (contar as noites e somar `nightPrice`) foi copiado em `PriceCalculator.java:55-60`, `:65-70`, `:77-82` e `:94-99`. Os planos com desconto só diferem na taxa aplicada (`:71`, `:83`, `:100`).
- **Dois cálculos que contradizem a validação:** `calculateWeekendPackage` fixa `2` noites e ignora os parâmetros `checkIn` e `checkOut` que recebe (`PriceCalculator.java:87-90`). `getDiscountForCompany` devolve desconto zero, sem avisar, quando o código falta ou não existe (`PriceCalculator.java:104-109`). Os dois só dão certo porque o serviço validou antes, em outra classe. Se o `PriceCalculator` for chamado sem passar por essa validação, ele dá um preço errado sem nenhum erro.
- **`Optional` como campo:** o `Optional` virou componente de record e campo (`NewReservation.java:13`, `Reservation.java:16`), contra o uso idiomático em Java. Há também nomes totalmente qualificados no meio do código (`ReservationService.java:69`, `HttpApi.java:128-129`).
- **O que está certo:** dinheiro continua em `BigDecimal` com o mesmo `cents` HALF_EVEN, as datas em `LocalDate` e os planos no `enum`.

São defeitos localizados, mas reais, por isso a nota é `com problemas`.

## Testes

**Números:** 46 testes rodaram, com 0 falhas, 0 erros e 0 pulados; há 46 métodos escritos, contra 26 na base, todos passando. A cobertura de linhas foi de 93,8% para 94,4% (541 de 573), e a de ramos de 87,1% para 88,5% (131 de 148). No pacote `hotel.reservations`, a cobertura é de 273 de 287 linhas e 77 de 82 ramos (`measures/tests.md` e `measures/tests-base.md`).
**Nota:** com problemas

O agente não apagou nem mudou os testes da base: o patch só acrescenta linhas. Os três exemplos do financeiro são conferidos com valores:
- Tecnosul: 779,80 / 93,58 / 686,22 (`PriceCalculatorTest.java:78-91`);
- pacote no deluxe: 820,00 (`:94-106`);
- estadia longa: 1.749,30 / 174,93 / 1.574,37 (`:109-121`).

As devoluções também são conferidas com valores: 410,00 no pacote (`ReservationServiceTest.java:259-275`), tudo de volta na estadia longa (`:278-294`) e tudo de volta no convênio (`:297-313`). As recusas são conferidas no serviço, com o código `INVALID_REQUEST` (`:164-184`, `:203-213`, `:231-241`), e pela API, só pelo status 400 (`HttpApiTest.java:124-138`, `:155-162`, `:179-186`). Há testes nos três níveis, cálculo, serviço com o banco e API, e nenhum mock.

Fazendo a pergunta da mutação, várias regras novas ficam sem rede:
- **Agrovale e Farmavida:** se 0.18 virasse 0.81, ou 0.25 virasse 0.52 (`PriceCalculator.java:27-28`), nenhum teste falharia. `corporateCodeValidation` só confere as chaves do mapa (`PriceCalculatorTest.java:134-139`).
- **Pacote no standard e na suíte:** os preços de R$ 520,00 e R$ 1.200,00 (`PriceCalculator.java:31`, `:33`) não são conferidos, e nem o pacote em alta temporada.
- **Estadia longa com noites de temporada:** nenhum teste confere esse caso.
- **Datas do pacote:** se alguém apagasse a condição `checkIn.plusDays(2).equals(checkOut)` (`PriceCalculator.java:131`), nenhum teste falharia. O caso de sexta a segunda (`PriceCalculatorTest.java:130-131`) já é recusado pela condição do domingo, e falta um caso de sexta a um domingo de outra semana.
- **Erros na API:** os testes de recusa pela API não leem o corpo da resposta, então não conferem o `INVALID_REQUEST` (`HttpApiTest.java:130`, `:137`).

Os testes passam e cobrem bem o código. A nota é `com problemas` porque parte das regras novas ficou sem conferência de valor.

## Banco

**Números:** 3 tabelas, todas com chave primária. O Flyway aplicou 2 migrações: a `V1` está igual à da base, e a `V2__add_company_code_column.sql` é nova. O dinheiro está todo em `numeric(10,2)`, a chave estrangeira tem índice e as datas estão em `date`. A conferência acusou colunas de situação e de tipo sem `CHECK` (`rate_plan`, `status`, `type`, `channel`), todas vindas da base. Há 4 trechos de SQL montados com `+`, todos com texto constante (`measures/database.md`).
**Nota:** bom

A mudança no esquema foi feita do jeito certo: uma migração nova, que não muda a `V1` e acrescenta uma coluna que aceita nulo (`final/src/main/resources/db/migration/V2__add_company_code_column.sql:1`). Assim, um banco que já roda continua subindo, e as reservas antigas ficam com `company_code` nulo. O código continua com parâmetros (`ReservationStore.java:31-33`, `:50`) e lê a coluna nova com `Optional.ofNullable` (`:126`, `:134`). A proteção contra reserva duplicada da base continua: a restrição `EXCLUDE` (`V1__create_tables.sql:38-40`) e o tratamento do código `23P01` no `ReservationStore`. Um detalhe: o código da empresa é gravado mesmo quando o plano não é o convênio (`HttpApi.java:128-129` → `ReservationStore.java:50`). O texto diz que ele "pode ignorar" o campo nos outros planos, e isso não afeta o preço (veja as Dúvidas). A falta de `CHECK` em `rate_plan` vem da base.

## Comportamento

**Números:** 72 de 72 conferências passaram (`measures/behavior.md`):
- parte comum: 46 de 46;
- dados depois de reiniciar: 3 de 3;
- cenário 01: 21 de 21;
- reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada, tudo o que ele pediu funciona pela API, e os valores conferem com as contas do financeiro:
- Tecnosul: R$ 779,80 / R$ 93,58 / R$ 686,22;
- Agrovale e Farmavida com 18% e 25%;
- pacote: R$ 520,00 / R$ 820,00 / R$ 1.200,00, sem temporada, e devolve R$ 410,00 no deluxe;
- estadia longa: R$ 1.574,37, com a temporada valendo nas noites de julho;
- devolução integral no convênio e na estadia longa.

As recusas respondem 400 `INVALID_REQUEST`: convênio sem código ou com código desconhecido, pacote fora de sexta a domingo e estadia longa com 6 noites. O `companyCode` é ignorado no preço dos outros planos. O normal e o não reembolsável continuam iguais, e a parte comum, a persistência e a proteção contra reservas simultâneas seguem funcionando.

## Dúvidas

- O código da empresa fica guardado no banco também nas reservas que não são de convênio (`HttpApi.java:128-129`, `ReservationStore.java:50`). O texto diz "pode ignorar". O preço ignora o código, mas o dado fica gravado. É pequeno, e não pesou em nenhuma coluna.
- O código da empresa é comparado exatamente, sem `trim` nem conversão para maiúsculas (`PriceCalculator.java:135`). Assim, "tecnosul" é recusado. O texto diz que o código "é o nome dela em maiúsculas", e por isso não contei como erro.
